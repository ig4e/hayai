package hayai.novel.tts.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import eu.kanade.tachiyomi.util.compose.LocalBackPress
import eu.kanade.tachiyomi.util.compose.currentOrThrow
import yokai.presentation.AppBarType
import yokai.presentation.YokaiScaffold
import yokai.presentation.core.enterAlwaysAppBarScrollBehavior
import yokai.util.Screen

/**
 * Voyager wrapper around [TtsSettingsContent] for app-level navigation. The reader-side entry
 * uses [TtsLaunchSheet] which hosts the same content directly without this scaffold.
 */
class TtsSettingsScreen : Screen() {

    @Composable
    override fun Content() {
        val onBackPress = LocalBackPress.currentOrThrow
        val screenModel = rememberScreenModel { TtsSettingsScreenModel() }

        YokaiScaffold(
            onNavigationIconClicked = onBackPress,
            title = "Text-to-speech",
            appBarType = AppBarType.SMALL,
            scrollBehavior = enterAlwaysAppBarScrollBehavior(),
        ) { innerPadding ->
            TtsSettingsContent(
                screenModel = screenModel,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
