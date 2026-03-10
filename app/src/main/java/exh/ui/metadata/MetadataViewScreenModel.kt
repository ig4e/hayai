package exh.ui.metadata

import android.content.Context
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.source.online.MetadataSource
import exh.metadata.metadata.RaisedSearchMetadata
import exh.source.getMainSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MetadataViewScreenModel(
    val mangaId: Long,
    val sourceId: Long,
    val context: Context = Injekt.get(),
    private val getFlatMetadataById: GetFlatMetadataById = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
) : StateScreenModel<MetadataViewState>(MetadataViewState.Loading) {
    private val _manga = MutableStateFlow<Manga?>(null)
    val manga = _manga.asStateFlow()

    init {
        screenModelScope.launchIO {
            _manga.value = getManga.await(mangaId)
        }

        screenModelScope.launchIO {
            val source = sourceManager.get(sourceId)
            val metadataSource = source?.getMainSource<MetadataSource<*, *>>()
            if (metadataSource == null) {
                mutableState.value = MetadataViewState.SourceNotFound
                return@launchIO
            }

            mutableState.value = when (val flatMetadata = getFlatMetadataById.await(mangaId)) {
                null -> MetadataViewState.MetadataNotFound
                else -> MetadataViewState.Success(
                    meta = flatMetadata.raise(metadataSource.metaClass),
                    sourceName = source.toString(),
                )
            }
        }
    }
}

sealed class MetadataViewState {
    data object Loading : MetadataViewState()
    data class Success(
        val meta: RaisedSearchMetadata,
        val sourceName: String,
    ) : MetadataViewState()
    data object MetadataNotFound : MetadataViewState()
    data object SourceNotFound : MetadataViewState()
}
