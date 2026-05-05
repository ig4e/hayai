package hayai.novel.reader.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import yokai.presentation.theme.YokaiTheme

/**
 * M3 Expressive shape-morphing loading indicator with an optional caption. Mounted as a
 * [ComposeView] over a viewer's content container while a chapter is being fetched.
 *
 * Rationale: replaces the previous "Loading..." `TextView` (in [NovelViewer]) and the
 * blank loading HTML (in [NovelWebViewViewer]) with the same Material 3 Expressive
 * `LoadingIndicator` so both renderers feel the same and pick up Material You theming.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NovelLoadingOverlay(
    backgroundColor: Color,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    caption: String? = null,
) {
    // Paint the entire overlay with the resolved theme background so the loader sits on
    // the correct surface from the very first frame — avoids a flash of "wrong" theme
    // before the chapter renders.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LoadingIndicator(color = indicatorColor)
            caption?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Builds a [ComposeView] that fills its parent with [NovelLoadingOverlay]. Caller is responsible
 * for adding/removing the view from its hierarchy.
 */
fun buildNovelLoadingComposeView(
    context: Context,
    backgroundArgb: Int,
    indicatorArgb: Int? = null,
    caption: String? = null,
): ComposeView {
    return ComposeView(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        setContent {
            YokaiTheme {
                NovelLoadingOverlay(
                    backgroundColor = Color(backgroundArgb),
                    indicatorColor = indicatorArgb?.let { Color(it) }
                        ?: MaterialTheme.colorScheme.primary,
                    caption = caption,
                )
            }
        }
    }
}
