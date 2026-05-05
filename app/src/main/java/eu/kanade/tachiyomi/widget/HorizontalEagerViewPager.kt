package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ViewConfiguration
import androidx.viewpager.widget.ViewPager

/**
 * ViewPager that grabs horizontal motion at the standard touch slop instead of the larger paging
 * touch slop. By default, [ViewPager.mTouchSlop] is initialized from
 * `ViewConfiguration.scaledPagingTouchSlop` (~12dp), while a nested [androidx.recyclerview.widget.RecyclerView]
 * claims its own gesture at `scaledTouchSlop` (~8dp). The recycler always wins the first ~4dp of
 * motion, so a brief vertical wobble at the start of a swipe lets it grab the gesture and never
 * give it back — making horizontal swipes feel sluggish or sometimes unactivatable.
 *
 * Lowering the pager's slop to match the recycler's puts both at the same threshold; whichever
 * direction dominates wins immediately. This is the same effect Compose's `HorizontalPager`
 * (which uses Compose's unified gesture detection) achieves out of the box.
 */
class HorizontalEagerViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ViewPager(context, attrs) {

    init {
        try {
            val field = ViewPager::class.java.getDeclaredField("mTouchSlop")
            field.isAccessible = true
            field.setInt(this, ViewConfiguration.get(context).scaledTouchSlop)
        } catch (_: Throwable) {
            // Reflection blocked (e.g. future Android tightens AndroidX private-field access).
            // The pager still works — just at the default sluggish slop.
        }
    }
}
