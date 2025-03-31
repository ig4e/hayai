package eu.kanade.presentation.browse.components

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.components.material.BottomSheetAlertDialog

@Composable
fun MigrationExitDialog(
    onDismissRequest: () -> Unit,
    exitMigration: () -> Unit,
) {
    BottomSheetAlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = exitMigration) {
                Text(text = stringResource(SYMR.strings.action_stop))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(text = stringResource(SYMR.strings.stop_migrating))
        },
    )
}
