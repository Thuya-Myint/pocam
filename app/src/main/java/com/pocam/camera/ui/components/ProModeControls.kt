package com.pocam.camera.ui.components

import android.hardware.camera2.CaptureRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocam.camera.camera.CameraController
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import java.util.Locale

/**
 * Shared Pro state prevents the vertical rail and bottom tray from disagreeing
 * about the selected parameter or its actual Camera2 request value.
 */
@Stable
class ProUiState {
    var selected by mutableStateOf("ISO")
    var isoFraction by mutableFloatStateOf(0f)
    var isoAuto by mutableStateOf(true)
    var shutterFraction by mutableFloatStateOf(0f)
    var shutterAuto by mutableStateOf(true)
    var focusFraction by mutableFloatStateOf(0f)
    var focusAuto by mutableStateOf(true)
    var whiteBalance by mutableIntStateOf(CaptureRequest.CONTROL_AWB_MODE_AUTO)
    fun reset() {
        isoFraction = 0f; shutterFraction = 0f; focusFraction = 0f
        whiteBalance = CaptureRequest.CONTROL_AWB_MODE_AUTO
        isoAuto = true; shutterAuto = true; focusAuto = true
        selected = "ISO"
    }
}

@Composable
fun ProControlRail(
    state: ProUiState, controller: CameraController, cameraGeneration: Int,
    exposureIndex: Int, exposureStepEv: Float,
    expanded: Boolean, onExpanded: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val caps = remember(controller, cameraGeneration) { controller.manualCapabilities() }
    val isoRange = caps?.isoRange
    val expRange = caps?.exposureRangeNs
    val labels = listOf(
        "ISO" to (if (state.isoAuto || isoRange == null) "AUTO"
            else isoAt(isoRange, state.isoFraction).toString()),
        "S" to (if (state.shutterAuto || expRange == null) "AUTO"
            else shutterLabel(shutterAt(expRange, state.shutterFraction))),
        "WB" to when (state.whiteBalance) {
            CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT -> "DAY"
            CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT -> "CLOUD"
            CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT -> "TUNG"
            else -> "AUTO"
        },
        "FOCUS" to (if (state.focusAuto) "AF" else "MF"),
        "EV" to String.format(Locale.US, "%+.1f", exposureIndex * exposureStepEv)
    )
    Column(
        modifier.width(62.dp).background(PocamPalette.Glass, RoundedCornerShape(27.dp))
            .border(1.dp, PocamPalette.Steel.copy(alpha = .55f), RoundedCornerShape(27.dp))
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        labels.forEach { (key, value) ->
            val active = (if (state.selected == "SHUTTER") "S" else state.selected) == key
            Column(
                Modifier.fillMaxWidth()
                    .background(if (active) PocamPalette.Orange.copy(alpha=.16f)
                        else Color.Transparent, RoundedCornerShape(20.dp))
                    .border(if (active) 1.dp else 0.dp,
                        if (active) PocamPalette.Orange else Color.Transparent,
                        RoundedCornerShape(20.dp))
                    .clickable {
                        state.selected = if (key == "S") "SHUTTER" else key
                        onExpanded(true)
                    }.padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(key, color = if (active) PocamPalette.Orange else PocamPalette.Silver,
                    fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Text(value, color = if (active) PocamPalette.OrangeSoft else Color.White,
                    fontSize = 9.sp, maxLines = 1)
            }
            Spacer(Modifier.height(2.dp))
        }
        Box(Modifier.fillMaxWidth().height(31.dp).clickable { onExpanded(!expanded) },
            contentAlignment = Alignment.Center) {
            Icon(if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse Pro adjustment tray" else "Expand Pro adjustment tray",
                tint = PocamPalette.Silver, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ProParameterTray(
    state: ProUiState, controller: CameraController, cameraGeneration: Int,
    exposureIndex: Int, exposureRange: IntRange, exposureStepEv: Float,
    onExposure: (Int) -> Unit, onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val caps = remember(controller, cameraGeneration) { controller.manualCapabilities() }
    val active = state.selected
    Column(
        modifier.fillMaxWidth().background(PocamPalette.Glass, RoundedCornerShape(25.dp))
            .border(1.dp, PocamPalette.Steel.copy(alpha=.45f), RoundedCornerShape(25.dp))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically) {
            listOf("ISO", "SHUTTER", "WB", "FOCUS", "EV").forEach { item ->
                val selected = active == item
                Box(Modifier.weight(1f)
                    .background(if (selected) PocamPalette.Orange else PocamPalette.Graphite,
                        CircleShape)
                    .clickable { state.selected = item }
                    .padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                    Text(if (item == "SHUTTER") "S" else item, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) PocamPalette.Black else Color.White)
                }
            }
            Box(Modifier.size(30.dp).background(PocamPalette.Graphite, CircleShape)
                .clickable(onClick=onCollapse), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.KeyboardArrowDown, "Close adjustment tray",
                    tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        when(active) {
            "ISO" -> {
                val range = caps?.isoRange
                if (range == null) {
                    NotSupported("Manual ISO not available for this lens")
                } else {
                    val value = isoAt(range, state.isoFraction)
                    Text(if (state.isoAuto) "ISO · AUTO" else "ISO · $value",
                        color = PocamPalette.OrangeSoft, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    StraightRuler(
                        value=state.isoFraction, valueRange=0f..1f,
                        label=if (state.isoAuto) "AUTO" else "$value",
                        onValueChange={ fraction ->
                            state.isoFraction = fraction
                            state.isoAuto = false
                            controller.setManualIso(isoAt(range, fraction))
                        }
                    )
                    PresetRow(
                        listOf("AUTO", "100", "200", "400", "800"),
                        active=if(state.isoAuto) "AUTO" else value.toString()
                    ) { preset ->
                        if(preset=="AUTO") {
                            state.isoFraction=0f; state.isoAuto=true; controller.setManualIso(null)
                        } else {
                            val iso=preset.toInt().coerceIn(range)
                            state.isoAuto=false
                            state.isoFraction=(iso-range.first).toFloat()/
                                (range.last-range.first).coerceAtLeast(1)
                            controller.setManualIso(iso)
                        }
                    }
                }
            }
            "SHUTTER" -> {
                val range=caps?.exposureRangeNs
                if(range==null) NotSupported("Manual shutter speed not available for this lens")
                else {
                    val ns=shutterAt(range,state.shutterFraction)
                    Text(if(state.shutterAuto) "SHUTTER · AUTO"
                        else "SHUTTER · ${shutterLabel(ns)}",
                        color=PocamPalette.OrangeSoft,fontWeight=FontWeight.Bold,fontSize=13.sp)
                    StraightRuler(
                        value=state.shutterFraction,valueRange=0f..1f,
                        label=if(state.shutterAuto) "AUTO" else shutterLabel(ns),
                        onValueChange={ f ->
                            state.shutterFraction=f
                            state.shutterAuto=false
                            controller.setManualExposureNs(shutterAt(range,f))
                        })
                    PresetRow(listOf("AUTO","1/30","1/60","1/125","1/250"),
                        active=if(state.shutterAuto) "AUTO" else shutterLabel(ns)) { preset ->
                        if(preset=="AUTO") { state.shutterFraction=0f; state.shutterAuto=true; controller.setManualExposureNs(null) }
                        else {
                            val denominator=preset.substringAfter("/").toInt()
                            val target=(1_000_000_000L/denominator).coerceIn(range)
                            state.shutterFraction=fractionAt(range,target)
                            state.shutterAuto=false
                            controller.setManualExposureNs(target)
                        }
                    }
                }
            }
            "WB" -> {
                Text("WHITE BALANCE",color=PocamPalette.OrangeSoft,fontWeight=FontWeight.Bold,fontSize=13.sp)
                val modes=listOf(
                    "AUTO" to CaptureRequest.CONTROL_AWB_MODE_AUTO,
                    "DAY" to CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT,
                    "CLOUD" to CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT,
                    "TUNG" to CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT
                ).filter { caps?.supportedAwbModes?.contains(it.second)==true }
                if (modes.isEmpty()) NotSupported("White balance controls unavailable")
                else PresetRow(modes.map { it.first },
                    modes.firstOrNull { it.second==state.whiteBalance }?.first ?: "AUTO"
                ) { label ->
                    val mode=modes.first { it.first==label }.second
                    state.whiteBalance=mode;controller.setWhiteBalance(mode)
                }
            }
            "FOCUS" -> {
                val distance=caps?.minFocusDistance ?: 0f
                if(distance<=0f) NotSupported("Manual focus not available for this lens")
                else {
                    Text(if(state.focusAuto) "FOCUS · AUTO"
                         else String.format(Locale.US,"FOCUS · %.2f D",distance*state.focusFraction),
                        color=PocamPalette.OrangeSoft,fontWeight=FontWeight.Bold,fontSize=13.sp)
                    StraightRuler(state.focusFraction,0f..1f,
                        { f -> state.focusFraction=f
                            state.focusAuto=false
                            controller.setManualFocus(f*distance)
                        },if(state.focusAuto) "AF" else "MF")
                    PresetRow(listOf("AUTO","MANUAL"),
                        if(state.focusAuto)"AUTO" else "MANUAL") { choice ->
                        state.focusAuto=choice=="AUTO"
                        controller.setManualFocus(
                            if(state.focusAuto) null else state.focusFraction*distance
                        )
                    }
                }
            }
            "EV" -> {
                if(exposureRange.first>=exposureRange.last) NotSupported("Exposure compensation unavailable")
                else {
                    val label=String.format(Locale.US,"EV · %+.1f",exposureIndex*exposureStepEv)
                    Text(label,color=PocamPalette.OrangeSoft,fontWeight=FontWeight.Bold,fontSize=13.sp)
                    StraightRuler(exposureIndex.toFloat(),
                        exposureRange.first.toFloat()..exposureRange.last.toFloat(),
                        { value ->
                            val next=value.roundToInt().coerceIn(exposureRange)
                            if(next!=exposureIndex)onExposure(next)
                        },label)
                }
            }
        }
    }
}

@Composable
private fun NotSupported(message:String) {
    Text(message,color=PocamPalette.Silver,fontSize=13.sp,modifier=Modifier.padding(vertical=18.dp))
}
@Composable
private fun PresetRow(presets:List<String>,active:String,onChoose:(String)->Unit) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        presets.forEach { label ->
            Box(Modifier.weight(1f).background(
                if(label==active)PocamPalette.Orange else PocamPalette.Graphite,CircleShape)
                .clickable{onChoose(label)}.padding(vertical=7.dp),
                contentAlignment=Alignment.Center) {
                Text(label,fontSize=10.sp,fontWeight=FontWeight.Bold,
                    color=if(label==active)PocamPalette.Black else Color.White)
            }
        }
    }
}
private fun isoAt(range:IntRange,f:Float)=
    (range.first+(range.last-range.first)*f).roundToInt().coerceIn(range)

private fun shutterAt(range:LongRange,f:Float):Long {
    val low=range.first.coerceAtLeast(1L)
    val high=range.last.coerceAtLeast(low)
    if(high==low)return low
    return exp(ln(low.toDouble())+(ln(high.toDouble())-ln(low.toDouble()))*f)
        .toLong().coerceIn(low,high)
}
private fun shutterLabel(ns:Long):String =
    if(ns<1_000_000_000L) "1/${(1_000_000_000.0/ns.coerceAtLeast(1)).roundToInt()}s"
    else String.format(Locale.US,"%.1fs",ns/1_000_000_000.0)
private fun fractionAt(range:LongRange,value:Long):Float{
    val low=range.first.coerceAtLeast(1L).toDouble()
    val high=range.last.coerceAtLeast(range.first).toDouble()
    if(high<=low)return 0f
    return ((ln(value.toDouble())-ln(low))/(ln(high)-ln(low))).toFloat().coerceIn(0f,1f)
}
