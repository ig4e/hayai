package hayai.novel.reader.tts

import android.content.Context
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import yokai.domain.ui.settings.ReaderPreferences

/**
 * Owns the TTS state machine. Every play / pause / skip / chapter-advance / engine
 * event is funnelled through a single [Channel] consumed serially by the command loop,
 * which is the choke point that eliminates the threading races the legacy viewer-owned
 * implementation had.
 *
 * Audio focus is acquired on play and abandoned on stop. Paragraph-level progress is
 * persisted to [ReaderPreferences.novelTtsLastReadParagraph] on every paragraph
 * transition. End-of-chapter auto-advance routes through [TtsSource.awaitNextChapterReady]
 * which suspends on real chapter-Ready signals — no fixed-duration delays.
 */
class NovelTtsController(
    context: Context,
    private val preferences: ReaderPreferences,
) {
    val engine = NovelTtsEngine(context)
    val audioFocus = NovelTtsAudioFocus(context)
    val mediaSession = NovelTtsMediaSession(context)

    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    val state: StateFlow<TtsState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val commands = Channel<TtsCommand>(capacity = Channel.UNLIMITED)

    @Volatile private var source: TtsSource? = null
    private var commandJob: Job? = null
    private var engineEventJob: Job? = null
    private var focusJob: Job? = null
    private var stateUpdateJob: Job? = null

    /**
     * Listener invoked whenever the state changes and the MediaSession/notification need
     * to be refreshed. The service registers a single listener that rebuilds the
     * foreground notification — this replaces the legacy 750 ms polling loop.
     */
    fun interface StateListener {
        fun onStateChanged(state: TtsState, novelTitle: String, chapterTitle: String, mangaId: Long, chapterId: Long)
    }
    @Volatile private var stateListener: StateListener? = null
    fun setStateListener(listener: StateListener?) { stateListener = listener }

    /**
     * Active playback session, or `null` when idle. The whole object is replaced on each
     * `Play / StartAt / NextChapter` so the controller never partially updates a session.
     */
    private data class Session(
        val chapterId: Long,
        val paragraphs: List<String>,
        var paragraphIndex: Int,
        var lastPersistedParagraph: Int,
    )

    private var session: Session? = null

    init {
        start()
    }

    fun attachSource(source: TtsSource) {
        this.source = source
    }

    fun detachSource(source: TtsSource) {
        if (this.source === source) this.source = null
    }

    /** Enqueue a user intent. Returns immediately; the loop consumes serially. */
    fun dispatch(command: TtsCommand) {
        val result = commands.trySend(command)
        if (result.isFailure) {
            Logger.w { "TTS: command channel rejected $command (${result.exceptionOrNull()?.message})" }
        }
    }

    /** Convenience for the action-bar toggle (Play / Pause / Resume from a single tap). */
    fun toggle() {
        when (_state.value) {
            is TtsState.Idle, is TtsState.Error -> dispatch(TtsCommand.Play)
            is TtsState.Paused -> dispatch(TtsCommand.Resume)
            is TtsState.Speaking -> dispatch(TtsCommand.Pause)
            is TtsState.Preparing, is TtsState.AdvancingChapter -> dispatch(TtsCommand.Stop)
        }
    }

    fun shutdown() {
        commandJob?.cancel()
        engineEventJob?.cancel()
        focusJob?.cancel()
        stateUpdateJob?.cancel()
        commands.close()
        engine.shutdown()
        audioFocus.abandon()
        mediaSession.release()
        scope.cancel()
    }

    private fun start() {
        mediaSession.ensure { cmd -> dispatch(cmd) }

        commandJob = scope.launch {
            for (cmd in commands) {
                try {
                    handle(cmd)
                } catch (t: Throwable) {
                    Logger.e(t) { "TTS: command handler crashed on $cmd" }
                    _state.value = TtsState.Error(t.message ?: t::class.simpleName.orEmpty())
                    cleanupSession(stopEngine = true)
                }
            }
        }

        engineEventJob = scope.launch {
            engine.events.collect { event ->
                when (event) {
                    is UtteranceEvent.Started ->
                        utteranceIdToParagraph(event.utteranceId)?.let {
                            dispatch(TtsCommand.InternalParagraphStarted(it))
                        }
                    is UtteranceEvent.Done -> {
                        val parts = utteranceIdToParts(event.utteranceId) ?: return@collect
                        // Only emit ParagraphDone on the LAST chunk so the controller
                        // doesn't advance to the next paragraph while earlier chunks of
                        // a long paragraph are still queued in the engine.
                        if (parts.utteranceIndex == parts.totalChunks - 1) {
                            dispatch(TtsCommand.InternalParagraphDone(parts.paragraphIndex))
                        }
                    }
                    is UtteranceEvent.Error ->
                        dispatch(TtsCommand.InternalEngineError(event.message))
                }
            }
        }

        focusJob = scope.launch {
            audioFocus.focusEvents.collect { event ->
                when (event) {
                    FocusEvent.Gained -> {
                        if (_state.value is TtsState.Paused) dispatch(TtsCommand.Resume)
                    }
                    FocusEvent.LostTransient,
                    FocusEvent.LostTransientCanDuck -> dispatch(TtsCommand.Pause)
                    FocusEvent.Lost -> dispatch(TtsCommand.Stop)
                }
            }
        }

        // Push every state change into the MediaSession + the service's notification, so
        // the lockscreen, the foreground notification and the Compose action bar all
        // observe the same source of truth and never drift.
        stateUpdateJob = scope.launch {
            state.collect { current ->
                val src = source
                val chapterId = when (current) {
                    is TtsState.Speaking -> current.chapterId
                    is TtsState.Paused -> current.chapterId
                    else -> src?.currentVisibleChapterId() ?: -1L
                }
                val novelTitle = src?.novelTitle().orEmpty().ifBlank { "TTS playback" }
                val chapterTitle = src?.chapterTitle(chapterId).orEmpty()
                mediaSession.update(current, novelTitle, chapterTitle)
                stateListener?.onStateChanged(
                    state = current,
                    novelTitle = novelTitle,
                    chapterTitle = chapterTitle,
                    mangaId = -1L,
                    chapterId = chapterId,
                )
            }
        }
    }

    /** Used by [NovelTtsMediaSession] to render the notification with lockscreen art. */
    fun mediaSessionToken() = mediaSession.sessionToken

    private suspend fun handle(cmd: TtsCommand) {
        when (cmd) {
            is TtsCommand.Play -> onPlay()
            is TtsCommand.StartAt -> onStartAt(cmd.chapterId, cmd.paragraphIndex)
            is TtsCommand.Pause -> onPause()
            is TtsCommand.Resume -> onResume()
            is TtsCommand.Stop -> onStop()
            is TtsCommand.SkipParagraph -> onSkipParagraph(delta = 1)
            is TtsCommand.PreviousParagraph -> onSkipParagraph(delta = -1)
            is TtsCommand.NextChapter -> onAdvanceChapter(forward = true)
            is TtsCommand.PreviousChapter -> onAdvanceChapter(forward = false)
            is TtsCommand.ReapplySettings -> applyEngineSettings()
            is TtsCommand.InternalParagraphStarted -> onParagraphStarted(cmd.paragraphIndex)
            is TtsCommand.InternalParagraphDone -> onParagraphDone(cmd.paragraphIndex)
            is TtsCommand.InternalEngineError -> onEngineError(cmd.message)
        }
    }

    // -------------------------------------------------------------------- Commands

    private suspend fun onPlay() {
        val src = source ?: run {
            Logger.w { "TTS: Play with no source attached — ignoring" }
            return
        }
        val chapterId = src.currentVisibleChapterId() ?: run {
            _state.value = TtsState.Error("No current chapter visible")
            return
        }
        val paragraphIndex = src.currentVisibleParagraphIndex() ?: 0
        onStartAt(chapterId, paragraphIndex)
    }

    private suspend fun onStartAt(chapterId: Long, paragraphIndex: Int) {
        val src = source ?: run {
            Logger.w { "TTS: StartAt with no source attached — ignoring" }
            return
        }
        _state.value = TtsState.Preparing
        if (!engine.ensureInitialized(preferences.novelTtsEngine.get().ifBlank { null })) {
            _state.value = TtsState.Error("TTS engine unavailable")
            return
        }
        applyEngineSettings()

        val paragraphs = src.awaitChapterParagraphs(chapterId) ?: run {
            _state.value = TtsState.Error("Chapter text unavailable")
            return
        }
        if (paragraphs.isEmpty()) {
            _state.value = TtsState.Error("Chapter has no readable paragraphs")
            return
        }

        if (!audioFocus.request()) {
            _state.value = TtsState.Error("Could not acquire audio focus")
            return
        }

        val startIndex = paragraphIndex.coerceIn(0, paragraphs.size - 1)
        session = Session(
            chapterId = chapterId,
            paragraphs = paragraphs,
            paragraphIndex = startIndex,
            lastPersistedParagraph = -1,
        )
        speakCurrentParagraph(flush = true)
    }

    private fun onPause() {
        val s = session ?: return
        engine.stop()
        _state.value = TtsState.Paused(s.chapterId, s.paragraphIndex, s.paragraphs.size)
    }

    private fun onResume() {
        val s = session ?: return
        // Re-acquire focus if it was lost while paused.
        if (!audioFocus.request()) {
            _state.value = TtsState.Error("Could not acquire audio focus")
            return
        }
        speakCurrentParagraph(flush = true)
    }

    private fun onStop() {
        cleanupSession(stopEngine = true)
        _state.value = TtsState.Idle
    }

    private fun onSkipParagraph(delta: Int) {
        val s = session ?: return
        val next = (s.paragraphIndex + delta).coerceIn(0, s.paragraphs.size - 1)
        if (next == s.paragraphIndex) {
            // At a boundary — stop for previous-from-first, halt cleanly for next-from-last.
            if (delta > 0) onStop() else return
            return
        }
        s.paragraphIndex = next
        speakCurrentParagraph(flush = true)
    }

    private fun onParagraphStarted(paragraphIndex: Int) {
        val s = session ?: return
        if (s.paragraphIndex != paragraphIndex) {
            // Stale callback (e.g. delayed Started after a skip flushed the queue): ignore.
            return
        }
        _state.value = TtsState.Speaking(s.chapterId, paragraphIndex, s.paragraphs.size)
        source?.paintHighlight(s.chapterId, paragraphIndex)
        persistProgress(s, paragraphIndex)
    }

    private suspend fun onParagraphDone(paragraphIndex: Int) {
        val s = session ?: return
        if (s.paragraphIndex != paragraphIndex) return

        val nextIndex = paragraphIndex + 1
        if (nextIndex >= s.paragraphs.size) {
            // End of chapter — auto-advance if the user has the pref on; otherwise stop
            // cleanly so the action-bar icon and notification reflect "idle".
            if (preferences.novelTtsAutoNextChapter.get()) {
                onAdvanceChapter(forward = true)
            } else {
                onStop()
            }
            return
        }
        s.paragraphIndex = nextIndex
        speakCurrentParagraph(flush = false)
    }

    private fun onEngineError(message: String) {
        Logger.e { "TTS: engine error — $message" }
        _state.value = TtsState.Error(message)
        cleanupSession(stopEngine = true)
    }

    /**
     * Move to the next (or previous) chapter and start at paragraph 0. Triggered both by
     * end-of-chapter auto-advance and by an explicit user [TtsCommand.NextChapter] /
     * [TtsCommand.PreviousChapter]. Uses the source's `awaitNext/PreviousChapterReady`
     * which suspends until the new chapter's tagged paragraph list is actually present —
     * eliminating the hardcoded `delay(1000)` race the legacy implementation had.
     *
     * If no adjacent chapter is available (start/end of novel), playback stops cleanly.
     */
    private suspend fun onAdvanceChapter(forward: Boolean) {
        val src = source ?: run {
            Logger.w { "TTS: AdvanceChapter with no source attached" }
            return
        }
        // Tear down the engine and audio focus from the just-finished chapter before we
        // attempt the next start. `cleanupSession` clears the highlight too, which would
        // otherwise stay anchored on the last paragraph of the previous chapter while we
        // suspend on chapter load.
        cleanupSession(stopEngine = true)
        _state.value = TtsState.AdvancingChapter

        val newId = if (forward) src.awaitNextChapterReady() else src.awaitPreviousChapterReady()
        if (newId == null) {
            Logger.d { "TTS: no ${if (forward) "next" else "previous"} chapter; stopping" }
            _state.value = TtsState.Idle
            return
        }

        val paragraphs = src.awaitChapterParagraphs(newId)
        if (paragraphs.isNullOrEmpty()) {
            _state.value = TtsState.Error("Chapter $newId has no readable paragraphs")
            return
        }

        if (!audioFocus.request()) {
            _state.value = TtsState.Error("Could not acquire audio focus")
            return
        }

        session = Session(
            chapterId = newId,
            paragraphs = paragraphs,
            paragraphIndex = 0,
            lastPersistedParagraph = -1,
        )
        speakCurrentParagraph(flush = true)
    }

    // ------------------------------------------------------------------ Internals

    private fun speakCurrentParagraph(flush: Boolean) {
        val s = session ?: return
        val text = s.paragraphs.getOrNull(s.paragraphIndex) ?: run {
            onStop()
            return
        }
        engine.speakParagraph(s.paragraphIndex, text, flush)
    }

    private fun applyEngineSettings() {
        engine.applySettings(
            voiceTag = preferences.novelTtsVoice.get(),
            pitch = preferences.novelTtsPitch.get(),
            speechRate = preferences.novelTtsSpeed.get(),
            engineName = preferences.novelTtsEngine.get(),
        )
    }

    /**
     * Persists the in-progress paragraph for this chapter so a later "resume reading"
     * entry can jump straight there. Writes only on actual paragraph transitions to
     * avoid the chunk-level drift the old implementation had.
     */
    private fun persistProgress(s: Session, paragraphIndex: Int) {
        if (s.lastPersistedParagraph == paragraphIndex) return
        s.lastPersistedParagraph = paragraphIndex

        val current = preferences.novelTtsLastReadParagraph.get()
        val map = try {
            JSON.decodeFromString<Map<String, Int>>(current).toMutableMap()
        } catch (_: Exception) {
            mutableMapOf()
        }
        map[s.chapterId.toString()] = paragraphIndex
        try {
            preferences.novelTtsLastReadParagraph.set(JSON.encodeToString(map))
        } catch (e: Exception) {
            Logger.w(e) { "TTS: failed to persist paragraph progress" }
        }
    }

    private fun cleanupSession(stopEngine: Boolean) {
        if (stopEngine) engine.stop()
        audioFocus.abandon()
        source?.clearHighlight()
        session = null
    }

    /**
     * Identifying parts of an utterance id. The engine encodes `paragraphIndex`,
     * `utteranceIndex` (chunk number within a paragraph), and `totalChunks` (the count
     * of chunks the paragraph was split into) so the controller knows exactly when a
     * paragraph's final chunk has finished — critical for paragraphs longer than
     * `TextToSpeech.getMaxSpeechInputLength()` that get split internally.
     */
    private data class UtteranceParts(
        val paragraphIndex: Int,
        val utteranceIndex: Int,
        val totalChunks: Int,
    )

    private fun utteranceIdToParagraph(utteranceId: String): Int? =
        utteranceIdToParts(utteranceId)?.paragraphIndex

    private fun utteranceIdToParts(utteranceId: String): UtteranceParts? {
        val match = UTTERANCE_ID_REGEX.matchEntire(utteranceId) ?: return null
        val paragraph = match.groupValues[1].toIntOrNull() ?: return null
        val utterance = match.groupValues[2].toIntOrNull() ?: return null
        val total = match.groupValues[3].toIntOrNull()?.coerceAtLeast(1) ?: return null
        return UtteranceParts(paragraph, utterance, total)
    }

    companion object {
        private val UTTERANCE_ID_REGEX = Regex("""tts-p(\d+)-u(\d+)-of(\d+)""")
        private val JSON = Json { ignoreUnknownKeys = true }

        fun makeUtteranceId(paragraphIndex: Int, utteranceIndex: Int, totalChunks: Int) =
            "tts-p$paragraphIndex-u$utteranceIndex-of$totalChunks"
    }
}
