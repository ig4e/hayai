package tachiyomi.presentation.core.components.material

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties

/**
 * A Dialog component that provides a Material 3 bottom sheet on phones
 * and a centered dialog on tablets
 */
@Composable
fun BottomSheetDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable (Boolean) -> Unit,
) {
    CoreAdaptiveSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        enableSwipeDismiss = properties.dismissOnClickOutside,
        content = content,
    )
}
