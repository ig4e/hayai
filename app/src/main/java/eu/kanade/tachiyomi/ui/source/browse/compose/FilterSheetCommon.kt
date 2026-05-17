package eu.kanade.tachiyomi.ui.source.browse.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Shared scaffolding for the source filter sheet — style tokens, the title-left/widget-right row
 * scaffold every per-type composable wraps, the trailing value chip used by Select / Sort / Group
 * / AutoComplete rows, and small utility composables (empty state, scroll bridge).
 *
 * Kept in one file so the rest of the sheet code (`FilterRowComposables.kt`,
 * `AutoCompleteFilterRow.kt`, `SourceFilterSheetContent.kt`) only contains feature-specific code.
 *
 * Everything here is `internal` so files in this package can reuse it without re-exporting.
 */

// region Style tokens — mirror PreferenceCommon so the sheet reads as part of the same family.

internal val FilterRowHorizontalPadding = 16.dp
internal val FilterRowMinHeight = 40.dp
internal val ValueChipMaxWidth = 160.dp

// endregion

// region Row scaffold — title on the left, widget on the right.

/**
 * The canonical filter row layout used by every per-type composable in the sheet. Modelled on
 * `yokai.presentation.component.preference.widget.SwitchPreferenceWidget`: full-width tap target,
 * title `bodyMedium` on the left, control on the right. No card background; sections separate via
 * the [FilterHeaderRow] label instead.
 */
@Composable
internal fun FilterPreferenceRow(
    title: String,
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit,
) {
    val rowMod = Modifier
        .fillMaxWidth()
        .heightIn(min = FilterRowMinHeight)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(horizontal = FilterRowHorizontalPadding, vertical = 2.dp)
    Row(modifier = rowMod, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Box(modifier = Modifier.padding(start = 12.dp)) { trailing() }
    }
}

// endregion

// region Trailing value chip — shared by Select / Sort / Group / AutoComplete rows.

/**
 * Borderless trailing chip — `Label · Value ▾` style — used by every drill-down or pop-menu row in
 * the sheet. Colour alone communicates active/idle; active picks up the primary tint that the
 * legacy filter sheet's "Clear filters" text button used (`?attr/colorSecondary`).
 */
@Composable
internal fun ValueChip(
    value: String?,
    trailing: ImageVector,
    active: Boolean,
    constrainValueWidth: Boolean = true,
) {
    val contentColor = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.heightIn(min = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (!value.isNullOrEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (constrainValueWidth) {
                    Modifier.widthIn(max = ValueChipMaxWidth)
                } else {
                    Modifier
                },
            )
        }
        Icon(
            imageVector = trailing,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

// endregion

// region Empty state — used by every screen body that may have no items.

@Composable
internal fun FilterSheetEmptyState(icon: ImageVector, message: String) {
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
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// endregion

// region Scroll bridge — drives sheetBehavior.isDraggable from list scroll state.

/**
 * Replicates `E2EBottomSheetDialog`'s "only drag when scrolled to top" gesture, originally wired
 * for RecyclerView. Each LazyColumn inside the sheet body opts in by calling this with its list
 * state and the host class's callback.
 */
@Composable
internal fun BridgeScrollState(
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

// endregion

// region Display ordering — reorder filters within each Header-bounded section.

/**
 * Reorders `filters` for display only. Headers anchor their section — every filter originally
 * between two Headers stays in that section — but within a section, filters sort by
 * [displayPriority] so the layout reads top-to-bottom: most-important inputs first.
 *
 * The original [Filter] instances are preserved by reference; only display order changes, so the
 * in-place mutation contract `BrowseSourceController.showFilters()` depends on is still honoured.
 */
internal fun organizeForDisplay(filters: FilterList): List<Filter<*>> {
    if (filters.isEmpty()) return emptyList()
    return splitIntoSections(filters)
        .flatMap { (header, items) ->
            listOfNotNull(header) + items.sortedBy(::displayPriority)
        }
}

private data class FilterSection(
    val header: Filter.Header?,
    val items: List<Filter<*>>,
)

private fun splitIntoSections(filters: FilterList): List<FilterSection> {
    val sections = mutableListOf<FilterSection>()
    var header: Filter.Header? = null
    var items = mutableListOf<Filter<*>>()
    fun flush() {
        if (header != null || items.isNotEmpty()) {
            sections.add(FilterSection(header, items))
            items = mutableListOf()
            header = null
        }
    }
    filters.forEach { f ->
        if (f is Filter.Header) {
            flush()
            header = f
        } else {
            items.add(f)
        }
    }
    flush()
    return sections
}

/**
 * Lower value = rendered closer to the top of its section.
 *  - Sort         : 10 — primary axis of the listing
 *  - AutoComplete : 20 — main search input (tags, etc.)
 *  - Select       : 30
 *  - Group        : 40 — drill-downs feel like sub-sections, lift above toggles
 *  - TriState     : 50
 *  - CheckBox     : 60 — boolean toggles bundle together
 *  - Text         : 70 — page/jump-style numeric inputs at the bottom
 *  - Separator    : 80
 *  - Header       : 100 — should never reach here, sections are pre-extracted
 */
private fun displayPriority(f: Filter<*>): Int = when (f) {
    is Filter.Sort -> 10
    is Filter.AutoComplete -> 20
    is Filter.Select<*> -> 30
    is Filter.Group<*> -> 40
    is Filter.TriState -> 50
    is Filter.CheckBox -> 60
    is Filter.Text -> 70
    is Filter.Separator -> 80
    else -> 100
}

// endregion

// region Filter.Group inspection — how many of its children are non-default.

/**
 * Counts non-default children inside a [Filter.Group]. Used by [FilterGroupRow]'s trailing count
 * chip so the drill-down preview shows how many filters are active without opening it.
 */
internal fun groupActiveCount(group: Filter.Group<*>): Int = group.state.count { child ->
    when (child) {
        is Filter.CheckBox -> child.state
        is Filter.TriState -> child.state != Filter.TriState.STATE_IGNORE
        is Filter.Text -> child.state.isNotEmpty()
        is Filter.Select<*> -> child.state != 0
        else -> false
    }
}

// endregion
