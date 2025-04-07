package eu.kanade.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ProvideTextStyle

/**
 * A unified bottom sheet component that provides a consistent Material 3 look and feel
 * for dialogs and sheets on phone interfaces.
 *
 * @param onDismissRequest Called when the user tries to dismiss the sheet.
 * @param modifier Optional modifier for the sheet content.
 * @param sheetState The state of the bottom sheet.
 * @param title Optional composable lambda for the title area.
 * @param actions Optional composable lambda for the sticky actions area at the bottom.
 * @param content The main content of the bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    title: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier.widthIn(max = LocalConfiguration.current.screenWidthDp.dp),
        // TODO: Consider window insets adjustments if needed
        // windowInsets = WindowInsets.navigationBars, // Example
    ) {
        Column {
            // Optional Title
            if (title != null) {
                Box(
                    modifier = Modifier.padding(
                        PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp), // Consistent padding
                    ),
                ) {
                    ProvideTextStyle(value = MaterialTheme.typography.titleLarge) {
                        title()
                    }
                }
            }

            // Scrollable Content Area
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .weight(1f, fill = false) // Expand to fill space, but allow shrinking
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp), // Add horizontal padding
            ) {
                this.content()
            }

            // Sticky Actions Area below content
            if (actions != null) {
                Box(
                    modifier = Modifier.padding(
                        PaddingValues(horizontal = 24.dp, vertical = 16.dp), // Add horizontal padding
                    ),
                ) {
                    actions()
                }
            }
        }
    }
}
