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
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class SeatWatchWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val library = LibraryRepository(applicationContext)
        val watched = library.library.first().classes.filter { it.watchSeats }
        if (watched.isEmpty()) return Result.success()
        val catalogs = mutableMapOf<String, CatalogRepository>()

        // Check every watched class sequentially, sharing each school's HTTP connection pool.
        watched.forEach { saved ->
            currentCoroutineContext().ensureActive()
            try {
                val catalog = catalogs.getOrPut(saved.schoolId) {
                    CatalogRepository(SchoolCatalogs.byId(saved.schoolId))
                }
                val details = catalog.details(saved.semesterCode, saved.section)
                currentCoroutineContext().ensureActive()
                val seats = LibraryRepository.availableSeats(details.seats) ?: return@forEach
                // The user may have removed this class or stopped watching during the request.
                val current = library.library.first().classes.firstOrNull {
                    it.id == saved.id && it.watchSeats
                } ?: return@forEach
                if (current.lastSeats != null && seats > current.lastSeats) {
                    notifyOpenSeat(current, seats)
                }
                library.updateSeats(saved.id, seats)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // A temporary PATH failure should leave this section's baseline intact.
            }
        }
        return Result.success()
    }

    private fun notifyOpenSeat(saved: SavedClass, seats: Int) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL_ID,
                "Class seat alerts",
                NotificationManager.IMPORTANCE_DEFAULT,
            ))
        }
        val openClass = Intent(applicationContext, MainActivity::class.java).apply {
            action = ACTION_OPEN_SAVED_CLASS
            data = Uri.Builder().scheme("irishsearch").authority("saved-class")
                .appendPath(saved.id).build()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_SAVED_CLASS_ID, saved.id)
        }
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            openClass,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.notify(saved.id, 0, NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("A seat opened in ${saved.section.code}")
            .setContentText("${saved.section.title} · ${saved.section.section} · $seats available")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build())
    }

    companion object {
        const val ACTION_OPEN_SAVED_CLASS = "com.iheanyiekechukwu.irishsearch.OPEN_SAVED_CLASS"
        const val EXTRA_SAVED_CLASS_ID = "com.iheanyiekechukwu.irishsearch.extra.SAVED_CLASS_ID"
        private const val CHANNEL_ID = "irishsearch_seats"
        private const val WORK_NAME = "saved_class_seat_checks"

        fun configure(context: Context, library: SavedLibrary) {
            val manager = WorkManager.getInstance(context)
            if (library.classes.none { it.watchSeats }) {
                manager.cancelUniqueWork(WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<SeatWatchWorker>(1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build())
                .build()
            manager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
