package org.nooklin.photos.ui.common

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs

// The keys of the visible cells in the order they are shown, headers and other cells left out
fun orderedKeys(visible: List<Pair<Int, Any>>, isItemKey: (Any) -> Boolean): List<Any> =
    visible.sortedBy { it.first }.map { it.second }.filter(isItemKey)

// Drag across cells in selection mode. A drag that starts sideways takes over from scrolling and then follows the finger
// in every direction. Everything from the first cell to the cell under the finger, in reading order, is toggled:
// moving right and down selects whole rows, moving back shrinks the run, and cells that were selected are deselected.
fun Modifier.dragToSelect(
    enabled: Boolean,
    hitTest: (Offset) -> Any?,
    orderedKeys: () -> List<Any>,
    isSelected: (Any) -> Boolean,
    onItemDragged: (key: Any, select: Boolean) -> Unit
): Modifier = if (!enabled) this else this.pointerInput(enabled) {
    awaitEachGesture {
        // Initial pass: look before the grid does, or its scrolling takes the vertical part of the drag
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val slop = viewConfiguration.touchSlop
        var moved = Offset.Zero
        var claimed = false
        var keys: List<Any> = emptyList()
        val indexOfKey = HashMap<Any, Int>()
        var anchor = -1
        var runLo = -1
        var runHi = -1
        // State of a cell before the drag touched it, to give it back when the run shrinks
        val before = HashMap<Any, Boolean>()

        fun toggleCell(i: Int) {
            val key = keys[i]
            val was = before.getOrPut(key) { isSelected(key) }
            onItemDragged(key, !was)
        }

        fun restoreCell(i: Int) {
            val key = keys[i]
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

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break

            if (!claimed) {
                moved += change.positionChange()
                if (moved.getDistance() > slop) {
                    // Mostly vertical: the user scrolls
                    if (abs(moved.y) > abs(moved.x)) break
                    claimed = true
                    keys = orderedKeys()
                    keys.forEachIndexed { i, key -> indexOfKey[key] = i }
                    anchor = hitTest(down.position)?.let { indexOfKey[it] } ?: -1
                    if (anchor >= 0) moveRun(anchor, anchor)
                }
            }

            if (claimed) {
                val hit = hitTest(change.position)?.let { indexOfKey[it] }
                if (hit != null && anchor >= 0) {
                    val lo = minOf(anchor, hit)
                    val hi = maxOf(anchor, hit)
                    if (lo != runLo || hi != runHi) moveRun(lo, hi)
                }
                change.consume()
            }
        }
    }
}
