package yokai.domain.recents.interactor

import yokai.domain.recents.RecentsHiddenRepository
import yokai.domain.recents.models.RecentsHidden

class GetHiddenRecents(
    private val recentsHiddenRepository: RecentsHiddenRepository,
) {
    suspend fun await(tab: Int): List<RecentsHidden> = recentsHiddenRepository.getHidden(tab)

    suspend fun awaitIsHidden(tab: Int, chapterId: Long): Boolean =
        recentsHiddenRepository.isHidden(tab, chapterId)
}
