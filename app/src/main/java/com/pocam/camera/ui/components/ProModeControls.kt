package com.pocam.camera.ui.components

import android.hardware.camera2.CaptureRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    modifier: Modifier = Modifier, motionEnabled: Boolean = true
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
    // Pencil-thin control rail; fixed cell heights make every row line up.
    val chevronRotation = animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = if (motionEnabled) tween(155) else snap(),
        label = "pocam-pro-chevron"
    )
    val railShape = RoundedCornerShape(25.dp)
    Column(
        modifier.width(51.dp)
            .background(Color(0xEB17191F), railShape)
            .border(0.75.dp, PocamPalette.Steel.copy(alpha = .56f), railShape)
            .padding(horizontal = 3.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        labels.forEach { (key, value) ->
            val active = (if (state.selected == "SHUTTER") "S" else state.selected) == key
            val activeFill by animateColorAsState(
                targetValue = if (active) PocamPalette.Orange.copy(alpha = .15f) else Color.Transparent,
                animationSpec = if (motionEnabled) tween(145) else snap(),
                label = "pocam-pro-rail-highlight"
            )
            Column(
                Modifier.fillMaxWidth().height(58.dp)
                    .background(activeFill, CircleShape)
                    .border(if (active) .65.dp else 0.dp,
                        if (active) PocamPalette.Orange else Color.Transparent, CircleShape)
                    .clickable {
                        state.selected = if (key == "S") "SHUTTER" else key
                        onExpanded(true)
                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(key, color = if (active) PocamPalette.Orange else Color.White,
                    fontWeight = FontWeight.Medium, fontSize = 9.sp, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                Text(value, color = if (active) PocamPalette.OrangeSoft else PocamPalette.Silver,
                    fontSize = 8.sp, maxLines = 1)
            }
            Spacer(Modifier.height(2.dp))
        }
        Box(Modifier.fillMaxWidth().height(25.dp).clickable { onExpanded(!expanded) },
            contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse Pro adjustment tray" else "Expand Pro adjustment tray",
                tint = Color.White, modifier = Modifier.size(18.dp)
                    .graphicsLayer { rotationZ = chevronRotation.value })
        }
    }
}

@Composable
fun ProParameterTray(
    state: ProUiState, controller: CameraController, cameraGeneration: Int,
    exposureIndex: Int, exposureRange: IntRange, exposureStepEv: Float,
    onExposure: (Int) -> Unit,
    expanded: Boolean, onDetailsToggle: () -> Unit,
    modifier: Modifier = Modifier, motionEnabled: Boolean = true
) {
    val caps = remember(controller, cameraGeneration) { controller.manualCapabilities() }
    val active = state.selected
    val enter = remember { Animatable(if (motionEnabled) 0f else 1f) }
    val bodyFade = remember { Animatable(1f) }
    LaunchedEffect(motionEnabled) {
        if (motionEnabled) enter.animateTo(1f, tween(155))
        else enter.snapTo(1f)
    }
    LaunchedEffect(active, motionEnabled) {
        if (motionEnabled) {
            bodyFade.snapTo(.9f)
            bodyFade.animateTo(1f, tween(125))
        } else bodyFade.snapTo(1f)
    }
    Column(
        modifier.fillMaxWidth()
            .graphicsLayer {
                alpha = enter.value
                translationY = 7.dp.toPx() * (1f - enter.value)
            }
            .background(Color(0xF3191B22), RoundedCornerShape(18.dp))
            .border(.75.dp, PocamPalette.Steel.copy(alpha=.55f), RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically) {
            listOf("ISO", "SHUTTER", "WB", "FOCUS", "EV").forEach { item ->
                val selected = active == item
                Box(Modifier.weight(1f)
                    .background(if (selected) PocamPalette.Orange.copy(alpha = .16f)
                        else Color.Transparent, CircleShape)
                    .border(0.dp, Color.Transparent, CircleShape)
                    .clickable { state.selected = item }
                    .height(29.dp), contentAlignment = Alignment.Center) {
                    Text(if (item == "SHUTTER") "S" else item, fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selected) PocamPalette.Orange else Color.White)
                }
            }
            Box(Modifier.size(25.dp).background(PocamPalette.Graphite, CircleShape)
                .clickable(onClick = onDetailsToggle),
                contentAlignment = Alignment.Center) {
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowDown
                    else Icons.Rounded.KeyboardArrowUp,
                    contentDescription = if (expanded) "Show compact Pro controls"
                        else "Show all Pro presets",
                    tint = Color.White, modifier = Modifier.size(17.dp)
                )
            }
        }
        Column(
            Modifier.graphicsLayer { alpha = bodyFade.value },
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
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
                        }, showLabel = false, compact = true, darkOnLight = false
                    )
                    if (expanded) PresetRow(
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
                        }, showLabel = false, compact = true, darkOnLight = false)
                    if (expanded) PresetRow(listOf("AUTO","1/30","1/60","1/125","1/250"),
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
                        },if(state.focusAuto) "AF" else "MF",
                        showLabel = false, compact = true, darkOnLight = false)
                    if (expanded) PresetRow(listOf("AUTO","MANUAL"),
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
                        },label, showLabel = false, compact = true, darkOnLight = false)
                }
            }
        }
        }
    }
}

@Composable
private fun NotSupported(message:String) {
    Text(message,color=PocamPalette.Silver,fontSize=11.sp,modifier=Modifier.padding(vertical=10.dp))
}
@Composable
private fun PresetRow(presets:List<String>,active:String,onChoose:(String)->Unit) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        presets.forEach { label ->
            Box(Modifier.weight(1f)
                .background(if(label==active)PocamPalette.Orange.copy(alpha = .18f)
                    else PocamPalette.Graphite,CircleShape)
                .border(0.dp,Color.Transparent,CircleShape)
                .clickable{onChoose(label)}.padding(vertical=6.dp),
                contentAlignment=Alignment.Center) {
                Text(label,fontSize=9.sp,fontWeight=FontWeight.Medium,
                    color=if(label==active)PocamPalette.Orange else Color.White)
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
