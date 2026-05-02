package hayai.novel.tts.playback

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.Immutable
import co.touchlab.kermit.Logger
import hayai.novel.preferences.NovelPreferences
import hayai.novel.reader.NovelBlock
import hayai.novel.tts.engine.TtsChunk
import hayai.novel.tts.engine.TtsEngine
import hayai.novel.tts.engine.TtsEngineFactory
import hayai.novel.tts.engine.TtsRequest
import hayai.novel.tts.engine.TtsVoice
import hayai.novel.tts.engine.WordTiming
import hayai.novel.tts.engine.audio.TtsAudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Sequences TTS playback for a chapter. Owns the active engine, the audio player, the sentence
 * queue, and the highlight scheduler.
 *
 * Lifecycle:
 *  - [load] takes the chapter's [NovelBlock] list (whatever's currently rendered) and prepares
 *    the segmenter + sentence queue. Doesn't start audio.
 *  - [play] kicks off synthesis from [fromSentenceIndex]; audio + highlights stream until the
 *    chapter ends (or the user pauses).
 *  - [pause] / [resume] toggle the audio player; the engine keeps any in-flight synthesis going
 *    so resume is instant.
 *  - [stop] tears everything down — engine.release(), player.stop(), highlight cleared.
 *
 * Cross-chapter advance: when the last sentence completes and `continuous` is true, the host
 * (TtsPlaybackService) is responsible for calling [load] again with the next chapter and
 * `play(0)`.
 */
