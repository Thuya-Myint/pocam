package com.pocam.camera.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.pocam.camera.domain.model.CameraMode
import kotlinx.coroutines.withTimeoutOrNull

// Keep shutter long-press gestures in exactly the same order as the curved strip.
private val shutterModes = listOf(CameraMode.VIDEO, CameraMode.NIGHT,
    CameraMode.PHOTO, CameraMode.PRO, CameraMode.PORTRAIT)

@Composable
fun ShutterButton(mode: CameraMode, busy: Boolean = false, recording: Boolean = false, onCapture: () -> Unit, onSound: () -> Unit = {}, onModeSelected: (CameraMode) -> Unit, onWheelChange: (Boolean) -> Unit = {}, onHoverMode: (CameraMode) -> Unit = {}) {
    val haptics = LocalHapticFeedback.current
    val soundNow by androidx.compose.runtime.rememberUpdatedState(onSound)
    val busyNow by androidx.compose.runtime.rememberUpdatedState(busy)
    var expanded by remember { mutableStateOf(false) }
    var hovered by remember { mutableStateOf(mode) }
    var pressing by remember { mutableStateOf(false) }
    val shutterScale by animateFloatAsState(if (expanded) 0.88f else if (pressing) 0.94f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh), label = "shutter")
    Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.zIndex(3f).scale(shutterScale).alpha(if (busy) .55f else 1f).size(88.dp).border(3.dp, Color.White, CircleShape)
                .background(Color(0xFF202126), CircleShape)
                .pointerInput(mode) {
                    awaitEachGesture {
                        try {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (busyNow) {
                            do {
                                val ignored = awaitPointerEvent()
                            } while (ignored.changes.any { it.pressed })
                            return@awaitEachGesture
                        }
                        val pointerId = down.id
                        pressing = true
                        val releasedBeforeHold = withTimeoutOrNull(350L) {
                            var released = false
                            while (!released) {
                                val event = awaitPointerEvent()
                                released = event.changes.none { it.id == pointerId && it.pressed }
                            }
                            true
                        }
                        if (releasedBeforeHold == true) {
                            pressing = false
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            soundNow()
                            onCapture()
                        } else if (recording) {
                            onCapture()
                        } else {
                            expanded = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onWheelChange(true)
                            hovered = mode
                            onHoverMode(mode)
                            var pressed = true
                            var horizontalDistance = 0f
                            var previousX: Float? = null
                            var currentIndex = shutterModes.indexOf(mode).coerceAtLeast(0)
                            while (pressed) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                if (change == null || !change.pressed) {
                                    pressed = false
                                } else {
                                    // Horizontal swipes move across the same five modes shown in the curved ribbon.
                                    // A full step requires 42dp; movement works in both directions.
                                    val oldX = previousX
                                    if (oldX != null) {
                                        horizontalDistance += change.position.x - oldX
                                        val step = 42.dp.toPx()
                                        while (horizontalDistance >= step) {
                                            currentIndex = (currentIndex + 1) % shutterModes.size
                                            horizontalDistance -= step
                                        }
                                        while (horizontalDistance <= -step) {
                                            currentIndex = (currentIndex - 1 + shutterModes.size) % shutterModes.size
                                            horizontalDistance += step
                                        }
                                    }
                                    previousX = change.position.x
                                    val next = shutterModes[currentIndex]
                                    if (next != hovered) {
                                        soundNow()
                                        hovered = next
                                        onHoverMode(next)
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }
                            expanded = false
                            pressing = false
                            onWheelChange(false)
                            if (hovered != mode) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onModeSelected(hovered)
                            }
                        }
                        } finally {
                            pressing = false
                            expanded = false
                            onWheelChange(false)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(if (recording) 40.dp else 70.dp)
                .background(if (recording) Color(0xFFFF5555) else Color.White,
                    if (recording) androidx.compose.foundation.shape.RoundedCornerShape(12.dp) else CircleShape))
        }
    }
}
