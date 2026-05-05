package eu.kanade.tachiyomi.ui.library

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.activityBinding
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import eu.kanade.tachiyomi.widget.ViewPagerAdapter

/**
 * Hosts one [AutofitRecyclerView] per category in tabbed library mode. Each page's recycler reserves
 * top padding for the pinned appbar+tabs strip, so only its own content scrolls.
 */
class LibraryPagerAdapter(
    private val controller: LibraryController,
) : ViewPagerAdapter() {

    var categories: List<Category> = emptyList()
        set(value) {
            val changed = field.map { it.id } != value.map { it.id }
            field = value
            if (changed) notifyDataSetChanged()
        }

    private val pageAdapters = mutableMapOf<Int, LibraryCategoryAdapter>()
    private val pageRecyclers = mutableMapOf<Int, AutofitRecyclerView>()

    override fun createView(container: ViewGroup, position: Int): View {
        val recycler = LayoutInflater.from(container.context)
            .inflate(R.layout.library_pager_page, container, false) as AutofitRecyclerView
        val outerController = this@LibraryPagerAdapter.controller
        val adapter = LibraryCategoryAdapter(outerController).apply {
            isPagedMode = true
            setDisplayHeadersAtStartUp(false)
            showOutline = outerController.uiPreferences.outlineOnCovers().get()
            showNumber = outerController.preferences.categoryNumberOfItems().get()
        }
        recycler.adapter = adapter
        recycler.tag = position
        pageAdapters[position] = adapter
        pageRecyclers[position] = recycler
        configureRecycler(recycler)
        bindCategoryItems(position, adapter)
        return recycler
    }

    override fun destroyView(container: ViewGroup, position: Int, view: View) {
        super.destroyView(container, position, view)
        pageAdapters.remove(position)
        pageRecyclers.remove(position)
    }

    override fun getCount(): Int = categories.size

    // POSITION_NONE forces ViewPager to recreate every page on notifyDataSetChanged(), which we
    // need because LibraryCategoryAdapter wraps an outer controller and isn't safe to reposition.
    override fun getItemPosition(`object`: Any): Int = POSITION_NONE

    private fun configureRecycler(recycler: AutofitRecyclerView) {
        val isList = controller.libraryLayoutValue == LibraryItem.LAYOUT_LIST
        val bottomNav = if (controller.isSubClass) null else controller.activityBinding?.bottomNav

        recycler.useStaggered(controller.preferences, controller.uiPreferences)
        if (isList) {
            recycler.spanCount = 1
            recycler.updatePaddingRelative(start = 0, end = 0)
        } else {
            recycler.setGridSize(controller.preferences)
            recycler.updatePaddingRelative(start = 5.dpToPx, end = 5.dpToPx)
        }
        recycler.updatePaddingRelative(
            top = controller.pageRecyclerTopPadding(),
            bottom = 50.dpToPx + (bottomNav?.height ?: 0),
        )
        recycler.clipToPadding = false
        recycler.setBackgroundColor(Color.TRANSPARENT)

        recycler.clearOnScrollListeners()
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                controller.onPageRecyclerScrolled(rv, dy)
            }
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    controller.onPageRecyclerScrollIdle(rv)
                }
            }
        })
        (recycler.manager as? GridLayoutManager)?.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                if (isList) return recycler.managerSpanCount
                val item = (recycler.adapter as? LibraryCategoryAdapter)?.getItem(position)
                return if (item is LibraryHeaderItem || item is LibraryPlaceholderItem) {
                    recycler.managerSpanCount
                } else {
                    1
                }
            }
        }
    }

    private fun bindCategoryItems(position: Int, adapter: LibraryCategoryAdapter) {
        val category = categories.getOrNull(position) ?: return
        adapter.setItems(controller.presenter.libraryToDisplay[category].orEmpty())
        adapter.hideAllHeaders()
        controller.applySelectionStateTo(adapter)
    }

    fun refreshAll() {
        pageAdapters.forEach { (idx, adapter) -> bindCategoryItems(idx, adapter) }
    }

    fun reattachAll() {
        pageRecyclers.values.forEach(::configureRecycler)
    }

    fun recyclerForPosition(position: Int): AutofitRecyclerView? = pageRecyclers[position]

    fun adapterForPosition(position: Int): LibraryCategoryAdapter? = pageAdapters[position]

    fun forEachPage(block: (LibraryCategoryAdapter, RecyclerView) -> Unit) {
        pageAdapters.forEach { (idx, adapter) ->
            val recycler = pageRecyclers[idx] ?: return@forEach
            block(adapter, recycler)
        }
    }
}
