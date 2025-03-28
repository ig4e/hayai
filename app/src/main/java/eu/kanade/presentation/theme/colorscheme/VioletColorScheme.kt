package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Violet theme with Zinc background
 * Based on Tailwind CSS Violet and Zinc color palettes
 * https://tailwindcss.com/docs/customizing-colors
 */
internal object VioletColorScheme : BaseColorScheme() {

    override val lightScheme = lightColorScheme(
        // Violet colors for primaries and accents - more vibrant
        primary = Color(0xFF6D28DD),          // Rich violet purple
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF8B4FF7),  // Vibrant purple
        onPrimaryContainer = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFFD3BDFF),    // Light lavender
        secondary = Color(0xFF6D28DD),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFEDE0FF), // Soft violet
        onSecondaryContainer = Color(0xFF2E0C7A), // Deep violet
        tertiary = Color(0xFF5B20C4),          // Deep violet
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFEFE0FF), // Soft violet
        onTertiaryContainer = Color(0xFF2B0B66), // Very deep violet

        // Zinc colors for backgrounds and surfaces
        background = Color(0xFFfafafa),
        onBackground = Color(0xFF18181b),
        surface = Color(0xFFf4f4f5),
        onSurface = Color(0xFF18181b),
        surfaceVariant = Color(0xFFe4e4e7),
        onSurfaceVariant = Color(0xFF3f3f46),
        surfaceTint = Color(0xFF6D28DD),       // Match primary
        inverseSurface = Color(0xFF27272a),
        inverseOnSurface = Color(0xFFf4f4f5),
        outline = Color(0xFF71717a),
        outlineVariant = Color(0xFFa1a1aa),
        error = Color(0xFFba1a1a),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFffdad6),
        onErrorContainer = Color(0xFF410002),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFf4f4f5),
        surfaceContainer = Color(0xFFe4e4e7),
        surfaceContainerHigh = Color(0xFFd4d4d8),
        surfaceContainerHighest = Color(0xFFa1a1aa),
    )

    override val darkScheme = darkColorScheme(
        // Violet colors for primaries and accents - more vibrant
        primary = Color(0xFFD3BDFF),          // Light lavender
        onPrimary = Color(0xFF2E0C7A),        // Deep violet
        primaryContainer = Color(0xFF6D28DD),  // Rich violet
        onPrimaryContainer = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFF2E0C7A),    // Deep violet
        secondary = Color(0xFFD3BDFF),         // Light lavender
        onSecondary = Color(0xFF2E0C7A),       // Deep violet
        secondaryContainer = Color(0xFF5B20C4), // Deep violet
        onSecondaryContainer = Color(0xFFEFE0FF), // Soft violet
        tertiary = Color(0xFFEFE0FF),          // Soft violet
        onTertiary = Color(0xFF2E0C7A),        // Deep violet
        tertiaryContainer = Color(0xFF4913AA),  // Strong violet
        onTertiaryContainer = Color(0xFFEFE0FF), // Soft violet

        // Zinc colors for backgrounds and surfaces
        background = Color(0xFF18181b),
        onBackground = Color(0xFFf4f4f5),
        surface = Color(0xFF27272a),
        onSurface = Color(0xFFf4f4f5),
        surfaceVariant = Color(0xFF3f3f46),
        onSurfaceVariant = Color(0xFFd4d4d8),
        surfaceTint = Color(0xFFD3BDFF),       // Match primary
        inverseSurface = Color(0xFFf4f4f5),
        inverseOnSurface = Color(0xFF27272a),
        outline = Color(0xFFa1a1aa),
        outlineVariant = Color(0xFF52525b),
        error = Color(0xFFffb4ab),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000a),
        onErrorContainer = Color(0xFFffdad6),
        surfaceContainerLowest = Color(0xFF09090b),
        surfaceContainerLow = Color(0xFF18181b),
        surfaceContainer = Color(0xFF27272a),
        surfaceContainerHigh = Color(0xFF3f3f46),
        surfaceContainerHighest = Color(0xFF52525b),
    )
}
