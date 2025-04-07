package eu.kanade.presentation.library.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import eu.kanade.presentation.components.UnifiedBottomSheet
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun SyncFavoritesConfirmDialog(
    onDismissRequest: () -> Unit,
    onAccept: () -> Unit,
) {
    UnifiedBottomSheet(
        onDismissRequest = onDismissRequest,
        actions = @Composable {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
            TextButton(onClick = onAccept) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        title = @Composable {
            Text(stringResource(SYMR.strings.favorites_sync))
        },
        content = @Composable {
            Text(text = stringResource(SYMR.strings.favorites_sync_conformation_message))
        },
    )
}
