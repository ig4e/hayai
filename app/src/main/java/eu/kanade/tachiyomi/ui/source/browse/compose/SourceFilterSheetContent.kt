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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import yokai.domain.source.browse.filter.models.SavedSearch
import yokai.i18n.MR

/**
 * Root composable for the redesigned source filter sheet.
 *
 * Design language mirrors the rest of the app:
 *  - Section headers ([FilterHeaderRow]) match `PreferenceGroupHeader` — `secondary` colour,
 *    `labelMedium` text.
 *  - Each filter renders as a flat full-width row (title left, control right) via
 *    [FilterPreferenceRow] — same pattern as `SwitchPreferenceWidget`, no card wrapping.
 *  - Drill-down sub-screens replace the body when the user opens a Group or the AutoComplete
 *    tag picker; the [BackHandler] returns to the main list.
 *
 * Layout invariants that keep the action bar sticky inside the peek window:
 *  - XML container is `wrap_content` so the Column sizes to its content.
 *  - The body LazyColumn is bounded by [BodyMaxHeight]. With tabs (~48 dp) and action bar
 *    (~80 dp) added, the Column's natural height stays under the configured `peekHeight`.
 *  - The body scrolls internally when filters overflow — the action bar never moves.
 *
 * The composition mutates filter state IN PLACE on the [FilterList] passed in — matching the
 * legacy contract that [eu.kanade.tachiyomi.ui.source.browse.BrowseSourceController.showFilters]
 * relies on. Do not substitute a UI-state copy here.
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
    // Drill-down navigation. The target is held by reference so back-gesture from inside a sub-
    // screen always returns to the top-level filters even if the underlying FilterList shifts
    // (reset / saved-search apply replaces the entire list).
    var drillTarget by remember(filters) { mutableStateOf<FilterDrillTarget?>(null) }

    BackHandler(enabled = drillTarget != null) { drillTarget = null }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetTopBar(
                drillTarget = drillTarget,
                onBack = { drillTarget = null },
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it },
                savedSearchCount = savedSearches.size,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = BodyMaxHeight),
            ) {
                SheetBody(
                    drillTarget = drillTarget,
                    selectedTab = selectedTab,
                    filters = filters,
                    savedSearches = savedSearches,
                    filterVersion = filterVersion,
                    savedSearchesVersion = savedSearchesVersion,
                    onDrillTarget = { drillTarget = it },
                    onSavedSearchClicked = onSavedSearchClicked,
                    onDeleteSavedSearchClicked = onDeleteSavedSearchClicked,
                    onListScrollChange = onListScrollChange,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            FilterSheetActionBar(onReset, onSave, onApply)
        }
    }
}

internal enum class FilterSheetTab { Filters, Saved }

private val BodyMaxHeight = 400.dp

/**
 * Where the user has drilled to inside the sheet body. Sub-screens swap the main filter list out
 * for a focused view of one filter.
 */
private sealed interface FilterDrillTarget {
    val name: String
    data class Group(val filter: Filter.Group<*>) : FilterDrillTarget {
        override val name: String get() = filter.name
    }
    data class TagList(val filter: Filter.AutoComplete) : FilterDrillTarget {
        override val name: String get() = filter.name
    }
}

// region Top bar — tabs at the top level, drill-down header when inside a sub-screen.

