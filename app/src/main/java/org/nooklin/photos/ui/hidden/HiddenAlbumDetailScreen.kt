package org.nooklin.photos.ui.hidden

import org.nooklin.photos.data.local.MAX_GRID_COLUMNS
import org.nooklin.photos.data.local.MIN_GRID_COLUMNS
import org.nooklin.photos.ui.common.cellAspectRatio
import org.nooklin.photos.ui.common.pinchToChangeGridColumns
import org.nooklin.photos.ui.common.rememberScreenGridColumns
import org.nooklin.photos.ui.common.scrollToItemCentered

import org.nooklin.photos.ui.common.ScrollToPendingZoomKey
import org.nooklin.photos.ui.common.zoomSource
import org.nooklin.photos.ui.common.zoomBackdropSource
import androidx.compose.foundation.lazy.grid.rememberLazyGridState

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
import org.nooklin.photos.ui.albums.AlbumHero
import org.nooklin.photos.ui.albums.CoverPickerDialog
import org.nooklin.photos.ui.albums.FolderRow
import org.nooklin.photos.ui.albums.SlideshowDialog
import org.nooklin.photos.ui.albums.SlideshowPhoto
import org.nooklin.photos.ui.albums.NewFolderDialog
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Visibility
import org.nooklin.photos.ui.common.MenuIcon
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.nooklin.photos.ui.common.freshImageRequest
import org.nooklin.photos.ui.common.heroImageRequest
import org.nooklin.photos.data.HiddenAlbumWithPreview
import org.nooklin.photos.data.HiddenItem
import org.nooklin.photos.ui.common.ConfirmDeleteDialog
import org.nooklin.photos.ui.albums.AlbumContextMenu
import org.nooklin.photos.ui.albums.RenameAlbumDialog
import org.nooklin.photos.ui.common.CircleIconButton
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.PillButton
import org.nooklin.photos.ui.common.GlassPillTextField
import org.nooklin.photos.ui.common.PillListRow
import org.nooklin.photos.R
import org.nooklin.photos.ui.common.deleteWording
import org.nooklin.photos.ui.common.SelectionBadge
import org.nooklin.photos.appString
import org.nooklin.photos.ui.common.EmptyState

