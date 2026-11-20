package com.pocam.camera.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val paper = Color(0xFFF4F5F7)
private val ink = Color(0xFF252830)
private val secondaryInk = Color(0xFF767D87)
private val hairline = Color(0xFFE7E9ED)
private val idleControl = Color(0xFFE9EBEE)

/** Fully native, flat Settings screen. No blur or full-screen transitions. */
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
    motionEnabled: Boolean, onMotionEnabled: (Boolean) -> Unit,
    onClose: () -> Unit, modifier: Modifier = Modifier
) {
    val tabs = remember { listOf("Camera", "Video", "General", "About") }
    var tab by remember { mutableStateOf("Camera") }
    var draftOpacity by remember { mutableFloatStateOf(headerOpacity) }
    val contentScroll = rememberScrollState()

    LaunchedEffect(tab) { contentScroll.scrollTo(0) }

    Column(modifier.fillMaxSize().background(paper)) {
        Row(
            Modifier.fillMaxWidth().background(paper)
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp)
                    .background(Color.White, CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ArrowBack, "Close Settings", tint = ink,
                    modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = ink, modifier = Modifier.weight(1f))
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val icons = listOf(Icons.Rounded.PhotoCamera, Icons.Rounded.Videocam,
                Icons.Rounded.Settings, Icons.Rounded.Info)
            tabs.forEachIndexed { index, title ->
                val active = tab == title
                val fill by animateColorAsState(
                    targetValue = if (active) ink else Color.White,
                    animationSpec = if (motionEnabled) tween(140) else snap(),
                    label = "settings-tab-fill"
                )
                val textColor by animateColorAsState(
                    targetValue = if (active) Color.White else ink,
                    animationSpec = if (motionEnabled) tween(140) else snap(),
                    label = "settings-tab-ink"
                )
                Column(
                    Modifier.weight(1f).height(71.dp)
                        .background(fill, RoundedCornerShape(19.dp))
                        .clickable { if (tab != title) tab = title },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(icons[index], null, tint = textColor,
                        modifier = Modifier.size(21.dp))
                    Spacer(Modifier.height(5.dp))
                    Text(title, color = textColor, fontSize = 10.sp,
                        fontWeight = FontWeight.Medium)
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(contentScroll)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            when (tab) {
                "Camera" -> {
                    SettingsGroup("Capture") {
                        FlatToggle(Icons.Rounded.GridOn, "Grid lines", null,
                            grid, onGrid, motionEnabled = motionEnabled)
                        FlatDivider()
                        FlatToggle(Icons.Rounded.CenterFocusStrong, "Center guide", null,
                            level, onLevel, motionEnabled = motionEnabled)
                        FlatDivider()
                        FlatToggle(Icons.Rounded.VolumeUp, "Shutter sound", null,
                            shutterSound, onShutterSound, motionEnabled = motionEnabled)
                    }
                    SettingsGroup("Photo") {
                        FlatInfo(Icons.Rounded.AspectRatio, "Photo resolution", "Automatic")
                        FlatDivider()
                        FlatInfo(Icons.Rounded.Image, "Image format", "JPEG")
                        FlatDivider()
                        FlatInfo(Icons.Rounded.Tune, "HDR extension", "Disabled in v0.5")
                    }
                    SettingsGroup("Capture timer") {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            listOf(0, 3, 5, 10).forEach { seconds ->
                                FlatChip(
                                    label = if (seconds == 0) "OFF" else "${seconds}s",
                                    selected = timer == seconds, motionEnabled = motionEnabled,
                                    modifier = Modifier.weight(1f)
                                ) { onTimer(seconds) }
                            }
                        }
                    }
                }
                "Video" -> {
                    SettingsGroup("Recording") {
                        FlatInfo(Icons.Rounded.Videocam, "Resolution", videoQuality)
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            listOf("720p", "1080p", "4K").forEach { quality ->
                                FlatChip(quality, videoQuality == quality, motionEnabled,
                                    Modifier.weight(1f)) {
                                    if (quality != videoQuality) onVideoQuality(quality)
                                }
                            }
                        }
                        FlatDivider()
                        FlatToggle(Icons.Rounded.Stabilization, "Video stabilization",
                            "Applies where supported", videoStabilization,
                            onVideoStabilization, motionEnabled = motionEnabled)
                        FlatDivider()
                        FlatInfo(Icons.Rounded.Tune, "Frame rate", "Auto")
                        FlatDivider()
                        FlatInfo(Icons.Rounded.Movie, "Video codec", "Device managed")
                    }
                }
                "General" -> {
                    SettingsGroup("Controls") {
                        FlatToggle(Icons.Rounded.FlashOn, "Live torch",
                            if (torchAvailable) null else "Unavailable on this lens",
                            torch, onTorch, torchAvailable, motionEnabled)
                        FlatDivider()
                        FlatToggle(Icons.Rounded.AutoAwesome, "Motion & animation",
                            "Smooth, lightweight control feedback",
                            motionEnabled, onMotionEnabled, motionEnabled = motionEnabled)
                    }
                    SettingsGroup("Appearance") {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Color accents", fontSize = 13.sp, color = ink,
                                    fontWeight = FontWeight.Medium)
                                Text("Charcoal, white and Pocam orange",
                                    fontSize = 10.sp, color = secondaryInk)
                            }
                            listOf(ink, Color.White, PocamPalette.Orange).forEach { color ->
                                Box(Modifier.padding(start = 6.dp).size(24.dp)
                                    .background(color, CircleShape)
                                    .border(.7.dp, hairline, CircleShape))
                            }
                        }
                        FlatDivider()
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Header opacity", color = ink, fontSize = 13.sp)
                                Text("${(draftOpacity * 100).roundToInt()}%",
                                    color = secondaryInk, fontSize = 12.sp)
                            }
                            Slider(
                                value = draftOpacity,
                                onValueChange = { draftOpacity = it },
                                onValueChangeFinished = { onHeaderOpacity(draftOpacity) },
                                valueRange = .45f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = ink,
                                    activeTrackColor = ink,
                                    inactiveTrackColor = hairline
                                )
                            )
                        }
                    }
                    SettingsGroup("Exposure") {
                        if (exposureRange.first < exposureRange.last) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Exposure compensation", color = ink, fontSize = 13.sp)
                                    Text("${exposure} steps", color = secondaryInk, fontSize = 11.sp)
                                }
                                Slider(
                                    value = exposure.toFloat(),
                                    onValueChange = {
                                        onExposure(it.roundToInt().coerceIn(exposureRange))
                                    },
                                    valueRange = exposureRange.first.toFloat()..
                                        exposureRange.last.toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = ink, activeTrackColor = ink,
                                        inactiveTrackColor = hairline
                                    )
                                )
                            }
                        } else {
                            FlatInfo(Icons.Rounded.Tune, "Exposure", "Not supported")
                        }
                    }
                }
                else -> {
                    SettingsGroup("Pocam") {
                        FlatInfo(Icons.Rounded.Info, "App version", "v0.5 preview")
                        FlatDivider()
                        FlatInfo(Icons.Rounded.Lock, "Processing", "On-device only")
                        FlatDivider()
                        FlatInfo(Icons.Rounded.CloudOff, "Cloud account", "Not required")
                    }
                    SettingsGroup("Work in progress") {
                        FlatInfo(Icons.Rounded.NightsStay, "Night / Portrait",
                            "Special processing not yet implemented")
                        FlatDivider()
                        FlatInfo(Icons.Rounded.MicOff, "Video audio",
                            "Not yet implemented")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String, content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp))
        Column(
            Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            content = content
        )
    }
}

