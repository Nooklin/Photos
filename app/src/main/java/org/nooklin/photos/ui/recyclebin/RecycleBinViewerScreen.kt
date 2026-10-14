package org.nooklin.photos.ui.recyclebin

import org.nooklin.photos.ui.common.VideoControlBar
import org.nooklin.photos.ui.common.VideoControlState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.nooklin.photos.data.local.LocalMetadataStore.RecycleBinEntry
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.StableList
import org.nooklin.photos.ui.common.ZoomBackdrop
import org.nooklin.photos.ui.common.ZoomLaunch
import org.nooklin.photos.ui.common.VideoPlayerView
import org.nooklin.photos.ui.common.ZoomableImage
import org.nooklin.photos.ui.common.rememberPhotoZoomTransition
import org.nooklin.photos.ui.common.HeroPhotoFrame
import org.nooklin.photos.ui.common.UNKNOWN_PHOTO_ASPECT_RATIO
import org.nooklin.photos.ui.common.aspectRatioOrUnknown
import org.nooklin.photos.ui.common.zoomPagerBounds
import androidx.compose.animation.core.animateFloatAsState
import kotlin.math.min
import androidx.compose.ui.Alignment
import org.nooklin.photos.ui.viewer.chromeGate
import androidx.compose.foundation.layout.PaddingValues
import org.nooklin.photos.ui.viewer.videoFramePadding
import org.nooklin.photos.ui.viewer.photoFrameInset
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import org.nooklin.photos.ui.common.VideoPrewarm
import org.nooklin.photos.ui.common.videoUriOf
import kotlinx.coroutines.delay
import org.nooklin.photos.R
import org.nooklin.photos.appString

