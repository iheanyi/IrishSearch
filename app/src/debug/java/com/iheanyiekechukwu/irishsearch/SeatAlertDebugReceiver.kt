package com.iheanyiekechukwu.irishsearch

import android.app.NotificationManager
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val DEBUG_WORK_NAME = "debug_seat_alert_validation"
private const val DEBUG_LOG = "IRISH_SEAT_DEBUG"
private val debugFixture = SavedClass("notre-dame", "202610", "Fall Semester 2026",
    CourseSection("", "TEST 10001", "Debug seat alert validation", "01", "debug-seat-alert", "", "", "", "", "", ""),
    lastSeats = 0, watchSeats = true)

/** Normal workers must never send a live PATH request for the harness fixture. */
internal class SeatAlertDebugApplication : Application(), SeatAlertStoreProvider {
    override fun seatAlertStore(defaultStore: SeatAlertStore): SeatAlertStore = object : SeatAlertStore {
        override suspend fun watchedClasses(): List<SavedClass> {
            val watched = defaultStore.watchedClasses()
            if (watched.any { it.id == debugFixture.id }) Log.i(DEBUG_LOG, "normal worker excluded debug fixture")
            return watched.filterNot { it.id == debugFixture.id }
        }
        override suspend fun watchedClass(id: String): SavedClass? =
            if (id == debugFixture.id) null else defaultStore.watchedClass(id)
        override suspend fun record(id: String, seats: Int?, checkedAt: Long, failed: Boolean) {
            if (id != debugFixture.id) defaultStore.record(id, seats, checkedAt, failed)
        }
    }
}

/** This exported entry point and all fake availability exist only in debug APKs. */
class SeatAlertDebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val library = LibraryRepository(context.applicationContext)
                val manager = context.getSystemService(NotificationManager::class.java)
                when (intent.getStringExtra("operation") ?: "inspect") {
                    "setup" -> {
                        WorkManager.getInstance(context).cancelUniqueWork(DEBUG_WORK_NAME).result.get()
                        manager.cancel(debugFixture.id, 0)
                        library.remove(debugFixture.id)
                        library.restore(debugFixture, emptySet())
                        Log.i(DEBUG_LOG, "setup id=${debugFixture.id} baseline=0 watched=true")
                    }
                    "check" -> {
                        val request = OneTimeWorkRequestBuilder<SeatAlertDebugWorker>()
                            .setInputData(workDataOf("seats" to intent.getIntExtra("seats", 1),
                                "fail" to intent.getBooleanExtra("fail", false),
                                "delay_ms" to intent.getLongExtra("delay_ms", 0L))).build()
                        WorkManager.getInstance(context).enqueueUniqueWork(DEBUG_WORK_NAME,
                            ExistingWorkPolicy.REPLACE, request).result.get()
                        Log.i(DEBUG_LOG, "enqueued work=${request.id}")
                    }
                    "unwatch" -> { library.setWatch(debugFixture.id, false); Log.i(DEBUG_LOG, "unwatched") }
                    "remove" -> { library.remove(debugFixture.id); Log.i(DEBUG_LOG, "removed") }
                    "cancel" -> {
                        WorkManager.getInstance(context).cancelUniqueWork(DEBUG_WORK_NAME).result.get()
                        Log.i(DEBUG_LOG, "cancelled work")
                    }
                    "tap" -> {
                        val notification = manager.activeNotifications.firstOrNull { it.tag == debugFixture.id }
                        checkNotNull(notification) { "No validation notification is active" }
                        notification.notification.contentIntent.send()
                        Log.i(DEBUG_LOG, "sent genuine notification PendingIntent")
                    }
                    "cleanup" -> {
                        WorkManager.getInstance(context).cancelUniqueWork(DEBUG_WORK_NAME).result.get()
                        library.remove(debugFixture.id)
                        manager.cancel(debugFixture.id, 0)
                        Log.i(DEBUG_LOG, "cleaned up fixture/work/notification")
                    }
                    "inspect" -> {
                        val saved = library.library.first().classes.firstOrNull { it.id == debugFixture.id }
                        val active = manager.activeNotifications.filter { it.tag == debugFixture.id }
                        Log.i(DEBUG_LOG, "inspect baseline=${saved?.lastSeats} watched=${saved?.watchSeats} " +
                            "lastChecked=${saved?.lastCheckedAt} failed=${saved?.lastCheckFailed} notifications=${active.size} " +
                            "postTime=${active.firstOrNull()?.postTime} permitted=${SeatAlertNotifications.enabled(context)}")
                    }
                }
            } catch (error: Exception) { Log.e(DEBUG_LOG, "validation operation failed", error) }
            finally { pending.finish() }
        }
    }
}

class SeatAlertDebugWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val delegate = LibrarySeatAlertStore(LibraryRepository(applicationContext))
        // Fake results must never touch the user's real watched classes.
        val store = object : SeatAlertStore {
            override suspend fun watchedClasses() = delegate.watchedClasses().filter { it.id == debugFixture.id }
            override suspend fun watchedClass(id: String) =
                if (id == debugFixture.id) delegate.watchedClass(id) else null
            override suspend fun record(id: String, seats: Int?, checkedAt: Long, failed: Boolean) {
                if (id == debugFixture.id) delegate.record(id, seats, checkedAt, failed)
            }
        }
        return try {
            val summary = SeatAlertChecker(store, SeatAvailabilitySource {
                Log.i(DEBUG_LOG, "fetch started work=$id")
                delay(inputData.getLong("delay_ms", 0L).coerceIn(0, 60_000))
                if (inputData.getBoolean("fail", false)) error("Deterministic debug PATH failure")
                inputData.getInt("seats", 1)
            }, SeatAlertNotifications(applicationContext)).check()
            Log.i(DEBUG_LOG, "completed checked=${summary.checked} failed=${summary.failed} alerts=${summary.alerts}")
            Result.success(workDataOf("checked" to summary.checked, "failed" to summary.failed, "alerts" to summary.alerts))
        } catch (cancelled: CancellationException) {
            Log.i(DEBUG_LOG, "worker cancelled")
            throw cancelled
        }
    }
}
