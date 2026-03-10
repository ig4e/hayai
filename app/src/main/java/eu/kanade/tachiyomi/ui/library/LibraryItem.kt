package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.source.getNameForMangaInfo
import eu.kanade.tachiyomi.util.lang.containsFuzzy
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.LocalSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

private const val LOCAL_SOURCE_ID_ALIAS = "local"

data class LibraryItem(
    val libraryManga: LibraryManga,
    val downloadCount: Long = -1,
    val unreadCount: Long = -1,
    val isLocal: Boolean = false,
    val sourceLanguage: String = "",
    private val sourceManager: SourceManager = Injekt.get(),
) {
    val id: Long = libraryManga.id

    /**
     * Checks if a query matches the manga
     *
     * @param constraint the query to check.
     * @return true if the manga matches the query, false otherwise.
     */
    fun matches(constraint: String): Boolean {
        val sourceName by lazy { sourceManager.getOrStub(libraryManga.manga.source).getNameForMangaInfo() }
        if (constraint.startsWith("id:", true)) {
            return id == constraint.substringAfter("id:").toLongOrNull()
        } else if (constraint.startsWith("src:", true)) {
            val querySource = constraint.substringAfter("src:")
            return if (querySource.equals(LOCAL_SOURCE_ID_ALIAS, ignoreCase = true)) {
                libraryManga.manga.source == LocalSource.ID
            } else {
                libraryManga.manga.source == querySource.toLongOrNull()
            }
        }
        return libraryManga.manga.title.containsFuzzy(constraint) ||
            (libraryManga.manga.author?.containsFuzzy(constraint) ?: false) ||
            (libraryManga.manga.artist?.containsFuzzy(constraint) ?: false) ||
            (libraryManga.manga.description?.containsFuzzy(constraint) ?: false) ||
            constraint.split(",").map { it.trim() }.all { subconstraint ->
                checkNegatableConstraint(subconstraint) {
                    sourceName.containsFuzzy(it) ||
                        (libraryManga.manga.genre?.any { genre -> genre.containsFuzzy(it) } ?: false)
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
