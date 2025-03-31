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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.category.visualName
import eu.kanade.tachiyomi.util.lang.containsFuzzy
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.material.BottomSheetAlertDialog
import tachiyomi.presentation.core.components.material.Checkbox
import tachiyomi.presentation.core.components.material.CustomTextField
import tachiyomi.presentation.core.components.material.TriStateCheckbox
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.source.local.isLocal
import kotlin.time.Duration.Companion.seconds

/**
 * Extension function to convert a CheckboxState.TriState to a ToggleableState for use with TriStateCheckbox
 */
fun <T> CheckboxState.TriState<T>.asToggleableState(): ToggleableState = when (this) {
    is CheckboxState.TriState.Exclude -> ToggleableState.Indeterminate
    is CheckboxState.TriState.Include -> ToggleableState.On
    is CheckboxState.TriState.None -> ToggleableState.Off
}

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

    BottomSheetAlertDialog(
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
            Column {
                TextField(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = stringResource(MR.strings.name)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    isError = nameAlreadyExists,
                    supportingText = {
                        // SY -->
                        if (extraMessage != null) {
                            Text(text = extraMessage)
                        }
                        // SY <--
                        if (nameAlreadyExists) {
                            // SY -->
                            Text(text = stringResource(alreadyExistsError))
                            // SY <--
                        }
                    },
                    singleLine = true,
                )
            }
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
    // SY -->
    extraMessage: String? = null,
    alreadyExistsError: StringResource = MR.strings.error_category_exists,
    // SY <--
) {
    var name by remember { mutableStateOf(category) }

    val focusRequester = remember { FocusRequester() }
    val nameAlreadyExists = remember(name) { categories.contains(name) }

    BottomSheetAlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                enabled = name.isNotEmpty() && !nameAlreadyExists && name != category,
                onClick = {
                    onRename(name)
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(MR.strings.action_rename))
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
            Column {
                TextField(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = stringResource(MR.strings.name)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    isError = nameAlreadyExists,
                    supportingText = {
                        // SY -->
                        if (extraMessage != null) {
                            Text(text = extraMessage)
                        }
                        // SY <--
                        if (nameAlreadyExists) {
                            // SY -->
                            Text(text = stringResource(alreadyExistsError))
                            // SY <--
                        }
                    },
                    singleLine = true,
                )
            }
        },
    )
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
    BottomSheetAlertDialog(
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
    if (initialSelection.isEmpty()) {
        BottomSheetAlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismissRequest()
                        onEditCategories()
                    },
                ) {
                    Text(text = stringResource(MR.strings.action_edit_categories))
                }
            },
            title = {
                Text(text = stringResource(MR.strings.action_move_category))
            },
            text = {
                Text(text = stringResource(MR.strings.information_empty_category_dialog))
            },
        )
        return
    }

    var selection by remember { mutableStateOf(initialSelection) }
    var searchQuery by remember { mutableStateOf("") }

    val context = LocalContext.current

    val filteredSelection = remember(selection, searchQuery) {
        // First sort the categories so selected ones appear at the top
        val sortedSelection = selection.sortedWith(
            compareByDescending<CheckboxState<Category>> { checkbox ->
                when (checkbox) {
                    is CheckboxState.State.Checked,
                    is CheckboxState.TriState.Include -> true
                    else -> false
                }
            }
        ).toImmutableList()

        // Then apply the search filter if needed
        if (searchQuery.isEmpty()) {
            sortedSelection
        } else {
            sortedSelection.filter { checkbox ->
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

    BottomSheetAlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Dialog header with more compact title
                Text(
                    text = stringResource(MR.strings.action_move_category),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                )

                // Search bar
                CustomTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    placeholder = { Text(text = stringResource(MR.strings.action_search)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                )

                // Categories list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                ) {
                    items(
                        items = filteredSelection,
                        key = { it.value.id }
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
                            }
                        )
                    }
                }

                Divider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Action buttons with improved layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            onDismissRequest()
                            onEditCategories()
                        },
                    ) {
                        Text(text = stringResource(MR.strings.action_edit))
                    }

                    Row {
                        TextButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
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
                            }
                        ) {
                            Text(text = stringResource(MR.strings.action_ok))
                        }
                    }
                }
            }
        }
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
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (checkbox) {
            is CheckboxState.TriState -> {
                TriStateCheckbox(
                    state = checkbox.asToggleableState(),
                    onClick = { onToggle(checkbox) },
                )
            }
            is CheckboxState.State -> {
                Checkbox(
                    checked = checkbox.isChecked,
                    onCheckedChange = { onToggle(checkbox) },
                )
            }
        }

        Text(
            text = categoryName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
