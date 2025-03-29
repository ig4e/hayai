package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.TachiyomiPreviewTheme

@Composable
fun ModernPreferenceItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: CharSequence? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null,
) {
    BasePreferenceWidget(
        modifier = modifier.clickable(enabled = onClick != null) { onClick?.invoke() },
        title = title,
        subcomponent = {
            if (subtitle != null) {
                androidx.compose.material3.Text(
                    text = subtitle.toString(),
                    modifier = Modifier.padding(horizontal = PrefsHorizontalPadding),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (content != null) {
                Column(
                    modifier = Modifier.padding(
                        top = if (subtitle != null) 8.dp else 0.dp,
                        start = PrefsHorizontalPadding,
                        end = PrefsHorizontalPadding,
                    ),
                ) {
                    content()
                }
            }
        },
        icon = if (icon != null) {
            {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else null,
    )
}

@PreviewLightDark
@Composable
private fun ModernPreferenceItemPreview() {
    TachiyomiPreviewTheme {
        Surface {
            Column {
                ModernPreferenceItem(
                    title = "Preference with icon",
                    subtitle = "This is a subtitle",
                    icon = Icons.Filled.Settings,
                    onClick = {},
                )
                ModernPreferenceItem(
                    title = "Preference without icon",
                    subtitle = "This is a subtitle",
                    onClick = {},
                )
                ModernPreferenceItem(
                    title = "Preference with content",
                    icon = Icons.Filled.Settings,
                    onClick = {},
                ) {
                    androidx.compose.material3.Text("Custom content here")
                }
            }
        }
    }
}
