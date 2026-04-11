package exh.md.similar

import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.online.all.MangaDex
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
// TODO: Wire up with RecommendationPagingSource when available

/**
 * MangaDexSimilarPagingSource placeholder.
 * TODO: Extend RecommendationPagingSource when recommendations infrastructure is available
 */
class MangaDexSimilarPagingSource(
    private val mangaDex: MangaDex,
) {

    suspend fun requestNextPage(currentPage: Int): MangasPage {
        val mangasPage = coroutineScope {
            val similarPageDef = async { mangaDex.getMangaSimilar(null!!) } // TODO: Pass manga SManga
            val relatedPageDef = async { mangaDex.getMangaRelated(null!!) } // TODO: Pass manga SManga
            val similarPage = similarPageDef.await()
            val relatedPage = relatedPageDef.await()

            MangasPage(
                relatedPage.mangas + similarPage.mangas,
                false,
            )
        }

        return mangasPage.takeIf { it.mangas.isNotEmpty() }
            ?: throw Exception("No results")
    }
}
