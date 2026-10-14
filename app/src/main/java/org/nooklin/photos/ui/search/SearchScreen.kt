package org.nooklin.photos.ui.search

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import org.nooklin.photos.ui.common.GridThumbnail
import androidx.compose.ui.graphics.FilterQuality
import org.nooklin.photos.ui.common.freshImageRequest
import androidx.compose.ui.platform.LocalContext
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.R
import org.nooklin.photos.appString

// Filename search only (DISPLAY_NAME), no full-text or EXIF
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    items: List<MediaItem>,
    gridColumns: Int,
    groupActions: GroupActions? = null,
    onBack: () -> Unit,
    onItemClick: (MediaItem, List<MediaItem>) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

    val results = remember(query, items) {
        if (query.isBlank()) {
            emptyList()
        } else {
            items.filter { item -> item.displayName?.contains(query, ignoreCase = true) == true }
        }
    }

    val selection = rememberGroupSelection()
    val searchGridState = rememberLazyGridState()
    ScrollToPendingZoomKey(
        lazyIndexOf = { key -> results.indexOfFirst { it.id == key }.takeIf { it >= 0 } },
        scrollToIndex = { i -> searchGridState.scrollToItemCentered(i) }
    )

    Scaffold(
        modifier = Modifier.zoomBackdropSource(),
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(appString(R.string.search_search_by_filename)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                },
                actions = {
                    if (groupActions != null && results.isNotEmpty()) GroupSelectionActions(selection, results, groupActions)
                }
            )
        }
    ) { padding ->
        if (query.isBlank()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(appString(R.string.search_search_by_filename))
            }
            return@Scaffold
        }
        if (results.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(appString(R.string.search_no_matches))
            }
            return@Scaffold
        }
        LazyVerticalGrid(
            state = searchGridState,
            columns = GridCells.Fixed(gridColumns),
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(padding)
        ) {
            items(results.size, key = { results[it].id }) { index ->
                val item = results[index]
                GroupSelectableCell(
                    selection = if (groupActions != null) selection else null,
                    id = item.id,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .zoomSource(item.id),
                    onClick = { onItemClick(item, results) }
                ) {
                    GridThumbnail(uri = item.uri, dateModified = item.dateModified, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
