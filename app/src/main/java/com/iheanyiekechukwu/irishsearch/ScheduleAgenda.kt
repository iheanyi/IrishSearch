package com.iheanyiekechukwu.irishsearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A readable alternative to the week grid, including classes with unpublished times. */
internal fun LazyListScope.scheduleAgenda(
    classes: List<SavedClass>,
    horizontal: Dp,
    onOpenClass: (SavedClass) -> Unit,
) {
    val meetings = classes.flatMap(::meetingsFor)
    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    meetings.groupBy { it.day }.toSortedMap().forEach { (day, dayMeetings) ->
        item(key = "agenda-day:$day") {
            Row(Modifier.fillMaxWidth().padding(horizontal = horizontal).padding(top = AppSpacing.Inline, bottom = AppSpacing.Detail),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(dayNames[day], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val count = dayMeetings.distinctBy { Triple(it.saved.id, it.start, it.end) }.size
                Text("$count ${if (count == 1) "meeting" else "meetings"}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(dayMeetings.sortedWith(compareBy({ it.start }, { it.end }, { it.saved.section.code }))
            .distinctBy { Triple(it.saved.id, it.start, it.end) },
            key = { "agenda:$day:${it.saved.id}:${it.start}:${it.end}" }) { meeting ->
            val overlaps = dayMeetings.filter { other ->
                other.saved.id != meeting.saved.id &&
                    conflictingIds(listOf(meeting, other)).isNotEmpty()
            }.map { "${it.saved.section.code} · ${it.saved.section.section}" }.distinct()
            AgendaClassRow(
                saved = meeting.saved,
                time = agendaTime(meeting.start),
                endTime = agendaTime(meeting.end),
                supporting = overlaps.takeIf { it.isNotEmpty() }
                    ?.joinToString(prefix = "Overlaps ", separator = "; "),
                conflict = overlaps.isNotEmpty(),
                modifier = Modifier.padding(horizontal = horizontal),
                onClick = { onOpenClass(meeting.saved) },
            )
        }
    }
    val withoutTimes = classes.filter { saved -> meetings.none { it.saved.id == saved.id } }
    if (withoutTimes.isNotEmpty()) {
        item(key = "agenda-unannounced") {
            Column(Modifier.padding(horizontal = horizontal), verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
                Text("Time to be announced", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text("These classes are in your schedule. Check their times before registering.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(withoutTimes, key = { "agenda-tba:${it.id}" }) { saved ->
            AgendaClassRow(saved = saved, time = "TBA", endTime = null, supporting = null,
                conflict = false, modifier = Modifier.padding(horizontal = horizontal),
                onClick = { onOpenClass(saved) })
        }
    }
}

@Composable
private fun AgendaClassRow(
    saved: SavedClass,
    time: String,
    endTime: String?,
    supporting: String?,
    conflict: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val timeWidth = 72.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.Row)) {
        Column(Modifier.width(timeWidth).padding(top = AppSpacing.Row), verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
            Text(time, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            endTime?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Surface(onClick = onClick, modifier = Modifier.weight(1f).heightIn(min = 72.dp).semantics(mergeDescendants = true) {
                contentDescription = if (endTime == null) "Meeting time to be announced" else "$time to $endTime"
            },
            shape = MaterialTheme.shapes.medium,
            color = if (conflict) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = if (conflict) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onSurface) {
            Row(Modifier.padding(AppSpacing.Row), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
                    Text("${saved.section.code} · ${saved.section.section}",
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(saved.section.title, style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    supporting?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null,
                    tint = if (conflict) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun agendaTime(minutes: Int): String {
    val hour = minutes / 60
    return "${if (hour % 12 == 0) 12 else hour % 12}:${(minutes % 60).toString().padStart(2, '0')} ${if (hour < 12) "AM" else "PM"}"
}
