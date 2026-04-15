package hayai.novel.reader

import android.graphics.PointF
import android.os.Build
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.BaseViewer
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import uy.kohesive.injekt.injectLazy

/**
 * Novel text viewer following the exact same architecture as WebtoonViewer.
 *
 * Uses RecyclerView with NovelAdapter for chapter content
 * and transitions. Supports the same navigation, key events, and lifecycle
 * patterns as the manga viewers.
 */
class NovelViewer(val activity: ReaderActivity) : BaseViewer {

    val downloadManager: DownloadManager by injectLazy()

    private val scope = MainScope()

    /**
     * RecyclerView for displaying novel content and chapter transitions.
     */
    private val recycler = RecyclerView(activity).apply {
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        itemAnimator = null
    }

    /**
     * Frame containing the recycler view.
     */
    private val frame = FrameLayout(activity).apply {
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    }

    /**
     * Scroll distance for tap/key navigation (3/4 of screen height).
     */
    private val scrollDistance = activity.resources.displayMetrics.heightPixels * 3 / 4

    /**
     * Layout manager for the recycler view.
     */
    private val layoutManager = LinearLayoutManager(activity)

    /**
     * Adapter managing novel pages and transitions.
     */
    private val adapter = NovelAdapter(this)

    /**
     * Currently active item (ReaderPage or ChapterTransition).
     */
    private var currentPage: Any? = null

    /**
     * Measured heights for chapter pages, used for chapter-local progress and restore.
     */
    private val measuredPageHeights = mutableMapOf<ReaderPage, Int>()

    private var pendingRestoreChapterId: Long? = null
    private var pendingRestorePercent: Int? = null
    private var pendingRestoreAttempts: Int = 0
    private var lastRestoreMeasuredCount: Int = 0

    private var lastReportedChapterId: Long? = null
    private var lastReportedProgress: Int = -1

    /**
     * Configuration for the novel reader (font, colors, spacing, navigation).
     */
    val config = NovelConfig(scope)

