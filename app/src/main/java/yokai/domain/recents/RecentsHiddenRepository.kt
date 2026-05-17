package yokai.domain.recents

import yokai.domain.recents.models.RecentsHidden

interface RecentsHiddenRepository {
    suspend fun hide(tab: Int, chapterId: Long, mangaId: Long)
    suspend fun hideBulk(tab: Int, items: List<Pair<Long, Long>>) // (chapterId, mangaId)
    suspend fun unhide(tab: Int, chapterId: Long)
    suspend fun unhideBulk(tab: Int, chapterIds: List<Long>)
    suspend fun unhideAll(tab: Int)
    suspend fun isHidden(tab: Int, chapterId: Long): Boolean
    suspend fun getHidden(tab: Int): List<RecentsHidden>
}
