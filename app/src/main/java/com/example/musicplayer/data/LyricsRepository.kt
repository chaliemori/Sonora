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
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class TrackLyrics(
    val synced: List<LyricLine>,
    val plain: String?,
    val source: String
) {
    val isSynced: Boolean get() = synced.isNotEmpty()
    val isEmpty: Boolean get() = synced.isEmpty() && plain.isNullOrBlank()
}

data class LyricsLookupResult(
    val lyrics: TrackLyrics?,
    val hadNetworkError: Boolean = false,
    val checkedProviders: List<String> = emptyList()
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

object LyricsRepository {

    private val LRC_TAG_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val INLINE_WORD_TAG_REGEX = Regex("""<\d{1,3}:\d{2}(?:[.:]\d{1,3})?>""")
    private val LRC_OFFSET_REGEX = Regex("""\[offset:\s*([+-]?\d+)\s*]""", RegexOption.IGNORE_CASE)
    private val LEADING_TRACK_NUM_REGEX = Regex("""^\d{1,3}\s*[.\-–_]\s+""")
    private val CLEAN_BRACKETS_REGEX = Regex(
        """\s*[\[(](?:feat\.?|ft\.?|official.*|audio|video|lyrics|visualizer|hd|hq|flac|mp3|dsd|explicit|clean|remaster(?:ed)?(?:\s*\d{4})?|prod\.?.*?)[\])]""",
        RegexOption.IGNORE_CASE
    )
    private val ALL_BRACKETS_REGEX = Regex("""\s*[\[(][^\])]*[\])]""")
    private val FEAT_SPLIT_REGEX = Regex("""\s+(?:feat\.?|ft\.?|with|x)\s+.*$""", RegexOption.IGNORE_CASE)
    private val MULTI_ARTIST_SPLIT_REGEX = Regex("""\s*(?:,|;|&|/|\bfeat\.?\b|\bft\.?\b)\s*""", RegexOption.IGNORE_CASE)
    private val KUGOU_CREDIT_LINE_REGEX = Regex("""^(?:作曲|作词|编曲|制作人|混音|母带|监制|出品|吉他|贝斯|鼓|和声)\s*[:：]""")

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
                    checkedProviders = listOf(cached.source)
                )
            }
        }

        if (customQuery.isNullOrBlank()) {
            findLocalLrc(context, track)?.let { local ->
                saveToCache(cacheFile, local)
                return@withContext LyricsLookupResult(
                    lyrics = local,
                    hadNetworkError = false,
                    checkedProviders = listOf(local.source)
                )
            }
        }

        val candidates = buildQueryCandidates(track, customQuery)
        if (candidates.isEmpty()) {
            return@withContext LyricsLookupResult(lyrics = null, hadNetworkError = false)
        }

        val outcome = fetchAcrossAllProviders(candidates)
        val winner = outcome.lyrics
        if (winner != null && !winner.isEmpty) {
            saveToCache(cacheFile, winner)
        }
        outcome
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
        val bestSynced = validLyrics
            .filter { it.isSynced }
            .maxByOrNull { lyricsPriorityScore(it) }
        val bestPlain = validLyrics
            .maxByOrNull { lyricsPriorityScore(it) }

        var winner = bestSynced ?: bestPlain

        // If user entered a free-text query (no separate artist/title) and LRCMux wasn't directly callable
        // or returned nothing, try LRCMux once more if LRCLIB/KuGou didn't find synced lyrics.
        val hadNetworkErr = outcomes.all { it.lyrics == null && it.networkError }
        return@coroutineScope LyricsLookupResult(
            lyrics = winner,
            hadNetworkError = hadNetworkErr,
            checkedProviders = outcomes.map { it.providerName }
        )
    }

    private fun lyricsPriorityScore(lyrics: TrackLyrics): Int {
        val syncBonus = if (lyrics.isSynced) {
            if (lyrics.synced.size >= 4) 10_000 else 5_000
        } else {
            0
        }
        val sourceBonus = when {
            lyrics.source.startsWith("LRCMux", ignoreCase = true) -> 300
            lyrics.source.startsWith("LRCLIB", ignoreCase = true) -> 200
            lyrics.source.startsWith("KuGou", ignoreCase = true) -> 100
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
        val relaxedTitle = FEAT_SPLIT_REGEX.replace(ALL_BRACKETS_REGEX.replace(cleanTitle, ""), "").trim()
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

    // ==================== 1. LRCMux (Deezer + KuGou + YouTube Music) ====================

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
                    if (startMs >= 0L) {
                        synced.add(LyricLine(timeMs = startMs, text = text))
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

        // Exact /get without brittle album filter first
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

        // Fallback to /api/search
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
                        // Avoid unrelated 10-minute mashups if duration is known
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

    // ==================== 3. KuGou Direct ====================

    private fun fetchFromKuGou(primary: QueryCandidate, relaxed: QueryCandidate?): ProviderOutcome {
        var anyNetErr = false

        fun searchCandidate(cand: QueryCandidate): TrackLyrics? {
            if (cand.combined.isBlank()) return null

            // Step 1: Search songs to obtain audio hash
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

            // Step 2: Query krcs.kugou.com/search for lyric id + accesskey
            val candidatesToTry = mutableListOf<Pair<String, String>>() // (id, accesskey)

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

            // Fallback Step 2b: direct keyword search on krcs.kugou.com
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

            // Step 3: Download LRC (Base64 encoded UTF-8)
            for ((id, accesskey) in candidatesToTry) {
                val dlUrl = "https://lyrics.kugou.com/download?ver=1&client=pc&id=${encode(id)}&accesskey=${encode(accesskey)}&fmt=lrc&charset=utf8"
                when (val dlRes = httpGet(dlUrl)) {
                    is HttpResponseResult.Ok -> {
                        try {
                            val b64 = JSONObject(dlRes.body).optString("content", "")
                            if (b64.isNotBlank()) {
                                val rawLrc = String(Base64.decode(b64, Base64.DEFAULT), Charsets.UTF_8)
                                val parsed = parseLrcText(rawLrc, filterKuGouCredits = true)
                                if (parsed.isNotEmpty()) {
                                    return TrackLyrics(
                                        synced = parsed,
                                        plain = parsed.joinToString("\n") { it.text },
                                        source = "KuGou"
                                    )
                                } else {
                                    val plainFallback = rawLrc.lines()
                                        .map { LRC_TAG_REGEX.replace(it, "").trim() }
                                        .filter { it.isNotBlank() && !it.startsWith("[") }
                                        .joinToString("\n")
                                    if (plainFallback.isNotBlank()) {
                                        return TrackLyrics(synced = emptyList(), plain = plainFallback, source = "KuGou")
                                    }
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                    is HttpResponseResult.NetworkFailure -> anyNetErr = true
                    is HttpResponseResult.HttpError -> Unit
                }
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

    // ==================== LRC Parsing & Helpers ====================

    fun parseLrcText(raw: String, filterKuGouCredits: Boolean = false): List<LyricLine> {
        val offsetMs = LRC_OFFSET_REGEX.find(raw)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        val lines = mutableListOf<LyricLine>()

        raw.lineSequence().forEach { line ->
            val matches = LRC_TAG_REGEX.findAll(line).toList()
            if (matches.isEmpty()) return@forEach
            val cleaned = INLINE_WORD_TAG_REGEX.replace(LRC_TAG_REGEX.replace(line, ""), "").trim()
            if (cleaned.isEmpty()) return@forEach

            for (m in matches) {
                val min = m.groupValues[1].toLongOrNull() ?: continue
                val sec = m.groupValues[2].toLongOrNull() ?: continue
                val fracStr = m.groupValues[3]
                val msPart = when (fracStr.length) {
                    0 -> 0L
                    1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                    2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                    else -> fracStr.take(3).toLongOrNull() ?: 0L
                }
                val timeMs = (min * 60_000L + sec * 1_000L + msPart - offsetMs).coerceAtLeast(0L)
                lines.add(LyricLine(timeMs = timeMs, text = cleaned))
            }
        }

        val sorted = lines.sortedBy { it.timeMs }
        if (filterKuGouCredits && sorted.size > 4) {
            val withoutCredits = sorted.filterNot { KUGOU_CREDIT_LINE_REGEX.containsMatchIn(it.text) }
            if (withoutCredits.isNotEmpty()) return withoutCredits
        }
        return sorted
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
        var s = raw.replace('_', ' ').trim()
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
        val key = "${track.id}_${track.contentUri.toString().hashCode()}"
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
                item.put("s", line.text)
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
                    val s = item.optString("s", "")
                    if (t >= 0L && s.isNotBlank()) {
                        add(LyricLine(t, s))
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
