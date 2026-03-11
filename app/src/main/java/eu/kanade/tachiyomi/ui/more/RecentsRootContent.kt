package eu.kanade.tachiyomi.ui.more

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.presentation.history.HistoryUiModel
import eu.kanade.presentation.more.RecentsHistoryScreen
import eu.kanade.presentation.more.RecentsScreen as RecentsCombinedScreen
import eu.kanade.presentation.more.RecentsUpdatesScreen
import eu.kanade.tachiyomi.ui.download.DownloadQueuePane
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecentsRootContent(
    navigateUp: (() -> Unit)? = null,
    historyItems: List<HistoryUiModel.Item>,
    updateItems: List<UpdatesItem>,
    onClickHistoryCover: (Long) -> Unit,
    onClickHistory: (Long, Long) -> Unit,
    onClickHistoryFavorite: (Long) -> Unit,
    onDeleteHistory: (HistoryWithRelations, Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onClickUpdateCover: (Long) -> Unit,
    onClickUpdate: (Long, Long) -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showDownloadQueue by rememberSaveable { mutableStateOf(false) }
    var showClearHistoryDialog by rememberSaveable { mutableStateOf(false) }
    val normalizedQuery = remember(searchQuery) {
        searchQuery
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.lowercase()
    }
    val filteredHistoryItems = remember(historyItems, normalizedQuery) {
        historyItems.filter { item ->
            normalizedQuery == null ||
                item.item.title.lowercase().contains(normalizedQuery)
        }
    }
    val filteredUpdateItems = remember(updateItems, normalizedQuery) {
        updateItems.filter { item ->
            normalizedQuery == null ||
                item.update.mangaTitle.lowercase().contains(normalizedQuery) ||
                item.update.chapterName.lowercase().contains(normalizedQuery)
        }
    }
    val pagerState = rememberPagerState(initialPage = selectedTabIndex) { 3 }

    LaunchedEffect(pagerState.currentPage) {
        selectedTabIndex = pagerState.currentPage
    }

    if (showDownloadQueue) {
        ModalBottomSheet(
            onDismissRequest = { showDownloadQueue = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            DownloadQueuePane(
                navigateUp = { showDownloadQueue = false },
                screenModelTag = if (navigateUp == null) {
                    "recents_tab_download_queue"
                } else {
                    "recents_screen_download_queue"
                },
            )
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            text = { Text(text = stringResource(MR.strings.clear_history_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryDialog = false
                        onClearHistory()
                    },
                ) {
                    Text(text = stringResource(MR.strings.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            },
        )
    }

    TabbedScreen(
        titleRes = MR.strings.label_recents,
        state = pagerState,
        tabs = persistentListOf(
            TabContent(
                titleRes = MR.strings.all,
                searchEnabled = true,
                searchQuery = searchQuery,
                onChangeSearchQuery = { searchQuery = it },
            ) { _, _ ->
                RecentsCombinedScreen(
                    historyItems = filteredHistoryItems,
                    updateItems = filteredUpdateItems,
                    onClickHistoryCover = onClickHistoryCover,
                    onClickHistory = onClickHistory,
                    onClickHistoryFavorite = onClickHistoryFavorite,
                    onDeleteHistory = onDeleteHistory,
                    onClickUpdateCover = onClickUpdateCover,
                    onClickUpdate = onClickUpdate,
                )
            },
            TabContent(
                titleRes = MR.strings.history,
                searchEnabled = true,
                searchQuery = searchQuery,
                onChangeSearchQuery = { searchQuery = it },
            ) { _, _ ->
                RecentsHistoryScreen(
                    historyItems = filteredHistoryItems,
                    onClickHistoryCover = onClickHistoryCover,
                    onClickHistory = onClickHistory,
                    onClickHistoryFavorite = onClickHistoryFavorite,
                    onDeleteHistory = onDeleteHistory,
                )
            },
            TabContent(
                titleRes = MR.strings.label_recent_updates,
                searchEnabled = true,
                searchQuery = searchQuery,
                onChangeSearchQuery = { searchQuery = it },
            ) { _, _ ->
                RecentsUpdatesScreen(
                    updateItems = filteredUpdateItems,
                    onClickUpdateCover = onClickUpdateCover,
                    onClickUpdate = onClickUpdate,
                )
            },
        ),
        navigateUp = navigateUp,
        rootActions = persistentListOf(
            AppBar.Action(
                title = stringResource(MR.strings.label_download_queue),
                icon = Icons.Outlined.GetApp,
                onClick = { showDownloadQueue = true },
            ),
            AppBar.Action(
                title = stringResource(MR.strings.label_stats),
                icon = Icons.Outlined.QueryStats,
                onClick = onOpenStats,
            ),
            AppBar.Action(
                title = stringResource(MR.strings.action_settings),
                icon = Icons.Outlined.Settings,
                onClick = onOpenSettings,
            ),
            AppBar.Action(
                title = stringResource(MR.strings.pref_clear_history),
                icon = Icons.Outlined.DeleteSweep,
                onClick = { showClearHistoryDialog = true },
            ),
        ),
    )
}
