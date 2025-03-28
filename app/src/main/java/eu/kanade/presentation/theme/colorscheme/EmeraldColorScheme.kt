package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Emerald theme with Zinc background
 * Based on Tailwind CSS Emerald and Zinc color palettes
 * https://tailwindcss.com/docs/customizing-colors
 */
internal object EmeraldColorScheme : BaseColorScheme() {

    override val lightScheme = lightColorScheme(
        // Emerald colors for primaries and accents - more vibrant
        primary = Color(0xFF00875A),           // Deeper emerald green
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF00C382),  // Brighter emerald
        onPrimaryContainer = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFF2AE5AB),    // Very bright emerald
        secondary = Color(0xFF00875A),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFF7DFFD4), // Bright mint green
        onSecondaryContainer = Color(0xFF003929), // Deep forest
        tertiary = Color(0xFF006647),          // Darker emerald
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFA0FFD9), // Bright mint
        onTertiaryContainer = Color(0xFF00261B), // Very dark forest

        // Zinc colors for backgrounds and surfaces
        background = Color(0xFFfafafa),
        onBackground = Color(0xFF18181b),
        surface = Color(0xFFf4f4f5),
        onSurface = Color(0xFF18181b),
        surfaceVariant = Color(0xFFe4e4e7),
        onSurfaceVariant = Color(0xFF3f3f46),
        surfaceTint = Color(0xFF00875A),       // Match primary
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
        // Emerald colors for primaries and accents - more vibrant
        primary = Color(0xFF2AE5AB),          // Bright emerald
        onPrimary = Color(0xFF00261B),        // Very dark forest
        primaryContainer = Color(0xFF00875A),  // Deep emerald
        onPrimaryContainer = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFF003929),    // Very dark emerald
        secondary = Color(0xFF2AE5AB),         // Bright emerald
        onSecondary = Color(0xFF00261B),       // Very dark forest
        secondaryContainer = Color(0xFF006647), // Deeper emerald
        onSecondaryContainer = Color(0xFFA0FFD9), // Very bright mint
        tertiary = Color(0xFF7DFFD4),          // Bright mint
        onTertiary = Color(0xFF003929),        // Very dark forest
        tertiaryContainer = Color(0xFF005237), // Dark emerald
        onTertiaryContainer = Color(0xFFA0FFD9), // Bright mint

        // Zinc colors for backgrounds and surfaces
        background = Color(0xFF18181b),
        onBackground = Color(0xFFf4f4f5),
        surface = Color(0xFF27272a),
        onSurface = Color(0xFFf4f4f5),
        surfaceVariant = Color(0xFF3f3f46),
        onSurfaceVariant = Color(0xFFd4d4d8),
        surfaceTint = Color(0xFF2AE5AB),       // Match primary
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
