package com.iheanyiekechukwu.irishsearch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val DetailEaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

@Composable
internal fun CourseDetailPage(
    state: SearchUiState,
    horizontal: Dp,
    openBookstore: (String) -> Unit,
    onPreview: () -> Unit,
    onRelatedCourse: (String) -> Unit,
    activePlanId: String?,
    onSelectPlan: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    val section = state.selectedSection ?: return
    val details = state.details
    val detailKey = "${state.selectedSchool?.id}:${state.selectedSemester?.code}:${section.crn}"
    var expandedDescription by rememberSaveable(detailKey) { mutableStateOf(false) }
    var descriptionOverflow by remember(detailKey, details?.description) { mutableStateOf(false) }
    val enrollment = details?.seats?.let(::parseEnrollment)
    val meetings = readableMeetings(section)
    val previewInteraction = remember(detailKey) { MutableInteractionSource() }
    val previewPressed by previewInteraction.collectIsPressedAsState()
    val touchInput = LocalInputModeManager.current.inputMode == InputMode.Touch
    val previewScale by animateFloatAsState(
        targetValue = if (previewPressed && touchInput) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label = "Preview press",
    )
    PullToRefreshBox(
        isRefreshing = state.loadingDetails && details != null,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = horizontal, end = horizontal, top = AppSpacing.Page, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Row),
        ) {
            item(key = "heading") {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                    Text("${section.code} · Section ${section.section}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(section.title, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    Text(listOfNotNull(state.selectedSemester?.label?.takeIf { it.isNotBlank() },
                        section.crn.takeIf { it.isNotBlank() }?.let { "CRN $it" }).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item(key = "class-summary") {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.fillMaxWidth().padding(AppSpacing.Group), verticalArrangement = Arrangement.spacedBy(AppSpacing.Row)) {
                        if (meetings.isEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.Inline), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, null, Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Meeting times to be announced", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else meetings.forEach { meeting ->
                            val parts = meeting.split(" · ", limit = 2)
                            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.Inline), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Schedule, null, Modifier.padding(top = AppSpacing.Detail).size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
                                    Text(parts.first(), style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(parts.getOrElse(1) { meeting }, style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.Row),
                            verticalAlignment = Alignment.Top) {
                            val credits = details?.creditHours?.replace(Regex("\\b(?:Credit\\s+)?Hours?\\b", RegexOption.IGNORE_CASE), "credits")
                                ?.takeIf { it.isNotBlank() } ?: "Credits pending"
                            Text(credits, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            readableDateRange(section)?.let { range ->
                                Text(range, Modifier.weight(1.6f), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (section.instructor.isNotBlank()) Text("Instructor: ${section.instructor}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AnimatedVisibility(
                            visible = enrollment != null && (enrollment.maximum != null || enrollment.available != null),
                            enter = fadeIn(tween(180, easing = DetailEaseOut)),
                            exit = fadeOut(tween(140, easing = DetailEaseOut)),
                        ) {
                            enrollment?.let { summary ->
                                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Row)) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    EnrollmentInline(summary)
                                }
                            }
                        }
                    }
                }
            }
            item(key = "preview") {
                OutlinedButton(onClick = onPreview, interactionSource = previewInteraction,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).graphicsLayer {
                        scaleX = previewScale
                        scaleY = previewScale
                    }) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("Preview in schedule", Modifier.padding(start = AppSpacing.Inline))
                }
            }
            if (state.loadingDetails && details == null) item(key = "loading") {
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.Inline), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Loading class details…", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            state.detailError?.let { error -> item(key = "error") {
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.errorContainer) {
                    Column(Modifier.padding(AppSpacing.Group), verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
                        Text(error, color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onRefresh) { Text("Try again") }
                    }
                }
            } }
            details?.let { loaded ->
                if (loaded.description.isNotBlank()) item(key = "description") {
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                        Text("About this class", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Crossfade(targetState = expandedDescription,
                            animationSpec = tween(180, easing = DetailEaseOut), label = "Description length") { expanded ->
                            Text(loaded.description, style = MaterialTheme.typography.bodyMedium,
                                maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis,
                                onTextLayout = { if (!expanded) descriptionOverflow = it.hasVisualOverflow })
                        }
                        if (loaded.description.length > 220 || descriptionOverflow || expandedDescription) TextButton(
                            onClick = { expandedDescription = !expandedDescription },
                        ) { Text(if (expandedDescription) "Show less" else "Show more") }
                    }
                }
                val requirements = loaded.requirements
                val entries = listOf(
                    "Prerequisites" to requirements.prerequisites,
                    "Enrollment restrictions" to requirements.enrollmentRestrictions,
                    "Corequisites" to requirements.corequisites,
                    "Cannot have taken" to requirements.cannotHaveTaken,
                    "Required sections" to requirements.requiredSections,
                    "Section notes" to requirements.notes,
                ).filter { it.second.isNotBlank() }
                if (entries.isNotEmpty()) item(key = "requirements") {
                    val subjects = state.subjects.map { it.code.uppercase() }.toSet()
                    val matchingPlans = state.library.plans.filter {
                        it.schoolId == state.selectedSchool?.id && it.semesterCode == state.selectedSemester?.code
                    }
                    val corequisites = courseReferences(requirements.corequisites, subjects).map { it.code }.distinct()
                    val plan = matchingPlans.firstOrNull { it.id == activePlanId } ?: matchingPlans.firstOrNull()
                    val planCodes = state.library.classes.filter { it.id in plan?.classIds.orEmpty() }
                        .map { it.section.code.uppercase() }.toSet()
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text("Requirements", Modifier.padding(top = AppSpacing.Inline), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        if (corequisites.isNotEmpty() && matchingPlans.isNotEmpty()) {
                            Text("Check corequisites in", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                                matchingPlans.forEach { option ->
                                    FilterChip(selected = option.id == plan?.id, onClick = { onSelectPlan(option.id) },
                                        label = { Text(option.name) })
                                }
                            }
                        }
                        entries.forEachIndexed { index, (label, text) ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            DetailSection(label, text, subjects, onRelatedCourse,
                                stateKey = detailKey,
                                inPlan = if (label == "Corequisites" && plan != null) planCodes else null)
                        }
                    }
                }
                loaded.bookUrl?.let { bookUrl -> item(key = "bookstore") {
                    TextButton(onClick = { openBookstore(bookUrl) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                        Text("Check textbooks and materials", Modifier.padding(start = AppSpacing.Inline))
                    }
                } }
            }
        }
    }
}

