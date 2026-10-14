package org.nooklin.photos.ui.albums

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.Modifier
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.R
import org.nooklin.photos.appString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumMergeScreen(
    sourceAlbumId: Long,
    sourceAlbumName: String,
    candidates: List<AlbumWithPreview>,
    onMerge: (targetAlbumId: Long) -> Unit,
    onBack: () -> Unit
) {
    var pendingTarget by remember { mutableStateOf<AlbumWithPreview?>(null) }
    val otherAlbums = candidates.filter { it.id != sourceAlbumId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.common_merge_into), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(otherAlbums, key = { it.id }) { album ->
                ListItem(
                    headlineContent = { Text(album.name) },
                    supportingContent = { Text(appString(R.string.common_items, album.itemCount)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pendingTarget = album }
                )
            }
        }
    }

    pendingTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingTarget = null },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(appString(R.string.album_merge_merge_albums)) },
            text = {
                Text(appString(R.string.album_merge_merge_into_this_can_t, sourceAlbumName, target.name))
            },
            confirmButton = {
                TextButton(onClick = {
                    onMerge(target.id)
                    pendingTarget = null
                }) {
                    Text(appString(R.string.album_merge_merge))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingTarget = null }) { Text(appString(R.string.common_cancel)) }
            }
        )
    }
}
