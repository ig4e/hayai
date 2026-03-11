package eu.kanade.tachiyomi.ui.more

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.history.HistoryScreenModel
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.more.RecentsTab.Command.OpenDownloads
import eu.kanade.tachiyomi.ui.more.RecentsTab.Command.OpenHistory
import eu.kanade.tachiyomi.ui.more.RecentsTab.Command.OpenUpdates
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.stats.StatsScreen
import eu.kanade.tachiyomi.ui.updates.UpdatesScreenModel
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data object RecentsTab : Tab {

    private val commandChannel = Channel<Command>(1, BufferOverflow.DROP_OLDEST)

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_updates_enter)
            return TabOptions(
                index = 1u,
                title = stringResource(MR.strings.label_recents),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(RecentsScreen())
    }

    fun showDownloads() {
        commandChannel.trySend(OpenDownloads)
    }

    fun openUpdates() {
        commandChannel.trySend(OpenUpdates)
    }

    fun openHistory() {
        commandChannel.trySend(OpenHistory)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        var startTabIndex by remember { mutableIntStateOf(-1) }
        var pendingOpenDownloads by remember { mutableStateOf(false) }
        val historyScreenModel = rememberScreenModel(tag = "recents_tab_history") { HistoryScreenModel() }
        val updatesScreenModel = rememberScreenModel(tag = "recents_tab_updates") { UpdatesScreenModel() }
        val historyState by historyScreenModel.state.collectAsState()
        val updatesState by updatesScreenModel.state.collectAsState()

        RecentsRootContent(
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
            startTabIndex = startTabIndex.takeIf { it >= 0 },
            onStartTabConsumed = { startTabIndex = -1 },
            openDownloadQueue = pendingOpenDownloads,
            onDownloadQueueOpened = { pendingOpenDownloads = false },
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

        LaunchedEffect(Unit) {
            commandChannel.receiveAsFlow().collectLatest { command ->
                when (command) {
                    OpenDownloads -> pendingOpenDownloads = true
                    OpenUpdates -> startTabIndex = 2
                    OpenHistory -> startTabIndex = 1
                }
            }
        }

        LaunchedEffect(historyScreenModel) {
            historyScreenModel.events.collectLatest { event ->
                if (event is HistoryScreenModel.Event.HistoryCleared) {
                    context.toast(MR.strings.clear_history_completed)
                }
            }
        }
    }

    private sealed interface Command {
        data object OpenDownloads : Command
        data object OpenUpdates : Command
        data object OpenHistory : Command
    }
}
