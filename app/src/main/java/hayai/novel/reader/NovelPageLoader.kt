package hayai.novel.reader

import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.source.model.Page
import hayai.novel.source.NovelSource
import java.io.ByteArrayInputStream

/**
 * PageLoader for online novel chapters.
 * Fetches chapter text via NovelSource.getChapterText() and serves it
 * as a single-page stream (UTF-8 encoded HTML).
 */
class NovelPageLoader(
    private val chapter: ReaderChapter,
    private val source: NovelSource,
) : PageLoader() {

    override val isLocal: Boolean = false

    override suspend fun getPages(): List<ReaderPage> {
        return listOf(ReaderPage(index = 0).apply {
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
        } catch (e: Exception) {
            page.status = Page.State.Error
            throw e
        }
    }

    override fun retryPage(page: ReaderPage) {
        page.status = Page.State.Queue
    }
}
