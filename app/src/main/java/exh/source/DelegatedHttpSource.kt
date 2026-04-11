package exh.source

import android.content.Context
import eu.kanade.tachiyomi.source.online.HttpSource

/**
 * EXH delegated HTTP source base class.
 * Extension sources that match the delegation table get wrapped with [EnhancedHttpSource],
 * which delegates calls through this class for enhanced metadata handling.
 *
 * This is the EXH version of DelegatedHttpSource, separate from the J2K
 * DelegatedHttpSource in source-api which handles deep links.
 */
abstract class DelegatedHttpSource(
    val originalSource: HttpSource,
    val context: Context,
) {
    /**
     * Called to update/fill metadata for a manga after fetch.
     */
    open suspend fun beforeMangaFetch(mangaUrl: String) {}

    /**
     * Called after manga details are fetched to update metadata.
     */
    open suspend fun afterMangaFetch() {}
}
