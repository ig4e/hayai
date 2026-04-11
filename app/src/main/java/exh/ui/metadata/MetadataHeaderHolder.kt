package exh.ui.metadata

import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.metadata.metadata.EightMusesSearchMetadata
import exh.metadata.metadata.HBrowseSearchMetadata
import exh.metadata.metadata.LanraragiSearchMetadata
import exh.metadata.metadata.MangaDexSearchMetadata
import exh.metadata.metadata.NHentaiSearchMetadata
import exh.metadata.metadata.PururinSearchMetadata
import exh.metadata.metadata.TsuminoSearchMetadata
import exh.ui.metadata.adapters.EHentaiDescription
import exh.ui.metadata.adapters.EightMusesDescription
import exh.ui.metadata.adapters.HBrowseDescription
import exh.ui.metadata.adapters.LanraragiDescription
import exh.ui.metadata.adapters.MangaDexDescription
import exh.ui.metadata.adapters.NHentaiDescription
import exh.ui.metadata.adapters.PururinDescription
import exh.ui.metadata.adapters.TsuminoDescription
import yokai.presentation.theme.YokaiTheme

class MetadataHeaderHolder(
    view: View,
    adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
) : BaseFlexibleViewHolder(view, adapter) {

    private val composeView: ComposeView = itemView.findViewById(R.id.compose_metadata)

    init {
        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
        )
    }

    fun bind(item: MetadataHeaderItem) {
        val meta = item.meta ?: return
        val mangaId = item.mangaId
        val sourceId = item.sourceId

        composeView.setContent {
            YokaiTheme {
                when (meta) {
                    is EHentaiSearchMetadata -> EHentaiDescription(
                        meta = meta,
                        openMetadataViewer = {
                            // TODO: Navigate to MetadataViewScreen
                        },
                        onSearch = { query ->
                            // TODO: Trigger search with query
                        },
                    )
                    is NHentaiSearchMetadata -> NHentaiDescription(
                        meta = meta,
                        openMetadataViewer = {
                            // TODO: Navigate to MetadataViewScreen
                        },
                    )
                    is MangaDexSearchMetadata -> MangaDexDescription(
                        meta = meta,
                        openMetadataViewer = {
                            // TODO: Navigate to MetadataViewScreen
                        },
                    )
                    is EightMusesSearchMetadata -> EightMusesDescription(
                        meta = meta,
                        openMetadataViewer = {
                            // TODO: Navigate to MetadataViewScreen
                        },
                    )
                    is HBrowseSearchMetadata -> HBrowseDescription(
                        meta = meta,
                        openMetadataViewer = {
                            // TODO: Navigate to MetadataViewScreen
                        },
                    )
                    is PururinSearchMetadata -> PururinDescription(
                        meta = meta,
                        openMetadataViewer = {
                            // TODO: Navigate to MetadataViewScreen
                        },
                    )
                    is TsuminoSearchMetadata -> TsuminoDescription(
                        meta = meta,
                        openMetadataViewer = {
                            // TODO: Navigate to MetadataViewScreen
                        },
                    )
                    is LanraragiSearchMetadata -> LanraragiDescription(
                        meta = meta,
                        openMetadataViewer = {
                            // TODO: Navigate to MetadataViewScreen
                        },
                    )
                    else -> {
                        // Unknown metadata type, show nothing
                    }
                }
            }
        }
    }
}
