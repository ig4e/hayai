package hayai.novel.tts.playback

import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.widget.TextView

/**
 * Applies and clears the active-word [BackgroundColorSpan] on the chapter's TextViews as TTS
 * playback progresses. The dispatcher doesn't own scheduling — the playback controller computes
 * the right wall-clock time for each [hayai.novel.tts.engine.WordTiming] and calls [highlight]
 * at that moment.
 *
 * The [findSegment] resolver returns the [TextView] currently rendering the requested block
 * range and the offset of [charInBlock] within that TextView's text — handling multi-TextView
 * blocks like list items where the highlight needs to land on the right item, not just the
 * block's first item. Returns null when the block isn't currently bound (recycled / scrolled
 * off-screen); [highlight] no-ops gracefully.
 */
class HighlightDispatcher(
    private val highlightColor: Int,
    private val findSegment: (blockIndex: Int, charInBlock: Int) -> TextSegment?,
) {

    private var activeView: TextView? = null
    private var activeSpannable: SpannableString? = null
    private var activeSpan: BackgroundColorSpan? = null

    /**
     * Highlights the half-open `[start, end)` char range inside the given block. `start` and
     * `end` are character offsets within the block's plain-text content (which may span multiple
     * TextViews for [hayai.novel.reader.NovelBlock.ListItems]).
     */
    fun highlight(blockIndex: Int, start: Int, end: Int) {
        val seg = findSegment(blockIndex, start) ?: run {
            clear()
            return
        }
        val view = seg.textView
        val text = view.text ?: return
        val len = text.length

        val localStart = seg.charInTextView.coerceIn(0, len)
        // If end falls past this TextView, just highlight to the end of this view rather than
        // straddling into the next one. Multi-TextView highlights aren't supported in v1.
        val intendedLength = (end - start).coerceAtLeast(0)
        val localEnd = (localStart + intendedLength).coerceIn(localStart, len)
        if (localStart == localEnd) return

        val spannable = if (view === activeView && activeSpannable != null) {
            activeSpannable!!
        } else {
            val s = SpannableString(text)
            view.setText(s, TextView.BufferType.SPANNABLE)
            s
        }
        activeSpan?.let { spannable.removeSpan(it) }
        val span = BackgroundColorSpan(highlightColor)
        spannable.setSpan(span, localStart, localEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        activeView = view
        activeSpannable = spannable
        activeSpan = span
    }

    fun clear() {
        val span = activeSpan
        val spannable = activeSpannable
        if (span != null && spannable != null) {
            spannable.removeSpan(span)
        }
        activeView = null
        activeSpannable = null
        activeSpan = null
    }

    /**
     * Mapping from a block + char offset to the actual TextView that's currently rendering that
     * portion of the block. [charInTextView] is the position within the TextView's text that
     * corresponds to the requested block-relative offset (i.e. already translated into the
     * TextView's own coordinate space).
     */
    data class TextSegment(
        val textView: TextView,
        val charInTextView: Int,
    )
}
