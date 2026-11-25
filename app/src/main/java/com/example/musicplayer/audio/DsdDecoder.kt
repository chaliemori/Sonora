package com.example.musicplayer.audio

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

data class DsdHeaderInfo(
    val isDsf: Boolean,
    val sampleRateHz: Int,
    val channels: Int,
    val bitsPerSample: Int,
    val sampleCountPerChannel: Long,
    val blockSizePerChannel: Int,
    val dataOffset: Long,
    val dataSizeBytes: Long,
    val metadataOffset: Long,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null
) {
    val durationMs: Long
        get() = if (sampleRateHz > 0) (sampleCountPerChannel * 1000L) / sampleRateHz else 0L

    val bitrateKbps: Int
        get() = ((sampleRateHz.toLong() * channels.toLong()) / 1000L).toInt().coerceAtLeast(64)
}

object DsdParser {

    fun isDsdFileName(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext == "dsf" || ext == "dff" || ext == "dsdiff"
    }

    fun isDsdUri(context: Context, uri: Uri): Boolean {
        val path = uri.toString().lowercase()
        if (path.endsWith(".dsf") || path.endsWith(".dff") || path.endsWith(".dsdiff")) {
            return true
        }
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val magic = ByteArray(16)
                val read = readAtMost(stream, magic, 16)
                isDsfMagic(magic, read) || isDffMagic(magic, read)
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun parseHeader(context: Context, uri: Uri): DsdHeaderInfo? {
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                FileInputStream(afd.fileDescriptor).use { fis ->
                    val ch = fis.channel
                    parseHeaderFromChannel(ch)
                }
            } ?: context.contentResolver.openInputStream(uri)?.use { stream ->
                val buf = ByteArray(4096)
                val read = readAtMost(stream, buf, buf.size)
                parseHeaderFromBuffer(buf, read)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseHeaderFromChannel(ch: FileChannel): DsdHeaderInfo? {
        val head = ByteBuffer.allocate(96).order(ByteOrder.LITTLE_ENDIAN)
        ch.position(0L)
        val n = ch.read(head)
        if (n < 16) return null
        val arr = head.array()

        if (isDsfMagic(arr, n)) {
            if (n < 80) return null
            val totalFileSize = getLongLE(arr, 12)
            val metadataOffset = getLongLE(arr, 20)
            // fmt chunk starts at 28
            if (arr[28] != 'f'.code.toByte() || arr[29] != 'm'.code.toByte() ||
                arr[30] != 't'.code.toByte() || arr[31] != ' '.code.toByte()
            ) {
                return null
            }
            val fmtSize = getLongLE(arr, 32).coerceAtLeast(52L)
            val channels = getIntLE(arr, 52).coerceIn(1, 8)
            val sampleRate = getIntLE(arr, 56)
            val bitsPerSample = getIntLE(arr, 60)
            val sampleCount = getLongLE(arr, 64)
            val blockSize = getIntLE(arr, 72).coerceAtLeast(1)

            val dataChunkPos = 28L + fmtSize
            val dataHead = ByteBuffer.allocate(12)
            ch.position(dataChunkPos)
            if (ch.read(dataHead) < 12) return null
            val dh = dataHead.array()
            if (dh[0] != 'd'.code.toByte() || dh[1] != 'a'.code.toByte() ||
                dh[2] != 't'.code.toByte() || dh[3] != 'a'.code.toByte()
            ) {
                return null
            }
            val dataChunkSize = getLongLE(dh, 4)
            val dataOffset = dataChunkPos + 12L
            val dataSize = (dataChunkSize - 12L).coerceAtLeast(0L)
                .let { if (it == 0L && totalFileSize > dataOffset) totalFileSize - dataOffset else it }

            var title: String? = null
            var artist: String? = null
            var album: String? = null
            if (metadataOffset > dataOffset && metadataOffset + 10L < ch.size()) {
                val tags = parseId3v2FromChannel(ch, metadataOffset)
                title = tags.first
                artist = tags.second
                album = tags.third
            }

            if (sampleRate < 44100 || sampleCount <= 0L) return null
            return DsdHeaderInfo(
                isDsf = true,
                sampleRateHz = sampleRate,
                channels = channels,
                bitsPerSample = bitsPerSample,
                sampleCountPerChannel = sampleCount,
                blockSizePerChannel = blockSize,
                dataOffset = dataOffset,
                dataSizeBytes = dataSize,
                metadataOffset = metadataOffset,
                title = title,
                artist = artist,
                album = album
            )
        }

        if (isDffMagic(arr, n)) {
            return parseDffFromChannel(ch)
        }
        return null
    }

    private fun parseDffFromChannel(ch: FileChannel): DsdHeaderInfo? {
        val fileSize = ch.size()
        var pos = 16L
        var sampleRate = 0
        var channels = 2
        var dataOffset = 0L
        var dataSize = 0L

        val hdr = ByteBuffer.allocate(12)
        while (pos + 12L <= fileSize) {
            hdr.clear()
            ch.position(pos)
            if (ch.read(hdr) < 12) break
            val b = hdr.array()
            val id = String(b, 0, 4, Charsets.US_ASCII)
            val chunkSize = getLongBE(b, 4)
            val payloadPos = pos + 12L

            when (id) {
                "PROP" -> {
                    val propEnd = (payloadPos + chunkSize).coerceAtMost(fileSize)
                    var subPos = payloadPos + 4L // skip "SND "
                    while (subPos + 12L <= propEnd) {
                        hdr.clear()
                        ch.position(subPos)
                        if (ch.read(hdr) < 12) break
                        val sb = hdr.array()
                        val subId = String(sb, 0, 4, Charsets.US_ASCII)
                        val subSize = getLongBE(sb, 4)
                        val subPayload = subPos + 12L
                        if (subId == "FS  " && subSize >= 4L) {
                            val fsBuf = ByteBuffer.allocate(4)
                            ch.position(subPayload)
                            if (ch.read(fsBuf) == 4) {
                                sampleRate = getIntBE(fsBuf.array(), 0)
                            }
                        } else if (subId == "CHNL" && subSize >= 2L) {
                            val chBuf = ByteBuffer.allocate(2)
                            ch.position(subPayload)
                            if (ch.read(chBuf) == 2) {
                                val c = ((chBuf.array()[0].toInt() and 0xFF) shl 8) or
                                    (chBuf.array()[1].toInt() and 0xFF)
                                if (c in 1..8) channels = c
                            }
                        }
                        subPos = subPayload + subSize + (subSize and 1L)
                    }
                }
                "DSD " -> {
                    dataOffset = payloadPos
                    dataSize = chunkSize.coerceAtMost((fileSize - payloadPos).coerceAtLeast(0L))
                    break
                }
            }
            pos = payloadPos + chunkSize + (chunkSize and 1L)
        }

        if (sampleRate < 44100 || dataOffset <= 0L || dataSize <= 0L) return null
        val sampleCount = (dataSize * 8L) / channels
        return DsdHeaderInfo(
            isDsf = false,
            sampleRateHz = sampleRate,
            channels = channels,
            bitsPerSample = 8,
            sampleCountPerChannel = sampleCount,
            blockSizePerChannel = 1,
            dataOffset = dataOffset,
            dataSizeBytes = dataSize,
            metadataOffset = 0L
        )
    }

    private fun parseHeaderFromBuffer(arr: ByteArray, n: Int): DsdHeaderInfo? {
        if (!isDsfMagic(arr, n) || n < 92) return null
        val metadataOffset = getLongLE(arr, 20)
        val fmtSize = getLongLE(arr, 32).coerceAtLeast(52L)
        val channels = getIntLE(arr, 52).coerceIn(1, 8)
        val sampleRate = getIntLE(arr, 56)
        val bitsPerSample = getIntLE(arr, 60)
        val sampleCount = getLongLE(arr, 64)
        val blockSize = getIntLE(arr, 72).coerceAtLeast(1)
        val dataChunkPos = (28L + fmtSize).toInt()
        if (dataChunkPos + 12 > n) return null
        val dataChunkSize = getLongLE(arr, dataChunkPos + 4)
        val dataOffset = dataChunkPos + 12L
        if (sampleRate < 44100 || sampleCount <= 0L) return null
        return DsdHeaderInfo(
            isDsf = true,
            sampleRateHz = sampleRate,
            channels = channels,
            bitsPerSample = bitsPerSample,
            sampleCountPerChannel = sampleCount,
            blockSizePerChannel = blockSize,
            dataOffset = dataOffset,
            dataSizeBytes = (dataChunkSize - 12L).coerceAtLeast(0L),
            metadataOffset = metadataOffset
        )
    }

    private fun parseId3v2FromChannel(ch: FileChannel, offset: Long): Triple<String?, String?, String?> {
        return try {
            val header = ByteBuffer.allocate(10)
            ch.position(offset)
            if (ch.read(header) < 10) return Triple(null, null, null)
            val hb = header.array()
            if (hb[0] != 'I'.code.toByte() || hb[1] != 'D'.code.toByte() || hb[2] != '3'.code.toByte()) {
                return Triple(null, null, null)
            }
            val version = hb[3].toInt() and 0xFF
            val tagSize = ((hb[6].toInt() and 0x7F) shl 21) or
                ((hb[7].toInt() and 0x7F) shl 14) or
                ((hb[8].toInt() and 0x7F) shl 7) or
                (hb[9].toInt() and 0x7F)
            val safeSize = tagSize.coerceIn(0, 512 * 1024)
            val data = ByteBuffer.allocate(safeSize)
            if (ch.read(data) <= 0) return Triple(null, null, null)
            val arr = data.array()
            var pos = 0
            var title: String? = null
            var artist: String? = null
            var album: String? = null
            while (pos + 10 <= arr.size) {
                val frameId = String(arr, pos, 4, Charsets.US_ASCII)
                if (frameId[0] == '\u0000') break
                val frameSize = if (version >= 4) {
                    ((arr[pos + 4].toInt() and 0x7F) shl 21) or
                        ((arr[pos + 5].toInt() and 0x7F) shl 14) or
                        ((arr[pos + 6].toInt() and 0x7F) shl 7) or
                        (arr[pos + 7].toInt() and 0x7F)
                } else {
                    getIntBE(arr, pos + 4)
                }
                if (frameSize <= 0 || pos + 10 + frameSize > arr.size) break
                if (frameId == "TIT2" || frameId == "TPE1" || frameId == "TALB") {
                    val text = decodeId3Text(arr, pos + 10, frameSize)
                    when (frameId) {
                        "TIT2" -> if (title == null && text.isNotBlank()) title = text
                        "TPE1" -> if (artist == null && text.isNotBlank()) artist = text
                        "TALB" -> if (album == null && text.isNotBlank()) album = text
                    }
                }
                pos += 10 + frameSize
            }
            Triple(title, artist, album)
        } catch (_: Exception) {
            Triple(null, null, null)
        }
    }

    private fun decodeId3Text(arr: ByteArray, start: Int, len: Int): String {
        if (len <= 1) return ""
        val encoding = arr[start].toInt() and 0xFF
        val charset = when (encoding) {
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            3 -> Charsets.UTF_8
            else -> Charsets.ISO_8859_1
        }
        return String(arr, start + 1, len - 1, charset).trimEnd('\u0000').trim()
    }

    private fun isDsfMagic(arr: ByteArray, len: Int): Boolean {
        return len >= 4 &&
            arr[0] == 'D'.code.toByte() && arr[1] == 'S'.code.toByte() &&
            arr[2] == 'D'.code.toByte() && arr[3] == ' '.code.toByte()
    }

    private fun isDffMagic(arr: ByteArray, len: Int): Boolean {
        return len >= 16 &&
            arr[0] == 'F'.code.toByte() && arr[1] == 'R'.code.toByte() &&
            arr[2] == 'M'.code.toByte() && arr[3] == '8'.code.toByte() &&
            arr[12] == 'D'.code.toByte() && arr[13] == 'S'.code.toByte() &&
            arr[14] == 'D'.code.toByte() && arr[15] == ' '.code.toByte()
    }

    private fun readAtMost(stream: java.io.InputStream, buf: ByteArray, count: Int): Int {
        var total = 0
        while (total < count) {
            val r = stream.read(buf, total, count - total)
            if (r <= 0) break
            total += r
        }
        return total
    }

    private fun getIntLE(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xFF) or
            ((b[o + 1].toInt() and 0xFF) shl 8) or
            ((b[o + 2].toInt() and 0xFF) shl 16) or
            ((b[o + 3].toInt() and 0xFF) shl 24)

    private fun getLongLE(b: ByteArray, o: Int): Long =
        (getIntLE(b, o).toLong() and 0xFFFFFFFFL) or
            ((getIntLE(b, o + 4).toLong() and 0xFFFFFFFFL) shl 32)

    private fun getIntBE(b: ByteArray, o: Int): Int =
        ((b[o].toInt() and 0xFF) shl 24) or
            ((b[o + 1].toInt() and 0xFF) shl 16) or
            ((b[o + 2].toInt() and 0xFF) shl 8) or
            (b[o + 3].toInt() and 0xFF)

    private fun getLongBE(b: ByteArray, o: Int): Long =
        ((getIntBE(b, o).toLong() and 0xFFFFFFFFL) shl 32) or
            (getIntBE(b, o + 4).toLong() and 0xFFFFFFFFL)
}

@UnstableApi
class DsdAwareDataSourceFactory(private val context: Context) : DataSource.Factory {
    private val defaultFactory = DefaultDataSource.Factory(context)

    override fun createDataSource(): DataSource {
        return DsdRoutingDataSource(context, defaultFactory.createDataSource())
    }
}

@UnstableApi
private class DsdRoutingDataSource(
    private val context: Context,
    private val fallback: DataSource
) : BaseDataSource(/* isNetwork = */ false) {

    private var activeDsdSource: DsdWavDataSource? = null
    private var usingDsd = false

    override fun open(dataSpec: DataSpec): Long {
        usingDsd = DsdParser.isDsdUri(context, dataSpec.uri)
        return if (usingDsd) {
            val dsd = DsdWavDataSource(context)
            activeDsdSource = dsd
            dsd.open(dataSpec)
        } else {
            fallback.open(dataSpec)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return if (usingDsd) {
            activeDsdSource?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT
        } else {
            fallback.read(buffer, offset, length)
        }
    }

    override fun getUri(): Uri? {
        return if (usingDsd) activeDsdSource?.uri else fallback.uri
    }

    override fun close() {
        if (usingDsd) {
            activeDsdSource?.close()
            activeDsdSource = null
            usingDsd = false
        } else {
            fallback.close()
        }
    }
}

/**
 * Декодирует 1-битный DSD (.dsf / .dff) в 16-битный PCM WAV поток на лету с поддержкой seek.
 */
@UnstableApi
private class DsdWavDataSource(private val context: Context) : BaseDataSource(/* isNetwork = */ false) {

    private var uri: Uri? = null
    private var pfd: ParcelFileDescriptor? = null
    private var fis: FileInputStream? = null
    private var channel: FileChannel? = null
    private var header: DsdHeaderInfo? = null
    private var wavHeader = ByteArray(44)

    private var pcmRate = 88200
    private var decimationRatio = 32
    private var bytesPerPcmSample = 4
    private var totalWavBytes = 44L
    private var currentPos = 0L

    private var chBlockBuf = ByteArray(4096)
    private var dffInterleavedBuf = ByteArray(4096)

    // 2-ступенчатый декодер по архитектуре dsd2pcm (Stage 1: 8:1 в ~352.8 кГц, Stage 2: в 88.2 кГц)
    private val stage1Bytes = 12 // 96-tap FIR
    private var stage1Lut: Array<DoubleArray> = emptyArray()
    private var stage1Fifo: Array<ByteArray> = emptyArray()

    private var stage2Ratio = 4
    private var stage2Taps = DoubleArray(48)
    private var stage2History: Array<DoubleArray> = emptyArray()

    private fun buildStage1Lut(lsbFirst: Boolean): Array<DoubleArray> {
        val totalTaps = stage1Bytes * 8 // 96 taps
        val fc = 0.017 // ~48 kHz при DSD64, глубокое подавление >150 kHz
        val raw = DoubleArray(totalTaps)
        val mid = (totalTaps - 1) / 2.0
        var sum = 0.0
        for (i in 0 until totalTaps) {
            val n = i - mid
            val sinc = if (n == 0.0) 2.0 * fc else kotlin.math.sin(2.0 * Math.PI * fc * n) / (Math.PI * n)
            // 4-term Blackman-Harris окно (-92 dB боковые лепестки)
            val a = 2.0 * Math.PI * i / (totalTaps - 1)
            val w = 0.35875 - 0.48829 * kotlin.math.cos(a) +
                0.14128 * kotlin.math.cos(2.0 * a) - 0.01168 * kotlin.math.cos(3.0 * a)
            val coeff = sinc * w
            raw[i] = coeff
            sum += coeff
        }
        for (i in 0 until totalTaps) raw[i] /= sum

        return Array(stage1Bytes) { byteIdx ->
            DoubleArray(256) { byteVal ->
                var acc = 0.0
                for (bit in 0 until 8) {
                    val bitPos = if (lsbFirst) bit else (7 - bit)
                    val sign = if ((byteVal and (1 shl bitPos)) != 0) 1.0 else -1.0
                    acc += sign * raw[byteIdx * 8 + bit]
                }
                acc
            }
        }
    }

    private fun buildStage2Taps(ratio: Int, intermediateRate: Int): DoubleArray {
        val numTaps = (ratio * 12).coerceIn(32, 96)
        val fc = (20_000.0 / intermediateRate.toDouble()).coerceIn(0.01, 0.45)
        val taps = DoubleArray(numTaps)
        val mid = (numTaps - 1) / 2.0
        var sum = 0.0
        for (i in 0 until numTaps) {
            val n = i - mid
            val sinc = if (n == 0.0) 2.0 * fc else kotlin.math.sin(2.0 * Math.PI * fc * n) / (Math.PI * n)
            val a = 2.0 * Math.PI * i / (numTaps - 1)
            val w = 0.35875 - 0.48829 * kotlin.math.cos(a) +
                0.14128 * kotlin.math.cos(2.0 * a) - 0.01168 * kotlin.math.cos(3.0 * a)
            val coeff = sinc * w
            taps[i] = coeff
            sum += coeff
        }
        // DSD имеет максимальную амплитуду модуляции ~0.5, масштабируем в 16-bit PCM без клиппинга
        val gain = 44000.0 / sum
        for (i in 0 until numTaps) taps[i] *= gain
        return taps
    }

    private fun processDsdBytesToPcm(channelIdx: Int, src: ByteArray, srcOffset: Int, stride: Int): Short {
        val fifo = stage1Fifo[channelIdx]
        val hist = stage2History[channelIdx]
        val taps = stage2Taps
        val numTaps = taps.size

        for (step in 0 until stage2Ratio) {
            val b = src[srcOffset + step * stride]
            System.arraycopy(fifo, 1, fifo, 0, stage1Bytes - 1)
            fifo[stage1Bytes - 1] = b

            var s1 = 0.0
            for (k in 0 until stage1Bytes) {
                s1 += stage1Lut[k][fifo[k].toInt() and 0xFF]
            }

            System.arraycopy(hist, 1, hist, 0, numTaps - 1)
            hist[numTaps - 1] = s1
        }

        var pcm = 0.0
        for (i in 0 until numTaps) {
            pcm += hist[i] * taps[i]
        }
        return kotlin.math.round(pcm).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            .toShort()
    }

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        transferInitializing(dataSpec)

        val info = DsdParser.parseHeader(context, dataSpec.uri)
            ?: throw IOException("Unsupported or corrupted DSD file")
        header = info

        pcmRate = when {
            info.sampleRateHz % 88200 == 0 -> 88200
            info.sampleRateHz % 96000 == 0 -> 96000
            else -> 44100
        }
        decimationRatio = (info.sampleRateHz / pcmRate).coerceAtLeast(8)
        bytesPerPcmSample = (decimationRatio / 8).coerceAtLeast(1)
        stage2Ratio = bytesPerPcmSample
        val lsbFirst = info.isDsf && info.bitsPerSample == 1
        stage1Lut = buildStage1Lut(lsbFirst)
        stage1Fifo = Array(info.channels) { ByteArray(stage1Bytes) { 0x69.toByte() } }
        stage2Taps = buildStage2Taps(stage2Ratio, info.sampleRateHz / 8)
        stage2History = Array(info.channels) { DoubleArray(stage2Taps.size) }

        val totalPcmFrames = (info.sampleCountPerChannel / decimationRatio).coerceAtLeast(1L)
        val pcmDataBytes = totalPcmFrames * info.channels * 2L
        totalWavBytes = 44L + pcmDataBytes
        wavHeader = buildWavHeader(pcmRate, info.channels, pcmDataBytes)

        val afd = context.contentResolver.openAssetFileDescriptor(dataSpec.uri, "r")
            ?: throw IOException("Cannot open DSD descriptor")
        pfd = afd.parcelFileDescriptor
        val stream = FileInputStream(afd.fileDescriptor)
        fis = stream
        channel = stream.channel

        currentPos = dataSpec.position.coerceAtLeast(0L)
        transferStarted(dataSpec)

        return if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            (totalWavBytes - currentPos).coerceAtLeast(0L)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (currentPos >= totalWavBytes) return C.RESULT_END_OF_INPUT

        val info = header ?: return C.RESULT_END_OF_INPUT
        val ch = channel ?: return C.RESULT_END_OF_INPUT

        var written = 0

        // 1. Заголовок WAV (первые 44 байта)
        if (currentPos < 44L) {
            val hdrRemaining = (44L - currentPos).toInt()
            val toCopy = minOf(hdrRemaining, length)
            System.arraycopy(wavHeader, currentPos.toInt(), buffer, offset, toCopy)
            currentPos += toCopy
            written += toCopy
            bytesTransferred(toCopy)
            if (written == length || currentPos >= totalWavBytes) return written
        }

        // 2. Децимация DSD -> 16-bit PCM (выравнивание по полному PCM фрейму)
        val frameBytes = info.channels * 2
        val maxFrames = minOf(
            (length - written) / frameBytes,
            ((totalWavBytes - currentPos) / frameBytes).toInt()
        )
        if (maxFrames <= 0) return if (written > 0) written else C.RESULT_END_OF_INPUT

        val pcmOffset = currentPos - 44L
        val startFrame = pcmOffset / frameBytes
        val outOffset = offset + written

        val producedBytes = if (info.isDsf) {
            decodeDsfFrames(ch, info, startFrame, maxFrames, buffer, outOffset)
        } else {
            decodeDffFrames(ch, info, startFrame, maxFrames, buffer, outOffset)
        }

        if (producedBytes <= 0) {
            return if (written > 0) written else C.RESULT_END_OF_INPUT
        }

        currentPos += producedBytes
        written += producedBytes
        bytesTransferred(producedBytes)
        return written
    }

    private fun decodeDsfFrames(
        ch: FileChannel,
        info: DsdHeaderInfo,
        startFrame: Long,
        frameCount: Int,
        out: ByteArray,
        outOffset: Int
    ): Int {
        val channels = info.channels
        val blockSize = info.blockSizePerChannel
        val bpp = bytesPerPcmSample

        var framesLeft = frameCount
        var curFrame = startFrame
        var writePos = outOffset

        if (chBlockBuf.size < blockSize * channels) {
            chBlockBuf = ByteArray(blockSize * channels)
        }

        while (framesLeft > 0) {
            val dsdByteIndex = curFrame * bpp
            val blockIndex = dsdByteIndex / blockSize
            val inBlockByte = (dsdByteIndex % blockSize).toInt()
            val blockFilePos = info.dataOffset + blockIndex * blockSize.toLong() * channels.toLong()
            if (blockFilePos >= info.dataOffset + info.dataSizeBytes) break

            val blockBytes = blockSize * channels
            val bb = ByteBuffer.wrap(chBlockBuf, 0, blockBytes)
            ch.position(blockFilePos)
            var totalRead = 0
            while (totalRead < blockBytes) {
                val r = ch.read(bb)
                if (r <= 0) break
                totalRead += r
            }
            if (totalRead < channels * bpp) break

            val framesInBlock = minOf(framesLeft, (blockSize - inBlockByte) / bpp)
            if (framesInBlock <= 0) break

            for (f in 0 until framesInBlock) {
                val sampleByteOffset = inBlockByte + f * bpp
                for (c in 0 until channels) {
                    val pcm = processDsdBytesToPcm(
                        channelIdx = c,
                        src = chBlockBuf,
                        srcOffset = c * blockSize + sampleByteOffset,
                        stride = 1
                    ).toInt()
                    out[writePos++] = (pcm and 0xFF).toByte()
                    out[writePos++] = ((pcm ushr 8) and 0xFF).toByte()
                }
            }

            curFrame += framesInBlock
            framesLeft -= framesInBlock
        }

        return writePos - outOffset
    }

    private fun decodeDffFrames(
        ch: FileChannel,
        info: DsdHeaderInfo,
        startFrame: Long,
        frameCount: Int,
        out: ByteArray,
        outOffset: Int
    ): Int {
        val channels = info.channels
        val bpp = bytesPerPcmSample
        val bytesNeeded = frameCount * bpp * channels
        val filePos = info.dataOffset + startFrame * bpp * channels
        if (filePos >= info.dataOffset + info.dataSizeBytes) return 0

        if (dffInterleavedBuf.size < bytesNeeded) {
            dffInterleavedBuf = ByteArray(bytesNeeded)
        }
        val bb = ByteBuffer.wrap(dffInterleavedBuf, 0, bytesNeeded)
        ch.position(filePos)
        var totalRead = 0
        while (totalRead < bytesNeeded) {
            val r = ch.read(bb)
            if (r <= 0) break
            totalRead += r
        }

        val decodedFrames = totalRead / (bpp * channels)
        if (decodedFrames <= 0) return 0

        var writePos = outOffset
        var srcBase = 0
        for (f in 0 until decodedFrames) {
            for (c in 0 until channels) {
                val pcm = processDsdBytesToPcm(
                    channelIdx = c,
                    src = dffInterleavedBuf,
                    srcOffset = srcBase + c,
                    stride = channels
                ).toInt()
                out[writePos++] = (pcm and 0xFF).toByte()
                out[writePos++] = ((pcm ushr 8) and 0xFF).toByte()
            }
            srcBase += bpp * channels
        }
        return writePos - outOffset
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        header = null
        try {
            channel?.close()
        } catch (_: Exception) {
        }
        try {
            fis?.close()
        } catch (_: Exception) {
        }
        try {
            pfd?.close()
        } catch (_: Exception) {
        }
        channel = null
        fis = null
        pfd = null
        transferEnded()
    }

    private fun buildWavHeader(sampleRate: Int, channels: Int, dataSize: Long): ByteArray {
        val byteRate = sampleRate.toLong() * channels * 2L
        val blockAlign = channels * 2
        val totalSize = (36L + dataSize).coerceAtMost(0xFFFFFFFFL)
        val clampedData = dataSize.coerceAtMost(0xFFFFFFFFL)

        return ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalSize.toInt())
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // PCM fmt chunk size
            putShort(1) // PCM format
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate.toInt())
            putShort(blockAlign.toShort())
            putShort(16) // 16-bit
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(clampedData.toInt())
        }.array()
    }
}
