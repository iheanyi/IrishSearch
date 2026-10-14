package com.iheanyiekechukwu.irishsearch

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect

/** The section detail's primary action. The parent Scaffold places it above navigation. */
@Composable
internal fun ScheduleActionFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("Add to schedule") },
    )
}

/**
 * Keeps the destination visible behind section details as the back gesture progresses.
 * Do not install a second BackHandler for this layer; PredictiveBackHandler handles both
 * a completed gesture and a three-button back press.
 */
@Composable
internal fun PredictiveDetailLayer(
    enabled: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    destination: @Composable () -> Unit,
    detail: @Composable () -> Unit,
) {
    var gestureProgress by remember { mutableFloatStateOf(0f) }
    var gestureActive by remember { mutableStateOf(false) }
    var swipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    val progress by animateFloatAsState(
        targetValue = gestureProgress,
        animationSpec = tween(durationMillis = if (gestureActive) 0 else 180),
        label = "Section back preview",
    )

    PredictiveBackHandler(enabled = enabled) { events ->
        try {
            gestureActive = true
            events.collect { event ->
                swipeEdge = event.swipeEdge
                gestureProgress = event.progress.coerceIn(0f, 1f)
            }
            onBack()
        } catch (_: CancellationException) {
            // A cancelled swipe returns the detail to its original position.
        } finally {
            gestureActive = false
            gestureProgress = 0f
        }
    }

    Box(modifier.fillMaxSize()) {
        destination()
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    val direction = if (swipeEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f
                    translationX = direction * size.width * 0.14f * progress
                    scaleX = 1f - 0.045f * progress
                    scaleY = 1f - 0.045f * progress
                    shape = RoundedCornerShape(24.dp)
                    clip = progress > 0f
                }
                .background(MaterialTheme.colorScheme.surface),
        ) { detail() }
    }
}
