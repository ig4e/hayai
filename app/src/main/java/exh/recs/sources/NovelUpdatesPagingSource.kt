package exh.recs.sources

import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class NovelUpdatesPagingSource(manga: Manga?) : RecommendationPagingSource(manga) {
    override val name: String = "NovelUpdates"

    private val client by lazy { Injekt.get<NetworkHelper>().client }

    override suspend fun requestNextPage(currentPage: Int): MangasPage {
        if (currentPage != 1) {
            return MangasPage(emptyList(), false)
        }

        val query = manga?.title?.takeIf { it.isNotBlank() } ?: throw NoResultsException()
        val url = "https://www.novelupdates.com/".toHttpUrl()
            .newBuilder()
            .addQueryParameter("s", query)
            .addQueryParameter("post_type", "seriesplans")
            .build()

        val response = client.newCall(GET(url)).awaitSuccess()
        val html = response.body.string()
        val results = parseResults(Jsoup.parse(html, url.toString()))

        if (results.isEmpty()) {
            throw NoResultsException()
        }

        return MangasPage(results, false)
    }

    private fun parseResults(document: Document): List<SManga> {
        val containerResults = document.select("div.search_main_box_nu, div.search_body_nu, div.search_body")
            .mapNotNull(::parseResultContainer)

        val results = containerResults.ifEmpty {
            document.select("a[href*=/series/]")
                .mapNotNull { link -> parseResultLink(link, link.parent()) }
        }

        return results
            .distinctBy { it.url }
            .take(MAX_RESULTS)
    }

    private fun parseResultContainer(container: Element): SManga? {
        val link = container.selectFirst("a[href*=/series/][title], a[href*=/series/]")
            ?: return null
        return parseResultLink(link, container)
    }

    private fun parseResultLink(link: Element, container: Element?): SManga? {
        val title = link.attr("title").ifBlank { link.text() }.trim()
        val url = link.absUrl("href").takeIf { it.isNotBlank() } ?: return null
        if (title.isBlank()) return null

        return SManga.create().also { manga ->
            manga.title = title
            manga.url = url
            manga.thumbnail_url = container
                ?.selectFirst("img")
                ?.absUrl("src")
                ?.takeIf { it.isNotBlank() }
            manga.initialized = true
        }
    }

    private companion object {
        const val MAX_RESULTS = 20
    }
}
