package org.nooklin.photos.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.data.HiddenAlbumWithPreview
import org.nooklin.photos.ui.albums.AlbumWithPreview
import org.nooklin.photos.ui.hidden.HiddenAlbumThumbnail

class AlbumTarget(
    val id: Long,
    val name: String,
    val parentId: Long?,
    val itemCount: Int,
    val thumb: @Composable (Dp) -> Unit
)

fun List<AlbumWithPreview>.asTargets(): List<AlbumTarget> = map { a ->
    AlbumTarget(a.id, a.name, a.parentId, a.itemCount) { size ->
        AlbumThumbnail(a.previewItems, size, fillSlots = a.folderCount >= 2)
    }
}

// previews of the hidden section are never shown here
fun List<HiddenAlbumWithPreview>.asHiddenTargets(): List<AlbumTarget> = map { a ->
    AlbumTarget(a.id, a.name, a.parentId, a.itemCount) { size ->
        HiddenAlbumThumbnail(emptyList(), size, previewOff = true)
    }
}

/**
 * New album by name, or pick an existing album or folder. Without onNew only the picker is shown.
 */
@Composable
fun AddToAlbumDialog(
    albums: List<AlbumTarget>,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
    onNew: ((String) -> Unit)? = null,
    backgroundColor: Color = GlassPill.adaptivePopupBackground,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    title: String = appString(R.string.common_add_to_album),
    confirmLabel: String = appString(R.string.photo_viewer_add),
    emptyMessage: String = appString(R.string.common_no_albums_yet_create_one)
) {
    var picking by remember { mutableStateOf(onNew == null) }
    var name by remember { mutableStateOf("") }

    if (onNew == null && albums.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = GlassPill.shape,
            containerColor = backgroundColor,
            titleContentColor = contentColor,
            textContentColor = contentColor,
            title = { Text(title) },
            text = { Text(emptyMessage) },
            confirmButton = { TextButton(onClick = onDismiss) { Text(appString(R.string.common_cancel)) } }
        )
    } else if (picking) {
        AlbumPicker(albums, onPick, onClose = { if (onNew == null) onDismiss() else picking = false })
    } else AlertDialog(
        onDismissRequest = onDismiss,
        shape = GlassPill.shape,
        containerColor = backgroundColor,
        titleContentColor = contentColor,
        textContentColor = contentColor,
        title = { Text(title) },
        text = {
            Column {
                GlassPillTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = appString(R.string.common_new_album_name)
                )
                if (albums.isNotEmpty()) {
                    Box(Modifier.padding(top = 12.dp)) {
                        PillListRow(
                            label = appString(R.string.album_choose_existing),
                            contentColor = contentColor,
                            onClick = { picking = true }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onNew?.invoke(name) },
                enabled = name.isNotBlank()
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(appString(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun AlbumPicker(albums: List<AlbumTarget>, onPick: (Long) -> Unit, onClose: () -> Unit) {
    var currentId by remember { mutableStateOf<Long?>(null) }
    var query by remember { mutableStateOf("") }
    val current = albums.find { it.id == currentId }

    fun goBack() {
        if (query.isNotEmpty()) query = ""
        else if (current != null) currentId = current.parentId
        else onClose()
    }

    Dialog(
        onDismissRequest = { goBack() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                    Text(
                        current?.name ?: appString(R.string.album_pick_title),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    GlassPillTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = appString(R.string.album_pick_search)
                    )
                }
                Box(Modifier.weight(1f)) {
                    if (query.isNotBlank()) {
                        val found = albums.filter { it.name.contains(query.trim(), ignoreCase = true) }
                            .sortedBy { it.name.lowercase() }
                        if (found.isEmpty()) {
                            Text(
                                appString(R.string.album_pick_none),
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                                items(found, key = { it.id }) { album ->
                                    val parent = albums.find { it.id == album.parentId }
                                    PillListRow(
                                        label = if (parent != null) "${parent.name} / ${album.name}" else album.name,
                                        onClick = { query = ""; currentId = album.id }
                                    )
                                }
                            }
                        }
                    } else {
                        val shown = albums.filter { it.parentId == currentId }.sortedBy { it.name.lowercase() }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                        ) {
                            items(shown, key = { it.id }) { album ->
                                Column(Modifier.clickable { currentId = album.id }) {
                                    BoxWithConstraints { album.thumb(maxWidth) }
                                    Text(
                                        album.name,
                                        modifier = Modifier.padding(top = 6.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        album.itemCount.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                if (current != null && query.isBlank()) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        PillButton(text = appString(R.string.album_place_here), onClick = { onPick(current.id) })
                    }
                }
            }
        }
    }
}
