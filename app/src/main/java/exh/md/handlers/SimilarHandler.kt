package exh.md.handlers

import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import exh.md.dto.RelationListDto
import exh.md.dto.SimilarMangaDto
import exh.md.service.MangaDexService
import exh.md.service.SimilarService
import exh.md.utils.MdUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SimilarHandler(
    private val lang: String,
    private val service: MangaDexService,
    private val similarService: SimilarService,
) {

    suspend fun getSimilar(manga: SManga): MangasPage {
        val similarDto = withContext(Dispatchers.IO) { similarService.getSimilarManga(MdUtil.getMangaId(manga.url)) }
        return similarDtoToMangaListPage(similarDto)
    }

    private suspend fun similarDtoToMangaListPage(
        similarMangaDto: SimilarMangaDto,
    ): MangasPage {
        val ids = similarMangaDto.matches.map {
            it.id
        }

        val mangaList = service.viewMangas(ids).data.map {
            MdUtil.createMangaEntry(it, lang)
        }

        return MangasPage(mangaList, false)
    }

    suspend fun getRelated(manga: SManga): MangasPage {
        val relatedListDto = withContext(Dispatchers.IO) { service.relatedManga(MdUtil.getMangaId(manga.url)) }
        return relatedDtoToMangaListPage(relatedListDto)
    }

    private suspend fun relatedDtoToMangaListPage(
        relatedListDto: RelationListDto,
    ): MangasPage {
        val ids = relatedListDto.data
            .mapNotNull { it.relationships.firstOrNull() }
            .map { it.id }

        val mangaList = service.viewMangas(ids).data.map {
            MdUtil.createMangaEntry(it, lang)
        }

        return MangasPage(mangaList, false)
    }
}
