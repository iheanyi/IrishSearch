package com.iheanyiekechukwu.irishsearch

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class SeatAlertCheckerTest {
    @Test fun aSeatIncreasePostsOnceAndUnchangedOrLowerCountsDoNotPost() = runBlocking {
        val store = FakeStore(saved(lastSeats = 0))
        val notifier = FakeNotifier()
        var seats = 0
        val checker = SeatAlertChecker(store, SeatAvailabilitySource { seats }, notifier) { 123L }
        assertEquals(SeatCheckSummary(1, 0, 0), checker.check())
        seats = 1
        assertEquals(SeatCheckSummary(1, 0, 1), checker.check())
        assertEquals(SeatCheckSummary(1, 0, 0), checker.check())
        seats = 0
        assertEquals(SeatCheckSummary(1, 0, 0), checker.check())
        assertEquals(listOf(1), notifier.posts)
        assertEquals(0, store.current?.lastSeats)
        assertEquals(123L, store.current?.lastCheckedAt)
    }

    @Test fun unknownBaselineEstablishesAvailabilityWithoutAnAlert() = runBlocking {
        val store = FakeStore(saved(lastSeats = null))
        val notifier = FakeNotifier()
        assertEquals(SeatCheckSummary(1, 0, 0),
            SeatAlertChecker(store, SeatAvailabilitySource { 3 }, notifier).check())
        assertEquals(3, store.current?.lastSeats)
        assertTrue(notifier.posts.isEmpty())
    }

    @Test fun notificationPermissionDenialStillRecordsSuccessfulAvailability() = runBlocking {
        val store = FakeStore(saved(lastSeats = 0))
        val notifier = FakeNotifier(allowed = false)
        val checker = SeatAlertChecker(store, SeatAvailabilitySource { 1 }, notifier) { 456L }
        assertEquals(SeatCheckSummary(1, 0, 0), checker.check())
        assertEquals(1, store.current?.lastSeats)
        assertEquals(456L, store.current?.lastCheckedAt)
        assertFalse(store.current!!.lastCheckFailed)
        notifier.allowed = true
        assertEquals(SeatCheckSummary(1, 0, 0), checker.check())
        assertTrue(notifier.posts.isEmpty())
    }

    @Test fun failedOrUnavailableChecksKeepTheLastSuccessfulBaselineAndTimestamp() = runBlocking {
        for (source in listOf(SeatAvailabilitySource { throw IOException("temporary failure") },
            SeatAvailabilitySource { null })) {
            val store = FakeStore(saved(lastSeats = 0).copy(lastCheckedAt = 123L))
            val notifier = FakeNotifier()
            assertEquals(SeatCheckSummary(1, 1, 0), SeatAlertChecker(store, source, notifier) { 456L }.check())
            assertEquals(0, store.current?.lastSeats)
            assertEquals(123L, store.current?.lastCheckedAt)
            assertTrue(store.current!!.lastCheckFailed)
            assertTrue(notifier.posts.isEmpty())
        }
    }

    @Test fun unwatchOrRemovalDuringFetchPreventsRecordingAndAlerting() = runBlocking {
        for (remove in listOf(false, true)) {
            val store = FakeStore(saved(lastSeats = 0))
            val notifier = FakeNotifier()
            val checker = SeatAlertChecker(store, SeatAvailabilitySource {
                store.current = if (remove) null else store.current!!.copy(watchSeats = false)
                1
            }, notifier)
            assertEquals(SeatCheckSummary(0, 0, 0), checker.check())
            assertEquals(0, store.records)
            assertTrue(notifier.posts.isEmpty())
        }
    }

    @Test fun cancellationDuringFetchDoesNotRecordFailureOrSendAnAlert() = runBlocking {
        val store = FakeStore(saved(lastSeats = 0))
        val notifier = FakeNotifier()
        val started = CompletableDeferred<Unit>()
        val checker = SeatAlertChecker(store, SeatAvailabilitySource {
            started.complete(Unit)
            awaitCancellation()
        }, notifier)
        val work = launch { checker.check() }
        started.await()
        work.cancelAndJoin()
        assertEquals(0, store.records)
        assertEquals(0, store.current?.lastSeats)
        assertFalse(store.current!!.lastCheckFailed)
        assertTrue(notifier.posts.isEmpty())
    }

    @Test fun overlappingPeriodicAndManualChecksDoNotDuplicateAnIncrease() = runBlocking {
        val store = FakeStore(saved(lastSeats = 0))
        val notifier = FakeNotifier()
        val first = async { SeatAlertChecker(store, SeatAvailabilitySource { 1 }, notifier).check() }
        val second = async { SeatAlertChecker(store, SeatAvailabilitySource { 1 }, notifier).check() }
        assertEquals(1, first.await().alerts + second.await().alerts)
        assertEquals(listOf(1), notifier.posts)
    }

    private class FakeStore(var current: SavedClass?) : SeatAlertStore {
        var records = 0
        override suspend fun watchedClasses(): List<SavedClass> = listOfNotNull(current).filter { it.watchSeats }
        override suspend fun watchedClass(id: String): SavedClass? = current?.takeIf { it.id == id && it.watchSeats }
        override suspend fun record(id: String, seats: Int?, checkedAt: Long, failed: Boolean) {
            records++
            current = current?.takeIf { it.id == id && it.watchSeats }?.let {
                it.copy(lastSeats = seats ?: it.lastSeats,
                    lastCheckedAt = if (!failed && seats != null) checkedAt else it.lastCheckedAt,
                    lastCheckFailed = failed || seats == null)
            }
        }
    }

    private class FakeNotifier(var allowed: Boolean = true) : SeatAlertNotifier {
        val posts = mutableListOf<Int>()
        override fun enabled(): Boolean = allowed
        override fun post(saved: SavedClass, seats: Int): Boolean { posts += seats; return true }
    }

    private fun saved(lastSeats: Int?) = SavedClass("notre-dame", "202610", "Fall Semester 2026",
        CourseSection("", "CSE 10101", "Elements of Computing I", "01", "12427", "", "", "", "", "", ""),
        lastSeats = lastSeats, watchSeats = true)
}
