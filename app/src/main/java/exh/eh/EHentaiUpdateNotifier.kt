package exh.eh

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.R
import yokai.domain.manga.models.Manga
import java.math.RoundingMode
import java.text.NumberFormat

class EHentaiUpdateNotifier(private val context: Context) {

    private val percentFormatter = NumberFormat.getPercentInstance().apply {
        roundingMode = RoundingMode.DOWN
        maximumFractionDigits = 0
    }

    /**
     * Bitmap of the app for notifications.
     */
    private val notificationBitmap by lazy {
        BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
    }

    /**
     * Cached progress notification to avoid creating a lot.
     */
    val progressNotificationBuilder by lazy {
        ensureChannel()
        NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setContentTitle(context.getString(R.string.app_name))
            setSmallIcon(R.drawable.ic_refresh_24dp)
            setLargeIcon(notificationBitmap)
            setOngoing(true)
            setOnlyAlertOnce(true)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "E-Hentai Updates",
                NotificationManager.IMPORTANCE_LOW,
            )
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * Shows the notification containing the currently updating manga and the progress.
     *
     * @param manga the manga that are being updated.
     * @param current the current progress.
     * @param total the total progress.
     */
    fun showProgressNotification(manga: Manga, current: Int, total: Int) {
        progressNotificationBuilder
            .setContentTitle(
                "Updating... ${percentFormatter.format(current.toFloat() / total)}",
            )

        val updatingText = manga.title.take(40)
        progressNotificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(updatingText))

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(
            NOTIFICATION_ID_PROGRESS,
            progressNotificationBuilder
                .setProgress(total, current, false)
                .build(),
        )
    }

    /**
     * Cancels the progress notification.
     */
    fun cancelProgressNotification() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID_PROGRESS)
    }

    companion object {
        const val CHANNEL_ID = "library_ehentai"
        const val NOTIFICATION_ID_PROGRESS = -600
    }
}
