package yokai.domain.manga.models

data class MergedMangaReference(
    val id: Long,
    val isInfoManga: Boolean,
    val getChapterUpdates: Boolean,
    val chapterSortMode: Int,
    val chapterPriority: Int,
    val downloadChapters: Boolean,
    val mergeId: Long,
    val mergeUrl: String,
    val mangaId: Long?,
    val mangaUrl: String,
    val mangaSourceId: Long,
) {
    companion object {
        const val CHAPTER_SORT_NONE = 0
        const val CHAPTER_SORT_NO_DEDUPE = 1
        const val CHAPTER_SORT_PRIORITY = 2
        const val CHAPTER_SORT_MOST_CHAPTERS = 3
        const val CHAPTER_SORT_HIGHEST_CHAPTER_NUMBER = 4
    }
}
