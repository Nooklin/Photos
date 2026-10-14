package org.nooklin.photos.ui.common

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.ui.albums.AlbumWithPreview
import org.nooklin.photos.ui.gallery.ShareSelectionSheet

// Selection for the screens that show a group of photos (Media Types, Utilities, albums, Search, pets).
// The Library has its own, with drag select and the timeline bar.
@Stable
class GroupSelection {
    var isSelecting by mutableStateOf(false)
        private set
    var selectedIds by mutableStateOf(setOf<Long>())
        private set

    fun start(id: Long) {
        isSelecting = true
        selectedIds = selectedIds + id
    }

    fun begin() {
        isSelecting = true
    }

    fun toggle(id: Long) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    fun exit() {
        isSelecting = false
        selectedIds = emptySet()
    }
}

@Composable
fun rememberGroupSelection(): GroupSelection = remember { GroupSelection() }

// What a selection can do. Handed in once from the app level, so every group screen offers the same actions.
class GroupActions(
    val albums: List<AlbumWithPreview>,
    val removeExifOnShare: Boolean,
    val onToggleFavorite: (MediaItem) -> Unit,
    val onMoveToRecycleBin: (MediaItem) -> Unit,
    val onAddToAlbum: (albumId: Long, item: MediaItem) -> Unit
)

// A grid cell that selects instead of opening while a selection is active. Long press starts a selection.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupSelectableCell(
    selection: GroupSelection?,
    id: Long,
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val selecting = selection?.isSelecting == true
    val selected = selection != null && id in selection.selectedIds
    Box(
        modifier.combinedClickable(
            onClick = { if (selecting) selection?.toggle(id) else onClick() },
            onLongClick = { if (selection != null && !selecting) selection.start(id) }
        )
    ) {
        content()
        if (selected) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = appString(R.string.common_selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
            )
        }
    }
}

// For the top bar: Select, or while selecting the actions menu and a close button
@Composable
fun GroupSelectionActions(selection: GroupSelection, items: List<MediaItem>, actions: GroupActions) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showAlbums by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }
    val chosen = items.filter { it.id in selection.selectedIds }

    BackHandler(enabled = selection.isSelecting) { selection.exit() }

    if (!selection.isSelecting) {
        PillButton(text = appString(R.string.common_select), onClick = { selection.begin() })
    } else {
        Box {
        CircleIconButton(
            icon = Icons.Filled.MoreVert,
            contentDescription = appString(R.string.common_more),
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
                    text = { Text(appString(R.string.common_share)) },
                    leadingIcon = { MenuIcon(Icons.Filled.IosShare) },
                    enabled = chosen.isNotEmpty(),
                    onClick = {
                        menuExpanded = false
                        showShare = true
                    }
                )
                DropdownMenuItem(
                    text = { Text(appString(R.string.common_favorite)) },
                    leadingIcon = { MenuIcon(Icons.Filled.FavoriteBorder) },
                    enabled = chosen.isNotEmpty(),
                    onClick = {
                        menuExpanded = false
                        chosen.forEach { actions.onToggleFavorite(it) }
                        selection.exit()
                    }
                )
                DropdownMenuItem(
                    text = { Text(appString(R.string.common_add_to_album)) },
                    leadingIcon = { MenuIcon(Icons.Filled.PhotoAlbum) },
                    enabled = chosen.isNotEmpty(),
                    onClick = {
                        menuExpanded = false
                        showAlbums = true
                    }
                )
                DropdownMenuItem(
                    text = { Text(appString(R.string.common_move_to_recycle_bin), color = GlassPill.destructiveText) },
                    leadingIcon = { MenuIcon(Icons.Filled.Delete) },
                    enabled = chosen.isNotEmpty(),
                    onClick = {
                        menuExpanded = false
                        showDelete = true
                    }
                )
            }
        }
        }
        CircleIconButton(
            icon = Icons.Filled.Close,
            contentDescription = appString(R.string.common_cancel_selection),
            onClick = { selection.exit() }
        )
    }

    if (showShare) {
        ShareSelectionSheet(
            selectedItems = chosen,
            removeMetadataDefault = actions.removeExifOnShare,
            onDismiss = { showShare = false },
            onFinished = {
                showShare = false
                selection.exit()
            }
        )
    }

    if (showAlbums) {
        AlertDialog(
            onDismissRequest = { showAlbums = false },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(appString(R.string.common_add_to_album)) },
            text = {
                if (actions.albums.isEmpty()) {
                    Text(appString(R.string.common_no_albums_yet_create_one))
                } else {
                    Column {
                        actions.albums.forEach { album ->
                            PillListRow(
                                label = album.name,
                                onClick = {
                                    chosen.forEach { actions.onAddToAlbum(album.id, it) }
                                    showAlbums = false
                                    selection.exit()
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlbums = false }) { Text(appString(R.string.common_cancel)) }
            }
        )
    }

    if (showDelete) {
        val count = chosen.size
        ConfirmDeleteDialog(
            title = appString(R.string.common_move_to_recycle_bin_2),
            text = if (count == 1) {
                appString(R.string.common_this_photo_will_be_deleted)
            } else {
                appString(R.string.common_these_photos_will_be_deleted)
            },
            confirmLabel = if (count == 1) appString(R.string.common_delete_photo) else appString(R.string.common_delete_photos, count),
            onConfirm = {
                showDelete = false
                chosen.forEach { actions.onMoveToRecycleBin(it) }
                selection.exit()
            },
            onDismiss = { showDelete = false }
        )
    }
}

// Title while selecting
@Composable
fun groupSelectionTitle(selection: GroupSelection, normal: String): String =
    if (selection.isSelecting) appString(R.string.share_sheet_selected, selection.selectedIds.size) else normal
