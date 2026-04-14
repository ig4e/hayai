package hayai.novel.ui

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.databinding.ExtensionCardHeaderBinding
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder

/**
 * Holder for novel plugin group headers.
 * Reuses ExtensionCardHeaderBinding layout.
 */
class NovelPluginGroupHolder(
    view: View,
    adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
) : BaseFlexibleViewHolder(view, adapter) {

    private val binding = ExtensionCardHeaderBinding.bind(view)

    fun bind(item: NovelPluginGroupItem) {
        binding.title.text = "${item.name} (${item.size})"
        binding.extButton.isVisible = false
        binding.extSort.isVisible = false
    }
}
