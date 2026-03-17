package tachiyomi.domain.recents.service

import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

class RecentsPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun showRecentsDownloads() = preferenceStore.getEnum(
        "show_dls_in_recents",
        ShowRecentsDLs.All,
    )

    fun showRecentsRemHistory() = preferenceStore.getBoolean("show_rem_history_in_recents", true)

    fun showReadInAllRecents() = preferenceStore.getBoolean("show_read_in_all_recents", false)

    fun showTitleFirstInRecents() = preferenceStore.getBoolean("show_title_first_in_recents", false)

    fun uniformCovers() = preferenceStore.getBoolean("uniform_grid", true)

    fun outlineOnCovers() = preferenceStore.getBoolean("outline_on_covers", true)

    fun groupChaptersHistory() = preferenceStore.getEnum(
        "group_chapters_history_type",
        GroupType.ByWeek,
    )

    fun collapseGroupedHistory() = preferenceStore.getBoolean("collapse_group_history", true)

    fun showUpdatedTime() = preferenceStore.getBoolean("show_updated_time", false)

    fun sortFetchedTime() = preferenceStore.getBoolean("sort_fetched_time", false)

    fun collapseGroupedUpdates() = preferenceStore.getBoolean("group_chapters_updates", false)

    enum class ShowRecentsDLs {
        None,
        OnlyUnread,
        OnlyDownloaded,
        UnreadOrDownloaded,
        All,
    }

    enum class GroupType {
        BySeries,
        ByWeek,
        ByDay,
        Never,
        ;

        val isByTime: Boolean
            get() = this == ByWeek || this == ByDay
    }
}
