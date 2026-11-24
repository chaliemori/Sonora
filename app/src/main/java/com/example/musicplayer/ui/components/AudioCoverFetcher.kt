package com.example.musicplayer.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import java.io.File
import java.io.FileOutputStream

class AudioCoverFetcher(
    private val context: Context,
    private val uri: Uri
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val scheme = uri.scheme.orEmpty()
        if (scheme != "content" && scheme != "file") return null

        val coverDir = File(context.cacheDir, "covers").apply { mkdirs() }
        val cacheKey = uri.toString().hashCode().toString()
        val cachedFile = File(coverDir, "art_$cacheKey.jpg")

        if (cachedFile.exists() && cachedFile.length() > 0L) {
            val cachedBmp = BitmapFactory.decodeFile(cachedFile.absolutePath)
            if (cachedBmp != null) {
                return DrawableResult(
                    drawable = BitmapDrawable(context.resources, cachedBmp),
                    isSampled = false,
                    dataSource = DataSource.DISK
                )
            }
        }

        val retriever = MediaMetadataRetriever()
        val bitmap = try {
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture ?: return null
            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

            val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
            val sampleSize = if (maxSide > 600) maxSide / 600 else 1

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize.coerceAtLeast(1)
            }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return null
            try {
                FileOutputStream(cachedFile).use { out ->
                    decoded.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
            } catch (_: Exception) {
            }
            decoded
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        } ?: return null

        return DrawableResult(
            drawable = BitmapDrawable(context.resources, bitmap),
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val s = data.toString()
            val isAudioCandidate = s.contains("/document/") ||
                s.contains("/audio/media/") ||
                s.endsWith(".mp3", true) ||
                s.endsWith(".flac", true) ||
                s.endsWith(".m4a", true) ||
                s.endsWith(".ogg", true) ||
                s.endsWith(".wav", true)
            return if (isAudioCandidate) AudioCoverFetcher(context, data) else null
        }
    }
}
