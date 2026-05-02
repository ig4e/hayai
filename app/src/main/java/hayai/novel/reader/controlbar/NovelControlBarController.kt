package hayai.novel.reader.controlbar

import android.app.Activity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.databinding.ReaderActivityBinding
import hayai.novel.preferences.NovelPreferences
import hayai.novel.reader.NovelViewer
import hayai.novel.tts.engine.TtsEngineFactory
import hayai.novel.tts.playback.HighlightDispatcher
import hayai.novel.tts.playback.TtsPlaybackController
import hayai.novel.tts.ui.TtsLaunchSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Drives the floating [NovelControlBar] from inside [eu.kanade.tachiyomi.ui.reader.ReaderActivity].
 *
 *  - Visible only when the active viewer is a [NovelViewer].
 *  - Owns a [TtsPlaybackController] cached per activity so playback state survives configuration
 *    changes (released from [release]).
 *  - On user toggle: starts/pauses/resumes TTS or auto-scroll.
 *  - On TTS slider drag: seeks to the chosen sentence and (TODO once block-to-textview lookup
 *    lands) scrolls the recycler so the sentence is on screen.
 *  - On TTS long-press: opens [TtsLaunchSheet] for voice/speed configuration.
 */
object NovelControlBarController {

    private val controllers = mutableMapOf<Int, TtsPlaybackController>()

    fun bind(activity: Activity, binding: ReaderActivityBinding, viewer: Any?) {
        val composeView: ComposeView = binding.novelControlBar
        val novelViewer = viewer as? NovelViewer
        if (novelViewer == null) {
            composeView.isVisible = false
            composeView.setContent { /* clear */ }
            return
        }

        val ttsController = controllerFor(activity, novelViewer)
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        composeView.isVisible = true
        composeView.setContent {
            yokai.presentation.theme.YokaiTheme {
                val autoRunning by novelViewer.autoScroller.isRunning.collectAsState()
                val ttsState by ttsController.state.collectAsState()
                NovelControlBar(
                    autoScroll = AutoScrollState(isRunning = autoRunning),
                    tts = TtsControlState(
                        isPlaying = ttsState.isPlaying,
                        isPaused = ttsState.isPaused,
                        currentSentenceIndex = ttsState.currentSentenceIndex,
                        totalSentences = ttsState.totalSentences,
                        secondsPerSentence = SECONDS_PER_SENTENCE_ESTIMATE,
                    ),
                    onToggleAutoScroll = { novelViewer.autoScroller.toggle() },
                    onTogglePlayPause = { toggleTts(ttsController, novelViewer) },
                    onSeek = { idx -> seekTts(ttsController, novelViewer, idx) },
                    onLongPressTts = { TtsLaunchSheet.show(activity) },
                )
            }
        }
    }

    fun setVisible(binding: ReaderActivityBinding, visible: Boolean) {
        // Only honour show requests if the bar has actually been bound (controller is set up).
        // Hide is always allowed.
        if (visible) {
            // Bind decided whether to make it visible — don't override if it was a non-novel viewer.
            return
        }
        binding.novelControlBar.isVisible = false
    }

    fun release(activity: Activity) {
        controllers.remove(System.identityHashCode(activity))?.release()
    }

    private fun toggleTts(controller: TtsPlaybackController, viewer: NovelViewer) {
        val state = controller.state.value
        when {
            state.isPlaying -> controller.pause()
            state.isPaused -> controller.resume()
            else -> {
                val blocks = viewer.currentChapterBlocks() ?: return
                controller.load(blocks)
                controller.play(0)
            }
        }
    }

    private fun seekTts(controller: TtsPlaybackController, viewer: NovelViewer, sentenceIndex: Int) {
        // If TTS hasn't been loaded yet, load it from the current chapter so the seek lands somewhere.
        if (controller.state.value.totalSentences == 0) {
            val blocks = viewer.currentChapterBlocks() ?: return
            controller.load(blocks)
        }
        controller.play(sentenceIndex)
        // TODO once NovelPageHolder exposes per-block TextViews, scroll the recycler to the
        // sentence's TextView here.
    }

    private fun controllerFor(activity: Activity, viewer: NovelViewer): TtsPlaybackController {
        val key = System.identityHashCode(activity)
        controllers[key]?.let { return it }

        val novelPreferences = Injekt.get<NovelPreferences>()
        val engineFactory = Injekt.get<TtsEngineFactory>()
        val highlight = HighlightDispatcher(
            highlightColor = HIGHLIGHT_COLOR,
            findSegment = { blockIndex, charInBlock ->
                viewer.textSegmentForBlock(blockIndex, charInBlock)
            },
        )

        // Forward-declared so the onChapterComplete callback can reference it.
        lateinit var newController: TtsPlaybackController
        newController = TtsPlaybackController(
            context = activity,
            novelPreferences = novelPreferences,
            engineFactory = engineFactory,
            highlightDispatcher = highlight,
            onChapterComplete = {
                onChapterFinished(activity, viewer, newController, novelPreferences)
            },
        )
        controllers[key] = newController
        return newController
    }

    /**
     * Continuous reading hook — when the current chapter finishes, advance to the next one and
     * restart playback at sentence 0. Runs on the activity's lifecycle scope so it cancels when
     * the activity dies. Blocks are re-fetched after the chapter swap completes (we poll because
     * the swap goes through the existing loadNewChapter → setChapters async flow).
     */
    private fun onChapterFinished(
        activity: Activity,
        viewer: NovelViewer,
        controller: TtsPlaybackController,
        novelPreferences: NovelPreferences,
    ) {
        if (!novelPreferences.ttsContinuous().get()) {
            controller.stop()
            return
        }
        val owner = activity as? LifecycleOwner ?: run {
            controller.stop()
            return
        }
        owner.lifecycleScope.launch {
            val advanced = viewer.advanceToNextChapterForTts()
            if (!advanced) {
                controller.stop()
                return@launch
            }
            // Wait up to ~3s for the chapter swap + page load to settle, then restart.
            repeat(MAX_CONTINUE_ATTEMPTS) {
                delay(CONTINUE_POLL_DELAY_MS)
                val blocks = viewer.currentChapterBlocks()
                if (blocks != null) {
                    controller.load(blocks)
                    controller.play(0)
                    return@launch
                }
            }
            controller.stop()
        }
    }

    // Rough estimate — system TTS at 1.0× speech rate averages around 4 seconds per sentence
    // for typical English prose. Used only for the elapsed/total label on the slider; once the
    // controller can report actual playback elapsed time we can swap this for the real number.
    private const val SECONDS_PER_SENTENCE_ESTIMATE = 4.0f
    private const val HIGHLIGHT_COLOR = 0x40FFEB3B.toInt()

    private const val MAX_CONTINUE_ATTEMPTS = 15
    private const val CONTINUE_POLL_DELAY_MS = 250L
}
