package yokai.presentation.component.preference

import yokai.util.koin.get
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import yokai.presentation.theme.isReducedMotion
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.core.storage.preference.collectAsState
import eu.kanade.tachiyomi.data.track.TrackPreferences
import kotlinx.coroutines.launch
import yokai.presentation.component.preference.widget.EditTextPreferenceWidget
import yokai.presentation.component.preference.widget.InfoWidget
import yokai.presentation.component.preference.widget.ListPreferenceWidget
import yokai.presentation.component.preference.widget.MultiSelectListPreferenceWidget
import yokai.presentation.component.preference.widget.SliderPreferenceWidget
import yokai.presentation.component.preference.widget.SwitchPreferenceWidget
import yokai.presentation.component.preference.widget.TextPreferenceWidget
import yokai.presentation.component.preference.widget.TrackingPreferenceWidget

val LocalPreferenceHighlighted = compositionLocalOf(structuralEqualityPolicy()) { false }
val LocalPreferenceMinHeight = compositionLocalOf(structuralEqualityPolicy()) { 56.dp }

@Composable
fun StatusWrapper(
    item: Preference.PreferenceItem<*>,
    highlightKey: String?,
    content: @Composable () -> Unit,
) {
    val enabled = item.enabled
    val highlighted = item.title == highlightKey
    val reducedMotion = isReducedMotion
    AnimatedVisibility(
        visible = enabled,
        enter = if (reducedMotion) EnterTransition.None else expandVertically() + fadeIn(),
        exit = if (reducedMotion) ExitTransition.None else shrinkVertically() + fadeOut(),
        content = {
            CompositionLocalProvider(
                LocalPreferenceHighlighted provides highlighted,
                content = content,
            )
        },
    )
}

@Composable
internal fun PreferenceItem(
    item: Preference.PreferenceItem<*>,
    highlightKey: String?,
) {
    val scope = rememberCoroutineScope()
    StatusWrapper(
        item = item,
        highlightKey = highlightKey,
    ) {
        when (item) {
            is Preference.PreferenceItem.SwitchPreference -> {
                val value by item.pref.collectAsState()
                SwitchPreferenceWidget(
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    checked = value,
                    onCheckedChanged = { newValue ->
                        scope.launch {
                            if (item.onValueChanged(newValue)) {
                                item.pref.set(newValue)
                            }
                        }
                    },
                )
            }
            is Preference.PreferenceItem.SliderPreference -> {
                SliderPreferenceWidget(
                    title = item.title,
                    subtitle = item.subtitle.takeUnless { it.isNullOrEmpty() } ?: item.value.toString(),
                    value = item.value,
                    min = item.min,
                    max = item.max,
                    onValueChange = {
                        scope.launch {
                            item.onValueChanged(it.toInt())
                        }
                    },
                )
            }
            is Preference.PreferenceItem.ListPreference<*> -> {
                val value by item.pref.collectAsState()
                ListPreferenceWidget(
                    value = value,
                    title = item.title,
                    subtitle = item.internalSubtitleProvider(value, item.entries),
                    icon = item.icon,
                    entries = item.entries,
                    onValueChange = { newValue ->
                        scope.launch {
                            if (item.internalOnValueChanged(newValue!!)) {
                                item.internalSet(newValue)
                            }
                        }
                    },
                )
            }
            is Preference.PreferenceItem.BasicListPreference -> {
                ListPreferenceWidget(
                    value = item.value,
                    title = item.title,
                    subtitle = item.subtitleProvider(item.value, item.entries),
                    icon = item.icon,
                    entries = item.entries,
                    onValueChange = { scope.launch { item.onValueChanged(it) } },
                )
            }
            is Preference.PreferenceItem.MultiSelectListPreference -> {
                val values by item.pref.collectAsState()
                MultiSelectListPreferenceWidget(
                    preference = item,
                    values = values,
                    onValuesChange = { newValues ->
                        scope.launch {
                            if (item.onValueChanged(newValues)) {
                                item.pref.set(newValues.toMutableSet())
                            }
                        }
                    },
                )
            }
            is Preference.PreferenceItem.TextPreference -> {
                TextPreferenceWidget(
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    onPreferenceClick = item.onClick,
                )
            }
            is Preference.PreferenceItem.EditTextPreference -> {
                val values by item.pref.collectAsState()
                EditTextPreferenceWidget(
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    value = values,
                    onConfirm = {
                        val accepted = item.onValueChanged(it)
                        if (accepted) item.pref.set(it)
                        accepted
                    },
                )
            }
            is Preference.PreferenceItem.TrackerPreference -> {
                val uName by get<TrackPreferences>()
                    .trackUsername(item.tracker)
                    .collectAsState()
                item.tracker.run {
                    TrackingPreferenceWidget(
                        tracker = this,
                        checked = uName.isNotEmpty(),
                        onClick = { if (isLogged) item.logout() else item.login() },
                    )
                }
            }
            is Preference.PreferenceItem.InfoPreference -> {
                InfoWidget(text = item.title)
            }
            is Preference.PreferenceItem.CustomPreference -> {
                item.content(item)
            }
        }
    }
}
