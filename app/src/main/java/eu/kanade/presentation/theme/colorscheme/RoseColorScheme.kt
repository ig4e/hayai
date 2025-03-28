package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Rose theme with Zinc background
 * Based on Tailwind CSS Rose and Zinc color palettes
 * https://tailwindcss.com/docs/customizing-colors
 */
internal object RoseColorScheme : BaseColorScheme() {

    override val lightScheme = lightColorScheme(
        // Rose colors for primaries and accents - more vibrant
        primary = Color(0xFFE01039),          // Rich rose red
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFF2D56),  // Vibrant rose
        onPrimaryContainer = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFFFF91A4),    // Light rose pink
        secondary = Color(0xFFE01039),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFD9DE), // Soft rose
        onSecondaryContainer = Color(0xFF650017), // Deep rose
        tertiary = Color(0xFFB90030),          // Deep rose
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD9DE), // Soft rose
        onTertiaryContainer = Color(0xFF650017), // Deep rose

        // Zinc colors for backgrounds and surfaces
        background = Color(0xFFfafafa),
        onBackground = Color(0xFF18181b),
        surface = Color(0xFFf4f4f5),
        onSurface = Color(0xFF18181b),
        surfaceVariant = Color(0xFFe4e4e7),
        onSurfaceVariant = Color(0xFF3f3f46),
        surfaceTint = Color(0xFFE01039),       // Match primary
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
        // Rose colors for primaries and accents - more vibrant
        primary = Color(0xFFFF91A4),          // Light rose pink
        onPrimary = Color(0xFF650017),        // Deep rose
        primaryContainer = Color(0xFFE01039),  // Rich rose red
        onPrimaryContainer = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFF650017),    // Deep rose
        secondary = Color(0xFFFF91A4),         // Light rose pink
        onSecondary = Color(0xFF650017),       // Deep rose
        secondaryContainer = Color(0xFFB90030), // Deep rose
        onSecondaryContainer = Color(0xFFFFDADF), // Light rose
        tertiary = Color(0xFFFFDADF),          // Light rose
        onTertiary = Color(0xFF650017),        // Deep rose
        tertiaryContainer = Color(0xFF8C0025), // Strong rose
        onTertiaryContainer = Color(0xFFFFDADF), // Light rose

        // Zinc colors for backgrounds and surfaces
        background = Color(0xFF18181b),
        onBackground = Color(0xFFf4f4f5),
        surface = Color(0xFF27272a),
        onSurface = Color(0xFFf4f4f5),
        surfaceVariant = Color(0xFF3f3f46),
        onSurfaceVariant = Color(0xFFd4d4d8),
        surfaceTint = Color(0xFFFF91A4),       // Match primary
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
