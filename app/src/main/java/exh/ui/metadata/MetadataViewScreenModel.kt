package exh.ui.metadata

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.source.online.MetadataSource
import exh.metadata.metadata.RaisedSearchMetadata
import exh.source.getMainSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.source.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MetadataViewScreenModel(
    val mangaId: Long,
    val sourceId: Long,
    private val sourceManager: SourceManager = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
) : StateScreenModel<MetadataViewState>(MetadataViewState.Loading) {
    private val _manga = MutableStateFlow<Manga?>(null)
    val manga = _manga.asStateFlow()

    init {
        screenModelScope.launch(Dispatchers.IO) {
            _manga.value = db.getManga(mangaId).executeAsBlocking()
        }

        screenModelScope.launch(Dispatchers.IO) {
            val source = sourceManager.get(sourceId)
            val metadataSource = source?.getMainSource<MetadataSource<*, *>>()
            if (metadataSource == null) {
                mutableState.value = MetadataViewState.SourceNotFound
                return@launch
            }

            val flatMetadata = db.getFlatMetadataForManga(mangaId).executeAsBlocking()
            mutableState.value = when (flatMetadata) {
                null -> MetadataViewState.MetadataNotFound
                else -> MetadataViewState.Success(flatMetadata.raise(metadataSource.metaClass))
            }
        }
    }
}

sealed class MetadataViewState {
    data object Loading : MetadataViewState()
    data class Success(val meta: RaisedSearchMetadata) : MetadataViewState()
    data object MetadataNotFound : MetadataViewState()
    data object SourceNotFound : MetadataViewState()
}
