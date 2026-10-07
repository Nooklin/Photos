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
                title = { Text("Hidden Albums", fontWeight = FontWeight.Bold) },
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
                "Hidden Albums",
                listOf({ SettingsToggleRow("Show Hidden Albums", hiddenAlbumsEnabled, Icons.Filled.PhotoAlbum, onCheckedChange = onHiddenAlbumsEnabledChange) })
            )
            Text(
                AnnotatedString.fromHtml(
                    "When Off: Hidden Albums section is <b>not</b> shown in the Hidden Section. " +
                        "Your hidden albums and everything in them are not deleted or changed in " +
                        "any way, they're only hidden from view."
                ),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                AnnotatedString.fromHtml(
                    "When On: Hidden Albums section is <b>shown</b> in the Hidden Section. " +
                        "Turning this back on makes them reappear exactly as you left them."
                ),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }
    }
}
