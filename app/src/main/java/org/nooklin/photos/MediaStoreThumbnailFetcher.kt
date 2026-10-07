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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

// Grid cells use loadThumbnail(), decoding originals is too slow.
// Original and near-full-screen requests fall through to Coil.
class MediaStoreThumbnailFetcher(
    private val uri: Uri,
    private val options: Options
) : Fetcher {
    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        val width = options.size.width.pxOrElse { 512 }
        val height = options.size.height.pxOrElse { 512 }
        val context = options.context
        // Large cells decode the photo at cell size, small cells use Android's thumbnail
        val screenWidth = context.resources.displayMetrics.widthPixels
        // Above this it is the viewer, not a grid cell
        val largeCell = !uri.path.orEmpty().contains("/video/media") &&
            width >= screenWidth * LARGE_CELL_FRACTION && width < screenWidth * MAX_CELL_FRACTION
        val decoded = if (largeCell) decodeLimited(width, height) else null
        val bitmap = decoded ?: context.contentResolver.loadThumbnail(uri, android.util.Size(width, height), null)
        ImageFetchResult(
            image = bitmap.asImage(),
            // false: Coil rejects cached bitmaps smaller than the request, thumbnails nearly always are
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }

    // Few at a time: 6 to 8 in parallel took over a second each
    private suspend fun decodeLimited(width: Int, height: Int): android.graphics.Bitmap? =
        decodeSlots.withPermit { decodeAtCellSize(width, height) }

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
            // Near-full-screen requests (viewer, prefetch) fall through to Coil
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

// 2 and 3 column grids
private const val LARGE_CELL_FRACTION = 0.28f

// Above this it is the viewer, not a grid cell
private const val MAX_CELL_FRACTION = 0.6f

private val decodeSlots = Semaphore(3)
