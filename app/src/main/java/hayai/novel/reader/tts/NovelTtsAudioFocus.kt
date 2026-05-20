package hayai.novel.reader.tts

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Wraps `AudioFocusRequestCompat` so the controller can request/abandon focus without
 * caring about API-level differences. Phase 1 stub.
 */
class NovelTtsAudioFocus(
    @Suppress("UnusedPrivateProperty") private val context: Context,
) {
    private val _focusEvents = MutableSharedFlow<FocusEvent>(
        replay = 0,
        extraBufferCapacity = 16,
    )
    val focusEvents: SharedFlow<FocusEvent> = _focusEvents.asSharedFlow()

    /** Returns true when focus was actually granted. */
    fun request(): Boolean = true

    fun abandon() {
        // Implemented in Phase 2.
    }
}

sealed class FocusEvent {
    object Gained : FocusEvent()

    /** Phone call, alarm: pause playback; the focus owner will return it. */
    object LostTransient : FocusEvent()

    /** Notification chime: lower volume. We treat this as a transient pause for now. */
    object LostTransientCanDuck : FocusEvent()

    /** Another media app took permanent focus: stop. */
    object Lost : FocusEvent()
}
