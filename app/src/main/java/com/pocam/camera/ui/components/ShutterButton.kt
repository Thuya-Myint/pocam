package com.pocam.camera.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * The shutter performs ONLY capture / start-stop video. Camera mode selection
 * is exclusively controlled by tapping the flat mode selector.
 */
@Composable
fun ShutterButton(
    busy: Boolean = false,
    recording: Boolean = false,
    onCapture: () -> Unit,
    onSound: () -> Unit = {},
    motionEnabled: Boolean = true
) {
    val haptics = LocalHapticFeedback.current
    val latestCapture by rememberUpdatedState(onCapture)
    val latestSound by rememberUpdatedState(onSound)
    val shutterScale = remember { Animatable(1f) }
    var pulse by remember { mutableIntStateOf(0) }
    LaunchedEffect(pulse, motionEnabled) {
        if (motionEnabled && pulse > 0) {
            shutterScale.snapTo(1f)
            shutterScale.animateTo(.90f, tween(65))
            shutterScale.animateTo(1f, spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            ))
        } else shutterScale.snapTo(1f)
    }

    Box(
        Modifier.size(74.dp)
            .border(3.dp, Color(0xFF24272D), CircleShape)
            .background(Color(0xFFF9FAFB), CircleShape)
            .semantics {
                contentDescription = if (recording) "Stop recording"
                    else if (busy) "Saving photo"
                    else "Take photo"
            }
            .clickable(enabled = !busy) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (motionEnabled) pulse++
                latestSound()
                latestCapture()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(if (recording) 34.dp else 58.dp)
                .graphicsLayer {
                    scaleX = shutterScale.value
                    scaleY = shutterScale.value
                }
                .background(
                    if (recording) Brush.linearGradient(listOf(Color(0xFFE43A1D),PocamPalette.Orange))
                    else Brush.linearGradient(listOf(Color.White,Color(0xFFF1F2F4),Color.White)),
                    if (recording) RoundedCornerShape(12.dp) else CircleShape
                )
        )
    }
}
