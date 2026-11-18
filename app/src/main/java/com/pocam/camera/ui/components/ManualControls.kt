package com.pocam.camera.ui.components

import android.hardware.camera2.CaptureRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pocam.camera.camera.CameraController
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

@Composable
fun ManualControls(controller: CameraController, cameraGeneration: Int, modifier: Modifier = Modifier) {
    var caps by remember { mutableStateOf<CameraController.ManualCapabilities?>(null) }
    var isoFraction by remember { mutableFloatStateOf(0f) }
    var shutterFraction by remember { mutableFloatStateOf(0f) }
    var focusFraction by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(cameraGeneration) {
        caps = controller.manualCapabilities()
        isoFraction = 0f
        shutterFraction = 0f
        focusFraction = 0f
    }
    var wb by remember { mutableStateOf(CaptureRequest.CONTROL_AWB_MODE_AUTO) }

    Column(modifier.width(220.dp)
        .background(Color(0xE6202126), RoundedCornerShape(16.dp))
        .verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("PRO CONTROLS", color = Color.White)
        Button(onClick = { caps = controller.manualCapabilities() }) {
            Text(if (caps == null) "Load controls" else "Refresh capabilities")
        }
        val current = caps
        if (current != null) {
            val isoRange = current.isoRange
            val exposureRange = current.exposureRangeNs
            if (isoRange != null && exposureRange != null) {
                val isoValue = (isoRange.first + (isoRange.last - isoRange.first) *
                    isoFraction).roundToInt().coerceIn(isoRange)
                Text(if (isoFraction == 0f) "ISO: AUTO" else "ISO: $isoValue", color = Color.White)
                Slider(value = isoFraction, valueRange = 0f..1f, onValueChange = {
                    isoFraction = it
                    controller.setManualIso(if (it == 0f) null else isoValueFor(isoRange, it))
                })
                val minNs = exposureRange.first.coerceAtLeast(1L)
                val maxNs = exposureRange.last.coerceAtMost(1_000_000_000L).coerceAtLeast(minNs)
                val selectedNs = shutterNs(minNs, maxNs, shutterFraction)
                Text(if (shutterFraction == 0f) "Shutter: AUTO"
                    else "Shutter: ${"%.1f".format(selectedNs / 1_000_000.0)} ms",
                    color = Color.White)
                Slider(value = shutterFraction, valueRange = 0f..1f, onValueChange = {
                    shutterFraction = it
                    controller.setManualExposureNs(
                        if (it == 0f) null else shutterNs(minNs, maxNs, it))
                })
                Text("ISO/shutter AUTO uses a manual baseline when the other is fixed",
                    color = Color.LightGray)
            } else Text("Manual ISO/shutter unavailable", color = Color.LightGray)
            if (current.minFocusDistance > 0f) {
                Text(if (focusFraction == 0f) "Focus: AUTO"
                    else "Focus: ${"%.2f".format(focusFraction * current.minFocusDistance)} D",
                    color = Color.White)
                Slider(value = focusFraction, onValueChange = {
                    focusFraction = it
                    controller.setManualFocus(if (it == 0f) null else
                        it * current.minFocusDistance)
                })
            } else Text("Manual focus unavailable", color = Color.LightGray)
            Text("White balance", color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Auto" to CaptureRequest.CONTROL_AWB_MODE_AUTO,
                    "Day" to CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT,
                    "Cloud" to CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT)
                    .filter { it.second in current.supportedAwbModes }
                    .forEach { (label, value) ->
                        Text(label, color = if (wb == value) Color.Cyan else Color.White,
                            modifier = Modifier.background(Color(0x663A3A3A),
                                RoundedCornerShape(6.dp))
                                .clickable { wb = value; controller.setWhiteBalance(value) }
                                .padding(5.dp))
                    }
            }
            Button(onClick = {
                isoFraction = 0f; shutterFraction = 0f; focusFraction = 0f
                wb = CaptureRequest.CONTROL_AWB_MODE_AUTO
                controller.resetManual()
            }) { Text("Reset all") }
        }
    }
}

private fun isoValueFor(range: IntRange, fraction: Float): Int =
    (range.first + (range.last - range.first) * fraction)
        .roundToInt().coerceIn(range)

private fun shutterNs(minNs: Long, maxNs: Long, fraction: Float): Long {
    if (minNs >= maxNs) return minNs
    return exp(ln(minNs.toDouble()) +
        (ln(maxNs.toDouble()) - ln(minNs.toDouble())) * fraction.toDouble())
        .toLong().coerceIn(minNs, maxNs)
}
