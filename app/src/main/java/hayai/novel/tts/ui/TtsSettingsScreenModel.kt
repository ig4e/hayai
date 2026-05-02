package hayai.novel.tts.ui

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import hayai.novel.preferences.NovelPreferences
import hayai.novel.tts.engine.TtsEngineFactory
import hayai.novel.tts.engine.TtsRequest
import hayai.novel.tts.engine.TtsVoice
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy

/**
 * State + actions for [TtsSettingsScreen]. Reads from + writes to [NovelPreferences]; the
 * playback layer subscribes to the same prefs so changes apply live.
 */
class TtsSettingsScreenModel : StateScreenModel<TtsSettingsScreenModel.State>(State()) {

    private val novelPreferences: NovelPreferences by injectLazy()
    private val engineFactory: TtsEngineFactory by injectLazy()

    init {
        // Seed state synchronously so the sheet renders populated values immediately on every
        // open — Preference.changes() only emits subsequent changes, not the current value.
        mutableState.value = State(
            voices = persistentListOf(),
            selectedVoiceId = novelPreferences.ttsVoiceId().get(),
            speed = novelPreferences.ttsSpeed().get(),
            pitch = novelPreferences.ttsPitch().get(),
            volume = novelPreferences.ttsVolume().get(),
            highlightAsSpoken = novelPreferences.ttsHighlight().get(),
            continuousReading = novelPreferences.ttsContinuous().get(),
            sentencePauseMs = novelPreferences.ttsSentencePauseMs().get(),
        )
        screenModelScope.launch {
            // Voice list comes from a one-shot engine prepare so the picker has real entries.
            val voices = withContext(Dispatchers.IO) {
                val engine = engineFactory.create()
                val ok = engine.prepare().isSuccess
                val list = if (ok) engine.voices.toImmutableList() else persistentListOf()
                engine.release()
                list
            }
            mutableState.update { it.copy(voices = voices) }

            combine(
                novelPreferences.ttsVoiceId().changes(),
                novelPreferences.ttsSpeed().changes(),
                novelPreferences.ttsPitch().changes(),
                novelPreferences.ttsVolume().changes(),
                novelPreferences.ttsHighlight().changes(),
            ) { voiceId, speed, pitch, volume, highlight ->
                quintuple(voiceId, speed, pitch, volume, highlight)
            }.collect { (voiceId, speed, pitch, volume, highlight) ->
                mutableState.update {
                    it.copy(
                        selectedVoiceId = voiceId,
                        speed = speed,
                        pitch = pitch,
                        volume = volume,
                        highlightAsSpoken = highlight,
                        continuousReading = novelPreferences.ttsContinuous().get(),
                        sentencePauseMs = novelPreferences.ttsSentencePauseMs().get(),
                    )
                }
            }
        }
    }

    fun setVoice(voiceId: String) = novelPreferences.ttsVoiceId().set(voiceId)
    fun setSpeed(value: Float) = novelPreferences.ttsSpeed().set(value)
    fun setPitch(value: Float) = novelPreferences.ttsPitch().set(value)
    fun setVolume(value: Float) = novelPreferences.ttsVolume().set(value)
    fun setHighlight(enabled: Boolean) = novelPreferences.ttsHighlight().set(enabled)
    fun setContinuous(enabled: Boolean) = novelPreferences.ttsContinuous().set(enabled)
    fun setSentencePauseMs(ms: Int) = novelPreferences.ttsSentencePauseMs().set(ms)

    fun previewVoice(voice: TtsVoice) {
        screenModelScope.launch {
            val engine = engineFactory.create()
            engine.prepare().onSuccess {
                engine.synthesize(
                    TtsRequest(
                        text = "The quick brown fox jumps over the lazy dog.",
                        voice = voice,
                        speed = novelPreferences.ttsSpeed().get(),
                        pitch = novelPreferences.ttsPitch().get(),
                    ),
                ).collect { /* The system engine handles its own audio output. */ }
            }
            engine.release()
        }
    }

    @Immutable
    data class State(
        val voices: ImmutableList<TtsVoice> = persistentListOf(),
        val selectedVoiceId: String = "",
        val speed: Float = NovelPreferences.DEFAULT_TTS_SPEED,
        val pitch: Float = NovelPreferences.DEFAULT_TTS_PITCH,
        val volume: Float = NovelPreferences.DEFAULT_TTS_VOLUME,
        val highlightAsSpoken: Boolean = true,
        val continuousReading: Boolean = true,
        val sentencePauseMs: Int = NovelPreferences.DEFAULT_TTS_SENTENCE_PAUSE_MS,
    )

    private data class Quintuple<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
    private fun <A, B, C, D, E> quintuple(a: A, b: B, c: C, d: D, e: E) = Quintuple(a, b, c, d, e)
}
