package hayai.novel.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.slider.Slider
import com.google.android.material.textview.MaterialTextView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.core.preference.Preference
import eu.kanade.tachiyomi.util.system.dpToPx
import hayai.novel.reader.autoscroll.AutoScroller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Compound row hosting auto-scroll's play/pause toggle on the left and a speed slider on the
 * right. Drives the [AutoScroller] passed in via [bind], reflects its [AutoScroller.isRunning]
 * state on the play/pause icon, and persists slider drags to the supplied speed preference.
 *
 * The widget is presentation-only — it doesn't own the scroller's lifetime, just toggles it.
 */
class NovelAutoScrollRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ConstraintLayout(context, attrs) {

    private val playPauseButton: ImageButton
    private val slider: Slider
    private val valueView: MaterialTextView

    private var speedPref: Preference<Int>? = null

    init {
        val playId = View.generateViewId()
        val sliderId = View.generateViewId()
        val valueId = View.generateViewId()

        playPauseButton = ImageButton(context).apply {
            id = playId
            background = ContextCompat.getDrawable(context, R.drawable.square_ripple)
            setImageResource(R.drawable.ic_play_arrow_24dp)
            contentDescription = context.getString(R.string.novel_auto_scroll)
        }
        slider = Slider(context).apply {
            id = sliderId
        }
        valueView = MaterialTextView(context).apply {
            id = valueId
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            alpha = 0.7f
            gravity = Gravity.END
        }

        addView(
            playPauseButton,
            LayoutParams(40.dpToPx, 40.dpToPx).apply {
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
                startToStart = LayoutParams.PARENT_ID
            },
        )
        addView(
            slider,
            LayoutParams(0, LayoutParams.WRAP_CONTENT).apply {
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
                startToEnd = playId
                endToStart = valueId
                marginStart = 8.dpToPx
            },
        )
        addView(
            valueView,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
                width = 64.dpToPx
                marginStart = 4.dpToPx
                marginEnd = 16.dpToPx
            },
        )
    }

    /**
     * Wires the row to a scroller and a speed preference. The lifecycle [scope] is used to track
     * the scroller's running state for the play/pause icon swap; cancel it (or pass a scope tied
     * to the host view's lifetime) to detach.
     */
    fun bind(
        scroller: AutoScroller,
        speedPref: Preference<Int>,
        valueFrom: Int,
        valueTo: Int,
        stepSize: Int,
        scope: CoroutineScope,
    ) {
        this.speedPref = speedPref
        slider.valueFrom = valueFrom.toFloat()
        slider.valueTo = valueTo.toFloat()
        slider.stepSize = stepSize.toFloat()
        val current = speedPref.get().coerceIn(valueFrom, valueTo)
        slider.value = current.toFloat()
        updateValueLabel(current)

        slider.addOnChangeListener { _, value, _ ->
            updateValueLabel(value.toInt())
        }
        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) = Unit
            override fun onStopTrackingTouch(slider: Slider) {
                speedPref.set(slider.value.toInt())
            }
        })

        playPauseButton.setOnClickListener { scroller.toggle() }
        scroller.isRunning
            .onEach { running ->
                playPauseButton.setImageResource(
                    if (running) R.drawable.ic_pause_24dp else R.drawable.ic_play_arrow_24dp,
                )
            }
            .launchIn(scope)
    }

    private fun updateValueLabel(value: Int) {
        valueView.text = context.getString(R.string.novel_auto_scroll_speed_format, value)
    }
}
