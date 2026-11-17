package org.nooklin.photos.ui.home


import org.nooklin.photos.data.local.AlbumOrder
import org.nooklin.photos.data.local.sortedAlbums
import org.nooklin.photos.ui.albums.HomeAlbumRows
import org.nooklin.photos.ui.common.FlatRowTile
import org.nooklin.photos.ui.common.FlatBlock
import org.nooklin.photos.ui.common.TileSpec
import org.nooklin.photos.ui.common.TileRow
import androidx.compose.foundation.lazy.grid.itemsIndexed
import org.nooklin.photos.ui.common.ScrollToPendingZoomKey



import org.nooklin.photos.ui.common.ZoomSources
import org.nooklin.photos.ui.common.zoomSource

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Panorama
import androidx.compose.material.icons.filled.RawOn
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.nooklin.photos.data.MediaCategory
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.data.local.HomeItems
import org.nooklin.photos.data.local.HomeSection
import org.nooklin.photos.ui.albums.AlbumWithPreview
import org.nooklin.photos.ui.common.AlbumThumbnail
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.freshImageRequest
import org.nooklin.photos.ui.common.prefetchScreenTierImage
import org.nooklin.photos.ui.discovered.DiscoveredAlbum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import org.nooklin.photos.R
import org.nooklin.photos.appString
import androidx.compose.ui.semantics.Role
import org.nooklin.photos.ui.theme.AppText
import org.nooklin.photos.ui.common.TouchTargetBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    items: List<MediaItem>,
    gridColumns: Int,
    libraryPreviewRows: Int,
    albums: List<AlbumWithPreview>,
    albumOrder: AlbumOrder,
    discoveredAlbums: List<DiscoveredAlbum>,
    sectionOrder: List<HomeSection>,
    hiddenSections: Set<HomeSection> = emptySet(),
    mediaTypesOrder: List<String> = HomeItems.MEDIA_TYPES,
    utilitiesOrder: List<String> = HomeItems.UTILITIES,
    onSeeFullLibrary: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenAlbumDetail: (Long) -> Unit,
    onOpenDiscovered: () -> Unit,
    onOpenDiscoveredDetail: (String) -> Unit,
    onOpenHidden: () -> Unit,
    onOpenRecentlyDeleted: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReorderHome: () -> Unit,
    onOpenCategory: (MediaCategory) -> Unit,
    onItemClick: (Int, Rect, ImageBitmap?) -> Unit,
    duplicatesMlEnabled: Boolean = false,
    onOpenCalendar: () -> Unit = {}
) {
    val previewColumns = gridColumns.coerceAtLeast(2) // the small Home preview never goes to one huge column

    val homeAlbums = remember(albums, albumOrder) {
        albums.sortedAlbums(albumOrder.sort, albumOrder.manual, { it.id }, { it.name }, { it.itemCount })
    }

    val homeDiscovered = remember(discoveredAlbums) { discoveredAlbums.sortedByDescending { it.itemCount }.take(10) }

    var albumsExpanded by remember { mutableStateOf(true) }
    var discoveredExpanded by remember { mutableStateOf(true) }
    var mediaTypesExpanded by remember { mutableStateOf(true) }
    var utilitiesExpanded by remember { mutableStateOf(true) }
    val backdropGraphicsLayer = rememberGraphicsLayer()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.drawWithContent {
            backdropGraphicsLayer.record { this@drawWithContent.drawContent() }
            drawLayer(backdropGraphicsLayer)
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            for (section in sectionOrder) {
                // Sections switched off in Customize Home are skipped, Library and Settings can not be
                if (section in hiddenSections) continue
                when (section) {
                    HomeSection.LIBRARY -> item {
                        LibraryBlock(
                            libraryItems = items,
                            previewColumns = previewColumns,
                            previewRows = libraryPreviewRows,
                            onSeeFullLibrary = onSeeFullLibrary,
                            backdropGraphicsLayer = backdropGraphicsLayer,
                            coroutineScope = coroutineScope,
                            onItemClick = onItemClick
                        )
                    }
                    HomeSection.ALBUMS -> item {
                        AlbumsBlock(
                            homeAlbums = homeAlbums,
                            rows = albumOrder.homeRows,
                            expanded = albumsExpanded,
                            onToggle = { albumsExpanded = !albumsExpanded },
                            onOpenAlbums = onOpenAlbums,
                            onOpenAlbumDetail = onOpenAlbumDetail
                        )
                    }
                    HomeSection.DISCOVERED -> {
                        if (homeDiscovered.isNotEmpty()) {
                            item {
                                DiscoveredBlock(
                                    homeDiscovered = homeDiscovered,
                                    expanded = discoveredExpanded,
                                    onToggle = { discoveredExpanded = !discoveredExpanded },
                                    onOpenDiscovered = onOpenDiscovered,
                                    onOpenDiscoveredDetail = onOpenDiscoveredDetail
                                )
                            }
                        }
                    }
                    HomeSection.MEDIA_TYPES -> item {
                        MediaTypesBlock(
                            order = mediaTypesOrder,
                            expanded = mediaTypesExpanded,
                            onToggle = { mediaTypesExpanded = !mediaTypesExpanded },
                            onOpenCategory = onOpenCategory
                        )
                    }
                    HomeSection.UTILITIES -> item {
                        UtilitiesBlock(
                            order = utilitiesOrder,
                            expanded = utilitiesExpanded,
                            onToggle = { utilitiesExpanded = !utilitiesExpanded },
                            onOpenHidden = onOpenHidden,
                            onOpenRecentlyDeleted = onOpenRecentlyDeleted,
                            onOpenCategory = onOpenCategory,
                            duplicatesMlEnabled = duplicatesMlEnabled,
                            onOpenCalendar = onOpenCalendar
                        )
                    }
                    HomeSection.SETTINGS -> item {
                        SettingsRowBlock(onOpenSettings = onOpenSettings)
                    }
                }
            }

            item { ReorderHomeRowBlock(onOpenReorderHome = onOpenReorderHome) }

            item { Box(Modifier.height(32.dp).fillMaxWidth()) {} }
        }
    }
}

