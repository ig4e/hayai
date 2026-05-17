package eu.kanade.tachiyomi.ui.setting

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.CrossfadeTransition
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import yokai.presentation.theme.LocalReducedMotion
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.util.view.isControllerVisible
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

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isEnter && isControllerVisible) {
            (activity as? eu.kanade.tachiyomi.ui.main.MainActivity)?.chromeBinder?.bind(this, describeChrome())
        }
    }

    override fun describeChrome(): eu.kanade.tachiyomi.ui.main.chrome.ChromeSpec =
        eu.kanade.tachiyomi.ui.main.chrome.ChromeSpec(
            appBarVisible = true,
            includeTabsInLayout = false,
            scrollSource = null,
            useSmallToolbar = true,
            tabs = null,
        )
}
