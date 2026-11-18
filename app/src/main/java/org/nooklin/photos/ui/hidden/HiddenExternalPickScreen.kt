package org.nooklin.photos.ui.hidden

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.data.HiddenItem
import org.nooklin.photos.data.createExifStrippedShareUriFromFile
import org.nooklin.photos.ui.common.SelectionBadge
import org.nooklin.photos.ui.common.freshImageRequest
import org.nooklin.photos.ui.theme.Spacing

// Another app asked for a photo: the Hidden item goes out as a temporary copy (EXIF stripped when that setting is on)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenExternalPickScreen(
    items: List<HiddenItem>,
    allowMultiple: Boolean,
    columns: Int,
    stripExif: Boolean,
    onBack: () -> Unit,
    onResult: (List<Uri>?) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(listOf<String>()) }
    var busy by remember { mutableStateOf(false) }

    fun send(chosen: List<HiddenItem>) {
        if (busy || chosen.isEmpty()) return
        busy = true
        scope.launch {
            val uris = withContext(Dispatchers.IO) {
                val stamp = System.currentTimeMillis()
                chosen.mapIndexedNotNull { index, item ->
                    createExifStrippedShareUriFromFile(context, item.file, item.isVideo, stripExif, item.mimeType, baseName = "pick_${stamp}_$index")
                }
            }
            busy = false
            if (uris.size == chosen.size) {
                onResult(uris)
            } else {
                Toast.makeText(context, appString(R.string.external_pick_hidden_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.common_hidden_section)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                },
                actions = {
                    if (allowMultiple && selected.isNotEmpty()) {
                        TextButton(onClick = {
                            val byName = items.associateBy { it.file.name }
                            send(selected.mapNotNull { byName[it] })
                        }) {
                            Text(appString(R.string.external_pick_done, selected.size))
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            items(items, key = { it.file.name }) { item ->
                val isSelected = item.file.name in selected
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .padding(1.dp)
                        .clip(MaterialTheme.shapes.small)
                        .clickable(role = Role.Button) {
                            if (!allowMultiple) {
                                send(listOf(item))
                            } else {
                                selected = if (isSelected) selected - item.file.name else selected + item.file.name
                            }
                        }
                ) {
                    AsyncImage(
                        model = freshImageRequest(context, item.thumbnailFile, item.thumbnailFile.lastModified()),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().alpha(if (isSelected) 0.6f else 1f)
                    )
                    if (isSelected) {
                        SelectionBadge(Modifier.align(Alignment.TopEnd).padding(Spacing.Sm))
                    }
                }
            }
        }
    }
}
