package eu.kanade.presentation.updates

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.UnifiedBottomSheet
import eu.kanade.presentation.util.isTabletUi
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun UpdatesDeleteConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    val onConfirmAction = {
        onConfirm()
        onDismissRequest()
    }

    if (isTabletUi()) {
        AlertDialog(
            text = {
                Text(text = stringResource(MR.strings.confirm_delete_chapters))
            },
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(onClick = onConfirmAction) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            },
        )
    } else {
        UnifiedBottomSheet(
            onDismissRequest = onDismissRequest,
            title = {
                Text(text = stringResource(MR.strings.are_you_sure))
            },
            actions = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
                TextButton(onClick = onConfirmAction) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            },
        ) {
            Text(
                text = stringResource(MR.strings.confirm_delete_chapters),
            )
        }
    }
}
