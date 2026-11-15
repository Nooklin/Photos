package org.nooklin.photos.ui.common

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.maxBitmapSize
import coil3.size.Size
import kotlin.math.abs
import kotlinx.coroutines.delay
import org.nooklin.photos.data.MediaItem
import androidx.compose.runtime.rememberUpdatedState
import android.net.Uri
import java.io.File
import org.nooklin.photos.VideoFirstFrame
import org.nooklin.photos.data.VideoTrims

@Composable
fun ZoomableImage(
    model: Any,
    freshnessKey: Any,
    isCurrentPage: Boolean,
    onZoomStateChanged: (Boolean) -> Unit,
    // Cache key of the thumbnail the grid drew, so the entrance starts from it
    placeholderCacheKey: String? = null,
    modifier: Modifier = Modifier,
    isVideo: Boolean = false,
    // Hoisted so a caller can read the live zoom without lag, default is private
    onTap: () -> Unit = {},
    zoomScale: MutableState<Float> = remember { mutableStateOf(1f) },
    zoomOffset: MutableState<Offset> = remember { mutableStateOf(Offset.Zero) }
) {
    var scale by zoomScale
    var offset by zoomOffset
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val context = LocalContext.current
    val screenSize = remember { screenTierSize(context) }
    // Once pinched past 1x keep the full tier, avoids re-decoding on every pinch.
    var hasZoomedIn by remember { mutableStateOf(false) }
    val currentOnTap by rememberUpdatedState(onTap)
    // Only the zoomed-in boolean is observed here, so pinch frames do not recompose this composable.
    val zoomedIn by remember(zoomScale) { derivedStateOf { zoomScale.value > 1.01f } }
    LaunchedEffect(zoomedIn) {
        if (zoomedIn && !isVideo) hasZoomedIn = true
    }

    LaunchedEffect(isCurrentPage, zoomedIn) {
        if (isCurrentPage) onZoomStateChanged(zoomedIn)
    }

    fun clampOffset(newScale: Float, candidate: Offset): Offset {
        val maxX = (containerSize.width * (newScale - 1f) / 2f).coerceAtLeast(0f)
        val maxY = (containerSize.height * (newScale - 1f) / 2f).coerceAtLeast(0f)
        return Offset(candidate.x.coerceIn(-maxX, maxX), candidate.y.coerceIn(-maxY, maxY))
    }

    var isLoading by remember { mutableStateOf(false) }
    var showLoadingSpinner by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(400)
            if (isLoading) showLoadingSpinner = true
        } else {
            showLoadingSpinner = false
        }
    }

    Box(
        modifier
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { currentOnTap() },
                    onDoubleTap = {
                        if (scale > 1.01f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 3f
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectPinchPanGestures(
                    getScale = { scale },
                    onGesture = { pan, zoom ->
                        val newScale = (scale * zoom).coerceIn(1f, 6f)
                        scale = newScale
                        offset = if (newScale <= 1f) Offset.Zero else clampOffset(newScale, offset + pan)
                    }
                )
            }
    ) {
        val fullResolution = hasZoomedIn && !isVideo
        // A video keeps one small request: a size change is a new request and frames at screen size are slow.
        val sizeTier = when {
            fullResolution -> Size.ORIGINAL
            isVideo -> PREVIEW_TIER_SIZE
            isCurrentPage -> screenSize
            else -> PREVIEW_TIER_SIZE
        }
        val maxBitmapTier = when {
            fullResolution -> Size(2048, 2048)
            isVideo -> PREVIEW_TIER_SIZE
            isCurrentPage -> screenSize
            else -> PREVIEW_TIER_SIZE
        }
        val pinchZoomTransform: Modifier.() -> Modifier = {
            graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
        }
        // One cache key per tier, and none shared with the grid: the grid stores its small cell-size bitmap under "model|freshness"
        val tierName = when {
            fullResolution -> "full"
            isVideo -> "preview"
            isCurrentPage -> "screen"
            else -> "preview"
        }
        val imageRequest = remember(model, freshnessKey, sizeTier, maxBitmapTier, placeholderCacheKey) {
            ImageRequest.Builder(context)
                .data(model)
                .size(sizeTier)
                .maxBitmapSize(maxBitmapTier)
                .memoryCacheKey(viewerCacheKey(model, freshnessKey, tierName))
                .diskCacheKey(viewerCacheKey(model, freshnessKey, tierName))
                .placeholderMemoryCacheKey(placeholderCacheKey ?: "$model|$freshnessKey")
                // No crossfade: fading from the small preview to the full size image scales the two to a common size
                .crossfade(false)
                .build()
        }
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            onLoading = {
                isLoading = true
            },
            onSuccess = {
                isLoading = false
            },
            onError = { isLoading = false },
            // Always Fit: only mounted after the hero transition has settled
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pinchZoomTransform()
        )
        // The frame the player starts on, drawn over the system thumbnail (a frame from elsewhere in the clip) so the still matches the first video fr
        val firstFrameSource: Any? = when {
            !isVideo -> null
            model is File -> model
            model is Uri -> model
            model is String && model.contains("/video/media") -> Uri.parse(model)
            else -> null
        }
        if (firstFrameSource != null) {
            val trimStartMs = VideoTrims.get(firstFrameSource)?.startMs ?: 0L
            val firstFrameRequest = remember(firstFrameSource, freshnessKey, trimStartMs) {
                ImageRequest.Builder(context)
                    .data(VideoFirstFrame(firstFrameSource, trimStartMs))
                    .memoryCacheKey("$firstFrameSource|$freshnessKey|first|$trimStartMs")
                    .diskCacheKey("$firstFrameSource|$freshnessKey|first|$trimStartMs")
                    .crossfade(300)
                    .build()
            }
            AsyncImage(
                model = firstFrameRequest,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pinchZoomTransform()
            )
        }
        if (showLoadingSpinner) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

internal fun viewerCacheKey(model: Any, freshnessKey: Any, tier: String): String = "view|$model|$freshnessKey|$tier"

private val PREVIEW_TIER_SIZE = Size(1024, 1024)


internal fun screenTierSize(context: android.content.Context): Size {
    val metrics = context.resources.displayMetrics
    return Size(metrics.widthPixels, metrics.heightPixels)
}

// Fired from the grid's tap handler so the decode runs in parallel with navigation (~300-450ms)
internal fun prefetchScreenTierImage(context: android.content.Context, item: MediaItem) {
    val mimeType = item.mimeType?.lowercase()
    val isHeic = mimeType != null && ("heic" in mimeType || "heif" in mimeType)
    if (item.isVideo || isHeic) return
    val size = screenTierSize(context)
    val request = ImageRequest.Builder(context)
        .data(item.uri)
        .size(size)
        .maxBitmapSize(size)
        .memoryCacheKey(viewerCacheKey(item.uri, item.dateModified, "screen"))
        .diskCacheKey(viewerCacheKey(item.uri, item.dateModified, "screen"))
        .build()
    context.imageLoader.enqueue(request)
}

internal suspend fun PointerInputScope.detectPinchPanGestures(
    getScale: () -> Float,
    onGesture: (pan: Offset, zoom: Float) -> Unit
) {
    awaitEachGesture {
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                if (event.changes.size < 2 && getScale() <= 1.01f) {
                    continue
                }

                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    pan += panChange
                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val panMotion = pan.getDistance()
                    if (zoomMotion > touchSlop || panMotion > touchSlop) {
                        pastTouchSlop = true
                    }
                }

                if (pastTouchSlop) {
                    if (zoomChange != 1f || panChange != Offset.Zero) {
                        onGesture(panChange, zoomChange)
                    }
                    event.changes.forEach { it.consume() }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
    }
}
