package eu.kanade.tachiyomi.ui.source.browse.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.model.Filter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import yokai.util.search.FuzzyMatcher

/**
 * AutoComplete handling for the source filter sheet.
 *
 *  - [FilterAutoCompleteRow] — row in the main filter list. Shows include/exclude pills for the
 *    currently-selected tags directly beneath the row so the user can see and edit selections
 *    without re-opening the picker. Tap pill body cycles include⇄exclude (if the source declares
 *    `-` in `validPrefixes`); trailing × removes.
 *
 *  - [AutoCompleteScreen] — full sheet-body picker. Uses the shared [SheetSearchField] for the
 *    search bar, fuzzy ranking via [FuzzyMatcher.score], and pins currently-selected tags to the
 *    top of the list. The pinned ordering is snapshotted once per query change so tapping rows
 *    to include/exclude never reorders the list — the row stays put and only its colour changes.
 *
 * State mutation routed through [FilterMutations] so the in-place contract
 * `BrowseSourceController.showFilters()` snapshots and compares stays intact.
 */

// region Main-list row — title row + selected-tag pills below.

@Composable
internal fun FilterAutoCompleteRow(
    filter: Filter.AutoComplete,
    onDrill: (Filter.AutoComplete) -> Unit,
) {
    var selectionVersion by remember(filter) { mutableIntStateOf(0) }
    val active = remember(filter, selectionVersion) { filter.state.toList() }
    Column(modifier = Modifier.fillMaxWidth()) {
        FilterPreferenceRow(
            title = filter.name,
            onClick = { onDrill(filter) },
            trailing = {
                ValueChip(
                    value = if (active.isNotEmpty()) active.size.toString() else null,
                    trailing = Icons.Outlined.ChevronRight,
                    active = active.isNotEmpty(),
                    constrainValueWidth = false,
                )
            },
        )
        if (active.isNotEmpty()) {
            SelectedTagPills(
                filter = filter,
                selectedState = active,
                onChange = { selectionVersion++ },
            )
        }
    }
}

// endregion

// region Selected-tag pills — main-list row only. Removed from inside the picker so the picker
// only shows pinned rows.

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectedTagPills(
    filter: Filter.AutoComplete,
    selectedState: List<String>,
    onChange: () -> Unit,
) {
    val supportsExclude = "-" in filter.validPrefixes
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Iterate the raw selection order — NOT partitioned by include/exclude — so toggling a
        // pill's state never repositions it within the row. The mutator replaces in place.
        selectedState.forEach { entry ->
            val isExcluded = entry.startsWith("-")
            val baseTag = if (isExcluded) entry.removePrefix("-") else entry
            val state = if (isExcluded) AutoCompleteTagState.Excluded else AutoCompleteTagState.Included
            TagPill(
                label = baseTag,
                state = state,
                onClick = when {
                    isExcluded -> {
                        {
                            FilterMutations.setAutoCompleteTagState(filter, baseTag, AutoCompleteTagState.Included)
                            onChange()
                        }
                    }
                    supportsExclude -> {
                        {
                            FilterMutations.setAutoCompleteTagState(filter, baseTag, AutoCompleteTagState.Excluded)
                            onChange()
                        }
                    }
                    else -> null
                },
                onRemove = {
                    FilterMutations.setAutoCompleteTagState(filter, baseTag, AutoCompleteTagState.Off)
                    onChange()
                },
            )
        }
    }
}

