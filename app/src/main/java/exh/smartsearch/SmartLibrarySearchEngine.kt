package exh.smartsearch

import exh.util.removeArticles

/**
 * Smart search engine that performs fuzzy matching against library entries.
 */
class SmartLibrarySearchEngine<T>(
    private val extraSearchParams: String? = null,
    private val threshold: Double = 0.7,
    private val getTitle: (T) -> String,
) {

    suspend fun smartSearch(library: List<T>, title: String): T? {
        return deepSearch(
            { query ->
                library.filter { getTitle(it).contains(query, true) }
            },
            title,
        )
    }

    private suspend fun deepSearch(
        search: suspend (String) -> List<T>,
        title: String,
    ): T? {
        val cleanTitle = title.removeArticles().trim()
        if (cleanTitle.isBlank()) return null

        // Try exact match first
        val exactResults = search(cleanTitle)
        if (exactResults.size == 1) return exactResults.first()

        // Try progressively shorter queries
        val words = cleanTitle.split(" ").filter { it.isNotBlank() }
        for (i in words.size downTo 1) {
            val query = words.take(i).joinToString(" ")
            val results = search(query)
            if (results.size == 1) return results.first()
        }

        return null
    }
}
