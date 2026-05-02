package hayai.novel.tts.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import eu.kanade.tachiyomi.R
import hayai.novel.preferences.NovelPreferences
import hayai.novel.tts.engine.TtsEngineFactory
import uy.kohesive.injekt.injectLazy

/**
 * Foreground service hosting [TtsPlaybackController] so playback survives app backgrounding and
 * shows on the lockscreen / notification shade with media controls. The reader binds via
 * [LocalBinder] to drive playback (load/play/pause/stop); the media session callbacks route
 * lockscreen actions back into the controller.
 *
 * The service's notification is a [MediaStyle] notification anchored to the embedded
 * [TtsMediaSession]. When playback stops we drop the foreground state but keep the service alive
 * for the binder; the reader fully releases on close.
 */
class TtsPlaybackService : Service() {

    private val novelPreferences: NovelPreferences by injectLazy()
    private val engineFactory: TtsEngineFactory by injectLazy()

    private lateinit var controller: TtsPlaybackController
    private lateinit var mediaSession: TtsMediaSession

    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        val controller: TtsPlaybackController get() = this@TtsPlaybackService.controller
    }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        mediaSession = TtsMediaSession(
            context = this,
            onPlay = { controller.resume() },
            onPause = { controller.pause() },
            onStop = { stopPlayback() },
            onSkipNext = { /* wired by host when continuous reading is on */ },
        )
        controller = TtsPlaybackController(
            context = this,
            novelPreferences = novelPreferences,
            engineFactory = engineFactory,
            // The service hosts a no-op highlight dispatcher; reader-driven playback uses the
            // resolver wired by NovelControlBarController instead.
            highlightDispatcher = HighlightDispatcher(
                highlightColor = 0x40_FF_EB_3B.toInt(),
                findSegment = { _, _ -> null },
            ),
        )
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundIfNeeded()
        return START_STICKY
    }

    override fun onDestroy() {
        controller.release()
        mediaSession.release()
        super.onDestroy()
    }

    private fun stopPlayback() {
        controller.stop()
        mediaSession.setStopped()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun startForegroundIfNeeded() {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tachij2k_notification)
            .setContentTitle(getString(R.string.novel_tts_notification_title))
            .setContentText(getString(R.string.novel_tts_notification_subtitle))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(MediaStyle().setMediaSession(mediaSession.session.sessionToken))
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        mediaSession.setPlaying()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null) return
        mgr.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.novel_tts_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private companion object {
        const val NOTIFICATION_CHANNEL_ID = "novel_tts_playback"
        const val NOTIFICATION_ID = 0xCAFE
    }
}
