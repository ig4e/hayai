package eu.kanade.tachiyomi.ui.main.chrome

import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.Controller
import com.google.android.material.tabs.TabLayout
import eu.kanade.tachiyomi.databinding.MainActivityBinding
import eu.kanade.tachiyomi.ui.base.SmallToolbarInterface
import eu.kanade.tachiyomi.util.view.bindStringTabs

/**
 * Sole writer to the shared activity chrome (AppBar visual state, mainTabs strip,
 * Conductor options-menu participation).
 *
 * Lifecycle
 * ---------
 * Held by [eu.kanade.tachiyomi.ui.main.MainActivity] for the activity's lifetime.
 * Stateless aside from a single `lastOwner` weak reference used to flip
 * [Controller.setOptionsMenuHidden] correctly on owner transitions (the previous
 * owner needs to be hidden before the new owner is shown, else Conductor's menu
 * walk would briefly include both).
 *
 * Why the "reset then apply" pattern
 * ----------------------------------
 * Each [bind] call is independent: it first restores the chrome to a clean baseline
 * (full alpha, visible, unlocked, tab strip cleared, no scrollbar bound) and only
 * then applies the new [ChromeSpec]. This is the key invariant that eliminates the
 * leak-across-tabs bugs — no controller can leave residual state visible after it
 * stops being the owner.
 */
class ChromeBinder(private val binding: MainActivityBinding) {

    private var lastOwner: Controller? = null

    /**
     * Apply [spec] as the chrome state, with [owner] as the controller responsible
     * for its menu participation. Resets the chrome to defaults first so no state
     * from the previous owner can bleed through.
     */
    fun bind(owner: Controller, spec: ChromeSpec) {
        applyMenuOwnership(owner)
        resetToBaseline()
        applyAppBarVisibility(spec)
        applyToolbarMode(owner, spec)
        applyTabs(spec.tabs)
        applyScrollSource(spec)
        lastOwner = owner
    }

    /**
     * Update only the AppBar visibility (alpha + isInvisible) for the current
     * owner. Used by Browse during its extension-sheet drag, where alpha tweens
     * with the sheet's expansion progress — calling a full [bind] would tear
     * down and rebuild the tab strip on every drag frame.
     */
    fun updateAppBarVisibility(appBarAlpha: Float, appBarVisible: Boolean) {
        binding.appBar.alpha = appBarAlpha
        binding.appBar.isInvisible = !appBarVisible
    }

    /**
     * Drop ownership entirely (e.g. controller being destroyed) — restores the
     * chrome to defaults. Safe to call repeatedly.
     */
    fun release() {
        lastOwner?.setOptionsMenuHidden(true)
        lastOwner = null
        resetToBaseline()
    }

    private fun applyMenuOwnership(newOwner: Controller) {
        val prev = lastOwner
        if (prev !== null && prev !== newOwner) {
            prev.setOptionsMenuHidden(true)
        }
        // Always un-hide the new owner, even when it matches lastOwner. The previous
        // owner can have been hidden by an in-tab push (Library → MangaDetails calls
        // `setOptionsMenuHidden(true)` on Library) and then popped back to itself
        // without an owner swap; without this line the menu would stay hidden after
        // POP_ENTER's onTabActivated → bind(Library, ...) call.
        newOwner.setOptionsMenuHidden(false)
    }

    /**
     * Reset every chrome property this binder owns to its at-rest default. Called
     * before every spec apply — the new spec then layers its desired state on
     * top of this clean baseline.
     */
    private fun resetToBaseline() {
        binding.appBar.apply {
            alpha = 1f
            isInvisible = false
            lockYPos = false
        }
        // Clear the tab strip up-front so a new spec that lacks tabs leaves the
        // strip empty, and a new spec with tabs always starts from a clean list
        // (no stale labels from the previous owner mid-render).
        if (binding.tabsFrameLayout.isVisible) {
            binding.tabsFrameLayout.isVisible = false
            binding.tabsFrameLayout.alpha = 0f
        }
        binding.mainTabs.clearOnTabSelectedListeners()
        binding.mainTabs.removeAllTabs()
    }

    private fun applyAppBarVisibility(spec: ChromeSpec) {
        binding.appBar.alpha = spec.appBarAlpha
        binding.appBar.isInvisible = !spec.appBarVisible
        binding.appBar.lockYPos = spec.lockAppBarY
    }

    private fun applyToolbarMode(owner: Controller, spec: ChromeSpec) {
        val small = spec.useSmallToolbar || owner is SmallToolbarInterface
        binding.appBar.hideBigView(small)
        binding.appBar.setToolbarModeBy(owner)
        binding.appBar.useTabsInPreLayout = spec.includeTabsInLayout
    }

    private fun applyTabs(tabs: TabsSpec?) {
        if (tabs == null) {
            // resetToBaseline already cleared + hid the strip; nothing more to do.
            return
        }
        binding.mainTabs.tabMode = tabs.mode.tabLayoutMode
        binding.mainTabs.tabGravity = TabLayout.GRAVITY_FILL
        binding.mainTabs.bindStringTabs(
            labels = tabs.labels,
            selectedIndex = tabs.selectedIndex,
            onSelected = tabs.onSelected,
            onReselected = tabs.onReselected,
        )
        binding.tabsFrameLayout.alpha = 1f
        binding.tabsFrameLayout.isVisible = true
    }

    private fun applyScrollSource(spec: ChromeSpec) {
        val source = spec.scrollSource ?: return
        // Snap the AppBar to the top and resync to the current recycler scroll
        // position. Ongoing scroll tracking is owned by scrollViewWith's
        // OnScrollListener registered on the recycler — the binder just sets the
        // initial Y so scroll-collapse starts from a consistent state when the
        // controller becomes the visible owner.
        binding.appBar.y = 0f
        binding.appBar.updateAppBarAfterY(source)
    }
}
