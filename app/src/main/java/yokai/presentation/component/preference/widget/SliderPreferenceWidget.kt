package yokai.presentation.component.preference.widget

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import yokai.util.secondaryItemAlpha

@Composable
fun SliderPreferenceWidget(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Float) -> Unit,
) {
    BasePreferenceWidget(
        modifier = modifier,
        title = title,
        icon = if (icon != null) {
            { Icon(imageVector = icon, contentDescription = null) }
        } else {
            null
        },
        subcomponent = {
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    modifier = Modifier
                        .padding(horizontal = PrefsHorizontalPadding)
                        .secondaryItemAlpha(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 10,
                )
            }
            Slider(
                value = value.toFloat(),
                onValueChange = onValueChange,
                valueRange = min.toFloat()..max.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PrefsHorizontalPadding),
            )
        },
    )
}
