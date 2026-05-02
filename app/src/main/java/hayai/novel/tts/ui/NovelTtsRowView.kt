package hayai.novel.tts.ui

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
 * Tappable "Text-to-speech" row in the novel reader settings sheet — opens [TtsLaunchSheet] on
 * tap so the voice picker, speed/pitch/volume controls, and toggles are reachable without the
 * user discovering the chrome long-press.
 *
 * Visually mirrors [hayai.novel.theme.ui.NovelThemeRowView] so the two settings rows feel
 * consistent.
 */
class NovelTtsRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ConstraintLayout(context, attrs) {

    private val titleView: MaterialTextView
    private val valueView: MaterialTextView

    init {
        val titleId = View.generateViewId()
        val valueId = View.generateViewId()
        val caretId = View.generateViewId()

        background = ContextCompat.getDrawable(context, R.drawable.square_ripple)
        isClickable = true
        isFocusable = true

        titleView = MaterialTextView(context).apply {
            id = titleId
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            text = context.getString(R.string.novel_tts)
        }
        valueView = MaterialTextView(context).apply {
            id = valueId
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            alpha = 0.7f
            gravity = Gravity.END
            maxLines = 1
            text = context.getString(R.string.novel_tts_settings)
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

        setOnClickListener { TtsLaunchSheet.show(context) }
    }
}
