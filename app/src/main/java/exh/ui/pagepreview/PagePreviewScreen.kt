package exh.ui.pagepreview

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import coil3.ImageLoader
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.compose.LocalBackPress
import eu.kanade.tachiyomi.util.compose.currentOrThrow
import exh.ui.pagepreview.components.PagePreviewContent
import yokai.util.Screen
import yokai.util.coil.appImageLoader
import yokai.util.coil.loaderForSource

class PagePreviewScreen(private val mangaId: Long) : Screen() {

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { PagePreviewScreenModel(mangaId) }
        val context = LocalContext.current
        val state by screenModel.state.collectAsState()
        val onBackPress = LocalBackPress.currentOrThrow

        // Key only on the source instance so the loader survives state updates from
        // loadMore — otherwise every appended page rebuilds the loader and already-
        // rendered thumbnails flicker. The source-routed loader inherits memory cache,
        // disk cache, decoders, and dispatcher pools from the app singleton, so an
        // earlier preview that loaded via the singleton stays warm here.
        val source = (state as? PagePreviewState.Success)?.source
        val imageLoader: ImageLoader = remember(context, source) {
            val sourceClient = (source as? HttpSource)?.client
            if (sourceClient != null) loaderForSource(context, sourceClient) else appImageLoader(context)
        }

        PagePreviewContent(
            state = state,
            imageLoader = imageLoader,
            onOpenPage = { openPage(context, state, it) },
            onLoadMore = screenModel::loadMore,
            onJumpToPage = screenModel::jumpToPage,
            scrollEvents = screenModel.scrollEvents,
            navigateUp = onBackPress,
        )
    }

    private fun openPage(context: Context, state: PagePreviewState, page: Int) {
        if (state !is PagePreviewState.Success) return
        val intent = ReaderActivity.newIntent(context, state.manga, state.chapter, page)
        context.startActivity(intent)
    }
}
