package com.iheanyiekechukwu.irishsearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ScheduleExportTest {
    @Test fun usesActualFirstWeekdayAndInclusiveLastOccurrencePerMeeting() {
        val saved = saved("2026-08-25", "2026-09-05", meetings(0, 2))
        val calendar = unfolded(exportScheduleIcs(plan(saved), listOf(saved), Stamp))
        assertEquals(2, calendar.windowed("BEGIN:VEVENT".length).count { it == "BEGIN:VEVENT" })
        assertTrue(calendar.contains("DTSTART;TZID=$ScheduleTimeZone:20260831T090000"))
        assertTrue(calendar.contains("DTSTART;TZID=$ScheduleTimeZone:20260826T090000"))
        assertTrue(calendar.contains("RRULE:FREQ=WEEKLY;BYDAY=MO;UNTIL=20260831T130000Z"))
        assertTrue(calendar.contains("RRULE:FREQ=WEEKLY;BYDAY=WE;UNTIL=20260902T130000Z"))
        assertFalse(calendar.contains("20260825T090000"))
    }

    @Test fun keepsLocalWallClockAcrossDaylightSavingAndIncludesTimezoneObservances() {
        val saved = saved("2026-10-26", "2026-11-09", meetings(0))
        val calendar = unfolded(exportScheduleIcs(plan(saved), listOf(saved), Stamp))
        assertTrue(calendar.contains("DTSTART;TZID=$ScheduleTimeZone:20261026T090000"))
        assertTrue(calendar.contains("UNTIL=20261109T140000Z"))
        assertTrue(calendar.contains("BEGIN:VTIMEZONE\r\nTZID:$ScheduleTimeZone"))
        assertTrue(calendar.contains("DTSTART:20261101T020000\r\nTZOFFSETFROM:-0400\r\nTZOFFSETTO:-0500"))
    }

    @Test fun preservesSeparateSameDayMeetingSeriesWithoutDuplicates() {
        val saved = saved("2026-08-24", "2026-12-11",
            "[{\"meet_day\":\"0\",\"start_time\":\"900\",\"end_time\":\"950\"}," +
                "{\"meet_day\":\"0\",\"start_time\":\"1300\",\"end_time\":\"1350\"}," +
                "{\"meet_day\":\"0\",\"start_time\":\"900\",\"end_time\":\"950\"}]")
        val calendar = unfolded(exportScheduleIcs(plan(saved), listOf(saved, saved), Stamp))
        assertEquals(2, calendar.lineSequence().count { it == "BEGIN:VEVENT" })
        assertEquals(2, scheduleExportSummary(plan(saved), listOf(saved)).eventCount)
        assertTrue(calendar.contains("20260824T130000"))
    }

    @Test fun escapesTextAndFoldsUtf8WithoutSplittingCharactersOrUsingBareNewlines() {
        val original = saved("2026-08-24", "2026-12-11", meetings(0))
        val title = "Research; A,B\\C\nnext " + "é🙂".repeat(40)
        val saved = original.copy(section = original.section.copy(title = title))
        val calendar = exportScheduleIcs(plan(saved), listOf(saved), Stamp)
        assertTrue(calendar.endsWith("\r\n"))
        assertFalse(calendar.replace("\r\n", "").contains('\n'))
        assertFalse(calendar.replace("\r\n", "").contains('\r'))
        calendar.split("\r\n").forEach { line ->
            assertTrue("Content line longer than 75 octets: $line", line.toByteArray(Charsets.UTF_8).size <= 75)
        }
        assertTrue(unfolded(calendar).contains("Research\\; A\\,B\\\\C\\nnext " + "é🙂".repeat(40)))
        assertEquals(calendar, calendar.toByteArray(Charsets.UTF_8).toString(Charsets.UTF_8))
    }

    @Test fun recurringSeriesUidsRemainStableAcrossRepeatedExports() {
        val saved = saved("2026-08-24", "2026-12-11", meetings(0, 2))
        val plan = plan(saved)
        val first = unfolded(exportScheduleIcs(plan, listOf(saved), Stamp)).lineSequence().filter { it.startsWith("UID:") }.toList()
        val second = unfolded(exportScheduleIcs(plan, listOf(saved), Stamp.plusSeconds(300))).lineSequence().filter { it.startsWith("UID:") }.toList()
        assertEquals(first, second)
        assertEquals(first.size, first.distinct().size)
    }

    @Test fun includesCommitmentsAndReportsUndatedOrTbaClasses() {
        val known = saved("2026-08-24", "2026-12-11", meetings(0))
        val undated = known.copy(section = known.section.copy(crn = "2", starts = ""))
        val tba = known.copy(section = known.section.copy(crn = "3", meetingTimes = "[]", meetingTime = "TBA"))
        val classes = listOf(known, undated, tba)
        val plan = plan(known).copy(classIds = classes.mapTo(linkedSetOf()) { it.id }, commitments =
            listOf(PersonalCommitment("job", "Campus job", setOf(1, 3), 900, 1020, "2026-08-24", "2026-12-11")))
        val summary = scheduleExportSummary(plan, classes)
        assertEquals(3, summary.eventCount)
        assertEquals(2, summary.skippedClassCount)
        val calendar = unfolded(exportScheduleIcs(plan, classes, Stamp))
        assertEquals(2, calendar.lineSequence().count { it == "SUMMARY:Campus job" })
        assertFalse(calendar.contains("CRN 2"))
        assertFalse(calendar.contains("CRN 3"))
    }

    @Test fun noMatchingWeekdayOrInvalidDateNeverCreatesInventedEvents() {
        val noMonday = saved("2026-08-25", "2026-08-25", meetings(0))
        val invalid = noMonday.copy(section = noMonday.section.copy(crn = "2", starts = "2026-02-30"))
        val classes = listOf(noMonday, invalid)
        val plan = plan(noMonday).copy(classIds = classes.mapTo(linkedSetOf()) { it.id })
        assertEquals(0, scheduleExportSummary(plan, classes).eventCount)
        assertEquals(2, scheduleExportSummary(plan, classes).skippedClassCount)
        assertThrows(IllegalArgumentException::class.java) { exportScheduleIcs(plan, classes, Stamp) }
    }

    @Test fun doesNotExportUnselectedOrOtherSemesterClasses() {
        val selected = saved("2026-08-24", "2026-12-11", meetings(0))
        val other = selected.copy(semesterCode = "202710")
        val unselected = selected.copy(section = selected.section.copy(crn = "3"))
        val plan = plan(selected).copy(classIds = setOf(selected.id, other.id))
        assertEquals(1, scheduleExportSummary(plan, listOf(selected, other, unselected)).eventCount)
    }

    private fun saved(starts: String, ends: String, meetingTimes: String) = SavedClass(
        "notre-dame", "202610", "Fall 2026",
        CourseSection("", "CSE 20311", "Fundamentals", "01", "1", "", "Professor", "Open", starts, ends, meetingTimes),
        "3 Hours",
    )

    private fun plan(saved: SavedClass) = SchedulePlan("plan", "Fall schedule", "notre-dame", "202610", "Fall 2026", setOf(saved.id))
    private fun meetings(vararg days: Int) = days.joinToString(prefix = "[", postfix = "]") {
        "{\"meet_day\":\"$it\",\"start_time\":\"900\",\"end_time\":\"950\"}"
    }
    private fun unfolded(raw: String) = raw.replace("\r\n ", "")
    private companion object { val Stamp = Instant.parse("2026-10-14T12:00:00Z") }
}
