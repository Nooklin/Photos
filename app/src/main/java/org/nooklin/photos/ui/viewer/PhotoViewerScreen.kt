package org.nooklin.photos.ui.viewer

import org.nooklin.photos.ui.common.VideoControlBar
import org.nooklin.photos.ui.common.VideoControlState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.nooklin.photos.ui.albums.AlbumWithPreview
import org.nooklin.photos.ui.common.ConfirmDeleteDialog
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.GlassPillContainer
import org.nooklin.photos.ui.common.GlassPillTextField
import org.nooklin.photos.ui.common.PillListRow
import org.nooklin.photos.ui.common.StableList
import org.nooklin.photos.ui.common.ZoomBackdrop
import org.nooklin.photos.ui.common.ZoomLaunch
import org.nooklin.photos.ui.common.VideoPlayerView
import org.nooklin.photos.ui.common.rememberPhotoZoomTransition
import org.nooklin.photos.ui.common.HeroPhotoFrame
import org.nooklin.photos.ui.common.UNKNOWN_PHOTO_ASPECT_RATIO
import org.nooklin.photos.ui.common.aspectRatioOrUnknown
import org.nooklin.photos.ui.common.zoomPagerBounds
import org.nooklin.photos.ui.common.ZoomableImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.nooklin.photos.ui.common.SharePreparingDialog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.data.MediaStoreRepository
import org.nooklin.photos.data.PhotoMetadata
import org.nooklin.photos.data.createExifStrippedShareUri
import org.nooklin.photos.data.formatDayMonth
import org.nooklin.photos.data.formatTime
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import kotlin.math.min
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import org.nooklin.photos.ui.common.VideoPrewarm
import org.nooklin.photos.ui.common.videoUriOf
import org.nooklin.photos.ui.common.prefetchScreenTierImage
import kotlinx.coroutines.delay
import org.nooklin.photos.R
import org.nooklin.photos.appString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    items: StableList<MediaItem>,
    zoomLaunch: ZoomLaunch?,
    darkTheme: Boolean,
    startIndex: Int,
    existingAlbums: List<AlbumWithPreview>,
    removeExifOnShare: Boolean,
    shareEnabled: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onMoveToRecycleBin: (MediaItem) -> Unit,
    hideEnabled: Boolean = true,
    onHide: (MediaItem) -> Unit,
    onEdit: (MediaItem) -> Unit,
    onAddToAlbum: (albumId: Long, item: MediaItem) -> Unit,
    onCreateAlbumAndAdd: (name: String, item: MediaItem) -> Unit,
    onLibraryChanged: () -> Unit
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
    val zoom = rememberPhotoZoomTransition(zoomLaunch)
    val viewerBackground = if (darkTheme && currentItem != null) Color.Black else MaterialTheme.colorScheme.background
    // Keyed on the page index so a swipe starts at 1x. Declared before animateBackOut, locals can't be forward-referenced.
    val currentZoomScale = remember(pagerState.currentPage) { mutableStateOf(1f) }
    val currentZoomOffset = remember(pagerState.currentPage) { mutableStateOf(Offset.Zero) }
    // Shared by VideoPlayerView and the control bar, declared before both
    val videoControls = remember { VideoControlState() }
    suspend fun animateBackOut() {
        val item = currentItem
        zoom.exit(
            currentKey = item?.id,
            currentModel = item?.uri,
            currentCacheKey = item?.let { "${it.uri}|${it.dateModified}" },
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
    // Chrome flips with the themed background
    val chromeColor = if (darkTheme) Color.White else Color.Black
    val popupBackground = if (darkTheme) GlassPill.fixedPopupBackground else GlassPill.adaptivePopupBackground
    val toolbarBackground = if (darkTheme) GlassPill.fixedGlassBackground else GlassPill.adaptiveGlassBackground
    val toolbarBorder = if (darkTheme) null else GlassPill.adaptiveBorder

    val context = LocalContext.current
    val repository = remember { MediaStoreRepository(context) }
    var showInfo by remember { mutableStateOf(false) }
    var showTrashConfirm by remember { mutableStateOf(false) }
    var currentPageZoomedIn by remember { mutableStateOf(false) }
    // VideoPlayerView never reports a zoom state, a video could inherit a stuck "zoomed in" from the previous photo
    LaunchedEffect(pagerState.currentPage) { currentPageZoomedIn = false }
    // Keyed on the page index, a new photo gets a fresh unzoomed state
    var metadata by remember { mutableStateOf<PhotoMetadata?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var preparingJob by remember { mutableStateOf<Job?>(null) }
    if (preparingJob != null) {
        SharePreparingDialog(onCancel = {
            preparingJob?.cancel()
            preparingJob = null
        })
    }
    var showAddToAlbumDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentItem) { metadata = null }
    LaunchedEffect(showInfo, currentItem) {
        val item = currentItem
        if (showInfo && item != null) {
            metadata = withContext(Dispatchers.IO) { repository.queryMetadata(item) }
        }
    }

    var chromeVisible by remember { mutableStateOf(true) }
    var hasSwiped by remember { mutableStateOf(false) }
    // Prepare neighbouring videos after the open animation
    val transitionDone by remember { derivedStateOf { zoom.progressState.value >= 1f } }
    // Pager must be at rest: settledPage updates when the finger lifts, mounting the player mid-slide showed black
    val pagerIdle by remember {
        derivedStateOf { !pagerState.isScrollInProgress && pagerState.currentPageOffsetFraction == 0f }
    }
    LaunchedEffect(pagerState.settledPage, items.value, transitionDone, pagerIdle) {
        // Player creation costs main-thread time, keep it out of a swipe
        if (!transitionDone || !pagerIdle) return@LaunchedEffect
        delay(250)
        val current = pagerState.settledPage
        val near = listOf(current - 1, current + 1).mapNotNull { index -> items.value.getOrNull(index)?.takeIf { it.isVideo }?.let { videoUriOf(it.uri) } }.toSet()
        val currentVideo = items.value.getOrNull(current)?.takeIf { it.isVideo }?.let { videoUriOf(it.uri) }
        VideoPrewarm.update(context, warm = near, keep = near + listOfNotNull(currentVideo))
        // Decode the neighbours now so the first swipe is a memory-cache hit
        listOf(current + 1, current - 1).forEach { index ->
            items.value.getOrNull(index)?.let { prefetchScreenTierImage(context, it) }
        }
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
                    currentItem?.let { item ->
                        val millis = item.dateAdded * 1000L
                        DateTimePill(
                            dateLabel = formatDayMonth(millis),
                            timeLabel = formatTime(millis),
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
                                    text = { Text(appString(R.string.common_add_to_album)) },
                                    onClick = {
                                        menuExpanded = false
                                        showAddToAlbumDialog = true
                                    }
                                )
                                if (hideEnabled) DropdownMenuItem(
                                    text = { Text(appString(R.string.common_hide)) },
                                    onClick = {
                                        menuExpanded = false
                                        currentItem?.let {
                                            onHide(it)
                                            onBack()
                                        }
                                    }
                                )
                                if (shareEnabled) {
                                    DropdownMenuItem(
                                        text = { Text(appString(R.string.photo_viewer_copy)) },
                                        onClick = {
                                            menuExpanded = false
                                            currentItem?.let { item ->
                                                preparingJob = coroutineScope.launch {
                                                    val copyUri = withContext(Dispatchers.IO) {
                                                        val running = coroutineContext[Job]
                                                        createExifStrippedShareUri(context, item, removeExifOnShare) { running?.isActive == false }
                                                    }
                                                    preparingJob = null
                                                    if (copyUri != null) {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        clipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, appString(R.string.photo_viewer_photo), copyUri))
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(appString(R.string.photo_viewer_duplicate)) },
                                    onClick = {
                                        menuExpanded = false
                                        currentItem?.let { item ->
                                            coroutineScope.launch {
                                                if (repository.duplicate(item)) onLibraryChanged()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (darkTheme && currentItem != null) Color.Black else MaterialTheme.colorScheme.background
                )
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
                        // Share is hidden when sharingDisabled is on, the spacer keeps the center pill balanced
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
                                            createExifStrippedShareUri(context, item, removeExifOnShare) { running?.isActive == false }
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
        Box(
            Modifier.fillMaxSize()
        ) {
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
                    val pageFrame = Modifier.fillMaxSize().padding(if (pageItem.isVideo) PaddingValues() else photoInset)
                    // HeroPhotoFrame owns the transition, ZoomableImage the settled state
                    val pager = zoom.pagerBounds
                    if (isCurrentPage && zoom.initialBounds != null && pager == null) {
                        // Bounds not measured yet: falling through mounted ZoomableImage full size for a frame
                    } else if (isCurrentPage && zoom.initialBounds != null && pager != null && zoom.progressState.value < 1f) {
                        val initial = zoom.initialBounds!!
                        HeroPhotoFrame(
                            model = pageItem.uri,
                            cacheKey = "${pageItem.uri}|${pageItem.dateModified}",
                            initialAspectRatio = pageItem.aspectRatioOrUnknown(),
                            cellRect = { Rect(initial.left - pager.left, initial.top - pager.top, initial.right - pager.left, initial.bottom - pager.top) },
                            viewportRect = { Rect(0f, 0f, pager.width, pager.height) },
                            progress = { zoom.progressState.value },
                            modifier = pageFrame
                        )
                    } else {
                        Box(Modifier.fillMaxSize()) {
                            ZoomableImage(
                                model = pageItem.uri,
                                freshnessKey = pageItem.dateModified,
                                isCurrentPage = isCurrentPage,
                                isVideo = pageItem.isVideo,
                                onZoomStateChanged = { zoomedIn -> currentPageZoomedIn = zoomedIn },
                                onTap = { chromeVisible = !chromeVisible },
                                modifier = pageFrame,
                                // Only the current page shares the hoisted zoom state, a neighbour sharing it would appear pre-zoomed
                                zoomScale = if (isCurrentPage) currentZoomScale else remember { mutableStateOf(1f) },
                                zoomOffset = if (isCurrentPage) currentZoomOffset else remember { mutableStateOf(Offset.Zero) }
                            )
                            if (pageItem.isVideo && isCurrentPage && pagerIdle) {
                                // Video fades in over the still image, nothing is swapped mid-swipe
                                VideoPlayerView(
                                    model = pageItem.uri,
                                    modifier = pageFrame,
                                    controlState = videoControls,
                                    onTap = { chromeVisible = !chromeVisible },
                                    playEnabled = pagerIdle,
                                    poster = {
                                        if (!hasSwiped && page == safeStartIndex && pager != null) {
                                            val fullFrame = Rect(0f, 0f, pager.width, pager.height)
                                            HeroPhotoFrame(
                                                model = pageItem.uri,
                                                cacheKey = "${pageItem.uri}|${pageItem.dateModified}",
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
                // AnimatedVisibility so swiping between photo and video grows the bottom bar smoothly
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
                    thumbnails = remember(items.value) { items.value.map { it.uri } },
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

            if (showAddToAlbumDialog && currentItem != null) {
                AddToAlbumDialog(
                    existingAlbums = existingAlbums,
                    backgroundColor = popupBackground,
                    contentColor = chromeColor,
                    onDismiss = { showAddToAlbumDialog = false },
                    onConfirmExisting = { albumId ->
                        showAddToAlbumDialog = false
                        onAddToAlbum(albumId, currentItem)
                    },
                    onConfirmNew = { newAlbumName ->
                        showAddToAlbumDialog = false
                        onCreateAlbumAndAdd(newAlbumName, currentItem)
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
                        onMoveToRecycleBin(currentItem)
                        onBack()
                    },
                    onDismiss = { showTrashConfirm = false }
                )
            }
        }
    }
}

@Composable
private fun AddToAlbumDialog(
    existingAlbums: List<AlbumWithPreview>,
    backgroundColor: Color,
    contentColor: Color,
    onDismiss: () -> Unit,
    onConfirmExisting: (albumId: Long) -> Unit,
    onConfirmNew: (String) -> Unit
) {
    var newAlbumName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = GlassPill.shape,
        containerColor = backgroundColor,
        titleContentColor = contentColor,
        textContentColor = contentColor,
        title = { Text(appString(R.string.common_add_to_album)) },
        text = {
            Column {
                GlassPillTextField(
                    value = newAlbumName,
                    onValueChange = { newAlbumName = it },
                    placeholder = appString(R.string.common_new_album_name)
                )
                if (existingAlbums.isNotEmpty()) {
                    Text(appString(R.string.common_or_choose_existing), modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                    LazyColumn {
                        items(existingAlbums, key = { it.id }) { album ->
                            PillListRow(
                                label = album.name,
                                contentColor = contentColor,
                                onClick = { onConfirmExisting(album.id) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (newAlbumName.isNotBlank()) onConfirmNew(newAlbumName) },
                enabled = newAlbumName.isNotBlank()
            ) {
                Text(appString(R.string.photo_viewer_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(appString(R.string.common_cancel)) }
        }
    )
}

@Composable
fun ViewerCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    backgroundColor: Color,
    border: androidx.compose.foundation.BorderStroke?,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .padding(4.dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint)
    }
}


