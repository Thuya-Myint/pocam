package com.pocam.camera.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.draw.scale
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
import kotlin.math.roundToInt

@Composable
fun ShutterButton(mode: CameraMode, onCapture: () -> Unit, onModeSelected: (CameraMode) -> Unit, onWheelChange: (Boolean) -> Unit = {}, onHoverMode: (CameraMode) -> Unit = {}) {
    val haptics = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    var hovered by remember { mutableStateOf(mode) }
    var pressing by remember { mutableStateOf(false) }
    val shutterScale by animateFloatAsState(if (expanded) 0.88f else if (pressing) 0.94f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh), label = "shutter")
    Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.zIndex(3f).scale(shutterScale).size(88.dp).border(3.dp, Color.White, CircleShape)
                .background(Color(0xFF202126), CircleShape)
                .pointerInput(mode) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
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
                            onCapture()
                        } else {
                            expanded = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onWheelChange(true)
                            hovered = mode
                            onHoverMode(mode)
                            var pressed = true
                            while (pressed) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                if (change == null || !change.pressed) {
                                    pressed = false
                                } else {
                                    val dx = change.position.x - size.width / 2f
                                    val threshold = 22.dp.toPx()
                                    // Horizontal movement steps through the six options.
                                    // Returning to the center restores the original mode.
                                    val step = if (kotlin.math.abs(dx) < threshold) 0 else
                                        (dx / 48.dp.toPx()).roundToInt().coerceIn(-3, 3)
                                    val origin = wheelModes.indexOf(mode).coerceAtLeast(0)
                                    val next = wheelModes[
                                        (origin + step + wheelModes.size) % wheelModes.size]
                                    if (next != hovered) {
                                        hovered = next
                                        onHoverMode(next)
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }
                            expanded = false
                            pressing = false
                            onWheelChange(false)
                            onModeSelected(hovered)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(70.dp).background(Color.White, CircleShape))
        }
    }
}
