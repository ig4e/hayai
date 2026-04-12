package eu.kanade.tachiyomi.ui.source.filter

import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.source.model.Filter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoCompleteItem(val filter: Filter.AutoComplete) : AbstractFlexibleItem<AutoCompleteItem.Holder>() {

    override fun getLayoutRes(): Int = android.R.layout.simple_list_item_1

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
    ): Holder {
        val composeView = ComposeView(view.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        return Holder(composeView, adapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: Holder,
        position: Int,
        payloads: MutableList<Any?>?,
    ) {
        (holder.itemView as ComposeView).setContent {
            MaterialTheme {
                AutoCompleteContent(
                    name = filter.name,
                    hint = filter.hint,
                    state = filter.state.toImmutableList(),
                    values = filter.values.toImmutableList(),
                    skipAutoFillTags = filter.skipAutoFillTags.toImmutableList(),
                    validPrefixes = filter.validPrefixes.toImmutableList(),
                    onChange = { filter.state = it },
                )
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return filter == (other as AutoCompleteItem).filter
    }

    override fun hashCode(): Int = filter.hashCode()

    class Holder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>) :
        FlexibleViewHolder(view, adapter)
}

@Composable
private fun AutoCompleteContent(
    name: String,
    hint: String,
    state: ImmutableList<String>,
    values: ImmutableList<String>,
    skipAutoFillTags: ImmutableList<String>,
    validPrefixes: ImmutableList<String>,
    onChange: (List<String>) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        AutoCompleteTextField(
            values = values,
            label = name,
            placeholder = hint,
            onValueFilter = { tag ->
                val prefix = validPrefixes.find { tag.startsWith(it) }
                val tagNoPrefix = if (prefix != null) tag.removePrefix(prefix) else tag
                Pair({ it: String -> it.contains(tagNoPrefix, true) }, prefix)
            },
            onSubmit = { tag ->
                val tagNoPrefix = validPrefixes.find { tag.startsWith(it) }?.let { tag.removePrefix(it).trim() } ?: tag
                if (tagNoPrefix !in skipAutoFillTags) {
                    onChange(state + tag)
                    true
                } else {
                    false
                }
            },
        )
        FlowRow(
            modifier = Modifier.padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            state.forEach {
                InputChip(
                    selected = false,
                    onClick = { onChange(state - it) },
                    label = {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingIcon = {
                        Icon(Icons.Default.Close, contentDescription = it)
                    },
                    colors = InputChipDefaults.inputChipColors(
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        labelColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        }
    }
}

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
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )

        val filteredValues by produceState(emptyList(), value) {
            withContext(Dispatchers.Default) {
                val (filter, prefix) = onValueFilter(value.text)
                this@produceState.value = values.asSequence()
                    .filter(filter)
                    .take(100)
                    .let {
                        if (prefix != null) {
                            it.map { tag -> prefix + tag }
                        } else {
                            it
                        }
                    }
                    .toList()
            }
        }
        if (value.text.length > 2 && filteredValues.isNotEmpty()) {
            ExposedDropdownMenu(
                modifier = Modifier.exposedDropdownSize(matchAnchorWidth = true),
                expanded = expanded,
                onDismissRequest = { expanded = false },
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
