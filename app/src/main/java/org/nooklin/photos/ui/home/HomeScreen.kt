package org.nooklin.photos.ui.home

import org.nooklin.photos.ui.common.previewWindowStartRow

import org.nooklin.photos.ui.common.ScrollToPendingZoomKey

import androidx.compose.runtime.mutableIntStateOf

import androidx.compose.runtime.saveable.rememberSaveable

import org.nooklin.photos.ui.common.ZoomSources
import org.nooklin.photos.ui.common.zoomSource

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Surface
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
import kotlin.math.ceil
import kotlin.math.min
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
import androidx.compose.foundation.layout.widthIn

// Single scroll home, no bottom nav. Sections are collapsible glass cards (translucent, no real backdrop blur).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    items: List<MediaItem>,
    gridColumns: Int,
    libraryPreviewRows: Int,
    albums: List<AlbumWithPreview>,
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
    duplicatesMlEnabled: Boolean = false
) {
    val previewColumns = gridColumns.coerceAtLeast(2) // the small Home preview never goes to one huge column
    // Home Library Preview Rows setting
    val maxPreviewRows = libraryPreviewRows
    // First preview row shown. Normally 0 (newest photos). When the viewer is
    // exited on a photo outside the preview, the preview follows to that part
    // of the library with the photo in its middle row, so the return zoom
    // always lands on the photo's own thumbnail. Kept for the session.
    var previewStartRow by rememberSaveable { mutableIntStateOf(0) }
    val previewStart = previewStartRow.coerceIn(0, previewWindowStartRow(Int.MAX_VALUE / 2, items.size, previewColumns, maxPreviewRows)) * previewColumns
    val previewItems = remember(items, previewStart, previewColumns, maxPreviewRows) {
        val from = previewStart.coerceIn(0, items.size)
        items.subList(from, minOf(items.size, from + previewColumns * maxPreviewRows))
    }
    // The window only follows a photo so the zoom back from the viewer lands on its thumbnail. Once that animation
    // is over (or when Home is opened again) it goes back to the newest photos.
    val returnOverlay = ZoomSources.returnOverlay
    LaunchedEffect(returnOverlay) {
        if (returnOverlay == null && previewStartRow != 0) previewStartRow = 0
    }
    ScrollToPendingZoomKey(
        lazyIndexOf = { key -> items.indexOfFirst { it.id == key }.takeIf { it >= 0 } },
        scrollToIndex = { i -> previewStartRow = previewWindowStartRow(i, items.size, previewColumns, maxPreviewRows) }
    )
    val neededRows = min(
        maxPreviewRows,
        if (previewItems.isEmpty()) 0 else ceil(previewItems.size / previewColumns.toFloat()).toInt()
    )

    // Top 10 by item count, the full list is in AlbumsScreen
    val homeAlbums = albums.sortedByDescending { it.itemCount }.take(10)

    val homeDiscovered = discoveredAlbums.sortedByDescending { it.itemCount }.take(10)

    var albumsExpanded by remember { mutableStateOf(true) }
    var discoveredExpanded by remember { mutableStateOf(true) }
    var mediaTypesExpanded by remember { mutableStateOf(true) }
    var utilitiesExpanded by remember { mutableStateOf(true) }
    // One-shot backdrop capture, see GalleryScreen
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
                // Sections switched off in Reorder Home Screen are skipped, Library and Settings can not be.
                if (section in hiddenSections) continue
                when (section) {
                    HomeSection.LIBRARY -> item {
                        LibraryBlock(
                            itemCount = items.size,
                            previewItems = previewItems,
                            previewColumns = previewColumns,
                            neededRows = neededRows,
                            onSeeFullLibrary = onSeeFullLibrary,
                            backdropGraphicsLayer = backdropGraphicsLayer,
                            coroutineScope = coroutineScope,
                            // Preview index -> library index (preview may be shifted).
                            onItemClick = { i, bounds, backdrop -> onItemClick(previewStart + i, bounds, backdrop) }
                        )
                    }
                    HomeSection.ALBUMS -> item {
                        AlbumsBlock(
                            homeAlbums = homeAlbums,
                            expanded = albumsExpanded,
                            onToggle = { albumsExpanded = !albumsExpanded },
                            onOpenAlbums = onOpenAlbums,
                            onOpenAlbumDetail = onOpenAlbumDetail
                        )
                    }
                    HomeSection.DISCOVERED -> {
                        // Only shown once something is detected, an empty section is clutter
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
                            duplicatesMlEnabled = duplicatesMlEnabled
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
    itemCount: Int,
    previewItems: List<MediaItem>,
    previewColumns: Int,
    neededRows: Int,
    onSeeFullLibrary: () -> Unit,
    backdropGraphicsLayer: GraphicsLayer,
    coroutineScope: CoroutineScope,
    onItemClick: (Int, Rect, ImageBitmap?) -> Unit
) {
    GlassBlock {
        Column {
            SectionHeader(
                "Library",
                expanded = true,
                onTitleClick = onSeeFullLibrary,
                showChevron = false,
                subtitle = "$itemCount items",
                titleFontSize = 20.sp
            ) {}
            if (neededRows > 0) {
                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    val cellSize = maxWidth / previewColumns
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(previewColumns),
                        modifier = Modifier.fillMaxWidth().height(cellSize * neededRows),
                        userScrollEnabled = false,
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        items(previewItems.size, key = { previewItems[it].id }) { index ->
                            val context = LocalContext.current
                            AsyncImage(
                                model = freshImageRequest(context, previewItems[index].uri, previewItems[index].dateModified),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .fillMaxWidth()
                                    .padding(1.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .zoomSource(previewItems[index].id)
                                    .clickable {
                                        prefetchScreenTierImage(context, previewItems[index])
                                        coroutineScope.launch {
                                            val backdrop = runCatching { backdropGraphicsLayer.toImageBitmap() }.getOrNull()
                                            onItemClick(index, (ZoomSources.boundsOf(previewItems[index].id) ?: Rect.Zero), backdrop)
                                        }
                                    }
                            )
                        }
                    }
                }
            }
            Box(Modifier.height(12.dp).fillMaxWidth()) {}
        }
    }
}

@Composable
private fun AlbumsBlock(
    homeAlbums: List<AlbumWithPreview>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenAlbumDetail: (Long) -> Unit
) {
    GlassBlock {
        Column {
            SectionHeader(
                "Albums",
                expanded,
                onTitleClick = onOpenAlbums,
                titleFontSize = 20.sp
            ) { onToggle() }
            if (expanded) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(homeAlbums.size, key = { homeAlbums[it].id }) { index ->
                        val album = homeAlbums[index]
                        Column(
                            Modifier.width(120.dp).clickable { onOpenAlbumDetail(album.id) }
                        ) {
                            AlbumThumbnail(album.previewItems, size = 120.dp)
                            Text(album.name, maxLines = 1)
                            Text("${album.itemCount}", color = Color.Gray)
                        }
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
    GlassBlock {
        Column {
            SectionHeader(
                "Discovered",
                expanded,
                onTitleClick = onOpenDiscovered,
                titleFontSize = 20.sp
            ) { onToggle() }
            if (expanded) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(homeDiscovered.size, key = { homeDiscovered[it].tagId }) { index ->
                        val album = homeDiscovered[index]
                        Column(
                            Modifier.width(120.dp).clickable { onOpenDiscoveredDetail(album.tagId) }
                        ) {
                            AlbumThumbnail(album.previewItems, size = 120.dp)
                            Text(album.label, maxLines = 1)
                            Text("${album.itemCount}", color = Color.Gray)
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
    GlassBlock {
        Column {
            SectionHeader("Media Types", expanded, titleFontSize = 20.sp) { onToggle() }
            if (expanded) {
                val icons = mapOf(
                    MediaCategory.VIDEOS to Icons.Filled.Videocam,
                    MediaCategory.PANORAMAS to Icons.Filled.Panorama,
                    MediaCategory.SCREENSHOTS to Icons.Filled.Screenshot,
                    MediaCategory.SCREEN_RECORDINGS to Icons.AutoMirrored.Filled.ScreenShare,
                    MediaCategory.SLOW_MOTION to Icons.Filled.SlowMotionVideo,
                    MediaCategory.RAW to Icons.Filled.RawOn
                )
                // In the order chosen in Reorder Home Screen.
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
    duplicatesMlEnabled: Boolean
) {
    GlassBlock {
        Column {
            SectionHeader("Utilities", expanded, titleFontSize = 20.sp) { onToggle() }
            if (expanded) {
                UtilitiesGrid(
                    order = order,
                    onOpenHidden = onOpenHidden,
                    onOpenRecentlyDeleted = onOpenRecentlyDeleted,
                    onOpenCategory = onOpenCategory,
                    duplicatesMlEnabled = duplicatesMlEnabled
                )
            }
        }
    }
}

@Composable
private fun SettingsRowBlock(onOpenSettings: () -> Unit) {
    GlassBlock {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clickable { onOpenSettings() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.padding(end = 16.dp))
            Text("Settings")
        }
    }
}

/** Bottom-of-Home entry point into ReorderHomeScreen, always last regardless of sectionOrder. */
@Composable
private fun ReorderHomeRowBlock(onOpenReorderHome: () -> Unit) {
    GlassBlock {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clickable { onOpenReorderHome() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Reorder, contentDescription = null, modifier = Modifier.padding(end = 16.dp))
            Text("Reorder Home Screen")
        }
    }
}

// Translucent surface with a subtle border (iOS 26 look). A radial glow read as a dot, so it was dropped.
@Composable
private fun GlassBlock(content: @Composable () -> Unit) {
    Surface(
        color = GlassPill.adaptiveGlassBackground,
        shape = RoundedCornerShape(20.dp),
        border = GlassPill.adaptiveBorder,
        modifier = Modifier
            .fillMaxWidth()
            // 4dp horizontal so pills reach the screen edge, one shared value for every Home section
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        content()
    }
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
                .let { if (onTitleClick != null) it.clickable { onTitleClick() } else it }
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = titleFontSize)
            if (subtitle != null) {
                Text(subtitle, color = Color.Gray, fontSize = 13.sp)
            }
        }
        if (showChevron) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(GlassPill.adaptiveGlassBackground)
                    .border(GlassPill.adaptiveBorder, CircleShape)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.graphicsLayer { rotationZ = if (expanded) 180f else 0f }
                )
            }
        }
    }
}

/** Max 2 rows tall, continues into new columns to the right, scrolls horizontally. */
@Composable
private fun CategoryGrid(entries: List<Pair<MediaCategory, ImageVector>>, onClick: (MediaCategory) -> Unit) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth().height(112.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(entries.size, key = { entries[it].first.name }) { index ->
            val (category, icon) = entries[index]
            CategoryRow(category.label, icon) { onClick(category) }
        }
    }
}

@Composable
private fun UtilitiesGrid(
    order: List<String>,
    onOpenHidden: () -> Unit,
    onOpenRecentlyDeleted: () -> Unit,
    onOpenCategory: (MediaCategory) -> Unit,
    duplicatesMlEnabled: Boolean
) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth().height(112.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        // In the order chosen in Reorder Home Screen. Fills column by column, so with the default order Favorites
        // and Hidden Section sit on top of Recently Saved and Recently Deleted.
        for (key in order) {
            when (key) {
                "FAVORITES" -> item { CategoryRow(MediaCategory.FAVORITES.label, Icons.Filled.Favorite) { onOpenCategory(MediaCategory.FAVORITES) } }
                "HIDDEN" -> item { CategoryRow("Hidden Section", Icons.Filled.Lock, onOpenHidden) }
                "RECENTLY_SAVED" -> item { CategoryRow(MediaCategory.RECENTLY_SAVED.label, Icons.Filled.History) { onOpenCategory(MediaCategory.RECENTLY_SAVED) } }
                "RECENTLY_DELETED" -> item { CategoryRow("Recently Deleted", Icons.Filled.Delete, onOpenRecentlyDeleted) }
                // Only when Machine Learning > Duplicates is on, real perceptual-hash comparison lives behind it.
                "DUPLICATES" -> if (duplicatesMlEnabled) {
                    item { CategoryRow(MediaCategory.DUPLICATES.label, Icons.Filled.ContentCopy) { onOpenCategory(MediaCategory.DUPLICATES) } }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        // At least 180dp, but wide enough for the whole label: a fixed width wrapped "Recently Deleted" and showed only
        // "Recently".
        Modifier
            .widthIn(min = 180.dp)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
        Text(label, maxLines = 1, softWrap = false)
    }
}
