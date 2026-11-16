package org.nooklin.photos.ui.albums

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.data.local.AlbumSort
import org.nooklin.photos.ui.common.CircleIconButton
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.MenuIcon

@Composable
fun AlbumSortButton(sort: AlbumSort, withManual: Boolean, onSort: (AlbumSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        CircleIconButton(
            icon = Icons.AutoMirrored.Filled.Sort,
            contentDescription = appString(R.string.album_sort),
            onClick = { open = true }
        )
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                SortItem(appString(R.string.album_sort_name), sort == AlbumSort.NAME) { open = false; onSort(AlbumSort.NAME) }
                SortItem(appString(R.string.album_sort_size), sort == AlbumSort.SIZE) { open = false; onSort(AlbumSort.SIZE) }
                if (withManual) {
                    SortItem(appString(R.string.album_sort_manual), sort == AlbumSort.MANUAL) { open = false; onSort(AlbumSort.MANUAL) }
                }
            }
        }
    }
}

@Composable
private fun SortItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { if (selected) MenuIcon(Icons.Filled.Check) },
        onClick = onClick
    )
}

// long-press an item, drag over another one and it swaps places
@Composable
fun Modifier.dragToReorder(
    state: LazyGridState,
    enabled: Boolean,
    onStart: (Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onDone: () -> Unit
): Modifier {
    val haptic = LocalHapticFeedback.current
    return if (!enabled) this else pointerInput(state) {
    var current = -1
    var pos = Offset.Zero
    fun indexAt(p: Offset): Int = state.layoutInfo.visibleItemsInfo.firstOrNull {
        p.x >= it.offset.x && p.x < it.offset.x + it.size.width &&
            p.y >= it.offset.y && p.y < it.offset.y + it.size.height
    }?.index ?: -1
    detectDragGesturesAfterLongPress(
        onDragStart = { start ->
            pos = start
            current = indexAt(start)
            if (current >= 0) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onStart(current)
            }
        },
        onDrag = { change, amount ->
            change.consume()
            pos += amount
            val target = indexAt(pos)
            if (current >= 0 && target >= 0 && target != current) {
                onMove(current, target)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                current = target
            }
        },
        onDragEnd = { current = -1; onDone() },
        onDragCancel = { current = -1; onDone() }
    )
    }
}

private val HomeTileHeight = 182.dp

@Composable
fun <T> HomeAlbumRows(albums: List<T>, rows: Int, key: (T) -> Long, tile: @Composable (T) -> Unit) {
    val padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
    if (rows <= 1) {
        LazyRow(contentPadding = padding, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(albums, key = { key(it) }) { tile(it) }
        }
    } else {
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            modifier = Modifier.height(HomeTileHeight * 2 + 10.dp + 8.dp),
            contentPadding = padding,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(albums, key = { key(it) }) { tile(it) }
        }
    }
}
