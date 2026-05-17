package eu.kanade.tachiyomi.ui.source.browse.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import eu.kanade.tachiyomi.source.model.Filter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Autocomplete tag input. Visually matches the redesigned filter sheet — rounded filled text
 * field, chip stack flowing below. Behaviour mirrored verbatim from the legacy
 * `AutoCompleteItem.kt` (length-3 trigger, 100-item cap, prefix-stripped matching,
 * `skipAutoFillTags` rejection, tap-chip-to-remove), so any source that depends on this
 * (most notably NHentai) keeps producing the same query.
 *
 * State mutation routed through [FilterMutations] so unit tests can exercise the same path.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AutoCompleteFilterRow(filter: Filter.AutoComplete) {
    var chips by remember(filter) { mutableStateOf(filter.state) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AutoCompleteTextField(
            label = filter.name,
            placeholder = filter.hint,
            values = filter.values,
            onValueFilter = { tag ->
                val prefix = filter.validPrefixes.find { tag.startsWith(it) }
                val tagNoPrefix = if (prefix != null) tag.removePrefix(prefix) else tag
                Pair({ it: String -> it.contains(tagNoPrefix, ignoreCase = true) }, prefix)
            },
            onSubmit = { tag ->
                val added = FilterMutations.addAutoCompleteTag(filter, tag)
                if (added) chips = filter.state
                added
            },
        )
        if (chips.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                chips.forEach { chip ->
                    InputChip(
                        selected = true,
                        onClick = {
                            FilterMutations.removeAutoCompleteTag(filter, chip)
                            chips = filter.state
                        },
                        shape = RoundedCornerShape(50),
                        label = {
                            Text(
                                text = chip,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = null,
                                modifier = Modifier.size(InputChipDefaults.IconSize),
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTrailingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoCompleteTextField(
    label: String,
    placeholder: String,
    values: List<String>,
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
            expanded = false
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
            placeholder = {
                Text(
                    text = if (placeholder.isNotEmpty()) placeholder else label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardActions = KeyboardActions(onAny = { submit() }),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )

        val filteredValues by produceState(emptyList<String>(), value) {
            withContext(Dispatchers.Default) {
                val (predicate, prefix) = onValueFilter(value.text)
                this@produceState.value = values.asSequence()
                    .filter(predicate)
                    .take(MAX_AUTOCOMPLETE_SUGGESTIONS)
                    .let {
                        if (prefix != null) it.map { tag -> prefix + tag } else it
                    }
                    .toList()
            }
        }
        if (value.text.length > AUTOCOMPLETE_TRIGGER_LENGTH && filteredValues.isNotEmpty()) {
            ExposedDropdownMenu(
                modifier = Modifier.exposedDropdownSize(matchAnchorWidth = true),
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(20.dp),
            ) {
                filteredValues.fastForEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            value = TextFieldValue(suggestion, TextRange(suggestion.length))
                            submit()
                        },
                    )
                }
            }
        }
    }
}

private const val MAX_AUTOCOMPLETE_SUGGESTIONS = 100
private const val AUTOCOMPLETE_TRIGGER_LENGTH = 2
