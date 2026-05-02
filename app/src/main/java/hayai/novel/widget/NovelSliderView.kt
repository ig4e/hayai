package hayai.novel.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import com.google.android.material.slider.Slider
import com.google.android.material.textview.MaterialTextView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.core.preference.Preference
import eu.kanade.tachiyomi.util.system.dpToPx

/**
 * Slider row for the novel reader settings sheet. Title on the left, slider in the middle,
 * current value on the right — all on a single 40dp-tall horizontal line so it occupies the
 * same vertical real estate as the spinner rows it sits beside.
 */
class NovelSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    private val titleView: MaterialTextView
    private val valueView: MaterialTextView
    private val slider: Slider

    private var valueSuffix: String = ""
    private var pref: Preference<Int>? = null
    private var onChangeListener: ((Int) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        titleView = MaterialTextView(context).apply {
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            maxLines = 1
        }
        slider = Slider(context).apply {
            trackHeight = 4.dpToPx
            // Slim thumb + halo: keeps the row at ~36dp tall while staying tappable.
            thumbRadius = 7.dpToPx
            haloRadius = 14.dpToPx
            isTickVisible = false
            // Hide focused-state halo and tick fills explicitly — they default to opaque tints
            // on Material 3 and cause subtle visual noise even when not focused.
            haloTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        }
        valueView = MaterialTextView(context).apply {
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            alpha = 0.7f
            gravity = Gravity.END
            maxLines = 1
            minWidth = 48.dpToPx
        }

        // Horizontal layout params — title hugs left at intrinsic width, slider stretches, value
        // anchors right at intrinsic width.
        addView(
            titleView,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = 16.dpToPx
                marginEnd = 8.dpToPx
            },
        )
        addView(
            slider,
            LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f),
        )
        addView(
            valueView,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = 8.dpToPx
                marginEnd = 16.dpToPx
            },
        )

        context.withStyledAttributes(attrs, R.styleable.NovelSliderView) {
            titleView.text = getString(R.styleable.NovelSliderView_novelSliderTitle).orEmpty()
            slider.valueFrom = getFloat(R.styleable.NovelSliderView_novelSliderValueFrom, 0f)
            slider.valueTo = getFloat(R.styleable.NovelSliderView_novelSliderValueTo, 100f)
            slider.stepSize = getFloat(R.styleable.NovelSliderView_novelSliderStepSize, 1f)
            valueSuffix = getString(R.styleable.NovelSliderView_novelSliderValueSuffix).orEmpty()
        }

        slider.addOnChangeListener { _, value, fromUser ->
            updateValueLabel(value.toInt())
            if (fromUser) onChangeListener?.invoke(value.toInt())
        }
        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) = Unit
            override fun onStopTrackingTouch(slider: Slider) {
                pref?.set(slider.value.toInt())
            }
        })
    }

    fun bindToIntPreference(
        pref: Preference<Int>,
        valueFrom: Int,
        valueTo: Int,
        stepSize: Int,
        onChange: ((Int) -> Unit)? = null,
    ) {
        this.pref = pref
        this.onChangeListener = onChange
        slider.valueFrom = valueFrom.toFloat()
        slider.valueTo = valueTo.toFloat()
        slider.stepSize = stepSize.toFloat()
        val current = pref.get().coerceIn(valueFrom, valueTo)
        slider.value = current.toFloat()
        updateValueLabel(current)
    }

    fun setValueSuffix(suffix: String) {
        valueSuffix = suffix
        updateValueLabel(slider.value.toInt())
    }

    private fun updateValueLabel(value: Int) {
        valueView.text = if (valueSuffix.isEmpty()) value.toString() else "$value$valueSuffix"
    }
}
