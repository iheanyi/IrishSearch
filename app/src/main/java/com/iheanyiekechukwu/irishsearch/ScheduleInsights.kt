package com.iheanyiekechukwu.irishsearch

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

internal data class ScheduleInsights(
    val courseCount: Int,
    val sectionCount: Int,
    val unknownMeetingClassCount: Int,
    val conflictingClassIds: Set<String>,
    /** Unique pairs of classes or personal commitments, even when a pair overlaps on several days. */
    val conflictCount: Int,
    /** Several selected sections of one course may represent alternatives or required components. */
    val duplicateCourseSections: Map<String, List<SavedClass>>,
    val earliestStartMinutes: Int?,
    val latestEndMinutes: Int?,
    /** Days without known meetings. Unknown meeting times mean these days are not confirmed free. */
    val freeWeekdays: Set<Int>,
    val conflictingCommitmentIds: Set<String> = emptySet(),
)

/** Summarizes selected sections; duplicate saved records and unrelated meetings do not inflate totals. */
internal fun scheduleInsights(
    classes: List<SavedClass>,
    meetings: List<CalendarMeeting> = classes.flatMap(::meetingsFor),
    commitments: List<PersonalCommitment> = emptyList(),
): ScheduleInsights {
    val sections = classes.distinctBy { it.id }
    val sectionIds = sections.mapTo(mutableSetOf()) { it.id }
    val knownMeetings = meetings.filter { it.saved.id in sectionIds && validMeeting(it) }
        .distinctBy { listOf(it.saved.id, it.day, it.start, it.end) }
    val classesWithMeetings = knownMeetings.mapTo(mutableSetOf()) { it.saved.id }
    val courses = sections.groupBy { saved ->
        listOf(saved.schoolId, saved.semesterCode, normalizedCourseCode(saved).ifBlank { saved.id })
    }
    val blocks = scheduleBlocks(knownMeetings, commitments)
    val conflicts = scheduleBlockConflictPairs(blocks)
    val conflictingIds = conflicts.flatten().toSet()
    return ScheduleInsights(
        courseCount = courses.size,
        sectionCount = sections.size,
        unknownMeetingClassCount = sections.count { it.id !in classesWithMeetings },
        conflictingClassIds = conflictingIds.intersect(sectionIds),
        conflictCount = conflicts.size,
        duplicateCourseSections = courses.values.filter {
            it.size > 1 && it.first().section.code.isNotBlank()
        }.associate { group -> normalizedCourseCode(group.first()) to group },
        earliestStartMinutes = blocks.minOfOrNull { it.start },
        latestEndMinutes = blocks.maxOfOrNull { it.end },
        freeWeekdays = (0..4).filterTo(linkedSetOf()) { day -> blocks.none { it.day == day } },
        conflictingCommitmentIds = commitments.filter { "personal:${it.id}" in conflictingIds }.mapTo(linkedSetOf()) { it.id },
    )
}

/** Missing or malformed date ranges cannot establish that two meetings are safely separated. */
internal fun scheduleMeetingsConflict(first: CalendarMeeting, second: CalendarMeeting): Boolean {
    if (!validMeeting(first) || !validMeeting(second) || first.saved.id == second.saved.id ||
        first.saved.schoolId != second.saved.schoolId ||
        first.saved.semesterCode != second.saved.semesterCode || first.day != second.day ||
        first.start >= second.end || second.start >= first.end
    ) return false

    val firstDates = meetingDateRange(first.saved.section) ?: return true
    val secondDates = meetingDateRange(second.saved.section) ?: return true
    val start = maxOf(firstDates.first, secondDates.first)
    val end = minOf(firstDates.last, secondDates.last)
    if (start > end) return false

    // Meeting weekdays use Monday=0. A shared range must contain that weekday to overlap.
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).apply {
        timeInMillis = start * MillisecondsPerDay
    }
    val weekday = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val daysUntilMeeting = (first.day - weekday + 7) % 7
    return start + daysUntilMeeting <= end
}

internal fun scheduleConflictingIds(meetings: List<CalendarMeeting>): Set<String> =
    conflictingSectionPairs(meetings).flatMapTo(linkedSetOf()) { it }

private fun conflictingSectionPairs(meetings: List<CalendarMeeting>): Set<Set<String>> = buildSet {
    meetings.forEachIndexed { index, first ->
        for (secondIndex in index + 1 until meetings.size) {
            val second = meetings[secondIndex]
            if (scheduleMeetingsConflict(first, second)) add(setOf(first.saved.id, second.saved.id))
        }
    }
}

private fun normalizedCourseCode(saved: SavedClass): String =
    saved.section.code.trim().replace(Regex("\\s+"), " ").uppercase(Locale.ROOT)

private fun validMeeting(meeting: CalendarMeeting): Boolean =
    meeting.day in 0..6 && meeting.start in 0 until 24 * 60 &&
        meeting.end in 1..24 * 60 && meeting.end > meeting.start

private const val MillisecondsPerDay = 86_400_000L

private fun meetingDateRange(section: CourseSection): LongRange? {
    val start = dateEpochDay(section.starts) ?: return null
    val end = dateEpochDay(section.ends) ?: return null
    return (start..end).takeIf { start <= end }
}

internal fun dateEpochDay(raw: String): Long? {
    if (!raw.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return null
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val position = ParsePosition(0)
    val date = formatter.parse(raw, position) ?: return null
    if (position.index != raw.length) return null
    return Math.floorDiv(date.time, MillisecondsPerDay)
}
