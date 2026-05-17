package eu.kanade.tachiyomi.ui.source.browse.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.source.model.Filter
import yokai.i18n.MR

/**
 * Per-type row composables. Each receives the original [Filter] instance and mutates `.state` via
 * [FilterMutations] in response to user gestures. The mutation contract is what
 * [eu.kanade.tachiyomi.ui.source.browse.BrowseSourceController.showFilters] snapshots and
 * compares; do not work on copies of the filter.
 */

@Composable
internal fun FilterHeaderRow(filter: Filter.Header) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Text(
            text = filter.name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
internal fun FilterSeparatorRow(@Suppress("UNUSED_PARAMETER") filter: Filter.Separator) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
internal fun FilterCheckboxRow(filter: Filter.CheckBox) {
    var checked by remember(filter) { mutableStateOf(filter.state) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Checkbox }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = {
                FilterMutations.toggleCheckbox(filter)
                checked = filter.state
            },
        )
        Text(
            text = filter.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun FilterTriStateRow(filter: Filter.TriState) {
    var state by remember(filter) { mutableIntStateOf(filter.state) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = filter.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        FilterChip(
            selected = state == Filter.TriState.STATE_INCLUDE,
            onClick = {
                FilterMutations.setTriState(filter, Filter.TriState.STATE_INCLUDE)
                state = filter.state
            },
            label = { Text(stringResource(MR.strings.include)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 2.dp),
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
        )
        FilterChip(
            selected = state == Filter.TriState.STATE_EXCLUDE,
            onClick = {
                FilterMutations.setTriState(filter, Filter.TriState.STATE_EXCLUDE)
                state = filter.state
            },
            label = { Text(stringResource(MR.strings.exclude)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Remove,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 2.dp),
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterSelectRow(filter: Filter.Select<*>) {
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember(filter) { mutableIntStateOf(filter.state) }
    val displayValue = filter.values.getOrNull(selectedIndex)?.toString().orEmpty()

    ExposedDropdownMenuBox(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(filter.name) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            filter.values.forEachIndexed { index, value ->
                DropdownMenuItem(
                    text = { Text(value.toString()) },
                    onClick = {
                        FilterMutations.setSelect(filter, index)
                        selectedIndex = filter.state
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun FilterTextRow(filter: Filter.Text) {
    var value by remember(filter) { mutableStateOf(filter.state) }
    // Keep filter.state in sync with the rendered value even if the user navigates
    // tabs without committing — the legacy contract treats every keystroke as a
    // state change.
    LaunchedEffect(value) {
        if (filter.state != value) FilterMutations.setText(filter, value)
    }
    OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        label = { Text(filter.name) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

@Composable
internal fun FilterSortRow(filter: Filter.Sort, name: String, index: Int) {
    var state by remember(filter) { mutableStateOf(filter.state) }
    val selected = state?.index == index
    val ascending = state?.ascending == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = selected,
            onClick = {
                FilterMutations.toggleSort(filter, index)
                state = filter.state
            },
            label = {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            leadingIcon = if (selected) {
                {
                    Icon(
                        imageVector = if (ascending) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                        contentDescription = stringResource(
                            if (ascending) MR.strings.sort_ascending else MR.strings.sort_descending,
                        ),
                    )
                }
            } else {
                null
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Renders one row per child filter inside a [Filter.Group]'s accordion. The supported child types
 * mirror legacy `BrowseSourcePresenter.toItems()` — Checkbox, TriState, Text, Select. Unknown
 * child types are silently skipped, matching the legacy `else -> null` branch.
 */
@Composable
internal fun GroupChildren(group: Filter.Group<*>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        group.state.forEach { child ->
            when (child) {
                is Filter.CheckBox -> FilterCheckboxRow(child)
                is Filter.TriState -> FilterTriStateRow(child)
                is Filter.Text -> FilterTextRow(child)
                is Filter.Select<*> -> FilterSelectRow(child)
                else -> Unit
            }
        }
    }
}

/**
 * Summary line for a [Filter.Group] header — counts how many children are non-default.
 * Returns null if nothing meaningful is selected so the accordion shows just the title.
 */
internal fun groupSelectionSummary(group: Filter.Group<*>): String? {
    val active = group.state.count { child ->
        when (child) {
            is Filter.CheckBox -> child.state
            is Filter.TriState -> child.state != Filter.TriState.STATE_IGNORE
            is Filter.Text -> child.state.isNotEmpty()
            is Filter.Select<*> -> child.state != 0
            else -> false
        }
    }
    return if (active > 0) "$active selected" else null
}

internal fun sortSelectionSummary(filter: Filter.Sort): String? {
    val sel = filter.state ?: return null
    val label = filter.values.getOrNull(sel.index) ?: return null
    return if (sel.ascending) "$label ↑" else "$label ↓"
}
