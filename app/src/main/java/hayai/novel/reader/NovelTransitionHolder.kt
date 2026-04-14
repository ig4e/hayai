package hayai.novel.reader

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderTransitionView
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.setText
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import yokai.i18n.MR
import yokai.presentation.theme.YokaiTheme
import yokai.util.lang.getString

/**
 * Holder for chapter transitions in the novel reader.
 * Follows the exact same pattern as WebtoonTransitionHolder.
 */
class NovelTransitionHolder(
    val layout: LinearLayout,
    viewer: NovelViewer,
) : NovelBaseHolder(layout, viewer) {

    private val scope = MainScope()
    private var stateJob: Job? = null

    private val transitionView = ReaderTransitionView(context)

    private var pagesContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
    }

    init {
        layout.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER

        val paddingVertical = 128.dpToPx
        val paddingHorizontal = 32.dpToPx
        layout.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)

        val childMargins = 16.dpToPx
        val childParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            setMargins(0, childMargins, 0, childMargins)
        }

        layout.addView(transitionView)
        layout.addView(pagesContainer, childParams)
    }

    fun bind(transition: ChapterTransition) {
        val colors = viewer.config.getColors()
        val backgroundColor = Color.parseColor(colors.backgroundColor)
        val textColor = Color.parseColor(colors.textColor)

        layout.setBackgroundColor(backgroundColor)
        pagesContainer.setBackgroundColor(backgroundColor)
        transitionView.setBackgroundColor(backgroundColor)
        transitionView.bind(
            viewer.config.readerTheme,
            transition,
            viewer.downloadManager,
            viewer.activity.viewModel.manga,
            contentColor = textColor,
            containerColor = backgroundColor,
        )
        transition.to?.let { observeStatus(it, transition) }
    }

    override fun recycle() {
        stateJob?.cancel()
    }

    private fun observeStatus(chapter: ReaderChapter, transition: ChapterTransition) {
        stateJob?.cancel()
        stateJob = scope.launch {
            chapter.stateFlow
                .collectLatest { state ->
                    pagesContainer.removeAllViews()
                    when (state) {
                        is ReaderChapter.State.Loading -> setLoading()
                        is ReaderChapter.State.Error -> setError(state.error, transition)
                        is ReaderChapter.State.Wait, is ReaderChapter.State.Loaded -> {}
                    }
                    pagesContainer.isVisible = pagesContainer.isNotEmpty()
                }
        }
    }

    private fun setLoading() {
        val textColor = Color.parseColor(viewer.config.getColors().textColor)
        val progress = ComposeView(context).apply {
            layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                YokaiTheme { CircularProgressIndicator() }
            }
        }

        val textView = AppCompatTextView(context).apply {
            wrapContent()
            setText(MR.strings.loading_pages)
            setTextColor(textColor)
        }

        pagesContainer.addView(progress)
        pagesContainer.addView(textView)
    }

    private fun setError(error: Throwable, transition: ChapterTransition) {
        val textColor = Color.parseColor(viewer.config.getColors().textColor)
        val textView = AppCompatTextView(context).apply {
            wrapContent()
            text = context.getString(MR.strings.failed_to_load_pages_, error.message ?: "")
            setTextColor(textColor)
        }

        val retryBtn = AppCompatButton(context).apply {
            wrapContent()
            setText(MR.strings.retry)
            setTextColor(textColor)
            backgroundTintList = ColorStateList.valueOf(adjustAlpha(textColor, 0.2f))
            setOnClickListener {
                val toChapter = transition.to
                if (toChapter != null) {
                    viewer.activity.requestPreloadChapter(toChapter)
                }
            }
        }

        pagesContainer.addView(textView)
        pagesContainer.addView(retryBtn)
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
