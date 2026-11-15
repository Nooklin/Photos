package org.nooklin.photos.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.theme.AppColors
import org.nooklin.photos.ui.theme.Spacing

internal fun formatClock(ms: Long): String {
    val totalTenths = ms / 100
    val minutes = totalTenths / 600
    val seconds = (totalTenths % 600) / 10
    val tenths = totalTenths % 10
    return "%d:%02d.%d".format(minutes, seconds, tenths)
}

@Composable
internal fun TrimPill(background: Color, border: BorderStroke?, content: @Composable () -> Unit) {
    Surface(
        color = background,
        shape = GlassPill.shape,
        border = border,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = Spacing.Sm)
    ) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { content() }
    }
}

@Composable
internal fun TrimRangeBar(
    range: ClosedFloatingPointRange<Float>,
    durationMs: Float,
    positionMs: Float,
    minGapMs: Float,
    enabled: Boolean,
    color: Color,
    frames: List<ImageBitmap?>,
    onChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onDragEnd: (startMoved: Boolean) -> Unit
) {
    val currentRange by rememberUpdatedState(range)
    val currentOnChange by rememberUpdatedState(onChange)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    var movingStart by remember { mutableStateOf(true) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            frames.forEach { frame ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color.copy(alpha = 0.1f))
                ) {
                    if (frame != null) {
                        Image(
                            bitmap = frame,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(enabled, durationMs) {
                    if (!enabled || durationMs <= 0f) return@pointerInput
                    val inset = 12.dp.toPx()
                    fun valueAt(x: Float): Float {
                        val span = size.width - 2f * inset
                        return if (span <= 0f) 0f else ((x - inset) / span).coerceIn(0f, 1f) * durationMs
                    }
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val v = valueAt(offset.x)
                            movingStart = abs(v - currentRange.start) <= abs(v - currentRange.endInclusive)
                        },
                        onHorizontalDrag = { change, _ ->
                            val v = valueAt(change.position.x)
                            val r = currentRange
                            val new = if (movingStart) {
                                v.coerceAtMost(r.endInclusive - minGapMs).coerceAtLeast(0f)..r.endInclusive
                            } else {
                                r.start..v.coerceAtLeast(r.start + minGapMs).coerceAtMost(durationMs)
                            }
                            currentOnChange(new)
                        },
                        onDragEnd = { currentOnDragEnd(movingStart) },
                        onDragCancel = { currentOnDragEnd(movingStart) }
                    )
                }
        ) {
            val inset = 12.dp.toPx()
            val left = inset
            val right = size.width - inset
            val barHeight = size.height
            fun xOf(v: Float) = if (durationMs <= 0f) left else left + (right - left) * (v / durationMs)
            val startX = xOf(range.start)
            val endX = xOf(range.endInclusive)
            val dim = Color.Black.copy(alpha = 0.55f)
            drawRect(dim, topLeft = Offset(left, 0f), size = Size((startX - left).coerceAtLeast(0f), barHeight))
            drawRect(dim, topLeft = Offset(endX, 0f), size = Size((right - endX).coerceAtLeast(0f), barHeight))
            val border = 3.dp.toPx()
            drawRect(
                AppColors.SelectedYellow,
                topLeft = Offset(startX, border / 2f),
                size = Size((endX - startX).coerceAtLeast(0f), barHeight - border),
                style = Stroke(width = border)
            )
            val radius = CornerRadius(6.dp.toPx())
            drawRoundRect(AppColors.SelectedYellow, topLeft = Offset(startX - inset, 0f), size = Size(inset, barHeight), cornerRadius = radius)
            drawRoundRect(AppColors.SelectedYellow, topLeft = Offset(endX, 0f), size = Size(inset, barHeight), cornerRadius = radius)
            val notch = Color.Black.copy(alpha = 0.6f)
            drawLine(notch, Offset(startX - inset / 2f, barHeight * 0.3f), Offset(startX - inset / 2f, barHeight * 0.7f), 2.dp.toPx())
            drawLine(notch, Offset(endX + inset / 2f, barHeight * 0.3f), Offset(endX + inset / 2f, barHeight * 0.7f), 2.dp.toPx())
            val playX = xOf(positionMs.coerceIn(range.start, range.endInclusive))
            drawLine(Color.White, Offset(playX, 0f), Offset(playX, barHeight), 2.dp.toPx())
        }
    }
}
