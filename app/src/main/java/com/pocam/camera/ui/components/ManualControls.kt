package com.pocam.camera.ui.components

import android.hardware.camera2.CaptureRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pocam.camera.camera.CameraController
import kotlin.math.roundToInt

@Composable
fun ManualControls(controller: CameraController, modifier: Modifier = Modifier) {
    var capabilities by remember { mutableStateOf<CameraController.ManualCapabilities?>(null) }
    var iso by remember { mutableFloatStateOf(0f) }
    var shutter by remember { mutableFloatStateOf(0f) }
    var focus by remember { mutableFloatStateOf(0f) }
    var wb by remember { mutableStateOf(CaptureRequest.CONTROL_AWB_MODE_AUTO) }
    val caps = capabilities
    Column(modifier.background(Color(0xDD202126), RoundedCornerShape(14.dp)).padding(12.dp)) {
        Text("PRO MANUAL", color = Color.White)
        if (caps == null) {
            Button(onClick = { capabilities = controller.manualCapabilities() }) {
                Text("Load camera controls")
            }
        } else {
            val isoRange = caps.isoRange
            val exposureRange = caps.exposureRangeNs
            if (isoRange != null && exposureRange != null) {
                Text("ISO: ${if (iso == 0f) "AUTO" else iso.roundToInt()}", color = Color.White)
                Slider(value = iso, onValueChange = {
                    iso = it
                    controller.setManualIso(if (it == 0f) null else it.roundToInt())
                }, valueRange = 0f..isoRange.last.toFloat(), modifier = Modifier.width(180.dp))
                Text("Shutter: ${if (shutter == 0f) "AUTO" else "${(shutter * 1000).roundToInt()} ms"}", color = Color.White)
                val maxSeconds = (exposureRange.last / 1_000_000_000f).coerceAtMost(1f)
                Slider(value = shutter, onValueChange = {
                    shutter = it
                    controller.setManualExposureNs(if (it == 0f) null else
                        (it * 1_000_000_000L).toLong().coerceIn(exposureRange.first, exposureRange.last))
                }, valueRange = 0f..maxSeconds.coerceAtLeast(0.001f), modifier = Modifier.width(180.dp))
            } else Text("Manual sensor exposure unavailable", color = Color.LightGray)
            if (caps.minFocusDistance > 0f) {
                Text("Focus: ${if (focus == 0f) "AUTO" else "%.2f D".format(focus)}", color = Color.White)
                Slider(value = focus, onValueChange = {
                    focus = it
                    controller.setManualFocus(if (it == 0f) null else it)
                }, valueRange = 0f..caps.minFocusDistance, modifier = Modifier.width(180.dp))
            } else Text("Manual focus unavailable", color = Color.LightGray)
            Text("White balance", color = Color.White)
            Row {
                listOf(
                    "Auto" to CaptureRequest.CONTROL_AWB_MODE_AUTO,
                    "Day" to CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT,
                    "Cloud" to CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
                ).filter { it.second in caps.supportedAwbModes }.forEach { (label, mode) ->
                    Text(label, color = if (wb == mode) Color.Cyan else Color.White,
                        modifier = Modifier.padding(end = 12.dp).then(
                            Modifier.background(Color(0x663A3A3A), RoundedCornerShape(8.dp))
                        ).padding(6.dp))
                }
            }
            Button(onClick = {
                iso = 0f; shutter = 0f; focus = 0f
                wb = CaptureRequest.CONTROL_AWB_MODE_AUTO
                controller.resetManual()
            }) { Text("Reset auto") }
        }
    }
}
