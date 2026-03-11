package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.launch
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource as presentationStringResource
import tachiyomi.presentation.core.util.collectAsState as collectPreferenceAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Settings screen for download-related preferences.
 * Manages storage, removal behavior, and automated downloads.
 */
object SettingsDownloadScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_downloads

    @Composable
    override fun getPreferences(): List<Preference> {
        val getCategories = remember { Injekt.get<GetCategories>() }
        val allCategories by getCategories.subscribe().collectAsState(initial = emptyList())

        val downloadPreferences = remember { Injekt.get<DownloadPreferences>() }

        return listOf(
            getGeneralGroup(downloadPreferences = downloadPreferences),
            getDeleteChaptersGroup(downloadPreferences = downloadPreferences, categories = allCategories),
            getDownloadNewChaptersGroup(downloadPreferences = downloadPreferences, categories = allCategories),
            getDownloadAheadGroup(downloadPreferences = downloadPreferences),
            getAutomaticRemovalGroup(downloadPreferences = downloadPreferences),
        )
    }

    /**
     * General download settings including Wi-Fi restrictions and cache management.
     */
    @Composable
    private fun getGeneralGroup(
        downloadPreferences: DownloadPreferences,
    ): Preference.PreferenceGroup {
        val downloadCache = remember { Injekt.get<DownloadCache>() }
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val parallelSourceLimit by downloadPreferences.parallelSourceLimit().collectPreferenceAsState()

        return Preference.PreferenceGroup(
            title = presentationStringResource(MR.strings.pref_category_general),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = downloadPreferences.downloadOnlyOverWifi(),
                    title = presentationStringResource(MR.strings.connected_to_wifi),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = downloadPreferences.saveChaptersAsCBZ(),
                    title = presentationStringResource(MR.strings.save_as_cbz),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = downloadPreferences.splitTallImages(),
                    title = presentationStringResource(MR.strings.split_tall_images),
                    subtitle = presentationStringResource(MR.strings.split_tall_images_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = downloadPreferences.downloadWithId(),
                    title = presentationStringResource(MR.strings.pref_download_with_id),
                    subtitle = presentationStringResource(MR.strings.pref_download_with_id_summary),
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = parallelSourceLimit,
                    valueRange = 1..10,
                    title = presentationStringResource(MR.strings.pref_download_concurrent_sources),
                    onValueChanged = {
                        downloadPreferences.parallelSourceLimit().set(it)
                        true
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = presentationStringResource(MR.strings.pref_invalidate_download_cache),
                    subtitle = presentationStringResource(MR.strings.pref_invalidate_download_cache_summary),
                    onClick = {
                        scope.launch {
                            downloadCache.invalidateCache()
                            context.toast(MR.strings.download_cache_invalidated)
                        }
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = presentationStringResource(MR.strings.pref_remove_all_downloads),
                    subtitle = presentationStringResource(MR.strings.pref_remove_all_downloads_summary),
                    onClick = {
                        // TODO: confirmation dialog
                        context.toast(MR.strings.all_downloads_removed)
                    },
                ),
            ),
        )
    }

    /**
     * Settings for automatic deletion of read chapters.
     */
    @Composable
    private fun getDeleteChaptersGroup(
        downloadPreferences: DownloadPreferences,
        categories: List<Category>,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = presentationStringResource(MR.strings.pref_category_delete_chapters),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = downloadPreferences.removeAfterMarkedAsRead(),
                    title = presentationStringResource(MR.strings.pref_remove_after_marked_as_read),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = downloadPreferences.removeAfterReadSlots(),
                    entries = persistentMapOf(
                        -1 to presentationStringResource(MR.strings.disabled),
                        0 to presentationStringResource(MR.strings.last_read_chapter),
                        1 to presentationStringResource(MR.strings.pref_second_to_last_read_chapter),
                        2 to presentationStringResource(MR.strings.pref_third_to_last_read_chapter),
                        3 to presentationStringResource(MR.strings.pref_fourth_to_last_read_chapter),
                        4 to presentationStringResource(MR.strings.pref_fifth_to_last_read_chapter),
                    ),
                    title = presentationStringResource(MR.strings.pref_remove_after_read),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = downloadPreferences.removeExcludeCategories(),
                    entries = categories.associate { it.id.toString() to it.name }.toImmutableMap(),
                    title = presentationStringResource(MR.strings.pref_remove_exclude_categories),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = downloadPreferences.removeBookmarkedChapters(),
                    title = presentationStringResource(MR.strings.pref_remove_bookmarked_chapters),
                ),
            ),
        )
    }

    /**
     * Settings for automatic downloading of new chapters.
     */
    @Composable
    private fun getDownloadNewChaptersGroup(
        downloadPreferences: DownloadPreferences,
        categories: List<Category>,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = presentationStringResource(MR.strings.pref_category_download_new_chapters),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = downloadPreferences.downloadNewChapters(),
                    title = presentationStringResource(MR.strings.pref_download_new_chapters),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = downloadPreferences.downloadNewChapterCategories(),
                    entries = categories.associate { it.id.toString() to it.name }.toImmutableMap(),
                    title = presentationStringResource(MR.strings.pref_download_new_categories),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = downloadPreferences.downloadNewChapterCategoriesExclude(),
                    entries = categories.associate { it.id.toString() to it.name }.toImmutableMap(),
                    title = presentationStringResource(MR.strings.pref_download_new_categories_exclude),
                ),
            ),
        )
    }

    /**
     * Settings for pre-downloading upcoming chapters.
     */
    @Composable
    private fun getDownloadAheadGroup(
        downloadPreferences: DownloadPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = presentationStringResource(MR.strings.download_ahead),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = downloadPreferences.autoDownloadWhileReading(),
                    entries = persistentMapOf(
                        0 to presentationStringResource(MR.strings.disabled),
                        2 to presentationStringResource(MR.strings.next_2_chapters),
                        3 to presentationStringResource(MR.strings.next_3_chapters),
                        5 to presentationStringResource(MR.strings.next_5_chapters),
                        10 to presentationStringResource(MR.strings.next_10_chapters),
                    ),
                    title = presentationStringResource(MR.strings.auto_download_while_reading),
                ),
                Preference.PreferenceItem.InfoPreference(presentationStringResource(MR.strings.download_ahead_info)),
            ),
        )
    }

    /**
     * Automatic removal settings for chapters deleted online.
     */
    @Composable
    private fun getAutomaticRemovalGroup(
        downloadPreferences: DownloadPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = presentationStringResource(MR.strings.delete_removed_chapters),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = downloadPreferences.deleteRemovedChapters(),
                    entries = persistentMapOf(
                        0 to presentationStringResource(MR.strings.ask_on_chapters_page),
                        1 to presentationStringResource(MR.strings.always_keep),
                        2 to presentationStringResource(MR.strings.always_delete),
                    ),
                    title = presentationStringResource(MR.strings.delete_removed_chapters),
                    subtitle = presentationStringResource(MR.strings.delete_downloaded_if_removed_online),
                ),
            ),
        )
    }
}
