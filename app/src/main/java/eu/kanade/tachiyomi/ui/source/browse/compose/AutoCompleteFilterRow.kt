package eu.kanade.tachiyomi.ui.source.browse.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.model.Filter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AutoComplete handling for the source filter sheet.
 *
 * The sheet renders an [Filter.AutoComplete] in two places:
 *
 *  - [FilterAutoCompleteRow] — a flat title-left/value-right row in the main filter list. Shows
 *    the count of currently-selected tags and a chevron; tap drills into [AutoCompleteScreen].
 *
 *  - [AutoCompleteScreen] — a full sheet-body page (sibling of the Group drill-down screen) with
 *    a search bar at the top and the entire `filter.values` list rendered as scrollable rows.
 *    Each row cycles Off → Include → Exclude (when the source declares `-` in `validPrefixes`,
 *    as on e-hentai / ex-hentai / n-hentai). Custom tags not in `values` are still addable via
 *    IME Send so power-user flows are preserved.
 *
 * State mutation routed through [FilterMutations.cycleAutoCompleteTag] so the in-place contract
 * `BrowseSourceController.showFilters()` snapshots and compares stays intact, and the cycle
 * semantics are unit-tested.
 */

// region Main-list row — entry point that drills into the tag picker.

@Composable
internal fun FilterAutoCompleteRow(
    filter: Filter.AutoComplete,
    onDrill: (Filter.AutoComplete) -> Unit,
) {
    val count = filter.state.size
    FilterPreferenceRow(
        title = filter.name,
        onClick = { onDrill(filter) },
        trailing = {
            ValueChip(
                value = if (count > 0) count.toString() else null,
                trailing = Icons.Outlined.ChevronRight,
                active = count > 0,
                constrainValueWidth = false,
            )
        },
    )
}

// endregion

// region Drill page — search bar on top, scrollable tag list.

@Composable
internal fun AutoCompleteScreen(
    filter: Filter.AutoComplete,
    onListScrollChange: ((canScrollUp: Boolean) -> Unit)?,
) {
    var query by remember(filter) { mutableStateOf("") }
    // selectionVersion bumps on every cycle so the LazyColumn keys regenerate and the +/−
    // indicators repaint immediately — `filter.state` is an external mutable property we can't
    // observe through Compose snapshots.
    var selectionVersion by remember(filter) { mutableIntStateOf(0) }
    val focusManager = LocalFocusManager.current

    val visibleTags by visibleTagsState(filter, query)

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
        AutoCompleteSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = filter.hint.ifEmpty { filter.name },
            onSubmit = ::submitCustomTag,
        )

        AutoCompleteTagList(
            tags = visibleTags,
            state = filter.state,
            supportsExclude = "-" in filter.validPrefixes,
            selectionVersion = selectionVersion,
            onCycle = { tag ->
                FilterMutations.cycleAutoCompleteTag(filter, tag)
                selectionVersion++
            },
            onListScrollChange = onListScrollChange,
        )
    }
}

/**
 * Filters [Filter.AutoComplete.values] against [query] off the main thread. Empty `query` yields
 * the full list minus skipped tags so the user sees the full catalogue immediately on opening
 * the screen.
 */
@Composable
private fun visibleTagsState(
    filter: Filter.AutoComplete,
    query: String,
) = produceState(initialValue = filter.values, key1 = filter, key2 = query) {
    withContext(Dispatchers.Default) {
        val prefix = filter.validPrefixes.find { p -> query.startsWith(p) }
        val stripped = (if (prefix != null) query.removePrefix(prefix) else query).trim()
        value = if (stripped.isEmpty()) {
            filter.values.filter { it !in filter.skipAutoFillTags }
        } else {
            filter.values.asSequence()
                .filter { it.contains(stripped, ignoreCase = true) }
                .filter { it !in filter.skipAutoFillTags }
                .toList()
        }
    }
}

@Composable
private fun AutoCompleteSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    onSubmit: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardActions = KeyboardActions(onAny = { onSubmit() }),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun AutoCompleteTagList(
    tags: List<String>,
    state: List<String>,
    supportsExclude: Boolean,
    selectionVersion: Int,
    onCycle: (String) -> Unit,
    onListScrollChange: ((canScrollUp: Boolean) -> Unit)?,
) {
    if (tags.isEmpty()) return
    val listState = rememberLazyListState()
    BridgeScrollState(listState, onListScrollChange)
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(
            items = tags,
            key = { "tag-$selectionVersion-$it" },
        ) { tag ->
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
            .heightIn(min = 36.dp)
            .clickable(onClick = onClick)
            .background(visual.background)
            .padding(horizontal = 16.dp, vertical = 4.dp),
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

private data class TagRowVisual(
    val background: Color,
    val contentColor: Color,
    val icon: ImageVector?,
)

@Composable
private fun tagRowVisual(state: AutoCompleteTagState): TagRowVisual = when (state) {
    AutoCompleteTagState.Included -> TagRowVisual(
        background = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        contentColor = MaterialTheme.colorScheme.primary,
        icon = Icons.Outlined.Add,
    )
    AutoCompleteTagState.Excluded -> TagRowVisual(
        background = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
        contentColor = MaterialTheme.colorScheme.error,
        icon = Icons.Outlined.Block,
    )
    AutoCompleteTagState.Off -> TagRowVisual(
        background = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        icon = null,
    )
}

// endregion
