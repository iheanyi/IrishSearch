package com.iheanyiekechukwu.irishsearch

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal interface SeatAlertStore {
    suspend fun watchedClasses(): List<SavedClass>
    suspend fun watchedClass(id: String): SavedClass?
    suspend fun record(id: String, seats: Int?, checkedAt: Long, failed: Boolean)
}

internal interface SeatAlertStoreProvider {
    fun seatAlertStore(defaultStore: SeatAlertStore): SeatAlertStore
}

internal fun interface SeatAvailabilitySource {
    suspend fun availableSeats(saved: SavedClass): Int?
}

internal interface SeatAlertNotifier {
    fun enabled(): Boolean
    fun post(saved: SavedClass, seats: Int): Boolean
}

internal data class SeatCheckSummary(val checked: Int, val failed: Int, val alerts: Int)

/** Shared by scheduled work, manual refresh, and the debug-only WorkManager validation harness. */
internal class SeatAlertChecker(
    private val store: SeatAlertStore,
    private val source: SeatAvailabilitySource,
    private val notifier: SeatAlertNotifier,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun check(): SeatCheckSummary = checkLock.withLock {
        var checked = 0
        var failed = 0
        var alerts = 0
        for (saved in store.watchedClasses()) {
            currentCoroutineContext().ensureActive()
            val seats = try {
                source.availableSeats(saved)?.takeIf { it >= 0 }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            currentCoroutineContext().ensureActive()
            val current = store.watchedClass(saved.id) ?: continue
            checked++
            if (seats == null) {
                failed++
                store.record(saved.id, null, now(), failed = true)
                continue
            }
            // Commit first so overlapping periodic/manual work cannot repeat the same increase.
            store.record(saved.id, seats, now(), failed = false)
            currentCoroutineContext().ensureActive()
            val latest = store.watchedClass(saved.id) ?: continue
            if (current.lastSeats != null && seats > current.lastSeats && notifier.enabled()) {
                currentCoroutineContext().ensureActive()
                if (notifier.post(latest, seats)) alerts++
            }
        }
        SeatCheckSummary(checked, failed, alerts)
    }

    private companion object {
        // WorkManager may run a manual refresh alongside the periodic request in this process.
        val checkLock = Mutex()
    }
}
