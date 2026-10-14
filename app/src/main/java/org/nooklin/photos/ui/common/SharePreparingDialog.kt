package org.nooklin.photos.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.nooklin.photos.R
import org.nooklin.photos.appString

// Shown while a file is prepared for sharing or copying (video can take a while). Cancel stops the work.
@Composable
fun SharePreparingDialog(onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        shape = GlassPill.shape,
        containerColor = GlassPill.adaptivePopupBackground,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(appString(R.string.share_sheet_preparing)) },
        text = {
            Column {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) { Text(appString(R.string.common_cancel)) }
        }
    )
}
