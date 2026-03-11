package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.core.preference.asState
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.browse.ExtensionReposScreen
import eu.kanade.tachiyomi.ui.browse.migration.sources.MigrationSourcesScreen
import eu.kanade.tachiyomi.ui.category.sources.SourceCategoryScreen
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.authenticate
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import mihon.domain.extensionrepo.interactor.GetExtensionRepoCount
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource as presentationStringResource
import tachiyomi.presentation.core.util.collectAsState as collectPreferenceAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsBrowseScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = tachiyomi.i18n.MR.strings.browse

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val sourcePreferences = remember { Injekt.get<SourcePreferences>() }
        val sourceManager = remember { Injekt.get<SourceManager>() }
        val getExtensionRepoCount = remember { Injekt.get<GetExtensionRepoCount>() }

        val reposCount by getExtensionRepoCount.subscribe().collectAsState(0)

        // SY -->
        val scope = rememberCoroutineScope()
        val hideFeedTab by remember { Injekt.get<UiPreferences>().hideFeedTab().asState(scope) }
        val uiPreferences = remember { Injekt.get<UiPreferences>() }
        // SY <--

        return listOf(
            Preference.PreferenceGroup(
                title = presentationStringResource(tachiyomi.i18n.MR.strings.pref_category_general),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = sourcePreferences.hideInLibraryItems(),
                        title = presentationStringResource(tachiyomi.i18n.MR.strings.pref_hide_in_library_items),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = presentationStringResource(tachiyomi.i18n.MR.strings.label_extensions),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = presentationStringResource(tachiyomi.i18n.MR.strings.label_extension_repos),
                        subtitle = pluralStringResource(tachiyomi.i18n.MR.plurals.num_repos, reposCount, reposCount),
                        onClick = {
                            navigator.push(ExtensionReposScreen())
                        },
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = sourcePreferences.onlySearchPinned(),
                        title = presentationStringResource(tachiyomi.i18n.MR.strings.only_search_pinned_when),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = presentationStringResource(tachiyomi.i18n.MR.strings.label_migration),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = presentationStringResource(tachiyomi.i18n.MR.strings.source_migration),
                        onClick = { navigator.push(MigrationSourcesScreen) },
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = sourcePreferences.skipPreMigration(),
                        title = presentationStringResource(tachiyomi.i18n.MR.strings.skip_pre_migration),
                        subtitle = presentationStringResource(tachiyomi.i18n.MR.strings.use_last_saved_migration_preferences),
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = presentationStringResource(tachiyomi.i18n.MR.strings.match_pinned_sources),
                        subtitle = presentationStringResource(tachiyomi.i18n.MR.strings.only_enable_pinned_for_migration),
                        onClick = {
                            val pinnedSources = sourcePreferences.pinnedSources().get()
                                .mapNotNull { it.toLongOrNull() }
                            sourcePreferences.migrationSources().set(pinnedSources)
                            context.toast(tachiyomi.i18n.MR.strings.migration_sources_changed)
                        },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = presentationStringResource(tachiyomi.i18n.MR.strings.match_enabled_sources),
                        subtitle = presentationStringResource(tachiyomi.i18n.MR.strings.only_enable_enabled_for_migration),
                        onClick = {
                            val languages = sourcePreferences.enabledLanguages().get()
                            val hiddenCatalogues = sourcePreferences.disabledSources().get()
                            val enabledSources = sourceManager.getCatalogueSources()
                                .filter { it.lang in languages }
                                .filterNot { it.id.toString() in hiddenCatalogues }
                                .map { it.id }
                            sourcePreferences.migrationSources().set(enabledSources)
                            context.toast(tachiyomi.i18n.MR.strings.migration_sources_changed)
                        },
                    ),
                ),
            ),
            // SY -->
            Preference.PreferenceGroup(
                title = presentationStringResource(tachiyomi.i18n.MR.strings.label_sources),
                preferenceItems = persistentListOf(
                    kotlin.run {
                        val count by sourcePreferences.sourcesTabCategories().collectPreferenceAsState()
                        Preference.PreferenceItem.TextPreference(
                            title = presentationStringResource(tachiyomi.i18n.MR.strings.action_edit_categories),
                            subtitle = pluralStringResource(tachiyomi.i18n.MR.plurals.num_categories, count.size, count.size),
                            onClick = {
                                navigator.push(SourceCategoryScreen())
                            },
                        )
                    },
                    Preference.PreferenceItem.SwitchPreference(
                        preference = sourcePreferences.sourcesTabCategoriesFilter(),
                        title = presentationStringResource(SYMR.strings.pref_source_source_filtering),
                        subtitle = presentationStringResource(SYMR.strings.pref_source_source_filtering_summery),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = uiPreferences.useNewSourceNavigation(),
                        title = presentationStringResource(SYMR.strings.pref_source_navigation),
                        subtitle = presentationStringResource(SYMR.strings.pref_source_navigation_summery),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = sourcePreferences.allowLocalSourceHiddenFolders(),
                        title = presentationStringResource(SYMR.strings.pref_local_source_hidden_folders),
                        subtitle = presentationStringResource(SYMR.strings.pref_local_source_hidden_folders_summery),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = uiPreferences.enableSourceSwipeAction(),
                        title = presentationStringResource(tachiyomi.i18n.MR.strings.enable_source_swipe_action),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = presentationStringResource(SYMR.strings.feed),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = uiPreferences.hideFeedTab(),
                        title = presentationStringResource(SYMR.strings.pref_hide_feed),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = uiPreferences.feedTabInFront(),
                        title = presentationStringResource(SYMR.strings.pref_feed_position),
                        subtitle = presentationStringResource(SYMR.strings.pref_feed_position_summery),
                        enabled = hideFeedTab.not(),
                    ),
                ),
            ),
            // SY <--
            Preference.PreferenceGroup(
                title = presentationStringResource(tachiyomi.i18n.MR.strings.pref_category_nsfw_content),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = sourcePreferences.showNsfwSource(),
                        title = presentationStringResource(tachiyomi.i18n.MR.strings.pref_show_nsfw_source),
                        subtitle = presentationStringResource(tachiyomi.i18n.MR.strings.requires_app_restart),
                        onValueChanged = {
                            (context as FragmentActivity).authenticate(
                                title = context.stringResource(tachiyomi.i18n.MR.strings.pref_category_nsfw_content),
                            )
                        },
                    ),
                    Preference.PreferenceItem.InfoPreference(presentationStringResource(tachiyomi.i18n.MR.strings.parental_controls_info)),
                ),
            ),
        )
    }
}
