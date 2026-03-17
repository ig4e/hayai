package eu.kanade.presentation.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import kotlinx.coroutines.launch
import tachiyomi.domain.recents.service.RecentsPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsOptionsSheet(
    onDismissRequest: () -> Unit,
    recentsPreferences: RecentsPreferences,
    initialTab: Int = 0,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialTab) { 3 }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text(stringResource(MR.strings.all)) },
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text(stringResource(MR.strings.history)) },
                )
                Tab(
                    selected = pagerState.currentPage == 2,
                    onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                    text = { Text(stringResource(MR.strings.label_recent_updates)) },
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                when (page) {
                    0 -> GeneralOptionsTab(recentsPreferences)
                    1 -> HistoryOptionsTab(recentsPreferences)
                    2 -> UpdatesOptionsTab(recentsPreferences)
                }
            }
        }
    }
}

@Composable
private fun GeneralOptionsTab(prefs: RecentsPreferences) {
    val showRemHistory by prefs.showRecentsRemHistory().collectAsState()
    val showReadInAll by prefs.showReadInAllRecents().collectAsState()
    val showTitleFirst by prefs.showTitleFirstInRecents().collectAsState()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
    ) {
        SwitchPreferenceWidget(
            title = stringResource(MR.strings.pref_show_remove_history),
            checked = showRemHistory,
            onCheckedChanged = { prefs.showRecentsRemHistory().set(it) },
        )
        SwitchPreferenceWidget(
            title = stringResource(MR.strings.pref_show_read_in_all),
            checked = showReadInAll,
            onCheckedChanged = { prefs.showReadInAllRecents().set(it) },
        )
        SwitchPreferenceWidget(
            title = stringResource(MR.strings.pref_show_title_first),
            checked = showTitleFirst,
            onCheckedChanged = { prefs.showTitleFirstInRecents().set(it) },
        )
    }
}

@Composable
private fun HistoryOptionsTab(prefs: RecentsPreferences) {
    val collapseGrouped by prefs.collapseGroupedHistory().collectAsState()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
    ) {
        SwitchPreferenceWidget(
            title = stringResource(MR.strings.pref_collapse_grouped),
            checked = collapseGrouped,
            onCheckedChanged = { prefs.collapseGroupedHistory().set(it) },
        )
    }
}

@Composable
private fun UpdatesOptionsTab(prefs: RecentsPreferences) {
    val showUpdatedTime by prefs.showUpdatedTime().collectAsState()
    val sortFetchedTime by prefs.sortFetchedTime().collectAsState()
    val collapseGrouped by prefs.collapseGroupedUpdates().collectAsState()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
    ) {
        SwitchPreferenceWidget(
            title = stringResource(MR.strings.pref_show_updated_time),
            checked = showUpdatedTime,
            onCheckedChanged = { prefs.showUpdatedTime().set(it) },
        )
        SwitchPreferenceWidget(
            title = stringResource(MR.strings.pref_sort_fetched_time),
            checked = sortFetchedTime,
            onCheckedChanged = { prefs.sortFetchedTime().set(it) },
        )
        SwitchPreferenceWidget(
            title = stringResource(MR.strings.pref_collapse_grouped_updates),
            checked = collapseGrouped,
            onCheckedChanged = { prefs.collapseGroupedUpdates().set(it) },
        )
    }
}
