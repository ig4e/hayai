package exh.recs.sources

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.database.models.seriesType
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import eu.kanade.tachiyomi.domain.manga.models.Manga
import yokai.i18n.MR

class AniListPagingSource(manga: Manga?) : TrackerRecommendationPagingSource(
    "https://graphql.anilist.co/", manga,
) {
    override val name: String
        get() = "AniList"

    override val category: StringResource
        get() = MR.strings.community_recommendations

    override val associatedTrackerId: Long
        get() = trackManager.aniList.id

    // AniList's MediaType.MANGA covers manga, manhwa, manhua, oneshots AND light novels
    // (NOVEL is a MediaFormat under it). We want to surface only the format that matches
    // the entry the user is browsing — otherwise novel readers see manga recs (and vice
    // versa) since the unfiltered query mixes them. Both the source-entry lookup and the
    // recommendations get filtered using the same flag.
    private val isNovel: Boolean = manga?.seriesType() == Manga.TYPE_NOVEL

    override val contentType: RecommendationContentType =
        if (isNovel) RecommendationContentType.NOVEL else RecommendationContentType.MANGA

    private val formatFilterFragment: String =
        if (isNovel) "format_in: [NOVEL]" else "format_not_in: [NOVEL]"

    private fun countOccurrence(arr: JsonArray, search: String): Int {
        return arr.count {
            val synonym = it.jsonPrimitive.content
            synonym.contains(search, true)
        }
    }

    private fun languageContains(obj: JsonObject, language: String, search: String): Boolean {
        return obj["title"]?.jsonObject?.get(language)?.jsonPrimitive?.contentOrNull?.contains(search, true) == true
    }

    private fun getTitle(obj: JsonObject): String {
        val titleObj = obj["title"]!!.jsonObject

        val english = titleObj["english"]?.jsonPrimitive?.contentOrNull
        val romaji = titleObj["romaji"]?.jsonPrimitive?.contentOrNull
        val native = titleObj["native"]?.jsonPrimitive?.contentOrNull
        val synonym = obj["synonyms"]!!.jsonArray.getOrNull(0)?.jsonPrimitive?.contentOrNull

        val isJP = obj["countryOfOrigin"]?.jsonPrimitive?.contentOrNull == "JP"

        return when {
            !english.isNullOrBlank() -> english
            isJP && !romaji.isNullOrBlank() -> romaji
            !synonym.isNullOrBlank() -> synonym
            !isJP && !romaji.isNullOrBlank() -> romaji
            else -> native ?: "NO NAME FOUND"
        }
    }

    /**
     * Drop recommendations whose format doesn't match the active scope. Defensive belt
     * against AniList returning a NOVEL recommendation off a MANGA entry (or vice versa)
     * even when the source query was filtered — the recommendations field is independent.
     */
    private fun matchesScope(rec: JsonObject): Boolean {
        val format = rec["format"]?.jsonPrimitive?.contentOrNull
        return if (isNovel) format == "NOVEL" else format != "NOVEL"
    }

    private suspend fun getRecs(
        query: String,
        variables: JsonObject,
        queryParam: String? = null,
        filter: List<kotlinx.serialization.json.JsonElement>.() -> List<kotlinx.serialization.json.JsonElement> = { this },
    ): List<SManga> {
        val payload = buildJsonObject {
            put("query", query)
            put("variables", variables)
        }
        val payloadBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val data = client.newCall(POST(endpoint, body = payloadBody)).awaitSuccess()
            .parseAs<JsonObject>()

        val media = data["data"]!!
            .jsonObject["Page"]!!
            .jsonObject["media"]!!
            .jsonArray
            .ifEmpty { throw NoResultsException() }
            .filter()

        return media.flatMap { it.jsonObject["recommendations"]!!.jsonObject["edges"]!!.jsonArray }
            .mapNotNull {
                val rec = it.jsonObject["node"]!!.jsonObject["mediaRecommendation"]!!.jsonObject
                if (!matchesScope(rec)) return@mapNotNull null
                val recTitle = getTitle(rec)
                SManga.create().also { manga ->
                    manga.title = recTitle
                    manga.thumbnail_url = rec["coverImage"]!!.jsonObject["large"]!!.jsonPrimitive.content
                    manga.initialized = true
                    manga.url = rec["siteUrl"]!!.jsonPrimitive.content
                }
            }
    }

    override suspend fun getRecsById(id: String): List<SManga> {
        val query =
            """
            |query Recommendations(${'$'}id: Int!) {
                |Page {
                    |media(id: ${'$'}id, type: MANGA) {
                        |format
                        |recommendations {
                            |edges {
                                |node {
                                    |mediaRecommendation {
                                        |format
                                        |countryOfOrigin
                                        |siteUrl
                                        |title {
                                            |romaji
                                            |english
                                            |native
                                        |}
                                        |synonyms
                                        |coverImage {
                                            |large
                                        |}
                                    |}
                                |}
                            |}
                        |}
                    |}
                |}
            |}
            |
            """.trimMargin()
        val variables = buildJsonObject {
            put("id", id)
        }

        return getRecs(
            query = query,
            variables = variables,
        )
    }

    override suspend fun getRecsBySearch(search: String): List<SManga> {
        val query =
            """
            |query Recommendations(${'$'}search: String!) {
                |Page {
                    |media(search: ${'$'}search, type: MANGA, $formatFilterFragment) {
                        |format
                        |title {
                            |romaji
                            |english
                            |native
                        |}
                        |synonyms
                        |recommendations {
                            |edges {
                                |node {
                                    |mediaRecommendation {
                                        |format
                                        |countryOfOrigin
                                        |siteUrl
                                        |title {
                                            |romaji
                                            |english
                                            |native
                                        |}
                                        |synonyms
                                        |coverImage {
                                            |large
                                        |}
                                    |}
                                |}
                            |}
                        |}
                    |}
                |}
            |}
            |
            """.trimMargin()
        val variables = buildJsonObject {
            put("search", search)
        }
        return getRecs(
            queryParam = search,
            query = query,
            variables = variables,
            filter = {
                filter {
                    val jsonObject = it.jsonObject
                    languageContains(jsonObject, "romaji", search) ||
                        languageContains(jsonObject, "english", search) ||
                        languageContains(jsonObject, "native", search) ||
                        countOccurrence(jsonObject["synonyms"]!!.jsonArray, search) > 0
                }
            },
        )
    }
}
