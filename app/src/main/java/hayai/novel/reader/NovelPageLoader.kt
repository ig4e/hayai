package hayai.novel.reader

import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.source.model.Page
import hayai.novel.source.NovelSource
import kotlinx.coroutines.CancellationException
import java.io.ByteArrayInputStream

/**
 * PageLoader for online novel chapters.
 * Fetches chapter text via NovelSource.getChapterText() and serves it
 * as a single-page stream (UTF-8 encoded HTML).
 */
interface NovelImageUrlResolver {
    fun resolveNovelImageUrl(url: String): String
}

class NovelPageLoader(
    private val chapter: ReaderChapter,
    private val source: NovelSource,
) : PageLoader(), NovelImageUrlResolver {

    override val isLocal: Boolean = false

    override suspend fun getPages(): List<ReaderPage> {
        return listOf(ReaderPage(index = 0, url = source.resolveUrl(chapter.chapter.url)).apply {
            this.chapter = this@NovelPageLoader.chapter
        })
    }

    override suspend fun loadPage(page: ReaderPage) {
        if (page.status == Page.State.Ready) return

        page.status = Page.State.LoadPage
        try {
            val html = source.getChapterText(chapter.chapter.url)
            val bytes = html.toByteArray(Charsets.UTF_8)
            page.stream = { ByteArrayInputStream(bytes) }
            page.status = Page.State.Ready
        } catch (e: Throwable) {
            page.status = Page.State.Error
            if (e is CancellationException) throw e
            Logger.e(e) { "NovelPageLoader: Failed to load chapter ${chapter.chapter.url}" }
        }
    }

    override fun retryPage(page: ReaderPage) {
        page.status = Page.State.Queue
    }

    override fun resolveNovelImageUrl(url: String): String {
        return source.resolveUrl(url)
    }
}
