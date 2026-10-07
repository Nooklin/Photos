package org.nooklin.photos.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.nooklin.photos.data.local.GridStyle
import org.nooklin.photos.data.local.GroupBy
import org.nooklin.photos.data.local.MAX_GRID_COLUMNS
import org.nooklin.photos.data.local.MAX_LIBRARY_PREVIEW_ROWS
import org.nooklin.photos.data.local.MIN_GRID_COLUMNS
import org.nooklin.photos.data.local.MIN_LIBRARY_PREVIEW_ROWS

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
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gallery", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SettingsSectionTitle("Photo Grid Size")
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (MIN_GRID_COLUMNS..MAX_GRID_COLUMNS).forEach { count ->
                    FilterChip(
                        selected = gridColumns == count,
                        onClick = { onGridColumnsChange(count) },
                        label = { Text("$count") }
                    )
                }
            }

            SettingsSectionTitle("Home Library Preview Rows")
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (MIN_LIBRARY_PREVIEW_ROWS..MAX_LIBRARY_PREVIEW_ROWS).forEach { count ->
                    FilterChip(
                        selected = libraryPreviewRows == count,
                        onClick = { onLibraryPreviewRowsChange(count) },
                        label = { Text("$count") }
                    )
                }
            }
            Text(
                "How many rows of thumbnails Home's Library section shows before \"See Full Library\".",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
            )

            SettingsGroup(
                "Ratio",
                listOf(
                    GridStyle.SQUARE to "Square",
                    GridStyle.ORIGINAL to "Aspect"
                ).map { (style, label) ->
                    { SettingsRadioRow(label, gridStyle == style) { onGridStyleChange(style) } }
                }
            )

            SettingsGroup(
                "Group by",
                listOf(
                    GroupBy.NONE to "None",
                    GroupBy.DAY to "Day",
                    GroupBy.WEEK to "Week",
                    GroupBy.MONTH to "Month",
                    GroupBy.YEAR to "Year"
                ).map { (mode, label) ->
                    { SettingsRadioRow(label, groupBy == mode) { onGroupByChange(mode) } }
                }
            )
            Text(
                "Applies to the full Library grid in Square ratio mode only.",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
            )
        }
    }
}
