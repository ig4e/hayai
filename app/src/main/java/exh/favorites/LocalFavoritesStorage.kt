package exh.favorites

import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.source.online.all.EHentai
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.source.EXH_SOURCE_ID
import exh.source.isEhBasedManga
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import yokai.domain.category.interactor.GetCategories
import yokai.domain.manga.EhFavoritesRepository
import yokai.domain.manga.interactor.GetManga

/**
 * Represents a favorite entry from the E-Hentai/ExHentai favorites system.
 */
data class FavoriteEntry(
    val title: String,
    val gid: String,
    val token: String,
    val category: Int,
    val otherGid: String? = null,
    val otherToken: String? = null,
) {
    fun getUrl() = "/g/$gid/$token/"
}

class LocalFavoritesStorage(
    private val getManga: GetManga = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val ehFavoritesRepository: EhFavoritesRepository = Injekt.get(),
) {

    suspend fun getChangedDbEntries() = getManga.awaitFavorites()
        .asFlow()
        .loadDbCategories()
        .parseToFavoriteEntries()
        .getChangedEntries()

    suspend fun getChangedRemoteEntries(entries: List<EHentai.ParsedManga>): ChangeSet {
        // Convert parsed manga to domain mangas with category info
        return entries.asFlow()
            .map { parsed ->
                val manga = MangaImpl(source = EXH_SOURCE_ID, url = parsed.manga.url).apply {
                    title = parsed.manga.title
                    copyFrom(parsed.manga)
                    favorite = true
                    date_added = System.currentTimeMillis()
                }
                parsed.fav to manga
            }
            .parseToFavoriteEntries()
            .getChangedEntries()
    }

    suspend fun snapshotEntries() {
        val dbMangas = getManga.awaitFavorites()
            .asFlow()
            .loadDbCategories()
            .parseToFavoriteEntries()

        // Delete old snapshot
        ehFavoritesRepository.deleteAll()

        // Insert new snapshots
        ehFavoritesRepository.insertAll(dbMangas.toList())
    }

    suspend fun clearSnapshots() {
        ehFavoritesRepository.deleteAll()
    }

    private suspend fun Flow<FavoriteEntry>.getChangedEntries(): ChangeSet {
        val terminated = toList()

        val databaseEntries = ehFavoritesRepository.getAll()

        val added = terminated.groupBy { it.gid to it.token }
            .filter { (_, values) ->
                values.all { queryListForEntry(databaseEntries, it) == null }
            }
            .map { it.value.first() }

        val removed = databaseEntries
            .groupBy { it.gid to it.token }
            .filter { (_, values) ->
                values.all { queryListForEntry(terminated, it) == null }
            }
            .map { it.value.first() }

        return ChangeSet(added, removed)
    }

    private fun FavoriteEntry.urlEquals(other: FavoriteEntry) = (gid == other.gid && token == other.token) ||
        (otherGid != null && otherToken != null && (otherGid == other.gid && otherToken == other.token)) ||
        (other.otherGid != null && other.otherToken != null && (gid == other.otherGid && token == other.otherToken)) ||
        (
            otherGid != null &&
                otherToken != null &&
                other.otherGid != null &&
                other.otherToken != null &&
                otherGid == other.otherGid &&
                otherToken == other.otherToken
            )

    private fun queryListForEntry(list: List<FavoriteEntry>, entry: FavoriteEntry) =
        list.find { it.urlEquals(entry) && it.category == entry.category }

    private suspend fun Flow<Manga>.loadDbCategories(): Flow<Pair<Int, Manga>> {
        val dbCategories = getCategories.await()
            .filter { it.id != 0 } // Filter out default category

        return filter { validateDbManga(it) }.mapNotNull { manga ->
            val categories = getCategories.awaitByMangaId(manga.id)

            val idx = dbCategories.indexOf(
                categories.firstOrNull()
                    ?: return@mapNotNull null,
            )
            if (idx < 0) return@mapNotNull null
            idx to manga
        }
    }

    private fun Flow<Pair<Int, Manga>>.parseToFavoriteEntries() =
        filter { (_, manga) ->
            validateDbManga(manga)
        }.mapNotNull { (categoryId, manga) ->
            try {
                FavoriteEntry(
                    title = manga.title,
                    gid = EHentaiSearchMetadata.galleryId(manga.url),
                    token = EHentaiSearchMetadata.galleryToken(manga.url),
                    category = categoryId,
                ).also {
                    if (it.category > MAX_CATEGORIES) {
                        return@mapNotNull null
                    }
                }
            } catch (e: Exception) {
                null
            }
        }

    private fun validateDbManga(manga: Manga) =
        manga.favorite && manga.isEhBasedManga()

    companion object {
        const val MAX_CATEGORIES = 9
    }
}

data class ChangeSet(
    val added: List<FavoriteEntry>,
    val removed: List<FavoriteEntry>,
)
