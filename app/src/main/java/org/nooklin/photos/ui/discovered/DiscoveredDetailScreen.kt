package org.nooklin.photos.ui.discovered

import org.nooklin.photos.data.local.MAX_GRID_COLUMNS
import org.nooklin.photos.data.local.MIN_GRID_COLUMNS
import org.nooklin.photos.ui.common.cellAspectRatio
import org.nooklin.photos.ui.common.pinchToChangeGridColumns
import org.nooklin.photos.ui.common.rememberScreenGridColumns
import org.nooklin.photos.ui.common.ScrollToPendingZoomKey
import org.nooklin.photos.ui.common.zoomSource
import org.nooklin.photos.ui.common.zoomBackdropSource
import org.nooklin.photos.ui.common.scrollToItemCentered
import androidx.compose.foundation.lazy.grid.rememberLazyGridState

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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoAlbum
import org.nooklin.photos.ui.common.MenuIcon
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pets
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
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.nooklin.photos.ui.common.freshImageRequest
import androidx.compose.ui.platform.LocalContext
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.ui.MediaFilter
import org.nooklin.photos.ui.albums.AlbumWithPreview
import org.nooklin.photos.ui.common.ConfirmDeleteDialog
import org.nooklin.photos.ui.common.CircleIconButton
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.PillButton
import org.nooklin.photos.ui.common.GlassPillTextField
import org.nooklin.photos.ui.common.PillListRow
import org.nooklin.photos.ui.pets.PetWithPreview
import org.nooklin.photos.R
import org.nooklin.photos.appString

