package hayai.novel.reader

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.crossfade
import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.setText
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import yokai.i18n.MR
import yokai.presentation.theme.YokaiTheme
import yokai.util.lang.getString

/**
 * Holder for native novel chapter content. Raw chapter HTML is parsed into
 * native text/image blocks so RecyclerView owns all scrolling and measurement.
 */
class NovelPageHolder(
    private val container: FrameLayout,
    viewer: NovelViewer,
) : NovelBaseHolder(container, viewer) {

    private val scope = MainScope()
    private var page: ReaderPage? = null
    private var loadJob: Job? = null

    private val contentLayout: LinearLayout
    private val progressView: ComposeView
    private val errorLayout: LinearLayout
    private val imageViews = mutableListOf<AppCompatImageView>()
    private val imageRequests = mutableListOf<Disposable>()

    /**
     * Per-block list of (TextView, charStartInBlock) pairs. Index = local block index inside
     * this page (matches the page's [hayai.novel.reader.NovelBlock] order). Used by the TTS
     * highlight pipeline to find the right TextView for a given (blockIndex, charInBlock).
     *
     * - Plain text blocks contribute one entry of (textView, 0).
     * - List item blocks contribute one entry per item with the running char offset (each item's
     *   plain text + 1 for the joining newline that the segmenter inserts).
     * - Image / divider blocks contribute an empty list (no TextView, not narratable).
     */
    private val blockTextSegments = mutableListOf<List<BlockTextSegment>>()

    /** Number of [hayai.novel.reader.NovelBlock]s currently rendered for this page. */
    val renderedBlockCount: Int get() = blockTextSegments.size

    fun textViewForBlockChar(localBlockIndex: Int, charInBlock: Int): hayai.novel.tts.playback.HighlightDispatcher.TextSegment? {
        val segments = blockTextSegments.getOrNull(localBlockIndex) ?: return null
        for (segment in segments) {
            if (charInBlock in segment.charStartInBlock until segment.charEndInBlock) {
                return hayai.novel.tts.playback.HighlightDispatcher.TextSegment(
                    textView = segment.textView,
                    charInTextView = charInBlock - segment.charStartInBlock,
                )
            }
        }
        // Fallback: highlight at the end of the last segment if the requested offset overshoots.
        val last = segments.lastOrNull() ?: return null
        return hayai.novel.tts.playback.HighlightDispatcher.TextSegment(
            textView = last.textView,
            charInTextView = (last.charEndInBlock - last.charStartInBlock).coerceAtLeast(0),
        )
    }

    private data class BlockTextSegment(
        val textView: android.widget.TextView,
        val charStartInBlock: Int,
        val charEndInBlock: Int,
    )

    init {
        container.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

        contentLayout = createContentLayout()
        progressView = createProgressView()
        errorLayout = createErrorLayout()

        container.addView(contentLayout)
        container.addView(progressView)
        container.addView(errorLayout)

        contentLayout.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            reportMeasuredHeight()
        }

        contentLayout.isVisible = false
        progressView.isVisible = false
        errorLayout.isVisible = false
    }

    private fun createContentLayout(): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            orientation = LinearLayout.VERTICAL
            clipToPadding = false
            setOnTouchListener { _, event: MotionEvent ->
                viewer.onContentTouch(event)
                false
            }
        }
    }

    private fun createProgressView(): ComposeView {
        return ComposeView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                MATCH_PARENT,
                context.resources.displayMetrics.heightPixels,
                Gravity.CENTER,
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                YokaiTheme {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    private fun createErrorLayout(): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.CENTER)
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val padding = 32.dpToPx
            setPadding(padding, 128.dpToPx, padding, padding)
        }
    }

    fun bind(page: ReaderPage) {
        this.page = page
        loadJob?.cancel()
        setLoading()
        loadJob = scope.launch { loadPageAndProcessStatus() }
    }

    private suspend fun loadPageAndProcessStatus() {
        val page = page ?: return
        val loader = page.chapter.pageLoader ?: return

        // Treat a stale Error state as a retry trigger rather than a value to render. Without
        // this, the StateFlow's initial emission would flash setError() before the relaunched
        // loadPage transitions through LoadPage -> Ready.
        if (page.status == Page.State.Error) {
            page.status = Page.State.Queue
        }

        supervisorScope {
            launch(kotlinx.coroutines.Dispatchers.IO) { loader.loadPage(page) }
            page.statusFlow.collectLatest { status ->
                when (status) {
                    Page.State.Queue -> setQueued()
                    Page.State.LoadPage -> setLoading()
                    Page.State.DownloadImage -> setLoading()
                    Page.State.Ready -> setContent()
                    Page.State.Error -> setError()
                }
            }
        }
    }

    private fun setQueued() {
        setLoading()
    }

    private fun setLoading() {
        applyReaderColors()
        clearContent()
        contentLayout.isVisible = false
        progressView.isVisible = true
        errorLayout.isVisible = false
    }

    private fun setContent() {
        val currentPage = page ?: return
        val stream = currentPage.stream
        if (stream == null) {
            currentPage.chapter.pageLoader?.retryPage(currentPage)
            setLoading()
            return
        }

        try {
            val html = stream().bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (page != currentPage) return

            val imageUrlResolver = currentPage.chapter.pageLoader as? NovelImageUrlResolver
            val blocks = NovelHtmlParser.parse(
                html = html,
                baseUrl = currentPage.url.takeIf { it.isNotBlank() },
                imageUrlResolver = imageUrlResolver?.let { resolver ->
                    { url -> resolver.resolveNovelImageUrl(url) }
                },
            )

            applyReaderColors()
            clearContent()

            if (blocks.isEmpty()) {
                addTextView(
                    html = context.getString(MR.strings.no_pages_found),
                    style = TextStyle.Paragraph,
                )
            } else {
                blocks.forEach { block -> renderBlock(block) }
            }

            contentLayout.isVisible = true
            progressView.isVisible = false
            errorLayout.isVisible = false
            contentLayout.requestLayout()
            contentLayout.post { reportMeasuredHeight() }
        } catch (e: Exception) {
            Logger.e(e) { "NovelPageHolder: Failed to render native content" }
            setError()
        }
    }

    private fun setError() {
        applyReaderColors()
        clearContent()
        contentLayout.isVisible = false
        progressView.isVisible = false
        errorLayout.isVisible = true

        errorLayout.removeAllViews()

        val colors = viewer.config.getColors()
        val textColor = Color.parseColor(colors.textColor)

        val textView = AppCompatTextView(context).apply {
            wrapContent()
            text = context.getString(MR.strings.failed_to_load_pages_, page?.status?.toString() ?: "")
            setTextColor(textColor)
        }

        val retryBtn = AppCompatButton(context).apply {
            wrapContent()
            setText(MR.strings.retry)
            setTextColor(textColor)
            backgroundTintList = ColorStateList.valueOf(adjustAlpha(textColor, 0.2f))
            setOnClickListener {
                page?.let { p ->
                    val loader = p.chapter.pageLoader
                    if (loader != null) {
                        loader.retryPage(p)
                    } else {
                        p.status = Page.State.Queue
                        bind(p)
                    }
                }
            }
        }

        errorLayout.addView(textView)
        errorLayout.addView(retryBtn)
    }

    override fun recycle() {
        loadJob?.cancel()
        clearContent()
        page = null
        contentLayout.isVisible = false
        progressView.isVisible = false
        errorLayout.isVisible = false
    }

    private fun renderBlock(block: NovelBlock) {
        val segments = when (block) {
            is NovelBlock.Text -> {
                val view = addTextView(block.html, block.style)
                val plainLen = view.text?.length ?: 0
                listOf(BlockTextSegment(view, 0, plainLen))
            }
            is NovelBlock.ListItems -> addListView(block)
            is NovelBlock.Image -> {
                addImageView(block)
                emptyList()
            }
            NovelBlock.Divider -> {
                addDivider()
                emptyList()
            }
        }
        blockTextSegments += segments
    }

    private fun addTextView(html: String, style: TextStyle): AppCompatTextView {
        val config = viewer.config
        val colors = config.getColors()
        val textColor = Color.parseColor(colors.textColor)

        val view = AppCompatTextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                // Headings get a slight extra gap to keep them visually separated from body text
                // even when paragraph spacing is dialed down.
                val baseSpacing = config.paragraphSpacing
                val bottom = when (style) {
                    TextStyle.Heading1,
                    TextStyle.Heading2,
                    TextStyle.Heading3,
                    TextStyle.Heading4,
                    TextStyle.Heading5,
                    TextStyle.Heading6,
                    -> (baseSpacing + 2).dpToPx
                    else -> baseSpacing.dpToPx
                }
                setMargins(0, 0, 0, bottom)
            }
            includeFontPadding = true
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeForStyle(style, config.fontSize))
            typeface = typefaceForStyle(style, config.fontFamily, config.fontWeight)
            setLineSpacing(0f, config.lineHeight)
            textAlignment = textAlignmentForConfig(config.textAlign)
            gravity = gravityForConfig(config.textAlign)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && config.textAlign == "justify") {
                justificationMode = Layout.JUSTIFICATION_MODE_INTER_WORD
            }
            text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
        }

        when (style) {
            TextStyle.Quote -> {
                view.setPadding(16.dpToPx, 8.dpToPx, 0, 8.dpToPx)
                view.setTextColor(adjustAlpha(textColor, 0.88f))
                view.setBackgroundColor(adjustAlpha(textColor, 0.08f))
            }
            TextStyle.Code -> {
                view.setPadding(10.dpToPx, 8.dpToPx, 10.dpToPx, 8.dpToPx)
                view.setBackgroundColor(adjustAlpha(textColor, 0.10f))
            }
            else -> Unit
        }

        contentLayout.addView(view)
        return view
    }

    private fun addListView(block: NovelBlock.ListItems): List<BlockTextSegment> {
        // Mirror the segmenter's flat-text layout: each item joined by a single newline. The
        // returned segments map block-relative char offsets back to the right per-item TextView
        // for highlighting.
        val segments = mutableListOf<BlockTextSegment>()
        var charOffset = 0
        block.items.forEachIndexed { index, item ->
            val prefix = if (block.ordered) "${index + 1}. " else "- "
            val view = addTextView(prefix + item, TextStyle.Paragraph)
            val plainLen = view.text?.length ?: 0
            segments += BlockTextSegment(view, charOffset, charOffset + plainLen)
            charOffset += plainLen + 1 // +1 for the joining newline the segmenter inserts
        }
        return segments
    }

    private fun addImageView(block: NovelBlock.Image) {
        val boundPage = page ?: return
        val imageView = AppCompatImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setMargins(0, 8.dpToPx, 0, 16.dpToPx)
            }
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = block.alt
            setBackgroundColor(Color.TRANSPARENT)
            isVisible = false
        }
        imageViews.add(imageView)
        contentLayout.addView(imageView)

        val request = ImageRequest.Builder(context)
            .data(block.url)
            .target(
                onSuccess = { result ->
                    if (page == boundPage) {
                        imageView.setImageDrawable(result.asDrawable(context.resources))
                        imageView.isVisible = true
                        imageView.post { reportMeasuredHeight() }
                    }
                },
                onError = {
                    if (page == boundPage) {
                        imageView.isVisible = false
                        imageView.post { reportMeasuredHeight() }
                    }
                },
            )
            .crossfade(false)
            .build()
        imageRequests.add(context.imageLoader.enqueue(request))
    }

    private fun addDivider() {
        val colors = viewer.config.getColors()
        val textColor = Color.parseColor(colors.textColor)
        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 1.dpToPx).apply {
                setMargins(0, 18.dpToPx, 0, 18.dpToPx)
            }
            setBackgroundColor(adjustAlpha(textColor, 0.25f))
        }
        contentLayout.addView(divider)
    }

    private fun clearContent() {
        imageRequests.forEach { it.dispose() }
        imageRequests.clear()
        imageViews.forEach { it.setImageDrawable(null) }
        imageViews.clear()
        blockTextSegments.clear()
        contentLayout.removeAllViews()
    }

    private fun applyReaderColors() {
        val config = viewer.config
        val colors = config.getColors()
        val backgroundColor = Color.parseColor(colors.backgroundColor)
        container.setBackgroundColor(backgroundColor)
        contentLayout.setBackgroundColor(backgroundColor)
        contentLayout.setPadding(
            config.paddingHorizontal.dpToPx,
            config.paddingVertical.dpToPx,
            config.paddingHorizontal.dpToPx,
            config.paddingVertical.dpToPx,
        )
        progressView.setBackgroundColor(backgroundColor)
        errorLayout.setBackgroundColor(backgroundColor)
    }

    private fun reportMeasuredHeight() {
        val currentPage = page ?: return
        if (!contentLayout.isVisible) return
        val height = contentLayout.measuredHeight.takeIf { it > 0 } ?: contentLayout.height
        if (height > 0) {
            viewer.onPageContentMeasured(currentPage, height)
        }
    }

    private fun textSizeForStyle(style: TextStyle, baseSize: Int): Float {
        val multiplier = when (style) {
            TextStyle.Heading1 -> 1.45f
            TextStyle.Heading2 -> 1.32f
            TextStyle.Heading3 -> 1.22f
            TextStyle.Heading4 -> 1.12f
            TextStyle.Heading5 -> 1.05f
            TextStyle.Heading6 -> 1.0f
            TextStyle.Code -> 0.9f
            TextStyle.Paragraph,
            TextStyle.Quote,
            -> 1.0f
        }
        return (baseSize * multiplier).coerceAtLeast(10f)
    }

    private fun typefaceForStyle(style: TextStyle, configuredFamily: String, weight: Int): Typeface {
        val base = when {
            style == TextStyle.Code -> Typeface.MONOSPACE
            configuredFamily == "sans-serif" -> Typeface.SANS_SERIF
            configuredFamily == "monospace" -> Typeface.MONOSPACE
            configuredFamily == "system-ui" -> Typeface.DEFAULT
            else -> Typeface.SERIF
        }
        val italic = style == TextStyle.Quote
        // Headings always render bold regardless of the user's body weight; body text uses the
        // configured weight verbatim.
        val isHeading = when (style) {
            TextStyle.Heading1, TextStyle.Heading2, TextStyle.Heading3,
            TextStyle.Heading4, TextStyle.Heading5, TextStyle.Heading6,
            -> true
            else -> false
        }
        val effectiveWeight = if (isHeading) weight.coerceAtLeast(700) else weight

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Typeface.create(base, effectiveWeight, italic)
        } else {
            // Pre-API 28: collapse to NORMAL/BOLD/ITALIC/BOLD_ITALIC.
            val typefaceStyle = when {
                effectiveWeight >= 600 && italic -> Typeface.BOLD_ITALIC
                effectiveWeight >= 600 -> Typeface.BOLD
                italic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            Typeface.create(base, typefaceStyle)
        }
    }

    private fun textAlignmentForConfig(textAlign: String): Int {
        return when (textAlign) {
            "center" -> View.TEXT_ALIGNMENT_CENTER
            "justify" -> View.TEXT_ALIGNMENT_TEXT_START
            else -> View.TEXT_ALIGNMENT_TEXT_START
        }
    }

    private fun gravityForConfig(textAlign: String): Int {
        return when (textAlign) {
            "center" -> Gravity.CENTER_HORIZONTAL
            else -> Gravity.START
        }
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        return Color.argb(
            (Color.alpha(color) * factor).toInt().coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )
    }
}
