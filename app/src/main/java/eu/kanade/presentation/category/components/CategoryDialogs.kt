package eu.kanade.presentation.category.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.core.preference.asToggleableState
import eu.kanade.presentation.category.visualName
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.seconds
import androidx.compose.material3.rememberTopAppBarState
import tachiyomi.presentation.core.components.material.Checkbox
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.material.CustomTextField
import eu.kanade.tachiyomi.util.lang.containsFuzzy

@Composable
fun CategoryCreateDialog(
    onDismissRequest: () -> Unit,
    onCreate: (String) -> Unit,
    categories: ImmutableList<String>,
    // SY -->
    title: String = stringResource(MR.strings.action_add_category),
    extraMessage: String? = null,
    alreadyExistsError: StringResource = MR.strings.error_category_exists,
    // SY <--
) {
    var name by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val nameAlreadyExists = remember(name) { categories.contains(name) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                enabled = name.isNotEmpty() && !nameAlreadyExists,
                onClick = {
                    onCreate(name)
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(MR.strings.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            // SY -->
            Text(text = title)
            // SY <--
        },
        text = {
            // SY -->
            Column {
                extraMessage?.let { Text(it) }
                // SY <--

                CustomTextField(
                    modifier = Modifier
                        .focusRequester(focusRequester),
                    value = name,
                    onValueChange = { name = it },
                    labelText = stringResource(MR.strings.name),
                    placeholder = {
                        val msgRes = if (name.isNotEmpty() && nameAlreadyExists) {
                            // SY -->
                            alreadyExistsError
                            // SY <--
                        } else {
                            MR.strings.information_required_plain
                        }
                        Text(text = stringResource(msgRes))
                    },
                    singleLine = true,
                )
                // SY -->
            }
            // SY <--
        },
    )

    LaunchedEffect(focusRequester) {
        // TODO: https://issuetracker.google.com/issues/204502668
        delay(0.1.seconds)
        focusRequester.requestFocus()
    }
}

@Composable
fun CategoryRenameDialog(
    onDismissRequest: () -> Unit,
    onRename: (String) -> Unit,
    categories: ImmutableList<String>,
    category: String,
) {
    var name by remember { mutableStateOf(category) }
    var valueHasChanged by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val nameAlreadyExists = remember(name) { categories.contains(name) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                enabled = valueHasChanged && !nameAlreadyExists,
                onClick = {
                    onRename(name)
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(text = stringResource(MR.strings.action_rename_category))
        },
        text = {
            CustomTextField(
                modifier = Modifier.focusRequester(focusRequester),
                value = name,
                onValueChange = {
                    valueHasChanged = name != it
                    name = it
                },
                labelText = stringResource(MR.strings.name),
                placeholder = {
                    val msgRes = if (valueHasChanged && nameAlreadyExists) {
                        MR.strings.error_category_exists
                    } else {
                        MR.strings.information_required_plain
                    }
                    Text(text = stringResource(msgRes))
                },
                singleLine = true,
            )
        },
    )

    LaunchedEffect(focusRequester) {
        // TODO: https://issuetracker.google.com/issues/204502668
        delay(0.1.seconds)
        focusRequester.requestFocus()
    }
}

@Composable
fun CategoryDeleteDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    // SY -->
    category: String = "",
    title: String = stringResource(MR.strings.delete_category),
    text: String = stringResource(MR.strings.delete_category_confirmation, category),
    // SY <--
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onDelete()
                onDismissRequest()
            }) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            // SY -->
            Text(text = title)
            // SY <--
        },
        text = {
            // SY -->
            Text(text = text)
            // SY <--
        },
    )
}

@Composable
fun ChangeCategoryDialog(
    initialSelection: ImmutableList<CheckboxState<Category>>,
    onDismissRequest: () -> Unit,
    onEditCategories: () -> Unit,
    onConfirm: (List<Long>, List<Long>) -> Unit,
) {
    var selection by remember { mutableStateOf(initialSelection) }
    var searchQuery by remember { mutableStateOf("") }

    val context = LocalContext.current

    val isEmpty = remember(initialSelection) { initialSelection.isEmpty() }

    val filteredSelection = remember(selection, searchQuery) {
        if (searchQuery.isEmpty()) {
            selection
        } else {
            selection.filter { checkbox ->
                val categoryName = checkbox.value.name
                val defaultName = if (checkbox.value.isSystemCategory) {
                    context.getString(MR.strings.label_default.resourceId)
                } else {
                    ""
                }

                containsFuzzy(categoryName, searchQuery, ignoreCase = true) ||
                    (defaultName.isNotEmpty() && containsFuzzy(defaultName, searchQuery, ignoreCase = true))
            }.toImmutableList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(MR.strings.action_move_category))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isEmpty) {
                    Text(
                        text = stringResource(MR.strings.information_empty_category_dialog),
                        modifier = Modifier.padding(vertical = MaterialTheme.padding.medium),
                    )
                } else {
                    // Search bar
                    CustomTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = MaterialTheme.padding.small),
                        placeholder = { Text(text = stringResource(MR.strings.action_search)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                    )

                    // Categories list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp), // Keep fixed height for now
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                    ) {
                        items(
                            items = filteredSelection,
                            key = { it.value.id },
                        ) { checkbox ->
                            CategoryRow(
                                checkbox = checkbox,
                                onToggle = { toggledCheckbox ->
                                    val index = selection.indexOf(toggledCheckbox)
                                    if (index != -1) {
                                        val mutableList = selection.toMutableList()
                                        mutableList[index] = toggledCheckbox.next()
                                        selection = mutableList.toList().toImmutableList()
                                    }
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                if (isEmpty) {
                    TextButton(
                        onClick = {
                            onDismissRequest()
                            onEditCategories()
                        },
                    ) {
                        Text(text = stringResource(MR.strings.action_edit_categories))
                    }
                } else {
                    TextButton(
                        onClick = {
                            onDismissRequest()
                            onEditCategories()
                        },
                    ) {
                        Text(text = stringResource(MR.strings.action_edit))
                    }
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(MR.strings.action_cancel))
                    }
                    Button(
                        onClick = {
                            onDismissRequest()
                            onConfirm(
                                selection
                                    .filter { it is CheckboxState.State.Checked || it is CheckboxState.TriState.Include }
                                    .map { it.value.id },
                                selection
                                    .filter { it is CheckboxState.State.None || it is CheckboxState.TriState.None }
                                    .map { it.value.id },
                            )
                        },
                    ) {
                        Text(text = stringResource(MR.strings.action_ok))
                    }
                }
            }
        },
        dismissButton = if (!isEmpty) {
            {
                // Place Cancel and OK inside confirmButton Row when not empty
            }
        } else {
            {
                // Standard Cancel button when empty
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            }
        },
    )
}

@Composable
private fun CategoryRow(
    checkbox: CheckboxState<Category>,
    onToggle: (CheckboxState<Category>) -> Unit,
) {
    val category = checkbox.value
    val categoryName = if (category.isSystemCategory) {
        stringResource(MR.strings.label_default)
    } else {
        category.name
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(checkbox) }
            .padding(vertical = MaterialTheme.padding.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (checkbox) {
            is CheckboxState.TriState -> {
                tachiyomi.presentation.core.components.material.TriStateCheckbox(
                    state = checkbox.asToggleableState(),
                    onClick = { onToggle(checkbox) },
                )
            }
            is CheckboxState.State -> {
                tachiyomi.presentation.core.components.material.Checkbox(
                    checked = checkbox.isChecked,
                    onCheckedChange = { onToggle(checkbox) },
                )
            }
        }

        Text(
            text = categoryName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
