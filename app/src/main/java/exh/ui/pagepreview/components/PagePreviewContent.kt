package exh.ui.pagepreview.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.source.PagePreviewInfo
import exh.ui.pagepreview.PagePreviewState
import yokai.i18n.MR
import yokai.presentation.AppBarType
import yokai.presentation.YokaiScaffold
import yokai.presentation.theme.LocalReducedMotion

@Composable
fun PagePreviewContent(
    state: PagePreviewState,
    imageLoader: ImageLoader? = null,
    onOpenPage: (Int) -> Unit,
    onLoadMore: () -> Unit,
    navigateUp: () -> Unit,
) {
    YokaiScaffold(
        onNavigationIconClicked = navigateUp,
        title = stringResource(MR.strings.page_previews),
        appBarType = AppBarType.SMALL,
    ) { paddingValues ->
        when (state) {
            is PagePreviewState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.error.message.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            PagePreviewState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is PagePreviewState.Success -> {
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    val itemPerRowCount = remember(maxWidth) {
                        (maxWidth / 120.dp).toInt().coerceAtLeast(1)
                    }
                    val chunkedItems = remember(state.pagePreviews, itemPerRowCount) {
                        state.pagePreviews.chunked(itemPerRowCount)
                    }
                    val lazyListState = rememberLazyListState()

                    // Trigger load more when near bottom
                    val shouldLoadMore by remember {
                        derivedStateOf {
                            val lastVisible = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            val totalItems = lazyListState.layoutInfo.totalItemsCount
                            lastVisible >= totalItems - 3 && state.hasNextPage && !state.isLoadingMore
                        }
                    }
                    LaunchedEffect(shouldLoadMore) {
                        if (shouldLoadMore) onLoadMore()
                    }

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(chunkedItems) { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                row.forEach { page ->
                                    PagePreviewItem(
                                        modifier = Modifier.weight(1F),
                                        page = page,
                                        imageLoader = imageLoader,
                                        onOpenPage = onOpenPage,
                                    )
                                }
                                repeat(itemPerRowCount - row.size) {
                                    Box(Modifier.weight(1F))
                                }
                            }
                        }
                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PagePreviewItem(
    modifier: Modifier,
    page: PagePreviewInfo,
    imageLoader: ImageLoader? = null,
    onOpenPage: (Int) -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }

    Column(
        modifier
            .clip(MaterialTheme.shapes.small)
            .clickable { onOpenPage(page.index - 1) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .height(200.dp)
                .width(120.dp)
                .clip(MaterialTheme.shapes.small),
        ) {
            if (imageLoader != null) {
                AsyncImage(
                    model = page.imageUrl,
                    contentDescription = stringResource(MR.strings.page_preview_image, page.index),
                    imageLoader = imageLoader,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillWidth,
                    onState = { state -> isLoading = state is AsyncImagePainter.State.Loading },
                )
            } else {
                AsyncImage(
                    model = page.imageUrl,
                    contentDescription = stringResource(MR.strings.page_preview_image, page.index),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillWidth,
                    onState = { state -> isLoading = state is AsyncImagePainter.State.Loading },
                )
            }
            if (isLoading) {
                val reducedMotion = LocalReducedMotion.current
                val alpha = if (reducedMotion) {
                    0.25f
                } else {
                    val infiniteTransition = rememberInfiniteTransition(label = "imgShimmer")
                    val animatedAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.15f,
                        targetValue = 0.4f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "imgShimmerAlpha",
                    )
                    animatedAlpha
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
                ) {}
            }
        }
        Text(
            text = page.index.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
