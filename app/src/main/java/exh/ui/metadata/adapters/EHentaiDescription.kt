package exh.ui.metadata.adapters

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.ui.metadata.GenreChip
import exh.ui.metadata.MetadataUIUtil
import exh.ui.metadata.getRatingColor
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EHentaiDescription(
    meta: EHentaiSearchMetadata,
    openMetadataViewer: () -> Unit,
    onSearch: (String) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Row 1: Genre chip on left, rating on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val genreInfo = meta.genre?.let { MetadataUIUtil.getGenreAndColour(it, isDark) }
            val genreText = genreInfo?.second ?: meta.genre ?: "Unknown"
            val genreColor = genreInfo?.first
            GenreChip(genre = genreText, color = genreColor)

            val ratingFloat = meta.averageRating?.toFloat() ?: 0F
            val ratingColor = getRatingColor(ratingFloat)
            val fullStars = ratingFloat.toInt()
            val hasHalf = (ratingFloat - fullStars) >= 0.5f
            val starText = buildString {
                repeat(fullStars) { append("★") }
                if (hasHalf) append("½")
                repeat(5 - fullStars - if (hasHalf) 1 else 0) { append("☆") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = starText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ratingColor,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "%.2f".format(ratingFloat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Row 2: uploader + info button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = meta.uploader ?: "Unknown",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.combinedClickable(
                    onClick = { meta.uploader?.let { onSearch("uploader:\"$it\"") } },
                    onLongClick = { clipboardManager.setText(AnnotatedString(meta.uploader ?: "")) },
                ),
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = openMetadataViewer,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                )
            }
        }

        // Row 3: labeled stats (pages · size · favorites · posted · language)
        val hasAnyStats = meta.length != null ||
            (meta.size != null && meta.size!! > 0) ||
            meta.favorites != null ||
            meta.datePosted != null ||
            meta.language != null
        if (hasAnyStats) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                meta.length?.let {
                    StatItem("$it pages")
                }
                meta.size?.takeIf { it > 0 }?.let {
                    StatItem(MetadataUtil.humanReadableByteCount(it, true))
                }
                meta.favorites?.let {
                    StatItem("♡ ${NumberFormat.getInstance().format(it)}")
                }
                meta.datePosted?.let {
                    StatItem(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)))
                }
                meta.language?.let {
                    StatItem(it)
                }
            }
        }
    }
}

@Composable
private fun StatItem(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
