package eu.kanade.tachiyomi.ui.more

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import tachiyomi.i18n.MR

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
            onDeleteHistory = { history, removeEverything ->
                if (removeEverything) {
                    historyScreenModel.removeAllFromHistory(history.mangaId)
                } else {
                    historyScreenModel.removeFromHistory(history)
                }
            },
            onClearHistory = historyScreenModel::removeAllHistory,
            onClickUpdateCover = { navigator.push(MangaScreen(it)) },
            onClickUpdate = { mangaId, chapterId ->
                context.startActivity(ReaderActivity.newIntent(context, mangaId, chapterId))
            },
            onUpdateSelected = updatesScreenModel::toggleSelection,
            onUpdateSwipeRead = { item, read ->
                updatesScreenModel.markUpdatesRead(listOf(item), read)
            },
            onUpdateSwipeDownload = { item, action ->
                updatesScreenModel.downloadChapters(listOf(item), action)
            },
            onDownloadUpdates = updatesScreenModel::downloadChapters,
            onMultiBookmarkUpdates = updatesScreenModel::bookmarkUpdates,
            onMultiMarkAsReadUpdates = updatesScreenModel::markUpdatesRead,
            onMultiDeleteUpdates = updatesScreenModel::showConfirmDeleteChapters,
            onOpenStats = { navigator.push(StatsScreen()) },
            onOpenSettings = { navigator.push(SettingsScreen()) },
        )

        when (val dialog = updatesState.dialog) {
            is UpdatesScreenModel.Dialog.DeleteConfirmation -> {
                eu.kanade.presentation.updates.UpdatesDeleteConfirmationDialog(
                    onDismissRequest = { updatesScreenModel.setDialog(null) },
                    onConfirm = { updatesScreenModel.deleteChapters(dialog.toDelete) },
                )
            }
            UpdatesScreenModel.Dialog.FilterSheet,
            null,
            -> Unit
        }

        LaunchedEffect(historyScreenModel) {
            historyScreenModel.events.collectLatest { event ->
                if (event is HistoryScreenModel.Event.HistoryCleared) {
                    context.toast(MR.strings.clear_history_completed)
                }
            }
        }
    }
}
