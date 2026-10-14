package com.iheanyiekechukwu.irishsearch

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

internal class SeatAlertNotifications(private val context: Context) : SeatAlertNotifier {
    override fun enabled(): Boolean = enabled(context)

    override fun post(saved: SavedClass, seats: Int): Boolean {
        if (!enabled()) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL_ID, "Class seat alerts", NotificationManager.IMPORTANCE_DEFAULT,
            ))
        }
        val openClass = Intent(context, MainActivity::class.java).apply {
            action = SeatWatchWorker.ACTION_OPEN_SAVED_CLASS
            data = Uri.Builder().scheme("irishsearch").authority("saved-class").appendPath(saved.id).build()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(SeatWatchWorker.EXTRA_SAVED_CLASS_ID, saved.id)
        }
        val contentIntent = PendingIntent.getActivity(context, 0, openClass,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return try {
            manager.notify(saved.id, 0, NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle("A seat opened in ${saved.section.code}")
                .setContentText("${saved.section.title} · ${saved.section.section} · $seats available")
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build())
            true
        } catch (_: SecurityException) {
            // Permission can change after the enabled check; successful seat checks still persist.
            false
        }
    }

    companion object {
        const val CHANNEL_ID = "irishsearch_seats"

        fun enabled(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL_ID)
                    ?.importance == NotificationManager.IMPORTANCE_NONE) return false
            return true
        }
    }
}
