package com.example.musicplayer.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.Locale
import kotlin.math.abs

data class ScanReport(
    val tracks: List<LocalTrack>,
    val scannedFilesCount: Int,
    val statusText: String
)

private data class AudioSpecs(
    val bitrateKbps: Int,
    val sampleRateHz: Int
)

class LocalMusicScanner(private val context: Context) {

    private val supportedExtensions = setOf("mp3", "flac", "m4a", "aac", "ogg", "opus", "wav", "alac")

    fun clearCoverCache() {
        try {
            val coverDir = File(context.cacheDir, "covers")
            if (coverDir.exists()) {
                coverDir.listFiles()?.forEach { it.delete() }
            }
        } catch (_: Exception) {
        }
    }

    suspend fun scanTracks(
        folderUris: List<Uri>,
        onProgress: (Int, String) -> Unit
    ): ScanReport = withContext(Dispatchers.IO) {
        val collected = mutableListOf<LocalTrack>()
        var checkedCount = 0

        if (folderUris.isNotEmpty()) {
            for (treeUri in folderUris) {
                val treeDocId = try {
                    DocumentsContract.getTreeDocumentId(treeUri)
                } catch (_: Exception) {
                    continue
                }
                val rootFolderName = treeDocId.substringAfterLast('/')
                    .substringAfterLast(':')
                    .ifBlank { "Музыка" }

                scanDocumentTree(
                    treeUri = treeUri,
                    parentDocId = treeDocId,
                    currentFolderName = rootFolderName,
                    outTracks = collected,
                    onFileChecked = {
                        checkedCount++
                        if (checkedCount % 25 == 0) {
                            onProgress(checkedCount, "Проверено $checkedCount файлов...")
                        }
                    }
                )
            }
        } else {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Audio.Media.IS_MUSIC
            )

            context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    checkedCount++
                    if (checkedCount % 40 == 0) {
                        onProgress(checkedCount, "Проверено $checkedCount файлов...")
                    }

                    val id = cursor.getLong(idCol)
                    val duration = cursor.getLong(durationCol)
                    if (duration < 3_000L) continue

                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val sizeBytes = cursor.getLong(sizeCol)
                    val displayName = cursor.getString(nameCol).orEmpty()
                    val mime = cursor.getString(mimeCol).orEmpty()
                    val format = resolveFormatLabel(displayName, mime)
                    val specs = resolveAudioSpecs(
                        uri = contentUri,
                        knownSizeBytes = sizeBytes,
                        durationMs = duration,
                        formatLabel = format
                    )

                    val rawTitle = cursor.getString(titleCol).orEmpty().trim()
                    val cleanTitle = if (rawTitle.isBlank() || rawTitle.equals("<unknown>", true)) {
                        displayName.substringBeforeLast('.').ifBlank { "Без названия" }
                    } else {
                        rawTitle
                    }

                    val rawArtist = cursor.getString(artistCol).orEmpty().trim()
                    val cleanArtist = if (rawArtist.isBlank() || rawArtist.equals("<unknown>", true)) {
                        "Неизвестный исполнитель"
                    } else {
                        rawArtist
                    }

                    val rawAlbum = cursor.getString(albumCol).orEmpty().trim()
                    val folder = cursor.getString(bucketCol).orEmpty().ifBlank { "Музыка" }

                    collected.add(
                        LocalTrack(
                            id = id,
                            title = cleanTitle,
                            artist = cleanArtist,
                            album = rawAlbum,
                            durationMs = duration,
                            contentUri = contentUri,
                            artworkUri = contentUri,
                            folderName = folder,
                            formatLabel = format,
                            bitrateKbps = specs.bitrateKbps,
                            sampleRateHz = specs.sampleRateHz
                        )
                    )
                }
            }
        }

        val uniqueTracks = collected.distinctBy { it.contentUri.toString() }
        val status = if (uniqueTracks.isEmpty()) {
            "Треки не найдены (проверено $checkedCount)"
        } else {
            "Готово: ${uniqueTracks.size} треков"
        }
        onProgress(checkedCount, status)
        ScanReport(
            tracks = uniqueTracks,
            scannedFilesCount = checkedCount,
            statusText = status
        )
    }

    private fun scanDocumentTree(
        treeUri: Uri,
        parentDocId: String,
        currentFolderName: String,
        outTracks: MutableList<LocalTrack>,
        onFileChecked: () -> Unit
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )

        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idCol) ?: continue
                    val name = cursor.getString(nameCol).orEmpty()
                    val mime = cursor.getString(mimeCol).orEmpty()
                    val sizeBytes = cursor.getLong(sizeCol)

                    if (name.startsWith(".")) continue

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        scanDocumentTree(
                            treeUri = treeUri,
                            parentDocId = docId,
                            currentFolderName = name.ifBlank { currentFolderName },
                            outTracks = outTracks,
                            onFileChecked = onFileChecked
                        )
                    } else {
                        onFileChecked()
                        val ext = name.substringAfterLast('.', "").lowercase(Locale.getDefault())
                        if (mime.startsWith("audio/") || ext in supportedExtensions) {
                            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            val track = extractTrackFromDocument(
                                docUri = docUri,
                                displayName = name,
                                mimeType = mime,
                                sizeBytes = sizeBytes,
                                folderName = currentFolderName
                            )
                            if (track != null) {
                                outTracks.add(track)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun extractTrackFromDocument(
        docUri: Uri,
        displayName: String,
        mimeType: String,
        sizeBytes: Long,
        folderName: String
    ): LocalTrack? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, docUri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            if (durationMs < 3_000L) return null

            val format = resolveFormatLabel(displayName, mimeType)
            val specs = resolveAudioSpecs(
                uri = docUri,
                knownSizeBytes = sizeBytes,
                durationMs = durationMs,
                formatLabel = format
            )

            val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()
            val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()
            val metaAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim()

            val title = if (!metaTitle.isNullOrBlank()) {
                metaTitle
            } else {
                displayName.substringBeforeLast('.').ifBlank { "Без названия" }
            }
            val artist = if (!metaArtist.isNullOrBlank()) metaArtist else "Неизвестный исполнитель"
            val album = metaAlbum.orEmpty()

            val stableId = docUri.toString().hashCode().toLong() and 0x7FFFFFFFL

            LocalTrack(
                id = stableId,
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                contentUri = docUri,
                artworkUri = docUri,
                folderName = folderName,
                formatLabel = format,
                bitrateKbps = specs.bitrateKbps,
                sampleRateHz = specs.sampleRateHz
            )
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun resolveAudioSpecs(
        uri: Uri,
        knownSizeBytes: Long,
        durationMs: Long,
        formatLabel: String
    ): AudioSpecs {
        // 1. Прямой бинарный разбор заголовка файла (FLAC STREAMINFO / WAV fmt / MP3 frame)
        val headerSpecs = parseBinaryHeaderSpecs(uri, durationMs, knownSizeBytes)
        if (headerSpecs != null && headerSpecs.bitrateKbps > 0 && headerSpecs.sampleRateHz > 0) {
            return headerSpecs
        }

        var foundSampleRate = headerSpecs?.sampleRateHz ?: 0
        var foundBitrate = headerSpecs?.bitrateKbps ?: 0

        // 2. MediaExtractor из системного демультиплексора
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    if (foundSampleRate == 0 && trackFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        val sr = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        if (sr in 8000..384000) foundSampleRate = sr
                    }
                    if (foundBitrate == 0 && trackFormat.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        val br = trackFormat.getInteger(MediaFormat.KEY_BIT_RATE) / 1000
                        if (br in 32..6000) foundBitrate = br
                    }
                    break
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                extractor.release()
            } catch (_: Exception) {
            }
        }

        // 3. Физический размер аудиоданных за вычетом ID3/картинок
        if (foundBitrate == 0) {
            val physicalBytes = if (knownSizeBytes > 0L) {
                knownSizeBytes
            } else {
                try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        pfd.statSize.coerceAtLeast(0L)
                    } ?: 0L
                } catch (_: Exception) {
                    0L
                }
            }
            val headerOffset = estimateMetadataPrefixBytes(uri)
            val pureBytes = (physicalBytes - headerOffset).coerceAtLeast(0L)
            if (pureBytes > 0L && durationMs > 0L) {
                val rawKbps = ((pureBytes * 8L) / durationMs).toInt()
                if (rawKbps in 32..6000) {
                    foundBitrate = if (formatLabel == "MP3" || formatLabel == "M4A" || formatLabel == "AAC") {
                        val standardRates = listOf(64, 96, 128, 160, 192, 224, 256, 320)
                        val nearest = standardRates.minByOrNull { abs(it - rawKbps) }
                        if (nearest != null && abs(nearest - rawKbps) <= 18) nearest else rawKbps
                    } else {
                        rawKbps
                    }
                }
            }
        }

        return AudioSpecs(
            bitrateKbps = if (foundBitrate > 0) foundBitrate else 320,
            sampleRateHz = if (foundSampleRate > 0) foundSampleRate else 44100
        )
    }

    private fun parseBinaryHeaderSpecs(
        uri: Uri,
        durationMs: Long,
        knownSizeBytes: Long
    ): AudioSpecs? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val head = ByteArray(16)
                if (readFully(stream, head, 0, 12) < 12) return null

                // A. FLAC: начинается с "fLaC"
                if (head[0] == 'f'.code.toByte() &&
                    head[1] == 'L'.code.toByte() &&
                    head[2] == 'a'.code.toByte() &&
                    head[3] == 'C'.code.toByte()
                ) {
                    // head[0..3] = "fLaC", head[4..7] = заголовок блока STREAMINFO, head[8..11] = первые 4 байта STREAMINFO
                    val blockHeader = head.copyOfRange(4, 8)
                    val blockType = blockHeader[0].toInt() and 0x7F
                    if (blockType == 0) {
                        val info = ByteArray(34)
                        System.arraycopy(head, 8, info, 0, 4)
                        if (readFully(stream, info, 4, 30) < 30) return null
                        // Байты 10..13 содержат: sampleRate (20 бит), channels-1 (3 бита), bps-1 (5 бит), totalSamples (36 бит)
                        val b10 = info[10].toInt() and 0xFF
                        val b11 = info[11].toInt() and 0xFF
                        val b12 = info[12].toInt() and 0xFF
                        val sampleRate = (b10 shl 12) or (b11 shl 4) or (b12 ushr 4)

                        // Пропускаем оставшиеся метаданные FLAC (включая PICTURE), чтобы вычислить чистый битрейт аудиопотока
                        var metadataTotalBytes = 4L + 4L + 34L
                        var isLast = (blockHeader[0].toInt() and 0x80) != 0
                        while (!isLast) {
                            val bh = ByteArray(4)
                            if (readFully(stream, bh, 0, 4) < 4) break
                            isLast = (bh[0].toInt() and 0x80) != 0
                            val len = ((bh[1].toInt() and 0xFF) shl 16) or
                                ((bh[2].toInt() and 0xFF) shl 8) or
                                (bh[3].toInt() and 0xFF)
                            metadataTotalBytes += 4L + len.toLong()
                            skipFully(stream, len.toLong())
                        }

                        val totalSize = if (knownSizeBytes > 0L) knownSizeBytes else queryStatSize(uri)
                        val audioStreamBytes = (totalSize - metadataTotalBytes).coerceAtLeast(0L)
                        val kbps = if (audioStreamBytes > 0L && durationMs > 0L) {
                            ((audioStreamBytes * 8L) / durationMs).toInt().coerceIn(64, 6000)
                        } else {
                            0
                        }
                        if (sampleRate in 8000..384000) {
                            return AudioSpecs(bitrateKbps = kbps, sampleRateHz = sampleRate)
                        }
                    }
                }

                // B. WAV: "RIFF" ... "WAVE"
                if (head[0] == 'R'.code.toByte() &&
                    head[1] == 'I'.code.toByte() &&
                    head[2] == 'F'.code.toByte() &&
                    head[3] == 'F'.code.toByte() &&
                    head[8] == 'W'.code.toByte() &&
                    head[9] == 'A'.code.toByte() &&
                    head[10] == 'V'.code.toByte() &&
                    head[11] == 'E'.code.toByte()
                ) {
                    val fmtBuf = ByteArray(32)
                    if (readFully(stream, fmtBuf, 0, 24) >= 24) {
                        val sampleRate = (fmtBuf[12].toInt() and 0xFF) or
                            ((fmtBuf[13].toInt() and 0xFF) shl 8) or
                            ((fmtBuf[14].toInt() and 0xFF) shl 16) or
                            ((fmtBuf[15].toInt() and 0xFF) shl 24)
                        val byteRate = (fmtBuf[16].toInt() and 0xFF) or
                            ((fmtBuf[17].toInt() and 0xFF) shl 8) or
                            ((fmtBuf[18].toInt() and 0xFF) shl 16) or
                            ((fmtBuf[19].toInt() and 0xFF) shl 24)
                        val kbps = (byteRate * 8) / 1000
                        if (sampleRate in 8000..384000 && kbps in 32..10000) {
                            return AudioSpecs(bitrateKbps = kbps, sampleRateHz = sampleRate)
                        }
                    }
                }

                // C. MP3: пропускаем ID3v2 если есть, ищем sync word 0xFFE0
                var id3Size = 0L
                if (head[0] == 'I'.code.toByte() &&
                    head[1] == 'D'.code.toByte() &&
                    head[2] == '3'.code.toByte()
                ) {
                    id3Size = (((head[6].toInt() and 0x7F) shl 21) or
                        ((head[7].toInt() and 0x7F) shl 14) or
                        ((head[8].toInt() and 0x7F) shl 7) or
                        (head[9].toInt() and 0x7F)).toLong() + 10L
                    val alreadyRead = 12L
                    if (id3Size > alreadyRead) {
                        skipFully(stream, id3Size - alreadyRead)
                    }
                }

                val scanBuf = ByteArray(4096)
                val n = if (id3Size == 0L) {
                    System.arraycopy(head, 0, scanBuf, 0, 12)
                    12 + readFully(stream, scanBuf, 12, scanBuf.size - 12)
                } else {
                    readFully(stream, scanBuf, 0, scanBuf.size)
                }

                for (i in 0 until n - 4) {
                    val b1 = scanBuf[i].toInt() and 0xFF
                    val b2 = scanBuf[i + 1].toInt() and 0xFF
                    if (b1 == 0xFF && (b2 and 0xE0) == 0xE0) {
                        val versionBits = (b2 ushr 3) and 0x03
                        val layerBits = (b2 ushr 1) and 0x03
                        if (versionBits == 1 || layerBits == 0) continue

                        val b3 = scanBuf[i + 2].toInt() and 0xFF
                        val bitrateIdx = (b3 ushr 4) and 0x0F
                        val sampleIdx = (b3 ushr 2) and 0x03
                        if (bitrateIdx == 0 || bitrateIdx == 15 || sampleIdx == 3) continue

                        val sampleRate = when (versionBits) {
                            3 -> listOf(44100, 48000, 32000)[sampleIdx] // MPEG 1
                            2 -> listOf(22050, 24000, 16000)[sampleIdx] // MPEG 2
                            0 -> listOf(11025, 12000, 8000)[sampleIdx]  // MPEG 2.5
                            else -> 44100
                        }

                        // Считаем реальный средний битрейт по весу MP3-фреймов без ID3v2
                        val totalSize = if (knownSizeBytes > 0L) knownSizeBytes else queryStatSize(uri)
                        val mp3DataBytes = (totalSize - id3Size).coerceAtLeast(0L)
                        val avgKbps = if (mp3DataBytes > 0L && durationMs > 0L) {
                            val raw = ((mp3DataBytes * 8L) / durationMs).toInt()
                            val std = listOf(64, 96, 128, 160, 192, 224, 256, 320)
                            val near = std.minByOrNull { abs(it - raw) }
                            if (near != null && abs(near - raw) <= 16) near else raw
                        } else {
                            0
                        }

                        val frameKbps = if (versionBits == 3 && layerBits == 1) {
                            listOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320)[bitrateIdx]
                        } else {
                            128
                        }

                        val finalKbps = if (avgKbps in 32..320) avgKbps else frameKbps
                        return AudioSpecs(bitrateKbps = finalKbps, sampleRateHz = sampleRate)
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun estimateMetadataPrefixBytes(uri: Uri): Long {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val head = ByteArray(10)
                if (readFully(stream, head, 0, 10) < 10) return 0L
                if (head[0] == 'I'.code.toByte() && head[1] == 'D'.code.toByte() && head[2] == '3'.code.toByte()) {
                    return (((head[6].toInt() and 0x7F) shl 21) or
                        ((head[7].toInt() and 0x7F) shl 14) or
                        ((head[8].toInt() and 0x7F) shl 7) or
                        (head[9].toInt() and 0x7F)).toLong() + 10L
                }
                0L
            } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun queryStatSize(uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize.coerceAtLeast(0L)
            } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun readFully(stream: InputStream, buffer: ByteArray, offset: Int, length: Int): Int {
        var total = 0
        while (total < length) {
            val read = stream.read(buffer, offset + total, length - total)
            if (read <= 0) break
            total += read
        }
        return total
    }

    private fun skipFully(stream: InputStream, bytesToSkip: Long) {
        var remaining = bytesToSkip
        while (remaining > 0L) {
            val skipped = stream.skip(remaining)
            if (skipped <= 0L) {
                if (stream.read() == -1) break
                remaining--
            } else {
                remaining -= skipped
            }
        }
    }

    private fun resolveFormatLabel(fileName: String, mimeType: String): String {
        val ext = fileName.substringAfterLast('.', "").uppercase(Locale.getDefault())
        if (ext in setOf("FLAC", "WAV", "MP3", "M4A", "AAC", "OGG", "OPUS", "ALAC")) {
            return ext
        }
        val m = mimeType.lowercase(Locale.getDefault())
        return when {
            "flac" in m -> "FLAC"
            "wav" in m || "x-wav" in m -> "WAV"
            "opus" in m -> "OPUS"
            "ogg" in m -> "OGG"
            "mp4" in m || "m4a" in m || "aac" in m -> "M4A"
            "mpeg" in m || "mp3" in m -> "MP3"
            else -> "AUDIO"
        }
    }
}