// No edit/favorite/hide here, these are pending deletion
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinViewerScreen(
    entries: StableList<RecycleBinEntry>,
    startIndex: Int,
    onBack: () -> Unit,
    onRestore: (Long) -> Unit,
    restoreConfirmTitle: String? = null,
    restoreConfirmText: String? = null,
    zoomLaunch: ZoomLaunch? = null
) {
    val list = entries.value
    val context = LocalContext.current
    if (list.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val safeStartIndex = startIndex.coerceIn(0, list.size - 1)
    val pagerState = rememberPagerState(initialPage = safeStartIndex) { list.size }
    val currentEntry = list.getOrNull(pagerState.currentPage)
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var currentPageZoomedIn by remember { mutableStateOf(false) }
    // VideoPlayerView never reports a zoom state, a video could inherit a stuck "zoomed in" from the previous photo
    LaunchedEffect(pagerState.currentPage) { currentPageZoomedIn = false }
    // Keyed on the page index, a new photo gets a fresh unzoomed state
    val currentZoomScale = remember(pagerState.currentPage) { mutableStateOf(1f) }
    val currentZoomOffset = remember(pagerState.currentPage) { mutableStateOf(Offset.Zero) }
    // Shared by VideoPlayerView and the control bar, declared before both
    val videoControls = remember { VideoControlState() }
    val coroutineScope = rememberCoroutineScope()

    val zoom = rememberPhotoZoomTransition(zoomLaunch)
    suspend fun animateBackOut() {
        val entry = currentEntry
        zoom.exit(
            currentKey = entry?.mediaId,
            currentModel = entry?.uri,
            currentCacheKey = entry?.let { "${it.uri}|${it.deletedAt}" },
            photoAspectRatio = entry?.aspectRatioOrUnknown() ?: UNKNOWN_PHOTO_ASPECT_RATIO,
            scrimColor = Color.Black,
            zoomScale = currentZoomScale.value,
            zoomOffset = currentZoomOffset.value,
            onBack = onBack
        )
    }
    BackHandler(enabled = true) {
        coroutineScope.launch { animateBackOut() }
    }

    var chromeVisible by remember { mutableStateOf(true) }
    var hasSwiped by remember { mutableStateOf(false) }
    // Prepare neighbouring videos after the open animation
    val transitionDone by remember { derivedStateOf { zoom.progressState.value >= 1f } }
    // Pager must be at rest: settledPage updates when the finger lifts, mounting the player mid-slide showed black
    val pagerIdle by remember {
        derivedStateOf { !pagerState.isScrollInProgress && pagerState.currentPageOffsetFraction == 0f }
    }
    LaunchedEffect(pagerState.settledPage, list, transitionDone, pagerIdle) {
        // Player creation costs main-thread time, keep it out of a swipe
        if (!transitionDone || !pagerIdle) return@LaunchedEffect
        delay(250)
        val current = pagerState.settledPage
        val near = listOf(current - 1, current + 1).mapNotNull { index -> list.getOrNull(index)?.takeIf { it.isVideo }?.let { videoUriOf(it.uri) } }.toSet()
        val currentVideo = list.getOrNull(current)?.takeIf { it.isVideo }?.let { videoUriOf(it.uri) }
        VideoPrewarm.update(context, warm = near, keep = near + listOfNotNull(currentVideo))
    }
    DisposableEffect(Unit) { onDispose { VideoPrewarm.clear() } }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != safeStartIndex) hasSwiped = true
    }
    val chromeToggleAlpha by animateFloatAsState(if (chromeVisible) 1f else 0f, label = "viewerChrome")

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                modifier = Modifier
                    .graphicsLayer { alpha = min(zoom.chromeAlpha(), chromeToggleAlpha) }
                    .chromeGate(chromeVisible) { chromeVisible = true },
                title = {
                    currentEntry?.let {
                        Text(appString(R.string.recycle_bin_viewer_days_left, daysRemaining(it.deletedAt)), color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { coroutineScope.launch { animateBackOut() } }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        bottomBar = {
            Column(
                Modifier
                    .graphicsLayer { alpha = min(zoom.chromeAlpha(), chromeToggleAlpha) }
                    .chromeGate(chromeVisible) { chromeVisible = true }
                    .background(Color.Black)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp)
            ) {
                currentEntry?.let { entry ->
                    Button(
                        onClick = {
                            if (restoreConfirmTitle != null && restoreConfirmText != null) {
                                showRestoreConfirm = true
                            } else {
                                onRestore(entry.mediaId)
                                onBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(appString(R.string.common_restore))
                    }
                }
            }
        }
    ) { padding ->
        val pagerPadding = videoFramePadding(padding, 64.dp)
        val photoInset = photoFrameInset(64.dp, 0.dp)
        val settledIsVideo = list.getOrNull(pagerState.settledPage)?.isVideo == true
        Box(Modifier.fillMaxSize()) {
            ZoomBackdrop(zoom)
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pagerPadding)
                    .padding(if (settledIsVideo) PaddingValues() else photoInset)
                    .zoomPagerBounds(zoom)
            )
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = !currentPageZoomedIn,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pagerPadding)
            ) { page ->
                val pageEntry = list.getOrNull(page)
                if (pageEntry != null) {
                    val isCurrentPage = page == pagerState.currentPage
                    val pageFrame = Modifier.fillMaxSize().padding(if (pageEntry.isVideo) PaddingValues() else photoInset)
                    // No stored width/height for recycle-bin entries, the hero assumes square
                    val pager = zoom.pagerBounds
                    if (isCurrentPage && zoom.initialBounds != null && pager == null) {
                        // Bounds not measured yet: falling through mounted ZoomableImage full size for a frame
                    } else if (isCurrentPage && zoom.initialBounds != null && pager != null && zoom.progressState.value < 1f) {
                        val initial = zoom.initialBounds!!
                        HeroPhotoFrame(
                            model = pageEntry.uri,
                            cacheKey = "${pageEntry.uri}|${pageEntry.deletedAt}",
                            initialAspectRatio = pageEntry.aspectRatioOrUnknown(),
                            cellRect = { Rect(initial.left - pager.left, initial.top - pager.top, initial.right - pager.left, initial.bottom - pager.top) },
                            viewportRect = { Rect(0f, 0f, pager.width, pager.height) },
                            progress = { zoom.progressState.value },
                            modifier = pageFrame
                        )
                    } else {
                        Box(Modifier.fillMaxSize()) {
                            ZoomableImage(
                                model = pageEntry.uri,
                                freshnessKey = pageEntry.deletedAt,
                                isCurrentPage = isCurrentPage,
                                isVideo = pageEntry.isVideo,
                                onZoomStateChanged = { zoomedIn -> currentPageZoomedIn = zoomedIn },
                                onTap = { chromeVisible = !chromeVisible },
                                modifier = pageFrame,
                                zoomScale = if (isCurrentPage) currentZoomScale else remember { mutableStateOf(1f) },
                                zoomOffset = if (isCurrentPage) currentZoomOffset else remember { mutableStateOf(Offset.Zero) }
                            )
                            if (isCurrentPage && pageEntry.isVideo && pagerIdle) {
                                // Video fades in over the still image, nothing is swapped mid-swipe
                                VideoPlayerView(
                                    model = pageEntry.uri,
                                    modifier = pageFrame,
                                    controlState = videoControls,
                                    onTap = { chromeVisible = !chromeVisible },
                                    playEnabled = pagerIdle,
                                    poster = {
                                        if (!hasSwiped && page == safeStartIndex && pager != null) {
                                            val fullFrame = Rect(0f, 0f, pager.width, pager.height)
                                            HeroPhotoFrame(
                                                model = pageEntry.uri,
                                                cacheKey = "${pageEntry.uri}|${pageEntry.deletedAt}",
                                                initialAspectRatio = pageEntry.aspectRatioOrUnknown(),
                                                cellRect = { fullFrame },
                                                viewportRect = { fullFrame },
                                                progress = { 1f },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = padding.calculateBottomPadding())
                    .padding(horizontal = 16.dp)
                    .graphicsLayer { alpha = min(zoom.chromeAlpha(), chromeToggleAlpha) }
                    .chromeGate(chromeVisible) { chromeVisible = true }
            ) {
                AnimatedVisibility(
                    visible = currentEntry?.isVideo == true,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    VideoControlBar(
                        state = videoControls,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                }
            }
        }
    }

    if (showRestoreConfirm && restoreConfirmTitle != null && restoreConfirmText != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            shape = GlassPill.shape,
            containerColor = GlassPill.fixedPopupBackground,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text(restoreConfirmTitle) },
            text = { Text(restoreConfirmText) },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    currentEntry?.let { onRestore(it.mediaId) }
                    onBack()
                }) {
                    Text(appString(R.string.common_restore))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text(appString(R.string.common_cancel)) }
            }
        )
    }
}
