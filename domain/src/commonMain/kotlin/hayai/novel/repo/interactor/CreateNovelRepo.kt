package hayai.novel.repo.interactor

import hayai.novel.repo.NovelRepoRepository
import hayai.novel.repo.model.NovelRepo

class CreateNovelRepo(
    private val repository: NovelRepoRepository,
) {
    sealed class Result {
        data object Success : Result()
        data object InvalidUrl : Result()
        data object InvalidIndex : Result()
        data object EmptyRepo : Result()
        data object RepoAlreadyExists : Result()
    }

    suspend fun await(url: String): Result {
        val trimmedUrl = normalizeUrl(url) ?: return Result.InvalidUrl

        val existing = repository.getAll()
        if (existing.any { it.baseUrl == trimmedUrl }) {
            return Result.RepoAlreadyExists
        }

        repository.insert(
            NovelRepo(
                baseUrl = trimmedUrl,
                name = deriveName(trimmedUrl),
            ),
        )
        return Result.Success
    }

    private fun normalizeUrl(url: String): String? {
        val trimmedUrl = url.trim()
        if (!trimmedUrl.startsWith("https://") && !trimmedUrl.startsWith("http://")) {
            return null
        }
        return trimmedUrl.trimEnd('/')
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
