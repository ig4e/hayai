package tachiyomi.presentation.core.components.material

import androidx.annotation.IntRange
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun Slider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: IntProgression = 0..1,
    @IntRange(from = 0) steps: Int = with(valueRange) { (last - first) - 1 },
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    Slider(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.roundToInt()) },
        modifier = modifier,
        enabled = enabled,
        valueRange = with(valueRange) { first.toFloat()..last.toFloat() },
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interactionSource,
        thumb = {
            CustomThumb(
                interactionSource = interactionSource,
                enabled = enabled,
            )
        },
    )
}

@Composable
private fun CustomThumb(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
) {
    // Fixed size thumb with Material3 styling
    val size = 20.dp

    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.38f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {}
    }
}
