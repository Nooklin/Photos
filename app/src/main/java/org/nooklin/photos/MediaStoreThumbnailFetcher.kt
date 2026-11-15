package org.nooklin.photos

import android.graphics.ImageDecoder
import android.net.Uri
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import coil3.size.pxOrElse
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class MediaStoreThumbnailFetcher(
    private val uri: Uri,
    private val options: Options
) : Fetcher {
    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        val width = options.size.width.pxOrElse { 512 }
        val height = options.size.height.pxOrElse { 512 }
        val context = options.context
        val screenWidth = context.resources.displayMetrics.widthPixels
        val largeCell = !uri.path.orEmpty().contains("/video/media") &&
            width >= screenWidth * 0.28f && width < screenWidth * 0.6f
        val decodeKey = "${options.diskCacheKey ?: uri}|$width|$height"
        val decoded = if (largeCell) decodedCache.get(decodeKey) ?: decodeLimited(decodeKey, width, height) else null
        val bitmap = decoded ?: context.contentResolver.loadThumbnail(uri, android.util.Size(width, height), null)
        ImageFetchResult(
            image = bitmap.asImage(),
            // false: Coil rejects cached bitmaps smaller than the request, thumbnails nearly always are
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }

    private suspend fun decodeLimited(key: String, width: Int, height: Int): android.graphics.Bitmap? =
        decodeSlots.withPermit {
            withContext(NonCancellable) {
                decodeAtCellSize(width, height)?.also { decodedCache.put(key, it) }
            }
        }

    private fun decodeAtCellSize(width: Int, height: Int): android.graphics.Bitmap? = try {
        val source = ImageDecoder.createSource(options.context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val scale = maxOf(width.toFloat() / info.size.width, height.toFloat() / info.size.height).coerceAtMost(1f)
            decoder.setTargetSize(
                (info.size.width * scale).toInt().coerceAtLeast(1),
                (info.size.height * scale).toInt().coerceAtLeast(1)
            )
        }
    } catch (e: Exception) {
        null
    }

    class Factory : Fetcher.Factory<coil3.Uri> {
        override fun create(data: coil3.Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val androidUri = Uri.parse(data.toString())
            if (androidUri.scheme != "content" || androidUri.authority != "media") return null
            val path = androidUri.path.orEmpty()
            if (!path.contains("/images/media") && !path.contains("/video/media")) return null
            // Size.ORIGINAL has no pixel size, don't cap it at 512px
            val isVideo = path.contains("/video/media")
            if (!isVideo && options.size == coil3.size.Size.ORIGINAL) return null
            val width = options.size.width.pxOrElse { 0 }
            val height = options.size.height.pxOrElse { 0 }
            val metrics = options.context.resources.displayMetrics
            val isNearFullScreen = width >= metrics.widthPixels * NEAR_FULL_SCREEN_FRACTION &&
                height >= metrics.heightPixels * NEAR_FULL_SCREEN_FRACTION
            if (isNearFullScreen && !isVideo) return null
            return MediaStoreThumbnailFetcher(androidUri, options)
        }
    }
}

private const val NEAR_FULL_SCREEN_FRACTION = 0.9

private val decodeSlots = Semaphore(4)

private val decodedCache = object : LruCache<String, android.graphics.Bitmap>(32 * 1024 * 1024) {
    override fun sizeOf(key: String, value: android.graphics.Bitmap): Int = value.allocationByteCount
}
