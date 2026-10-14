package org.nooklin.photos.ui.albums

import org.nooklin.photos.ui.common.rememberGroupSelection
import org.nooklin.photos.ui.common.groupSelectionTitle
import org.nooklin.photos.ui.common.GroupSelectionActions
import org.nooklin.photos.ui.common.GroupSelectableCell
import org.nooklin.photos.ui.common.GroupActions
import org.nooklin.photos.data.local.MAX_GRID_COLUMNS
import org.nooklin.photos.data.local.MIN_GRID_COLUMNS
import org.nooklin.photos.ui.common.cellAspectRatio
import org.nooklin.photos.ui.common.pinchToChangeGridColumns
import org.nooklin.photos.ui.common.rememberScreenGridColumns
import org.nooklin.photos.ui.common.scrollToItemCentered

import org.nooklin.photos.ui.common.ScrollToPendingZoomKey
import org.nooklin.photos.ui.common.ZoomSources
import org.nooklin.photos.ui.common.zoomSource
import androidx.compose.foundation.lazy.grid.rememberLazyGridState

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import org.nooklin.photos.ui.common.MenuIcon
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.nooklin.photos.ui.common.GridThumbnail
import androidx.compose.ui.graphics.FilterQuality
import kotlinx.coroutines.launch
import org.nooklin.photos.ui.common.freshImageRequest
import org.nooklin.photos.ui.common.prefetchScreenTierImage
import androidx.compose.ui.platform.LocalContext
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.ui.common.CircleIconButton
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.GlassPillTextField
import org.nooklin.photos.ui.common.ConfirmDeleteDialog
import org.nooklin.photos.R
import org.nooklin.photos.appString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumName: String,
    photos: List<MediaItem>,
    gridColumns: Int,
    groupActions: GroupActions? = null,
    onBack: () -> Unit,
    onItemClick: (MediaItem, Rect, ImageBitmap?) -> Unit,
    onRename: (String) -> Unit,
    onMergeRequested: () -> Unit,
    onDeleteAlbum: () -> Unit,
    onAddPhotos: () -> Unit
) {
    // Own column count, set by pinching
    val screenColumns = rememberScreenGridColumns("albums", gridColumns)
    val cols = screenColumns.columns
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteAlbumConfirm by remember { mutableStateOf(false) }
    val backdropGraphicsLayer = rememberGraphicsLayer()
    val coroutineScope = rememberCoroutineScope()

    val selection = rememberGroupSelection()
    val albumGridState = rememberLazyGridState()
    ScrollToPendingZoomKey(
        lazyIndexOf = { key -> photos.indexOfFirst { it.id == key }.takeIf { it >= 0 } },
        scrollToIndex = { i -> albumGridState.scrollToItemCentered(i) }
    )

    Scaffold(
        modifier = Modifier.drawWithContent {
            backdropGraphicsLayer.record { this@drawWithContent.drawContent() }
            drawLayer(backdropGraphicsLayer)
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.Bottom) {
                        if (selection.isSelecting) {
                            Text(groupSelectionTitle(selection, albumName), fontWeight = FontWeight.Bold)
                        } else {
                            Text(albumName, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Text(appString(R.string.common_items, photos.size), color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                },
                actions = {
                    if (groupActions != null && photos.isNotEmpty()) GroupSelectionActions(selection, photos, groupActions)
                    CircleIconButton(
                        icon = Icons.Filled.Add,
                        contentDescription = appString(R.string.common_add_photos),
                        onClick = onAddPhotos
                    )
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
                                leadingIcon = { MenuIcon(Icons.Filled.Edit) },
                                onClick = {
                                    menuExpanded = false
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(appString(R.string.common_merge_into)) },
                                leadingIcon = { MenuIcon(Icons.Filled.CallMerge) },
                                onClick = {
                                    menuExpanded = false
                                    onMergeRequested()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(appString(R.string.common_delete_album), color = GlassPill.destructiveText) },
                                leadingIcon = { MenuIcon(Icons.Filled.Delete) },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteAlbumConfirm = true
                                }
                            )
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
                Text(appString(R.string.album_detail_no_photos_in_this_album))
            }
        } else {
            LazyVerticalGrid(
                state = albumGridState,
                columns = GridCells.Fixed(cols),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(padding).pinchToChangeGridColumns(true, cols, MIN_GRID_COLUMNS, MAX_GRID_COLUMNS, screenColumns.setColumns)
            ) {
                items(photos.size, key = { photos[it].id }) { index ->
                    val item = photos[index]
                    GroupSelectableCell(
                        selection = if (groupActions != null) selection else null,
                        id = item.id,
                        modifier = Modifier
                            .aspectRatio(if (cols == 1) item.cellAspectRatio() else 1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .zoomSource(item.id),
                        onClick = {
                            prefetchScreenTierImage(context, item)
                            coroutineScope.launch {
                                val backdrop = runCatching { backdropGraphicsLayer.toImageBitmap() }.getOrNull()
                                onItemClick(item, (ZoomSources.boundsOf(item.id) ?: Rect.Zero), backdrop)
                            }
                        }
                    ) {
                        GridThumbnail(uri = item.uri, dateModified = item.dateModified, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    if (showDeleteAlbumConfirm) {
        ConfirmDeleteDialog(
            title = appString(R.string.common_delete_this_album),
            text = appString(R.string.album_detail_the_photos_stay_in_your),
            confirmLabel = appString(R.string.common_delete_album),
            onConfirm = {
                showDeleteAlbumConfirm = false
                onDeleteAlbum()
            },
            onDismiss = { showDeleteAlbumConfirm = false }
        )
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(albumName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(appString(R.string.album_detail_rename_album)) },
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
                    Text(appString(R.string.common_rename))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text(appString(R.string.common_cancel)) }
            }
        )
    }
}
