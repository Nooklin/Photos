package org.nooklin.photos.ui.common

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import org.nooklin.photos.ui.theme.MinTouchTarget

fun Modifier.touchTarget(): Modifier = this.defaultMinSize(minWidth = MinTouchTarget, minHeight = MinTouchTarget)

@Composable
fun TouchTargetBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.Button,
    visual: @Composable BoxScope.(feedback: Modifier) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = LocalIndication.current
    Box(
        modifier
            .clickable(interactionSource = interactionSource, indication = null, role = role, onClick = onClick)
            .touchTarget(),
        contentAlignment = Alignment.Center
    ) {
        visual(Modifier.indication(interactionSource, indication))
    }
}
