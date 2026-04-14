package exh.ui.pagepreview

import android.os.Bundle
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController

class PagePreviewViewController(
    bundle: Bundle? = null,
) : BaseComposeController(bundle) {

    constructor(mangaId: Long) : this(
        Bundle().apply {
            putLong(MANGA_ID_KEY, mangaId)
        },
    )

    private val mangaId: Long get() = args.getLong(MANGA_ID_KEY)

    @Composable
    override fun ScreenContent() {
        Navigator(
            screen = PagePreviewScreen(mangaId),
        )
    }

    companion object {
        private const val MANGA_ID_KEY = "manga_id"
    }
}
