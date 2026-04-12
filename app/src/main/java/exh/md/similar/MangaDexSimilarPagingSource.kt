package exh.md.similar

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.all.MangaDex
import exh.recs.sources.NoResultsException
import exh.recs.sources.RecommendationPagingSource
import exh.source.getMainSource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import eu.kanade.tachiyomi.domain.manga.models.Manga
import yokai.i18n.MR

/**
 * MangaDexSimilarPagingSource inherited from the general recommendation source.
 */
class MangaDexSimilarPagingSource(
    manga: Manga?,
    private val mangaDex: MangaDex,
) : RecommendationPagingSource(manga) {

    override val name: String
        get() = "MangaDex"

    override val category: StringResource
        get() = MR.strings.similar_titles

    override val associatedSourceId: Long
        get() = mangaDex.getMainSource<MangaDex>()?.id ?: mangaDex.id

    override suspend fun requestNextPage(currentPage: Int): MangasPage {
        val sourceManga = manga ?: throw NoResultsException()
        val sManga = SManga.create().also {
            it.url = sourceManga.url
            it.title = sourceManga.title
        }

        val mangasPage = coroutineScope {
            try {
                val similarPageDef = async { mangaDex.getMangaSimilar(sManga) }
                val relatedPageDef = async { mangaDex.getMangaRelated(sManga) }
                val similarPage = similarPageDef.await()
                val relatedPage = relatedPageDef.await()

                MangasPage(
                    relatedPage.mangas + similarPage.mangas,
                    false,
                )
            } catch (e: HttpException) {
                when (e.code) {
                    404 -> throw NoResultsException()
                    else -> throw e
                }
            }
        }

        return mangasPage.takeIf { it.mangas.isNotEmpty() } ?: throw NoResultsException()
    }
}
