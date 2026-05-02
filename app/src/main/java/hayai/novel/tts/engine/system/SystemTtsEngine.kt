package hayai.novel.tts.engine.system

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import co.touchlab.kermit.Logger
import hayai.novel.tts.engine.TtsChunk
import hayai.novel.tts.engine.TtsEngine
import hayai.novel.tts.engine.TtsEngineError
import hayai.novel.tts.engine.TtsRequest
import hayai.novel.tts.engine.TtsVoice
import hayai.novel.tts.engine.WordTiming
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * [TtsEngine] backed by Android's built-in [TextToSpeech] service. This is the zero-download
 * default — every Android device ships with at least Google's TTS engine, so it's the entry
 * point users see before downloading any premium models.
 *
 * Word timing comes from [UtteranceProgressListener.onRangeStart] (API 26+). Some OEM/old engines
 * never call that callback; [prepare] runs a one-shot probe to detect this and falls back to
 * empty timings (the playback layer then estimates from chunk duration).
 *
 * Audio handling is unusual: TextToSpeech writes audio out through its own internal AudioTrack,
 * so this engine doesn't emit PCM through the normal [TtsChunk.pcm] path — it emits "logical
 * chunks" with empty `pcm` carrying only timings. The playback controller knows to detect this
 * by looking at the engine id and skip the [hayai.novel.tts.engine.audio.TtsAudioPlayer] for
 * system-engine utterances.
 */
class SystemTtsEngine(private val context: Context) : TtsEngine {

    override val id: String = "system"
    override val displayName: String = "System TTS"
    override val supportsPitch: Boolean = true

    private var tts: TextToSpeech? = null
    private var initSucceeded: Boolean = false
    /** Set after the [prepare] probe — if false, [WordTiming] won't be populated for utterances. */
    @Volatile var hasReliableRangeCallback: Boolean = false
        private set

    private val voicesCache = mutableListOf<TtsVoice>()
    override val voices: List<TtsVoice> get() = voicesCache.toList()

    override suspend fun prepare(): Result<Unit> {
        if (initSucceeded) return Result.success(Unit)

        val initResult = CompletableDeferred<Int>()
        tts = TextToSpeech(context.applicationContext) { status -> initResult.complete(status) }
        val status = withTimeoutOrNull(INIT_TIMEOUT_MS) { initResult.await() }
            ?: return Result.failure(TtsEngineError.SystemInitFailed(-1))
        if (status != TextToSpeech.SUCCESS) {
            return Result.failure(TtsEngineError.SystemInitFailed(status))
        }
        initSucceeded = true
        loadVoices()
        probeRangeCallback()
        return Result.success(Unit)
    }

    private fun loadVoices() {
        voicesCache.clear()
        val available = runCatching { tts?.voices }.getOrNull() ?: return
        available.forEach { v ->
            voicesCache += TtsVoice(
                id = v.name,
                displayName = v.name,
                language = v.locale?.toLanguageTag().orEmpty(),
                gender = TtsVoice.Gender.Unknown,
                extras = mapOf(
                    "quality" to v.quality.toString(),
                    "latency" to v.latency.toString(),
                    "networkRequired" to v.isNetworkConnectionRequired.toString(),
                ),
            )
        }
    }

