package hayai.novel.reader.bars

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ManageSearch
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.getResourceColor
import yokai.i18n.MR

private val actionBarSlideAnimationSpec = tween<IntOffset>(durationMillis = 200)
private val actionBarFadeAnimationSpec = tween<Float>(durationMillis = 150)

@Composable
private fun ActionBarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color,
    rippleColor: Color,
    iconSize: Dp = 24.dp,
    buttonSize: Dp = 48.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(buttonSize)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = buttonSize / 2, color = rippleColor),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NovelReaderActionBar(
    visible: Boolean,
    isTtsActive: Boolean,
    isTtsPaused: Boolean,
    isAutoScrolling: Boolean,
    onToggleTts: () -> Unit,
    onLongPressTts: () -> Unit,
    onTtsStartFromViewport: () -> Unit,
    onToggleAutoScroll: () -> Unit,
    onScrollToTop: () -> Unit,
    onClickFindReplace: () -> Unit,
    onClickQuotes: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val backgroundColor = remember(context) {
        Color(context.getResourceColor(R.attr.colorSurface)).copy(alpha = 200f / 255f)
    }
    val iconTint = remember(context) {
        Color(context.getResourceColor(R.attr.actionBarTintColor))
    }
    val rippleColor = remember(context) {
        Color(context.getResourceColor(R.attr.colorControlHighlight))
    }
    val activeTint = MaterialTheme.colorScheme.primary
    val iconSize = 24.dp
    val buttonSize = 48.dp

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = actionBarSlideAnimationSpec,
        ) + fadeIn(animationSpec = actionBarFadeAnimationSpec),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = actionBarSlideAnimationSpec,
        ) + fadeOut(animationSpec = actionBarFadeAnimationSpec),
        modifier = modifier,
    ) {
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(50.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val ttsInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(buttonSize)
                        .combinedClickable(
                            interactionSource = ttsInteraction,
                            indication = ripple(bounded = false, radius = buttonSize / 2, color = rippleColor),
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
                        contentDescription = stringResource(MR.strings.text_to_speech),
                        tint = if (isTtsActive) activeTint else iconTint,
                        modifier = Modifier.size(iconSize),
                    )
                }

                ActionBarButton(
                    icon = Icons.Outlined.Visibility,
                    contentDescription = stringResource(MR.strings.start_tts_here),
                    onClick = onTtsStartFromViewport,
                    tint = iconTint,
                    rippleColor = rippleColor,
                    iconSize = iconSize,
                    buttonSize = buttonSize,
                )

                ActionBarButton(
                    icon = if (isAutoScrolling) Icons.Outlined.Stop else Icons.Outlined.SwapVert,
                    contentDescription = stringResource(
                        if (isAutoScrolling) MR.strings.stop_auto_scroll else MR.strings.auto_scroll,
                    ),
                    onClick = onToggleAutoScroll,
                    tint = if (isAutoScrolling) activeTint else iconTint,
                    rippleColor = rippleColor,
                    iconSize = iconSize,
                    buttonSize = buttonSize,
                )

                ActionBarButton(
                    icon = Icons.Outlined.VerticalAlignTop,
                    contentDescription = stringResource(MR.strings.scroll_to_top),
                    onClick = onScrollToTop,
                    tint = iconTint,
                    rippleColor = rippleColor,
                    iconSize = iconSize,
                    buttonSize = buttonSize,
                )

                onClickQuotes?.let { quotes ->
                    ActionBarButton(
                        icon = Icons.Outlined.FormatQuote,
                        contentDescription = stringResource(MR.strings.novel_quotes),
                        onClick = quotes,
                        tint = iconTint,
                        rippleColor = rippleColor,
                        iconSize = iconSize,
                        buttonSize = buttonSize,
                    )
                }

                ActionBarButton(
                    icon = Icons.AutoMirrored.Outlined.ManageSearch,
                    contentDescription = stringResource(MR.strings.novel_find_replace),
                    onClick = onClickFindReplace,
                    tint = iconTint,
                    rippleColor = rippleColor,
                    iconSize = iconSize,
                    buttonSize = buttonSize,
                )
            }
        }
    }
}
