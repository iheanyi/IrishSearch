package com.iheanyiekechukwu.irishsearch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CommitmentEditor(
    plan: SchedulePlan,
    classes: List<SavedClass>,
    existing: PersonalCommitment?,
    onDismiss: () -> Unit,
    onSave: (PersonalCommitment) -> Unit,
    onRemove: (String) -> Unit,
) {
    val defaults = remember(plan.id, classes) { commitmentDateDefaults(plan, classes) }
    var title by rememberSaveable(plan.id, existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var dayMask by rememberSaveable(plan.id, existing?.id) {
        mutableIntStateOf(existing?.days?.fold(0) { mask, day -> mask or (1 shl day) } ?: 0)
    }
    var start by rememberSaveable(plan.id, existing?.id) { mutableIntStateOf(existing?.startMinute ?: 540) }
    var end by rememberSaveable(plan.id, existing?.id) { mutableIntStateOf(existing?.endMinute ?: 600) }
    var starts by rememberSaveable(plan.id, existing?.id) { mutableStateOf(existing?.starts ?: defaults.first) }
    var ends by rememberSaveable(plan.id, existing?.id) { mutableStateOf(existing?.ends ?: defaults.second) }
    var picker by rememberSaveable { mutableStateOf<String?>(null) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    fun finish(action: () -> Unit) {
        if (closing) return
        closing = true
        keyboard?.hide()
        scope.launch { sheetState.hide(); action() }
    }
    val draft = PersonalCommitment(existing?.id.orEmpty(), title.trim(),
        (0..6).filterTo(linkedSetOf()) { dayMask and (1 shl it) != 0 }, start, end, starts, ends)
    val error = commitmentValidation(draft)
    val previewBlocks = if (error == null) scheduleBlocks(emptyList(), listOf(draft.copy(id = existing?.id ?: "draft"))) else emptyList()
    val otherBlocks = scheduleBlocks(classes.flatMap(::meetingsFor), plan.commitments.filterNot { it.id == existing?.id })
    val overlappingTitles = otherBlocks.filter { other -> previewBlocks.any { scheduleBlocksConflict(it, other) } }
        .map { it.title }.distinct()
    val maxHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() * 0.92f }
    ModalBottomSheet(onDismissRequest = { if (!closing) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().heightIn(max = maxHeight)) {
            Row(Modifier.padding(horizontal = AppSpacing.Page), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (existing == null) "Add commitment" else "Edit commitment",
                        style = MaterialTheme.typography.headlineSmall)
                    Text(plan.semesterLabel.replace(" Semester ", " "), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { finish(onDismiss) }, enabled = !closing) { Icon(Icons.Default.Close, "Close commitment editor") }
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.Page, vertical = AppSpacing.Group),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Group)) {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Name") }, placeholder = { Text("Work, practice, or study time") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small,
                    supportingText = { Text("${title.length}/80") },
                    isError = title.length > 80 || (attempted && title.isBlank()))
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                    Text("Repeats weekly", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.Inline),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEachIndexed { day, name ->
                            FilterChip(selected = dayMask and (1 shl day) != 0,
                                onClick = { dayMask = dayMask xor (1 shl day) }, label = { Text(name) },
                                modifier = Modifier.heightIn(min = 48.dp).semantics {
                                    contentDescription = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")[day]
                                })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.Row)) {
                    CommitmentField("Starts at", commitmentTime(start), Modifier.weight(1f)) { picker = "startTime" }
                    CommitmentField("Ends at", commitmentTime(end), Modifier.weight(1f)) { picker = "endTime" }
                }
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                    Text("Date range", style = MaterialTheme.typography.titleSmall)
                    Text("Review the dates for this semester. Overnight commitments need a separate block for each day.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CommitmentField("First date", starts, Modifier.fillMaxWidth()) { picker = "startDate" }
                    CommitmentField("Last date", ends, Modifier.fillMaxWidth()) { picker = "endDate" }
                }
                if (overlappingTitles.isNotEmpty()) Text(overlappingTitles.joinToString(prefix = "Overlaps "),
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Text("Personal time appears in this schedule and its conflict checks. It adds no course credits.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
            if (attempted && error != null) Text(error,
                modifier = Modifier.padding(horizontal = AppSpacing.Page, vertical = AppSpacing.Inline)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            FlowRow(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.Page, vertical = AppSpacing.Row),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.Inline, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                if (existing != null) TextButton(onClick = { deleting = true }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = { finish(onDismiss) }, enabled = !closing) { Text("Cancel") }
                Button(enabled = !closing, onClick = {
                    attempted = true
                    if (error == null) {
                        val result = draft.copy(id = existing?.id ?: UUID.randomUUID().toString())
                        finish { onSave(result) }
                    }
                }) { Text(if (existing == null) "Add commitment" else "Save changes") }
            }
        }
    }
    if (picker == "startTime" || picker == "endTime") {
        val current = if (picker == "startTime") start else end
        val time = rememberTimePickerState(initialHour = (current / 60) % 24, initialMinute = current % 60)
        AlertDialog(onDismissRequest = { picker = null },
            title = { Text(if (picker == "startTime") "Start time" else "End time") },
            text = { TimeInput(state = time, colors = TimePickerDefaults.colors(
                periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )) },
            confirmButton = { TextButton(onClick = {
                if (picker == "startTime") start = time.hour * 60 + time.minute else end = time.hour * 60 + time.minute
                picker = null
            }) { Text("Done") } },
            dismissButton = { TextButton(onClick = { picker = null }) { Text("Cancel") } })
    }
    if (picker == "startDate" || picker == "endDate") {
        val current = if (picker == "startDate") starts else ends
        val date = rememberDatePickerState(initialSelectedDateMillis = dateEpochDay(current)?.times(86_400_000L))
        DatePickerDialog(onDismissRequest = { picker = null },
            confirmButton = { TextButton(enabled = date.selectedDateMillis != null, onClick = {
                date.selectedDateMillis?.let { millis ->
                    val value = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(millis))
                    if (picker == "startDate") starts = value else ends = value
                }
                picker = null
            }) { Text("Done") } },
            dismissButton = { TextButton(onClick = { picker = null }) { Text("Cancel") } }) {
            DatePicker(state = date)
        }
    }
    if (deleting && existing != null) AlertDialog(onDismissRequest = { deleting = false },
        title = { Text("Remove ${existing.title}?") },
        text = { Text("This removes the commitment from ${plan.name}.") },
        confirmButton = { TextButton(enabled = !closing, onClick = {
            deleting = false
            finish { onRemove(existing.id) }
        }) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Keep commitment") } })
}

@Composable
private fun CommitmentField(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 64.dp),
        shape = MaterialTheme.shapes.small, contentPadding = PaddingValues(AppSpacing.Row)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
