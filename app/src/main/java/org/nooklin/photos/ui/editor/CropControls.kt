package org.nooklin.photos.ui.editor

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.theme.AppColors
import org.nooklin.photos.ui.theme.AppText
import org.nooklin.photos.ui.theme.MinTouchTarget
import org.nooklin.photos.ui.theme.Spacing

// Height of CropTopRow, the editors keep this free above the picture in Crop
val CropTopRowHeight = 56.dp

// The fine adjustments of the Crop screen: round buttons in a row and one ruler for the selected one
enum class GeometryTool(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val min: Float,
    val max: Float,
    // Value distance between two marks on the ruler, a long mark follows every five
    val step: Float
) {
    STRAIGHTEN(R.string.photo_editor_straighten, Icons.Filled.Straighten, -45f, 45f, 1f),
    VERTICAL(R.string.photo_editor_vertical, Icons.Filled.Height, -1f, 1f, 0.05f),
    HORIZONTAL(R.string.photo_editor_horizontal, Icons.Filled.SwapHoriz, -1f, 1f, 0.05f);

    val label: String get() = appString(labelRes)

    // Degrees with one decimal for Straighten, a percentage for the two perspective tools
    fun displayValue(value: Float): String = when (this) {
        STRAIGHTEN -> (if (value > 0f) "+" else "") + "%.1f".format(value) + "°"
        else -> (value * 100).toInt().let { if (it > 0) "+$it" else "$it" }
    }

    // Short form inside the round button, like the Adjust buttons
    fun shortValue(value: Float): String = when (this) {
        STRAIGHTEN -> Math.round(value).let { if (it > 0) "+$it" else "$it" }
        else -> (value * 100).toInt().let { if (it > 0) "+$it" else "$it" }
    }
}

