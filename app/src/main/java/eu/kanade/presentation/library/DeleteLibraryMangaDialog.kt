package eu.kanade.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.UnifiedBottomSheet
import eu.kanade.presentation.util.isTabletUi
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.LabeledCheckbox
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun DeleteLibraryMangaDialog(
    containsLocalManga: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (Boolean, Boolean) -> Unit,
) {
    var list by remember {
        mutableStateOf(
            buildList<CheckboxState.State<StringResource>> {
                add(CheckboxState.State.None(MR.strings.manga_from_library))
                if (!containsLocalManga) {
                    add(CheckboxState.State.None(MR.strings.downloaded_chapters))
                }
            },
        )
    }

    val onConfirmAction = {
        onDismissRequest()
        onConfirm(
            list[0].isChecked,
            list.getOrElse(1) { CheckboxState.State.None(0) }.isChecked,
        )
    }

    val isConfirmEnabled = list.any { it.isChecked }

    if (isTabletUi()) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = isConfirmEnabled,
                    onClick = onConfirmAction,
                ) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            },
            title = {
                Text(text = stringResource(MR.strings.action_remove))
            },
            text = {
                Column {
                    list.forEach {
                        CheckboxContent(state = it) { newState ->
                            val index = list.indexOf(it)
                            if (index != -1) {
                                val mutableList = list.toMutableList()
                                mutableList[index] = newState
                                list = mutableList.toList()
                            }
                        }
                    }
                }
            },
        )
    } else {
        UnifiedBottomSheet(
            onDismissRequest = onDismissRequest,
            title = {
                Text(text = stringResource(MR.strings.action_remove))
            },
            actions = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(MR.strings.action_cancel))
                    }
                    TextButton(
                        enabled = isConfirmEnabled,
                        onClick = onConfirmAction,
                    ) {
                        Text(text = stringResource(MR.strings.action_ok))
                    }
                }
            },
        ) {
            Column(
            ) {
                list.forEach {
                    CheckboxContent(state = it) { newState ->
                        val index = list.indexOf(it)
                        if (index != -1) {
                            val mutableList = list.toMutableList()
                            mutableList[index] = newState
                            list = mutableList.toList()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckboxContent(
    state: CheckboxState.State<StringResource>,
    onCheckedChange: (CheckboxState.State<StringResource>) -> Unit,
) {
    LabeledCheckbox(
        label = stringResource(state.value),
        checked = state.isChecked,
        onCheckedChange = { onCheckedChange(state.next() as CheckboxState.State<StringResource>) },
    )
}
