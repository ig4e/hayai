package exh.ui.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.util.system.toast
import exh.source.ExhPreferences
import exh.uconfig.EHConfigurator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import yokai.i18n.MR
import kotlin.time.Duration.Companion.seconds

@Composable
fun ConfigureExhDialog(run: Boolean, onRunning: () -> Unit) {
    val exhPreferences = remember { Injekt.get<ExhPreferences>() }
    var warnDialogOpen by remember { mutableStateOf(false) }
    var configureDialogOpen by remember { mutableStateOf(false) }
    var configureFailedDialogOpen by remember { mutableStateOf<Exception?>(null) }

    LaunchedEffect(run) {
        if (run) {
            if (exhPreferences.exhShowSettingsUploadWarning().get()) {
                warnDialogOpen = true
            } else {
                configureDialogOpen = true
            }
            onRunning()
        }
    }

    if (warnDialogOpen) {
        AlertDialog(
            onDismissRequest = { warnDialogOpen = false },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
            confirmButton = {
                TextButton(
                    onClick = {
                        exhPreferences.exhShowSettingsUploadWarning().set(false)
                        configureDialogOpen = true
                        warnDialogOpen = false
                    },
                ) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            },
            title = {
                Text(text = stringResource(MR.strings.settings_profile_note))
            },
            text = {
                Text(text = stringResource(MR.strings.settings_profile_note_message))
            },
        )
    }

    if (configureDialogOpen) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO + NonCancellable) {
                try {
                    delay(0.2.seconds)
                    EHConfigurator(context).configureAll()
                    withContext(Dispatchers.Main) {
                        context.toast(MR.strings.eh_settings_successfully_uploaded)
                    }
                } catch (e: Exception) {
                    configureFailedDialogOpen = e
                } finally {
                    configureDialogOpen = false
                }
            }
        }
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
            confirmButton = {},
            title = {
                Text(text = stringResource(MR.strings.eh_settings_uploading_to_server))
            },
            text = {
                Text(text = stringResource(MR.strings.eh_settings_uploading_to_server_message))
            },
        )
    }

    if (configureFailedDialogOpen != null) {
        AlertDialog(
            onDismissRequest = { configureFailedDialogOpen = null },
            confirmButton = {
                TextButton(onClick = { configureFailedDialogOpen = null }) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            },
            title = {
                Text(text = stringResource(MR.strings.eh_settings_configuration_failed))
            },
            text = {
                Text(
                    text = stringResource(MR.strings.eh_settings_configuration_failed_message),
                )
            },
        )
    }
}
