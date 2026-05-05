package yokai.domain.library

import eu.kanade.tachiyomi.core.preference.PreferenceStore

class LibraryPreferences(private val preferenceStore: PreferenceStore) {
    fun randomSortSeed() = preferenceStore.getInt("library_random_sort_seed", 0)

    fun markDuplicateReadChapterAsRead() = preferenceStore.getStringSet("mark_duplicate_read_chapter_read", emptySet())

    // NOVEL: When true, restore scroll position for novels even if the chapter is marked read.
    fun novelReadProgress100() = preferenceStore.getBoolean("pref_novel_read_progress_100", true)

    companion object {
        const val MARK_DUPLICATE_READ_CHAPTER_READ_NEW = "new"
        const val MARK_DUPLICATE_READ_CHAPTER_READ_EXISTING = "existing"
    }
}
