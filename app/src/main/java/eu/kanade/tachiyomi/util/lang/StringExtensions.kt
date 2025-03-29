package eu.kanade.tachiyomi.util.lang

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
