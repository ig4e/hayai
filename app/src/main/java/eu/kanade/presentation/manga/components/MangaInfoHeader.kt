package eu.kanade.presentation.manga.components

import android.app.Activity
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallMerge
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CallMerge
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Recommend
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.system.copyToClipboard
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clickableNoIndication
import tachiyomi.presentation.core.util.secondaryItemAlpha
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import eu.kanade.presentation.manga.components.NamespaceTags

private val whitespaceLineRegex = Regex("[\\r\\n]{2,}", setOf(RegexOption.MULTILINE))

@Composable
fun MangaInfoBox(
    isTabletUi: Boolean,
    appBarPadding: Dp,
    manga: Manga,
    sourceName: String,
    isStubSource: Boolean,
    onCoverClick: () -> Unit,
    doSearch: (query: String, global: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // Backdrop with enhanced gradient blend
        val backdropGradientColors = listOf(
            Color.Transparent,
            Color.Transparent,
            MaterialTheme.colorScheme.background.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
            MaterialTheme.colorScheme.background,
        )
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(manga)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = backdropGradientColors,
                            startY = 0f,
                            endY = size.height,
                        ),
                    )
                }
                .blur(16.dp)
                .alpha(0.65f),
        )

        // Manga & source info
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            if (!isTabletUi) {
                MangaAndSourceTitlesSmall(
                    appBarPadding = appBarPadding,
                    manga = manga,
                    sourceName = sourceName,
                    isStubSource = isStubSource,
                    onCoverClick = onCoverClick,
                    doSearch = doSearch,
                )
            } else {
                MangaAndSourceTitlesLarge(
                    appBarPadding = appBarPadding,
                    manga = manga,
                    sourceName = sourceName,
                    isStubSource = isStubSource,
                    onCoverClick = onCoverClick,
                    doSearch = doSearch,
                )
            }
        }
    }
}

