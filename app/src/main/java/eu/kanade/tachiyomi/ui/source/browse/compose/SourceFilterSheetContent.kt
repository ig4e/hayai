package eu.kanade.tachiyomi.ui.source.browse.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.source.model.FilterList
import kotlinx.coroutines.flow.distinctUntilChanged
import yokai.domain.source.browse.filter.models.SavedSearch
import yokai.i18n.MR

/**
 * Root composable for the M3 Expressive source filter sheet.
 *
 * The composition mutates filter state IN PLACE on the [FilterList] passed in — matching the
 * legacy contract that [eu.kanade.tachiyomi.ui.source.browse.BrowseSourceController.showFilters]
 * relies on (`oldFilters` snapshot vs current `presenter.sourceFilters` comparison). Do not
 * substitute a UI-state copy here or that comparison will silently fail.
 *
 * The two `*Version` ints are change tokens — incremented by the host class when the underlying
 * data identity changes (`setFilters` after a reset, `scrollToTop` after a save) so the
 * composition picks up the new pointer and rebuilds.
 */
@Composable
internal fun SourceFilterSheetContent(
    filters: FilterList,
    savedSearches: List<SavedSearch>,
    filterVersion: Int,
    savedSearchesVersion: Int,
    onApply: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onSavedSearchClicked: (Long) -> Unit,
    onDeleteSavedSearchClicked: (Long) -> Unit,
    onListScrollChange: ((canScrollUp: Boolean) -> Unit)? = null,
) {
    var selectedTab by rememberSaveable { mutableStateOf(FilterSheetTab.Filters) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            FilterSheetTabRow(
                selected = selectedTab,
                onSelected = { selectedTab = it },
                savedSearchCount = savedSearches.size,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 540.dp),
            ) {
                when (selectedTab) {
                    FilterSheetTab.Filters -> FiltersList(
                        filters = filters,
                        filterVersion = filterVersion,
                        onListScrollChange = onListScrollChange,
                    )
                    FilterSheetTab.Saved -> SavedSearchesList(
                        savedSearches = savedSearches,
                        savedSearchesVersion = savedSearchesVersion,
                        onApply = onSavedSearchClicked,
                        onDelete = onDeleteSavedSearchClicked,
                    )
                }
            }

            FilterSheetActionBar(
                onReset = onReset,
                onSave = onSave,
                onApply = onApply,
            )
        }
    }
}

internal enum class FilterSheetTab { Filters, Saved }

@Composable
private fun FilterSheetTabRow(
    selected: FilterSheetTab,
    onSelected: (FilterSheetTab) -> Unit,
    savedSearchCount: Int,
) {
    PrimaryTabRow(
        selectedTabIndex = selected.ordinal,
        containerColor = MaterialTheme.colorScheme.surface,
        divider = {},
    ) {
        Tab(
            selected = selected == FilterSheetTab.Filters,
            onClick = { onSelected(FilterSheetTab.Filters) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.FilterAlt,
                    contentDescription = null,
                )
            },
            text = { Text(stringResource(MR.strings.filter)) },
        )
        Tab(
            selected = selected == FilterSheetTab.Saved,
            onClick = { onSelected(FilterSheetTab.Saved) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                )
            },
            text = {
                Text(
                    text = if (savedSearchCount > 0) {
                        stringResource(MR.strings.saved_searches) + "  ·  $savedSearchCount"
                    } else {
                        stringResource(MR.strings.saved_searches)
                    },
                )
            },
        )
    }
}

@Composable
private fun FiltersList(
    filters: FilterList,
    filterVersion: Int,
    onListScrollChange: ((canScrollUp: Boolean) -> Unit)?,
) {
    val listState = rememberLazyListState()
    BridgeScrollState(listState, onListScrollChange)

    if (filters.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(MR.strings.source_has_no_filters),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(
            items = filters,
            // Identity by position + version: position survives in-place state mutation
            // (which we want — no re-key on every keystroke), but `filterVersion` bumps when the
            // controller swaps the FilterList wholesale (reset or saved-search apply), so the
            // composition rebuilds cleanly with the new instances.
            key = { index, _ -> "f-$filterVersion-$index" },
        ) { _, filter ->
            FilterRow(filter)
        }
    }
}

@Composable
private fun FilterRow(filter: eu.kanade.tachiyomi.source.model.Filter<*>) {
    when (filter) {
        is eu.kanade.tachiyomi.source.model.Filter.Header -> FilterHeaderRow(filter)
        is eu.kanade.tachiyomi.source.model.Filter.Separator -> FilterSeparatorRow(filter)
        is eu.kanade.tachiyomi.source.model.Filter.CheckBox -> FilterCheckboxRow(filter)
        is eu.kanade.tachiyomi.source.model.Filter.TriState -> FilterTriStateRow(filter)
        is eu.kanade.tachiyomi.source.model.Filter.Text -> FilterTextRow(filter)
        is eu.kanade.tachiyomi.source.model.Filter.Select<*> -> FilterSelectRow(filter)
        is eu.kanade.tachiyomi.source.model.Filter.AutoComplete -> AutoCompleteFilterRow(filter)
        is eu.kanade.tachiyomi.source.model.Filter.Group<*> -> {
            FilterAccordion(
                title = filter.name,
                selectionSummary = groupSelectionSummary(filter),
            ) {
                GroupChildren(filter)
            }
        }
        is eu.kanade.tachiyomi.source.model.Filter.Sort -> {
            FilterAccordion(
                title = filter.name,
                selectionSummary = sortSelectionSummary(filter),
            ) {
                filter.values.forEachIndexed { index, name ->
                    FilterSortRow(filter = filter, name = name, index = index)
                }
            }
        }
    }
}

@Composable
private fun SavedSearchesList(
    savedSearches: List<SavedSearch>,
    savedSearchesVersion: Int,
    onApply: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    if (savedSearches.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(MR.strings.no_saved_searches),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = savedSearches,
            key = { "saved-$savedSearchesVersion-${it.id}" },
        ) { search ->
            SavedSearchRow(
                search = search,
                onApply = { onApply(search.id) },
                onDelete = { onDelete(search.id) },
            )
        }
    }
}

@Composable
private fun SavedSearchRow(
    search: SavedSearch,
    onApply: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        onClick = onApply,
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.BookmarkBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = search.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(MR.strings.save_search_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FilterSheetActionBar(
    onReset: () -> Unit,
    onSave: () -> Unit,
    onApply: () -> Unit,
) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onReset) {
            Text(stringResource(MR.strings.reset))
        }
        IconButton(onClick = onSave) {
            Icon(
                imageVector = Icons.Outlined.Save,
                contentDescription = stringResource(MR.strings.save),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onApply) {
            Text(stringResource(MR.strings.filter))
        }
    }
}

/**
 * Drives `sheetBehavior.isDraggable` from the inner LazyColumn's scroll state. The legacy
 * E2EBottomSheetDialog wired this for RecyclerView; we replicate the same UX for LazyColumn so
 * the sheet only drags when the list is scrolled to the top.
 */
@Composable
private fun BridgeScrollState(
    state: LazyListState,
    onChange: ((canScrollUp: Boolean) -> Unit)?,
) {
    if (onChange == null) return
    LaunchedEffect(state, onChange) {
        snapshotFlow { state.canScrollBackward }
            .distinctUntilChanged()
            .collect { onChange(it) }
    }
}
