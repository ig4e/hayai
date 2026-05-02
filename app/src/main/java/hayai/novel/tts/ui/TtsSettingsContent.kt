package hayai.novel.tts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hayai.novel.preferences.NovelPreferences

/**
 * Reusable TTS settings body. Hosted by [TtsSettingsScreen] for app-level navigation and by
 * [TtsLaunchSheet] for the in-reader bottom sheet entry. Identical content in both places so
 * the user gets the same UI no matter where they enter from.
 */
@Composable
fun TtsSettingsContent(
    screenModel: TtsSettingsScreenModel,
    modifier: Modifier = Modifier,
) {
    val state by screenModel.state.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        item { SectionHeader("Voice") }
        if (state.voices.isEmpty()) {
            item {
                Text(
                    text = "No voices available — install a TTS engine in Android Settings → System → Languages & input → On-device speech.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }
        } else {
            items(state.voices, key = { it.id }) { voice ->
                VoiceRow(
                    displayName = voice.displayName,
                    language = voice.language,
                    selected = voice.id == state.selectedVoiceId,
                    onSelect = { screenModel.setVoice(voice.id) },
                    onPreview = { screenModel.previewVoice(voice) },
                )
            }
        }

        item { SectionHeader("Playback") }
        item {
            SliderRow(
                title = "Speed",
                value = state.speed,
                valueRange = NovelPreferences.MIN_TTS_RATE..NovelPreferences.MAX_TTS_RATE,
                onValueChange = screenModel::setSpeed,
            )
        }
        item {
            SliderRow(
                title = "Pitch",
                value = state.pitch,
                valueRange = NovelPreferences.MIN_TTS_RATE..NovelPreferences.MAX_TTS_RATE,
                onValueChange = screenModel::setPitch,
            )
        }
        item {
            SliderRow(
                title = "Volume",
                value = state.volume,
                valueRange = 0f..1f,
                onValueChange = screenModel::setVolume,
            )
        }
        item {
            ToggleRow(
                title = "Highlight as spoken",
                value = state.highlightAsSpoken,
                onValueChange = screenModel::setHighlight,
            )
        }
        item {
            ToggleRow(
                title = "Continue to next chapter",
                value = state.continuousReading,
                onValueChange = screenModel::setContinuous,
            )
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

@Composable
private fun VoiceRow(
    displayName: String,
    language: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = language.ifEmpty { "—" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onPreview) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = "Preview",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                text = String.format("%.2f", value),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
        )
    }
}

@Composable
private fun ToggleRow(title: String, value: Boolean, onValueChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onValueChange)
    }
}
