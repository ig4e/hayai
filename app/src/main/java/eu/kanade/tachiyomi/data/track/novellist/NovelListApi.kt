package eu.kanade.tachiyomi.data.track.novellist

import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Network layer for [NovelList]. Mirrors Tsundoku's network code line-by-line; uses Hayai's
 * `Track.media_id` (Long hash) for table joins while keeping the tracking URL as the canonical
 * source of the API UUID.
 */
class NovelListApi(
    private val tracker: NovelList,
    private val client: OkHttpClient,
    private val json: Json,
) {

    private val baseUrl = "https://novellist-be-960019704910.asia-east1.run.app"

    private fun authBuilder(url: String): Request.Builder {
        val token = tracker.run { trackPreferences.trackPassword(this).get() }
        return Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "*/*")
            .addHeader("Accept-Language", "en-US,en;q=0.5")
            .addHeader("Origin", "https://www.novellist.co")
            .addHeader("Referer", "https://www.novellist.co/")
            .addHeader("Sec-Fetch-Dest", "empty")
            .addHeader("Sec-Fetch-Mode", "cors")
            .addHeader("Sec-Fetch-Site", "cross-site")
    }

    /** Best-effort CORS preflight; some Cloud Run setups require it. */
    private suspend fun sendOptions(url: String, method: String) {
        try {
            val request = Request.Builder()
                .url(url)
                .method("OPTIONS", null)
                .addHeader("Accept", "*/*")
                .addHeader("Access-Control-Request-Method", method)
                .addHeader("Access-Control-Request-Headers", "authorization,content-type")
                .addHeader("Origin", "https://www.novellist.co")
                .addHeader("Referer", "https://www.novellist.co/")
                .addHeader("Sec-Fetch-Dest", "empty")
                .addHeader("Sec-Fetch-Mode", "cors")
                .addHeader("Sec-Fetch-Site", "cross-site")
                .build()
            client.newCall(request).awaitSuccess()
        } catch (e: Exception) {
            Logger.d { "NovelList: OPTIONS preflight failed (continuing): ${e.message}" }
        }
    }

    suspend fun update(track: Track) {
        val uuid = tracker.uuidFromTrack(track)
        if (uuid.isEmpty()) return
        val url = "$baseUrl/api/users/current/reading-list/$uuid"
        sendOptions(url, "PUT")
        val body = buildJsonObject {
            put("chapter_count", track.last_chapter_read.toInt())
            put("status", tracker.mapStatusToApi(track.status))
            if (track.score > 0f) put("rating", track.score.toInt())
        }.toString().toRequestBody("application/json".toMediaType())
        val request = authBuilder(url).put(body).build()
        client.newCall(request).awaitSuccess()
    }

    suspend fun bind(track: Track) {
        val uuid = tracker.uuidFromTrack(track)
        if (uuid.isEmpty()) return
        val url = "$baseUrl/api/users/current/reading-list/$uuid"
        sendOptions(url, "PUT")
        val body = buildJsonObject {
            put("status", if (track.last_chapter_read > 0f) "IN_PROGRESS" else "PLANNED")
            put("chapter_count", track.last_chapter_read.toInt())
            put("rating", 0)
            put("note", "")
        }.toString().toRequestBody("application/json".toMediaType())
        client.newCall(authBuilder(url).put(body).build()).awaitSuccess()
    }

    suspend fun search(query: String): List<TrackSearch> {
        val body = buildJsonObject {
            put("page", 1)
            put("sort_order", "MOST_TRENDING")
            put("title_search_query", query)
            put("language", "UNKNOWN")
            putJsonArray("label_ids") {}
            putJsonArray("excluded_label_ids") {}
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/api/novels/filter")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .addHeader("Origin", "https://www.novellist.co")
            .addHeader("Referer", "https://www.novellist.co/")
            .build()

        return try {
            val response = client.newCall(request).awaitSuccess()
            val text = response.body.string()
            val list = json.decodeFromString<List<JsonObject>>(text)
            list.map { obj ->
                val track = TrackSearch.create(tracker.id)
                val idStr = obj["id"]?.jsonPrimitive?.contentOrNull ?: ""
                track.media_id = idStr.hashCode().toLong().let { if (it < 0) -it else it }
                track.title = obj["english_title"]?.jsonPrimitive?.contentOrNull
                    ?: obj["raw_title"]?.jsonPrimitive?.contentOrNull
                    ?: obj["title"]?.jsonPrimitive?.contentOrNull
                    ?: ""
                track.cover_url = obj["cover_image_link"]?.jsonPrimitive?.contentOrNull
                    ?: obj["image_url"]?.jsonPrimitive?.contentOrNull
                    ?: ""
                track.summary = obj["description"]?.jsonPrimitive?.contentOrNull ?: ""
                val slug = obj["slug"]?.jsonPrimitive?.contentOrNull ?: idStr
                track.tracking_url = "https://www.novellist.co/novel/$slug#$idStr"
                track.publishing_status = obj["status"]?.jsonPrimitive?.contentOrNull ?: ""
                track
            }
        } catch (e: Exception) {
            Logger.e(e) { "NovelList: search failed" }
            emptyList()
        }
    }

    suspend fun refresh(track: Track): Track? {
        val uuid = tracker.uuidFromTrack(track)
        if (uuid.isEmpty()) return null
        val url = "$baseUrl/api/users/current/reading-list/$uuid"
        return try {
            val response = client.newCall(authBuilder(url).get().build()).awaitSuccess()
            val obj = response.parseAs<JsonObject>()
            track.status = tracker.mapStatusFromApi(obj["status"]?.jsonPrimitive?.contentOrNull ?: "IN_PROGRESS")
            track.last_chapter_read = obj["chapter_count"]?.jsonPrimitive?.contentOrNull?.toFloatOrNull() ?: 0f
            track.score = obj["rating"]?.jsonPrimitive?.contentOrNull?.toFloatOrNull() ?: 0f
            track
        } catch (e: Exception) {
            Logger.e(e) { "NovelList: refresh failed" }
            null
        }
    }
}
