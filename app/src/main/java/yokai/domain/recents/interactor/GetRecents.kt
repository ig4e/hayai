package yokai.domain.recents.interactor

import eu.kanade.tachiyomi.data.database.models.MangaChapter
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import yokai.domain.chapter.ChapterRepository
import yokai.domain.history.HistoryRepository
import yokai.domain.recents.models.RecentsHidden
import yokai.util.limitAndOffset

class GetRecents(
    private val chapterRepository: ChapterRepository,
    private val historyRepository: HistoryRepository,
) {
    suspend fun awaitUpdates(
        filterScanlators: Boolean,
        isResuming: Boolean,
        showHidden: Boolean = false,
        search: String = "",
        offset: Long = 0L,
    ): List<MangaChapter> {
        val (limit, actualOffset) = limitAndOffset(true, isResuming, offset)

        return chapterRepository.getRecents(RecentsHidden.TAB_UPDATES.toLong(), filterScanlators, showHidden, search, limit, actualOffset)
    }

    suspend fun awaitUngrouped(
        filterScanlators: Boolean,
        isResuming: Boolean,
        showHidden: Boolean = false,
        search: String = "",
        offset: Long = 0L,
    ): List<MangaChapterHistory> {
        val (limit, actualOffset) = limitAndOffset(true, isResuming, offset)

        return historyRepository.getRecentsUngrouped(RecentsHidden.TAB_HISTORY.toLong(), filterScanlators, showHidden, search, limit, actualOffset)
    }

    suspend fun awaitBySeries(
        filterScanlators: Boolean,
        isResuming: Boolean,
        showHidden: Boolean = false,
        search: String = "",
        offset: Long = 0L,
    ): List<MangaChapterHistory> {
        val (limit, actualOffset) = limitAndOffset(true, isResuming, offset)

        return historyRepository.getRecentsBySeries(RecentsHidden.TAB_HISTORY.toLong(), filterScanlators, showHidden, search, limit, actualOffset)
    }

    suspend fun awaitAll(
        includeRead: Boolean,
        filterScanlators: Boolean,
        isEndless: Boolean,
        isResuming: Boolean,
        showHiddenHistory: Boolean = false,
        showHiddenUpdates: Boolean = false,
        search: String = "",
        offset: Long = 0L,
    ): List<MangaChapterHistory> {
        val (limit, actualOffset) = limitAndOffset(isEndless, isResuming, offset)

        return historyRepository.getRecentsAll(includeRead, filterScanlators, showHiddenHistory, showHiddenUpdates, search, limit, actualOffset)
    }

    suspend fun awaitUpdates(limit: Long = 0L): List<MangaChapterHistory> =
        historyRepository.getRecentsAll(
            includeRead = false,
            filterScanlators = true,
            search = "",
            limit = limit,
            offset = 0L
        )
}
