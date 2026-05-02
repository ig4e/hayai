package hayai.novel.tts.ui

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.CrossfadeTransition
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController

/**
 * Conductor wrapper for [TtsSettingsScreen]. Push from anywhere with a router via
 * `router.pushController(TtsSettingsController().withFadeTransaction())`.
 */
class TtsSettingsController : BaseComposeController() {

    @Composable
    override fun ScreenContent() {
        Navigator(
            screen = TtsSettingsScreen(),
            content = { CrossfadeTransition(navigator = it) },
        )
    }
}
