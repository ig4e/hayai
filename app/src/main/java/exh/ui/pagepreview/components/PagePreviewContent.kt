package exh.ui.pagepreview.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.source.PagePreviewInfo
import exh.ui.pagepreview.PagePreviewState
import kotlinx.coroutines.flow.SharedFlow
import yokai.i18n.MR
import yokai.presentation.AppBarType
import yokai.presentation.YokaiScaffold
import yokai.presentation.theme.rememberShimmerAlpha
import yokai.util.coil.hayaiPagePreviewDefaults

@Composable
fun PagePreviewContent(
    state: PagePreviewState,
    imageLoader: ImageLoader,
    onOpenPage: (Int) -> Unit,
    onLoadMore: () -> Unit,
    onJumpToPage: (Int) -> Unit = {},
    scrollEvents: SharedFlow<Int>? = null,
    navigateUp: () -> Unit,
) {
    var showBatchMenu by remember { mutableStateOf(false) }
    val successState = state as? PagePreviewState.Success
    val batchSize = successState?.batchSize
    val totalPages = successState?.estimatedTotalPages
    val batchRanges = remember(batchSize, totalPages) {
        if (batchSize == null || totalPages == null || batchSize <= 0 || totalPages <= 0) {
            emptyList()
        } else {
            val count = (totalPages + batchSize - 1) / batchSize
            List(count) { i ->
                val first = i * batchSize + 1
                val last = minOf((i + 1) * batchSize, totalPages)
                first to last
            }
        }
    }

    YokaiScaffold(
        onNavigationIconClicked = navigateUp,
        title = stringResource(MR.strings.page_previews),
        appBarType = AppBarType.SMALL,
        actions = {
            if (batchRanges.isNotEmpty()) {
                Box {
                    IconButton(onClick = { showBatchMenu = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Numbers,
                            contentDescription = stringResource(MR.strings.page_preview_go_to),
                        )
                    }
                    DropdownMenu(
                        expanded = showBatchMenu,
                        onDismissRequest = { showBatchMenu = false },
                    ) {
                        batchRanges.forEach { (first, last) ->
                            DropdownMenuItem(
                                text = { Text("$first – $last") },
                                onClick = {
                                    showBatchMenu = false
                                    onJumpToPage(first)
                                },
                            )
                        }
                    }
                }
            }
        },
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

                    // Scroll the list when the model emits a jump target. Keyed only on the flow
                    // (and itemPerRowCount for rotation safety) so list replacement after a jump
                    // doesn't cancel the collector mid-emit.
                    LaunchedEffect(scrollEvents, itemPerRowCount) {
                        scrollEvents?.collect { previewIndex ->
                            val rowIndex = previewIndex / itemPerRowCount
                            lazyListState.scrollToItem(rowIndex.coerceAtLeast(0))
                        }
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
    imageLoader: ImageLoader,
    onOpenPage: (Int) -> Unit,
) {
    // Re-key on imageUrl so recycled cells reset to the loading state for their new
    // page instead of inheriting the previous cell's "done" state and flashing
    // a stale image briefly.
    var isLoading by remember(page.imageUrl) { mutableStateOf(true) }
    val context = LocalContext.current
    val request = remember(page.imageUrl, context) {
        ImageRequest.Builder(context)
            .data(page.imageUrl)
            .hayaiPagePreviewDefaults()
            .build()
    }

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
            AsyncImage(
                model = request,
                contentDescription = stringResource(MR.strings.page_preview_image, page.index),
                imageLoader = imageLoader,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillWidth,
                onState = { state -> isLoading = state is AsyncImagePainter.State.Loading },
            )
            if (isLoading) {
                val alpha by rememberShimmerAlpha(label = "imgShimmer")
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

