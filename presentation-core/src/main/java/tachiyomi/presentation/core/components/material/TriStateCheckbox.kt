package tachiyomi.presentation.core.components.material

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp

/**
 * A modern implementation of the Material 3 TriStateCheckbox.
 * This component provides three states: checked, unchecked, and indeterminate.
 */
@Composable
fun TriStateCheckbox(
    state: ToggleableState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    // Checkbox properties
    val size = 22.dp
    val checkboxShape = RoundedCornerShape(8.dp)
    val borderWidth = 1.5.dp

    // Animation specs
    val animationSpec = tween<Float>(durationMillis = 200)

    // Animation values
    val iconScale by animateFloatAsState(
        targetValue = if (state != ToggleableState.Off) 1f else 0f,
        animationSpec = animationSpec,
        label = "IconScale",
    )

    val backgroundColor by animateColorAsState(
        targetValue = when (state) {
            ToggleableState.On -> if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            ToggleableState.Indeterminate -> if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            ToggleableState.Off -> if (enabled) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
        },
        label = "BackgroundColor",
    )

    val borderColor by animateColorAsState(
        targetValue = when (state) {
            ToggleableState.On, ToggleableState.Indeterminate -> Color.Transparent
            ToggleableState.Off -> if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        },
        label = "BorderColor",
    )

    val iconColor by animateColorAsState(
        targetValue = when (state) {
            ToggleableState.On, ToggleableState.Indeterminate -> if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
            ToggleableState.Off -> if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        },
        label = "IconColor",
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(checkboxShape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, checkboxShape),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            ToggleableState.On -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                            alpha = iconScale
                        },
                )
            }
            ToggleableState.Indeterminate -> {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                            alpha = iconScale
                        },
                )
            }
            else -> {
                // No icon for Off state
            }
        }
    }
}
