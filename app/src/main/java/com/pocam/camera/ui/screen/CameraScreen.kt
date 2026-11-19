package com.pocam.camera.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import androidx.camera.video.Quality
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
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.pocam.camera.ui.components.ManualControls
import com.pocam.camera.ui.components.PhotoEditor

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val accent = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicDarkColorScheme(context).primary
        } else Color(0xFFFFD88C)
    }
    val owner = LocalLifecycleOwner.current
    val tones = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 75) }
    var lastSoundAt by remember { mutableStateOf(0L) }
    fun playUiTone() {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastSoundAt >= 55L) {
            lastSoundAt = now
            tones.startTone(ToneGenerator.TONE_PROP_BEEP, 45)
        }
    }
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
    var maxZoom by remember { mutableStateOf(4f) }
    var activeLensId by remember { mutableStateOf<String?>(null) }
    var activeFocalMm by remember { mutableStateOf<Float?>(null) }
    var showProPanel by remember { mutableStateOf(false) }
    val preferences = remember(context) { context.getSharedPreferences("pocam_ui", 0) }
    val scope = rememberCoroutineScope()
    var captureBusy by remember { mutableStateOf(false) }
    var recordingVideo by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf<Int?>(null) }
    var countdownJob by remember { mutableStateOf<Job?>(null) }
    var timerSeconds by remember { mutableStateOf(preferences.getInt("timer", 0)) }
    var videoQuality by remember { mutableStateOf(preferences.getString("video_quality", "1080p") ?: "1080p") }
    LaunchedEffect(videoQuality) {
        controller.setVideoQuality(when (videoQuality) {
            "4K" -> Quality.UHD
            "720p" -> Quality.HD
            else -> Quality.FHD
        })
    }
    var torchEnabled by remember { mutableStateOf(false) }
    var crosshairEnabled by remember { mutableStateOf(preferences.getBoolean("crosshair", false)) }
    var gridEnabled by remember { mutableStateOf(preferences.getBoolean("grid", false)) }
    var showSettings by remember { mutableStateOf(false) }
    var wheelOpen by remember { mutableStateOf(false) }
    var wheelHover by remember { mutableStateOf(CameraMode.PHOTO) }
    var captureFlash by remember { mutableStateOf(false) }
    var focusPoint by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var focusSequence by remember { mutableStateOf(0) }
    var lastPhoto by remember {
        mutableStateOf(context.getSharedPreferences("pocam", 0)
            .getString("last_media_uri", null)?.let(Uri::parse))
    }
    fun rememberMedia(uri: Uri) {
        lastPhoto = uri
        context.getSharedPreferences("pocam", 0).edit()
            .putString("last_media_uri", uri.toString()).apply()
    }
    var error by remember { mutableStateOf<String?>(null) }
    var editPhoto by remember { mutableStateOf<Uri?>(null) }
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
    DisposableEffect(controller, tones) { onDispose { controller.release(); tones.release() } }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val availableHeight = maxHeight
        val proMaxHeight = (availableHeight - 260.dp).coerceAtLeast(120.dp)
        if (permitted) {
            AndroidView(factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this
                    controller.bind(owner, this, { error = it }, onReady = {
                        cameraGeneration++
                        exposureRange = controller.exposureRange()
                        maxZoom = controller.maxZoom()
                        activeLensId = controller.activeCameraId()
                        activeFocalMm = controller.activeFocalLength()
                    })
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

        // Minimal floating header: no opaque bar, no second toolbar.
        Row(Modifier.align(Alignment.TopCenter).zIndex(12f).fillMaxWidth()
            .statusBarsPadding().padding(start = 18.dp, end = 18.dp, top = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("pocam.", color = Color.White, fontSize = 23.sp,
                    fontWeight = FontWeight.Bold)
                Text("POCKET CAMERA", color = Color.White.copy(alpha = .76f),
                    fontSize = 8.sp, letterSpacing = 1.6.sp)
            }
            if (!wheelOpen) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(Color(0x88303842), CircleShape),
                        contentAlignment = Alignment.Center) {
                        IconButton(onClick = {
                            if (controller.hasTorch()) {
                                torchEnabled = !torchEnabled
                                controller.setTorch(torchEnabled)
                            }
                        }) {
                            Icon(if (torchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                                contentDescription = "Toggle torch", tint = if (torchEnabled) accent else Color.White)
                        }
                    }
                    Box(Modifier.size(44.dp).background(Color(0x88303842), CircleShape),
                        contentAlignment = Alignment.Center) {
                        IconButton(onClick = {
                            gridEnabled = !gridEnabled
                            preferences.edit().putBoolean("grid", gridEnabled).apply()
                        }) {
                            Icon(Icons.Rounded.GridOn, contentDescription = "Toggle grid",
                                tint = if (gridEnabled) accent else Color.White)
                        }
                    }
                    Box(Modifier.size(44.dp).background(Color(0x88303842), CircleShape),
                        contentAlignment = Alignment.Center) {
                        IconButton(onClick = { showSettings = !showSettings }) {
                            Icon(Icons.Rounded.Tune, contentDescription = "Camera settings",
                                tint = if (showSettings) accent else Color.White)
                        }
                    }
                }
            }
        }
        if (!wheelOpen) {
            Box(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 106.dp)
                .background(Color(0x77303842), CircleShape)
                .padding(horizontal = 13.dp, vertical = 5.dp)) {
                Text(mode.name, color = accent, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            if (mode == CameraMode.PRO) {
                Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 108.dp, end = 18.dp)
                    .background(Color(0x88303842), CircleShape)) {
                    IconButton(onClick = { showProPanel = !showProPanel }) {
                        Icon(if (showProPanel) Icons.Rounded.Close else Icons.Rounded.Tune,
                            contentDescription = "Pro controls", tint = accent)
                    }
                }
            }
        }
        if (showSettings && !wheelOpen) {
            Box(Modifier.align(Alignment.TopCenter).zIndex(15f)
                .statusBarsPadding().padding(top = 152.dp, start = 12.dp, end = 12.dp)
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
                videoQuality = videoQuality,
                exposure = exposureIndex,
                exposureRange = exposureRange,
                onExposure = { exposureIndex = it; controller.setExposure(it) },
                onVideoQuality = { selected ->
                    videoQuality = selected
                    preferences.edit().putString("video_quality", selected).apply()
                    if (!recordingVideo && mode == CameraMode.VIDEO) {
                        previewView?.let { view -> controller.bind(owner, view, { error = it }, { cameraGeneration++ }, mode) }
                    }
                },
                onTimer = {
                    timerSeconds = it
                    preferences.edit().putInt("timer", it).apply()
                },
                onClose = { showSettings = false }
            )
            }
        }
        // Keep the curved zoom control clear of the shutter and gallery/flip buttons.
        if (!wheelOpen && !showProPanel && !showSettings) {
            Box(Modifier.align(Alignment.BottomCenter).zIndex(12f)
                .padding(bottom = 166.dp, start = 8.dp, end = 8.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!controller.isFrontCamera() && controller.rearLenses.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        controller.rearLenses.forEach { lens ->
                            val selected = activeLensId == lens.id
                            Box(Modifier.background(
                                if (selected) accent.copy(alpha = .9f) else Color(0x88303842),
                                CircleShape).clickable(enabled = !captureBusy && !recordingVideo) {
                                controller.selectRearLens(lens.id)
                                zoom = 1f
                                previewView?.let { view ->
                                    controller.bind(owner, view, { error = it }, {
                                        cameraGeneration++
                                        maxZoom = controller.maxZoom()
                                        activeLensId = controller.activeCameraId()
                                        activeFocalMm = controller.activeFocalLength()
                                    }, mode)
                                }
                            }.padding(horizontal = 12.dp, vertical = 7.dp)) {
                                Text(lens.label, color = if (selected) Color.Black else Color.White,
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                ZoomRail(zoom, maxZoom = maxZoom, onSelect = { value ->
                    zoom = value
                    controller.setZoom(value)
                }, onTick = { playUiTone() })
                if (mode == CameraMode.MORE) {
                    Text("More modes coming soon",
                        fontSize = 10.sp, color = Color.White,
                        modifier = Modifier.background(Color(0xD51B1E25),
                            RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
            }
        }
        AnimatedVisibility(mode == CameraMode.PRO && showProPanel && !wheelOpen && !showSettings,
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(14f).padding(start = 16.dp, end = 16.dp, bottom = 126.dp),
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

        // Transparent floating controls over the edge-to-edge camera preview.
        // Keep the shutter's footprint fixed so the wheel cannot resize the dock.
        Row(Modifier.align(Alignment.BottomCenter).zIndex(10f).fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 60.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp)
                .background(Color(0xCC323640), CircleShape)
                .clickable(enabled = lastPhoto != null) {
                    lastPhoto?.let { uri ->
                        if (uri.toString().contains("/video/")) {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "video/mp4")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                })
                            } catch (_: Exception) { error = "No video viewer available" }
                        } else editPhoto = uri
                    }
                }, contentAlignment = Alignment.Center) {
                val thumbnail by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
                    initialValue = null, key1 = lastPhoto) {
                    value = withContext(Dispatchers.IO) {
                        lastPhoto?.let { uri ->
                            try {
                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                    BitmapFactory.decodeStream(stream)?.let { bitmap ->
                                        val scale = 128f / maxOf(bitmap.width, bitmap.height)
                                        val scaled = android.graphics.Bitmap.createScaledBitmap(
                                            bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1),
                                            (bitmap.height * scale).toInt().coerceAtLeast(1), true)
                                        scaled.asImageBitmap()
                                    }
                                }
                            } catch (_: Exception) { null }
                        }
                    }
                }
                if (thumbnail != null) {
                    Image(bitmap = thumbnail!!, contentDescription = "Last captured photo",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape))
                } else {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = "View last photo",
                        tint = if (lastPhoto == null) Color.LightGray else Color.White)
                }
            }
            Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                if (wheelOpen) {
                    // The 300dp wheel is centered 86dp above the shutter center.
                    // With the shutter dock 60dp above the bottom, this leaves
                    // approximately 40dp below the full wheel, not a clipped half-wheel.
                    RadialModeWheel(
                        wheelHover,
                        Modifier.zIndex(1f).offset(y = (-86).dp)
                    )
                }
                ShutterButton(mode = mode, busy = captureBusy, recording = recordingVideo,
                    onSound = { playUiTone() },
                    onCapture = {
                        if (captureBusy) return@ShutterButton
                        if (mode == CameraMode.VIDEO) {
                            controller.toggleVideo(
                                onSaved = { uri -> rememberMedia(uri) },
                                onError = { error = it },
                                onState = { recordingVideo = it })
                        } else if (mode != CameraMode.MORE) {
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
                                controller.focusThenTakePhoto(
                                onSaved = { uri ->
                                    captureFlash = true
                                    captureBusy = false
                                    rememberMedia(uri)
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
                        if (captureBusy || recordingVideo) return@ShutterButton
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
                        previewView?.let { view ->
                            controller.bind(owner, view, { error = it }, {
                                cameraGeneration++
                                exposureRange = controller.exposureRange()
                                maxZoom = controller.maxZoom()
                            activeLensId = controller.activeCameraId()
                            activeFocalMm = controller.activeFocalLength()
                            }, it)
                        }
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
                .background(Color(0xCC323640), CircleShape)
                .clickable(enabled = !captureBusy && !recordingVideo) {
                    showSettings = false
                    showProPanel = false
                    previewView?.let { view ->
                        controller.flip(owner, view, { error = it }) {
                            cameraGeneration++
                            exposureRange = controller.exposureRange()
                            exposureIndex = 0
                            maxZoom = controller.maxZoom()
                        }
                        zoom = 1f
                        torchEnabled = false
                    }
                }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Cameraswitch, contentDescription = "Switch camera",
                    tint = Color.White)
            }
        }
        if (recordingVideo) {
            Text("● REC  ·  TAP SHUTTER TO STOP", color = Color(0xFFFF7777),
                fontWeight = FontWeight.Bold, fontSize = 12.sp,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 170.dp)
                    .background(Color(0xDD15171C), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp))
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
        editPhoto?.let { uri ->
            PhotoEditor(uri = uri, onClose = { editPhoto = null }, onSaved = {
                rememberMedia(it)
                editPhoto = null
                Toast.makeText(context, "Edited photo saved", Toast.LENGTH_SHORT).show()
            }, onError = { error = it })
        }
        error?.let {
            Text(it, color = Color.White, modifier = Modifier.align(Alignment.Center)
                .background(Color(0xCC550000), RoundedCornerShape(12.dp))
                .clickable { error = null }.padding(12.dp))
        }
    }
}
