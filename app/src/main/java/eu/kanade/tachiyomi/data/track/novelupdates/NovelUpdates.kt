package eu.kanade.tachiyomi.data.track.novelupdates

import android.content.Context
import android.graphics.Color
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.TrackContentType
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import yokai.i18n.MR
import yokai.util.lang.getString

/**
 * NovelUpdates tracker — https://www.novelupdates.com.
 *
 * Cookie-based authentication: the user logs in via WebView elsewhere; this class consumes
 * the session cookies stored as the tracker password to talk to the WordPress backend.
 *
 * Status mapping ports Tsundoku's NovelUpdates verbatim. The network layer lives in
 * [NovelUpdatesApi] so this class stays focused on TrackService glue.
 */
class NovelUpdates(private val context: Context, id: Long) : TrackService(id) {

    companion object {
        const val READING = 1
        const val COMPLETED = 2
        const val ON_HOLD = 3
        const val DROPPED = 4
        const val PLAN_TO_READ = 5
    }

    override val supportedContentTypes: Set<TrackContentType> = setOf(TrackContentType.NOVEL)

    private val api by lazy { NovelUpdatesApi(this, client) }

    override fun nameRes() = MR.strings.novelupdates

    override fun getLogo() = R.drawable.ic_tracker_novelupdates

    override fun getTrackerColor() = Color.rgb(255, 102, 0)

    override fun getLogoColor() = Color.rgb(255, 102, 0)

    override fun getStatusList(): List<Int> =
        listOf(READING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_READ)

    override fun isCompletedStatus(index: Int) = getStatusList()[index] == COMPLETED

    override fun completedStatus() = COMPLETED
    override fun readingStatus() = READING
    override fun planningStatus() = PLAN_TO_READ

    override fun getStatus(status: Int): String = with(context) {
        when (status) {
            READING -> getString(MR.strings.reading)
            COMPLETED -> getString(MR.strings.completed)
            ON_HOLD -> getString(MR.strings.on_hold)
            DROPPED -> getString(MR.strings.dropped)
            PLAN_TO_READ -> getString(MR.strings.plan_to_read)
            else -> ""
        }
    }

    override fun getGlobalStatus(status: Int): String = getStatus(status)

    override fun getScoreList(): ImmutableList<String> = persistentListOf(
        "10", "9", "8", "7", "6", "5", "4", "3", "2", "1", "",
    )

    override fun indexToScore(index: Int): Float =
        if (index == 10) 0f else (10 - index).toFloat()

    override fun displayScore(track: Track): String =
        if (track.score == 0f) "-" else track.score.toInt().toString()

    /** Convert our internal status to a NovelUpdates list ID. Tsundoku verbatim. */
    private fun statusToListId(status: Int): Long = when (status) {
        READING -> 0L
        COMPLETED -> 1L
        PLAN_TO_READ -> 2L
        ON_HOLD -> 3L
        DROPPED -> 4L
        else -> 0L
    }

    override suspend fun add(track: Track): Track {
        return update(track)
    }

    override suspend fun update(track: Track, setToRead: Boolean): Track {
        updateTrackStatus(track, setToRead)
        val novelId = track.media_id.toString()
        api.moveToList(novelId, statusToListId(track.status))
        if (track.last_chapter_read > 0f) {
            api.updateNotesProgress(novelId, track.last_chapter_read.toInt())
        }
        return track
    }

    override suspend fun bind(track: Track): Track {
        val slug = Regex("series/([^/]+)/?").find(track.tracking_url)?.groupValues?.get(1)
        if (slug != null) {
            api.getNovelId("https://www.novelupdates.com/series/$slug/")?.let { id ->
                track.media_id = id.toLongOrNull() ?: track.media_id
            }
        }
        val currentStatus = api.getReadingListStatus(track.media_id.toString())
        // Tsundoku heuristic: if the user has any progress on the novel, default to READING;
        // otherwise PLAN_TO_READ. Hayai's TrackService.bind doesn't take the hasReadChapters
        // flag Tsundoku does, so we derive it from track.last_chapter_read.
        track.status = currentStatus
            ?: if (track.last_chapter_read > 0f) READING else PLAN_TO_READ
        val progress = api.getNotesProgress(track.media_id.toString())
        if (progress > 0) track.last_chapter_read = progress.toFloat()
        return track
    }

    override suspend fun search(query: String): List<TrackSearch> = api.search(query)

    override suspend fun refresh(track: Track): Track {
        val novelId = track.media_id.toString()
        api.getReadingListStatus(novelId)?.let { track.status = it }
        val progress = api.getNotesProgress(novelId)
        if (progress > 0) track.last_chapter_read = progress.toFloat()
        return track
    }

    override suspend fun login(username: String, password: String): Boolean {
        // NovelUpdates uses cookie-based auth from the website. The password field carries the
        // session cookies as the user copied them in (or scraped via WebView login).
        if (password.isBlank()) return false
        saveCredentials(username.ifBlank { "NovelUpdates" }, password)
        return true
    }
}
