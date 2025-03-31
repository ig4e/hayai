package eu.kanade.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.BottomSheetDefaults
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.lifecycle.DisposableEffectIgnoringConfiguration
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.util.ScreenTransition
import eu.kanade.presentation.util.isTabletUi

@OptIn(InternalVoyagerApi::class)
@Composable
fun NavigatorAdaptiveSheet(
    screen: Screen,
    enableSwipeDismiss: (Navigator) -> Boolean = { true },
    onDismissRequest: () -> Unit,
    isTabletUi: Boolean = isTabletUi(),
) {
    Navigator(
        screen = screen,
        content = { sheetNavigator ->
            if (isTabletUi) {
                // For tablet UI, use a regular dialog
                Dialog(
                    onDismissRequest = onDismissRequest,
                    properties = dialogProperties,
                ) {
                    Surface(
                        modifier = Modifier
                            .requiredWidthIn(max = 460.dp)
                            .padding(vertical = 16.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        NavigatorContent(sheetNavigator)
                    }
                }
            } else {
                // For phone UI, use Material 3 ModalBottomSheet
                ModalBottomSheet(
                    onDismissRequest = onDismissRequest,
                    dragHandle = if (enableSwipeDismiss(sheetNavigator)) {
                        { BottomSheetDefaults.DragHandle() }
                    } else null,
                    shape = MaterialTheme.shapes.extraLarge,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    scrimColor = Color.Transparent, // Transparent scrim to fix the background filling issue
                ) {
                    NavigatorContent(sheetNavigator)
                }
            }

            // Make sure screens are disposed no matter what
            if (sheetNavigator.parent?.disposeBehavior?.disposeNestedNavigators == false) {
                DisposableEffectIgnoringConfiguration {
                    onDispose {
                        sheetNavigator.items
                            .asReversed()
                            .forEach(sheetNavigator::dispose)
                    }
                }
            }
        },
    )
}

@Composable
private fun NavigatorContent(navigator: Navigator) {
    ScreenTransition(
        navigator = navigator,
        transition = {
            fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith
                fadeOut(animationSpec = tween(90))
        },
    )

    BackHandler(
        enabled = navigator.size > 1,
        onBack = navigator::pop,
    )
}

/**
 * Sheet with adaptive position aligned to bottom on small screen, otherwise aligned to center
 * and will not be able to dismissed with swipe gesture.
 *
 * Max width of the content is set to 460 dp.
 */
@Composable
fun AdaptiveSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    enableSwipeDismiss: Boolean = true,
    isTabletUi: Boolean = isTabletUi(),
    scrimColor: Color = Color.Black.copy(alpha = 0.32f),
    content: @Composable () -> Unit,
) {
    if (isTabletUi) {
        // For tablet UI, use a regular dialog
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = dialogProperties,
        ) {
            Surface(
                modifier = modifier
                    .requiredWidthIn(max = 460.dp)
                    .padding(vertical = 16.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                content()
            }
        }
    } else {
        // For phone UI, use Material 3 ModalBottomSheet
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            dragHandle = if (enableSwipeDismiss) {
                { BottomSheetDefaults.DragHandle() }
            } else null,
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            scrimColor = scrimColor,
        ) {
            content()
        }
    }
}

private val dialogProperties = DialogProperties(
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = true,
)

/**
 * Dialog with transparent scrim background
 */
@Composable
fun TransparentDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Add a transparent spacer to fill the entire area
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            )

            // The actual content
            content()
        }
    }
}

/**
 * Sheet with adaptive position aligned to bottom on small screen, otherwise aligned to center
 * and will not be able to dismissed with swipe gesture with NO background scrim.
 */
@Composable
fun AdaptiveSheetNoScrim(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    enableSwipeDismiss: Boolean = true,
    isTabletUi: Boolean = isTabletUi(),
    content: @Composable () -> Unit,
) {
    TransparentDialog(
        onDismissRequest = onDismissRequest,
        properties = dialogProperties,
    ) {
        AdaptiveSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            enableSwipeDismiss = enableSwipeDismiss,
            isTabletUi = isTabletUi,
            scrimColor = Color.Transparent,
            content = content,
        )
    }
}
