package eu.kanade.tachiyomi.data.track.myanimelist

import android.content.Context
import android.graphics.Color
import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALOAuth
import eu.kanade.tachiyomi.data.track.updateNewTrackInfo
import eu.kanade.tachiyomi.util.system.e
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import yokai.util.koin.injectLazy
import yokai.i18n.MR
import yokai.util.lang.getString

class MyAnimeList(private val context: Context, id: Long) : TrackService(id) {

    private val json: Json by injectLazy()
    private val interceptor by lazy { MyAnimeListInterceptor(this) }
    private val api by lazy { MyAnimeListApi(client, interceptor) }

    override fun nameRes() = MR.strings.myanimelist

    override val supportsReadingDates: Boolean = true

    override fun getLogo() = R.drawable.ic_tracker_mal

    override fun getTrackerColor() = getLogoColor()

    override fun getLogoColor() = Color.rgb(46, 82, 162)

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

    override fun getStatusList(): List<Int> {
        return listOf(READING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_READ)
    }

    override fun isCompletedStatus(index: Int) = getStatusList()[index] == COMPLETED

    override fun completedStatus(): Int = COMPLETED
    override fun readingStatus() = READING
    override fun planningStatus() = PLAN_TO_READ

    override fun getScoreList(): ImmutableList<String> {
        return IntRange(0, 10).map(Int::toString).toImmutableList()
    }

    override fun displayScore(track: Track): String {
        return track.score.toInt().toString()
    }

    override suspend fun add(track: Track): Track {
        track.status = READING
        track.score = 0F
        updateNewTrackInfo(track)
        return api.updateItem(track)
    }

    override suspend fun update(track: Track, setToRead: Boolean): Track {
        updateTrackStatus(track, setToRead)
        return api.updateItem(track)
    }

    override suspend fun bind(track: Track): Track {
        val remoteTrack = api.findListItem(track)
        return if (remoteTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            update(track)
        } else {
            // Set default fields if it's not found in the list
            add(track)
        }
    }

    override fun canRemoveFromService(): Boolean = true

    override suspend fun removeFromService(track: Track): Boolean {
        return api.remove(track)
    }

    override suspend fun search(query: String): List<TrackSearch> {
        if (query.startsWith(SEARCH_ID_PREFIX)) {
            query.substringAfter(SEARCH_ID_PREFIX).toIntOrNull()?.let { id ->
                return listOf(api.getMangaDetails(id))
            }
        }

        if (query.startsWith(SEARCH_LIST_PREFIX)) {
            query.substringAfter(SEARCH_LIST_PREFIX).let { title ->
                return api.findListItems(title)
            }
        }

        return api.search(query)
    }

    override suspend fun refresh(track: Track): Track {
        return api.findListItem(track) ?: add(track)
    }

    override suspend fun login(username: String, password: String) = login(password)

    suspend fun login(authCode: String): Boolean {
        return try {
            val oauth = api.getAccessToken(authCode)
            interceptor.setAuth(oauth)
            val username = api.getCurrentUser()
            saveCredentials(username, oauth.accessToken)
            true
        } catch (e: Exception) {
            Logger.e(e) { "Unable to login" }
            logout()
            false
        }
    }

    override fun logout() {
        super.logout()
        trackPreferences.trackToken(this).delete()
        interceptor.setAuth(null)
    }

    fun getIfAuthExpired(): Boolean {
        return trackPreferences.trackAuthExpired(this).get()
    }

    fun setAuthExpired() {
        trackPreferences.trackAuthExpired(this).set(true)
    }

    fun saveOAuth(oAuth: MALOAuth?) {
        trackPreferences.trackToken(this).set(json.encodeToString(oAuth))
    }

    fun loadOAuth(): MALOAuth? {
        return try {
            json.decodeFromString<MALOAuth>(trackPreferences.trackToken(this).get())
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val READING = 1
        const val COMPLETED = 2
        const val ON_HOLD = 3
        const val DROPPED = 4
        const val PLAN_TO_READ = 6
        const val REREADING = 7

        const val DEFAULT_STATUS = READING
        const val DEFAULT_SCORE = 0

        private const val SEARCH_ID_PREFIX = "id:"
        private const val SEARCH_LIST_PREFIX = "my:"

        const val BASE_URL = "https://myanimelist.net"
        const val USER_SESSION_COOKIE = "MALSESSIONID"
        const val LOGGED_IN_COOKIE = "is_logged_in"
    }
}
