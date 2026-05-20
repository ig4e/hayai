package hayai.novel.reader.tts

import android.content.Context
import android.media.AudioManager
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Audio focus owner for the TTS playback. Wraps [AudioFocusRequestCompat] so the
 * controller never has to worry about API-level differences between SDK 23 and 26+.
 *
 * Focus is requested as `AUDIOFOCUS_GAIN` with `USAGE_MEDIA` + `CONTENT_TYPE_SPEECH` so
 * the system treats TTS like any other media player — it will duck for navigation
 * prompts, lose focus to phone calls, etc.
 */
class NovelTtsAudioFocus(context: Context) {

    private val audioManager: AudioManager =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _focusEvents = MutableSharedFlow<FocusEvent>(
        replay = 0,
        extraBufferCapacity = 16,
    )
    val focusEvents: SharedFlow<FocusEvent> = _focusEvents.asSharedFlow()

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> _focusEvents.tryEmit(FocusEvent.Gained)
            AudioManager.AUDIOFOCUS_LOSS -> _focusEvents.tryEmit(FocusEvent.Lost)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> _focusEvents.tryEmit(FocusEvent.LostTransient)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                _focusEvents.tryEmit(FocusEvent.LostTransientCanDuck)
        }
    }

    private val request: AudioFocusRequestCompat by lazy {
        AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributesCompat.Builder()
                    .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                    .setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setOnAudioFocusChangeListener(focusListener)
            .setWillPauseWhenDucked(false)
            .build()
    }

    @Volatile private var holdingFocus: Boolean = false

    /**
     * Request media audio focus. Returns `true` if granted. Safe to call when focus is
     * already held — re-requesting is a no-op.
     */
    fun request(): Boolean {
        if (holdingFocus) return true
        val result = AudioManagerCompat.requestAudioFocus(audioManager, request)
        holdingFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (!holdingFocus) {
            Logger.w { "TTS: audio-focus request denied (result=$result)" }
        }
        return holdingFocus
    }

    /** Drop focus. Idempotent. */
    fun abandon() {
        if (!holdingFocus) return
        AudioManagerCompat.abandonAudioFocusRequest(audioManager, request)
        holdingFocus = false
    }
}

sealed class FocusEvent {
    object Gained : FocusEvent()

    /** Phone call, alarm: pause playback; the focus owner will return it. */
    object LostTransient : FocusEvent()

    /** Notification chime: duck. For TTS we treat as a transient pause. */
    object LostTransientCanDuck : FocusEvent()

    /** Another media app took permanent focus: stop. */
    object Lost : FocusEvent()
}
