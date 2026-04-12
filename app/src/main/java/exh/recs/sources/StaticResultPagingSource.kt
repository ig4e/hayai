package exh.recs.sources

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import exh.recs.batch.RankedSearchResults

class StaticResultPagingSource(
    val data: RankedSearchResults,
) : RecommendationPagingSource(manga = null) {

    override val name: String get() = data.recSourceName
    override val category: StringResource get() = StringResource(data.recSourceCategoryResId)
    override val associatedSourceId: Long? get() = data.recAssociatedSourceId

    override suspend fun requestNextPage(currentPage: Int): MangasPage =
        // Use virtual paging to improve performance for large lists
        data.results
            .entries
            .chunked(PAGE_SIZE)
            .getOrElse(currentPage - 1) { emptyList() }
            .let { chunk ->
                MangasPage(
                    mangas = chunk.map { it.key },
                    hasNextPage = data.results.size > currentPage * PAGE_SIZE,
                )
            }

    companion object {
        const val PAGE_SIZE = 25
    }
}
