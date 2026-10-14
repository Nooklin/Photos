package org.nooklin.photos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.nooklin.photos.data.local.HomeItems
import org.nooklin.photos.data.local.HomeSection
import org.nooklin.photos.R
import org.nooklin.photos.appString

// Up/down buttons, not drag and drop: that needs a third-party library or a hand-rolled gesture
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderHomeScreen(
    initialOrder: List<HomeSection>,
    mediaTypesOrder: List<String>,
    utilitiesOrder: List<String>,
    hiddenSections: Set<HomeSection>,
    onReorder: (List<HomeSection>) -> Unit,
    onMediaTypesReorder: (List<String>) -> Unit,
    onUtilitiesReorder: (List<String>) -> Unit,
    onHiddenSectionsChange: (Set<HomeSection>) -> Unit,
    onBack: () -> Unit
) {
    var order by remember(initialOrder) { mutableStateOf(initialOrder) }
    var mediaTypes by remember(mediaTypesOrder) { mutableStateOf(mediaTypesOrder) }
    var utilities by remember(utilitiesOrder) { mutableStateOf(utilitiesOrder) }
    var hidden by remember(hiddenSections) { mutableStateOf(hiddenSections) }

    fun <T> moved(list: List<T>, index: Int, delta: Int): List<T>? {
        val target = index + delta
        if (target < 0 || target >= list.size) return null
        val mutable = list.toMutableList()
        mutable.add(target, mutable.removeAt(index))
        return mutable
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.common_reorder_home_screen), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Text(
                appString(R.string.reorder_home_choose_the_order_sections_appear),
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            order.forEachIndexed { index, section ->
                ReorderRow(
                    label = section.label,
                    canMoveUp = index > 0,
                    canMoveDown = index < order.size - 1,
                    onMoveUp = { moved(order, index, -1)?.let { order = it; onReorder(it) } },
                    onMoveDown = { moved(order, index, 1)?.let { order = it; onReorder(it) } },
                    shown = if (section in HomeSection.HIDEABLE) section !in hidden else null,
                    onShownChange = { shown ->
                        hidden = if (shown) hidden - section else hidden + section
                        onHiddenSectionsChange(hidden)
                    }
                )
            }

            Text(
                appString(R.string.common_media_types),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
            )
            mediaTypes.forEachIndexed { index, key ->
                ReorderRow(
                    label = HomeItems.label(key),
                    canMoveUp = index > 0,
                    canMoveDown = index < mediaTypes.size - 1,
                    onMoveUp = { moved(mediaTypes, index, -1)?.let { mediaTypes = it; onMediaTypesReorder(it) } },
                    onMoveDown = { moved(mediaTypes, index, 1)?.let { mediaTypes = it; onMediaTypesReorder(it) } }
                )
            }

            Text(
                appString(R.string.common_utilities),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
            )
            utilities.forEachIndexed { index, key ->
                ReorderRow(
                    label = HomeItems.label(key),
                    canMoveUp = index > 0,
                    canMoveDown = index < utilities.size - 1,
                    onMoveUp = { moved(utilities, index, -1)?.let { utilities = it; onUtilitiesReorder(it) } },
                    onMoveDown = { moved(utilities, index, 1)?.let { utilities = it; onUtilitiesReorder(it) } }
                )
            }
            Text(
                appString(R.string.reorder_home_the_order_of_these_tiles),
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

/** One row: label, an optional shown switch, and the up and down buttons. [shown] null means the row can't be hidden. */
@Composable
private fun ReorderRow(
    label: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    shown: Boolean? = null,
    onShownChange: (Boolean) -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (shown != null) {
                Switch(checked = shown, onCheckedChange = onShownChange)
            }
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = appString(R.string.reorder_home_move_up))
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = appString(R.string.reorder_home_move_down))
            }
        }
    }
}

// Hidden has its own show/hide switches, separate from Home's
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenHomeCustomizeScreen(
    hiddenSections: Set<HomeSection>,
    onHiddenSectionsChange: (Set<HomeSection>) -> Unit,
    onBack: () -> Unit
) {
    var hidden by remember(hiddenSections) { mutableStateOf(hiddenSections) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.common_show_or_hide_sections), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                appString(R.string.reorder_home_choose_which_sections_the_hidden),
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            listOf(HomeSection.ALBUMS to appString(R.string.common_hidden_albums), HomeSection.MEDIA_TYPES to appString(R.string.common_media_types), HomeSection.UTILITIES to appString(R.string.common_utilities))
                .forEach { (section, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label)
                        Switch(
                            checked = section !in hidden,
                            onCheckedChange = { shown ->
                                hidden = if (shown) hidden - section else hidden + section
                                onHiddenSectionsChange(hidden)
                            }
                        )
                    }
                }
        }
    }
}
