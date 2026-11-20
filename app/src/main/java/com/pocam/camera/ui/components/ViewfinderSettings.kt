package com.pocam.camera.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Settings design fidelity:
 * - fixed, flat compact header and four-tab strip
 * - independent scrolling content (header never scrolls away)
 * - dense grouped rows, fine dividers, small orange switch and selected pills
 * - no camera rebind or full-screen animations during interaction
 *
 * All controls reflect currently implemented device capabilities. The status
 * labels on device-managed properties are informational, not fake buttons.
 */
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
    val tabs = remember { listOf("Capture", "Camera", "Video", "Appearance") }
    var tab by remember { mutableStateOf("Capture") }
    var draftHeaderOpacity by remember { mutableFloatStateOf(headerOpacity) }
    val scroll = rememberScrollState()
    val contentAlpha = remember { Animatable(1f) }
    val motionState by rememberUpdatedState(motionEnabled)
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(tab) {
        scroll.scrollTo(0)
        if (motionState) {
            contentAlpha.snapTo(.92f)
            contentAlpha.animateTo(1f, tween(110, easing = FastOutSlowInEasing))
        } else contentAlpha.snapTo(1f)
    }

    Column(modifier.fillMaxSize().background(PocamPalette.Carbon)) {
        // Flat reference header with compact circular close/setting icons.
        Row(
            Modifier.fillMaxWidth()
                .background(Color(0xF115171C))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(35.dp)
                    .background(PocamPalette.Graphite, CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Close, "Close Settings",
                    tint = Color.White, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Settings", color = Color.White, fontSize = 17.sp,
                    fontWeight = FontWeight.Bold)
                Text("Pocam Camera", color = PocamPalette.Silver, fontSize = 9.sp)
            }
            Box(Modifier.size(33.dp).background(PocamPalette.Graphite, CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Tune, null, tint = PocamPalette.Silver,
                    modifier = Modifier.size(17.dp))
            }
        }
        Row(
            Modifier.fillMaxWidth()
                .background(PocamPalette.Carbon)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEach { section ->
                val selected = section == tab
                val fill by animateColorAsState(
                    if (selected) PocamPalette.Orange else PocamPalette.Graphite,
                    if (motionEnabled) tween(120) else snap(),
                    label = "settings-tab-background"
                )
                val foreground by animateColorAsState(
                    if (selected) PocamPalette.Black else Color.White,
                    if (motionEnabled) tween(120) else snap(),
                    label = "settings-tab-text"
                )
                Box(
                    Modifier.weight(1f).height(32.dp)
                        .background(fill, CircleShape)
                        .clickable {
                            if (tab != section) {
                                tab = section
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(section, color = foreground, fontSize = 10.sp,
                        maxLines = 1, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = .07f))
        Column(
            Modifier.weight(1f)
                .verticalScroll(scroll)
                .graphicsLayer { alpha = contentAlpha.value }
                .padding(start = 12.dp, end = 12.dp, top = 11.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            when (tab) {
                "Capture" -> {
                    SettingGroup("Capture", Icons.Rounded.PhotoLibrary) {
                        SettingInfo(Icons.Rounded.PhotoLibrary, "Photo resolution",
                            "Highest supported quality", "AUTO")
                        SettingDivider()
                        SettingInfo(Icons.Rounded.Image, "Image format",
                            "Maximum compatibility", "JPEG")
                        SettingDivider()
                        SettingInfo(Icons.Rounded.Tune, "HDR",
                            "OEM extension currently disabled", "OFF")
                        SettingDivider()
                        SettingToggle(Icons.Rounded.GridOn, "Grid lines",
                            "Rule of thirds", grid, onGrid, motionEnabled = motionEnabled)
                        SettingDivider()
                        SettingToggle(Icons.Rounded.CenterFocusStrong, "Level guide",
                            "Center composition marker", level, onLevel,
                            motionEnabled = motionEnabled)
                    }
                    SettingGroup("Capture timer", Icons.Rounded.Timer) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0, 3, 5, 10).forEach { seconds ->
                                SettingPill(
                                    if (seconds == 0) "OFF" else "${seconds}s",
                                    timer == seconds,
                                    modifier = Modifier.weight(1f),
                                    motionEnabled = motionEnabled
                                ) { onTimer(seconds) }
                            }
                        }
                    }
                }
                "Camera" -> {
                    SettingGroup("Camera", Icons.Rounded.Tune) {
                        SettingToggle(Icons.Rounded.FlashOn, "Live torch",
                            if (torchAvailable) "Illuminate the preview" else "Not supported on this lens",
                            torch, onTorch, enabled = torchAvailable,
                            motionEnabled = motionEnabled)
                        SettingDivider()
                        SettingToggle(Icons.Rounded.VolumeUp, "Shutter sound",
                            "Play capture feedback", shutterSound, onShutterSound,
                            motionEnabled = motionEnabled)
                        SettingDivider()
                        SettingInfo(Icons.Rounded.CenterFocusStrong, "Touch focus",
                            "Tap to focus · hold to lock", "ON")
                        SettingDivider()
                        SettingInfo(Icons.Rounded.Tune, "Focus tracking",
                            "Not available in this build", "—")
                    }
                    SettingGroup("Exposure compensation", Icons.Rounded.Tune) {
                        if (exposureRange.first >= exposureRange.last) {
                            Text("Not available on this camera",
                                color = PocamPalette.Silver, fontSize = 11.sp,
                                modifier = Modifier.padding(12.dp))
                        } else {
                            Column(Modifier.fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 7.dp)) {
                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Exposure", fontSize = 12.sp, color = Color.White)
                                    Text("${exposure} steps", fontSize = 11.sp,
                                        color = PocamPalette.Orange)
                                }
                                StraightRuler(exposure.toFloat(),
                                    exposureRange.first.toFloat()..exposureRange.last.toFloat(),
                                    { v -> onExposure(v.roundToInt().coerceIn(exposureRange)) },
                                    "${exposure} EV", showLabel = false, compact = true)
                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End) {
                                    SettingPill("RESET", exposure == 0,
                                        motionEnabled = motionEnabled
                                    ) { onExposure(0.coerceIn(exposureRange)) }
                                }
                            }
                        }
                    }
                }
                "Video" -> {
                    SettingGroup("Video", Icons.Rounded.Videocam) {
                        SettingInfo(Icons.Rounded.Videocam, "Video resolution",
                            "Requested quality · hardware may fall back", videoQuality)
                        Row(Modifier.fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp, bottom = 12.dp, top = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("720p", "1080p", "4K").forEach { quality ->
                                SettingPill(quality, quality == videoQuality,
                                    modifier = Modifier.weight(1f),
                                    motionEnabled = motionEnabled
                                ) {
                                    if (quality != videoQuality) onVideoQuality(quality)
                                }
                            }
                        }
                        SettingDivider()
                        SettingToggle(Icons.Rounded.Videocam, "Video stabilization",
                            "Request smoother video where supported",
                            videoStabilization, onVideoStabilization,
                            motionEnabled = motionEnabled)
                        SettingDivider()
                        SettingInfo(Icons.Rounded.Tune, "Frame rate",
                            "Controlled by camera hardware", "AUTO")
                        SettingDivider()
                        SettingInfo(Icons.Rounded.Tune, "Video codec",
                            "Managed by Android recorder", "DEVICE")
                    }
                }
                else -> {
                    SettingGroup("Appearance", Icons.Rounded.Palette) {
                        Row(Modifier.fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Studio palette", color = Color.White,
                                    fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("Fixed orange · graphite · silver",
                                    color = PocamPalette.Silver, fontSize = 10.sp)
                            }
                            listOf(
                                PocamPalette.Graphite, PocamPalette.Silver,
                                PocamPalette.Black, PocamPalette.Orange
                            ).forEachIndexed { i, color ->
                                Box(
                                    Modifier.padding(start = 5.dp).size(22.dp)
                                        .background(color, CircleShape)
                                        .border(
                                            if (i == 3) 1.7.dp else .6.dp,
                                            if (i == 3) PocamPalette.OrangeSoft
                                            else PocamPalette.Steel, CircleShape
                                        )
                                )
                            }
                        }
                        SettingDivider()
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Header transparency", color = Color.White,
                                    fontSize = 12.sp)
                                Text("${(draftHeaderOpacity * 100).roundToInt()}%",
                                    color = PocamPalette.Orange, fontSize = 11.sp)
                            }
                            Slider(
                                value = draftHeaderOpacity,
                                onValueChange = { draftHeaderOpacity = it },
                                onValueChangeFinished = {
                                    onHeaderOpacity(draftHeaderOpacity)
                                },
                                valueRange = .45f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = PocamPalette.Orange,
                                    activeTrackColor = PocamPalette.Orange,
                                    inactiveTrackColor = PocamPalette.Steel
                                )
                            )
                        }
                        SettingDivider()
                        SettingInfo(Icons.Rounded.GridOn, "Control layout",
                            "Curved zoom and curved mode selector", "FIXED")
                        SettingDivider()
                        SettingToggle(Icons.Rounded.AutoAwesome, "Pocam motion",
                            "Lightweight animations for camera controls",
                            motionEnabled, onMotionEnabled,
                            motionEnabled = motionEnabled)
                    }
                    SettingGroup("Advanced tools", Icons.Rounded.Tune) {
                        SettingInfo(Icons.Rounded.Tune, "RAW / Histogram",
                            "Planned · not yet available", "SOON")
                        SettingDivider()
                        SettingInfo(Icons.Rounded.Tune, "Focus peaking / Zebra",
                            "Planned · not yet available", "SOON")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingGroup(
    title: String, icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.padding(start = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = PocamPalette.Orange, modifier = Modifier.size(15.dp))
            Text(title, color = Color.White, fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold)
        }
        Column(
            Modifier.fillMaxWidth()
                .background(Color(0xDE1D2027), RoundedCornerShape(13.dp))
                .border(.7.dp, PocamPalette.Steel.copy(alpha = .30f),
                    RoundedCornerShape(13.dp))
                .padding(vertical = 2.dp),
            content = content
        )
    }
}

