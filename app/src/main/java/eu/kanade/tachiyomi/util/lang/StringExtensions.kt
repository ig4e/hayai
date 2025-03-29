package eu.kanade.tachiyomi.util.lang

/**
 * Fuzzy search function that checks if characters appear in sequence.
 * Returns true if the [other] string is found in this string with characters in sequence.
 */
fun String.containsFuzzy(other: String, ignoreCase: Boolean = true): Boolean {
    if (other.isEmpty()) return true
    if (this.isEmpty()) return false

    val sourceText = if (ignoreCase) this.lowercase() else this
    val searchText = if (ignoreCase) other.lowercase() else other

    var sourceIndex = 0
    var searchIndex = 0

    while (sourceIndex < sourceText.length && searchIndex < searchText.length) {
        if (sourceText[sourceIndex] == searchText[searchIndex]) {
            searchIndex++
        }
        sourceIndex++
    }

    return searchIndex == searchText.length
}
