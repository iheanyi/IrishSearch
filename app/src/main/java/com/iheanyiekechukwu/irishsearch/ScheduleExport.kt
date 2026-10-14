package com.iheanyiekechukwu.irishsearch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.UUID

internal const val ScheduleTimeZone = "America/Indiana/Indianapolis"
private val CampusZone = ZoneId.of(ScheduleTimeZone)
private val CalendarLocalTime = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss", Locale.US)
private val CalendarUtcTime = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US)
    .withZone(ZoneOffset.UTC)
private val ExportWeekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
private val CalendarWeekdays = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")

internal data class ScheduleExportSummary(
    /** One recurrence series per distinct meeting time and weekday. */
    val eventCount: Int,
    val skippedClassCount: Int,
    val skippedCommitmentCount: Int,
    val skippedMeetingCount: Int,
) {
    val message: String get() = buildList {
        add("$eventCount calendar series")
        if (skippedClassCount > 0) add("$skippedClassCount ${if (skippedClassCount == 1) "class" else "classes"} without usable dates or times")
        if (skippedCommitmentCount > 0) add("$skippedCommitmentCount ${if (skippedCommitmentCount == 1) "commitment" else "commitments"} without dated meetings")
        if (skippedMeetingCount > 0) add("$skippedMeetingCount meeting series outside its date range")
    }.joinToString(" · ")
}

internal fun scheduleExportSummary(plan: SchedulePlan, classes: List<SavedClass>): ScheduleExportSummary {
    val selected = exportClasses(plan, classes)
    val events = scheduleExportEvents(plan, selected)
    val exportedClassIds = events.filterNot { it.personal }.mapTo(mutableSetOf()) { it.sourceId }
    val exportedCommitmentIds = events.filter { it.personal }.mapTo(mutableSetOf()) { it.sourceId }
    val skippedMeetings = selected.sumOf { saved ->
        val dates = exportDateRange(saved.section.starts, saved.section.ends)
        if (dates == null) 0 else meetingsFor(saved).distinctBy { listOf(it.day, it.start, it.end) }
            .count { firstOccurrence(dates.first, dates.second, it.day) == null }
    }
    return ScheduleExportSummary(events.size, selected.count { it.id !in exportedClassIds },
        plan.commitments.count { it.id !in exportedCommitmentIds }, skippedMeetings)
}

/** RFC 5545 text, UTF-8, CRLF and 75-octet content-line folding. No dates or times are invented. */
internal fun exportScheduleIcs(
    plan: SchedulePlan,
    classes: List<SavedClass>,
    generatedAt: Instant = Instant.now(),
): String {
    val events = scheduleExportEvents(plan, exportClasses(plan, classes))
    require(events.isNotEmpty()) { "This schedule has no meetings with published dates and times to export." }
    val lines = buildList {
        add("BEGIN:VCALENDAR")
        add("VERSION:2.0")
        add("PRODID:-//IrishSearch//Schedule Export//EN")
        add("CALSCALE:GREGORIAN")
        add("X-WR-CALNAME:${calendarText(plan.name)}")
        add("X-WR-TIMEZONE:$ScheduleTimeZone")
        addAll(calendarTimeZone(events.minOf { it.starts }, events.maxOf { it.ends }))
        for (event in events) {
            val first = firstOccurrence(event.starts, event.ends, event.day) ?: continue
            val last = first.plusWeeks(java.time.temporal.ChronoUnit.DAYS.between(first, event.ends) / 7)
            val start = localEventTime(first, event.startMinute)
            val end = localEventTime(first, event.endMinute)
            val until = localEventTime(last, event.startMinute).atZone(CampusZone).toInstant()
            val uid = UUID.nameUUIDFromBytes(
                listOf(plan.id, event.sourceId, event.personal, event.day, event.startMinute, event.endMinute)
                    .joinToString("|").toByteArray(Charsets.UTF_8),
            )
            add("BEGIN:VEVENT")
            add("UID:$uid@irishsearch")
            add("DTSTAMP:${CalendarUtcTime.format(generatedAt)}")
            add("DTSTART;TZID=$ScheduleTimeZone:${CalendarLocalTime.format(start)}")
            add("DTEND;TZID=$ScheduleTimeZone:${CalendarLocalTime.format(end)}")
            add("RRULE:FREQ=WEEKLY;BYDAY=${CalendarWeekdays[event.day]};UNTIL=${CalendarUtcTime.format(until)}")
            add("SUMMARY:${calendarText(event.title)}")
            add("DESCRIPTION:${calendarText(event.description)}")
            add("END:VEVENT")
        }
        add("END:VCALENDAR")
    }
    return lines.joinToString(separator = "\r\n", postfix = "\r\n", transform = ::foldCalendarLine)
}

