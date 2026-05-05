package eu.kanade.tachiyomi.ui.reader.loader

import androidx.annotation.CallSuper
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage

/**
 * A loader used to load pages into the reader. Any open resources must be cleaned up when the
 * method [recycle] is called.
 */
abstract class PageLoader {

    abstract val isLocal: Boolean

    /**
     * Whether this loader has been already recycled.
     */
    var isRecycled = false
        private set

    /**
     * Recycles this loader. Implementations must override this method to clean up any active
     * resources.
     */
    @CallSuper
    open fun recycle() {
        isRecycled = true
    }

    /**
     * Returns the list of pages of a chapter.
     */
    abstract suspend fun getPages(): List<ReaderPage>

    /**
     * Loads the page. May also preload other pages.
     * Progress of the page loading should be followed via [page.statusFlow].
     * [loadPage] is not currently guaranteed to complete, so it should be launched asynchronously.
     */
    abstract suspend fun loadPage(page: ReaderPage)

    /**
     * Retries the given [page] in case it failed to load. This method only makes sense when an
     * online source is used.
     */
    open fun retryPage(page: ReaderPage) {}

    /**
     * Returns an [InputStream] for an arbitrary asset referenced by [path] inside the chapter
     * (e.g. an EPUB image at `hayai-novel-image://chapter/img.png`). Implementations that don't
     * carry packaged assets return null, in which case callers should silently skip the asset.
     */
    open fun getPageDataStream(path: String): java.io.InputStream? = null
}
