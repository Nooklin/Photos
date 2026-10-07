package org.nooklin.photos.ui.hidden

import org.nooklin.photos.ui.common.previewWindowStartRow

import org.nooklin.photos.ui.common.ScrollToPendingZoomKey

import androidx.compose.runtime.mutableIntStateOf

import androidx.compose.runtime.saveable.rememberSaveable

import org.nooklin.photos.ui.common.zoomSource
import org.nooklin.photos.ui.common.zoomBackdropSource

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Panorama
import androidx.compose.material.icons.filled.RawOn
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.nooklin.photos.ui.common.freshImageRequest
import kotlin.math.ceil
import kotlin.math.min
import org.nooklin.photos.data.HiddenAlbumWithPreview
import org.nooklin.photos.data.HiddenItem
import org.nooklin.photos.data.local.HomeItems
import org.nooklin.photos.data.local.HomeSection
import org.nooklin.photos.ui.common.GlassPill

// Landing screen after the gate, mirrors HomeScreen's blocks. Media Types use flags captured at hide-time.
// Duplicates isn't offered, it needs ML data hidden items don't have.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenHomeScreen(
    items: List<HiddenItem>,
    albums: List<HiddenAlbumWithPreview>,
    hiddenAlbumsEnabled: Boolean,
    gridColumns: Int,
    // The Hidden section has its own switched off sections, apart from Home. The tile order is shared with Home.
    hiddenSections: Set<HomeSection> = emptySet(),
    mediaTypesOrder: List<String> = HomeItems.MEDIA_TYPES,
    utilitiesOrder: List<String> = HomeItems.UTILITIES,
    onOpenCustomize: () -> Unit = {},
    onOpenLibrary: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenAlbumDetail: (Long) -> Unit,
    onOpenVideos: () -> Unit,
    onOpenSlowMotion: () -> Unit,
    onOpenRaw: () -> Unit,
    onOpenScreenshots: () -> Unit,
    onOpenPanoramas: () -> Unit,
    onOpenScreenRecordings: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenRecentlyDeleted: () -> Unit,
    onOpenRecentlySaved: () -> Unit,
    onItemClick: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val previewColumns = gridColumns.coerceAtLeast(2) // the small Home preview never goes to one huge column
    val maxPreviewRows = 3
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
    ScrollToPendingZoomKey(
        lazyIndexOf = { key -> items.indexOfFirst { it.file.absolutePath == key }.takeIf { it >= 0 } },
        scrollToIndex = { i -> previewStartRow = previewWindowStartRow(i, items.size, previewColumns, maxPreviewRows) }
    )
    val neededRows = min(
        maxPreviewRows,
        if (previewItems.isEmpty()) 0 else ceil(previewItems.size / previewColumns.toFloat()).toInt()
    )

    val homeAlbums = albums.sortedByDescending { it.itemCount }.take(10)

    var albumsExpanded by remember { mutableStateOf(true) }
    var mediaTypesExpanded by remember { mutableStateOf(true) }
    var utilitiesExpanded by remember { mutableStateOf(true) }

    Scaffold(
        modifier = Modifier.zoomBackdropSource(),
        topBar = {
            TopAppBar(
                title = { Text("Hidden Section", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            item {
                GlassBlock {
                    Column {
                        SectionHeader(
                            "Hidden Library",
                            expanded = true,
                            onTitleClick = onOpenLibrary,
                            showChevron = false,
                            subtitle = "${items.size} items",
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
                                    items(previewItems.size, key = { previewItems[it].file.name }) { index ->
                                        AsyncImage(
                                            model = freshImageRequest(
                                                context,
                                                previewItems[index].thumbnailFile,
                                                previewItems[index].thumbnailFile.lastModified()
                                            ),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .fillMaxWidth()
                                                .padding(1.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .zoomSource(previewItems[index].file.absolutePath)
                                                .clickable { onItemClick(previewStart + index) }
                                        )
                                    }
                                }
                            }
                        }
                        Box(Modifier.height(12.dp).fillMaxWidth()) {}
                    }
                }
            }

            item {
                if (hiddenAlbumsEnabled && HomeSection.ALBUMS !in hiddenSections) {
                    GlassBlock {
                        Column {
                            SectionHeader(
                                "Hidden Albums",
                                albumsExpanded,
                                onTitleClick = onOpenAlbums,
                                titleFontSize = 20.sp
                            ) { albumsExpanded = !albumsExpanded }
                            if (albumsExpanded) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(homeAlbums.size, key = { homeAlbums[it].id }) { index ->
                                        val album = homeAlbums[index]
                                        Column(
                                            Modifier.width(120.dp).clickable { onOpenAlbumDetail(album.id) }
                                        ) {
                                            HiddenAlbumThumbnail(album.previewFiles, size = 120.dp)
                                            Text(album.name, maxLines = 1)
                                            Text("${album.itemCount}", color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (HomeSection.MEDIA_TYPES !in hiddenSections) item {
                GlassBlock {
                    Column {
                        SectionHeader("Media Types", mediaTypesExpanded, titleFontSize = 20.sp) {
                            mediaTypesExpanded = !mediaTypesExpanded
                        }
                        if (mediaTypesExpanded) {
                            HiddenCategoryGrid(
                                mapOf(
                                    "VIDEOS" to HiddenCategoryRowSpec("Videos", Icons.Filled.Videocam, onOpenVideos),
                                    "SLOW_MOTION" to HiddenCategoryRowSpec("Slow Motion", Icons.Filled.SlowMotionVideo, onOpenSlowMotion),
                                    "RAW" to HiddenCategoryRowSpec("RAW", Icons.Filled.RawOn, onOpenRaw),
                                    "SCREENSHOTS" to HiddenCategoryRowSpec("Screenshots", Icons.Filled.Screenshot, onOpenScreenshots),
                                    "PANORAMAS" to HiddenCategoryRowSpec("Panoramas", Icons.Filled.Panorama, onOpenPanoramas),
                                    "SCREEN_RECORDINGS" to HiddenCategoryRowSpec("Screen Recordings", Icons.AutoMirrored.Filled.ScreenShare, onOpenScreenRecordings)
                                ).let { specs -> mediaTypesOrder.mapNotNull { specs[it] } }
                            )
                        }
                    }
                }
            }

            if (HomeSection.UTILITIES !in hiddenSections) item {
                GlassBlock {
                    Column {
                        SectionHeader("Utilities", utilitiesExpanded, titleFontSize = 20.sp) {
                            utilitiesExpanded = !utilitiesExpanded
                        }
                        if (utilitiesExpanded) {
                            HiddenCategoryGrid(
                                mapOf(
                                    "FAVORITES" to HiddenCategoryRowSpec("Favorites", Icons.Filled.Favorite, onOpenFavorites),
                                    "RECENTLY_DELETED" to HiddenCategoryRowSpec("Recently Deleted", Icons.Filled.Delete, onOpenRecentlyDeleted),
                                    "RECENTLY_SAVED" to HiddenCategoryRowSpec("Recently Saved", Icons.Filled.History, onOpenRecentlySaved)
                                ).let { specs -> utilitiesOrder.mapNotNull { specs[it] } }
                            )
                        }
                    }
                }
            }

            // Entry into the screen where Hidden's own sections can be switched off, apart from Home.
            item {
                GlassBlock {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenCustomize() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show or Hide Sections")
                    }
                }
            }

            item { Box(Modifier.height(32.dp).fillMaxWidth()) {} }
        }
    }
}

private data class HiddenCategoryRowSpec(val label: String, val icon: ImageVector, val onClick: () -> Unit)

// Local copy of HomeScreen's CategoryGrid, those composables are private
@Composable
private fun HiddenCategoryGrid(rows: List<HiddenCategoryRowSpec>) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth().height(112.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(rows.size, key = { rows[it].label }) { index ->
            val row = rows[index]
            Row(
                Modifier
                    .width(180.dp)
                    .padding(vertical = 8.dp, horizontal = 4.dp)
                    .clickable { row.onClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(row.icon, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                Text(row.label, maxLines = 1)
            }
        }
    }
}

// Same as HomeScreen's GlassBlock
@Composable
private fun GlassBlock(content: @Composable () -> Unit) {
    Surface(
        color = GlassPill.adaptiveGlassBackground,
        shape = RoundedCornerShape(20.dp),
        border = GlassPill.adaptiveBorder,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        content()
    }
}

// Same as HomeScreen's SectionHeader
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
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier
                    .clickable { onToggle() }
                    .graphicsLayer { rotationZ = if (expanded) 180f else 0f }
            )
        }
    }
}
