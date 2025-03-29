package exh.ui.metadata

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.manga.components.MangaMetadataPage
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.source.online.all.EHentai
import eu.kanade.tachiyomi.source.online.all.MangaDex
import eu.kanade.tachiyomi.util.system.copyToClipboard
import exh.source.getMainSource
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaMetadataScreen(private val mangaId: Long, private val sourceId: Long) : Screen() {

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { MetadataViewScreenModel(mangaId, sourceId) }
        val navigator = LocalNavigator.currentOrThrow
        val state by screenModel.state.collectAsState()
        val manga by screenModel.manga.collectAsState()
        val sourceManager = Injekt.get<SourceManager>()

        when (val currentState = state) {
            is MetadataViewState.Success -> {
                val context = LocalContext.current
                val source = remember { sourceManager.get(sourceId) }
                val metadataSource = remember { source?.getMainSource<MetadataSource<*, *>>() }
                val meta = currentState.meta

                // Determine if this is an EHentai source
                val isEhentaiSource = metadataSource is EHentai
                val isSpecialSource = metadataSource is EHentai || metadataSource is MangaDex

                // Extract metadata information based on the source
                val sourceName = source?.name ?: ""
                val thumbnailUrl = manga?.thumbnailUrl
                val mangaTitle = manga?.title ?: ""

                // Tags organized by namespace (primarily for EHentai)
                val tagsByNamespace = meta.tags.groupBy { it.namespace ?: "" }
                    .mapValues { entry -> entry.value.map { it.name } }

                // Extract other metadata
                val originalLanguage = meta.tags.find { it.namespace?.equals("language", ignoreCase = true) == true }?.name
                val alternativeTitles = meta.titles.filter { it.type != 0 }.map { it.title }
                val externalLinks = emptyMap<String, String>() // Could be populated from metadata if available

                // Publication info
                val status = meta.tags.find { it.namespace?.equals("status", ignoreCase = true) == true }?.name
                val releaseDate = meta.tags.find { it.namespace?.equals("date", ignoreCase = true) == true }?.name
                val lastUpdate = null // Could be extracted if available

                // Statistics
                val rating = meta.tags.find { it.namespace?.equals("rating", ignoreCase = true) == true }?.name?.toFloatOrNull()
                val ratingCount = null // Could be extracted if available
                val viewCount = null // Could be extracted if available

                // Creator info
                val authors = meta.tags.filter { it.namespace?.equals("artist", ignoreCase = true) == true ||
                                                it.namespace?.equals("author", ignoreCase = true) == true }
                                     .map { it.name }
                val artists = meta.tags.filter { it.namespace?.equals("artist", ignoreCase = true) == true }
                                     .map { it.name }

                // Source URL
                val sourceUrl = null // We don't have direct access to this

                MangaMetadataPage(
                    onBackPressed = navigator::pop,
                    mangaTitle = mangaTitle,
                    thumbnailUrl = thumbnailUrl,
                    isEhentaiSource = isEhentaiSource,
                    sourceName = sourceName,
                    originalLanguage = originalLanguage,
                    alternativeTitles = alternativeTitles,
                    externalLinks = externalLinks,
                    status = status,
                    releaseDate = releaseDate,
                    lastUpdate = lastUpdate,
                    rating = rating,
                    ratingCount = ratingCount,
                    viewCount = viewCount,
                    authors = authors,
                    artists = artists,
                    sourceUrl = sourceUrl,
                    tagsByNamespace = tagsByNamespace,
                    onTagClick = { tag -> /* Implement tag search */ },
                    onCopyTagToClipboard = { tag -> context.copyToClipboard(tag, tag) },
                    isSpecialSource = isSpecialSource,
                    onViewMoreInfo = null // Already showing detailed info
                )
            }
            MetadataViewState.Loading,
            MetadataViewState.MetadataNotFound,
            MetadataViewState.SourceNotFound -> {
                // Fallback to original MetadataViewScreen content for these states
                MetadataViewScreen(mangaId, sourceId).Content()
            }
        }
    }
}
