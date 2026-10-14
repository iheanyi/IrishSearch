package com.iheanyiekechukwu.irishsearch

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Copies the selected CRNs so students can use their plan when registering in NOVO. */
@Composable
internal fun RegistrationHandoff(plan: SchedulePlan, classes: List<SavedClass>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var copied by remember(plan.id, classes.map { it.id }) { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.Inline)) {
        Text("Ready to register?", style = MaterialTheme.typography.titleMedium)
        Text("This plan is saved on your phone. Registration, eligibility, and current seat availability are confirmed in NOVO.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (classes.isNotEmpty()) TextButton(onClick = {
            val crns = classes.map { it.section.crn }.filter { it.isNotBlank() }.distinct()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("${plan.name} · ${plan.semesterLabel}", crns.joinToString(", ")))
            copied = true
        }) {
            Icon(Icons.Default.ContentCopy, contentDescription = null)
            Text(if (copied) "Section IDs copied" else "Copy section IDs (CRNs)", Modifier.padding(start = AppSpacing.Inline))
        }
    }
}
