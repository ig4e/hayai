package eu.kanade.tachiyomi.ui.setting.controllers

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import androidx.preference.PreferenceScreen
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import androidx.preference.R as preferenceR
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.MainActivityTabsOwner
import eu.kanade.tachiyomi.ui.base.SmallToolbarInterface
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.setting.SettingsLegacyController
import eu.kanade.tachiyomi.util.view.activityBinding
import eu.kanade.tachiyomi.util.view.bindStringTabs
import eu.kanade.tachiyomi.util.view.isControllerVisible
import yokai.i18n.MR
import yokai.util.lang.getString
import eu.kanade.tachiyomi.ui.setting.titleMRes as titleRes

/**
 * Reader settings hub. Hosts the Manga reader and Novel reader preference
 * screens behind real tabs — mirroring the activity-level `mainTabs` pattern
 * used by [eu.kanade.tachiyomi.ui.recents.RecentsController].
 *
 * The actual preference DSL lives in [SettingsMangaReaderController] /
 * [SettingsNovelReaderController] (and their `populateManga…` /
 * `populateNovel…` file-level extension functions). This controller delegates
 * its preference screen to whichever sub-controller matches the selected tab,
 * and rebinds the activity's tab strip on each entry / tab switch.
 *
 * Every preference key is preserved; switching tabs only rebuilds the visible
 * preference screen — no migration, no recreate-the-controller dance.
 */
class SettingsReaderHubController : SettingsLegacyController(), MainActivityTabsOwner {

    override val ownsActivityTabs: Boolean = true

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

    private fun bindTabs() {
        val activity = activity ?: return
        val tabs = activityBinding?.mainTabs ?: return
        val labels = listOf(
            activity.getString(MR.strings.manga),
            activity.getString(MR.strings.novel),
        )
        tabs.bindStringTabs(
            labels = labels,
            selectedIndex = selectedTab.ordinal,
            onSelected = { idx ->
                val target = ReaderTab.entries[idx]
                if (target != selectedTab) {
                    selectedTab = target
                    rebuildPreferenceScreen()
                }
            },
            onReselected = { listView.scrollToPosition(0) },
        )
        (activity as? MainActivity)?.showTabBar(true)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        // Re-bind in case the activity tab strip was torn down while another
        // controller was on top (returning here via back navigation).
        if (isAttached) bindTabs()
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isEnter) {
            if (isControllerVisible) {
                bindTabs()
            }
        } else {
            val lastController = router.backstack.lastOrNull()?.controller
            val nextOwnsTabs = (lastController as? MainActivityTabsOwner)?.ownsActivityTabs == true
            if (lastController !is DialogController && !nextOwnsTabs) {
                (activity as? MainActivity)?.showTabBar(
                    show = false,
                    animate = lastController !is SmallToolbarInterface,
                )
            }
        }
    }

    override fun describeChrome(): eu.kanade.tachiyomi.ui.main.chrome.ChromeSpec {
        val act = activity
        val labels = if (act != null) listOf(
            act.getString(MR.strings.manga),
            act.getString(MR.strings.novel),
        ) else listOf("", "")
        return eu.kanade.tachiyomi.ui.main.chrome.ChromeSpec(
            appBarVisible = true,
            includeTabsInLayout = true,
            scrollSource = if (view != null) listView else null,
            useSmallToolbar = true,
            tabs = eu.kanade.tachiyomi.ui.main.chrome.TabsSpec(
                labels = labels,
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
