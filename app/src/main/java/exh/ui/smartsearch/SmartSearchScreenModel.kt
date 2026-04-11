package exh.ui.smartsearch

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.SourceManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SmartSearchScreenModel(
    sourceId: Long,
    private val origTitle: String,
    sourceManager: SourceManager = Injekt.get(),
) : StateScreenModel<SmartSearchScreenModel.SearchResults?>(null) {

    val source = sourceManager.get(sourceId) as CatalogueSource

    init {
        screenModelScope.launch(Dispatchers.IO) {
            val result = try {
                // TODO: Implement smart search engine integration
                // val resultManga = smartSearchEngine.deepSearch(source, origTitle)
                SearchResults.NotFound
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                SearchResults.Error
            }

            mutableState.value = result
        }
    }

    sealed class SearchResults {
        data class Found(val manga: Manga) : SearchResults()
        data object NotFound : SearchResults()
        data object Error : SearchResults()
    }
}
