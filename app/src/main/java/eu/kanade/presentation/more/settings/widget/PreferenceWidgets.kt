package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.material.Checkbox
import tachiyomi.presentation.core.components.material.Switch
import tachiyomi.presentation.core.components.material.padding

@Composable
fun TextPreferenceWidget(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: CharSequence? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    widget: @Composable (() -> Unit)? = null,
    onPreferenceClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    PreferenceWidget(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        icon = icon,
        iconTint = iconTint,
        enabled = enabled,
        onClick = onPreferenceClick,
        widget = widget,
    )
}

@Composable
fun SwitchPreferenceWidget(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: CharSequence? = null,
    icon: ImageVector? = null,
    checked: Boolean = false,
    enabled: Boolean = true,
    onCheckedChanged: (Boolean) -> Unit,
) {
    PreferenceWidget(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        icon = icon,
        enabled = enabled,
        onClick = { onCheckedChanged(!checked) },
        widget = {
            tachiyomi.presentation.core.components.material.Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = { onCheckedChanged(it) },
            )
        },
    )
}

@Composable
fun PreferenceWidget(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: CharSequence? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    widget: @Composable (() -> Unit)? = null,
) {
    val alpha = if (enabled) 1f else 0.38f
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(enabled = enabled, onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(clickableModifier)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(24.dp)
                        .alpha(alpha),
                    tint = iconTint,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = if (widget != null) 16.dp else 0.dp)
                    .alpha(alpha),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                )

                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 20,
                    )
                }
            }

            widget?.invoke()
        }
    }
}

@Composable
fun PreferenceGroupHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun CheckboxPreferenceWidget(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: CharSequence? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChanged: (Boolean) -> Unit,
) {
    PreferenceWidget(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        icon = icon,
        enabled = enabled,
        onClick = { onCheckedChanged(!checked) },
        widget = {
            tachiyomi.presentation.core.components.material.Checkbox(
                checked = checked,
                onCheckedChange = { onCheckedChanged(it) },
                enabled = enabled,
            )
        }
    )
}

@Composable
fun SliderPreferenceWidget(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: CharSequence? = null,
    icon: ImageVector? = null,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChanged: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        PreferenceWidget(
            title = title,
            subtitle = subtitle,
            icon = icon,
            enabled = enabled,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = value,
            onValueChange = onValueChanged,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
