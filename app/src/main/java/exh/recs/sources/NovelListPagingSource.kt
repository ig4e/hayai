package exh.recs.sources

import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.source.model.MangasPage
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import yokai.domain.track.interactor.GetTrack

/**
 * Recommendation source backed by novellist.co. NovelList groups novels into user-curated
 * lists rather than per-entry "similar" widgets, so we resolve the source novel's page and
 * walk the lists it appears in to surface related novels — same fallback shape as
 * [NovelUpdatesPagingSource]. If a NovelList tracker is linked we skip the search step.
 */
class NovelListPagingSource(manga: Manga?) : RecommendationPagingSource(manga) {
    override val name: String = "NovelList"

    // NovelList only catalogues novels.
    override val contentType: RecommendationContentType = RecommendationContentType.NOVEL

    private val client by lazy { Injekt.get<NetworkHelper>().client }
    private val getTrack: GetTrack by injectLazy()
    private val trackManager: TrackManager by injectLazy()

    override suspend fun requestNextPage(currentPage: Int): MangasPage {
        if (currentPage != 1) {
            return MangasPage(emptyList(), false)
        }

        val novelUrl = resolveNovelUrlFromTracker()
            ?: run {
                val query = manga?.title?.takeIf { it.isNotBlank() } ?: throw NoResultsException()
                resolveNovelUrlBySearch(query)
            }
            ?: throw NoResultsException()

        val novelDocument = fetchDocument(novelUrl)
        val excludeNormalized = NovelListParser.normalizeUrl(novelUrl)

        val results = buildList {
            for (listUrl in NovelListParser.parseUserListUrls(novelDocument).take(MAX_LISTS)) {
                runCatching {
                    NovelListParser.parseNovelLinks(fetchDocument(listUrl), excludeUrl = excludeNormalized)
                }.getOrNull()?.let(::addAll)
                if (size >= MAX_RESULTS) break
            }
        }
            .distinctBy { NovelListParser.normalizeUrl(it.url) }
            .take(MAX_RESULTS)

        if (results.isEmpty()) {
            throw NoResultsException()
        }

        return MangasPage(results, false)
    }

    /** Pull the novel URL from a linked NovelList tracker; null when there's no usable URL. */
    private suspend fun resolveNovelUrlFromTracker(): String? {
        val mangaId = manga?.id ?: return null
        val tracks = getTrack.awaitAllByMangaId(mangaId)
        val track = tracks.find { it.sync_id == trackManager.novelList.id } ?: return null
        val raw = track.tracking_url.takeIf { it.isNotBlank() } ?: return null
        // The tracker stores `https://www.novellist.co/novel/{slug}#{uuid}`; the public
        // site actually serves the plural `/novels/` path. Normalise so the fetch succeeds.
        return raw.substringBefore('#')
            .replace("/novel/", "/novels/")
            .takeIf { it.contains("novellist.co/novels/") }
    }

    /**
     * Title-search NovelList via its filter API to find a matching novel slug. We rely on
     * the same backend the NovelList tracker uses — public, no auth required for search.
     */
    private suspend fun resolveNovelUrlBySearch(query: String): String? {
        for (search in NovelListParser.buildSearchQueries(query)) {
            val results = runCatching { searchNovels(search) }.getOrDefault(emptyList())
            val best = NovelListParser.selectBestSearchResult(results, query)
            if (best != null) {
                return "$BASE_URL/novels/${best.slug}"
            }
        }
        return null
    }

    private suspend fun searchNovels(query: String): List<NovelListSearchResult> {
        val payload = buildJsonObject {
            put("page", 1)
            put("sort_order", "MOST_TRENDING")
            put("title_search_query", query)
            put("language", "UNKNOWN")
            putJsonArray("label_ids") {}
            putJsonArray("excluded_label_ids") {}
        }.toString().toRequestBody("application/json".toMediaType())

        val request = POST(
            "$API_BASE/api/novels/filter",
            headers = apiHeaders,
            body = payload,
        )
        val response = client.newCall(request).awaitSuccess()
        val raw = response.parseAs<List<JsonObject>>()
        return raw.mapNotNull { obj ->
            val title = obj["english_title"]?.jsonPrimitive?.contentOrNull
                ?: obj["raw_title"]?.jsonPrimitive?.contentOrNull
                ?: obj["title"]?.jsonPrimitive?.contentOrNull
                ?: return@mapNotNull null
            val slug = obj["slug"]?.jsonPrimitive?.contentOrNull
                ?: return@mapNotNull null
            NovelListSearchResult(title = title, slug = slug)
        }
    }

    private suspend fun fetchDocument(url: String) = client.newCall(GET(url, browserHeaders))
        .awaitSuccess()
        .use { response ->
            Jsoup.parse(response.body.string(), response.request.url.toString())
        }

    private val apiHeaders: Headers by lazy {
        Headers.Builder()
            .add("Content-Type", "application/json")
            .add("Accept", "application/json")
            .add("Origin", BASE_URL)
            .add("Referer", "$BASE_URL/")
            .build()
    }

    private val browserHeaders: Headers by lazy {
        Headers.Builder()
            .add("Referer", "$BASE_URL/")
            .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()
    }

    companion object {
        private const val BASE_URL = "https://www.novellist.co"
        // Same Cloud Run backend the NovelList tracker hits; the search endpoint is public.
        private const val API_BASE = "https://novellist-be-960019704910.asia-east1.run.app"
        private const val MAX_RESULTS = 20
        private const val MAX_LISTS = 4
    }
}