private data class ExportEvent(
    val sourceId: String,
    val title: String,
    val description: String,
    val day: Int,
    val startMinute: Int,
    val endMinute: Int,
    val starts: LocalDate,
    val ends: LocalDate,
    val personal: Boolean,
)

private fun exportClasses(plan: SchedulePlan, classes: List<SavedClass>): List<SavedClass> =
    classes.filter { it.id in plan.classIds && it.schoolId == plan.schoolId &&
        it.semesterCode == plan.semesterCode }.distinctBy { it.id }

private fun scheduleExportEvents(plan: SchedulePlan, classes: List<SavedClass>): List<ExportEvent> = buildList {
    classes.forEach { saved ->
        val section = saved.section
        val dates = exportDateRange(section.starts, section.ends) ?: return@forEach
        meetingsFor(saved).distinctBy { listOf(it.day, it.start, it.end) }.forEach { meeting ->
            if (firstOccurrence(dates.first, dates.second, meeting.day) != null) {
                add(ExportEvent(saved.id, section.code + " · " + section.title,
                    buildList {
                        add("Schedule: ${plan.name}")
                        add("${plan.semesterLabel} · Section ${section.section} · CRN ${section.crn}")
                        if (section.instructor.isNotBlank()) add("Instructor: ${section.instructor}")
                        if (saved.creditHours.isNotBlank()) add("Credits: ${saved.creditHours}")
                        add("Published dates: ${section.starts} through ${section.ends}")
                    }.joinToString("\n"), meeting.day, meeting.start, meeting.end,
                    dates.first, dates.second, false))
            }
        }
    }
    plan.commitments.forEach { commitment ->
        val dates = exportDateRange(commitment.starts, commitment.ends) ?: return@forEach
        if (commitment.startMinute !in 0..1439 || commitment.endMinute !in 1..1440 ||
            commitment.startMinute >= commitment.endMinute) return@forEach
        commitment.days.sorted().filter { it in 0..6 }.forEach { day ->
            if (firstOccurrence(dates.first, dates.second, day) != null) {
                add(ExportEvent(commitment.id, commitment.title,
                    "Personal commitment\nSchedule: ${plan.name}\nDates: ${commitment.starts} through ${commitment.ends}",
                    day, commitment.startMinute, commitment.endMinute, dates.first, dates.second, true))
            }
        }
    }
}.distinctBy { listOf(it.sourceId, it.personal, it.day, it.startMinute, it.endMinute, it.starts, it.ends) }
    .sortedWith(compareBy({ it.personal }, { it.sourceId }, { it.day }, { it.startMinute }, { it.endMinute }))

private fun exportDateRange(starts: String, ends: String): Pair<LocalDate, LocalDate>? = runCatching {
    require(starts.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) && ends.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    val first = LocalDate.parse(starts)
    val last = LocalDate.parse(ends)
    require(first <= last && first.year in 1900..2200 && last.year in 1900..2200)
    first to last
}.getOrNull()

private fun firstOccurrence(starts: LocalDate, ends: LocalDate, day: Int): LocalDate? {
    if (day !in 0..6) return null
    val first = starts.plusDays(((day - (starts.dayOfWeek.value - 1) + 7) % 7).toLong())
    return first.takeIf { it <= ends }
}

