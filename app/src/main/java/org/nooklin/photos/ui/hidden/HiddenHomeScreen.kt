package org.nooklin.photos.ui.hidden


import org.nooklin.photos.ui.common.FlatRowTile
import org.nooklin.photos.ui.common.FlatBlock
import org.nooklin.photos.ui.common.TileSpec
import org.nooklin.photos.ui.common.TileRow



import org.nooklin.photos.ui.common.zoomSource
import org.nooklin.photos.ui.common.zoomBackdropSource

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import org.nooklin.photos.ui.theme.Spacing
import org.nooklin.photos.ui.theme.AppText
import org.nooklin.photos.ui.common.TouchTargetBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenHomeScreen(
    items: List<HiddenItem>,
    albums: List<HiddenAlbumWithPreview>,
    hiddenAlbumsEnabled: Boolean,
    gridColumns: Int,
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
                FlatBlock {
                    Column {
                        SectionHeader(
                            appString(R.string.common_hidden_library),
                            expanded = true,
                            onTitleClick = onOpenLibrary,
                            showChevron = false,
                            subtitle = appString(R.string.common_items, items.size),
                            titleFontSize = 20.sp
                        ) {}
                        Column(Modifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp, top = 6.dp)) {
                            for (row in 0 until neededRows) {
                                Row(Modifier.fillMaxWidth()) {
                                    for (col in 0 until previewColumns) {
                                        val index = row * previewColumns + col
                                        val item = items.getOrNull(index)
                                        if (item == null) {
                                            Spacer(Modifier.weight(1f))
                                            continue
                                        }
                                        Box(
                                            Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(1.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .zoomSource(item.file.absolutePath)
                                                .clickable(role = Role.Button) { onItemClick(index) }
                                        ) {
                                            AsyncImage(
                                                model = freshImageRequest(
                                                    context,
                                                    item.thumbnailFile,
                                                    item.thumbnailFile.lastModified()
                                                ),
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
                        }
                        Text(
                            appString(R.string.home_see_all),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button, onClick = onOpenLibrary)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item {
                if (hiddenAlbumsEnabled && HomeSection.ALBUMS !in hiddenSections) {
                    FlatBlock {
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(homeAlbums.size, key = { homeAlbums[it].id }) { index ->
                                        val album = homeAlbums[index]
                                        Column(
                                            Modifier.width(136.dp).clickable(role = Role.Button) { onOpenAlbumDetail(album.id) }
                                        ) {
                                            HiddenAlbumThumbnail(album.previewFiles, size = 136.dp, fillSlots = album.folderCount >= 2, corner = 18.dp)
                                            Text(album.name, maxLines = 1, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                                            Text(appString(R.string.common_items, album.itemCount), color = Color.Gray, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (HomeSection.MEDIA_TYPES !in hiddenSections) item {
                FlatBlock {
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
                FlatBlock {
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

            item { FlatRowTile(appString(R.string.common_show_or_hide_sections), null, onOpenCustomize) }

            item { Box(Modifier.height(32.dp).fillMaxWidth()) {} }
        }
    }
}

private data class HiddenCategoryRowSpec(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
private fun HiddenCategoryGrid(rows: List<HiddenCategoryRowSpec>) {
    TileRow(rows.map { TileSpec(it.label, it.icon, it.onClick) })
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
