package exh.smartsearch

import info.debatty.java.stringsimilarity.NormalizedLevenshtein
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

typealias SearchAction<T> = suspend (String) -> List<T>

abstract class BaseSmartSearchEngine<T>(
    private val extraSearchParams: String? = null,
    private val eligibleThreshold: Double = MIN_ELIGIBLE_THRESHOLD,
) {
    private val normalizedLevenshtein = NormalizedLevenshtein()

    // Cache for cleaned titles to avoid redundant processing
    private val cleanedTitleCache = ConcurrentHashMap<String, String>()

    // Cache for search results to avoid redundant API calls
    private val searchResultsCache = ConcurrentHashMap<String, List<T>>()

    protected abstract fun getTitle(result: T): String

    protected suspend fun smartSearch(searchAction: SearchAction<T>, title: String): T? {
        // Get or compute cleaned title with caching
        val cleanedTitle = cleanedTitleCache.getOrPut(title) { cleanSmartSearchTitle(title) }

        val queries = getSmartSearchQueries(cleanedTitle)

        // If the title is very short, use more precise matching to avoid false positives
        val effectiveThreshold = if (cleanedTitle.length <= 5) eligibleThreshold + 0.2 else eligibleThreshold

        val eligibleManga = supervisorScope {
            // Process only first 3 queries for performance if title is long enough
            val limitedQueries = if (cleanedTitle.length > 10) queries.take(3) else queries

            limitedQueries.map { query ->
                async(Dispatchers.Default) {
                    // Check cache first before making network call
                    val builtQuery = if (extraSearchParams != null) {
                        "$query ${extraSearchParams.trim()}"
                    } else {
                        query
                    }

                    // Get search results with caching
                    val searchResults = searchResultsCache.getOrPut(builtQuery) {
                        searchAction(builtQuery)
                    }

                    if (searchResults.isEmpty()) return@async emptyList()

                    // Process in batches for better performance
                    withContext(Dispatchers.Default) {
                        searchResults.map {
                            val mangaTitle = getTitle(it)
                            val cleanedMangaTitle = cleanedTitleCache.getOrPut(mangaTitle) {
                                cleanSmartSearchTitle(mangaTitle)
                            }
                            val normalizedDistance = normalizedLevenshtein.similarity(cleanedTitle, cleanedMangaTitle)
                            SearchEntry(it, normalizedDistance)
                        }.filter { (_, normalizedDistance) ->
                            normalizedDistance >= effectiveThreshold
                        }
                    }
                }
            }.flatMap { it.await() }
        }

        // Early return if we found exact match
        val exactMatch = eligibleManga.find { it.dist > 0.9 }
        if (exactMatch != null) return exactMatch.manga

        return eligibleManga.maxByOrNull { it.dist }?.manga
    }

    protected suspend fun normalSearch(searchAction: SearchAction<T>, title: String): T? {
        val eligibleManga = supervisorScope {
            val searchQuery = if (extraSearchParams != null) {
                "$title ${extraSearchParams.trim()}"
            } else {
                title
            }

            // Use cache for search results
            val searchResults = searchResultsCache.getOrPut(searchQuery) {
                searchAction(searchQuery)
            }

            if (searchResults.isEmpty()) {
                return@supervisorScope emptyList()
            }

            // Early return for single result
            if (searchResults.size == 1) {
                return@supervisorScope listOf(SearchEntry(searchResults.first(), 0.0))
            }

            // Batch process all results
            withContext(Dispatchers.Default) {
                searchResults.map {
                    val mangaTitle = getTitle(it)
                    val normalizedDistance = normalizedLevenshtein.similarity(title, mangaTitle)
                    SearchEntry(it, normalizedDistance)
                }.filter { (_, normalizedDistance) ->
                    normalizedDistance >= eligibleThreshold
                }
            }
        }

        // Early return if we found exact match
        val exactMatch = eligibleManga.find { it.dist > 0.9 }
        if (exactMatch != null) return exactMatch.manga

        return eligibleManga.maxByOrNull { it.dist }?.manga
    }

    private fun cleanSmartSearchTitle(title: String): String {
        val preTitle = title.lowercase(Locale.getDefault())

        // Remove text in brackets
        var cleanedTitle = removeTextInBrackets(preTitle, true)
        if (cleanedTitle.length <= 5) { // Title is suspiciously short, try parsing it backwards
            cleanedTitle = removeTextInBrackets(preTitle, false)
        }

        // Strip chapter reference RU
        cleanedTitle = cleanedTitle.replace(chapterRefCyrillicRegexp, " ").trim()

        // Strip non-special characters
        val cleanedTitleEng = cleanedTitle.replace(titleRegex, " ")

        // Do not strip foreign language letters if cleanedTitle is too short
        if (cleanedTitleEng.length <= 5) {
            cleanedTitle = cleanedTitle.replace(titleCyrillicRegex, " ")
        } else {
            cleanedTitle = cleanedTitleEng
        }

        // Strip splitters and consecutive spaces
        cleanedTitle = cleanedTitle.trim().replace(" - ", " ").replace(consecutiveSpacesRegex, " ").trim()

        return cleanedTitle
    }

    private fun removeTextInBrackets(text: String, readForward: Boolean): String {
        val bracketPairs = listOf(
            '(' to ')',
            '[' to ']',
            '<' to '>',
            '{' to '}',
        )
        var openingBracketPairs = bracketPairs.mapIndexed { index, (opening, _) ->
            opening to index
        }.toMap()
        var closingBracketPairs = bracketPairs.mapIndexed { index, (_, closing) ->
            closing to index
        }.toMap()

        // Reverse pairs if reading backwards
        if (!readForward) {
            val tmp = openingBracketPairs
            openingBracketPairs = closingBracketPairs
            closingBracketPairs = tmp
        }

        val depthPairs = bracketPairs.map { 0 }.toMutableList()

        val result = StringBuilder()
        for (c in if (readForward) text else text.reversed()) {
            val openingBracketDepthIndex = openingBracketPairs[c]
            if (openingBracketDepthIndex != null) {
                depthPairs[openingBracketDepthIndex]++
            } else {
                val closingBracketDepthIndex = closingBracketPairs[c]
                if (closingBracketDepthIndex != null) {
                    depthPairs[closingBracketDepthIndex]--
                } else {
                    @Suppress("ControlFlowWithEmptyBody")
                    if (depthPairs.all { it <= 0 }) {
                        result.append(c)
                    } else {
                        // In brackets, do not append to result
                    }
                }
            }
        }

        return result.toString()
    }

    private fun getSmartSearchQueries(cleanedTitle: String): List<String> {
        val splitCleanedTitle = cleanedTitle.split(" ")
        val splitSortedByLargest = splitCleanedTitle.sortedByDescending { it.length }

        if (splitCleanedTitle.isEmpty()) {
            return emptyList()
        }

        // For very long titles, we want to be more targeted with our search queries
        if (splitCleanedTitle.size > 5) {
            return listOf(
                cleanedTitle,
                splitSortedByLargest.take(2).joinToString(" "),
                splitSortedByLargest.first()
            )
        }

        // For regular titles, use the standard approach but optimize order
        val searchQueries = listOf(
            listOf(cleanedTitle),
            splitSortedByLargest.take(2),
            splitSortedByLargest.take(1),
            splitCleanedTitle.take(2),
            splitCleanedTitle.take(1),
        )

        return searchQueries.map {
            it.joinToString(" ").trim()
        }.distinct()
    }

    // Clear caches if they get too large
    open fun clearCaches() {
        if (cleanedTitleCache.size > 1000) {
            cleanedTitleCache.clear()
        }
        if (searchResultsCache.size > 200) {
            searchResultsCache.clear()
        }
    }

    companion object {
        const val MIN_ELIGIBLE_THRESHOLD = 0.4

        private val titleRegex = Regex("[^a-zA-Z0-9- ]")
        private val titleCyrillicRegex = Regex("[^\\p{L}0-9- ]")
        private val consecutiveSpacesRegex = Regex(" +")
        private val chapterRefCyrillicRegexp = Regex("""((- часть|- глава) \d*)""")
    }
}

data class SearchEntry<T>(val manga: T, val dist: Double)
