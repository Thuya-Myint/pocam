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
    var gridEnabled by remember { mutableStateOf(false) }
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
            PerformanceHud()
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
        AnimatedVisibility(showSettings, modifier = Modifier.align(Alignment.TopEnd)
            .padding(top = 74.dp, end = 12.dp),
            enter = fadeIn(tween(170)) + slideInVertically { -it / 3 },
            exit = fadeOut(tween(130)) + slideOutVertically { -it / 3 }) {
            Column(Modifier.background(Color(0xF121232A), RoundedCornerShape(16.dp))
                .padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("VIEWFINDER", color = Color.White, fontWeight = FontWeight.Bold)
                Text(if (gridEnabled) "Grid  ·  ON" else "Grid  ·  OFF",
                    color = Color.White, modifier = Modifier.clickable {
                        gridEnabled = !gridEnabled
                    }.padding(vertical = 8.dp))
                Text("Performance overlay", color = Color.LightGray, fontSize = 11.sp)
                PerformanceHud()
                Text("Close", color = Color(0xFFFFD36A),
                    modifier = Modifier.clickable { showSettings = false }.padding(6.dp))
            }
        }
        AnimatedVisibility(!wheelOpen && !showProPanel,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 132.dp),
            enter = fadeIn(tween(180)), exit = fadeOut(tween(100))) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(mode.name, color = Color.White, fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.background(Color(0xCB202329),
                        RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 7.dp))
                ZoomRail(zoom, onSelect = { value ->
                    zoom = value
                    controller.setZoom(value)
                })
            }
        }

        AnimatedVisibility(mode == CameraMode.PRO && showProPanel,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp, top = 96.dp, bottom = 140.dp),
            enter = fadeIn(tween(220)) + slideInVertically { it / 3 },
            exit = fadeOut(tween(180)) + slideOutVertically { it / 3 }) {
            Column(Modifier.heightIn(max = proMaxHeight)
                .background(Color(0xEB17191E), RoundedCornerShape(18.dp))
                .verticalScroll(rememberScrollState())
                .padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("PRO", color = Color.White, fontWeight = FontWeight.Bold)
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

        // The 264dp gesture area is intentionally centered in an overlay.
        // The gallery and flip buttons align with its central shutter, not its outer bounds.
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xF321242C), Color(0xFF101217))),
                RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .padding(horizontal = 26.dp, vertical = 30.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                lastPhoto?.let { uri ->
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "image/jpeg")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        })
                    } catch (_: Exception) { error = "No photo viewer available" }
                }
            }, enabled = lastPhoto != null) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = "View last photo",
                    tint = if (lastPhoto == null) Color.Gray else Color.White)
            }
            Spacer(Modifier.width(112.dp))
            IconButton(onClick = {
                previewView?.let { view ->
                    controller.flip(owner, view, { error = it }) {
                        cameraGeneration++
                        exposureRange = controller.exposureRange()
                        exposureIndex = 0
                    }
                    zoom = 1f
                }
            }) {
                Icon(Icons.Rounded.Cameraswitch, contentDescription = "Switch camera",
                    tint = Color.White)
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).offset(y = 72.dp),
            contentAlignment = Alignment.Center) {
                ShutterButton(mode = mode,
                    onCapture = {
                        if (mode == CameraMode.PHOTO || mode == CameraMode.PRO) {
                            captureFlash = true
                            controller.takePhoto(
                                onSaved = { uri ->
                                    lastPhoto = uri
                                    Toast.makeText(context, "Saved to Pocam photos",
                                        Toast.LENGTH_SHORT).show()
                                },
                                onError = { error = it })
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
        error?.let {
            Text(it, color = Color.White, modifier = Modifier.align(Alignment.Center)
                .background(Color(0xCC550000), RoundedCornerShape(12.dp))
                .clickable { error = null }.padding(12.dp))
        }
    }
}
