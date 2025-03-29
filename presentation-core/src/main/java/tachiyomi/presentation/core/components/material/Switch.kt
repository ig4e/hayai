package tachiyomi.presentation.core.components.material

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * A more minimalistic and modern implementation of Material 3 Switch.
 * This implementation aims to be more elegant with smoother animations
 * and a more refined visual appearance.
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    // Track properties
    val trackWidth = 44.dp
    val trackHeight = 24.dp
    val trackShape = RoundedCornerShape(12.dp)

    // Thumb properties
    val thumbSize = 18.dp
    val thumbPadding = 3.dp

    // Animation specs
    val animationSpec = tween<Float>(durationMillis = 250)

    // Animation values
    val thumbPosition by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = animationSpec,
        label = "ThumbPosition",
    )

    // Define colors using Material Theme
    val thumbColor by animateColorAsState(
        targetValue = when {
            checked -> Color.White
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "ThumbColor",
    )

    val trackColor by animateColorAsState(
        targetValue = when {
            checked && enabled -> MaterialTheme.colorScheme.primary
            checked && !enabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            !checked && enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        },
        label = "TrackColor",
    )

    // Make it toggleable
    val toggleModifier = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
            interactionSource = interactionSource,
            indication = null,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(toggleModifier),
        contentAlignment = Alignment.Center,
    ) {
        // Track
        Box(
            modifier = Modifier
                .size(width = trackWidth, height = trackHeight)
                .clip(trackShape)
                .background(trackColor),
        ) {
            // Thumb
            Box(
                modifier = Modifier
                    .padding(thumbPadding)
                    .size(thumbSize)
                    .offset(x = (trackWidth - thumbSize - (thumbPadding * 2)) * thumbPosition)
                    .clip(CircleShape)
                    .background(thumbColor)
                    .then(
                        if (checked) {
                            Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}
