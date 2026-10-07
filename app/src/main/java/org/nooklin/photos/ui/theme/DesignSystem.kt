package org.nooklin.photos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.nooklin.photos.data.local.AppFont
import org.nooklin.photos.data.local.ContrastColor

enum class DesignStyle { IOS, ANDROID }

data class DesignTokens(
    val style: DesignStyle,
    val cornerRadius: Dp,
    val gridSpacing: Dp,
    val accentColor: Color
)

val IosTokens = DesignTokens(
    style = DesignStyle.IOS,
    cornerRadius = 10.dp,
    gridSpacing = 2.dp,
    accentColor = Color(0xFF0A84FF)
)

val AndroidTokens = DesignTokens(
    style = DesignStyle.ANDROID,
    cornerRadius = 16.dp,
    gridSpacing = 4.dp,
    accentColor = Color(0xFF6750A4)
)

val LocalDesignTokens = staticCompositionLocalOf { IosTokens }

// null color is DEFAULT: no tint, a real selectable option
data class ContrastColorState(val color: Color?, val isDarkTheme: Boolean, val isBlackMode: Boolean = false)

val LocalContrastColorState = staticCompositionLocalOf { ContrastColorState(null, false) }

@Composable
fun NooklinTheme(
    style: DesignStyle,
    darkTheme: Boolean = isSystemInDarkTheme(),
    contrastColor: ContrastColor = ContrastColor.DEFAULT,
    appFont: AppFont = AppFont.SYSTEM_DEFAULT,
    blackMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val tokens = if (style == DesignStyle.IOS) IosTokens else AndroidTokens
    val baseColorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    val colorScheme = (if (contrastColor.color != null) {
        baseColorScheme.copy(primary = contrastColor.color)
    } else {
        // DEFAULT left primary on Material3's purple baseline instead of neutral
        baseColorScheme.copy(primary = baseColorScheme.onSurface)
    }).let { scheme ->
        if (blackMode) scheme.copy(background = Color.Black, surface = Color.Black) else scheme
    }
    CompositionLocalProvider(
        LocalDesignTokens provides tokens,
        LocalContrastColorState provides ContrastColorState(contrastColor.color, darkTheme, blackMode)
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = typographyFor(appFont), content = content)
    }
}
