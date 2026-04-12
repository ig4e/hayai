package exh.recs.sources

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import eu.kanade.tachiyomi.domain.manga.models.Manga
import yokai.i18n.MR

class MyAnimeListPagingSource(manga: Manga?) : TrackerRecommendationPagingSource(
    "https://api.jikan.moe/v4/", manga,
) {
    override val name: String
        get() = "MyAnimeList"

    override val category: StringResource
        get() = MR.strings.community_recommendations

    override val associatedTrackerId: Long
        get() = trackManager.myAnimeList.id

    override suspend fun getRecsById(id: String): List<SManga> {
        val apiUrl = endpoint.toHttpUrl()
            .newBuilder()
            .addPathSegment("manga")
            .addPathSegment(id)
            .addPathSegment("recommendations")
            .build()

        val data = client.newCall(GET(apiUrl)).awaitSuccess().parseAs<JsonObject>()
        return data["data"]?.jsonArray
            ?.mapNotNull { element ->
                val rec = element.jsonObject["entry"]?.jsonObject ?: return@mapNotNull null
                val title = rec["title"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val url = rec["url"]?.jsonPrimitive?.content ?: return@mapNotNull null
                SManga.create().also { manga ->
                    manga.title = title
                    manga.url = url
                    manga.thumbnail_url = rec["images"]
                        ?.let(JsonElement::jsonObject)
                        ?.let(::getImage)
                    manga.initialized = true
                }
            } ?: emptyList()
    }

    fun getImage(imageObject: JsonObject): String? {
        return imageObject["webp"]
            ?.jsonObject
            ?.get("image_url")
            ?.jsonPrimitive
            ?.contentOrNull
            ?: imageObject["jpg"]
                ?.jsonObject
                ?.get("image_url")
                ?.jsonPrimitive
                ?.contentOrNull
    }

    override suspend fun getRecsBySearch(search: String): List<SManga> {
        val url = endpoint.toHttpUrl()
            .newBuilder()
            .addPathSegment("manga")
            .addQueryParameter("q", search)
            .build()

        val data = client.newCall(GET(url)).awaitSuccess()
            .parseAs<JsonObject>()
        val malId = data["data"]?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("mal_id")
            ?.jsonPrimitive
            ?.content
            ?: return emptyList()
        return getRecsById(malId)
    }
}
