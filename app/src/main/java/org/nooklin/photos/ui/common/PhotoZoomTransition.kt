package org.nooklin.photos.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size as CoilSize
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

// Grid to viewer zoom shared by every viewer so they can't drift apart

private val HERO_EASING = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

private const val CHROME_FADE_START = 0.9f
private val GRID_CELL_CORNER = 8.dp
const val UNKNOWN_PHOTO_ASPECT_RATIO = 1f

fun org.nooklin.photos.data.MediaItem.aspectRatioOrUnknown(): Float {
    val w = width; val h = height
    return if (w != null && h != null && h > 0) w.toFloat() / h.toFloat() else UNKNOWN_PHOTO_ASPECT_RATIO
}

fun org.nooklin.photos.data.HiddenItem.aspectRatioOrUnknown(): Float {
    val w = width; val h = height
    return if (w != null && h != null && h > 0) w.toFloat() / h.toFloat() else UNKNOWN_PHOTO_ASPECT_RATIO
}

fun org.nooklin.photos.data.local.LocalMetadataStore.RecycleBinEntry.aspectRatioOrUnknown(): Float {
    val w = width; val h = height
    return if (w != null && h != null && h > 0) w.toFloat() / h.toFloat() else UNKNOWN_PHOTO_ASPECT_RATIO
}

internal fun chromeFadeAlpha(zoomProgress: Float): Float =
    ((zoomProgress - CHROME_FADE_START) / (1f - CHROME_FADE_START)).coerceIn(0f, 1f)

@Immutable
class ZoomLaunch(
    val initialBounds: Rect?,
    val backdrop: ImageBitmap?,
    val cellBounds: Map<Any, Rect>
)

@Immutable
class ReturnOverlayRequest(
    val key: Any,
    val model: Any,
    val cacheKey: String,
    val photoAspectRatio: Float,
    val viewportBounds: Rect,
    val scrimColor: Color
)

@Stable
object ZoomSources {
    // Plain HashMap: written on every layout pass while scrolling, snapshot state would write every frame
    internal val liveCoordinates = HashMap<Any, LayoutCoordinates>()
    internal var backdropLayer: GraphicsLayer? = null
    var pendingScrollKey by mutableStateOf<Any?>(null)
    var returnOverlay by mutableStateOf<ReturnOverlayRequest?>(null)

    fun boundsOf(key: Any): Rect? = liveCoordinates[key]?.takeIf { it.isAttached }?.boundsInWindow()

    fun snapshotBounds(): Map<Any, Rect> {
        val result = HashMap<Any, Rect>(liveCoordinates.size)
        for ((key, coordinates) in liveCoordinates) {
            if (coordinates.isAttached) result[key] = coordinates.boundsInWindow()
        }
        return result
    }

    // For grids that don't capture their own bounds
    suspend fun capture(tappedKey: Any): ZoomLaunch {
        val snapshot = snapshotBounds()
        val backdrop = runCatching { backdropLayer?.toImageBitmap() }.getOrNull()
        return ZoomLaunch(snapshot[tappedKey], backdrop, snapshot)
    }
}

fun Modifier.zoomSource(key: Any): Modifier = composed {
    DisposableEffect(key) { onDispose { ZoomSources.liveCoordinates.remove(key) } }
    Modifier.onPlaced { ZoomSources.liveCoordinates[key] = it }
}

fun Modifier.zoomBackdropSource(): Modifier = composed {
    val layer = rememberGraphicsLayer()
    DisposableEffect(layer) {
        ZoomSources.backdropLayer = layer
        onDispose { if (ZoomSources.backdropLayer === layer) ZoomSources.backdropLayer = null }
    }
    Modifier.drawWithContent {
        layer.record { this@drawWithContent.drawContent() }
        drawLayer(layer)
    }
}

// Scrolls to a photo that wasn't visible at tap time so the return overlay can land, no-op if already visible
@Composable
fun ScrollToPendingZoomKey(lazyIndexOf: (Any) -> Int?, scrollToIndex: suspend (Int) -> Unit) {
    val key = ZoomSources.pendingScrollKey
    val currentIndexOf by rememberUpdatedState(lazyIndexOf)
    val currentScroll by rememberUpdatedState(scrollToIndex)
    LaunchedEffect(key) {
        if (key != null) {
            if (ZoomSources.liveCoordinates[key] == null) {
                currentIndexOf(key)?.let { currentScroll(it) }
            }
            ZoomSources.pendingScrollKey = null
        }
    }
}

@Stable
class PhotoZoomTransitionState internal constructor(private val launch: ZoomLaunch?) {
    val progress = Animatable(if (launch?.initialBounds != null) 0f else 1f)
    val progressState: State<Float> = progress.asState()
    internal val chromeOverride = Animatable(1f)
    internal var pagerBounds by mutableStateOf<Rect?>(null)
    val initialBounds: Rect? get() = launch?.initialBounds
    val backdrop: ImageBitmap? get() = launch?.backdrop

