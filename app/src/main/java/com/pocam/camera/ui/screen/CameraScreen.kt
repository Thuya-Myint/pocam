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

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val availableHeight = maxHeight
        val proMaxHeight = (availableHeight - 190.dp).coerceAtLeast(160.dp)
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

        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth()
            .background(Color(0xC7111319)).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("pocam.", color = Color.White, fontSize = 25.sp,
                    fontWeight = FontWeight.Bold)
                Text("POCKET CAMERA", color = Color(0xFFB0B5C0), fontSize = 9.sp,
                    letterSpacing = 2.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(mode.name, color = Color.White, fontSize = 11.sp,
                    modifier = Modifier.background(Color(0xFF343942), RoundedCornerShape(20.dp))
                        .padding(horizontal = 9.dp, vertical = 7.dp))
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = { showSettings = !showSettings }) {
                    Icon(Icons.Rounded.Tune, contentDescription = "Camera display settings",
                        tint = Color.White)
                }
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
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 210.dp),
            enter = fadeIn(tween(180)), exit = fadeOut(tween(100))) {
            ZoomRail(zoom, onSelect = { value ->
                zoom = value
                controller.setZoom(value)
            })
        }

        AnimatedVisibility(mode == CameraMode.PRO && showProPanel,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp, top = 72.dp, bottom = 110.dp),
            enter = fadeIn(tween(220)) + slideInVertically { it / 3 },
            exit = fadeOut(tween(180)) + slideOutVertically { it / 3 }) {
            Column(Modifier.heightIn(max = proMaxHeight).verticalScroll(rememberScrollState())
                .background(Color(0xEB17191E), RoundedCornerShape(18.dp))
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

        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .background(Color(0xB9101218))
            .padding(horizontal = 22.dp, vertical = 8.dp),
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
            Spacer(Modifier.width(100.dp))
        }
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 0.dp),
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
        /* Camera flip sits above the shutter gesture region. */
        Row(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 28.dp)) {
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
        if (mode == CameraMode.PRO) {
            IconButton(onClick = { showProPanel = !showProPanel },
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 132.dp)
                    .background(Color(0xD923252B), CircleShape)) {
                Icon(if (showProPanel) Icons.Rounded.Close else Icons.Rounded.Tune,
                    contentDescription = "Toggle Pro controls", tint = Color.White)
            }
        }
        Text(if (wheelOpen) "DRAG TO CHOOSE MODE" else "HOLD SHUTTER · CHANGE MODE", color = Color(0xFFBFC2C9),
            fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = 174.dp))
        error?.let {
            Text(it, color = Color.White, modifier = Modifier.align(Alignment.Center)
                .background(Color(0xCC550000), RoundedCornerShape(12.dp))
                .clickable { error = null }.padding(12.dp))
        }
    }
}
