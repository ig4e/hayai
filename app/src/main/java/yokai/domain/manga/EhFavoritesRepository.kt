package yokai.domain.manga

import exh.favorites.FavoriteEntry

interface EhFavoritesRepository {
    suspend fun getAll(): List<FavoriteEntry>
    suspend fun insertAll(entries: List<FavoriteEntry>)
    suspend fun deleteAll()
}
