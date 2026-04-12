package exh.ui.metadata

import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.ui.manga.MangaDetailsAdapter
import eu.kanade.tachiyomi.util.view.withFadeTransaction
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

    private fun openMetadataViewer(mangaId: Long, sourceId: Long) {
        val mangaDetailsAdapter = mAdapter as? MangaDetailsAdapter ?: return
        mangaDetailsAdapter.controller.router.pushController(
            MetadataViewController(mangaId, sourceId).withFadeTransaction(),
        )
    }

    private fun triggerSearch(query: String) {
        val mangaDetailsAdapter = mAdapter as? MangaDetailsAdapter ?: return
        mangaDetailsAdapter.controller.globalSearch(query)
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
                            openMetadataViewer(mangaId, sourceId)
                        },
                        onSearch = { query ->
                            triggerSearch(query)
                        },
                    )
                    is NHentaiSearchMetadata -> NHentaiDescription(
                        meta = meta,
                        openMetadataViewer = {
                            openMetadataViewer(mangaId, sourceId)
                        },
                    )
                    is MangaDexSearchMetadata -> MangaDexDescription(
                        meta = meta,
                        openMetadataViewer = {
                            openMetadataViewer(mangaId, sourceId)
                        },
                    )
                    is EightMusesSearchMetadata -> EightMusesDescription(
                        meta = meta,
                        openMetadataViewer = {
                            openMetadataViewer(mangaId, sourceId)
                        },
                    )
                    is HBrowseSearchMetadata -> HBrowseDescription(
                        meta = meta,
                        openMetadataViewer = {
                            openMetadataViewer(mangaId, sourceId)
                        },
                    )
                    is PururinSearchMetadata -> PururinDescription(
                        meta = meta,
                        openMetadataViewer = {
                            openMetadataViewer(mangaId, sourceId)
                        },
                    )
                    is TsuminoSearchMetadata -> TsuminoDescription(
                        meta = meta,
                        openMetadataViewer = {
                            openMetadataViewer(mangaId, sourceId)
                        },
                    )
                    is LanraragiSearchMetadata -> LanraragiDescription(
                        meta = meta,
                        openMetadataViewer = {
                            openMetadataViewer(mangaId, sourceId)
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
