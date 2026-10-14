package org.nooklin.photos.ui.hidden

import org.nooklin.photos.ui.common.VideoControlBar
import org.nooklin.photos.ui.common.VideoControlState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.nooklin.photos.ui.common.SharePreparingDialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nooklin.photos.data.HiddenAlbumWithPreview
import org.nooklin.photos.data.HiddenItem
import org.nooklin.photos.data.PhotoMetadata
import org.nooklin.photos.data.buildFileMetadata
import org.nooklin.photos.data.createExifStrippedShareUriFromFile
import org.nooklin.photos.ui.common.ConfirmDeleteDialog
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.GlassPillContainer
import org.nooklin.photos.ui.common.GlassPillTextField
import org.nooklin.photos.ui.common.PillListRow
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
import org.nooklin.photos.ui.viewer.DateTimePill
import org.nooklin.photos.ui.viewer.PhotoFilmstrip
import org.nooklin.photos.ui.viewer.PhotoInfoSheet
import org.nooklin.photos.ui.viewer.ViewerCircleButton
import org.nooklin.photos.ui.viewer.ViewerPillHeight
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import kotlin.math.min
import org.nooklin.photos.ui.viewer.chromeGate
import org.nooklin.photos.ui.viewer.ViewerTopBarHeight
import org.nooklin.photos.ui.viewer.FilmstripHeight
import androidx.compose.foundation.layout.PaddingValues
import org.nooklin.photos.ui.viewer.videoFramePadding
import org.nooklin.photos.ui.viewer.photoFrameInset
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import org.nooklin.photos.ui.common.VideoPrewarm
import org.nooklin.photos.ui.common.videoUriOf
import kotlinx.coroutines.delay
import org.nooklin.photos.R
import org.nooklin.photos.appString

