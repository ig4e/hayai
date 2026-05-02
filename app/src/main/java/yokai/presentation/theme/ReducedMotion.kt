package yokai.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf

/**
 * Whether the user has opted into reduced motion (Appearance → Motion → Reduce motion).
 *
 * Compose code that runs an animation should check this and either skip the animation
 * entirely or use a snap/instant spec. The CompositionLocal is provided by [YokaiTheme]
 * — accessing it outside the theme falls back to `false`.
 */
val LocalReducedMotion = compositionLocalOf { false }

/** Convenience accessor for non-`@Composable` call sites that already have a composer. */
val isReducedMotion: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalReducedMotion.current
