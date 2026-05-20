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
import yokai.domain.ui.settings.ReaderPreferences

/**
 * Owns the TTS state machine. Every play / pause / skip / chapter-advance / engine event
 * is funnelled through a single [Channel] consumed serially by [processCommands], which
 * is the choke point that eliminates the threading races present in the old
 * viewer-owned implementation.
 *
 * Phase 1: command loop wired, state transitions are no-ops. Phase 2 fills in engine +
 * audio focus and unblocks single-chapter playback. Phase 3 wires the viewer as the
 * [TtsSource]. Phase 4 enables chapter advance. Phase 5 brings MediaSession in.
 */
class NovelTtsController(
    context: Context,
    @Suppress("UnusedPrivateProperty") private val preferences: ReaderPreferences,
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
        when (val s = _state.value) {
            is TtsState.Idle, is TtsState.Error -> dispatch(TtsCommand.Play)
            is TtsState.Paused -> dispatch(TtsCommand.Resume)
            is TtsState.Speaking -> dispatch(TtsCommand.Pause)
            is TtsState.Preparing, is TtsState.AdvancingChapter -> {
                // Mid-transition: queue a stop so a second tap reliably halts the engine.
                dispatch(TtsCommand.Stop)
            }
        }
    }

    fun shutdown() {
        commandJob?.cancel()
        engineEventJob?.cancel()
        commands.close()
        engine.shutdown()
        audioFocus.abandon()
        mediaSession.release()
        scope.cancel()
    }

    private fun start() {
        commandJob = scope.launch {
            for (cmd in commands) {
                try {
                    handle(cmd)
                } catch (t: Throwable) {
                    Logger.e(t) { "TTS: command handler crashed on $cmd" }
                    _state.value = TtsState.Error(t.message ?: t::class.simpleName.orEmpty())
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
                    is UtteranceEvent.Done ->
                        utteranceIdToParagraph(event.utteranceId)?.let {
                            dispatch(TtsCommand.InternalParagraphDone(it))
                        }
                    is UtteranceEvent.Error ->
                        dispatch(TtsCommand.InternalEngineError(event.message))
                }
            }
        }
    }

    private suspend fun handle(cmd: TtsCommand) {
        // Phase 1: command loop is wired but every branch is a no-op until Phase 2 fills
        // in engine.speak / audioFocus.request / etc. Logging the dispatch so we can
        // verify the loop is alive during Phase 1 manual testing.
        Logger.d { "TTS: handle $cmd (state=${_state.value})" }
    }

    private fun utteranceIdToParagraph(utteranceId: String): Int? {
        // Format: "tts-p<paragraphIndex>-u<utteranceIndex>" — paragraph is the second token.
        val match = UTTERANCE_ID_REGEX.matchEntire(utteranceId) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    companion object {
        private val UTTERANCE_ID_REGEX = Regex("""tts-p(\d+)-u\d+""")
        fun makeUtteranceId(paragraphIndex: Int, utteranceIndex: Int) =
            "tts-p$paragraphIndex-u$utteranceIndex"
    }
}
