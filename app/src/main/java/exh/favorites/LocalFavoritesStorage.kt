package exh.favorites

import eu.kanade.tachiyomi.source.online.all.EHentai
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.source.EXH_SOURCE_ID
// TODO: import exh.source.isEhBasedManga when Manga extension is available
import yokai.domain.manga.models.Manga

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

class LocalFavoritesStorage {
    // TODO: Wire up repository dependencies when available
    // These will be injected via Injekt once the domain layer is ported

    suspend fun getChangedDbEntries(): ChangeSet {
        // TODO: Implement when manga repositories are available
        return ChangeSet(emptyList(), emptyList())
    }

    suspend fun getChangedRemoteEntries(entries: List<EHentai.ParsedManga>): ChangeSet {
        // TODO: Implement when manga repositories are available
        return ChangeSet(emptyList(), emptyList())
    }

    suspend fun snapshotEntries() {
        // TODO: Implement when manga repositories are available
    }

    suspend fun clearSnapshots() {
        // TODO: Implement when manga repositories are available
    }

    companion object {
        const val MAX_CATEGORIES = 9
    }
}

data class ChangeSet(
    val added: List<FavoriteEntry>,
    val removed: List<FavoriteEntry>,
)
