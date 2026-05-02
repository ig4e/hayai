package yokai.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import com.google.accompanist.themeadapter.material3.createMdc3Theme
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

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

    val reducedMotionPref = remember { Injekt.get<PreferencesHelper>().reducedMotion() }
    val reducedMotion by reducedMotionPref.changes().collectAsState(initial = reducedMotionPref.get())

    MaterialTheme(colorScheme = colourScheme!!) {
        CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
            content()
        }
    }
}
