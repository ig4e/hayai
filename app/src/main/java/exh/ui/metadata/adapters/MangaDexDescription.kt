package exh.ui.metadata.adapters

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import dev.icerock.moko.resources.compose.stringResource
import exh.metadata.metadata.MangaDexSearchMetadata
import exh.ui.metadata.MetadataUIUtil
import exh.ui.metadata.RatingRow
import exh.ui.metadata.getRatingString
import yokai.i18n.MR
import kotlin.math.round

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MangaDexDescription(
    meta: MangaDexSearchMetadata,
    openMetadataViewer: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Rating
        val ratingFloat = meta.rating
        if (ratingFloat != null) {
            val displayRating = (round(ratingFloat * 100.0) / 100.0).toFloat()
            val ratingText = "$displayRating - ${getRatingString(ratingFloat)}"
            RatingRow(rating = ratingFloat / 2F, ratingText = ratingText)

            Text(
                text = ratingText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { clipboardManager.setText(AnnotatedString(ratingText)) },
                ),
            )
        }

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
