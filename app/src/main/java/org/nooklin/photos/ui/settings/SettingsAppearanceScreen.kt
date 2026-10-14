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
import org.nooklin.photos.R
import org.nooklin.photos.appString
import android.app.LocaleManager
import android.os.LocaleList
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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
    val context = LocalContext.current
    val localeManager = remember { context.getSystemService(LocaleManager::class.java) }
    var languageTags by remember { mutableStateOf(localeManager.applicationLocales.toLanguageTags()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.common_appearance), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SettingsGroup(
                appString(R.string.settings_appearance_theme),
                listOf(
                    AppThemeMode.SYSTEM to appString(R.string.settings_appearance_follow_system),
                    AppThemeMode.LIGHT to appString(R.string.settings_appearance_light),
                    AppThemeMode.DARK to appString(R.string.settings_appearance_dark),
                    AppThemeMode.BLACK to appString(R.string.settings_appearance_black)
                ).map { (mode, label) ->
                    { SettingsRadioRow(label, themeMode == mode) { onThemeModeChange(mode) } }
                }
            )
            Text(
                appString(R.string.settings_appearance_black_uses_a_true_black),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            )

            SettingsGroup(
                appString(R.string.settings_appearance_language),
                listOf(
                    "" to appString(R.string.settings_appearance_follow_system),
                    "en" to "English",
                    "nl" to "Nederlands"
                ).map { (tag, label) ->
                    {
                        SettingsRadioRow(label, if (tag.isEmpty()) languageTags.isEmpty() else languageTags.startsWith(tag)) {
                            languageTags = tag
                            localeManager.applicationLocales = LocaleList.forLanguageTags(tag)
                        }
                    }
                }
            )
            Text(
                appString(R.string.settings_appearance_language_explanation),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            )

            SettingsGroup(
                appString(R.string.settings_appearance_font),
                listOf(
                    AppFont.SYSTEM_DEFAULT to AppFont.SYSTEM_DEFAULT.label,
                    AppFont.WORK_SANS to AppFont.WORK_SANS.label
                ).map { (font, label) ->
                    { SettingsRadioRow(label, appFont == font, fontFamily = fontFamilyFor(font)) { onAppFontChange(font) } }
                }
            )

            SettingsGroup(
                appString(R.string.settings_appearance_contrast_color),
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
                appString(R.string.settings_appearance_tints_buttons_accents_and_pill),
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
                contentDescription = appString(R.string.common_selected),
                tint = if (option.color != null) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
