package yokai.presentation.extension.repo

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.CrossfadeTransition
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import yokai.i18n.MR

class ExtensionRepoController(private val repoUrl: String? = null) : BaseComposeController() {

    @Composable
    override fun ScreenContent() {
        Navigator(
            screen = ExtensionRepoScreen(
                title = stringResource(MR.strings.repos),
                repoUrl = repoUrl,
            ),
            content = {
                CrossfadeTransition(navigator = it)
            },
        )
    }
}
