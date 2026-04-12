package exh.source

/**
 * Source helpers
 */

/**
 * Populates [metadataDelegatedSourceIds], [nHentaiSourceIds], [mangaDexSourceIds],
 * [lanraragiSourceIds], and [LIBRARY_UPDATE_EXCLUDED_SOURCES] from
 * [eu.kanade.tachiyomi.source.SourceManager.currentDelegatedSources].
 *
 * Currently unused — the delegated source IDs in [DomainSourceHelpers] keep their
 * defaults. Wire this up after SourceManager initialization if dynamic population
 * is needed.
 */
fun handleSourceLibrary() {
    // No-op: see KDoc above
}
