package com.iheanyiekechukwu.irishsearch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun WeeklyCalendar(
    meetings: List<CalendarMeeting>,
    conflicts: Set<String>,
    commitments: List<PersonalCommitment> = emptyList(),
    onEditCommitment: ((PersonalCommitment) -> Unit)? = null,
    onOpenClass: ((SavedClass) -> Unit)? = null,
) {
    val blocks = remember(meetings, commitments) { scheduleBlocks(meetings, commitments) }
    val placements = remember(blocks) { layoutScheduleBlocks(blocks) }
    val allConflicts = conflicts + scheduleBlockConflictPairs(blocks).flatten()
    val onOpenBlock: ((ScheduleBlock) -> Unit)? = if (onOpenClass == null && onEditCommitment == null) null else { block ->
        block.saved?.let { onOpenClass?.invoke(it) }
        block.commitment?.let { onEditCommitment?.invoke(it) }
    }
    val visibleMeetings = placements.map { it.meeting }
    val firstHour = ((visibleMeetings.minOfOrNull { it.start } ?: 480) / 60).coerceAtLeast(0)
    val lastHour = (((visibleMeetings.maxOfOrNull { it.end } ?: 1080) + 59) / 60).coerceAtMost(24)
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    // Preserve time proportions while reserving 48dp plus the 2dp event gap for every target.
    val shortestMeeting = visibleMeetings.minOfOrNull { it.end - it.start }?.coerceAtLeast(1) ?: 60
    val baseHourHeight = calendarHourHeightDp(shortestMeeting)
    if (baseHourHeight == null) {
        ShortMeetingWeek(visibleMeetings, allConflicts, onOpenBlock)
        return
    }
    val hourHeight = baseHourHeight.dp * fontScale
    val headerHeight = 38.dp * fontScale
    val totalHeight = hourHeight * (lastHour - firstHour)
    val days = if (visibleMeetings.any { it.day >= 5 }) 0..6 else 0..4
    val largestLaneCount = placements.maxOfOrNull { it.laneCount } ?: 1
    val byDay = placements.groupBy { it.meeting.day }
    val laneGap = AppSpacing.Detail
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 600.dp
        val timeWidth = 46.dp * fontScale
        val availableWidth = (maxWidth - AppSpacing.Page * 2 - timeWidth).coerceAtLeast(0.dp)
        val minimumLaneWidth = (if (compact && largestLaneCount == 1) 52.dp else if (compact) 96.dp else 128.dp) * fontScale
        val minimumDayWidth = minimumLaneWidth * largestLaneCount +
            laneGap * (largestLaneCount - 1) + 8.dp
        val dayWidth = (availableWidth / days.count()).coerceAtLeast(minimumDayWidth)
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
            if (dayWidth * days.count() > availableWidth) {
                Text("Scroll sideways to see the full week.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = AppSpacing.Page))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.Page)) {
                Column(Modifier.width(timeWidth)) {
                    Spacer(Modifier.height(headerHeight))
                    for (hour in firstHour until lastHour) {
                        val displayHour = if (hour % 12 == 0) 12 else hour % 12
                        Text("$displayHour ${if (hour < 12) "AM" else "PM"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.height(hourHeight).padding(top = 2.dp))
                    }
                }
                Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                    days.forEach { day ->
                        Column(Modifier.width(dayWidth).padding(end = AppSpacing.Detail)) {
                            Text(DayNames[day], style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.height(headerHeight).padding(start = AppSpacing.Detail)
                                    .semantics { heading() })
                            Box(Modifier.height(totalHeight).fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.small)) {
                                for (hour in firstHour until lastHour) {
                                    HorizontalDivider(Modifier.offset(y = hourHeight * (hour - firstHour)),
                                        color = MaterialTheme.colorScheme.outlineVariant)
                                }
                                byDay[day].orEmpty().forEach { placement ->
                                    val meeting = placement.meeting
                                    val top = hourHeight * ((meeting.start - firstHour * 60) / 60f)
                                    val height = hourHeight * ((meeting.end - meeting.start) / 60f)
                                    val laneWidth = (dayWidth - 8.dp - laneGap * (placement.laneCount - 1)) /
                                        placement.laneCount
                                    val left = 2.dp + (laneWidth + laneGap) * placement.lane
                                    CalendarEvent(meeting, meeting.id in allConflicts, onOpenBlock,
                                        Modifier.offset(x = left, y = top).width(laneWidth)
                                            .height((height - 2.dp).coerceAtLeast(1.dp)),
                                        showTime = !compact && height >= 44.dp * fontScale,
                                        showTitle = !compact && height >= 92.dp * fontScale,
                                        compact = compact)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortMeetingWeek(
    meetings: List<ScheduleBlock>,
    conflicts: Set<String>,
    onOpenBlock: ((ScheduleBlock) -> Unit)?,
) {
    Column(Modifier.padding(horizontal = AppSpacing.Page),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
        Text("Short meetings are shown in a list so each item is easy to open.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        meetings.sortedWith(compareBy({ it.day }, { it.start })).forEach { meeting ->
            val conflict = meeting.id in conflicts
            val modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)
                .then(if (onOpenBlock == null) Modifier else Modifier.clickable(
                    role = Role.Button, onClickLabel = if (meeting.commitment == null) "Open class details" else "Edit commitment",
                ) { onOpenBlock(meeting) })
            Surface(modifier = modifier, shape = MaterialTheme.shapes.small,
                color = if (conflict) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = if (conflict) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSurface) {
                Column(Modifier.padding(AppSpacing.Row),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
                    Text(meeting.title,
                        style = MaterialTheme.typography.titleSmall)
                    if (meeting.commitment != null) Text("Personal commitment", style = MaterialTheme.typography.labelMedium)
                    Text("${DayNames[meeting.day]} · ${calendarTime(meeting.start, true)}–${calendarTime(meeting.end, true)}",
                        style = MaterialTheme.typography.bodyMedium)
                    if (conflict) Text("Time conflict", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun CalendarEvent(
    meeting: ScheduleBlock,
    conflict: Boolean,
    onOpenBlock: ((ScheduleBlock) -> Unit)?,
    modifier: Modifier,
    showTime: Boolean,
    showTitle: Boolean,
    compact: Boolean,
) {
    val weekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val description = buildList {
        add("${meeting.title}. ${meeting.subtitle}")
        add("${weekdays[meeting.day]}, ${calendarTime(meeting.start, true)} to ${calendarTime(meeting.end, true)}")
        if (meeting.starts.isNotBlank() && meeting.ends.isNotBlank()) {
            add("Dates ${meeting.starts} through ${meeting.ends}")
        }
        if (conflict) add("Time conflict")
    }.joinToString(". ")
    val interactive = if (onOpenBlock == null) Modifier else Modifier.clickable(
        role = Role.Button, onClickLabel = if (meeting.commitment == null) "Open class details" else "Edit commitment",
    ) { onOpenBlock(meeting) }
    Surface(
        modifier = modifier.then(interactive).clearAndSetSemantics {
            contentDescription = description
            if (onOpenBlock != null) {
                role = Role.Button
                onClick(label = if (meeting.commitment == null) "Open class details" else "Edit commitment") {
                    onOpenBlock(meeting)
                    true
                }
            }
        },
        color = if (conflict) MaterialTheme.colorScheme.errorContainer
            else if (meeting.commitment != null) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (conflict) MaterialTheme.colorScheme.onErrorContainer
            else if (meeting.commitment != null) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Column(Modifier.padding(AppSpacing.Detail)) {
            val label = meeting.title
            Text(label,
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold, maxLines = if (compact) 2 else 1, overflow = TextOverflow.Ellipsis)
            if (showTime) Text("${calendarTime(meeting.start)}–${calendarTime(meeting.end)}",
                style = MaterialTheme.typography.labelSmall, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            if (showTitle) Text(meeting.subtitle, style = MaterialTheme.typography.bodySmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun calendarTime(minutes: Int, includePeriod: Boolean = false): String {
    val hour = (minutes / 60) % 24
    val displayHour = if (hour % 12 == 0) 12 else hour % 12
    val time = "$displayHour:${(minutes % 60).toString().padStart(2, '0')}"
    return if (includePeriod) "$time ${if (hour < 12) "AM" else "PM"}" else time
}
