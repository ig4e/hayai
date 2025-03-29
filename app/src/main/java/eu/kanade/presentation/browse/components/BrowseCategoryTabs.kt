package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import eu.kanade.presentation.components.TabContent
import tachiyomi.presentation.core.components.CategoryChip
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Modern chip-style tabs for browse categories
 */
@Composable
fun BrowseCategoryTabs(
    tabs: List<TabContent>,
    pagerState: PagerState,
    onTabItemClick: (Int) -> Unit,
) {
    val currentPageIndex = pagerState.currentPage.coerceAtMost(tabs.lastIndex)
    val listState = rememberLazyListState()

    // Automatically scroll to make the selected category visible
    LaunchedEffect(currentPageIndex) {
        listState.animateScrollToItem(currentPageIndex.coerceAtMost(tabs.lastIndex))
    }

    Column(
        modifier = Modifier.zIndex(1f),
    ) {
        // Scrolling row of category chips
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            itemsIndexed(tabs) { index, tab ->
                CategoryChip(
                    text = stringResource(tab.titleRes),
                    count = tab.badgeNumber,
                    selected = currentPageIndex == index,
                    onClick = { onTabItemClick(index) }
                )
            }
        }

        // Bottom divider
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            thickness = 1.dp
        )
    }
}
