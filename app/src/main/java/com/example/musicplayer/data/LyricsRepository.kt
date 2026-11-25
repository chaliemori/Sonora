package com.example.musicplayer.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.zip.InflaterInputStream
import kotlin.math.abs

data class LyricWord(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val endMs: Long = 0L,
    val words: List<LyricWord> = emptyList()
)

data class TrackLyrics(
    val synced: List<LyricLine>,
    val plain: String?,
    val source: String
) {
    val isSynced: Boolean get() = synced.isNotEmpty()
    val isWordSynced: Boolean get() = synced.any { it.words.isNotEmpty() }
    val isEmpty: Boolean get() = synced.isEmpty() && plain.isNullOrBlank()
}

data class LyricsLookupResult(
    val lyrics: TrackLyrics?,
    val hadNetworkError: Boolean = false,
    val checkedProviders: List<String> = emptyList(),
    val alternatives: List<TrackLyrics> = emptyList()
)

private sealed interface HttpResponseResult {
    data class Ok(val body: String) : HttpResponseResult
    data class HttpError(val code: Int) : HttpResponseResult
    data class NetworkFailure(val reason: String) : HttpResponseResult
}

private data class QueryCandidate(
    val artist: String,
    val title: String,
    val combined: String,
    val durationSec: Int
)

private data class ProviderOutcome(
    val providerName: String,
    val lyrics: TrackLyrics?,
    val networkError: Boolean
)

private data class ScoredCandidate(
    val artistName: String,
    val trackName: String,
    val durationSec: Int,
    val lyrics: TrackLyrics,
    val baseScore: Int,
    val fingerprint: String
)

object LyricsRepository {

    private val LRC_TAG_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val INLINE_WORD_TAG_REGEX = Regex("""<(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?>""")
    private val LRC_OFFSET_REGEX = Regex("""\[offset:\s*([+-]?\d+)\s*]""", RegexOption.IGNORE_CASE)
    private val KRC_LINE_REGEX = Regex("""^\[(\d+),(\d+)](.*)$""")
    private val KRC_WORD_REGEX = Regex("""<(\d+),(\d+),\d+>([^<]*)""")
    private val KUGOU_KRC_KEY = byteArrayOf(
        64, 71, 97, 119, 94, 50, 116, 71,
        81, 54, 49, 45, 206.toByte(), 210.toByte(), 110, 105
    )

    private val LEADING_TRACK_NUM_REGEX = Regex("""^\d{1,3}\s*[.\-–_]\s+""")
    private val FILE_EXT_REGEX = Regex("""\.(?:mp3|flac|wav|m4a|aac|ogg|opus|dsf|dff|alac|ape|wv)$""", RegexOption.IGNORE_CASE)
    private val CLEAN_BRACKETS_REGEX = Regex(
        """\s*[\[(](?:feat\.?|ft\.?|official.*|audio|video|lyrics|visualizer|hd|hq|flac|mp3|dsd|explicit|clean|remaster(?:ed)?(?:\s*\d{4})?|prod\.?.*?)[\])]""",
        RegexOption.IGNORE_CASE
    )
    private val REUPLOAD_TAGS_REGEX = Regex(
        """\s*[\[(]?(?:slowed\s*(?:&|\+|and)?\s*reverb|speed\s*up|sped\s*up|nightcore|bass\s*boosted|tiktok\s*version|8d\s*audio|reupload)[\])]?""",
        RegexOption.IGNORE_CASE
    )
    private val ALL_BRACKETS_REGEX = Regex("""\s*[\[(][^\])]*[\])]""")
    private val FEAT_SPLIT_REGEX = Regex("""\s+(?:feat\.?|ft\.?|with|x)\s+.*$""", RegexOption.IGNORE_CASE)
    private val MULTI_ARTIST_SPLIT_REGEX = Regex("""\s*(?:,|;|&|/|\bfeat\.?\b|\bft\.?\b)\s*""", RegexOption.IGNORE_CASE)
    private val KUGOU_CREDIT_LINE_REGEX = Regex("""^(?:作曲|作词|编曲|制作人|混音|母带|监制|出品|吉他|贝斯|鼓|和声)\s*[:：]""")
    private val SPECIAL_VERSION_WORDS = listOf("remix", "cover", "karaoke", "instrumental", "tribute", "acoustic", "live")

    suspend fun searchLyrics(
        context: Context,
        track: LocalTrack,
        forceRefresh: Boolean = false,
        customQuery: String? = null
    ): LyricsLookupResult = withContext(Dispatchers.IO) {
        val cacheFile = cacheFileFor(context, track)

        if (!forceRefresh && customQuery.isNullOrBlank() && cacheFile.exists()) {
            loadFromCache(cacheFile)?.let { cached ->
                return@withContext LyricsLookupResult(
                    lyrics = cached,
                    hadNetworkError = false,
                    checkedProviders = listOf(cached.source),
                    alternatives = listOf(cached)
                )
            }
        }

        if (customQuery.isNullOrBlank()) {
            findLocalLrc(context, track)?.let { local ->
                saveToCache(cacheFile, local)
                return@withContext LyricsLookupResult(
                    lyrics = local,
                    hadNetworkError = false,
                    checkedProviders = listOf(local.source),
                    alternatives = listOf(local)
                )
            }
        }

        val candidates = buildQueryCandidates(track, customQuery)
        if (candidates.isEmpty()) {
            return@withContext LyricsLookupResult(lyrics = null, hadNetworkError = false)
        }

        val phase1 = fetchAcrossAllProviders(candidates)
        if (phase1.lyrics != null && !phase1.lyrics.isEmpty) {
            saveToCache(cacheFile, phase1.lyrics)
            return@withContext phase1
        }

        val titleOnly = extractPureTitle(track, customQuery)
        if (titleOnly.isNotBlank()) {
            val phase2 = fetchByTitleOnlyAndCompare(
                track = track,
                titleOnly = titleOnly,
                priorNetworkError = phase1.hadNetworkError
            )
            val winner = phase2.lyrics
            if (winner != null && !winner.isEmpty) {
                saveToCache(cacheFile, winner)
            }
            return@withContext phase2
        }

        phase1
    }

