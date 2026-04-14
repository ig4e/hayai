package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.util.AttributeSet
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.AbstractComposeView
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.util.isLocal
import eu.kanade.tachiyomi.util.system.ThemeUtil
import yokai.presentation.reader.ChapterTransition
import yokai.presentation.theme.YokaiTheme

class ReaderTransitionView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    AbstractComposeView(context, attrs) {

    private var data: Data? by mutableStateOf(null)

    init {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    fun bind(
        theme: Int,
        transition: ChapterTransition,
        downloadManager: DownloadManager,
        manga: Manga?,
        contentColor: Int? = null,
        containerColor: Int? = null,
    ) {
        data = if (manga != null) {
            Data(
                theme = theme,
                manga = manga,
                transition = transition,
                contentColor = contentColor,
                containerColor = containerColor,
                currChapterDownloaded = transition.from.pageLoader?.isLocal == true,
                goingToChapterDownloaded = manga.isLocal() ||
                    transition.to?.chapter?.let { goingToChapter ->
                        downloadManager.isChapterDownloaded(
                            chapter = goingToChapter,
                            manga = manga,
                            skipCache = true,
                        )
                    } ?: false,
            )
        } else {
            null
        }
    }

    @Composable
    override fun Content() {
        data?.let {
            YokaiTheme {
                CompositionLocalProvider (
                    LocalTextStyle provides MaterialTheme.typography.bodySmall,
                    LocalContentColor provides (
                        it.contentColor?.let(::ComposeColor)
                            ?: ThemeUtil.readerContentColor(it.theme, MaterialTheme.colorScheme.onBackground)
                        ),
                ) {
                    Surface(color = it.containerColor?.let(::ComposeColor) ?: ComposeColor.Transparent) {
                        ChapterTransition(
                            manga = it.manga,
                            transition = it.transition,
                            currChapterDownloaded = it.currChapterDownloaded,
                            goingToChapterDownloaded = it.goingToChapterDownloaded,
                        )
                    }
                }
            }
        }
    }

    private data class Data(
        val theme: Int,
        val manga: Manga,
        val transition: ChapterTransition,
        val contentColor: Int?,
        val containerColor: Int?,
        val currChapterDownloaded: Boolean,
        val goingToChapterDownloaded: Boolean,
    )
}

fun missingChapterCount(transition: ChapterTransition): Int {
    if (transition.to == null) {
        return 0
    }

    val hasMissingChapters = when (transition) {
        is ChapterTransition.Prev -> hasMissingChapters(transition.from, transition.to)
        is ChapterTransition.Next -> hasMissingChapters(transition.to, transition.from)
    }

    if (!hasMissingChapters) {
        return 0
    }

    val chapterDifference = when (transition) {
        is ChapterTransition.Prev -> calculateChapterDifference(transition.from, transition.to)
        is ChapterTransition.Next -> calculateChapterDifference(transition.to, transition.from)
    }

    return chapterDifference.toInt()
}
