package eu.kanade.tachiyomi.source.online.all

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import exh.source.MERGED_SOURCE_ID

/**
 * Virtual source that merges chapters from multiple real sources into a single manga.
 *
 * TODO: Full implementation will be provided by the EH sources agent.
 */
class MergedSource : Source {
    override val id: Long = MERGED_SOURCE_ID
    override val name: String = "Merged Source"

    override suspend fun getMangaDetails(manga: SManga): SManga {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override suspend fun getChapterList(manga: SManga): List<SChapter> {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        throw UnsupportedOperationException("Not yet implemented")
    }
}
