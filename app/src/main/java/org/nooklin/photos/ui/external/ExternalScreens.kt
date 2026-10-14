package org.nooklin.photos.ui.external

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.nooklin.photos.ExternalRequest
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.ui.common.VideoPlayerView
import org.nooklin.photos.ui.common.ZoomableImage
import org.nooklin.photos.ui.common.freshImageRequest

// One file opened from another app. Shows only that file, never the library.
@Composable
fun ExternalViewScreen(request: ExternalRequest.View, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val isVideo = request.mimeType?.startsWith("video/") == true
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (isVideo) {
            VideoPlayerView(model = request.uri, modifier = Modifier.fillMaxSize())
        } else {
            ZoomableImage(
                model = request.uri,
                freshnessKey = request.uri.toString(),
                isCurrentPage = true,
                onZoomStateChanged = {},
                modifier = Modifier.fillMaxSize()
            )
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(4.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = appString(R.string.common_back),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

// Another app asked for a photo or video. Single choice returns at once, multiple ends with Done.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalPickScreen(
    items: List<MediaItem>,
    allowMultiple: Boolean,
    columns: Int,
    onResult: (List<Uri>?) -> Unit
) {
    BackHandler { onResult(null) }
    val context = LocalContext.current
    var selected by remember { mutableStateOf(listOf<Long>()) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.external_pick_title)) },
                navigationIcon = {
                    IconButton(onClick = { onResult(null) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_cancel))
                    }
                },
                actions = {
                    if (allowMultiple && selected.isNotEmpty()) {
                        TextButton(onClick = {
                            val byId = items.associateBy { it.id }
                            onResult(selected.mapNotNull { byId[it]?.uri })
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
            items(items, key = { it.id }) { item ->
                val isSelected = item.id in selected
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .padding(1.dp)
                        .clip(MaterialTheme.shapes.small)
                        .clickable {
                            if (!allowMultiple) {
                                onResult(listOf(item.uri))
                            } else {
                                selected = if (isSelected) selected - item.id else selected + item.id
                            }
                        }
                ) {
                    AsyncImage(
                        model = freshImageRequest(context, item.uri, item.dateModified),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().alpha(if (isSelected) 0.6f else 1f)
                    )
                    if (isSelected) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = appString(R.string.common_selected),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                        )
                    }
                }
            }
        }
    }
}
