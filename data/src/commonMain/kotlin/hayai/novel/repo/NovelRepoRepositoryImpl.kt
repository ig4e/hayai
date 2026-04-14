package hayai.novel.repo

import hayai.novel.repo.model.NovelRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import yokai.data.DatabaseHandler

class NovelRepoRepositoryImpl(
    private val handler: DatabaseHandler,
) : NovelRepoRepository {

    override fun subscribeAll(): Flow<List<NovelRepo>> {
        return handler.subscribeToList { novel_reposQueries.findAll() }
            .map { rows -> rows.map { NovelRepo(baseUrl = it.base_url, name = it.name) } }
    }

    override suspend fun getAll(): List<NovelRepo> {
        return handler.awaitList { novel_reposQueries.findAll() }
            .map { NovelRepo(baseUrl = it.base_url, name = it.name) }
    }

    override suspend fun insert(repo: NovelRepo) {
        handler.await { novel_reposQueries.insert(repo.baseUrl, repo.name) }
    }

    override suspend fun delete(baseUrl: String) {
        handler.await { novel_reposQueries.deleteByUrl(baseUrl) }
    }
}
