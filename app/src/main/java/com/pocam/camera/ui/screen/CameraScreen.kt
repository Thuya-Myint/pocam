package com.pocam.camera.ui.screen

import android.Manifest
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
        Text("pocam.", color = Color.White,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 24.dp))
        PerformanceHud(Modifier.align(Alignment.TopEnd).padding(20.dp))
        Text(modeSymbol(mode), color = Color.White,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 34.dp))
        Row(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1f, 2f, 4f).forEach { value ->
                Text("${value.toInt()}×", color = Color.White,
                    modifier = Modifier.background(
                        if (zoom == value) Color(0xFF66676B) else Color(0xCC25262A),
                        CircleShape).clickable { zoom = value; controller.setZoom(value) }
                        .padding(horizontal = 12.dp, vertical = 9.dp))
            }
        }
        if (mode == CameraMode.PRO) {
            Column(Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).heightIn(max = 520.dp)
                .background(Color(0xCC25262A), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .padding(12.dp)) {
                Text("EXPOSURE", color = Color.White)
                Text(if (exposureRange.first == exposureRange.last) "Unavailable"
                     else "${exposureIndex} steps", color = Color.White)
                Slider(
                    value = exposureIndex.toFloat(),
                    onValueChange = { value ->
                        exposureIndex = value.toInt().coerceIn(exposureRange.first, exposureRange.last)
                        controller.setExposure(exposureIndex)
                    },
                    valueRange = exposureRange.first.toFloat()..
                        (if (exposureRange.first == exposureRange.last) exposureRange.last + 1f
                         else exposureRange.last.toFloat()),
                    enabled = exposureRange.first != exposureRange.last,
                    modifier = Modifier.width(150.dp)
                )
                Text("Tap preview to focus", color = Color.LightGray)
                ManualControls(controller, cameraGeneration)
            }
        }
        Text("⟳", color = Color.White, modifier = Modifier.align(Alignment.BottomEnd)
            .padding(end = 22.dp, bottom = 124.dp)
            .background(Color(0xCC25262A), CircleShape)
            .clickable { previewView?.let { controller.flip(owner, it, { message -> error = message }) {
                cameraGeneration++
                exposureRange = controller.exposureRange()
                exposureIndex = 0
            }; zoom = 1f } }
            .padding(14.dp))
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)) {
            ShutterButton(mode = mode,
                onCapture = {
                    if (mode == CameraMode.PHOTO || mode == CameraMode.PRO) {
                        controller.takePhoto(
                            onSaved = { Toast.makeText(context, "Saved: $it", Toast.LENGTH_SHORT).show() },
                            onError = { error = it })
                    } else {
                        Toast.makeText(context, "${mode.name} coming soon", Toast.LENGTH_SHORT).show()
                    }
                },
                onModeSelected = {
                    mode = it
                    exposureRange = controller.exposureRange()
                    exposureIndex = 0
                    controller.setExposure(0)
                })
        }
        error?.let {
            Text(it, color = Color.White, modifier = Modifier.align(Alignment.Center)
                .background(Color(0xCC550000)).padding(12.dp))
        }
    }
}