@Composable
private fun LibraryBlock(
    libraryItems: List<MediaItem>,
    previewColumns: Int,
    previewRows: Int,
    onSeeFullLibrary: () -> Unit,
    backdropGraphicsLayer: GraphicsLayer,
    coroutineScope: CoroutineScope,
    onItemClick: (Int, Rect, ImageBitmap?) -> Unit
) {
    FlatBlock {
        Column {
            SectionHeader(
                appString(R.string.common_library),
                expanded = true,
                onTitleClick = onSeeFullLibrary,
                showChevron = false,
                subtitle = appString(R.string.common_items, libraryItems.size),
                titleFontSize = 20.sp
            ) {}
            val context = LocalContext.current
            val shown = remember(libraryItems, previewRows) { libraryItems.take(30 * previewRows) }
            val rowState = rememberLazyGridState()
            LaunchedEffect(libraryItems.firstOrNull()?.id) { rowState.scrollToItem(0) }
            ScrollToPendingZoomKey(
                lazyIndexOf = { key -> shown.indexOfFirst { it.id == key }.takeIf { it >= 0 } },
                scrollToIndex = { i -> rowState.scrollToItem(i) }
            )
            BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                val cell = (maxWidth - 32.dp) / (previewColumns + 0.5f)
                val rows = previewRows.coerceAtMost(shown.size).coerceAtLeast(1)
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(rows),
                    state = rowState,
                    contentPadding = PaddingValues(horizontal = 15.dp),
                    modifier = Modifier.fillMaxWidth().height(cell * rows)
                ) {
                    itemsIndexed(shown, key = { _, item -> item.id }) { index, item ->
                        Box(
                            Modifier
                                .width(cell)
                                .fillMaxHeight()
                                .padding(1.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .zoomSource(item.id)
                                .clickable(role = Role.Button) {
                                    prefetchScreenTierImage(context, item)
                                    coroutineScope.launch {
                                        val backdrop = runCatching { backdropGraphicsLayer.toImageBitmap() }.getOrNull()
                                        onItemClick(index, (ZoomSources.boundsOf(item.id) ?: Rect.Zero), backdrop)
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = freshImageRequest(context, item.uri, item.dateModified),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (item.isFavorite) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            Text(
                appString(R.string.home_see_all),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onSeeFullLibrary)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AlbumsBlock(
    homeAlbums: List<AlbumWithPreview>,
    rows: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenAlbumDetail: (Long) -> Unit
) {
    FlatBlock {
        Column {
            SectionHeader(
                appString(R.string.common_albums),
                expanded,
                onTitleClick = onOpenAlbums,
                titleFontSize = 20.sp
            ) { onToggle() }
            if (expanded) {
                HomeAlbumRows(homeAlbums, rows, key = { it.id }) { album ->
                    Column(
                        Modifier.width(136.dp).clickable(role = Role.Button) { onOpenAlbumDetail(album.id) }
                    ) {
                        AlbumThumbnail(album.previewItems, size = 136.dp, fillSlots = album.folderCount >= 2, corner = 18.dp)
                        Text(album.name, maxLines = 1, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                        Text(appString(R.string.common_items, album.itemCount), color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveredBlock(
    homeDiscovered: List<DiscoveredAlbum>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenDiscovered: () -> Unit,
    onOpenDiscoveredDetail: (String) -> Unit
) {
    FlatBlock {
        Column {
            SectionHeader(
                appString(R.string.common_discovered),
                expanded,
                onTitleClick = onOpenDiscovered,
                titleFontSize = 20.sp
            ) { onToggle() }
            if (expanded) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(homeDiscovered.size, key = { homeDiscovered[it].tagId }) { index ->
                        val album = homeDiscovered[index]
                        Column(
                            Modifier.width(136.dp).clickable(role = Role.Button) { onOpenDiscoveredDetail(album.tagId) }
                        ) {
                            AlbumThumbnail(album.previewItems, size = 136.dp, corner = 18.dp)
                            Text(album.label, maxLines = 1, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                            Text(appString(R.string.common_items, album.itemCount), color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaTypesBlock(
    order: List<String>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenCategory: (MediaCategory) -> Unit
) {
    FlatBlock {
        Column {
            SectionHeader(appString(R.string.common_media_types), expanded, titleFontSize = 20.sp) { onToggle() }
            if (expanded) {
                val icons = mapOf(
                    MediaCategory.VIDEOS to Icons.Filled.Videocam,
                    MediaCategory.PANORAMAS to Icons.Filled.Panorama,
                    MediaCategory.SCREENSHOTS to Icons.Filled.Screenshot,
                    MediaCategory.SCREEN_RECORDINGS to Icons.AutoMirrored.Filled.ScreenShare,
                    MediaCategory.SLOW_MOTION to Icons.Filled.SlowMotionVideo,
                    MediaCategory.RAW to Icons.Filled.RawOn
                )
                val mediaTypeEntries = order.mapNotNull { key ->
                    icons.entries.find { it.key.name == key }?.toPair()
                }
                CategoryGrid(mediaTypeEntries, onOpenCategory)
            }
        }
    }
}

@Composable
private fun UtilitiesBlock(
    order: List<String>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenHidden: () -> Unit,
    onOpenRecentlyDeleted: () -> Unit,
    onOpenCategory: (MediaCategory) -> Unit,
    duplicatesMlEnabled: Boolean,
    onOpenCalendar: () -> Unit
) {
    FlatBlock {
        Column {
            SectionHeader(appString(R.string.common_utilities), expanded, titleFontSize = 20.sp) { onToggle() }
            if (expanded) {
                UtilitiesGrid(
                    order = order,
                    onOpenHidden = onOpenHidden,
                    onOpenRecentlyDeleted = onOpenRecentlyDeleted,
                    onOpenCategory = onOpenCategory,
                    duplicatesMlEnabled = duplicatesMlEnabled,
                    onOpenCalendar = onOpenCalendar
                )
            }
        }
    }
}

@Composable
private fun SettingsRowBlock(onOpenSettings: () -> Unit) {
    FlatRowTile(appString(R.string.common_settings), Icons.Filled.Settings, onOpenSettings)
}

/** Bottom-of-Home entry point into ReorderHomeScreen, always last regardless of sectionOrder. */
@Composable
private fun ReorderHomeRowBlock(onOpenReorderHome: () -> Unit) {
    FlatRowTile(appString(R.string.common_reorder_home_screen), Icons.Filled.Reorder, onOpenReorderHome)
}

@Composable
private fun SectionHeader(
    title: String,
    expanded: Boolean,
    onTitleClick: (() -> Unit)? = null,
    subtitle: String? = null,
    showChevron: Boolean = true,
    titleFontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    onToggle: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .let { if (onTitleClick != null) it.clickable(role = Role.Button) { onTitleClick() } else it }
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = titleFontSize)
            if (subtitle != null) {
                Text(subtitle, color = Color.Gray, fontSize = AppText.Body)
            }
        }
        if (showChevron) {
            TouchTargetBox(onClick = { onToggle() }) { feedback ->
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GlassPill.adaptiveGlassBackground)
                        .then(feedback)
                        .border(GlassPill.adaptiveBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) appString(R.string.common_collapse) else appString(R.string.common_expand),
                        modifier = Modifier.graphicsLayer { rotationZ = if (expanded) 180f else 0f }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryGrid(entries: List<Pair<MediaCategory, ImageVector>>, onClick: (MediaCategory) -> Unit) {
    CategoryList(entries.map { (category, icon) -> CategoryItem(category.label, icon) { onClick(category) } })
}

@Composable
private fun UtilitiesGrid(
    order: List<String>,
    onOpenHidden: () -> Unit,
    onOpenRecentlyDeleted: () -> Unit,
    onOpenCategory: (MediaCategory) -> Unit,
    duplicatesMlEnabled: Boolean,
    onOpenCalendar: () -> Unit
) {
    val rows = mutableListOf<CategoryItem>()
    for (key in order) {
        when (key) {
            "FAVORITES" -> rows += CategoryItem(MediaCategory.FAVORITES.label, Icons.Filled.Favorite) { onOpenCategory(MediaCategory.FAVORITES) }
            "HIDDEN" -> rows += CategoryItem(appString(R.string.common_hidden_section), Icons.Filled.Lock, onOpenHidden)
            "RECENTLY_SAVED" -> rows += CategoryItem(MediaCategory.RECENTLY_SAVED.label, Icons.Filled.History) { onOpenCategory(MediaCategory.RECENTLY_SAVED) }
            "RECENTLY_DELETED" -> rows += CategoryItem(appString(R.string.common_recently_deleted), Icons.Filled.Delete, onOpenRecentlyDeleted)
            "CALENDAR" -> rows += CategoryItem(appString(R.string.home_calendar), Icons.Filled.CalendarMonth, onOpenCalendar)
            "DUPLICATES" -> if (duplicatesMlEnabled) {
                rows += CategoryItem(MediaCategory.DUPLICATES.label, Icons.Filled.ContentCopy) { onOpenCategory(MediaCategory.DUPLICATES) }
            }
        }
    }
    CategoryList(rows)
}

private data class CategoryItem(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
private fun CategoryList(rows: List<CategoryItem>) {
    TileRow(rows.map { TileSpec(it.label, it.icon, it.onClick) })
}
