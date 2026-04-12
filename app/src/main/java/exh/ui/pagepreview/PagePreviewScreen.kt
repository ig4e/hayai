package exh.ui.pagepreview

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.compose.LocalBackPress
import eu.kanade.tachiyomi.util.compose.currentOrThrow
import exh.ui.pagepreview.components.PagePreviewContent
import yokai.util.Screen

class PagePreviewScreen(private val mangaId: Long) : Screen() {

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { PagePreviewScreenModel(mangaId) }
        val context = LocalContext.current
        val state by screenModel.state.collectAsState()
        val onBackPress = LocalBackPress.currentOrThrow
        PagePreviewContent(
            state = state,
            pageDialogOpen = screenModel.pageDialogOpen,
            onPageSelected = screenModel::moveToPage,
            onOpenPage = { openPage(context, state, it) },
            onOpenPageDialog = { screenModel.pageDialogOpen = true },
            onDismissPageDialog = { screenModel.pageDialogOpen = false },
            navigateUp = onBackPress,
        )
    }

    private fun openPage(context: Context, state: PagePreviewState, page: Int) {
        if (state !is PagePreviewState.Success) return
        val intent = ReaderActivity.newIntent(context, state.manga, state.chapter)
        context.startActivity(intent)
    }
}
