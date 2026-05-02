package yokai.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import com.google.accompanist.themeadapter.material3.createMdc3Theme

@Composable
fun YokaiTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current

    val (colourScheme) =
        @Suppress("DEPRECATION")
        createMdc3Theme(
            context = context,
            layoutDirection = LayoutDirection.Rtl,
            setTextColors = true,
            readTypography = false,
        )

    // Observe via the central accessor so toggling reduced motion recomposes the tree.
    val reducedMotion by ReducedMotion.changes().collectAsState(initial = ReducedMotion.isEnabled())

    MaterialTheme(colorScheme = colourScheme!!) {
        CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
            content()
        }
    }
}
