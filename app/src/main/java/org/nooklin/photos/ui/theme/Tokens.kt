package org.nooklin.photos.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Design tokens. Screens refer to these instead of writing sizes and colors in place

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

// Text sizes by role. Body is never below 14 sp, Caption (12 sp) is for meta information only
object AppText {
    val Caption = 12.sp
    val Body = 14.sp
    val BodyLarge = 16.sp
    val Title = 22.sp
    val Headline = 28.sp
}

val MinTouchTarget = 48.dp

object Motion {
    const val Short = 150
    const val Medium = 300
    const val Long = 450
}

object AppColors {
    val SelectedYellow = Color(0xFFFFD60A)
    val DestructiveText = Color(0xFFFF3B30)
    val DestructiveButton = Color(0xFFD70015)
    val PopupDark = Color(0xFF1C1C1E)
    val AccentBlue = Color(0xFF0A84FF)
    val AndroidAccent = Color(0xFF6750A4)
}
