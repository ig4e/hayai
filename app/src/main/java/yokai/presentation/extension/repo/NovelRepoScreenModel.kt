package yokai.presentation.extension.repo

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.system.launchIO
import hayai.novel.repo.interactor.CreateNovelRepo
import hayai.novel.repo.interactor.DeleteNovelRepo
import hayai.novel.repo.interactor.GetNovelRepo
import hayai.novel.repo.model.NovelRepo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import uy.kohesive.injekt.injectLazy
import yokai.i18n.MR

class NovelRepoScreenModel : StateScreenModel<NovelRepoScreenModel.State>(State.Loading) {

    private val getNovelRepo: GetNovelRepo by injectLazy()
    private val createNovelRepo: CreateNovelRepo by injectLazy()
    private val deleteNovelRepo: DeleteNovelRepo by injectLazy()
    private val networkHelper: NetworkHelper by injectLazy()
    private val json = Json { ignoreUnknownKeys = true }

    private val internalEvent: MutableStateFlow<ExtensionRepoEvent> = MutableStateFlow(ExtensionRepoEvent.NoOp)
    val event: StateFlow<ExtensionRepoEvent> = internalEvent.asStateFlow()

    init {
        screenModelScope.launchIO {
            getNovelRepo.subscribeAll().collectLatest { repos ->
                mutableState.update { State.Success(repos = repos.toImmutableList()) }
            }
        }
    }

    fun addRepo(url: String) {
        screenModelScope.launchIO {
            when (val validation = validateRepo(url)) {
                RepoValidation.InvalidUrl -> {
                    internalEvent.value = ExtensionRepoEvent.InvalidUrl
                }
                RepoValidation.InvalidIndex -> {
                    internalEvent.value = NovelRepoEvent.InvalidIndex
                }
                RepoValidation.EmptyRepo -> {
                    internalEvent.value = NovelRepoEvent.EmptyRepo
                }
                is RepoValidation.Success -> when (createNovelRepo.await(validation.indexUrl)) {
                is CreateNovelRepo.Result.Success -> {
                    internalEvent.value = ExtensionRepoEvent.Success
                }
                is CreateNovelRepo.Result.InvalidUrl -> {
                    internalEvent.value = ExtensionRepoEvent.InvalidUrl
                }
                is CreateNovelRepo.Result.InvalidIndex -> {
                    internalEvent.value = NovelRepoEvent.InvalidIndex
                }
                is CreateNovelRepo.Result.EmptyRepo -> {
                    internalEvent.value = NovelRepoEvent.EmptyRepo
                }
                is CreateNovelRepo.Result.RepoAlreadyExists -> {
                    internalEvent.value = ExtensionRepoEvent.RepoAlreadyExists
                }
                }
            }
        }
    }

    fun deleteRepo(url: String) {
        screenModelScope.launchIO {
            deleteNovelRepo.await(url)
        }
    }

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data class Success(
            val repos: ImmutableList<NovelRepo>,
        ) : State
    }

    private suspend fun validateRepo(rawUrl: String): RepoValidation {
        val candidateUrls = buildCandidateUrls(rawUrl) ?: return RepoValidation.InvalidUrl

        for (candidateUrl in candidateUrls) {
            when (fetchRepoState(candidateUrl)) {
                RepoState.Empty -> return RepoValidation.EmptyRepo
                RepoState.Invalid -> continue
                RepoState.Valid -> return RepoValidation.Success(candidateUrl)
            }
        }

        return RepoValidation.InvalidIndex
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

            val body = response.body?.string().orEmpty()
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

    private sealed interface RepoValidation {
        data object InvalidUrl : RepoValidation
        data object InvalidIndex : RepoValidation
        data object EmptyRepo : RepoValidation
        data class Success(val indexUrl: String) : RepoValidation
    }

    private enum class RepoState {
        Invalid,
        Empty,
        Valid,
    }
}

sealed class NovelRepoEvent {
    data object InvalidIndex : ExtensionRepoEvent.LocalizedMessage(MR.strings.novel_repo_invalid)
    data object EmptyRepo : ExtensionRepoEvent.LocalizedMessage(MR.strings.novel_repo_empty)
}
