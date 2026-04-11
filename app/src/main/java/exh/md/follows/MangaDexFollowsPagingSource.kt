package exh.md.follows

import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.online.all.MangaDex
// TODO: Wire up with BaseSourcePagingSource when available

/**
 * MangaDex follows paging source placeholder.
 * TODO: Extend BaseSourcePagingSource when the paging infrastructure is available
 */
class MangaDexFollowsPagingSource(val mangadex: MangaDex) {

    suspend fun requestNextPage(currentPage: Int): MangasPage {
        return mangadex.fetchFollows(currentPage)
    }
}
