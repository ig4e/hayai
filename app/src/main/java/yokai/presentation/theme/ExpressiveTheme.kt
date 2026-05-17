package yokai.presentation.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import com.google.accompanist.themeadapter.material3.createMdc3Theme

/**
 * M3 Expressive theme wrapper. Mirrors [YokaiTheme]'s color extraction from the app's MDC3 XML
 * theme so widget colors stay consistent across XML and Compose surfaces, but wraps the content
 * in [MaterialExpressiveTheme] to opt into the Expressive component defaults (tonal pill shapes,
 * spring motion, larger emphasis).
 *
 * Scoped to call sites that explicitly target M3 Expressive (currently the source-browse filter
 * sheet). Global [YokaiTheme] is left untouched until the rest of the app is migrated.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveYokaiTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current

    val (colourScheme) =
        @Suppress("DEPRECATION")
        createMdc3Theme(
            context = context,
            layoutDirection = LayoutDirection.Rtl,
            setTextColors = true,
            readTypography = false,
        )

    val reducedMotion by ReducedMotion.changes().collectAsState(initial = ReducedMotion.isEnabled())

    MaterialExpressiveTheme(colorScheme = colourScheme!!) {
        CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
            content()
        }
    }
}
