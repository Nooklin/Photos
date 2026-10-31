package org.nooklin.photos.ui.hidden


import org.nooklin.photos.ui.common.ScrollToPendingZoomKey

import androidx.compose.runtime.mutableIntStateOf

import androidx.compose.runtime.saveable.rememberSaveable

import org.nooklin.photos.ui.common.zoomSource
import org.nooklin.photos.ui.common.zoomBackdropSource

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.CalendarMonth
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
import org.nooklin.photos.R
import org.nooklin.photos.appString
import androidx.compose.ui.semantics.Role
import org.nooklin.photos.ui.common.touchTarget
import org.nooklin.photos.ui.theme.Spacing
import org.nooklin.photos.ui.theme.AppText
import org.nooklin.photos.ui.common.TouchTargetBox

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
    onOpenCalendar: () -> Unit = {},
    onItemClick: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val previewColumns = gridColumns.coerceAtLeast(2) // the small Home preview never goes to one huge column
    val maxPreviewRows = 3
    // The Hidden Library card scrolls sideways through the whole Hidden library, newest first, like Library Home.
    // Scrolls to a photo the viewer was closed on when it is out of view, so the return zoom lands on its thumbnail.
    val libraryGridState = rememberLazyGridState()
    ScrollToPendingZoomKey(
        lazyIndexOf = { key -> items.indexOfFirst { it.file.absolutePath == key }.takeIf { it >= 0 } },
        scrollToIndex = { i -> libraryGridState.scrollToItem(i) }
    )
    val neededRows = min(
        maxPreviewRows,
        if (items.isEmpty()) 0 else ceil(items.size / previewColumns.toFloat()).toInt()
    )

    val homeAlbums = albums.sortedByDescending { it.itemCount }.take(10)

    var albumsExpanded by remember { mutableStateOf(true) }
    var mediaTypesExpanded by remember { mutableStateOf(true) }
    var utilitiesExpanded by remember { mutableStateOf(true) }

    Scaffold(
        modifier = Modifier.zoomBackdropSource(),
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.common_hidden_section), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
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
                            appString(R.string.common_hidden_library),
                            expanded = true,
                            onTitleClick = onOpenLibrary,
                            showChevron = false,
                            subtitle = appString(R.string.common_items, items.size),
                            titleFontSize = 20.sp
                        ) {}
                        if (neededRows > 0) {
                            BoxWithConstraints(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                val cellSize = maxWidth / previewColumns
                                LazyHorizontalGrid(
                                    rows = GridCells.Fixed(neededRows),
                                    state = libraryGridState,
                                    modifier = Modifier.fillMaxWidth().height(cellSize * neededRows),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    items(items.size, key = { items[it].file.name }) { index ->
                                        AsyncImage(
                                            model = freshImageRequest(
                                                context,
                                                items[index].thumbnailFile,
                                                items[index].thumbnailFile.lastModified()
                                            ),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .aspectRatio(1f)
                                                .padding(1.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .zoomSource(items[index].file.absolutePath)
                                                .clickable(role = Role.Button) { onItemClick(index) }
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
                                appString(R.string.common_hidden_albums),
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
                                            Modifier.width(120.dp).clickable(role = Role.Button) { onOpenAlbumDetail(album.id) }
                                        ) {
                                            HiddenAlbumThumbnail(album.previewFiles, size = 120.dp, fillSlots = album.folderCount >= 2)
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
                        SectionHeader(appString(R.string.common_media_types), mediaTypesExpanded, titleFontSize = 20.sp) {
                            mediaTypesExpanded = !mediaTypesExpanded
                        }
                        if (mediaTypesExpanded) {
                            HiddenCategoryGrid(
                                mapOf(
                                    "VIDEOS" to HiddenCategoryRowSpec(appString(R.string.common_videos), Icons.Filled.Videocam, onOpenVideos),
                                    "SLOW_MOTION" to HiddenCategoryRowSpec(appString(R.string.common_slow_motion), Icons.Filled.SlowMotionVideo, onOpenSlowMotion),
                                    "RAW" to HiddenCategoryRowSpec("RAW", Icons.Filled.RawOn, onOpenRaw),
                                    "SCREENSHOTS" to HiddenCategoryRowSpec(appString(R.string.common_screenshots), Icons.Filled.Screenshot, onOpenScreenshots),
                                    "PANORAMAS" to HiddenCategoryRowSpec(appString(R.string.common_panoramas), Icons.Filled.Panorama, onOpenPanoramas),
                                    "SCREEN_RECORDINGS" to HiddenCategoryRowSpec(appString(R.string.common_screen_recordings), Icons.AutoMirrored.Filled.ScreenShare, onOpenScreenRecordings)
                                ).let { specs -> mediaTypesOrder.mapNotNull { specs[it] } }
                            )
                        }
                    }
                }
            }

            if (HomeSection.UTILITIES !in hiddenSections) item {
                GlassBlock {
                    Column {
                        SectionHeader(appString(R.string.common_utilities), utilitiesExpanded, titleFontSize = 20.sp) {
                            utilitiesExpanded = !utilitiesExpanded
                        }
                        if (utilitiesExpanded) {
                            HiddenCategoryGrid(
                                mapOf(
                                    "FAVORITES" to HiddenCategoryRowSpec(appString(R.string.common_favorites), Icons.Filled.Favorite, onOpenFavorites),
                                    "RECENTLY_DELETED" to HiddenCategoryRowSpec(appString(R.string.common_recently_deleted), Icons.Filled.Delete, onOpenRecentlyDeleted),
                                    "RECENTLY_SAVED" to HiddenCategoryRowSpec(appString(R.string.common_recently_saved), Icons.Filled.History, onOpenRecentlySaved),
                                    "CALENDAR" to HiddenCategoryRowSpec(appString(R.string.home_calendar), Icons.Filled.CalendarMonth, onOpenCalendar)
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
                            .clickable(role = Role.Button) { onOpenCustomize() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(appString(R.string.common_show_or_hide_sections))
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
                    // At least 180dp but wide enough for the whole label, same as Library Home
                    .widthIn(min = 180.dp)
                    .padding(vertical = 8.dp, horizontal = 4.dp)
                    .clickable(role = Role.Button) { row.onClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(row.icon, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                Text(row.label, maxLines = 1, softWrap = false)
            }
        }
    }
}

// Same as HomeScreen's GlassBlock, 2dp horizontal so pills reach the screen edge
@Composable
private fun GlassBlock(content: @Composable () -> Unit) {
    Surface(
        color = GlassPill.adaptiveGlassBackground,
        shape = RoundedCornerShape(20.dp),
        border = GlassPill.adaptiveBorder,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = Spacing.Sm)
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
