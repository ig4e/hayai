package exh.recs

import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.ui.source.globalsearch.GlobalSearchController

/**
 * Mangas surfaced by external recommendation sources (AniList, MAL, NovelUpdates, etc.) are built
 * in-memory with [Manga.source] = -1 and a null id — they don't exist in our DB. Opening them in
 * [MangaDetailsController] would NPE on the non-null id assertion, so the previous code silently
 * dropped the click. Mirror TachiyomiSY's behavior and fall back to global search by title, letting
 * the user pick an installed source to read from. Same fallback applies if an associated-source
 * recommendation hasn't been imported into our DB yet (id still null).
 */
internal fun recommendationDestination(manga: Manga): Controller {
    val noLocalManga = manga.source == -1L || manga.id == null
    return if (noLocalManga) {
        GlobalSearchController(manga.title)
    } else {
        MangaDetailsController(manga, fromCatalogue = true)
    }
}
