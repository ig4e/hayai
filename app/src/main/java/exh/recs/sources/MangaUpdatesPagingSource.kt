package exh.recs.sources

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.database.models.seriesType
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import eu.kanade.tachiyomi.domain.manga.models.Manga
import yokai.i18n.MR

abstract class MangaUpdatesPagingSource(manga: Manga?) : TrackerRecommendationPagingSource(
    "https://api.mangaupdates.com/v1/", manga,
) {
    override val associatedTrackerId: Long
        get() = trackManager.mangaUpdates.id

    // MangaUpdates indexes light novels under the same `/series/` namespace as manga, so
    // the by-id and by-search endpoints work for either type — but a title search without
    // a type filter would happily resolve a novel title to the manga adaptation. Detect
    // novel scope from the source entry and bias both the search filter and the badge.
    private val isNovel: Boolean = manga?.seriesType() == Manga.TYPE_NOVEL

    override val contentType: RecommendationContentType =
        if (isNovel) RecommendationContentType.NOVEL else RecommendationContentType.MANGA

    protected abstract val recommendationJsonObjectName: String

    override suspend fun getRecsById(id: String): List<SManga> {
        val apiUrl = endpoint.toHttpUrl()
            .newBuilder()
            .addPathSegment("series")
            .addPathSegment(id)
            .build()

        val data = client.newCall(GET(apiUrl)).awaitSuccess().parseAs<JsonObject>()
        return getRecommendations(data[recommendationJsonObjectName]!!.jsonArray)
    }

    private fun getRecommendations(recommendations: JsonArray): List<SManga> {
        return recommendations
            .map(JsonElement::jsonObject)
            .map { rec ->
                SManga.create().also { manga ->
                    manga.title = rec["series_name"]!!.jsonPrimitive.content
                    manga.url = rec["series_url"]!!.jsonPrimitive.content
                    manga.thumbnail_url = rec["series_image"]
                        ?.jsonObject
                        ?.get("url")
                        ?.jsonObject
                        ?.get("original")
                        ?.jsonPrimitive
                        ?.contentOrNull
                    manga.initialized = true
                }
            }
    }

    override suspend fun getRecsBySearch(search: String): List<SManga> {
        val url = endpoint.toHttpUrl()
            .newBuilder()
            .addPathSegments("series/search")
            .build()
            .toString()

        val payload = buildJsonObject {
            put("search", search)
            put("stype", "title")
            // The v1 search endpoint accepts a `type` array; restrict to "Novel" for novel
            // scope so the title lookup doesn't land on the manga adaptation.
            if (isNovel) {
                putJsonArray("type") { add(JsonPrimitive("Novel")) }
            }
        }

        val body = payload
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val data = client.newCall(POST(url, body = body))
            .awaitSuccess()
            .parseAs<JsonObject>()
        return getRecsById(
            data["results"]!!
                .jsonArray
                .ifEmpty { throw NoResultsException() }
                .first()
                .jsonObject["record"]!!
                .jsonObject["series_id"]!!
                .jsonPrimitive.content,
        )
    }
}

class MangaUpdatesCommunityPagingSource(manga: Manga?) : MangaUpdatesPagingSource(manga) {
    // Disambiguate from the sibling source: both consume the same MangaUpdates endpoint
    // but expose different recommendation lists (`recommendations` vs `category_recommendations`).
    // Showing two cards titled plainly "MangaUpdates" reads as an accidental duplicate.
    override val name: String
        get() = "MangaUpdates – Community"
    override val category: StringResource
        get() = MR.strings.community_recommendations
    override val recommendationJsonObjectName: String
        get() = "recommendations"
}

class MangaUpdatesSimilarPagingSource(manga: Manga?) : MangaUpdatesPagingSource(manga) {
    override val name: String
        get() = "MangaUpdates – Similar"
    override val category: StringResource
        get() = MR.strings.similar_titles
    override val recommendationJsonObjectName: String
        get() = "category_recommendations"
}
