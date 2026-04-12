package eu.kanade.tachiyomi.ui.manga

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.base.FlatMetadata
import exh.ui.metadata.adapters.EHentaiDescription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import yokai.domain.manga.metadata.MangaMetadataRepository

private object MetadataLoader : KoinComponent {
    val metadataRepository: MangaMetadataRepository by inject()
}

@Composable
fun MangaContinueReadingButton(
    readButtonText: String,
    readEnabled: Boolean,
    showButton: Boolean,
    accentColorInt: Int?,
    onReadClick: () -> Unit,
) {
    if (!showButton) return

    val containerColor = if (accentColorInt != null) {
        Color(accentColorInt)
    } else {
        MaterialTheme.colorScheme.primary
    }
    val contentColor = if (accentColorInt != null) {
        if (ColorUtils.calculateLuminance(accentColorInt) > 0.5) Color.Black else Color.White
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    Button(
        onClick = onReadClick,
        enabled = readEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = readButtonText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MangaMetadataSection(
    mangaId: Long,
    sourceId: Long,
    openMetadataViewer: () -> Unit,
    onSearch: (String) -> Unit,
) {
    var metadata by remember { mutableStateOf<RaisedSearchMetadata?>(null) }

    LaunchedEffect(mangaId) {
        withContext(Dispatchers.IO) {
            try {
                val repo = MetadataLoader.metadataRepository
                val searchMeta = repo.getMetadataById(mangaId) ?: return@withContext
                val tags = repo.getTagsById(mangaId)
                val titles = repo.getTitlesById(mangaId)
                val flat = FlatMetadata(metadata = searchMeta, tags = tags, titles = titles)
                metadata = flat.raise<RaisedSearchMetadata>()
            } catch (_: Exception) {
                // No metadata available
            }
        }
    }

    val meta = metadata ?: return

    when (meta) {
        is EHentaiSearchMetadata -> {
            EHentaiDescription(
                meta = meta,
                openMetadataViewer = openMetadataViewer,
                onSearch = onSearch,
            )
        }
        else -> {
            // Other metadata types can be added here
        }
    }
}