@Composable
private fun TagPill(
    label: String,
    state: AutoCompleteTagState,
    onClick: (() -> Unit)?,
    onRemove: () -> Unit,
) {
    val container: Color
    val content: Color
    val leading: ImageVector
    when (state) {
        AutoCompleteTagState.Included -> {
            container = MaterialTheme.colorScheme.primaryContainer
            content = MaterialTheme.colorScheme.onPrimaryContainer
            leading = Icons.Outlined.Add
        }
        AutoCompleteTagState.Excluded -> {
            container = MaterialTheme.colorScheme.errorContainer
            content = MaterialTheme.colorScheme.onErrorContainer
            leading = Icons.Outlined.Block
        }
        AutoCompleteTagState.Off -> return
    }
    // Surface + Row instead of InputChip — InputChip's default shape is squarish (8dp). We want
    // fully-rounded pill chrome with an explicit container colour, so build it directly.
    Surface(
        onClick = onClick ?: onRemove,
        shape = CircleShape,
        color = container,
        contentColor = content,
        modifier = Modifier.heightIn(min = 32.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Icon(
                imageVector = leading,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

// endregion

// region Drill page — shared search bar + pinned-selected tag list.

@Composable
internal fun AutoCompleteScreen(
    filter: Filter.AutoComplete,
    onListScrollChange: ((canScrollUp: Boolean) -> Unit)?,
) {
    var query by remember(filter) { mutableStateOf("") }
    // selectionVersion bumps on every cycle. The outer state list snapshot rebuilds via this key
    // so per-row colour updates ride the recomposition. LazyColumn keys are kept stable (tag
    // name only) so rows don't get disposed/recreated — the row stays in place and only repaints.
    var selectionVersion by remember(filter) { mutableIntStateOf(0) }
    val focusManager = LocalFocusManager.current

    val visibleTags by visibleTagsState(filter, query)
    val currentState = remember(filter, selectionVersion) { filter.state.toList() }
    // Pinned ordering is snapshotted from filter.state once per `visibleTags` change (which only
    // shifts on query change) — tapping a tag to include/exclude does NOT recompute this list.
    // That is the fix for the jumpy "row teleports as I click it" UX.
    val orderedTags = remember(visibleTags, filter) {
        val selectedBase = filter.state.map { it.removePrefix("-") }.toSet()
        val (pinned, rest) = visibleTags.partition { it in selectedBase }
        pinned + rest
    }

    fun submitCustomTag() {
        val text = query.trim()
        if (text.isEmpty()) return
        if (FilterMutations.addAutoCompleteTag(filter, text)) {
            selectionVersion++
            query = ""
            focusManager.clearFocus()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SheetSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = filter.hint.ifEmpty { filter.name },
            onSubmit = ::submitCustomTag,
        )

        AutoCompleteTagList(
            tags = orderedTags,
            state = currentState,
            supportsExclude = "-" in filter.validPrefixes,
            onCycle = { tag ->
                FilterMutations.cycleAutoCompleteTag(filter, tag)
                selectionVersion++
            },
            onListScrollChange = onListScrollChange,
        )
    }
}

/**
 * Filters [Filter.AutoComplete.values] against [query] off the main thread, then ranks matches by
 * [FuzzyMatcher.score]. Empty `query` yields the full list (minus skipped tags) so the user sees
 * the full catalogue on open.
 */
@Composable
private fun visibleTagsState(
    filter: Filter.AutoComplete,
    query: String,
) = produceState(initialValue = filter.values, key1 = filter, key2 = query) {
    withContext(Dispatchers.Default) {
        val prefix = filter.validPrefixes.find { p -> query.startsWith(p) }
        val stripped = (if (prefix != null) query.removePrefix(prefix) else query).trim()
        val baseList = filter.values.filter { it !in filter.skipAutoFillTags }
        value = if (stripped.isEmpty()) {
            baseList
        } else {
            baseList.asSequence()
                .map { it to FuzzyMatcher.score(stripped, it) }
                .filter { it.second >= FuzzyTagThreshold }
                .sortedByDescending { it.second }
                .map { it.first }
                .toList()
        }
    }
}

// Lower than the conventional 70 because tag names are short — a forgiving cutoff catches
// substring queries like "elf" → "long-elven-hair" without surfacing noise.
private const val FuzzyTagThreshold = 60

@Composable
private fun AutoCompleteTagList(
    tags: List<String>,
    state: List<String>,
    supportsExclude: Boolean,
    onCycle: (String) -> Unit,
    onListScrollChange: ((canScrollUp: Boolean) -> Unit)?,
) {
    if (tags.isEmpty()) return
    val listState = rememberLazyListState()
    BridgeScrollState(listState, onListScrollChange)
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        items(items = tags, key = { it }) { tag ->
            val tagState = tagStateFor(tag, state, supportsExclude)
            AutoCompleteTagRow(
                tag = tag,
                state = tagState,
                onClick = { onCycle(tag) },
            )
        }
    }
}

private fun tagStateFor(
    tag: String,
    state: List<String>,
    supportsExclude: Boolean,
): AutoCompleteTagState = when {
    tag in state -> AutoCompleteTagState.Included
    supportsExclude && "-$tag" in state -> AutoCompleteTagState.Excluded
    else -> AutoCompleteTagState.Off
}

@Composable
private fun AutoCompleteTagRow(
    tag: String,
    state: AutoCompleteTagState,
    onClick: () -> Unit,
) {
    val visual = tagRowVisual(state)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(TagRowShape)
            .background(visual.background)
            .clickable(onClick = onClick)
            .heightIn(min = 40.dp)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.bodyMedium,
            color = visual.contentColor,
            fontWeight = if (state == AutoCompleteTagState.Off) FontWeight.Normal else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        visual.icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = visual.contentColor,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private val TagRowShape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)

private data class TagRowVisual(
    val background: Color,
    val contentColor: Color,
    val icon: ImageVector?,
)

@Composable
private fun tagRowVisual(state: AutoCompleteTagState): TagRowVisual = when (state) {
    AutoCompleteTagState.Included -> TagRowVisual(
        background = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        icon = Icons.Outlined.Add,
    )
    AutoCompleteTagState.Excluded -> TagRowVisual(
        background = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        icon = Icons.Outlined.Block,
    )
    AutoCompleteTagState.Off -> TagRowVisual(
        background = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        icon = null,
    )
}

// endregion
