package yokai.data.manga

import exh.favorites.FavoriteEntry
import yokai.data.DatabaseHandler
import yokai.domain.manga.EhFavoritesRepository

class EhFavoritesRepositoryImpl(
    private val handler: DatabaseHandler,
) : EhFavoritesRepository {

    override suspend fun getAll(): List<FavoriteEntry> =
        handler.awaitList {
            eh_favoritesQueries.selectAll { gid, token, title, category ->
                FavoriteEntry(
                    title = title,
                    gid = gid,
                    token = token,
                    category = category.toInt(),
                )
            }
        }

    override suspend fun insertAll(entries: List<FavoriteEntry>) =
        handler.await(inTransaction = true) {
            entries.forEach { entry ->
                eh_favoritesQueries.insertEhFavorites(
                    entry.title,
                    entry.gid,
                    entry.token,
                    entry.category.toLong(),
                )
            }
        }

    override suspend fun deleteAll() {
        handler.await { eh_favoritesQueries.deleteAll() }
    }
}