    /**
     * Gesture detector for tap navigation (avoids consuming scroll/fling events).
     */
    private val gestureDetector = GestureDetector(
        activity,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                // Correct tap coordinates accounting for system insets
                val viewPosition = IntArray(2)
                recycler.getLocationOnScreen(viewPosition)
                val pos = PointF(
                    (e.rawX - viewPosition[0]) / recycler.width,
                    (e.rawY - viewPosition[1]) / recycler.height,
                )
                when (config.navigator.getAction(pos)) {
                    ViewerNavigation.NavigationRegion.MENU -> activity.toggleMenu()
                    ViewerNavigation.NavigationRegion.NEXT,
                    ViewerNavigation.NavigationRegion.RIGHT,
                    -> moveToNext()
                    ViewerNavigation.NavigationRegion.PREV,
                    ViewerNavigation.NavigationRegion.LEFT,
                    -> moveToPrevious()
                }
                return true
            }
        },
    )

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            recycler.setItemViewCacheSize(RECYCLER_VIEW_CACHE_SIZE)
        }
        recycler.isVisible = false // Don't layout until chapters are set
        recycler.layoutManager = layoutManager
        recycler.adapter = adapter

        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                onScrolled()

                if (dy > config.menuThreshold || dy < -config.menuThreshold) {
                    activity.hideMenu()
                }

                if (dy < 0) {
                    val firstIndex = layoutManager.findFirstVisibleItemPosition()
                    val firstItem = adapter.items.getOrNull(firstIndex)
                    if (firstItem is ChapterTransition.Prev && firstItem.to != null) {
                        activity.requestPreloadChapter(firstItem.to)
                    }
                }

                val lastIndex = layoutManager.findLastVisibleItemPosition()
                val lastItem = adapter.items.getOrNull(lastIndex)
                if (lastItem is ChapterTransition.Next && lastItem.to == null) {
                    activity.showMenu()
                }
            }
        })

        // Tap navigation via gesture detector
        recycler.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false // Don't consume - let RecyclerView handle scrolling
        }

        config.textPropertyChangedListener = {
            refreshAdapter()
        }

        config.navigationModeChangedListener = {
            val showOnStart = config.navigationOverlayForNewUser
            activity.binding.navigationOverlay.setNavigation(config.navigator, showOnStart)
        }

        config.navigationModeInvertedListener = {
            activity.binding.navigationOverlay.showNavigationAgain()
        }

        frame.addView(recycler)
    }

    override fun getView(): View = frame

    override fun destroy() {
        super.destroy()
        scope.cancel()
    }

    fun onContentTouch(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    fun onPageContentMeasured(page: ReaderPage, pixelHeight: Int) {
        if (pixelHeight <= 0) return
        measuredPageHeights[page] = pixelHeight
        if (pendingRestoreChapterId == page.chapter.chapter.id) {
            recycler.post { restoreSavedProgressIfReady() }
        }
    }

    private fun checkAllowPreload(page: ReaderPage?): Boolean {
        // Transition page selected.
        page ?: return true

        // Initial selection.
        currentPage ?: return true

        val nextItem = adapter.items.getOrNull(adapter.items.size - 1)
        val nextChapter = (nextItem as? ChapterTransition.Next)?.to ?: (nextItem as? ReaderPage)?.chapter

        return when (page.chapter) {
            (currentPage as? ReaderPage)?.chapter -> true
            nextChapter -> true
            else -> false
        }
    }

    private fun onPageSelected(page: ReaderPage, allowPreload: Boolean) {
        val pages = page.chapter.pages ?: return
        Logger.d { "Novel onPageSelected: ${page.number}/${pages.size}" }
        activity.onPageSelected(page, false)

        // Preload next chapter when near end of current
        val inPreloadRange = pages.size - page.number < 5
        if (inPreloadRange && allowPreload && page.chapter == adapter.currentChapter) {
            val nextItem = adapter.items.getOrNull(adapter.items.size - 1)
            val transitionChapter = (nextItem as? ChapterTransition.Next)?.to ?: (nextItem as? ReaderPage)?.chapter
            if (transitionChapter != null) {
                activity.requestPreloadChapter(transitionChapter)
            }
        }
    }

    private fun onTransitionSelected(transition: ChapterTransition) {
        Logger.d { "Novel onTransitionSelected: $transition" }
        val toChapter = transition.to
        if (toChapter != null) {
            activity.requestPreloadChapter(toChapter)
        }
    }

    override fun setChapters(chapters: ViewerChapters) {
        Logger.d { "Novel setChapters" }
        val forceTransition = config.alwaysShowChapterTransition || currentPage is ChapterTransition
        adapter.setChapters(chapters, forceTransition)
        measuredPageHeights.clear()
        lastReportedChapterId = null
        lastReportedProgress = -1

        pendingRestoreChapterId = chapters.currChapter.chapter.id
        pendingRestorePercent = chapters.currChapter.chapter.last_page_read
            .coerceIn(0, 100)
            .takeIf { it > 0 }
        pendingRestoreAttempts = 0
        lastRestoreMeasuredCount = 0

        if (recycler.isGone) {
            val pages = chapters.currChapter.pages ?: return
            moveToPage(pages[min(chapters.currChapter.requestedPage, pages.lastIndex)])
            recycler.isVisible = true
        }

        if (pendingRestorePercent != null) {
            recycler.post { restoreSavedProgressIfReady() }
        }
    }

    override fun moveToPage(page: ReaderPage, animated: Boolean) {
        val position = adapter.items.indexOf(page)
        if (position != -1) {
            recycler.scrollToPosition(position)
            if (layoutManager.findLastVisibleItemPosition() == -1) {
                onScrolled(position)
            }
        } else {
            Logger.d { "Novel page $page not found in adapter" }
        }
    }

    private fun onScrolled(pos: Int? = null) {
        val position = pos ?: layoutManager.findLastVisibleItemPosition()
        val item = adapter.items.getOrNull(position)
        val allowPreload = checkAllowPreload(item as? ReaderPage)
        if (item != null && currentPage != item) {
            currentPage = item
            when (item) {
                is ReaderPage -> onPageSelected(item, allowPreload)
                is ChapterTransition -> onTransitionSelected(item)
            }
        }

        if (item is ReaderPage) {
            updateChapterProgress(item)
        }
    }

    private fun updateChapterProgress(page: ReaderPage) {
        val chapter = page.chapter.chapter
        val chapterId = chapter.id

        if (pendingRestorePercent != null && pendingRestoreChapterId == chapterId) {
            return
        }

        val progress = computeChapterProgress(page) ?: return
        chapter.last_page_read = progress
        chapter.pages_left = (100 - progress).coerceIn(0, 100)

        if (chapterId == null) {
            activity.onNovelProgressChanged(page, progress)
            return
        }

        if (lastReportedChapterId != chapterId) {
            lastReportedChapterId = chapterId
            lastReportedProgress = -1
        }

        if (progress != lastReportedProgress) {
            lastReportedProgress = progress
            activity.onNovelProgressChanged(page, progress)
        }
    }

    private fun computeChapterProgress(page: ReaderPage): Int? {
        val pages = page.chapter.pages ?: return null
        if (pages.isEmpty()) return null

        val recyclerHeight = recycler.height
        if (recyclerHeight <= 0) return null

        val pagePosition = adapter.items.indexOf(page)
        if (pagePosition == -1) return null

        val pageView = layoutManager.findViewByPosition(pagePosition) ?: return null
        val currentPageHeight = getPageHeight(page)
        if (currentPageHeight <= 0) return null

        val cumulativeOffset = pages
            .takeWhile { it != page }
            .sumOf { getPageHeight(it) }

        val maxOffsetInPage = max(currentPageHeight - recyclerHeight, 0)
        val offsetInPage = (-pageView.top).coerceIn(0, maxOffsetInPage)

        val chapterHeight = pages.sumOf { getPageHeight(it) }
        if (chapterHeight <= 0) return null

        val maxScrollable = max(chapterHeight - recyclerHeight, 0)
        if (maxScrollable <= 0) {
            // If content fits on screen, keep existing progress instead of auto-marking as fully read.
            return page.chapter.chapter.last_page_read.coerceIn(0, 100)
        }

        val offset = (cumulativeOffset + offsetInPage).coerceIn(0, maxScrollable)
        return ((offset.toFloat() / maxScrollable) * 100f).roundToInt().coerceIn(0, 100)
    }

    private fun getPageHeight(page: ReaderPage): Int {
        measuredPageHeights[page]?.let { measured ->
            if (measured > 0) return measured
        }
        return 0
    }

    fun moveToProgress(progressPercent: Int) {
        val clampedProgress = progressPercent.coerceIn(0, 100)
        clearPendingRestore()

        if (!scrollToChapterProgress(clampedProgress)) {
            recycler.post { scrollToChapterProgress(clampedProgress) }
        }
    }

    private fun restoreSavedProgressIfReady() {
        val chapterId = pendingRestoreChapterId ?: return
        val savedProgress = pendingRestorePercent ?: return
        val currentChapter = adapter.currentChapter ?: return
        if (currentChapter.chapter.id != chapterId) return

        val pages = currentChapter.pages ?: return
        val measuredCount = pages.count { getPageHeight(it) > 0 }

        if (measuredCount == 0) {
            if (pendingRestoreAttempts++ < MAX_PENDING_RESTORE_ATTEMPTS) {
                recycler.post { restoreSavedProgressIfReady() }
            } else {
                clearPendingRestore()
            }
            return
        }

        val restored = scrollToChapterProgress(savedProgress, allowEstimatedHeights = true)
        if (!restored) {
            if (pendingRestoreAttempts++ < MAX_PENDING_RESTORE_ATTEMPTS) {
                recycler.post { restoreSavedProgressIfReady() }
            } else {
                clearPendingRestore()
            }
            return
        }

        if (measuredCount == pages.size || pendingRestoreAttempts >= MAX_PENDING_RESTORE_ATTEMPTS) {
            clearPendingRestore()
        } else if (measuredCount > lastRestoreMeasuredCount) {
            lastRestoreMeasuredCount = measuredCount
            pendingRestoreAttempts++
            recycler.post { restoreSavedProgressIfReady() }
        } else {
            clearPendingRestore()
        }
    }

    private fun scrollToChapterProgress(progressPercent: Int, allowEstimatedHeights: Boolean = false): Boolean {
        val currentChapter = adapter.currentChapter ?: return false

        val pages = currentChapter.pages ?: return false
        if (pages.isEmpty()) return false

        val recyclerHeight = recycler.height
        if (recyclerHeight <= 0) {
            return false
        }

        val pageHeights = pages.map { page ->
            val measured = measuredPageHeights[page]
            if (measured != null && measured > 0) {
                measured
            } else {
                val index = adapter.items.indexOf(page)
                if (index == -1) 0 else layoutManager.findViewByPosition(index)?.height ?: 0
            }
        }

        val measuredHeights = pageHeights.filter { it > 0 }
        if (measuredHeights.isEmpty()) {
            return false
        }
        if (!allowEstimatedHeights && measuredHeights.size != pageHeights.size) {
            return false
        }

        val estimatedHeight = measuredHeights.average().roundToInt().coerceAtLeast(recyclerHeight)
        val resolvedHeights = pageHeights.map { if (it > 0) it else estimatedHeight }

        val chapterHeight = resolvedHeights.sum()
        val maxScrollable = max(chapterHeight - recyclerHeight, 0)
        val targetOffset = if (maxScrollable <= 0) 0 else {
            (maxScrollable * (progressPercent / 100f)).roundToInt().coerceIn(0, maxScrollable)
        }

        var remainingOffset = targetOffset
        var targetPageIndex = 0
        for (index in pages.indices) {
            val pageHeight = resolvedHeights[index]
            if (index == pages.lastIndex || remainingOffset < pageHeight) {
                targetPageIndex = index
                break
            }
            remainingOffset -= pageHeight
        }

        if (targetPageIndex == pages.lastIndex) {
            remainingOffset = remainingOffset.coerceIn(0, max(resolvedHeights[targetPageIndex] - recyclerHeight, 0))
        }

        val targetPage = pages[targetPageIndex]
        val targetPosition = adapter.items.indexOf(targetPage)
        if (targetPosition == -1) return false

        layoutManager.scrollToPositionWithOffset(targetPosition, -remainingOffset)
        onScrolled(targetPosition)
        return true
    }

    private fun clearPendingRestore() {
        pendingRestoreChapterId = null
        pendingRestorePercent = null
        pendingRestoreAttempts = 0
        lastRestoreMeasuredCount = 0
    }

    override fun moveToPrevious() {
        recycler.smoothScrollBy(0, -scrollDistance)
    }

    override fun moveToNext() {
        recycler.smoothScrollBy(0, scrollDistance)
    }

    override fun handleKeyEvent(event: KeyEvent): Boolean {
        val isUp = event.action == KeyEvent.ACTION_UP

        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (!config.volumeKeysEnabled || activity.menuVisible) {
                    return false
                } else if (isUp) {
                    if (!config.volumeKeysInverted) moveToNext() else moveToPrevious()
                }
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (!config.volumeKeysEnabled || activity.menuVisible) {
                    return false
                } else if (isUp) {
                    if (!config.volumeKeysInverted) moveToPrevious() else moveToNext()
                }
            }
            KeyEvent.KEYCODE_MENU -> if (isUp) activity.toggleMenu()

            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_PAGE_UP,
            -> if (isUp) moveToPrevious()

            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_PAGE_DOWN,
            -> if (isUp) moveToNext()
            else -> return false
        }
        return true
    }

    override fun handleGenericMotionEvent(event: MotionEvent): Boolean = false

    private fun refreshAdapter() {
        if (adapter.itemCount == 0) return

        val firstVisible = layoutManager.findFirstVisibleItemPosition()
            .takeIf { it != RecyclerView.NO_POSITION }
            ?: layoutManager.findLastVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION) return

        val start = max(0, firstVisible - 1)
        val end = min(adapter.itemCount - 1, layoutManager.findLastVisibleItemPosition().coerceAtLeast(firstVisible) + 1)
        val count = end - start + 1

        adapter.items
            .subList(start, end + 1)
            .filterIsInstance<ReaderPage>()
            .forEach { measuredPageHeights.remove(it) }
        lastReportedProgress = -1
        adapter.notifyItemRangeChanged(start, count)
    }
}

private const val RECYCLER_VIEW_CACHE_SIZE = 4
private const val MAX_PENDING_RESTORE_ATTEMPTS = 4
