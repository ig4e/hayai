package hayai.novel.reader

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.hasMissingChapters

/**
 * RecyclerView adapter for the novel reader.
 * Follows the exact same pattern as WebtoonAdapter: manages a list of
 * ReaderPage and ChapterTransition items with DiffUtil updates.
 */
class NovelAdapter(val viewer: NovelViewer) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items: List<Any> = emptyList()
        private set

    var currentChapter: ReaderChapter? = null

    fun setChapters(chapters: ViewerChapters, forceTransition: Boolean) {
        val newItems = mutableListOf<Any>()

        val prevHasMissingChapters = hasMissingChapters(chapters.currChapter, chapters.prevChapter)
        val nextHasMissingChapters = hasMissingChapters(chapters.nextChapter, chapters.currChapter)

        // Add previous chapter pages and transition
        if (chapters.prevChapter != null) {
            val prevPages = chapters.prevChapter.pages
            if (prevPages != null) {
                newItems.addAll(prevPages.takeLast(2))
            }
        }

        if (prevHasMissingChapters || forceTransition || chapters.prevChapter?.state !is ReaderChapter.State.Loaded) {
            newItems.add(ChapterTransition.Prev(chapters.currChapter, chapters.prevChapter))
        }

        // Add current chapter pages
        val currPages = chapters.currChapter.pages
        if (currPages != null) {
            newItems.addAll(currPages)
        }

        currentChapter = chapters.currChapter

        // Add next chapter transition and pages
        if (nextHasMissingChapters || forceTransition || chapters.nextChapter?.state !is ReaderChapter.State.Loaded) {
            newItems.add(ChapterTransition.Next(chapters.currChapter, chapters.nextChapter))
        }

        if (chapters.nextChapter != null) {
            val nextPages = chapters.nextChapter.pages
            if (nextPages != null) {
                newItems.addAll(nextPages.take(2))
            }
        }

        val result = DiffUtil.calculateDiff(Callback(items, newItems))
        items = newItems
        result.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is ReaderPage -> PAGE_VIEW
            is ChapterTransition -> TRANSITION_VIEW
            else -> error("Unknown view type for ${item.javaClass}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            PAGE_VIEW -> {
                val frame = FrameLayout(parent.context)
                NovelPageHolder(frame, viewer)
            }
            TRANSITION_VIEW -> {
                val layout = LinearLayout(parent.context)
                NovelTransitionHolder(layout, viewer)
            }
            else -> error("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is NovelPageHolder -> holder.bind(item as ReaderPage)
            is NovelTransitionHolder -> holder.bind(item as ChapterTransition)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        when (holder) {
            is NovelPageHolder -> holder.recycle()
            is NovelTransitionHolder -> holder.recycle()
        }
    }

    private class Callback(
        private val oldItems: List<Any>,
        private val newItems: List<Any>,
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return true
        }

        override fun getOldListSize(): Int = oldItems.size
        override fun getNewListSize(): Int = newItems.size
    }

    private companion object {
        const val PAGE_VIEW = 0
        const val TRANSITION_VIEW = 1
    }
}
