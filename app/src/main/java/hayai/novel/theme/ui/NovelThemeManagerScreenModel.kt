package hayai.novel.theme.ui

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import hayai.novel.preferences.NovelPreferences
import hayai.novel.theme.NovelTheme
import hayai.novel.theme.NovelThemeRegistry
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.injectLazy
import java.util.UUID

/**
 * Screen model for the novel theme manager. Exposes the merged list of built-in + custom themes,
 * the currently selected id, and the in-progress editor target. Mutations write straight back to
 * [NovelPreferences]; the recycler observes those preferences via [hayai.novel.reader.NovelConfig]
 * so applying a theme is a one-line update.
 */
class NovelThemeManagerScreenModel : StateScreenModel<NovelThemeManagerScreenModel.State>(State.Loading) {

    private val novelPreferences: NovelPreferences by injectLazy()

    init {
        // Seed Ready state synchronously — Preference.changes() only emits on subsequent
        // changes, so without this the sheet would re-open on Loading every time.
        mutableState.value = State.Ready(
            selectedId = novelPreferences.selectedThemeId().get(),
            builtIns = NovelThemeRegistry.builtIns.toImmutableList(),
            customThemes = novelPreferences.customThemes().get().toImmutableList(),
            editor = null,
        )
        screenModelScope.launch {
            combine(
                novelPreferences.selectedThemeId().changes(),
                novelPreferences.customThemes().changes(),
            ) { selectedId, customs ->
                State.Ready(
                    selectedId = selectedId,
                    builtIns = NovelThemeRegistry.builtIns.toImmutableList(),
                    customThemes = customs.toImmutableList(),
                    editor = null,
                )
            }.collect { newState ->
                mutableState.update { current ->
                    if (current is State.Ready) newState.copy(editor = current.editor) else newState
                }
            }
        }
    }

    fun selectTheme(themeId: String) {
        novelPreferences.selectedThemeId().set(themeId)
    }

    fun beginAddCustom() {
        mutableState.update { current ->
            if (current !is State.Ready) return@update current
            current.copy(
                editor = ThemeEditor(
                    target = null,
                    name = DEFAULT_NEW_NAME,
                    textColor = DEFAULT_TEXT_COLOR,
                    backgroundColor = DEFAULT_BACKGROUND_COLOR,
                ),
            )
        }
    }

    fun beginEditCustom(theme: NovelTheme) {
        if (theme.builtIn) return
        mutableState.update { current ->
            if (current !is State.Ready) return@update current
            current.copy(
                editor = ThemeEditor(
                    target = theme,
                    name = theme.name,
                    textColor = theme.textColor,
                    backgroundColor = theme.backgroundColor,
                ),
            )
        }
    }

    fun cancelEdit() {
        mutableState.update { current ->
            if (current is State.Ready) current.copy(editor = null) else current
        }
    }

    fun updateEditorName(name: String) {
        mutableState.update { current ->
            if (current !is State.Ready) return@update current
            val editor = current.editor ?: return@update current
            current.copy(editor = editor.copy(name = name))
        }
    }

    fun updateEditorTextColor(color: String) {
        mutableState.update { current ->
            if (current !is State.Ready) return@update current
            val editor = current.editor ?: return@update current
            current.copy(editor = editor.copy(textColor = color))
        }
    }

    fun updateEditorBackgroundColor(color: String) {
        mutableState.update { current ->
            if (current !is State.Ready) return@update current
            val editor = current.editor ?: return@update current
            current.copy(editor = editor.copy(backgroundColor = color))
        }
    }

    fun saveEditor() {
        val current = state.value as? State.Ready ?: return
        val editor = current.editor ?: return
        val sanitized = NovelTheme(
            id = editor.target?.id ?: UUID.randomUUID().toString(),
            name = editor.name.trim().ifBlank { DEFAULT_NEW_NAME },
            textColor = editor.textColor,
            backgroundColor = editor.backgroundColor,
            builtIn = false,
        )
        val newCustoms = if (editor.target == null) {
            current.customThemes + sanitized
        } else {
            current.customThemes.map { if (it.id == sanitized.id) sanitized else it }
        }
        novelPreferences.customThemes().set(newCustoms)
        // Auto-select a brand-new theme so the user sees their work applied immediately.
        if (editor.target == null) novelPreferences.selectedThemeId().set(sanitized.id)
        cancelEdit()
    }

    fun deleteCustom(theme: NovelTheme) {
        if (theme.builtIn) return
        val current = state.value as? State.Ready ?: return
        val newCustoms = current.customThemes.filterNot { it.id == theme.id }
        novelPreferences.customThemes().set(newCustoms)
        if (current.selectedId == theme.id) {
            novelPreferences.selectedThemeId().set(NovelThemeRegistry.DEFAULT_ID)
        }
    }

    @Immutable
    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data class Ready(
            val selectedId: String,
            val builtIns: ImmutableList<NovelTheme>,
            val customThemes: ImmutableList<NovelTheme>,
            val editor: ThemeEditor?,
        ) : State
    }

    /**
     * Holds the in-progress edit. `target == null` means the editor is creating a new theme;
     * non-null means we're editing that existing custom theme.
     */
    @Immutable
    data class ThemeEditor(
        val target: NovelTheme?,
        val name: String,
        val textColor: String,
        val backgroundColor: String,
    )

    private companion object {
        const val DEFAULT_NEW_NAME = "Custom"
        const val DEFAULT_TEXT_COLOR = "#212121"
        const val DEFAULT_BACKGROUND_COLOR = "#FFFFFF"
    }
}
