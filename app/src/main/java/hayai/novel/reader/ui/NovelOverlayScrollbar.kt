package hayai.novel.reader.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * View-based scrollbar overlay used by the novel reader. Draws a single rounded thumb
 * on either the left or right edge of its bounds, with the track sized to either the full
 * visible height or a vertically-centered half of it. Owners drive its state via
 * [setProgress] and [setVisibleFraction]; the view fades itself out after a short idle
 * period and fades back in on the next update.
 *
 * Why not the WebView's native scrollbar? WebView doesn't expose APIs for position
 * (left/right) or track size (half/full), and its scrollbar reliably gets cleared by
 * page reloads / theme changes. Drawing the bar ourselves means the user-facing
 * preferences actually apply.
 */
class NovelOverlayScrollbar @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null,
) : View(context, attrs) {

    enum class Side { LEFT, RIGHT }
    enum class Track { FULL, HALF }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.TRANSPARENT
        style = Paint.Style.FILL
    }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x66888888
        style = Paint.Style.FILL
    }

    private var progress: Float = 0f      // 0..1 — scroll position
    private var visibleFraction: Float = 1f  // 0..1 — viewport / content ratio (thumb size)
    private var side: Side = Side.RIGHT
    private var track: Track = Track.FULL

    private var fadeAlpha: Int = 0
    private var fadeAnimator: ValueAnimator? = null
    private var idleHideTrigger = Runnable { fadeOut() }

    private val barWidthPx: Float
    private val barInsetPx: Float
    private val minThumbHeightPx: Float

    init {
        val dm = context.resources.displayMetrics
        barWidthPx = 4f * dm.density
        barInsetPx = 4f * dm.density
        minThumbHeightPx = 28f * dm.density
        // Fully transparent until the first scroll update.
        thumbPaint.alpha = 0
    }

    fun configure(side: Side, track: Track, thumbColor: Int? = null) {
        this.side = side
        this.track = track
        if (thumbColor != null) thumbPaint.color = thumbColor
        invalidate()
    }

    /** Scroll position 0..1; visible only when the content is actually scrollable. */
    fun setProgress(progress: Float) {
        this.progress = progress.coerceIn(0f, 1f)
        showAndScheduleHide()
        invalidate()
    }

    /**
     * Briefly fade the bar in so the user sees a config change applied (side, size,
     * visibility toggle) even when they're not currently scrolling. Without this the
     * bar stays at alpha=0 until the next real scroll event, making the prefs feel
     * broken: toggle "show vertical scrollbar" → no visible change → "doesn't work".
     *
     * Unlike [setProgress] this does NOT respect the visibleFraction guard: if the
     * user explicitly toggled a scrollbar pref we owe them visual feedback even when
     * the current chapter happens to fit on one screen. We force visibleFraction to
     * a thumb-sized value just for the pulse so the user sees a real thumb, not a
     * full-track filled bar; the next real scroll call from
     * [setVisibleFraction]/[setProgress] will overwrite it with accurate metrics.
     */
    fun pulse() {
        if (visibleFraction >= 0.999f) {
            // Make the thumb a sensible quarter-track placeholder so the user can see
            // it rather than a full-track filled bar.
            visibleFraction = 0.25f
        }
        // Bypass showAndScheduleHide's own fraction guard by inlining the fade-in
        // here — the fraction was just re-set above so the guard would be redundant.
        fadeAnimator?.cancel()
        if (fadeAlpha < 200) {
            fadeAnimator = ValueAnimator.ofInt(fadeAlpha, 200).apply {
                duration = 120
                addUpdateListener {
                    fadeAlpha = it.animatedValue as Int
                    thumbPaint.alpha = fadeAlpha
                    invalidate()
                }
                start()
            }
        } else {
            fadeAlpha = 200
            thumbPaint.alpha = fadeAlpha
        }
        removeCallbacks(idleHideTrigger)
        postDelayed(idleHideTrigger, IDLE_HIDE_MS)
        invalidate()
    }

    /**
     * Ratio of viewport height to total content height (0..1). At 1 the thumb fills the
     * track (no scrollable area) and the bar fades out; below 1 the thumb scales down.
     */
    fun setVisibleFraction(fraction: Float) {
        visibleFraction = fraction.coerceIn(0f, 1f)
        if (visibleFraction >= 0.999f) {
            // No scrollable content — keep hidden.
            fadeOut(immediate = true)
        }
        invalidate()
    }

    private fun showAndScheduleHide() {
        if (visibleFraction >= 0.999f) return
        fadeAnimator?.cancel()
        if (fadeAlpha < 200) {
            fadeAnimator = ValueAnimator.ofInt(fadeAlpha, 200).apply {
                duration = 120
                addUpdateListener {
                    fadeAlpha = it.animatedValue as Int
                    thumbPaint.alpha = fadeAlpha
                    invalidate()
                }
                start()
            }
        } else {
            fadeAlpha = 200
            thumbPaint.alpha = fadeAlpha
        }
        removeCallbacks(idleHideTrigger)
        postDelayed(idleHideTrigger, IDLE_HIDE_MS)
    }

    private fun fadeOut(immediate: Boolean = false) {
        removeCallbacks(idleHideTrigger)
        fadeAnimator?.cancel()
        if (immediate || fadeAlpha == 0) {
            fadeAlpha = 0
            thumbPaint.alpha = 0
            invalidate()
            return
        }
        fadeAnimator = ValueAnimator.ofInt(fadeAlpha, 0).apply {
            duration = 240
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                fadeAlpha = it.animatedValue as Int
                thumbPaint.alpha = fadeAlpha
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (fadeAlpha == 0) return
        if (width == 0 || height == 0) return

        // Track geometry derived from the configured size.
        val trackHeight = when (track) {
            Track.FULL -> height.toFloat() - barInsetPx * 2
            Track.HALF -> height / 2f
        }
        val trackTop = (height - trackHeight) / 2f
        val thumbHeight = (trackHeight * visibleFraction).coerceAtLeast(minThumbHeightPx)
        val thumbRange = (trackHeight - thumbHeight).coerceAtLeast(0f)
        val thumbTop = trackTop + thumbRange * progress

        val xLeft: Float
        val xRight: Float
        when (side) {
            Side.RIGHT -> {
                xRight = width - barInsetPx
                xLeft = xRight - barWidthPx
            }
            Side.LEFT -> {
                xLeft = barInsetPx
                xRight = xLeft + barWidthPx
            }
        }

        canvas.drawRoundRect(
            RectF(xLeft, thumbTop, xRight, thumbTop + thumbHeight),
            barWidthPx / 2f,
            barWidthPx / 2f,
            thumbPaint,
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        fadeAnimator?.cancel()
        removeCallbacks(idleHideTrigger)
    }

    private companion object {
        const val IDLE_HIDE_MS = 1500L
    }
}
