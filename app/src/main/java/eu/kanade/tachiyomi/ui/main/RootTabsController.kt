package eu.kanade.tachiyomi.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeType
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.SimpleSwapChangeHandler
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.library.LibraryController
import eu.kanade.tachiyomi.ui.library.LibraryComposeController
import eu.kanade.tachiyomi.ui.recents.RecentsController
import eu.kanade.tachiyomi.ui.source.BrowseController
import uy.kohesive.injekt.injectLazy
import yokai.domain.base.BasePreferences

/**
 * Single persistent root controller that hosts Library / Recents / Browse simultaneously
 * in sibling FrameLayouts. Tab swaps just toggle container visibility — the child
 * controllers are never destroyed by switching tabs, so subsequent visits skip the
 * inflate + layout work entirely. Inflation is deferred per tab: the child router for a
 * given tab is only initialized the first time the user actually selects it.
 *
 * Per-tab back-stack: pushing MangaDetails (or any controller) from inside a tab pushes
 * onto that tab's child router. Switching tabs and returning preserves the pushed stack.
 *
 * This replaces the legacy `router.setRoot(LibraryController/RecentsController/...)` pattern
 * where every nav swap destroyed and reinflated the entire root controller view tree.
 */
class RootTabsController : Controller() {

    private val basePreferences: BasePreferences by injectLazy()

    private val containers = mutableMapOf<Int, FrameLayout>()

    // -1 sentinel until selectTab() is first called. Prevents premature visibility
    // assignment in onCreateView from showing an empty Library container before the host
    // activity has decided which startingTab to use.
    var currentTabId: Int = -1
        private set

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?,
    ): View {
        val ctx = activity!!
        val root = FrameLayout(ctx)
        for (tabId in TAB_IDS) {
            val tabContainer = FrameLayout(ctx).apply {
                id = tabContainerId(tabId)
                // All hidden until selectTab() picks one. Avoids initial flash of empty
                // Library container before the host activity selects the user's startingTab.
                isVisible = false
            }
            root.addView(
                tabContainer,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
            containers[tabId] = tabContainer
        }
        // Don't ensureChildRoot here — wait for selectTab. The host activity calls
        // nav.selectedItemId = startingTab() right after the controller is attached, which
        // triggers selectTab() and materialises the correct tab on demand. This skips
        // inflating Library when the user's startingTab is Recents or Browse.
        return root
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentTabId = savedInstanceState.getInt(KEY_CURRENT_TAB, currentTabId)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_TAB, currentTabId)
    }

    /**
     * Make [tabId] the visible tab. Lazy-creates the tab's child router and root controller
     * on first selection. Idempotent — no-op when already on [tabId].
     */
    fun selectTab(tabId: Int) {
        if (!containers.containsKey(tabId)) return
        val prev = currentTabId
        if (tabId == prev && containers[tabId]?.isVisible == true) {
            // Already visible — ensure the child router has a root in case the activity
            // recreated us empty (defensive).
            ensureChildRoot(tabId)
            return
        }
        // A "tab swap" is when the user actually moved from one realized tab to another.
        // First-time tab selection (prev == -1) and restore-into-saved-tab (prev == tabId
        // but isVisible == false) shouldn't synthesise EXIT/ENTER — Conductor's natural
        // ENTER (fired by ensureChildRoot's setRoot on the child router) handles first
        // creation, and restore preserves the controller's already-entered state.
        val isTabSwap = (prev != -1 && prev != tabId)
        val handler = SimpleSwapChangeHandler(false)

        if (isTabSwap) {
            val outgoing = childRouterFor(prev)?.backstack?.lastOrNull()?.controller
            outgoing?.runCatching {
                onChangeStarted(handler, ControllerChangeType.PUSH_EXIT)
                onChangeEnded(handler, ControllerChangeType.PUSH_EXIT)
            }
        }

        if (prev != -1) containers[prev]?.isVisible = false
        val target = containers[tabId] ?: return
        target.isVisible = true
        currentTabId = tabId
        ensureChildRoot(tabId)

        if (isTabSwap) {
            val incoming = childRouterFor(tabId)?.backstack?.lastOrNull()?.controller
            incoming?.runCatching {
                onChangeStarted(handler, ControllerChangeType.PUSH_ENTER)
                onChangeEnded(handler, ControllerChangeType.PUSH_ENTER)
            }
        }
        (activity as? MainActivity)?.onActiveTabChanged(prev, tabId)
    }

    private fun childRouterFor(tabId: Int): Router? {
        val container = containers[tabId] ?: return null
        return getChildRouter(container, childRouterTag(tabId))
    }

    /**
     * The child router for the currently visible tab. Returns null only if the view tree
     * is not yet created.
     */
    fun activeChildRouter(): Router? {
        val container = containers[currentTabId] ?: return null
        return getChildRouter(container, childRouterTag(currentTabId))
    }

    private fun ensureChildRoot(tabId: Int) {
        val container = containers[tabId] ?: return
        val childRouter = getChildRouter(container, childRouterTag(tabId))
        if (!childRouter.hasRootController()) {
            val controller = controllerForTab(tabId)
            childRouter.setRoot(
                RouterTransaction.with(controller)
                    .pushChangeHandler(SimpleSwapChangeHandler(false))
                    .popChangeHandler(SimpleSwapChangeHandler(false))
                    .tag(tabId.toString()),
            )
        }
    }

    private fun controllerForTab(id: Int): Controller = when (id) {
        R.id.nav_library ->
            if (basePreferences.composeLibrary().get()) LibraryComposeController() else LibraryController()
        R.id.nav_recents -> RecentsController()
        else -> BrowseController()
    }

    override fun handleBack(): Boolean = activeChildRouter()?.handleBack() == true

    private fun childRouterTag(tabId: Int): String = "root_tabs_child_$tabId"

    private fun tabContainerId(navId: Int): Int = when (navId) {
        R.id.nav_library -> CONTAINER_ID_LIBRARY
        R.id.nav_recents -> CONTAINER_ID_RECENTS
        else -> CONTAINER_ID_BROWSE
    }

    companion object {
        private const val KEY_CURRENT_TAB = "RootTabsController.currentTabId"
        // Stable container ids so Conductor's child router restores into the same FrameLayout
        // across config changes. Picked from the 0x7E_xx_xxxx custom-id range to avoid R clash.
        private const val CONTAINER_ID_LIBRARY = 0x7E001001
        private const val CONTAINER_ID_RECENTS = 0x7E001002
        private const val CONTAINER_ID_BROWSE = 0x7E001003
        private val TAB_IDS = listOf(R.id.nav_library, R.id.nav_recents, R.id.nav_browse)
    }
}