// Share is opt-in (Hidden Image Sharing in Settings), easy sharing works against hiding
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenPhotoViewerScreen(
    // StableList so unrelated state changes can skip recomposition
    items: StableList<HiddenItem>,
    // Captured by the grid at tap time (see ui/common/PhotoZoomTransition.kt).
    zoomLaunch: ZoomLaunch? = null,
    darkTheme: Boolean,
    startIndex: Int,
    existingAlbums: List<HiddenAlbumWithPreview>,
    shareEnabled: Boolean,
    removeExifOnShare: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: (HiddenItem) -> Unit,
    onUnhide: (HiddenItem) -> Unit,
    onTrash: (HiddenItem) -> Unit,
    onEdit: (HiddenItem) -> Unit,
    onAddToAlbum: (albumId: Long, item: HiddenItem) -> Unit,
    onCreateAlbum: (name: String, onCreated: (Long) -> Unit) -> Unit
) {
    if (items.value.isEmpty()) {
        // onBack is navigation, defer it out of composition
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val safeStartIndex = startIndex.coerceIn(0, items.value.size - 1)
    val pagerState = rememberPagerState(initialPage = safeStartIndex) { items.value.size }
    val currentItem = items.value.getOrNull(pagerState.currentPage)
    val coroutineScope = rememberCoroutineScope()
    // Same zoom as the main viewer
    val zoom = rememberPhotoZoomTransition(zoomLaunch)
    val viewerBackground = if (darkTheme && currentItem != null) Color.Black else MaterialTheme.colorScheme.background
    // Keyed on the page index so a swipe starts at 1x without pan.
    // Declared before animateBackOut, locals can't be forward-referenced.
    val currentZoomScale = remember(pagerState.currentPage) { mutableStateOf(1f) }
    val currentZoomOffset = remember(pagerState.currentPage) { mutableStateOf(Offset.Zero) }
    // Shared by VideoPlayerView and the control bar, declared before both
    val videoControls = remember { VideoControlState() }
    // VideoPlayerView never reports a zoom state, so a video could inherit a stuck "zoomed in" from the previous photo
    suspend fun animateBackOut() {
        val item = currentItem
        zoom.exit(
            currentKey = item?.file?.absolutePath,
            currentModel = item?.file,
            currentCacheKey = item?.let { "${it.file}|${it.file.lastModified()}" },
            photoAspectRatio = item?.aspectRatioOrUnknown() ?: UNKNOWN_PHOTO_ASPECT_RATIO,
            scrimColor = viewerBackground,
            zoomScale = currentZoomScale.value,
            zoomOffset = currentZoomOffset.value,
            onBack = onBack
        )
    }
    BackHandler(enabled = true) {
        coroutineScope.launch { animateBackOut() }
    }
    val context = LocalContext.current
    // Chrome flips with the themed background
    val chromeColor = if (darkTheme) Color.White else Color.Black
    val popupBackground = if (darkTheme) GlassPill.fixedPopupBackground else GlassPill.adaptivePopupBackground
    val toolbarBackground = if (darkTheme) GlassPill.fixedGlassBackground else GlassPill.adaptiveGlassBackground
    val toolbarBorder = if (darkTheme) null else GlassPill.adaptiveBorder

    var showInfo by remember { mutableStateOf(false) }
    var showUnhideConfirm by remember { mutableStateOf(false) }
    var showTrashConfirm by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var preparingJob by remember { mutableStateOf<Job?>(null) }
    if (preparingJob != null) {
        SharePreparingDialog(onCancel = {
            preparingJob?.cancel()
            preparingJob = null
        })
    }
    var showAddToAlbumDialog by remember { mutableStateOf(false) }
    var metadata by remember { mutableStateOf<PhotoMetadata?>(null) }
    var pillMetadata by remember { mutableStateOf<PhotoMetadata?>(null) }
    var currentPageZoomedIn by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState.currentPage) { currentPageZoomedIn = false }
    // Keyed on the page index, a new photo gets a fresh unzoomed state

    // Always runs (for the pill), the info sheet load only runs when opened. EXIF is read off the main thread.
    LaunchedEffect(currentItem) {
        val item = currentItem
        pillMetadata = if (item != null) {
            withContext(Dispatchers.IO) { buildFileMetadata(item.file, item.isVideo) }
        } else null
    }

    LaunchedEffect(currentItem) { metadata = null }
    LaunchedEffect(showInfo, currentItem) {
        val item = currentItem
        if (showInfo && item != null) {
            metadata = withContext(Dispatchers.IO) { buildFileMetadata(item.file, item.isVideo) }
        }
    }

    // Tap anywhere on the media to hide or show every control, like the iPhone viewer.
    var chromeVisible by remember { mutableStateOf(true) }
    var hasSwiped by remember { mutableStateOf(false) }
    // Prepare the videos next to the current page once the open animation is done, so a swipe onto one starts quickly.
    val transitionDone by remember { derivedStateOf { zoom.progressState.value >= 1f } }
    // True only when the pager is fully at rest. settledPage can update the moment the finger lifts, while the
    // page is still sliding, which mounted the player mid-slide and showed a black surface.
    val pagerIdle by remember {
        derivedStateOf { !pagerState.isScrollInProgress && pagerState.currentPageOffsetFraction == 0f }
    }
    LaunchedEffect(pagerState.settledPage, items.value, transitionDone, pagerIdle) {
        // Only when the pager is fully at rest, and after a short pause: creating a player costs main-thread time
        // that must not land in the middle of a swipe.
        if (!transitionDone || !pagerIdle) return@LaunchedEffect
        delay(250)
        val current = pagerState.settledPage
        val near = listOf(current - 1, current + 1).mapNotNull { index -> items.value.getOrNull(index)?.takeIf { it.isVideo }?.let { videoUriOf(it.file) } }.toSet()
        val currentVideo = items.value.getOrNull(current)?.takeIf { it.isVideo }?.let { videoUriOf(it.file) }
        VideoPrewarm.update(context, warm = near, keep = near + listOfNotNull(currentVideo))
    }
    DisposableEffect(Unit) { onDispose { VideoPrewarm.clear() } }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != safeStartIndex) hasSwiped = true
    }
    val chromeToggleAlpha by animateFloatAsState(if (chromeVisible) 1f else 0f, label = "viewerChrome")

    Scaffold(
        containerColor = if (darkTheme && currentItem != null) Color.Black else MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                expandedHeight = ViewerPillHeight + 8.dp,
                modifier = Modifier
                    .graphicsLayer { alpha = min(zoom.chromeAlpha(), chromeToggleAlpha) }
                    .chromeGate(chromeVisible) { chromeVisible = true },
                title = {
                    pillMetadata?.let { info ->
                        DateTimePill(
                            dateLabel = info.weekdayDateLabel.substringAfter("\u2022 "),
                            timeLabel = info.timeLabel,
                            backgroundColor = toolbarBackground,
                            border = toolbarBorder,
                            contentColor = chromeColor
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { coroutineScope.launch { animateBackOut() } }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back), tint = chromeColor)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = appString(R.string.common_more), tint = chromeColor)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            shape = GlassPill.shape,
                            containerColor = popupBackground
                        ) {
                            CompositionLocalProvider(LocalContentColor provides chromeColor) {
                                DropdownMenuItem(
                                    text = { Text(appString(R.string.common_unhide)) },
                                    onClick = {
                                        menuExpanded = false
                                        showUnhideConfirm = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(appString(R.string.common_add_to_album)) },
                                    onClick = {
                                        menuExpanded = false
                                        showAddToAlbumDialog = true
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (darkTheme && currentItem != null) Color.Black else MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Column(
                Modifier
                    .graphicsLayer { alpha = min(zoom.chromeAlpha(), chromeToggleAlpha) }
                    .chromeGate(chromeVisible) { chromeVisible = true }
                    .background(if (darkTheme) Color.Black else MaterialTheme.colorScheme.background)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    currentItem?.let { item ->
                        // Unhide lives in the "..." menu. Without Share an invisible 56dp spacer keeps the center pill balanced.
                        if (shareEnabled) {
                            ViewerCircleButton(
                                icon = Icons.Filled.IosShare,
                                contentDescription = appString(R.string.common_share),
                                backgroundColor = toolbarBackground,
                                border = toolbarBorder,
                                tint = chromeColor,
                                onClick = {
                                    preparingJob = coroutineScope.launch {
                                        val shareUri = withContext(Dispatchers.IO) {
                                            val running = coroutineContext[Job]
                                            createExifStrippedShareUriFromFile(context, item.file, item.isVideo, removeExifOnShare, item.mimeType) { running?.isActive == false }
                                        }
                                        preparingJob = null
                                        // Fails closed: if stripping fails, never share the original with its EXIF/GPS
                                        if (shareUri != null) {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = if (item.isVideo) "video/*" else "image/*"
                                                putExtra(Intent.EXTRA_STREAM, shareUri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, null))
                                        }
                                    }
                                }
                            )
                        } else {
                            Spacer(Modifier.size(56.dp))
                        }

                        GlassPillContainer(
                            modifier = Modifier.weight(1f),
                            backgroundColor = toolbarBackground,
                            border = toolbarBorder,
                            verticalContentPadding = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IconButton(onClick = { onToggleFavorite(item) }) {
                                    Icon(
                                        imageVector = if (item.isFavorite) {
                                            Icons.Filled.Favorite
                                        } else {
                                            Icons.Filled.FavoriteBorder
                                        },
                                        contentDescription = appString(R.string.common_favorite),
                                        tint = chromeColor
                                    )
                                }
                                IconButton(onClick = { showInfo = true }) {
                                    Icon(Icons.Filled.Info, contentDescription = appString(R.string.common_info), tint = chromeColor)
                                }
                                IconButton(onClick = { onEdit(item) }) {
                                    Icon(Icons.Filled.Tune, contentDescription = appString(R.string.common_edit), tint = chromeColor)
                                }
                            }
                        }

                        ViewerCircleButton(
                            icon = Icons.Filled.Delete,
                            contentDescription = appString(R.string.common_move_to_recycle_bin_3),
                            backgroundColor = toolbarBackground,
                            border = toolbarBorder,
                            tint = chromeColor,
                            onClick = { showTrashConfirm = true }
                        )
                    }
                }
            }
        }
    ) { padding ->
        val pagerPadding = videoFramePadding(padding, ViewerTopBarHeight)
        val photoInset = photoFrameInset(ViewerTopBarHeight, FilmstripHeight)
        val settledIsVideo = items.value.getOrNull(pagerState.settledPage)?.isVideo == true
        Box(Modifier.fillMaxSize()) {
            ZoomBackdrop(zoom)
            // Measures the settled page's frame for the zoom transition. Empty, so it never touches input.
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
                    .pointerInput(Unit) {
                        var accumulatedDrag = 0f
                        detectVerticalDragGestures(
                            onDragStart = { accumulatedDrag = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                accumulatedDrag += dragAmount
                                if (accumulatedDrag < -100f) showInfo = true
                            }
                        )
                    }
            ) { page ->
                val pageItem = items.value.getOrNull(page)
                if (pageItem != null) {
                    val isCurrentPage = page == pagerState.currentPage
                    // Photos keep their old area inside the video frame; videos use the whole frame.
                    val pageFrame = Modifier.fillMaxSize().padding(if (pageItem.isVideo) PaddingValues() else photoInset)
                    // HeroPhotoFrame owns the transition, ZoomableImage the settled state
                    val pager = zoom.pagerBounds
                    if (isCurrentPage && zoom.initialBounds != null && pager == null) {
                        // Bounds not measured yet: falling through mounted ZoomableImage full size for a frame before the entrance
                    } else if (isCurrentPage && zoom.initialBounds != null && pager != null && zoom.progressState.value < 1f) {
                        val initial = zoom.initialBounds!!
                        HeroPhotoFrame(
                            model = pageItem.file,
                            cacheKey = "${pageItem.thumbnailFile}|${pageItem.thumbnailFile.lastModified()}",
                            initialAspectRatio = pageItem.aspectRatioOrUnknown(),
                            cellRect = { Rect(initial.left - pager.left, initial.top - pager.top, initial.right - pager.left, initial.bottom - pager.top) },
                            viewportRect = { Rect(0f, 0f, pager.width, pager.height) },
                            progress = { zoom.progressState.value },
                            modifier = pageFrame
                        )
                    } else {
                        Box(Modifier.fillMaxSize()) {
                            ZoomableImage(
                                model = pageItem.file,
                                freshnessKey = pageItem.file.lastModified(),
                                isCurrentPage = isCurrentPage,
                                isVideo = pageItem.isVideo,
                                placeholderCacheKey = "${pageItem.thumbnailFile}|${pageItem.thumbnailFile.lastModified()}",
                                onZoomStateChanged = { zoomedIn -> currentPageZoomedIn = zoomedIn },
                                onTap = { chromeVisible = !chromeVisible },
                                modifier = pageFrame,
                                zoomScale = if (isCurrentPage) currentZoomScale else remember { mutableStateOf(1f) },
                                zoomOffset = if (isCurrentPage) currentZoomOffset else remember { mutableStateOf(Offset.Zero) }
                            )
                            if (pageItem.isVideo && isCurrentPage && pagerIdle) {
                                // Mounted only once the pager is fully at rest. The still image above stays in place
                                // the whole time and the video fades in over it, so nothing is swapped mid-swipe.
                                VideoPlayerView(
                                    model = pageItem.file,
                                    modifier = pageFrame,
                                    controlState = videoControls,
                                    onTap = { chromeVisible = !chromeVisible },
                                    playEnabled = pagerIdle,
                                    poster = {
                                        // Only straight after the open animation, which ends on this exact frame.
                                        if (!hasSwiped && page == safeStartIndex && pager != null) {
                                            val fullFrame = Rect(0f, 0f, pager.width, pager.height)
                                            HeroPhotoFrame(
                                                model = pageItem.file,
                                                cacheKey = "${pageItem.thumbnailFile}|${pageItem.thumbnailFile.lastModified()}",
                                                initialAspectRatio = pageItem.aspectRatioOrUnknown(),
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

            // Video controls and filmstrip float over the media. For a photo the pager already ends above
            // them and the background is solid, so the photo layout is unchanged.
            val overlayBase = if (darkTheme) Color.Black else MaterialTheme.colorScheme.background
            val overlayBackground by animateColorAsState(
                targetValue = if (currentItem?.isVideo == true) overlayBase.copy(alpha = 0f) else overlayBase,
                label = "viewerOverlayBackground"
            )
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = padding.calculateBottomPadding())
                    .graphicsLayer { alpha = min(zoom.chromeAlpha(), chromeToggleAlpha) }
                    .chromeGate(chromeVisible) { chromeVisible = true }
                    .background(overlayBackground)
            ) {
                // Video only: control bar in the pill style. AnimatedVisibility so the bottom bar grows smoothly when swiping between photo and video.
                AnimatedVisibility(
                    visible = currentItem?.isVideo == true,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    VideoControlBar(
                        state = videoControls,
                        backgroundColor = toolbarBackground,
                        border = toolbarBorder,
                        contentColor = chromeColor,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                PhotoFilmstrip(
                    thumbnails = remember(items.value) { items.value.map { it.thumbnailFile } },
                    currentIndex = pagerState.currentPage,
                    selectedBorderColor = chromeColor,
                    onSelect = { index ->
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    }
                )
            }

            metadata?.let { info ->
                if (showInfo) {
                    PhotoInfoSheet(metadata = info, onDismiss = { showInfo = false })
                }
            }
        }
    }

    if (showUnhideConfirm && currentItem != null) {
        AlertDialog(
            onDismissRequest = { showUnhideConfirm = false },
            shape = GlassPill.shape,
            containerColor = popupBackground,
            titleContentColor = chromeColor,
            textContentColor = chromeColor,
            title = { Text(appString(R.string.common_unhide)) },
            text = { Text(appString(R.string.hidden_photo_viewer_move_this_photo_back_to)) },
            confirmButton = {
                TextButton(onClick = {
                    showUnhideConfirm = false
                    onUnhide(currentItem)
                    onBack()
                }) {
                    Text(appString(R.string.common_unhide))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnhideConfirm = false }) { Text(appString(R.string.common_cancel)) }
            }
        )
    }

    if (showTrashConfirm && currentItem != null) {
        ConfirmDeleteDialog(
            title = appString(R.string.common_move_to_recycle_bin_2),
            text = appString(R.string.common_this_photo_will_be_deleted),
            confirmLabel = appString(R.string.common_delete_photo),
            containerColor = popupBackground,
            contentColor = chromeColor,
            onConfirm = {
                showTrashConfirm = false
                onTrash(currentItem)
                onBack()
            },
            onDismiss = { showTrashConfirm = false }
        )
    }

    if (showAddToAlbumDialog && currentItem != null) {
        var newAlbumName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddToAlbumDialog = false },
            shape = GlassPill.shape,
            containerColor = popupBackground,
            titleContentColor = chromeColor,
            textContentColor = chromeColor,
            title = { Text(appString(R.string.common_add_to_hidden_album)) },
            text = {
                Column {
                    // With no hidden albums the dialog lets you create one right here
                    GlassPillTextField(
                        value = newAlbumName,
                        onValueChange = { newAlbumName = it },
                        placeholder = appString(R.string.common_new_album_name)
                    )
                    if (existingAlbums.isNotEmpty()) {
                        Text(appString(R.string.common_or_choose_existing), modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                        existingAlbums.forEach { album ->
                            PillListRow(
                                label = album.name,
                                contentColor = chromeColor,
                                onClick = {
                                    onAddToAlbum(album.id, currentItem)
                                    showAddToAlbumDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newAlbumName.isNotBlank(),
                    onClick = {
                        onCreateAlbum(newAlbumName) { newId -> onAddToAlbum(newId, currentItem) }
                        showAddToAlbumDialog = false
                    }
                ) { Text(appString(R.string.common_create_add)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddToAlbumDialog = false }) { Text(appString(R.string.common_cancel)) }
            }
        )
    }
}
