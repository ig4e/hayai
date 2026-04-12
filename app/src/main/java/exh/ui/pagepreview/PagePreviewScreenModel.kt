package exh.ui.pagepreview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
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

    private val page = MutableStateFlow(1)

    var pageDialogOpen by mutableStateOf(false)

    init {
        screenModelScope.launch(Dispatchers.IO) {
            val manga = getManga.awaitById(mangaId)
            if (manga == null) {
                mutableState.update {
                    PagePreviewState.Error(Exception("Manga not found"))
                }
                return@launch
            }
            val chapters = getChapter.awaitAll(mangaId, filterScanlators = false)
            val chapter = chapters.minByOrNull { it.source_order }
            if (chapter == null) {
                mutableState.update {
                    PagePreviewState.Error(Exception("No chapters found"))
                }
                return@launch
            }
            val source = sourceManager.getOrStub(manga.source)
            val previewSource = source.getMainSource<PagePreviewSource>()
            if (previewSource == null) {
                mutableState.update {
                    PagePreviewState.Error(Exception("Source does not support page previews"))
                }
                return@launch
            }
            page
                .onEach { currentPage ->
                    try {
                        val previewPage = previewSource.getPagePreviewList(
                            manga,
                            chapters.sortedByDescending { it.source_order },
                            currentPage,
                        )
                        mutableState.update { currentState ->
                            when (currentState) {
                                PagePreviewState.Loading, is PagePreviewState.Error -> {
                                    PagePreviewState.Success(
                                        page = currentPage,
                                        pagePreviews = previewPage.pagePreviews,
                                        hasNextPage = previewPage.hasNextPage,
                                        pageCount = previewPage.pagePreviewPages,
                                        manga = manga,
                                        chapter = chapter,
                                        source = source,
                                    )
                                }
                                is PagePreviewState.Success -> currentState.copy(
                                    page = currentPage,
                                    pagePreviews = previewPage.pagePreviews,
                                    hasNextPage = previewPage.hasNextPage,
                                    pageCount = previewPage.pagePreviewPages,
                                )
                            }
                        }
                    } catch (e: Exception) {
                        mutableState.update {
                            PagePreviewState.Error(e)
                        }
                    }
                }
                .catch { e ->
                    mutableState.update {
                        PagePreviewState.Error(e)
                    }
                }
                .collect()
        }
    }

    fun moveToPage(page: Int) {
        this.page.value = page
    }
}

sealed class PagePreviewState {
    data object Loading : PagePreviewState()

    data class Success(
        val page: Int,
        val pagePreviews: List<PagePreviewInfo>,
        val hasNextPage: Boolean,
        val pageCount: Int?,
        val manga: Manga,
        val chapter: Chapter,
        val source: Source,
    ) : PagePreviewState()

    data class Error(val error: Throwable) : PagePreviewState()
}
