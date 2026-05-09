package exh.recs.sources

import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.model.MangasPage
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.FormBody
import okhttp3.Headers
import org.jsoup.Jsoup
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import yokai.domain.track.interactor.GetTrack

class NovelUpdatesPagingSource(manga: Manga?) : RecommendationPagingSource(manga) {
    override val name: String = "NovelUpdates"

    // NovelUpdates only catalogues novels, so this source is always novel-typed.
    override val contentType: RecommendationContentType = RecommendationContentType.NOVEL

    private val client by lazy { Injekt.get<NetworkHelper>().client }
    private val getTrack: GetTrack by injectLazy()
    private val trackManager: TrackManager by injectLazy()

    override suspend fun requestNextPage(currentPage: Int): MangasPage {
        if (currentPage != 1) {
            return MangasPage(emptyList(), false)
        }

        // Prefer the URL from a linked NovelUpdates tracker — it's exact and skips the
        // ajax search + slug-guess round trip. The title-search fallback handles entries
        // without a tracker (or with a stale/empty tracking_url).
        val seriesUrl = resolveSeriesUrlFromTracker()
            ?: run {
                val query = manga?.title?.takeIf { it.isNotBlank() } ?: throw NoResultsException()
                resolveSeriesUrl(query)
            }
            ?: throw NoResultsException()
        val seriesDocument = fetchDocument(seriesUrl)

        val results = buildList {
            addAll(NovelUpdatesParser.parseSeriesRecommendations(seriesDocument, excludeUrl = seriesUrl))
            if (isEmpty()) {
                for (recommendationListUrl in NovelUpdatesParser.parseRecommendationListUrls(seriesDocument)
                    .take(MAX_RECOMMENDATION_LISTS)
                ) {
                    runCatching {
                        NovelUpdatesParser.parseRecommendationListEntries(
                            fetchDocument(recommendationListUrl),
                            excludeUrl = seriesUrl,
                        )
                    }.getOrNull()?.let(::addAll)

                    if (size >= MAX_RESULTS) {
                        break
                    }
                }
            }
        }
            .distinctBy { it.url }
            .take(MAX_RESULTS)

        if (results.isEmpty()) {
            throw NoResultsException()
        }

        return MangasPage(results, false)
    }

    /**
     * Look up a linked NovelUpdates tracker for the source manga and return its
     * `tracking_url` if present and recognisably a NovelUpdates series URL. Returns null
     * when there's no manga id, no NU track, or the URL doesn't look like one we can use.
     */
    private suspend fun resolveSeriesUrlFromTracker(): String? {
        val mangaId = manga?.id ?: return null
        val tracks = getTrack.awaitAllByMangaId(mangaId)
        val nuTrack = tracks.find { it.sync_id == trackManager.novelUpdates.id } ?: return null
        val url = nuTrack.tracking_url.takeIf { it.isNotBlank() } ?: return null
        return url.takeIf { it.contains("novelupdates.com/series/") }
    }

    private suspend fun resolveSeriesUrl(query: String): String? {
        NovelUpdatesParser.buildSearchQueries(query).forEach { searchQuery ->
            val searchResults = runCatching {
                val body = FormBody.Builder()
                    .add("action", "nd_ajaxsearchmain")
                    .add("strType", "desktop")
                    .add("strOne", searchQuery)
                    .add("strSearchType", "series")
                    .build()

                client.newCall(
                    POST(
                        SEARCH_ENDPOINT,
                        headers = ajaxHeaders,
                        body = body,
                    ),
                ).awaitSuccess().use { response ->
                    NovelUpdatesParser.parseSearchResults(response.body.string())
                }
            }.getOrDefault(emptyList())

            val bestMatch = NovelUpdatesParser.selectBestSearchResult(searchResults, query)
            if (bestMatch != null) {
                return bestMatch.url
            }
        }

        NovelUpdatesParser.buildSlugCandidates(query).forEach { slug ->
            val url = BASE_URL.toHttpUrl()
                .newBuilder()
                .addPathSegment("series")
                .addPathSegment(slug)
                .build()
                .toString()

            val document = runCatching { fetchDocument(url) }.getOrNull() ?: return@forEach
            if (NovelUpdatesParser.isValidSeriesPage(document, query)) {
                return url
            }
        }

        return null
    }

    private suspend fun fetchDocument(url: String) = client.newCall(GET(url, ajaxHeaders))
        .awaitSuccess()
        .use { response ->
            Jsoup.parse(response.body.string(), response.request.url.toString())
        }

    private val ajaxHeaders by lazy {
        Headers.Builder()
            .add("Referer", BASE_URL)
            .add("X-Requested-With", "XMLHttpRequest")
            .build()
    }

    companion object {
        const val BASE_URL = "https://www.novelupdates.com/"

        private const val SEARCH_ENDPOINT = "${BASE_URL}wp-admin/admin-ajax.php"
        const val MAX_RESULTS = 20
        private const val MAX_RECOMMENDATION_LISTS = 3
    }
}
