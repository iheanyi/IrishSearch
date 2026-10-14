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

internal data class ScheduleBlockPlacement(val meeting: ScheduleBlock, val lane: Int, val laneCount: Int)

/** Dates do not affect lanes: all recurring ranges share the same visible weekday grid. */
internal fun layoutCalendarMeetings(meetings: List<CalendarMeeting>): List<CalendarMeetingPlacement> =
    layoutTimeItems(meetings, { it.saved.id }, { it.day }, { it.start }, { it.end })
        .map { CalendarMeetingPlacement(it.item, it.lane, it.laneCount) }

internal fun layoutScheduleBlocks(blocks: List<ScheduleBlock>): List<ScheduleBlockPlacement> =
    layoutTimeItems(blocks, { it.id }, { it.day }, { it.start }, { it.end })
        .map { ScheduleBlockPlacement(it.item, it.lane, it.laneCount) }

private data class TimePlacement<T>(val item: T, val lane: Int, val laneCount: Int)

private fun <T> layoutTimeItems(
    items: List<T>, id: (T) -> String, day: (T) -> Int, start: (T) -> Int, end: (T) -> Int,
): List<TimePlacement<T>> = buildList {
    items.filter { day(it) in 0..6 && start(it) in 0..1439 && end(it) in 1..1440 && start(it) < end(it) }
        .distinctBy { listOf(id(it), day(it), start(it), end(it)) }
        .groupBy(day).toSortedMap().values.forEach { dayItems ->
            val sorted = dayItems.sortedWith(compareBy<T> { start(it) }.thenBy { end(it) }.thenBy { id(it) })
            val group = mutableListOf<Pair<T, Int>>()
            val laneEnds = mutableListOf<Int>()
            var groupEnd = -1
            fun finishGroup() {
                group.forEach { (item, lane) -> add(TimePlacement(item, lane, laneEnds.size)) }
                group.clear()
                laneEnds.clear()
            }
            sorted.forEach { item ->
                if (start(item) >= groupEnd) finishGroup()
                val reusable = laneEnds.indexOfFirst { it <= start(item) }
                val lane = if (reusable >= 0) reusable else laneEnds.size
                if (lane == laneEnds.size) laneEnds.add(end(item)) else laneEnds[lane] = end(item)
                group.add(item to lane)
                groupEnd = maxOf(groupEnd, end(item))
            }
            finishGroup()
        }
}
