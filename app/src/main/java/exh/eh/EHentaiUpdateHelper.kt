package exh.eh

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.domain.manga.models.Manga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import yokai.domain.category.interactor.GetCategories
import yokai.domain.category.interactor.SetMangaCategories
import yokai.domain.chapter.interactor.GetChapter
import yokai.domain.chapter.interactor.InsertChapter
import yokai.domain.chapter.interactor.UpdateChapter
import yokai.domain.chapter.models.ChapterUpdate
import yokai.domain.history.interactor.GetHistory
import yokai.domain.history.interactor.UpsertHistory
import yokai.domain.manga.interactor.GetManga
import yokai.domain.manga.interactor.UpdateManga
import yokai.domain.manga.models.MangaUpdate
import uy.kohesive.injekt.injectLazy
import java.io.File

data class ChapterChain(val manga: Manga, val chapters: List<Chapter>, val history: List<History>)

class EHentaiUpdateHelper(context: Context) {
    val parentLookupTable =
        MemAutoFlushingLookupTable(
            File(context.filesDir, "exh-plt.maftable"),
            GalleryEntry.Serializer(),
        )

    private val getChapter: GetChapter by injectLazy()
    private val getManga: GetManga by injectLazy()
    private val updateManga: UpdateManga by injectLazy()
    private val updateChapter: UpdateChapter by injectLazy()
    private val insertChapter: InsertChapter by injectLazy()
    private val setMangaCategories: SetMangaCategories by injectLazy()
    private val getCategories: GetCategories by injectLazy()
    private val upsertHistory: UpsertHistory by injectLazy()
    private val getHistory: GetHistory by injectLazy()

    /**
     * @param chapters Cannot be an empty list!
     *
     * @return Triple<Accepted, Discarded, HasNew>
     */
    suspend fun findAcceptedRootAndDiscardOthers(
        sourceId: Long,
        chapters: List<Chapter>,
    ): Triple<ChapterChain, List<ChapterChain>, List<Chapter>> {
        // Find other chains by looking up all manga that share chapter URLs
        val chains = chapters
            .flatMap { chapter ->
                getChapter.awaitAllByUrl(chapter.url, false).mapNotNull { it.manga_id }
            }
            .distinct()
            .mapNotNull { mangaId ->
                coroutineScope {
                    val manga = async(Dispatchers.IO) {
                        getManga.awaitById(mangaId)
                    }
                    val chapterList = async(Dispatchers.IO) {
                        getChapter.awaitAll(mangaId, false)
                    }
                    val history = async(Dispatchers.IO) {
                        getHistory.awaitAllByMangaId(mangaId)
                    }
                    val mangaResult = manga.await() ?: return@coroutineScope null
                    ChapterChain(
                        mangaResult,
                        chapterList.await(),
                        history.await(),
                    )
                }
            }
            .filter { it.manga.source == sourceId }

        // Accept oldest chain (lowest id)
        val accepted = chains.minBy { it.manga.id ?: Long.MAX_VALUE }

        val toDiscard = chains.filter { it.manga.favorite && it.manga.id != accepted.manga.id }
        val mangaUpdates = mutableListOf<MangaUpdate>()

        val chainsAsChapters = chains.flatMap { it.chapters }
        val chainsAsHistory = chains.flatMap { it.history }

        return if (toDiscard.isNotEmpty()) {
            // Copy chain chapters to accepted
            val (chapterUpdates, newChapters, _) = getChapterList(accepted, toDiscard, chainsAsChapters)

            toDiscard.forEach {
                mangaUpdates += MangaUpdate(
                    id = it.manga.id!!,
                    favorite = false,
                    dateAdded = 0,
                )
            }
            if (!accepted.manga.favorite) {
                mangaUpdates += MangaUpdate(
                    id = accepted.manga.id!!,
                    favorite = true,
                    dateAdded = System.currentTimeMillis(),
                )
            }

            val newAccepted = ChapterChain(accepted.manga, newChapters, emptyList())
            val rootsToMutate = toDiscard + newAccepted

            // Apply changes to all manga
            updateManga.awaitAll(mangaUpdates)
            // Update existing chapters
            updateChapter.awaitAll(chapterUpdates)
            // Insert new chapters for accepted manga
            if (newChapters.isNotEmpty()) {
                insertChapter.awaitBulk(newChapters)
            }

            // Handle history merging
            val currentChapters = getChapter.awaitAll(accepted.manga.id!!, false)
            val (newHistory, _) = getHistoryMerge(
                currentChapters,
                chainsAsChapters,
                chainsAsHistory,
            )

            // Upsert merged history
            newHistory.forEach { (chapterId, lastRead, timeRead) ->
                upsertHistory.await(chapterId, lastRead, timeRead)
            }

            // Copy categories from all chains to accepted manga
            val newCategories = rootsToMutate.flatMap { chapterChain ->
                getCategories.awaitByMangaId(chapterChain.manga.id).map { it.id!!.toLong() }
            }.distinct()
            rootsToMutate.forEach {
                setMangaCategories.await(it.manga.id, newCategories)
            }

            Triple(newAccepted, toDiscard, newChapters)
        } else {
            Triple(accepted, emptyList(), emptyList())
        }
    }

