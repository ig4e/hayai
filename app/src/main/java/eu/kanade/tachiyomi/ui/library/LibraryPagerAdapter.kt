package eu.kanade.tachiyomi.ui.library

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
        configureRecycler(recycler)
        val outer = this.controller
        val adapter = LibraryCategoryAdapter(outer).apply {
            isPagedMode = true
            setDisplayHeadersAtStartUp(false)
            showOutline = outer.uiPreferences.outlineOnCovers().get()
            showNumber = outer.preferences.categoryNumberOfItems().get()
        }
        recycler.adapter = adapter
        recycler.tag = position
        pageAdapters[position] = adapter
        pageRecyclers[position] = recycler
        bindCategoryItems(position, adapter)
        return recycler
    }

    override fun destroyView(container: ViewGroup, position: Int, view: View) {
        super.destroyView(container, position, view)
        pageAdapters.remove(position)
        pageRecyclers.remove(position)
    }

    override fun getCount(): Int = categories.size

    override fun getItemPosition(`object`: Any): Int = POSITION_NONE

    private fun configureRecycler(recycler: AutofitRecyclerView) {
        val bottomNav = if (controller.isSubClass) null else controller.activityBinding?.bottomNav
        recycler.useStaggered(controller.preferences, controller.uiPreferences)
        if (controller.libraryLayoutValue == LibraryItem.LAYOUT_LIST) {
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
        recycler.setBackgroundColor(android.graphics.Color.TRANSPARENT)
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
                if (controller.libraryLayoutValue == LibraryItem.LAYOUT_LIST) return recycler.managerSpanCount
                val adapter = recycler.adapter as? LibraryCategoryAdapter ?: return 1
                val item = adapter.getItem(position)
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
        val items = controller.presenter.libraryToDisplay[category].orEmpty()
        adapter.setItems(items)
        adapter.hideAllHeaders()
        controller.applySelectionStateTo(adapter)
    }

    fun refreshAll() {
        pageAdapters.forEach { (idx, adapter) -> bindCategoryItems(idx, adapter) }
    }

    fun reattachAll() {
        pageRecyclers.forEach { (_, recycler) -> configureRecycler(recycler) }
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
