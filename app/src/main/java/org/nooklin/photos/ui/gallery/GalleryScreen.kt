package org.nooklin.photos.ui.gallery

import org.nooklin.photos.ui.common.scrollToItemCentered

import org.nooklin.photos.ui.common.ScrollToPendingZoomKey
import org.nooklin.photos.ui.common.zoomLazyIndexOf
import org.nooklin.photos.ui.common.ZoomSources
import org.nooklin.photos.ui.common.zoomSource

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.VisibilityOff
import org.nooklin.photos.ui.common.MenuIcon
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.nooklin.photos.ui.common.GridThumbnail
import org.nooklin.photos.ui.common.PrefetchAhead
import androidx.compose.ui.graphics.FilterQuality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.data.groupItemsByDate
import org.nooklin.photos.data.local.GridStyle
import org.nooklin.photos.data.local.GroupBy
import org.nooklin.photos.data.local.MAX_GRID_COLUMNS
import org.nooklin.photos.data.local.MIN_GRID_COLUMNS
import org.nooklin.photos.ui.MediaFilter
import org.nooklin.photos.ui.SortOrder
import org.nooklin.photos.ui.albums.AlbumWithPreview
import org.nooklin.photos.ui.common.ConfirmDeleteDialog
import org.nooklin.photos.ui.common.CircleIconButton
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.PillButton
import org.nooklin.photos.ui.common.PillListRow
import org.nooklin.photos.ui.common.dragToSelect
import org.nooklin.photos.ui.common.freshImageRequest
import org.nooklin.photos.ui.common.pinchToChangeGridColumns
import org.nooklin.photos.ui.common.prefetchScreenTierImage
import org.nooklin.photos.R
import org.nooklin.photos.appString

