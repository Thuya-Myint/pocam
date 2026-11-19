package com.pocam.camera.ui.components

import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocam.camera.camera.CameraController
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * One compact Pro panel with one ruler per active control.
 * The parent provides the scrollable, glass-style surface.
 */
@Composable
fun ManualControls(
    controller: CameraController,
    cameraGeneration: Int,
    exposureIndex: Int,
    exposureRange: IntRange,
    exposureStepEv: Float,
    onExposure: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accent = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            dynamicDarkColorScheme(context).primary
        else Color(0xFFFFD88C)
    }
    var caps by remember { mutableStateOf<CameraController.ManualCapabilities?>(null) }
    var isoFraction by remember { mutableFloatStateOf(0f) }
    var shutterFraction by remember { mutableFloatStateOf(0f) }
    var focusFraction by remember { mutableFloatStateOf(0f) }
    var wb by remember { mutableStateOf(CaptureRequest.CONTROL_AWB_MODE_AUTO) }
    var activeControl by remember { mutableStateOf("ISO") }

    LaunchedEffect(cameraGeneration) {
        caps = controller.manualCapabilities()
        isoFraction = 0f
        shutterFraction = 0f
        focusFraction = 0f
        wb = CaptureRequest.CONTROL_AWB_MODE_AUTO
    }

    Column(
        modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            listOf("ISO", "SHUTTER", "WB", "EV", "FOCUS").forEach { control ->
                val selected = activeControl == control
                Box(
                    Modifier.weight(1f)
                        .background(if (selected) accent else Color(0xAA363943), CircleShape)
                        .clickable { activeControl = control }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        control,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color.Black else Color.White
                    )
                }
            }
        }

        val current = caps
        when (activeControl) {
            "ISO" -> {
                val range = current?.isoRange
                if (range == null) {
                    Text("Manual ISO unavailable", color = Color.LightGray)
                } else {
                    val isoValue = isoValueFor(range, isoFraction)
                    Text(
                        if (isoFraction == 0f) "ISO: AUTO" else "ISO: $isoValue",
                        color = Color.White
                    )
                    StraightRuler(
                        value = isoFraction, valueRange = 0f..1f,
                        label = if (isoFraction == 0f) "AUTO" else "$isoValue",
                        onValueChange = {
                            isoFraction = it
                            controller.setManualIso(
                                if (it == 0f) null else isoValueFor(range, it)
                            )
                        }
                    )
                }
            }
            "SHUTTER" -> {
                val range = current?.exposureRangeNs
                if (range == null) {
                    Text("Manual shutter unavailable", color = Color.LightGray)
                } else {
                    val minNs = range.first.coerceAtLeast(1L)
                    val maxNs = range.last.coerceAtMost(1_000_000_000L).coerceAtLeast(minNs)
                    val selectedNs = shutterNs(minNs, maxNs, shutterFraction)
                    val label = if (shutterFraction == 0f) "AUTO"
                        else String.format(
                            java.util.Locale.US, "%.1f ms", selectedNs / 1_000_000.0
                        )
                    Text("Shutter: $label", color = Color.White)
                    StraightRuler(
                        value = shutterFraction, valueRange = 0f..1f, label = label,
                        onValueChange = {
                            shutterFraction = it
                            controller.setManualExposureNs(
                                if (it == 0f) null else shutterNs(minNs, maxNs, it)
                            )
                        }
                    )
                }
            }
            "WB" -> {
                Text("White balance", color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val options = listOf(
                        "Auto" to CaptureRequest.CONTROL_AWB_MODE_AUTO,
                        "Day" to CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT,
                        "Cloud" to CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
                    ).filter { current?.supportedAwbModes?.contains(it.second) == true }
                    if (options.isEmpty()) Text("White balance unavailable", color = Color.LightGray)
                    options.forEach { (label, value) ->
                        Box(
                            Modifier.background(
                                if (wb == value) accent else Color(0xAA363943), CircleShape
                            ).clickable {
                                wb = value
                                controller.setWhiteBalance(value)
                            }.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                label, fontSize = 12.sp,
                                color = if (wb == value) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
            "EV" -> {
                if (exposureRange.first >= exposureRange.last) {
                    Text("Exposure compensation unavailable", color = Color.LightGray)
                } else {
                    val label = String.format(
                        java.util.Locale.US, "%.1f EV", exposureIndex * exposureStepEv
                    )
                    Text("Exposure: $label", color = Color.White)
                    StraightRuler(
                        value = exposureIndex.toFloat(),
                        valueRange = exposureRange.first.toFloat()..exposureRange.last.toFloat(),
                        label = label,
                        onValueChange = {
                            val next = it.roundToInt().coerceIn(exposureRange)
                            if (next != exposureIndex) onExposure(next)
                        }
                    )
                }
            }
            "FOCUS" -> {
                val distance = current?.minFocusDistance ?: 0f
                if (distance <= 0f) {
                    Text("Manual focus unavailable", color = Color.LightGray)
                } else {
                    val label = if (focusFraction == 0f) "AUTO"
                        else String.format(
                            java.util.Locale.US, "%.2f D", focusFraction * distance
                        )
                    Text("Focus: $label", color = Color.White)
                    StraightRuler(
                        value = focusFraction, valueRange = 0f..1f, label = label,
                        onValueChange = {
                            focusFraction = it
                            controller.setManualFocus(
                                if (it == 0f) null else it * distance
                            )
                        }
                    )
                }
            }
        }

        Box(
            Modifier.background(Color(0xAA363943), CircleShape)
                .clickable {
                    isoFraction = 0f
                    shutterFraction = 0f
                    focusFraction = 0f
                    wb = CaptureRequest.CONTROL_AWB_MODE_AUTO
                    controller.resetManual()
                    onExposure(0.coerceIn(exposureRange))
                }.padding(horizontal = 18.dp, vertical = 9.dp)
        ) {
            Text("Reset controls", color = Color.White, fontSize = 12.sp)
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
