package org.nooklin.photos.ui.common

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange

// Drag across cells to select a run, only in selection mode
fun Modifier.dragToSelect(
    enabled: Boolean,
    hitTest: (Offset) -> Any?,
    isSelected: (Any) -> Boolean,
    onItemDragged: (key: Any, select: Boolean) -> Unit
): Modifier = if (!enabled) this else this.pointerInput(enabled) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var pastSlop = false
        var accumulatedDrag = 0f
        val slop = viewConfiguration.touchSlop
        var lastHit: Any? = null
        var dragMode: Boolean? = null

        do {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break

            if (!pastSlop) {
                accumulatedDrag += change.positionChange().getDistance()
                if (accumulatedDrag > slop) {
                    pastSlop = true
                    // The cell the drag started on counts too and sets the drag mode
                    hitTest(down.position)?.let { hit ->
                        val mode = !isSelected(hit)
                        dragMode = mode
                        lastHit = hit
                        onItemDragged(hit, mode)
                    }
                }
            }

            if (pastSlop) {
                val mode = dragMode
                val hit = hitTest(change.position)
                if (hit != null && hit != lastHit && mode != null) {
                    lastHit = hit
                    onItemDragged(hit, mode)
                }
                change.consume()
            }
        } while (event.changes.any { it.pressed })
    }
}
