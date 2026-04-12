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
import dev.icerock.moko.resources.compose.stringResource
import exh.metadata.metadata.HBrowseSearchMetadata
import yokai.i18n.MR

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HBrowseDescription(
    meta: HBrowseSearchMetadata,
    openMetadataViewer: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
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
