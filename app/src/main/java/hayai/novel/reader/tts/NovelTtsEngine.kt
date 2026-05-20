package hayai.novel.reader.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Thin wrapper around [TextToSpeech]. Owns exactly one engine instance and exposes
 * utterance lifecycle events as a [SharedFlow] so the controller can consume them on
 * a single coroutine context — eliminating the threading races the old viewer-owned
 * implementation had.
 *
 * Phase 1 stub: signatures only; engine work lands in Phase 2.
 */
class NovelTtsEngine(
    @Suppress("UnusedPrivateProperty") private val context: Context,
) {
    private val _events = MutableSharedFlow<UtteranceEvent>(
        replay = 0,
        extraBufferCapacity = 64,
    )
    val events: SharedFlow<UtteranceEvent> = _events.asSharedFlow()

    /** Lazy-initialises [TextToSpeech]. Returns true if the engine bound successfully. */
    suspend fun ensureInitialized(engineName: String? = null): Boolean = false

    fun applySettings(
        voiceTag: String,
        pitch: Float,
        speechRate: Float,
        engineName: String,
    ) {
        // Implemented in Phase 2.
    }

    fun speak(utteranceId: String, text: String, flush: Boolean) {
        // Implemented in Phase 2.
    }

    fun stop() {
        // Implemented in Phase 2.
    }

    fun shutdown() {
        // Implemented in Phase 2.
    }

    fun maxInputLength(): Int = TextToSpeech.getMaxSpeechInputLength()
}

/**
 * Events lifted out of [android.speech.tts.UtteranceProgressListener] into a Kotlin
 * type the controller can pattern-match on.
 */
sealed class UtteranceEvent {
    data class Started(val utteranceId: String) : UtteranceEvent()
    data class Done(val utteranceId: String) : UtteranceEvent()
    data class Error(val utteranceId: String?, val message: String) : UtteranceEvent()
}
