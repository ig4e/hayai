package tachiyomi.presentation.core.components.material

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Custom NavigationBarItem with modern animation and styling
 */
@Composable
fun RowScope.NavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = false,
    badgeCount: Int? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val styledIcon = @Composable {
        val iconColor by animateColorAsState(
            targetValue = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            animationSpec = tween(durationMillis = 300),
            label = "IconColor",
        )

        // Add scale animation for the icon
        val iconScale by animateFloatAsState(
            targetValue = if (selected) 1.2f else 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "IconScale"
        )

        Box(
            modifier = Modifier
                .padding(4.dp)
                .scale(iconScale),
        ) {
            CompositionLocalProvider(LocalContentColor provides iconColor) {
                if (badgeCount != null) {
                    BadgedBox(
                        badge = {
                            Badge {
                                Text(text = badgeCount.toString())
                            }
                        },
                        modifier = Modifier.align(Alignment.Center),
                    ) {
                        Box {
                            icon()
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.align(Alignment.Center),
                    ) {
                        icon()
                    }
                }
            }
        }
    }

    val styledLabel: @Composable (() -> Unit)? = label?.let {
        @Composable {
            val textColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(durationMillis = 300),
                label = "TextColor",
            )

            CompositionLocalProvider(LocalContentColor provides textColor) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .fillMaxWidth(),
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides textColor,
                        content = it,
                    )
                }
            }
        }
    }

    Box(
        modifier = modifier
            .clickable(
                onClick = onClick,
                enabled = enabled,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            )
            .weight(1f)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            styledIcon()

            // Show label based on selection state and alwaysShowLabel
            if (label != null) {
                AnimatedVisibility(
                    visible = selected || alwaysShowLabel,
                    enter = fadeIn(animationSpec = tween(150)) + scaleIn(
                        initialScale = 0.8f,
                        animationSpec = tween(150)
                    ),
                    exit = fadeOut(animationSpec = tween(100)) + scaleOut(
                        targetScale = 0.8f,
                        animationSpec = tween(100)
                    )
                ) {
                    styledLabel?.invoke()
                }
            }
        }
    }
}
