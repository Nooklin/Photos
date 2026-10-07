package org.nooklin.photos.ui.category

import org.nooklin.photos.ui.common.scrollToItemCentered

import org.nooklin.photos.ui.common.ScrollToPendingZoomKey
import org.nooklin.photos.ui.common.zoomSource
import org.nooklin.photos.ui.common.zoomBackdropSource
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.nooklin.photos.ui.common.GridThumbnail
import androidx.compose.ui.graphics.FilterQuality
import org.nooklin.photos.ui.common.freshImageRequest
import org.nooklin.photos.data.MediaCategory
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.data.filterByCategory
import org.nooklin.photos.data.local.GridStyle
import org.nooklin.photos.data.local.MAX_GRID_COLUMNS
import org.nooklin.photos.data.local.MIN_GRID_COLUMNS
import org.nooklin.photos.ui.common.pinchToChangeGridColumns
import org.nooklin.photos.ui.common.rememberScreenGridColumns

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    category: MediaCategory,
    allItems: List<MediaItem>,
    gridStyleSetting: GridStyle,
    gridColumns: Int,
    duplicateMediaIds: Set<Long> = emptySet(),
    onBack: () -> Unit,
    // Second arg: the viewer pages through this list only
    onItemClick: (MediaItem, List<MediaItem>) -> Unit
) {
    val context = LocalContext.current
    // Own column count per category, set by pinching
    val screenColumns = rememberScreenGridColumns("category_${category.name}", gridColumns)
    val cols = screenColumns.columns
    // One column keeps each photo's own shape
    val gridStyle = if (cols == 1) GridStyle.ORIGINAL else gridStyleSetting
    // DUPLICATES needs an async DB read, the caller resolves it
    val filtered = if (category == MediaCategory.DUPLICATES) {
        allItems.filter { it.id in duplicateMediaIds }
    } else {
        filterByCategory(allItems, category)
    }

    val squareGridState = rememberLazyGridState()
    val staggeredGridState = rememberLazyStaggeredGridState()
    ScrollToPendingZoomKey(
        lazyIndexOf = { key -> filtered.indexOfFirst { it.id == key }.takeIf { it >= 0 } },
        scrollToIndex = { i -> if (gridStyle == GridStyle.SQUARE) squareGridState.scrollToItemCentered(i) else staggeredGridState.scrollToItemCentered(i) }
    )

    Scaffold(
        modifier = Modifier.zoomBackdropSource(),
        topBar = {
            TopAppBar(
                title = { Text(category.label, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (filtered.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Nothing here yet")
            }
            return@Scaffold
        }

        if (gridStyle == GridStyle.SQUARE) {
            LazyVerticalGrid(
                state = squareGridState,
                columns = GridCells.Fixed(cols),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(padding).pinchToChangeGridColumns(true, cols, MIN_GRID_COLUMNS, MAX_GRID_COLUMNS, screenColumns.setColumns)
            ) {
                items(filtered.size, key = { filtered[it].id }) { index ->
                    val item = filtered[index]
                    GridThumbnail(
                        uri = item.uri,
                        dateModified = item.dateModified,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .zoomSource(item.id)
                            .clickable { onItemClick(item, filtered) }
                    )
                }
            }
        } else {
            LazyVerticalStaggeredGrid(
                state = staggeredGridState,
                columns = StaggeredGridCells.Fixed(cols),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalItemSpacing = 4.dp,
                modifier = Modifier.padding(padding).pinchToChangeGridColumns(true, cols, MIN_GRID_COLUMNS, MAX_GRID_COLUMNS, screenColumns.setColumns)
            ) {
                staggeredItems(filtered, key = { it.id }) { item ->
                    val ratio = if (item.width != null && item.height != null && item.height > 0) {
                        item.width.toFloat() / item.height.toFloat()
                    } else 1f
                    GridThumbnail(
                        uri = item.uri,
                        dateModified = item.dateModified,
                        modifier = Modifier
                            .aspectRatio(ratio)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .zoomSource(item.id)
                            .clickable { onItemClick(item, filtered) }
                    )
                }
            }
        }
    }
}
