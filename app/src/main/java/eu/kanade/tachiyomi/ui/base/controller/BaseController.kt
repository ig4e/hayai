package eu.kanade.tachiyomi.ui.base.controller

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import co.touchlab.kermit.Logger
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.main.chrome.ChromeAware
import eu.kanade.tachiyomi.util.view.BackHandlerControllerInterface
import eu.kanade.tachiyomi.util.view.activityBinding
import eu.kanade.tachiyomi.util.view.isControllerVisible
import eu.kanade.tachiyomi.util.view.removeQueryListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

abstract class BaseController(bundle: Bundle? = null) :
    Controller(bundle), BackHandlerControllerInterface, BaseControllerPreferenceControllerCommonInterface {

    abstract val shouldHideLegacyAppBar: Boolean

    lateinit var viewScope: CoroutineScope
    var isDragging = false

    init {
        addLifecycleListener(
            object : LifecycleListener() {
                override fun postCreateView(controller: Controller, view: View) {
                    onViewCreated(view)
                }

                override fun preCreateView(controller: Controller) {
                    viewScope = MainScope()
                    Logger.v { "Create view for ${controller.instance()}" }
                }

                override fun preAttach(controller: Controller, view: View) {
                    Logger.v { "Attach view for ${controller.instance()}" }
                }

                override fun preDetach(controller: Controller, view: View) {
                    Logger.v { "Detach view for ${controller.instance()}" }
                }

                override fun preDestroyView(controller: Controller, view: View) {
                    viewScope.cancel()
                    Logger.v { "Destroy view for ${controller.instance()}" }
                }
            },
        )
    }

    open fun onViewCreated(view: View) { }

    internal fun setAppBarVisibility() {
        if (shouldHideLegacyAppBar) hideLegacyAppBar() else showLegacyAppBar()
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        if (type.isEnter && !isControllerVisible) {
            view?.alpha = 0f
        } else {
            removeQueryListener()
        }
        setHasOptionsMenu(type.isEnter && isControllerVisible)
        // Hoisted chrome bind: every ChromeAware controller's PUSH_ENTER / POP_ENTER
        // rebinds the shared activity chrome from its declared spec. Leaf controllers
        // no longer need to call chromeBinder.bind manually — the only remaining
        // direct caller pattern is RootTabContent.onTabActivated, which fires outside
        // Conductor's lifecycle.
        if (type.isEnter && isControllerVisible) {
            val chromeAware = this as? ChromeAware
            if (chromeAware != null) {
                (activity as? MainActivity)?.chromeBinder?.bind(this, chromeAware.describeChrome())
            }
        }
        super.onChangeStarted(handler, type)
    }

    open fun canStillGoBack(): Boolean { return false }

    open val mainRecycler: RecyclerView?
        get() = null

    override fun onActivityPaused(activity: Activity) {
        super.onActivityPaused(activity)
        removeQueryListener(false)
    }

    private fun Controller.instance(): String {
        return "${javaClass.simpleName}@${Integer.toHexString(hashCode())}"
    }

    /**
     * Workaround for buggy menu item layout after expanding/collapsing an expandable item like a SearchView.
     * This method should be removed when fixed upstream.
     * Issue link: https://issuetracker.google.com/issues/37657375
     */
    var expandActionViewFromInteraction = false
    fun MenuItem.fixExpand(onExpand: ((MenuItem) -> Boolean)? = null, onCollapse: ((MenuItem) -> Boolean)? = null) {
        setOnActionExpandListener(
            object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    hideItemsIfExpanded(item, activityBinding?.searchToolbar?.menu, true)
                    return onExpand?.invoke(item) ?: true
                }

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    activity?.invalidateOptionsMenu()

                    return onCollapse?.invoke(item) ?: true
                }
            },
        )

        if (expandActionViewFromInteraction) {
            expandActionViewFromInteraction = false
            expandActionView()
        }
    }

    open fun onSearchActionViewLongClickQuery(): String? = null

    fun hideItemsIfExpanded(searchItem: MenuItem?, menu: Menu?, isExpanded: Boolean = false) {
        menu ?: return
        searchItem ?: return
        if (searchItem.isActionViewExpanded || isExpanded) {
            menu.forEach { it.isVisible = false }
        }
    }

    fun MenuItem.fixExpandInvalidate() {
        fixExpand { invalidateMenuOnExpand() }
    }

    /**
     * Workaround for menu items not disappearing when expanding an expandable item like a SearchView.
     * [expandActionViewFromInteraction] should be set to true in [onOptionsItemSelected] when the expandable item is selected
     * This method should be called as part of [MenuItem.OnActionExpandListener.onMenuItemActionExpand]
     */
    fun invalidateMenuOnExpand(): Boolean {
        return if (expandActionViewFromInteraction) {
            activity?.invalidateOptionsMenu()
            false
        } else {
            true
        }
    }

    fun hideLegacyAppBar() {
        (activity as? AppCompatActivity)?.findViewById<View>(R.id.app_bar)?.isVisible = false
    }

    fun showLegacyAppBar() {
        (activity as? AppCompatActivity)?.findViewById<View>(R.id.app_bar)?.isVisible = true
    }
}

interface BaseControllerPreferenceControllerCommonInterface {
    fun onActionViewExpand(item: MenuItem?) { }
    fun onActionViewCollapse(item: MenuItem?) { }
}
