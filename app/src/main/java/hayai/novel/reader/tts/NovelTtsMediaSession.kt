package hayai.novel.reader.tts

import android.content.Context
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import co.touchlab.kermit.Logger

/**
 * MediaSession wrapper. Surfaces TTS playback to the lockscreen, Bluetooth headset, car
 * head units, and any other media-button source — anything that listens to media-session
 * broadcasts will see the novel-reader TTS as a playing media app.
 *
 * Ownership: lives on the controller. The controller's command channel is the only
 * consumer of the callback dispatcher, so user input from the lockscreen flows through
 * the exact same serial loop as user input from the action bar.
 */
class NovelTtsMediaSession(private val context: Context) {

    private var session: MediaSessionCompat? = null

    val sessionToken: MediaSessionCompat.Token?
        get() = session?.sessionToken

    /**
     * Idempotent — first call creates the session and wires the callback. The dispatcher
     * is captured by the callback so the controller doesn't need to hold a reference to
     * this object after wiring.
     */
    fun ensure(dispatch: (TtsCommand) -> Unit) {
        if (session != null) return
        session = MediaSessionCompat(context.applicationContext, "NovelTts").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    Logger.d { "TTS MediaSession: onPlay" }
                    dispatch(TtsCommand.Resume)
                }
                override fun onPause() {
                    Logger.d { "TTS MediaSession: onPause" }
                    dispatch(TtsCommand.Pause)
                }
                override fun onStop() {
                    Logger.d { "TTS MediaSession: onStop" }
                    dispatch(TtsCommand.Stop)
                }
                override fun onSkipToNext() {
                    Logger.d { "TTS MediaSession: onSkipToNext" }
                    dispatch(TtsCommand.SkipParagraph)
                }
                override fun onSkipToPrevious() {
                    Logger.d { "TTS MediaSession: onSkipToPrevious" }
                    dispatch(TtsCommand.PreviousParagraph)
                }
                override fun onCustomAction(action: String?, extras: android.os.Bundle?) {
                    when (action) {
                        ACTION_NEXT_CHAPTER -> dispatch(TtsCommand.NextChapter)
                        ACTION_PREVIOUS_CHAPTER -> dispatch(TtsCommand.PreviousChapter)
                    }
                }
            })
            isActive = true
        }
    }

    /**
     * Push the latest playback state + metadata into the session so the lockscreen, the
     * MediaStyle notification, and any external media controllers all see one consistent
     * "what's playing" view.
     */
    fun update(state: TtsState, novelTitle: String, chapterTitle: String) {
        val s = session ?: return

        val (playbackState, position, paragraphIdx, paragraphTotal) = when (state) {
            is TtsState.Speaking ->
                PlaybackTuple(PlaybackStateCompat.STATE_PLAYING, state.paragraphIndex.toLong(), state.paragraphIndex, state.totalParagraphs)
            is TtsState.Paused ->
                PlaybackTuple(PlaybackStateCompat.STATE_PAUSED, state.paragraphIndex.toLong(), state.paragraphIndex, state.totalParagraphs)
            is TtsState.Preparing, is TtsState.AdvancingChapter ->
                PlaybackTuple(PlaybackStateCompat.STATE_BUFFERING, 0L, 0, 0)
            is TtsState.Idle ->
                PlaybackTuple(PlaybackStateCompat.STATE_STOPPED, 0L, 0, 0)
            is TtsState.Error ->
                PlaybackTuple(PlaybackStateCompat.STATE_ERROR, 0L, 0, 0)
        }

        val actions = PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_STOP or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS

        val playbackStateBuilder = PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(playbackState, position, 1.0f, SystemClock.elapsedRealtime())
            .addCustomAction(
                ACTION_NEXT_CHAPTER,
                "Next chapter",
                android.R.drawable.ic_media_ff,
            )
            .addCustomAction(
                ACTION_PREVIOUS_CHAPTER,
                "Previous chapter",
                android.R.drawable.ic_media_rew,
            )

        if (state is TtsState.Error) {
            playbackStateBuilder.setErrorMessage(
                PlaybackStateCompat.ERROR_CODE_APP_ERROR,
                state.reason,
            )
        }

        s.setPlaybackState(playbackStateBuilder.build())

        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, chapterTitle.ifBlank { "TTS playback" })
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, novelTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, novelTitle)
            .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, paragraphTotal.toLong())
            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, (paragraphIdx + 1).toLong())
            .build()
        s.setMetadata(metadata)
    }

    fun release() {
        session?.isActive = false
        session?.release()
        session = null
    }

    private data class PlaybackTuple(
        val state: Int,
        val position: Long,
        val paragraphIndex: Int,
        val paragraphTotal: Int,
    )

    companion object {
        const val ACTION_NEXT_CHAPTER = "hayai.tts.NEXT_CHAPTER"
        const val ACTION_PREVIOUS_CHAPTER = "hayai.tts.PREVIOUS_CHAPTER"
    }
}