// Matches GalleryScreen's top bar, select mode and filter
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DiscoveredDetailScreen(
    label: String,
    photos: List<MediaItem>,
    gridColumns: Int,
    existingAlbums: List<AlbumWithPreview>,
    isPetsCategory: Boolean = false,
    existingPets: List<PetWithPreview> = emptyList(),
    onAddToPet: (petId: Long, item: MediaItem) -> Unit = { _, _ -> },
    onCreatePetAndAdd: (name: String, items: List<MediaItem>) -> Unit = { _, _ -> },
    onOpenPets: () -> Unit = {},
    onToggleFavorite: (MediaItem) -> Unit,
    onMoveToRecycleBin: (MediaItem) -> Unit,
    onAddToAlbum: (albumId: Long, item: MediaItem) -> Unit,
    onBack: () -> Unit,
    // Second arg: the viewer pages through this filtered list
    onItemClick: (MediaItem, List<MediaItem>) -> Unit
) {
    // Own column count, set by pinching
    val screenColumns = rememberScreenGridColumns("discovered_$label", gridColumns)
    val cols = screenColumns.columns
    val context = LocalContext.current
    var filter by remember { mutableStateOf(MediaFilter.ALL) }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var isSelecting by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var actionsMenuExpanded by remember { mutableStateOf(false) }
    var showAddToAlbumDialog by remember { mutableStateOf(false) }
    var showAddToPetDialog by remember { mutableStateOf(false) }
    // Set by the "+" badge on a Pets photo (quick add). Null means the dialog acts on the selection.
    var quickAddPetItem by remember { mutableStateOf<MediaItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val filtered = when (filter) {
        MediaFilter.ALL -> photos
        MediaFilter.FAVORITES -> photos.filter { it.isFavorite }
        MediaFilter.VIDEOS -> photos.filter { it.isVideo }
    }

    fun exitSelection() {
        isSelecting = false
        selectedIds = emptySet()
    }

    fun toggleSelect(item: MediaItem) {
        selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id
    }

    val discoveredGridState = rememberLazyGridState()
    ScrollToPendingZoomKey(
        lazyIndexOf = { key -> filtered.indexOfFirst { it.id == key }.takeIf { it >= 0 } },
        scrollToIndex = { i -> discoveredGridState.scrollToItemCentered(i) }
    )

    Scaffold(
        modifier = Modifier.zoomBackdropSource(),
        topBar = {
            TopAppBar(
                title = { Text(label, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                },
                actions = {
                    if (isPetsCategory) {
                        CircleIconButton(
                            icon = Icons.Filled.Pets,
                            contentDescription = appString(R.string.discovered_detail_view_pets),
                            onClick = onOpenPets
                        )
                    }
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
                                    onClick = { filter = MediaFilter.ALL; filterMenuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(appString(R.string.common_favorites)) },
                                    trailingIcon = {
                                        if (filter == MediaFilter.FAVORITES) {
                                            Icon(Icons.Filled.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = { filter = MediaFilter.FAVORITES; filterMenuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(appString(R.string.common_videos)) },
                                    trailingIcon = {
                                        if (filter == MediaFilter.VIDEOS) {
                                            Icon(Icons.Filled.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = { filter = MediaFilter.VIDEOS; filterMenuExpanded = false }
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
                                        text = { Text(appString(R.string.common_favorite)) },
                                        leadingIcon = { MenuIcon(Icons.Filled.FavoriteBorder) },
                                        onClick = {
                                            actionsMenuExpanded = false
                                            filtered.filter { it.id in selectedIds }.forEach { onToggleFavorite(it) }
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
                                    if (isPetsCategory) {
                                        DropdownMenuItem(
                                            text = { Text(appString(R.string.discovered_detail_add_to_pet)) },
                                            leadingIcon = { MenuIcon(Icons.Filled.Pets) },
                                            onClick = {
                                                actionsMenuExpanded = false
                                                showAddToPetDialog = true
                                            }
                                        )
                                    }
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
        if (filtered.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(if (photos.isEmpty()) appString(R.string.common_no_photos_here) else appString(R.string.discovered_detail_nothing_matches_this_filter))
            }
        } else {
            LazyVerticalGrid(
                state = discoveredGridState,
                columns = GridCells.Fixed(cols),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(padding).pinchToChangeGridColumns(!isSelecting, cols, MIN_GRID_COLUMNS, MAX_GRID_COLUMNS, screenColumns.setColumns)
            ) {
                items(filtered.size, key = { filtered[it].id }) { index ->
                    val item = filtered[index]
                    val selected = item.id in selectedIds
                    Box {
                        AsyncImage(
                            model = freshImageRequest(context, item.uri, item.dateModified),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(if (cols == 1) item.cellAspectRatio() else 1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .zoomSource(item.id)
                                .combinedClickable(
                                    onClick = { if (isSelecting) toggleSelect(item) else onItemClick(item, filtered) },
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
                        // Quick add: opens the Add to Pet dialog for this photo without select mode
                        if (isPetsCategory && !isSelecting) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .size(22.dp)
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                    .clickable {
                                        quickAddPetItem = item
                                        showAddToPetDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = appString(R.string.discovered_detail_add_to_pet),
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
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
                                    filtered.filter { it.id in selectedIds }.forEach { onAddToAlbum(album.id, it) }
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

    if (showAddToPetDialog) {
        var newPetName by remember { mutableStateOf("") }
        val targets = quickAddPetItem?.let { listOf(it) } ?: filtered.filter { it.id in selectedIds }
        fun closeAddToPetDialog() {
            showAddToPetDialog = false
            quickAddPetItem = null
        }
        AlertDialog(
            onDismissRequest = { closeAddToPetDialog() },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(appString(R.string.discovered_detail_add_to_pet)) },
            text = {
                Column {
                    GlassPillTextField(
                        value = newPetName,
                        onValueChange = { newPetName = it },
                        placeholder = appString(R.string.discovered_detail_new_pet_name)
                    )
                    if (existingPets.isNotEmpty()) {
                        Text(
                            appString(R.string.discovered_detail_or_add_to_an_existing),
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                        Column {
                            existingPets.forEach { pet ->
                                ListItem(
                                    headlineContent = { Text(pet.name) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            targets.forEach { onAddToPet(pet.id, it) }
                                            closeAddToPetDialog()
                                            if (quickAddPetItem == null) exitSelection()
                                        }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newPetName.isNotBlank(),
                    onClick = {
                        val wasQuickAdd = quickAddPetItem != null
                        onCreatePetAndAdd(newPetName, targets)
                        closeAddToPetDialog()
                        if (!wasQuickAdd) exitSelection()
                    }
                ) {
                    Text(appString(R.string.common_create_add))
                }
            },
            dismissButton = {
                TextButton(onClick = { closeAddToPetDialog() }) { Text(appString(R.string.common_cancel)) }
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
                filtered.filter { it.id in selectedIds }.forEach { onMoveToRecycleBin(it) }
                exitSelection()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

