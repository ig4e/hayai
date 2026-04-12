package eu.kanade.tachiyomi.ui.source.filter

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedSuggestionChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yokai.domain.source.browse.filter.models.SavedSearch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeSourceFilterSheet(
    filters: FilterList,
    savedSearches: List<SavedSearch>,
    onFilterChanged: () -> Unit,
    onResetClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onSaveClicked: () -> Unit,
    onSavedSearchClicked: (Long) -> Unit,
    onDeleteSavedSearchClicked: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Recomposition trigger: filters mutate in-place, so we bump this counter to force recompose
    var revision by remember { mutableIntStateOf(0) }
    val triggerUpdate = {
        revision++
        onFilterChanged()
    }

    ModalBottomSheet(
        onDismissRequest = {
            onSearchClicked()
            onDismiss()
        },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            // Header with action buttons
            item {
                FilterSheetHeader(
                    onReset = {
                        onResetClicked()
                        revision++
                    },
                    onSave = onSaveClicked,
                    onFilter = {
                        scope.launch {
                            onSearchClicked()
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                )
            }

            // Saved searches
            if (savedSearches.isNotEmpty()) {
                item {
                    SavedSearchesRow(
                        searches = savedSearches,
                        onSearchClicked = onSavedSearchClicked,
                        onDeleteClicked = onDeleteSavedSearchClicked,
                    )
                }
            }

            // Filter items — read revision here so items recompose on reset
            items(filters.size) { index ->
                @Suppress("UNUSED_EXPRESSION")
                revision
                FilterItem(
                    filter = filters[index],
                    onUpdate = triggerUpdate,
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun FilterSheetHeader(
    onReset: () -> Unit,
    onSave: () -> Unit,
    onFilter: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onReset,
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("Reset")
        }

        Spacer(Modifier.weight(1f))

        IconButton(onClick = onSave) {
            Icon(
                Icons.Default.Save,
                contentDescription = "Save search",
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        FilledTonalButton(
            onClick = onFilter,
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("Filter")
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun SavedSearchesRow(
    searches: List<SavedSearch>,
    onSearchClicked: (Long) -> Unit,
    onDeleteClicked: (Long) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            "Saved Searches",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyRow(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(searches, key = { it.id }) { search ->
                ElevatedSuggestionChip(
                    onClick = { onSearchClicked(search.id) },
                    label = { Text(search.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    icon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onDeleteClicked(search.id) },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = SuggestionChipDefaults.elevatedSuggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
            }
        }
    }
}

@Composable
private fun FilterItem(filter: Filter<*>, onUpdate: () -> Unit) {
    when (filter) {
        is Filter.Header -> {
            Text(
                text = filter.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        is Filter.Separator -> {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
        is Filter.CheckBox -> {
            var checked by remember(filter) { mutableStateOf(filter.state) }
            FilterCheckbox(
                label = filter.name,
                checked = checked,
                onCheckedChange = {
                    checked = !checked
                    filter.state = checked
                    onUpdate()
                },
            )
        }
        is Filter.TriState -> {
            FilterTriState(filter, onUpdate)
        }
        is Filter.Text -> {
            FilterText(filter, onUpdate)
        }
        is Filter.Select<*> -> {
            FilterSelect(filter, onUpdate)
        }
        is Filter.Sort -> {
            var sortState by remember(filter) { mutableStateOf(filter.state) }
            FilterCollapsibleGroup(filter.name) {
                filter.values.forEachIndexed { index, item ->
                    val isSelected = sortState?.index == index
                    val ascending = if (isSelected) sortState?.ascending else null
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val asc = if (isSelected) !sortState!!.ascending else true
                                sortState = Filter.Sort.Selection(index, asc)
                                filter.state = sortState
                                onUpdate()
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                        )
                        Text(
                            text = item + when (ascending) {
                                true -> " ▲"
                                false -> " ▼"
                                null -> ""
                            },
                            modifier = Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
        is Filter.Group<*> -> {
            FilterCollapsibleGroup(filter.name) {
                filter.state.filterIsInstance<Filter<*>>().forEach {
                    FilterItem(it, onUpdate)
                }
            }
        }
        is Filter.AutoComplete -> {
            FilterAutoComplete(filter, onUpdate)
        }
    }
}

@Composable
private fun FilterCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onCheckedChange() })
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun FilterTriState(filter: Filter.TriState, onUpdate: () -> Unit) {
    var state by remember(filter) { mutableIntStateOf(filter.state) }
    val states = listOf("Ignore", "Include", "Exclude")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                state = (state + 1) % 3
                filter.state = state
                onUpdate()
            }
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = state != Filter.TriState.STATE_IGNORE,
            onCheckedChange = {
                state = (state + 1) % 3
                filter.state = state
                onUpdate()
            },
        )
        Text(
            text = filter.name,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (state != Filter.TriState.STATE_IGNORE) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (state == Filter.TriState.STATE_INCLUDE) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
            ) {
                Text(
                    text = states[state],
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state == Filter.TriState.STATE_INCLUDE) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    },
                )
            }
        }
    }
}

@Composable
private fun FilterText(filter: Filter.Text, onUpdate: () -> Unit) {
    var text by remember { mutableStateOf(filter.state) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            filter.state = it
            onUpdate()
        },
        label = { Text(filter.name) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSelect(filter: Filter.Select<*>, onUpdate: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(filter.state) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        OutlinedTextField(
            value = filter.values[selectedIndex].toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text(filter.name) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            filter.values.forEachIndexed { index, value ->
                DropdownMenuItem(
                    text = { Text(value.toString()) },
                    onClick = {
                        selectedIndex = index
                        filter.state = index
                        expanded = false
                        onUpdate()
                    },
                )
            }
        }
    }
}

@Composable
private fun FilterCollapsibleGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Surface(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 8.dp)) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterAutoComplete(filter: Filter.AutoComplete, onUpdate: () -> Unit) {
    val values = remember { filter.values.toImmutableList() }
    val skipAutoFillTags = remember { filter.skipAutoFillTags.toImmutableList() }
    val validPrefixes = remember { filter.validPrefixes.toImmutableList() }
    var tags by remember { mutableStateOf(filter.state.toImmutableList()) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        AutoCompleteTextField(
            values = values,
            label = filter.name,
            placeholder = filter.hint,
            onValueFilter = { tag ->
                val prefix = validPrefixes.find { tag.startsWith(it) }
                val tagNoPrefix = if (prefix != null) tag.removePrefix(prefix) else tag
                Pair({ it: String -> it.contains(tagNoPrefix, true) }, prefix)
            },
            onSubmit = { tag ->
                val tagNoPrefix = validPrefixes.find { tag.startsWith(it) }
                    ?.let { tag.removePrefix(it).trim() } ?: tag
                if (tagNoPrefix !in skipAutoFillTags) {
                    val newTags = tags + tag
                    tags = newTags.toImmutableList()
                    filter.state = newTags
                    onUpdate()
                    true
                } else {
                    false
                }
            },
        )
        FlowRow(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tags.forEach { tag ->
                InputChip(
                    selected = false,
                    onClick = {
                        val newTags = tags - tag
                        tags = newTags.toImmutableList()
                        filter.state = newTags
                        onUpdate()
                    },
                    label = {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove $tag",
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = InputChipDefaults.inputChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoCompleteTextField(
    label: String? = null,
    placeholder: String? = null,
    values: ImmutableList<String>,
    onValueFilter: (String) -> Pair<(String) -> Boolean, String?>,
    onSubmit: (String) -> Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf(TextFieldValue("")) }
    val focusManager = LocalFocusManager.current

    fun submit() {
        if (onSubmit(value.text)) {
            focusManager.clearFocus()
            value = TextFieldValue("")
        }
    }

    BackHandler(expanded) {
        focusManager.clearFocus()
        expanded = false
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                value = it
                expanded = true
            },
            label = if (label != null) {
                { Text(label) }
            } else {
                null
            },
            placeholder = if (placeholder != null) {
                { Text(placeholder) }
            } else {
                null
            },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .fillMaxWidth(),
            singleLine = true,
            keyboardActions = KeyboardActions { submit() },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            shape = RoundedCornerShape(16.dp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )

        val filteredValues by produceState(emptyList(), value) {
            withContext(Dispatchers.Default) {
                val (filter, prefix) = onValueFilter(value.text)
                this@produceState.value = values.asSequence()
                    .filter(filter)
                    .take(100)
                    .let {
                        if (prefix != null) it.map { tag -> prefix + tag } else it
                    }
                    .toList()
            }
        }

        if (value.text.length > 2 && filteredValues.isNotEmpty()) {
            ExposedDropdownMenu(
                modifier = Modifier.exposedDropdownSize(matchAnchorWidth = true),
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                filteredValues.fastForEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            value = TextFieldValue(it, TextRange(it.length))
                            submit()
                        },
                    )
                }
            }
        }
    }
}
