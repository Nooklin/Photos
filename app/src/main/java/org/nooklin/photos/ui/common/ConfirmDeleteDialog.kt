package org.nooklin.photos.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ConfirmDeleteDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Delete",
    containerColor: Color = GlassPill.adaptivePopupBackground,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = GlassPill.shape,
        containerColor = containerColor,
        titleContentColor = contentColor,
        textContentColor = contentColor,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            Box(
                Modifier
                    .clip(GlassPill.shape)
                    .background(Color.Gray.copy(alpha = 0.15f))
                    .border(BorderStroke(1.dp, Color.Gray.copy(alpha = 0.25f)), GlassPill.shape)
                    .clickable(onClick = onConfirm)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(confirmLabel, color = GlassPill.destructiveText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
