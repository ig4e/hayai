package hayai.novel.tts.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

/**
 * Audio-focus + becoming-noisy handling for TTS playback. Wraps the API differences between
 * pre/post-Android-O focus requests so the playback controller doesn't have to.
 *
 * - On `AUDIOFOCUS_LOSS` (permanent, e.g. another media app starts) — call [onPermanentLoss].
 * - On `AUDIOFOCUS_LOSS_TRANSIENT` (call ringtone, navigation prompt) — call [onTransientLoss].
 * - On `AUDIOFOCUS_GAIN` after a transient loss — call [onResumeAfterLoss].
 */
class TtsAudioFocus(
    private val context: Context,
    private val onPermanentLoss: () -> Unit,
    private val onTransientLoss: () -> Unit,
    private val onResumeAfterLoss: () -> Unit,
) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null

    private val focusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> onPermanentLoss()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
            -> onTransientLoss()
            AudioManager.AUDIOFOCUS_GAIN -> onResumeAfterLoss()
        }
    }

    /** Requests focus. Returns true on success; the caller should not start audio if false. */
    fun request(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(focusListener)
                .setWillPauseWhenDucked(true)
                .build()
            focusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN,
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    fun abandon() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
    }
}