private fun localEventTime(date: LocalDate, minutes: Int): LocalDateTime =
    date.atStartOfDay().plusMinutes(minutes.toLong())

private fun calendarText(raw: String): String = raw.replace("\r\n", "\n").replace('\r', '\n')
    .filter { it == '\n' || it == '\t' || it.code >= 32 }
    .replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")

private fun foldCalendarLine(raw: String): String {
    val result = StringBuilder()
    var bytes = 0
    var offset = 0
    while (offset < raw.length) {
        val codePoint = raw.codePointAt(offset)
        val character = String(Character.toChars(codePoint))
        val characterBytes = character.toByteArray(Charsets.UTF_8).size
        if (bytes + characterBytes > 75) {
            result.append("\r\n ")
            bytes = 1
        }
        result.append(character)
        bytes += characterBytes
        offset += Character.charCount(codePoint)
    }
    return result.toString()
}

/** Explicit timezone observances cover the complete export range, including historical offsets. */
private fun calendarTimeZone(starts: LocalDate, ends: LocalDate): List<String> = buildList {
    val baseline = LocalDate.of(starts.year - 1, 1, 1).atStartOfDay(CampusZone)
    val limit = LocalDate.of(ends.year + 1, 12, 31).atTime(LocalTime.MAX).atZone(CampusZone).toInstant()
    val rules = CampusZone.rules
    add("BEGIN:VTIMEZONE")
    add("TZID:$ScheduleTimeZone")
    val baselineKind = if (rules.isDaylightSavings(baseline.toInstant())) "DAYLIGHT" else "STANDARD"
    add("BEGIN:$baselineKind")
    add("DTSTART:${CalendarLocalTime.format(baseline.toLocalDateTime())}")
    add("TZOFFSETFROM:${calendarOffset(baseline.offset)}")
    add("TZOFFSETTO:${calendarOffset(baseline.offset)}")
    add("END:$baselineKind")
    var cursor = baseline.toInstant()
    while (true) {
        val transition = rules.nextTransition(cursor) ?: break
        if (transition.instant > limit) break
        val kind = if (rules.isDaylightSavings(transition.instant.plusSeconds(1))) "DAYLIGHT" else "STANDARD"
        add("BEGIN:$kind")
        add("DTSTART:${CalendarLocalTime.format(transition.dateTimeBefore)}")
        add("TZOFFSETFROM:${calendarOffset(transition.offsetBefore)}")
        add("TZOFFSETTO:${calendarOffset(transition.offsetAfter)}")
        add("END:$kind")
        cursor = transition.instant.plusSeconds(1)
    }
    add("END:VTIMEZONE")
}

private fun calendarOffset(offset: ZoneOffset): String {
    val seconds = kotlin.math.abs(offset.totalSeconds)
    val sign = if (offset.totalSeconds < 0) "-" else "+"
    val base = String.format(Locale.US, "%s%02d%02d", sign, seconds / 3600, seconds / 60 % 60)
    return if (seconds % 60 == 0) base else base + String.format(Locale.US, "%02d", seconds % 60)
}

