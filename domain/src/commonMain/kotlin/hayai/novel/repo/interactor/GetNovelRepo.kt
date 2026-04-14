package hayai.novel.repo.interactor

import hayai.novel.repo.NovelRepoRepository
import hayai.novel.repo.model.NovelRepo
import kotlinx.coroutines.flow.Flow

class GetNovelRepo(
    private val repository: NovelRepoRepository,
) {
    fun subscribeAll(): Flow<List<NovelRepo>> = repository.subscribeAll()

    suspend fun getAll(): List<NovelRepo> = repository.getAll()
}
