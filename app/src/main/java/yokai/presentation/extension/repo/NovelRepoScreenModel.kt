package yokai.presentation.extension.repo

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
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
import uy.kohesive.injekt.injectLazy
import yokai.i18n.MR

class NovelRepoScreenModel : StateScreenModel<NovelRepoScreenModel.State>(State.Loading) {

    private val getNovelRepo: GetNovelRepo by injectLazy()
    private val createNovelRepo: CreateNovelRepo by injectLazy()
    private val deleteNovelRepo: DeleteNovelRepo by injectLazy()

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
            when (createNovelRepo.await(url)) {
                is CreateNovelRepo.Result.Success -> {
                    internalEvent.value = ExtensionRepoEvent.Success
                }
                is CreateNovelRepo.Result.InvalidUrl -> {
                    internalEvent.value = ExtensionRepoEvent.InvalidUrl
                }
                is CreateNovelRepo.Result.RepoAlreadyExists -> {
                    internalEvent.value = ExtensionRepoEvent.RepoAlreadyExists
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
}