@Composable
private fun SettingInfo(
    icon: ImageVector, title: String, subtitle: String, status: String
) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 47.dp)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = PocamPalette.Silver, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 12.sp,
                fontWeight = FontWeight.Medium)
            Text(subtitle, color = PocamPalette.Silver, fontSize = 9.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(status, color = PocamPalette.Silver,
            fontSize = 9.sp, fontWeight = FontWeight.Medium,
            maxLines = 1)
    }
}

/** Full-row accessible switch with a compositor-only thumb glide. */
@Composable
private fun SettingToggle(
    icon: ImageVector, title: String, subtitle: String,
    value: Boolean, onChange: (Boolean) -> Unit,
    enabled: Boolean = true, motionEnabled: Boolean = true
) {
    val source = remember { MutableInteractionSource() }
    val thumb by animateFloatAsState(
        targetValue = if (value) 1f else 0f,
        animationSpec = if (motionEnabled) tween(145,
            easing = FastOutSlowInEasing) else snap(),
        label = "setting-switch-thumb"
    )
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 47.dp)
            .toggleable(
                value = value,
                onValueChange = onChange,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = source,
                indication = null
            )
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null,
            tint = if (enabled) PocamPalette.Silver else PocamPalette.Steel,
            modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) Color.White else PocamPalette.Steel,
                fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = PocamPalette.Silver, fontSize = 9.sp)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.width(43.dp).height(25.dp)
                .background(
                    if (value) PocamPalette.Orange else PocamPalette.Graphite,
                    CircleShape
                )
                .border(1.dp,
                    if (value) PocamPalette.Orange else PocamPalette.Steel,
                    CircleShape)
                .padding(3.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier.size(19.dp)
                    .graphicsLayer { translationX = 18.dp.toPx() * thumb }
                    .background(if (enabled) Color.White
                        else PocamPalette.Silver, CircleShape)
            )
        }
    }
}

@Composable
private fun SettingPill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    motionEnabled: Boolean,
    onClick: () -> Unit
) {
    val fill by animateColorAsState(
        targetValue = if (selected) PocamPalette.Orange else PocamPalette.Graphite,
        animationSpec = if (motionEnabled) tween(125) else snap(),
        label = "setting-pill-background"
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) PocamPalette.Black else Color.White,
        animationSpec = if (motionEnabled) tween(125) else snap(),
        label = "setting-pill-text"
    )
    Box(
        modifier.height(34.dp)
            .background(fill, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = foreground, fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 11.dp),
        color = PocamPalette.Steel.copy(alpha = .19f),
        thickness = .6.dp
    )
}
