package exh.recs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.util.compose.LocalBackPress
import eu.kanade.tachiyomi.util.compose.currentOrThrow
import exh.recs.batch.RankedSearchResults
import eu.kanade.tachiyomi.domain.manga.models.Manga
import yokai.i18n.MR
import yokai.presentation.AppBarType
import yokai.presentation.YokaiScaffold
import yokai.util.Screen
import java.io.Serializable

class BrowseRecommendsScreen(
    private val args: Args,
    private val isExternalSource: Boolean,
) : Screen() {

    sealed interface Args : Serializable {
        data class SingleSourceManga(
            val mangaId: Long,
            val sourceId: Long,
            val recommendationSourceName: String,
        ) : Args
        data class MergedSourceMangas(val results: RankedSearchResults) : Args
    }

    @Composable
    override fun Content() {
        val backPress = LocalBackPress.currentOrThrow

        val screenModel = rememberScreenModel { BrowseRecommendsScreenModel(args) }
        val state by screenModel.state.collectAsState()

        val title = remember {
            when (val currentArgs = args) {
                is Args.SingleSourceManga -> currentArgs.recommendationSourceName
                    .substringAfterLast('.')
                    .replace("PagingSource", "")
                is Args.MergedSourceMangas -> currentArgs.results.recSourceName
            }
        }

        YokaiScaffold(
            onNavigationIconClicked = backPress::invoke,
            title = title,
            navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
            appBarType = AppBarType.SMALL,
        ) { contentPadding ->
            when (val currentState = state) {
                is BrowseRecommendsScreenModel.State.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is BrowseRecommendsScreenModel.State.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = currentState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                is BrowseRecommendsScreenModel.State.Success -> {
                    if (currentState.mangas.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(contentPadding),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(MR.strings.rec_no_results),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    } else {
                        BrowseRecommendsGrid(
                            mangas = currentState.mangas,
                            contentPadding = contentPadding,
                            onClickItem = { manga ->
                                // TODO: Navigate to manga details or smart search
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseRecommendsGrid(
    mangas: List<Manga>,
    contentPadding: PaddingValues,
    onClickItem: (Manga) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 8.dp,
            start = 8.dp,
            end = 8.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = mangas,
            key = { "${it.source}-${it.url}" },
        ) { manga ->
            BrowseRecommendCard(
                manga = manga,
                onClick = { onClickItem(manga) },
            )
        }
    }
}

@Composable
private fun BrowseRecommendCard(
    manga: Manga,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