@Composable
fun MangaActionRow(
    favorite: Boolean,
    trackingCount: Int,
    nextUpdate: Instant?,
    isUserIntervalMode: Boolean,
    onAddToLibraryClicked: () -> Unit,
    onTrackingClicked: () -> Unit,
    onEditIntervalClicked: (() -> Unit)?,
    onEditCategory: (() -> Unit)?,
    // SY -->
    onMergeClicked: (() -> Unit)?,
    onRecommendClicked: (() -> Unit)?,
    // SY <--
    modifier: Modifier = Modifier,
) {
    val defaultActionColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
    val activeActionColor = MaterialTheme.colorScheme.primary

    // Calculate available actions
    val actions = buildList {
        add(
            ActionItem(
                title = if (favorite) stringResource(MR.strings.in_library) else stringResource(MR.strings.add_to_library),
                icon = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                color = if (favorite) activeActionColor else defaultActionColor,
                active = favorite,
                onClick = onAddToLibraryClicked,
                onLongClick = onEditCategory,
            )
        )

        onEditIntervalClicked?.let {
            val updateDays = if (nextUpdate != null) {
                val now = Instant.now()
                now.until(nextUpdate, ChronoUnit.DAYS).toInt().coerceAtLeast(0)
            } else {
                null
            }

            add(
                ActionItem(
                    title = when (updateDays) {
                        null -> stringResource(MR.strings.not_applicable)
                        0 -> stringResource(MR.strings.manga_interval_expected_update_soon)
                        else -> pluralStringResource(MR.plurals.day, count = updateDays, updateDays)
                    },
                    icon = Icons.Default.HourglassEmpty,
                    color = if (isUserIntervalMode) activeActionColor else defaultActionColor,
                    active = isUserIntervalMode,
                    onClick = onEditIntervalClicked,
                )
            )
        }

        add(
            ActionItem(
                title = if (trackingCount == 0) {
                    stringResource(MR.strings.manga_tracking_tab)
                } else {
                    pluralStringResource(MR.plurals.num_trackers, count = trackingCount, trackingCount)
                },
                icon = if (trackingCount == 0) Icons.Outlined.Sync else Icons.Outlined.Done,
                color = if (trackingCount == 0) defaultActionColor else activeActionColor,
                active = trackingCount > 0,
                onClick = onTrackingClicked,
            )
        )

        // SY -->
        onRecommendClicked?.let {
            add(
                ActionItem(
                    title = stringResource(SYMR.strings.az_recommends_short),
                    icon = Icons.Outlined.Recommend,
                    color = defaultActionColor,
                    onClick = onRecommendClicked,
                )
            )
        }

        onMergeClicked?.let {
            add(
                ActionItem(
                    title = stringResource(SYMR.strings.merge),
                    icon = Icons.AutoMirrored.Outlined.CallMerge,
                    color = defaultActionColor,
                    onClick = onMergeClicked,
                )
            )
        }
        // SY <--
    }

    Surface(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        Layout(
            content = {
                actions.forEach { action ->
                    CompactActionButton(
                        title = action.title,
                        icon = action.icon,
                        color = action.color,
                        active = action.active,
                        onClick = action.onClick,
                        onLongClick = action.onLongClick,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().padding(4.dp),
        ) { measurables, constraints ->
            // Adaptive layout based on available width
            val itemCount = measurables.size
            val itemWidth = (constraints.maxWidth / itemCount.coerceAtMost(4)).coerceAtLeast(56)

            // Decide how many items per row based on available width
            val columns = (constraints.maxWidth / itemWidth).coerceAtLeast(1)
            val rows = (itemCount + columns - 1) / columns

            val itemConstraints = constraints.copy(
                minWidth = itemWidth,
                maxWidth = itemWidth,
                minHeight = 0,
            )

            val placeables = measurables.map { it.measure(itemConstraints) }
            val height = placeables.maxOf { it.height } * rows

            layout(constraints.maxWidth, height) {
                var x = 0
                var y = 0
                var rowHeight = 0

                placeables.forEachIndexed { index, placeable ->
                    placeable.placeRelative(x, y)
                    rowHeight = maxOf(rowHeight, placeable.height)

                    x += itemWidth
                    if ((index + 1) % columns == 0) {
                        x = 0
                        y += rowHeight
                        rowHeight = 0
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val textColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else color

    Surface(
        modifier = modifier,
        color = if (active) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        shape = MaterialTheme.shapes.small,
        tonalElevation = if (active) 4.dp else 0.dp,
        shadowElevation = if (active) 2.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier
                .clickableNoIndication(
                    onClick = onClick,
                    onLongClick = onLongClick?.let { longClick ->
                        {
                            longClick()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                )
                .padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = textColor,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private data class ActionItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit,
    val active: Boolean = false,
    val onLongClick: (() -> Unit)? = null,
)

@Composable
fun ExpandableMangaDescription(
    defaultExpandState: Boolean,
    description: String?,
    tagsProvider: () -> List<String>?,
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,
    // SY -->
    searchMetadataChips: SearchMetadataChips?,
    doSearch: (query: String, global: Boolean) -> Unit,
    // For MangaDex/E-Hentai Sources
    isSpecialSource: Boolean = false,
    onViewMoreInfo: (() -> Unit)? = null,
    // SY <--
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        val (expanded, onExpanded) = rememberSaveable {
            mutableStateOf(defaultExpandState)
        }
        val desc =
            description.takeIf { !it.isNullOrBlank() } ?: stringResource(MR.strings.description_placeholder)
        val trimmedDescription = remember(desc) {
            desc
                .replace(whitespaceLineRegex, "\n")
                .trimEnd()
        }

        // Section title with decorative element
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(MR.strings.description),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Improved manga summary
        MangaSummary(
            expandedDescription = desc,
            shrunkDescription = trimmedDescription,
            expanded = expanded,
            modifier = Modifier
                .fillMaxWidth()
                .clickableNoIndication { onExpanded(!expanded) },
        )

        // Tags section
        val tags = tagsProvider()
        if (!tags.isNullOrEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(MR.strings.genres),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Enhanced tags display with animation
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessLow,
                        )
                    ),
            ) {
                if (expanded) {
                    // When expanded, use FlowRow to display tags in multiple rows
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tags.forEach { tag ->
                            val haptic = LocalHapticFeedback.current
                            TagChip(
                                text = tag,
                                onClick = { onTagSearch(tag) },
                                onLongClick = {
                                    onCopyTagToClipboard(tag)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                            )
                        }
                    }
                } else {
                    // When collapsed, use LazyRow for horizontal scrolling
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tags) { tag ->
                            val haptic = LocalHapticFeedback.current
                            TagChip(
                                text = tag,
                                onClick = { onTagSearch(tag) },
                                onLongClick = {
                                    onCopyTagToClipboard(tag)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                            )
                        }
                    }
                }
            }
        }

        // SY -->
        searchMetadataChips?.let { chips ->
            // Only show if chips has content
            if (chips.hasContent()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(MR.strings.tags),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                // Ensure proper display when expanded
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = 0.8f,
                                stiffness = Spring.StiffnessLow,
                            )
                        )
                ) {
                    // Display chips from SearchMetadataChips, passing the expanded state
                    chips.display(doSearch, expanded)
                }
            }
        }

        // More Info button for MangaDex/E-Hentai sources
        if (isSpecialSource && onViewMoreInfo != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                MoreInfoButton(
                    onClick = onViewMoreInfo
                )
            }
        }
        // SY <--
    }
}

@Composable
private fun TagChip(
    text: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(vertical = 2.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 1.dp,
    ) {
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickableNoIndication(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun MangaAndSourceTitlesLarge(
    appBarPadding: Dp,
    manga: Manga,
    sourceName: String,
    isStubSource: Boolean,
    onCoverClick: () -> Unit,
    doSearch: (query: String, global: Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = appBarPadding + 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Enhanced cover with subtle elevation shadow
        Box(
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .padding(bottom = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            MangaCover.Book(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(4.dp),
                        clip = true,
                    ),
                data = ImageRequest.Builder(LocalContext.current)
                    .data(manga)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(MR.strings.manga_cover),
                shape = RoundedCornerShape(4.dp),
                onClick = onCoverClick,
            )
        }

        // Decorative element to separate cover from info
        Box(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .height(2.dp)
                .fillMaxWidth(0.4f)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0f),
                        )
                    ),
                    shape = MaterialTheme.shapes.small,
                )
        )

        MangaContentInfo(
            title = manga.title,
            author = manga.author,
            artist = manga.artist,
            status = manga.status,
            sourceName = sourceName,
            isStubSource = isStubSource,
            doSearch = doSearch,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MangaAndSourceTitlesSmall(
    appBarPadding: Dp,
    manga: Manga,
    sourceName: String,
    isStubSource: Boolean,
    onCoverClick: () -> Unit,
    doSearch: (query: String, global: Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = appBarPadding + 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Enhanced cover with subtle elevation shadow
        Box(
            modifier = Modifier
                .sizeIn(maxWidth = 100.dp)
                .align(Alignment.Top),
            contentAlignment = Alignment.Center,
        ) {
            MangaCover.Book(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(8.dp),
                        clip = true,
                    ),
                data = ImageRequest.Builder(LocalContext.current)
                    .data(manga)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(MR.strings.manga_cover),
                shape = RoundedCornerShape(4.dp),
                onClick = onCoverClick,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MangaContentInfo(
                title = manga.title,
                author = manga.author,
                artist = manga.artist,
                status = manga.status,
                sourceName = sourceName,
                isStubSource = isStubSource,
                doSearch = doSearch,
            )
        }
    }
}

@Composable
private fun ColumnScope.MangaContentInfo(
    title: String,
    author: String?,
    artist: String?,
    status: Long,
    sourceName: String,
    isStubSource: Boolean,
    doSearch: (query: String, global: Boolean) -> Unit,
    textAlign: TextAlign? = LocalTextStyle.current.textAlign,
) {
    val context = LocalContext.current
    Text(
        text = title.ifBlank { stringResource(MR.strings.unknown_title) },
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.clickableNoIndication(
            onLongClick = {
                if (title.isNotBlank()) {
                    context.copyToClipboard(
                        title,
                        title,
                    )
                }
            },
            onClick = { if (title.isNotBlank()) doSearch(title, true) },
        ),
        textAlign = textAlign,
    )

    Spacer(modifier = Modifier.height(2.dp))

    Row(
        modifier = Modifier.secondaryItemAlpha(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.PersonOutline,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = author?.takeIf { it.isNotBlank() }
                ?: stringResource(MR.strings.unknown_author),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .clickableNoIndication(
                    onLongClick = {
                        if (!author.isNullOrBlank()) {
                            context.copyToClipboard(
                                author,
                                author,
                            )
                        }
                    },
                    onClick = { if (!author.isNullOrBlank()) doSearch(author, true) },
                ),
            textAlign = textAlign,
        )
    }

    if (!artist.isNullOrBlank() && author != artist) {
        Row(
            modifier = Modifier.secondaryItemAlpha(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Brush,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .clickableNoIndication(
                        onLongClick = { context.copyToClipboard(artist, artist) },
                        onClick = { doSearch(artist, true) },
                    ),
                textAlign = textAlign,
            )
        }
    }

    Spacer(modifier = Modifier.height(2.dp))

    Row(
        modifier = Modifier.secondaryItemAlpha(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when (status) {
                SManga.ONGOING.toLong() -> Icons.Outlined.Schedule
                SManga.COMPLETED.toLong() -> Icons.Outlined.DoneAll
                SManga.LICENSED.toLong() -> Icons.Outlined.AttachMoney
                SManga.PUBLISHING_FINISHED.toLong() -> Icons.Outlined.Done
                SManga.CANCELLED.toLong() -> Icons.Outlined.Close
                SManga.ON_HIATUS.toLong() -> Icons.Outlined.Pause
                else -> Icons.Outlined.Block
            },
            contentDescription = null,
            modifier = Modifier
                .padding(end = 4.dp)
                .size(16.dp),
        )
        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
            Text(
                text = when (status) {
                    SManga.ONGOING.toLong() -> stringResource(MR.strings.ongoing)
                    SManga.COMPLETED.toLong() -> stringResource(MR.strings.completed)
                    SManga.LICENSED.toLong() -> stringResource(MR.strings.licensed)
                    SManga.PUBLISHING_FINISHED.toLong() -> stringResource(MR.strings.publishing_finished)
                    SManga.CANCELLED.toLong() -> stringResource(MR.strings.cancelled)
                    SManga.ON_HIATUS.toLong() -> stringResource(MR.strings.on_hiatus)
                    else -> stringResource(MR.strings.unknown)
                },
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            DotSeparatorText()
            if (isStubSource) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = sourceName,
                modifier = Modifier.clickableNoIndication {
                    doSearch(
                        sourceName,
                        false,
                    )
                },
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun DotSeparatorText() {
    Text(text = " • ", style = LocalTextStyle.current)
}

@Composable
private fun MangaSummary(
    expandedDescription: String,
    shrunkDescription: String,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.Transparent,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Text content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = if (expanded) 8.dp else 24.dp)
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = 0.8f,
                                stiffness = Spring.StiffnessLow,
                            ),
                        )
                ) {
                    SelectionContainer {
                        Text(
                            text = if (expanded) expandedDescription else shrunkDescription,
                            maxLines = if (expanded) Int.MAX_VALUE else 3,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 20.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                // Add fade effect for collapsed state
                if (!expanded) {
                    val fadeColors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(
                                brush = Brush.verticalGradient(fadeColors),
                            )
                    )

                    // Add indicator on top of fade effect
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val arrowRotation by animateFloatAsState(
                            targetValue = if (expanded) 180f else 0f,
                            label = "arrow_rotation"
                        )
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (expanded)
                                stringResource(MR.strings.manga_info_collapse)
                            else
                                stringResource(MR.strings.manga_info_expand),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(24.dp)
                                .clipToBounds()
                                .padding(2.dp)
                                .rotate(arrowRotation),
                        )
                    }
                } else {
                    // Add indicator for expanded state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val arrowRotation by animateFloatAsState(
                            targetValue = if (expanded) 180f else 0f,
                            label = "arrow_rotation"
                        )
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (expanded)
                                stringResource(MR.strings.manga_info_collapse)
                            else
                                stringResource(MR.strings.manga_info_expand),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(24.dp)
                                .clipToBounds()
                                .padding(2.dp)
                                .rotate(arrowRotation),
                        )
                    }
                }
            }
        }
    }
}