// Library grid with its own top bar. The title count is the visible count (after filter and hide screenshots).
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    items: List<MediaItem>,
    filter: MediaFilter,
    hideScreenshots: Boolean,
    gridStyle: GridStyle,
    gridColumns: Int,
    onGridColumnsChange: (Int) -> Unit,
    groupBy: GroupBy,
    existingAlbums: List<AlbumWithPreview>,
    onFilterChange: (MediaFilter) -> Unit,
    onHideScreenshotsChange: (Boolean) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    // Tapped cell bounds in window coordinates at tap time, the viewer animates its entrance from them
    onItemClick: (Int, Rect, ImageBitmap?) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onMoveToRecycleBin: (MediaItem) -> Unit,
    hideEnabled: Boolean = true,
    onHideSelected: (List<MediaItem>) -> Unit,
    onAddToAlbum: (albumId: Long, item: MediaItem) -> Unit,
    onOpenSearch: () -> Unit,
    // Starting value of the metadata switch in the share sheet (Settings, Sharing)
    removeExifOnShare: Boolean = true,
    onBack: () -> Unit
) {
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var isSelecting by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var actionsMenuExpanded by remember { mutableStateOf(false) }
    var showAddToAlbumDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    // Years and Months are overviews, All is the grid below. The bar shows while scrolling and in the overviews.
    var timelineMode by rememberSaveable { mutableStateOf(TimelineMode.ALL) }
    var focusYear by remember { mutableStateOf<Int?>(null) }
    var pendingJumpId by remember { mutableStateOf<Long?>(null) }
    var recentlyScrolled by remember { mutableStateOf(false) }
    // One state per grid type, LazyGridState and LazyStaggeredGridState are unrelated types
    val gridState = rememberLazyGridState()
    val staggeredGridState = rememberLazyStaggeredGridState()
    // One-shot backdrop capture: record() is free, the readback is the expensive step
    val backdropGraphicsLayer = rememberGraphicsLayer()
    val coroutineScope = rememberCoroutineScope()
    // After backing out on a photo that wasn't on screen, scroll to it so the return zoom lands
    ScrollToPendingZoomKey(
        lazyIndexOf = { key ->
            zoomLazyIndexOf(
                key,
                items.map { it.id },
                if (gridStyle == GridStyle.SQUARE && groupBy != GroupBy.NONE) {
                    groupItemsByDate(items, groupBy).map { (_, group) -> group.map { it.id } }
                } else null
            )
        },
        scrollToIndex = { i -> if (gridStyle == GridStyle.SQUARE) gridState.scrollToItemCentered(i) else staggeredGridState.scrollToItemCentered(i) }
    )

    fun exitSelection() {
        isSelecting = false
        selectedIds = emptySet()
    }

    fun toggleSelect(item: MediaItem) {
        selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id
    }

    // Selecting only works in the grid
    LaunchedEffect(isSelecting) {
        if (isSelecting) timelineMode = TimelineMode.ALL
    }
    val scrolling = gridState.isScrollInProgress || staggeredGridState.isScrollInProgress
    LaunchedEffect(scrolling) {
        if (scrolling) {
            recentlyScrolled = true
        } else {
            delay(2000)
            recentlyScrolled = false
        }
    }
    val timelineBarVisible = !isSelecting && (timelineMode != TimelineMode.ALL || recentlyScrolled)
    val timelineBarAlpha by animateFloatAsState(if (timelineBarVisible) 1f else 0f, label = "timelineBar")
    // A tap on a month in the overview opens the grid at that month
    LaunchedEffect(timelineMode, pendingJumpId) {
        val id = pendingJumpId
        if (timelineMode == TimelineMode.ALL && id != null) {
            val grouped = gridStyle == GridStyle.SQUARE && groupBy != GroupBy.NONE
            val index = zoomLazyIndexOf(
                id,
                items.map { it.id },
                if (grouped) groupItemsByDate(items, groupBy).map { (_, group) -> group.map { it.id } } else null
            )
            if (index != null) {
                // One back from the photo so the date header stays in view
                val target = (if (grouped) index - 1 else index).coerceAtLeast(0)
                if (gridStyle == GridStyle.SQUARE) gridState.scrollToItem(target) else staggeredGridState.scrollToItem(target)
            }
            pendingJumpId = null
        }
    }

    Scaffold(
        modifier = Modifier.drawWithContent {
            backdropGraphicsLayer.record { this@drawWithContent.drawContent() }
            drawLayer(backdropGraphicsLayer)
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(appString(R.string.common_library), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Text(appString(R.string.common_items, items.size), color = Color.Gray, fontSize = 13.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                },
                actions = {
                    Box {
                        CircleIconButton(
                            icon = Icons.Filled.FilterList,
                            contentDescription = appString(R.string.common_filter),
                            onClick = { filterMenuExpanded = true }
                        )
                        DropdownMenu(
                            expanded = filterMenuExpanded,
                            onDismissRequest = { filterMenuExpanded = false },
                            shape = GlassPill.shape,
                            containerColor = GlassPill.adaptivePopupBackground
                        ) {
                            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                                DropdownMenuItem(
                                    text = { Text(appString(R.string.common_all)) },
                                    trailingIcon = {
                                        if (filter == MediaFilter.ALL) {
                                            Icon(Icons.Filled.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = { onFilterChange(MediaFilter.ALL) }
                                )
                                DropdownMenuItem(
                                    text = { Text(appString(R.string.common_favorites)) },
                                    trailingIcon = {
                                        if (filter == MediaFilter.FAVORITES) {
                                            Icon(Icons.Filled.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = { onFilterChange(MediaFilter.FAVORITES) }
                                )
                                DropdownMenuItem(
                                    text = { Text(appString(R.string.common_videos)) },
                                    trailingIcon = {
                                        if (filter == MediaFilter.VIDEOS) {
                                            Icon(Icons.Filled.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = { onFilterChange(MediaFilter.VIDEOS) }
                                )
                                DropdownMenuItem(
                                    text = { Text(appString(R.string.common_screenshots)) },
                                    trailingIcon = {
                                        if (!hideScreenshots) {
                                            Icon(Icons.Filled.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = { onHideScreenshotsChange(!hideScreenshots) }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(appString(R.string.common_newest_first)) },
                                    trailingIcon = {
                                        if (sortOrder == SortOrder.NEWEST_FIRST) {
                                            Icon(Icons.Filled.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = { onSortOrderChange(SortOrder.NEWEST_FIRST) }
                                )
                                DropdownMenuItem(
                                    text = { Text(appString(R.string.common_oldest_first)) },
                                    trailingIcon = {
                                        if (sortOrder == SortOrder.OLDEST_FIRST) {
                                            Icon(Icons.Filled.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = { onSortOrderChange(SortOrder.OLDEST_FIRST) }
                                )
                            }
                        }
                    }

                    if (isSelecting) {
                        Box {
                            CircleIconButton(
                                icon = Icons.Filled.MoreVert,
                                contentDescription = appString(R.string.common_more),
                                onClick = { actionsMenuExpanded = true }
                            )
                            DropdownMenu(
                                expanded = actionsMenuExpanded,
                                onDismissRequest = { actionsMenuExpanded = false },
                                shape = GlassPill.shape,
                                containerColor = GlassPill.adaptivePopupBackground
                            ) {
                                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                                    DropdownMenuItem(
                                        text = { Text(appString(R.string.common_share)) },
                                        leadingIcon = { MenuIcon(Icons.Filled.IosShare) },
                                        enabled = selectedIds.isNotEmpty(),
                                        onClick = {
                                            actionsMenuExpanded = false
                                            showShareSheet = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(appString(R.string.common_favorite)) },
                                        leadingIcon = { MenuIcon(Icons.Filled.FavoriteBorder) },
                                        onClick = {
                                            actionsMenuExpanded = false
                                            items.filter { it.id in selectedIds }.forEach { onToggleFavorite(it) }
                                            exitSelection()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(appString(R.string.common_add_to_album)) },
                                        leadingIcon = { MenuIcon(Icons.Filled.PhotoAlbum) },
                                        onClick = {
                                            actionsMenuExpanded = false
                                            showAddToAlbumDialog = true
                                        }
                                    )
                                    if (hideEnabled) DropdownMenuItem(
                                        text = { Text(appString(R.string.common_hide)) },
                                        leadingIcon = { MenuIcon(Icons.Filled.VisibilityOff) },
                                        onClick = {
                                            actionsMenuExpanded = false
                                            onHideSelected(items.filter { it.id in selectedIds })
                                            exitSelection()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(appString(R.string.common_move_to_recycle_bin), color = GlassPill.destructiveText) },
                                        leadingIcon = { MenuIcon(Icons.Filled.Delete) },
                                        onClick = {
                                            actionsMenuExpanded = false
                                            showDeleteConfirm = true
                                        }
                                    )
                                }
                            }
                        }
                        CircleIconButton(
                            icon = Icons.Filled.Close,
                            contentDescription = appString(R.string.common_cancel_selection),
                            onClick = { exitSelection() }
                        )
                    } else {
                        PillButton(text = appString(R.string.common_select), onClick = { isSelecting = true })
                    }
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            // The row has a fixed height, so the search button does not move when the bar disappears.
            // The bar sits in the middle of the screen, the search button at the right edge.
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(56.dp)) {
                // Not composed while invisible: a hidden bar would still catch taps meant for the photos below it
                if (isSelecting) {
                    // Same look as the bar, so it is easy to read over photos
                    Surface(
                        color = GlassPill.adaptivePopupBackground,
                        shape = GlassPill.shape,
                        border = GlassPill.adaptiveBorder,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        // Same height as the timeline bar and the search button
                        Box(Modifier.height(56.dp).padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                appString(R.string.share_sheet_selected, selectedIds.size),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else if (timelineBarAlpha > 0f) {
                    TimelineBar(
                        selected = timelineMode,
                        onSelect = { timelineMode = it },
                        modifier = Modifier.align(Alignment.Center).alpha(timelineBarAlpha)
                    )
                }
                Box(Modifier.align(Alignment.CenterEnd)) {
                    // 56dp like the timeline bar, no outer margin so the row stays 56dp
                    CircleIconButton(
                        icon = Icons.Filled.Search,
                        contentDescription = appString(R.string.gallery_search),
                        onClick = onOpenSearch,
                        size = 56.dp,
                        outerPadding = 0.dp
                    )
                }
            }
        }
    ) { padding ->
        // Shared by both square grid branches, only the staggered one needs its own
        val hitTestSquareGrid: (Offset) -> Any? = { pos ->
            gridState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                pos.x >= info.offset.x && pos.x <= info.offset.x + info.size.width &&
                    pos.y >= info.offset.y && pos.y <= info.offset.y + info.size.height
            }?.key
        }
        if (gridStyle == GridStyle.SQUARE) PrefetchAhead(gridState, items, gridColumns)
        if (timelineMode != TimelineMode.ALL) {
            TimelineOverview(
                items = items,
                mode = timelineMode,
                focusYear = focusYear,
                onOpenYear = { year ->
                    focusYear = year
                    timelineMode = TimelineMode.MONTHS
                },
                onOpenMonth = { firstItemId ->
                    pendingJumpId = firstItemId
                    timelineMode = TimelineMode.ALL
                },
                modifier = Modifier.padding(padding)
            )
        } else Column(Modifier.fillMaxSize().padding(padding)) {
            if (gridStyle == GridStyle.SQUARE) {
                if (groupBy == GroupBy.NONE) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(gridColumns),
                        contentPadding = PaddingValues(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .dragToSelect(
                                enabled = isSelecting,
                                hitTest = hitTestSquareGrid,
                                isSelected = { key -> key is Long && key in selectedIds },
                                onItemDragged = { key, select ->
                                    if (key is Long) {
                                        selectedIds = if (select) selectedIds + key else selectedIds - key
                                    }
                                }
                            )
                            .pinchToChangeGridColumns(
                                enabled = !isSelecting,
                                columns = gridColumns,
                                minColumns = MIN_GRID_COLUMNS,
                                maxColumns = MAX_GRID_COLUMNS,
                                onColumnsChange = onGridColumnsChange
                            )
                    ) {
                        items(items.size, key = { items[it].id }, contentType = { "photo" }) { index ->
                            GridCell(
                                items[index], index,
                                onItemClick, onItemLongClick,
                                isSelecting, items[index].id in selectedIds,
                                onToggleSelect = { toggleSelect(items[index]) },
                                backdropGraphicsLayer = backdropGraphicsLayer,
                                coroutineScope = coroutineScope
                            )
                        }
                    }
                } else {
                    // remember() has to live here, LazyGridScope is not composable
                    val grouped = remember(items, groupBy) { groupItemsByDate(items, groupBy) }
                    val indexById = remember(items) { items.withIndex().associate { (i, item) -> item.id to i } }
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(gridColumns),
                        contentPadding = PaddingValues(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .dragToSelect(
                                enabled = isSelecting,
                                hitTest = hitTestSquareGrid,
                                isSelected = { key -> key is Long && key in selectedIds },
                                onItemDragged = { key, select ->
                                    if (key is Long) {
                                        selectedIds = if (select) selectedIds + key else selectedIds - key
                                    }
                                }
                            )
                            .pinchToChangeGridColumns(
                                enabled = !isSelecting,
                                columns = gridColumns,
                                minColumns = MIN_GRID_COLUMNS,
                                maxColumns = MAX_GRID_COLUMNS,
                                onColumnsChange = onGridColumnsChange
                            )
                    ) {
                        grouped.forEach { (label, groupItems) ->
                            item(span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                                Text(
                                    label,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp)
                                )
                            }
                            items(groupItems.size, key = { groupItems[it].id }, contentType = { "photo" }) { idx ->
                                val item = groupItems[idx]
                                GridCell(
                                    item, indexById.getValue(item.id),
                                    onItemClick, onItemLongClick,
                                    isSelecting, item.id in selectedIds,
                                    onToggleSelect = { toggleSelect(item) },
                                    backdropGraphicsLayer = backdropGraphicsLayer,
                                    coroutineScope = coroutineScope
                                )
                            }
                        }
                    }
                }
            } else {
                // O(1) lookup instead of indexOf on every tap
                val indexById = remember(items) { items.withIndex().associate { (i, item) -> item.id to i } }
                val hitTestStaggeredGrid: (Offset) -> Any? = { pos ->
                    staggeredGridState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                        pos.x >= info.offset.x && pos.x <= info.offset.x + info.size.width &&
                            pos.y >= info.offset.y && pos.y <= info.offset.y + info.size.height
                    }?.key
                }
                LazyVerticalStaggeredGrid(
                    state = staggeredGridState,
                    columns = StaggeredGridCells.Fixed(gridColumns),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalItemSpacing = 4.dp,
                    modifier = Modifier
                        .dragToSelect(
                            enabled = isSelecting,
                            hitTest = hitTestStaggeredGrid,
                            isSelected = { key -> key is Long && key in selectedIds },
                            onItemDragged = { key, select ->
                                if (key is Long) {
                                    selectedIds = if (select) selectedIds + key else selectedIds - key
                                }
                            }
                        )
                        .pinchToChangeGridColumns(
                            enabled = !isSelecting,
                            columns = gridColumns,
                            minColumns = MIN_GRID_COLUMNS,
                            maxColumns = MAX_GRID_COLUMNS,
                            onColumnsChange = onGridColumnsChange
                        )
                ) {
                    staggeredItems(items, key = { it.id }) { item ->
                        val ratio = if (item.width != null && item.height != null && item.height > 0) {
                            item.width.toFloat() / item.height.toFloat()
                        } else 1f
                        val selected = item.id in selectedIds
                        val context = LocalContext.current
                        Box {
                            GridThumbnail(
                                uri = item.uri,
                                dateModified = item.dateModified,
                                modifier = Modifier
                                    .aspectRatio(ratio)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .zoomSource(item.id)
                                    .combinedClickable(
                                        onClick = {
                                            if (isSelecting) {
                                                toggleSelect(item)
                                            } else {
                                                prefetchScreenTierImage(context, item)
                                                coroutineScope.launch {
                                                    val backdrop = runCatching { backdropGraphicsLayer.toImageBitmap() }.getOrNull()
                                                    onItemClick(indexById.getValue(item.id), (ZoomSources.boundsOf(item.id) ?: Rect.Zero), backdrop)
                                                }
                                            }
                                        },
                                        onLongClick = { onItemLongClick(item) }
                                    )
                            )
                            if (item.isFavorite) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                                )
                            }
                            if (isSelecting && selected) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = appString(R.string.common_selected),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showShareSheet) {
        ShareSelectionSheet(
            selectedItems = items.filter { it.id in selectedIds },
            removeMetadataDefault = removeExifOnShare,
            onDismiss = { showShareSheet = false },
            onFinished = {
                showShareSheet = false
                exitSelection()
            }
        )
    }

    if (showAddToAlbumDialog) {
        AlertDialog(
            onDismissRequest = { showAddToAlbumDialog = false },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(appString(R.string.common_add_to_album)) },
            text = {
                if (existingAlbums.isEmpty()) {
                    Text(appString(R.string.common_no_albums_yet_create_one))
                } else {
                    Column {
                        existingAlbums.forEach { album ->
                            PillListRow(
                                label = album.name,
                                onClick = {
                                    items.filter { it.id in selectedIds }.forEach { onAddToAlbum(album.id, it) }
                                    showAddToAlbumDialog = false
                                    exitSelection()
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddToAlbumDialog = false }) { Text(appString(R.string.common_cancel)) }
            }
        )
    }

    if (showDeleteConfirm) {
        val count = selectedIds.size
        ConfirmDeleteDialog(
            title = appString(R.string.common_move_to_recycle_bin_2),
            text = if (count == 1) {
                appString(R.string.common_this_photo_will_be_deleted)
            } else {
                appString(R.string.common_these_photos_will_be_deleted)
            },
            confirmLabel = if (count == 1) appString(R.string.common_delete_photo) else appString(R.string.common_delete_photos, count),
            onConfirm = {
                showDeleteConfirm = false
                items.filter { it.id in selectedIds }.forEach { onMoveToRecycleBin(it) }
                exitSelection()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun GridCell(
    item: MediaItem,
    index: Int,
    onItemClick: (Int, Rect, ImageBitmap?) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    isSelecting: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    backdropGraphicsLayer: GraphicsLayer,
    coroutineScope: CoroutineScope
) {
    Box {
        val context = LocalContext.current
        // Captured continuously so the bounds at the moment of the tap go to the viewer
        GridThumbnail(
            uri = item.uri,
            dateModified = item.dateModified,
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .zoomSource(item.id)
                .combinedClickable(
                    onClick = {
                        if (isSelecting) {
                            onToggleSelect()
                        } else {
                            prefetchScreenTierImage(context, item)
                            coroutineScope.launch {
                                val backdrop = runCatching { backdropGraphicsLayer.toImageBitmap() }.getOrNull()
                                onItemClick(index, (ZoomSources.boundsOf(item.id) ?: Rect.Zero), backdrop)
                            }
                        }
                    },
                    onLongClick = { onItemLongClick(item) }
                )
        )
        if (item.isFavorite) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
            )
        }
        if (isSelecting && isSelected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = appString(R.string.common_selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
            )
        }
    }
}
