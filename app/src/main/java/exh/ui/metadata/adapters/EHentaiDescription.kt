package exh.ui.metadata.adapters

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
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
import exh.metadata.MetadataUtil
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.ui.metadata.GenreChip
import exh.ui.metadata.MetadataUIUtil
import exh.ui.metadata.RatingRow
import exh.ui.metadata.getRatingString
import yokai.i18n.MR

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EHentaiDescription(
    meta: EHentaiSearchMetadata,
    openMetadataViewer: () -> Unit,
    onSearch: (String) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Genre chip
        val isDark = isSystemInDarkTheme()
        val genreInfo = meta.genre?.let { MetadataUIUtil.getGenreAndColour(it, isDark) }
        val genreText = genreInfo?.second ?: meta.genre ?: "Unknown"
        val genreColor = genreInfo?.first
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GenreChip(genre = genreText, color = genreColor)
        }

        // Visibility
        val visibilityValue = meta.visible ?: stringResource(MR.strings.unknown)
        Text(
            text = stringResource(MR.strings.visibility_label, visibilityValue),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = { clipboardManager.setText(AnnotatedString(visibilityValue)) },
            ),
        )

        // Favorites
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Book,
                contentDescription = stringResource(MR.strings.favorites_label),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = (meta.favorites ?: 0).toString(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { clipboardManager.setText(AnnotatedString((meta.favorites ?: 0).toString())) },
                ),
            )
        }

        // Uploader (clickable for search)
        Text(
            text = meta.uploader ?: "Unknown",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.combinedClickable(
                onClick = { meta.uploader?.let { onSearch("uploader:\"$it\"") } },
                onLongClick = { clipboardManager.setText(AnnotatedString(meta.uploader ?: "Unknown")) },
            ),
        )

        // Size
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.SdCard,
                contentDescription = stringResource(MR.strings.file_size_label),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = MetadataUtil.humanReadableByteCount(meta.size ?: 0, true),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { clipboardManager.setText(AnnotatedString(MetadataUtil.humanReadableByteCount(meta.size ?: 0, true))) },
                ),
            )
        }

        // Pages
        val pageCount = meta.length ?: 0
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

        // Language
        val language = meta.language ?: stringResource(MR.strings.unknown)
        val languageText = if (meta.translated == true) {
            stringResource(MR.strings.language_translated, language)
        } else {
            language
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = stringResource(MR.strings.language_label),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = languageText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { clipboardManager.setText(AnnotatedString(languageText)) },
                ),
            )
        }

        // Rating
        val ratingFloat = meta.averageRating?.toFloat() ?: 0F
        val ratingText = "$ratingFloat - ${getRatingString(ratingFloat * 2)}"
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