    private data class HistoryEntry(val chapterId: Long, val lastRead: Long, val timeRead: Long)

    private fun getHistoryMerge(
        currentChapters: List<Chapter>,
        chainsAsChapters: List<Chapter>,
        chainsAsHistory: List<History>,
    ): Pair<List<HistoryEntry>, List<Long>> {
        val history = chainsAsHistory.groupBy { hist -> chainsAsChapters.find { it.id == hist.chapter_id }?.url }
        val newHistory = currentChapters.mapNotNull { chapter ->
            val bestHistory = history[chapter.url]
                ?.maxByOrNull { it.last_read }
                ?.takeIf { it.chapter_id != (chapter.id ?: -1L) && it.last_read > 0 }
            if (bestHistory != null) {
                HistoryEntry(chapter.id!!, bestHistory.last_read, bestHistory.time_read)
            } else {
                null
            }
        }
        val currentChapterIds = currentChapters.mapNotNull { it.id }
        val historyToDelete = chainsAsHistory.filterNot { it.chapter_id in currentChapterIds }
            .mapNotNull { it.id }
        return newHistory to historyToDelete
    }

    private fun getChapterList(
        accepted: ChapterChain,
        toDiscard: List<ChapterChain>,
        chainsAsChapters: List<Chapter>,
    ): Triple<List<ChapterUpdate>, List<Chapter>, Boolean> {
        var new = false
        val acceptedMangaId = accepted.manga.id!!

        return toDiscard
            .flatMap { chain -> chain.chapters }
            .fold(accepted.chapters) { curChapters, chapter ->
                val newLastPageRead = chainsAsChapters.maxOfOrNull { it.last_page_read }

                if (curChapters.any { it.url == chapter.url }) {
                    curChapters.map {
                        if (it.url == chapter.url) {
                            val read = it.read || chapter.read
                            var lastPageRead = it.last_page_read.coerceAtLeast(chapter.last_page_read)
                            if (newLastPageRead != null && lastPageRead <= 0) {
                                lastPageRead = newLastPageRead
                            }
                            val bookmark = it.bookmark || chapter.bookmark
                            it.copy().apply {
                                this.read = read
                                this.last_page_read = lastPageRead
                                this.bookmark = bookmark
                            }
                        } else {
                            it
                        }
                    }
                } else {
                    new = true
                    curChapters + Chapter.create().apply {
                        id = null
                        manga_id = acceptedMangaId
                        url = chapter.url
                        name = chapter.name
                        this.read = chapter.read
                        this.bookmark = chapter.bookmark
                        last_page_read = if (newLastPageRead != null && chapter.last_page_read <= 0) {
                            newLastPageRead
                        } else {
                            chapter.last_page_read
                        }
                        date_fetch = chapter.date_fetch
                        date_upload = chapter.date_upload
                        chapter_number = -1f
                        scanlator = null
                        source_order = -1
                    }
                }
            }
            .sortedBy { it.date_upload }
            .let { chapters ->
                val updates = mutableListOf<ChapterUpdate>()
                val newChapters = mutableListOf<Chapter>()
                chapters.mapIndexed { index, chapter ->
                    val name = "v${index + 1}: " + chapter.name.substringAfter(" ")
                    val chapterNumber = (index + 1).toFloat()
                    val sourceOrder = (chapters.lastIndex - index)
                    if (chapter.id == null) {
                        newChapters.add(
                            chapter.copy().apply {
                                this.name = name
                                this.chapter_number = chapterNumber
                                this.source_order = sourceOrder
                            },
                        )
                    } else {
                        updates.add(
                            ChapterUpdate(
                                id = chapter.id!!,
                                name = name.takeUnless { chapter.name == it },
                                chapterNumber = chapterNumber.toDouble().takeUnless { chapter.chapter_number.toDouble() == it },
                                sourceOrder = sourceOrder.toLong().takeUnless { chapter.source_order.toLong() == it },
                            ),
                        )
                    }
                }
                Triple(updates.toList(), newChapters.toList(), new)
            }
    }
}
