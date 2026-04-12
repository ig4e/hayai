package exh.recs

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.database.models.create
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.SourceManager
import exh.recs.sources.RecommendationPagingSource
import exh.recs.sources.StaticResultPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import yokai.domain.manga.interactor.GetManga

class BrowseRecommendsScreenModel(
    private val args: BrowseRecommendsScreen.Args,
    private val getManga: GetManga = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
) : StateScreenModel<BrowseRecommendsScreenModel.State>(State.Loading) {

    private suspend fun resolveRecommendationSource(): RecommendationPagingSource? {
        return when (args) {
            is BrowseRecommendsScreen.Args.MergedSourceMangas -> StaticResultPagingSource(args.results)
            is BrowseRecommendsScreen.Args.SingleSourceManga -> {
                val manga = getManga.awaitById(args.mangaId) ?: return null
                val source = sourceManager.get(args.sourceId) as? CatalogueSource ?: return null
                RecommendationPagingSource.createSources(manga, source).firstOrNull {
                    it::class.qualifiedName == args.recommendationSourceName
                }
            }
        }
    }

    init {
        loadResults()
    }

    private fun loadResults(page: Int = 1) {
        screenModelScope.launch(Dispatchers.IO) {
            try {
                val source = resolveRecommendationSource()
                if (source == null) {
                    mutableState.update { State.Error("Could not resolve recommendation source") }
                    return@launch
                }
                val result = source.requestNextPage(page)
                val mangas = result.mangas.map { sManga ->
                    val sourceId = source.associatedSourceId
                    val resolvedManga = if (sourceId != null) {
                        getManga.awaitByUrlAndSource(sManga.url, sourceId)
                    } else {
                        null
                    }
                    resolvedManga ?: Manga.create(sManga.url, sManga.title, sourceId ?: -1).apply {
                        artist = sManga.artist
                        author = sManga.author
                        description = sManga.description
                        genre = sManga.genre
                        status = sManga.status
                        thumbnail_url = sManga.thumbnail_url
                        update_strategy = sManga.update_strategy
                        initialized = sManga.initialized
                    }
                }

                mutableState.update {
                    State.Success(
                        mangas = mangas,
                        hasNextPage = result.hasNextPage,
                        currentPage = page,
                        sourceName = source.name,
                        sourceCategory = source.category,
                    )
                }
            } catch (e: Exception) {
                mutableState.update { State.Error(e.message ?: "Unknown error") }
            }
        }
    }

    fun loadNextPage() {
        val currentState = state.value as? State.Success ?: return
        if (!currentState.hasNextPage) return
        loadResults(currentState.currentPage + 1)
    }

    sealed class State {
        data object Loading : State()
        data class Success(
            val mangas: List<Manga>,
            val hasNextPage: Boolean,
            val currentPage: Int,
            val sourceName: String,
            val sourceCategory: dev.icerock.moko.resources.StringResource,
        ) : State()
        data class Error(val message: String) : State()
    }
}
