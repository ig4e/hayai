package hayai.novel.repo

import eu.kanade.tachiyomi.network.NetworkHelper
import hayai.novel.repo.interactor.NovelRepoIndexValidation
import hayai.novel.repo.interactor.NovelRepoIndexValidator
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

class NovelRepoIndexValidatorImpl(
    private val networkHelper: NetworkHelper,
) : NovelRepoIndexValidator {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun validate(rawUrl: String): NovelRepoIndexValidation {
        val candidateUrls = buildCandidateUrls(rawUrl) ?: return NovelRepoIndexValidation.InvalidUrl

        for (candidateUrl in candidateUrls) {
            when (fetchRepoState(candidateUrl)) {
                RepoState.Empty -> return NovelRepoIndexValidation.EmptyRepo
                RepoState.Invalid -> continue
                RepoState.Valid -> return NovelRepoIndexValidation.Valid(candidateUrl)
            }
        }

        return NovelRepoIndexValidation.InvalidIndex
    }

    private suspend fun fetchRepoState(indexUrl: String): RepoState {
        return try {
            val request = Request.Builder()
                .url(indexUrl)
                .addHeader("pragma", "no-cache")
                .addHeader("cache-control", "no-cache")
                .build()

            val response = networkHelper.client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return RepoState.Invalid
            }

            val body = response.body.string()
            response.close()

            val payload = json.parseToJsonElement(body) as? JsonArray ?: return RepoState.Invalid
            if (payload.isEmpty()) {
                return RepoState.Empty
            }

            val hasValidEntries = payload.any { element ->
                val item = element as? JsonObject ?: return@any false
                item["id"] != null && item["name"] != null && item["url"] != null
            }

            if (hasValidEntries) RepoState.Valid else RepoState.Invalid
        } catch (_: Exception) {
            RepoState.Invalid
        }
    }

    private fun buildCandidateUrls(rawUrl: String): List<String>? {
        val trimmedUrl = rawUrl.trim()
        if (!trimmedUrl.startsWith("https://") && !trimmedUrl.startsWith("http://")) {
            return null
        }

        val httpUrl = trimmedUrl.toHttpUrlOrNull() ?: return null
        val baseUrl = httpUrl.newBuilder()
            .query(null)
            .fragment(null)
            .build()
            .toString()
            .trimEnd('/')

        return linkedSetOf<String>().apply {
            if (baseUrl.endsWith(".json")) {
                add(baseUrl)
            } else {
                add("$baseUrl/plugins.min.json")
                add("$baseUrl/index.min.json")
                add(baseUrl)
            }
        }.toList()
    }

    private enum class RepoState {
        Invalid,
        Empty,
        Valid,
    }
}
