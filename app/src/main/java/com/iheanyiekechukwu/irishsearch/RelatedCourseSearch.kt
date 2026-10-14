package com.iheanyiekechukwu.irishsearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Browses every section of one exact course in the term selected for the source class. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RelatedCourseSearchSheet(
    code: String,
    semesterLabel: String,
    sections: List<CourseSection>,
    loading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSelectSection: (CourseSection) -> Unit,
) {
    var query by rememberSaveable(code, semesterLabel) { mutableStateOf("") }
    val exactSections = sections.filter { it.code.trim().equals(code.trim(), ignoreCase = true) }
    val filteredSections = exactSections.filter { section ->
        query.isBlank() || listOf(
            section.section,
            section.title,
            section.crn,
            relatedSectionStatus(section.status),
            readableMeetings(section).joinToString(" "),
        ).any { it.contains(query.trim(), ignoreCase = true) }
    }
    val density = LocalDensity.current
    val sheetHeight = with(density) {
        LocalWindowInfo.current.containerSize.height.toDp() * 0.86f
    }.coerceAtMost(740.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().height(sheetHeight)
                .padding(horizontal = AppSpacing.Page),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Row),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Explore $code",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close sections")
                }
            }
            Text(
                semesterLabel.ifBlank { "Selected semester" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Find a section") },
                placeholder = { Text("Section, time, or ID") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    { IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear section search")
                    } }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                if (loading) "Finding sections…" else
                    "${filteredSections.size} ${if (filteredSections.size == 1) "section" else "sections"}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when {
                loading -> CircularProgressIndicator(Modifier.padding(vertical = 24.dp))
                error != null -> Text(
                    error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                exactSections.isEmpty() -> Text(
                    "No sections found for ${semesterLabel.ifBlank { "this semester" }}.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                filteredSections.isEmpty() -> Text(
                    "No sections match “$query”.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(bottom = AppSpacing.Section),
                ) {
                    items(filteredSections, key = { it.key.ifBlank { it.crn } }) { section ->
                        RelatedSectionRow(section, onClick = { onSelectSection(section) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun RelatedSectionRow(section: CourseSection, onClick: () -> Unit) {
    val meeting = readableMeetings(section).joinToString("; ").ifBlank { "Time to be announced" }
    ListItem(
        overlineContent = {
            Text(
                "Section ${section.section} · ID ${section.crn}",
                color = MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = {
            Text(section.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(meeting, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(relatedSectionStatus(section.status),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

private fun relatedSectionStatus(code: String): String = when (code.trim().uppercase()) {
    "A" -> "Open"
    "F" -> "Full"
    else -> "Status unavailable"
}
