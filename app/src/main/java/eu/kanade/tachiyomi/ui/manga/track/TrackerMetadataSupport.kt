package eu.kanade.tachiyomi.ui.manga.track

import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import exh.util.trimOrNull
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.model.Track

data class TrackerMetadataCandidate(
    val track: Track,
    val tracker: Tracker,
    val metadata: TrackMangaMetadata,
)

data class TrackerMetadataFormState(
    val title: String = "",
    val author: String = "",
    val artist: String = "",
    val thumbnailUrl: String = "",
    val description: String = "",
)

fun TrackMangaMetadata.toNormalizedMetadata(): TrackMangaMetadata {
    return copy(
        title = title?.trimOrNull(),
        thumbnailUrl = thumbnailUrl?.trimOrNull(),
        description = description?.trimOrNull(),
        authors = authors?.trimOrNull(),
        artists = artists?.trimOrNull(),
    )
}

fun TrackMangaMetadata.hasEditableData(): Boolean {
    val normalized = toNormalizedMetadata()
    return listOf(
        normalized.title,
        normalized.authors,
        normalized.artists,
        normalized.thumbnailUrl,
        normalized.description,
    ).any { !it.isNullOrBlank() }
}

fun TrackMangaMetadata.toFormState(): TrackerMetadataFormState {
    val normalized = toNormalizedMetadata()
    return TrackerMetadataFormState(
        title = normalized.title.orEmpty(),
        author = normalized.authors.orEmpty(),
        artist = normalized.artists.orEmpty(),
        thumbnailUrl = normalized.thumbnailUrl.orEmpty(),
        description = normalized.description.orEmpty(),
    )
}

fun TrackerMetadataFormState.toMetadata(): TrackMangaMetadata {
    return TrackMangaMetadata(
        title = title.trimOrNull(),
        authors = author.trimOrNull(),
        artists = artist.trimOrNull(),
        thumbnailUrl = thumbnailUrl.trimOrNull(),
        description = description.trimOrNull(),
    )
}

suspend fun loadTrackerMetadataCandidates(
    mangaId: Long,
    getTracks: GetTracks,
    trackerManager: TrackerManager,
    onError: suspend (Tracker, Throwable) -> Unit = { _, _ -> },
): List<TrackerMetadataCandidate> {
    return getTracks.await(mangaId)
        .mapNotNull { track ->
            val tracker = trackerManager.get(track.trackerId) ?: return@mapNotNull null
            val metadata = try {
                tracker.getMangaMetadata(track)
            } catch (_: NotImplementedError) {
                null
            } catch (e: Throwable) {
                onError(tracker, e)
                null
            } ?: return@mapNotNull null

            metadata
                .takeIf { it.hasEditableData() }
                ?.let { TrackerMetadataCandidate(track = track, tracker = tracker, metadata = it.toNormalizedMetadata()) }
        }
}
