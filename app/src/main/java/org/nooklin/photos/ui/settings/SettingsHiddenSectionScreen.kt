package org.nooklin.photos.ui.settings

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.nooklin.photos.R
import org.nooklin.photos.appString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHiddenSectionScreen(
    hiddenSectionEnabled: Boolean,
    onHiddenSectionEnabledChange: (Boolean) -> Unit,
    hiddenPinSet: Boolean,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.common_hidden_section), fontWeight = FontWeight.Bold) },
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
                appString(R.string.common_hidden_section),
                listOf({ 
                    SettingsToggleRow(appString(R.string.settings_hidden_section_enable_hidden_section), hiddenSectionEnabled, Icons.Filled.Lock, onCheckedChange = onHiddenSectionEnabledChange) 
                })
            )
            if (hiddenSectionEnabled) {
                Text(
                    appString(R.string.settings_hidden_section_when_disabling_hidden_section_all),
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
                )
                SettingsGroup(
                    appString(R.string.settings_hidden_section_hidden_section_settings),
                    listOf(
                        { SettingsNavRow(appString(R.string.common_hidden_albums), Icons.Filled.PhotoAlbum) { onNavigate("hiddenalbums") } },
                        { SettingsNavRow(appString(R.string.common_hidden_image_sharing), Icons.Filled.Share) { onNavigate("hiddensharing") } },
                        {
                            SettingsNavRow(
                                if (hiddenPinSet) appString(R.string.settings_hidden_section_extra_pin_on) else appString(R.string.settings_hidden_section_extra_pin_off),
                                Icons.Filled.Password
                            ) { onNavigate("hiddensection/hidden_pin") }
                        },
                        { SettingsNavRow(appString(R.string.common_hidden_photos_backup), Icons.Filled.CloudUpload) { onNavigate("hiddenbackup") } }
                    )
                )
            }
        }
    }
}
