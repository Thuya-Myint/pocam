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
                        val releasedBeforeHold = withTimeoutOrNull(450L) {
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
                            var accumulatedRotation = 0.0
                            var lastAngle: Double? = null
                            var currentIndex = wheelModes.indexOf(mode).coerceAtLeast(0)
                            while (pressed) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                if (change == null || !change.pressed) {
                                    pressed = false
                                } else {
                                    // Rotate the mode carousel by tracking angular movement
                                    // around the shutter center, with wraparound at 360 degrees.
                                    val dx = change.position.x - size.width / 2f
                                    val dy = change.position.y - size.height / 2f
                                    val distance = kotlin.math.hypot(dx, dy)
                                    if (distance >= 24.dp.toPx()) {
                                        val angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble()))
                                        val previous = lastAngle
                                        if (previous != null) {
                                            val delta = ((angle - previous + 540.0) % 360.0) - 180.0
                                            accumulatedRotation += delta
                                            while (accumulatedRotation >= 35.0) {
                                                currentIndex = (currentIndex + 1) % wheelModes.size
                                                accumulatedRotation -= 35.0
                                            }
                                            while (accumulatedRotation <= -35.0) {
                                                currentIndex = (currentIndex - 1 + wheelModes.size) % wheelModes.size
                                                accumulatedRotation += 35.0
                                            }
                                        }
                                        lastAngle = angle
                                    } else {
                                        lastAngle = null
                                    }
                                    val next = wheelModes[currentIndex]
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
                            if (hovered != mode) onModeSelected(hovered)
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
