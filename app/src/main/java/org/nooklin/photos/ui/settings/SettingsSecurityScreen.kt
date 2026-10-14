package org.nooklin.photos.ui.settings

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Screenshot
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
fun SettingsSecurityScreen(
    appLockEnabled: Boolean,
    onAppLockEnabledChange: (Boolean) -> Unit,
    disableScreenshots: Boolean,
    onDisableScreenshotsChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.common_privacy), fontWeight = FontWeight.Bold) },
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
                appString(R.string.common_app_lock),
                listOf({ SettingsToggleRow(appString(R.string.common_app_lock), appLockEnabled, Icons.Filled.Lock, onCheckedChange = onAppLockEnabledChange) })
            )
            Text(
                appString(R.string.settings_security_app_lock_uses_your_device),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            )

            SettingsGroup(
                appString(R.string.common_screenshots),
                listOf({ SettingsToggleRow(appString(R.string.common_disable_screenshots), disableScreenshots, Icons.Filled.Screenshot, onCheckedChange = onDisableScreenshotsChange) })
            )
            Text(
                appString(R.string.settings_security_when_on_screenshots_and_screen),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            )
        }
    }
}
