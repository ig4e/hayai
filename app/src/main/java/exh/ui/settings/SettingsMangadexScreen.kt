package exh.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.core.storage.preference.collectAsState
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.util.system.toast
import exh.md.utils.MdUtil
import exh.source.ExhPreferences
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import yokai.i18n.MR
import yokai.presentation.component.preference.Preference
import yokai.presentation.settings.ComposableSettings

object SettingsMangadexScreen : ComposableSettings() {

    private fun readResolve(): Any = SettingsMangadexScreen

    @Composable
    @ReadOnlyComposable
    override fun getTitleRes(): StringResource = MR.strings.pref_category_mangadex

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current

        return buildList {
            add(getLoginPreference())
            add(getSyncMangaDexIntoThis())
            add(getSyncLibraryToMangaDex())
        }.toPersistentList()
    }

    @Composable
    private fun getLoginPreference(): Preference.PreferenceItem.TextPreference {
        val context = LocalContext.current
        var logoutDialogOpen by remember { mutableStateOf(false) }

        if (logoutDialogOpen) {
            AlertDialog(
                onDismissRequest = { logoutDialogOpen = false },
                title = { Text(text = stringResource(MR.strings.logout)) },
                confirmButton = {
                    TextButton(onClick = {
                        logoutDialogOpen = false
                        // TODO: Implement MangaDex logout
                    }) {
                        Text(text = stringResource(MR.strings.logout))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { logoutDialogOpen = false }) {
                        Text(text = stringResource(MR.strings.action_cancel))
                    }
                },
            )
        }

        return Preference.PreferenceItem.TextPreference(
            title = "MangaDex Login",
            subtitle = stringResource(MR.strings.log_in_to_mangadex),
            onClick = {
                // TODO: Implement MangaDex login flow
            },
        )
    }

    @Composable
    private fun getSyncMangaDexIntoThis(): Preference.PreferenceItem.TextPreference {
        val context = LocalContext.current
        var dialogOpen by remember { mutableStateOf(false) }

        if (dialogOpen) {
            val items = remember {
                listOf("Reading", "Plan to Read", "Completed", "Re-Reading", "Dropped", "On Hold")
            }
            val selection = remember {
                List(items.size) { index -> index == 0 || index == 5 }.toMutableStateList()
            }
            AlertDialog(
                onDismissRequest = { dialogOpen = false },
                title = { Text(text = stringResource(MR.strings.mangadex_sync_follows_to_library)) },
                text = {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        items.forEachIndexed { index, followOption ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val checked = selection.getOrNull(index) ?: false
                                        selection[index] = !checked
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = selection.getOrNull(index) ?: false,
                                    onCheckedChange = null,
                                )
                                Text(
                                    text = followOption,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        dialogOpen = false
                        // TODO: Start sync follows job
                    }) {
                        Text(text = stringResource(MR.strings.action_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialogOpen = false }) {
                        Text(text = stringResource(MR.strings.action_cancel))
                    }
                },
            )
        }

        return Preference.PreferenceItem.TextPreference(
            title = stringResource(MR.strings.mangadex_sync_follows_to_library),
            subtitle = stringResource(MR.strings.mangadex_sync_follows_to_library_summary),
            onClick = { dialogOpen = true },
        )
    }

    @Composable
    private fun getSyncLibraryToMangaDex(): Preference.PreferenceItem.TextPreference {
        val context = LocalContext.current
        return Preference.PreferenceItem.TextPreference(
            title = stringResource(MR.strings.mangadex_push_favorites_to_mangadex),
            subtitle = stringResource(MR.strings.mangadex_push_favorites_to_mangadex_summary),
            onClick = {
                // TODO: Start push favorites job
            },
        )
    }
}
