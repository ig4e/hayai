package eu.kanade.tachiyomi.ui.setting.controllers

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import androidx.preference.PreferenceScreen
import androidx.preference.R as preferenceR
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import eu.kanade.tachiyomi.ui.base.TabItem
import eu.kanade.tachiyomi.ui.base.TabMode
import eu.kanade.tachiyomi.ui.setting.SettingsLegacyController
import eu.kanade.tachiyomi.util.view.activityBinding
import yokai.i18n.MR
import yokai.util.lang.getString
import eu.kanade.tachiyomi.ui.setting.titleMRes as titleRes

/**
 * Reader settings hub. Delegates its preference screen to either
 * [SettingsMangaReaderController] or [SettingsNovelReaderController] based on
 * [selectedTab]. Preference keys are preserved across switches — no migration.
 *
 * The Manga/Novel tab strip is rendered on the activity-global appBar (this
 * controller doesn't host its own appBar, so we drive [activityBinding.appBar]
 * directly). Tabs are applied on entry and cleared on exit so they don't leak.
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

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        when (type) {
            ControllerChangeType.PUSH_ENTER, ControllerChangeType.POP_ENTER -> applyTabs()
            ControllerChangeType.PUSH_EXIT, ControllerChangeType.POP_EXIT -> activityBinding?.appBar?.clearTabs()
            else -> Unit
        }
    }

    override fun onDestroyView(view: View) {
        activityBinding?.appBar?.clearTabs()
        super.onDestroyView(view)
    }

    private fun applyTabs() {
        val act = activity ?: return
        val labels = listOf(act.getString(MR.strings.manga), act.getString(MR.strings.novel))
        activityBinding?.appBar?.applyTabs(
            items = labels.map { TabItem.Label(it) },
            selectedIndex = selectedTab.ordinal,
            mode = TabMode.Fixed,
            onSelected = { idx ->
                val target = ReaderTab.entries[idx]
                if (target != selectedTab) {
                    selectedTab = target
                    rebuildPreferenceScreen()
                }
            },
            onReselected = { if (view != null) listView.scrollToPosition(0) },
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
