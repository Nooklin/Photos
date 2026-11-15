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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.nooklin.photos.ui.theme.MinTouchTarget

private const val MIN_FRACTION = 0.1f

private enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

@Composable
fun CropOverlay(
    cropRect: CropRect,
    onCropRectChange: (CropRect) -> Unit,
    modifier: Modifier = Modifier,
    inset: Dp = 0.dp
) {
    val density = LocalDensity.current
    val currentCropRect = rememberUpdatedState(cropRect)

    BoxWithConstraints(modifier.fillMaxSize()) {
        val insetPx = with(density) { inset.toPx() }
        val widthPx = constraints.maxWidth.toFloat() - 2f * insetPx
        val heightPx = constraints.maxHeight.toFloat() - 2f * insetPx
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

        val rectLeftDp = with(density) { (cropRect.left * widthPx + insetPx).toDp() }
        val rectTopDp = with(density) { (cropRect.top * heightPx + insetPx).toDp() }
        val rectWidthDp = with(density) { (cropRect.width * widthPx).toDp() }
        val rectHeightDp = with(density) { (cropRect.height * heightPx).toDp() }

        Box(
            Modifier
                .offset { IntOffset((cropRect.left * widthPx + insetPx).toInt(), (cropRect.top * heightPx + insetPx).toInt()) }
                .size(rectWidthDp, rectHeightDp)
                .border(BorderStroke(2.dp, Color.White))
        )

        val touchSize = MinTouchTarget
        val half = touchSize / 2

        @Composable
        fun Handle(corner: Corner, x: Dp, y: Dp) {
            Box(
                Modifier
                    .offset(x = x - half, y = y - half)
                    .size(touchSize)
                    .pointerInput(widthPx, heightPx) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            updateCorner(corner, dragAmount)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(24.dp).background(Color.White, CircleShape))
            }
        }

        Handle(Corner.TOP_LEFT, rectLeftDp, rectTopDp)
        Handle(Corner.TOP_RIGHT, rectLeftDp + rectWidthDp, rectTopDp)
        Handle(Corner.BOTTOM_LEFT, rectLeftDp, rectTopDp + rectHeightDp)
        Handle(Corner.BOTTOM_RIGHT, rectLeftDp + rectWidthDp, rectTopDp + rectHeightDp)
    }
}
