package hayai.novel.repo.interactor

import hayai.novel.repo.NovelRepoRepository

class DeleteNovelRepo(
    private val repository: NovelRepoRepository,
) {
    suspend fun await(baseUrl: String) {
        repository.delete(baseUrl)
    }
}
