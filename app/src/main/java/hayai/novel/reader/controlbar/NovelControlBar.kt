package hayai.novel.reader.controlbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwipeVertical
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Floating reader-chrome controls for novels — auto-scroll toggle on the left, TTS player on the
 * right. Both render as elevated Material 3 surfaces; the TTS player slider doubles as a seek
 * control over the chapter's sentences.
 *
 * The TTS subsection only takes meaningful screen space when [tts.totalSentences] > 0 (i.e. a
 * chapter has been loaded for playback at least once); otherwise it collapses to a single
 * play button so first-time users have a clear "start listening" entry point.
 */
@Composable
fun NovelControlBar(
    autoScroll: AutoScrollState,
    tts: TtsControlState,
    onToggleAutoScroll: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (sentenceIndex: Int) -> Unit,
    onLongPressTts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AutoScrollPill(
            running = autoScroll.isRunning,
            onClick = onToggleAutoScroll,
        )
        TtsPill(
            tts = tts,
            onTogglePlayPause = onTogglePlayPause,
            onSeek = onSeek,
            onLongPress = onLongPressTts,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AutoScrollPill(running: Boolean, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 6.dp,
        modifier = Modifier.size(56.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            FilledIconToggleButton(
                checked = running,
                onCheckedChange = { onClick() },
                colors = IconButtonDefaults.filledIconToggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    imageVector = if (running) Icons.Filled.Pause else Icons.Filled.SwipeVertical,
                    contentDescription = if (running) "Pause auto-scroll" else "Start auto-scroll",
                )
            }
        }
    }
}

@Composable
private fun TtsPill(
    tts: TtsControlState,
    onTogglePlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 6.dp,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledIconButton(
                onClick = onTogglePlayPause,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (tts.isPlaying || tts.isPaused)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (tts.isPlaying || tts.isPaused)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = when {
                        tts.isPlaying -> Icons.Filled.Pause
                        else -> Icons.Outlined.VolumeUp
                    },
                    contentDescription = if (tts.isPlaying) "Pause reading" else "Start reading",
                )
            }

            if (tts.totalSentences > 0) {
                ProgressSlider(
                    currentSentence = tts.currentSentenceIndex,
                    totalSentences = tts.totalSentences,
                    secondsPerSentence = tts.secondsPerSentence,
                    onSeek = onSeek,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Text(
                    text = "Tap play to listen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ProgressSlider(
    currentSentence: Int,
    totalSentences: Int,
    secondsPerSentence: Float,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Local override while the user is dragging so the slider tracks their finger smoothly even
    // though playback hasn't seeked yet (we only seek on touch-up to avoid synthesis thrash).
    var draftValue by remember { mutableStateOf<Float?>(null) }
    val effective = draftValue ?: currentSentence.toFloat()
    val totalSec = (totalSentences * secondsPerSentence).toInt()
    val elapsedSec = (effective * secondsPerSentence).toInt()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Slider(
            value = effective,
            onValueChange = { v -> draftValue = v },
            onValueChangeFinished = {
                draftValue?.let { v -> onSeek(v.toInt().coerceIn(0, totalSentences - 1)) }
                draftValue = null
            },
            valueRange = 0f..(totalSentences - 1).coerceAtLeast(1).toFloat(),
            steps = (totalSentences - 2).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                thumbColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${formatDuration(elapsedSec)} / ${formatDuration(totalSec)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 4.dp, end = 8.dp),
        )
    }
}

private fun formatDuration(seconds: Int): String {
    if (seconds < 0) return "0:00"
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

/** Animated wrapper used by the chrome layer — fades the bar in/out with the reader menu. */
@Composable
fun NovelControlBarHost(
    visible: Boolean,
    autoScroll: AutoScrollState,
    tts: TtsControlState,
    onToggleAutoScroll: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onLongPressTts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        NovelControlBar(
            autoScroll = autoScroll,
            tts = tts,
            onToggleAutoScroll = onToggleAutoScroll,
            onTogglePlayPause = onTogglePlayPause,
            onSeek = onSeek,
            onLongPressTts = onLongPressTts,
        )
    }
}

data class AutoScrollState(val isRunning: Boolean)

data class TtsControlState(
    val isPlaying: Boolean,
    val isPaused: Boolean,
    val currentSentenceIndex: Int,
    val totalSentences: Int,
    val secondsPerSentence: Float,
)
