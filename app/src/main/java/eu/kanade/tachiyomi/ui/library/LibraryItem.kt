package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.source.getNameForMangaInfo
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class LibraryItem(
    val libraryManga: LibraryManga,
    val downloadCount: Long = -1,
    val unreadCount: Long = -1,
    val isLocal: Boolean = false,
    val sourceLanguage: String = "",
    private val sourceManager: SourceManager = Injekt.get(),
) {
    /**
     * Checks if a query matches the manga
     *
     * @param constraint the query to check.
     * @return true if the manga matches the query, false otherwise.
     */
    fun matches(constraint: String): Boolean {
        val sourceName by lazy { sourceManager.getOrStub(libraryManga.manga.source).getNameForMangaInfo(null) }

        // Fuzzy search function that checks if characters appear in sequence
        fun String.containsFuzzy(other: String, ignoreCase: Boolean = true): Boolean {
            if (other.isEmpty()) return true
            if (this.isEmpty()) return false

            val sourceText = if (ignoreCase) this.lowercase() else this
            val searchText = if (ignoreCase) other.lowercase() else other

            var sourceIndex = 0
            var searchIndex = 0

            while (sourceIndex < sourceText.length && searchIndex < searchText.length) {
                if (sourceText[sourceIndex] == searchText[searchIndex]) {
                    searchIndex++
                }
                sourceIndex++
            }

            return searchIndex == searchText.length
        }

        return libraryManga.manga.title.containsFuzzy(constraint) ||
            (libraryManga.manga.author?.containsFuzzy(constraint) ?: false) ||
            (libraryManga.manga.artist?.containsFuzzy(constraint) ?: false) ||
            (libraryManga.manga.description?.containsFuzzy(constraint) ?: false) ||
            constraint.split(",").map { it.trim() }.all { subconstraint ->
                checkNegatableConstraint(subconstraint) {
                    sourceName.containsFuzzy(it) ||
                        (libraryManga.manga.genre?.any { genre -> genre.equals(it, true) } ?: false)
                }
            }
    }

    /**
     * Checks a predicate on a negatable constraint. If the constraint starts with a minus character,
     * the minus is stripped and the result of the predicate is inverted.
     *
     * @param constraint the argument to the predicate. Inverts the predicate if it starts with '-'.
     * @param predicate the check to be run against the constraint.
     * @return !predicate(x) if constraint = "-x", otherwise predicate(constraint)
     */
    private fun checkNegatableConstraint(
        constraint: String,
        predicate: (String) -> Boolean,
    ): Boolean {
        return if (constraint.startsWith("-")) {
            !predicate(constraint.substringAfter("-").trimStart())
        } else {
            predicate(constraint)
        }
    }
}
