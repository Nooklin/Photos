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
import org.nooklin.photos.data.HiddenAlbumWithPreview
import org.nooklin.photos.data.HiddenItem
import org.nooklin.photos.ui.common.ConfirmDeleteDialog
import org.nooklin.photos.ui.common.CircleIconButton
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.PillButton
import org.nooklin.photos.ui.common.GlassPillTextField
import org.nooklin.photos.ui.common.PillListRow
import org.nooklin.photos.R
import org.nooklin.photos.appString

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
    onBack: () -> Unit
) {
    // Own column count, set by pinching
    val screenColumns = rememberScreenGridColumns("albums", gridColumns)
    val cols = screenColumns.columns
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteAlbumConfirm by remember { mutableStateOf(false) }
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
        lazyIndexOf = { key -> items.indexOfFirst { it.file.absolutePath == key }.takeIf { it >= 0 } },
        scrollToIndex = { i -> hiddenAlbumGridState.scrollToItemCentered(i) }
    )

    Scaffold(
        modifier = Modifier.zoomBackdropSource(),
        topBar = {
            TopAppBar(
                title = { Text(albumName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
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
                        CircleIconButton(
                            icon = Icons.Filled.Close,
                            contentDescription = appString(R.string.common_cancel_selection),
                            onClick = { exitSelection() }
                        )
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
                                    text = { Text(appString(R.string.common_rename)) },
                                    onClick = {
                                        menuExpanded = false
                                        showRenameDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(appString(R.string.common_delete_album), color = GlassPill.destructiveText) },
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
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(appString(R.string.hidden_album_detail_no_photos_in_this_album))
            }
        } else {
            LazyVerticalGrid(
                state = hiddenAlbumGridState,
                columns = GridCells.Fixed(cols),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(padding).pinchToChangeGridColumns(!isSelecting, cols, MIN_GRID_COLUMNS, MAX_GRID_COLUMNS, screenColumns.setColumns)
            ) {
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

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(albumName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(appString(R.string.hidden_album_detail_rename_hidden_album)) },
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

    if (showDeleteAlbumConfirm) {
        ConfirmDeleteDialog(
            title = appString(R.string.common_delete_this_album),
            text = appString(R.string.hidden_album_detail_this_only_removes_the_album, items.size),
            confirmLabel = appString(R.string.common_delete_album),
            onConfirm = {
                showDeleteAlbumConfirm = false
                onDeleteAlbum()
                onBack()
            },
            onDismiss = { showDeleteAlbumConfirm = false }
        )
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
