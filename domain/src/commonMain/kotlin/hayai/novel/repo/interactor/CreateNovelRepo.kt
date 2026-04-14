package hayai.novel.repo.interactor

import hayai.novel.repo.NovelRepoRepository
import hayai.novel.repo.model.NovelRepo

class CreateNovelRepo(
    private val repository: NovelRepoRepository,
) {
    sealed class Result {
        data object Success : Result()
        data object InvalidUrl : Result()
        data object RepoAlreadyExists : Result()
    }

    suspend fun await(url: String): Result {
        val trimmedUrl = url.trim()

        // Basic URL validation
        if (!trimmedUrl.startsWith("https://") && !trimmedUrl.startsWith("http://")) {
            return Result.InvalidUrl
        }

        // Check if repo already exists
        val existing = repository.getAll()
        if (existing.any { it.baseUrl == trimmedUrl }) {
            return Result.RepoAlreadyExists
        }

        // Extract name from URL
        val name = try {
            val segments = trimmedUrl.split("/")
            segments.getOrElse(segments.size - 3) { "Novel Repo" }
        } catch (_: Exception) {
            "Novel Repo"
        }

        repository.insert(NovelRepo(baseUrl = trimmedUrl, name = name))
        return Result.Success
    }
}
