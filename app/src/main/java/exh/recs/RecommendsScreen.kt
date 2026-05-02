package exh.recs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.util.compose.LocalBackPress
import eu.kanade.tachiyomi.util.compose.LocalRouter
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import eu.kanade.tachiyomi.util.compose.currentOrThrow
import exh.recs.batch.RankedSearchResults
import exh.recs.sources.RecommendationPagingSource
import exh.recs.sources.StaticResultPagingSource
import yokai.i18n.MR
import yokai.presentation.AppBarType
import yokai.presentation.YokaiScaffold
import yokai.util.Screen
import java.io.Serializable

class RecommendsScreen(private val args: Args) : Screen() {

    sealed interface Args : Serializable {
        data class SingleSourceManga(val mangaId: Long, val sourceId: Long) : Args
        data class MergedSourceMangas(val mergedResults: List<RankedSearchResults>) : Args
    }

    @Composable
    override fun Content() {
        val backPress = LocalBackPress.currentOrThrow
        val router = LocalRouter.current
        val navigator = LocalNavigator.current

        val screenModel = rememberScreenModel { RecommendsScreenModel(args) }
        val state by screenModel.state.collectAsState()

        val title = when (args) {
            is Args.SingleSourceManga -> {
                stringResource(MR.strings.similar, state.title.orEmpty())
            }
            is Args.MergedSourceMangas -> {
                stringResource(MR.strings.rec_common_recommendations)
            }
        }

        YokaiScaffold(
            onNavigationIconClicked = backPress::invoke,
            title = title,
            navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
            appBarType = AppBarType.SMALL,
        ) { contentPadding ->
            RecommendsContent(
                items = state.filteredItems,
                isFullyLoaded = state.progress == state.total && state.total > 0,
                contentPadding = contentPadding,
                onClickSource = onClickSource@{ pagingSource ->
                    val nav = navigator ?: return@onClickSource
                    when (val currentArgs = args) {
                        is Args.SingleSourceManga -> nav.push(
                            BrowseRecommendsScreen(
                                args = BrowseRecommendsScreen.Args.SingleSourceManga(
                                    mangaId = currentArgs.mangaId,
                                    sourceId = currentArgs.sourceId,
                                    recommendationSourceName = pagingSource::class.qualifiedName!!,
                                ),
                                isExternalSource = false,
                            ),
                        )
                        is Args.MergedSourceMangas -> {
                            val staticSource = pagingSource as? StaticResultPagingSource
                                ?: return@onClickSource
                            nav.push(
                                BrowseRecommendsScreen(
                                    args = BrowseRecommendsScreen.Args.MergedSourceMangas(staticSource.data),
                                    isExternalSource = false,
                                ),
                            )
                        }
                    }
                },
                onClickItem = { manga ->
                    router?.pushController(
                        recommendationDestination(manga).withFadeTransaction(),
                    )
                },
                onLongClickItem = { manga ->
                    router?.pushController(
                        recommendationDestination(manga).withFadeTransaction(),
                    )
                },
            )
        }
    }
}

@Composable
private fun RecommendsContent(
    items: Map<RecommendationPagingSource, RecommendationItemResult>,
    isFullyLoaded: Boolean,
    contentPadding: PaddingValues,
    onClickSource: (RecommendationPagingSource) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            if (isFullyLoaded) {
                Text(
                    text = stringResource(MR.strings.rec_no_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                CircularProgressIndicator()
            }
        }
        return
    }

    LazyColumn(
        contentPadding = contentPadding,
    ) {
        items.forEach { (source, recResult) ->
            item(key = "${source::class.simpleName}-${source.name}-${source.category.resourceId}") {
                RecommendationSourceItem(
                    source = source,
                    result = recResult,
                    onClickSource = onClickSource,
                    onClickItem = onClickItem,
                    onLongClickItem = onLongClickItem,
                )
            }
        }
    }
}

@Composable
private fun RecommendationSourceItem(
    source: RecommendationPagingSource,
    result: RecommendationItemResult,
    onClickSource: (RecommendationPagingSource) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Source header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClickSource(source) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(source.category),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = stringResource(MR.strings.view_all_recommendations),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Results
        when (result) {
            RecommendationItemResult.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is RecommendationItemResult.Success -> {
                if (result.isEmpty) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(MR.strings.no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    RecommendationCardRow(
                        titles = result.result,
                        onClickItem = onClickItem,
                        onLongClickItem = onLongClickItem,
                    )
                }
            }
            is RecommendationItemResult.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = result.throwable.message ?: stringResource(MR.strings.rec_error_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationCardRow(
    titles: List<Manga>,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = titles,
            key = { "${it.source}-${it.url}" },
        ) { manga ->
            RecommendationCard(
                manga = manga,
                onClick = { onClickItem(manga) },
                onLongClick = { onLongClickItem(manga) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecommendationCard(
    manga: Manga,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(manga.thumbnail_url)
                    .crossfade(true)
                    .build(),
                contentDescription = manga.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = manga.title,
                modifier = Modifier.padding(4.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
