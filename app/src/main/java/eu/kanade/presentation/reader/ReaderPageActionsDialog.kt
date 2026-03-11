package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.ActionButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ReaderPageActionsDialog(
    onDismissRequest: () -> Unit,
    // SY -->
    onSetAsCover: (useExtraPage: Boolean) -> Unit,
    onShare: (copy: Boolean, useExtraPage: Boolean) -> Unit,
    onSave: (useExtraPage: Boolean) -> Unit,
    onShareCombined: (copy: Boolean) -> Unit,
    onSaveCombined: () -> Unit,
    hasExtraPage: Boolean,
    // SY <--
) {
    var showSetCoverDialog by remember { mutableStateOf(false) }
    var useExtraPage by remember { mutableStateOf(false) }

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.padding.medium)
                    .padding(bottom = MaterialTheme.padding.small),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 1.dp,
            ) {
                Text(
                    text = stringResource(MR.strings.pref_reader_actions),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                )
            }

            Row(
                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(
                        if (hasExtraPage) {
                            SYMR.strings.action_set_first_page_cover
                        } else {
                            MR.strings.set_as_cover
                        },
                    ),
                    icon = Icons.Outlined.Photo,
                    onClick = {
                        useExtraPage = false
                        showSetCoverDialog = true
                    },
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(
                        if (hasExtraPage) {
                            SYMR.strings.action_copy_first_page
                        } else {
                            MR.strings.action_copy_to_clipboard
                        },
                    ),
                    icon = Icons.Outlined.ContentCopy,
                    onClick = {
                        onShare(true, false)
                        onDismissRequest()
                    },
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(
                        if (hasExtraPage) {
                            SYMR.strings.action_share_first_page
                        } else {
                            MR.strings.action_share
                        },
                    ),
                    icon = Icons.Outlined.Share,
                    onClick = {
                        onShare(false, false)
                        onDismissRequest()
                    },
                )

                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(
                        if (hasExtraPage) {
                            SYMR.strings.action_save_first_page
                        } else {
                            MR.strings.action_save
                        },
                    ),
                    icon = Icons.Outlined.Save,
                    onClick = {
                        onSave(false)
                        onDismissRequest()
                    },
                )
            }
            if (hasExtraPage) {
                Row(
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(SYMR.strings.action_set_second_page_cover),
                        icon = Icons.Outlined.Photo,
                        onClick = {
                            useExtraPage = true
                            showSetCoverDialog = true
                        },
                    )
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(SYMR.strings.action_copy_second_page),
                        icon = Icons.Outlined.ContentCopy,
                        onClick = {
                            onShare(true, true)
                            onDismissRequest()
                        },
                    )
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(SYMR.strings.action_share_second_page),
                        icon = Icons.Outlined.Share,
                        onClick = {
                            onShare(false, true)
                            onDismissRequest()
                        },
                    )
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(SYMR.strings.action_save_second_page),
                        icon = Icons.Outlined.Save,
                        onClick = {
                            onSave(true)
                            onDismissRequest()
                        },
                    )
                }
                Row(
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(SYMR.strings.action_copy_combined_page),
                        icon = Icons.Outlined.ContentCopy,
                        onClick = {
                            onShareCombined(true)
                            onDismissRequest()
                        },
                    )
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(SYMR.strings.action_share_combined_page),
                        icon = Icons.Outlined.Share,
                        onClick = {
                            onShareCombined(false)
                            onDismissRequest()
                        },
                    )
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(SYMR.strings.action_save_combined_page),
                        icon = Icons.Outlined.Save,
                        onClick = {
                            onSaveCombined()
                            onDismissRequest()
                        },
                    )
                }
            }
        }
    }

    if (showSetCoverDialog) {
        SetCoverDialog(
            onConfirm = {
                onSetAsCover(useExtraPage)
                showSetCoverDialog = false
                useExtraPage = false
            },
            onDismiss = {
                showSetCoverDialog = false
                useExtraPage = false
            },
        )
    }
}

@Composable
private fun SetCoverDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        text = {
            Text(stringResource(MR.strings.confirm_set_image_as_cover))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
        onDismissRequest = onDismiss,
    )
}
