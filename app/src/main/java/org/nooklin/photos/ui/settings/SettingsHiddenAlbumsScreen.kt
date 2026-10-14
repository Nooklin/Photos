package org.nooklin.photos.ui.settings

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import org.nooklin.photos.R
import org.nooklin.photos.appString

// Display-only: turning it off deletes nothing
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHiddenAlbumsScreen(
    hiddenAlbumsEnabled: Boolean,
    onHiddenAlbumsEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.common_hidden_albums), fontWeight = FontWeight.Bold) },
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
                appString(R.string.common_hidden_albums),
                listOf({ SettingsToggleRow(appString(R.string.common_show_hidden_albums), hiddenAlbumsEnabled, Icons.Filled.PhotoAlbum, onCheckedChange = onHiddenAlbumsEnabledChange) })
            )
            Text(
                AnnotatedString.fromHtml(
                    appString(R.string.settings_hidden_albums_when_off_hidden_albums_section)
                ),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                AnnotatedString.fromHtml(
                    appString(R.string.settings_hidden_albums_when_on_hidden_albums_section)
                ),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }
    }
}
