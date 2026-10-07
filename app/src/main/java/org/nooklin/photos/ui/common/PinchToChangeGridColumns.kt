package org.nooklin.photos.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import kotlin.math.ln
import kotlin.math.roundToInt

// Pinch out gives fewer, bigger columns, pinch in more. Same callback as the Settings chips.
@Composable
fun Modifier.pinchToChangeGridColumns(
    enabled: Boolean,
    columns: Int,
    minColumns: Int,
    maxColumns: Int,
    onColumnsChange: (Int) -> Unit
): Modifier {
    if (!enabled) return this
    val columnsState = rememberUpdatedState(columns)
    val liveScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    val gesture = Modifier.pointerInput(minColumns, maxColumns) {
        awaitEachGesture {
            var accumulatedZoom = 1f
            awaitFirstDown(requireUnconsumed = false)
            do {
                val event = awaitPointerEvent()
                if (event.changes.size < 2) continue
                val zoomChange = event.calculateZoom()
                if (zoomChange != 1f) {
                    accumulatedZoom *= zoomChange
                    // Live feedback only, snapTo tracks the fingers. The commit happens on release.
                    val target = accumulatedZoom.coerceIn(1f / MAX_PREVIEW_SCALE, MAX_PREVIEW_SCALE)
                    scope.launch { liveScale.snapTo(target) }
                    event.changes.forEach { it.consume() }
                }
            } while (event.changes.any { it.pressed })

            // Fingers lifted: commit the column change
            if (accumulatedZoom != 1f) {
                val steps = (ln(accumulatedZoom) / ln(1f + STEP)).roundToInt()
                val newColumns = (columnsState.value - steps).coerceIn(minColumns, maxColumns)
                if (newColumns != columnsState.value) onColumnsChange(newColumns)
            }
            // Then ease the preview back to 1
            scope.launch { liveScale.animateTo(1f, animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)) }
        }
    }

    return this
        .graphicsLayer {
            scaleX = liveScale.value
            scaleY = liveScale.value
        }
        .then(gesture)
}

// One step per ~60% pinch, raised twice (from 20, then 40) because it changed too fast
private const val STEP = 0.60f
private const val MAX_PREVIEW_SCALE = 1f + STEP * 1.5f
private const val ANIMATION_DURATION_MS = 250
