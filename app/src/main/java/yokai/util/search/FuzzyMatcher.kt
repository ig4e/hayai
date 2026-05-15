package yokai.util.search

import me.xdrop.fuzzywuzzy.FuzzySearch

object FuzzyMatcher {
    private fun normalize(s: String): String = s.trim().lowercase()

    fun score(query: String, candidate: String?): Int {
        if (candidate.isNullOrEmpty() || query.isEmpty()) return 0
        // Fast path for short queries: avoid the library call entirely so older devices stay responsive
        if (query.length < 2) {
            return if (candidate.contains(query, ignoreCase = true)) 100 else 0
        }
        return FuzzySearch.partialRatio(normalize(query), normalize(candidate))
    }

    fun matches(query: String, candidate: String?, threshold: Int = 70): Boolean =
        score(query, candidate) >= threshold

    fun <T> match(query: String, candidates: List<T>, key: (T) -> String, threshold: Int = 70): List<T> {
        if (query.isBlank()) return candidates
        return candidates
            .map { it to score(query, key(it)) }
            .filter { it.second >= threshold }
            .sortedByDescending { it.second }
            .map { it.first }
    }
}
