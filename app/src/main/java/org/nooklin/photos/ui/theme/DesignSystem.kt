package org.nooklin.photos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
    accentColor = AppColors.AccentBlue
)

val AndroidTokens = DesignTokens(
    style = DesignStyle.ANDROID,
    cornerRadius = 16.dp,
    gridSpacing = 4.dp,
    accentColor = AppColors.AndroidAccent
)

val LocalDesignTokens = staticCompositionLocalOf { IosTokens }

data class ContrastColorState(val color: Color?, val isDarkTheme: Boolean, val isBlackMode: Boolean = false)

val LocalContrastColorState = staticCompositionLocalOf { ContrastColorState(null, false) }

@Composable
fun NooklinTheme(
    style: DesignStyle,
    darkTheme: Boolean = isSystemInDarkTheme(),
    contrastColor: ContrastColor = ContrastColor.DEFAULT,
    appFont: AppFont = AppFont.INTER,
    blackMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val tokens = if (style == DesignStyle.IOS) IosTokens else AndroidTokens
    val baseColorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    // DEFAULT is neutral (black or white).
    val accent = contrastColor.color ?: baseColorScheme.onSurface
    val colorScheme = baseColorScheme.withoutPurple(accent, darkTheme).let { scheme ->
        if (blackMode) scheme.copy(background = Color.Black, surface = Color.Black) else scheme
    }
    CompositionLocalProvider(
        LocalDesignTokens provides tokens,
        LocalContrastColorState provides ContrastColorState(contrastColor.color, darkTheme, blackMode)
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = typographyFor(appFont), content = content)
    }
}

private fun ColorScheme.withoutPurple(accent: Color, dark: Boolean): ColorScheme {
    val grey = if (dark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
    val outlineGrey = Color(0xFF8E8E93)
    val outlineVariantGrey = if (dark) Color(0xFF48484A) else Color(0xFFC7C7CC)
    val mutedText = if (dark) Color(0xFFAEAEB2) else Color(0xFF6C6C70)
    return copy(
        primary = accent,
        onPrimary = if (accent.luminance() > 0.5f) Color.Black else Color.White,
        primaryContainer = grey,
        onPrimaryContainer = onSurface,
        inversePrimary = grey,
        secondary = accent,
        onSecondary = if (accent.luminance() > 0.5f) Color.Black else Color.White,
        secondaryContainer = grey,
        onSecondaryContainer = onSurface,
        tertiary = accent,
        onTertiary = if (accent.luminance() > 0.5f) Color.Black else Color.White,
        tertiaryContainer = grey,
        onTertiaryContainer = onSurface,
        surfaceTint = accent,
        surfaceVariant = grey,
        onSurfaceVariant = mutedText,
        surfaceContainerHighest = grey,
        outline = outlineGrey,
        outlineVariant = outlineVariantGrey
    )
}
