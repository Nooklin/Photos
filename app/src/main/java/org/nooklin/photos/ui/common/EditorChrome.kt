package org.nooklin.photos.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.nooklin.photos.ui.theme.AppColors
import org.nooklin.photos.ui.theme.AppText
import org.nooklin.photos.ui.theme.MinTouchTarget
import org.nooklin.photos.ui.theme.Spacing

class EditorModeItem<T>(val mode: T, val icon: ImageVector, val label: String)

// The row of editor modes at the bottom. Same pill, height and icon buttons as the toolbar of the viewer.
@Composable
fun <T> EditorModeBar(
    items: List<EditorModeItem<T>>,
    selected: T,
    chromeColor: Color,
    pillBackground: Color,
    pillBorder: BorderStroke?,
    enabled: Boolean = true,
    onSelect: (T) -> Unit
) {
    GlassPillContainer(
        backgroundColor = pillBackground,
        border = pillBorder,
        verticalContentPadding = 0.dp
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { item ->
                val isSelected = item.mode == selected
                IconButton(
                    onClick = { onSelect(item.mode) },
                    enabled = enabled,
                    modifier = Modifier.semantics { this.selected = isSelected }
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) AppColors.SelectedYellow else chromeColor
                    )
                }
            }
        }
    }
}

// The red pill in the top bar of the editors (Revert, Delete edit). White text on a red that reaches 4.5:1.
@Composable
fun EditorDestructivePill(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .defaultMinSize(minHeight = MinTouchTarget)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(end = Spacing.Xs),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .clip(CircleShape)
                .background(AppColors.DestructiveButton)
                .padding(horizontal = Spacing.Lg, vertical = Spacing.Sm)
        ) {
            Text(text, color = Color.White, fontSize = AppText.Body, fontWeight = FontWeight.Medium)
        }
    }
}
