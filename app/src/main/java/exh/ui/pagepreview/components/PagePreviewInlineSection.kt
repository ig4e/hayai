package exh.ui.pagepreview.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.source.PagePreviewInfo
import eu.kanade.tachiyomi.source.PagePreviewSource
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import exh.source.getMainSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import yokai.domain.chapter.interactor.GetChapter
import yokai.domain.manga.interactor.GetManga
import yokai.i18n.MR
import yokai.presentation.theme.LocalReducedMotion

private sealed class PreviewState {
    data object Loading : PreviewState()
    data object Unavailable : PreviewState()
    data class Success(
        val previews: List<PagePreviewInfo>,
        val sourceClient: OkHttpClient? = null,
    ) : PreviewState()
}

private val THUMB_HEIGHT = 150.dp
private val THUMB_WIDTH = 105.dp

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
                val source = sourceManager.getOrStub(manga.source)
                val previewSource = source.getMainSource<PagePreviewSource>() ?: run {
                    state = PreviewState.Unavailable; return@withContext
                }
                val httpClient = (previewSource as? HttpSource)?.client
                val previewPage = previewSource.getPagePreviewList(
                    manga,
                    chapters.sortedByDescending { it.source_order },
                    page = 1,
                )
                state = if (previewPage.pagePreviews.isNotEmpty()) {
                    PreviewState.Success(previewPage.pagePreviews, httpClient)
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
            // Skeleton loading cards
            val reducedMotion = LocalReducedMotion.current
            val alpha = if (reducedMotion) {
                0.25f
            } else {
                val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
                val animatedAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.15f,
                    targetValue = 0.4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "shimmerAlpha",
                )
                animatedAlpha
            }
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(5) {
                    Surface(
                        modifier = Modifier
                            .height(THUMB_HEIGHT)
                            .width(THUMB_WIDTH),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
                    ) {}
                }
            }
        }
        PreviewState.Unavailable -> { /* Don't show anything */ }
        is PreviewState.Success -> {
            val context = LocalContext.current
            val imageLoader = remember(s.sourceClient) {
                val clientLazy = lazy { s.sourceClient ?: okhttp3.OkHttpClient() }
                ImageLoader.Builder(context)
                    .components {
                        add(OkHttpNetworkFetcherFactory(clientLazy::value))
                    }
                    .build()
            }
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
                                imageLoader = imageLoader,
                                modifier = Modifier
                                    .height(THUMB_HEIGHT)
                                    .width(THUMB_WIDTH)
                                    .clip(MaterialTheme.shapes.small),
                                contentScale = ContentScale.FillWidth,
                            )
                            Text(
                                text = preview.index.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPagePreview() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(MR.strings.view_all),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