/** A document image, with the full schedule listed below the active dated week. */
internal suspend fun writeScheduleImage(
    context: Context,
    plan: SchedulePlan,
    classes: List<SavedClass>,
    weekStart: LocalDate? = null,
): File = withContext(Dispatchers.IO) {
    val selected = exportClasses(plan, classes)
    require(selected.size + plan.commitments.size <= 100) { "This schedule is too large for an image. Export a calendar instead." }
    val events = scheduleExportEvents(plan, selected)
    val today = LocalDate.now(CampusZone)
    val preferred = if (events.isNotEmpty() && today in events.minOf { it.starts }..events.maxOf { it.ends })
        today else events.minOfOrNull { firstOccurrence(it.starts, it.ends, it.day)!! } ?: today
    val monday = (weekStart ?: preferred).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val active = events.filter { monday.plusDays(it.day.toLong()) in it.starts..it.ends }
    val placements = layoutExportWeek(active)
    val days = if (events.any { it.day >= 5 }) 7 else 5
    val firstHour = ((active.minOfOrNull { it.startMinute } ?: 480) / 60).coerceIn(0, 23)
    val lastHour = (((active.maxOfOrNull { it.endMinute } ?: 1080) + 59) / 60).coerceIn(firstHour + 1, 24)
    val hourHeight = 144f
    val gridHeight = (lastHour - firstHour) * hourHeight
    val rows = exportDocumentRows(plan, selected, events, monday)
    val width = 1600
    val margin = 64f
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create("sans-serif", Typeface.NORMAL) }
    val rowLines = rows.map { row -> wrapExportText(row, width - margin * 2, text.apply { textSize = 28f }) }
    val ledgerHeight = rowLines.sumOf { (it.size * 38 + 28).toDouble() }.toFloat()
    val headerHeight = 280f
    val height = (headerHeight + gridHeight + 160f + ledgerHeight + 170f).toInt()
    require(height <= 16_000) { "This schedule is too large for an image. Export a calendar instead." }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    try {
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        text.color = Color.rgb(26, 32, 36)
        text.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        text.textSize = 48f
        canvas.drawText(ellipsizeExportText(plan.name, width - margin * 2, text), margin, 88f, text)
        text.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        text.textSize = 30f
        canvas.drawText(ellipsizeExportText("${plan.semesterLabel} · ${creditTotal(selected)}", width - margin * 2, text), margin, 135f, text)
        text.textSize = 27f
        val dateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
        canvas.drawText("${dateFormat.format(monday)} – ${dateFormat.format(monday.plusDays(6))} · Notre Dame time", margin, 180f, text)
        text.color = Color.rgb(88, 99, 107)
        text.textSize = 25f
        canvas.drawText("Only meetings active this week appear in the grid. Full schedule below.", margin, 222f, text)
        val timeWidth = 115f
        val dayWidth = (width - margin * 2 - timeWidth) / days
        val gridTop = headerHeight + 62f
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(206, 216, 222); strokeWidth = 1.5f }
        val shortDate = DateTimeFormatter.ofPattern("MMM d", Locale.US)
        for (day in 0 until days) {
            val x = margin + timeWidth + day * dayWidth + 10f
            text.color = Color.rgb(26, 32, 36)
            text.textSize = 26f
            text.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            canvas.drawText(ExportWeekdays[day].take(3), x, headerHeight + 12f, text)
            text.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            text.textSize = 24f
            text.color = Color.rgb(88, 99, 107)
            canvas.drawText(shortDate.format(monday.plusDays(day.toLong())), x, headerHeight + 45f, text)
            canvas.drawLine(x - 10f, gridTop, x - 10f, gridTop + gridHeight, line)
        }
        for (hour in firstHour..lastHour) {
            val y = gridTop + (hour - firstHour) * hourHeight
            canvas.drawLine(margin + timeWidth, y, width - margin, y, line)
            text.color = Color.rgb(88, 99, 107)
            text.textSize = 24f
            if (hour < lastHour) canvas.drawText(exportClock(hour * 60), margin, y + 25f, text)
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        placements.forEach { placement ->
            val event = placement.event
            val laneWidth = (dayWidth - 12f) / placement.lanes
            val left = margin + timeWidth + event.day * dayWidth + placement.lane * laneWidth + 6f
            val top = gridTop + (event.startMinute - firstHour * 60) / 60f * hourHeight + 2f
            val bottom = gridTop + (event.endMinute - firstHour * 60) / 60f * hourHeight - 2f
            val rect = RectF(left, top, left + laneWidth - 5f, maxOf(top + 6f, bottom))
            val overlapping = active.any { other -> other != event && other.day == event.day &&
                other.startMinute < event.endMinute && other.endMinute > event.startMinute }
            fill.color = when {
                overlapping -> Color.rgb(255, 218, 213)
                event.personal -> Color.rgb(229, 235, 239)
                else -> Color.rgb(221, 233, 240)
            }
            canvas.drawRoundRect(rect, 9f, 9f, fill)
            canvas.save()
            canvas.clipRect(rect)
            text.color = when {
                overlapping -> Color.rgb(111, 33, 27)
                event.personal -> Color.rgb(38, 62, 75)
                else -> Color.rgb(25, 52, 73)
            }
            text.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            text.textSize = 25f
            var y = top + 30f
            val label = if (event.personal) "Personal: ${event.title}" else selected.first { it.id == event.sourceId }.section.code
            if (rect.height() < 72f) {
                // A 15-minute block is 32px high; center one line instead of losing its label.
                text.textSize = 20f
                val metrics = text.fontMetrics
                if (rect.height() >= metrics.descent - metrics.ascent + 4f) {
                    val baseline = rect.centerY() - (metrics.ascent + metrics.descent) / 2f
                    canvas.drawText(ellipsizeExportText(label, maxOf(1f, rect.width() - 16f), text),
                        left + 8f, baseline, text)
                }
            } else {
                wrapExportText(label, maxOf(1f, rect.width() - 16f), text).take(2).forEach { part ->
                    if (y <= bottom - 5f) canvas.drawText(part, left + 8f, y, text)
                    y += 30f
                }
                text.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                text.textSize = 22f
                if (y + 24f <= bottom) canvas.drawText(ellipsizeExportText("${exportClock(event.startMinute)}–${exportClock(event.endMinute)}", maxOf(1f, rect.width() - 16f), text), left + 8f, y + 10f, text)
            }
            canvas.restore()
        }
        if (active.isEmpty()) {
            text.color = Color.rgb(88, 99, 107)
            text.textSize = 30f
            canvas.drawText("No dated meetings this week. See the full schedule below.", margin + timeWidth + 24f, gridTop + 80f, text)
        }
        var y = gridTop + gridHeight + 70f
        text.color = Color.rgb(26, 32, 36)
        text.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        text.textSize = 34f
        canvas.drawText("Full schedule", margin, y, text)
        text.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        text.textSize = 25f
        text.color = Color.rgb(88, 99, 107)
        canvas.drawText("Classes in blue · Personal commitments in grey · Overlaps in red", margin, y + 42f, text)
        y += 96f
        text.textSize = 28f
        text.color = Color.rgb(26, 32, 36)
        rowLines.forEach { parts ->
            parts.forEach { part -> canvas.drawText(part, margin, y, text); y += 38f }
            y += 28f
        }
        text.textSize = 23f
        text.color = Color.rgb(88, 99, 107)
        canvas.drawText("IrishSearch · Planning copy · Published times may change · $ScheduleTimeZone", margin, y + 16f, text)
        val directory = File(context.cacheDir, "shared-schedules")
        require(directory.isDirectory || directory.mkdirs()) { "Could not create the shared schedule." }
        val file = File.createTempFile("irishsearch-schedule-", ".png", directory)
        file.outputStream().use { output ->
            require(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not create the schedule image." }
        }
        file
    } finally {
        bitmap.recycle()
    }
}

