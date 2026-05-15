package yokai.util.widget

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

// Scales fling velocity proportional to item count so long chapter lists
// don't feel sluggish. Overrides fling() directly — using an OnFlingListener
// here would re-enter via RecyclerView.fling() -> listener -> fling() and
// blow the stack on the first fling.
class VelocityScaledRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RecyclerView(context, attrs, defStyleAttr) {

    override fun fling(velocityX: Int, velocityY: Int): Boolean {
        val count = adapter?.itemCount ?: 0
        val factor = 1f + (count / 500f).coerceIn(0f, 4f)
        return super.fling((velocityX * factor).toInt(), (velocityY * factor).toInt())
    }
}
