package exh.ui.metadata.adapters

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.compose.stringResource
import exh.metadata.metadata.PururinSearchMetadata
import exh.ui.metadata.GenreChip
import exh.ui.metadata.MetadataUIUtil
import exh.ui.metadata.RatingRow
import exh.ui.metadata.getRatingString
import yokai.i18n.MR
import kotlin.math.round

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PururinDescription(
    meta: PururinSearchMetadata,
    openMetadataViewer: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Genre
        val genreTag = meta.tags.find { it.namespace == PururinSearchMetadata.TAG_NAMESPACE_CATEGORY }
        val genreName = genreTag?.name ?: "Unknown"
        val isDark = isSystemInDarkTheme()
        val genreInfo = MetadataUIUtil.getGenreAndColour(genreName, isDark)
        GenreChip(
            genre = genreInfo?.second ?: genreName,
            color = genreInfo?.first,
        )

        // Uploader
        val uploaderText = meta.uploaderDisp ?: meta.uploader.orEmpty()
        if (uploaderText.isNotEmpty()) {
            Text(
                text = uploaderText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { clipboardManager.setText(AnnotatedString(uploaderText)) },
                ),
            )
        }

        // Size
        val sizeText = meta.fileSize ?: stringResource(MR.strings.unknown)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.SdCard,
                contentDescription = stringResource(MR.strings.file_size_label),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = sizeText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { clipboardManager.setText(AnnotatedString(sizeText)) },
                ),
            )
        }

        // Pages
        val pageCount = meta.pages ?: 0
        val pagesText = stringResource(MR.strings.page_count_format, pageCount)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = stringResource(MR.strings.page_count_label),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = pagesText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { clipboardManager.setText(AnnotatedString(pagesText)) },
                ),
            )
        }

        // Rating
        val ratingFloat = meta.averageRating?.toFloat() ?: 0F
        val displayRating = (round(ratingFloat * 100.0) / 100.0).toFloat()
        val ratingText = "$displayRating - ${getRatingString(ratingFloat * 2)}"
        RatingRow(rating = ratingFloat, ratingText = ratingText)

        // More info button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = openMetadataViewer) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(MR.strings.more_info),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
