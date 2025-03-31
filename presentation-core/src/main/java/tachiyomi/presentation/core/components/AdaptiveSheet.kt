package tachiyomi.presentation.core.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import tachiyomi.presentation.core.components.material.CoreAdaptiveSheet
import tachiyomi.presentation.core.util.isTabletUi

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
    content: @Composable (Boolean) -> Unit,
) {
    CoreAdaptiveSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        enableSwipeDismiss = enableSwipeDismiss,
        isTabletUi = isTabletUi,
        scrimColor = scrimColor,
        content = content,
    )
}
