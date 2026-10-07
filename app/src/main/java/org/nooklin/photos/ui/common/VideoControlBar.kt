package org.nooklin.photos.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player

// State shared by the player and the control bar, they live in different places (pager page and bottom bar)
@Stable
class VideoControlState {
    internal var player by mutableStateOf<Player?>(null)
    var isPlaying by mutableStateOf(false)
    var positionMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(0L)
    var isMuted by mutableStateOf(false)
}

// Pill with play/pause, seek bar and mute, styled like the date pill
@Composable
fun VideoControlBar(
    state: VideoControlState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlassPill.fixedGlassBackground,
    border: BorderStroke? = null,
    contentColor: Color = Color.White
) {
    Surface(
        color = backgroundColor,
        shape = GlassPill.shape,
        border = border,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val p = state.player
                    if (p != null) {
                        if (p.playWhenReady) p.pause() else p.play()
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .semantics { contentDescription = if (state.isPlaying) "Pause" else "Play" }
            ) {
                Canvas(Modifier.size(22.dp)) {
                    if (state.isPlaying) pauseGlyph(contentColor) else playGlyph(contentColor)
                }
            }
            VideoSeekBar(
                state = state,
                color = contentColor,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            )
            IconButton(
                onClick = { state.isMuted = !state.isMuted },
                modifier = Modifier
                    .size(44.dp)
                    .semantics { contentDescription = if (state.isMuted) "Unmute" else "Mute" }
            ) {
                Canvas(Modifier.size(22.dp)) {
                    speakerGlyph(contentColor, muted = state.isMuted)
                }
            }
        }
    }
}

// Keeps the thumb from being cut off at 0% and 100%
private val SEEK_BAR_INSET = 7.dp

private fun seekFraction(x: Float, widthPx: Int, insetPx: Float): Float {
    val span = widthPx - 2f * insetPx
    return if (span <= 0f) 0f else ((x - insetPx) / span).coerceIn(0f, 1f)
}

/** Tap or drag anywhere on the bar to seek; shows the dragged position while dragging, seeks on release. */
@Composable
private fun VideoSeekBar(state: VideoControlState, color: Color, modifier: Modifier = Modifier) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs
    val playedFraction = if (duration > 0L) (state.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val shownFraction = dragFraction ?: playedFraction

    // Reads player and duration when called, the handlers aren't restarted on update
    fun seekTo(fraction: Float) {
        val p = state.player
        val d = state.durationMs
        if (p != null && d > 0L) p.seekTo((fraction * d).toLong())
    }

    Canvas(
        modifier
            .height(40.dp)
            .pointerInput(state) {
                detectTapGestures(
                    onTap = { offset ->
                        seekTo(seekFraction(offset.x, size.width, SEEK_BAR_INSET.toPx()))
                    }
                )
            }
            .pointerInput(state) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragFraction = seekFraction(offset.x, size.width, SEEK_BAR_INSET.toPx())
                    },
                    onHorizontalDrag = { change, _ ->
                        dragFraction = seekFraction(change.position.x, size.width, SEEK_BAR_INSET.toPx())
                    },
                    onDragEnd = {
                        dragFraction?.let { seekTo(it) }
                        dragFraction = null
                    },
                    onDragCancel = { dragFraction = null }
                )
            }
    ) {
        val inset = SEEK_BAR_INSET.toPx()
        val centerY = size.height / 2f
        val start = inset
        val end = size.width - inset
        val strokeWidth = 4.dp.toPx()
        drawLine(
            color = color.copy(alpha = 0.3f),
            start = Offset(start, centerY),
            end = Offset(end, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        val x = start + (end - start) * shownFraction
        if (shownFraction > 0f) {
            drawLine(
                color = color,
                start = Offset(start, centerY),
                end = Offset(x, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
        drawCircle(color = color, radius = inset, center = Offset(x, centerY))
    }
}

private fun DrawScope.playGlyph(color: Color) {
    val w = size.width
    val h = size.height
    val triangle = Path().apply {
        moveTo(w * 0.28f, h * 0.18f)
        lineTo(w * 0.28f, h * 0.82f)
        lineTo(w * 0.84f, h * 0.50f)
        close()
    }
    drawPath(triangle, color)
}

private fun DrawScope.pauseGlyph(color: Color) {
    val w = size.width
    val h = size.height
    val barWidth = w * 0.20f
    val corner = CornerRadius(barWidth * 0.25f, barWidth * 0.25f)
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.22f, h * 0.18f),
        size = Size(barWidth, h * 0.64f),
        cornerRadius = corner
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.58f, h * 0.18f),
        size = Size(barWidth, h * 0.64f),
        cornerRadius = corner
    )
}

private fun DrawScope.speakerGlyph(color: Color, muted: Boolean) {
    val w = size.width
    val h = size.height
    val body = Path().apply {
        moveTo(w * 0.10f, h * 0.38f)
        lineTo(w * 0.28f, h * 0.38f)
        lineTo(w * 0.48f, h * 0.20f)
        lineTo(w * 0.48f, h * 0.80f)
        lineTo(w * 0.28f, h * 0.62f)
        lineTo(w * 0.10f, h * 0.62f)
        close()
    }
    drawPath(body, color)
    val lineWidth = w * 0.08f
    if (muted) {
        // A cross beside the speaker instead of sound waves.
        drawLine(color, Offset(w * 0.64f, h * 0.38f), Offset(w * 0.88f, h * 0.62f), strokeWidth = lineWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.88f, h * 0.38f), Offset(w * 0.64f, h * 0.62f), strokeWidth = lineWidth, cap = StrokeCap.Round)
    } else {
        val centerX = w * 0.48f
        val centerY = h * 0.50f
        val stroke = Stroke(width = lineWidth, cap = StrokeCap.Round)
        for (radius in listOf(w * 0.22f, w * 0.38f)) {
            drawArc(
                color = color,
                startAngle = -50f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2f, radius * 2f),
                style = stroke
            )
        }
    }
}