    fun chromeAlpha(): Float = min(chromeFadeAlpha(progress.value), chromeOverride.value)

    // Always hands off to the return overlay and pops at once, the in-place shrink couldn't guarantee the grid was laid out
    suspend fun exit(
        currentKey: Any?,
        currentModel: Any?,
        currentCacheKey: String?,
        photoAspectRatio: Float,
        scrimColor: Color,
        zoomScale: Float = 1f,
        zoomOffset: Offset = Offset.Zero,
        onBack: () -> Unit
    ) {
        val pager = pagerBounds
        if (launch?.initialBounds == null || pager == null || currentKey == null || currentModel == null || currentCacheKey == null) {
            onBack()
            return
        }
        // snapTo, not animateTo: the fade delayed the zoom-out before it began
        chromeOverride.snapTo(0f)
        ZoomSources.pendingScrollKey = currentKey
        val viewport = if (zoomScale > 1.01f) {
            val pagerLocal = Rect(0f, 0f, pager.width, pager.height)
            val fitted = fittedRect(pagerLocal, if (photoAspectRatio > 0f) photoAspectRatio else UNKNOWN_PHOTO_ASPECT_RATIO)
            val cx = pagerLocal.center.x + zoomOffset.x
            val cy = pagerLocal.center.y + zoomOffset.y
            val w = fitted.width * zoomScale
            val h = fitted.height * zoomScale
            Rect(pager.left + cx - w / 2f, pager.top + cy - h / 2f, pager.left + cx + w / 2f, pager.top + cy + h / 2f)
        } else {
            pager
        }
        ZoomSources.returnOverlay = ReturnOverlayRequest(currentKey, currentModel, currentCacheKey, photoAspectRatio, viewport, scrimColor)
        onBack()
    }
}

@Composable
fun rememberPhotoZoomTransition(launch: ZoomLaunch?): PhotoZoomTransitionState {
    val state = remember { PhotoZoomTransitionState(launch) }
    LaunchedEffect(Unit) {
        if (launch?.initialBounds != null) {
            snapshotFlow { state.pagerBounds }.filterNotNull().first()
            state.progress.animateTo(1f, tween(350, easing = HERO_EASING))
        }
    }
    return state
}

// after the pager's padding, so it reports post-padding bounds
fun Modifier.zoomPagerBounds(state: PhotoZoomTransitionState): Modifier =
    onGloballyPositioned { state.pagerBounds = it.boundsInWindow() }

internal fun fittedRect(viewport: Rect, photoAspectRatio: Float): Rect {
    val ar = if (photoAspectRatio > 0f) photoAspectRatio else UNKNOWN_PHOTO_ASPECT_RATIO
    val scale = min(viewport.width / ar, viewport.height / 1f)
    val w = ar * scale
    val h = scale
    val cx = viewport.center.x
    val cy = viewport.center.y
    return Rect(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
}

@Composable
fun HeroPhotoFrame(
    model: Any,
    cacheKey: String,
    initialAspectRatio: Float,
    cellRect: () -> Rect,
    viewportRect: () -> Rect,
    progress: () -> Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initialAr = if (initialAspectRatio > 0f) initialAspectRatio else UNKNOWN_PHOTO_ASPECT_RATIO
    // Decode size is fixed per instance, keyed on (model, cacheKey) only: a changed ratio must not rebuild the request
    val viewport = viewportRect()
    val fitScaleForDecode = min(viewport.width / initialAr, viewport.height / 1f)
    val decodeWidth = (initialAr * fitScaleForDecode).roundToInt().coerceAtLeast(1)
    val decodeHeight = fitScaleForDecode.roundToInt().coerceAtLeast(1)
    val request = remember(model, cacheKey) {
        ImageRequest.Builder(context)
            .data(model)
            .size(CoilSize(decodeWidth, decodeHeight))
            // Own key: cacheKey is also the grid's key, whose small cell-size bitmap Coil would accept here as the full image
            .memoryCacheKey("zoom|$cacheKey")
            .placeholderMemoryCacheKey(cacheKey)
            .crossfade(150)
            .build()
    }
    // Starts at the guess, corrected to Coil's decoded size: MediaStore width/height don't always match how a photo displays
    var decodedAspectRatio by remember(model) { mutableFloatStateOf(initialAr) }
    val density = LocalDensity.current
    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer {
                val t = progress()
                val clipRect = lerpRect(cellRect(), fittedRect(viewportRect(), decodedAspectRatio), t)
                clip = t < 1f
                shape = if (t < 1f) {
                    ClipWindowShape(clipRect, GRID_CELL_CORNER.toPx() * (1f - t))
                } else {
                    RectangleShape
                }
            }
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            onSuccess = { state ->
                val image = state.result.image
                if (image.width > 0 && image.height > 0) {
                    decodedAspectRatio = image.width.toFloat() / image.height.toFloat()
                }
            },
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .size(with(density) { decodeWidth.toDp() }, with(density) { decodeHeight.toDp() })
                .graphicsLayer {
                    val ar = decodedAspectRatio
                    val cell = cellRect()
                    val vp = viewportRect()
                    val t = progress()
                    val fitScale = min(vp.width / ar, vp.height / 1f)
                    val thumbScale = max(cell.width / ar, cell.height / 1f)
                    val curScale = thumbScale + (fitScale - thumbScale) * t
                    val renderScale = curScale / fitScale
                    scaleX = renderScale
                    scaleY = renderScale
                    val cx = cell.center.x + (vp.center.x - cell.center.x) * t
                    val cy = cell.center.y + (vp.center.y - cell.center.y) * t
                    translationX = cx - decodeWidth / 2f
                    translationY = cy - decodeHeight / 2f
                }
        )
    }
}

