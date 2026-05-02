package hayai.novel.theme.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hayai.novel.theme.ui.component.NovelThemeEditorDialog
import hayai.novel.theme.ui.component.NovelThemeRow

/**
 * Reusable theme-manager body. Both the standalone Voyager screen and the in-reader bottom-sheet
 * host call this composable so the UI stays a single source of truth.
 *
 * Surface comes from the host (sheet renders on `surfaceContainerHigh`, Voyager screen on
 * `background`); padding matches Material 3 list styling so rows breathe and the FAB doesn't
 * collide with content.
 */
@Composable
fun NovelThemeManagerContent(
    screenModel: NovelThemeManagerScreenModel,
    modifier: Modifier = Modifier,
) {
    val state by screenModel.state.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        when (val s = state) {
            NovelThemeManagerScreenModel.State.Loading -> Unit
            is NovelThemeManagerScreenModel.State.Ready -> ThemeList(
                state = s,
                screenModel = screenModel,
            )
        }

        ExtendedFloatingActionButton(
            onClick = { screenModel.beginAddCustom() },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("New theme") },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )

        val ready = state as? NovelThemeManagerScreenModel.State.Ready
        ready?.editor?.let { editor ->
            NovelThemeEditorDialog(
                editor = editor,
                onNameChange = screenModel::updateEditorName,
                onTextColorChange = screenModel::updateEditorTextColor,
                onBackgroundColorChange = screenModel::updateEditorBackgroundColor,
                onCancel = screenModel::cancelEdit,
                onSave = screenModel::saveEditor,
            )
        }
    }
}

@Composable
private fun ThemeList(
    state: NovelThemeManagerScreenModel.State.Ready,
    screenModel: NovelThemeManagerScreenModel,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item { SectionHeader("Built-in") }
        items(items = state.builtIns, key = { it.id }) { theme ->
            NovelThemeRow(
                theme = theme,
                selected = theme.id == state.selectedId,
                onSelect = { screenModel.selectTheme(theme.id) },
            )
        }

        item { SectionHeader("Custom") }
        if (state.customThemes.isEmpty()) {
            item {
                Text(
                    text = "Tap “New theme” to create your own colors.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }
        } else {
            items(items = state.customThemes, key = { it.id }) { theme ->
                NovelThemeRow(
                    theme = theme,
                    selected = theme.id == state.selectedId,
                    onSelect = { screenModel.selectTheme(theme.id) },
                    onEdit = { screenModel.beginEditCustom(theme) },
                    onDelete = { screenModel.deleteCustom(theme) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
    )
}
