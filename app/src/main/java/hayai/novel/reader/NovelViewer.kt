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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import uy.kohesive.injekt.injectLazy

/**
 * Novel text viewer following the exact same architecture as WebtoonViewer.
 *
 * Uses RecyclerView with NovelAdapter for chapter content (WebView-based)
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

        if (recycler.isGone) {
            val pages = chapters.currChapter.pages ?: return
            moveToPage(pages[min(chapters.currChapter.requestedPage, pages.lastIndex)])
            recycler.isVisible = true

            // NOVEL --> Restore scroll position (percentage-based)
            val savedProgress = chapters.currChapter.chapter.last_page_read
            if (savedProgress > 0) {
                recycler.post {
                    val scrollRange = recycler.computeVerticalScrollRange()
                    recycler.scrollBy(0, (scrollRange * savedProgress / 100))
                }
            }
            // NOVEL <--
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
        if (item != null && currentPage != item) {
            currentPage = item
            when (item) {
                is ReaderPage -> onPageSelected(item, true)
                is ChapterTransition -> onTransitionSelected(item)
            }
        }

        // NOVEL --> Track scroll progress (percentage-based)
        if (item is ReaderPage) {
            val scrollRange = recycler.computeVerticalScrollRange()
            val scrollOffset = recycler.computeVerticalScrollOffset()
            if (scrollRange > 0) {
                val progress = ((scrollOffset.toFloat() / scrollRange) * 100).toInt().coerceIn(0, 100)
                item.chapter.chapter.last_page_read = progress
                item.chapter.chapter.pages_left = 100 - progress
                if (progress >= 95) item.chapter.chapter.read = true
            }
        }
        // NOVEL <--
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
        val position = layoutManager.findLastVisibleItemPosition()
        adapter.notifyItemRangeChanged(
            max(0, position - 1),
            min(position + 1, adapter.itemCount - 1),
        )
    }
}

private const val RECYCLER_VIEW_CACHE_SIZE = 4
