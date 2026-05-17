package hayai.novel.ui

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R

/**
 * Header item for grouping novel plugins (by language, installed/available).
 *
 * Inflates the same XML as ExtensionGroupItem via the `novel_plugin_card_header` alias so
 * FlexibleAdapter/RecyclerView see a distinct viewType — same invariant as
 * [NovelPluginItem]: distinct holder type must mean distinct viewType when pools are shared.
 */
data class NovelPluginGroupItem(
    val name: String,
    val size: Int,
    var canUpdate: Boolean? = null,
    var installedSorting: Int? = null,
) : AbstractHeaderItem<NovelPluginGroupHolder>() {

    override fun getLayoutRes(): Int = R.layout.novel_plugin_card_header

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
    ): NovelPluginGroupHolder {
        return NovelPluginGroupHolder(view, adapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: NovelPluginGroupHolder,
        position: Int,
        payloads: MutableList<Any?>?,
    ) {
        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is NovelPluginGroupItem) return name == other.name
        return false
    }

    override fun hashCode(): Int = name.hashCode()
}
