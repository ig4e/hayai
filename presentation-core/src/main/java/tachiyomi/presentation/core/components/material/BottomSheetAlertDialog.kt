package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import tachiyomi.presentation.core.util.isTabletUi

/**
 * Bottom sheet version of AlertDialog that provides the same API
 * but always shows as a bottom sheet (or centered dialog on tablets)
 */
@Composable
fun BottomSheetAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
) {
    CoreAdaptiveSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        enableSwipeDismiss = properties.dismissOnClickOutside,
    ) { isTabletUi ->
        if (isTabletUi) {
            // For tablet UI, use the standard AlertDialogContent
            AlertDialogContent(
                buttons = {
                    if (dismissButton != null) {
                        dismissButton()
                    }
                    confirmButton()
                },
                icon = icon,
                title = title,
                text = { text?.invoke() ?: Box {} },
            )
        } else {
            // For phone UI, use a flatter layout without the extra Surface and padding
            BottomSheetContent(
                buttons = {
                    if (dismissButton != null) {
                        dismissButton()
                    }
                    confirmButton()
                },
                icon = icon,
                title = title,
                text = { text?.invoke() ?: Box {} },
            )
        }
    }
}

/**
 * Content layout specifically designed for bottom sheets without the
 * extra dialog-like styling that causes the "dialog in a sheet" appearance
 */
@Composable
private fun BottomSheetContent(
    buttons: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        if (icon != null || title != null) {
            Column(
                modifier = Modifier
                    .padding(
                        start = DialogPadding,
                        top = 8.dp,
                        end = DialogPadding,
                    )
                    .fillMaxWidth(),
            ) {
                icon?.let {
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.secondary,
                    ) {
                        Box(
                            Modifier
                                .padding(IconPadding)
                                .align(Alignment.CenterHorizontally),
                        ) {
                            icon()
                        }
                    }
                }
                title?.let {
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                    ) {
                        val textStyle = MaterialTheme.typography.headlineSmall
                        ProvideTextStyle(textStyle) {
                            Box(
                                // Align the title to the center when an icon is present.
                                Modifier
                                    .padding(
                                        start = 0.dp,
                                        top = if (icon == null) 0.dp else IconPadding.calculateTopPadding(),
                                        end = 0.dp,
                                        bottom = TitlePadding.calculateBottomPadding(),
                                    )
                                    .align(
                                        if (icon == null) {
                                            Alignment.Start
                                        } else {
                                            Alignment.CenterHorizontally
                                        },
                                    ),
                            ) {
                                title()
                            }
                        }
                    }
                }
            }
        }

        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            val textStyle = MaterialTheme.typography.bodyMedium
            ProvideTextStyle(textStyle) {
                Box(
                    Modifier
                        .padding(horizontal = DialogPadding)
                        .padding(top = if (icon == null && title == null) 8.dp else 0.dp, bottom = 12.dp)
                        .align(Alignment.Start),
                ) {
                    text()
                }
            }
        }

        Row(
            modifier = Modifier
                .padding(
                    start = DialogPadding,
                    end = DialogPadding,
                    bottom = 8.dp,
                )
                .align(Alignment.End),
        ) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.primary,
            ) {
                val textStyle = MaterialTheme.typography.labelLarge
                ProvideTextStyle(value = textStyle, content = buttons)
            }
        }
    }
}

/**
 * Sheet with adaptive position aligned to bottom on small screen, otherwise aligned to center
 * and will not be able to dismissed with swipe gesture.
 *
 * Max width of the content is set to 460 dp.
 */
@Composable
fun CoreAdaptiveSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    enableSwipeDismiss: Boolean = true,
    isTabletUi: Boolean = isTabletUi(),
    scrimColor: Color = Color.Black.copy(alpha = 0.32f),
    content: @Composable (Boolean) -> Unit,
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
                content(true)
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
            tonalElevation = 0.dp,
        ) {
            content(false)
        }
    }
}

private val dialogProperties = DialogProperties(
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = true,
)

// Constants copied from AlertDialog
private val DialogPadding = 24.dp
private val IconPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
private val TitlePadding = PaddingValues(top = 16.dp, bottom = 16.dp)
private val TextPadding = PaddingValues(bottom = 16.dp)
private val MinWidth = 280.dp
private val MaxWidth = 560.dp
