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
import org.nooklin.photos.R
import org.nooklin.photos.appString

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
        title = { Text(appString(R.string.backup_notice_thank_you_for_installing_nooklin), fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    appString(R.string.backup_notice_below_you_will_find_some),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    appString(R.string.backup_notice_if_you_use_the_hidden),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    appString(R.string.backup_notice_why_photos_you_hide_are),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    appString(R.string.backup_notice_how_to_protect_them_before),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row {
                    Checkbox(checked = checked, onCheckedChange = { checked = it })
                    Text(
                        appString(R.string.backup_notice_i_have_read_and_acknowledge),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                Text(
                    appString(R.string.backup_notice_we_wish_you_happy_use),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAcknowledge, enabled = checked) {
                Text(appString(R.string.common_confirm))
            }
        }
    )
}
