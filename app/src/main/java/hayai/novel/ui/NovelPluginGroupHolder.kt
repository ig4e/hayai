package hayai.novel.ui

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.databinding.ExtensionCardHeaderBinding
import eu.kanade.tachiyomi.extension.model.InstalledExtensionsOrder
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.view.setText

/**
 * Holder for novel plugin group headers.
 * Reuses ExtensionCardHeaderBinding layout.
 */
class NovelPluginGroupHolder(
    view: View,
    adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
) : BaseFlexibleViewHolder(view, adapter) {

    private val binding = ExtensionCardHeaderBinding.bind(view)

    init {
        binding.extButton.setOnClickListener {
            (adapter as? NovelPluginAdapter)?.listener?.onNovelUpdateAllClicked(bindingAdapterPosition)
        }
        binding.extSort.setOnClickListener {
            (adapter as? NovelPluginAdapter)?.listener?.onNovelSortClicked(binding.extSort, bindingAdapterPosition)
        }
    }

    fun bind(item: NovelPluginGroupItem) {
        binding.title.text = "${item.name} (${item.size})"
        binding.extButton.isVisible = item.canUpdate != null
        binding.extButton.isEnabled = item.canUpdate == true
        binding.extSort.isVisible = item.installedSorting != null
        binding.extSort.setText(InstalledExtensionsOrder.fromValue(item.installedSorting ?: 0).nameRes)
    }
}
