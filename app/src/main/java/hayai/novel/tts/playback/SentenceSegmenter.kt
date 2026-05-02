package hayai.novel.tts.playback

import androidx.core.text.HtmlCompat
import hayai.novel.reader.NovelBlock
import java.text.BreakIterator
import java.util.Locale

/**
 * Builds a flat plain-text view of a chapter and segments it into sentences for TTS.
 *
 * The chapter's [NovelBlock] list is flattened into one long string; each `Text` block's HTML is
 * stripped via [HtmlCompat.fromHtml] so the engine reads the visible text only. Block boundaries
 * are tracked in [BlockOffsets] so the highlight dispatcher can map a sentence's char range back
 * to (blockIndex, charOffsetWithinBlock) and find the right TextView in the recycler.
 *
 * Sentence segmentation uses the JDK [BreakIterator] (US English locale by default), which
 * handles common edge cases — abbreviations like "Mr.", quoted dialogue, ellipses — without
 * needing ICU.
 */
class SentenceSegmenter(private val locale: Locale = Locale.US) {

    /**
     * Returns the flat chapter text plus per-block offset map; segment via [segment].
     */
    fun flatten(blocks: List<NovelBlock>): FlatChapter {
        val builder = StringBuilder()
        val offsets = mutableListOf<BlockOffsets>()
        for ((index, block) in blocks.withIndex()) {
            val start = builder.length
            when (block) {
                is NovelBlock.Text -> {
                    val plain = HtmlCompat.fromHtml(block.html, HtmlCompat.FROM_HTML_MODE_COMPACT)
                        .toString()
                        .trim()
                    if (plain.isNotEmpty()) {
                        builder.append(plain)
                        builder.append("\n\n")
                    }
                }
                is NovelBlock.ListItems -> {
                    for (item in block.items) {
                        val plain = HtmlCompat.fromHtml(item, HtmlCompat.FROM_HTML_MODE_COMPACT)
                            .toString()
                            .trim()
                        if (plain.isNotEmpty()) {
                            builder.append(plain)
                            builder.append("\n")
                        }
                    }
                    builder.append("\n")
                }
                // Images and dividers are skipped by the segmenter — they have no spoken content.
                is NovelBlock.Image, NovelBlock.Divider -> Unit
            }
            offsets += BlockOffsets(blockIndex = index, flatStart = start, flatEnd = builder.length)
        }
        return FlatChapter(text = builder.toString(), blockOffsets = offsets)
    }

    fun segment(flat: FlatChapter): List<SentenceSpan> {
        val text = flat.text
        if (text.isEmpty()) return emptyList()
        val it = BreakIterator.getSentenceInstance(locale).apply { setText(text) }
        val spans = mutableListOf<SentenceSpan>()
        var start = it.first()
        var end = it.next()
        while (end != BreakIterator.DONE) {
            val sliceStart = start
            val sliceEnd = end
            val piece = text.substring(sliceStart, sliceEnd).trim()
            if (piece.isNotEmpty()) {
                spans += SentenceSpan(charStart = sliceStart, charEnd = sliceEnd, text = piece)
            }
            start = end
            end = it.next()
        }
        return spans
    }

    /**
     * Translates a flat-text char offset to (blockIndex, offsetWithinBlock). Used by tap-to-start
     * to find the right sentence from the [TextView] tap coordinates.
     */
    fun resolveCharOffset(flat: FlatChapter, flatOffset: Int): BlockChar? {
        val owner = flat.blockOffsets.firstOrNull { flatOffset in it.flatStart until it.flatEnd } ?: return null
        return BlockChar(blockIndex = owner.blockIndex, offsetInBlock = flatOffset - owner.flatStart)
    }
}

data class FlatChapter(val text: String, val blockOffsets: List<BlockOffsets>)
data class BlockOffsets(val blockIndex: Int, val flatStart: Int, val flatEnd: Int)
data class SentenceSpan(val charStart: Int, val charEnd: Int, val text: String)
data class BlockChar(val blockIndex: Int, val offsetInBlock: Int)
