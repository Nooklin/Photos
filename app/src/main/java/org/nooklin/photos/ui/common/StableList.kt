package org.nooklin.photos.ui.common

import androidx.compose.runtime.Immutable

// Skips recomposition. Not kotlinx.collections.immutable: its release needs Kotlin 2.3.0, this project pins 2.2.10.
@Immutable
data class StableList<T>(val value: List<T>)
