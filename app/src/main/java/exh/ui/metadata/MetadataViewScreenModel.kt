package exh.ui.metadata

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.source.SourceManager
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.base.FlatMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import yokai.domain.manga.interactor.GetManga
import yokai.domain.manga.metadata.MangaMetadataRepository

class MetadataViewScreenModel(
    val mangaId: Long,
    val sourceId: Long,
    private val sourceManager: SourceManager = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
) : StateScreenModel<MetadataViewState>(MetadataViewState.Loading), KoinComponent {

    private val metadataRepository: MangaMetadataRepository by inject()

    private val _mangaTitle = MutableStateFlow<String?>(null)
    val mangaTitle = _mangaTitle.asStateFlow()

    init {
        screenModelScope.launch(Dispatchers.IO) {
            _mangaTitle.value = getManga.awaitById(mangaId)?.title
        }

        screenModelScope.launch(Dispatchers.IO) {
            try {
                val searchMetadata = metadataRepository.getMetadataById(mangaId)
                if (searchMetadata == null) {
                    mutableState.value = MetadataViewState.MetadataNotFound
                    return@launch
                }
                val tags = metadataRepository.getTagsById(mangaId)
                val titles = metadataRepository.getTitlesById(mangaId)
                val flatMetadata = FlatMetadata(
                    metadata = searchMetadata,
                    tags = tags,
                    titles = titles,
                )
                val raised = flatMetadata.raise<RaisedSearchMetadata>()
                mutableState.value = MetadataViewState.Success(raised)
            } catch (e: Exception) {
                mutableState.value = MetadataViewState.MetadataNotFound
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
