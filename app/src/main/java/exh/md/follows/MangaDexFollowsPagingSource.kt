package exh.md.follows

import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.online.all.MangaDex

/**
 * MangaDex follows paging source.
 *
 * Fetches paginated follow lists from the MangaDex API via the delegated source.
 */
class MangaDexFollowsPagingSource(val mangadex: MangaDex) {

    suspend fun requestNextPage(currentPage: Int): MangasPage {
        return mangadex.fetchFollows(currentPage)
    }
}
