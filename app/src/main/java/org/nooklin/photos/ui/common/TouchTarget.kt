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

// Makes the tappable area at least 48 dp while the drawn part can stay smaller.
// Put it after clickable, so the clickable area is the enlarged one:
//   Box(Modifier.clickable { ... }.touchTarget()) { SmallVisual() }
fun Modifier.touchTarget(): Modifier = this.defaultMinSize(minWidth = MinTouchTarget, minHeight = MinTouchTarget)

// A tap area of at least 48 dp around a smaller drawn shape. The press feedback (ripple) shows only on the
// shape: [visual] gets a modifier to put on it AFTER its clip and background, like
//   TouchTargetBox(onClick = ...) { feedback -> Box(Modifier.size(32.dp).clip(CircleShape).then(feedback)) { ... } }
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
