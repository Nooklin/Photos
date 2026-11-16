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
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.nooklin.photos.ui.albums.AlbumOrderViewModel
import org.nooklin.photos.R
import org.nooklin.photos.appString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHiddenAlbumsScreen(
    hiddenAlbumsEnabled: Boolean,
    onHiddenAlbumsEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val vm: AlbumOrderViewModel = viewModel()
    val previewsOff by vm.hiddenPreviewsOff.collectAsStateWithLifecycle()
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
            SettingsHeading(appString(R.string.common_hidden_albums))
            SettingsGroup(
                null,
                listOf({ SettingsToggleRow(appString(R.string.common_show_hidden_albums), hiddenAlbumsEnabled, Icons.Filled.PhotoAlbum, onCheckedChange = onHiddenAlbumsEnabledChange) })
            )
            SettingsHint(AnnotatedString.fromHtml(
                    appString(R.string.settings_hidden_albums_when_off_hidden_albums_section)
                ))
            Spacer(modifier = Modifier.height(8.dp))
            SettingsHint(AnnotatedString.fromHtml(
                    appString(R.string.settings_hidden_albums_when_on_hidden_albums_section)
                ))
            SettingsHeading(appString(R.string.settings_head_home))
            AlbumHomeSettings(hidden = true)

            SettingsHeading(appString(R.string.settings_head_previews))
            SettingsGroup(
                null,
                listOf<@Composable () -> Unit>({
                    SettingsToggleRow(
                        label = appString(R.string.hidden_previews_off),
                        checked = previewsOff,
                        icon = Icons.Filled.PhotoAlbum,
                        onCheckedChange = { vm.setHiddenPreviewsOff(it) }
                    )
                })
            )
            SettingsHint(appString(R.string.hidden_previews_off_sub))
        }
    }
}
