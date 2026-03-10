package eu.kanade.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import eu.kanade.tachiyomi.R

private val OutfitFontFamily = FontFamily(
    Font(R.font.outfit_thin, FontWeight.Thin),
    Font(R.font.outfit_extralight, FontWeight.ExtraLight),
    Font(R.font.outfit_light, FontWeight.Light),
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
    Font(R.font.outfit_extrabold, FontWeight.ExtraBold),
    Font(R.font.outfit_black, FontWeight.Black),
)

val HayaiTypography: Typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = OutfitFontFamily),
        displayMedium = displayMedium.copy(fontFamily = OutfitFontFamily),
        displaySmall = displaySmall.copy(fontFamily = OutfitFontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = OutfitFontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = OutfitFontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = OutfitFontFamily),
        titleLarge = titleLarge.copy(fontFamily = OutfitFontFamily),
        titleMedium = titleMedium.copy(fontFamily = OutfitFontFamily),
        titleSmall = titleSmall.copy(fontFamily = OutfitFontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = OutfitFontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = OutfitFontFamily),
        bodySmall = bodySmall.copy(fontFamily = OutfitFontFamily),
        labelLarge = labelLarge.copy(fontFamily = OutfitFontFamily),
        labelMedium = labelMedium.copy(fontFamily = OutfitFontFamily),
        labelSmall = labelSmall.copy(fontFamily = OutfitFontFamily),
    )
}
