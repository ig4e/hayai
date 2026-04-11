package exh.debug

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import eu.kanade.tachiyomi.util.compose.LocalBackPress
import eu.kanade.tachiyomi.util.compose.currentOrThrow
import exh.util.capitalize
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yokai.presentation.AppBarType
import yokai.presentation.YokaiScaffold
import yokai.presentation.component.preference.widget.TextPreferenceWidget
import yokai.presentation.component.preference.widget.TrailingWidgetBuffer
import yokai.util.Screen
import java.util.Locale
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions

class SettingsDebugScreen : Screen() {

    data class DebugToggle(val name: String, val enabled: Boolean, val default: Boolean, val toggle: DebugToggles)

    @Composable
    override fun Content() {
        val backPress = LocalBackPress.currentOrThrow
        val scope = rememberCoroutineScope()

        val functions by produceState<ImmutableList<Pair<KFunction<*>, String>>?>(initialValue = null) {
            value = withContext(Dispatchers.Default) {
                DebugFunctions::class.declaredFunctions.filter {
                    it.visibility == KVisibility.PUBLIC
                }.map {
                    it to it.name.replace("(.)(\\p{Upper})".toRegex(), "$1 $2")
                        .lowercase(Locale.getDefault())
                        .capitalize(Locale.getDefault())
                }.toImmutableList()
            }
        }

        var toggles by remember {
            mutableStateOf(
                DebugToggles.entries.map {
                    DebugToggle(it.name, it.enabled, it.default, it)
                }.toImmutableList(),
            )
        }

        YokaiScaffold(
            onNavigationIconClicked = backPress::invoke,
            title = "DEBUG MENU",
            navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
            appBarType = AppBarType.SMALL,
        ) { paddingValues ->
            Crossfade(functions == null, label = "debug_functions") {
                when (it) {
                    true -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    false -> FunctionList(paddingValues, functions ?: persistentListOf(), toggles, scope) { newToggles ->
                        toggles = newToggles
                    }
                }
            }
        }
    }

    @Composable
    fun FunctionList(
        paddingValues: PaddingValues,
        functions: ImmutableList<Pair<KFunction<*>, String>>,
        toggles: ImmutableList<DebugToggle>,
        scope: CoroutineScope,
        onTogglesChanged: (ImmutableList<DebugToggle>) -> Unit,
    ) {
        Box(Modifier.fillMaxSize()) {
            var running by remember { mutableStateOf(false) }
            var result by remember { mutableStateOf<Pair<String, String>?>(null) }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = paddingValues,
            ) {
                item {
                    Text(
                        text = "Functions",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                items(functions) { (func, name) ->
                    TextPreferenceWidget(
                        title = name,
                        onPreferenceClick = {
                            scope.launch(Dispatchers.Default) {
                                val text = try {
                                    running = true
                                    "Function returned result:\n\n${func.call(DebugFunctions)}"
                                } catch (e: Exception) {
                                    "Function threw exception:\n\n${Log.getStackTraceString(e)}"
                                } finally {
                                    running = false
                                }
                                result = name to text
                            }
                        },
                    )
                }
                item {
                    HorizontalDivider()
                }
                item {
                    Text(
                        text = "Toggles",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                items(toggles) { debugToggle ->
                    TextPreferenceWidget(
                        title = debugToggle.name.replace('_', ' ')
                            .lowercase(Locale.getDefault())
                            .capitalize(Locale.getDefault()),
                        subtitle = if (debugToggle.enabled != debugToggle.default) "MODIFIED" else null,
                        widget = {
                            Switch(
                                checked = debugToggle.enabled,
                                onCheckedChange = null,
                                modifier = Modifier.padding(start = TrailingWidgetBuffer),
                            )
                        },
                        onPreferenceClick = {
                            debugToggle.toggle.enabled = !debugToggle.enabled
                            onTogglesChanged(
                                DebugToggles.entries.map {
                                    DebugToggle(it.name, it.enabled, it.default, it)
                                }.toImmutableList(),
                            )
                        },
                    )
                }
                item {
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
            AnimatedVisibility(
                running && result == null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(color = Color.White.copy(alpha = 0.3F))
                        .pointerInput(running && result == null) {},
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            ResultTextDialog(
                result = result,
                onDismissRequest = { result = null },
            )
        }
    }

    @Composable
    private fun ResultTextDialog(result: Pair<String, String>?, onDismissRequest: () -> Unit) {
        if (result != null) {
            AlertDialog(
                onDismissRequest = onDismissRequest,
                title = {
                    Text(text = result.first)
                },
                confirmButton = {},
                text = {
                    SelectionContainer(Modifier.verticalScroll(rememberScrollState())) {
                        Text(text = result.second)
                    }
                },
            )
        }
    }
}
