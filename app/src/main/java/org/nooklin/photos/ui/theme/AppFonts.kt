package org.nooklin.photos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import org.nooklin.photos.R
import org.nooklin.photos.data.local.AppFont

// Variable font used as one Font() entry, license in THIRD-PARTY-LICENSES/work-sans/OFL.txt
private val WorkSansFontFamily = FontFamily(Font(R.font.work_sans))

// SYSTEM_DEFAULT is FontFamily.Default, not hardcoded Roboto: some OEM skins substitute their own
fun fontFamilyFor(font: AppFont): FontFamily = when (font) {
    AppFont.SYSTEM_DEFAULT -> FontFamily.Default
    AppFont.WORK_SANS -> WorkSansFontFamily
}

fun typographyFor(font: AppFont): Typography {
    val family = fontFamilyFor(font)
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family)
    )
}
