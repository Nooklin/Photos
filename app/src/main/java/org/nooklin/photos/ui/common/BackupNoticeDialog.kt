package org.nooklin.photos.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

// Shown once and not dismissible: it has to be acknowledged
@Composable
fun BackupNoticeDialog(onAcknowledge: () -> Unit) {
    var checked by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        shape = GlassPill.shape,
        containerColor = GlassPill.adaptivePopupBackground,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("Thank you for installing Nooklin Photos.", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Below you will find some very important information you SHOULD READ!",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    "If you use the Hidden Section of this app, please read this carefully.",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "Why: photos you hide are stored privately on this device only, in a " +
                        "location only this app can read. That is what keeps them invisible to " +
                        "every other app on your phone. But that same privacy means Android " +
                        "permanently deletes them the moment this app is uninstalled — this is " +
                        "how your device handles all private app storage, not a bug, and it " +
                        "cannot be undone once it happens.",
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "How to protect them: before you uninstall this app, or switch to a new " +
                        "device, open Settings > Hidden Section > Hidden Photos Backup and export a " +
                        "backup zip file to a location you control (for example another folder " +
                        "on your device, an SD card, or a cloud folder you manage yourself). " +
                        "This app will never do this automatically or without you asking — " +
                        "backing up is always your choice, on your terms.",
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row {
                    Checkbox(checked = checked, onCheckedChange = { checked = it })
                    Text(
                        "I have read and acknowledge the information above.",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                Text(
                    "We wish you happy use of the app!\nGreetings,\nNooklin Team",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAcknowledge, enabled = checked) {
                Text("Confirm")
            }
        }
    )
}
