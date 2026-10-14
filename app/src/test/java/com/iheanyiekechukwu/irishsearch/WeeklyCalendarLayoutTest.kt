package com.iheanyiekechukwu.irishsearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyCalendarLayoutTest {
    @Test fun ordinaryAndShortEventsReserve48dpWithoutChangingTimeProportions() {
        assertEquals(60f, calendarHourHeightDp(50)!!, 0.001f)
        assertEquals(200f, calendarHourHeightDp(15)!!, 0.001f)
        for (duration in 15..180) {
            val hourHeight = calendarHourHeightDp(duration)!!
            assertTrue(hourHeight <= 200f)
            assertTrue(hourHeight * duration / 60f - 2f >= 47.999f)
        }
    }

    @Test fun veryShortEventsUseListInsteadOfAnUnboundedCalendar() {
        for (duration in 1..14) assertEquals(null, calendarHourHeightDp(duration))
    }

    @Test fun overlappingMeetingsHaveSeparateLanes() {
        val result = layoutCalendarMeetings(listOf(meeting("a", 540, 600), meeting("b", 570, 630)))
        assertEquals(2, result.size)
        assertEquals(setOf(0, 1), result.map { it.lane }.toSet())
        assertTrue(result.all { it.laneCount == 2 })
    }

    @Test fun adjacentMeetingsReuseLaneWithinAnOverlapGroup() {
        val result = layoutCalendarMeetings(listOf(meeting("long", 540, 720),
            meeting("first", 550, 600), meeting("next", 600, 660)))
        val lanes = result.associate { it.meeting.saved.section.crn to it.lane }
        assertEquals(lanes["first"], lanes["next"])
        assertTrue(lanes["long"] != lanes["first"])
        assertTrue(result.all { it.laneCount == 2 })
    }

    @Test fun separateGroupsUseTheirOwnFullWidth() {
        val result = layoutCalendarMeetings(listOf(meeting("a", 540, 600),
            meeting("b", 570, 600), meeting("later", 600, 660)))
        val later = result.single { it.meeting.saved.section.crn == "later" }
        assertEquals(0, later.lane)
        assertEquals(1, later.laneCount)
    }

    @Test fun separateDateRangesStillNeedVisibleLanesInAWeeklyOverview() {
        val early = meeting("a", 540, 600, starts = "2026-08-24", ends = "2026-10-09")
        val late = meeting("b", 540, 600, starts = "2026-10-12", ends = "2026-12-04")
        val result = layoutCalendarMeetings(listOf(early, late))
        assertEquals(2, result.map { it.lane }.toSet().size)
        assertTrue(result.all { it.laneCount == 2 })
    }

    @Test fun placementIsStableAcrossInputOrderAndIndependentAcrossDays() {
        val meetings = listOf(meeting("b", 540, 600), meeting("a", 540, 600),
            meeting("tuesday", 540, 600, day = 1))
        assertEquals(layoutCalendarMeetings(meetings), layoutCalendarMeetings(meetings.reversed()))
        val tuesday = layoutCalendarMeetings(meetings).single { it.meeting.day == 1 }
        assertEquals(0, tuesday.lane)
        assertEquals(1, tuesday.laneCount)
    }

    @Test fun noTimeOverlapsShareALaneAndEveryDistinctMeetingIsPlaced() {
        val meetings = listOf(meeting("a", 540, 660), meeting("b", 550, 570),
            meeting("c", 555, 610), meeting("d", 570, 590), meeting("e", 590, 650))
        val result = layoutCalendarMeetings(meetings)
        assertEquals(meetings.toSet(), result.map { it.meeting }.toSet())
        result.forEachIndexed { index, first ->
            result.drop(index + 1).filter { it.lane == first.lane }.forEach { second ->
                assertTrue(first.meeting.start >= second.meeting.end || second.meeting.start >= first.meeting.end)
            }
        }
        assertTrue(result.all { it.laneCount == 3 })
    }

    @Test fun duplicateAndInvalidMeetingsDoNotAddPhantomLanes() {
        val valid = meeting("a", 540, 600)
        val result = layoutCalendarMeetings(listOf(valid, valid,
            meeting("invalid", 600, 600), meeting("bad-day", 540, 600, day = 7),
            meeting("bad-start", -1, 600), meeting("bad-end", 540, 1441)))
        assertEquals(listOf(CalendarMeetingPlacement(valid, 0, 1)), result)
    }

    private fun meeting(
        crn: String,
        start: Int,
        end: Int,
        day: Int = 0,
        starts: String = "2026-08-24",
        ends: String = "2026-12-04",
    ) = CalendarMeeting(SavedClass(
        schoolId = "nd", semesterCode = "202610", semesterLabel = "Fall 2026",
        section = CourseSection("", "CSE 20311", "Computer Science", "01", crn,
            "", "", "", starts, ends, ""),
    ), day, start, end)
}
