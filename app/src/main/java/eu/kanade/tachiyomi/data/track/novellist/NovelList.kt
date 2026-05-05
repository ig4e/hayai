package eu.kanade.tachiyomi.data.track.novellist

import android.content.Context
import android.graphics.Color
import android.util.Base64
import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.TrackContentType
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import yokai.i18n.MR
import yokai.util.lang.getString

/**
 * NovelList tracker — https://www.novellist.co (LNReader-compatible API).
 *
 * Logic ported line-by-line from Tsundoku; class structure adapted to Hayai's [TrackService].
 * The user logs in via WebView; the resulting JWT access token is stored as the tracker
 * password and used as a `Bearer` header by [NovelListApi].
 */
class NovelList(private val context: Context, id: Long) : TrackService(id) {

    companion object {
        const val READING = 1
        const val COMPLETED = 2
        const val ON_HOLD = 3
        const val DROPPED = 4
        const val PLAN_TO_READ = 5

        const val LOGIN_URL = "https://www.novellist.co/login"
    }

    override val supportedContentTypes: Set<TrackContentType> = setOf(TrackContentType.NOVEL)

    private val json by lazy { Json { ignoreUnknownKeys = true } }
    internal val api by lazy { NovelListApi(this, client, json) }

    override fun nameRes() = MR.strings.novellist

    override fun getLogo() = R.drawable.ic_tracker_novellist

    override fun getTrackerColor() = Color.rgb(83, 109, 254)

    override fun getLogoColor() = Color.rgb(83, 109, 254)

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

    internal fun mapStatusToApi(status: Int): String = when (status) {
        READING -> "IN_PROGRESS"
        COMPLETED -> "COMPLETED"
        ON_HOLD -> "PLANNED"
        DROPPED -> "DROPPED"
        PLAN_TO_READ -> "PLANNED"
        else -> "IN_PROGRESS"
    }

    internal fun mapStatusFromApi(status: String): Int = when (status) {
        "IN_PROGRESS" -> READING
        "COMPLETED" -> COMPLETED
        "PLANNED" -> PLAN_TO_READ
        "DROPPED" -> DROPPED
        else -> READING
    }

    /** Tsundoku-format `tracking_url`: `https://www.novellist.co/novel/<slug>#<uuid>`. */
    internal fun uuidFromTrack(track: Track): String =
        track.tracking_url.substringAfter("#", "")

    override suspend fun add(track: Track): Track {
        return bind(track)
    }

    override suspend fun update(track: Track, setToRead: Boolean): Track {
        updateTrackStatus(track, setToRead)
        api.update(track)
        return track
    }

    override suspend fun bind(track: Track): Track {
        if (track.status == 0) track.status = if (track.last_chapter_read > 0f) READING else PLAN_TO_READ
        api.bind(track)
        return track
    }

    override suspend fun search(query: String): List<TrackSearch> = api.search(query)

    override suspend fun refresh(track: Track): Track = api.refresh(track) ?: track

    /**
     * Decode the `novellist` cookie that the WebView login captures. Cookie format:
     * `base64-{base64 JSON containing access_token}`.
     */
    fun extractTokenFromCookie(cookieValue: String): String? = try {
        if (!cookieValue.startsWith("base64-")) {
            null
        } else {
            val payload = cookieValue.removePrefix("base64-")
            val decoded = String(Base64.decode(payload, Base64.DEFAULT), Charsets.UTF_8)
            val obj = json.decodeFromString<JsonObject>(decoded)
            obj["access_token"]?.jsonPrimitive?.contentOrNull
        }
    } catch (e: Exception) {
        Logger.e(e) { "NovelList: failed to extract token from cookie" }
        null
    }

    override suspend fun login(username: String, password: String): Boolean {
        if (password.isBlank()) return false
        saveCredentials(username.ifBlank { "NovelList User" }, password)
        return true
    }
}
