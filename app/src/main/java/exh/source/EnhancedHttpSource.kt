package exh.source

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource

/**
 * Wraps an extension [HttpSource] with an EXH [DelegatedHttpSource] to provide
 * enhanced metadata handling (search_metadata, search_tags, search_titles).
 *
 * All catalogue/http operations delegate to the original source, while the
 * enhanced source hooks into lifecycle events for metadata management.
 */
class EnhancedHttpSource(
    val originalSource: HttpSource,
    val enhancedSource: DelegatedHttpSource,
) : CatalogueSource {

    override val id: Long get() = originalSource.id
    override val name: String get() = originalSource.name
    override val lang: String get() = originalSource.lang
    override val supportsLatest: Boolean get() = originalSource.supportsLatest

    override suspend fun getMangaDetails(manga: SManga): SManga {
        return originalSource.getMangaDetails(manga)
    }

    override suspend fun getChapterList(manga: SManga): List<SChapter> {
        return originalSource.getChapterList(manga)
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        return originalSource.getPageList(chapter)
    }

    override suspend fun getPopularManga(page: Int): MangasPage {
        return originalSource.getPopularManga(page)
    }

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage {
        return originalSource.getSearchManga(page, query, filters)
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage {
        return originalSource.getLatestUpdates(page)
    }

    override fun getFilterList(): FilterList = originalSource.getFilterList()

    override fun toString(): String = originalSource.toString()

    override fun hashCode(): Int = originalSource.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EnhancedHttpSource) return false
        return originalSource == other.originalSource
    }
}
