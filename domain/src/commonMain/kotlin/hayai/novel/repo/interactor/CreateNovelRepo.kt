package hayai.novel.repo.interactor

import hayai.novel.repo.NovelRepoRepository
import hayai.novel.repo.model.NovelRepo

class CreateNovelRepo(
    private val repository: NovelRepoRepository,
    private val validator: NovelRepoIndexValidator,
) {
    sealed class Result {
        data object Success : Result()
        data object InvalidUrl : Result()
        data object InvalidIndex : Result()
        data object EmptyRepo : Result()
        data object RepoAlreadyExists : Result()
    }

    suspend fun await(url: String): Result {
        val validation = validator.validate(url)
        val indexUrl = when (validation) {
            NovelRepoIndexValidation.EmptyRepo -> return Result.EmptyRepo
            NovelRepoIndexValidation.InvalidIndex -> return Result.InvalidIndex
            NovelRepoIndexValidation.InvalidUrl -> return Result.InvalidUrl
            is NovelRepoIndexValidation.Valid -> validation.indexUrl
        }

        val existing = repository.getAll()
        if (existing.any { it.baseUrl == indexUrl }) {
            return Result.RepoAlreadyExists
        }

        repository.insert(
            NovelRepo(
                baseUrl = indexUrl,
                name = deriveName(indexUrl),
            ),
        )
        return Result.Success
    }

    private fun deriveName(indexUrl: String): String {
        val host = indexUrl
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
            .removePrefix("www.")
            .substringBefore('.')
            .ifBlank { "Novel" }
        return host.replaceFirstChar { it.titlecase() } + " Novel Repo"
    }
}

interface NovelRepoIndexValidator {
    suspend fun validate(rawUrl: String): NovelRepoIndexValidation
}

sealed interface NovelRepoIndexValidation {
    data object InvalidUrl : NovelRepoIndexValidation
    data object InvalidIndex : NovelRepoIndexValidation
    data object EmptyRepo : NovelRepoIndexValidation
    data class Valid(val indexUrl: String) : NovelRepoIndexValidation
}
