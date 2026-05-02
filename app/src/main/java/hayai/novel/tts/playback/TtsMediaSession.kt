package hayai.novel.tts.playback

import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

/**
 * Wraps a [MediaSessionCompat] with the playback state machinery TTS needs (play/pause, skip-to-
 * next-chapter). The session powers the lockscreen / notification controls and routes Bluetooth
 * remote commands back into [TtsPlaybackController].
 */
class TtsMediaSession(
    context: Context,
    private val onPlay: () -> Unit,
    private val onPause: () -> Unit,
    private val onStop: () -> Unit,
    private val onSkipNext: () -> Unit,
) {

    val session: MediaSessionCompat = MediaSessionCompat(context, "hayai-novel-tts").apply {
        setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() = this@TtsMediaSession.onPlay()
            override fun onPause() = this@TtsMediaSession.onPause()
            override fun onStop() = this@TtsMediaSession.onStop()
            override fun onSkipToNext() = this@TtsMediaSession.onSkipNext()
        })
        isActive = true
    }

    fun setPlaying() = updateState(PlaybackStateCompat.STATE_PLAYING)
    fun setPaused() = updateState(PlaybackStateCompat.STATE_PAUSED)
    fun setStopped() = updateState(PlaybackStateCompat.STATE_STOPPED)

    fun release() {
        session.isActive = false
        session.release()
    }

    private fun updateState(state: Int) {
        val actions = PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_STOP or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .build(),
        )
    }
}
