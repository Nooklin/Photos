package org.nooklin.photos.ui.gallery

import org.nooklin.photos.ui.common.GlassPillTextField
import org.nooklin.photos.ui.common.yearOf
import androidx.compose.foundation.layout.Column
import org.nooklin.photos.ui.common.SearchFilterRow
import org.nooklin.photos.ui.common.SearchFilterState
import org.nooklin.photos.ui.common.rememberGroupSelection
import org.nooklin.photos.ui.common.GroupSelectionActions
import org.nooklin.photos.ui.common.GroupSelectableCell
import org.nooklin.photos.ui.common.groupDragToSelect
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
import androidx.compose.ui.unit.dp
import org.nooklin.photos.ui.common.GridThumbnail
import androidx.compose.ui.platform.LocalContext
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.ui.common.EmptyState

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

    val filters = remember { SearchFilterState() }
    val years = remember(items) { items.map { yearOf(it.sortDate * 1000L) }.distinct().sortedDescending() }
    val results = remember(query, items, filters.photosOnly, filters.videosOnly, filters.favoritesOnly, filters.year) {
        if (query.isBlank() && !filters.active) {
            emptyList()
        } else {
            items.filter { item ->
                (query.isBlank() || item.displayName?.contains(query.trim(), ignoreCase = true) == true) &&
                    filters.matches(item.isVideo, item.isFavorite, item.sortDate * 1000L)
            }
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
            Column {
            TopAppBar(
                title = {
                    GlassPillTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = appString(R.string.search_search_by_filename),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (selection.isSelecting) selection.exit() else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                },
                actions = {
                    if (groupActions != null && results.isNotEmpty()) GroupSelectionActions(selection, results, groupActions)
                }
            )
            SearchFilterRow(filters, years)
            }
        }
    ) { padding ->
        if (query.isBlank() && !filters.active) {
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
                EmptyState(message = appString(R.string.search_no_matches), actionLabel = appString(R.string.common_back), onAction = onBack)
            }
            return@Scaffold
        }
        LazyVerticalGrid(
            state = searchGridState,
            columns = GridCells.Fixed(gridColumns),
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(padding).groupDragToSelect(selection, searchGridState)
        ) {
            items(results.size, key = { results[it].id }) { index ->
                val item = results[index]
                GroupSelectableCell(
                    selection = if (groupActions != null) selection else null,
                    item = item,
                    actions = groupActions,
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
