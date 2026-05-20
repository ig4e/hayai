package exh.ui.pagepreview

import yokai.util.koin.get
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import yokai.domain.chapter.interactor.GetChapter
import yokai.domain.manga.interactor.GetManga

class PagePreviewScreenModel(
    private val mangaId: Long,
    private val getManga: GetManga = get(),
    private val getChapter: GetChapter = get(),
    private val sourceManager: SourceManager = get(),
) : StateScreenModel<PagePreviewState>(PagePreviewState.Loading) {

    private var currentPage = 1
    private var isLoadingMore = false
    private var hasNextPage = true
    private var previewSource: PagePreviewSource? = null
    private var manga: Manga? = null
    private var chapter: Chapter? = null
    private var source: Source? = null
    private var sortedChapters: List<Chapter> = emptyList()
    private var batchSize: Int? = null

    private val _scrollEvents = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val scrollEvents: SharedFlow<Int> = _scrollEvents.asSharedFlow()

    init {
        screenModelScope.launch(Dispatchers.IO) {
            val manga = getManga.awaitById(mangaId)
            if (manga == null) {
                mutableState.update { PagePreviewState.Error(Exception("Manga not found")) }
                return@launch
            }
            this@PagePreviewScreenModel.manga = manga
            val chapters = getChapter.awaitAll(mangaId, filterScanlators = false)
            // Prefer the chapter that shares manga.url's gallery so tapping a preview
            // opens the SAME gallery whose thumbnails we render. For EH multi-version
            // galleries that means we open the version the user added rather than the
            // newest revision. Match by EH gallery id (path segment `/g/{gid}/…`); for
            // non-EH manga this returns null and falls back to the previous behaviour.
            val mangaGalleryId = extractEhGalleryId(manga.url)
            val chapter = mangaGalleryId
                ?.let { gid -> chapters.firstOrNull { extractEhGalleryId(it.url) == gid } }
                ?: chapters.minByOrNull { it.source_order }
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

    private suspend fun loadPage(page: Int, replace: Boolean = false) {
        try {
            val previewPage = previewSource!!.getPagePreviewList(
                manga!!,
                sortedChapters,
                page,
            )
            if (batchSize == null && previewPage.pagePreviews.isNotEmpty()) {
                batchSize = previewPage.pagePreviews.size
            }
            hasNextPage = previewPage.hasNextPage
            currentPage = page
            val estimatedTotal = batchSize?.let { size ->
                previewPage.pagePreviewPages?.let { it * size }
            }
            mutableState.update { currentState ->
                when {
                    currentState is PagePreviewState.Success && !replace -> currentState.copy(
                        pagePreviews = currentState.pagePreviews + previewPage.pagePreviews,
                        hasNextPage = previewPage.hasNextPage,
                        isLoadingMore = false,
                        estimatedTotalPages = estimatedTotal ?: currentState.estimatedTotalPages,
                        batchSize = batchSize ?: currentState.batchSize,
                    )
                    currentState is PagePreviewState.Success -> currentState.copy(
                        pagePreviews = previewPage.pagePreviews,
                        hasNextPage = previewPage.hasNextPage,
                        isLoadingMore = false,
                        estimatedTotalPages = estimatedTotal ?: currentState.estimatedTotalPages,
                        batchSize = batchSize ?: currentState.batchSize,
                    )
                    else -> PagePreviewState.Success(
                        pagePreviews = previewPage.pagePreviews,
                        hasNextPage = previewPage.hasNextPage,
                        isLoadingMore = false,
                        manga = manga!!,
                        chapter = chapter!!,
                        source = source!!,
                        estimatedTotalPages = estimatedTotal,
                        batchSize = batchSize,
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

    fun jumpToPage(targetPage: Int) {
        if (targetPage < 1) return
        val current = state.value as? PagePreviewState.Success ?: return
        // If the page is already loaded, just scroll to it — no network.
        val existing = current.pagePreviews.indexOfFirst { it.index == targetPage }
        if (existing >= 0) {
            _scrollEvents.tryEmit(existing)
            return
        }
        val size = batchSize ?: return
        val targetBatch = ((targetPage - 1) / size) + 1
        if (isLoadingMore) return
        isLoadingMore = true
        mutableState.update { (it as? PagePreviewState.Success)?.copy(isLoadingMore = true) ?: it }
        screenModelScope.launch(Dispatchers.IO) {
            loadPage(targetBatch, replace = true)
            val updated = state.value as? PagePreviewState.Success ?: return@launch
            val idx = updated.pagePreviews.indexOfFirst { it.index == targetPage }
            _scrollEvents.tryEmit(if (idx >= 0) idx else 0)
        }
    }
}

private val EH_GALLERY_ID_REGEX = Regex("""/g/(\d+)/""")

private fun extractEhGalleryId(url: String): String? =
    EH_GALLERY_ID_REGEX.find(url)?.groupValues?.getOrNull(1)

sealed class PagePreviewState {
    data object Loading : PagePreviewState()

    data class Success(
        val pagePreviews: List<PagePreviewInfo>,
        val hasNextPage: Boolean,
        val isLoadingMore: Boolean = false,
        val manga: Manga,
        val chapter: Chapter,
        val source: Source,
        val estimatedTotalPages: Int? = null,
        val batchSize: Int? = null,
    ) : PagePreviewState()

    data class Error(val error: Throwable) : PagePreviewState()
}
