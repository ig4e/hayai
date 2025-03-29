package tachiyomi.presentation.core.components.material

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * A modern and minimalistic implementation of Material 3 Checkbox.
 * This implementation provides a cleaner look with smooth animations.
 */
@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
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
    val checkScale by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = animationSpec,
        label = "CheckScale",
    )

    // Define colors directly instead of using internal APIs
    val backgroundColor by animateColorAsState(
        targetValue = when {
            checked && enabled -> MaterialTheme.colorScheme.primary
            checked && !enabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            !checked && enabled -> MaterialTheme.colorScheme.surfaceContainerHighest
            else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
        },
        label = "BackgroundColor",
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            checked -> Color.Transparent
            enabled -> MaterialTheme.colorScheme.outline
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        },
        label = "BorderColor",
    )

    val checkmarkColor by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
        },
        label = "CheckmarkColor",
    )

    // Make it toggleable
    val toggleModifier = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Checkbox,
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
        // Checkbox container
        Box(
            modifier = Modifier
                .size(size)
                .clip(checkboxShape)
                .background(backgroundColor)
                .border(borderWidth, borderColor, checkboxShape),
            contentAlignment = Alignment.Center,
        ) {
            // Checkmark
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = checkmarkColor,
                modifier = Modifier
                    .padding(3.dp)
                    .scale(checkScale)
                    .graphicsLayer {
                        alpha = checkScale
                    },
            )
        }
    }
}
