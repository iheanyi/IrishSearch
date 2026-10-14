package com.iheanyiekechukwu.irishsearch

import org.json.JSONArray

internal data class MeetingSlot(val id: String, val label: String, val start: Int, val end: Int)

internal object MeetingPreferences {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val slots = listOf(
        MeetingSlot("morning", "8:00 AM–12:00 PM", 8 * 60, 12 * 60),
        MeetingSlot("midday", "12:00–3:00 PM", 12 * 60, 15 * 60),
        MeetingSlot("afternoon", "3:00–6:00 PM", 15 * 60, 18 * 60),
        MeetingSlot("evening", "6:00–10:00 PM", 18 * 60, 22 * 60),
    )

    /** Every meeting must fit the selected days and combined time windows. */
    fun matches(section: CourseSection, selectedDays: Set<Int>, selectedSlots: Set<String>): Boolean =
        runCatching {
            val meetings = JSONArray(section.meetingTimes)
            if (meetings.length() == 0) return false
            (0 until meetings.length()).all { index ->
                val meeting = meetings.optJSONObject(index) ?: return@all false
                val day = meeting.optInt("meet_day", -1)
                val start = parsePathClock(meeting.optString("start_time")) ?: return@all false
                val end = parsePathClock(meeting.optString("end_time")) ?: return@all false
                acceptsMeeting(day, start, end, selectedDays, selectedSlots)
            }
        }.getOrDefault(false)

    internal fun acceptsMeeting(
        day: Int,
        start: Int,
        end: Int,
        selectedDays: Set<Int>,
        selectedSlots: Set<String>,
    ): Boolean = day in 0..6 && end > start &&
        (selectedDays.isEmpty() || day in selectedDays) &&
        (selectedSlots.isEmpty() || fitsWindows(start, end,
            slots.filter { it.id in selectedSlots }.sortedBy { it.start }))

    private fun fitsWindows(start: Int, end: Int, windows: List<MeetingSlot>): Boolean {
        var coveredUntil = start
        for (window in windows) {
            if (window.start > coveredUntil) break
            if (window.end > coveredUntil) coveredUntil = window.end
            if (coveredUntil >= end) return true
        }
        return false
    }
}
