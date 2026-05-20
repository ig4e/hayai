package hayai.novel.reader.tts

/**
 * The single source of truth for the novel TTS feature. Owned by [NovelTtsController];
 * every other consumer (action-bar icon, settings sheet, notification, MediaSession) reads
 * from `controller.state` and never holds its own copy.
 */
sealed class TtsState {
    object Idle : TtsState()

    /** Engine bootstrapping or waiting on a chapter's paragraphs. */
    object Preparing : TtsState()

    data class Speaking(
        val chapterId: Long,
        val paragraphIndex: Int,
        val totalParagraphs: Int,
    ) : TtsState()

    data class Paused(
        val chapterId: Long,
        val paragraphIndex: Int,
        val totalParagraphs: Int,
    ) : TtsState()

    /** Waiting for the source to finish loading the next/previous chapter. */
    object AdvancingChapter : TtsState()

    data class Error(val reason: String) : TtsState()
}

/**
 * User intents + internal events, all funnelled through a single channel inside
 * [NovelTtsController] so that user input and utterance callbacks are serialised.
 */
sealed class TtsCommand {
    /** Start from the visible chapter's first-visible paragraph (bottom-bar Play). */
    object Play : TtsCommand()

    /** Start at a specific chapter + paragraph (sentence-tap / resume-reading entry). */
    data class StartAt(val chapterId: Long, val paragraphIndex: Int) : TtsCommand()

    object Pause : TtsCommand()
    object Resume : TtsCommand()
    object Stop : TtsCommand()

    /** Jump one paragraph forward / backward within the current chapter. */
    object SkipParagraph : TtsCommand()
    object PreviousParagraph : TtsCommand()

    /** Explicit chapter-level navigation; goes through the source's await* path. */
    object NextChapter : TtsCommand()
    object PreviousChapter : TtsCommand()

    /** Reapply voice/pitch/rate/engine — typically when the user changes a setting. */
    object ReapplySettings : TtsCommand()

    /** Engine emitted a fatal error; controller routes to TtsState.Error. */
    data class InternalEngineError(val message: String) : TtsCommand()

    /** Engine finished the last queued utterance for paragraph [paragraphIndex]. */
    data class InternalParagraphDone(val paragraphIndex: Int) : TtsCommand()

    /** Engine started the first utterance for paragraph [paragraphIndex]. */
    data class InternalParagraphStarted(val paragraphIndex: Int) : TtsCommand()
}
