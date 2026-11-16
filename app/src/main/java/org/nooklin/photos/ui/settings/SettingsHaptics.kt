package org.nooklin.photos.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.data.local.AppPreferences

@Composable
fun HapticsSetting() {
    val appContext = LocalContext.current.applicationContext
    val prefs = remember { AppPreferences(appContext) }
    val on by prefs.hapticsEnabled.collectAsStateWithLifecycle(true)
    val scope = rememberCoroutineScope()
    SettingsToggleRow(
        label = appString(R.string.settings_haptics),
        checked = on,
        icon = Icons.Filled.Vibration,
        onCheckedChange = { scope.launch { prefs.setHapticsEnabled(it) } }
    )
}
