package exh.ui.pagepreview.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.source.PagePreviewInfo
import eu.kanade.tachiyomi.source.PagePreviewSource
import eu.kanade.tachiyomi.source.SourceManager
import exh.source.getMainSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import yokai.domain.chapter.interactor.GetChapter
import yokai.domain.manga.interactor.GetManga

private sealed class PreviewState {
    data object Loading : PreviewState()
    data object Unavailable : PreviewState()
    data class Success(val previews: List<PagePreviewInfo>) : PreviewState()
}

@Composable
fun PagePreviewInlineSection(
    mangaId: Long,
    sourceId: Long,
    onOpenPagePreview: () -> Unit,
    onOpenReaderAtPage: (Int) -> Unit,
) {
    var state by remember { mutableStateOf<PreviewState>(PreviewState.Loading) }

    LaunchedEffect(mangaId) {
        withContext(Dispatchers.IO) {
            try {
                val getManga: GetManga = Injekt.get()
                val getChapter: GetChapter = Injekt.get()
                val sourceManager: SourceManager = Injekt.get()

                val manga = getManga.awaitById(mangaId) ?: run {
                    state = PreviewState.Unavailable; return@withContext
                }
                val chapters = getChapter.awaitAll(mangaId, filterScanlators = false)
                if (chapters.isEmpty()) {
                    state = PreviewState.Unavailable; return@withContext
                }
                val source = sourceManager.getOrStub(manga.source)
                val previewSource = source.getMainSource<PagePreviewSource>() ?: run {
                    state = PreviewState.Unavailable; return@withContext
                }
                val previewPage = previewSource.getPagePreviewList(
                    manga,
                    chapters.sortedByDescending { it.source_order },
                    page = 1,
                )
                state = if (previewPage.pagePreviews.isNotEmpty()) {
                    PreviewState.Success(previewPage.pagePreviews)
                } else {
                    PreviewState.Unavailable
                }
            } catch (_: Exception) {
                state = PreviewState.Unavailable
            }
        }
    }

    when (val s = state) {
        PreviewState.Loading -> {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        PreviewState.Unavailable -> { /* Don't show anything */ }
        is PreviewState.Success -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp),
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(s.previews) { preview ->
                        Column(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onOpenReaderAtPage(preview.index - 1) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AsyncImage(
                                model = preview.imageUrl,
                                contentDescription = "Page ${preview.index}",
                                modifier = Modifier
                                    .height(112.dp)
                                    .width(80.dp)
                                    .clip(MaterialTheme.shapes.small),
                                contentScale = ContentScale.FillWidth,
                            )
                            Text(
                                text = preview.index.toString(),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .height(112.dp)
                                .width(64.dp)
                                .clickable { onOpenPagePreview() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "View all",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}
