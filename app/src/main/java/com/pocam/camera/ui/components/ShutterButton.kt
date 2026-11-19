package com.pocam.camera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * The shutter performs ONLY capture / start-stop video. Camera mode selection
 * is exclusively controlled by tapping the curved mode selector.
 */
@Composable
fun ShutterButton(
    busy: Boolean = false,
    recording: Boolean = false,
    onCapture: () -> Unit,
    onSound: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    val latestCapture by rememberUpdatedState(onCapture)
    val latestSound by rememberUpdatedState(onSound)

    Box(
        Modifier.size(88.dp)
            .border(3.dp, Color.White, CircleShape)
            .background(Color(0xFF202126), CircleShape)
            .semantics {
                contentDescription = if (recording) "Stop recording"
                    else if (busy) "Saving photo"
                    else "Take photo"
            }
            .clickable(enabled = !busy) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                latestSound()
                latestCapture()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(if (recording) 40.dp else 70.dp)
                .background(
                    if (recording) Color(0xFFFF5555) else Color.White,
                    if (recording) RoundedCornerShape(12.dp) else CircleShape
                )
        )
    }
}
