package exh.ui.pagepreview.components

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
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
import androidx.compose.material.icons.outlined.UTurnRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.source.PagePreviewInfo
import exh.ui.pagepreview.PagePreviewState
import kotlinx.coroutines.launch
import yokai.i18n.MR
import yokai.presentation.AppBarType
import yokai.presentation.YokaiScaffold
import yokai.presentation.component.ToolTipButton
import kotlin.math.roundToInt

@Composable
fun PagePreviewContent(
    state: PagePreviewState,
    pageDialogOpen: Boolean,
    onPageSelected: (Int) -> Unit,
    onOpenPage: (Int) -> Unit,
    onOpenPageDialog: () -> Unit,
    onDismissPageDialog: () -> Unit,
    navigateUp: () -> Unit,
) {
    val showGoToAction = state is PagePreviewState.Success &&
        (state.pageCount != null && state.pageCount > 1)

    YokaiScaffold(
        onNavigationIconClicked = navigateUp,
        title = stringResource(MR.strings.page_previews),
        appBarType = AppBarType.SMALL,
        actions = {
            if (showGoToAction) {
                ToolTipButton(
                    toolTipLabel = stringResource(MR.strings.page_preview_go_to),
                    icon = Icons.Outlined.UTurnRight,
                    buttonClicked = onOpenPageDialog,
                )
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
                    val lazyListState = key(state.page) {
                        rememberLazyListState()
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
                                        onOpenPage = onOpenPage,
                                    )
                                }
                                // Fill remaining space if row is not full
                                repeat(itemPerRowCount - row.size) {
                                    Box(Modifier.weight(1F))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (pageDialogOpen && state is PagePreviewState.Success) {
        PagePreviewPageDialog(
            currentPage = state.page,
            pageCount = state.pageCount!!,
            onDismissPageDialog = onDismissPageDialog,
            onPageSelected = onPageSelected,
        )
    }
}

@Composable
private fun PagePreviewItem(
    modifier: Modifier,
    page: PagePreviewInfo,
    onOpenPage: (Int) -> Unit,
) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.small)
            .clickable { onOpenPage(page.index - 1) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        AsyncImage(
            model = page.imageUrl,
            contentDescription = stringResource(MR.strings.page_preview_image, page.index),
            modifier = Modifier
                .height(200.dp)
                .width(120.dp)
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.FillWidth,
        )
        Text(
            text = page.index.toString(),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PagePreviewPageDialog(
    currentPage: Int,
    pageCount: Int,
    onDismissPageDialog: () -> Unit,
    onPageSelected: (Int) -> Unit,
) {
    var page by remember(currentPage) {
        mutableStateOf(currentPage.toFloat())
    }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismissPageDialog,
        confirmButton = {
            TextButton(onClick = {
                onPageSelected(page.roundToInt())
                onDismissPageDialog()
            }) {
                Text(stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissPageDialog) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(stringResource(MR.strings.page_preview_go_to))
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = page.roundToInt().toString())
                Slider(
                    modifier = Modifier.weight(1f),
                    value = page,
                    onValueChange = { page = it },
                    onValueChangeFinished = {
                        scope.launch {
                            val newPage = page
                            AnimationState(
                                newPage,
                            ).animateTo(newPage.roundToInt().toFloat()) {
                                page = value
                            }
                        }
                    },
                    valueRange = 1F..pageCount.toFloat(),
                )
                Text(text = pageCount.toString())
            }
        },
    )
}
