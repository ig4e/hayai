package tachiyomi.presentation.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import tachiyomi.presentation.core.R

/**
 * Defines the Outfit font family to be used throughout the application
 */
val OutfitFont = FontFamily(
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

/**
 * Creates a Typography instance using Outfit font family
 */
fun createOutfitTypography(): Typography {
    return Typography().copy(
        displayLarge = Typography().displayLarge.copy(fontFamily = OutfitFont),
        displayMedium = Typography().displayMedium.copy(fontFamily = OutfitFont),
        displaySmall = Typography().displaySmall.copy(fontFamily = OutfitFont),

        headlineLarge = Typography().headlineLarge.copy(fontFamily = OutfitFont),
        headlineMedium = Typography().headlineMedium.copy(fontFamily = OutfitFont),
        headlineSmall = Typography().headlineSmall.copy(fontFamily = OutfitFont),

        titleLarge = Typography().titleLarge.copy(fontFamily = OutfitFont),
        titleMedium = Typography().titleMedium.copy(fontFamily = OutfitFont),
        titleSmall = Typography().titleSmall.copy(fontFamily = OutfitFont),

        bodyLarge = Typography().bodyLarge.copy(fontFamily = OutfitFont),
        bodyMedium = Typography().bodyMedium.copy(fontFamily = OutfitFont),
        bodySmall = Typography().bodySmall.copy(fontFamily = OutfitFont),

        labelLarge = Typography().labelLarge.copy(fontFamily = OutfitFont),
        labelMedium = Typography().labelMedium.copy(fontFamily = OutfitFont),
        labelSmall = Typography().labelSmall.copy(fontFamily = OutfitFont),
    )
}
