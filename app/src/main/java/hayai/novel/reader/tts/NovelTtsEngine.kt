package hayai.novel.reader.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.ui.reader.viewer.text.NovelViewerTextUtils
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Wraps [TextToSpeech] so the controller never touches the platform API directly.
 *
 * Threading: [UtteranceProgressListener] callbacks land on a TTS-engine thread; we lift
 * them into a [MutableSharedFlow] so the controller can consume them serially on its own
 * coroutine context. No engine-state lives here beyond the [TextToSpeech] handle.
 *
 * Long paragraph handling: when [speakParagraph] is given text longer than
 * [TextToSpeech.getMaxSpeechInputLength], it splits via
 * [NovelViewerTextUtils.splitTextForTts] and emits one utterance per chunk, all tagged
 * with the same `paragraphIndex` so the controller still sees a single logical paragraph.
 */
class NovelTtsEngine(private val context: Context) {

    private val _events = MutableSharedFlow<UtteranceEvent>(
        replay = 0,
        extraBufferCapacity = 128,
    )
    val events: SharedFlow<UtteranceEvent> = _events.asSharedFlow()

    private var tts: TextToSpeech? = null
    @Volatile private var initialized: Boolean = false
    @Volatile private var initEngineName: String? = null

    /**
     * Idempotent. Suspends until the platform engine reports SUCCESS or FAILURE. Returns
     * `true` if the engine is bound and usable. If [engineName] differs from the current
     * binding, the existing engine is torn down and a fresh one is created.
     */
    suspend fun ensureInitialized(engineName: String? = null): Boolean {
        val current = tts
        if (current != null && initialized && initEngineName == engineName) return true

        // Engine identity changed: tear down before re-creating.
        if (current != null) {
            try { current.stop() } catch (_: Exception) {}
            try { current.shutdown() } catch (_: Exception) {}
            tts = null
            initialized = false
        }

        return suspendCancellableCoroutine { cont ->
            val listener = TextToSpeech.OnInitListener { status ->
                if (status == TextToSpeech.SUCCESS) {
                    initialized = true
                    initEngineName = engineName
                    tts?.setOnUtteranceProgressListener(progressListener)
                    if (cont.isActive) cont.resume(true)
                } else {
                    Logger.e { "TTS: engine init failed status=$status engine=$engineName" }
                    initialized = false
                    if (cont.isActive) cont.resume(false)
                }
            }
            tts = try {
                if (!engineName.isNullOrBlank()) {
                    TextToSpeech(context.applicationContext, listener, engineName)
                } else {
                    TextToSpeech(context.applicationContext, listener)
                }
            } catch (e: Exception) {
                Logger.e(e) { "TTS: failed to construct TextToSpeech" }
                if (cont.isActive) cont.resume(false)
                null
            }
        }
    }

    /**
     * Reads voice/pitch/rate/engine from the caller-supplied values and applies them to
     * the underlying engine. Safe to call repeatedly. If [voiceTag] does not resolve to
     * an installed voice, falls back to setting the engine's [Locale] from the same tag.
     */
    fun applySettings(
        voiceTag: String,
        pitch: Float,
        speechRate: Float,
        @Suppress("UnusedParameter") engineName: String,
    ) {
        val engine = tts ?: return
        try {
            if (voiceTag.isNotBlank()) {
                val voice = engine.voices?.firstOrNull { it.name == voiceTag }
                if (voice != null) {
                    engine.voice = voice
                } else {
                    engine.language = runCatching { Locale.forLanguageTag(voiceTag) }
                        .getOrDefault(Locale.getDefault())
                }
            } else {
                engine.language = Locale.getDefault()
            }
            engine.setPitch(pitch.coerceAtLeast(0.1f))
            engine.setSpeechRate(speechRate.coerceAtLeast(0.1f))
        } catch (e: Exception) {
            Logger.w(e) { "TTS: applySettings failed" }
        }
    }

    /**
     * Speaks one logical paragraph. The text is automatically split into engine-sized
     * utterances if longer than [maxInputLength]. All emitted utterance ids share the
     * same [paragraphIndex] so the controller's `paragraph started/done` mapping is
     * one-to-one with the user's paragraph regardless of internal chunking.
     */
    fun speakParagraph(paragraphIndex: Int, text: String, flush: Boolean) {
        val engine = tts ?: run {
            _events.tryEmit(UtteranceEvent.Error(null, "engine not initialized"))
            return
        }
        if (text.isBlank()) {
            // Nothing to say — synthesise a started+done pair so the controller still
            // advances. Use a unique id so the listener routes deliver in order.
            val id = NovelTtsController.makeUtteranceId(paragraphIndex, 0, totalChunks = 1)
            _events.tryEmit(UtteranceEvent.Started(id))
            _events.tryEmit(UtteranceEvent.Done(id))
            return
        }

        val chunks = if (text.length <= maxInputLength()) {
            listOf(text)
        } else {
            NovelViewerTextUtils.splitTextForTts(text, maxInputLength())
        }
        val total = chunks.size
        chunks.forEachIndexed { utteranceIndex, chunk ->
            val mode = if (flush && utteranceIndex == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val id = NovelTtsController.makeUtteranceId(paragraphIndex, utteranceIndex, total)
            val result = engine.speak(chunk, mode, null, id)
            if (result == TextToSpeech.ERROR) {
                _events.tryEmit(UtteranceEvent.Error(id, "speak() returned ERROR"))
            }
        }
    }

    fun stop() {
        try { tts?.stop() } catch (e: Exception) {
            Logger.w(e) { "TTS: stop() threw" }
        }
    }

    fun shutdown() {
        try { tts?.stop() } catch (_: Exception) {}
        try { tts?.shutdown() } catch (_: Exception) {}
        tts = null
        initialized = false
        initEngineName = null
    }

    fun maxInputLength(): Int = TextToSpeech.getMaxSpeechInputLength()

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            utteranceId ?: return
            _events.tryEmit(UtteranceEvent.Started(utteranceId))
        }

        override fun onDone(utteranceId: String?) {
            utteranceId ?: return
            _events.tryEmit(UtteranceEvent.Done(utteranceId))
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            _events.tryEmit(UtteranceEvent.Error(utteranceId, "onError"))
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            _events.tryEmit(UtteranceEvent.Error(utteranceId, "onError code=$errorCode"))
        }
    }
}

/**
 * Engine lifecycle events, lifted out of [UtteranceProgressListener] into a Kotlin type
 * the controller can pattern-match on.
 */
sealed class UtteranceEvent {
    data class Started(val utteranceId: String) : UtteranceEvent()
    data class Done(val utteranceId: String) : UtteranceEvent()
    data class Error(val utteranceId: String?, val message: String) : UtteranceEvent()
}