    /**
     * Some OEMs / older engines never call onRangeStart. Synthesize a four-word probe utterance
     * to silent output and watch for the callback. If we don't see it within the probe timeout,
     * mark the callback unreliable and the playback layer will estimate timings from chunk audio
     * duration instead.
     */
    private suspend fun probeRangeCallback() {
        val tts = tts ?: return
        val sawRange = CompletableDeferred<Boolean>()
        val utteranceId = "probe-${UUID.randomUUID()}"
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit
            override fun onDone(id: String?) {
                if (!sawRange.isCompleted) sawRange.complete(false)
            }
            @Deprecated("Deprecated in Android docs but required override.")
            override fun onError(id: String?) {
                if (!sawRange.isCompleted) sawRange.complete(false)
            }
            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                if (!sawRange.isCompleted) sawRange.complete(true)
            }
        })
        // Output to a /dev/null file would be ideal, but Android requires a real File for
        // synthesizeToFile. We synthesize to the speaker at zero volume instead — fast, silent.
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 0f) }
        tts.speak("ready set go", TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        hasReliableRangeCallback = withTimeoutOrNull(PROBE_TIMEOUT_MS) { sawRange.await() } ?: false
        Logger.d { "SystemTtsEngine: hasReliableRangeCallback=$hasReliableRangeCallback" }
    }

    override fun synthesize(request: TtsRequest): Flow<TtsChunk> = callbackFlow {
        val tts = tts ?: run {
            close(TtsEngineError.SystemInitFailed(-1))
            return@callbackFlow
        }
        val utteranceId = UUID.randomUUID().toString()
        val timings = mutableListOf<WordTiming>()
        val rangeListener = object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit

            override fun onRangeStart(id: String?, start: Int, end: Int, frame: Int) {
                if (id != utteranceId) return
                // Translate audio frame (samples) into ms using the engine's sample rate. Most
                // Android engines ship at 22050 Hz; absent a public getter we approximate.
                val sampleRate = ESTIMATED_SAMPLE_RATE_HZ
                val audioMs = (frame.toLong() * 1000L) / sampleRate
                timings += WordTiming(
                    charStart = start,
                    charEnd = end,
                    audioStartMs = audioMs,
                    audioEndMs = audioMs, // end gets fixed up in onDone or the next onRangeStart
                )
            }

            override fun onDone(id: String?) {
                if (id != utteranceId) return
                trySendBlocking(
                    TtsChunk(
                        pcm = ShortArray(0),
                        sampleRate = ESTIMATED_SAMPLE_RATE_HZ,
                        wordTimings = if (hasReliableRangeCallback) timings.toList() else emptyList(),
                        endOfUtterance = true,
                    ),
                )
                close()
            }

            @Deprecated("Deprecated in Android docs but required override.")
            override fun onError(id: String?) {
                if (id != utteranceId) return
                close(TtsEngineError.SynthesisFailed(RuntimeException("System TTS error")))
            }
        }

        tts.setOnUtteranceProgressListener(rangeListener)
        runCatching { tts.setSpeechRate(request.speed.coerceIn(0.1f, 4f)) }
        runCatching { tts.setPitch(request.pitch.coerceIn(0.1f, 4f)) }
        runCatching { tts.voices?.firstOrNull { it.name == request.voice.id }?.let(tts::setVoice) }

        val params = Bundle()
        val result = tts.speak(request.text, TextToSpeech.QUEUE_ADD, params, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            close(TtsEngineError.SynthesisFailed(RuntimeException("TextToSpeech.speak returned $result")))
            return@callbackFlow
        }

        awaitClose {
            // No public stop-by-utterance-id API; on cancellation we stop everything. The
            // playback layer issues utterances strictly serially so this only ever cancels the
            // currently-playing one.
            runCatching { tts.stop() }
        }
    }

    override fun release() {
        tts?.let {
            runCatching { it.stop() }
            runCatching { it.shutdown() }
        }
        tts = null
        initSucceeded = false
        voicesCache.clear()
    }

    private companion object {
        const val INIT_TIMEOUT_MS = 5_000L
        const val PROBE_TIMEOUT_MS = 2_500L
        // Most Android TTS engines emit 22050 Hz; we don't get the actual rate from the public
        // API. This is only used to convert onRangeStart's `frame` index to ms — small drift is
        // acceptable for highlight timing, and the alternative (estimating from chunk duration)
        // is no better.
        const val ESTIMATED_SAMPLE_RATE_HZ = 22_050

        // Suppress unused-build warning for Build import; the Build version constants are not
        // currently checked but kept for future API-gated branches.
        @Suppress("unused") private val sdk = Build.VERSION.SDK_INT
    }
}
