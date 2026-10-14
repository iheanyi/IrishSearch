package com.iheanyiekechukwu.irishsearch

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.text.DateFormat
import java.util.Date

internal enum class RegistrationSeatGroup(val label: String) {
    OPEN("Seats available"), FULL("No seats available"), UNKNOWN("Needs checking"),
}

internal fun registrationSeatGroup(saved: SavedClass): RegistrationSeatGroup = when {
    saved.lastCheckFailed || saved.lastSeats == null -> RegistrationSeatGroup.UNKNOWN
    saved.lastSeats > 0 -> RegistrationSeatGroup.OPEN
    else -> RegistrationSeatGroup.FULL
}

@Composable
internal fun RegistrationDashboard(
    library: SavedLibrary,
    onOpen: (SavedClass) -> Unit,
    onRefresh: () -> Unit,
    onWatch: (SavedClass, Boolean) -> Unit,
    onMessage: (String) -> Unit,
    checking: Boolean = false,
    horizontal: Dp = AppSpacing.Page,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var notificationsEnabled by remember { mutableStateOf(SeatAlertNotifications.enabled(context)) }
    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) notificationsEnabled = SeatAlertNotifications.enabled(context)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val watched = library.classes.filter { it.watchSeats }
    LaunchedEffect(watched.size) { notificationsEnabled = SeatAlertNotifications.enabled(context) }
    LazyColumn(Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = horizontal, vertical = AppSpacing.Group),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Group)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
                    Text("Registration", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text("Track seats and copy course registration numbers (CRNs).", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onRefresh, enabled = watched.isNotEmpty() && !checking,
                    modifier = Modifier.semantics {
                        contentDescription = if (checking) "Checking seat availability" else "Refresh seat availability"
                    }) {
                    if (checking) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Refresh, contentDescription = null)
                }
            }
        }
        if (!notificationsEnabled) item {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.fillMaxWidth().padding(AppSpacing.Section),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
                        Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text("Notifications are off", style = MaterialTheme.typography.titleSmall)
                    }
                    Text("Enable notifications in Android settings to receive seat alerts.",
                        style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = {
                        val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}"))
                        val settings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        } else details
                        try { context.startActivity(settings) } catch (_: android.content.ActivityNotFoundException) {
                            context.startActivity(details)
                        }
                    }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Notification settings") }
                }
            }
        }
        item {
            Text(if (watched.isEmpty()) "Turn on seat alerts for a saved class to start tracking availability."
                else "${watched.size} watched · Checks run about hourly with internet and sufficient battery.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        watched.groupBy { it.schoolId to it.semesterCode }.forEach { (term, termClasses) ->
            item(key = "registration-term:${term.first}:${term.second}") {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
                    Text(termClasses.first().semesterLabel.replace(" Semester ", " "),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (library.classes.map { it.schoolId }.distinct().size > 1) {
                        Text(SchoolCatalogs.byId(term.first).name, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { copyRegistrationCrns(context, termClasses, onMessage) },
                        modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Copy watched CRNs", Modifier.padding(start = AppSpacing.Inline))
                    }
                }
            }
            RegistrationSeatGroup.entries.forEach { group ->
                val classes = termClasses.filter { registrationSeatGroup(it) == group }
                if (classes.isNotEmpty()) {
                    item(key = "registration-group:${term.first}:${term.second}:$group") {
                        Text(group.label, style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(classes, key = { "watched:${it.id}" }) { saved ->
                        RegistrationWatchCard(saved, onOpen, onWatch, onMessage)
                    }
                }
            }
        }
        val unwatched = library.classes.filterNot { it.watchSeats }
        if (unwatched.isNotEmpty()) {
            item { Text("Add seat alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            items(unwatched, key = { "unwatched:${it.id}" }) { saved ->
                Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.Row)) {
                    Column(Modifier.weight(1f).heightIn(min = 48.dp).clickable { onOpen(saved) }
                        .padding(vertical = AppSpacing.Inline)) {
                        Text("${saved.section.code} · ${saved.section.section}", style = MaterialTheme.typography.titleSmall)
                        Text(saved.semesterLabel.replace(" Semester ", " "), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = false, onCheckedChange = { onWatch(saved, it) }, modifier = Modifier.semantics {
                        contentDescription = "Seat alerts for ${saved.section.code}, section ${saved.section.section}"
                    })
                }
            }
        } else if (library.classes.isEmpty()) item {
            Text("Save classes from Browse, then return here to watch for open seats.",
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RegistrationWatchCard(
    saved: SavedClass,
    onOpen: (SavedClass) -> Unit,
    onWatch: (SavedClass, Boolean) -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(AppSpacing.Section), verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.Row)) {
                Column(Modifier.weight(1f).heightIn(min = 48.dp).clickable { onOpen(saved) },
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.Detail)) {
                    Text("${saved.section.code} · ${saved.section.section}", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium)
                    Text(saved.section.title, style = MaterialTheme.typography.bodyMedium)
                }
                Switch(checked = true, onCheckedChange = { onWatch(saved, it) }, modifier = Modifier.semantics {
                    contentDescription = "Seat alerts for ${saved.section.code}, section ${saved.section.section}"
                })
            }
            Text(saved.lastSeats?.let { "Last known: $it ${if (it == 1) "seat" else "seats"}" }
                ?: "Availability unknown", style = MaterialTheme.typography.titleSmall,
                color = if (registrationSeatGroup(saved) == RegistrationSeatGroup.OPEN) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface)
            val checked = saved.lastCheckedAt?.let {
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))
            }
            Text(when {
                saved.lastCheckFailed && checked != null -> "Check failed · Last success $checked"
                saved.lastCheckFailed -> "Check failed · No successful check yet"
                checked != null -> "Last checked $checked"
                else -> "Not checked yet"
            }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("CRN ${saved.section.crn}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { copyRegistrationCrns(context, listOf(saved), onMessage) },
                    modifier = Modifier.heightIn(min = 48.dp)) { Text("Copy CRN") }
            }
        }
    }
}

private fun copyRegistrationCrns(context: Context, classes: List<SavedClass>, onMessage: (String) -> Unit) {
    val crns = classes.map { it.section.crn }.filter { it.matches(Regex("\\d+")) }.distinct()
    if (crns.isEmpty()) { onMessage("No registration numbers available"); return }
    context.getSystemService(ClipboardManager::class.java)
        .setPrimaryClip(ClipData.newPlainText("Registration CRNs", crns.joinToString(", ")))
    onMessage(if (crns.size == 1) "CRN copied" else "${crns.size} CRNs copied")
}
