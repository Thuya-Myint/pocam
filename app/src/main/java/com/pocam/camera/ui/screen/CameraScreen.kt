package com.pocam.camera.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import kotlinx.coroutines.delay
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.pocam.camera.ui.components.ShutterButton
import com.pocam.camera.ui.components.PerformanceHud
import com.pocam.camera.ui.components.ManualControls

@Composable
fun CameraScreen() {
    val context = LocalContext.current
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
    var timerSeconds by remember { mutableStateOf(preferences.getInt("timer", 0)) }
    var torchEnabled by remember { mutableStateOf(false) }
    var crosshairEnabled by remember { mutableStateOf(preferences.getBoolean("crosshair", false)) }
    var performanceEnabled by remember { mutableStateOf(preferences.getBoolean("performance", false)) }
    var gridEnabled by remember { mutableStateOf(preferences.getBoolean("grid", false)) }
    var showSettings by remember { mutableStateOf(false) }
    var wheelOpen by remember { mutableStateOf(false) }
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
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this
                    controller.bind(owner, this, { error = it }) {
                        cameraGeneration++
                        exposureRange = controller.exposureRange()
                    }
                    setOnTouchListener { view, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            controller.focusAt(this, event.x, event.y)
                            focusPoint = Pair(event.x / width.coerceAtLeast(1),
                                event.y / height.coerceAtLeast(1))
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
            val x = with(density) { (maxWidth * point.first).toPx() }
            val y = with(density) { (maxHeight * point.second).toPx() }
            FocusReticle(point.first, point.second,
                Modifier.offset { IntOffset(x.toInt() - with(density) { 32.dp.roundToPx() },
                    y.toInt() - with(density) { 32.dp.roundToPx() }) })
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
                Text(if (mode == CameraMode.PRO) "PRO" else "PHOTO",
                    color = Color(0xFFFFD88C), fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                if (performanceEnabled) PerformanceHud()
            }
        }
        // Settings live in a vertical toolbar, not stacked over the branding.
        AnimatedVisibility(!wheelOpen,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 112.dp),
            enter = fadeIn(tween(160)), exit = fadeOut(tween(100))) {
            Column(Modifier.shadow(12.dp, RoundedCornerShape(24.dp))
                .background(Color(0xE91B1E24), RoundedCornerShape(24.dp))
                .padding(horizontal = 4.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { showSettings = !showSettings }) {
                    Icon(Icons.Rounded.Tune, contentDescription = "Viewfinder settings",
                        tint = Color.White)
                }
                if (mode == CameraMode.PRO) {
                    IconButton(onClick = { showProPanel = !showProPanel }) {
                        Icon(if (showProPanel) Icons.Rounded.Close else Icons.Rounded.Tune,
                            contentDescription = "Manual exposure controls", tint = Color.White)
                    }
                }
                Text(if (gridEnabled) "▦" else "⊞", color = Color.White,
                    fontSize = 24.sp, modifier = Modifier.clickable {
                        gridEnabled = !gridEnabled
                    }.padding(12.dp))
            }
        }
        AnimatedVisibility(showSettings, modifier = Modifier.align(Alignment.TopCenter)
            .padding(top = 156.dp, start = 12.dp, end = 12.dp),
            enter = fadeIn(tween(190)) + slideInVertically { -it / 4 },
            exit = fadeOut(tween(130)) + slideOutVertically { -it / 4 }) {
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
        AnimatedVisibility(!wheelOpen && !showProPanel,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 150.dp),
            enter = fadeIn(tween(180)), exit = fadeOut(tween(100))) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.background(Color(0xD921242C), RoundedCornerShape(24.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(CameraMode.PHOTO, CameraMode.PRO, CameraMode.VIDEO).forEach { target ->
                        Text(target.name, fontSize = 10.sp, letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (mode == target) Color.Black else Color.LightGray,
                            modifier = Modifier.background(
                                if (mode == target) Color(0xFFFFD88C) else Color.Transparent,
                                RoundedCornerShape(18.dp))
                                .clickable {
                                    if (mode == CameraMode.PRO && target != CameraMode.PRO) {
                                        controller.resetManual()
                                        showProPanel = false
                                    }
                                    mode = target
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp))
                    }
                }
                ZoomRail(zoom, onSelect = { value ->
                    zoom = value
                    controller.setZoom(value)
                })
            }
        }

        AnimatedVisibility(mode == CameraMode.PRO && showProPanel,
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 150.dp),
            enter = fadeIn(tween(220)) + slideInVertically { it / 3 },
            exit = fadeOut(tween(180)) + slideOutVertically { it / 3 }) {
            Column(Modifier.fillMaxWidth().heightIn(max = proMaxHeight.coerceAtMost(350.dp))
                .shadow(18.dp, RoundedCornerShape(26.dp))
                .background(Color(0xF51D2028), RoundedCornerShape(26.dp))
                .verticalScroll(rememberScrollState())
                .padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("MANUAL STUDIO  ·  PRO", color = Color(0xFFFFD88C),
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
                    modifier = Modifier.width(180.dp))
                ManualControls(controller, cameraGeneration)
            }
        }

        // Single integrated control dock. The shutter has an 88dp footprint,
        // so it no longer pushes the other controls into disconnected positions.
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
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
                ShutterButton(mode = mode,
                    onCapture = {
                        if (mode == CameraMode.PHOTO || mode == CameraMode.PRO) {
                            if (captureBusy) return@ShutterButton
                            captureBusy = true
                            scope.launch {
                                if (timerSeconds > 0) {
                                    for (remaining in timerSeconds downTo 1) {
                                        countdown = remaining
                                        delay(1000)
                                    }
                                }
                                countdown = null
                                captureFlash = true
                                controller.takePhoto(
                                onSaved = { uri ->
                                    captureBusy = false
                                    lastPhoto = uri
                                    Toast.makeText(context, "Saved to Pocam photos",
                                        Toast.LENGTH_SHORT).show()
                                },
                                onError = { captureBusy = false; error = it })
                            }
                        } else {
                            Toast.makeText(context, "${mode.name} coming soon",
                                Toast.LENGTH_SHORT).show()
                        }
                    },
                    onModeSelected = {
                        if (mode == CameraMode.PRO && it != CameraMode.PRO) {
                            controller.resetManual()
                            showProPanel = false
                        }
                        mode = it
                        exposureRange = controller.exposureRange()
                        exposureIndex = 0
                        controller.setExposure(0)
                    }, onWheelChange = { wheelOpen = it })

            }
            Box(Modifier.size(52.dp)
                .background(Color(0xFF323640), RoundedCornerShape(16.dp))
                .clickable {
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
            Box(Modifier.align(Alignment.Center)
                .background(Color(0xBB111319), CircleShape).size(110.dp),
                contentAlignment = Alignment.Center) {
                Text("$remaining", color = Color.White, fontSize = 60.sp,
                    fontWeight = FontWeight.Bold)
            }
        }
        error?.let {
            Text(it, color = Color.White, modifier = Modifier.align(Alignment.Center)
                .background(Color(0xCC550000), RoundedCornerShape(12.dp))
                .clickable { error = null }.padding(12.dp))
        }
    }
}
