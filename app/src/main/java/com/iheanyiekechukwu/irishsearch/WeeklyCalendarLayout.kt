package com.iheanyiekechukwu.irishsearch

internal data class CalendarMeetingPlacement(
    val meeting: CalendarMeeting,
    val lane: Int,
    val laneCount: Int,
)

/** Null uses a readable list rather than stretching an hour beyond 200dp. */
internal fun calendarHourHeightDp(shortestMeetingMinutes: Int): Float? =
    if (shortestMeetingMinutes < 15) null
    else maxOf(56f, 50f * 60f / shortestMeetingMinutes)

/**
 * Places overlapping time ranges side by side in a weekly overview.
 * Dates deliberately do not affect placement: sections in separate date ranges still occupy
 * the same visible weekday and time. Actual scheduling conflicts are calculated separately.
 */
internal fun layoutCalendarMeetings(meetings: List<CalendarMeeting>): List<CalendarMeetingPlacement> =
    buildList {
        meetings.filter {
            it.day in 0..6 && it.start in 0 until 24 * 60 &&
                it.end in 1..24 * 60 && it.start < it.end
        }.distinctBy { listOf(it.saved.id, it.day, it.start, it.end) }
            .groupBy { it.day }.toSortedMap().values.forEach { dayMeetings ->
                val sorted = dayMeetings.sortedWith(compareBy<CalendarMeeting> { it.start }
                    .thenBy { it.end }.thenBy { it.saved.id })
                val group = mutableListOf<Pair<CalendarMeeting, Int>>()
                val laneEnds = mutableListOf<Int>()
                var groupEnd = -1

                fun finishGroup() {
                    group.forEach { (meeting, lane) ->
                        add(CalendarMeetingPlacement(meeting, lane, laneEnds.size))
                    }
                    group.clear()
                    laneEnds.clear()
                }

                sorted.forEach { meeting ->
                    if (meeting.start >= groupEnd) finishGroup()
                    val reusable = laneEnds.indexOfFirst { it <= meeting.start }
                    val lane = if (reusable >= 0) reusable else laneEnds.size
                    if (lane == laneEnds.size) laneEnds.add(meeting.end)
                    else laneEnds[lane] = meeting.end
                    group.add(meeting to lane)
                    groupEnd = maxOf(groupEnd, meeting.end)
                }
                finishGroup()
            }
    }
