package org.nooklin.photos.ui.hidden

import androidx.compose.ui.text.font.FontWeight
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import org.nooklin.photos.ui.common.ConfirmDeleteDialog
import org.nooklin.photos.ui.common.PillButton
import org.nooklin.photos.ui.common.groupSelectionTitle
import org.nooklin.photos.ui.common.rememberGroupSelection
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import org.nooklin.photos.ui.common.MenuIcon
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.nooklin.photos.data.HiddenAlbumWithPreview
import org.nooklin.photos.ui.common.CircleIconButton
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.GlassPillTextField
import org.nooklin.photos.R
import org.nooklin.photos.appString

// Hidden albums, only reachable inside the gate. Stored in HiddenDatabase, not NooklinDatabase.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HiddenAlbumsScreen(
    albums: List<HiddenAlbumWithPreview>,
    gridColumns: Int,
    onAlbumClick: (Long) -> Unit,
    onCreateAlbum: (String) -> Unit,
    onDeleteAlbums: (Set<Long>) -> Unit,
    onBack: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    val selection = rememberGroupSelection()
    var menuExpanded by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    BackHandler(enabled = selection.isSelecting) { selection.exit() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        groupSelectionTitle(selection, appString(R.string.common_hidden_albums)),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                },
                actions = {
                    if (selection.isSelecting) {
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
                                        text = { Text(appString(R.string.common_delete_album), color = GlassPill.destructiveText) },
                                        leadingIcon = { MenuIcon(Icons.Filled.Delete) },
                                        enabled = selection.selectedIds.isNotEmpty(),
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
                    } else {
                        if (albums.isNotEmpty()) {
                            PillButton(text = appString(R.string.common_select), onClick = { selection.begin() })
                        }
                        CircleIconButton(
                            icon = Icons.Filled.Add,
                            contentDescription = appString(R.string.hidden_albums_new_hidden_album),
                            onClick = { showCreateDialog = true }
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (albums.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    appString(R.string.hidden_albums_no_hidden_albums_yet_tap),
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(albums, key = { it.id }) { album ->
                    val selected = album.id in selection.selectedIds
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { if (selection.isSelecting) selection.toggle(album.id) else onAlbumClick(album.id) },
                                onLongClick = { if (!selection.isSelecting) selection.start(album.id) }
                            )
                    ) {
                        Box {
                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                                HiddenAlbumThumbnail(album.previewFiles, size = maxWidth)
                            }
                            if (selected) {
                                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.3f)))
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = appString(R.string.common_selected),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                                )
                            }
                        }
                        Text(
                            album.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Text(
                            appString(R.string.common_items, album.itemCount),
                            color = Color.Gray,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (showDelete) {
        ConfirmDeleteDialog(
            title = appString(R.string.common_delete_this_album),
            text = appString(R.string.album_detail_the_photos_stay_in_your),
            confirmLabel = if (selection.selectedIds.size == 1) {
                appString(R.string.common_delete_album)
            } else {
                appString(R.string.albums_delete_albums, selection.selectedIds.size)
            },
            onConfirm = {
                showDelete = false
                onDeleteAlbums(selection.selectedIds)
                selection.exit()
            },
            onDismiss = { showDelete = false }
        )
    }

    if (showCreateDialog) {
        var newAlbumName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(appString(R.string.hidden_albums_new_hidden_album_2)) },
            text = {
                GlassPillTextField(
                    value = newAlbumName,
                    onValueChange = { newAlbumName = it },
                    placeholder = appString(R.string.common_album_name)
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newAlbumName.isNotBlank(),
                    onClick = {
                        onCreateAlbum(newAlbumName)
                        showCreateDialog = false
                    }
                ) {
                    Text(appString(R.string.common_create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text(appString(R.string.common_cancel)) }
            }
        )
    }
}
