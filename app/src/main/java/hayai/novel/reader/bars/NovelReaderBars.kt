package hayai.novel.reader.bars

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.NavigateBefore
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.presentation.reader.appbars.BottomBarItem
import eu.kanade.presentation.reader.appbars.BottomBarItemState
import yokai.i18n.MR
import yokai.presentation.component.AppBar
import yokai.presentation.component.AppBarActions

/**
 * Compose overlay for the novel reader. Adapts Tsundoku's `NovelReaderAppBars` design to Hayai's
 * existing `yokai.presentation.component.AppBar` action system + Material 3 Expressive components.
 *
 * Top bar: title + subtitle + bookmark + overflow (reload/share/open in browser).
 * Bottom bar: progress slider + horizontal-scrolling icon row driven by [BottomBarItemState].
 *
 * Mounted as a `ComposeView` in `ReaderActivity` and gated on novel-mode; the existing XML
 * overlay (`binding.appBar`, `binding.readerNav`, `binding.chaptersSheet.root`) is hidden.
 */

private val readerBarsSlideAnimationSpec = tween<IntOffset>(durationMillis = 200)
private val readerBarsFadeAnimationSpec = tween<Float>(durationMillis = 150)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NovelReaderBars(
    visible: Boolean,
    // Top bar
    novelTitle: String?,
    chapterTitle: String?,
    bookmarked: Boolean,
    onNavigateUp: () -> Unit,
    onToggleBookmark: () -> Unit,
    onReload: () -> Unit,
    onShare: (() -> Unit)?,
    onOpenInBrowser: (() -> Unit)?,
    // Progress slider
    showProgressSlider: Boolean,
    currentProgress: Int, // 0..100
    onProgressChange: (Int) -> Unit,
    // Chapter navigation
    onPreviousChapter: () -> Unit,
    enabledPrevious: Boolean,
    onNextChapter: () -> Unit,
    enabledNext: Boolean,
    // Bottom bar actions
    onClickSettings: () -> Unit,
    onScrollToTop: () -> Unit,
    isAutoScrolling: Boolean,
    onToggleAutoScroll: () -> Unit,
    isTtsActive: Boolean,
    isTtsPaused: Boolean,
    onToggleTts: () -> Unit,
    onLongPressTts: () -> Unit,
    onTtsStartFromViewport: () -> Unit,
    // Customization
    bottomBarItems: List<BottomBarItemState>,
    isWebView: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)

    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = readerBarsSlideAnimationSpec,
            ) + fadeIn(animationSpec = readerBarsFadeAnimationSpec),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = readerBarsSlideAnimationSpec,
            ) + fadeOut(animationSpec = readerBarsFadeAnimationSpec),
        ) {
            NovelTopAppBar(
                novelTitle = novelTitle,
                chapterTitle = chapterTitle,
                bookmarked = bookmarked,
                onNavigateUp = onNavigateUp,
                onToggleBookmark = onToggleBookmark,
                onReload = onReload,
                onShare = onShare,
                onOpenInBrowser = onOpenInBrowser,
                backgroundColor = backgroundColor,
            )
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f, fill = true))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = readerBarsSlideAnimationSpec,
            ) + fadeIn(animationSpec = readerBarsFadeAnimationSpec),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = readerBarsSlideAnimationSpec,
            ) + fadeOut(animationSpec = readerBarsFadeAnimationSpec),
        ) {
            Surface(
                color = backgroundColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                Column {
                    if (showProgressSlider) {
                        NovelProgressSlider(
                            currentProgress = currentProgress,
                            onProgressChange = onProgressChange,
                            backgroundColor = backgroundColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    NovelBottomBar(
                        items = bottomBarItems,
                        isWebView = isWebView,
                        onPreviousChapter = onPreviousChapter,
                        enabledPrevious = enabledPrevious,
                        onNextChapter = onNextChapter,
                        enabledNext = enabledNext,
                        onClickSettings = onClickSettings,
                        onScrollToTop = onScrollToTop,
                        isAutoScrolling = isAutoScrolling,
                        onToggleAutoScroll = onToggleAutoScroll,
                        isTtsActive = isTtsActive,
                        isTtsPaused = isTtsPaused,
                        onToggleTts = onToggleTts,
                        onLongPressTts = onLongPressTts,
                        onTtsStartFromViewport = onTtsStartFromViewport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NovelTopAppBar(
    novelTitle: String?,
    chapterTitle: String?,
    bookmarked: Boolean,
    onNavigateUp: () -> Unit,
    onToggleBookmark: () -> Unit,
    onReload: () -> Unit,
    onShare: (() -> Unit)?,
    onOpenInBrowser: (() -> Unit)?,
    backgroundColor: Color,
) {
    TopAppBar(
        title = {
            Column {
                novelTitle?.let {
                    Text(
                        text = it,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                chapterTitle?.let {
                    Text(
                        text = it,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(MR.strings.action_bar_up_description),
                )
            }
        },
        actions = {
            AppBarActions(
                actions = kotlinx.collections.immutable.persistentListOf<AppBar.AppBarAction>().builder()
                    .apply {
                        add(
                            AppBar.Action(
                                title = stringResource(
                                    if (bookmarked) MR.strings.bookmarked else MR.strings.not_bookmarked,
                                ),
                                icon = if (bookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                                onClick = onToggleBookmark,
                            ),
                        )
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.refresh),
                                onClick = onReload,
                            ),
                        )
                        onShare?.let {
                            add(
                                AppBar.OverflowAction(
                                    title = stringResource(MR.strings.share),
                                    onClick = it,
                                ),
                            )
                        }
                        onOpenInBrowser?.let {
                            add(
                                AppBar.OverflowAction(
                                    title = stringResource(MR.strings.open_in_browser),
                                    onClick = it,
                                ),
                            )
                        }
                    }
                    .build(),
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor,
        ),
        windowInsets = WindowInsets.statusBars,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NovelBottomBar(
    items: List<BottomBarItemState>,
    isWebView: Boolean,
    onPreviousChapter: () -> Unit,
    enabledPrevious: Boolean,
    onNextChapter: () -> Unit,
    enabledNext: Boolean,
    onClickSettings: () -> Unit,
    onScrollToTop: () -> Unit,
    isAutoScrolling: Boolean,
    onToggleAutoScroll: () -> Unit,
    isTtsActive: Boolean,
    isTtsPaused: Boolean,
    onToggleTts: () -> Unit,
    onLongPressTts: () -> Unit,
    onTtsStartFromViewport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // EDIT only renders in WebView mode (it relies on contentEditable). Translation is still
    // out of scope for the initial port.
    val enabledItems = remember(items, isWebView) {
        items.filter { state ->
            state.enabled && when (state.item) {
                BottomBarItem.EDIT -> isWebView
                BottomBarItem.TRANSLATE -> false
                else -> true
            }
        }
    }

    val iconSize = 24.dp
    val buttonSize = 48.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        enabledItems.forEach { state ->
            when (state.item) {
                BottomBarItem.PREV_CHAPTER -> IconButton(
                    onClick = onPreviousChapter,
                    enabled = enabledPrevious,
                    modifier = Modifier.size(buttonSize),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.NavigateBefore,
                        contentDescription = stringResource(MR.strings.previous_chapter),
                        modifier = Modifier.size(iconSize),
                    )
                }
                BottomBarItem.NEXT_CHAPTER -> IconButton(
                    onClick = onNextChapter,
                    enabled = enabledNext,
                    modifier = Modifier.size(buttonSize),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.NavigateNext,
                        contentDescription = stringResource(MR.strings.next_chapter),
                        modifier = Modifier.size(iconSize),
                    )
                }
                BottomBarItem.SCROLL_TO_TOP -> IconButton(
                    onClick = onScrollToTop,
                    modifier = Modifier.size(buttonSize),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VerticalAlignTop,
                        contentDescription = "Scroll to top",
                        modifier = Modifier.size(iconSize),
                    )
                }
                BottomBarItem.AUTO_SCROLL -> IconButton(
                    onClick = onToggleAutoScroll,
                    modifier = Modifier.size(buttonSize),
                    colors = if (isAutoScrolling) {
                        IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        IconButtonDefaults.iconButtonColors()
                    },
                ) {
                    Icon(
                        imageVector = if (isAutoScrolling) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        contentDescription = if (isAutoScrolling) "Stop auto-scroll" else "Start auto-scroll",
                        modifier = Modifier.size(iconSize),
                    )
                }
                BottomBarItem.TTS -> Box(
                    modifier = Modifier
                        .size(buttonSize)
                        .combinedClickable(
                            onClick = onToggleTts,
                            onLongClick = onLongPressTts,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when {
                            isTtsActive && !isTtsPaused -> Icons.Outlined.Pause
                            isTtsActive && isTtsPaused -> Icons.Outlined.PlayArrow
                            else -> Icons.Outlined.RecordVoiceOver
                        },
                        contentDescription = "Text-to-speech",
                        tint = if (isTtsActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            androidx.compose.material3.LocalContentColor.current
                        },
                        modifier = Modifier.size(iconSize),
                    )
                }
                BottomBarItem.TTS_VIEWPORT -> IconButton(
                    onClick = onTtsStartFromViewport,
                    modifier = Modifier.size(buttonSize),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = "Start TTS here",
                        modifier = Modifier.size(iconSize),
                    )
                }
                BottomBarItem.SETTINGS -> IconButton(
                    onClick = onClickSettings,
                    modifier = Modifier.size(buttonSize),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(MR.strings.settings),
                        modifier = Modifier.size(iconSize),
                    )
                }
                // Items intentionally not rendered in v1 — out of scope.
                BottomBarItem.TRANSLATE,
                BottomBarItem.QUOTES,
                BottomBarItem.EDIT,
                BottomBarItem.ORIENTATION -> Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NovelProgressSlider(
    currentProgress: Int,
    onProgressChange: (Int) -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val sliderDragged by interactionSource.collectIsDraggedAsState()

    LaunchedEffect(currentProgress) {
        if (sliderDragged) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Reserve space for "100%" so the slider doesn't shift as the value changes.
        Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.width(48.dp)) {
            Text(text = "$currentProgress%", style = MaterialTheme.typography.labelMedium)
        }

        Slider(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            value = currentProgress.toFloat(),
            valueRange = 0f..100f,
            steps = 99,
            onValueChange = { newValue ->
                val rounded = newValue.toInt()
                if (rounded != currentProgress) onProgressChange(rounded)
            },
            interactionSource = interactionSource,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )

        Text(
            text = "100%",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(40.dp),
        )
    }
}
