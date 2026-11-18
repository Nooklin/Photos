package org.nooklin.photos.ui.hidden

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.delay
import org.nooklin.photos.ui.gallery.TimelineBar
import org.nooklin.photos.ui.gallery.TimelineMode
import org.nooklin.photos.ui.common.AddToAlbumDialog
import org.nooklin.photos.ui.common.asHiddenTargets
import org.nooklin.photos.ui.common.SelectionCountBar
import androidx.compose.material3.FabPosition
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.foundation.gestures.scrollBy
import org.nooklin.photos.ui.common.MenuSectionHeader
import androidx.activity.compose.BackHandler
import org.nooklin.photos.ui.common.aspectRatioOrUnknown
import org.nooklin.photos.ui.common.scrollToItemCentered

import org.nooklin.photos.ui.common.ScrollToPendingZoomKey
import org.nooklin.photos.ui.common.zoomLazyIndexOf
import org.nooklin.photos.ui.common.zoomSource
import org.nooklin.photos.ui.common.zoomBackdropSource

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Visibility
import org.nooklin.photos.ui.common.MenuIcon
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.nooklin.photos.data.HiddenAlbumWithPreview
import org.nooklin.photos.data.HiddenItem
import org.nooklin.photos.data.groupHiddenItemsByDate
import org.nooklin.photos.data.local.GridStyle
import org.nooklin.photos.data.local.GroupBy
import org.nooklin.photos.data.local.MAX_GRID_COLUMNS
import org.nooklin.photos.data.local.MIN_GRID_COLUMNS
import org.nooklin.photos.ui.MediaFilter
import org.nooklin.photos.ui.SortOrder
import org.nooklin.photos.ui.common.ConfirmDeleteDialog
import org.nooklin.photos.ui.common.CircleIconButton
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.PillButton
import org.nooklin.photos.ui.common.GlassPillTextField
import org.nooklin.photos.ui.common.PillListRow
import org.nooklin.photos.ui.common.dragToSelect
import org.nooklin.photos.ui.common.orderedKeys
import org.nooklin.photos.ui.common.freshImageRequest
import org.nooklin.photos.ui.common.pinchToChangeGridColumns
import org.nooklin.photos.R
import org.nooklin.photos.ui.common.deleteWording
import org.nooklin.photos.ui.common.SelectionBadge
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.TopAppBarDefaults
import org.nooklin.photos.ui.albums.AlbumHero
import org.nooklin.photos.ui.albums.CoverPickerDialog
import org.nooklin.photos.ui.albums.SlideshowDialog
import org.nooklin.photos.ui.albums.SlideshowPhoto
import org.nooklin.photos.ui.common.heroImageRequest
import org.nooklin.photos.appString
import org.nooklin.photos.ui.common.EmptyState

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HiddenGalleryScreen(
    items: List<HiddenItem>,
    gridColumns: Int,
    onGridColumnsChange: (Int) -> Unit = {},
    existingAlbums: List<HiddenAlbumWithPreview>,
    onItemClick: (Int) -> Unit,
    onToggleFavorite: (HiddenItem) -> Unit,
    onUnhide: (HiddenItem) -> Unit,
    onMoveToRecycleBin: (HiddenItem) -> Unit,
    onAddToAlbum: (albumId: Long, item: HiddenItem) -> Unit,
    onCreateAlbum: (name: String, onCreated: (Long) -> Unit) -> Unit,
    onBack: () -> Unit,
    title: String = appString(R.string.common_hidden_library),
    // Favorites: a big photo at the top like an album (only with the square grid without groups)
    showHero: Boolean = false,
    heroCoverName: String? = null,
    onSetHeroCover: (String?) -> Unit = {},
    showFilterMenu: Boolean = false,
    filter: MediaFilter = MediaFilter.ALL,
    onFilterChange: (MediaFilter) -> Unit = {},
    hideScreenshots: Boolean = true,
    onHideScreenshotsChange: (Boolean) -> Unit = {},
    sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    onSortOrderChange: (SortOrder) -> Unit = {},
    gridStyle: GridStyle = GridStyle.SQUARE,
    groupBy: GroupBy = GroupBy.NONE,
    shareEnabled: Boolean = false,
    removeExifOnShare: Boolean = true,
    onOpenSearch: (() -> Unit)? = null,
    onDuplicate: ((HiddenItem) -> Unit)? = null
) {
    val gridStyle = if (gridColumns == 1) GridStyle.ORIGINAL else gridStyle
    val groupBy = if (gridColumns == 1) GroupBy.NONE else groupBy
    var isSelecting by remember { mutableStateOf(false) }
    var selectedNames by remember { mutableStateOf(setOf<String>()) }
    var menuItem by remember { mutableStateOf<HiddenItem?>(null) }
    var actionsMenuExpanded by remember { mutableStateOf(false) }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddToAlbumDialog by remember { mutableStateOf(false) }
    val hasHero = showHero && items.isNotEmpty() && gridStyle == GridStyle.SQUARE && groupBy == GroupBy.NONE
    val heroItem = if (hasHero) (items.firstOrNull { it.file.name == heroCoverName } ?: items.first()) else null
    val headerCount = if (hasHero) 1 else 0
    var showSlideshow by remember { mutableStateOf(false) }
    var showCoverPicker by remember { mutableStateOf(false) }
    var heroMenuExpanded by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val staggeredGridState = rememberLazyStaggeredGridState()
    var timelineMode by rememberSaveable { mutableStateOf(TimelineMode.ALL) }
    var focusYear by remember { mutableStateOf<Int?>(null) }
    var pendingJump by remember { mutableStateOf<HiddenItem?>(null) }
    var recentlyScrolled by remember { mutableStateOf(false) }
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
    LaunchedEffect(timelineMode, pendingJump) {
        val target = pendingJump
        if (timelineMode == TimelineMode.ALL && target != null) {
            val grouped = gridStyle == GridStyle.SQUARE && groupBy != GroupBy.NONE
            val index = zoomLazyIndexOf(
                target.file.absolutePath,
                items.map { it.file.absolutePath },
                if (grouped) groupHiddenItemsByDate(items, groupBy).map { (_, group) -> group.map { it.file.absolutePath } } else null
            )
            if (index != null) {
                // one back from the photo so the date header stays in view
                val scrollTo = (if (grouped) index - 1 else index).coerceAtLeast(0)
                if (gridStyle == GridStyle.SQUARE) gridState.scrollToItem(scrollTo) else staggeredGridState.scrollToItem(scrollTo)
            }
            pendingJump = null
        }
    }
    ScrollToPendingZoomKey(
        lazyIndexOf = { key ->
            zoomLazyIndexOf(
                key,
                items.map { it.file.absolutePath },
                if (gridStyle == GridStyle.SQUARE && groupBy != GroupBy.NONE) {
                    groupHiddenItemsByDate(items, groupBy).map { (_, group) -> group.map { it.file.absolutePath } }
                } else null
            )
        },
        scrollToIndex = { i -> if (gridStyle == GridStyle.SQUARE) gridState.scrollToItemCentered(i + headerCount) else staggeredGridState.scrollToItemCentered(i) }
    )

    fun exitSelection() {
        isSelecting = false
        selectedNames = emptySet()
    }

    fun toggleSelect(item: HiddenItem) {
        val name = item.file.name
        selectedNames = if (name in selectedNames) selectedNames - name else selectedNames + name
    }

    BackHandler(enabled = isSelecting) { exitSelection() }

    val shareHidden = rememberHiddenShare(removeExifOnShare)

    menuItem?.let { item ->
        HiddenItemMenu(
            item = item,
            onOpen = { onItemClick(items.indexOf(item)) },
            onSelect = { isSelecting = true; toggleSelect(item) },
            onFavorite = { onToggleFavorite(item) },
            onUnhide = { onUnhide(item) },
            onDelete = { isSelecting = true; selectedNames = setOf(item.file.name); showDeleteConfirm = true },
            onAddToAlbum = { isSelecting = true; selectedNames = setOf(item.file.name); showAddToAlbumDialog = true },
            onShare = if (shareEnabled) ({ shareHidden(listOf(item)) {} }) else null,
            onDuplicate = onDuplicate?.let { duplicate -> { duplicate(item) } },
            onDismiss = { menuItem = null }
        )
    }

    Scaffold(
        modifier = Modifier.zoomBackdropSource(),
        topBar = {
            TopAppBar(
                colors = if (hasHero) {
                    TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent)
                } else {
                    TopAppBarDefaults.topAppBarColors()
                },
                title = { if (!hasHero) Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (hasHero) {
                        CircleIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = appString(R.string.common_back),
                            onClick = { if (isSelecting) exitSelection() else onBack() }
                        )
                    } else {
                        IconButton(onClick = { if (isSelecting) exitSelection() else onBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                        }
                    }
                },
                actions = {
                    if (hasHero && !isSelecting) {
                        Box {
                            CircleIconButton(
                                icon = Icons.Filled.MoreVert,
                                contentDescription = appString(R.string.common_more_options),
                                onClick = { heroMenuExpanded = true }
                            )
                            DropdownMenu(
                                expanded = heroMenuExpanded,
                                onDismissRequest = { heroMenuExpanded = false },
                                shape = GlassPill.shape,
                                containerColor = GlassPill.adaptivePopupBackground
                            ) {
                                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                                    DropdownMenuItem(
                                        text = { Text(appString(R.string.album_set_cover)) },
                                        leadingIcon = { MenuIcon(Icons.Filled.Image) },
                                        onClick = {
                                            heroMenuExpanded = false
                                            showCoverPicker = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (showFilterMenu && !isSelecting) {
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
                                    MenuSectionHeader(appString(R.string.filter_menu_show))
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
                                    MenuSectionHeader(appString(R.string.filter_menu_sort))
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
                                    if (shareEnabled) DropdownMenuItem(
                                        text = { Text(appString(R.string.common_share)) },
                                        leadingIcon = { MenuIcon(Icons.Filled.IosShare) },
                                        enabled = selectedNames.isNotEmpty(),
                                        onClick = {
                                            actionsMenuExpanded = false
                                            shareHidden(items.filter { it.file.name in selectedNames }) { exitSelection() }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(appString(R.string.common_favorite)) },
                                        leadingIcon = { MenuIcon(Icons.Filled.FavoriteBorder) },
                                        onClick = {
                                            actionsMenuExpanded = false
                                            items.filter { it.file.name in selectedNames }.forEach { onToggleFavorite(it) }
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
                                    DropdownMenuItem(
                                        text = { Text(appString(R.string.common_unhide)) },
                                        leadingIcon = { MenuIcon(Icons.Filled.Visibility) },
                                        onClick = {
                                            actionsMenuExpanded = false
                                            items.filter { it.file.name in selectedNames }.forEach { onUnhide(it) }
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
                        PillButton(text = appString(R.string.common_cancel), color = GlassPill.destructiveText, onClick = { exitSelection() })
                    }
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            if (isSelecting && !hasHero) {
                SelectionCountBar(selectedNames.size)
            } else if (!isSelecting && onOpenSearch != null) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(56.dp)) {
                    if (timelineBarAlpha > 0f) {
                        TimelineBar(
                            selected = timelineMode,
                            onSelect = { timelineMode = it },
                            modifier = Modifier.align(Alignment.Center).alpha(timelineBarAlpha)
                        )
                    }
                    Box(Modifier.align(Alignment.CenterEnd)) {
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
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(message = appString(R.string.hidden_gallery_no_hidden_items), actionLabel = appString(R.string.common_back), onAction = onBack)
            }
            return@Scaffold
        }
        if (timelineMode != TimelineMode.ALL) {
            HiddenTimelineOverview(
                items = items,
                mode = timelineMode,
                focusYear = focusYear,
                onOpenYear = { year ->
                    focusYear = year
                    timelineMode = TimelineMode.MONTHS
                },
                onOpenMonth = { first ->
                    pendingJump = first
                    timelineMode = TimelineMode.ALL
                },
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }
        val itemNames = remember(items) { items.mapTo(HashSet()) { it.file.name } }
        val hitTestSquareGrid: (Offset) -> Any? = { pos ->
            gridState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                pos.x >= info.offset.x && pos.x <= info.offset.x + info.size.width &&
                    pos.y >= info.offset.y && pos.y <= info.offset.y + info.size.height
            }?.key?.takeIf { it != "hero" }
        }
        Column(Modifier.fillMaxSize().then(if (hasHero) Modifier.padding(bottom = padding.calculateBottomPadding()) else Modifier.padding(padding))) {
            if (gridStyle == GridStyle.SQUARE) {
                if (groupBy == GroupBy.NONE) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(gridColumns),
                        contentPadding = if (hasHero) PaddingValues(bottom = 2.dp) else PaddingValues(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (hasHero) 2.dp else 4.dp),
                        verticalArrangement = Arrangement.spacedBy(if (hasHero) 2.dp else 4.dp),
                        modifier = Modifier
                            .dragToSelect(
                                enabled = isSelecting,
                                hitTest = hitTestSquareGrid,
                                scrollBy = { gridState.scrollBy(it) },
                                orderedKeys = {
                                    orderedKeys(gridState.layoutInfo.visibleItemsInfo.map { it.index to it.key }) { it is String && it in itemNames }
                                },
                                isSelected = { key -> key is String && key in selectedNames },
                                onItemDragged = { key, select ->
                                    if (key is String) {
                                        selectedNames = if (select) selectedNames + key else selectedNames - key
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
                        if (heroItem != null) {
                            item(key = "hero", span = { GridItemSpan(maxLineSpan) }, contentType = "hero") {
                                val heroContext = LocalContext.current
                                AlbumHero(
                                    title = if (isSelecting) appString(R.string.share_sheet_selected, selectedNames.size) else title,
                                    itemCount = items.size,
                                    showPlay = true,
                                    onPlay = { showSlideshow = true },
                                    image = {
                                        val heroFile = if (heroItem.isVideo) heroItem.thumbnailFile else heroItem.file
                                        AsyncImage(
                                            model = heroImageRequest(heroContext, heroFile, heroFile.lastModified()),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                )
                            }
                        }
                        items(items.size, key = { items[it].file.name }, contentType = { "photo" }) { index ->
                            val item = items[index]
                            HiddenGridCell(
                                item, index, isSelecting, item.file.name in selectedNames,
                                onItemClick = onItemClick,
                                onToggleSelect = { toggleSelect(item) },
                                onLongPressStartSelect = { menuItem = item }
                            )
                        }
                    }
                } else {
                    val grouped = remember(items, groupBy) { groupHiddenItemsByDate(items, groupBy) }
                    val indexByName = remember(items) { items.withIndex().associate { (i, item) -> item.file.name to i } }
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
                                scrollBy = { gridState.scrollBy(it) },
                                orderedKeys = {
                                    orderedKeys(gridState.layoutInfo.visibleItemsInfo.map { it.index to it.key }) { it is String && it in itemNames }
                                },
                                isSelected = { key -> key is String && key in selectedNames },
                                onItemDragged = { key, select ->
                                    if (key is String) {
                                        selectedNames = if (select) selectedNames + key else selectedNames - key
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
                            items(groupItems.size, key = { groupItems[it].file.name }, contentType = { "photo" }) { idx ->
                                val item = groupItems[idx]
                                HiddenGridCell(
                                    item, indexByName.getValue(item.file.name), isSelecting, item.file.name in selectedNames,
                                    onItemClick = onItemClick,
                                    onToggleSelect = { toggleSelect(item) },
                                    onLongPressStartSelect = { menuItem = item }
                                )
                            }
                        }
                    }
                }
            } else {
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
                            scrollBy = { staggeredGridState.scrollBy(it) },
                            orderedKeys = {
                                orderedKeys(staggeredGridState.layoutInfo.visibleItemsInfo.map { it.index to it.key }) { it is String && it in itemNames }
                            },
                            isSelected = { key -> key is String && key in selectedNames },
                            onItemDragged = { key, select ->
                                if (key is String) {
                                    selectedNames = if (select) selectedNames + key else selectedNames - key
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
                    staggeredItems(items, key = { it.file.name }) { item ->
                        val selected = item.file.name in selectedNames
                        val context = LocalContext.current
                        Box {
                            AsyncImage(
                                model = freshImageRequest(context, item.thumbnailFile, item.thumbnailFile.lastModified()),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .aspectRatio(item.aspectRatioOrUnknown())
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .zoomSource(item.file.absolutePath)
                                    .combinedClickable(
                                        onClick = { if (isSelecting) toggleSelect(item) else onItemClick(items.indexOf(item)) },
                                        onLongClick = { if (!isSelecting) menuItem = item }
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
                                SelectionBadge(Modifier.align(Alignment.TopEnd).padding(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSlideshow) {
        SlideshowDialog(
            photos = items.filter { !it.isVideo }.map {
                SlideshowPhoto(
                    model = it.file,
                    thumbnail = it.thumbnailFile,
                    freshness = it.thumbnailFile.lastModified(),
                    aspect = if (it.width != null && it.height != null && it.height > 0) it.width.toFloat() / it.height.toFloat() else null
                )
            },
            title = title,
            secure = true,
            onClose = { showSlideshow = false }
        )
    }

    if (showCoverPicker) {
        CoverPickerDialog(
            count = items.size,
            key = { items[it].file.name },
            thumbnail = { index ->
                AsyncImage(
                    model = freshImageRequest(LocalContext.current, items[index].thumbnailFile, items[index].thumbnailFile.lastModified()),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            },
            onPick = { index ->
                onSetHeroCover(items[index].file.name)
                showCoverPicker = false
            },
            onUseNewest = {
                onSetHeroCover(null)
                showCoverPicker = false
            },
            onDismiss = { showCoverPicker = false }
        )
    }

    if (showDeleteConfirm) {
        val wording = deleteWording(items.filter { it.file.name in selectedNames }.map { it.isVideo })
        ConfirmDeleteDialog(
            title = appString(R.string.common_move_to_recycle_bin_2),
            text = wording.text,
            confirmLabel = wording.confirmLabel,
            onConfirm = {
                showDeleteConfirm = false
                items.filter { it.file.name in selectedNames }.forEach { onMoveToRecycleBin(it) }
                exitSelection()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (showAddToAlbumDialog) {
        val targets = remember(existingAlbums) { existingAlbums.asHiddenTargets() }
        AddToAlbumDialog(
            albums = targets,
            title = appString(R.string.common_add_to_hidden_album),
            confirmLabel = appString(R.string.common_create_add),
            onPick = { id ->
                items.filter { it.file.name in selectedNames }.forEach { onAddToAlbum(id, it) }
                showAddToAlbumDialog = false
                exitSelection()
            },
            onNew = { newName ->
                val toAdd = items.filter { it.file.name in selectedNames }
                onCreateAlbum(newName) { newId ->
                    toAdd.forEach { onAddToAlbum(newId, it) }
                }
                showAddToAlbumDialog = false
                exitSelection()
            },
            onDismiss = { showAddToAlbumDialog = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HiddenGridCell(
    item: HiddenItem,
    index: Int,
    isSelecting: Boolean,
    isSelected: Boolean,
    onItemClick: (Int) -> Unit,
    onToggleSelect: () -> Unit,
    onLongPressStartSelect: () -> Unit
) {
    Box {
        val context = LocalContext.current
        AsyncImage(
            model = freshImageRequest(context, item.thumbnailFile, item.thumbnailFile.lastModified()),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .zoomSource(item.file.absolutePath)
                .combinedClickable(
                    onClick = { if (isSelecting) onToggleSelect() else onItemClick(index) },
                    onLongClick = { if (!isSelecting) onLongPressStartSelect() }
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
            SelectionBadge(Modifier.align(Alignment.TopEnd).padding(4.dp))
        }
    }
}

