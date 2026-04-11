package exh.ui.metadata

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.source.SourceManager
import exh.metadata.metadata.RaisedSearchMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MetadataViewScreenModel(
    val mangaId: Long,
    val sourceId: Long,
    private val sourceManager: SourceManager = Injekt.get(),
) : StateScreenModel<MetadataViewState>(MetadataViewState.Loading) {

    init {
        screenModelScope.launch(Dispatchers.IO) {
            // TODO: Wire up metadata DB when available (getFlatMetadataForManga, raise, etc.)
            mutableState.value = MetadataViewState.MetadataNotFound
        }
    }
}

sealed class MetadataViewState {
    data object Loading : MetadataViewState()
    data class Success(val meta: RaisedSearchMetadata) : MetadataViewState()
    data object MetadataNotFound : MetadataViewState()
    data object SourceNotFound : MetadataViewState()
}