    suspend fun cacheSelection(context: Context, track: LocalTrack, lyrics: TrackLyrics) = withContext(Dispatchers.IO) {
        if (!lyrics.isEmpty) {
            saveToCache(cacheFileFor(context, track), lyrics)
        }
    }

    suspend fun getLyrics(
        context: Context,
        track: LocalTrack,
        forceRefresh: Boolean = false,
        customQuery: String? = null
    ): TrackLyrics? = searchLyrics(context, track, forceRefresh, customQuery).lyrics

    private suspend fun fetchAcrossAllProviders(
        candidates: List<QueryCandidate>
    ): LyricsLookupResult = coroutineScope {
        val primary = candidates.first()
        val relaxed = candidates.getOrNull(1)

        val lrcMuxDeferred = async { fetchFromLrcMux(primary, relaxed) }
        val lrcLibDeferred = async { fetchFromLrcLib(primary, relaxed) }
        val kuGouDeferred = async { fetchFromKuGou(primary, relaxed) }

        val outcomes = listOf(
            lrcMuxDeferred.await(),
            lrcLibDeferred.await(),
            kuGouDeferred.await()
        )

        val validLyrics = outcomes.mapNotNull { it.lyrics?.takeIf { l -> !l.isEmpty } }
        val sortedValid = validLyrics.sortedByDescending { lyricsPriorityScore(it) }
        val winner = sortedValid.firstOrNull()
        val hadNetworkErr = outcomes.all { it.lyrics == null && it.networkError }

        return@coroutineScope LyricsLookupResult(
            lyrics = winner,
            hadNetworkError = hadNetworkErr,
            checkedProviders = outcomes.map { it.providerName },
            alternatives = sortedValid.distinctBy { lyricsFingerprint(it) }.take(4)
        )
    }

    // ==================== Phase 2: Title-Only + Reuploader Comparison ====================

    private suspend fun fetchByTitleOnlyAndCompare(
        track: LocalTrack,
        titleOnly: String,
        priorNetworkError: Boolean
    ): LyricsLookupResult = coroutineScope {
        val durationSec = (track.durationMs / 1000L).toInt().coerceAtLeast(0)
        val lrcLibDeferred = async { collectLrcLibTitleCandidates(track, titleOnly, durationSec) }
        val kuGouDeferred = async { collectKuGouTitleCandidates(track, titleOnly, durationSec) }

        val (libList, libErr) = lrcLibDeferred.await()
        val (kugouList, kugouErr) = kuGouDeferred.await()
        val allCandidates = libList + kugouList

        if (allCandidates.isEmpty()) {
            return@coroutineScope LyricsLookupResult(
                lyrics = null,
                hadNetworkError = priorNetworkError && libErr && kugouErr,
                checkedProviders = listOf("LRCLIB (по названию)", "KuGou (по названию)")
            )
        }

        val groupCounts = allCandidates.groupingBy { it.fingerprint }.eachCount()

        val reranked = allCandidates
            .map { cand ->
                val consensusBonus = ((groupCounts[cand.fingerprint] ?: 1) - 1).coerceIn(0, 3) * 350
                cand to (cand.baseScore + consensusBonus)
            }
            .sortedByDescending { it.second }

        val uniqueTop = mutableListOf<Pair<ScoredCandidate, Int>>()
        val seenFingerprints = HashSet<String>()
        for (entry in reranked) {
            if (seenFingerprints.add(entry.first.fingerprint)) {
                uniqueTop.add(entry)
                if (uniqueTop.size >= 4) break
            }
        }

        val finalAlternatives = uniqueTop.mapIndexed { idx, (cand, _) ->
            if (idx == 0 && cand.artistName.isNotBlank() && cand.trackName.isNotBlank()) {
                val upgraded = tryUpgradeViaLrcMux(cand.artistName, cand.trackName, durationSec)
                if (upgraded != null &&
                    (upgraded.isWordSynced || (upgraded.isSynced && !cand.lyrics.isWordSynced))
                ) {
                    upgraded.copy(source = "${upgraded.source} • ${cand.artistName}")
                } else {
                    cand.lyrics
                }
            } else {
                cand.lyrics
            }
        }

        return@coroutineScope LyricsLookupResult(
            lyrics = finalAlternatives.firstOrNull(),
            hadNetworkError = false,
            checkedProviders = listOf("LRCLIB (по названию)", "KuGou (по названию)"),
            alternatives = finalAlternatives
        )
    }

    private fun tryUpgradeViaLrcMux(artist: String, title: String, durationSec: Int): TrackLyrics? {
        val url = buildString {
            append("https://api.lrcmux.dev/get?")
            append("artist=").append(encode(artist))
            append("&title=").append(encode(title))
            if (durationSec > 0) append("&duration=").append(durationSec)
        }
        return when (val res = httpGet(url, userAgent = "Sonora/1.2 (Android Music Player)")) {
            is HttpResponseResult.Ok -> parseLrcMuxJson(res.body)
            else -> null
        }
    }

