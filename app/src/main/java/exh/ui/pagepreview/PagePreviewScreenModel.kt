package exh.ui.pagepreview

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.source.PagePreviewInfo
import eu.kanade.tachiyomi.source.PagePreviewSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import exh.source.getMainSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import yokai.domain.chapter.interactor.GetChapter
import yokai.domain.manga.interactor.GetManga

class PagePreviewScreenModel(
    private val mangaId: Long,
    private val getManga: GetManga = Injekt.get(),
    private val getChapter: GetChapter = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
) : StateScreenModel<PagePreviewState>(PagePreviewState.Loading) {

    private var currentPage = 1
    private var isLoadingMore = false
    private var hasNextPage = true
    private var previewSource: PagePreviewSource? = null
    private var manga: Manga? = null
    private var chapter: Chapter? = null
    private var source: Source? = null
    private var sortedChapters: List<Chapter> = emptyList()

    init {
        screenModelScope.launch(Dispatchers.IO) {
            val manga = getManga.awaitById(mangaId)
            if (manga == null) {
                mutableState.update { PagePreviewState.Error(Exception("Manga not found")) }
                return@launch
            }
            this@PagePreviewScreenModel.manga = manga
            val chapters = getChapter.awaitAll(mangaId, filterScanlators = false)
            val chapter = chapters.minByOrNull { it.source_order }
            if (chapter == null) {
                mutableState.update { PagePreviewState.Error(Exception("No chapters found")) }
                return@launch
            }
            this@PagePreviewScreenModel.chapter = chapter
            this@PagePreviewScreenModel.sortedChapters = chapters.sortedByDescending { it.source_order }
            val source = sourceManager.getOrStub(manga.source)
            this@PagePreviewScreenModel.source = source
            val previewSource = source.getMainSource<PagePreviewSource>()
            if (previewSource == null) {
                mutableState.update { PagePreviewState.Error(Exception("Source does not support page previews")) }
                return@launch
            }
            this@PagePreviewScreenModel.previewSource = previewSource
            loadPage(1)
        }
    }

    private suspend fun loadPage(page: Int) {
        try {
            val previewPage = previewSource!!.getPagePreviewList(
                manga!!,
                sortedChapters,
                page,
            )
            hasNextPage = previewPage.hasNextPage
            currentPage = page
            mutableState.update { currentState ->
                when (currentState) {
                    is PagePreviewState.Success -> currentState.copy(
                        pagePreviews = currentState.pagePreviews + previewPage.pagePreviews,
                        hasNextPage = previewPage.hasNextPage,
                        isLoadingMore = false,
                    )
                    else -> PagePreviewState.Success(
                        pagePreviews = previewPage.pagePreviews,
                        hasNextPage = previewPage.hasNextPage,
                        isLoadingMore = false,
                        manga = manga!!,
                        chapter = chapter!!,
                        source = source!!,
                    )
                }
            }
        } catch (e: Exception) {
            if (state.value !is PagePreviewState.Success) {
                mutableState.update { PagePreviewState.Error(e) }
            } else {
                mutableState.update { (it as? PagePreviewState.Success)?.copy(isLoadingMore = false) ?: it }
            }
        } finally {
            isLoadingMore = false
        }
    }

    fun loadMore() {
        if (isLoadingMore || !hasNextPage) return
        isLoadingMore = true
        mutableState.update { (it as? PagePreviewState.Success)?.copy(isLoadingMore = true) ?: it }
        screenModelScope.launch(Dispatchers.IO) {
            loadPage(currentPage + 1)
        }
    }
}

sealed class PagePreviewState {
    data object Loading : PagePreviewState()

    data class Success(
        val pagePreviews: List<PagePreviewInfo>,
        val hasNextPage: Boolean,
        val isLoadingMore: Boolean = false,
        val manga: Manga,
        val chapter: Chapter,
        val source: Source,
    ) : PagePreviewState()

    data class Error(val error: Throwable) : PagePreviewState()
}
