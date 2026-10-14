package com.iheanyiekechukwu.irishsearch

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

internal data class LibraryTransferActions(
    val openBackup: () -> Unit,
    val exportCalendar: (SchedulePlan) -> Unit,
    val shareImage: (SchedulePlan) -> Unit,
)

/** Files are chosen explicitly through Android's picker; restore is reviewed before merging. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberLibraryTransfers(
    library: SavedLibrary,
    onImport: (SavedLibrary, (Boolean) -> Unit) -> Unit,
    onMessage: (String) -> Unit,
): LibraryTransferActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showBackup by rememberSaveable { mutableStateOf(false) }
    var calendarPlanId by rememberSaveable { mutableStateOf<String?>(null) }
    var incoming by remember { mutableStateOf<SavedLibrary?>(null) }
    var busy by remember { mutableStateOf(false) }
    var closingBackup by remember { mutableStateOf(false) }
    val backupSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val backupMaxHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() * 0.9f }
    fun closeBackup(afterClose: () -> Unit = {}) {
        if (closingBackup) return
        closingBackup = true
        scope.launch {
            try {
                backupSheetState.hide()
                showBackup = false
                afterClose()
            } finally { closingBackup = false }
        }
    }

    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    val encoded = encodeBackup(library)
                    requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).bufferedWriter(Charsets.UTF_8).use { it.write(encoded) }
                }
                onMessage("Backup saved")
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { onMessage("Couldn't save backup. Please choose another location.") }
            finally { busy = false }
        }
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                incoming = withContext(Dispatchers.IO) {
                    // Bound input before parsing, including providers without size metadata.
                    val bytes = requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            require(output.size() + count <= MaxBackupBytes) { "Backup too large" }
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                    val json = Charsets.UTF_8.newDecoder()
                        .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                        .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                        .decode(java.nio.ByteBuffer.wrap(bytes)).toString()
                    decodeBackup(json)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { onMessage("Couldn't restore this file. Choose a valid IrishSearch backup.") }
            finally { busy = false }
        }
    }
    val createCalendar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")) { uri ->
        val plan = library.plans.firstOrNull { it.id == calendarPlanId }
        calendarPlanId = null
        if (uri != null) {
            if (plan == null) onMessage("This schedule is no longer available")
            else scope.launch {
                busy = true
                try {
                    val classes = library.classes.filter { it.id in plan.classIds }
                    withContext(Dispatchers.IO) {
                        val calendar = exportScheduleIcs(plan, classes)
                        requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).bufferedWriter(Charsets.UTF_8).use { it.write(calendar) }
                    }
                    onMessage(scheduleExportSummary(plan, classes).message)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { onMessage("Couldn't export calendar. Check meeting dates and try again.") }
                finally { busy = false }
            }
        }
    }
    if (showBackup) ModalBottomSheet(
        onDismissRequest = { if (!closingBackup) showBackup = false },
        sheetState = backupSheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().heightIn(max = backupMaxHeight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppSpacing.Page).padding(bottom = AppSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Group)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Backup & restore", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = { closeBackup() }, enabled = !closingBackup) {
                    Icon(Icons.Default.Close, contentDescription = "Close backup and restore")
                }
            }
            Text("Keep your saved classes, schedules, and personal commitments in a file you control.",
                style = MaterialTheme.typography.bodyMedium)
            Button(onClick = {
                closeBackup { createBackup.launch("IrishSearch-backup-${LocalDate.now()}.json") }
            }, enabled = !busy && !closingBackup, modifier = Modifier.fillMaxWidth()) { Text("Save backup") }
            OutlinedButton(onClick = {
                closeBackup { openBackup.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
            }, enabled = !busy && !closingBackup, modifier = Modifier.fillMaxWidth()) { Text("Restore from file") }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Restore adds missing items and keeps existing ones. Imported seat alerts start off. Files you export remain wherever you save them.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    incoming?.let { backup ->
        val newClasses = backup.classes.count { item -> library.classes.none { it.id == item.id } }
        val newPlans = backup.plans.count { item -> library.plans.none { it.id == item.id } }
        AlertDialog(onDismissRequest = { if (!busy) incoming = null },
            title = { Text("Restore backup?") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AppSpacing.Group)) {
                    Text("Add $newClasses saved ${if (newClasses == 1) "class" else "classes"} and $newPlans ${if (newPlans == 1) "schedule" else "schedules"}. Existing items are kept. Seat alerts for imported classes start off.")
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
            confirmButton = { Button(enabled = !busy, onClick = {
                busy = true
                onImport(backup) { success ->
                    busy = false
                    incoming = null
                    onMessage(if (success) "Backup restored" else "Couldn't restore backup. Your existing data is unchanged.")
                }
            }) { Text(if (busy) "Restoring…" else "Restore") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { incoming = null }) { Text("Cancel") } })
    }
    return LibraryTransferActions(
        openBackup = { showBackup = true },
        exportCalendar = { plan ->
            if (!busy) {
                val classes = library.classes.filter { it.id in plan.classIds }
                val summary = scheduleExportSummary(plan, classes)
                if (summary.eventCount == 0) onMessage("No meetings with complete dates and times to export")
                else {
                    calendarPlanId = plan.id
                    createCalendar.launch("${safeExportName(plan.name)}.ics")
                }
            }
        },
        shareImage = { plan ->
            if (!busy) scope.launch {
                busy = true
                onMessage("Preparing schedule image…")
                try {
                    val classes = library.classes.filter { it.id in plan.classIds }
                    val file = writeScheduleImage(context, plan, classes)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = ClipData.newUri(context.contentResolver, plan.name, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(send, "Share schedule"))
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { onMessage("Couldn't share schedule. Please try again.") }
                finally { busy = false }
            }
        },
    )
}

private fun safeExportName(name: String): String = name.replace(Regex("[^A-Za-z0-9 _-]"), "_").take(60).ifBlank { "IrishSearch-schedule" }