private val DefaultTagChipModifier = Modifier.padding(vertical = 4.dp)

@Composable
private fun MangaActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        color = if (active) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = if (active) 4.dp else 0.dp,
        shadowElevation = if (active) 2.dp else 0.dp,
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier
                .padding(vertical = 1.dp, horizontal = 2.dp),
            onLongClick = onLongClick,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = title,
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// Extension function for SearchMetadataChips to check if it has content
@Composable
private fun SearchMetadataChips.hasContent(): Boolean {
    return tags.isNotEmpty()
}

// Extension function for SearchMetadataChips to display chips
@Composable
private fun SearchMetadataChips.display(doSearch: (String, Boolean) -> Unit, expanded: Boolean) {
    if (expanded) {
        // Enhanced expanded view with better namespace grouping
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            tags.forEach { (namespace, values) ->
                // Create a section for each namespace
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Namespace header
                    if (namespace.isNotBlank()) {
                        Text(
                            text = namespace,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }

                    // Tags for this namespace
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        values.forEach { value ->
                            EnhancedTagChip(
                                text = value.text,
                                namespace = namespace.takeIf { it.isNotBlank() },
                                border = value.border,
                                onClick = { doSearch(value.search, false) },
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Compact view that still preserves namespace grouping
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            tags.forEach { (namespace, values) ->
                if (values.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .width(180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Namespace header in compact view
                            if (namespace.isNotBlank()) {
                                Text(
                                    text = namespace,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                )
                            }

                            // Show first few tags
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                val displayedValues = values.take(5)
                                displayedValues.forEach { value ->
                                    EnhancedTagChip(
                                        text = value.text,
                                        namespace = null, // Don't show namespace in compact chip
                                        border = value.border,
                                        onClick = { doSearch(value.search, false) },
                                        compact = true,
                                    )
                                }

                                // Show count of remaining tags if needed
                                if (values.size > 5) {
                                    val remaining = values.size - 5
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+$remaining",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper composable for displaying namespace tags in a FlowRow - no longer needed with new design
@Composable
private fun NamespaceTagsInFlow(
    tags: SearchMetadataChips,
    onClick: (String) -> Unit,
) {
    tags.tags.forEach { (namespace, values) ->
        values.forEach { value ->
            val displayValue = if (namespace.isNotBlank()) "$namespace:${value.text}" else value.text
            TagChip(
                text = displayValue,
                onClick = { onClick(value.search) },
                onLongClick = {},  // No long click handler needed for now
            )
        }
    }
}

// New enhanced tag chip that can show both compact and full versions
@Composable
private fun EnhancedTagChip(
    text: String,
    namespace: String?,
    border: Int?,
    onClick: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val borderWidth = border?.dp ?: 1.dp
    val borderColor = when (border) {
        2 -> MaterialTheme.colorScheme.primary
        1 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(
            alpha = if (border != null && border > 0) 0.9f else 0.7f
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = borderWidth,
            color = borderColor
        )
    ) {
        Box(
            modifier = Modifier
                .clickableNoIndication(onClick = onClick)
                .padding(
                    horizontal = if (compact) 8.dp else 10.dp,
                    vertical = if (compact) 4.dp else 6.dp
                )
        ) {
            if (namespace != null && !compact) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append(namespace)
                            append(":")
                        }
                        append(" ")
                        append(text)
                    },
                    style = if (compact)
                        MaterialTheme.typography.labelSmall
                    else
                        MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    text = text,
                    style = if (compact)
                        MaterialTheme.typography.labelSmall
                    else
                        MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// New compact and attractive More Info button
@Composable
private fun MoreInfoButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .clickableNoIndication(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Public,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(MR.strings.more_info),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
