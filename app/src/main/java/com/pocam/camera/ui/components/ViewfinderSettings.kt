package com.pocam.camera.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

/** Pocam v0.5 — real camera settings in a unified graphite/steel/orange UI. */
@Composable
fun ViewfinderSettings(
    grid: Boolean, onGrid: (Boolean) -> Unit,
    level: Boolean, onLevel: (Boolean) -> Unit,
    torch: Boolean, torchAvailable: Boolean, onTorch: (Boolean) -> Unit,
    timer: Int, onTimer: (Int) -> Unit,
    videoQuality: String, onVideoQuality: (String) -> Unit,
    videoStabilization: Boolean, onVideoStabilization: (Boolean) -> Unit,
    shutterSound: Boolean, onShutterSound: (Boolean) -> Unit,
    headerOpacity: Float, onHeaderOpacity: (Float) -> Unit,
    exposure: Int, exposureRange: IntRange, onExposure: (Int) -> Unit,
    onClose: () -> Unit, modifier: Modifier = Modifier
) {
    var tab by remember { mutableStateOf("Capture") }
    val orange = PocamPalette.Orange
    Column(modifier.fillMaxWidth(), verticalArrangement=Arrangement.spacedBy(17.dp)) {
        Row(Modifier.fillMaxWidth()
            .background(PocamPalette.GlassLight, RoundedCornerShape(28.dp))
            .border(1.dp, PocamPalette.Steel.copy(alpha=.38f), RoundedCornerShape(28.dp))
            .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(PocamPalette.Graphite, CircleShape)
                .clickable(onClick=onClose),contentAlignment=Alignment.Center) {
                Icon(Icons.Rounded.Close,contentDescription="Return to camera",tint=Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Settings",color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold)
                Text("POCAM  ·  CAMERA",color=PocamPalette.Silver,
                    fontSize=10.sp,letterSpacing=1.sp)
            }
            Box(Modifier.size(46.dp).background(PocamPalette.Graphite,CircleShape),
                contentAlignment=Alignment.Center) {
                Icon(Icons.Rounded.Tune,contentDescription=null,tint=orange)
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("Capture","Camera","Video","Appearance").forEach { section ->
                val selected=section==tab
                Box(Modifier.weight(1f).background(
                    if(selected)orange else PocamPalette.Graphite,CircleShape)
                    .clickable { tab=section }.padding(vertical=12.dp),
                    contentAlignment=Alignment.Center) {
                    Text(section,fontSize=11.sp,
                        color=if(selected)PocamPalette.Black else Color.White,
                        fontWeight=FontWeight.SemiBold)
                }
            }
        }
        AnimatedContent(targetState=tab,transitionSpec={
            fadeIn(tween(145)) togetherWith fadeOut(tween(110))
        },label="settings-tabs") { selected ->
            Column(verticalArrangement=Arrangement.spacedBy(17.dp)) {
                when(selected) {
                    "Capture" -> {
                        SettingsSection("Capture",Icons.Rounded.PhotoLibrary) {
                            SettingsInfo(Icons.Rounded.PhotoLibrary,"Photo resolution",
                                "Highest stable quality available on device")
                            SettingsSeparator()
                            SettingsInfo(Icons.Rounded.Image,"Image format",
                                "JPEG  ·  maximum compatibility")
                            SettingsSeparator()
                            SettingsInfo(Icons.Rounded.Tune,"HDR",
                                "Uses device extensions in supported modes")
                            SettingsSeparator()
                            SettingsSwitch(Icons.Rounded.GridOn,"Grid lines",
                                "Rule-of-thirds guide",grid,onGrid)
                            SettingsSeparator()
                            SettingsSwitch(Icons.Rounded.CenterFocusStrong,"Center guide",
                                "Composition crosshair",level,onLevel)
                        }
                        SettingsSection("Capture timer",Icons.Rounded.Timer) {
                            Row(Modifier.fillMaxWidth().padding(vertical=6.dp),
                                horizontalArrangement=Arrangement.SpaceBetween) {
                                listOf(0,3,5,10).forEach { value ->
                                    val active=timer==value
                                    Box(Modifier.size(57.dp).background(
                                        if(active)orange else PocamPalette.Graphite,CircleShape)
                                        .clickable { onTimer(value) },contentAlignment=Alignment.Center) {
                                        Text(if(value==0)"OFF" else "${value}s",
                                            fontSize=12.sp,fontWeight=FontWeight.Bold,
                                            color=if(active)PocamPalette.Black else Color.White)
                                    }
                                }
                            }
                        }
                    }
                    "Camera" -> {
                        SettingsSection("Camera",Icons.Rounded.Tune) {
                            SettingsSwitch(Icons.Rounded.FlashOn,"Live torch",
                                if(torchAvailable)"Illuminate the preview" else "Flash not available on this camera",
                                torch,onTorch,enabled=torchAvailable)
                            SettingsSeparator()
                            SettingsSwitch(Icons.Rounded.VolumeUp,"Shutter sound",
                                "Play a capture confirmation tone",shutterSound,onShutterSound)
                            SettingsSeparator()
                            SettingsInfo(Icons.Rounded.CenterFocusStrong,"Touch focus",
                                "Tap preview to focus · hold to lock")
                            SettingsSeparator()
                            SettingsInfo(Icons.Rounded.Tune,"Focus tracking",
                                "Not available in this build")
                        }
                        SettingsSection("Exposure compensation",Icons.Rounded.Tune) {
                            if(exposureRange.first>=exposureRange.last) {
                                Text("Unavailable on this camera",color=PocamPalette.Silver,fontSize=13.sp)
                            } else {
                                Text("EV steps: $exposure",color=Color.White,fontSize=13.sp)
                                StraightRuler(exposure.toFloat(),
                                    exposureRange.first.toFloat()..exposureRange.last.toFloat(),
                                    { v -> onExposure(v.roundToInt().coerceIn(exposureRange)) },
                                    "$exposure")
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                                    SettingsChip("RESET",exposure==0) { onExposure(0.coerceIn(exposureRange)) }
                                }
                            }
                        }
                    }
                    "Video" -> {
                        SettingsSection("Video",Icons.Rounded.Videocam) {
                            SettingsInfo(Icons.Rounded.Videocam,"Video resolution",
                                "Selected: $videoQuality  ·  device may use supported fallback")
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                listOf("720p","1080p","4K").forEach { quality ->
                                    Box(Modifier.weight(1f).background(
                                        if(quality==videoQuality)orange else PocamPalette.Graphite,CircleShape)
                                        .clickable { onVideoQuality(quality) }
                                        .padding(vertical=11.dp),contentAlignment=Alignment.Center) {
                                        Text(quality,color=if(quality==videoQuality)PocamPalette.Black
                                            else Color.White,fontWeight=FontWeight.Bold,fontSize=12.sp)
                                    }
                                }
                            }
                            SettingsSeparator()
                            SettingsSwitch(Icons.Rounded.Videocam,"Video stabilization",
                                "Request smoother video where supported",
                                videoStabilization,onVideoStabilization)
                            SettingsSeparator()
                            SettingsInfo(Icons.Rounded.Tune,"Frame rate",
                                "Automatic  ·  selected by camera hardware")
                            SettingsSeparator()
                            SettingsInfo(Icons.Rounded.Tune,"Video codec",
                                "Managed by Android's recorder")
                        }
                    }
                    else -> {
                        SettingsSection("Studio appearance",Icons.Rounded.Tune) {
                            Row(verticalAlignment=Alignment.CenterVertically,
                                horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                listOf(
                                    PocamPalette.Black,PocamPalette.Graphite,
                                    PocamPalette.Silver,PocamPalette.Orange
                                ).forEach { swatch ->
                                    Box(Modifier.size(36.dp).background(swatch,CircleShape)
                                        .border(1.dp,PocamPalette.Steel,CircleShape))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("Fixed studio palette  ·  Orange / Black / Gray / Silver",
                                fontSize=12.sp,color=PocamPalette.Silver)
                            SettingsSeparator()
                            Text("Camera header opacity",color=Color.White,fontWeight=FontWeight.Medium)
                            Slider(value=headerOpacity,onValueChange=onHeaderOpacity,
                                valueRange=.45f..1f,colors=SliderDefaults.colors(
                                    thumbColor=orange,activeTrackColor=orange,
                                    inactiveTrackColor=PocamPalette.Steel))
                            Text("${(headerOpacity*100).roundToInt()}%  ·  adjusts the preview header",
                                fontSize=12.sp,color=PocamPalette.Silver)
                            SettingsSeparator()
                            SettingsInfo(Icons.Rounded.GridOn,"Control layout",
                                "Curved zoom and mode selector  ·  fixed")
                            SettingsSeparator()
                            SettingsInfo(Icons.Rounded.Tune,"UI animations",
                                "Fast fade and slide transitions")
                        }
                        SettingsSection("Advanced tools",Icons.Rounded.Tune) {
                            SettingsInfo(Icons.Rounded.Tune,"RAW DNG / Histogram",
                                "Planned  ·  not yet available")
                            SettingsSeparator()
                            SettingsInfo(Icons.Rounded.Tune,"Focus peaking / Zebra",
                                "Planned  ·  not yet available")
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}
@Composable
private fun SettingsSection(
    title:String, icon:ImageVector, content:@Composable ColumnScope.()->Unit
) {
    Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
        Row(Modifier.padding(start=10.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Icon(icon,null,tint=PocamPalette.Orange,modifier=Modifier.size(20.dp))
            Text(title,color=Color.White,fontWeight=FontWeight.SemiBold,fontSize=15.sp)
        }
        Column(Modifier.fillMaxWidth().background(
            PocamPalette.GlassLight,RoundedCornerShape(26.dp))
            .border(1.dp,PocamPalette.Steel.copy(alpha=.22f),RoundedCornerShape(26.dp))
            .padding(horizontal=17.dp,vertical=10.dp),content=content)
    }
}
@Composable
private fun SettingsInfo(icon:ImageVector,name:String,description:String) {
    Row(Modifier.fillMaxWidth().padding(vertical=10.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Icon(icon,null,tint=PocamPalette.Silver,modifier=Modifier.size(23.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name,color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Medium)
            Text(description,color=PocamPalette.Silver,fontSize=11.sp)
        }
    }
}
@Composable
private fun SettingsSwitch(icon:ImageVector,name:String,description:String,
    value:Boolean,onChange:(Boolean)->Unit,enabled:Boolean=true) {
    Row(Modifier.fillMaxWidth().padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
        Icon(icon,null,tint=if(enabled)PocamPalette.Silver else PocamPalette.Steel,
            modifier=Modifier.size(23.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name,color=if(enabled)Color.White else PocamPalette.Steel,
                fontSize=14.sp,fontWeight=FontWeight.Medium)
            Text(description,color=PocamPalette.Silver,fontSize=11.sp)
        }
        Switch(checked=value,onCheckedChange=onChange,enabled=enabled,
            colors=SwitchDefaults.colors(
                checkedTrackColor=PocamPalette.Orange,
                checkedThumbColor=Color.White,
                uncheckedTrackColor=PocamPalette.Graphite,
                uncheckedThumbColor=PocamPalette.Silver
            ))
    }
}
@Composable private fun SettingsSeparator(){
    HorizontalDivider(color=PocamPalette.Steel.copy(alpha=.23f))
}
@Composable private fun SettingsChip(label:String,selected:Boolean,onClick:()->Unit){
    Box(Modifier.background(if(selected)PocamPalette.Orange else PocamPalette.Graphite,CircleShape)
        .clickable(onClick=onClick).padding(horizontal=14.dp,vertical=8.dp),
        contentAlignment=Alignment.Center) {
        Text(label,color=if(selected)PocamPalette.Black else Color.White,fontSize=11.sp)
    }
}
