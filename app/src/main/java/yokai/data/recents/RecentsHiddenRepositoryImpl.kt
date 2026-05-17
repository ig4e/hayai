package yokai.data.recents

import yokai.data.DatabaseHandler
import yokai.domain.recents.RecentsHiddenRepository
import yokai.domain.recents.models.RecentsHidden

class RecentsHiddenRepositoryImpl(private val handler: DatabaseHandler) : RecentsHiddenRepository {

    override suspend fun hide(tab: Int, chapterId: Long, mangaId: Long) {
        val now = System.currentTimeMillis()
        handler.await(inTransaction = true) {
            recents_hiddenQueries.insert(tab.toLong(), chapterId, mangaId, now)
        }
    }

    override suspend fun hideBulk(tab: Int, items: List<Pair<Long, Long>>) {
        if (items.isEmpty()) return
        val now = System.currentTimeMillis()
        val tabLong = tab.toLong()
        handler.await(inTransaction = true) {
            items.forEach { (chapterId, mangaId) ->
                recents_hiddenQueries.insert(tabLong, chapterId, mangaId, now)
            }
        }
    }

    override suspend fun unhide(tab: Int, chapterId: Long) {
        handler.await(inTransaction = true) {
            recents_hiddenQueries.delete(tab.toLong(), chapterId)
        }
    }

    override suspend fun unhideBulk(tab: Int, chapterIds: List<Long>) {
        if (chapterIds.isEmpty()) return
        val tabLong = tab.toLong()
        handler.await(inTransaction = true) {
            chapterIds.forEach { chapterId ->
                recents_hiddenQueries.delete(tabLong, chapterId)
            }
        }
    }

    override suspend fun unhideAll(tab: Int) {
        handler.await(inTransaction = true) {
            recents_hiddenQueries.deleteAllForTab(tab.toLong())
        }
    }

    override suspend fun isHidden(tab: Int, chapterId: Long): Boolean =
        handler.awaitOne { recents_hiddenQueries.isHidden(tab.toLong(), chapterId) }

    override suspend fun getHidden(tab: Int): List<RecentsHidden> =
        handler.awaitList { recents_hiddenQueries.getHiddenForTab(tab.toLong()) }
            .map {
                RecentsHidden(
                    tab = it.hidden_tab.toInt(),
                    chapterId = it.hidden_chapter_id,
                    mangaId = it.hidden_manga_id,
                    hiddenAt = it.hidden_at,
                )
            }
}
