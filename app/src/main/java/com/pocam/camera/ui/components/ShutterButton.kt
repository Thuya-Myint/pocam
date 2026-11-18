package com.pocam.camera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.pocam.camera.domain.model.CameraMode
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun ShutterButton(mode: CameraMode, onCapture: () -> Unit, onModeSelected: (CameraMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var hovered by remember { mutableStateOf(mode) }
    Box(Modifier.size(264.dp), contentAlignment = Alignment.Center) {
        if (expanded) RadialModeWheel(hovered)
        Box(
            Modifier.size(88.dp).border(3.dp, Color.White, CircleShape)
                .background(Color(0xFF202126), CircleShape)
                .pointerInput(mode) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val pointerId = down.id
                        val up = withTimeoutOrNull(450L) {
                            var eventUp: Boolean? = null
                            while (eventUp == null) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                if (change == null || !change.pressed) eventUp = change != null
                            }
                            eventUp
                        }
                        if (up != null) {
                            if (up) onCapture()
                        } else {
                            expanded = true
                            hovered = mode
                            var pressed = true
                            while (pressed) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                if (change == null || !change.pressed) {
                                    pressed = false
                                } else {
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val dx = change.position.x - center.x
                                    val dy = change.position.y - center.y
                                    if (hypot(dx, dy) > size.width * 0.15f) {
                                        val degrees = (atan2(dy, dx) * 180 / PI + 90 + 360) % 360
                                        val slot = (degrees / 60.0).roundToInt() % 6
                                        hovered = wheelModes[slot]
                                    }
                                }
                            }
                            expanded = false
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
