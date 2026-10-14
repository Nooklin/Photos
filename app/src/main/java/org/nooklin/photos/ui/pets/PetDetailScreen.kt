package org.nooklin.photos.ui.pets

import org.nooklin.photos.ui.common.rememberGroupSelection
import org.nooklin.photos.ui.common.groupSelectionTitle
import org.nooklin.photos.ui.common.GroupSelectionActions
import org.nooklin.photos.ui.common.GroupSelectableCell
import org.nooklin.photos.ui.common.GroupActions
import org.nooklin.photos.ui.common.ScrollToPendingZoomKey
import org.nooklin.photos.ui.common.zoomSource
import org.nooklin.photos.ui.common.zoomBackdropSource
import org.nooklin.photos.ui.common.scrollToItemCentered
import androidx.compose.foundation.lazy.grid.rememberLazyGridState

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.nooklin.photos.ui.common.freshImageRequest
import androidx.compose.ui.platform.LocalContext
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.ui.common.ConfirmDeleteDialog
import org.nooklin.photos.ui.common.CircleIconButton
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.GlassPillTextField
import org.nooklin.photos.R
import org.nooklin.photos.appString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(
    petName: String,
    photos: List<MediaItem>,
    gridColumns: Int,
    groupActions: GroupActions? = null,
    onBack: () -> Unit,
    // Viewer pages through this pet's photos only
    onItemClick: (MediaItem, List<MediaItem>) -> Unit,
    onAddPhotos: () -> Unit,
    onRename: (String) -> Unit,
    onDeletePet: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val selection = rememberGroupSelection()
    val petGridState = rememberLazyGridState()
    ScrollToPendingZoomKey(
        lazyIndexOf = { key -> photos.indexOfFirst { it.id == key }.takeIf { it >= 0 } },
        scrollToIndex = { i -> petGridState.scrollToItemCentered(i) }
    )

    Scaffold(
        modifier = Modifier.zoomBackdropSource(),
        topBar = {
            TopAppBar(
                title = { Text(groupSelectionTitle(selection, petName), fontWeight = FontWeight.Bold) },
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
                                onClick = {
                                    menuExpanded = false
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(appString(R.string.pet_detail_delete_pet), color = GlassPill.destructiveText) },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteConfirm = true
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
                Text(appString(R.string.pet_detail_no_photos_assigned_to_yet, petName))
            }
        } else {
            LazyVerticalGrid(
                state = petGridState,
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(photos.size, key = { photos[it].id }) { index ->
                    val item = photos[index]
                    GroupSelectableCell(
                        selection = if (groupActions != null) selection else null,
                        id = item.id,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .zoomSource(item.id),
                        onClick = { onItemClick(item, photos) }
                    ) {
                        AsyncImage(
                            model = freshImageRequest(context, item.uri, item.dateModified),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(petName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(appString(R.string.pet_detail_rename_pet)) },
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

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = appString(R.string.pet_detail_delete_pet_2),
            text = appString(R.string.pet_detail_will_be_removed_the_photos, petName),
            confirmLabel = appString(R.string.pet_detail_delete_pet),
            onConfirm = {
                showDeleteConfirm = false
                onDeletePet()
                onBack()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
