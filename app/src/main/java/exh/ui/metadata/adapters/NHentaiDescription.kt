package exh.ui.metadata.adapters

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
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
import exh.metadata.MetadataUtil
import exh.metadata.metadata.NHentaiSearchMetadata
import exh.ui.metadata.GenreChip
import exh.ui.metadata.MetadataUIUtil
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NHentaiDescription(
    meta: NHentaiSearchMetadata,
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
        val categoriesText = meta.tags
            .filter { it.namespace == NHentaiSearchMetadata.NHENTAI_CATEGORIES_NAMESPACE }
            .takeIf { it.isNotEmpty() }
            ?.joinToString { it.name }
            ?: "Unknown"
        val genreInfo = MetadataUIUtil.getGenreAndColour(categoriesText)
        GenreChip(
            genre = genreInfo?.second ?: categoriesText,
            color = genreInfo?.first,
        )

        // Favorites
        meta.favoritesCount?.takeIf { it > 0 }?.let { favCount ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = favCount.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = { clipboardManager.setText(AnnotatedString(favCount.toString())) },
                    ),
                )
            }
        }

        // Upload date
        val dateText = MetadataUtil.EX_DATE_FORMAT.format(
            ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(meta.uploadDate ?: 0),
                ZoneId.systemDefault(),
            ),
        )
        Text(
            text = dateText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = { clipboardManager.setText(AnnotatedString(dateText)) },
            ),
        )

        // Pages
        val pageCount = meta.pageImagePreviewUrls.size
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$pageCount pages",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { clipboardManager.setText(AnnotatedString("$pageCount pages")) },
                ),
            )
        }

        // ID
        val idText = "#${meta.nhId ?: 0}"
        Text(
            text = idText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = { clipboardManager.setText(AnnotatedString(idText)) },
            ),
        )

        // More info button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = openMetadataViewer) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "More info",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
