package com.pocam.camera.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import com.pocam.camera.ui.components.CameraGrid
import com.pocam.camera.ui.components.ZoomRail
import com.pocam.camera.ui.components.CaptureFlash
import com.pocam.camera.ui.components.FocusReticle
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.widget.Toast
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.dynamicDarkColorScheme
import android.os.Build
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.pocam.camera.ui.components.modeSymbol
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.pocam.camera.ui.components.ViewfinderSettings
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pocam.camera.camera.CameraController
import com.pocam.camera.domain.model.CameraMode
import com.pocam.camera.ui.components.RadialModeWheel
import com.pocam.camera.ui.components.ShutterButton
import com.pocam.camera.ui.components.PerformanceHud
import com.pocam.camera.ui.components.ManualControls

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val accent = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicDarkColorScheme(context).primary
        } else Color(0xFFFFD88C)
    }
    val owner = LocalLifecycleOwner.current
    val controller = remember(context) { CameraController(context) }
    var permitted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED)
    }
    var mode by remember { mutableStateOf(CameraMode.PHOTO) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraGeneration by remember { mutableStateOf(0) }
    var exposureIndex by remember { mutableStateOf(0) }
    var exposureRange by remember { mutableStateOf(0..0) }
    var zoom by remember { mutableStateOf(1f) }
    var showProPanel by remember { mutableStateOf(false) }
    val preferences = remember(context) { context.getSharedPreferences("pocam_ui", 0) }
    val scope = rememberCoroutineScope()
    var captureBusy by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf<Int?>(null) }
    var countdownJob by remember { mutableStateOf<Job?>(null) }
    var timerSeconds by remember { mutableStateOf(preferences.getInt("timer", 0)) }
    var torchEnabled by remember { mutableStateOf(false) }
    var crosshairEnabled by remember { mutableStateOf(preferences.getBoolean("crosshair", false)) }
    var performanceEnabled by remember { mutableStateOf(preferences.getBoolean("performance", false)) }
    var gridEnabled by remember { mutableStateOf(preferences.getBoolean("grid", false)) }
    var showSettings by remember { mutableStateOf(false) }
    var wheelOpen by remember { mutableStateOf(false) }
    var wheelHover by remember { mutableStateOf(CameraMode.PHOTO) }
    var captureFlash by remember { mutableStateOf(false) }
    var focusPoint by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var focusSequence by remember { mutableStateOf(0) }
    var lastPhoto by remember { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permitted = it
        if (!it) error = "Camera permission required"
    }
    LaunchedEffect(Unit) {
        if (!permitted) permissionRequest.launch(Manifest.permission.CAMERA)
    }
    LaunchedEffect(focusSequence) {
        if (focusSequence > 0) {
            delay(1100)
            focusPoint = null
        }
    }
    LaunchedEffect(captureFlash) {
        if (captureFlash) {
            delay(160)
            captureFlash = false
        }
    }
    DisposableEffect(controller) { onDispose { controller.release() } }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black).navigationBarsPadding()) {
        val availableHeight = maxHeight
        val proMaxHeight = (availableHeight - 260.dp).coerceAtLeast(120.dp)
        if (permitted) {
            AndroidView(factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this
                    controller.bind(owner, this, { error = it }) {
                        cameraGeneration++
                        exposureRange = controller.exposureRange()
                    }
                    setOnTouchListener { view, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            controller.focusAt(this, event.x, event.y)
                            focusPoint = Pair(
                                (event.x / width.coerceAtLeast(1)).coerceIn(0f, 1f),
                                (event.y / height.coerceAtLeast(1)).coerceIn(0f, 1f))
                            focusSequence++
                            view.performClick()
                        }
                        true
                    }
                }
            }, modifier = Modifier.fillMaxSize())
        }
        if (crosshairEnabled) {
            Box(Modifier.align(Alignment.Center).size(20.dp),
                contentAlignment = Alignment.Center) {
                Text("+", color = Color.White.copy(alpha = .8f), fontSize = 24.sp)
            }
        }
        if (gridEnabled) {
            CameraGrid(Modifier.fillMaxSize().padding(top = 80.dp, bottom = 200.dp))
        }
        focusPoint?.let { point ->
            val density = LocalDensity.current
            val reticleSize = with(density) { 64.dp.roundToPx() }
            val viewWidth = with(density) { maxWidth.toPx() }.toInt()
            val viewHeight = with(density) { maxHeight.toPx() }.toInt()
            val x = viewWidth * point.first
            val y = viewHeight * point.second
            FocusReticle(point.first, point.second,
                Modifier.offset {
                    IntOffset(
                        (x.toInt() - reticleSize / 2).coerceIn(0,
                            (viewWidth - reticleSize).coerceAtLeast(0)),
                        (y.toInt() - reticleSize / 2).coerceIn(0,
                            (viewHeight - reticleSize).coerceAtLeast(0)))
                })
        }
        CaptureFlash(captureFlash, Modifier.fillMaxSize())

        // Dark gradient keeps the viewfinder immersive without opaque header bars.
        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xED0C0E13), Color(0x9D101218))))
            .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("pocam.", color = Color.White, fontSize = 28.sp,
                    fontWeight = FontWeight.Bold)
                Text("POCKET CAMERA", color = Color(0xFFADB2BC), fontSize = 9.sp,
                    letterSpacing = 2.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(mode.name,
                    color = accent, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                if (performanceEnabled) PerformanceHud()
            }
        }
        // Settings live in a vertical toolbar, not stacked over the branding.
        if (!wheelOpen) {
            Box(Modifier.align(Alignment.TopCenter).padding(top = 98.dp).zIndex(12f)) {
            Row(Modifier.shadow(12.dp, RoundedCornerShape(26.dp))
                .background(Color(0xE91B1E24), RoundedCornerShape(26.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = { showSettings = !showSettings }) {
                    Icon(Icons.Rounded.Tune, contentDescription = "Camera settings",
                        tint = if (showSettings) accent else Color.White)
                }
                IconButton(onClick = {
                    gridEnabled = !gridEnabled
                    preferences.edit().putBoolean("grid", gridEnabled).apply()
                }) {
                    Icon(Icons.Rounded.GridOn, contentDescription = "Toggle composition grid",
                        tint = if (gridEnabled) accent else Color.White)
                }
                IconButton(onClick = {
                    if (controller.hasTorch()) {
                        torchEnabled = !torchEnabled
                        controller.setTorch(torchEnabled)
                    }
                }) {
                    Icon(if (torchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                        contentDescription = "Toggle live torch",
                        tint = if (torchEnabled) accent else Color.White)
                }
                IconButton(onClick = {
                    timerSeconds = when (timerSeconds) { 0 -> 3; 3 -> 5; 5 -> 10; else -> 0 }
                    preferences.edit().putInt("timer", timerSeconds).apply()
                }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Timer, contentDescription = "Cycle capture timer",
                            tint = if (timerSeconds > 0) accent else Color.White)
                        if (timerSeconds > 0) Text("${timerSeconds}s",
                            color = accent, fontSize = 9.sp)
                    }
                }
                if (mode == CameraMode.PRO) {
                    IconButton(onClick = { showProPanel = !showProPanel }) {
                        Icon(if (showProPanel) Icons.Rounded.Close else Icons.Rounded.Tune,
                            contentDescription = "Pro manual studio", tint = accent)
                    }
                }
            }
            }
        }
        if (wheelOpen) {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp).zIndex(20f)) {
                RadialModeWheel(wheelHover)
            }
        }
        if (showSettings && !wheelOpen) {
            Box(Modifier.align(Alignment.TopCenter).zIndex(15f)
                .padding(top = 164.dp, start = 12.dp, end = 12.dp)
                .heightIn(max = (availableHeight - 310.dp).coerceAtLeast(180.dp))
                .verticalScroll(rememberScrollState())) {
            ViewfinderSettings(
                grid = gridEnabled,
                onGrid = { gridEnabled = it; preferences.edit().putBoolean("grid", it).apply() },
                level = crosshairEnabled,
                onLevel = {
                    crosshairEnabled = it
                    preferences.edit().putBoolean("crosshair", it).apply()
                },
                torch = torchEnabled,
                torchAvailable = controller.hasTorch(),
                onTorch = { torchEnabled = it; controller.setTorch(it) },
                timer = timerSeconds,
                onTimer = {
                    timerSeconds = it
                    preferences.edit().putInt("timer", it).apply()
                },
                performance = performanceEnabled,
                onPerformance = {
                    performanceEnabled = it
                    preferences.edit().putBoolean("performance", it).apply()
                },
                onClose = { showSettings = false }
            )
            }
        }
        // Shooting controls are always reachable and drawn above the preview.
        if (!wheelOpen && !showProPanel && !showSettings) {
            Box(Modifier.align(Alignment.BottomCenter).zIndex(12f)
                .padding(bottom = 144.dp, start = 8.dp, end = 8.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .background(Color(0xE91B1F27), RoundedCornerShape(24.dp))
                    .padding(horizontal = 7.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    listOf(CameraMode.PORTRAIT, CameraMode.PHOTO, CameraMode.PRO,
                        CameraMode.VIDEO, CameraMode.NIGHT, CameraMode.MORE).forEach { target ->
                        val active = mode == target
                        Text(target.name.take(5), fontSize = 10.sp, letterSpacing = .3.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color(0xFF15171C) else Color.White,
                            modifier = Modifier
                                .background(if (active) accent else Color.Transparent,
                                    RoundedCornerShape(16.dp))
                                .clickable(enabled = !captureBusy) {
                                    if (mode == CameraMode.PRO && target != CameraMode.PRO) {
                                        controller.resetManual()
                                        showProPanel = false
                                    }
                                    mode = target
                                    showProPanel = target == CameraMode.PRO
                                    showSettings = false
                                }
                                .padding(horizontal = 9.dp, vertical = 9.dp))
                    }
                }
                ZoomRail(zoom, onSelect = { value ->
                    zoom = value
                    controller.setZoom(value)
                })
                if (mode != CameraMode.PHOTO && mode != CameraMode.PRO) {
                    Text("${mode.name} capture is not available yet",
                        fontSize = 10.sp, color = Color.White,
                        modifier = Modifier.background(Color(0xD51B1E25),
                            RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
            }
        }
        AnimatedVisibility(mode == CameraMode.PRO && showProPanel && !wheelOpen && !showSettings,
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(14f).padding(start = 16.dp, end = 16.dp, bottom = 144.dp),
            enter = fadeIn(tween(220)) + slideInVertically { it / 3 },
            exit = fadeOut(tween(180)) + slideOutVertically { it / 3 }) {
            Column(Modifier.fillMaxWidth().heightIn(max = proMaxHeight.coerceAtMost(350.dp))
                .shadow(18.dp, RoundedCornerShape(26.dp))
                .background(Color(0xF51D2028), RoundedCornerShape(26.dp))
                .verticalScroll(rememberScrollState())
                .padding(12.dp)) {
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("MANUAL STUDIO  ·  PRO", color = accent,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 12.sp)
                    IconButton(onClick = { showProPanel = false }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close Pro controls",
                            tint = Color.White)
                    }
                }
                Text("EV ${exposureIndex * controller.exposureStepEv()}",
                    color = Color.White, fontSize = 12.sp)
                Slider(value = exposureIndex.toFloat(),
                    onValueChange = {
                        exposureIndex = it.toInt().coerceIn(exposureRange.first, exposureRange.last)
                        controller.setExposure(exposureIndex)
                    },
                    valueRange = exposureRange.first.toFloat()..
                        (if (exposureRange.first == exposureRange.last) 1f
                        else exposureRange.last.toFloat()),
                    enabled = exposureRange.first != exposureRange.last,
                    modifier = Modifier.fillMaxWidth())
                ManualControls(controller, cameraGeneration)
            }
        }

        // Single integrated control dock. The shutter has an 88dp footprint,
        // so it no longer pushes the other controls into disconnected positions.
        Row(Modifier.align(Alignment.BottomCenter).zIndex(10f).fillMaxWidth()
            .background(Brush.verticalGradient(
                listOf(Color(0xF222252D), Color(0xFF101217))),
                RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp)
                .background(Color(0xFF323640), RoundedCornerShape(16.dp))
                .clickable(enabled = lastPhoto != null) {
                    lastPhoto?.let { uri ->
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "image/jpeg")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            })
                        } catch (_: Exception) { error = "No photo viewer available" }
                    }
                }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = "View last photo",
                    tint = if (lastPhoto == null) Color.LightGray else Color.White)
            }
            Box(contentAlignment = Alignment.Center) {
                ShutterButton(mode = mode, busy = captureBusy,
                    onCapture = {
                        if (captureBusy) return@ShutterButton
                        if (mode == CameraMode.PHOTO || mode == CameraMode.PRO) {
                            captureBusy = true
                            countdownJob = scope.launch {
                                if (timerSeconds > 0) {
                                    for (remaining in timerSeconds downTo 1) {
                                        countdown = remaining
                                        delay(1000)
                                    }
                                }
                                countdown = null
                                countdownJob = null
                                controller.takePhoto(
                                onSaved = { uri ->
                                    captureFlash = true
                                    captureBusy = false
                                    lastPhoto = uri
                                    Toast.makeText(context, "Saved to Pocam photos",
                                        Toast.LENGTH_SHORT).show()
                                },
                                onError = {
                                    countdown = null
                                    countdownJob = null
                                    captureBusy = false
                                    error = it
                                })
                            }
                        } else {
                            Toast.makeText(context, "${mode.name} coming soon",
                                Toast.LENGTH_SHORT).show()
                        }
                    },
                    onModeSelected = {
                        if (captureBusy) return@ShutterButton
                        if (mode == CameraMode.PRO && it != CameraMode.PRO) {
                            controller.resetManual()
                            showProPanel = false
                        }
                        mode = it
                        showProPanel = it == CameraMode.PRO
                        showSettings = false
                        exposureRange = controller.exposureRange()
                        exposureIndex = 0
                        controller.setExposure(0)
                    }, onWheelChange = { wheelOpen = it }, onHoverMode = { wheelHover = it })
                if (captureBusy) {
                    Text(if (countdown != null) "TIMER" else "SAVING",
                        color = accent, fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.TopCenter)
                            .offset(y = (-14).dp))
                }
            }
            Box(Modifier.size(52.dp)
                .background(Color(0xFF323640), RoundedCornerShape(16.dp))
                .clickable(enabled = !captureBusy) {
                    showSettings = false
                    showProPanel = false
                    previewView?.let { view ->
                        controller.flip(owner, view, { error = it }) {
                            cameraGeneration++
                            exposureRange = controller.exposureRange()
                            exposureIndex = 0
                        }
                        zoom = 1f
                        torchEnabled = false
                    }
                }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Cameraswitch, contentDescription = "Switch camera",
                    tint = Color.White)
            }
        }
        countdown?.let { remaining ->
            Box(
                Modifier.align(Alignment.Center)
                    .size(136.dp)
                    .background(Color(0xDD111319), CircleShape)
                    .clickable {
                        countdownJob?.cancel()
                        countdownJob = null
                        countdown = null
                        captureBusy = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$remaining", color = Color.White, fontSize = 58.sp,
                        fontWeight = FontWeight.Bold)
                    Text("TAP TO CANCEL", color = Color.White, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
        error?.let {
            Text(it, color = Color.White, modifier = Modifier.align(Alignment.Center)
                .background(Color(0xCC550000), RoundedCornerShape(12.dp))
                .clickable { error = null }.padding(12.dp))
        }
    }
}
