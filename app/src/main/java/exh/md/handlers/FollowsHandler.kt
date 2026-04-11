package exh.md.handlers

import eu.kanade.tachiyomi.data.database.models.Track
// TODO: Add MDLIST constant when MdList tracker is available
private const val MDLIST = 60L
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import exh.md.dto.MangaDataDto
import exh.md.dto.PersonalRatingDto
import exh.md.dto.ReadingStatusDto
import exh.md.service.MangaDexAuthService
import exh.md.utils.FollowStatus
import exh.md.utils.MdUtil
import exh.md.utils.asMdMap
import exh.md.utils.mdListCall
import exh.metadata.metadata.MangaDexSearchMetadata
import exh.util.under
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class FollowsHandler(
    private val lang: String,
    private val service: MangaDexAuthService,
) {

    /**
     * fetch follows page
     */
    suspend fun fetchFollows(page: Int): MangasPage {
        return withContext(Dispatchers.IO) {
            val follows = service.userFollowList(MdUtil.mangaLimit * page)

            if (follows.data.isEmpty()) {
                return@withContext MangasPage(emptyList(), false)
            }

            val hasMoreResults = follows.limit + follows.offset under follows.total
            val statusListResponse = service.readingStatusAllManga()
            val results = followsParseMangaPage(follows.data, statusListResponse.statuses)

            MangasPage(results.map { it.first }, hasMoreResults)
        }
    }

    /**
     * Parse follows api to manga page
     * used when multiple follows
     */
    private fun followsParseMangaPage(
        response: List<MangaDataDto>,
        statuses: Map<String, String?>,
    ): List<Pair<SManga, MangaDexSearchMetadata>> {
        val comparator = compareBy<Pair<SManga, MangaDexSearchMetadata>> { it.second.followStatus }
            .thenBy { it.first.title }

        return response.map {
            MdUtil.createMangaEntry(
                it,
                lang,
            ) to MangaDexSearchMetadata().apply {
                followStatus = FollowStatus.fromDex(statuses[it.id]).long.toInt()
            }
        }.sortedWith(comparator)
    }

    /**
     * Change the status of a manga
     */
    suspend fun updateFollowStatus(mangaId: String, followStatus: FollowStatus): Boolean {
        return withContext(Dispatchers.IO) {
            val status = when (followStatus == FollowStatus.UNFOLLOWED) {
                true -> null
                false -> followStatus.toDex()
            }
            val readingStatusDto = ReadingStatusDto(status)

            if (followStatus == FollowStatus.UNFOLLOWED) {
                service.unfollowManga(mangaId)
            } else {
                service.followManga(mangaId)
            }

            service.updateReadingStatusForManga(mangaId, readingStatusDto).result == "ok"
        }
    }

    suspend fun updateRating(track: Track): Boolean {
        return withContext(Dispatchers.IO) {
            val mangaId = MdUtil.getMangaId(track.tracking_url)
            val result = runCatching {
                if (track.score == 0.0) {
                    service.deleteMangaRating(mangaId)
                } else {
                    service.updateMangaRating(mangaId, track.score.toInt())
                }.result == "ok"
            }
            result.getOrDefault(false)
        }
    }

    /**
     * fetch all manga from all possible pages
     */
    suspend fun fetchAllFollows(): List<Pair<SManga, MangaDexSearchMetadata>> {
        return withContext(Dispatchers.IO) {
            val results = async {
                mdListCall {
                    service.userFollowList(it)
                }
            }

            val readingStatusResponse = async { service.readingStatusAllManga().statuses }

            followsParseMangaPage(results.await(), readingStatusResponse.await())
        }
    }

    suspend fun fetchTrackingInfo(url: String): Track {
        return withContext(Dispatchers.IO) {
            val mangaId = MdUtil.getMangaId(url)
            val followStatusDef = async {
                FollowStatus.fromDex(service.readingStatusForManga(mangaId).status)
            }
            val ratingDef = async {
                service.mangasRating(mangaId).ratings.asMdMap<PersonalRatingDto>()[mangaId]
            }
            val (followStatus, rating) = followStatusDef.await() to ratingDef.await()
            Track.create(MDLIST).apply {
                title = ""
                status = followStatus.long
                tracking_url = url
                score = rating?.rating?.toDouble() ?: 0.0
            }
        }
    }
}
