package yokai.domain.recents.interactor

import yokai.domain.recents.RecentsHiddenRepository

class HideRecents(
    private val recentsHiddenRepository: RecentsHiddenRepository,
) {
    suspend fun await(tab: Int, items: List<Pair<Long, Long>>) {
        recentsHiddenRepository.hideBulk(tab, items)
    }

    suspend fun awaitOne(tab: Int, chapterId: Long, mangaId: Long) {
        recentsHiddenRepository.hide(tab, chapterId, mangaId)
    }
}
