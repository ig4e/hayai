package eu.kanade.tachiyomi.ui.source.browse.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.TurnedIn
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import kotlinx.coroutines.flow.distinctUntilChanged
import yokai.domain.source.browse.filter.models.SavedSearch
import yokai.i18n.MR

/**
 * Root composable for the redesigned source filter sheet.
 *
 * Design language mirrors the rest of the app:
 *  - Section headers ([FilterHeaderRow]) match `PreferenceGroupHeader` — `secondary` colour,
 *    `bodyMedium` text, 16-dp horizontal pad, top-12 / bottom-4 vertical.
 *  - Each filter is a flat full-width [Row] with title left / control right — same pattern as
 *    `SwitchPreferenceWidget`, no card backgrounds wrapping the rows.
 *  - Sections separate themselves with a hairline [HorizontalDivider] only when transitioning
 *    from one Header to the next, so visual grouping is implicit instead of boxed.
 *
 * Layout invariants that keep the action bar sticky inside the peek window:
 *  - The XML container is `wrap_content` so the Column sizes to its content.
 *  - The body LazyColumn is capped at `heightIn(max = BodyMaxHeight)`. With tabs (~48 dp) and
 *    action bar (~80 dp) added, the Column's natural height stays under the 480-dp `peekHeight`.
 *  - The body scrolls internally when filters overflow — the action bar never moves.
 *
 * The composition mutates filter state IN PLACE on the [FilterList] passed in — matching the
 * legacy contract that [eu.kanade.tachiyomi.ui.source.browse.BrowseSourceController.showFilters]
 * relies on (`oldFilters` snapshot vs current `presenter.sourceFilters` comparison). Do not
 * substitute a UI-state copy here or that comparison will silently fail.
 *
 * The two `*Version` ints are change tokens — incremented by the host class when the underlying
 * data identity changes (`refreshFilters` after a reset, `refreshSavedSearches` after a save) so
 * the composition picks up the new pointer and rebuilds.
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
    // Drill-down navigation for Filter.Group<*>. Holding the group instance (not its index) means
    // the back gesture from inside the group always returns to the top-level filters even if the
    // underlying FilterList shifts (reset / saved-search apply replaces the entire list).
    var drillGroup by remember(filters) { mutableStateOf<Filter.Group<*>?>(null) }

    BackHandler(enabled = drillGroup != null) { drillGroup = null }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (drillGroup == null) {
                FilterSheetTabRow(
                    selected = selectedTab,
                    onSelected = { selectedTab = it },
                    savedSearchCount = savedSearches.size,
                )
            } else {
                DrillDownHeader(
                    title = drillGroup!!.name,
                    onBack = { drillGroup = null },
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = BodyMaxHeight),
            ) {
                val currentDrill = drillGroup
                when {
                    currentDrill != null -> GroupChildrenScreen(
                        group = currentDrill,
                        filterVersion = filterVersion,
                        onListScrollChange = onListScrollChange,
                    )
                    selectedTab == FilterSheetTab.Filters -> FiltersBody(
                        filters = filters,
                        filterVersion = filterVersion,
                        onDrillGroup = { drillGroup = it },
                        onListScrollChange = onListScrollChange,
                    )
                    else -> SavedSearchesList(
                        savedSearches = savedSearches,
                        savedSearchesVersion = savedSearchesVersion,
                        onApply = onSavedSearchClicked,
                        onDelete = onDeleteSavedSearchClicked,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            FilterSheetActionBar(
                onReset = onReset,
                onSave = onSave,
                onApply = onApply,
            )
        }
    }
}

internal enum class FilterSheetTab { Filters, Saved }

private val BodyMaxHeight = 340.dp

// region Top bar

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
            text = {
                Text(
                    text = stringResource(MR.strings.filter),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected == FilterSheetTab.Filters) FontWeight.SemiBold else FontWeight.Normal,
                )
            },
        )
        Tab(
            selected = selected == FilterSheetTab.Saved,
            onClick = { onSelected(FilterSheetTab.Saved) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(MR.strings.saved_searches),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selected == FilterSheetTab.Saved) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    if (savedSearchCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Text(
                                text = savedSearchCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun DrillDownHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// endregion

// region Filters body — vertical list of rows.

@Composable
private fun FiltersBody(
    filters: FilterList,
    filterVersion: Int,
    onDrillGroup: (Filter.Group<*>) -> Unit,
    onListScrollChange: ((canScrollUp: Boolean) -> Unit)?,
) {
    val listState = rememberLazyListState()
    BridgeScrollState(listState, onListScrollChange)

    if (filters.isEmpty()) {
        EmptyFilters()
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        itemsIndexed(
            items = filters,
            key = { index, _ -> "filter-$filterVersion-$index" },
        ) { _, filter ->
            FilterRow(filter, onDrillGroup)
        }
    }
}

@Composable
private fun FilterRow(filter: Filter<*>, onDrillGroup: (Filter.Group<*>) -> Unit) {
    when (filter) {
        is Filter.Header -> FilterHeaderRow(filter)
        is Filter.Separator -> FilterSeparatorRow(filter)
        is Filter.CheckBox -> FilterCheckBoxRow(filter)
        is Filter.TriState -> FilterTriStateRow(filter)
        is Filter.Select<*> -> FilterSelectRow(filter)
        is Filter.Sort -> FilterSortRow(filter)
        is Filter.Text -> FilterTextRow(filter)
        is Filter.AutoComplete -> AutoCompleteFilterRow(filter)
        is Filter.Group<*> -> FilterGroupRow(filter, onDrill = onDrillGroup)
    }
}

@Composable
private fun GroupChildrenScreen(
    group: Filter.Group<*>,
    filterVersion: Int,
    onListScrollChange: ((canScrollUp: Boolean) -> Unit)?,
) {
    val listState = rememberLazyListState()
    BridgeScrollState(listState, onListScrollChange)

    val children = group.state
    if (children.isEmpty()) {
        EmptyFilters()
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        itemsIndexed(
            items = children,
            key = { index, _ -> "group-$filterVersion-$index" },
        ) { _, child ->
            when (child) {
                is Filter.CheckBox -> FilterCheckBoxRow(child)
                is Filter.TriState -> FilterTriStateRow(child)
                is Filter.Select<*> -> FilterSelectRow(child)
                is Filter.Text -> FilterTextRow(child)
                else -> Unit
            }
        }
    }
}

@Composable
private fun EmptyFilters() {
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
                imageVector = Icons.Outlined.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = stringResource(MR.strings.source_has_no_filters),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// endregion

// region Saved searches

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
                    imageVector = Icons.Outlined.TurnedIn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp),
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
        contentPadding = PaddingValues(vertical = 4.dp),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onApply)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.TurnedIn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = search.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = stringResource(MR.strings.save_search_delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// endregion

// region Action bar — mirrors the legacy library filter sheet pattern: borderless text buttons
// on the left (Reset uses the same colorSecondary that "Clear filters" used in
// filter_bottom_sheet.xml, Save uses colorOnBackground) plus a primary filled Filter button
// on the right.

@Composable
private fun FilterSheetActionBar(
    onReset: () -> Unit,
    onSave: () -> Unit,
    onApply: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onReset,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.secondary,
            ),
        ) {
            Text(
                text = stringResource(MR.strings.reset),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        TextButton(
            onClick = onSave,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onBackground,
            ),
        ) {
            Icon(
                imageVector = Icons.Outlined.TurnedIn,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(MR.strings.save),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onApply,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(MR.strings.filter),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// endregion

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
