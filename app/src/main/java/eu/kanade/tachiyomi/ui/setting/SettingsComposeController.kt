package eu.kanade.tachiyomi.ui.setting

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.CrossfadeTransition
import yokai.presentation.theme.LocalReducedMotion
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import yokai.presentation.settings.ComposableSettings

abstract class SettingsComposeController: BaseComposeController(), SettingsControllerInterface, eu.kanade.tachiyomi.ui.main.chrome.ChromeAware {
    override fun getTitle(): String? = __getTitle()
    override fun getSearchTitle(): String? = __getTitle()

    fun setTitle() = __setTitle()

    abstract fun getComposableSettings(): ComposableSettings

    @Composable
    override fun ScreenContent() {
        Navigator(
            screen = getComposableSettings(),
            content = { navigator ->
                if (LocalReducedMotion.current) CurrentScreen()
                else CrossfadeTransition(navigator = navigator)
            },
        )
    }


    override fun describeChrome(): eu.kanade.tachiyomi.ui.main.chrome.ChromeSpec =
        eu.kanade.tachiyomi.ui.main.chrome.ChromeSpec(
            appBarVisible = true,
            scrollSource = null,
            useSmallToolbar = true,
            tabs = null,
        )
}
