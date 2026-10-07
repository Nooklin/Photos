package org.nooklin.photos.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

private const val MIN_FRACTION = 0.1f

private enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

// No darkening scrim, border and corner handles are enough.
// Fractional coordinates avoid tracking the container size.
@Composable
fun CropOverlay(
    cropRect: CropRect,
    onCropRectChange: (CropRect) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    // Read fresh: the drag handles live in pointerInput(Unit) blocks that never restart
    val currentCropRect = rememberUpdatedState(cropRect)

    BoxWithConstraints(modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        if (widthPx <= 0f || heightPx <= 0f) return@BoxWithConstraints

        fun updateCorner(corner: Corner, dragPx: androidx.compose.ui.geometry.Offset) {
            val rect = currentCropRect.value
            val dxFraction = dragPx.x / widthPx
            val dyFraction = dragPx.y / heightPx
            val updated = when (corner) {
                Corner.TOP_LEFT -> rect.copy(
                    left = (rect.left + dxFraction).coerceIn(0f, rect.right - MIN_FRACTION),
                    top = (rect.top + dyFraction).coerceIn(0f, rect.bottom - MIN_FRACTION)
                )
                Corner.TOP_RIGHT -> rect.copy(
                    right = (rect.right + dxFraction).coerceIn(rect.left + MIN_FRACTION, 1f),
                    top = (rect.top + dyFraction).coerceIn(0f, rect.bottom - MIN_FRACTION)
                )
                Corner.BOTTOM_LEFT -> rect.copy(
                    left = (rect.left + dxFraction).coerceIn(0f, rect.right - MIN_FRACTION),
                    bottom = (rect.bottom + dyFraction).coerceIn(rect.top + MIN_FRACTION, 1f)
                )
                Corner.BOTTOM_RIGHT -> rect.copy(
                    right = (rect.right + dxFraction).coerceIn(rect.left + MIN_FRACTION, 1f),
                    bottom = (rect.bottom + dyFraction).coerceIn(rect.top + MIN_FRACTION, 1f)
                )
            }
            onCropRectChange(updated)
        }

        val rectLeftDp = with(density) { (cropRect.left * widthPx).toDp() }
        val rectTopDp = with(density) { (cropRect.top * heightPx).toDp() }
        val rectWidthDp = with(density) { (cropRect.width * widthPx).toDp() }
        val rectHeightDp = with(density) { (cropRect.height * heightPx).toDp() }

        Box(
            Modifier
                .offset { IntOffset((cropRect.left * widthPx).toInt(), (cropRect.top * heightPx).toInt()) }
                .size(rectWidthDp, rectHeightDp)
                .border(BorderStroke(2.dp, Color.White))
        )

        val handleSize = 24.dp
        val half = handleSize / 2

        Box(
            Modifier
                .offset(x = rectLeftDp - half, y = rectTopDp - half)
                .size(handleSize)
                .background(Color.White, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        updateCorner(Corner.TOP_LEFT, dragAmount)
                    }
                }
        )
        Box(
            Modifier
                .offset(x = rectLeftDp + rectWidthDp - half, y = rectTopDp - half)
                .size(handleSize)
                .background(Color.White, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        updateCorner(Corner.TOP_RIGHT, dragAmount)
                    }
                }
        )
        Box(
            Modifier
                .offset(x = rectLeftDp - half, y = rectTopDp + rectHeightDp - half)
                .size(handleSize)
                .background(Color.White, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        updateCorner(Corner.BOTTOM_LEFT, dragAmount)
                    }
                }
        )
        Box(
            Modifier
                .offset(x = rectLeftDp + rectWidthDp - half, y = rectTopDp + rectHeightDp - half)
                .size(handleSize)
                .background(Color.White, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        updateCorner(Corner.BOTTOM_RIGHT, dragAmount)
                    }
                }
        )
    }
}
