package hayai.novel.theme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import hayai.novel.theme.ui.NovelThemeManagerScreenModel.ThemeEditor

/**
 * Modal dialog for adding or editing a custom theme. Layout: name field, live preview swatch
 * (showing exactly how the reader will render with these colors), then a segmented button to
 * switch between Text / Background color editing on a single HSV picker.
 *
 * Both colors round-trip as `#RRGGBB` strings for storage compatibility with [hayai.novel.theme.NovelTheme].
 */
@Composable
fun NovelThemeEditorDialog(
    editor: ThemeEditor,
    onNameChange: (String) -> Unit,
    onTextColorChange: (String) -> Unit,
    onBackgroundColorChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
        title = { Text(if (editor.target == null) "New theme" else "Edit theme") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                ThemePreview(
                    textColor = editor.textColor,
                    backgroundColor = editor.backgroundColor,
                )

                ColorTabsAndPicker(
                    textColor = editor.textColor,
                    backgroundColor = editor.backgroundColor,
                    onTextColorChange = onTextColorChange,
                    onBackgroundColorChange = onBackgroundColorChange,
                )
            }
        },
    )
}

@Composable
private fun ThemePreview(textColor: String, backgroundColor: String) {
    val bg = parseColorOrFallback(backgroundColor, MaterialTheme.colorScheme.surface)
    val fg = parseColorOrFallback(textColor, contrastingOn(bg))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Chapter preview",
                color = fg,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "The quick brown fox jumps over the lazy dog. Pack my box with five dozen liquor jugs.",
                color = fg,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ColorTabsAndPicker(
    textColor: String,
    backgroundColor: String,
    onTextColorChange: (String) -> Unit,
    onBackgroundColorChange: (String) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("Text") }
            SegmentedButton(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("Background") }
        }

        ColorPickerPanel(
            initialColor = if (selectedTab == 0) textColor else backgroundColor,
            onColorChange = { hex ->
                if (selectedTab == 0) onTextColorChange(hex) else onBackgroundColorChange(hex)
            },
        )
    }
}

@Composable
private fun ColorPickerPanel(initialColor: String, onColorChange: (String) -> Unit) {
    val controller = rememberColorPickerController()
    val parsedInitial = parseColorOrFallback(initialColor, Color.White)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HsvColorPicker(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            controller = controller,
            initialColor = parsedInitial,
            onColorChanged = { envelope: ColorEnvelope -> onColorChange(envelope.toRgbHex()) },
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(controller.selectedColor.value),
            )
            Text(
                text = controller.selectedColor.value.toRgbHexString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun parseColorOrFallback(hex: String, fallback: Color): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)

private fun ColorEnvelope.toRgbHex(): String =
    "#" + hexCode.takeLast(6).uppercase()

private fun Color.toRgbHexString(): String {
    val r = (red * 255f).toInt() and 0xFF
    val g = (green * 255f).toInt() and 0xFF
    val b = (blue * 255f).toInt() and 0xFF
    return String.format("#%02X%02X%02X", r, g, b)
}