@Composable
private fun DetailSection(
    label: String,
    content: String,
    subjects: Set<String>,
    onRelatedCourse: (String) -> Unit,
    stateKey: String,
    inPlan: Set<String>? = null,
) {
    var expanded by rememberSaveable(stateKey, label, content) { mutableStateOf(false) }
    val display = content.replace(Regex("^\\s*${Regex.escape(label)}\\s*:\\s*", RegexOption.IGNORE_CASE), "")
    val references = if (label == "Section notes") emptyList() else courseReferences(display, subjects)
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(180, easing = DetailEaseOut),
        label = "$label disclosure",
    )
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
            .clickable(role = Role.Button) { expanded = !expanded }
            .padding(vertical = AppSpacing.Inline), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium)
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null,
                modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = chevronRotation },
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(visible = expanded,
            enter = fadeIn(tween(180, easing = DetailEaseOut)), exit = fadeOut(tween(140, easing = DetailEaseOut))) {
            if (label == "Corequisites" && inPlan != null && references.isNotEmpty()) {
                CorequisiteRequirementBody(display, references, inPlan, onRelatedCourse)
            } else RequirementTextBody(label, display, subjects, onRelatedCourse)
        }
    }
}

/** Keeps source operators and grouping literal when the catalog does not expose a logical tree. */
@Composable
private fun RequirementTextBody(
    label: String,
    display: String,
    subjects: Set<String>,
    onRelatedCourse: (String) -> Unit,
) {
    val structured = if (label == "Prerequisites") parseCourseRequirement(display, subjects) else null
    Column(Modifier.fillMaxWidth().padding(bottom = AppSpacing.Inline),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Row)) {
        if (structured != null) {
            StructuredRequirement(structured.expression, onRelatedCourse)
            structured.footnotes.forEach { Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else if (label == "Enrollment restrictions") {
            PathRequirements.enrollmentRows(display).forEach { row ->
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
                    row.label?.let { Text(it, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    LinkedRequirementText(row.text, subjects, onRelatedCourse)
                }
            }
        } else display.lines().filter { it.isNotBlank() }.forEach { paragraph ->
            if (paragraph.trimStart().startsWith("*")) Text(paragraph,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            else LinkedRequirementText(paragraph, if (label == "Section notes") emptySet() else subjects, onRelatedCourse)
        }
    }
}

@Composable
private fun StructuredRequirement(
    expression: CourseRequirementExpression,
    onRelatedCourse: (String) -> Unit,
    nested: Boolean = false,
) {
    if (expression is CourseRequirementExpression.Course) {
        AssistChip(onClick = { onRelatedCourse(expression.code) },
            label = { Text(expression.code + if (expression.markedWithAsterisk) "*" else "") },
            modifier = Modifier.heightIn(min = 48.dp).semantics {
                contentDescription = "${expression.code}${if (expression.markedWithAsterisk) ", see asterisk note" else ""}. View sections."
            }, shape = MaterialTheme.shapes.small,
            colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.primary))
        return
    }
    val alternatives = expression is CourseRequirementExpression.OneOf
    val children = when (expression) {
        is CourseRequirementExpression.AllOf -> expression.children
        is CourseRequirementExpression.OneOf -> expression.children
    }
    Surface(modifier = Modifier.then(if (nested) Modifier.padding(start = AppSpacing.Inline) else Modifier).fillMaxWidth(),
        shape = if (nested) MaterialTheme.shapes.small else MaterialTheme.shapes.medium,
        color = if (nested) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(AppSpacing.Row), verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
            Text(if (alternatives) "One of" else "All of", style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            children.forEachIndexed { index, child ->
                if (index > 0) Text(if (alternatives) "OR" else "AND",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                StructuredRequirement(child, onRelatedCourse, nested = child !is CourseRequirementExpression.Course)
            }
        }
    }
}

@Composable
private fun LinkedRequirementText(content: String, subjects: Set<String>, onRelatedCourse: (String) -> Unit) {
    val linkedColor = MaterialTheme.colorScheme.primary
    val references = courseReferences(content, subjects)
    val text = buildAnnotatedString {
        var next = 0
        for (reference in references) {
            // The OR/AND and following course form one unbroken readable span, including any trailing asterisk.
            val before = content.substring(next, reference.range.first)
            append(Regex("(\\b(?:or|and))\\s+$", RegexOption.IGNORE_CASE).replace(before) {
                "${it.groupValues[1]}\u00A0"
            })
            withLink(LinkAnnotation.Clickable(
                tag = reference.code,
                styles = TextLinkStyles(style = SpanStyle(color = linkedColor, textDecoration = TextDecoration.Underline)),
                linkInteractionListener = { onRelatedCourse(reference.code) },
            )) { append(reference.code.replace(' ', '\u00A0')) }
            next = reference.range.last + 1
        }
        append(content.substring(next))
    }
    Text(text, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = MaterialTheme.typography.bodyLarge.lineHeight))
}

/** Keeps the original AND/OR wording, replacing each code with one interactive status token. */
@Composable
private fun CorequisiteRequirementBody(
    display: String,
    references: List<CourseReference>,
    inPlan: Set<String>,
    onRelatedCourse: (String) -> Unit,
) {
    FlowRow(Modifier.fillMaxWidth().padding(bottom = AppSpacing.Inline), horizontalArrangement = Arrangement.spacedBy(AppSpacing.Inline),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
        var next = 0
        references.forEach { reference ->
            val before = display.substring(next, reference.range.first).trim()
            if (before.isNotEmpty()) Text(before, Modifier.align(Alignment.CenterVertically),
                style = MaterialTheme.typography.bodyMedium)
            val present = reference.code in inPlan
            val status = if (present) "In schedule" else "Not in schedule"
            AssistChip(
                onClick = { onRelatedCourse(reference.code) },
                label = { Text("${reference.code} · $status") },
                leadingIcon = {
                    Icon(if (present) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.heightIn(min = 48.dp).semantics {
                    contentDescription = "${reference.code}, $status. View sections."
                },
                shape = MaterialTheme.shapes.small, border = null,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (present) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = if (present) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    leadingIconContentColor = if (present) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
            next = reference.range.last + 1
        }
        val after = display.substring(next).trim()
        if (after.isNotEmpty()) Text(after, Modifier.align(Alignment.CenterVertically),
            style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EnrollmentInline(summary: EnrollmentSummary) {
    val maximum = summary.maximum
    val enrolled = summary.enrolled
    val occupancy = if (maximum != null && maximum > 0 && enrolled != null)
        (enrolled.toFloat() / maximum).coerceIn(0f, 1f) else null
    val animatedOccupancy by animateFloatAsState(targetValue = occupancy ?: 0f,
        animationSpec = tween(200, easing = DetailEaseOut), label = "Enrollment progress")
    val availabilityColor = when (summary.available) {
        0 -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.primary
    }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.Row)) {
            Text(when (val open = summary.available) {
                null -> "Availability unknown"
                0 -> "Class is full"
                1 -> "1 seat open"
                else -> "$open seats open"
            }, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, color = availabilityColor)
            if (maximum != null) Text("${enrolled ?: "—"} / $maximum enrolled",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (occupancy != null) LinearProgressIndicator(progress = { animatedOccupancy },
            modifier = Modifier.fillMaxWidth(), color = availabilityColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
        if (summary.waitlistTotal != null || summary.waitlistAvailable != null) {
            val spots = when (val open = summary.waitlistAvailable) {
                null -> "open spots unknown"
                0 -> "no open spots"
                1 -> "1 open spot"
                else -> "$open open spots"
            }
            Text("Waitlist: $spots" + (summary.waitlistTotal?.let { " · $it waiting" } ?: ""),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else Text("Waitlist unavailable", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun RelatedCourseDialog(
    state: SearchUiState,
    onDismiss: () -> Unit,
    onSelect: (CourseSection) -> Unit,
) {
    val code = state.relatedCourseCode ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(code) },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                if (state.loadingRelated) CircularProgressIndicator()
                state.relatedError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (!state.loadingRelated && state.relatedError == null && state.relatedSections.isEmpty()) {
                    Text("No sections found for ${state.selectedSemester?.label.orEmpty()}.")
                }
                state.relatedSections.forEach { section ->
                    TextButton(onClick = { onSelect(section) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Section ${section.section} · ${section.title}",
                            modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
internal fun CoursePreviewPage(
    state: SearchUiState,
    horizontal: Dp,
    onAddToSchedule: () -> Unit,
) {
    val section = state.selectedSection ?: return
    val school = state.selectedSchool ?: state.school
    val semester = state.selectedSemester ?: return
    val candidate = SavedClass(school.id, semester.code, semester.label, section,
        creditHours = state.details?.creditHours.orEmpty())
    val plans = state.library.plans.filter {
        it.schoolId == school.id && it.semesterCode == semester.code
    }
    var selectedPlanId by rememberSaveable(section.crn) { mutableStateOf<String?>(null) }
    val selectedPlan = plans.firstOrNull { it.id == selectedPlanId }
    val savedClasses = state.library.classes.filter { it.id in selectedPlan?.classIds.orEmpty() }
    val previewClasses = savedClasses.filterNot { it.id == candidate.id } + candidate
    val meetings = previewClasses.flatMap(::meetingsFor)
    val conflicts = conflictingIds(meetings)
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = AppSpacing.Page),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Group),
    ) {
        item {
            Column(Modifier.padding(horizontal = horizontal), verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                Text("Preview your week", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold)
                Text("See where ${section.code} fits before adding it to a saved schedule.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(creditTotal(previewClasses),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
        if (plans.isNotEmpty()) item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = horizontal), horizontalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                FilterChip(selected = selectedPlan == null, onClick = { selectedPlanId = null },
                    label = { Text("Just this class") })
                plans.forEach { plan ->
                    FilterChip(selected = selectedPlanId == plan.id,
                        onClick = { selectedPlanId = plan.id }, label = { Text(plan.name) })
                }
            }
        }
        if (conflicts.isNotEmpty()) item {
            Text(if (candidate.id in conflicts)
                "This class overlaps a class in ${selectedPlan?.name ?: "the schedule"}."
                else "${selectedPlan?.name ?: "This schedule"} already has overlapping classes.",
                modifier = Modifier.padding(horizontal = horizontal),
                color = MaterialTheme.colorScheme.error)
        }
        item {
            if (meetings.isEmpty()) Text("Meeting times are not available for this section.",
                modifier = Modifier.padding(horizontal = horizontal))
            else WeeklyCalendar(meetings, conflicts)
        }
        val withoutTimes = previewClasses.filter { meetingsFor(it).isEmpty() }
        if (withoutTimes.isNotEmpty() && meetings.isNotEmpty()) item {
            Text("Meeting times are unavailable for ${withoutTimes.joinToString { it.section.code }}. Check these before registering.",
                modifier = Modifier.padding(horizontal = horizontal),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
        }
        item {
            Button(onClick = onAddToSchedule,
                modifier = Modifier.fillMaxWidth().padding(horizontal = horizontal)) {
                Text("Add to a saved schedule")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddToScheduleDialog(
    state: SearchUiState,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onCreate: (String) -> Unit,
) {
    val section = state.selectedSection ?: return
    val school = state.selectedSchool ?: state.school
    val semester = state.selectedSemester ?: return
    val candidate = SavedClass(school.id, semester.code, semester.label, section,
        creditHours = state.details?.creditHours.orEmpty())
    val plans = state.library.plans.filter { it.schoolId == school.id && it.semesterCode == semester.code }
    var name by rememberSaveable(candidate.id) { mutableStateOf("") }
    var creating by rememberSaveable(candidate.id) { mutableStateOf(plans.isEmpty()) }
    var selectedId by rememberSaveable(candidate.id) {
        mutableStateOf(plans.firstOrNull { candidate.id !in it.classIds }?.id ?: plans.firstOrNull()?.id)
    }
    var submitting by remember { mutableStateOf(false) }
    val selected = plans.firstOrNull { it.id == selectedId }
    val alreadyAdded = selected?.classIds?.contains(candidate.id) == true
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun finish(action: () -> Unit) {
        if (submitting) return
        submitting = true
        scope.launch {
            sheetState.hide()
            action()
        }
    }
    val confirm: () -> Unit = {
        if (creating) {
            val submittedName = name.trim()
            if (submittedName.isNotBlank()) finish { onCreate(submittedName) }
        } else selected?.let { plan -> if (!alreadyAdded) finish { onAdd(plan.id) } }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gutter = if (maxWidth >= 600.dp) AppSpacing.Expanded else AppSpacing.Page
            Column(Modifier.fillMaxWidth().heightIn(max = maxHeight)) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(horizontal = gutter).padding(bottom = AppSpacing.Group),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.Group)) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                        Text(if (creating) "New schedule" else "Add to schedule",
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("${section.code} · Section ${section.section}", style = MaterialTheme.typography.titleMedium)
                        Text(semester.label, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (creating) {
                        OutlinedTextField(
                            value = name, onValueChange = { name = it.take(48) },
                            label = { Text("Schedule name") }, placeholder = { Text("e.g. Fall option A") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { if (!submitting) confirm() }),
                        )
                        if (plans.isNotEmpty()) TextButton(onClick = { creating = false },
                            modifier = Modifier.heightIn(min = 48.dp)) { Text("Choose an existing schedule") }
                    } else {
                        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
                            plans.forEach { plan ->
                                val chosen = plan.id == selectedId
                                val containsClass = candidate.id in plan.classIds
                                val existing = state.library.classes.filter { it.id in plan.classIds }
                                val resulting = existing.filterNot { it.id == candidate.id } + candidate
                                val rowColor = if (chosen) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                Surface(shape = MaterialTheme.shapes.medium,
                                    color = if (chosen) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerLow) {
                                    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)
                                        .selectable(selected = chosen, role = Role.RadioButton, onClick = { selectedId = plan.id })
                                        .padding(AppSpacing.Row), verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Row)) {
                                        RadioButton(selected = chosen, onClick = null)
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
                                            Text(plan.name, style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium, color = rowColor)
                                            Text(if (containsClass) "Already in this schedule · ${creditTotal(existing)}"
                                                else "${creditTotal(existing)} now → ${creditTotal(resulting)} after adding",
                                                style = MaterialTheme.typography.bodySmall, color = rowColor)
                                        }
                                    }
                                }
                            }
                        }
                        OutlinedButton(onClick = { creating = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Icon(Icons.Default.Add, null, Modifier.size(20.dp))
                            Text("New schedule", Modifier.padding(start = AppSpacing.Inline))
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FlowRow(Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = AppSpacing.Row),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.Inline, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                    TextButton(onClick = { finish(onDismiss) }, enabled = !submitting,
                        modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
                    Button(onClick = confirm,
                        enabled = !submitting && if (creating) name.isNotBlank() else selected != null && !alreadyAdded,
                        modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(if (creating) "Create and add" else if (alreadyAdded) "Added" else "Add")
                    }
                }
            }
        }
    }
}
