package eu.kanade.presentation.library.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.presentation.category.visualName
import eu.kanade.tachiyomi.ui.library.LibraryItem
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.MangaCover

@Composable
fun LibrarySections(
    categories: List<Category>,
    selection: Set<Long>,
    contentPadding: PaddingValues,
    hasActiveFilters: Boolean,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
    useStaggeredGrid: Boolean,
    onClickManga: (Category, LibraryManga) -> Unit,
    onLongClickManga: (Category, LibraryManga) -> Unit,
    onClickContinueReading: ((LibraryManga) -> Unit)?,
    onToggleCategorySection: (Category) -> Unit,
    isCategoryCollapsed: (Category) -> Boolean,
    getItemCountForCategory: (Category) -> Int?,
    getDisplayMode: (Int) -> PreferenceMutableState<LibraryDisplayMode>,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
    getItemsForCategory: (Category) -> List<LibraryItem>,
    outlineOnCovers: Boolean = false,
) {
    val layoutDirection = LocalLayoutDirection.current
    val displayMode by getDisplayMode(0)
    val columns = rememberSectionColumns(displayMode, getColumnsForOrientation)
    if (displayMode != LibraryDisplayMode.List && useStaggeredGrid) {
        LazyLibraryStaggeredGrid(
            columns = columns,
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(layoutDirection),
                top = contentPadding.calculateTopPadding(),
                end = contentPadding.calculateEndPadding(layoutDirection),
                bottom = 16.dp,
            ),
        ) {
            globalSearchItem(searchQuery, onGlobalSearchClicked)

            if (categories.isEmpty()) {
                item(
                    span = StaggeredGridItemSpan.FullLine,
                    contentType = "empty_state",
                ) {
                    LibraryPagerEmptyScreen(
                        searchQuery = searchQuery,
                        hasActiveFilters = hasActiveFilters,
                        contentPadding = PaddingValues(8.dp),
                        onGlobalSearchClicked = onGlobalSearchClicked,
                    )
                }
            } else {
                categories.forEach { category ->
                    val collapsed = isCategoryCollapsed(category)
                    item(
                        key = "section_header_${category.id}",
                        span = StaggeredGridItemSpan.FullLine,
                        contentType = "section_header",
                    ) {
                        LibrarySectionHeader(
                            title = category.visualName,
                            count = getItemCountForCategory(category),
                            collapsed = collapsed,
                            onClick = { onToggleCategorySection(category) },
                        )
                    }

                    if (!collapsed) {
                        val items = getItemsForCategory(category)
                        items(
                            count = items.size,
                            key = { index -> "section_${category.id}_${items[index].id}" },
                            contentType = {
                                when (displayMode) {
                                    LibraryDisplayMode.ComfortableGrid -> "library_section_comfortable_staggered_item"
                                    LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> "library_section_compact_staggered_item"
                                    LibraryDisplayMode.List -> "library_section_list_item"
                                }
                            },
                        ) { index ->
                            val libraryItem = items[index]
                            when (displayMode) {
                                LibraryDisplayMode.ComfortableGrid -> {
                                    LibrarySectionComfortableGridItem(
                                        libraryItem = libraryItem,
                                        isSelected = libraryItem.libraryManga.manga.id in selection,
                                        onClick = { onClickManga(category, libraryItem.libraryManga) },
                                        onLongClick = { onLongClickManga(category, libraryItem.libraryManga) },
                                        onClickContinueReading = if (onClickContinueReading != null && libraryItem.unreadCount > 0) {
                                            { onClickContinueReading(libraryItem.libraryManga) }
                                        } else {
                                            null
                                        },
                                        outlineOnCovers = outlineOnCovers,
                                    )
                                }

                                LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> {
                                    LibrarySectionCompactGridItem(
                                        libraryItem = libraryItem,
                                        showTitle = displayMode == LibraryDisplayMode.CompactGrid,
                                        isSelected = libraryItem.libraryManga.manga.id in selection,
                                        onClick = { onClickManga(category, libraryItem.libraryManga) },
                                        onLongClick = { onLongClickManga(category, libraryItem.libraryManga) },
                                        onClickContinueReading = if (onClickContinueReading != null && libraryItem.unreadCount > 0) {
                                            { onClickContinueReading(libraryItem.libraryManga) }
                                        } else {
                                            null
                                        },
                                        outlineOnCovers = outlineOnCovers,
                                    )
                                }

                                LibraryDisplayMode.List -> Unit
                            }
                        }
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(layoutDirection),
            top = contentPadding.calculateTopPadding(),
            end = contentPadding.calculateEndPadding(layoutDirection),
            bottom = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!searchQuery.isNullOrEmpty()) {
            item("global_search") {
                GlobalSearchItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    searchQuery = searchQuery,
                    onClick = onGlobalSearchClicked,
                )
            }
        }

        if (categories.isEmpty()) {
            item("empty_state") {
                LibraryPagerEmptyScreen(
                    searchQuery = searchQuery,
                    hasActiveFilters = hasActiveFilters,
                    contentPadding = PaddingValues(8.dp),
                    onGlobalSearchClicked = onGlobalSearchClicked,
                )
            }
        } else {
            categories.forEach { category ->
                val collapsed = isCategoryCollapsed(category)
                item("section_header_${category.id}") {
                    LibrarySectionHeader(
                        title = category.visualName,
                        count = getItemCountForCategory(category),
                        collapsed = collapsed,
                        onClick = { onToggleCategorySection(category) },
                    )
                }

                if (!collapsed) {
                    val items = getItemsForCategory(category)

                    when (displayMode) {
                        LibraryDisplayMode.List -> {
                            items(
                                items = items,
                                key = { "list_${category.id}_${it.id}" },
                                contentType = { "library_section_list_item" },
                            ) { libraryItem ->
                                LibrarySectionListItem(
                                    libraryItem = libraryItem,
                                    isSelected = libraryItem.libraryManga.manga.id in selection,
                                    onClick = { onClickManga(category, libraryItem.libraryManga) },
                                    onLongClick = { onLongClickManga(category, libraryItem.libraryManga) },
                                    onClickContinueReading = if (onClickContinueReading != null && libraryItem.unreadCount > 0) {
                                        { onClickContinueReading(libraryItem.libraryManga) }
                                    } else {
                                        null
                                    },
                                    outlineOnCovers = outlineOnCovers,
                                )
                            }
                        }
                        LibraryDisplayMode.ComfortableGrid -> {
                            items(
                                items = items.chunked(columns),
                                key = { row -> "comfortable_${category.id}_${row.firstOrNull()?.id ?: 0L}" },
                                contentType = { "library_section_grid_row" },
                            ) { row ->
                                LibrarySectionGridRow(
                                    rowItems = row,
                                    columns = columns,
                                    horizontalPadding = 12.dp,
                                ) { libraryItem ->
                                    LibrarySectionComfortableGridItem(
                                        libraryItem = libraryItem,
                                        isSelected = libraryItem.libraryManga.manga.id in selection,
                                        onClick = { onClickManga(category, libraryItem.libraryManga) },
                                        onLongClick = { onLongClickManga(category, libraryItem.libraryManga) },
                                        onClickContinueReading = if (onClickContinueReading != null && libraryItem.unreadCount > 0) {
                                            { onClickContinueReading(libraryItem.libraryManga) }
                                        } else {
                                            null
                                        },
                                        outlineOnCovers = outlineOnCovers,
                                    )
                                }
                            }
                        }
                        LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> {
                            items(
                                items = items.chunked(columns),
                                key = { row -> "compact_${category.id}_${row.firstOrNull()?.id ?: 0L}" },
                                contentType = { "library_section_grid_row" },
                            ) { row ->
                                LibrarySectionGridRow(
                                    rowItems = row,
                                    columns = columns,
                                    horizontalPadding = 12.dp,
                                ) { libraryItem ->
                                    LibrarySectionCompactGridItem(
                                        libraryItem = libraryItem,
                                        showTitle = displayMode == LibraryDisplayMode.CompactGrid,
                                        isSelected = libraryItem.libraryManga.manga.id in selection,
                                        onClick = { onClickManga(category, libraryItem.libraryManga) },
                                        onLongClick = { onLongClickManga(category, libraryItem.libraryManga) },
                                        onClickContinueReading = if (onClickContinueReading != null && libraryItem.unreadCount > 0) {
                                            { onClickContinueReading(libraryItem.libraryManga) }
                                        } else {
                                            null
                                        },
                                        outlineOnCovers = outlineOnCovers,
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

@Composable
private fun rememberSectionColumns(
    displayMode: LibraryDisplayMode,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
): Int {
    if (displayMode == LibraryDisplayMode.List) return 1

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val configuredColumns by remember(isLandscape) { getColumnsForOrientation(isLandscape) }
    if (configuredColumns > 0) return configuredColumns

    val widthDp = configuration.screenWidthDp
    return when (displayMode) {
        LibraryDisplayMode.ComfortableGrid -> when {
            widthDp >= 900 -> 5
            widthDp >= 600 -> 4
            else -> 3
        }
        LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> when {
            widthDp >= 900 -> 6
            widthDp >= 600 -> 5
            else -> 3
        }
        LibraryDisplayMode.List -> 1
    }
}

@Composable
private fun LibrarySectionHeader(
    title: String,
    count: Int?,
    collapsed: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.large,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (count != null) {
                        Text(
                            text = " $count",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = if (collapsed) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun LibrarySectionListItem(
    libraryItem: LibraryItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickContinueReading: (() -> Unit)?,
    outlineOnCovers: Boolean = false,
) {
    val manga = libraryItem.libraryManga.manga
    MangaListItem(
        isSelected = isSelected,
        title = manga.title,
        coverData = MangaCover(
            mangaId = manga.id,
            sourceId = manga.source,
            isMangaFavorite = manga.favorite,
            ogUrl = manga.thumbnailUrl,
            lastModified = manga.coverLastModified,
        ),
        badge = {
            DownloadsBadge(count = libraryItem.downloadCount)
            UnreadBadge(count = libraryItem.unreadCount)
            LanguageBadge(
                isLocal = libraryItem.isLocal,
                sourceLanguage = libraryItem.sourceLanguage,
            )
        },
        onLongClick = onLongClick,
        onClick = onClick,
        onClickContinueReading = onClickContinueReading,
        outlineOnCovers = outlineOnCovers,
    )
}

@Composable
private fun LibrarySectionGridRow(
    rowItems: List<LibraryItem>,
    columns: Int,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    content: @Composable (LibraryItem) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridHorizontalSpacer),
        verticalAlignment = Alignment.Top,
    ) {
        rowItems.forEach { libraryItem ->
            Box(modifier = Modifier.weight(1f)) {
                content(libraryItem)
            }
        }
        repeat(columns - rowItems.size) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun LibrarySectionCompactGridItem(
    libraryItem: LibraryItem,
    showTitle: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickContinueReading: (() -> Unit)?,
    outlineOnCovers: Boolean = false,
) {
    val manga = libraryItem.libraryManga.manga
    MangaCompactGridItem(
        isSelected = isSelected,
        title = manga.title.takeIf { showTitle },
        coverData = MangaCover(
            mangaId = manga.id,
            sourceId = manga.source,
            isMangaFavorite = manga.favorite,
            ogUrl = manga.thumbnailUrl,
            lastModified = manga.coverLastModified,
        ),
        coverBadgeStart = {
            DownloadsBadge(count = libraryItem.downloadCount)
        },
        coverBadgeEnd = {
            UnreadBadge(count = libraryItem.unreadCount)
        },
        coverBadgeBottom = {
            LanguageBadge(
                isLocal = libraryItem.isLocal,
                sourceLanguage = libraryItem.sourceLanguage,
            )
        },
        onLongClick = onLongClick,
        onClick = onClick,
        onClickContinueReading = onClickContinueReading,
        outlineOnCovers = outlineOnCovers,
    )
}

@Composable
private fun LibrarySectionComfortableGridItem(
    libraryItem: LibraryItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickContinueReading: (() -> Unit)?,
    outlineOnCovers: Boolean = false,
) {
    val manga = libraryItem.libraryManga.manga
    MangaComfortableGridItem(
        isSelected = isSelected,
        title = manga.title,
        coverData = MangaCover(
            mangaId = manga.id,
            sourceId = manga.source,
            isMangaFavorite = manga.favorite,
            ogUrl = manga.thumbnailUrl,
            lastModified = manga.coverLastModified,
        ),
        coverBadgeStart = {
            DownloadsBadge(count = libraryItem.downloadCount)
        },
        coverBadgeEnd = {
            UnreadBadge(count = libraryItem.unreadCount)
        },
        coverBadgeBottom = {
            LanguageBadge(
                isLocal = libraryItem.isLocal,
                sourceLanguage = libraryItem.sourceLanguage,
            )
        },
        onLongClick = onLongClick,
        onClick = onClick,
        onClickContinueReading = onClickContinueReading,
        outlineOnCovers = outlineOnCovers,
    )
}
