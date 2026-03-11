package eu.kanade.tachiyomi.ui.more

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.history.HistoryScreenModel
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.stats.StatsScreen
import eu.kanade.tachiyomi.ui.updates.UpdatesScreenModel

class RecentsScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val historyScreenModel = rememberScreenModel(tag = "recents_screen_history") { HistoryScreenModel() }
        val updatesScreenModel = rememberScreenModel(tag = "recents_screen_updates") { UpdatesScreenModel() }
        val historyState by historyScreenModel.state.collectAsState()
        val updatesState by updatesScreenModel.state.collectAsState()

        RecentsRootContent(
            navigateUp = navigator::pop,
            historyItems = historyState.list.orEmpty().filterIsInstance<eu.kanade.presentation.history.HistoryUiModel.Item>(),
            updateItems = updatesState.items,
            onClickHistoryCover = { navigator.push(MangaScreen(it)) },
            onClickHistory = { mangaId, chapterId ->
                context.startActivity(ReaderActivity.newIntent(context, mangaId, chapterId))
            },
            onClickHistoryFavorite = historyScreenModel::addFavorite,
            onDeleteHistory = historyScreenModel::removeFromHistory,
            onClickUpdateCover = { navigator.push(MangaScreen(it)) },
            onClickUpdate = { mangaId, chapterId ->
                context.startActivity(ReaderActivity.newIntent(context, mangaId, chapterId))
            },
            onOpenStats = { navigator.push(StatsScreen()) },
            onOpenSettings = { navigator.push(SettingsScreen()) },
        )
    }
}
