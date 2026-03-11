package eu.kanade.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabIndicatorScope
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.icerock.moko.resources.StringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.TabText
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TabbedScreen(
    titleRes: StringResource,
    tabs: ImmutableList<TabContent>,
    state: PagerState = rememberPagerState { tabs.size },
    navigateUp: (() -> Unit)? = null,
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
                navigateUp = navigateUp,
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
            modifier = Modifier.padding(
                top = contentPadding.calculateTopPadding(),
                start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
            ),
        ) {
            SecondaryTabRow(
                selectedTabIndex = state.currentPage,
                modifier = Modifier.zIndex(1f),
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { CustomTabIndicator(state) },
                divider = { }
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = state.currentPage == index
                    val textColor by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "TabTextColor"
                    )

                    Tab(
                        selected = selected,
                        onClick = { scope.launch { state.animateScrollToPage(index) } },
                        text = {
                            TabText(
                                text = stringResource(tab.titleRes),
                                badgeCount = tab.badgeNumber,
                                textColor = textColor
                            )
                        },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                state = state,
                verticalAlignment = Alignment.Top,
            ) { page ->
                tabs[page].content(
                    PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                    snackbarHostState,
                )
            }
        }
    }
}

@Composable
private fun TabIndicatorScope.CustomTabIndicator(
    state: PagerState,
) {
    SecondaryIndicator(
        modifier = Modifier
            .tabIndicatorOffset(state.currentPage)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .height(3.dp),
        color = MaterialTheme.colorScheme.primary,
    )
}

data class TabContent(
    val titleRes: StringResource,
    val badgeNumber: Int? = null,
    val searchEnabled: Boolean = false,
    val searchQuery: String? = null,
    val onChangeSearchQuery: (String?) -> Unit = {},
    val onSearch: (String) -> Unit = {},
    val actions: ImmutableList<AppBar.AppBarAction> = persistentListOf(),
    val content: @Composable (contentPadding: PaddingValues, snackbarHostState: SnackbarHostState) -> Unit,
)
