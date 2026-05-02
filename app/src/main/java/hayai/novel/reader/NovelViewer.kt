package hayai.novel.reader

import android.graphics.PointF
import android.os.Build
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
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
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.BaseViewer
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import hayai.novel.preferences.NovelPreferences
import hayai.novel.reader.autoscroll.AutoScroller
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    private val preferences: PreferencesHelper by injectLazy()
    private val novelPreferences: NovelPreferences by injectLazy()

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
     * Choreographer-driven auto-scroll. Off by default; toggled from the reader settings sheet.
     * Speed is read live from [NovelPreferences.autoScrollSpeed] so the in-sheet slider can adjust
     * mid-flight without restarting the scroller.
     */
    val autoScroller = AutoScroller(
        recycler = recycler,
        initialSpeedPxPerSec = novelPreferences.autoScrollSpeed().get(),
    )

    /**
     * Most recently parsed chapter content. Cached so the TTS highlight pipeline can walk pages
     * → block ranges without re-parsing on every word transition. Refreshed on every call to
     * [currentChapterBlocks]; the controller calls that on each playback start.
     */
    private var lastParsedChapter: ParsedChapter? = null

    /**
     * Parses the active chapter's HTML into [NovelBlock]s on demand. Used by the TTS controller
     * to grab text for narration without having to wait for the page holder to render. Returns
     * null when the chapter isn't loaded yet (no `Ready` page on the current chapter).
     */
    fun currentChapterBlocks(): List<NovelBlock>? {
        val chapter = adapter.currentChapter ?: return null
        val pages = chapter.pages ?: return null
        val perPage = mutableListOf<ParsedPage>()
        for (page in pages) {
            val streamFn = page.stream ?: continue
            val html = runCatching { streamFn().bufferedReader(Charsets.UTF_8).use { it.readText() } }
                .getOrNull() ?: continue
            val imageResolver = page.chapter.pageLoader as? NovelImageUrlResolver
            val parsed = runCatching {
                NovelHtmlParser.parse(
                    html = html,
                    baseUrl = page.url.takeIf { it.isNotBlank() },
                    imageUrlResolver = imageResolver?.let { r -> { url -> r.resolveNovelImageUrl(url) } },
                )
            }.getOrNull() ?: continue
            perPage += ParsedPage(page = page, blocks = parsed)
        }
        if (perPage.isEmpty()) return null
        lastParsedChapter = ParsedChapter(perPage)
        return perPage.flatMap { it.blocks }
    }

    /**
     * Resolves a global block index (within [currentChapterBlocks]) to the [HighlightDispatcher.TextSegment]
     * that's currently rendering the requested character within the block.
     *
     * Walks the cached per-page block layout to find the page containing the global index, looks
     * up that page's `NovelPageHolder` in the recycler, and asks the holder for the right
     * TextView. Returns null when the page isn't currently bound (recycled / scrolled off
     * screen) — callers (the highlight dispatcher) handle that gracefully.
     */
    fun textSegmentForBlock(globalBlockIndex: Int, charInBlock: Int): hayai.novel.tts.playback.HighlightDispatcher.TextSegment? {
        val parsed = lastParsedChapter ?: return null
        var offset = 0
        for (parsedPage in parsed.pages) {
            val count = parsedPage.blocks.size
            if (globalBlockIndex < offset + count) {
                val localIndex = globalBlockIndex - offset
                val adapterPos = adapter.items.indexOf(parsedPage.page)
                if (adapterPos < 0) return null
                val holder = recycler.findViewHolderForAdapterPosition(adapterPos) as? NovelPageHolder ?: return null
                return holder.textViewForBlockChar(localIndex, charInBlock)
            }
            offset += count
        }
        return null
    }

    /**
     * Programmatically advance to the next chapter for TTS continuous reading. Returns true if a
     * next chapter exists in the current viewer-chapters set; the actual chapter swap happens
     * asynchronously through the existing [eu.kanade.tachiyomi.ui.reader.ReaderViewModel.onPageSelected]
     * flow once the recycler scrolls onto the next chapter's first page.
     */
    fun advanceToNextChapterForTts(): Boolean {
        val nextChapter = adapter.items.asSequence()
            .mapNotNull { it as? ChapterTransition.Next }
            .firstOrNull()
            ?.to ?: return false
        val firstPage = nextChapter.pages?.firstOrNull() ?: return false
        moveToPage(firstPage)
        return true
    }

    private data class ParsedChapter(val pages: List<ParsedPage>)
    private data class ParsedPage(val page: ReaderPage, val blocks: List<NovelBlock>)

    /**
     * Gesture detector for tap navigation (avoids consuming scroll/fling events).
     */
    private val gestureDetector = GestureDetector(
        activity,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                // A pinch-zoom that just ended also fires a single-tap event for the lifted
                // finger; ignore it so the user doesn't get a stray page-nav tap.
                if (suppressNextTap) {
                    suppressNextTap = false
                    return true
                }
                // A tap that paused auto-scroll (handled in the touch listener) shouldn't also
                // navigate — the user's intent was just to pause.
                if (autoScrollPausedByTouch) {
                    autoScrollPausedByTouch = false
                    return true
                }
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

    /**
     * Flag set by [scaleDetector]'s end callback so the trailing single-tap event from the
     * gesture's lifted finger doesn't trigger page navigation.
     */
    private var suppressNextTap: Boolean = false

    /**
     * True when the most-recent ACTION_DOWN landed while [autoScroller] was running. The single-
     * tap handler reads this to suppress page-nav: a tap on a running scroller is meant to
     * pause, not navigate, so we consume that tap silently.
     */
    private var autoScrollPausedByTouch: Boolean = false

    /**
     * Pinch-to-zoom font size. We capture the baseline size at the start of the gesture, track
     * the cumulative scale factor during the pinch, and only apply / persist the new size on
     * gesture end — refreshing the recycler mid-pinch produces visible jank as visible items
     * rebind.
     */
    private var pinchBaselineFontSize: Int = 0
    private var pinchScaleAccumulator: Float = 1f

    private val scaleDetector = ScaleGestureDetector(
        activity,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                pinchBaselineFontSize = config.fontSize
                pinchScaleAccumulator = 1f
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                pinchScaleAccumulator *= detector.scaleFactor
                // Bound the accumulator so a runaway gesture can't compute an absurd target size
                // before the end-of-gesture clamp.
                pinchScaleAccumulator = pinchScaleAccumulator.coerceIn(0.4f, 3f)
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                val proposed = (pinchBaselineFontSize * pinchScaleAccumulator).roundToInt()
                    .coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
                suppressNextTap = true
                if (proposed != pinchBaselineFontSize) {
                    preferences.novelFontSize().set(proposed)
                }
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

        // Tap navigation + pinch-to-zoom font size + auto-scroll pause. Scale detector first so
        // it can claim multi-touch events before the tap detector sees them. ACTION_DOWN pauses
        // the auto-scroller (when enabled) so the user can read freely; a subsequent single-tap
        // is consumed silently by the gesture detector via `autoScrollPausedByTouch`.
        recycler.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN &&
                autoScroller.isRunning.value &&
                novelPreferences.autoScrollPauseOnTap().get()
            ) {
                autoScroller.stop()
                autoScrollPausedByTouch = true
            }
            scaleDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            false // Don't consume - let RecyclerView handle scrolling
        }

        // Live speed updates from the reader-sheet slider.
        novelPreferences.autoScrollSpeed().changes()
            .onEach { autoScroller.setSpeed(it) }
            .launchIn(scope)

        // Hide the reader chrome whenever auto-scroll starts so the page isn't covered.
        autoScroller.isRunning
            .onEach { running -> if (running) activity.hideMenu() }
            .launchIn(scope)

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

        updateChapterProgress()
    }

    /**
     * Computes and reports progress for [adapter.currentChapter].
     *
     * Progress is intentionally decoupled from the "last visible item": the adapter holds two
     * teaser pages of each adjacent chapter, and using one of those as the progress anchor would
     * walk a sibling chapter's full page list with mostly-unmeasured heights, producing bogus
     * 0% / 100% values that then corrupt the sibling chapter's last_page_read.
     */
    private fun updateChapterProgress() {
        val currChapter = adapter.currentChapter ?: return
        val chapter = currChapter.chapter
        val chapterId = chapter.id

        if (pendingRestorePercent != null && pendingRestoreChapterId == chapterId) {
            return
        }

        val progress = computeCurrentChapterProgress() ?: return
        chapter.last_page_read = progress
        chapter.pages_left = (100 - progress).coerceIn(0, 100)

        val pages = currChapter.pages ?: return
        val anchorPage = visibleCurrentChapterPage(pages) ?: pages.firstOrNull() ?: return

        if (chapterId == null) {
            activity.onNovelProgressChanged(anchorPage, progress)
            return
        }

        if (lastReportedChapterId != chapterId) {
            lastReportedChapterId = chapterId
            lastReportedProgress = -1
        }

        if (progress != lastReportedProgress) {
            lastReportedProgress = progress
            activity.onNovelProgressChanged(anchorPage, progress)
        }
    }

    private fun computeCurrentChapterProgress(): Int? {
        val currChapter = adapter.currentChapter ?: return null
        val pages = currChapter.pages ?: return null
        if (pages.isEmpty()) return null

        val recyclerHeight = recycler.height
        if (recyclerHeight <= 0) return null

        val firstPagePosition = adapter.items.indexOf(pages.first())
        val lastPagePosition = adapter.items.indexOf(pages.last())
        if (firstPagePosition == -1 || lastPagePosition == -1) return null

        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) {
            return null
        }

        // Viewport sits entirely above the chapter (peeking back at prev's teaser pages).
        if (lastVisible < firstPagePosition) return 0
        // Viewport sits entirely below the chapter (peeking ahead at next's teaser pages).
        if (firstVisible > lastPagePosition) return 100

        val (resolvedHeights, _) = resolveChapterPageHeights(pages) ?: return null
        val chapterHeight = resolvedHeights.sum()
        val maxScrollable = max(chapterHeight - recyclerHeight, 0)
        if (maxScrollable <= 0) {
            // The whole chapter fits on screen; preserve existing progress so we don't
            // auto-mark a short chapter as fully read just by opening it.
            return currChapter.chapter.last_page_read.coerceIn(0, 100)
        }

        val anchorIndex = pages.indexOfFirst { p ->
            adapter.items.indexOf(p) in firstVisible..lastVisible
        }
        if (anchorIndex < 0) return null
        val anchorPos = adapter.items.indexOf(pages[anchorIndex])
        val anchorView = layoutManager.findViewByPosition(anchorPos) ?: return null

        val anchorOffsetInChapter = (0 until anchorIndex).sumOf { resolvedHeights[it] }
        val viewportTopInChapter = anchorOffsetInChapter - anchorView.top
        val offset = viewportTopInChapter.coerceIn(0, maxScrollable)
        return ((offset.toFloat() / maxScrollable) * 100f).roundToInt().coerceIn(0, 100)
    }

    private fun visibleCurrentChapterPage(pages: List<ReaderPage>): ReaderPage? {
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) {
            return null
        }
        return pages.firstOrNull { p ->
            adapter.items.indexOf(p) in firstVisible..lastVisible
        }
    }

    /**
     * Returns per-page heights for [pages]. Pages whose view hasn't been measured yet are filled
     * in with the average of measured pages (floored at the recycler height) so that callers can
     * compute progress / scroll targets before the entire chapter has been laid out. Returns null
     * only when no page has been measured at all.
     *
     * Second component of the result is true iff every page in [pages] had a real measurement
     * (no estimation was needed).
     */
    private fun resolveChapterPageHeights(pages: List<ReaderPage>): Pair<List<Int>, Boolean>? {
        val raw = pages.map { page ->
            measuredPageHeights[page]?.takeIf { it > 0 }
                ?: adapter.items.indexOf(page).takeIf { it != -1 }
                    ?.let { layoutManager.findViewByPosition(it)?.height }
                    ?.takeIf { it > 0 }
                ?: 0
        }
        val measured = raw.filter { it > 0 }
        if (measured.isEmpty()) return null

        val recyclerHeight = recycler.height.coerceAtLeast(1)
        val estimate = measured.average().roundToInt().coerceAtLeast(recyclerHeight)
        val resolved = raw.map { if (it > 0) it else estimate }
        return resolved to (measured.size == pages.size)
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
        val measuredCount = pages.count { (measuredPageHeights[it] ?: 0) > 0 }

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
        if (recyclerHeight <= 0) return false

        val (resolvedHeights, fullyMeasured) = resolveChapterPageHeights(pages) ?: return false
        if (!allowEstimatedHeights && !fullyMeasured) return false

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

// Pinch-to-zoom font-size bounds. Keep in sync with the font-size spinner array values.
private const val MIN_FONT_SIZE = 10
private const val MAX_FONT_SIZE = 32
