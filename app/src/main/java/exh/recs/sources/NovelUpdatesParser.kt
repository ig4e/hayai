package exh.recs.sources

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.lang.toNormalized
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import yokai.util.normalizedLevenshteinSimilarity
import java.util.Locale

internal data class NovelUpdatesSearchResult(
    val title: String,
    val url: String,
    val thumbnailUrl: String?,
)

internal object NovelUpdatesParser {
    private val titleCleanupRegex = Regex("[^\\p{L}\\p{N}]+")
    private val bracketRegex = Regex("\\([^)]*\\)|\\[[^]]*]|\\{[^}]*}|<[^>]*>")

    fun parseSearchResults(html: String, baseUrl: String = NovelUpdatesPagingSource.BASE_URL): List<NovelUpdatesSearchResult> {
        val document = Jsoup.parseBodyFragment(html, baseUrl)
        return document.select("li.search_li_results a.a_search[href*=/series/]")
            .mapNotNull(::parseSearchResultLink)
            .distinctBy { normalizeSeriesUrl(it.url) }
    }

    fun selectBestSearchResult(results: List<NovelUpdatesSearchResult>, query: String): NovelUpdatesSearchResult? {
        val normalizedQuery = normalizeTitle(query)
        if (normalizedQuery.isBlank()) return results.firstOrNull()

        return results
            .maxByOrNull { result ->
                similarityScore(normalizeTitle(result.title), normalizedQuery)
            }
            ?.takeIf { result ->
                similarityScore(normalizeTitle(result.title), normalizedQuery) >= MIN_SEARCH_SCORE
            }
    }

    fun buildSearchQueries(title: String): List<String> {
        val trimmed = title.trim()
        val cleaned = bracketRegex.replace(trimmed, " ")
            .replace(titleCleanupRegex, " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return listOf(
            trimmed,
            cleaned,
            trimmed.substringBefore(" - ").trim(),
            trimmed.substringBefore(":").trim(),
        )
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
    }

    fun buildSlugCandidates(title: String): List<String> {
        return buildSearchQueries(title)
            .map(::normalizeTitle)
            .map { it.replace(' ', '-') }
            .filter(String::isNotBlank)
            .distinct()
    }

    fun isValidSeriesPage(document: Document, query: String): Boolean {
        if (document.selectFirst(".page-404") != null) return false

        val candidates = buildList {
            document.selectFirst(".seriestitlenu")
                ?.text()
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?.let(::add)
            document.selectFirst("#editassociated")
                ?.wholeText()
                ?.lineSequence()
                ?.map(String::trim)
                ?.filter(String::isNotBlank)
                ?.forEach(::add)
        }

        val normalizedQuery = normalizeTitle(query)
        return candidates.any {
            similarityScore(normalizeTitle(it), normalizedQuery) >= MIN_PAGE_MATCH_SCORE
        }
    }

    fun parseSeriesRecommendations(document: Document, excludeUrl: String? = null): List<SManga> {
        return buildList {
            addAll(parseSeriesLinksFromSection(document, "Recommendations"))
            addAll(parseSeriesLinksFromSection(document, "Related Series"))
        }
            .distinctBy { normalizeSeriesUrl(it.url) }
            .filterNot { normalizeSeriesUrl(it.url) == normalizeSeriesUrl(excludeUrl) }
    }

    fun parseRecommendationListUrls(document: Document): List<String> {
        return parseLinksFromSection(document, "Recommendation Lists", "/viewlist/")
            .mapNotNull { link -> link.absUrl("href").takeIf(String::isNotBlank) }
            .distinctBy(::normalizeSeriesUrl)
    }

    fun parseRecommendationListEntries(document: Document, excludeUrl: String? = null): List<SManga> {
        return document.select("div.search_body_nu div.search_title a[href*=/series/]")
            .mapNotNull(::parseSeriesLink)
            .distinctBy { normalizeSeriesUrl(it.url) }
            .filterNot { normalizeSeriesUrl(it.url) == normalizeSeriesUrl(excludeUrl) }
    }

    private fun parseSearchResultLink(link: Element): NovelUpdatesSearchResult? {
        val url = link.absUrl("href").takeIf(String::isNotBlank) ?: return null
        val title = link.text().trim().takeIf(String::isNotBlank) ?: return null
        val thumbnailUrl = link.selectFirst("img")
            ?.absUrl("src")
            ?.ifBlank { null }
        return NovelUpdatesSearchResult(title = title, url = url, thumbnailUrl = thumbnailUrl)
    }

    private fun parseSeriesLinksFromSection(document: Document, title: String): List<SManga> {
        return parseLinksFromSection(document, title, "/series/")
            .mapNotNull(::parseSeriesLink)
    }

    private fun parseLinksFromSection(document: Document, title: String, hrefNeedle: String): List<Element> {
        val sectionHeader = document.select("h5.seriesother")
            .firstOrNull { it.ownText().trim().startsWith(title, ignoreCase = true) }
            ?: return emptyList()

        return buildList {
            var sibling = sectionHeader.nextElementSibling()
            while (sibling != null && sibling.tagName() != "h5") {
                if (sibling.tagName() == "a" && sibling.attr("href").contains(hrefNeedle)) {
                    add(sibling)
                }
                addAll(sibling.select("a[href*='$hrefNeedle']"))
                sibling = sibling.nextElementSibling()
            }
        }
            .distinctBy { normalizeSeriesUrl(it.absUrl("href")) }
    }

    private fun parseSeriesLink(link: Element): SManga? {
        val title = link.text().trim().takeIf(String::isNotBlank) ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank) ?: return null

        return SManga.create().also { manga ->
            manga.title = title
            manga.url = url
            manga.initialized = true
        }
    }

    private fun similarityScore(lhs: String, rhs: String): Double {
        if (lhs.isBlank() || rhs.isBlank()) return 0.0
        if (lhs == rhs) return 1.0

        val baseScore = normalizedLevenshteinSimilarity(lhs, rhs)
        val containsBoost = if (lhs.contains(rhs) || rhs.contains(lhs)) 0.1 else 0.0
        return (baseScore + containsBoost).coerceAtMost(1.0)
    }

    private fun normalizeTitle(title: String): String {
        return title.toNormalized()
            .lowercase(Locale.ROOT)
            .replace(bracketRegex, " ")
            .replace(titleCleanupRegex, " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun normalizeSeriesUrl(url: String?): String {
        return url
            ?.substringBefore('#')
            ?.trimEnd('/')
            .orEmpty()
    }

    private const val MIN_SEARCH_SCORE = 0.45
    private const val MIN_PAGE_MATCH_SCORE = 0.55
}
