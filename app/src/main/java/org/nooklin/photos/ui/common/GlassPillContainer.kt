package org.nooklin.photos.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.nooklin.photos.ui.theme.LocalContrastColorState
import org.nooklin.photos.ui.theme.AppColors
import androidx.compose.ui.graphics.luminance

object GlassPill {
    val shape: Shape = RoundedCornerShape(24.dp)

    val fixedGlassBackground: Color = Color.White.copy(alpha = 0.1f)
    val fixedPopupBackground: Color = AppColors.PopupDark.copy(alpha = 0.94f)

    val adaptiveGlassBackground: Color
        @Composable get() {
            val contrast = LocalContrastColorState.current
            if (contrast.isBlackMode) {
                // Dark grey: true black would hide the edge of a subtle card
                return AppColors.PopupDark.copy(alpha = 0.9f)
            }
            return if (contrast.color != null) {
                // Tinted glass in both themes, low alpha because it sits over photos
                contrast.color.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            }
        }

    val adaptivePopupBackground: Color
        @Composable get() {
            val contrast = LocalContrastColorState.current
            if (contrast.isBlackMode) {
                return AppColors.PopupDark.copy(alpha = 0.97f)
            }
            return if (contrast.color != null) {
                lerp(MaterialTheme.colorScheme.surface, contrast.color, 0.12f).copy(alpha = 0.97f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
            }
        }

    val adaptiveBorder: BorderStroke
        @Composable get() = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

    // Destructive text: the iOS red on dark grounds, a darker red on light grounds so it reaches 4.5:1
    val destructiveText: Color
        @Composable get() = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
            AppColors.DestructiveButton
        } else {
            AppColors.DestructiveText
        }
}

@Composable
fun GlassPillContainer(
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlassPill.fixedGlassBackground,
    border: BorderStroke? = null,
    verticalContentPadding: Dp = 10.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .clip(GlassPill.shape)
            .background(backgroundColor)
            .then(if (border != null) Modifier.border(border, GlassPill.shape) else Modifier)
            .padding(horizontal = 8.dp, vertical = verticalContentPadding)
    ) {
        content()
    }
}
