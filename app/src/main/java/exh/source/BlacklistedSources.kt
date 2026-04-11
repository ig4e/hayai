package exh.source

/**
 * Sources that are blacklisted from being used with EXH features.
 * These are removed from the sources map when source blacklisting is enabled.
 */
object BlacklistedSources {
    /**
     * Extension sources that should be hidden entirely when blacklisting is active.
     */
    val BLACKLISTED_EXT_SOURCES: Set<Long> = emptySet()

    /**
     * Sources that should be hidden from the visible sources list
     * (e.g., EH/EXH internal sources that shouldn't appear in browse).
     */
    val HIDDEN_SOURCES: Set<Long> = setOf(
        EH_SOURCE_ID,
        EXH_SOURCE_ID,
        MERGED_SOURCE_ID,
    )
}
