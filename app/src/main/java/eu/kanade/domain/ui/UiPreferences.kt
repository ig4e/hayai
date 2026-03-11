package eu.kanade.domain.ui

import android.os.Build
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.isDynamicColorAvailable
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import tachiyomi.i18n.MR
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class UiPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun themeMode() = preferenceStore.getEnum(
        "pref_theme_mode_key",
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ThemeMode.SYSTEM
        } else {
            ThemeMode.LIGHT
        },
    )

    fun appTheme() = preferenceStore.getEnum(
        "pref_app_theme",
        if (DeviceUtil.isDynamicColorAvailable) {
            AppTheme.MONET
        } else {
            AppTheme.DEFAULT
        },
    )

    fun themeDarkAmoled() = preferenceStore.getBoolean("pref_theme_dark_amoled_key", false)

    fun relativeTime() = preferenceStore.getBoolean("relative_time_v2", true)

    fun dateFormat() = preferenceStore.getString("app_date_format", "")

    fun tabletUiMode() = preferenceStore.getEnum("tablet_ui_mode", TabletUiMode.AUTOMATIC)

    fun imagesInDescription() = preferenceStore.getBoolean("pref_render_images_description", true)

    fun startingTab() = preferenceStore.getInt("starting_tab", 0)

    fun backToStart() = preferenceStore.getBoolean("back_to_start", true)

    fun showSeriesInShortcuts() = preferenceStore.getBoolean("show_series_shortcuts", true)

    fun showSourcesInShortcuts() = preferenceStore.getBoolean("show_sources_shortcuts", true)

    fun openChapterInShortcuts() = preferenceStore.getBoolean("open_chapter_shortcuts", true)

    fun shouldAutoUpdate() = preferenceStore.getInt("should_auto_update", 1) // Default to Wi-Fi only (1)

    fun appLanguage() = preferenceStore.getString("app_language", "")

    fun longTapRecentsNavBehaviour() = preferenceStore.getEnum("pref_recents_long_tap", LongTapRecents.DEFAULT)

    enum class LongTapRecents(val titleResId: StringResource) {
        DEFAULT(tachiyomi.i18n.MR.strings.recents_long_tap_default),
        LAST_READ(tachiyomi.i18n.MR.strings.recents_long_tap_last_read),
    }

    fun longTapBrowseNavBehaviour() = preferenceStore.getEnum("pref_browser_long_tap", LongTapBrowse.DEFAULT)

    enum class LongTapBrowse(val titleResId: StringResource) {
        DEFAULT(tachiyomi.i18n.MR.strings.browse_long_tap_default),
        SEARCH(tachiyomi.i18n.MR.strings.browse_long_tap_search),
    }

    // SY -->

    fun expandFilters() = preferenceStore.getBoolean("eh_expand_filters", false)

    fun hideFeedTab() = preferenceStore.getBoolean("hide_latest_tab", false)

    fun feedTabInFront() = preferenceStore.getBoolean("latest_tab_position", false)

    fun recommendsInOverflow() = preferenceStore.getBoolean("recommends_in_overflow", false)

    fun mergeInOverflow() = preferenceStore.getBoolean("merge_in_overflow", true)

    fun previewsRowCount() = preferenceStore.getInt("pref_previews_row_count", 4)

    fun useNewSourceNavigation() = preferenceStore.getBoolean("use_new_source_navigation", true)

    fun bottomBarLabels() = preferenceStore.getBoolean("pref_show_bottom_bar_labels", true)

    fun showNavUpdates() = preferenceStore.getBoolean("pref_show_updates_button", true)

    fun showNavHistory() = preferenceStore.getBoolean("pref_show_history_button", true)

    fun expandedAppBars() = preferenceStore.getBoolean("pref_expanded_app_bars", true)

    fun floatingSearchBars() = preferenceStore.getBoolean("pref_floating_search_bars", true)

    fun coverThemedMangaDetails() = preferenceStore.getBoolean("pref_cover_themed_manga_details", true)

    fun hideBottomNavOnScroll() = preferenceStore.getBoolean("hide_bottom_nav_on_scroll", true)

    fun enableSourceSwipeAction() = preferenceStore.getBoolean("enable_source_swipe_action", true)

    fun dynamicShortcuts() = preferenceStore.getBoolean("pref_dynamic_shortcuts", true)

    // SY <--

    companion object {
        fun dateFormat(format: String): DateTimeFormatter = when (format) {
            "" -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            else -> DateTimeFormatter.ofPattern(format, Locale.getDefault())
        }
    }
}
