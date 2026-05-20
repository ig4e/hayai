package hayai.novel.reader.tts

import android.content.Context

/**
 * Wraps `MediaSessionCompat` for lockscreen / Bluetooth / hardware-media-key support.
 * Phase 1 stub — real session creation and PlaybackStateCompat updates land in Phase 5.
 */
class NovelTtsMediaSession(
    @Suppress("UnusedPrivateProperty") private val context: Context,
) {
    /** Lazy-init the session. Idempotent. */
    fun ensure() {
        // Implemented in Phase 5.
    }

    fun update(state: TtsState, novelTitle: String, chapterTitle: String) {
        // Implemented in Phase 5.
    }

    fun release() {
        // Implemented in Phase 5.
    }
}
