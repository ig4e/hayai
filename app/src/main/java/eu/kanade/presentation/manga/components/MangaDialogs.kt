package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import eu.kanade.tachiyomi.util.system.isDevFlavor
import eu.kanade.tachiyomi.util.system.isPreviewBuildType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import tachiyomi.domain.manga.interactor.FetchInterval
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.OutlinedNumericChooser
import tachiyomi.presentation.core.components.WheelTextPicker
import tachiyomi.presentation.core.components.material.BottomSheetAlertDialog
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

@Composable
fun DeleteChapterDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    BottomSheetAlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onConfirm()
                },
            ) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        title = {
            Text(text = stringResource(MR.strings.are_you_sure))
        },
        text = {
            Text(text = stringResource(MR.strings.confirm_delete_chapters))
        },
    )
}

@Composable
fun SetIntervalDialog(
    interval: Int,
    nextUpdate: Instant?,
    onDismissRequest: () -> Unit,
    onValueChanged: ((Int) -> Unit)? = null,
) {
    var days by remember { mutableStateOf((interval / (60 * 60 * 24)).toString()) }
    val isValid = remember(days) { days.toIntOrNull() != null }

    BottomSheetAlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onValueChanged?.invoke(
                        days.toIntOrNull()?.let { it * 60 * 60 * 24 } ?: (7 * 24 * 60 * 60),
                    )
                },
                enabled = isValid,
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
            Text(text = stringResource(MR.strings.pref_library_update_interval))
        },
        text = {
            OutlinedTextField(
                value = days,
                onValueChange = { days = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(MR.strings.update_interval_days)) },
                trailingIcon = { Text(text = stringResource(MR.strings.days_suffix)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                isError = !isValid,
                singleLine = true,
            )

            if (nextUpdate != null) {
                val dateTimeFormatter = remember {
                    DateTimeFormatter
                        .ofPattern("dd/MM/yyyy HH:mm")
                }
                val formattedDate = remember(nextUpdate) {
                    try {
                        val localDateTime = LocalDateTime.ofInstant(
                            nextUpdate,
                            ZoneId.systemDefault(),
                        )
                        dateTimeFormatter.format(localDateTime)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (formattedDate != null) {
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            text = stringResource(MR.strings.next_update, formattedDate),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
    )
}
