package exh.eh

import android.content.Context
import exh.metadata.metadata.EHentaiSearchMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import yokai.domain.manga.models.Manga
import java.io.File

data class ChapterChain(val manga: Manga, val chapters: List<ChapterData>, val history: List<HistoryData>)

/**
 * Simplified chapter data holder for update helper operations.
 * TODO: Replace with actual domain Chapter when available.
 */
data class ChapterData(
    val id: Long = -1,
    val mangaId: Long = -1,
    val url: String = "",
    val name: String = "",
    val read: Boolean = false,
    val bookmark: Boolean = false,
    val lastPageRead: Long = 0,
    val dateFetch: Long = 0,
    val dateUpload: Long = 0,
    val chapterNumber: Double = -1.0,
    val scanlator: String? = null,
    val sourceOrder: Long = -1,
)

/**
 * Simplified history data holder.
 * TODO: Replace with actual domain History when available.
 */
data class HistoryData(
    val id: Long = -1,
    val chapterId: Long = -1,
    val readAt: java.util.Date? = null,
    val readDuration: Long = 0,
)

class EHentaiUpdateHelper(context: Context) {
    val parentLookupTable =
        MemAutoFlushingLookupTable(
            File(context.filesDir, "exh-plt.maftable"),
            GalleryEntry.Serializer(),
        )

    // TODO: Wire up repository/interactor dependencies when available in Hayai
    // These will be injected via Injekt once the domain layer is ported

    /**
     * @param chapters Cannot be an empty list!
     *
     * @return Triple<Accepted, Discarded, HasNew>
     */
    suspend fun findAcceptedRootAndDiscardOthers(
        sourceId: Long,
        chapters: List<ChapterData>,
    ): Triple<ChapterChain, List<ChapterChain>, List<ChapterData>> {
        // TODO: Implement full chain resolution when domain interactors are available
        // For now, return a stub that accepts the first chain
        val stubManga = Manga(
            id = 0L,
            url = "",
            title = "",
            artist = null,
            author = null,
            description = null,
            genres = null,
            status = 0,
            thumbnailUrl = null,
            updateStrategy = eu.kanade.tachiyomi.source.model.UpdateStrategy.ALWAYS_UPDATE,
            initialized = false,
            source = sourceId,
            favorite = false,
            lastUpdate = 0L,
            dateAdded = 0L,
            viewerFlags = 0,
            chapterFlags = 0,
            hideTitle = false,
            filteredScanlators = null,
            coverLastModified = 0L,
        )
        val accepted = ChapterChain(stubManga, chapters, emptyList())
        return Triple(accepted, emptyList(), emptyList())
    }
}
