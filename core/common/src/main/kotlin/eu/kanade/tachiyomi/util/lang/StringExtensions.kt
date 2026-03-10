@file:JvmName("StringExtensionsKt")

package eu.kanade.tachiyomi.util.lang

import androidx.core.text.parseAsHtml
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator
import java.nio.charset.StandardCharsets
import kotlin.math.floor

/**
 * Replaces the given string to have at most [count] characters using [replacement] at its end.
 * If [replacement] is longer than [count] an exception will be thrown when `length > count`.
 */
fun String.chop(count: Int, replacement: String = "…"): String {
    return if (length > count) {
        take(count - replacement.length) + replacement
    } else {
        this
    }
}

/**
 * Replaces the given string to have at most [count] characters using [replacement] near the center.
 * If [replacement] is longer than [count] an exception will be thrown when `length > count`.
 */
fun String.truncateCenter(count: Int, replacement: String = "..."): String {
    if (length <= count) {
        return this
    }

    val pieceLength: Int = floor((count - replacement.length).div(2.0)).toInt()

    return "${take(pieceLength)}$replacement${takeLast(pieceLength)}"
}

/**
 * Case-insensitive natural comparator for strings.
 */
fun String.compareToCaseInsensitiveNaturalOrder(other: String): Int {
    val comparator = CaseInsensitiveSimpleNaturalComparator.getInstance<String>()
    return comparator.compare(this, other)
}

/**
 * Returns the size of the string as the number of bytes.
 */
fun String.byteSize(): Int {
    return toByteArray(StandardCharsets.UTF_8).size
}

/**
 * Returns a string containing the first [n] bytes from this string, or the entire string if this
 * string is shorter.
 */
fun String.takeBytes(n: Int): String {
    val bytes = toByteArray(StandardCharsets.UTF_8)
    return if (bytes.size <= n) {
        this
    } else {
        bytes.decodeToString(endIndex = n).replace("\uFFFD", "")
    }
}

/**
 * HTML-decode the string
 */
fun String.htmlDecode(): String {
    return this.parseAsHtml().toString()
}

/**
 * Fuzzy search function that checks if characters appear in sequence.
 * Returns true if the [query] string is found in [text] with characters in sequence,
 * with a minimum matching quality.
 *
 * @param text The source text to search in
 * @param query The text to search for
 * @param ignoreCase Whether to ignore case when comparing characters
 * @param minSimilarity Minimum similarity threshold (0.0-1.0) for a match to be considered valid
 * @param flags Unused parameter, included for compatibility
 * @param defaultValue Unused parameter, included for compatibility
 * @return true if the query string is found, false otherwise
 */
@JvmOverloads
fun containsFuzzy(
    text: String,
    query: String,
    ignoreCase: Boolean = true,
    minSimilarity: Float = 0.8f,
    flags: Int = 0,
    defaultValue: Any? = null
): Boolean {
    if (query.isBlank()) return true
    if (text.isBlank()) return false

    val sourceText = text.normalizeForFuzzySearch(ignoreCase)
    val searchText = query.normalizeForFuzzySearch(ignoreCase)

    if (searchText.isEmpty() || sourceText.contains(searchText)) return true

    val sourceTokens = sourceText.tokenizeForFuzzySearch()
    val queryTokens = searchText.tokenizeForFuzzySearch()

    if (queryTokens.isNotEmpty()) {
        val allTokensMatch = queryTokens.all { queryToken ->
            val tokenSimilarityThreshold = minSimilarity.coerceAtMost(0.45f)
            sourceTokens.any { sourceToken ->
                sourceToken.contains(queryToken) ||
                    fuzzySubsequenceSimilarity(sourceToken, queryToken) >= tokenSimilarityThreshold
            }
        }
        if (allTokensMatch) return true
    }

    val compactSource = sourceTokens.joinToString(separator = "")
    val compactQuery = queryTokens.joinToString(separator = "").ifEmpty { searchText.replace(" ", "") }

    if (compactQuery.isEmpty()) return true
    if (compactSource.contains(compactQuery)) return true

    val initials = sourceTokens.mapNotNull { it.firstOrNull() }.joinToString(separator = "")
    if (initials.startsWith(compactQuery) || initials.contains(compactQuery)) return true

    return fuzzySubsequenceSimilarity(compactSource, compactQuery) >= minSimilarity
}

@JvmName("containsFuzzyString")
@JvmOverloads
fun String.containsFuzzy(
    query: String,
    ignoreCase: Boolean = true,
    minSimilarity: Float = 0.8f,
): Boolean {
    return containsFuzzy(
        text = this,
        query = query,
        ignoreCase = ignoreCase,
        minSimilarity = minSimilarity,
    )
}

private fun String.normalizeForFuzzySearch(ignoreCase: Boolean): String {
    val normalized = if (ignoreCase) lowercase() else this
    return buildString(normalized.length) {
        normalized.forEach { char ->
            append(
                when {
                    char.isLetterOrDigit() -> char
                    else -> ' '
                },
            )
        }
    }.replace(Regex("\\s+"), " ").trim()
}

private fun String.tokenizeForFuzzySearch(): List<String> {
    return split(' ').filter { it.isNotBlank() }
}

private fun fuzzySubsequenceSimilarity(text: String, query: String): Float {
    if (query.isEmpty()) return 1f
    if (text.isEmpty() || query.length > text.length) return 0f

    if (query.length <= 2) {
        return if (text.contains(query)) 1f else 0f
    }

    var searchIndex = 0
    var longestRun = 0
    var currentRun = 0
    var firstMatchIndex = -1
    var lastMatchIndex = -1

    text.forEachIndexed { index, char ->
        if (searchIndex < query.length && char == query[searchIndex]) {
            if (firstMatchIndex == -1) {
                firstMatchIndex = index
            }
            lastMatchIndex = index
            searchIndex++
            currentRun++
            longestRun = maxOf(longestRun, currentRun)
        } else if (currentRun > 0) {
            currentRun = 0
        }
    }

    if (searchIndex != query.length) return 0f

    val window = (lastMatchIndex - firstMatchIndex + 1).coerceAtLeast(query.length)
    val density = query.length.toFloat() / window.toFloat()
    val continuity = longestRun.toFloat() / query.length.toFloat()
    return (density * 0.55f) + (continuity * 0.45f)
}
