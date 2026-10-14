package org.nooklin.photos.ui.hidden

import org.nooklin.photos.ui.common.aspectRatioOrUnknown
import org.nooklin.photos.ui.common.scrollToItemCentered

import org.nooklin.photos.ui.common.ScrollToPendingZoomKey
import org.nooklin.photos.ui.common.zoomLazyIndexOf
import org.nooklin.photos.ui.common.zoomSource
import org.nooklin.photos.ui.common.zoomBackdropSource

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Visibility
import org.nooklin.photos.ui.common.MenuIcon
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import org.nooklin.photos.ui.common.freshImageRequest
import org.nooklin.photos.ui.common.pinchToChangeGridColumns
import org.nooklin.photos.R
import org.nooklin.photos.appString

// Reached from Home, re-auth required each visit. Select mode mirrors the main Library.
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
    showFilterMenu: Boolean = false,
    filter: MediaFilter = MediaFilter.ALL,
    onFilterChange: (MediaFilter) -> Unit = {},
    hideScreenshots: Boolean = true,
    onHideScreenshotsChange: (Boolean) -> Unit = {},
    sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    onSortOrderChange: (SortOrder) -> Unit = {},
    gridStyle: GridStyle = GridStyle.SQUARE,
    groupBy: GroupBy = GroupBy.NONE
) {
    // This function keeps the plain names below, so they are re-bound here: one column keeps each photo's own shape
    // and drops the group headers.
    val gridStyle = if (gridColumns == 1) GridStyle.ORIGINAL else gridStyle
    val groupBy = if (gridColumns == 1) GroupBy.NONE else groupBy
    var isSelecting by remember { mutableStateOf(false) }
    var selectedNames by remember { mutableStateOf(setOf<String>()) }
    var actionsMenuExpanded by remember { mutableStateOf(false) }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddToAlbumDialog by remember { mutableStateOf(false) }
    // Same dragToSelect and pinch wiring as GalleryScreen
    val gridState = rememberLazyGridState()
    val staggeredGridState = rememberLazyStaggeredGridState()
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
        scrollToIndex = { i -> if (gridStyle == GridStyle.SQUARE) gridState.scrollToItemCentered(i) else staggeredGridState.scrollToItemCentered(i) }
    )

    fun exitSelection() {
        isSelecting = false
        selectedNames = emptySet()
    }

    fun toggleSelect(item: HiddenItem) {
        val name = item.file.name
        selectedNames = if (name in selectedNames) selectedNames - name else selectedNames + name
    }

    Scaffold(
        modifier = Modifier.zoomBackdropSource(),
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                },
                actions = {
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
                                        text = { Text(appString(R.string.common_favorite)) },
                                        leadingIcon = { MenuIcon(Icons.Filled.FavoriteBorder) },
                                        onClick = {
                                            actionsMenuExpanded = false
                                            items.filter { it.file.name in selectedNames }.forEach { onToggleFavorite(it) }
                                            exitSelection()
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
                                        text = { Text(appString(R.string.common_add_to_album)) },
                                        leadingIcon = { MenuIcon(Icons.Filled.PhotoAlbum) },
                                        onClick = {
                                            actionsMenuExpanded = false
                                            showAddToAlbumDialog = true
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
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(appString(R.string.hidden_gallery_no_hidden_items))
            }
            return@Scaffold
        }
        val hitTestSquareGrid: (Offset) -> Any? = { pos ->
            gridState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                pos.x >= info.offset.x && pos.x <= info.offset.x + info.size.width &&
                    pos.y >= info.offset.y && pos.y <= info.offset.y + info.size.height
            }?.key
        }
        Column(Modifier.fillMaxSize().padding(padding)) {
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
                        items(items.size, key = { items[it].file.name }, contentType = { "photo" }) { index ->
                            val item = items[index]
                            HiddenGridCell(
                                item, index, isSelecting, item.file.name in selectedNames,
                                onItemClick = onItemClick,
                                onToggleSelect = { toggleSelect(item) },
                                onLongPressStartSelect = { isSelecting = true; toggleSelect(item) }
                            )
                        }
                    }
                } else {
                    // remember() hoisted out of LazyGridScope, as in GalleryScreen
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
                                    onLongPressStartSelect = { isSelecting = true; toggleSelect(item) }
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
                                        onLongClick = { if (!isSelecting) { isSelecting = true; toggleSelect(item) } }
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

    if (showDeleteConfirm) {
        val count = selectedNames.size
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
                items.filter { it.file.name in selectedNames }.forEach { onMoveToRecycleBin(it) }
                exitSelection()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (showAddToAlbumDialog) {
        var newAlbumName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddToAlbumDialog = false },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(appString(R.string.common_add_to_hidden_album)) },
            text = {
                Column {
                    // With no hidden albums the dialog lets you create one right here, like the regular Add to Album
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
                                onClick = {
                                    items.filter { it.file.name in selectedNames }.forEach { onAddToAlbum(album.id, it) }
                                    showAddToAlbumDialog = false
                                    exitSelection()
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
                        val toAdd = items.filter { it.file.name in selectedNames }
                        onCreateAlbum(newAlbumName) { newId ->
                            toAdd.forEach { onAddToAlbum(newId, it) }
                        }
                        showAddToAlbumDialog = false
                        exitSelection()
                    }
                ) { Text(appString(R.string.common_create_add)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddToAlbumDialog = false }) { Text(appString(R.string.common_cancel)) }
            }
        )
    }
}

// Shared by the grouped and ungrouped square branches, like GalleryScreen's GridCell
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
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = appString(R.string.common_selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
            )
        }
    }
}

