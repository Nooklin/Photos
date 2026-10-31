package org.nooklin.photos.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.nooklin.photos.ui.theme.AppText
import org.nooklin.photos.ui.theme.Spacing

// A list or screen with nothing to show: a short text and exactly one button, so it is never a dead end
@Composable
fun EmptyState(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.padding(Spacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Lg)
    ) {
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = AppText.Body,
            textAlign = TextAlign.Center
        )
        FilledTonalButton(onClick = onAction) {
            Text(actionLabel)
        }
    }
}
