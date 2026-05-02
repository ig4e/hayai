package hayai.novel.tts.engine

import kotlinx.coroutines.flow.Flow

/**
 * One TTS backend implementation. The playback layer talks to engines exclusively through this
 * interface so it doesn't care which underlying TTS service produces the audio.
 *
 * Engines are stateful — [prepare] loads heavy resources, [synthesize] streams chunks for one
 * utterance, and [release] frees everything when the user closes the reader. Engines own no
 * shared state between utterances; the playback layer drives sequencing.
 */
interface TtsEngine {

    /** Stable identifier persisted to preferences. */
    val id: String

    /** User-facing name shown in the settings screen. */
    val displayName: String

    /** Voices the user can pick within this engine. May be empty if [prepare] hasn't run yet. */
    val voices: List<TtsVoice>

    /** True iff this engine respects the [TtsRequest.pitch] argument. UIs disable the pitch
     *  slider when this is false. */
    val supportsPitch: Boolean

    /**
     * Loads engine resources. Idempotent — repeat calls after success no-op. May suspend on the
     * IO dispatcher. Returns success on a fully-ready engine; failure carries a user-facing
     * reason ([TtsEngineError]).
     */
    suspend fun prepare(): Result<Unit>

    /**
     * Streams synthesized audio for one utterance. Each emitted [TtsChunk] is a slice of PCM the
     * playback layer writes into [android.media.AudioTrack] in order. The flow completes when the
     * utterance is fully synthesized; the final chunk has `endOfUtterance = true`.
     *
     * Cancellation propagates to the engine — the underlying synthesis task is aborted and any
     * partial state is torn down before the flow returns.
     */
    fun synthesize(request: TtsRequest): Flow<TtsChunk>

    /** Releases engine resources. The instance is unusable after this until [prepare] is called again. */
    fun release()
}

/**
 * Single utterance request. Speed and pitch are normalized rates (1.0 = unchanged); pitch is
 * ignored by engines whose [TtsEngine.supportsPitch] returns false.
 */
data class TtsRequest(
    val text: String,
    val voice: TtsVoice,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
)

sealed class TtsEngineError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** Android system TTS engine returned an init error. */
    class SystemInitFailed(code: Int) : TtsEngineError("Android TTS init failed with code $code")

    /** Generic synthesis failure — wraps the underlying cause for diagnostics. */
    class SynthesisFailed(cause: Throwable) : TtsEngineError("TTS synthesis failed: ${cause.message}", cause)
}
