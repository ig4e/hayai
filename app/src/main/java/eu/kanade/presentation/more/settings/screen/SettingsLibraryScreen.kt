package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.category.genre.SortTagScreen
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource as presentationStringResource
import tachiyomi.presentation.core.util.collectAsState as collectPreferenceAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Settings screen for library-related preferences.
 * Manages category behavior, global update settings, and library display options.
 */
object SettingsLibraryScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_library

    @Composable
    override fun getPreferences(): List<Preference> {
        val getCategories = remember { Injekt.get<GetCategories>() }
        val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }
        val allCategories by getCategories.subscribe().collectAsState(initial = emptyList())

        return listOf(
            getGeneralGroup(libraryPreferences),
            getCategoriesGroup(LocalNavigator.currentOrThrow, allCategories, libraryPreferences),
            getGlobalUpdateGroup(allCategories, libraryPreferences),
            getBehaviorGroup(libraryPreferences),
            // SY -->
            getSortingCategory(LocalNavigator.currentOrThrow, libraryPreferences),
            // SY <--
        )
    }

    /**
     * General library settings including article ignoring and search suggestions.
     */
    @Composable
    private fun getGeneralGroup(libraryPreferences: LibraryPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = presentationStringResource(MR.strings.pref_category_general),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.removeArticles(),
                    title = presentationStringResource(MR.strings.pref_ignore_articles),
                    subtitle = presentationStringResource(MR.strings.pref_ignore_articles_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.showLibrarySearchSuggestions(),
                    title = presentationStringResource(MR.strings.pref_search_suggestions),
                    subtitle = presentationStringResource(MR.strings.pref_search_suggestions_summary),
                ),
            ),
        )
    }

    /**
     * Category management and default selection settings.
     */
    @Composable
    private fun getCategoriesGroup(
        navigator: Navigator,
        allCategories: List<Category>,
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val defaultCategory by libraryPreferences.defaultCategory().collectPreferenceAsState()
        val defaultCategoryName = allCategories
            .find { it.id == defaultCategory.toLong() }
            ?.name
            ?: presentationStringResource(MR.strings.label_default)

        return Preference.PreferenceGroup(
            title = presentationStringResource(MR.strings.categories),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = presentationStringResource(MR.strings.action_edit_categories),
                    subtitle = pluralStringResource(MR.plurals.num_categories, allCategories.size, allCategories.size),
                    onClick = { navigator.push(CategoryScreen()) },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.defaultCategory(),
                    entries = (
                        mapOf(-1 to presentationStringResource(MR.strings.default_category_summary)) +
                            allCategories.associate { it.id.toInt() to it.name }
                        ).toImmutableMap(),
                    title = presentationStringResource(MR.strings.default_category),
                    subtitle = defaultCategoryName,
                ),
            ),
        )
    }

    /**
     * Settings for global library updates, including frequency and restrictions.
     */
    @Composable
    private fun getGlobalUpdateGroup(
        allCategories: List<Category>,
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val context = Injekt.get<android.app.Application>()

        val libraryUpdateIntervalPref = libraryPreferences.autoUpdateInterval()
        val libraryUpdateInterval by libraryUpdateIntervalPref.collectPreferenceAsState()

        return Preference.PreferenceGroup(
            title = presentationStringResource(MR.strings.pref_category_library_update),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = libraryUpdateIntervalPref,
                    entries = persistentMapOf(
                        0 to presentationStringResource(MR.strings.update_never),
                        1 to presentationStringResource(MR.strings.update_1hour),
                        2 to presentationStringResource(MR.strings.update_2hour),
                        3 to presentationStringResource(MR.strings.update_3hour),
                        6 to presentationStringResource(MR.strings.update_6hour),
                        12 to presentationStringResource(MR.strings.update_12hour),
                        24 to presentationStringResource(MR.strings.update_24hour),
                        48 to presentationStringResource(MR.strings.update_48hour),
                    ),
                    title = presentationStringResource(MR.strings.pref_library_update_interval),
                    onValueChanged = {
                        LibraryUpdateJob.setupTask(context, it)
                        true
                    },
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = libraryPreferences.autoUpdateDeviceRestrictions(),
                    entries = mapOf(
                        LibraryPreferences.DEVICE_ONLY_ON_WIFI to presentationStringResource(MR.strings.connected_to_wifi),
                        LibraryPreferences.DEVICE_CHARGING to presentationStringResource(MR.strings.charging),
                        "battery_not_low" to presentationStringResource(MR.strings.battery_not_low),
                    ).toImmutableMap(),
                    title = presentationStringResource(MR.strings.pref_library_update_restriction),
                    subtitle = presentationStringResource(MR.strings.pref_library_update_restriction_summary),
                    enabled = libraryUpdateInterval > 0,
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = libraryPreferences.autoUpdateMangaRestrictions(),
                    entries = mapOf(
                        LibraryPreferences.MANGA_HAS_UNREAD to presentationStringResource(MR.strings.pref_update_only_completely_read),
                        LibraryPreferences.MANGA_OUTSIDE_RELEASE_PERIOD to presentationStringResource(MR.strings.pref_update_only_in_release_period),
                    ).toImmutableMap(),
                    title = presentationStringResource(MR.strings.pref_library_update_manga_restriction),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = libraryPreferences.updateCategories(),
                    entries = allCategories.associate { it.id.toString() to it.name }.toImmutableMap(),
                    title = presentationStringResource(MR.strings.pref_library_update_categories),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = libraryPreferences.updateCategoriesExclude(),
                    entries = allCategories.associate { it.id.toString() to it.name }.toImmutableMap(),
                    title = presentationStringResource(MR.strings.pref_library_update_categories_exclude),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.autoUpdateCovers(),
                    title = presentationStringResource(MR.strings.pref_library_update_refresh_covers),
                ),
            ),
        )
    }

    /**
     * Settings for library user interaction and behavior.
     */
    @Composable
    private fun getBehaviorGroup(libraryPreferences: LibraryPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = presentationStringResource(MR.strings.pref_category_library_behavior),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.isChapterSwipeEnabled(),
                    title = presentationStringResource(MR.strings.pref_chapter_swipe),
                ),
            ),
        )
    }

    /**
     * Experimental fork-specific settings for grouping and sorting.
     */
    @Composable
    private fun getSortingCategory(
        navigator: Navigator,
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val tagCount by libraryPreferences.sortTagsForLibrary().collectPreferenceAsState()
        return Preference.PreferenceGroup(
            presentationStringResource(SYMR.strings.pref_category_fork),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = presentationStringResource(SYMR.strings.pref_tag_sorting),
                    subtitle = pluralStringResource(SYMR.plurals.pref_tag_sorting_desc, tagCount.size, tagCount.size),
                    onClick = {
                        navigator.push(SortTagScreen())
                    },
                ),
            ),
        )
    }
}
