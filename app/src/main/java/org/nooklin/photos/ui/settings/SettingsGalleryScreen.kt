package org.nooklin.photos.ui.settings

import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.nooklin.photos.data.local.GridStyle
import org.nooklin.photos.data.local.GroupBy
import org.nooklin.photos.data.local.MAX_GRID_COLUMNS
import org.nooklin.photos.data.local.MAX_LIBRARY_PREVIEW_ROWS
import org.nooklin.photos.data.local.MIN_GRID_COLUMNS
import org.nooklin.photos.data.local.MIN_LIBRARY_PREVIEW_ROWS
import org.nooklin.photos.R
import org.nooklin.photos.appString

// Group by only applies in Square mode, the staggered grid has no section header
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsGalleryScreen(
    gridStyle: GridStyle,
    onGridStyleChange: (GridStyle) -> Unit,
    gridColumns: Int,
    onGridColumnsChange: (Int) -> Unit,
    groupBy: GroupBy,
    onGroupByChange: (GroupBy) -> Unit,
    libraryPreviewRows: Int,
    onLibraryPreviewRowsChange: (Int) -> Unit,
    showScreenshotsInLibrary: Boolean,
    onShowScreenshotsInLibraryChange: (Boolean) -> Unit,
    hiddenAutoAlbums: Set<String>,
    onAutoAlbumHiddenChange: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.common_gallery), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SettingsHeading(appString(R.string.settings_head_grid))
            SettingsGroup(
                null,
                listOf<@Composable () -> Unit>(
                    {
                        SettingsChoiceRow(
                            appString(R.string.settings_gallery_photo_grid_size),
                            (MIN_GRID_COLUMNS..MAX_GRID_COLUMNS).map { it to "$it" },
                            gridColumns
                        ) { onGridColumnsChange(it) }
                    },
                    {
                        SettingsChoiceRow(
                            appString(R.string.settings_gallery_ratio),
                            listOf(
                                GridStyle.SQUARE to appString(R.string.settings_gallery_square),
                                GridStyle.ORIGINAL to appString(R.string.settings_gallery_aspect)
                            ),
                            gridStyle
                        ) { onGridStyleChange(it) }
                    },
                    {
                        SettingsChoiceRow(
                            appString(R.string.settings_gallery_group_by),
                            listOf(
                                GroupBy.NONE to appString(R.string.settings_gallery_none),
                                GroupBy.DAY to appString(R.string.settings_gallery_day),
                                GroupBy.WEEK to appString(R.string.settings_gallery_week),
                                GroupBy.MONTH to appString(R.string.settings_gallery_month),
                                GroupBy.YEAR to appString(R.string.settings_gallery_year)
                            ),
                            groupBy
                        ) { onGroupByChange(it) }
                    }
                )
            )
            SettingsHint(appString(R.string.settings_gallery_applies_to_the_full_library))

            SettingsHeading(appString(R.string.settings_head_home))
            SettingsGroup(
                null,
                listOf<@Composable () -> Unit>({
                    SettingsChoiceRow(
                        appString(R.string.settings_gallery_home_library_preview_rows),
                        (MIN_LIBRARY_PREVIEW_ROWS..MAX_LIBRARY_PREVIEW_ROWS).map { it to "$it" },
                        libraryPreviewRows
                    ) { onLibraryPreviewRowsChange(it) }
                })
            )
            SettingsHint(appString(R.string.settings_gallery_how_many_rows_of_thumbnails))

            SettingsHeading(appString(R.string.common_albums))
            AlbumHomeSettings(
                hidden = false,
                extra = listOf("camera" to "Camera", "screenshots" to "Screenshots", "downloads" to "Downloads", "whatsapp" to "WhatsApp").map { (rule, name) ->
                    {
                        SettingsToggleRow(
                            label = name,
                            checked = rule !in hiddenAutoAlbums,
                            icon = Icons.Filled.PhotoAlbum,
                            onCheckedChange = { show -> onAutoAlbumHiddenChange(rule, !show) }
                        )
                    }
                }
            )
            SettingsHint(appString(R.string.settings_auto_albums_hint))

            SettingsHeading(appString(R.string.settings_head_library))
            SettingsGroup(
                null,
                listOf<@Composable () -> Unit>({
                    SettingsToggleRow(
                        label = appString(R.string.settings_gallery_show_screenshots),
                        checked = showScreenshotsInLibrary,
                        icon = Icons.Filled.Screenshot,
                        onCheckedChange = onShowScreenshotsInLibraryChange
                    )
                })
            )
            SettingsHint(appString(R.string.settings_gallery_show_screenshots_hint))
        }
    }
}
