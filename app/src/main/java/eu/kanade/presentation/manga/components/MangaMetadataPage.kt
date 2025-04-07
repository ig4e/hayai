package eu.kanade.presentation.manga.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import eu.kanade.tachiyomi.util.system.copyToClipboard
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clickableNoIndication
import kotlin.math.roundToInt
import eu.kanade.presentation.manga.components.NamespaceTags
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.withStyle

/**
 * A detailed metadata page for manga from special sources like MangaDex and E-Hentai
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaMetadataPage(
    onBackPressed: () -> Unit,
    mangaTitle: String,
    thumbnailUrl: String?,
    // Parameters specific to the source
    isEhentaiSource: Boolean = false,
    // Common metadata parameters
    sourceName: String,
    originalLanguage: String? = null,
    alternativeTitles: List<String> = emptyList(),
    externalLinks: Map<String, String> = emptyMap(),
    status: String? = null,
    releaseDate: String? = null,
    lastUpdate: String? = null,
    // Statistics
    rating: Float? = null,
    ratingCount: Int? = null,
    viewCount: Int? = null,
    // Extra metadata
    authors: List<String> = emptyList(),
    artists: List<String> = emptyList(),
    // Source URLs
    sourceUrl: String? = null,
    // Extended Tag information (for E-Hentai)
    tagsByNamespace: Map<String, List<String>> = emptyMap(),
    // Callbacks
    onTagClick: (String) -> Unit = {},
    onCopyTagToClipboard: (String) -> Unit = {},
    // New parameters
    isSpecialSource: Boolean = false,
    onViewMoreInfo: (() -> Unit)? = null,
    // SY -->
    searchMetadataChips: SearchMetadataChips? = null,
    doSearch: (query: String, global: Boolean) -> Unit = { _, _ -> },
    extraInfo: List<Pair<String, String>> = emptyList(),
    // SY <--
) {
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEhentaiSource)
                            stringResource(MR.strings.gallery_info)
                        else
                            stringResource(MR.strings.more_info),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(MR.strings.action_close)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            // Header section with cover and title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // Backdrop with enhanced gradient blend
                thumbnailUrl?.let {
                    val backdropGradientColors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    )
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(thumbnailUrl)
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
                            .blur(12.dp)
                            .alpha(0.7f),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cover image
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(width = 100.dp, height = 140.dp)
                    ) {
                        if (thumbnailUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(thumbnailUrl)
                                    .build(),
                                contentDescription = mangaTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Photo,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            )
                        }
                    }

                    // Basic info
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = mangaTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = sourceName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            originalLanguage?.let {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape)
                                )

                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Ratings display if available
                        if (rating != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                val ratingValue = (rating * 10).roundToInt() / 10f
                                val starColor = when {
                                    ratingValue >= 4.0f -> MaterialTheme.colorScheme.primary
                                    ratingValue >= 3.0f -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.error
                                }

                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = starColor,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(
                                    text = "$ratingValue",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                ratingCount?.let {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "($it)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        // View statistics if available
                        viewCount?.let {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(
                                    text = formatNumber(it),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Status display
                        status?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Source button and links
            if (sourceUrl != null || externalLinks.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(MR.strings.links),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Source link
                        sourceUrl?.let {
                            LinkItem(
                                icon = Icons.Outlined.Public,
                                title = stringResource(MR.strings.view_on_website),
                                url = it,
                                onClick = { uriHandler.openUri(it) }
                            )
                        }

                        // External links
                        externalLinks.forEach { (title, url) ->
                            LinkItem(
                                icon = Icons.Outlined.Language,
                                title = title,
                                url = url,
                                onClick = { uriHandler.openUri(url) }
                            )
                        }
                    }
                }
            }

            // Publication information
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(MR.strings.information),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Release date
                    releaseDate?.let {
                        InfoItem(
                            icon = Icons.Outlined.CalendarMonth,
                            label = stringResource(MR.strings.release_date),
                            value = it
                        )
                    }

                    // Last update
                    lastUpdate?.let {
                        InfoItem(
                            icon = Icons.Outlined.CalendarMonth,
                            label = stringResource(MR.strings.last_updated),
                            value = it
                        )
                    }

                    // Authors
                    if (authors.isNotEmpty()) {
                        InfoItem(
                            icon = Icons.Outlined.Create,
                            label = stringResource(MR.strings.author),
                            value = authors.joinToString(", ")
                        )
                    }

                    // Artists (if different from authors)
                    if (artists.isNotEmpty() && artists != authors) {
                        InfoItem(
                            icon = Icons.Outlined.Create,
                            label = stringResource(MR.strings.artist),
                            value = artists.joinToString(", ")
                        )
                    }
                }
            }

            // Alternative titles section
            if (alternativeTitles.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(MR.strings.alternative_titles),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            TextButton(
                                onClick = { expanded = !expanded },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (expanded)
                                        stringResource(MR.strings.collapse)
                                    else
                                        stringResource(MR.strings.expand),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = expanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                alternativeTitles.forEach { title ->
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickableNoIndication(
                                                onClick = { onTagClick(title) },
                                                onLongClick = {
                                                    context.copyToClipboard(title, title)
                                                }
                                            )
                                            .padding(vertical = 4.dp)
                                    )

                                    if (title != alternativeTitles.last()) {
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }

                        if (!expanded) {
                            Text(
                                text = alternativeTitles.firstOrNull() ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (alternativeTitles.size > 1) {
                                Text(
                                    text = stringResource(MR.strings.more_titles, alternativeTitles.size - 1),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }

            // Extra Info Section
            if (extraInfo.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(MR.strings.details), // Or a more specific title if preferred
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        extraInfo.forEach { (key, value) ->
                            // Reusing InfoItem, assuming generic icon is acceptable
                            // If specific icons are needed per key, more logic would be required.
                            InfoItem(
                                // TODO: Maybe use a generic icon like Icons.Outlined.Info?
                                icon = Icons.Outlined.Info, // Placeholder icon
                                label = key,
                                value = value
                            )
                            // Add divider between items for clarity
                            if (extraInfo.last() != Pair(key, value)) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }

            // Tags section - using a more modern and organized layout
            if (tagsByNamespace.isNotEmpty()) {
                @OptIn(ExperimentalLayoutApi::class)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(MR.strings.tags),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        tagsByNamespace.forEach { (namespace, tags) ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Namespace header
                                Text(
                                    text = namespace,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                // Tags for this namespace
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    tags.forEach { tag ->
                                        TagChip(
                                            text = tag,
                                            onClick = { onTagClick(tag) },
                                            onLongClick = { onCopyTagToClipboard(tag) }
                                        )
                                    }
                                }

                                if (namespace != tagsByNamespace.keys.last()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }
            }

            // SY --> Additional metadata chips
            searchMetadataChips?.let { chips ->
                // Only show if chips has content
                if (chips.hasContent()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(MR.strings.tags),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Display search metadata chips
                            chips.display(doSearch, true)
                        }
                    }
                }
            }
            // SY <--

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

            // Extra space at the bottom
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun LinkItem(
    icon: ImageVector,
    title: String,
    url: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TagChip(
    text: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier
                .clickableNoIndication(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Helper function to format large numbers (like view count)
private fun formatNumber(number: Int): String {
    return when {
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000f)
        number >= 1_000 -> String.format("%.1fK", number / 1_000f)
        else -> number.toString()
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

// Extension function for SearchMetadataChips to check if it has content
@Composable
private fun SearchMetadataChips.hasContent(): Boolean {
    return tags.isNotEmpty()
}

// Extension function for SearchMetadataChips to display chips
@Composable
private fun SearchMetadataChips.display(doSearch: (query: String, global: Boolean) -> Unit, expanded: Boolean) {
    if (expanded) {
        // Enhanced expanded view with better namespace grouping
        Column(
            modifier = Modifier.fillMaxWidth(),
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
            contentPadding = PaddingValues(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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

