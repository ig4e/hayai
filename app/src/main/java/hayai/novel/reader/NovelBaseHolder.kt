package hayai.novel.reader

import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams
import androidx.recyclerview.widget.RecyclerView

/**
 * Base view holder for the novel reader's RecyclerView.
 * Follows the same pattern as WebtoonBaseHolder.
 */
abstract class NovelBaseHolder(
    view: View,
    protected val viewer: NovelViewer,
) : RecyclerView.ViewHolder(view) {

    val context: Context get() = itemView.context

    open fun recycle() {}

    protected fun View.wrapContent() {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }
}