    private fun collectLrcLibTitleCandidates(
        track: LocalTrack,
        titleOnly: String,
        durationSec: Int
    ): Pair<List<ScoredCandidate>, Boolean> {
        val urls = listOf(
            "https://lrclib.net/api/search?track_name=${encode(titleOnly)}",
            "https://lrclib.net/api/search?q=${encode(titleOnly)}"
        )
        val results = mutableListOf<ScoredCandidate>()
        val seenIds = HashSet<Long>()
        var allNetErr = true

        for (url in urls) {
            when (val res = httpGet(url)) {
                is HttpResponseResult.Ok -> {
                    allNetErr = false
                    val arr = try {
                        JSONArray(res.body)
                    } catch (_: Exception) {
                        continue
                    }
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val itemId = obj.optLong("id", i.toLong())
                        if (!seenIds.add(itemId)) continue
                        if (obj.optBoolean("instrumental", false)) continue

                        val candArtist = obj.optString("artistName", "").trim()
                        val candTitle = obj.optString("trackName", "").trim()
                        val candDur = obj.optDouble("duration", -1.0).toInt()
                        val label = if (candArtist.isNotBlank()) "LRCLIB • $candArtist" else "LRCLIB"
                        val parsed = parseLrcLibItem(obj, label) ?: continue
                        if (parsed.isEmpty) continue

                        val score = scoreCandidate(
                            track = track,
                            queryTitle = titleOnly,
                            trackDurSec = durationSec,
                            candArtist = candArtist,
                            candTitle = candTitle,
                            candDurSec = candDur,
                            lyrics = parsed
                        )
                        results.add(
                            ScoredCandidate(
                                artistName = candArtist,
                                trackName = candTitle,
                                durationSec = candDur,
                                lyrics = parsed,
                                baseScore = score,
                                fingerprint = lyricsFingerprint(parsed)
                            )
                        )
                    }
                }
                is HttpResponseResult.HttpError -> allNetErr = false
                is HttpResponseResult.NetworkFailure -> Unit
            }
            if (results.size >= 15) break
        }
        return results to allNetErr
    }

    private fun collectKuGouTitleCandidates(
        track: LocalTrack,
        titleOnly: String,
        durationSec: Int
    ): Pair<List<ScoredCandidate>, Boolean> {
        val searchUrl = "https://mobileservice.kugou.com/api/v3/search/song?keyword=${encode(titleOnly)}&page=1&pagesize=12&plat=0&version=9108"
        val songList = when (val res = httpGet(searchUrl)) {
            is HttpResponseResult.Ok -> {
                try {
                    JSONObject(res.body).optJSONObject("data")?.optJSONArray("info")
                } catch (_: Exception) {
                    null
                }
            }
            is HttpResponseResult.HttpError -> return emptyList<ScoredCandidate>() to false
            is HttpResponseResult.NetworkFailure -> return emptyList<ScoredCandidate>() to true
        } ?: return emptyList<ScoredCandidate>() to false

        data class RawKuGouSong(
            val hash: String,
            val artist: String,
            val title: String,
            val durSec: Int,
            val preScore: Int
        )

        val songs = mutableListOf<RawKuGouSong>()
        val seenHashes = HashSet<String>()
        for (i in 0 until songList.length()) {
            val item = songList.optJSONObject(i) ?: continue
            val hash = item.optString("hash", "").ifBlank { item.optString("320hash", "") }
            if (hash.isBlank() || !seenHashes.add(hash)) continue

            val singer = item.optString("singername", "").trim()
            val filename = item.optString("filename", "").trim()
            val songName = if (filename.contains(" - ")) {
                filename.substringAfter(" - ").trim()
            } else {
                filename
            }
            val itemDur = item.optInt("duration", 0)
            val diffPenalty = if (durationSec > 0 && itemDur > 0) abs(itemDur - durationSec) * 15 else 200
            val titleBonus = computeTitleMatchScore(titleOnly, songName, track.title)
            val hintBonus = computeCrossFieldArtistBonus(track, singer)
            songs.add(
                RawKuGouSong(
                    hash = hash,
                    artist = singer,
                    title = songName,
                    durSec = itemDur,
                    preScore = titleBonus + hintBonus - diffPenalty
                )
            )
        }

        val collected = mutableListOf<ScoredCandidate>()
        for (song in songs.sortedByDescending { it.preScore }.take(4)) {
            val krcSearchUrl = buildString {
                append("https://krcs.kugou.com/search?ver=1&client=mobi")
                append("&hash=").append(encode(song.hash))
                if (song.durSec > 0) append("&duration=").append(song.durSec * 1000L)
            }
            val krcBody = (httpGet(krcSearchUrl) as? HttpResponseResult.Ok)?.body ?: continue
            val firstCand = try {
                JSONObject(krcBody).optJSONArray("candidates")?.optJSONObject(0)
            } catch (_: Exception) {
                null
            } ?: continue

            val id = firstCand.optString("id", "")
            val key = firstCand.optString("accesskey", "")
            if (id.isBlank() || key.isBlank()) continue

            val label = if (song.artist.isNotBlank()) "KuGou • ${song.artist}" else "KuGou"
            val lyrics = downloadKuGouLyrics(id, key, label) ?: continue

            val score = scoreCandidate(
                track = track,
                queryTitle = titleOnly,
                trackDurSec = durationSec,
                candArtist = song.artist,
                candTitle = song.title,
                candDurSec = song.durSec,
                lyrics = lyrics
            )
            collected.add(
                ScoredCandidate(
                    artistName = song.artist,
                    trackName = song.title,
                    durationSec = song.durSec,
                    lyrics = lyrics,
                    baseScore = score,
                    fingerprint = lyricsFingerprint(lyrics)
                )
            )
        }
        return collected to false
    }

    private fun scoreCandidate(
        track: LocalTrack,
        queryTitle: String,
        trackDurSec: Int,
        candArtist: String,
        candTitle: String,
        candDurSec: Int,
        lyrics: TrackLyrics
    ): Int {
        var score = 0

        if (trackDurSec > 0 && candDurSec > 0) {
            val diff = abs(candDurSec - trackDurSec)
            score += when {
                diff <= 1 -> 1200
                diff <= 3 -> 950
                diff <= 6 -> 650
                diff <= 12 -> 250
                diff <= 25 -> 0
                else -> -1200
            }
        }

        score += computeTitleMatchScore(queryTitle, candTitle, track.title)
        score += computeCrossFieldArtistBonus(track, candArtist)

        if (lyrics.isWordSynced) {
            score += 650
        } else if (lyrics.isSynced) {
            score += if (lyrics.synced.size >= 5) 450 else 180
        }
        val plainLen = lyrics.plain?.length ?: 0
        if (plainLen < 40) {
            score -= 250
        } else {
            score += (plainLen / 40).coerceAtMost(80)
        }

        return score
    }

    private fun computeTitleMatchScore(queryTitle: String, candTitle: String, rawTrackTitle: String): Int {
        val qNorm = normalizeForCompare(queryTitle)
        val cNorm = normalizeForCompare(ALL_BRACKETS_REGEX.replace(candTitle, ""))
        val cFullNorm = normalizeForCompare(candTitle)
        if (qNorm.isBlank() || cFullNorm.isBlank()) return 0

        var score = when {
            qNorm == cFullNorm || qNorm == cNorm -> 600
            cNorm.contains(qNorm) || qNorm.contains(cNorm) -> 320
            else -> {
                val qTokens = qNorm.split(' ').filter { it.length >= 2 }.toSet()
                val cTokens = cFullNorm.split(' ').filter { it.length >= 2 }.toSet()
                val overlap = qTokens.intersect(cTokens).size
                if (qTokens.isEmpty()) 0 else ((overlap * 350) / qTokens.size) - 100
            }
        }

        val rawLower = rawTrackTitle.lowercase()
        val candLower = candTitle.lowercase()
        for (word in SPECIAL_VERSION_WORDS) {
            if (candLower.contains(word) && !rawLower.contains(word)) {
                score -= 350
            }
        }
        return score
    }

    private fun computeCrossFieldArtistBonus(track: LocalTrack, candArtist: String): Int {
        if (candArtist.isBlank()) return 0
        val candNorm = normalizeForCompare(candArtist)
        if (candNorm.isBlank()) return 0
        val candTokens = candNorm.split(' ').filter { it.length >= 3 }.toSet()
        if (candTokens.isEmpty()) return 0

        val metaText = normalizeForCompare(
            "${cleanUnknownArtist(track.artist)} ${track.album} ${track.folderName} ${track.title}"
        )
        if (metaText.contains(candNorm)) return 900

        val metaTokens = metaText.split(' ').filter { it.length >= 3 }.toSet()
        val matches = candTokens.intersect(metaTokens).size
        return (matches * 420).coerceAtMost(840)
    }

    private fun normalizeForCompare(raw: String): String {
        return raw.lowercase()
            .replace('ё', 'е')
            .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun lyricsFingerprint(lyrics: TrackLyrics): String {
        val text = (lyrics.plain ?: lyrics.synced.joinToString(" ") { it.text })
            .lowercase()
            .replace(Regex("""[^\p{L}\p{N}]"""), "")
        return text.take(96).ifBlank { lyrics.source }
    }

    private fun extractPureTitle(track: LocalTrack, customQuery: String?): String {
        val raw = if (!customQuery.isNullOrBlank()) {
            val split = splitArtistTitle(customQuery.trim())
            split?.second ?: customQuery.trim()
        } else {
            val split = splitArtistTitle(cleanTitleBase(track.title))
            split?.second ?: cleanTitleBase(track.title)
        }
        var t = FILE_EXT_REGEX.replace(raw, "")
        t = REUPLOAD_TAGS_REGEX.replace(t, "")
        t = FEAT_SPLIT_REGEX.replace(ALL_BRACKETS_REGEX.replace(t, ""), "")
        return t.replace(Regex("""\s+"""), " ").trim()
    }

    private fun lyricsPriorityScore(lyrics: TrackLyrics): Int {
        val syncBonus = when {
            lyrics.isWordSynced && lyrics.synced.size >= 4 -> 14_000
            lyrics.isWordSynced -> 11_000
            lyrics.isSynced && lyrics.synced.size >= 4 -> 10_000
            lyrics.isSynced -> 5_000
            else -> 0
        }
        val sourceBonus = when {
            lyrics.source.startsWith("LRCMux", ignoreCase = true) -> 300
            lyrics.source.startsWith("KuGou", ignoreCase = true) -> 220
            lyrics.source.startsWith("LRCLIB", ignoreCase = true) -> 180
            else -> 50
        }
        val lengthBonus = (lyrics.synced.size * 5).coerceAtMost(250) +
            ((lyrics.plain?.length ?: 0) / 50).coerceAtMost(50)
        return syncBonus + sourceBonus + lengthBonus
    }

    private fun buildQueryCandidates(track: LocalTrack, customQuery: String?): List<QueryCandidate> {
        val durationSec = (track.durationMs / 1000L).toInt().coerceAtLeast(0)

        if (!customQuery.isNullOrBlank()) {
            val q = customQuery.trim()
            val splitMatch = splitArtistTitle(q)
            return if (splitMatch != null) {
                listOf(
                    QueryCandidate(
                        artist = splitMatch.first,
                        title = splitMatch.second,
                        combined = "${splitMatch.first} ${splitMatch.second}".trim(),
                        durationSec = durationSec
                    )
                )
            } else {
                listOf(
                    QueryCandidate(
                        artist = "",
                        title = "",
                        combined = q,
                        durationSec = durationSec
                    )
                )
            }
        }

        val rawArtist = cleanUnknownArtist(track.artist)
        var cleanTitle = cleanTitleBase(track.title)
        var cleanArtist = cleanArtistBase(rawArtist)

        if (cleanArtist.isBlank()) {
            splitArtistTitle(cleanTitle)?.let { (a, t) ->
                cleanArtist = a
                cleanTitle = t
            }
        }

        if (cleanArtist.isBlank() && cleanTitle.isBlank()) return emptyList()

        val primaryCombined = listOf(cleanArtist, cleanTitle).filter { it.isNotBlank() }.joinToString(" ")
        val primary = QueryCandidate(
            artist = cleanArtist,
            title = cleanTitle,
            combined = primaryCombined,
            durationSec = durationSec
        )

        val relaxedArtist = primaryArtistOnly(cleanArtist)
        val relaxedTitle = extractPureTitle(track, null)
        val relaxedCombined = listOf(relaxedArtist, relaxedTitle).filter { it.isNotBlank() }.joinToString(" ")

        return if (relaxedCombined.isNotBlank() &&
            (!relaxedArtist.equals(cleanArtist, ignoreCase = true) ||
                !relaxedTitle.equals(cleanTitle, ignoreCase = true))
        ) {
            listOf(
                primary,
                QueryCandidate(
                    artist = relaxedArtist,
                    title = relaxedTitle,
                    combined = relaxedCombined,
                    durationSec = durationSec
                )
            )
        } else {
            listOf(primary)
        }
    }

    // ==================== 1. LRCMux (Deezer + KuGou + YouTube Music, word-level) ====================

    private fun fetchFromLrcMux(primary: QueryCandidate, relaxed: QueryCandidate?): ProviderOutcome {
        var anyNetErr = false

        fun queryCandidate(cand: QueryCandidate): TrackLyrics? {
            if (cand.artist.isBlank() || cand.title.isBlank()) return null
            val url = buildString {
                append("https://api.lrcmux.dev/get?")
                append("artist=").append(encode(cand.artist))
                append("&title=").append(encode(cand.title))
                if (cand.durationSec > 0) {
                    append("&duration=").append(cand.durationSec)
                }
            }
            when (val res = httpGet(url, userAgent = "Sonora/1.2 (Android Music Player)")) {
                is HttpResponseResult.Ok -> {
                    parseLrcMuxJson(res.body)?.let { return it }
                }
                is HttpResponseResult.NetworkFailure -> anyNetErr = true
                is HttpResponseResult.HttpError -> Unit
            }
            return null
        }

        queryCandidate(primary)?.let {
            return ProviderOutcome("LRCMux", it, false)
        }
        if (relaxed != null) {
            queryCandidate(relaxed)?.let {
                return ProviderOutcome("LRCMux", it, false)
            }
        }
        return ProviderOutcome("LRCMux", null, anyNetErr)
    }

    private fun parseLrcMuxJson(body: String): TrackLyrics? {
        return try {
            val obj = JSONObject(body)
            val meta = obj.optJSONObject("meta")
            val level = meta?.optString("level", "none") ?: "none"
            val srcName = meta?.optJSONObject("source")?.optString("name", "")?.takeIf { it.isNotBlank() }
            val label = if (srcName != null) "LRCMux • $srcName" else "LRCMux"

            val linesArr = obj.optJSONArray("lines") ?: return null
            if (linesArr.length() == 0) return null

            val synced = mutableListOf<LyricLine>()
            val plainLines = mutableListOf<String>()

            for (i in 0 until linesArr.length()) {
                val item = linesArr.optJSONObject(i) ?: continue
                val text = INLINE_WORD_TAG_REGEX.replace(item.optString("text", ""), "").trim()
                if (text.isEmpty()) continue
                plainLines.add(text)

                if (level != "none" && item.has("start")) {
                    val startMs = item.optLong("start", -1L)
                    val endMs = item.optLong("end", 0L)
                    if (startMs >= 0L) {
                        val wordsArr = item.optJSONArray("words")
                        val parsedWords = if (wordsArr != null && wordsArr.length() > 0) {
                            buildList {
                                for (wIdx in 0 until wordsArr.length()) {
                                    val wObj = wordsArr.optJSONObject(wIdx) ?: continue
                                    val wText = wObj.optString("text", "")
                                    if (wText.isEmpty()) continue
                                    val wStart = wObj.optLong("start", startMs)
                                    val wEnd = wObj.optLong("end", wStart + 120L).coerceAtLeast(wStart)
                                    add(LyricWord(wStart, wEnd, wText))
                                }
                            }
                        } else {
                            emptyList()
                        }
                        synced.add(
                            LyricLine(
                                timeMs = startMs,
                                text = text,
                                endMs = endMs,
                                words = parsedWords
                            )
                        )
                    }
                }
            }

            if (synced.isEmpty() && plainLines.isEmpty()) return null
            TrackLyrics(
                synced = synced.sortedBy { it.timeMs },
                plain = plainLines.joinToString("\n"),
                source = label
            )
        } catch (_: Exception) {
            null
        }
    }

    // ==================== 2. LRCLIB ====================

    private fun fetchFromLrcLib(primary: QueryCandidate, relaxed: QueryCandidate?): ProviderOutcome {
        var anyNetErr = false

        if (primary.artist.isNotBlank() && primary.title.isNotBlank() && primary.durationSec > 0) {
            val getUrl = buildString {
                append("https://lrclib.net/api/get?")
                append("track_name=").append(encode(primary.title))
                append("&artist_name=").append(encode(primary.artist))
                append("&duration=").append(primary.durationSec)
            }
            when (val res = httpGet(getUrl)) {
                is HttpResponseResult.Ok -> {
                    try {
                        parseLrcLibItem(JSONObject(res.body), "LRCLIB")?.takeIf { !it.isEmpty }?.let {
                            return ProviderOutcome("LRCLIB", it, false)
                        }
                    } catch (_: Exception) {
                    }
                }
                is HttpResponseResult.NetworkFailure -> anyNetErr = true
                is HttpResponseResult.HttpError -> Unit
            }
        }

        fun searchQuery(query: String, durationSec: Int): TrackLyrics? {
            if (query.isBlank()) return null
            val url = "https://lrclib.net/api/search?q=${encode(query)}"
            when (val res = httpGet(url)) {
                is HttpResponseResult.Ok -> {
                    val arr = try {
                        JSONArray(res.body)
                    } catch (_: Exception) {
                        return null
                    }
                    var bestSynced: TrackLyrics? = null
                    var bestSyncedDiff = Int.MAX_VALUE
                    var bestPlain: TrackLyrics? = null
                    var bestPlainDiff = Int.MAX_VALUE

                    for (i in 0 until arr.length()) {
                        val item = arr.optJSONObject(i) ?: continue
                        val parsed = parseLrcLibItem(item, "LRCLIB") ?: continue
                        if (parsed.isEmpty) continue

                        val itemDur = item.optDouble("duration", -1.0).toInt()
                        val diff = if (durationSec > 0 && itemDur > 0) abs(itemDur - durationSec) else 0
                        if (durationSec > 0 && itemDur > 0 && diff > 25 && arr.length() > 1) continue

                        if (parsed.isSynced && diff < bestSyncedDiff) {
                            bestSynced = parsed
                            bestSyncedDiff = diff
                        } else if (!parsed.isSynced && diff < bestPlainDiff) {
                            bestPlain = parsed
                            bestPlainDiff = diff
                        }
                    }
                    return bestSynced ?: bestPlain
                }
                is HttpResponseResult.NetworkFailure -> anyNetErr = true
                is HttpResponseResult.HttpError -> Unit
            }
            return null
        }

        searchQuery(primary.combined, primary.durationSec)?.let {
            return ProviderOutcome("LRCLIB", it, false)
        }
        if (relaxed != null) {
            searchQuery(relaxed.combined, relaxed.durationSec)?.let {
                return ProviderOutcome("LRCLIB", it, false)
            }
        }
        return ProviderOutcome("LRCLIB", null, anyNetErr)
    }

    private fun parseLrcLibItem(obj: JSONObject, source: String): TrackLyrics? {
        val syncedRaw = obj.optString("syncedLyrics", "").takeIf { it.isNotBlank() && it != "null" }
        val plainRaw = obj.optString("plainLyrics", "").takeIf { it.isNotBlank() && it != "null" }
        val syncedLines = if (syncedRaw != null) parseLrcText(syncedRaw) else emptyList()
        if (syncedLines.isEmpty() && plainRaw.isNullOrBlank()) return null
        return TrackLyrics(
            synced = syncedLines,
            plain = plainRaw ?: syncedLines.joinToString("\n") { it.text },
            source = source
        )
    }

    // ==================== 3. KuGou Direct (KRC word-level + LRC fallback) ====================

    private fun fetchFromKuGou(primary: QueryCandidate, relaxed: QueryCandidate?): ProviderOutcome {
        var anyNetErr = false

        fun searchCandidate(cand: QueryCandidate): TrackLyrics? {
            if (cand.combined.isBlank()) return null

            val searchUrl = "https://mobileservice.kugou.com/api/v3/search/song?keyword=${encode(cand.combined)}&page=1&pagesize=8&plat=0&version=9108"
            val songList = when (val res = httpGet(searchUrl)) {
                is HttpResponseResult.Ok -> {
                    try {
                        JSONObject(res.body).optJSONObject("data")?.optJSONArray("info")
                    } catch (_: Exception) {
                        null
                    }
                }
                is HttpResponseResult.NetworkFailure -> {
                    anyNetErr = true
                    null
                }
                is HttpResponseResult.HttpError -> null
            }

            data class SongHashCandidate(val hash: String, val durationMs: Long, val score: Int)
            val hashes = mutableListOf<SongHashCandidate>()

            if (songList != null) {
                for (i in 0 until songList.length()) {
                    val item = songList.optJSONObject(i) ?: continue
                    val hash = item.optString("hash", "").ifBlank { item.optString("320hash", "") }
                    if (hash.isBlank()) continue
                    val itemDurSec = item.optInt("duration", 0)
                    val durMs = (itemDurSec.toLong() * 1000L).coerceAtLeast(cand.durationSec * 1000L)
                    val diff = if (cand.durationSec > 0 && itemDurSec > 0) abs(itemDurSec - cand.durationSec) else 10
                    val filename = item.optString("filename", "").lowercase()
                    val artistMatch = if (cand.artist.isNotBlank() && filename.contains(cand.artist.lowercase())) -15 else 0
                    hashes.add(SongHashCandidate(hash, durMs, diff + artistMatch))
                }
            }

            val candidatesToTry = mutableListOf<Pair<String, String>>()

            for (entry in hashes.sortedBy { it.score }.take(3)) {
                val krcSearchUrl = buildString {
                    append("https://krcs.kugou.com/search?ver=1&client=mobi")
                    append("&hash=").append(encode(entry.hash))
                    if (entry.durationMs > 0L) {
                        append("&duration=").append(entry.durationMs)
                    }
                }
                when (val krcRes = httpGet(krcSearchUrl)) {
                    is HttpResponseResult.Ok -> {
                        try {
                            val arr = JSONObject(krcRes.body).optJSONArray("candidates")
                            if (arr != null && arr.length() > 0) {
                                val first = arr.getJSONObject(0)
                                val id = first.optString("id", "")
                                val key = first.optString("accesskey", "")
                                if (id.isNotBlank() && key.isNotBlank()) {
                                    candidatesToTry.add(id to key)
                                    break
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                    is HttpResponseResult.NetworkFailure -> anyNetErr = true
                    is HttpResponseResult.HttpError -> Unit
                }
            }

            if (candidatesToTry.isEmpty()) {
                val fallbackKrcUrl = buildString {
                    append("https://krcs.kugou.com/search?ver=1&client=mobi")
                    append("&keyword=").append(encode(cand.combined))
                    if (cand.durationSec > 0) {
                        append("&duration=").append(cand.durationSec * 1000L)
                    }
                }
                when (val krcRes = httpGet(fallbackKrcUrl)) {
                    is HttpResponseResult.Ok -> {
                        try {
                            val arr = JSONObject(krcRes.body).optJSONArray("candidates")
                            if (arr != null && arr.length() > 0) {
                                val first = arr.getJSONObject(0)
                                val id = first.optString("id", "")
                                val key = first.optString("accesskey", "")
                                if (id.isNotBlank() && key.isNotBlank()) {
                                    candidatesToTry.add(id to key)
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                    is HttpResponseResult.NetworkFailure -> anyNetErr = true
                    is HttpResponseResult.HttpError -> Unit
                }
            }

            for ((id, accesskey) in candidatesToTry) {
                downloadKuGouLyrics(id, accesskey, "KuGou")?.let { return it }
            }

            return null
        }

        searchCandidate(primary)?.let {
            return ProviderOutcome("KuGou", it, false)
        }
        if (relaxed != null) {
            searchCandidate(relaxed)?.let {
                return ProviderOutcome("KuGou", it, false)
            }
        }
        return ProviderOutcome("KuGou", null, anyNetErr)
    }

    private fun downloadKuGouLyrics(id: String, accesskey: String, label: String): TrackLyrics? {
        // 1. Try KRC first (word-by-word karaoke timing!)
        val krcUrl = "https://lyrics.kugou.com/download?ver=1&client=pc&id=${encode(id)}&accesskey=${encode(accesskey)}&fmt=krc&charset=utf8"
        (httpGet(krcUrl) as? HttpResponseResult.Ok)?.let { res ->
            try {
                val b64 = JSONObject(res.body).optString("content", "")
                if (b64.isNotBlank()) {
                    decodeKuGouKrc(b64)?.let { rawKrc ->
                        val parsed = parseKrcText(rawKrc)
                        if (parsed.isNotEmpty()) {
                            return TrackLyrics(
                                synced = parsed,
                                plain = parsed.joinToString("\n") { it.text },
                                source = label
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        // 2. Fallback to standard LRC
        val lrcUrl = "https://lyrics.kugou.com/download?ver=1&client=pc&id=${encode(id)}&accesskey=${encode(accesskey)}&fmt=lrc&charset=utf8"
        (httpGet(lrcUrl) as? HttpResponseResult.Ok)?.let { res ->
            try {
                val b64 = JSONObject(res.body).optString("content", "")
                if (b64.isNotBlank()) {
                    val rawLrc = String(Base64.decode(b64, Base64.DEFAULT), Charsets.UTF_8)
                    val parsed = parseLrcText(rawLrc, filterKuGouCredits = true)
                    if (parsed.isNotEmpty()) {
                        return TrackLyrics(
                            synced = parsed,
                            plain = parsed.joinToString("\n") { it.text },
                            source = label
                        )
                    }
                    val plainFallback = rawLrc.lines()
                        .map { LRC_TAG_REGEX.replace(it, "").trim() }
                        .filter { it.isNotBlank() && !it.startsWith("[") }
                        .joinToString("\n")
                    if (plainFallback.isNotBlank()) {
                        return TrackLyrics(synced = emptyList(), plain = plainFallback, source = label)
                    }
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun decodeKuGouKrc(base64Content: String): String? {
        return try {
            val raw = Base64.decode(base64Content, Base64.DEFAULT)
            if (raw.size <= 4) return null
            val decrypted = ByteArray(raw.size - 4)
            for (i in decrypted.indices) {
                decrypted[i] = (raw[i + 4].toInt() xor KUGOU_KRC_KEY[i % 16].toInt()).toByte()
            }
            InflaterInputStream(ByteArrayInputStream(decrypted)).bufferedReader(Charsets.UTF_8).use {
                it.readText()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun parseKrcText(raw: String): List<LyricLine> {
        val offsetMs = LRC_OFFSET_REGEX.find(raw)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        val lines = mutableListOf<LyricLine>()

        raw.lineSequence().forEach { line ->
            val match = KRC_LINE_REGEX.matchEntire(line.trim()) ?: return@forEach
            val lineStart = (match.groupValues[1].toLongOrNull() ?: return@forEach) - offsetMs
            val lineDur = match.groupValues[2].toLongOrNull() ?: 0L
            val content = match.groupValues[3]

            val words = mutableListOf<LyricWord>()
            val textBuilder = StringBuilder()

            for (wMatch in KRC_WORD_REGEX.findAll(content)) {
                val relStart = wMatch.groupValues[1].toLongOrNull() ?: 0L
                val wDur = wMatch.groupValues[2].toLongOrNull() ?: 0L
                val wText = wMatch.groupValues[3]
                if (wText.isEmpty()) continue
                val absStart = (lineStart + relStart).coerceAtLeast(0L)
                val absEnd = (absStart + wDur).coerceAtLeast(absStart)
                words.add(LyricWord(startMs = absStart, endMs = absEnd, text = wText))
                textBuilder.append(wText)
            }

            val fullText = textBuilder.toString().trim()
            if (fullText.isBlank()) return@forEach
            if (KUGOU_CREDIT_LINE_REGEX.containsMatchIn(fullText)) return@forEach

            lines.add(
                LyricLine(
                    timeMs = lineStart.coerceAtLeast(0L),
                    text = fullText,
                    endMs = (lineStart + lineDur).coerceAtLeast(0L),
                    words = words
                )
            )
        }
        return lines.sortedBy { it.timeMs }
    }

    // ==================== LRC Parsing (with Enhanced LRC word tags) & Helpers ====================

    fun parseLrcText(raw: String, filterKuGouCredits: Boolean = false): List<LyricLine> {
        val offsetMs = LRC_OFFSET_REGEX.find(raw)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        val lines = mutableListOf<LyricLine>()

        raw.lineSequence().forEach { line ->
            val matches = LRC_TAG_REGEX.findAll(line).toList()
            if (matches.isEmpty()) return@forEach
            val rawAfterTags = LRC_TAG_REGEX.replace(line, "")
            val cleaned = INLINE_WORD_TAG_REGEX.replace(rawAfterTags, "").trim()
            if (cleaned.isEmpty()) return@forEach

            val firstLineTimeMs = parseTagMs(matches.first(), offsetMs)
            val inlineWords = parseEnhancedLrcWords(rawAfterTags, firstLineTimeMs, offsetMs)

            for ((mIdx, m) in matches.withIndex()) {
                val timeMs = parseTagMs(m, offsetMs)
                lines.add(
                    LyricLine(
                        timeMs = timeMs,
                        text = cleaned,
                        words = if (mIdx == 0) inlineWords else emptyList()
                    )
                )
            }
        }

        val sorted = lines.sortedBy { it.timeMs }
        if (filterKuGouCredits && sorted.size > 4) {
            val withoutCredits = sorted.filterNot { KUGOU_CREDIT_LINE_REGEX.containsMatchIn(it.text) }
            if (withoutCredits.isNotEmpty()) return withoutCredits
        }
        return sorted
    }

    private fun parseTagMs(match: MatchResult, offsetMs: Long): Long {
        val min = match.groupValues[1].toLongOrNull() ?: 0L
        val sec = match.groupValues[2].toLongOrNull() ?: 0L
        val fracStr = match.groupValues[3]
        val msPart = when (fracStr.length) {
            0 -> 0L
            1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
            2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
            else -> fracStr.take(3).toLongOrNull() ?: 0L
        }
        return (min * 60_000L + sec * 1_000L + msPart - offsetMs).coerceAtLeast(0L)
    }

    private fun parseEnhancedLrcWords(rawBody: String, lineStartMs: Long, offsetMs: Long): List<LyricWord> {
        val tagMatches = INLINE_WORD_TAG_REGEX.findAll(rawBody).toList()
        if (tagMatches.isEmpty()) return emptyList()
        val words = mutableListOf<LyricWord>()
        for (i in tagMatches.indices) {
            val tag = tagMatches[i]
            val startMs = parseTagMs(tag, offsetMs)
            val endIndex = if (i + 1 < tagMatches.size) tagMatches[i + 1].range.first else rawBody.length
            val segText = rawBody.substring(tag.range.last + 1, endIndex)
            if (segText.isEmpty()) continue
            val nextStartMs = if (i + 1 < tagMatches.size) {
                parseTagMs(tagMatches[i + 1], offsetMs)
            } else {
                startMs + 350L
            }
            words.add(
                LyricWord(
                    startMs = startMs.coerceAtLeast(lineStartMs),
                    endMs = nextStartMs.coerceAtLeast(startMs),
                    text = segText
                )
            )
        }
        return words
    }

    private fun findLocalLrc(context: Context, track: LocalTrack): TrackLyrics? {
        return try {
            val filePath = resolveFilePath(context, track.contentUri) ?: return null
            val base = filePath.substringBeforeLast('.')
            val lrcFile = File("$base.lrc")
            if (!lrcFile.exists() || !lrcFile.canRead()) return null
            val raw = lrcFile.readText()
            val parsed = parseLrcText(raw)
            if (parsed.isNotEmpty()) {
                TrackLyrics(synced = parsed, plain = parsed.joinToString("\n") { it.text }, source = "LRC файл")
            } else if (raw.isNotBlank()) {
                TrackLyrics(synced = emptyList(), plain = raw.trim(), source = "LRC файл")
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveFilePath(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.path
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Audio.Media.DATA),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun httpGet(
        urlStr: String,
        userAgent: String = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
    ): HttpResponseResult {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 10000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "application/json, text/plain, */*")
            }
            val code = conn.responseCode
            if (code in 200..299) {
                val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                HttpResponseResult.Ok(body)
            } else {
                HttpResponseResult.HttpError(code)
            }
        } catch (e: Exception) {
            HttpResponseResult.NetworkFailure(e.message ?: "network error")
        } finally {
            conn?.disconnect()
        }
    }

    private fun cleanUnknownArtist(raw: String): String {
        val trimmed = raw.trim()
        return if (
            trimmed.equals("Неизвестный исполнитель", ignoreCase = true) ||
            trimmed.equals("<unknown>", ignoreCase = true) ||
            trimmed.equals("unknown artist", ignoreCase = true) ||
            trimmed.equals("unknown", ignoreCase = true)
        ) {
            ""
        } else {
            trimmed
        }
    }

    private fun cleanTitleBase(raw: String): String {
        var s = FILE_EXT_REGEX.replace(raw.replace('_', ' ').trim(), "")
        s = LEADING_TRACK_NUM_REGEX.replace(s, "")
        s = CLEAN_BRACKETS_REGEX.replace(s, "")
        return s.replace(Regex("""\s+"""), " ").trim()
    }

    private fun cleanArtistBase(raw: String): String {
        if (raw.isBlank()) return ""
        return CLEAN_BRACKETS_REGEX.replace(raw.replace('_', ' '), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun primaryArtistOnly(artist: String): String {
        if (artist.isBlank()) return ""
        return MULTI_ARTIST_SPLIT_REGEX.split(artist).firstOrNull()?.trim().orEmpty()
    }

    private fun splitArtistTitle(text: String): Pair<String, String>? {
        val separators = listOf(" — ", " – ", " - ")
        for (sep in separators) {
            val idx = text.indexOf(sep)
            if (idx > 0 && idx + sep.length < text.length) {
                val left = text.substring(0, idx).trim()
                val right = text.substring(idx + sep.length).trim()
                if (left.isNotEmpty() && right.isNotEmpty()) {
                    return left to right
                }
            }
        }
        return null
    }

    private fun encode(v: String): String = URLEncoder.encode(v, "UTF-8")

    private fun cacheFileFor(context: Context, track: LocalTrack): File {
        val dir = File(context.filesDir, "lyrics").apply { mkdirs() }
        val key = "${track.id}_${track.contentUri.toString().hashCode()}_v2"
        return File(dir, "$key.json")
    }

    private fun saveToCache(file: File, lyrics: TrackLyrics) {
        try {
            val obj = JSONObject()
            obj.put("source", lyrics.source)
            obj.put("plain", lyrics.plain ?: "")
            val arr = JSONArray()
            lyrics.synced.forEach { line ->
                val item = JSONObject()
                item.put("t", line.timeMs)
                item.put("e", line.endMs)
                item.put("s", line.text)
                if (line.words.isNotEmpty()) {
                    val wArr = JSONArray()
                    line.words.forEach { w ->
                        val wObj = JSONObject()
                        wObj.put("s", w.startMs)
                        wObj.put("e", w.endMs)
                        wObj.put("t", w.text)
                        wArr.put(wObj)
                    }
                    item.put("w", wArr)
                }
                arr.put(item)
            }
            obj.put("synced", arr)
            file.writeText(obj.toString())
        } catch (_: Exception) {
        }
    }

    private fun loadFromCache(file: File): TrackLyrics? {
        return try {
            val obj = JSONObject(file.readText())
            val source = obj.optString("source", "Кэш")
            val plain = obj.optString("plain", "").takeIf { it.isNotBlank() }
            val arr = obj.optJSONArray("synced") ?: JSONArray()
            val synced = buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val t = item.optLong("t", -1L)
                    val end = item.optLong("e", 0L)
                    val s = item.optString("s", "")
                    if (t >= 0L && s.isNotBlank()) {
                        val wArr = item.optJSONArray("w")
                        val words = if (wArr != null) {
                            buildList {
                                for (j in 0 until wArr.length()) {
                                    val wObj = wArr.optJSONObject(j) ?: continue
                                    val ws = wObj.optLong("s", t)
                                    val we = wObj.optLong("e", ws)
                                    val wt = wObj.optString("t", "")
                                    if (wt.isNotEmpty()) add(LyricWord(ws, we, wt))
                                }
                            }
                        } else emptyList()
                        add(LyricLine(timeMs = t, text = s, endMs = end, words = words))
                    }
                }
            }
            if (synced.isEmpty() && plain.isNullOrBlank()) null
            else TrackLyrics(synced = synced, plain = plain, source = source)
        } catch (_: Exception) {
            null
        }
    }
}
