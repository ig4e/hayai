package hayai.novel.reader.tts

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.notificationBuilder
import hayai.novel.reader.service.NovelTtsPlaybackService

/**
 * Builds the foreground TTS playback notification using [MediaStyle], bound to the
 * controller's [MediaSessionCompat]. The MediaStyle tie-in is what makes the lockscreen
 * controls and Bluetooth media-button events route to the same session — so a single
 * tap on a Bluetooth headset's play/pause hits the controller's command channel directly.
 *
 * Action buttons (Pause/Resume + Stop) still fire intents at the service so the user
 * can drive playback even when the lockscreen / MediaController isn't visible.
 */
object NovelTtsNotification {

    fun build(
        context: Context,
        sessionToken: MediaSessionCompat.Token?,
        isPaused: Boolean,
        progressPercent: Int,
        novelTitle: String,
        chapterTitle: String,
        mangaId: Long,
        chapterId: Long,
    ): Notification {
        val toggleIntent = PendingIntent.getService(
            context,
            1001,
            Intent(context, NovelTtsPlaybackService::class.java)
                .setAction(NovelTtsPlaybackService.ACTION_TOGGLE_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getService(
            context,
            1002,
            Intent(context, NovelTtsPlaybackService::class.java)
                .setAction(NovelTtsPlaybackService.ACTION_STOP_PLAYBACK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val openReaderIntent = ReaderActivity.newIntent(
            context = context,
            mangaId = mangaId.takeIf { it > 0L },
            chapterId = chapterId.takeIf { it > 0L },
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val openReaderPendingIntent = PendingIntent.getActivity(
            context,
            1003,
            openReaderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val statusText = if (isPaused) "Paused" else "Reading in background"
        val contentText = if (chapterTitle.isNotBlank()) "$chapterTitle · $statusText" else statusText

        val mediaStyle = MediaStyle()
            .setShowActionsInCompactView(0, 1)
        if (sessionToken != null) {
            mediaStyle.setMediaSession(sessionToken)
        }

        return context.notificationBuilder(Notifications.CHANNEL_TTS_PLAYBACK) {
            setSmallIcon(R.drawable.ic_hayai)
            setContentTitle(novelTitle.ifBlank { "TTS playback" })
            setContentText(contentText)
            setContentIntent(openReaderPendingIntent)
            setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setOngoing(true)
            setOnlyAlertOnce(true)
            setShowWhen(false)
            setProgress(100, progressPercent.coerceIn(0, 100), false)

            addAction(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (isPaused) "Resume" else "Pause",
                toggleIntent,
            )

            addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopIntent,
            )

            setStyle(mediaStyle)
        }.build()
    }
}
