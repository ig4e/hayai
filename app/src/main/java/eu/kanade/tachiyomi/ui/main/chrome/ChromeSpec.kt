package eu.kanade.tachiyomi.ui.main.chrome

import androidx.core.view.ScrollingView
import com.google.android.material.tabs.TabLayout

/**
 * Immutable description of the shared activity chrome state that a controller wants
 * applied while it's the visible root.
 *
 * Why this exists
 * ---------------
 * The activity AppBar, mainTabs strip, and Conductor options-menu participation are
 * shared resources owned by [eu.kanade.tachiyomi.ui.main.MainActivity]. Before this
 * abstraction, every root controller (Library, Recents, Browse) — plus their
 * dynamic features (Browse's extension sheet expand/collapse) — mutated those
 * resources directly. With persistent tabs ([eu.kanade.tachiyomi.ui.main.RootTabsController])
 * all three controllers are alive simultaneously, so any leak in one tab's state
 * bled into the next on swap (e.g., Browse's `appBar.isInvisible = true` carrying
 * over to Recents).
 *
 * The fix is declarative: controllers don't mutate the chrome; they describe what
 * they want it to look like as a [ChromeSpec], and a single
 * [eu.kanade.tachiyomi.ui.main.chrome.ChromeBinder] applies it from a clean
 * baseline. State can't leak because every bind resets first.
 */
data class ChromeSpec(
    /**
     * Whether the activity AppBar is visible. False hides it (e.g. Browse's
     * extension sheet expanded — the sheet itself owns the top of the screen).
     */
    val appBarVisible: Boolean = true,

    /**
     * AppBar alpha. < 1f is used during Browse's extension-sheet drag for a fade
     * effect. The binder applies this directly; controllers that want dynamic
     * alpha during a drag call back into the binder mid-drag.
     */
    val appBarAlpha: Float = 1f,

    /**
     * When true, the AppBar's translationY is pinned at 0 — scroll-collapse via
     * scrollViewWith is disabled. Used when the recycler is empty (initial load)
     * so the title doesn't collapse to nothing.
     */
    val lockAppBarY: Boolean = false,

    /**
     * Forces the small/compact toolbar mode regardless of the AppBar's default.
     * Driven by [eu.kanade.tachiyomi.ui.base.SmallToolbarInterface] markers on
     * specific controllers; the binder resolves this from the controller type.
     */
    val useSmallToolbar: Boolean = false,

    /**
     * Whether the AppBar should reserve room for the [TabLayout] strip in its
     * pre-layout height calculation. True for controllers that own the strip
     * (Recents, Browse, Library-tabbed); false for everything else.
     */
    val includeTabsInLayout: Boolean = false,

    /**
     * The recycler whose scroll position the AppBar should sync to on initial
     * bind (via `appBar.updateAppBarAfterY(scrollSource)`). Subsequent scroll
     * tracking is owned by `scrollViewWith`'s OnScrollListener — the binder just
     * snaps the AppBar to the right initial position when the controller
     * becomes visible.
     */
    val scrollSource: ScrollingView? = null,

    /**
     * The tabs strip configuration. Null hides the strip; non-null populates and
     * shows it.
     */
    val tabs: TabsSpec? = null,
) {
    companion object {
        /** Baseline chrome — what we reset to before applying a fresh spec. */
        val DEFAULT = ChromeSpec()
    }
}

/**
 * Description of the activity-level [TabLayout] strip ([R.id.main_tabs]) labels and
 * behavior for the current controller.
 */
data class TabsSpec(
    val labels: List<String>,
    val selectedIndex: Int,
    val mode: TabMode = TabMode.Fixed,
    val onSelected: (Int) -> Unit,
    val onReselected: ((Int) -> Unit)? = null,
)

/** Maps to the corresponding [TabLayout] mode flag. */
enum class TabMode(internal val tabLayoutMode: Int) {
    /** Fixed-width tabs that fill the available width — Recents, Browse Novel/Manga. */
    Fixed(TabLayout.MODE_FIXED),

    /** Variable-width tabs that scroll horizontally — Library categories. */
    Scrollable(TabLayout.MODE_SCROLLABLE),
}

/**
 * Implemented by controllers that participate in the [ChromeBinder] protocol. When a
 * controller becomes the visible root (tab swap, push, or dynamic state change), it
 * is asked for its [ChromeSpec] and the binder applies it.
 *
 * Currently scoped to root tabs (Library / Recents / Browse). Pushed controllers
 * (MangaDetails, Settings) continue to wire their chrome via the existing
 * `onChangeStarted` lifecycle path until Phase 2 of the refactor.
 */
interface ChromeAware {
    fun describeChrome(): ChromeSpec
}
