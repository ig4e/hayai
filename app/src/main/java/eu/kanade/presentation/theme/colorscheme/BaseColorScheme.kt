package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

internal abstract class BaseColorScheme {

    abstract val darkScheme: ColorScheme
    abstract val lightScheme: ColorScheme

    // Cannot be pure black as there's content scrolling behind it
    // https://m3.material.io/components/navigation-bar/guidelines#90615a71-607e-485e-9e09-778bfc080563
    private val surfaceContainer = Color(0xFF060606)
    private val surfaceContainerHigh = Color(0xFF0A0A0A)
    private val surfaceContainerHighest = Color(0xFF121212)

    fun getColorScheme(
        isDark: Boolean,
        isAmoled: Boolean,
        overrideDarkSurfaceContainers: Boolean = true,
    ): ColorScheme {
        if (!isDark) return lightScheme

        if (!isAmoled) {
            return if (overrideDarkSurfaceContainers) {
                darkScheme.copy(
                    surfaceVariant = surfaceContainer,
                    surfaceContainerLowest = surfaceContainer,
                    surfaceContainerLow = surfaceContainer,
                    surfaceContainer = surfaceContainer,
                    surfaceContainerHigh = surfaceContainerHigh,
                    surfaceContainerHighest = surfaceContainerHighest,
                )
            } else {
                darkScheme
            }
        }

        return darkScheme.copy(
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White,
            surfaceVariant = surfaceContainer, // Navigation bar background (ThemePrefWidget)
            surfaceContainerLowest = surfaceContainer,
            surfaceContainerLow = surfaceContainer,
            surfaceContainer = surfaceContainer, // Navigation bar background
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
        )
    }
}
