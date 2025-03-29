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
    if (query.isEmpty()) return true
    if (text.isEmpty()) return false

    val sourceText = if (ignoreCase) text.lowercase() else text
    val searchText = if (ignoreCase) query.lowercase() else query

    // If the query is longer than source, instant fail
    if (searchText.length > sourceText.length) return false

    // For very short queries (1-2 chars), require exact substring match
    if (searchText.length <= 2) {
        return sourceText.contains(searchText)
    }

    var score = 0
    var consecutiveMatches = 0
    var maxConsecutiveMatches = 0
    var sourceIndex = 0
    var searchIndex = 0

    while (sourceIndex < sourceText.length && searchIndex < searchText.length) {
        if (sourceText[sourceIndex] == searchText[searchIndex]) {
            score++
            searchIndex++
            consecutiveMatches++
            maxConsecutiveMatches = maxOf(maxConsecutiveMatches, consecutiveMatches)
        } else {
            consecutiveMatches = 0
        }
        sourceIndex++
    }

    // Calculate match quality metrics
    val matched = searchIndex == searchText.length
    if (!matched) return false

    // Calculate similarity score (0.0-1.0)
    val basicScore = score.toFloat() / searchText.length
    val consecutiveBonus = maxConsecutiveMatches.toFloat() / searchText.length
    val similarity = (basicScore * 0.5f) + (consecutiveBonus * 0.5f)

    return similarity >= minSimilarity
}