private data class ExportPlacement(val event: ExportEvent, val lane: Int, val lanes: Int)

private fun layoutExportWeek(events: List<ExportEvent>): List<ExportPlacement> = buildList {
    events.groupBy { it.day }.toSortedMap().values.forEach { dayEvents ->
        val group = mutableListOf<Pair<ExportEvent, Int>>()
        val laneEnds = mutableListOf<Int>()
        var groupEnd = -1
        fun flush() {
            group.forEach { (event, lane) -> add(ExportPlacement(event, lane, laneEnds.size)) }
            group.clear()
            laneEnds.clear()
        }
        dayEvents.sortedWith(compareBy({ it.startMinute }, { it.endMinute }, { it.sourceId })).forEach { event ->
            if (event.startMinute >= groupEnd) flush()
            val available = laneEnds.indexOfFirst { it <= event.startMinute }
            val lane = if (available >= 0) available else laneEnds.size
            if (lane == laneEnds.size) laneEnds.add(event.endMinute) else laneEnds[lane] = event.endMinute
            group.add(event to lane)
            groupEnd = maxOf(groupEnd, event.endMinute)
        }
        flush()
    }
}

private fun exportDocumentRows(
    plan: SchedulePlan,
    classes: List<SavedClass>,
    events: List<ExportEvent>,
    monday: LocalDate,
): List<String> = buildList {
    classes.forEach { saved ->
        val section = saved.section
        val meetings = meetingsFor(saved).distinctBy { listOf(it.day, it.start, it.end) }
        val times = meetings.sortedWith(compareBy({ it.day }, { it.start })).joinToString("; ") {
            "${ExportWeekdays[it.day].take(3)} ${exportClock(it.start)}–${exportClock(it.end)}"
        }.ifBlank { "Meeting time to be announced" }
        val dates = exportDateRange(section.starts, section.ends)
        val dateText = dates?.let { "${it.first} through ${it.second}" } ?: "Dates unpublished or unavailable"
        val active = events.any { !it.personal && it.sourceId == saved.id && monday.plusDays(it.day.toLong()) in it.starts..it.ends }
        add("${section.code} · ${section.title} · Section ${section.section} · ${saved.creditHours.ifBlank { "Credits pending" }}\n" +
            "$times · $dateText" + if (dates != null && meetings.isNotEmpty() && !active) " · Outside this week" else "")
    }
    plan.commitments.forEach { commitment ->
        val days = commitment.days.sorted().filter { it in 0..6 }.joinToString(", ") { ExportWeekdays[it].take(3) }
        val active = events.any { it.personal && it.sourceId == commitment.id && monday.plusDays(it.day.toLong()) in it.starts..it.ends }
        add("Personal: ${commitment.title}\n$days ${exportClock(commitment.startMinute)}–${exportClock(commitment.endMinute)} · ${commitment.starts} through ${commitment.ends}" +
            if (!active) " · Outside this week" else "")
    }
    if (isEmpty()) add("No classes or personal commitments in this schedule.")
}

