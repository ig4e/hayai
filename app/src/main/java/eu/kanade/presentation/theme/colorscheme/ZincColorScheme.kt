package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Zinc theme
 * Based on Tailwind CSS Zinc color palette
 * https://tailwindcss.com/docs/customizing-colors
 */
internal object ZincColorScheme : BaseColorScheme() {

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF52525b),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF71717a),
        onPrimaryContainer = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFFa1a1aa),
        secondary = Color(0xFF52525b),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFd4d4d8),
        onSecondaryContainer = Color(0xFF27272a),
        tertiary = Color(0xFF71717a),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFe4e4e7),
        onTertiaryContainer = Color(0xFF27272a),
        background = Color(0xFFfafafa),
        onBackground = Color(0xFF18181b),
        surface = Color(0xFFf4f4f5),
        onSurface = Color(0xFF18181b),
        surfaceVariant = Color(0xFFe4e4e7),
        onSurfaceVariant = Color(0xFF3f3f46),
        surfaceTint = Color(0xFF52525b),
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
        primary = Color(0xFFa1a1aa),
        onPrimary = Color(0xFF27272a),
        primaryContainer = Color(0xFF52525b),
        onPrimaryContainer = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFF3f3f46),
        secondary = Color(0xFFa1a1aa),
        onSecondary = Color(0xFF27272a),
        secondaryContainer = Color(0xFF52525b),
        onSecondaryContainer = Color(0xFFf4f4f5),
        tertiary = Color(0xFFd4d4d8),
        onTertiary = Color(0xFF27272a),
        tertiaryContainer = Color(0xFF3f3f46),
        onTertiaryContainer = Color(0xFFf4f4f5),
        background = Color(0xFF09090b),
        onBackground = Color(0xFFf4f4f5),
        surface = Color(0xFF18181b),
        onSurface = Color(0xFFf4f4f5),
        surfaceVariant = Color(0xFF27272a),
        onSurfaceVariant = Color(0xFFd4d4d8),
        surfaceTint = Color(0xFFa1a1aa),
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