private class ClipWindowShape(private val rect: Rect, private val cornerPx: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        if (cornerPx > 0f) {
            Outline.Rounded(RoundRect(rect, CornerRadius(cornerPx)))
        } else {
            Outline.Rectangle(rect)
        }
}

internal fun lerpRect(from: Rect, to: Rect, t: Float): Rect = Rect(
    from.left + (to.left - from.left) * t,
    from.top + (to.top - from.top) * t,
    from.right + (to.right - from.right) * t,
    from.bottom + (to.bottom - from.bottom) * t
)

// Grid snapshot behind the photo, fades with progress (switching it off in one frame showed a hard cut)
@Composable
fun ZoomBackdrop(state: PhotoZoomTransitionState) {
    val bitmap = state.backdrop ?: return
    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - state.progress.value }
    )
}

@Composable
fun ZoomReturnOverlay() {
    val request = ZoomSources.returnOverlay ?: return
    val progress = remember(request) { Animatable(1f) }
    var target by remember(request) { mutableStateOf<Rect?>(null) }
    // Starts at (0,0) so the photo draws on the first frame
    var origin by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(request) {
        var found: Rect? = null
        var frames = 0
        var framesSinceScrollCleared = 0
        while (found == null && frames < 30) {
            withFrameNanos { }
            frames++
            if (ZoomSources.pendingScrollKey == null) {
                framesSinceScrollCleared++
                if (framesSinceScrollCleared >= 2) found = ZoomSources.boundsOf(request.key)
            }
        }
        target = found
        progress.animateTo(0f, tween(280, easing = HERO_EASING))
        ZoomSources.returnOverlay = null
    }
    Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.boundsInWindow().topLeft }) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = progress.value }.background(request.scrimColor))
        val cell = target
        if (cell != null) {
            HeroPhotoFrame(
                model = request.model,
                cacheKey = request.cacheKey,
                initialAspectRatio = request.photoAspectRatio,
                cellRect = { Rect(cell.left - origin.x, cell.top - origin.y, cell.right - origin.x, cell.bottom - origin.y) },
                viewportRect = {
                    val v = request.viewportBounds
                    Rect(v.left - origin.x, v.top - origin.y, v.right - origin.x, v.bottom - origin.y)
                },
                progress = { progress.value }
            )
        } else {
            // The grid can't show the photo (e.g.
            val viewport = Rect(
                request.viewportBounds.left - origin.x, request.viewportBounds.top - origin.y,
                request.viewportBounds.right - origin.x, request.viewportBounds.bottom - origin.y
            )
            HeroPhotoFrame(
                model = request.model,
                cacheKey = request.cacheKey,
                initialAspectRatio = request.photoAspectRatio,
                cellRect = { viewport },
                viewportRect = { viewport },
                progress = { 1f },
                modifier = Modifier.graphicsLayer { alpha = progress.value }
            )
        }
    }
}

fun zoomLazyIndexOf(key: Any, keys: List<Any>, groups: List<List<Any>>?): Int? {
    if (groups == null) return keys.indexOf(key).takeIf { it >= 0 }
    var lazy = 0
    for (group in groups) {
        lazy++
        val i = group.indexOf(key)
        if (i >= 0) return lazy + i
        lazy += group.size
    }
    return null
}

// Two steps: item size and position are only known after layout
suspend fun LazyGridState.scrollToItemCentered(index: Int) {
    scrollToItem(index)
    val info = withTimeoutOrNull(500) {
        snapshotFlow { layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } }.filterNotNull().first()
    } ?: return
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    scrollBy(info.offset.y + info.size.height / 2f - viewportCenter)
}

suspend fun LazyStaggeredGridState.scrollToItemCentered(index: Int) {
    scrollToItem(index)
    val info = withTimeoutOrNull(500) {
        snapshotFlow { layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } }.filterNotNull().first()
    } ?: return
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    scrollBy(info.offset.y + info.size.height / 2f - viewportCenter)
}
