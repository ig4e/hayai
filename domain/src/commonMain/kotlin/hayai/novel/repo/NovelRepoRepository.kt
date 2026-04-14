package hayai.novel.repo

import hayai.novel.repo.model.NovelRepo
import kotlinx.coroutines.flow.Flow

interface NovelRepoRepository {
    fun subscribeAll(): Flow<List<NovelRepo>>
    suspend fun getAll(): List<NovelRepo>
    suspend fun insert(repo: NovelRepo)
    suspend fun delete(baseUrl: String)
}
