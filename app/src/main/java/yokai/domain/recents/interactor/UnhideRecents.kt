package yokai.domain.recents.interactor

import yokai.domain.recents.RecentsHiddenRepository

class UnhideRecents(
    private val recentsHiddenRepository: RecentsHiddenRepository,
) {
    suspend fun await(tab: Int, chapterIds: List<Long>) {
        recentsHiddenRepository.unhideBulk(tab, chapterIds)
    }

    suspend fun awaitOne(tab: Int, chapterId: Long) {
        recentsHiddenRepository.unhide(tab, chapterId)
    }

    suspend fun awaitAll(tab: Int) {
        recentsHiddenRepository.unhideAll(tab)
    }
}
