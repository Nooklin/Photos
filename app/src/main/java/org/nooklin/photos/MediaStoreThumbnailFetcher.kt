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
import org.nooklin.photos.data.ThumbnailStore
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
        val context = options.context
        val requestedWidth = options.size.width.pxOrElse { 512 }
        val requestedHeight = options.size.height.pxOrElse { 512 }
        // grid sized requests go through the thumbnail store, one size for all of them
        val storeKey = options.diskCacheKey?.removeSuffix("|quick")
            ?.takeIf { maxOf(requestedWidth, requestedHeight) <= STORE_MAX_REQUEST_PX }
        if (storeKey != null) {
            ThumbnailStore.read(context, storeKey)?.let { stored ->
                return@withContext ImageFetchResult(image = stored.asImage(), isSampled = false, dataSource = DataSource.DISK)
            }
        }
        val width = if (storeKey != null) ThumbnailStore.STORE_PX else requestedWidth
        val height = if (storeKey != null) ThumbnailStore.STORE_PX else requestedHeight
        val screenWidth = context.resources.displayMetrics.widthPixels
        val largeCell = !uri.path.orEmpty().contains("/video/media") &&
            width >= screenWidth * 0.28f && width < screenWidth * 0.6f
        val decodeKey = "${options.diskCacheKey ?: uri}|$width|$height"
        val decoded = if (largeCell) decodedCache.get(decodeKey) ?: decodeLimited(decodeKey, width, height) else null
        val bitmap = decoded ?: loadCovering(width, height)
        if (storeKey != null) ThumbnailStore.write(context, storeKey, bitmap)
        ImageFetchResult(
            image = bitmap.asImage(),
            // false: Coil rejects cached bitmaps smaller than the request, thumbnails nearly always are
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }

    // loadThumbnail fits the image inside the box, so a portrait photo in a square cell came back with a short side
    // below the cell and was scaled up (blurry). Ask for a bigger box, and once more when the shape was extreme
    private fun loadCovering(width: Int, height: Int): android.graphics.Bitmap {
        val resolver = options.context.contentResolver
        var scale = 1.34f
        var bitmap = resolver.loadThumbnail(uri, android.util.Size((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1)), null)
        val short = minOf(bitmap.width, bitmap.height)
        val wanted = minOf(width, height)
        if (short < wanted * 0.9f) {
            scale *= wanted.toFloat() / short
            bitmap = resolver.loadThumbnail(uri, android.util.Size((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1)), null)
        }
        return bitmap
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

private const val STORE_MAX_REQUEST_PX = 480

private val decodeSlots = Semaphore(4)

private val decodedCache = object : LruCache<String, android.graphics.Bitmap>(32 * 1024 * 1024) {
    override fun sizeOf(key: String, value: android.graphics.Bitmap): Int = value.allocationByteCount
}
