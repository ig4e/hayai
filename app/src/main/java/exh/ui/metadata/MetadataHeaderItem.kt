package exh.ui.metadata

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import exh.metadata.metadata.RaisedSearchMetadata

class MetadataHeaderItem(
    val meta: RaisedSearchMetadata?,
    val sourceId: Long,
    val mangaId: Long,
) : AbstractFlexibleItem<MetadataHeaderHolder>() {

    override fun getLayoutRes(): Int = R.layout.metadata_header_item

    override fun isSelectable(): Boolean = false

    override fun isSwipeable(): Boolean = false

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
    ): MetadataHeaderHolder {
        return MetadataHeaderHolder(view, adapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: MetadataHeaderHolder,
        position: Int,
        payloads: MutableList<Any?>?,
    ) {
        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MetadataHeaderItem) return false
        return mangaId == other.mangaId && sourceId == other.sourceId
    }

    override fun hashCode(): Int {
        var result = mangaId.hashCode()
        result = 31 * result + sourceId.hashCode()
        return result
    }
}
