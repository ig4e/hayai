package yokai.util.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.max

// Velocity-aware fling scaling: slow flicks pass through unscaled (precision),
// fast flicks ramp up to a count-dependent ceiling (acceleration for long
// chapter lists). The ramp is anchored to the device's own max fling velocity
// so the feel is consistent across densities. Overrides fling() directly —
// using an OnFlingListener would re-enter via RecyclerView.fling() and blow
// the stack on the first fling.
class VelocityScaledRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RecyclerView(context, attrs, defStyleAttr) {

    private val maxFling = ViewConfiguration.get(context).scaledMaximumFlingVelocity.toFloat()
    private val rampStart = maxFling * 0.20f
    private val rampEnd = maxFling * 0.70f

    override fun fling(velocityX: Int, velocityY: Int): Boolean {
        val count = adapter?.itemCount ?: 0
        val ceiling = (count / 500f).coerceIn(0f, 4f)
        val mag = max(abs(velocityX), abs(velocityY)).toFloat()
        val weight = ((mag - rampStart) / (rampEnd - rampStart)).coerceIn(0f, 1f)
        val factor = 1f + ceiling * weight
        return super.fling(
            (velocityX * factor).toInt(),
            (velocityY * factor).toInt(),
        )
    }
}
