package yokai.domain.recents

import eu.kanade.tachiyomi.core.preference.PreferenceStore
import eu.kanade.tachiyomi.core.preference.getEnum
import eu.kanade.tachiyomi.data.preference.PreferenceKeys
import eu.kanade.tachiyomi.ui.recents.RecentMangaAdapter

class RecentsPreferences(private val preferenceStore: PreferenceStore) {
    fun showRecentsDownloads() =
        preferenceStore.getEnum(PreferenceKeys.showDLsInRecents, RecentMangaAdapter.ShowRecentsDLs.All)

    fun showRecentsRemHistory() = preferenceStore.getBoolean(PreferenceKeys.showRemHistoryInRecents, true)

    fun showReadInAllRecents() = preferenceStore.getBoolean(PreferenceKeys.showReadInAllRecents, false)

    fun showTitleFirstInRecents() =
        preferenceStore.getBoolean(PreferenceKeys.showTitleFirstInRecents, false)

    fun groupChaptersUpdates() =
        preferenceStore.getEnum(PreferenceKeys.groupChaptersUpdates, UpdatesGroupType.ByDay)

    fun hiddenSourcesInHistory() =
        preferenceStore.getStringSet(PreferenceKeys.hiddenSourcesInHistory, emptySet())

    fun hiddenSourcesInUpdates() =
        preferenceStore.getStringSet(PreferenceKeys.hiddenSourcesInUpdates, emptySet())

    fun showHiddenInHistory() =
        preferenceStore.getBoolean(PreferenceKeys.showHiddenInHistory, false)

    fun showHiddenInUpdates() =
        preferenceStore.getBoolean(PreferenceKeys.showHiddenInUpdates, false)
}

enum class UpdatesGroupType { ByDay, BySource }
