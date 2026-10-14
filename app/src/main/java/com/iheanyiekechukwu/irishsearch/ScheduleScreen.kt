package com.iheanyiekechukwu.irishsearch

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SchedulesPage(
    state: SearchUiState,
    model: SearchViewModel,
    horizontal: androidx.compose.ui.unit.Dp,
    selectedPlanId: String?,
    selectPlan: (String?) -> Unit,
    onMessage: (String) -> Unit = {},
    onBrowse: (CatalogOption) -> Unit = {},
    onExportCalendar: (SchedulePlan) -> Unit = {},
    onShareSchedule: (SchedulePlan) -> Unit = {},
) {
    var creating by rememberSaveable { mutableStateOf(false) }
    var editingClasses by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var agendaView by rememberSaveable { mutableStateOf(true) }
    val selected = state.library.plans.firstOrNull { it.id == selectedPlanId }
    var commitmentEditorOpen by rememberSaveable(selectedPlanId) { mutableStateOf(false) }
    var editingCommitmentId by rememberSaveable(selectedPlanId) { mutableStateOf<String?>(null) }
    fun editCommitment(commitment: PersonalCommitment? = null) {
        editingCommitmentId = commitment?.id
        commitmentEditorOpen = true
    }
    BackHandler(enabled = selected != null && state.selectedSection == null) { selectPlan(null) }
    fun classesIn(plan: SchedulePlan): List<SavedClass> = state.library.classes.filter {
        it.id in plan.classIds && it.schoolId == plan.schoolId &&
            it.semesterCode == plan.semesterCode
    }
    if (selected == null) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = horizontal, vertical = AppSpacing.Group),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Group),
        ) {
            item {
                Row(Modifier.fillMaxWidth().widthIn(max = 760.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("My schedules", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    FilledTonalIconButton(onClick = { creating = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New schedule")
                    }
                }
            }
            if (state.library.plans.isEmpty()) item {
                EmptyState("No schedules yet", "Create a schedule, then add classes you've saved.")
            }
            state.library.plans.groupBy { Triple(it.schoolId, it.semesterCode, it.semesterLabel) }
                .forEach { (term, plans) ->
                    item(key = "term:${term.first}:${term.second}") {
                        Text(term.third.replace(" Semester ", " "),
                            modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp).padding(top = AppSpacing.Inline),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                    }
                    items(plans, key = { it.id }) { plan ->
                        val planClasses = classesIn(plan)
                        Card(onClick = { selectPlan(plan.id) }, modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                            Column(Modifier.padding(AppSpacing.Group), verticalArrangement = Arrangement.spacedBy(AppSpacing.Row)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(plan.name, style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(AppSpacing.Row)) {
                                    Text(creditTotal(planClasses), style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    Text("${planClasses.size} ${if (planClasses.size == 1) "section" else "sections"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (plan.commitments.isNotEmpty()) Text(
                                    "${plan.commitments.size} personal ${if (plan.commitments.size == 1) "commitment" else "commitments"}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                ScheduleSummary(planClasses, commitments = plan.commitments)
                            }
                        }
                    }
                }
        }
    } else {
        LaunchedEffect(selected.id) { model.refreshMissingCredits(selected.id) }
        val selectedClasses = classesIn(selected)
        val meetings = selectedClasses.flatMap { meetingsFor(it) }
        val conflicts = conflictingIds(meetings)
        var showActions by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = horizontal, vertical = AppSpacing.Inline),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectPlan(null) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "All schedules")
                    }
                    Column(Modifier.weight(1f)) {
                        Text(selected.name, style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(selected.semesterLabel.replace(" Semester ", " "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box {
                        IconButton(onClick = { showActions = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Schedule actions")
                        }
                        DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                            DropdownMenuItem(text = { Text("Add commitment") }, onClick = {
                                showActions = false
                                editCommitment()
                            })
                            DropdownMenuItem(text = { Text("Export calendar") }, onClick = {
                                showActions = false
                                onExportCalendar(selected)
                            })
                            DropdownMenuItem(text = { Text("Share schedule image") }, onClick = {
                                showActions = false
                                onShareSchedule(selected)
                            })
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Duplicate schedule") }, onClick = {
                                showActions = false
                                model.duplicatePlan(selected.id) { id -> selectPlan(id); onMessage("Schedule copied") }
                            })
                            DropdownMenuItem(text = { Text("Delete schedule") }, onClick = {
                                showActions = false
                                deleting = true
                            })
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(creditTotal(selectedClasses), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text(buildString {
                            append("${selectedClasses.size} ${if (selectedClasses.size == 1) "section" else "sections"}")
                            if (selected.commitments.isNotEmpty()) append(" · ${selected.commitments.size} personal")
                        },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick = { editingClasses = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Classes", Modifier.padding(start = AppSpacing.Inline))
                    }
                }
                ScheduleSummary(selectedClasses, showFreeDays = false, commitments = selected.commitments)
                if (selectedClasses.isNotEmpty() || selected.commitments.isNotEmpty()) SecondaryTabRow(selectedTabIndex = if (agendaView) 0 else 1) {
                    Tab(selected = agendaView, onClick = { agendaView = true }, text = { Text("Agenda") })
                    Tab(selected = !agendaView, onClick = { agendaView = false }, text = { Text("Week") })
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            Crossfade(targetState = agendaView, modifier = Modifier.weight(1f),
                animationSpec = tween(140), label = "Schedule view") { showAgenda ->
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = AppSpacing.Row, bottom = AppSpacing.Page),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.Row)) {
                    if (selectedClasses.isEmpty() && selected.commitments.isEmpty()) item {
                        Column(Modifier.padding(horizontal = horizontal), verticalArrangement = Arrangement.spacedBy(AppSpacing.Row)) {
                            Text("Start building your week", style = MaterialTheme.typography.titleLarge)
                            Text("Choose classes for this semester, or block out work, practice, and other personal time.",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { editingClasses = true }) { Text("Choose saved classes") }
                            OutlinedButton(onClick = { editCommitment() }) { Text("Add personal commitment") }
                            TextButton(onClick = { onBrowse(CatalogOption(selected.semesterCode, selected.semesterLabel)) }) {
                                Text("Find classes")
                            }
                        }
                    } else if (showAgenda) {
                        scheduleAgenda(selectedClasses, horizontal, selected.commitments, { editCommitment(it) }) { saved ->
                            model.openSection(saved.section, CatalogOption(saved.semesterCode, saved.semesterLabel), saved.schoolId)
                        }
                    } else {
                        item {
                            if (meetings.isEmpty() && selected.commitments.isEmpty()) Text("Meeting times have not been published for these sections.",
                                Modifier.padding(horizontal = horizontal))
                            else WeeklyCalendar(meetings, conflicts, selected.commitments, { editCommitment(it) }) { saved ->
                                model.openSection(saved.section, CatalogOption(saved.semesterCode, saved.semesterLabel), saved.schoolId)
                            }
                        }
                        if (selectedClasses.isNotEmpty()) item {
                            Text("Classes in this schedule", Modifier.padding(horizontal = horizontal),
                                style = MaterialTheme.typography.titleMedium)
                        }
                        items(selectedClasses, key = { it.id }) { saved ->
                            Column(Modifier.padding(horizontal = horizontal)) {
                                CourseRow(saved.section, true) {
                                    model.openSection(saved.section, CatalogOption(saved.semesterCode, saved.semesterLabel), saved.schoolId)
                                }
                            }
                        }
                    }
                    if (!showAgenda && selected.commitments.isNotEmpty()) {
                        item { Text("Personal commitments", Modifier.padding(horizontal = horizontal),
                            style = MaterialTheme.typography.titleMedium) }
                        items(selected.commitments, key = { "commitment:${it.id}" }) { commitment ->
                            Surface(onClick = { editCommitment(commitment) },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = horizontal),
                                shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                                Column(Modifier.padding(AppSpacing.Row), verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
                                    Text(commitment.title, style = MaterialTheme.typography.titleSmall)
                                    Text("${commitment.days.sorted().joinToString(" · ") { DayNames[it] }} · ${commitmentTime(commitment.startMinute)}–${commitmentTime(commitment.endMinute)}",
                                        style = MaterialTheme.typography.bodyMedium)
                                    Text("${commitment.starts} – ${commitment.ends} · Tap to edit",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    if (selectedClasses.isNotEmpty()) item {
                        RegistrationHandoff(selected, selectedClasses,
                            Modifier.fillMaxWidth().padding(horizontal = horizontal, vertical = AppSpacing.Row))
                    }
                }
            }
        }
        if (deleting) AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("Delete ${selected.name}?") },
            text = { Text("Personal commitments in this schedule will be removed. Your saved classes will still be available to add to other schedules.") },
            confirmButton = { TextButton(onClick = {
                model.removePlan(selected.id)
                deleting = false
                selectPlan(null)
            }) { Text("Delete schedule") } },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("Keep schedule") } },
        )
        if (commitmentEditorOpen) CommitmentEditor(
            plan = selected, classes = selectedClasses,
            existing = selected.commitments.firstOrNull { it.id == editingCommitmentId },
            onDismiss = { commitmentEditorOpen = false },
            onSave = { commitment ->
                model.saveCommitment(selected.id, commitment)
                commitmentEditorOpen = false
                onMessage(if (editingCommitmentId == null) "Commitment added" else "Commitment updated")
            },
            onRemove = { id ->
                model.removeCommitment(selected.id, id)
                commitmentEditorOpen = false
                onMessage("Commitment removed")
            },
        )
        if (editingClasses) {
            val available = state.library.classes.filter {
                it.schoolId == selected.schoolId && it.semesterCode == selected.semesterCode
            }
            AlertDialog(
                onDismissRequest = { editingClasses = false },
                title = { Text("Classes in ${selected.name}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Group)) {
                        Text("${selected.semesterLabel.replace(" Semester ", " ")} · ${creditTotal(selectedClasses)}",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (available.isEmpty()) {
                            Text("Save sections for this semester, then choose the ones that belong in this schedule.",
                                style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = {
                                editingClasses = false
                                onBrowse(CatalogOption(selected.semesterCode, selected.semesterLabel))
                            }) { Text("Find classes") }
                        } else LazyColumn(Modifier.heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                        items(available, key = { it.id }) { saved ->
                            val checked = saved.id in selected.classIds
                            Surface(shape = MaterialTheme.shapes.small,
                                color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface) {
                            Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).toggleable(checked, role = Role.Checkbox,
                                onValueChange = { model.toggleInPlan(selected.id, saved.id) })
                                .padding(AppSpacing.Row), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                                Column(Modifier.weight(1f)) {
                                    Text("${saved.section.code} · ${saved.section.section}", style = MaterialTheme.typography.titleSmall)
                                    Text(saved.section.title, style = MaterialTheme.typography.bodyMedium)
                                    Text(readableMeetings(saved.section).joinToString("; ").ifBlank { "Time to be announced" },
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                Checkbox(checked = checked, onCheckedChange = null)
                            }
                            }
                        }
                        }
                    }
                },
                confirmButton = { Button(onClick = { editingClasses = false }) { Text("Done") } },
            )
        }
    }
    if (creating) CreateScheduleDialog(
        semesters = state.semesters,
        initialTerm = state.semester ?: state.semesters.firstOrNull(),
        onDismiss = { creating = false },
        onCreate = { name, term ->
            model.createPlan(name, term) { id ->
                selectPlan(id)
                editingClasses = true
                onMessage("Schedule created")
            }
            creating = false
        },
    )
}

@Composable
private fun CreateScheduleDialog(
    semesters: List<CatalogOption>,
    initialTerm: CatalogOption?,
    onDismiss: () -> Unit,
    onCreate: (String, CatalogOption) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var termCode by rememberSaveable { mutableStateOf(initialTerm?.code) }
    val term = semesters.firstOrNull { it.code == termCode } ?: initialTerm
    var pickTerm by rememberSaveable { mutableStateOf(false) }
    val canCreate = name.isNotBlank() && term != null
    fun create() { if (canCreate) onCreate(name.trim(), requireNotNull(term)) }
    if (pickTerm) {
        OptionSheet("Semester", semesters, term?.code, { pickTerm = false }) {
            termCode = it.code
            pickTerm = false
        }
    } else AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New schedule") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Group)) {
                Text("Create an option, then choose saved classes.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Schedule name") }, placeholder = { Text("Fall option A") },
                    singleLine = true, shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { create() }),
                )
                OutlinedButton(onClick = { pickTerm = true }, modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small, contentPadding = PaddingValues(AppSpacing.Row)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
                        Text("Semester", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(term?.label?.replace(" Semester ", " ") ?: "Choose semester",
                            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
            }
        },
        confirmButton = { Button(onClick = { create() }, enabled = canCreate) { Text("Create schedule") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

internal data class CalendarMeeting(
    val saved: SavedClass,
    val day: Int,
    val start: Int,
    val end: Int,
)

internal fun meetingsFor(saved: SavedClass): List<CalendarMeeting> = runCatching {
    val array = JSONArray(saved.section.meetingTimes)
    (0 until array.length()).mapNotNull { index ->
        val item = array.optJSONObject(index) ?: return@mapNotNull null
        val day = item.optString("meet_day").toIntOrNull() ?: return@mapNotNull null
        val start = parsePathClock(item.optString("start_time")) ?: return@mapNotNull null
        val end = parsePathClock(item.optString("end_time")) ?: return@mapNotNull null
        if (day !in 0..6 || end <= start) null else CalendarMeeting(saved, day, start, end)
    }
}.getOrDefault(emptyList())

internal fun conflictingIds(meetings: List<CalendarMeeting>): Set<String> = scheduleConflictingIds(meetings)

@Composable
private fun ScheduleSummary(classes: List<SavedClass>, showFreeDays: Boolean = true,
    commitments: List<PersonalCommitment> = emptyList()) {
    if (classes.isEmpty() && commitments.isEmpty()) return
    val summary = scheduleInsights(classes, commitments = commitments)
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
        if (summary.conflictCount > 0) Text(
            "${summary.conflictCount} ${if (summary.conflictCount == 1) "overlapping pair" else "overlapping pairs"}",
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        if (summary.unknownMeetingClassCount > 0) Text(
            "${summary.unknownMeetingClassCount} ${if (summary.unknownMeetingClassCount == 1) "section needs" else "sections need"} meeting times",
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        if (summary.duplicateCourseSections.isNotEmpty()) Text(
            "Multiple sections: ${summary.duplicateCourseSections.keys.joinToString()}. Check required sections before registering.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        if (showFreeDays && summary.freeWeekdays.isNotEmpty()) Text(
            "${if (summary.unknownMeetingClassCount == 0) "Free" else "No known meetings"} ${summary.freeWeekdays.sorted().joinToString(" & ") { DayNames[it] }}",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (showFreeDays && summary.conflictCount == 0 && summary.unknownMeetingClassCount == 0) Text(
            "No known time conflicts", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
