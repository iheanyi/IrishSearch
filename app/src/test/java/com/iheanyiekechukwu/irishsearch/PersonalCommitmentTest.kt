package com.iheanyiekechukwu.irishsearch

import org.junit.Assert.*
import org.junit.Test

class PersonalCommitmentTest {
    private fun commitment(id: String = "work", days: Set<Int> = setOf(0, 2), start: Int = 570, end: Int = 630,
        starts: String = "2026-08-24", ends: String = "2026-12-09") =
        PersonalCommitment(id, "Campus work", days, start, end, starts, ends)

    private fun saved(starts: String = "2026-08-24", ends: String = "2026-12-09") = SavedClass(
        "nd", "202610", "Fall 2026", CourseSection("", "CSE 20311", "Computing", "01", "12832",
            "", "", "", starts, ends, ""), creditHours = "4")

    @Test fun requiresTitleDaysIncreasingTimesAndRealDates() {
        val valid = commitment()
        assertNull(commitmentValidation(valid))
        assertNotNull(commitmentValidation(valid.copy(title = " ")))
        assertNotNull(commitmentValidation(valid.copy(title = "x".repeat(81))))
        assertNotNull(commitmentValidation(valid.copy(days = emptySet())))
        assertNotNull(commitmentValidation(valid.copy(days = setOf(7))))
        assertNotNull(commitmentValidation(valid.copy(startMinute = -1)))
        assertNotNull(commitmentValidation(valid.copy(endMinute = valid.startMinute)))
        assertNotNull(commitmentValidation(valid.copy(startMinute = 1380, endMinute = 60)))
        assertNotNull(commitmentValidation(valid.copy(starts = "2026-02-30")))
        assertNotNull(commitmentValidation(valid.copy(ends = "2026-08-01")))
    }

    @Test fun selectedDaysMustOccurInInclusiveDateRange() {
        assertNotNull(commitmentValidation(commitment(days = setOf(1), starts = "2026-08-24", ends = "2026-08-24")))
        assertNull(commitmentValidation(commitment(days = setOf(0), starts = "2026-08-24", ends = "2026-08-24")))
        assertNull(commitmentValidation(commitment(days = setOf(5, 6), starts = "2026-08-29", ends = "2026-08-30")))
    }

    @Test fun rendersOnlyRealRecurringOccurrencesAndNeverInventsClasses() {
        val personal = commitment(days = setOf(0, 1), starts = "2026-08-24", ends = "2026-08-24")
        val blocks = scheduleBlocks(emptyList(), listOf(personal, personal))
        assertEquals(1, blocks.size)
        assertEquals(0, blocks.single().day)
        assertEquals(personal, blocks.single().commitment)
        assertNull(blocks.single().saved)
    }

    @Test fun personalTimeConflictsWithClassWithoutChangingSectionOrCreditTotals() {
        val course = saved()
        val meetings = listOf(CalendarMeeting(course, 0, 565, 615), CalendarMeeting(course, 2, 565, 615))
        val insights = scheduleInsights(listOf(course), meetings, listOf(commitment()))
        assertEquals(1, insights.courseCount)
        assertEquals(1, insights.sectionCount)
        assertEquals(1, insights.conflictCount)
        assertEquals(setOf(course.id), insights.conflictingClassIds)
        assertEquals(setOf("work"), insights.conflictingCommitmentIds)
        assertEquals("4 credits", creditTotal(listOf(course)))
    }

    @Test fun personalOnlyScheduleHasFreeDaysAndNoCourseCounts() {
        val insights = scheduleInsights(emptyList(), emptyList(), listOf(commitment(days = setOf(1, 3))))
        assertEquals(0, insights.courseCount)
        assertEquals(0, insights.sectionCount)
        assertEquals(setOf(0, 2, 4), insights.freeWeekdays)
        assertEquals(570, insights.earliestStartMinutes)
        assertEquals(630, insights.latestEndMinutes)
    }

    @Test fun personalPairsCountOnceAcrossRepeatedDaysAndAdjacentTimesDoNotConflict() {
        val first = commitment()
        val second = commitment(id = "practice", start = 600, end = 660)
        val adjacent = commitment(id = "study", start = 660, end = 720)
        val insights = scheduleInsights(emptyList(), emptyList(), listOf(first, second, adjacent))
        assertEquals(1, insights.conflictCount)
        assertEquals(setOf("work", "practice"), insights.conflictingCommitmentIds)
    }

    @Test fun disjointDateRangesAndSharedRangesWithoutMeetingWeekdayDoNotConflict() {
        val course = saved(starts = "2026-08-24", ends = "2026-08-26")
        val meetings = listOf(CalendarMeeting(course, 0, 565, 615))
        val after = commitment(starts = "2026-08-27", ends = "2026-09-07")
        val onlySharedWednesday = commitment(starts = "2026-08-25", ends = "2026-09-07")
        assertEquals(0, scheduleInsights(listOf(course), meetings, listOf(after)).conflictCount)
        assertEquals(0, scheduleInsights(listOf(course), meetings, listOf(onlySharedWednesday)).conflictCount)
    }

    @Test fun unknownClassDatesConservativelyWarnButInvalidPersonalTimeIsNotRendered() {
        val course = saved(starts = "", ends = "")
        val meetings = listOf(CalendarMeeting(course, 0, 565, 615))
        assertEquals(1, scheduleInsights(listOf(course), meetings, listOf(commitment())).conflictCount)
        assertTrue(scheduleBlocks(emptyList(), listOf(commitment(end = 500))).isEmpty())
    }

    @Test fun weekLanesSeparateClassAndPersonalOverlapAndReuseAdjacentSlot() {
        val course = saved()
        val blocks = scheduleBlocks(listOf(CalendarMeeting(course, 0, 565, 615)),
            listOf(commitment(days = setOf(0)), commitment(id = "later", days = setOf(0), start = 630, end = 690)))
        val placements = layoutScheduleBlocks(blocks)
        assertEquals(2, placements.first { it.meeting.saved != null }.laneCount)
        assertEquals(2, placements.first { it.meeting.id == "personal:work" }.laneCount)
        assertEquals(1, placements.first { it.meeting.id == "personal:later" }.laneCount)
    }

    @Test fun editingAndRemovalImmediatelyChangeConflictAndFreeDaySummary() {
        val first = commitment()
        val second = commitment(id = "practice", start = 600, end = 660)
        assertEquals(1, scheduleInsights(emptyList(), emptyList(), listOf(first, second)).conflictCount)
        assertEquals(0, scheduleInsights(emptyList(), emptyList(), listOf(first, second.copy(startMinute = 660, endMinute = 720))).conflictCount)
        assertEquals(setOf(0, 1, 2, 3, 4), scheduleInsights(emptyList(), emptyList(), emptyList()).freeWeekdays)
    }

    @Test fun dateDefaultsPreferClassDatesAndUseLabeledSemesterFallback() {
        val plan = SchedulePlan("plan", "Plan A", "nd", "202610", "Fall Semester 2026")
        assertEquals("2026-08-24" to "2026-12-09", commitmentDateDefaults(plan, listOf(saved())))
        assertEquals("2026-08-01" to "2026-12-31", commitmentDateDefaults(plan, emptyList()))
        assertEquals("2027-01-01" to "2027-05-31", commitmentDateDefaults(plan.copy(semesterLabel = "Spring 2027"), emptyList()))
    }
}
