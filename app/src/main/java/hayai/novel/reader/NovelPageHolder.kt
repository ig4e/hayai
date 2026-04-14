package hayai.novel.reader

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
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
 * Holder for novel chapter content. Contains a WebView that renders HTML text.
 * The WebView expands to its full content height so the parent RecyclerView
 * handles all scrolling (not the WebView).
 *
 * Follows the same bind/load/status pattern as WebtoonPageHolder.
 */
class NovelPageHolder(
    private val container: FrameLayout,
    viewer: NovelViewer,
) : NovelBaseHolder(container, viewer) {

    private val scope = MainScope()
    private var page: ReaderPage? = null
    private var loadJob: Job? = null

    private val webView: WebView
    private val progressView: ComposeView
    private val errorLayout: LinearLayout

    init {
        container.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

        webView = createWebView()
        progressView = createProgressView()
        errorLayout = createErrorLayout()

        container.addView(webView)
        container.addView(progressView)
        container.addView(errorLayout)

        // Start with progress visible, content hidden
        webView.isVisible = false
        progressView.isVisible = false
        errorLayout.isVisible = false
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView {
        return WebView(context).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = false
                allowFileAccess = false
                setSupportZoom(false)
                loadWithOverviewMode = false
                useWideViewPort = false
                cacheMode = WebSettings.LOAD_NO_CACHE
            }

            // Disable WebView's own scrolling - parent RecyclerView handles it
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = WebView.OVER_SCROLL_NEVER

            setOnTouchListener { _, event ->
                viewer.onWebContentTouch(event)
                false
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    // Expand WebView to full content height
                    val boundPage = page ?: return
                    view.evaluateJavascript("document.body.scrollHeight") { heightStr ->
                        val contentHeight = heightStr
                            ?.replace("\"", "")
                            ?.toFloatOrNull()
                            ?.toInt()
                            ?: view.contentHeight
                        val density = view.resources.displayMetrics.density
                        val pixelHeight = (maxOf(contentHeight, 1) * density).toInt().coerceAtLeast(1)
                        view.layoutParams = view.layoutParams.apply {
                            height = pixelHeight
                        }
                        view.requestLayout()

                        if (page == boundPage) {
                            viewer.onPageContentMeasured(boundPage, pixelHeight)
                            webView.isVisible = true
                            progressView.isVisible = false
                            errorLayout.isVisible = false
                        }
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    return true // Don't navigate away
                }
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
        webView.isVisible = false
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
            val styledHtml = applyReaderStyles(html)

            webView.isVisible = false
            progressView.isVisible = true
            errorLayout.isVisible = false
            webView.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            Logger.e(e) { "NovelPageHolder: Failed to render content" }
            setError()
        }
    }

    private fun setError() {
        webView.isVisible = false
        progressView.isVisible = false
        errorLayout.isVisible = true

        errorLayout.removeAllViews()

        val textView = AppCompatTextView(context).apply {
            wrapContent()
            text = context.getString(MR.strings.failed_to_load_pages_, page?.status?.toString() ?: "")
        }

        val retryBtn = AppCompatButton(context).apply {
            wrapContent()
            setText(MR.strings.retry)
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
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.isVisible = false
        progressView.isVisible = false
        errorLayout.isVisible = false
    }

    private fun applyReaderStyles(html: String): String {
        val config = viewer.config
        val colors = config.getColors()

        return """<!DOCTYPE html>
<html><head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
* { box-sizing: border-box; margin: 0; padding: 0; }
body {
    font-family: '${config.fontFamily}', serif;
    font-size: ${config.fontSize}px;
    line-height: ${config.lineHeight};
    color: ${colors.textColor};
    background-color: ${colors.backgroundColor};
    padding: ${config.paddingVertical}px ${config.paddingHorizontal}px;
    text-align: ${config.textAlign};
    word-wrap: break-word;
    overflow-wrap: break-word;
    -webkit-user-select: text;
    -webkit-text-size-adjust: 100%;
}
p { margin-bottom: 1em; }
h1, h2, h3, h4, h5, h6 {
    margin: 1.2em 0 0.6em 0;
    line-height: 1.3;
}
a { color: #82B1FF; text-decoration: none; }
img { max-width: 100%; height: auto; display: block; margin: 1em auto; }
hr { border: none; border-top: 1px solid rgba(128,128,128,0.3); margin: 2em 0; }
blockquote {
    border-left: 3px solid rgba(128,128,128,0.4);
    padding-left: 1em;
    margin: 1em 0;
    opacity: 0.85;
    font-style: italic;
}
table { width: 100%; border-collapse: collapse; margin: 1em 0; }
td, th { border: 1px solid rgba(128,128,128,0.3); padding: 8px; text-align: left; }
pre, code {
    font-family: monospace;
    background: rgba(128,128,128,0.1);
    padding: 2px 6px;
    border-radius: 3px;
    font-size: 0.9em;
}
pre { padding: 12px; overflow-x: auto; margin: 1em 0; }
</style>
</head>
<body>
$html
</body></html>"""
    }
}
