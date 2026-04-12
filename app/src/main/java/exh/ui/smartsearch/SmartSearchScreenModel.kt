package exh.ui.smartsearch

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.smartsearch.SmartSearchEngine
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

    private val smartSearchEngine = SmartSearchEngine(
        parentContext = screenModelScope.coroutineContext,
    )

    val source = sourceManager.get(sourceId) as CatalogueSource

    init {
        screenModelScope.launch(Dispatchers.IO) {
            val result = try {
                // Try deep search first (strips brackets, tries multiple query strategies)
                val resultManga = smartSearchEngine.smartSearch(source, origTitle)
                    ?: smartSearchEngine.normalSearch(source, origTitle)
                if (resultManga != null) {
                    val localManga = smartSearchEngine.networkToLocalManga(resultManga, source.id)
                    SearchResults.Found(localManga)
                } else {
                    SearchResults.NotFound
                }
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
