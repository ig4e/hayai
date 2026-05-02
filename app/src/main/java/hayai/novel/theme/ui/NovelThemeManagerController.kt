package hayai.novel.theme.ui

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.CrossfadeTransition
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController

/**
 * Conductor wrapper around [NovelThemeManagerScreen]. Push from anywhere with a router via
 * `router.pushController(NovelThemeManagerController().withFadeTransaction())`.
 *
 * Used as the entry point from app-level settings; the in-reader entry point launches the same
 * Compose content via a bottom-sheet host instead (so it can sit inside [ReaderActivity] without
 * bouncing the user back to [MainActivity]).
 */
class NovelThemeManagerController : BaseComposeController() {

    @Composable
    override fun ScreenContent() {
        Navigator(
            screen = NovelThemeManagerScreen(),
            content = { CrossfadeTransition(navigator = it) },
        )
    }
}
