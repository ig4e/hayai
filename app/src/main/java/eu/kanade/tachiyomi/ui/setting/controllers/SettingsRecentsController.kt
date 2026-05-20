package eu.kanade.tachiyomi.ui.setting.controllers

import yokai.util.koin.get
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.recents.RecentMangaAdapter
import eu.kanade.tachiyomi.ui.recents.RecentsPresenter
import eu.kanade.tachiyomi.ui.recents.RecentsViewType
import eu.kanade.tachiyomi.ui.setting.SettingsLegacyController
import eu.kanade.tachiyomi.ui.setting.bindTo
import eu.kanade.tachiyomi.ui.setting.defaultValue
import eu.kanade.tachiyomi.ui.setting.intListPreference
import eu.kanade.tachiyomi.ui.setting.listPreference
import eu.kanade.tachiyomi.ui.setting.onClick
import eu.kanade.tachiyomi.ui.setting.preference
import eu.kanade.tachiyomi.ui.setting.preferenceCategory
import eu.kanade.tachiyomi.ui.setting.switchPreference
import eu.kanade.tachiyomi.ui.setting.titleMRes as titleRes
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.setMessage
import eu.kanade.tachiyomi.util.view.setNegativeButton
import eu.kanade.tachiyomi.util.view.setPositiveButton
import eu.kanade.tachiyomi.util.view.setTitle
import yokai.util.koin.injectLazy
import yokai.domain.recents.RecentsPreferences
import yokai.domain.recents.UpdatesGroupType
import yokai.domain.recents.interactor.UnhideRecents
import yokai.domain.recents.models.RecentsHidden
import yokai.domain.ui.UiPreferences
import yokai.i18n.MR
import yokai.util.lang.getString
import android.R as AR

/**
 * Recents settings hub introduced in the recents-redesign omnibus
 * (Wave 2B). Mirrors the existing per-section settings controllers
 * (see [SettingsGeneralController]) — pure preference DSL, Conductor
 * controller backed by [SettingsLegacyController].
 *
 * Sections: Display (default tab + global recents toggles), History
 * (grouping, collapse, hidden management), Updates (grouping, sort,
 * hidden management). The bottom-sheet (TabbedRecentsOptionsSheet)
 * exposes the same prefs in a transient surface; this screen is the
 * persistent settings home for users who want to tune everything
 * without opening Recents first.
 */
class SettingsRecentsController : SettingsLegacyController() {

    private val uiPreferences: UiPreferences by injectLazy()
    private val recentsPreferences: RecentsPreferences by injectLazy()
    private val unhideRecents: UnhideRecents by injectLazy()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = MR.strings.recents

        preferenceCategory {
            titleRes = MR.strings.display_options

            intListPreference(activity) {
                bindTo(uiPreferences.recentsViewType())
                titleRes = MR.strings.recents_default_tab
                entriesRes = arrayOf(
                    MR.strings.grouped,
                    MR.strings.history,
                    MR.strings.updates,
                )
                entryValues = RecentsViewType.entries.map { it.mainValue }
                defaultValue = RecentsViewType.GroupedAll.mainValue
            }

            switchPreference {
                bindTo(recentsPreferences.showTitleFirstInRecents())
                titleRes = MR.strings.show_title_first
            }

            listPreference(activity) {
                bindTo(recentsPreferences.showRecentsDownloads())
                titleRes = MR.strings.show_download_button
                val entries = RecentMangaAdapter.ShowRecentsDLs.entries
                entriesRes = arrayOf(
                    MR.strings.none,
                    MR.strings.only_unread,
                    MR.strings.only_downloaded,
                    MR.strings.unread_or_downloaded,
                    MR.strings.always,
                )
                entryValues = entries.map { it.name }
            }

            switchPreference {
                bindTo(recentsPreferences.showRecentsRemHistory())
                titleRes = MR.strings.show_reset_history_button
            }

            switchPreference {
                bindTo(recentsPreferences.showReadInAllRecents())
                titleRes = MR.strings.show_read_chapters_all
            }
        }

