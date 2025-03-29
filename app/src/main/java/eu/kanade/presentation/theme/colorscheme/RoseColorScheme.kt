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
        // Rose colors for primaries and accents - moderate (400-600 range)
        primary = Color(0xFFF43F5E),          // Rose-500
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFB7185),  // Rose-400
        onPrimaryContainer = Color(0xFF881337),
        inversePrimary = Color(0xFFFDA4AF),    // Rose-300 -> Kept as is, not a primary color
        secondary = Color(0xFFF43F5E),         // Rose-500
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFB7185), // Rose-400
        onSecondaryContainer = Color(0xFF881337), // Rose-900
        tertiary = Color(0xFFE11D48),          // Rose-600
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFB7185), // Rose-400
        onTertiaryContainer = Color(0xFF881337), // Rose-900

        // Zinc colors for backgrounds and surfaces
        background = Color(0xFFfafafa),
        onBackground = Color(0xFF18181b),
        surface = Color(0xFFf4f4f5),
        onSurface = Color(0xFF18181b),
        surfaceVariant = Color(0xFFe4e4e7),
        onSurfaceVariant = Color(0xFF3f3f46),
        surfaceTint = Color(0xFFF43F5E),       // Match primary
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
        // Rose colors for primaries and accents - moderate (400-600 range)
        primary = Color(0xFFFB7185),          // Rose-400 (was 300)
        onPrimary = Color(0xFF881337),        // Rose-900
        primaryContainer = Color(0xFFE11D48),  // Rose-600
        onPrimaryContainer = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFF881337),    // Rose-900
        secondary = Color(0xFFFB7185),         // Rose-400 (was 300)
        onSecondary = Color(0xFF881337),       // Rose-900
        secondaryContainer = Color(0xFFE11D48), // Rose-600 (was 700)
        onSecondaryContainer = Color(0xFFFFE4E6), // Rose-100
        tertiary = Color(0xFFFB7185),          // Rose-400 (was 200)
        onTertiary = Color(0xFF881337),        // Rose-900
        tertiaryContainer = Color(0xFFE11D48),  // Rose-600 (was 800)
        onTertiaryContainer = Color(0xFFFFE4E6), // Rose-100

        // Zinc colors for backgrounds and surfaces
        background = Color(0xFF09090b),         // Zinc-950
        onBackground = Color(0xFFf4f4f5),
        surface = Color(0xFF18181b),            // Zinc-900
        onSurface = Color(0xFFf4f4f5),
        surfaceVariant = Color(0xFF27272a),     // Zinc-800
        onSurfaceVariant = Color(0xFFd4d4d8),
        surfaceTint = Color(0xFFFB7185),       // Match primary
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