class TtsPlaybackController(
    private val context: Context,
    private val novelPreferences: NovelPreferences,
    private val engineFactory: TtsEngineFactory,
    private val highlightDispatcher: HighlightDispatcher,
    private val onSentenceComplete: () -> Unit = {},
    private val onChapterComplete: () -> Unit = {},
    private val onError: (Throwable) -> Unit = {},
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val audioPlayer = TtsAudioPlayer()
    private val segmenter = SentenceSegmenter()
    private val audioFocus = TtsAudioFocus(
        context = context,
        onPermanentLoss = ::stop,
        onTransientLoss = ::pause,
        onResumeAfterLoss = ::resume,
    )

    private var engine: TtsEngine? = null
    private var sentences: List<SentenceSpan> = emptyList()
    private var flatChapter: FlatChapter = FlatChapter("", emptyList())
    private var playbackJob: Job? = null

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    /** Resolves a tap's flat-text offset to its containing sentence index. */
    fun sentenceIndexForOffset(flatOffset: Int): Int {
        return sentences.indexOfFirst { flatOffset in it.charStart until it.charEnd }
            .takeIf { it >= 0 } ?: 0
    }

    /** Builds the segmenter view of a chapter; clears any in-flight playback. */
    fun load(blocks: List<NovelBlock>) {
        stop()
        flatChapter = segmenter.flatten(blocks)
        sentences = segmenter.segment(flatChapter)
        _state.update { it.copy(totalSentences = sentences.size, currentSentenceIndex = 0) }
    }

    /** Starts synthesis from the given sentence; safe to call while paused or stopped. */
    fun play(fromSentenceIndex: Int = 0) {
        if (sentences.isEmpty()) return
        playbackJob?.cancel()
        if (!audioFocus.request()) {
            onError(RuntimeException("Audio focus denied"))
            return
        }
        val startIndex = fromSentenceIndex.coerceIn(0, sentences.lastIndex)
        _state.update { it.copy(isPlaying = true, currentSentenceIndex = startIndex) }
        playbackJob = scope.launch { runPlayback(startIndex) }
    }

    fun pause() {
        if (!_state.value.isPlaying) return
        audioPlayer.pause()
        _state.update { it.copy(isPlaying = false, isPaused = true) }
    }

    fun resume() {
        if (_state.value.isPaused) {
            audioPlayer.resume()
            _state.update { it.copy(isPlaying = true, isPaused = false) }
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        audioPlayer.stop()
        engine?.release()
        engine = null
        audioFocus.abandon()
        highlightDispatcher.clear()
        _state.update { State(totalSentences = sentences.size) }
    }

    fun release() {
        stop()
        scope.cancel()
    }

    /** Volume passes straight through to the audio player. */
    fun setVolume(volume: Float) = audioPlayer.setVolume(volume)

    private suspend fun runPlayback(startIndex: Int) {
        val active = ensureEngine() ?: return
        try {
            val voice = pickVoice(active) ?: run {
                onError(RuntimeException("No voice available for engine ${active.id}"))
                return
            }
            val speed = novelPreferences.ttsSpeed().get()
            val pitch = novelPreferences.ttsPitch().get()
            val highlightEnabled = novelPreferences.ttsHighlight().get()
            val sentencePauseMs = novelPreferences.ttsSentencePauseMs().get().toLong()

            for (index in startIndex until sentences.size) {
                _state.update { it.copy(currentSentenceIndex = index) }
                val sentence = sentences[index]
                val request = TtsRequest(text = sentence.text, voice = voice, speed = speed, pitch = pitch)

                synthesizeOneSentence(active, request, sentence, highlightEnabled)
                onSentenceComplete()
                if (sentencePauseMs > 0) delay(sentencePauseMs)
            }
            onChapterComplete()
        } catch (t: Throwable) {
            if (t !is kotlinx.coroutines.CancellationException) onError(t)
        } finally {
            highlightDispatcher.clear()
        }
    }

    private suspend fun synthesizeOneSentence(
        engine: TtsEngine,
        request: TtsRequest,
        sentence: SentenceSpan,
        highlightEnabled: Boolean,
    ) {
        var firstChunkAt: Long = -1L
        engine.synthesize(request).collect { chunk ->
            if (firstChunkAt < 0) firstChunkAt = SystemClock.uptimeMillis()
            if (chunk.pcm.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    audioPlayer.writeChunk(chunk.pcm, chunk.sampleRate)
                }
            }
            if (highlightEnabled && chunk.wordTimings.isNotEmpty()) {
                scheduleHighlights(sentence, chunk.wordTimings)
            }
        }
    }

    /**
     * Maps each [WordTiming]'s sentence-relative char range back to the flat chapter's coords,
     * resolves the owning block + offset within block via the segmenter, and posts a highlight
     * at the right wall-clock time relative to playback start.
     */
    private fun scheduleHighlights(sentence: SentenceSpan, timings: List<WordTiming>) {
        for (timing in timings) {
            val flatStart = sentence.charStart + timing.charStart
            val flatEnd = sentence.charStart + timing.charEnd
            val target = segmenter.resolveCharOffset(flatChapter, flatStart) ?: continue
            val blockEnd = segmenter.resolveCharOffset(flatChapter, flatEnd - 1) ?: target
            // If the word straddles two blocks (rare — long word at a paragraph join), highlight
            // only the first block's portion to keep the UI consistent.
            if (target.blockIndex != blockEnd.blockIndex) continue

            val delayUntil = timing.audioStartMs - audioPlayer.elapsedMs()
            scope.launch {
                if (delayUntil > 0) delay(delayUntil)
                highlightDispatcher.highlight(
                    blockIndex = target.blockIndex,
                    start = target.offsetInBlock,
                    end = target.offsetInBlock + (timing.charEnd - timing.charStart),
                )
            }
        }
    }

    private suspend fun ensureEngine(): TtsEngine? {
        engine?.let { return it }
        val newEngine = engineFactory.create()
        val result = newEngine.prepare()
        if (result.isFailure) {
            onError(result.exceptionOrNull() ?: RuntimeException("Engine prepare failed"))
            return null
        }
        engine = newEngine
        return newEngine
    }

    private fun pickVoice(engine: TtsEngine): TtsVoice? {
        val saved = novelPreferences.ttsVoiceId().get()
        return engine.voices.firstOrNull { it.id == saved } ?: engine.voices.firstOrNull()
    }

    @Immutable
    data class State(
        val isPlaying: Boolean = false,
        val isPaused: Boolean = false,
        val currentSentenceIndex: Int = 0,
        val totalSentences: Int = 0,
    )
}
