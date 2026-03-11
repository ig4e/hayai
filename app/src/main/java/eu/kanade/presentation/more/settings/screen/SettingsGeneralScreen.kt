package eu.kanade.presentation.more.settings.screen

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.appearance.AppLanguageScreen
import eu.kanade.tachiyomi.BuildConfig
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.LocalDate

object SettingsGeneralScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_general

    @Composable
    override fun getPreferences(): List<Preference> {
        val uiPreferences = remember { Injekt.get<UiPreferences>() }
        val navigator = LocalNavigator.currentOrThrow
        val context = androidx.compose.ui.platform.LocalContext.current

        val now = remember { LocalDate.now() }
        val dateFormat by uiPreferences.dateFormat().collectAsState()
        val formattedNow = remember(dateFormat) {
            UiPreferences.dateFormat(dateFormat).format(now)
        }

        val generalItems = buildList {
            add(
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.startingTab(),
                    entries = mapOf(
                        -1 to stringResource(MR.strings.label_library),
                        -2 to stringResource(MR.strings.label_recents),
                        -3 to stringResource(MR.strings.browse),
                        0 to stringResource(MR.strings.last_used_library_recents),
                    ).toImmutableMap(),
                    title = stringResource(MR.strings.starting_screen),
                ),
            )
            add(
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.backToStart(),
                    title = stringResource(MR.strings.back_to_start),
                    subtitle = stringResource(MR.strings.pressing_back_to_start),
                ),
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                add(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_manage_notifications),
                        onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        },
                    ),
                )
            }
        }

        val autoUpdateItems = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && BuildConfig.INCLUDE_UPDATER) {
            persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.shouldAutoUpdate(),
                    entries = mapOf(
                        0 to stringResource(MR.strings.over_any_network),
                        1 to stringResource(MR.strings.over_wifi_only),
                        2 to stringResource(MR.strings.dont_auto_update),
                    ).toImmutableMap(),
                    title = stringResource(MR.strings.auto_update_app),
                ),
            )
        } else {
            null
        }

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_category_general),
                preferenceItems = generalItems.toPersistentList(),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.app_shortcuts),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = uiPreferences.showSeriesInShortcuts(),
                        title = stringResource(MR.strings.show_recent_series),
                        subtitle = stringResource(MR.strings.includes_recently_read_updated_added),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = uiPreferences.showSourcesInShortcuts(),
                        title = stringResource(MR.strings.show_recent_sources),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = uiPreferences.openChapterInShortcuts(),
                        title = stringResource(MR.strings.series_opens_new_chapters),
                        subtitle = stringResource(MR.strings.no_new_chapters_open_details),
                    ),
                ),
            ),
            autoUpdateItems?.let {
                Preference.PreferenceGroup(
                    title = stringResource(MR.strings.auto_updates),
                    preferenceItems = it,
                )
            },
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.locale),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.language),
                        subtitle = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            stringResource(MR.strings.language_requires_app_restart)
                        } else {
                            null
                        },
                        onClick = { navigator.push(AppLanguageScreen()) },
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = uiPreferences.dateFormat(),
                        entries = DateFormats
                            .associateWith {
                                val formattedDate = UiPreferences.dateFormat(it).format(now)
                                "${it.ifEmpty { stringResource(MR.strings.label_default) }} ($formattedDate)"
                            }
                            .toImmutableMap(),
                        title = stringResource(MR.strings.pref_date_format),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = uiPreferences.relativeTime(),
                        title = stringResource(MR.strings.pref_relative_format),
                        subtitle = stringResource(
                            MR.strings.pref_relative_format_summary,
                            stringResource(MR.strings.relative_time_today),
                            formattedNow,
                        ),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.navigation),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        preference = uiPreferences.longTapRecentsNavBehaviour(),
                        entries = UiPreferences.LongTapRecents.entries
                            .associateWith { stringResource(it.titleResId) }
                            .toImmutableMap(),
                        title = stringResource(MR.strings.recents_long_tap),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = uiPreferences.longTapBrowseNavBehaviour(),
                        entries = UiPreferences.LongTapBrowse.entries
                            .associateWith { stringResource(it.titleResId) }
                            .toImmutableMap(),
                        title = stringResource(MR.strings.browse_long_tap),
                    ),
                ),
            ),
        ).filterNotNull()
    }
}

private val DateFormats = listOf(
    "", // Default
    "MM/dd/yy",
    "dd/MM/yy",
    "yyyy-MM-dd",
)
