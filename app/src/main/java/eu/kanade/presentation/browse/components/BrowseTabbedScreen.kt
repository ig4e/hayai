package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.components.TabContent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun BrowseTabbedScreen(
    titleRes: StringResource,
    tabs: ImmutableList<TabContent>,
    state: PagerState = rememberPagerState { tabs.size },
    rootActions: ImmutableList<AppBar.AppBarAction> = persistentListOf(),
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            val tab = tabs[state.currentPage]
            val searchEnabled = tab.searchEnabled

            SearchToolbar(
                titleContent = { AppBarTitle(stringResource(titleRes)) },
                searchEnabled = searchEnabled,
                searchQuery = if (searchEnabled) tab.searchQuery else null,
                onChangeSearchQuery = tab.onChangeSearchQuery,
                onSearch = tab.onSearch,
                actions = {
                    AppBarActions(
                        persistentListOf<AppBar.AppBarAction>().builder().apply {
                            addAll(rootActions)
                            addAll(tab.actions)
                        }.build(),
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
                ),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
            ) {
                Column {
                    BrowseCategoryTabs(
                        tabs = tabs,
                        pagerState = state,
                        onTabItemClick = { scope.launch { state.animateScrollToPage(it) } },
                    )
                    Spacer(modifier = Modifier.padding(top = 2.dp))
                    HorizontalPager(
                        modifier = Modifier.fillMaxSize(),
                        state = state,
                        verticalAlignment = Alignment.Top,
                    ) { page ->
                        tabs[page].content(
                            PaddingValues(bottom = contentPadding.calculateBottomPadding() + 8.dp),
                            snackbarHostState,
                        )
                    }
                }
            }
        }
    }
}
