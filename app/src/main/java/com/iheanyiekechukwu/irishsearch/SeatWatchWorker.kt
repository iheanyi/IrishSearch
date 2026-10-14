package com.iheanyiekechukwu.irishsearch

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class SeatWatchWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val catalogs = mutableMapOf<String, CatalogRepository>()
        val defaultStore = LibrarySeatAlertStore(LibraryRepository(applicationContext))
        val store = (applicationContext as? SeatAlertStoreProvider)?.seatAlertStore(defaultStore) ?: defaultStore
        return try {
            val result = SeatAlertChecker(store,
                SeatAvailabilitySource { saved ->
                    val catalog = catalogs.getOrPut(saved.schoolId) {
                        CatalogRepository(SchoolCatalogs.byId(saved.schoolId))
                    }
                    val details = catalog.details(saved.semesterCode, saved.section)
                    LibraryRepository.availableSeats(details.seats)
                }, SeatAlertNotifications(applicationContext)).check()
            Result.success(workDataOf("checked" to result.checked, "failed" to result.failed, "alerts" to result.alerts))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val ACTION_OPEN_SAVED_CLASS = "com.iheanyiekechukwu.irishsearch.OPEN_SAVED_CLASS"
        const val EXTRA_SAVED_CLASS_ID = "com.iheanyiekechukwu.irishsearch.extra.SAVED_CLASS_ID"
        private const val WORK_NAME = "saved_class_seat_checks"
        private const val REFRESH_WORK_NAME = "saved_class_seat_refresh"

        fun refresh(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(REFRESH_WORK_NAME, ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<SeatWatchWorker>().setConstraints(checkConstraints()).build())
        }

        fun checking(context: Context): Flow<Boolean> {
            val manager = WorkManager.getInstance(context)
            return combine(manager.getWorkInfosForUniqueWorkFlow(WORK_NAME),
                manager.getWorkInfosForUniqueWorkFlow(REFRESH_WORK_NAME)) { periodic, refresh ->
                (periodic + refresh).any { it.state == WorkInfo.State.RUNNING }
            }.distinctUntilChanged()
        }

        private fun checkConstraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build()

        fun configure(context: Context, library: SavedLibrary) {
            val manager = WorkManager.getInstance(context)
            if (library.classes.none { it.watchSeats }) {
                manager.cancelUniqueWork(WORK_NAME)
                manager.cancelUniqueWork(REFRESH_WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<SeatWatchWorker>(1, TimeUnit.HOURS)
                .setConstraints(checkConstraints())
                .build()
            manager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

internal class LibrarySeatAlertStore(private val library: LibraryRepository) : SeatAlertStore {
    override suspend fun watchedClasses(): List<SavedClass> = library.library.first().classes.filter { it.watchSeats }
    override suspend fun watchedClass(id: String): SavedClass? =
        library.library.first().classes.firstOrNull { it.id == id && it.watchSeats }
    override suspend fun record(id: String, seats: Int?, checkedAt: Long, failed: Boolean) =
        library.recordSeatCheck(id, seats, checkedAt, failed)
}
