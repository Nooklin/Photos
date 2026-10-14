package org.nooklin.photos.ui.people

import org.nooklin.photos.ui.common.ScrollToPendingZoomKey
import org.nooklin.photos.ui.common.zoomSource
import org.nooklin.photos.ui.common.zoomBackdropSource
import org.nooklin.photos.ui.common.scrollToItemCentered
import androidx.compose.foundation.lazy.grid.rememberLazyGridState

import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.RemoveCircleOutline
import org.nooklin.photos.ui.common.MenuIcon
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.nooklin.photos.ui.common.freshImageRequest
import androidx.compose.ui.platform.LocalContext
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.ui.albums.AlbumWithPreview
import org.nooklin.photos.ui.common.ConfirmDeleteDialog
import org.nooklin.photos.ui.common.CircleIconButton
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.PillButton
import org.nooklin.photos.ui.common.GlassPillTextField
import org.nooklin.photos.ui.common.PillListRow
import org.nooklin.photos.R
import org.nooklin.photos.appString

// A person's photos: rename like an album, select-mode actions like DiscoveredDetailScreen
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    displayName: String,
    isNamed: Boolean,
    photos: List<MediaItem>,
    facesByMediaId: Map<Long, List<Long>>,
    gridColumns: Int,
    existingAlbums: List<AlbumWithPreview>,
    otherPeople: List<PersonPickerEntry>,
    onRename: (String) -> Unit,
    onMergeWith: (otherPersonId: Long) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onMoveToRecycleBin: (MediaItem) -> Unit,
    onAddToAlbum: (albumId: Long, item: MediaItem) -> Unit,
    onSplitOutFaces: (List<Long>) -> Unit,
    onBack: () -> Unit,
    // Second arg: the viewer pages through this person's photos only
    onItemClick: (MediaItem, List<MediaItem>) -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }
    var isSelecting by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var actionsMenuExpanded by remember { mutableStateOf(false) }
    var showAddToAlbumDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun exitSelection() {
        isSelecting = false
        selectedIds = emptySet()
    }

    fun toggleSelect(item: MediaItem) {
        selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id
    }

    val personGridState = rememberLazyGridState()
    ScrollToPendingZoomKey(
        lazyIndexOf = { key -> photos.indexOfFirst { it.id == key }.takeIf { it >= 0 } },
        scrollToIndex = { i -> personGridState.scrollToItemCentered(i) }
    )

    Scaffold(
        modifier = Modifier.zoomBackdropSource(),
        topBar = {
            TopAppBar(
                title = {
                    if (!isNamed) {
                        Text(
                            appString(R.string.person_detail_tap_to_name, displayName),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { showRenameDialog = true }
                        )
                    } else {
                        Text(displayName, fontWeight = FontWeight.Bold)
                    }
                },
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
                                            photos.filter { it.id in selectedIds }.forEach { onToggleFavorite(it) }
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
                                        text = { Text(appString(R.string.person_detail_remove_from_this_person)) },
                                        leadingIcon = { MenuIcon(Icons.Filled.RemoveCircleOutline) },
                                        onClick = {
                                            actionsMenuExpanded = false
                                            val faceIds = selectedIds.flatMap { facesByMediaId[it].orEmpty() }
                                            onSplitOutFaces(faceIds)
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
                                    text = { Text(appString(R.string.person_detail_merge_with)) },
                                    onClick = {
                                        menuExpanded = false
                                        showMergeDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (photos.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(appString(R.string.common_no_photos_here))
            }
        } else {
            LazyVerticalGrid(
                state = personGridState,
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(photos.size, key = { photos[it].id }) { index ->
                    val item = photos[index]
                    val selected = item.id in selectedIds
                    Box {
                        AsyncImage(
                            model = freshImageRequest(context, item.uri, item.dateModified),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .zoomSource(item.id)
                                .combinedClickable(
                                    onClick = { if (isSelecting) toggleSelect(item) else onItemClick(item, photos) },
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
        var newName by remember { mutableStateOf(if (!isNamed) "" else displayName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(appString(R.string.person_detail_name_this_person)) },
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
                        onRename(newName)
                        showRenameDialog = false
                    }
                ) {
                    Text(appString(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text(appString(R.string.common_cancel)) }
            }
        )
    }

    if (showMergeDialog) {
        AlertDialog(
            onDismissRequest = { showMergeDialog = false },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(appString(R.string.person_detail_merge_with)) },
            text = {
                if (otherPeople.isEmpty()) {
                    Text(appString(R.string.person_detail_no_other_people_to_merge))
                } else {
                    Column {
                        otherPeople.forEach { entry ->
                            ListItem(
                                headlineContent = { Text(entry.label) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onMergeWith(entry.id)
                                        showMergeDialog = false
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMergeDialog = false }) { Text(appString(R.string.common_cancel)) }
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
                                    photos.filter { it.id in selectedIds }.forEach { onAddToAlbum(album.id, it) }
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
                photos.filter { it.id in selectedIds }.forEach { onMoveToRecycleBin(it) }
                exitSelection()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
