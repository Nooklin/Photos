package org.nooklin.photos.ui.discovered

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import org.nooklin.photos.ui.common.MenuIcon
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.nooklin.photos.ui.common.AlbumThumbnail
import org.nooklin.photos.ui.common.ConfirmDeleteDialog
import org.nooklin.photos.ui.common.CircleIconButton
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.PillButton
import org.nooklin.photos.R
import org.nooklin.photos.appString

// Smart albums from detection results, same collage grid as AlbumsScreen
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DiscoveredScreen(
    albums: List<DiscoveredAlbum>,
    gridColumns: Int,
    onAlbumClick: (String) -> Unit,
    onDeleteCategories: (Set<String>) -> Unit,
    onBack: () -> Unit
) {
    var isSelecting by remember { mutableStateOf(false) }
    var selectedTagIds by remember { mutableStateOf(setOf<String>()) }
    var actionsMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val visibleAlbums = albums

    fun exitSelection() {
        isSelecting = false
        selectedTagIds = emptySet()
    }

    fun toggleSelect(tagId: String) {
        selectedTagIds = if (tagId in selectedTagIds) selectedTagIds - tagId else selectedTagIds + tagId
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.common_discovered), fontWeight = FontWeight.Bold) },
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
                                        text = { Text(appString(R.string.common_delete), color = GlassPill.destructiveText) },
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
        if (visibleAlbums.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    appString(R.string.discovered_nothing_detected_yet_turn_on),
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
                items(visibleAlbums, key = { it.tagId }) { album ->
                    val selected = album.tagId in selectedTagIds
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (isSelecting) toggleSelect(album.tagId) else onAlbumClick(album.tagId)
                                },
                                onLongClick = {
                                    if (!isSelecting) { isSelecting = true; toggleSelect(album.tagId) }
                                }
                            )
                    ) {
                        Box {
                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                                AlbumThumbnail(album.previewItems, size = maxWidth)
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
                        Text(
                            album.label,
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

    if (showDeleteConfirm) {
        val selectedLabels = albums.filter { it.tagId in selectedTagIds }.joinToString { it.label }
        ConfirmDeleteDialog(
            title = appString(R.string.discovered_delete_detected_categories),
            text = appString(R.string.discovered_everything_detected_for_will_be, selectedLabels),
            confirmLabel = appString(R.string.common_delete),
            onConfirm = {
                showDeleteConfirm = false
                onDeleteCategories(selectedTagIds)
                exitSelection()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

