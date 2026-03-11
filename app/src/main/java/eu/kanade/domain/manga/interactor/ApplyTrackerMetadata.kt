package eu.kanade.domain.manga.interactor

import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.domain.manga.model.toSManga
import eu.kanade.tachiyomi.ui.manga.track.toNormalizedMetadata
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.interactor.SetCustomMangaInfo
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant

class ApplyTrackerMetadata(
    private val updateManga: UpdateManga = Injekt.get(),
    private val getCustomMangaInfo: GetCustomMangaInfo = Injekt.get(),
    private val setCustomMangaInfo: SetCustomMangaInfo = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
) {

    suspend fun await(
        manga: Manga,
        metadata: TrackMangaMetadata,
    ): Manga {
        val normalized = metadata.toNormalizedMetadata()
        val coverLastModified = normalized.thumbnailUrl
            ?.takeIf { it != manga.ogThumbnailUrl }
            ?.let { Instant.now().toEpochMilli() }

        val updatedManga = if (manga.isLocal()) {
            val localManga = manga.copy(
                ogTitle = normalized.title ?: manga.ogTitle,
                ogAuthor = normalized.authors ?: manga.ogAuthor,
                ogArtist = normalized.artists ?: manga.ogArtist,
                ogThumbnailUrl = normalized.thumbnailUrl ?: manga.ogThumbnailUrl,
                ogDescription = normalized.description ?: manga.ogDescription,
                coverLastModified = coverLastModified ?: manga.coverLastModified,
                lastUpdate = manga.lastUpdate + 1,
            )
            (sourceManager.get(LocalSource.ID) as LocalSource).updateMangaInfo(localManga.toSManga())
            updateManga.await(
                MangaUpdate(
                    id = manga.id,
                    title = localManga.ogTitle,
                    author = localManga.ogAuthor,
                    artist = localManga.ogArtist,
                    thumbnailUrl = localManga.ogThumbnailUrl,
                    description = localManga.ogDescription,
                    coverLastModified = coverLastModified,
                ),
            )
            localManga
        } else {
            updateManga.await(
                MangaUpdate(
                    id = manga.id,
                    title = normalized.title,
                    author = normalized.authors,
                    artist = normalized.artists,
                    thumbnailUrl = normalized.thumbnailUrl,
                    description = normalized.description,
                    coverLastModified = coverLastModified,
                ),
            )
            manga.copy(
                ogTitle = normalized.title ?: manga.ogTitle,
                ogAuthor = normalized.authors ?: manga.ogAuthor,
                ogArtist = normalized.artists ?: manga.ogArtist,
                ogThumbnailUrl = normalized.thumbnailUrl ?: manga.ogThumbnailUrl,
                ogDescription = normalized.description ?: manga.ogDescription,
                coverLastModified = coverLastModified ?: manga.coverLastModified,
                lastUpdate = manga.lastUpdate + 1,
            )
        }

        getCustomMangaInfo.get(manga.id)?.let { customMangaInfo ->
            setCustomMangaInfo.set(
                customMangaInfo.copy(
                    title = null,
                    author = null,
                    artist = null,
                    thumbnailUrl = null,
                    description = null,
                ),
            )
        }

        return updatedManga.copy()
    }
}

