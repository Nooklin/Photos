package org.nooklin.photos.ui.settings

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.nooklin.photos.data.local.AppFont
import org.nooklin.photos.data.local.AppThemeMode
import org.nooklin.photos.data.local.ContrastColor
import org.nooklin.photos.ui.theme.fontFamilyFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAppearanceScreen(
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    contrastColor: ContrastColor,
    onContrastColorChange: (ContrastColor) -> Unit,
    appFont: AppFont,
    onAppFontChange: (AppFont) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SettingsGroup(
                "Theme",
                listOf(
                    AppThemeMode.SYSTEM to "Follow System",
                    AppThemeMode.LIGHT to "Light",
                    AppThemeMode.DARK to "Dark",
                    AppThemeMode.BLACK to "Black"
                ).map { (mode, label) ->
                    { SettingsRadioRow(label, themeMode == mode) { onThemeModeChange(mode) } }
                }
            )
            Text(
                "Black uses a true black background everywhere (better battery life on OLED screens), with a dark grey tint on cards for contrast.",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            )

            SettingsGroup(
                "Font",
                listOf(
                    AppFont.SYSTEM_DEFAULT to AppFont.SYSTEM_DEFAULT.label,
                    AppFont.WORK_SANS to AppFont.WORK_SANS.label
                ).map { (font, label) ->
                    { SettingsRadioRow(label, appFont == font, fontFamily = fontFamilyFor(font)) { onAppFontChange(font) } }
                }
            )

            SettingsGroup(
                "Contrast Color",
                listOf({
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(ContrastColor.entries, key = { it.storageKey }) { option ->
                            ContrastColorSwatch(
                                option = option,
                                selected = option == contrastColor,
                                onClick = { onContrastColorChange(option) }
                            )
                        }
                    }
                })
            )
            Text(
                "Tints buttons, accents, and pill and popup backgrounds throughout the app, in both light and dark mode.",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            )
        }
    }
}

// DEFAULT is an outlined circle, not a filled swatch: it means "no tint"
@Composable
private fun ContrastColorSwatch(option: ContrastColor, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(4.dp)
            .size(40.dp)
            .clip(CircleShape)
            .then(
                if (option.color != null) {
                    Modifier.background(option.color)
                } else {
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                }
            )
            .then(
                if (selected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = if (option.color != null) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