        preferenceCategory {
            titleRes = MR.strings.history

            listPreference(activity) {
                bindTo(preferences.groupChaptersHistory())
                titleRes = MR.strings.group_chapters_together
                val entries = RecentsPresenter.GroupType.entries
                entriesRes = arrayOf(
                    MR.strings.by_series,
                    MR.strings.by_week,
                    MR.strings.by_day,
                    MR.strings.recents_group_by_source,
                    MR.strings.never,
                )
                entryValues = entries.map { it.name }
            }

            switchPreference {
                bindTo(preferences.collapseGroupedHistory())
                titleRes = MR.strings.collapse_grouped_chapters
            }

            switchPreference {
                bindTo(recentsPreferences.showHiddenInHistory())
                titleRes = MR.strings.recents_show_hidden
            }

            preference {
                key = "recents_hidden_sources_history"
                titleRes = MR.strings.recents_hidden_sources
                isPersistent = false
                summary = hiddenSourcesSummary(
                    recentsPreferences.hiddenSourcesInHistory().get(),
                )

                onClick {
                    showHiddenSourcesDialog(forHistory = true) { newSummary ->
                        summary = newSummary
                    }
                }
            }

            preference {
                key = "recents_clear_all_hidden_history"
                titleRes = MR.strings.recents_clear_all_hidden
                isPersistent = false
                onClick { confirmClearHidden(RecentsHidden.TAB_HISTORY) }
            }
        }

        preferenceCategory {
            titleRes = MR.strings.updates

            listPreference(activity) {
                bindTo(recentsPreferences.groupChaptersUpdates())
                titleRes = MR.strings.group_chapters_together
                val entries = UpdatesGroupType.entries
                entriesRes = arrayOf(
                    MR.strings.by_day,
                    MR.strings.recents_group_by_source,
                )
                entryValues = entries.map { it.name }
            }

            switchPreference {
                bindTo(preferences.showUpdatedTime())
                titleRes = MR.strings.show_updated_time
            }

            switchPreference {
                bindTo(preferences.sortFetchedTime())
                titleRes = MR.strings.sort_fetched_time
            }

            switchPreference {
                bindTo(recentsPreferences.showHiddenInUpdates())
                titleRes = MR.strings.recents_show_hidden
            }

            preference {
                key = "recents_hidden_sources_updates"
                titleRes = MR.strings.recents_hidden_sources
                isPersistent = false
                summary = hiddenSourcesSummary(
                    recentsPreferences.hiddenSourcesInUpdates().get(),
                )

                onClick {
                    showHiddenSourcesDialog(forHistory = false) { newSummary ->
                        summary = newSummary
                    }
                }
            }

            preference {
                key = "recents_clear_all_hidden_updates"
                titleRes = MR.strings.recents_clear_all_hidden
                isPersistent = false
                onClick { confirmClearHidden(RecentsHidden.TAB_UPDATES) }
            }
        }
    }

    private fun hiddenSourcesSummary(hidden: Set<String>): String {
        val ctx = activity ?: return ""
        return if (hidden.isEmpty()) {
            ctx.getString(MR.strings.recents_no_hidden_sources)
        } else {
            "${hidden.size}"
        }
    }

    private fun showHiddenSourcesDialog(forHistory: Boolean, onUpdated: (String) -> Unit) {
        val activity = activity ?: return
        val sourceManager = get<SourceManager>()
        val sources = sourceManager.getCatalogueSources()
            .map { it.id.toString() to it.name }
            .sortedBy { it.second.lowercase() }
        val pref = if (forHistory) {
            recentsPreferences.hiddenSourcesInHistory()
        } else {
            recentsPreferences.hiddenSourcesInUpdates()
        }
        if (sources.isEmpty()) {
            activity.materialAlertDialog()
                .setTitle(MR.strings.recents_hidden_sources)
                .setMessage(MR.strings.recents_no_hidden_sources)
                .setPositiveButton(AR.string.ok, null)
                .show()
            return
        }
        val currentlyHidden = pref.get().toMutableSet()
        val checked = BooleanArray(sources.size) { sources[it].first in currentlyHidden }
        val labels = sources.map { it.second }.toTypedArray()
        activity.materialAlertDialog()
            .setTitle(MR.strings.recents_hidden_sources)
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                val id = sources[which].first
                if (isChecked) currentlyHidden.add(id) else currentlyHidden.remove(id)
            }
            .setPositiveButton(AR.string.ok) { _, _ ->
                pref.set(currentlyHidden)
                onUpdated(hiddenSourcesSummary(currentlyHidden))
            }
            .setNegativeButton(AR.string.cancel, null)
            .show()
    }

    private fun confirmClearHidden(tab: Int) {
        val activity = activity ?: return
        activity.materialAlertDialog()
            .setTitle(MR.strings.recents_clear_all_hidden)
            .setMessage(MR.strings.recents_clear_all_hidden_confirm)
            .setPositiveButton(MR.strings.clear) { _, _ ->
                viewScope.launchIO {
                    unhideRecents.awaitAll(tab)
                }
                activity.toast(MR.strings.recents_cleared_hidden)
            }
            .setNegativeButton(AR.string.cancel, null)
            .show()
    }
}
