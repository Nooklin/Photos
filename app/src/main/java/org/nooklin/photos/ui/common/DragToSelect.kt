package org.nooklin.photos.ui.common

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

// (lazy index, key) of the item cells that are on screen, in order
fun orderedKeys(visible: List<Pair<Int, Any>>, isItemKey: (Any) -> Boolean): List<Pair<Int, Any>> =
    visible.sortedBy { it.first }.filter { isItemKey(it.second) }

// Selects the cells between where the finger went down and where it is now. When the finger stays near the top or
// bottom edge, scrollBy moves the grid (faster closer to the edge) and the selection follows.
fun Modifier.dragToSelect(
    enabled: Boolean,
    hitTest: (Offset) -> Any?,
    orderedKeys: () -> List<Pair<Int, Any>>,
    isSelected: (Any) -> Boolean,
    onItemDragged: (key: Any, select: Boolean) -> Unit,
    scrollBy: (suspend (Float) -> Unit)? = null
): Modifier = if (!enabled) this else this.pointerInput(enabled) {
    val edgeDirection = mutableIntStateOf(0)
    var edgeSpeed = 0f
    coroutineScope {
        if (scrollBy != null) {
            launch {
                snapshotFlow { edgeDirection.intValue }.collectLatest { direction ->
                    if (direction != 0) {
                        var last = 0L
                        while (true) {
                            val now = withFrameNanos { it }
                            if (last != 0L) scrollBy(direction * edgeSpeed * (now - last) / 1_000_000_000f)
                            last = now
                        }
                    }
                }
            }
        }
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val slop = viewConfiguration.touchSlop
            var moved = Offset.Zero
            var claimed = false
            val indexOfKey = HashMap<Any, Int>()
            val keyAtIndex = HashMap<Int, Any>()
            var anchor = -1
            var runLo = -1
            var runHi = -1
            val before = HashMap<Any, Boolean>()
            var lastPos = down.position

            fun refresh() {
                for ((index, key) in orderedKeys()) {
                    indexOfKey[key] = index
                    keyAtIndex[index] = key
                }
            }

            fun toggleCell(i: Int) {
                val key = keyAtIndex[i] ?: return
                val was = before.getOrPut(key) { isSelected(key) }
                onItemDragged(key, !was)
            }

            fun restoreCell(i: Int) {
                val key = keyAtIndex[i] ?: return
                before[key]?.let { onItemDragged(key, it) }
            }

            fun moveRun(lo: Int, hi: Int) {
                if (runLo < 0) {
                    for (i in lo..hi) toggleCell(i)
                } else {
                    for (i in lo..hi) if (i < runLo || i > runHi) toggleCell(i)
                    for (i in runLo..runHi) if (i < lo || i > hi) restoreCell(i)
                }
                runLo = lo
                runHi = hi
            }

            fun update(pos: Offset) {
                refresh()
                val height = size.height.toFloat()
                val zone = minOf(height * 0.2f, 110.dp.toPx())
                val intoBottom = pos.y - (height - zone)
                val intoTop = zone - pos.y
                when {
                    intoBottom > 0f -> {
                        edgeSpeed = (intoBottom / zone).coerceIn(0.05f, 1f) * 1400.dp.toPx()
                        edgeDirection.intValue = 1
                    }
                    intoTop > 0f -> {
                        edgeSpeed = (intoTop / zone).coerceIn(0.05f, 1f) * 1400.dp.toPx()
                        edgeDirection.intValue = -1
                    }
                    else -> edgeDirection.intValue = 0
                }
                val hit = hitTest(pos)?.let { indexOfKey[it] }
                if (hit != null && anchor >= 0) {
                    val lo = minOf(anchor, hit)
                    val hi = maxOf(anchor, hit)
                    if (lo != runLo || hi != runHi) moveRun(lo, hi)
                }
            }

            try {
                while (true) {
                    // while dragging, wake up now and then so a finger held at the edge keeps selecting
                    val event = if (claimed) {
                        withTimeoutOrNull(16L) { awaitPointerEvent(PointerEventPass.Initial) }
                    } else {
                        awaitPointerEvent(PointerEventPass.Initial)
                    }
                    if (event != null) {
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        lastPos = change.position
                        if (!claimed) {
                            moved += change.positionChange()
                            if (moved.getDistance() > slop) {
                                if (abs(moved.y) > abs(moved.x)) break
                                claimed = true
                                refresh()
                                anchor = hitTest(down.position)?.let { indexOfKey[it] } ?: -1
                                if (anchor >= 0) moveRun(anchor, anchor)
                            }
                        }
                        if (claimed) change.consume()
                    }
                    if (claimed) update(lastPos)
                }
            } finally {
                edgeDirection.intValue = 0
            }
        }
    }
}
