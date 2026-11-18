package com.pocam.camera.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlashOff
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
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
    var flashEnabled by remember { mutableStateOf(false) }
    var lastPhoto by remember { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permitted = it
        if (!it) error = "Camera permission required"
    }
    LaunchedEffect(Unit) {
        if (!permitted) permissionRequest.launch(Manifest.permission.CAMERA)
    }
    DisposableEffect(controller) { onDispose { controller.release() } }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
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
                            view.performClick()
                        }
                        true
                    }
                }
            }, modifier = Modifier.fillMaxSize())
        }
        // Translucent top bar, independent of the live camera preview.
        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth()
            .background(Color(0x990E1015)).padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("pocam.", color = Color.White, fontSize = 25.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
                Text("POCKET CAMERA", color = Color(0xFFADB2BC), fontSize = 9.sp,
                    letterSpacing = 2.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(mode.name, color = Color.White, fontSize = 11.sp,
                    modifier = Modifier.background(Color(0xFF343942), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp))
                Spacer(Modifier.width(8.dp))
                PerformanceHud()
            }
        }

        // Zoom controls stay above the shutter wheel and do not obstruct the preview.
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 216.dp)
            .background(Color(0xB91A1C22), RoundedCornerShape(32.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(1f, 2f, 4f).forEach { value ->
                val active = zoom == value
                Text("${value.toInt()}×", color = if (active) Color.Black else Color.White,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.background(
                        if (active) Color.White else Color.Transparent, CircleShape)
                        .clickable { zoom = value; controller.setZoom(value) }
                        .padding(horizontal = 14.dp, vertical = 9.dp))
            }
        }

        AnimatedVisibility(mode == CameraMode.PRO && showProPanel,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            enter = fadeIn(tween(220)) + slideInVertically { it / 3 },
            exit = fadeOut(tween(180)) + slideOutVertically { it / 3 }) {
            Column(Modifier.heightIn(max = 480.dp)
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
            Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                ShutterButton(mode = mode,
                    onCapture = {
                        if (mode == CameraMode.PHOTO || mode == CameraMode.PRO) {
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
                    })
            }
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
        Text("Hold shutter to change mode", color = Color(0xFFBFC2C9),
            fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = 112.dp))
        error?.let {
            Text(it, color = Color.White, modifier = Modifier.align(Alignment.Center)
                .background(Color(0xCC550000)).padding(12.dp))
        }
    }
}
