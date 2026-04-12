package exh.recs

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.database.models.create
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.SourceManager
import exh.recs.batch.RankedSearchResults
import exh.recs.sources.RecommendationPagingSource
import exh.recs.sources.StaticResultPagingSource
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import yokai.domain.manga.interactor.GetManga

open class RecommendsScreenModel(
    private val args: RecommendsScreen.Args,
    private val sourceManager: SourceManager = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
) : StateScreenModel<RecommendsScreenModel.State>(State()) {

    private val coroutineDispatcher = Dispatchers.IO.limitedParallelism(5)

    private val sortComparator = { map: Map<RecommendationPagingSource, RecommendationItemResult> ->
        compareBy<RecommendationPagingSource>(
            { (map[it] as? RecommendationItemResult.Success)?.isEmpty ?: true },
            { it.name },
            { it.category.resourceId },
        )
    }

    init {
        screenModelScope.launch(Dispatchers.IO) {
            val recommendationSources = when (args) {
                is RecommendsScreen.Args.SingleSourceManga -> {
                    val manga = getManga.awaitById(args.mangaId) ?: return@launch
                    mutableState.update { it.copy(title = manga.title) }

                    val source = sourceManager.get(args.sourceId) as? CatalogueSource ?: return@launch
                    RecommendationPagingSource.createSources(manga, source)
                }
                is RecommendsScreen.Args.MergedSourceMangas -> {
                    args.mergedResults.map(::StaticResultPagingSource)
                }
            }

            updateItems(
                recommendationSources
                    .associateWith { RecommendationItemResult.Loading as RecommendationItemResult }
                    .toPersistentMap(),
            )

            recommendationSources.map { recSource ->
                async {
                    if (state.value.items[recSource] !is RecommendationItemResult.Loading) {
                        return@async
                    }

                    try {
                        val page = withContext(coroutineDispatcher) {
                            recSource.requestNextPage(1)
                        }

                        val titles = page.mangas.map { sManga ->
                            val recSourceId = recSource.associatedSourceId
                            if (recSourceId != null) {
                                val localManga = getManga.awaitByUrlAndSource(sManga.url, recSourceId)
                                localManga ?: Manga.create(sManga.url, sManga.title, recSourceId).apply {
                                    artist = sManga.artist
                                    author = sManga.author
                                    description = sManga.description
                                    genre = sManga.genre
                                    status = sManga.status
                                    thumbnail_url = sManga.thumbnail_url
                                    update_strategy = sManga.update_strategy
                                    initialized = sManga.initialized
                                }
                            } else {
                                Manga.create(sManga.url, sManga.title, -1).apply {
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
                        }

                        if (isActive) {
                            updateItem(recSource, RecommendationItemResult.Success(titles))
                        }
                    } catch (e: Exception) {
                        if (isActive) {
                            updateItem(recSource, RecommendationItemResult.Error(e))
                        }
                    }
                }
            }.awaitAll()
        }
    }

    private fun updateItems(items: PersistentMap<RecommendationPagingSource, RecommendationItemResult>) {
        mutableState.update {
            it.copy(
                items = items
                    .toSortedMap(sortComparator(items))
                    .toPersistentMap(),
            )
        }
    }

    private fun updateItem(source: RecommendationPagingSource, result: RecommendationItemResult) {
        synchronized(state.value.items) {
            val newItems = state.value.items.mutate {
                it[source] = result
            }
            updateItems(newItems)
        }
    }

    @Immutable
    data class State(
        val title: String? = null,
        val items: PersistentMap<RecommendationPagingSource, RecommendationItemResult> = persistentMapOf(),
    ) {
        val progress: Int = items.count { it.value !is RecommendationItemResult.Loading }
        val total: Int = items.size
        val filteredItems = items.filter { (_, result) -> result.isVisible(false) }
            .toImmutableMap()
    }
}

sealed interface RecommendationItemResult {
    data object Loading : RecommendationItemResult

    data class Error(
        val throwable: Throwable,
    ) : RecommendationItemResult

    data class Success(
        val result: List<Manga>,
    ) : RecommendationItemResult {
        val isEmpty: Boolean
            get() = result.isEmpty()
    }

    fun isVisible(onlyShowHasResults: Boolean): Boolean {
        return !onlyShowHasResults || (this is Success && !this.isEmpty)
    }
}