@Composable
private fun FlatDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 47.dp, end = 10.dp),
        color = hairline, thickness = .7.dp
    )
}

@Composable
private fun FlatInfo(icon: ImageVector, title: String, status: String) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 54.dp)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = ink, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(13.dp))
        Text(title, color = ink, fontSize = 13.sp,
            fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        Text(status, color = secondaryInk, fontSize = 11.sp, maxLines = 2)
    }
}

@Composable
private fun FlatToggle(
    icon: ImageVector, title: String, description: String?,
    value: Boolean, onChange: (Boolean) -> Unit,
    enabled: Boolean = true, motionEnabled: Boolean = true
) {
    val source = remember { MutableInteractionSource() }
    val thumb by animateFloatAsState(
        targetValue = if (value) 1f else 0f,
        animationSpec = if (motionEnabled) tween(140, easing = FastOutSlowInEasing)
            else snap(),
        label = "flat-toggle-thumb"
    )
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 54.dp)
            .toggleable(
                value = value,
                onValueChange = onChange,
                enabled = enabled, role = Role.Switch,
                interactionSource = source, indication = null
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (enabled) ink else secondaryInk,
            modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) ink else secondaryInk,
                fontWeight = FontWeight.Medium, fontSize = 13.sp)
            if (description != null) {
                Text(description, color = secondaryInk, fontSize = 10.sp)
            }
        }
        Box(
            Modifier.width(42.dp).height(24.dp)
                .background(if (value) ink else idleControl, CircleShape)
                .padding(3.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier.size(18.dp)
                    .graphicsLayer { translationX = 18.dp.toPx() * thumb }
                    .background(if (value) Color.White else Color.White, CircleShape)
            )
        }
    }
}

@Composable
private fun FlatChip(
    label: String, selected: Boolean,
    motionEnabled: Boolean, modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val fill by animateColorAsState(
        targetValue = if (selected) ink else idleControl,
        animationSpec = if (motionEnabled) tween(130) else snap(),
        label = "flat-chip-background"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else ink,
        animationSpec = if (motionEnabled) tween(130) else snap(),
        label = "flat-chip-text"
    )
    Box(
        modifier.height(40.dp)
            .background(fill, RoundedCornerShape(19.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = textColor, fontWeight = FontWeight.Medium, fontSize = 11.sp)
    }
}