private fun exportClock(minutes: Int): String {
    val hour = minutes / 60 % 24
    return "${if (hour % 12 == 0) 12 else hour % 12}:${(minutes % 60).toString().padStart(2, '0')} ${if (hour < 12) "AM" else "PM"}"
}

private fun wrapExportText(raw: String, width: Float, paint: Paint): List<String> = buildList {
    raw.replace('\r', '\n').split('\n').forEach { paragraph ->
        var remainder = paragraph.trim()
        if (remainder.isEmpty()) add("")
        while (remainder.isNotEmpty()) {
            val measured = paint.breakText(remainder, true, maxOf(width, 1f), null).coerceAtLeast(1)
            var count = measured
            if (count < remainder.length) {
                val space = remainder.lastIndexOf(' ', count - 1)
                if (space > count / 2) count = space
            }
            if (count < remainder.length && Character.isHighSurrogate(remainder[count - 1]) && Character.isLowSurrogate(remainder[count])) count++
            add(remainder.take(count).trimEnd())
            remainder = remainder.drop(count).trimStart()
        }
    }
}

private fun ellipsizeExportText(raw: String, width: Float, paint: Paint): String {
    val single = raw.replace('\n', ' ').replace('\r', ' ')
    if (paint.measureText(single) <= width) return single
    val count = paint.breakText(single, true, maxOf(1f, width - paint.measureText("…")), null)
    val safeCount = if (count in 1 until single.length && Character.isHighSurrogate(single[count - 1])) count - 1 else count
    return single.take(safeCount) + "…"
}
