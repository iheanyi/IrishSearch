package com.iheanyiekechukwu.irishsearch

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** A recurring personal block in one semester plan. Weekdays use Monday=0. */
data class PersonalCommitment(
    val id: String,
    val title: String,
    val days: Set<Int>,
    val startMinute: Int,
    val endMinute: Int,
    val starts: String,
    val ends: String,
)

internal fun commitmentValidation(commitment: PersonalCommitment): String? = when {
    commitment.title.isBlank() -> "Enter a name for this commitment."
    commitment.title.length > 80 -> "Keep the name to 80 characters or fewer."
    commitment.days.isEmpty() || commitment.days.any { it !in 0..6 } -> "Choose at least one day."
    commitment.startMinute !in 0..1439 || commitment.endMinute !in 1..1440 ||
        commitment.endMinute <= commitment.startMinute -> "End time must be after start time on the same day."
    dateEpochDay(commitment.starts) == null || dateEpochDay(commitment.ends) == null -> "Choose valid start and end dates."
    dateEpochDay(commitment.ends)!! < dateEpochDay(commitment.starts)!! -> "End date must be on or after start date."
    commitment.days.none { day -> dateRangeContainsWeekday(commitment.starts, commitment.ends, day) } ->
        "The date range must include one of the selected days."
    else -> null
}

/** Shared presentation geometry; personal blocks never masquerade as registered classes. */
internal data class ScheduleBlock(
    val id: String,
    val title: String,
    val subtitle: String,
    val day: Int,
    val start: Int,
    val end: Int,
    val starts: String,
    val ends: String,
    val saved: SavedClass? = null,
    val commitment: PersonalCommitment? = null,
)

internal fun scheduleBlocks(
    meetings: List<CalendarMeeting>,
    commitments: List<PersonalCommitment> = emptyList(),
): List<ScheduleBlock> = buildList {
    meetings.filter { it.day in 0..6 && it.start in 0..1439 && it.end in 1..1440 && it.end > it.start }
        .forEach { meeting ->
            val section = meeting.saved.section
            add(ScheduleBlock(meeting.saved.id, section.code,
                listOf(section.section.takeIf(String::isNotBlank)?.let { "Section $it" }, section.title)
                    .filterNotNull().joinToString(" · "),
                meeting.day, meeting.start, meeting.end, section.starts, section.ends, saved = meeting.saved))
        }
    commitments.filter { commitmentValidation(it) == null }.distinctBy { it.id }.forEach { commitment ->
        commitment.days.sorted().filter { dateRangeContainsWeekday(commitment.starts, commitment.ends, it) }
            .forEach { day -> add(ScheduleBlock("personal:${commitment.id}", commitment.title, "Personal commitment",
                day, commitment.startMinute, commitment.endMinute, commitment.starts, commitment.ends,
                commitment = commitment)) }
    }
}.distinctBy { listOf(it.id, it.day, it.start, it.end) }

internal fun scheduleBlocksConflict(first: ScheduleBlock, second: ScheduleBlock): Boolean {
    if (first.id == second.id || first.day != second.day || first.start >= second.end || second.start >= first.end) return false
    if (first.saved != null && second.saved != null &&
        (first.saved.schoolId != second.saved.schoolId || first.saved.semesterCode != second.saved.semesterCode)) return false
    val firstStart = dateEpochDay(first.starts) ?: return true
    val firstEnd = dateEpochDay(first.ends) ?: return true
    val secondStart = dateEpochDay(second.starts) ?: return true
    val secondEnd = dateEpochDay(second.ends) ?: return true
    if (firstStart > firstEnd || secondStart > secondEnd) return true
    return epochRangeContainsWeekday(maxOf(firstStart, secondStart), minOf(firstEnd, secondEnd), first.day)
}

internal fun scheduleBlockConflictPairs(blocks: List<ScheduleBlock>): Set<Set<String>> = buildSet {
    blocks.forEachIndexed { index, first ->
        blocks.drop(index + 1).forEach { second ->
            if (scheduleBlocksConflict(first, second)) add(setOf(first.id, second.id))
        }
    }
}

private fun dateRangeContainsWeekday(starts: String, ends: String, day: Int): Boolean {
    return epochRangeContainsWeekday(dateEpochDay(starts) ?: return false, dateEpochDay(ends) ?: return false, day)
}

private fun epochRangeContainsWeekday(start: Long, end: Long, day: Int): Boolean {
    if (start > end) return false
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).apply { timeInMillis = start * 86_400_000L }
    val weekday = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    return start + (day - weekday + 7) % 7 <= end
}

/** Class dates are preferred; fallback dates are editable semester planning bounds, not official dates. */
internal fun commitmentDateDefaults(plan: SchedulePlan, classes: List<SavedClass>): Pair<String, String> {
    val starts = classes.map { it.section.starts }.filter { dateEpochDay(it) != null }
    val ends = classes.map { it.section.ends }.filter { dateEpochDay(it) != null }
    if (starts.isNotEmpty() && ends.isNotEmpty() && starts.min() <= ends.max()) return starts.min() to ends.max()
    val year = Regex("(?:19|20)\\d{2}").find(plan.semesterLabel)?.value
        ?: plan.semesterCode.take(4).takeIf { (it.toIntOrNull() ?: 0) in 1900..2099 }
        ?: Calendar.getInstance().get(Calendar.YEAR).toString()
    return when {
        plan.semesterLabel.contains("fall", ignoreCase = true) -> "$year-08-01" to "$year-12-31"
        plan.semesterLabel.contains("spring", ignoreCase = true) -> "$year-01-01" to "$year-05-31"
        plan.semesterLabel.contains("summer", ignoreCase = true) -> "$year-06-01" to "$year-08-31"
        else -> "$year-01-01" to "$year-12-31"
    }
}

internal fun commitmentTime(minutes: Int): String {
    val hour = (minutes / 60) % 24
    return "${if (hour % 12 == 0) 12 else hour % 12}:${(minutes % 60).toString().padStart(2, '0')} ${if (hour < 12) "AM" else "PM"}"
}
