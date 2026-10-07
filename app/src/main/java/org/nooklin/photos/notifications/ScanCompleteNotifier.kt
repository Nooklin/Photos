package org.nooklin.photos.notifications

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.nooklin.photos.R

// Only notification the app posts. Needs the setting on and POST_NOTIFICATIONS granted.
object ScanCompleteNotifier {
    private const val CHANNEL_ID = "scan_complete"
    private const val NOTIFICATION_ID = 1

    fun createChannelIfNeeded(application: Application) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = application.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            application.getString(R.string.notification_channel_scan_complete_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = application.getString(R.string.notification_channel_scan_complete_description)
        }
        manager.createNotificationChannel(channel)
    }

    // Only checks the permission, a background callback can't ask. Settings requests it.
    fun notifyScanComplete(application: Application, scannedCount: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                application,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val notification = NotificationCompat.Builder(application, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_scan_complete)
            .setContentTitle(application.getString(R.string.notification_scan_complete_title))
            .setContentText(
                application.resources.getQuantityString(
                    R.plurals.notification_scan_complete_body,
                    scannedCount,
                    scannedCount
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(application).notify(NOTIFICATION_ID, notification)
    }
}
