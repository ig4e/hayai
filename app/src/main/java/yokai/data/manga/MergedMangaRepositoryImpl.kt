package yokai.data.manga

import yokai.data.DatabaseHandler
import yokai.domain.manga.MergedMangaRepository
import yokai.domain.manga.models.MergedMangaReference

class MergedMangaRepositoryImpl(
    private val handler: DatabaseHandler,
) : MergedMangaRepository {

    override suspend fun getReferencesById(mergeId: Long): List<MergedMangaReference> =
        handler.awaitList { mergedQueries.selectByMergeId(mergeId, ::mergedMapper) }

    override suspend fun insert(reference: MergedMangaReference): Long? =
        handler.awaitOneOrNullExecutable(inTransaction = true) {
            mergedQueries.insert(
                infoManga = reference.isInfoManga,
                getChapterUpdates = reference.getChapterUpdates,
                chapterSortMode = reference.chapterSortMode.toLong(),
                chapterPriority = reference.chapterPriority.toLong(),
                downloadChapters = reference.downloadChapters,
                mergeId = reference.mergeId,
                mergeUrl = reference.mergeUrl,
                mangaId = reference.mangaId,
                mangaUrl = reference.mangaUrl,
                mangaSource = reference.mangaSourceId,
            )
            mergedQueries.selectLastInsertedRowId()
        }

    override suspend fun deleteByMergeId(mergeId: Long) {
        handler.await { mergedQueries.deleteByMergeId(mergeId) }
    }

    override suspend fun deleteById(id: Long) {
        handler.await { mergedQueries.deleteById(id) }
    }

    companion object {
        fun mergedMapper(
            id: Long,
            infoManga: Boolean,
            getChapterUpdates: Boolean,
            chapterSortMode: Long,
            chapterPriority: Long,
            downloadChapters: Boolean,
            mergeId: Long,
            mergeUrl: String,
            mangaId: Long?,
            mangaUrl: String,
            mangaSource: Long,
        ): MergedMangaReference = MergedMangaReference(
            id = id,
            isInfoManga = infoManga,
            getChapterUpdates = getChapterUpdates,
            chapterSortMode = chapterSortMode.toInt(),
            chapterPriority = chapterPriority.toInt(),
            downloadChapters = downloadChapters,
            mergeId = mergeId,
            mergeUrl = mergeUrl,
            mangaId = mangaId,
            mangaUrl = mangaUrl,
            mangaSourceId = mangaSource,
        )
    }
}
