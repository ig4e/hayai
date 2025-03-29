package tachiyomi.presentation.core.components.material

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Custom Text Field with a less Material 3 appearance
 */
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Animation duration for all animations
    val animationSpec = tween<Float>(durationMillis = 200)

    // Colors with animations
    val textColor = MaterialTheme.colorScheme.onSurface
    val borderColor by animateColorAsState(
        targetValue = if (isFocused)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        animationSpec = tween(durationMillis = 200),
        label = "BorderColor"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isFocused)
            MaterialTheme.colorScheme.surfaceContainerHighest
        else
            MaterialTheme.colorScheme.surfaceContainerLowest,
        animationSpec = tween(durationMillis = 150),
        label = "BackgroundColor"
    )

    // Border width animation
    val borderWidth by animateFloatAsState(
        targetValue = if (isFocused) 1.5f else 1f,
        animationSpec = animationSpec,
        label = "BorderWidth"
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = MaterialTheme.shapes.small,
            )
            .border(
                width = borderWidth.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.small,
            )
            .defaultMinSize(
                minWidth = 40.dp,
                minHeight = 48.dp,
            ),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = TextStyle.Default.copy(
            color = textColor,
            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
        ),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty() && placeholder != null) {
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    ) {
                        placeholder()
                    }
                }
                innerTextField()
            }
        },
    )
}

/**
 * Custom Outlined Text Field with a less Material 3 appearance
 */
@Composable
fun CustomOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Colors with animations
    val textColor = MaterialTheme.colorScheme.onSurface
    val borderColor by animateColorAsState(
        targetValue = if (isFocused)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        animationSpec = tween(durationMillis = 200),
        label = "BorderColor"
    )
    val backgroundColor = Color.Transparent

    // Animation for label
    val labelColor by animateColorAsState(
        targetValue = if (isFocused)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        animationSpec = tween(durationMillis = 200),
        label = "LabelColor"
    )

    // Border width animation
    val borderWidth by animateFloatAsState(
        targetValue = if (isFocused) 1.5f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "BorderWidth"
    )

    // Scale animation for label
    val labelScale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "LabelScale"
    )

    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        label?.let {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp, top = 4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(horizontal = 4.dp)
                    .graphicsLayer {
                        scaleX = labelScale
                        scaleY = labelScale
                    },
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides labelColor,
                ) {
                    label()
                }
            }
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (label != null) 10.dp else 0.dp)
                .background(
                    color = backgroundColor,
                    shape = MaterialTheme.shapes.small,
                )
                .border(
                    width = borderWidth.dp,
                    color = borderColor,
                    shape = MaterialTheme.shapes.small,
                )
                .defaultMinSize(
                    minWidth = 40.dp,
                    minHeight = 48.dp,
                ),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = TextStyle.Default.copy(
                color = textColor,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (leadingIcon != null) {
                        Box(
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            leadingIcon()
                        }
                    }

                    Box(
                        modifier = Modifier.padding(
                            start = if (leadingIcon != null) 28.dp else 0.dp,
                            end = if (trailingIcon != null) 28.dp else 0.dp,
                        ),
                    ) {
                        if (value.isEmpty() && placeholder != null) {
                            CompositionLocalProvider(
                                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            ) {
                                placeholder()
                            }
                        }
                        innerTextField()
                    }

                    if (trailingIcon != null) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .align(Alignment.CenterEnd),
                        ) {
                            trailingIcon()
                        }
                    }
                }
            },
        )
    }
}

/**
 * Custom Text Field with a fixed label positioned above the input field
 * instead of the default Material 3 shrinking label behavior.
 */
@Composable
fun CustomLabelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Animation duration for all animations
    val animationSpec = tween<Float>(durationMillis = 200)

    // Colors with animations
    val textColor = MaterialTheme.colorScheme.onSurface
    val borderColor by animateColorAsState(
        targetValue = if (isFocused)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        animationSpec = tween(durationMillis = 200),
        label = "BorderColor"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isFocused)
            MaterialTheme.colorScheme.surfaceContainerHighest
        else
            MaterialTheme.colorScheme.surfaceContainerLowest,
        animationSpec = tween(durationMillis = 150),
        label = "BackgroundColor"
    )

    // Label color animation
    val labelColor by animateColorAsState(
        targetValue = if (isFocused)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        animationSpec = tween(durationMillis = 200),
        label = "LabelColor"
    )

    // Border width animation
    val borderWidth by animateFloatAsState(
        targetValue = if (isFocused) 1.5f else 1f,
        animationSpec = animationSpec,
        label = "BorderWidth"
    )

    androidx.compose.foundation.layout.Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // Label above the text field
        if (label != null) {
            Box(
                modifier = Modifier
                    .padding(bottom = 4.dp, start = 4.dp)
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides labelColor,
                ) {
                    label()
                }
            }
        }

        // Text field
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = backgroundColor,
                    shape = MaterialTheme.shapes.small,
                )
                .border(
                    width = borderWidth.dp,
                    color = borderColor,
                    shape = MaterialTheme.shapes.small,
                )
                .defaultMinSize(
                    minWidth = 40.dp,
                    minHeight = 48.dp,
                ),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = TextStyle.Default.copy(
                color = textColor,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (leadingIcon != null) {
                        Box(
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            leadingIcon()
                        }
                    }

                    Box(
                        modifier = Modifier.padding(
                            start = if (leadingIcon != null) 28.dp else 0.dp,
                            end = if (trailingIcon != null) 28.dp else 0.dp,
                        ),
                    ) {
                        if (value.isEmpty() && placeholder != null) {
                            CompositionLocalProvider(
                                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            ) {
                                placeholder()
                            }
                        }
                        innerTextField()
                    }

                    if (trailingIcon != null) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .align(Alignment.CenterEnd),
                        ) {
                            trailingIcon()
                        }
                    }
                }
            },
        )
    }
}
