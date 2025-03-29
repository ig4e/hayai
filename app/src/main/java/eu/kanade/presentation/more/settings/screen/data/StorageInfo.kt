package eu.kanade.presentation.more.settings.screen.data

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.util.storage.DiskUtil
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.secondaryItemAlpha
import java.io.File

@Composable
fun StorageInfo(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val storages = remember { DiskUtil.getExternalStorages(context) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        storages.forEach {
            StorageInfoCard(it)
        }
    }
}

@Composable
private fun StorageInfoCard(
    file: File,
) {
    val context = LocalContext.current

    val available = remember(file) { DiskUtil.getAvailableStorageSpace(file) }
    val availableText = remember(available) { Formatter.formatFileSize(context, available) }
    val total = remember(file) { DiskUtil.getTotalStorageSpace(file) }
    val totalText = remember(total) { Formatter.formatFileSize(context, total) }

    val usedPercentage = remember(available, total) { (1 - (available / total.toFloat())) }

    // Get theme colors once to avoid MaterialTheme access within remember block
    val errorColor = MaterialTheme.colorScheme.error
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val primaryColor = MaterialTheme.colorScheme.primary

    val progressColor = remember(usedPercentage, errorColor, tertiaryColor, primaryColor) {
        when {
            usedPercentage > 0.9f -> errorColor
            usedPercentage > 0.75f -> tertiaryColor
            else -> primaryColor
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Storage,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = primaryColor
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = file.absolutePath,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .fillMaxWidth()
                    .height(12.dp),
                progress = { usedPercentage },
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(MR.strings.available_disk_space_info, availableText, totalText),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val usedPercentText = remember(usedPercentage) {
                    String.format("%.1f%%", usedPercentage * 100)
                }

                Text(
                    text = usedPercentText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = progressColor,
                )
            }
        }
    }
}
