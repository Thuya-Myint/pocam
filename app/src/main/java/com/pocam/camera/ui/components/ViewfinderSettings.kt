package com.pocam.camera.ui.components

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val surface = Color(0xC622252D)

@Composable
fun ViewfinderSettings(
    grid: Boolean, onGrid: (Boolean) -> Unit,
    level: Boolean, onLevel: (Boolean) -> Unit,
    torch: Boolean, torchAvailable: Boolean, onTorch: (Boolean) -> Unit,
    timer: Int, onTimer: (Int) -> Unit,
    videoQuality: String, onVideoQuality: (String) -> Unit,
    exposure: Int, exposureRange: IntRange, onExposure: (Int) -> Unit,
    onClose: () -> Unit, modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accent = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(context).primary
        else Color(0xFF89B9FF)
    }
    var section by remember { mutableStateOf("Capture") }
    var photoResolution by remember { mutableStateOf("Maximum available") }
    var imageFormat by remember { mutableStateOf("JPEG") }
    var hdr by remember { mutableStateOf(false) }
    var shutterSound by remember { mutableStateOf(true) }
    var focusTracking by remember { mutableStateOf(false) }
    var stabilization by remember { mutableStateOf(true) }
    var appearance by remember { mutableStateOf("System") }
    var dialog by remember { mutableStateOf<String?>(null) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth().background(surface, RoundedCornerShape(30.dp))
            .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(Color(0xFF30343D), CircleShape)
                .clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, "Back to camera", tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Settings", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Pocam Camera", color = Color.LightGray, fontSize = 13.sp)
            }
            Box(Modifier.size(48.dp).background(Color(0xFF30343D), CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Tune, null, tint = accent)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Capture", "Camera", "Video", "Appearance").forEach { name ->
                Box(Modifier.weight(1f).background(
                    if (section == name) accent else surface, CircleShape)
                    .clickable { section = name }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center) {
                    Text(name, color = if (section == name) Color.Black else Color.White,
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        AnimatedContent(targetState = section, transitionSpec = {
            androidx.compose.animation.fadeIn(tween(160)) togetherWith androidx.compose.animation.fadeOut(tween(110))
        }, label = "settings-section") { section ->
        when (section) {
            "Capture" -> {
                SettingsGroup("Capture", Icons.Rounded.PhotoLibrary, accent) {
                    SettingsChoice(Icons.Rounded.PhotoLibrary, "Photo resolution", photoResolution) { dialog = "Photo resolution" }
                    SettingsChoice(Icons.Rounded.PhotoLibrary, "Image format", imageFormat) { dialog = "Image format" }
                    SettingsToggle(Icons.Rounded.Tune, "HDR", "Improve highlight and shadow detail", hdr, accent, onCheckedChange = { hdr = it })
                    SettingsToggle(Icons.Rounded.GridOn, "Grid lines", "Rule of thirds", grid, accent, onGrid)
                    SettingsToggle(Icons.Rounded.GridOn, "Level guide", "Keep photos straight", level, accent, onLevel)
                }
                SettingsGroup("Capture timer", Icons.Rounded.Timer, accent) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf(0, 3, 5, 10).forEach { seconds ->
                            val selected = timer == seconds
                            Box(Modifier.size(58.dp).background(if (selected) accent else Color(0xFF383C46), CircleShape)
                                .clickable { onTimer(seconds) }, contentAlignment = Alignment.Center) {
                                Text(if (seconds == 0) "OFF" else "${seconds}s",
                                    color = if (selected) Color.Black else Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            "Camera" -> SettingsGroup("Camera", Icons.Rounded.Tune, accent) {
                SettingsToggle(Icons.Rounded.FlashOn, "Live torch", "Use camera light", torch, accent, onTorch, torchAvailable)
                SettingsToggle(Icons.Rounded.Tune, "Shutter sound", "Play sound when taking a photo", shutterSound, accent, onCheckedChange = { shutterSound = it })
                SettingsToggle(Icons.Rounded.Tune, "Focus tracking", "Track moving subjects", focusTracking, accent, onCheckedChange = { focusTracking = it })
                SettingsChoice(Icons.Rounded.Tune, "Exposure", "${exposure} steps") { dialog = "Exposure" }
            }
            "Video" -> SettingsGroup("Video", Icons.Rounded.Videocam, accent) {
                SettingsChoice(Icons.Rounded.Videocam, "Video resolution", videoQuality) { dialog = "Video resolution" }
                SettingsToggle(Icons.Rounded.Videocam, "Video stabilization", "Reduce shake while recording", stabilization, accent, onCheckedChange = { stabilization = it })
            }
            else -> SettingsGroup("Appearance", Icons.Rounded.Tune, accent) {
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("System", "Light", "Dark").forEach { name ->
                        Box(Modifier.weight(1f).background(if (appearance == name) accent else Color(0xFF343740), CircleShape)
                            .clickable { appearance = name }.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center) {
                            Text(name, color = if (appearance == name) Color.Black else Color.White)
                        }
                    }
                }
            }
        }
    }
    }
    dialog?.let { target ->
        val options = when (target) {
            "Photo resolution" -> listOf("Maximum available", "12 MP", "8 MP")
            "Image format" -> listOf("JPEG", "HEIF")
            "Video resolution" -> listOf("720p", "1080p", "4K")
            else -> emptyList()
        }
        AlertDialog(onDismissRequest = { dialog = null },
            title = { Text(target) },
            text = { Column {
                if (options.isEmpty()) Text("Adjust exposure using the camera's expanded header.")
                options.forEach { option ->
                    Text(option, Modifier.fillMaxWidth().clickable {
                        when (target) {
                            "Photo resolution" -> photoResolution = option
                            "Image format" -> imageFormat = option
                            "Video resolution" -> onVideoQuality(option)
                        }
                        dialog = null
                    }.padding(14.dp))
                }
            } },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Close") } })
    }
}

@Composable
private fun SettingsGroup(title: String, icon: ImageVector, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(start = 12.dp)) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.fillMaxWidth().background(surface, RoundedCornerShape(28.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp), content = content)
    }
}

@Composable
private fun SettingsChoice(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium)
            Text(value, color = Color.LightGray, fontSize = 12.sp)
        }
        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color.LightGray)
    }
}

@Composable
private fun SettingsToggle(icon: ImageVector, title: String, subtitle: String,
    checked: Boolean, accent: Color, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (enabled) Color.White else Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) Color.White else Color.Gray, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color.LightGray, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = accent))
    }
}
