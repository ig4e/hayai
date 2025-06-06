package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.database.models.toDomainChapter
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import exh.source.isEhBasedSource
import exh.util.DataSaver
import exh.util.DataSaver.Companion.getImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

/**
 * Loader used to load chapters from an online source.
 */
@OptIn(DelicateCoroutinesApi::class)
internal class HttpPageLoader(
    private val chapter: ReaderChapter,
    private val source: HttpSource,
    private val chapterCache: ChapterCache = Injekt.get(),
    // SY -->
    private val readerPreferences: ReaderPreferences = Injekt.get(),
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    // SY <--
) : PageLoader() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * A queue used to manage requests one by one while allowing priorities.
     */
    private val queue = PriorityBlockingQueue<PriorityPage>()

    // SY -->
    private val dataSaver = DataSaver(source, sourcePreferences)
    // SY <--

    init {
        // EXH -->
        repeat(readerPreferences.readerThreads().get()) {
            // EXH <--
            scope.launchIO {
                flow {
                    while (true) {
                        emit(runInterruptible { queue.take() }.page)
                    }
                }
                    .filter { it.status == Page.State.QUEUE }
                    .collect(::internalLoadPage)
            }
            // EXH -->
        }
        // EXH <--
    }

    override var isLocal: Boolean = false

    /**
     * Returns the page list for a chapter. It tries to return the page list from the local cache,
     * otherwise fallbacks to network.
     */
    override suspend fun getPages(): List<ReaderPage> {
        val pages = try {
            chapterCache.getPageListFromCache(chapter.chapter.toDomainChapter()!!)
        } catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }
            source.getPageList(chapter.chapter)
        }
        // SY -->
        val rp = pages.mapIndexed { index, page ->
            // Don't trust sources and use our own indexing
            ReaderPage(index, page.url, page.imageUrl)
        }
        if (readerPreferences.aggressivePageLoading().get()) {
            rp.forEach {
                if (it.status == Page.State.QUEUE) {
                    queue.offer(PriorityPage(it, 0))
                }
            }
        }
        return rp
        // SY <--
    }

    /**
     * Loads a page through the queue. Handles re-enqueueing pages if they were evicted from the cache.
     */
    override suspend fun loadPage(page: ReaderPage) = withIOContext {
        val imageUrl = page.imageUrl

        // Check if the image has been deleted
        if (page.status == Page.State.READY && imageUrl != null && !chapterCache.isImageInCache(imageUrl)) {
            page.status = Page.State.QUEUE
        }

        // Automatically retry failed pages when subscribed to this page
        if (page.status == Page.State.ERROR) {
            page.status = Page.State.QUEUE
        }

        val queuedPages = mutableListOf<PriorityPage>()
        if (page.status == Page.State.QUEUE) {
            queuedPages += PriorityPage(page, 1).also { queue.offer(it) }
        }

        // Apply preloading based on settings
        val shouldForcePreload = readerPreferences.forcePreload().get()
        val preloadAmount = readerPreferences.preloadSize().get()

        if (shouldForcePreload) {
            // Always preload next pages regardless of position
            queuedPages += forcePreloadPages(page, preloadAmount)
        } else {
            // Only preload pages when current page is loaded
            queuedPages += preloadNextPages(page, preloadAmount)
        }

        suspendCancellableCoroutine<Nothing> { continuation ->
            continuation.invokeOnCancellation {
                queuedPages.forEach {
                    if (it.page.status == Page.State.QUEUE) {
                        queue.remove(it)
                    }
                }
            }
        }
    }

    /**
     * Retries a page. This method is only called from user interaction on the viewer.
     */
    override fun retryPage(page: ReaderPage) {
        if (page.status == Page.State.ERROR) {
            page.status = Page.State.QUEUE
        }
        // EXH -->
        // Grab a new image URL on EXH sources
        if (source.isEhBasedSource()) {
            page.imageUrl = null
        }

        if (readerPreferences.readerInstantRetry().get()) // EXH <--
            {
                boostPage(page)
            } else {
            // EXH <--
            queue.offer(PriorityPage(page, 2))
        }
    }

    override fun recycle() {
        super.recycle()
        scope.cancel()
        queue.clear()

        // Cache current page list progress for online chapters to allow a faster reopen
        chapter.pages?.let { pages ->
            launchIO {
                try {
                    // Convert to pages without reader information
                    val pagesToSave = pages.map { Page(it.index, it.url, it.imageUrl) }
                    chapterCache.putPageListToCache(chapter.chapter.toDomainChapter()!!, pagesToSave)
                } catch (e: Throwable) {
                    if (e is CancellationException) {
                        throw e
                    }
                }
            }
        }
    }

    /**
     * Preloads the given [amount] of pages after the [currentPage] with a lower priority.
     *
     * @return a list of [PriorityPage] that were added to the [queue]
     */
    private fun preloadNextPages(currentPage: ReaderPage, amount: Int): List<PriorityPage> {
        val pageIndex = currentPage.index
        val pages = currentPage.chapter.pages ?: return emptyList()
        if (pageIndex == pages.lastIndex) return emptyList()

        return pages
            .subList(pageIndex + 1, min(pageIndex + 1 + amount, pages.size))
            .mapNotNull {
                if (it.status == Page.State.QUEUE) {
                    PriorityPage(it, 0).apply { queue.offer(this) }
                } else {
                    null
                }
            }
    }

    /**
     * Aggressively preload pages when force preload is enabled.
     * This will preload all remaining pages in the chapter.
     */
    private fun forcePreloadPages(currentPage: ReaderPage, amount: Int): List<PriorityPage> {
        val pageIndex = currentPage.index
        val pages = currentPage.chapter.pages ?: return emptyList()
        if (pageIndex == pages.lastIndex) return emptyList()

        // Calculate how many pages we should preload ahead
        val endIndex = min(pageIndex + 1 + amount, pages.size)

        return pages
            .subList(pageIndex + 1, endIndex)
            .mapNotNull {
                if (it.status == Page.State.QUEUE) {
                    // Use higher priority (0) for first few pages for immediate reading experience
                    val priority = if (it.index < pageIndex + 3) 0 else 1
                    PriorityPage(it, priority).apply { queue.offer(this) }
                } else {
                    null
                }
            }
    }

    /**
     * Loads the page, retrieving the image URL and downloading the image if necessary.
     * Downloaded images are stored in the chapter cache.
     *
     * @param page the page whose source image has to be downloaded.
     */
    private suspend fun internalLoadPage(page: ReaderPage) {
        try {
            if (page.imageUrl.isNullOrEmpty()) {
                page.status = Page.State.LOAD_PAGE
                page.imageUrl = source.getImageUrl(page)
            }
            val imageUrl = page.imageUrl!!

            if (!chapterCache.isImageInCache(imageUrl)) {
                page.status = Page.State.DOWNLOAD_IMAGE
                val imageResponse = source.getImage(page, dataSaver)
                chapterCache.putImageToCache(imageUrl, imageResponse)
            }

            page.stream = { chapterCache.getImageFile(imageUrl).inputStream() }
            page.status = Page.State.READY
        } catch (e: Throwable) {
            page.status = Page.State.ERROR
            if (e is CancellationException) {
                throw e
            }
        }
    }

    // EXH -->
    fun boostPage(page: ReaderPage) {
        if (page.status == Page.State.QUEUE) {
            scope.launchIO {
                loadPage(page)
            }
        }
    }
    // EXH <--
}

/**
 * Data class used to keep ordering of pages in order to maintain priority.
 */
private class PriorityPage(
    val page: ReaderPage,
    val priority: Int,
) : Comparable<PriorityPage> {
    companion object {
        private val idGenerator = AtomicInteger()
    }

    private val identifier = idGenerator.incrementAndGet()

    override fun compareTo(other: PriorityPage): Int {
        val p = other.priority.compareTo(priority)
        return if (p != 0) p else identifier.compareTo(other.identifier)
    }
}
