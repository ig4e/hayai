package hayai.novel.tts.engine.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Streaming PCM player wrapping [AudioTrack]. Engines emit [hayai.novel.tts.engine.TtsChunk]s of
 * 16-bit signed mono PCM; the playback controller hands each chunk to [writeChunk]. The track is
 * created lazily for the requested sample rate so we don't pay the allocation cost until audio
 * actually starts.
 *
 * State machine:
 *  - Idle (no track, nothing queued)
 *  - Playing (track in PLAYSTATE_PLAYING, writes flow through)
 *  - Paused (track in PLAYSTATE_PAUSED, writes block)
 *
 * [pause]/[resume] toggle PLAYSTATE without dropping queued audio. [stop] flushes the track and
 * frees it; the next [writeChunk] re-creates one.
 *
 * Tracks elapsed playback time via [framesWrittenSoFar] / [sampleRate] so the highlight
 * dispatcher can convert engine-supplied audio offsets to wall-clock delays.
 */
class TtsAudioPlayer {

    private var track: AudioTrack? = null
    private var trackSampleRate: Int = -1
    private var framesWritten: Long = 0L
    private var pauseStartMs: Long = -1L
    private var startUptimeMs: Long = -1L
    private var totalPausedMs: Long = 0L

    private val _state = MutableStateFlow(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    /**
     * Writes one PCM chunk. Auto-creates a fresh [AudioTrack] for the chunk's sample rate if
     * none exists yet (or if the rate changed mid-stream — shouldn't happen but cheap to guard).
     * Blocks until the track has buffer space.
     */
    fun writeChunk(pcm: ShortArray, sampleRate: Int) {
        if (pcm.isEmpty()) return
        ensureTrack(sampleRate)
        val t = track ?: return
        if (_state.value == State.Idle) {
            _state.value = State.Playing
            startUptimeMs = SystemClock.uptimeMillis()
            totalPausedMs = 0L
            t.play()
        }
        // Loop because AudioTrack.write may return partial counts on pre-Android 8 devices.
        var written = 0
        while (written < pcm.size) {
            val n = t.write(pcm, written, pcm.size - written, AudioTrack.WRITE_BLOCKING)
            if (n <= 0) break
            written += n
        }
        framesWritten += written
    }

    fun pause() {
        if (_state.value != State.Playing) return
        track?.pause()
        pauseStartMs = SystemClock.uptimeMillis()
        _state.value = State.Paused
    }

    fun resume() {
        if (_state.value != State.Paused) return
        track?.play()
        if (pauseStartMs >= 0) {
            totalPausedMs += SystemClock.uptimeMillis() - pauseStartMs
            pauseStartMs = -1L
        }
        _state.value = State.Playing
    }

    /**
     * Stops the track, drops any queued audio, and releases the underlying [AudioTrack]. After
     * this returns the player is back in [State.Idle] and a new track will be allocated on the
     * next [writeChunk].
     */
    fun stop() {
        track?.let { t ->
            runCatching { t.pause() }
            runCatching { t.flush() }
            runCatching { t.release() }
        }
        track = null
        trackSampleRate = -1
        framesWritten = 0L
        pauseStartMs = -1L
        startUptimeMs = -1L
        totalPausedMs = 0L
        _state.value = State.Idle
    }

    fun setVolume(volume: Float) {
        track?.setVolume(volume.coerceIn(0f, 1f))
    }

    /**
     * Returns elapsed audio playback time in ms for the current utterance, accounting for any
     * pauses. The highlight dispatcher uses this to align word transitions to current playback
     * progress.
     */
    fun elapsedMs(): Long {
        if (startUptimeMs < 0) return 0L
        val now = SystemClock.uptimeMillis()
        val pausedSoFar = totalPausedMs + (if (pauseStartMs >= 0) now - pauseStartMs else 0)
        return (now - startUptimeMs - pausedSoFar).coerceAtLeast(0L)
    }

    private fun ensureTrack(sampleRate: Int) {
        if (track != null && trackSampleRate == sampleRate && _state.value != State.Idle) return
        if (track != null) {
            // Sample rate changed or we were idle — recreate.
            stop()
        }
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(sampleRate / 2 * 2) // at least 0.5 s of audio for stable streaming

        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(minBuffer)
            .build()
        trackSampleRate = sampleRate
        framesWritten = 0L
    }

    enum class State { Idle, Playing, Paused }
}
