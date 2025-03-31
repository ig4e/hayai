package eu.kanade.presentation.webview.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.DialogProperties
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.material.BottomSheetAlertDialog
import tachiyomi.presentation.core.components.material.CustomTextField
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun IgneousDialog(
    onDismissRequest: () -> Unit,
    onIgneousSet: (String) -> Unit,
) {
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    BottomSheetAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(SYMR.strings.custom_igneous_cookie)) },
        text = {
            Column {
                Text(text = stringResource(SYMR.strings.custom_igneous_cookie_message))
                CustomTextField(
                    value = textFieldValue.text,
                    onValueChange = { textFieldValue = TextFieldValue(it) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
        ),
        confirmButton = {
            TextButton(
                onClick = {
                    onIgneousSet(textFieldValue.text.trim())
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
    )
}
