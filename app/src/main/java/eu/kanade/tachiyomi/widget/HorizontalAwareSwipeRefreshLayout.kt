package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlin.math.abs

/**
 * SwipeRefreshLayout that ignores horizontal drags so a child ViewPager (or any horizontally
 * scrolling child) gets the gesture cleanly. The default implementation latches onto the touch
 * stream as soon as it sees any vertical component, which steals horizontal swipes from the
 * ViewPager and snaps the user back to the originating tab.
 */
class HorizontalAwareSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SwipeRefreshLayout(context, attrs) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var startX = 0f
    private var startY = 0f
    private var horizontalDrag = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
                horizontalDrag = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!horizontalDrag) {
                    val dx = abs(ev.x - startX)
                    val dy = abs(ev.y - startY)
                    if (dx > touchSlop && dx > dy) {
                        horizontalDrag = true
                    }
                }
                if (horizontalDrag) return false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> horizontalDrag = false
        }
        return super.onInterceptTouchEvent(ev)
    }
}
