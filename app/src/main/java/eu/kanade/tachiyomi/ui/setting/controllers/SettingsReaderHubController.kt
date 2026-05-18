package eu.kanade.tachiyomi.ui.setting.controllers

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.preference.PreferenceScreen
import androidx.preference.R as preferenceR
import eu.kanade.tachiyomi.ui.setting.SettingsLegacyController
import yokai.i18n.MR
import yokai.util.lang.getString
import eu.kanade.tachiyomi.ui.setting.titleMRes as titleRes

/**
 * Reader settings hub. Hosts the Manga reader and Novel reader preference
 * screens behind real tabs — mirroring the activity-level `mainTabs` pattern
 * used by [eu.kanade.tachiyomi.ui.recents.RecentsController].
 *
 * Tab binding is fully declarative: [describeChrome] returns a `TabsSpec` and
 * [eu.kanade.tachiyomi.ui.main.chrome.ChromeBinder] applies it on every
 * activation. The bind itself is hoisted into
 * [eu.kanade.tachiyomi.ui.base.controller.BaseController.onChangeStarted], so this
 * controller does not call the binder directly.
 *
 * The actual preference DSL lives in [SettingsMangaReaderController] /
 * [SettingsNovelReaderController] (and their `populateManga…` /
 * `populateNovel…` file-level extension functions). This controller delegates
 * its preference screen to whichever sub-controller matches the selected tab.
 *
 * Every preference key is preserved; switching tabs only rebuilds the visible
 * preference screen — no migration, no recreate-the-controller dance.
 */
class SettingsReaderHubController : SettingsLegacyController() {

    private enum class ReaderTab { MANGA, NOVEL }

    private var selectedTab: ReaderTab = ReaderTab.MANGA

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = MR.strings.reader
        when (selectedTab) {
            ReaderTab.MANGA -> populateMangaReaderPreferences(this)
            ReaderTab.NOVEL -> populateNovelReaderPreferences(this)
        }
    }

    private fun rebuildPreferenceScreen() {
        if (!isAttached) return
        val newScreen = preferenceManager.createPreferenceScreen(getThemedContext())
        preferenceScreen = newScreen
        setupPreferenceScreen(newScreen)
        setTitle()
    }

    private fun getThemedContext(): Context {
        val tv = TypedValue()
        activity!!.theme.resolveAttribute(preferenceR.attr.preferenceTheme, tv, true)
        return ContextThemeWrapper(activity, tv.resourceId)
    }

    override fun describeChrome(): eu.kanade.tachiyomi.ui.main.chrome.ChromeSpec {
        val act = activity
        val labels = if (act != null) {
            listOf(act.getString(MR.strings.manga), act.getString(MR.strings.novel))
        } else {
            listOf("", "")
        }
        return eu.kanade.tachiyomi.ui.main.chrome.ChromeSpec(
            appBarVisible = true,
            scrollSource = if (view != null) listView else null,
            useSmallToolbar = true,
            tabs = eu.kanade.tachiyomi.ui.main.chrome.TabsSpec(
                items = labels.map { eu.kanade.tachiyomi.ui.main.chrome.TabItem.Label(it) },
                selectedIndex = selectedTab.ordinal,
                mode = eu.kanade.tachiyomi.ui.main.chrome.TabMode.Fixed,
                onSelected = { idx ->
                    val target = ReaderTab.entries[idx]
                    if (target != selectedTab) {
                        selectedTab = target
                        rebuildPreferenceScreen()
                    }
                },
                onReselected = { if (view != null) listView.scrollToPosition(0) },
            ),
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_SELECTED_TAB, selectedTab.ordinal)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val idx = savedInstanceState.getInt(STATE_SELECTED_TAB, ReaderTab.MANGA.ordinal)
        selectedTab = ReaderTab.entries.getOrElse(idx) { ReaderTab.MANGA }
    }

    companion object {
        private const val STATE_SELECTED_TAB = "SettingsReaderHubController.selectedTab"
    }
}
