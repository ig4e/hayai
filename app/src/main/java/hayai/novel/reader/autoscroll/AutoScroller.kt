package hayai.novel.reader.autoscroll

import android.view.Choreographer
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Choreographer-driven smooth auto-scroll for [RecyclerView]. Computes pixel deltas as
 * `pxPerSec * frameDeltaSeconds`, accumulates fractional pixels so slow speeds (10–20 px/sec)
 * still progress, and dispatches the rounded integer through `recycler.scrollBy(0, dy)` once a
 * whole pixel has accumulated.
 *
 * The scroller stops on its own when the recycler can no longer scroll forward — typically when
 * the user has reached the last page of the last loaded chapter. Owners can also call [stop]
 * (e.g. when the user taps to pause) or update the speed at runtime via [setSpeed]. State is
 * exposed via [isRunning] so reader UI can reflect it without owning the source-of-truth.
 *
 * Not thread-safe; call all methods from the main thread.
 */
class AutoScroller(
    private val recycler: RecyclerView,
    initialSpeedPxPerSec: Int,
) {

    private val choreographer: Choreographer = Choreographer.getInstance()
    private val frameCallback = Choreographer.FrameCallback(::onFrame)

    private var pxPerSec: Float = initialSpeedPxPerSec.toFloat()
    private var lastFrameNanos: Long = -1L
    private var pixelAccumulator: Float = 0f

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun start() {
        if (_isRunning.value) return
        _isRunning.value = true
        lastFrameNanos = -1L
        pixelAccumulator = 0f
        choreographer.postFrameCallback(frameCallback)
    }

    fun stop() {
        if (!_isRunning.value) return
        _isRunning.value = false
        choreographer.removeFrameCallback(frameCallback)
        lastFrameNanos = -1L
        pixelAccumulator = 0f
    }

    fun toggle() {
        if (_isRunning.value) stop() else start()
    }

    fun setSpeed(pxPerSec: Int) {
        this.pxPerSec = pxPerSec.toFloat().coerceAtLeast(0f)
    }

    private fun onFrame(frameTimeNanos: Long) {
        if (!_isRunning.value) return

        // First frame after start: skip the delta calculation so we don't scroll based on a stale
        // baseline (which would jump as far as the elapsed wall time since the previous run).
        if (lastFrameNanos < 0L) {
            lastFrameNanos = frameTimeNanos
            choreographer.postFrameCallback(frameCallback)
            return
        }

        val deltaSeconds = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
        lastFrameNanos = frameTimeNanos
        pixelAccumulator += pxPerSec * deltaSeconds
        val whole = pixelAccumulator.toInt()
        if (whole != 0) {
            pixelAccumulator -= whole.toFloat()
            recycler.scrollBy(0, whole)
        }

        // Stop when the recycler has nothing left below — usually the very end of the last
        // loaded chapter; the reader will preload the next chapter via the existing flow and
        // scrolling will resume on the next call to [start] if the user wants more.
        if (!recycler.canScrollVertically(1)) {
            stop()
            return
        }

        choreographer.postFrameCallback(frameCallback)
    }
}
