package org.nooklin.photos.ui.category

import org.nooklin.photos.ui.common.rememberGroupSelection
import org.nooklin.photos.ui.common.groupSelectionTitle
import org.nooklin.photos.ui.common.GroupSelectionActions
import org.nooklin.photos.ui.common.GroupSelectableCell
import org.nooklin.photos.ui.common.GroupActions
import org.nooklin.photos.ui.common.scrollToItemCentered

import org.nooklin.photos.ui.common.ScrollToPendingZoomKey
import org.nooklin.photos.ui.common.zoomSource
import org.nooklin.photos.ui.common.zoomBackdropSource
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState

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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import org.nooklin.photos.ui.albums.AlbumHero
import org.nooklin.photos.ui.albums.CoverPickerDialog
import org.nooklin.photos.ui.albums.SlideshowDialog
import org.nooklin.photos.ui.albums.SlideshowPhoto
import org.nooklin.photos.ui.common.CircleIconButton
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.MenuIcon
import org.nooklin.photos.ui.common.heroImageRequest
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    category: MediaCategory,
    allItems: List<MediaItem>,
    gridStyleSetting: GridStyle,
    gridColumns: Int,
    duplicateMediaIds: Set<Long> = emptySet(),
    groupActions: GroupActions? = null,
    onBack: () -> Unit,
    // Favorites only: the chosen big photo at the top (null: the newest favorite)
    heroCoverId: Long? = null,
    onSetHeroCover: (Long?) -> Unit = {},
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

    val selection = rememberGroupSelection()
    // Favorites has the same top as an album: a big photo with the title, the count and a play button
    val hasHero = category == MediaCategory.FAVORITES && filtered.isNotEmpty()
    val heroItem = if (hasHero) (filtered.firstOrNull { it.id == heroCoverId } ?: filtered.first()) else null
    val headerCount = if (hasHero) 1 else 0
    var showSlideshow by remember { mutableStateOf(false) }
    var showCoverPicker by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val squareGridState = rememberLazyGridState()
    val staggeredGridState = rememberLazyStaggeredGridState()
    ScrollToPendingZoomKey(
        lazyIndexOf = { key -> filtered.indexOfFirst { it.id == key }.takeIf { it >= 0 }?.plus(headerCount) },
        scrollToIndex = { i -> if (gridStyle == GridStyle.SQUARE) squareGridState.scrollToItemCentered(i) else staggeredGridState.scrollToItemCentered(i) }
    )

    Scaffold(
        modifier = Modifier.zoomBackdropSource(),
        topBar = {
            TopAppBar(
                // With the big photo the bar is see-through, the title is on the photo
                colors = if (hasHero) {
                    TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent)
                } else {
                    TopAppBarDefaults.topAppBarColors()
                },
                title = { if (!hasHero) Text(groupSelectionTitle(selection, category.label), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (hasHero) {
                        CircleIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = appString(R.string.common_back),
                            onClick = onBack
                        )
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                        }
                    }
                },
                actions = {
                    if (groupActions != null && filtered.isNotEmpty()) GroupSelectionActions(selection, filtered, groupActions)
                    if (hasHero && !selection.isSelecting) {
                        Box {
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
                                        text = { Text(appString(R.string.album_set_cover)) },
                                        leadingIcon = { MenuIcon(Icons.Filled.Image) },
                                        onClick = {
                                            menuExpanded = false
                                            showCoverPicker = true
                                        }
                                    )
                                }
                            }
                        }
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
                EmptyState(message = appString(R.string.category_nothing_here_yet), actionLabel = appString(R.string.common_back), onAction = onBack)
            }
            return@Scaffold
        }

        if (gridStyle == GridStyle.SQUARE) {
            LazyVerticalGrid(
                state = squareGridState,
                columns = GridCells.Fixed(cols),
                contentPadding = if (hasHero) PaddingValues(bottom = 2.dp) else PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(if (hasHero) 2.dp else 4.dp),
                verticalArrangement = Arrangement.spacedBy(if (hasHero) 2.dp else 4.dp),
                // The big photo goes under the see-through bar, so the top padding is left out
                modifier = (if (hasHero) Modifier.padding(bottom = padding.calculateBottomPadding()) else Modifier.padding(padding))
                    .pinchToChangeGridColumns(true, cols, MIN_GRID_COLUMNS, MAX_GRID_COLUMNS, screenColumns.setColumns)
            ) {
                if (heroItem != null) {
                    item(key = "hero", span = { GridItemSpan(maxLineSpan) }) { FavoritesHero(heroItem, selection, category.label, filtered.size) { showSlideshow = true } }
                }
                items(filtered.size, key = { filtered[it].id }) { index ->
                    val item = filtered[index]
                    GroupSelectableCell(
                        selection = if (groupActions != null) selection else null,
                        id = item.id,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .zoomSource(item.id),
                        onClick = { onItemClick(item, filtered) }
                    ) {
                        GridThumbnail(uri = item.uri, dateModified = item.dateModified, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        } else {
            LazyVerticalStaggeredGrid(
                state = staggeredGridState,
                columns = StaggeredGridCells.Fixed(cols),
                contentPadding = if (hasHero) PaddingValues(bottom = 2.dp) else PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(if (hasHero) 2.dp else 4.dp),
                verticalItemSpacing = if (hasHero) 2.dp else 4.dp,
                modifier = (if (hasHero) Modifier.padding(bottom = padding.calculateBottomPadding()) else Modifier.padding(padding))
                    .pinchToChangeGridColumns(true, cols, MIN_GRID_COLUMNS, MAX_GRID_COLUMNS, screenColumns.setColumns)
            ) {
                if (heroItem != null) {
                    item(key = "hero", span = StaggeredGridItemSpan.FullLine) { FavoritesHero(heroItem, selection, category.label, filtered.size) { showSlideshow = true } }
                }
                staggeredItems(filtered, key = { it.id }) { item ->
                    val ratio = if (item.width != null && item.height != null && item.height > 0) {
                        item.width.toFloat() / item.height.toFloat()
                    } else 1f
                    GroupSelectableCell(
                        selection = if (groupActions != null) selection else null,
                        id = item.id,
                        modifier = Modifier
                            .aspectRatio(ratio)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .zoomSource(item.id),
                        onClick = { onItemClick(item, filtered) }
                    ) {
                        GridThumbnail(uri = item.uri, dateModified = item.dateModified, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    if (showSlideshow) {
        SlideshowDialog(
            photos = filtered.filter { !it.isVideo }.map {
                SlideshowPhoto(
                    model = it.uri,
                    thumbnail = it.uri,
                    freshness = it.dateModified,
                    aspect = if (it.width != null && it.height != null && it.height > 0) it.width.toFloat() / it.height.toFloat() else null
                )
            },
            title = category.label,
            secure = false,
            onClose = { showSlideshow = false }
        )
    }

    if (showCoverPicker) {
        CoverPickerDialog(
            count = filtered.size,
            key = { filtered[it].id },
            thumbnail = { index ->
                GridThumbnail(uri = filtered[index].uri, dateModified = filtered[index].dateModified, modifier = Modifier.fillMaxSize())
            },
            onPick = { index ->
                onSetHeroCover(filtered[index].id)
                showCoverPicker = false
            },
            onUseNewest = {
                onSetHeroCover(null)
                showCoverPicker = false
            },
            onDismiss = { showCoverPicker = false }
        )
    }
}

@Composable
private fun FavoritesHero(
    heroItem: MediaItem,
    selection: org.nooklin.photos.ui.common.GroupSelection,
    label: String,
    count: Int,
    onPlay: () -> Unit
) {
    val context = LocalContext.current
    AlbumHero(
        title = groupSelectionTitle(selection, label),
        itemCount = count,
        showPlay = true,
        onPlay = onPlay,
        image = {
            AsyncImage(
                model = heroImageRequest(context, heroItem.uri, heroItem.dateModified),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    )
}
