package org.nooklin.photos.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Design tokens. Screens refer to these instead of writing sizes and colors in place.

// 4/8 grid, the steps from the design guide
object Spacing {
    val Xxs = 2.dp
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val Xxl = 32.dp
    val Xxxl = 48.dp
}

// Text sizes by role. Body is never below 14 sp, Caption (12 sp) is for meta information only.
object AppText {
    val Caption = 12.sp
    val Body = 14.sp
    val BodyLarge = 16.sp
    val Title = 22.sp
    val Headline = 28.sp
}

// Smallest size of anything that can be tapped (Material standard)
val MinTouchTarget = 48.dp

// Durations in milliseconds: short (element in or out), medium (sheet, dialog), long (full screen switch)
object Motion {
    const val Short = 150
    const val Medium = 300
    const val Long = 450
}

object AppColors {
    // Selected item in the editors
    val SelectedYellow = Color(0xFFFFD60A)
    // Destructive text on dark and light grounds (iOS system red)
    val DestructiveText = Color(0xFFFF3B30)
    // Destructive button with white text on it: darker than the text red so white reaches 4.5:1
    val DestructiveButton = Color(0xFFD70015)
    // Popups and dark glass
    val PopupDark = Color(0xFF1C1C1E)
    // Link and PIN accent
    val AccentBlue = Color(0xFF0A84FF)
    // Accent of the Android style
    val AndroidAccent = Color(0xFF6750A4)
}
