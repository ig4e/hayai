package yokai.presentation.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import yokai.presentation.theme.Size
import yokai.util.secondaryItemAlpha

@Composable
fun InfoScreen(
    icon: ImageVector,
    headingText: String,
    subtitleText: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    acceptText: String,
    onAcceptClick: () -> Unit,
    canAccept: Boolean,
    rejectText: String? = null,
    onRejectClick: (() -> Unit)? = null,
    tertiaryText: String? = null,
    onTertiaryClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    InfoScreen(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(bottom = Size.small)
                    .size(Size.huge),
                tint = tint,
            )
        },
        headingText = headingText,
        subtitleText = subtitleText,
        acceptText = acceptText,
        onAcceptClick = onAcceptClick,
        canAccept = canAccept,
        rejectText = rejectText,
        onRejectClick = onRejectClick,
        tertiaryText = tertiaryText,
        onTertiaryClick = onTertiaryClick,
        content = content,
    )
}

@Composable
fun InfoScreen(
    icon: @Composable () -> Unit,
    headingText: String,
    subtitleText: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    acceptText: String,
    onAcceptClick: () -> Unit,
    canAccept: Boolean,
    rejectText: String? = null,
    onRejectClick: (() -> Unit)? = null,
    tertiaryText: String? = null,
    onTertiaryClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        bottomBar = {
            val strokeWidth = Dp.Hairline
            val borderColor = MaterialTheme.colorScheme.outline
            Column(
                modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .drawBehind {
                        drawLine(
                            borderColor,
                            Offset(0f, 0f),
                            Offset(size.width, 0f),
                            strokeWidth.value,
                        )
                    }
                    .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                    .padding(
                        horizontal = Size.medium,
                        vertical = Size.small,
                    ),
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = tint),
                    onClick = onAcceptClick,
                ) {
                    Text(text = acceptText)
                }
                if (rejectText != null && onRejectClick != null) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        border =
                            BorderStroke(
                                width = Size.extraExtraTiny,
                                color = tint,
                            ),
                        onClick = onRejectClick,
                    ) {
                        Text(text = rejectText)
                    }
                }
                if (tertiaryText != null && onTertiaryClick != null) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        border =
                            BorderStroke(
                                width = Size.extraExtraTiny,
                                color = tint,
                            ),
                        onClick = onTertiaryClick,
                    ) {
                        Text(text = tertiaryText)
                    }
                }
            }
        },
    ) { paddingValues ->
        // Status bar scrim
        Box(
            modifier = Modifier
                .zIndex(2f)
                .secondaryItemAlpha()
                .background(MaterialTheme.colorScheme.background)
                .fillMaxWidth()
                .height(paddingValues.calculateTopPadding()),
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(top = Size.huge)
                .padding(horizontal = Size.medium),
        ) {
            icon()

            Text(
                text = headingText,
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = subtitleText,
                modifier = Modifier
                    .secondaryItemAlpha()
                    .padding(vertical = Size.small),
                style = MaterialTheme.typography.titleSmall,
            )

            content()
        }
    }
}

@Preview
@Composable
private fun InfoScreenPreview() {
    InfoScreen(
        icon = Icons.Outlined.RocketLaunch,
        headingText = "Welcome!",
        subtitleText = "Subtitle",
        acceptText = "Accept",
        onAcceptClick = {},
        canAccept = true,
        rejectText = "Reject",
        onRejectClick = {},
    ) {
        Text(text = "Hello World")
    }
}
