package com.iheanyiekechukwu.irishsearch

import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Locale

internal data class EnrollmentSummary(
    val maximum: Int?,
    val available: Int?,
    val waitlistTotal: Int?,
    val waitlistAvailable: Int?,
) {
    val enrolled: Int? get() = if (maximum != null && available != null)
        (maximum - available).coerceIn(0, maximum) else null
}

internal fun parseEnrollment(text: String): EnrollmentSummary {
    fun number(vararg labels: String): Int? = labels.firstNotNullOfOrNull { label ->
        Regex("${Regex.escape(label)}\\s*:?\\s*(\\d+)", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)?.toIntOrNull()
    }
    return EnrollmentSummary(
        maximum = number("Maximum Enrollment"),
        available = number("Seats Avail", "Seats Available"),
        waitlistTotal = number("Waitlist Total"),
        waitlistAvailable = number("Waitlist Avail", "Waitlist Available"),
    )
}

private val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

private fun clockLabel(minutes: Int): String {
    val hour = minutes / 60
    val minute = minutes % 60
    return "${if (hour % 12 == 0) 12 else hour % 12}:${minute.toString().padStart(2, '0')} ${if (hour < 12) "AM" else "PM"}"
}

internal fun readableMeetings(section: CourseSection): List<String> = runCatching {
    val meetings = JSONArray(section.meetingTimes)
    val byTime = linkedMapOf<Pair<Int, Int>, MutableList<Int>>()
    for (index in 0 until meetings.length()) {
        val meeting = meetings.optJSONObject(index) ?: continue
        val day = meeting.optInt("meet_day", -1)
        if (day !in dayNames.indices) continue
        val start = parsePathClock(meeting.optString("start_time")) ?: continue
        val end = parsePathClock(meeting.optString("end_time")) ?: continue
        byTime.getOrPut(start to end) { mutableListOf() }.add(day)
    }
    byTime.map { (times, days) ->
        val names = days.distinct().sorted().map { dayNames[it] }
        val dayText = when (names.size) {
            0 -> ""
            1 -> names.first()
            2 -> names.joinToString(" & ")
            else -> "${names.dropLast(1).joinToString(", ")} & ${names.last()}"
        }
        "$dayText · ${clockLabel(times.first)}–${clockLabel(times.second)}"
    }
}.getOrDefault(emptyList())

internal fun readableDateRange(section: CourseSection): String? = runCatching {
    val input = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    val start = input.parse(section.starts) ?: return@runCatching null
    val end = input.parse(section.ends) ?: return@runCatching null
    val year = SimpleDateFormat("yyyy", Locale.US)
    val first = SimpleDateFormat("MMM d", Locale.US)
    val last = SimpleDateFormat("MMM d, yyyy", Locale.US)
    if (year.format(start) == year.format(end))
        "${first.format(start)}–${last.format(end)}"
    else "${last.format(start)}–${last.format(end)}"
}.getOrNull()
