package exh.ui.metadata

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.manga.components.MangaMetadataPage
import eu.kanade.presentation.manga.components.SearchMetadataChips
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

class MetadataViewScreen(private val mangaId: Long, private val sourceId: Long) : Screen() {

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { MetadataViewScreenModel(mangaId, sourceId) }
        val navigator = LocalNavigator.currentOrThrow

        val state by screenModel.state.collectAsState()
        val manga by screenModel.manga.collectAsState()

        when (
            @Suppress("NAME_SHADOWING")
            val state = state
        ) {
            MetadataViewState.Loading -> LoadingScreen()
            MetadataViewState.MetadataNotFound -> EmptyScreen(MR.strings.no_results_found)
            MetadataViewState.SourceNotFound -> EmptyScreen(MR.strings.source_empty_screen)
            is MetadataViewState.Success -> {
                val currentManga = manga
                if (currentManga == null) {
                    LoadingScreen()
                    return
                }

                val alternativeTitles = remember(state.meta, currentManga.title) {
                    state.meta.titles
                        .map { it.title.trim() }
                        .filter { it.isNotEmpty() && it != currentManga.title }
                        .distinct()
                }
                val extraInfo = remember(state.meta) {
                    state.meta.getExtraInfoPairs(screenModel.context)
                }
                val searchMetadata = remember(state.meta, currentManga.genre) {
                    SearchMetadataChips(state.meta, sourceId, currentManga.genre)
                }

                MangaMetadataPage(
                    onBackPressed = navigator::pop,
                    mangaTitle = currentManga.title,
                    thumbnailUrl = currentManga.thumbnailUrl,
                    sourceName = state.sourceName,
                    uploader = state.meta.uploader,
                    alternativeTitles = alternativeTitles,
                    extraInfo = extraInfo,
                    searchMetadataChips = searchMetadata,
                    onTagSearch = { query ->
                        navigator.push(BrowseSourceScreen(sourceId, query))
                    },
                )
            }
        }
    }
}
