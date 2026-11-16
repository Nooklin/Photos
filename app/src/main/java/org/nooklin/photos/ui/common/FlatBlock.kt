package org.nooklin.photos.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.nooklin.photos.ui.theme.Spacing

// a Home section without the glass block around it
@Composable
fun FlatBlock(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = Spacing.Sm)) {
        content()
    }
}
