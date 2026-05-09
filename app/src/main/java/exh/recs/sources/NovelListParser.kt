package exh.recs.sources

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.lang.toNormalized
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import yokai.util.normalizedLevenshteinSimilarity
import java.util.Locale

/**
 * HTML parser for novellist.co. NovelList novel pages don't carry a per-entry "similar"
 * widget, but they do show user-curated lists that contain the novel — same situation as
 * NovelUpdates, so we use the same fallback shape: harvest the list links from the novel
 * page and treat the other entries on those lists as recommendations.
 */
internal object NovelListParser {
    private val titleCleanupRegex = Regex("[^\\p{L}\\p{N}]+")
    private val bracketRegex = Regex("\\([^)]*\\)|\\[[^\\]]*\\]|\\{[^}]*\\}|<[^>]*>")

    /** User-curated list URLs surfaced on a novel page. */
    fun parseUserListUrls(document: Document): List<String> {
        return document.select("a[href*=/users/][href*=/lists/]")
            .mapNotNull { link -> link.absUrl("href").takeIf(String::isNotBlank) }
            .distinctBy(::normalizeUrl)
    }

    /**
     * Novel links on a user-list page (or anywhere with `/novels/{slug}` anchors). Each
     * entry is rendered as two anchors sharing the same href — one wraps the cover `<img>`
     * (no visible text, generic `alt="Novel cover Image"`) and one wraps the title. We
     * group by normalised URL so the title-bearing anchor supplies the title and the
     * cover anchor still contributes a thumbnail; if we naively kept the first match the
     * alt text would leak through as every entry's title.
     */
    fun parseNovelLinks(document: Document, excludeUrl: String? = null): List<SManga> {
        val excludeNormalized = normalizeUrl(excludeUrl)
        val grouped = LinkedHashMap<String, MutableList<Element>>()
        for (link in document.select("a[href*=/novels/]")) {
            val href = link.absUrl("href").takeIf(String::isNotBlank) ?: continue
            if (!isNovelDetailUrl(href)) continue
            val key = normalizeUrl(href)
            if (key.isBlank() || key == excludeNormalized) continue
            grouped.getOrPut(key) { mutableListOf() }.add(link)
        }
        return grouped.values.mapNotNull(::mergeAnchorsToManga)
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

    /**
     * Match the best result from a NovelList API search payload (returns titles + slugs)
     * against the source title using the same Levenshtein-with-substring boost that
     * NovelUpdates uses, so a fuzzy English/romanised title still resolves to the right
     * series.
     */
    fun selectBestSearchResult(
        candidates: List<NovelListSearchResult>,
        query: String,
    ): NovelListSearchResult? {
        val normalizedQuery = normalizeTitle(query)
        if (normalizedQuery.isBlank()) return candidates.firstOrNull()

        return candidates
            .maxByOrNull { result -> similarityScore(normalizeTitle(result.title), normalizedQuery) }
            ?.takeIf { result ->
                similarityScore(normalizeTitle(result.title), normalizedQuery) >= MIN_SEARCH_SCORE
            }
    }

    private fun isNovelDetailUrl(href: String): Boolean {
        if (!href.contains("/novels/")) return false
        // Skip the "list of all novels by user" link and similar collection pages —
        // those don't have a slug after `/novels/`.
        val afterSegment = href.substringAfter("/novels/", "").trimEnd('/')
        return afterSegment.isNotBlank() && !afterSegment.contains('/')
    }

    private fun mergeAnchorsToManga(anchors: List<Element>): SManga? {
        val href = anchors.firstNotNullOfOrNull { it.absUrl("href").takeIf(String::isNotBlank) }
            ?: return null
        // Use the first anchor whose visible text is meaningful — the title anchor. The
        // cover anchor's only text candidate is the image's `alt`, which on NovelList is a
        // generic placeholder, so we deliberately don't fall back to it.
        val title = anchors.asSequence()
            .map { it.text().trim() }
            .firstOrNull(String::isNotBlank)
            ?: return null
        val thumbnail = anchors.asSequence()
            .mapNotNull { it.selectFirst("img")?.absUrl("src")?.ifBlank { null } }
            .firstOrNull()

        return SManga.create().also { manga ->
            manga.title = title
            manga.url = href
            manga.thumbnail_url = thumbnail
            manga.initialized = true
        }
    }

    fun normalizeUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        // Strip the `#uuid` fragment that NovelList tracker URLs carry, then normalise the
        // singular `/novel/` form NovelListApi stores into the plural `/novels/` form the
        // public site actually serves so equality checks across both forms work.
        return url.substringBefore('#')
            .replace("/novel/", "/novels/")
            .trimEnd('/')
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

    private const val MIN_SEARCH_SCORE = 0.45
}

internal data class NovelListSearchResult(
    val title: String,
    val slug: String,
)
