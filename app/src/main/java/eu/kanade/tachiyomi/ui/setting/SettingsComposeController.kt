package eu.kanade.tachiyomi.ui.setting

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.CrossfadeTransition
import yokai.presentation.theme.LocalReducedMotion
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import yokai.presentation.settings.ComposableSettings

abstract class SettingsComposeController: BaseComposeController(), SettingsControllerInterface {
    // The Compose body renders its own top bar via YokaiScaffold inside the Voyager
    // Navigator screen tree; the activity-global appBar must stay hidden behind it.
    override val hostsOwnAppBar: Boolean = true

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
}
