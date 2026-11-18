package com.pocam.camera.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pocam.camera.camera.CameraController
import com.pocam.camera.domain.model.CameraMode
import com.pocam.camera.ui.components.ShutterButton

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
                PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
            }, modifier = Modifier.fillMaxSize(), update = { view ->
                controller.bind(owner, view) { error = it }
            })
        }
        Text("pocam.", color = Color.White,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 24.dp))
        Text(mode.name.lowercase(), color = Color.White,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 34.dp))
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
                onModeSelected = { mode = it })
        }
        error?.let {
            Text(it, color = Color.White, modifier = Modifier.align(Alignment.Center)
                .background(Color(0xCC550000)).padding(12.dp))
        }
    }
}