// Mirrors HiddenGalleryScreen on purpose, like DiscoveredDetailScreen and PersonDetailScreen
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HiddenAlbumDetailScreen(
    albumName: String,
    items: List<HiddenItem>,
    gridColumns: Int,
    existingAlbums: List<HiddenAlbumWithPreview>,
    onItemClick: (Int) -> Unit,
    onToggleFavorite: (HiddenItem) -> Unit,
    onUnhide: (HiddenItem) -> Unit,
    onMoveToRecycleBin: (HiddenItem) -> Unit,
    onAddToAlbum: (albumId: Long, item: HiddenItem) -> Unit,
    onRemoveFromAlbum: (HiddenItem) -> Unit,
    onRenameAlbum: (String) -> Unit,
    onDeleteAlbum: () -> Unit,
    onAddPhotos: () -> Unit,
    onBack: () -> Unit,
    // A folder is a hidden album inside a hidden album, one level deep
    isFolder: Boolean = false,
    folders: List<HiddenAlbumWithPreview> = emptyList(),
    onFolderClick: (Long) -> Unit = {},
    onRenameFolder: (Long, String) -> Unit = { _, _ -> },
    onDeleteFolder: (Long) -> Unit = {},
    onCreateFolder: (String) -> Unit = {},
    // File name of the photo chosen for the top of the screen, null: the newest photo
    totalCount: Int = items.size,
    coverFileName: String? = null,
    onSetCover: (String?) -> Unit = {}
) {
    // Own column count, set by pinching
    val screenColumns = rememberScreenGridColumns("albums", gridColumns)
    val cols = screenColumns.columns
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    // A long press on a folder in the list opens its menu
    var menuFolderId by remember { mutableStateOf<Long?>(null) }
    var renameFolder by remember { mutableStateOf<HiddenAlbumWithPreview?>(null) }
    var deleteFolder by remember { mutableStateOf<HiddenAlbumWithPreview?>(null) }
    var showDeleteAlbumConfirm by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showCoverPicker by remember { mutableStateOf(false) }
    var showSlideshow by remember { mutableStateOf(false) }
    // Big photo at the top: the chosen cover, else the newest photo
    val heroItem = items.firstOrNull { it.file.name == coverFileName } ?: items.firstOrNull()
    val hasHero = heroItem != null
    val headerCount = (if (hasHero) 1 else 0) + folders.size
    var isSelecting by remember { mutableStateOf(false) }
    var selectedNames by remember { mutableStateOf(setOf<String>()) }
    var actionsMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddToAlbumDialog by remember { mutableStateOf(false) }

    fun exitSelection() {
        isSelecting = false
        selectedNames = emptySet()
    }

    fun toggleSelect(item: HiddenItem) {
        val name = item.file.name
        selectedNames = if (name in selectedNames) selectedNames - name else selectedNames + name
    }

    val hiddenAlbumGridState = rememberLazyGridState()
    ScrollToPendingZoomKey(
        // The big photo and the folder rows come before the photos in the grid
        lazyIndexOf = { key ->
            items.indexOfFirst { it.file.absolutePath == key }.takeIf { it >= 0 }?.plus(headerCount)
        },
        scrollToIndex = { i -> hiddenAlbumGridState.scrollToItemCentered(i) }
    )

    Scaffold(
        modifier = Modifier.zoomBackdropSource(),
        topBar = {
            TopAppBar(
                // With the big photo the bar is see-through, the title is on the photo
                colors = if (hasHero) {
                    TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent)
                } else {
                    TopAppBarDefaults.topAppBarColors()
                },
                title = { if (!hasHero) Text(albumName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (hasHero) {
                        CircleIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = appString(R.string.common_back),
                            onClick = onBack
                        )
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                        }
                    }
                },
                actions = {
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
                                        text = { Text(appString(R.string.hidden_album_detail_remove_from_this_album)) },
                                        leadingIcon = { MenuIcon(Icons.Filled.RemoveCircleOutline) },
                                        onClick = {
                                            actionsMenuExpanded = false
                                            items.filter { it.file.name in selectedNames }.forEach { onRemoveFromAlbum(it) }
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
                    } else {
                        CircleIconButton(
                            icon = Icons.Filled.Add,
                            contentDescription = appString(R.string.common_add_photos),
                            onClick = onAddPhotos
                        )
                        PillButton(text = appString(R.string.common_select), onClick = { isSelecting = true })
                        CircleIconButton(
                            icon = Icons.Filled.MoreVert,
                            contentDescription = appString(R.string.common_more_options),
                            onClick = { menuExpanded = true }
                        )
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            shape = GlassPill.shape,
                            containerColor = GlassPill.adaptivePopupBackground
                        ) {
                            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                                DropdownMenuItem(
                                    text = { Text(appString(if (isFolder) R.string.folder_rename else R.string.album_rename)) },
                                    onClick = {
                                        menuExpanded = false
                                        showRenameDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(appString(R.string.album_set_cover)) },
                                    enabled = items.isNotEmpty(),
                                    onClick = {
                                        menuExpanded = false
                                        showCoverPicker = true
                                    }
                                )
                                if (!isFolder) {
                                    DropdownMenuItem(
                                        text = { Text(appString(R.string.folder_new)) },
                                        onClick = {
                                            menuExpanded = false
                                            showNewFolderDialog = true
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            appString(if (isFolder) R.string.folder_delete else R.string.common_delete_album),
                                            color = GlassPill.destructiveText
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        showDeleteAlbumConfirm = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (items.isEmpty() && folders.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(message = appString(R.string.hidden_album_detail_no_photos_in_this_album), actionLabel = appString(R.string.common_back), onAction = onBack)
            }
        } else {
            LazyVerticalGrid(
                state = hiddenAlbumGridState,
                columns = GridCells.Fixed(cols),
                contentPadding = if (hasHero) PaddingValues(bottom = 2.dp) else PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(if (hasHero) 2.dp else 4.dp),
                verticalArrangement = Arrangement.spacedBy(if (hasHero) 2.dp else 4.dp),
                // The big photo goes under the see-through bar, so the top padding is left out
                modifier = (if (hasHero) Modifier.padding(bottom = padding.calculateBottomPadding()) else Modifier.padding(padding))
                    .pinchToChangeGridColumns(!isSelecting, cols, MIN_GRID_COLUMNS, MAX_GRID_COLUMNS, screenColumns.setColumns)
            ) {
                if (heroItem != null) {
                    item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
                        AlbumHero(
                            title = if (isSelecting) appString(R.string.share_sheet_selected, selectedNames.size) else albumName,
                            itemCount = totalCount,
                            showPlay = items.any { !it.isVideo },
                            onPlay = { showSlideshow = true },
                            image = {
                                // A video has no photo file, its thumbnail is used
                                val heroFile = if (heroItem.isVideo) heroItem.thumbnailFile else heroItem.file
                                AsyncImage(
                                    model = heroImageRequest(context, heroFile, heroFile.lastModified()),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        )
                    }
                }
                items(folders, key = { "folder_" + it.id }, span = { GridItemSpan(maxLineSpan) }) { folder ->
                    FolderRow(
                        name = folder.name,
                        itemCount = folder.itemCount,
                        onClick = { onFolderClick(folder.id) },
                        onLongClick = { menuFolderId = folder.id },
                        menu = {
                            AlbumContextMenu(
                                expanded = menuFolderId == folder.id,
                                onDismiss = { menuFolderId = null },
                                onShare = null,
                                onRename = { renameFolder = folder },
                                onNewFolder = null,
                                onMerge = null,
                                onDelete = { deleteFolder = folder },
                                isFolder = true
                            )
                        },
                        thumbnail = { size -> HiddenAlbumThumbnail(folder.previewFiles.take(1), size = size) }
                    )
                }
                items(items.size, key = { items[it].file.name }) { index ->
                    val item = items[index]
                    val selected = item.file.name in selectedNames
                    Box {
                        AsyncImage(
                            model = freshImageRequest(context, item.thumbnailFile, item.thumbnailFile.lastModified()),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(
                                    if (cols == 1 && item.width != null && item.height != null && item.height > 0) {
                                        item.width.toFloat() / item.height.toFloat()
                                    } else 1f
                                )
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .zoomSource(item.file.absolutePath)
                                .combinedClickable(
                                    onClick = { if (isSelecting) toggleSelect(item) else onItemClick(index) },
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
                            SelectionBadge(Modifier.align(Alignment.TopEnd).padding(4.dp))
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
            title = albumName,
            // Hidden photos stay out of screenshots in the slideshow window too
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
                    model = freshImageRequest(context, items[index].thumbnailFile, items[index].thumbnailFile.lastModified()),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            },
            onPick = { index ->
                onSetCover(items[index].file.name)
                showCoverPicker = false
            },
            onUseNewest = {
                onSetCover(null)
                showCoverPicker = false
            },
            onDismiss = { showCoverPicker = false }
        )
    }

    if (showNewFolderDialog) {
        NewFolderDialog(onCreate = onCreateFolder, onDismiss = { showNewFolderDialog = false })
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(albumName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(appString(if (isFolder) R.string.folder_rename else R.string.hidden_album_detail_rename_hidden_album)) },
            text = {
                GlassPillTextField(
                    value = newName,
                    onValueChange = { newName = it }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        onRenameAlbum(newName)
                        showRenameDialog = false
                    }
                ) {
                    Text(appString(R.string.common_rename))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text(appString(R.string.common_cancel)) }
            }
        )
    }

    renameFolder?.let { folder ->
        RenameAlbumDialog(
            current = folder.name,
            onRename = { onRenameFolder(folder.id, it) },
            onDismiss = { renameFolder = null },
            isFolder = true
        )
    }
    deleteFolder?.let { folder ->
        ConfirmDeleteDialog(
            title = appString(R.string.folder_delete_title),
            text = appString(R.string.folder_delete_hidden_text, folder.itemCount),
            confirmLabel = appString(R.string.folder_delete),
            onConfirm = {
                deleteFolder = null
                onDeleteFolder(folder.id)
            },
            onDismiss = { deleteFolder = null }
        )
    }

    if (showDeleteAlbumConfirm) {
        ConfirmDeleteDialog(
            title = appString(if (isFolder) R.string.folder_delete_title else R.string.common_delete_this_album),
            text = when {
                isFolder -> appString(R.string.folder_delete_hidden_text, items.size)
                // Deleting an album deletes its folders too, so say so first
                folders.isNotEmpty() -> appString(R.string.album_delete_with_folders_hidden, folders.size)
                else -> appString(R.string.hidden_album_detail_this_only_removes_the_album, items.size)
            },
            confirmLabel = appString(if (isFolder) R.string.folder_delete else R.string.common_delete_album),
            onConfirm = {
                showDeleteAlbumConfirm = false
                onDeleteAlbum()
            },
            onDismiss = { showDeleteAlbumConfirm = false }
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
        AlertDialog(
            onDismissRequest = { showAddToAlbumDialog = false },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(appString(R.string.common_add_to_hidden_album)) },
            text = {
                if (existingAlbums.isEmpty()) {
                    Text(appString(R.string.hidden_album_detail_no_other_hidden_albums_yet))
                } else {
                    Column {
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
                TextButton(onClick = { showAddToAlbumDialog = false }) { Text(appString(R.string.common_cancel)) }
            }
        )
    }
}
