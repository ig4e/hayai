package exh.ui.metadata

import android.os.Bundle
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController

class MetadataViewController(
    bundle: Bundle? = null,
) : BaseComposeController(bundle) {

    constructor(mangaId: Long, sourceId: Long) : this(
        Bundle().apply {
            putLong(MANGA_ID_KEY, mangaId)
            putLong(SOURCE_ID_KEY, sourceId)
        },
    )

    private val mangaId: Long get() = args.getLong(MANGA_ID_KEY)
    private val sourceId: Long get() = args.getLong(SOURCE_ID_KEY)

    @Composable
    override fun ScreenContent() {
        Navigator(
            screen = MetadataViewScreen(mangaId, sourceId),
        )
    }

    companion object {
        private const val MANGA_ID_KEY = "manga_id"
        private const val SOURCE_ID_KEY = "source_id"
    }
}
