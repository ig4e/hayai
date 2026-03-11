package eu.kanade.tachiyomi.ui.more

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.HistoryScreenModel
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.stats.StatsScreen
import eu.kanade.tachiyomi.ui.updates.UpdatesScreenModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data object RecentsTab : Tab {

    private val showDownloadsChannel = Channel<Unit>(1, BufferOverflow.DROP_OLDEST)

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
        showDownloadsChannel.trySend(Unit)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
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
            onDeleteHistory = historyScreenModel::removeFromHistory,
            onClickUpdateCover = { navigator.push(MangaScreen(it)) },
            onClickUpdate = { mangaId, chapterId ->
                context.startActivity(ReaderActivity.newIntent(context, mangaId, chapterId))
            },
            onOpenStats = { navigator.push(StatsScreen()) },
            onOpenSettings = { navigator.push(SettingsScreen()) },
        )

        LaunchedEffect(Unit) {
            showDownloadsChannel.receiveAsFlow().collectLatest {
                navigator.push(DownloadQueueScreen)
            }
        }
    }
}
