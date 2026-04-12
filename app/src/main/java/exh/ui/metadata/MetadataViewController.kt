package exh.ui.metadata

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController

class MetadataViewController(
    private val mangaId: Long,
    private val sourceId: Long,
) : BaseComposeController() {

    @Composable
    override fun ScreenContent() {
        Navigator(
            screen = MetadataViewScreen(mangaId, sourceId),
        )
    }
}
