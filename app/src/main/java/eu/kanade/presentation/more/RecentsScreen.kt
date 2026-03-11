package eu.kanade.presentation.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.core.util.insertSeparators
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.history.HistoryUiModel
import eu.kanade.presentation.history.components.HistoryItem
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import eu.kanade.tachiyomi.util.lang.toLocalDate
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import java.time.LocalDate

@Composable
fun RecentsScreen(
    historyItems: List<HistoryUiModel.Item>,
    updateItems: List<UpdatesItem>,
    onClickHistoryCover: (Long) -> Unit,
    onClickHistory: (Long, Long) -> Unit,
    onClickHistoryFavorite: (Long) -> Unit,
    onDeleteHistory: (HistoryWithRelations) -> Unit,
    onClickUpdateCover: (Long) -> Unit,
    onClickUpdate: (Long, Long) -> Unit,
) {
    val allItems = remember(historyItems, updateItems) {
        buildRecentsCombinedUiModels(historyItems, updateItems)
    }

    ScrollbarLazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (allItems.isEmpty()) {
            item {
                RecentsEmpty(stringResource(MR.strings.information_no_recent))
            }
        } else {
            items(
                items = allItems,
                key = { item ->
                    when (item) {
                        is RecentsCombinedUiModel.Header -> "header-${item.date}"
                        is RecentsCombinedUiModel.History -> "history-${item.item.item.chapterId}"
                        is RecentsCombinedUiModel.Update -> "update-${item.item.update.chapterId}"
                    }
                },
                contentType = { item ->
                    when (item) {
                        is RecentsCombinedUiModel.Header -> "header"
                        is RecentsCombinedUiModel.History -> "history"
                        is RecentsCombinedUiModel.Update -> "update"
                    }
                },
            ) { item ->
                when (item) {
                    is RecentsCombinedUiModel.Header -> {
                        ListGroupHeader(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = relativeDateText(item.date),
                        )
                    }
                    is RecentsCombinedUiModel.History -> {
                        HistoryItem(
                            history = item.item.item,
                            onClickCover = { onClickHistoryCover(item.item.item.mangaId) },
                            onClickResume = {
                                onClickHistory(
                                    item.item.item.mangaId,
                                    item.item.item.chapterId,
                                )
                            },
                            onClickDelete = { onDeleteHistory(item.item.item) },
                            onClickFavorite = { onClickHistoryFavorite(item.item.item.mangaId) },
                        )
                    }
                    is RecentsCombinedUiModel.Update -> {
                        RecentsUpdateRow(
                            item = item.item,
                            onClickCover = { onClickUpdateCover(item.item.update.mangaId) },
                            onClick = { onClickUpdate(item.item.update.mangaId, item.item.update.chapterId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentsHistoryScreen(
    historyItems: List<HistoryUiModel.Item>,
    onClickHistoryCover: (Long) -> Unit,
    onClickHistory: (Long, Long) -> Unit,
    onClickHistoryFavorite: (Long) -> Unit,
    onDeleteHistory: (HistoryWithRelations) -> Unit,
) {
    ScrollbarLazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            RecentsSection(
                title = stringResource(MR.strings.label_recent_manga),
                subtitle = stringResource(MR.strings.action_resume),
                icon = Icons.Outlined.History,
            ) {
                if (historyItems.isEmpty()) {
                    RecentsEmpty(stringResource(MR.strings.information_no_recent_manga))
                } else {
                    historyItems.forEach { item ->
                        HistoryItem(
                            history = item.item,
                            onClickCover = { onClickHistoryCover(item.item.mangaId) },
                            onClickResume = { onClickHistory(item.item.mangaId, item.item.chapterId) },
                            onClickDelete = { onDeleteHistory(item.item) },
                            onClickFavorite = { onClickHistoryFavorite(item.item.mangaId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentsUpdatesScreen(
    updateItems: List<UpdatesItem>,
    onClickUpdateCover: (Long) -> Unit,
    onClickUpdate: (Long, Long) -> Unit,
) {
    ScrollbarLazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            RecentsSection(
                title = stringResource(MR.strings.label_recent_updates),
                subtitle = stringResource(MR.strings.chapters),
                icon = Icons.Outlined.NewReleases,
            ) {
                if (updateItems.isEmpty()) {
                    RecentsEmpty(stringResource(MR.strings.information_no_recent))
                } else {
                    updateItems.forEach { item ->
                        RecentsUpdateRow(
                            item = item,
                            onClickCover = { onClickUpdateCover(item.update.mangaId) },
                            onClick = { onClickUpdate(item.update.mangaId, item.update.chapterId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentsSection(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun RecentsUpdateRow(
    item: UpdatesItem,
    onClickCover: () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.clickable(onClick = onClickCover),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Outlined.NewReleases,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(8.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.update.mangaTitle,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.update.chapterName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecentsEmpty(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private sealed interface RecentsCombinedUiModel {
    data class Header(val date: LocalDate) : RecentsCombinedUiModel
    data class History(val item: HistoryUiModel.Item) : RecentsCombinedUiModel
    data class Update(val item: UpdatesItem) : RecentsCombinedUiModel
}

private fun buildRecentsCombinedUiModels(
    historyItems: List<HistoryUiModel.Item>,
    updateItems: List<UpdatesItem>,
): List<RecentsCombinedUiModel> {
    return buildList {
        historyItems.forEach { add(RecentsCombinedUiModel.History(it)) }
        updateItems.forEach { add(RecentsCombinedUiModel.Update(it)) }
    }
        .sortedByDescending { item ->
            when (item) {
                is RecentsCombinedUiModel.History -> item.item.item.readAt?.time ?: 0L
                is RecentsCombinedUiModel.Update -> item.item.update.dateFetch
                is RecentsCombinedUiModel.Header -> 0L
            }
        }
        .insertSeparators { before, after ->
            val beforeDate = before?.toLocalDate()
            val afterDate = after?.toLocalDate()
            when {
                beforeDate != afterDate && afterDate != null -> RecentsCombinedUiModel.Header(afterDate)
                else -> null
            }
        }
}

private fun RecentsCombinedUiModel.toLocalDate(): LocalDate? {
    return when (this) {
        is RecentsCombinedUiModel.Header -> date
        is RecentsCombinedUiModel.History -> item.item.readAt?.time?.toLocalDate()
        is RecentsCombinedUiModel.Update -> item.update.dateFetch.toLocalDate()
    }
}
