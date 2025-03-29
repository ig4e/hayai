package tachiyomi.presentation.core.components.material

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.launch

/**
 * Custom dropdown menu component with a less Material 3 appearance
 */
@Composable
fun <T> CustomDropdownMenu(
    items: List<T>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    itemToString: (T) -> String = { it.toString() },
) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest

    // Animation specs
    val rotationAnimSpec = tween<Float>(durationMillis = 300)
    val scaleAnimSpec = tween<Float>(
        durationMillis = 200,
        easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    )

    // Container scale animation
    val containerScale by animateFloatAsState(
        targetValue = if (expanded) 1.02f else 1f,
        animationSpec = scaleAnimSpec,
        label = "ContainerScale"
    )

    // Store the width and position of the input field for correct menu placement
    var inputFieldWidth by remember { mutableStateOf(0) }
    var inputFieldHeight by remember { mutableStateOf(0) }
    var inputFieldPosition by remember { mutableStateOf(IntOffset(0, 0)) }
    val density = LocalDensity.current

    Column(modifier = modifier) {
        // Label with enhanced styling
        if (label != null) {
            Text(
                text = label,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .graphicsLayer {
                        alpha = if (expanded) 1f else 0.9f
                    },
                color = if (expanded)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // Dropdown selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = containerScale
                    scaleY = containerScale
                    // Apply a subtle shadow when expanded
                    shadowElevation = if (expanded) 2f else 0f
                }
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .border(
                    width = if (expanded) 1.5.dp else 1.dp,
                    color = if (expanded) MaterialTheme.colorScheme.primary else borderColor,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(
                    enabled = enabled,
                    onClick = { expanded = true },
                    interactionSource = interactionSource,
                    indication = null,
                )
                .defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = 16.dp)
                .onGloballyPositioned { coordinates ->
                    // Store the dimensions and position
                    inputFieldWidth = coordinates.size.width
                    inputFieldHeight = coordinates.size.height
                    inputFieldPosition = IntOffset(
                        x = coordinates.positionInRoot().x.toInt(),
                        y = coordinates.positionInRoot().y.toInt() + coordinates.size.height
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (selectedIndex >= 0 && selectedIndex < items.size) {
                    itemToString(items[selectedIndex])
                } else {
                    ""
                },
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(modifier = Modifier.width(8.dp))

            val rotation by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f,
                animationSpec = rotationAnimSpec,
                label = "dropdown_arrow_rotation",
            )

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation),
                tint = if (expanded)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Dropdown menu with improved animations
        if (expanded) {
            DropdownMenuContent(
                items = items,
                selectedIndex = selectedIndex,
                onItemSelected = { index ->
                    onItemSelected(index)
                    expanded = false
                },
                onDismissRequest = { expanded = false },
                inputFieldWidth = inputFieldWidth,
                inputFieldPosition = inputFieldPosition,
                borderColor = borderColor
            )
        }
    }
}

@Composable
private fun <T> DropdownMenuContent(
    items: List<T>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    inputFieldWidth: Int,
    inputFieldPosition: IntOffset,
    borderColor: Color,
    itemToString: (T) -> String = { it.toString() },
) {
    val density = LocalDensity.current

    // Animation values
    val animatedAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    val animatedScale = remember { androidx.compose.animation.core.Animatable(0.92f) }

    LaunchedEffect(key1 = true) {
        launch {
            animatedAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(180, easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f))
            )
        }
        launch {
            animatedScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(220, easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f))
            )
        }
    }

    val popupPositionProvider = remember(inputFieldPosition) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                return IntOffset(
                    x = inputFieldPosition.x,
                    y = inputFieldPosition.y + 4
                )
            }
        }
    }

    Popup(
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
        popupPositionProvider = popupPositionProvider,
    ) {
        Box(
            modifier = Modifier
                .width(with(density) { inputFieldWidth.toDp() })
                .graphicsLayer {
                    alpha = animatedAlpha.value
                    scaleY = animatedScale.value
                    transformOrigin = TransformOrigin(0.5f, 0f) // Scale from top center
                    shadowElevation = 16f
                }
        ) {
            Column(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(vertical = 8.dp),
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    CustomDropdownMenuItem(
                        text = itemToString(item),
                        selected = isSelected,
                        onClick = { onItemSelected(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomDropdownMenuItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "MenuItemBackground"
    )

    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }

    val textColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(containerColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun CustomDropdownDivider(
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
    )
}
