package com.iheanyiekechukwu.irishsearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleInsightsTest {
    @Test fun separatesCourseAndSectionTotalsWithoutCountingRepeatedRecords() {
        val first = saved("CSE 20311", "1")
        val alternate = saved(" cse  20311 ", "2")
        val other = saved("MATH 20630", "3")

        val insights = scheduleInsights(listOf(first, first, alternate, other), emptyList())

        assertEquals(2, insights.courseCount)
        assertEquals(3, insights.sectionCount)
        assertEquals(listOf(first, alternate), insights.duplicateCourseSections["CSE 20311"])
        assertEquals(3, insights.unknownMeetingClassCount)
    }

    @Test fun emptyPlansHaveNoTimeBounds() {
        val insights = scheduleInsights(emptyList(), emptyList())

        assertEquals(0, insights.courseCount)
        assertEquals(0, insights.sectionCount)
        assertEquals(0, insights.unknownMeetingClassCount)
        assertNull(insights.earliestStartMinutes)
        assertNull(insights.latestEndMinutes)
        assertEquals(setOf(0, 1, 2, 3, 4), insights.freeWeekdays)
    }

    @Test fun measuresKnownTimesAndFreeWeekdaysWhileReportingUnavailableTimes() {
        val first = saved("CSE 20311", "1")
        val unknown = saved("MATH 20630", "2")
        val unrelated = saved("HIST 10000", "3")
        val insights = scheduleInsights(listOf(first, unknown), listOf(
            CalendarMeeting(first, 0, 540, 600),
            CalendarMeeting(first, 2, 600, 675),
            CalendarMeeting(first, 5, 780, 840),
            CalendarMeeting(unknown, 1, 600, 500),
            CalendarMeeting(unrelated, 3, 480, 900),
        ))

        assertEquals(1, insights.unknownMeetingClassCount)
        assertEquals(540, insights.earliestStartMinutes)
        assertEquals(840, insights.latestEndMinutes)
        assertEquals(setOf(1, 3, 4), insights.freeWeekdays)
    }

    @Test fun countsEachConflictingSectionPairOnlyOnce() {
        val first = saved("CSE 20311", "1")
        val second = saved("MATH 20630", "2")
        val meetings = listOf(
            CalendarMeeting(first, 0, 600, 675),
            CalendarMeeting(second, 0, 630, 690),
            CalendarMeeting(first, 2, 600, 675),
            CalendarMeeting(second, 2, 630, 690),
        )
        val insights = scheduleInsights(listOf(first, second), meetings + meetings)

        assertEquals(1, insights.conflictCount)
        assertEquals(setOf(first.id, second.id), insights.conflictingClassIds)
        assertEquals(insights.conflictingClassIds, scheduleConflictingIds(meetings))
    }

    @Test fun meetingsMustOverlapInTimeAndDayAndBelongToDifferentSectionsInTheSameTerm() {
        val first = saved("CSE 20311", "1")
        val second = saved("MATH 20630", "2")
        val meeting = CalendarMeeting(first, 0, 600, 675)

        assertFalse(scheduleMeetingsConflict(meeting, CalendarMeeting(second, 0, 675, 720)))
        assertFalse(scheduleMeetingsConflict(meeting, CalendarMeeting(second, 2, 630, 690)))
        assertFalse(scheduleMeetingsConflict(meeting, meeting.copy(start = 630)))
        assertFalse(scheduleMeetingsConflict(meeting, CalendarMeeting(second.copy(semesterCode = "202710"), 0, 630, 690)))
        assertFalse(scheduleMeetingsConflict(meeting, CalendarMeeting(second.copy(schoolId = "other"), 0, 630, 690)))
        assertFalse(scheduleMeetingsConflict(meeting, CalendarMeeting(second, 0, -30, 630)))
    }

    @Test fun disjointDateRangesDoNotConflict() {
        val first = saved("CSE 20311", "1", "2026-08-24", "2026-10-16")
        val second = saved("MATH 20630", "2", "2026-10-19", "2026-12-11")

        assertFalse(scheduleMeetingsConflict(
            CalendarMeeting(first, 0, 600, 675), CalendarMeeting(second, 0, 630, 690),
        ))
    }

    @Test fun intersectingDateRangesMustContainTheMeetingWeekday() {
        val first = saved("CSE 20311", "1", "2026-08-24", "2026-10-17")
        val second = saved("MATH 20630", "2", "2026-10-17", "2026-12-11")

        assertFalse(scheduleMeetingsConflict(
            CalendarMeeting(first, 0, 600, 675), CalendarMeeting(second, 0, 630, 690),
        ))
        assertTrue(scheduleMeetingsConflict(
            CalendarMeeting(first, 5, 600, 675), CalendarMeeting(second, 5, 630, 690),
        ))
    }

    @Test fun incompleteMalformedAndInvertedDateRangesConservativelyConflict() {
        val complete = saved("CSE 20311", "1", "2026-08-24", "2026-10-16")
        val incompleteRanges = listOf(
            "2026-10-19" to "",
            "" to "2026-10-16",
            "TBA" to "2026-12-11",
            "2026-02-30" to "2026-12-11",
            "2026-12-11" to "2026-10-19",
            "2026-10-19 trailing text" to "2026-12-11",
        )
        incompleteRanges.forEach { (start, end) ->
            val second = saved("MATH 20630", "2", start, end)
            assertTrue("Unknown range $start..$end", scheduleMeetingsConflict(
                CalendarMeeting(complete, 0, 600, 675), CalendarMeeting(second, 0, 630, 690),
            ))
        }
    }

    @Test fun missingCourseCodesDoNotMergeUnrelatedSections() {
        val first = saved("", "1")
        val second = saved("", "2")
        val insights = scheduleInsights(listOf(first, second), emptyList())

        assertEquals(2, insights.courseCount)
        assertTrue(insights.duplicateCourseSections.isEmpty())
    }

    private fun saved(
        code: String,
        crn: String,
        starts: String = "2026-08-24",
        ends: String = "2026-12-11",
    ) = SavedClass(
        schoolId = "nd",
        semesterCode = "202610",
        semesterLabel = "Fall 2026",
        section = CourseSection("", code, code, "01", crn, "", "", "", starts, ends, ""),
        creditHours = "3 Hours",
    )
}
