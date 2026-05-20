package eu.kanade.tachiyomi.data.track.bangumi

import android.content.Context
import android.graphics.Color
import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.bangumi.dto.BGMOAuth
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.data.track.updateNewTrackInfo
import eu.kanade.tachiyomi.util.system.e
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import yokai.util.koin.injectLazy
import yokai.i18n.MR
import yokai.util.lang.getString

class Bangumi(private val context: Context, id: Long) : TrackService(id) {

    override fun nameRes() = MR.strings.bangumi

    private val json: Json by injectLazy()

    private val interceptor by lazy { BangumiInterceptor(this) }

    private val api by lazy { BangumiApi(id, client, interceptor) }

    override fun getScoreList(): ImmutableList<String> {
        return IntRange(0, 10).map(Int::toString).toImmutableList()
    }

    override fun displayScore(track: Track): String {
        return track.score.toInt().toString()
    }

    override suspend fun update(track: Track, setToRead: Boolean): Track {
        updateTrackStatus(track, setToRead, setToComplete = true, mustReadToComplete = false)
        return api.updateLibManga(track)
    }

    override suspend fun add(track: Track): Track {
        track.score = DEFAULT_SCORE.toFloat()
        track.status = DEFAULT_STATUS
        updateNewTrackInfo(track)
        api.addLibManga(track)
        return update(track)
    }

    override suspend fun bind(track: Track): Track {
        val statusTrack = api.statusLibManga(track)
        val remoteTrack = api.findLibManga(track)
        return if (statusTrack != null && remoteTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            track.library_id = remoteTrack.library_id
            track.status = remoteTrack.status
            track.last_chapter_read = remoteTrack.last_chapter_read
            refresh(track)
        } else {
            add(track)
        }
    }

    override suspend fun search(query: String): List<TrackSearch> {
        return api.search(query)
    }

    override suspend fun refresh(track: Track): Track {
        val statusTrack = api.statusLibManga(track) ?: throw Exception("Could not find manga")
        track.copyPersonalFrom(statusTrack)
        val remoteTrack = api.findLibManga(track)
        if (remoteTrack != null) {
            track.total_chapters = remoteTrack.total_chapters
            track.status = remoteTrack.status
        }
        return track
    }

    override fun getLogo() = R.drawable.ic_tracker_bangumi

    override fun getTrackerColor() = Color.rgb(240, 147, 155)

    override fun getLogoColor() = Color.rgb(240, 145, 153)

    override fun getStatusList(): List<Int> {
        return listOf(READING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_READ)
    }

    override fun isCompletedStatus(index: Int) = getStatusList()[index] == COMPLETED

    override fun completedStatus(): Int = COMPLETED
    override fun readingStatus() = READING
    override fun planningStatus() = PLAN_TO_READ

    override fun getStatus(status: Int): String = with(context) {
        when (status) {
            READING -> getString(MR.strings.reading)
            PLAN_TO_READ -> getString(MR.strings.plan_to_read)
            COMPLETED -> getString(MR.strings.completed)
            ON_HOLD -> getString(MR.strings.on_hold)
            DROPPED -> getString(MR.strings.dropped)
            else -> ""
        }
    }

    override fun getGlobalStatus(status: Int): String = with(context) {
        when (status) {
            READING -> getString(MR.strings.reading)
            PLAN_TO_READ -> getString(MR.strings.plan_to_read)
            COMPLETED -> getString(MR.strings.completed)
            ON_HOLD -> getString(MR.strings.on_hold)
            DROPPED -> getString(MR.strings.dropped)
            else -> ""
        }
    }

    override suspend fun login(username: String, password: String) = login(password)

    suspend fun login(code: String): Boolean {
        try {
            val oauth = api.accessToken(code)
            interceptor.newAuth(oauth)
            saveCredentials(oauth.userId.toString(), oauth.accessToken)
            return true
        } catch (e: Exception) {
            Logger.e(e) { "Unable to login" }
            logout()
        }
        return false
    }

    fun saveToken(oauth: BGMOAuth?) {
        trackPreferences.trackToken(this).set(json.encodeToString(oauth))
    }

    fun restoreToken(): BGMOAuth? {
        return try {
            json.decodeFromString<BGMOAuth>(trackPreferences.trackToken(this).get())
        } catch (e: Exception) {
            null
        }
    }

    override fun logout() {
        super.logout()
        trackPreferences.trackToken(this).delete()
        interceptor.newAuth(null)
    }

    companion object {
        const val PLAN_TO_READ = 1
        const val COMPLETED = 2
        const val READING = 3
        const val ON_HOLD = 4
        const val DROPPED = 5

        const val DEFAULT_STATUS = READING
        const val DEFAULT_SCORE = 0
    }
}