@Composable
private fun SheetTopBar(
    drillTarget: FilterDrillTarget?,
    onBack: () -> Unit,
    selectedTab: FilterSheetTab,
    onSelectTab: (FilterSheetTab) -> Unit,
    savedSearchCount: Int,
) {
    if (drillTarget == null) {
        FilterSheetTabRow(
            selected = selectedTab,
            onSelected = onSelectTab,
            savedSearchCount = savedSearchCount,
        )
    } else {
        DrillDownHeader(title = drillTarget.name, onBack = onBack)
    }
}

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
                TabLabel(
                    text = stringResource(MR.strings.filter),
                    active = selected == FilterSheetTab.Filters,
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
                    TabLabel(
                        text = stringResource(MR.strings.saved_searches),
                        active = selected == FilterSheetTab.Saved,
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
private fun TabLabel(text: String, active: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
    )
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

// region Body — dispatch to filters list / drill sub-screen / saved-searches tab.

@Composable
private fun SheetBody(
    drillTarget: FilterDrillTarget?,
    selectedTab: FilterSheetTab,
    filters: FilterList,
    savedSearches: List<SavedSearch>,
    filterVersion: Int,
    savedSearchesVersion: Int,
    onDrillTarget: (FilterDrillTarget?) -> Unit,
    onSavedSearchClicked: (Long) -> Unit,
    onDeleteSavedSearchClicked: (Long) -> Unit,
    onListScrollChange: ((canScrollUp: Boolean) -> Unit)?,
) {
    when (drillTarget) {
        is FilterDrillTarget.Group -> GroupChildrenScreen(
            group = drillTarget.filter,
            filterVersion = filterVersion,
            onListScrollChange = onListScrollChange,
        )
        is FilterDrillTarget.TagList -> AutoCompleteScreen(
            filter = drillTarget.filter,
            onListScrollChange = onListScrollChange,
        )
        null -> when (selectedTab) {
            FilterSheetTab.Filters -> FiltersBody(
                filters = filters,
                filterVersion = filterVersion,
                onDrillGroup = { onDrillTarget(FilterDrillTarget.Group(it)) },
                onDrillTags = { onDrillTarget(FilterDrillTarget.TagList(it)) },
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
}

@Composable
private fun FiltersBody(
    filters: FilterList,
    filterVersion: Int,
    onDrillGroup: (Filter.Group<*>) -> Unit,
    onDrillTags: (Filter.AutoComplete) -> Unit,
    onListScrollChange: ((canScrollUp: Boolean) -> Unit)?,
) {
    val listState = rememberLazyListState()
    BridgeScrollState(listState, onListScrollChange)

    if (filters.isEmpty()) {
        FilterSheetEmptyState(
            icon = Icons.Outlined.SearchOff,
            message = stringResource(MR.strings.source_has_no_filters),
        )
        return
    }

    // organizeForDisplay produces a display-only ordering (originals are still mutated by ref).
    val displayFilters = remember(filters, filterVersion) { organizeForDisplay(filters) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        itemsIndexed(
            items = displayFilters,
            key = { index, _ -> "filter-$filterVersion-$index" },
        ) { _, filter ->
            FilterRow(filter, onDrillGroup, onDrillTags)
        }
    }
}

@Composable
private fun FilterRow(
    filter: Filter<*>,
    onDrillGroup: (Filter.Group<*>) -> Unit,
    onDrillTags: (Filter.AutoComplete) -> Unit,
) {
    when (filter) {
        is Filter.Header -> FilterHeaderRow(filter)
        is Filter.Separator -> FilterSeparatorRow(filter)
        is Filter.CheckBox -> FilterCheckBoxRow(filter)
        is Filter.TriState -> FilterTriStateRow(filter)
        is Filter.Select<*> -> FilterSelectRow(filter)
        is Filter.Sort -> FilterSortRow(filter)
        is Filter.Text -> FilterTextRow(filter)
        is Filter.AutoComplete -> FilterAutoCompleteRow(filter, onDrill = onDrillTags)
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
        FilterSheetEmptyState(
            icon = Icons.Outlined.SearchOff,
            message = stringResource(MR.strings.source_has_no_filters),
        )
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
            // Group children mirror top-level rendering for the subset of filter types groups
            // actually contain in practice (Tachiyomi source extensions only nest checkboxes,
            // tri-states, selects and text inside groups).
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
        FilterSheetEmptyState(
            icon = Icons.Outlined.TurnedIn,
            message = stringResource(MR.strings.no_saved_searches),
        )
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

// region Action bar — M3 Expressive button group on the left, primary CTA on the right.
//
// Reset and Save are visually grouped via shape-joining (outer corners fully rounded, inner
// corners near-square) with only a 2-dp gap so they read as one piece. Each segment keeps its
// own intent colour:
//   Reset → errorContainer  — soft-danger "clear" action, no shouting red
//   Save  → secondaryContainer — neutral constructive action with bookmark icon
//   Filter (apply) → standalone primary Button — the prominent CTA, separated by weight(1f).
// All filled; colour does the hierarchy work (tonal pair reads quieter than the primary CTA).

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
        GroupedTonalButton(
            onClick = onReset,
            shape = ButtonGroupStartShape,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            label = stringResource(MR.strings.reset),
        )
        GroupedTonalButton(
            onClick = onSave,
            shape = ButtonGroupEndShape,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            icon = Icons.Outlined.TurnedIn,
            label = stringResource(MR.strings.save),
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onApply,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = stringResource(MR.strings.filter),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun GroupedTonalButton(
    onClick: () -> Unit,
    shape: androidx.compose.ui.graphics.Shape,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    FilledTonalButton(
        onClick = onClick,
        shape = shape,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** First segment of the connected button group — outer (start) corners fully rounded. */
private val ButtonGroupStartShape = RoundedCornerShape(
    topStartPercent = 50,
    bottomStartPercent = 50,
    topEndPercent = 12,
    bottomEndPercent = 12,
)

/** Last segment of the connected button group — outer (end) corners fully rounded. */
private val ButtonGroupEndShape = RoundedCornerShape(
    topStartPercent = 12,
    bottomStartPercent = 12,
    topEndPercent = 50,
    bottomEndPercent = 50,
)

// endregion
