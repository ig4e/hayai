package hayai.novel.theme.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.textview.MaterialTextView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.dpToPx

/**
 * Tappable row for the reader settings sheet that mirrors [eu.kanade.tachiyomi.widget.MaterialSpinnerView]'s
 * visual layout — title on the left, current value in the middle, caret icon on the right — but
 * routes its click to whatever the host wires up. Used by the novel reader sheet to replace the
 * legacy theme dropdown spinner with an entry point to the full theme manager.
 *
 * The host calls [setTitle] and [setValue] after binding; visual state is driven entirely from
 * outside, no preferences are touched here.
 */
class NovelThemeRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ConstraintLayout(context, attrs) {

    private val titleView: MaterialTextView
    private val valueView: MaterialTextView

    init {
        val titleId = View.generateViewId()
        val valueId = View.generateViewId()
        val caretId = View.generateViewId()

        // Match MaterialSpinnerView's tap surface so the row feels identical to its neighbours.
        background = ContextCompat.getDrawable(context, R.drawable.square_ripple)
        isClickable = true
        isFocusable = true

        titleView = MaterialTextView(context).apply {
            id = titleId
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
        }
        valueView = MaterialTextView(context).apply {
            id = valueId
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            alpha = 0.7f
            gravity = Gravity.END
            maxLines = 1
        }
        val caret = ImageView(context).apply {
            id = caretId
            setImageResource(R.drawable.ic_chevron_right_24dp)
            alpha = 0.6f
        }

        addView(
            titleView,
            LayoutParams(0, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 12.dpToPx
                bottomMargin = 12.dpToPx
                marginEnd = 8.dpToPx
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
                startToStart = LayoutParams.PARENT_ID
                endToStart = valueId
                horizontalChainStyle = LayoutParams.CHAIN_PACKED
                horizontalBias = 0f
            },
        )
        addView(
            valueView,
            LayoutParams(0, LayoutParams.WRAP_CONTENT).apply {
                topToTop = titleId
                bottomToBottom = titleId
                startToEnd = titleId
                endToStart = caretId
                marginEnd = 4.dpToPx
            },
        )
        addView(
            caret,
            LayoutParams(24.dpToPx, 24.dpToPx).apply {
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
            },
        )
    }

    fun setTitle(text: CharSequence) { titleView.text = text }

    fun setValue(text: CharSequence) { valueView.text = text }
}
