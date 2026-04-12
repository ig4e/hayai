package exh.recs.sources

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import eu.kanade.tachiyomi.domain.manga.models.Manga
import yokai.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

fun CatalogueSource.isComickSource() = name == "Comick"

class ComickPagingSource(
    manga: Manga?,
    private val comickSource: CatalogueSource,
) : RecommendationPagingSource(manga) {

    override val name: String
        get() = "Comick"

    override val category: StringResource
        get() = MR.strings.community_recommendations

    override val associatedSourceId: Long
        get() = comickSource.id

    private val client by lazy { Injekt.get<NetworkHelper>().client }
    private val thumbnailBaseUrl = "https://meo.comick.pictures/"

    override suspend fun requestNextPage(currentPage: Int): MangasPage {
        val mangaUrl = manga?.url ?: throw NoResultsException()

        val mangasPage = coroutineScope {
            val headers = Headers.Builder().apply {
                add("Referer", "api.comick.fun/")
                add("User-Agent", "Tachiyomi ${System.getProperty("http.agent")}")
            }

            // Comick extension populates the URL field with: '/comic/{hid}#'
            val url = "https://api.comick.fun/v1.0$mangaUrl".toHttpUrl()
                .newBuilder()
                .addQueryParameter("tachiyomi", "true")
                .build()

            val request = GET(url, headers.build())

            val data = client.newCall(request).awaitSuccess()
                .parseAs<JsonObject>()

            val recs = data["comic"]!!
                .jsonObject["recommendations"]!!
                .jsonArray
                .map { it.jsonObject["relates"]!! }
                .map { it.jsonObject }

            MangasPage(
                recs.map { rec ->
                    SManga.create().also { manga ->
                        manga.title = rec["title"]!!.jsonPrimitive.content
                        manga.url = "/comic/${rec["hid"]!!.jsonPrimitive.content}#"
                        manga.thumbnail_url = thumbnailBaseUrl + rec["md_covers"]!!
                            .jsonArray
                            .map { it.jsonObject["b2key"]!!.jsonPrimitive.content }
                            .first()
                        // Mark as uninitialized to force fetching missing metadata
                        manga.initialized = false
                    }
                },
                false,
            )
        }

        return mangasPage.takeIf { it.mangas.isNotEmpty() } ?: throw NoResultsException()
    }
}
