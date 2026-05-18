package eu.kanade.tachiyomi.ui.main.chrome

import androidx.core.view.ScrollingView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout

/**
 * Immutable description of the shared activity chrome state that a controller wants
 * applied while it's the visible root.
 *
 * Single-writer invariant
 * -----------------------
 * The activity AppBar, mainTabs strip, tab-strip frame, and Conductor options-menu
 * participation are shared resources owned by [eu.kanade.tachiyomi.ui.main.MainActivity].
 * No code outside [ChromeBinder] is permitted to mutate them — controllers describe
 * what they want as a [ChromeSpec] and the binder applies it from a clean baseline.
 * State cannot leak across controllers because every bind resets first.
 */
data class ChromeSpec(
    /**
     * Whether the activity AppBar is visible. False hides it (e.g. Browse's
     * extension sheet expanded — the sheet owns the top of the screen).
     */
    val appBarVisible: Boolean = true,

    /**
     * AppBar alpha. < 1f is used during Browse's extension-sheet drag for a fade
     * effect. Dynamic alpha during a drag is applied via
     * [ChromeBinder.updateAppBarVisibility] to avoid tearing down the tab strip
     * on every drag frame.
     */
    val appBarAlpha: Float = 1f,

    /**
     * When true, the AppBar's translationY is pinned at 0 — scroll-collapse is
     * disabled. Used when the recycler is empty (initial load) so the title
     * doesn't collapse to nothing.
     */
    val lockAppBarY: Boolean = false,

    /**
     * Forces the small/compact toolbar mode regardless of the AppBar's default.
     * Driven by [eu.kanade.tachiyomi.ui.base.SmallToolbarInterface] markers; the
     * binder resolves this from the controller type.
     */
    val useSmallToolbar: Boolean = false,

    /**
     * The recycler whose scroll position the AppBar should sync to on initial
     * bind. Ongoing scroll tracking is owned by `scrollViewWith`; the binder
     * just snaps the AppBar to the correct initial position on owner change.
     */
    val scrollSource: ScrollingView? = null,

    /**
     * The tab strip configuration. Null hides the strip; non-null populates and
     * shows it. The binder derives `appBar.useTabsInPreLayout` from whether
     * this is null.
     */
    val tabs: TabsSpec? = null,
) {
    companion object {
        /** Baseline chrome — what we reset to before applying a fresh spec. */
        val DEFAULT = ChromeSpec()
    }
}

/**
 * Description of the activity-level [TabLayout] strip ([R.id.main_tabs]) for the
 * current controller. Carries every visual variant the app currently uses.
 */
data class TabsSpec(
    val items: List<TabItem>,
    val selectedIndex: Int,
    val mode: TabMode = TabMode.Fixed,
    val onSelected: (Int) -> Unit,
    val onReselected: ((Int) -> Unit)? = null,
    /**
     * Optional pager binding. When provided, the binder owns a
     * [ViewPager.OnPageChangeListener] that drives tab selection from pager
     * swipes and forwards each settle to [TabsPagerSync.onPageSelected]. The
     * listener is detached on every subsequent bind.
     */
    val pagerSync: TabsPagerSync? = null,
)

/** A single tab entry in [TabsSpec.items]. */
sealed class TabItem {
    /** Plain text tab — used by Recents (History/Updates) and Browse (Manga/Novel). */
    data class Label(val text: String) : TabItem()

    /**
     * Text tab decorated with a circular count badge — used by Library category
     * pills. The binder inflates [eu.kanade.tachiyomi.R.layout.chrome_tab_with_count]
     * as the tab's custom view and binds [text] + [count] into it.
     */
    data class Badged(val text: String, val count: Int) : TabItem()
}

/** Binder-owned pager synchronisation for [TabsSpec]. */
data class TabsPagerSync(
    val pager: ViewPager,
    val onPageSelected: (Int) -> Unit,
)

/** Maps to the corresponding [TabLayout] mode flag. */
enum class TabMode(internal val tabLayoutMode: Int) {
    /** Fixed-width tabs that fill the available width — Recents, Browse Novel/Manga. */
    Fixed(TabLayout.MODE_FIXED),

    /** Variable-width tabs that scroll horizontally — Library categories. */
    Scrollable(TabLayout.MODE_SCROLLABLE),
}

/**
 * Implemented by controllers that participate in the [ChromeBinder] protocol.
 * The base controller calls [describeChrome] on every push/pop enter and rebinds
 * the chrome; root tab controllers also rebind explicitly on tab activation.
 */
interface ChromeAware {
    fun describeChrome(): ChromeSpec
}