// Round buttons in a row (only when there is more than one tool), the ruler underneath for the selected one
@Composable
fun GeometryTools(
    chromeColor: Color,
    tools: List<GeometryTool>,
    values: Map<GeometryTool, Float>,
    onValueChange: (GeometryTool, Float) -> Unit
) {
    var selected by remember { mutableStateOf(tools.first()) }
    val value = values[selected] ?: 0f
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (tools.size > 1) {
            Row(
                Modifier.fillMaxWidth().padding(top = Spacing.Sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Xl, Alignment.CenterHorizontally)
            ) {
                tools.forEach { tool ->
                    val toolValue = values[tool] ?: 0f
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(MinTouchTarget)
                                .clip(CircleShape)
                                .background(chromeColor.copy(alpha = 0.1f))
                                .then(
                                    if (tool == selected) Modifier.border(2.dp, AppColors.SelectedYellow, CircleShape) else Modifier
                                )
                                .clickable(role = Role.Button) { selected = tool },
                            contentAlignment = Alignment.Center
                        ) {
                            if (toolValue != 0f) {
                                Text(
                                    text = tool.shortValue(toolValue),
                                    color = chromeColor,
                                    fontSize = AppText.Body,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(tool.icon, contentDescription = tool.label, tint = chromeColor)
                            }
                        }
                        Text(
                            tool.label,
                            color = if (tool == selected) AppColors.SelectedYellow else chromeColor.copy(alpha = 0.7f),
                            fontSize = AppText.Caption,
                            modifier = Modifier.padding(top = Spacing.Xs)
                        )
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().height(MinTouchTarget).padding(horizontal = Spacing.Xl),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                selected.label + "  " + selected.displayValue(value),
                color = chromeColor.copy(alpha = 0.7f),
                fontSize = AppText.Body
            )
            // Always there, so the row keeps its place; it only acts when there is something to reset
            IconButton(
                onClick = { onValueChange(selected, 0f) },
                enabled = value != 0f
            ) {
                if (value != 0f) {
                    Icon(
                        Icons.Filled.RestartAlt,
                        contentDescription = appString(R.string.photo_editor_reset, selected.label),
                        tint = chromeColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
        GeometryRuler(
            value = value,
            min = selected.min,
            max = selected.max,
            step = selected.step,
            description = selected.label,
            chromeColor = chromeColor,
            onValueChange = { onValueChange(selected, it) }
        )
    }
}

// A row of marks that moves under a fixed yellow mark in the middle, like the ruler of the iOS Photos app.
// A drag moves the marks, a screen reader can set the value directly.
@Composable
private fun GeometryRuler(
    value: Float,
    min: Float,
    max: Float,
    step: Float,
    description: String,
    chromeColor: Color,
    onValueChange: (Float) -> Unit
) {
    val tickSpacingPx = with(LocalDensity.current) { 12.dp.toPx() }
    val currentValue by rememberUpdatedState(value)
    val currentChange by rememberUpdatedState(onValueChange)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(MinTouchTarget)
            .pointerInput(min, max, step) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    // The marks follow the finger, so dragging right lowers the value
                    val next = currentValue - dragAmount / tickSpacingPx * step
                    currentChange(next.coerceIn(min, max))
                }
            }
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(value, min..max)
                setProgress { target ->
                    currentChange(target.coerceIn(min, max))
                    true
                }
            }
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val centerTick = value / step
        val reach = (centerX / tickSpacingPx).toInt() + 2
        val first = floor(centerTick).toInt() - reach
        val last = ceil(centerTick).toInt() + reach
        val minIndex = Math.round(min / step)
        val maxIndex = Math.round(max / step)
        for (index in first..last) {
            if (index < minIndex || index > maxIndex) continue
            val x = centerX + (index - centerTick) * tickSpacingPx
            val long = index % 5 == 0
            val half = if (long) 12.dp.toPx() else 7.dp.toPx()
            drawLine(
                color = chromeColor.copy(alpha = if (long) 0.7f else 0.35f),
                start = Offset(x, centerY - half),
                end = Offset(x, centerY + half),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        drawLine(
            color = AppColors.SelectedYellow,
            start = Offset(centerX, centerY - 16.dp.toPx()),
            end = Offset(centerX, centerY + 16.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

// The row above the picture in Crop, like the iOS Photos app: flip and rotate on the left,
// the aspect ratio menu on the right.
@Composable
fun CropTopRow(
    chromeColor: Color,
    pillBackground: Color,
    pillBorder: BorderStroke?,
    selectedPreset: AspectPreset,
    onSelectPreset: (AspectPreset) -> Unit,
    onRotate: () -> Unit,
    onFlipHorizontal: () -> Unit,
    onFlipVertical: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .height(CropTopRowHeight)
            .padding(horizontal = Spacing.Lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .clip(GlassPill.shape)
                .background(pillBackground)
                .then(if (pillBorder != null) Modifier.border(pillBorder, GlassPill.shape) else Modifier)
                .padding(horizontal = Spacing.Xs)
        ) {
            IconButton(onClick = onRotate) {
                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = appString(R.string.photo_editor_rotate), tint = chromeColor)
            }
            IconButton(onClick = onFlipHorizontal) {
                Icon(Icons.Filled.Flip, contentDescription = appString(R.string.photo_editor_flip_horizontal), tint = chromeColor)
            }
            IconButton(onClick = onFlipVertical) {
                Icon(
                    imageVector = Icons.Filled.Flip,
                    contentDescription = appString(R.string.photo_editor_flip_vertical),
                    tint = chromeColor,
                    modifier = Modifier.rotate(90f)
                )
            }
        }
        Box {
            Box(
                Modifier
                    .clip(GlassPill.shape)
                    .background(pillBackground)
                    .then(if (pillBorder != null) Modifier.border(pillBorder, GlassPill.shape) else Modifier)
                    .padding(horizontal = Spacing.Xs)
            ) {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Filled.AspectRatio,
                        contentDescription = appString(R.string.photo_editor_aspect_ratio),
                        tint = if (selectedPreset.ratio != null) AppColors.SelectedYellow else chromeColor
                    )
                }
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                shape = GlassPill.shape
            ) {
                AspectPreset.entries.forEach { preset ->
                    val isSelected = preset == selectedPreset
                    DropdownMenuItem(
                        text = {
                            Text(
                                preset.label,
                                color = if (isSelected) AppColors.SelectedYellow else Color.Unspecified,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onSelectPreset(preset)
                        }
                    )
                }
            }
        }
    }
}
