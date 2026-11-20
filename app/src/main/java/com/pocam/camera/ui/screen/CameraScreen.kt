package com.pocam.camera.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.BoxScope
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
import com.pocam.camera.ui.components.StraightRuler
import com.pocam.camera.ui.components.ZoomRail
import com.pocam.camera.ui.components.CurvedModeSelector
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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.camera.video.Quality
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.pocam.camera.ui.components.ProUiState
import com.pocam.camera.ui.components.ProControlRail
import com.pocam.camera.ui.components.ProParameterTray
import com.pocam.camera.ui.components.PocamPalette
import com.pocam.camera.ui.components.PhotoEditor

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val accent = PocamPalette.Orange
    val proState = remember { ProUiState() }
    val owner = LocalLifecycleOwner.current
    val tones = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 75) }
    val lastSoundAt = remember { longArrayOf(0L) }
    fun playUiTone() {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastSoundAt[0] >= 120L) {
            lastSoundAt[0] = now
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
    var minZoom by remember { mutableStateOf(1f) }
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
    var shutterSound by remember { mutableStateOf(preferences.getBoolean("shutter_sound", true)) }
    var videoStabilization by remember { mutableStateOf(preferences.getBoolean("video_stabilization", true)) }
    var headerOpacity by remember { mutableStateOf(preferences.getFloat("header_opacity", .85f)) }
    var motionEnabled by remember { mutableStateOf(preferences.getBoolean("motion_enabled", true)) }
    LaunchedEffect(videoStabilization) { controller.setVideoStabilizationEnabled(videoStabilization) }
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
    // Changing video settings shouldn't tear down and reopen the active preview
    // while the user is tapping controls on the Settings screen.
    var pendingVideoRebind by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var showMoreExposure by remember { mutableStateOf(false) }
    var focusLocked by remember { mutableStateOf(false) }
    var shutterPressed by remember { mutableStateOf(false) }
    var captureFlash by remember { mutableStateOf(false) }
    var focusPoint by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var focusSequence by remember { mutableStateOf(0) }
    var focusLockPoint by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var lastPhoto by remember {
        mutableStateOf(context.getSharedPreferences("pocam", 0)
            .getString("last_media_uri", null)?.let(Uri::parse))
    }
    fun rememberMedia(uri: Uri) {
        lastPhoto = uri
        context.getSharedPreferences("pocam", 0).edit()
            .putString("last_media_uri", uri.toString()).apply()
    }
    // Keep the decoded image cached while Settings is open; don't decode
    // on every return to the camera controls.
    val thumbnail by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null, key1 = lastPhoto) {
        value = withContext(Dispatchers.IO) {
            lastPhoto?.let { uri ->
                try {
                    // Decode near thumbnail size instead of allocating a
                    // full-resolution photo (which causes GC stalls/OOM).
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, bounds)
                    }
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) null
                    else {
                        var sample = 1
                        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 256) {
                            sample *= 2
                        }
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = sample
                            inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                        }
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
                        }
                    }
                } catch (_: Exception) { null }
            }
        }
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
            if (!focusLocked) focusPoint = null
        }
    }
    LaunchedEffect(captureFlash) {
        if (captureFlash) {
            delay(160)
            captureFlash = false
        }
    }
    DisposableEffect(controller, tones) { onDispose { controller.release(); tones.release() } }
    LaunchedEffect(showSettings, pendingVideoRebind, recordingVideo, mode) {
        if (!showSettings && pendingVideoRebind &&
            mode == CameraMode.VIDEO && !recordingVideo) {
            pendingVideoRebind = false
            previewView?.let { view ->
                controller.bind(owner, view, { error = it }, {
                    cameraGeneration++
                    exposureRange = controller.exposureRange()
                    maxZoom = controller.maxZoom()
                    minZoom = controller.minZoom()
                }, mode)
            }
        }
    }
    BackHandler(enabled = showSettings || showProPanel || showMore) {
        when {
            showSettings -> showSettings = false
            showProPanel -> showProPanel = false
            showMore -> showMore = false
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val availableHeight = maxHeight
        if (permitted) {
            AndroidView(factory = { ctx ->
                PreviewView(ctx).apply {
                    // SurfaceView-first for lower GPU cost and faster preview frames.
                    // CameraX falls back automatically when the device requires it.
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this
                    controller.bind(owner, this, { error = it }, onReady = {
                        cameraGeneration++
                        exposureRange = controller.exposureRange()
                        maxZoom = controller.maxZoom()
                        minZoom = controller.minZoom()
                        activeLensId = controller.activeCameraId()
                        activeFocalMm = controller.activeFocalLength()
                    })

                }
            }, modifier = Modifier.fillMaxSize())
        }
        // Transparent Compose gesture layer sits above PreviewView (including its internal surface).
        // Camera controls are composed later and remain interactive above this layer.
        if (permitted && !showSettings) {
            Box(Modifier.fillMaxSize().pointerInput(focusLocked, previewView) {
                detectTapGestures(
                    onTap = { offset ->
                        val view = previewView
                        if (view != null) {
                            if (focusLocked) {
                                val point = focusLockPoint
                                if (point != null &&
                                    kotlin.math.abs(offset.x - point.first * size.width) < 80f &&
                                    kotlin.math.abs(offset.y - point.second * size.height) < 80f) {
                                    controller.unlockFocus()
                                    focusLocked = false
                                    focusLockPoint = null
                                    focusPoint = null
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                            } else {
                                controller.focusAt(view, offset.x, offset.y)
                                focusPoint = Pair(offset.x / size.width.coerceAtLeast(1),
                                    offset.y / size.height.coerceAtLeast(1))
                                focusSequence++
                            }
                        }
                    },
                    onLongPress = { offset ->
                        val view = previewView
                        if (view != null && !focusLocked) {
                            controller.lockFocusAt(view, offset.x, offset.y)
                            focusLocked = controller.isFocusLocked()
                            if (focusLocked) {
                                focusPoint = Pair(offset.x / size.width.coerceAtLeast(1),
                                    offset.y / size.height.coerceAtLeast(1))
                                focusLockPoint = focusPoint
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }
                        }
                    }
                )
            })
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
        // Style 4: floating controls. The chevron expands this surface vertically.
        if (!showSettings) {
            Column(Modifier.align(Alignment.TopCenter).zIndex(16f).fillMaxWidth()
                .background(PocamPalette.Black.copy(alpha=headerOpacity))
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(start = 13.dp, end = 13.dp, top = 8.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp)
                        .background(Color(0x991E2027), CircleShape)
                        .clickable { showMore = !showMore },
                        contentAlignment = Alignment.Center) {
                        Icon(if (showMore) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (showMore) "Collapse camera controls" else "Expand camera controls",
                            tint = Color.White)
                    }
                    Row(Modifier.weight(1f).padding(start = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("pocam", color = Color.White, fontSize = 18.sp,
                            fontWeight = FontWeight.Bold)
                        Text(".", color = PocamPalette.Orange, fontSize = 18.sp,
                            fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Box(Modifier.size(38.dp)
                            .background(Color(0x991E2027), CircleShape)
                            .clickable {
                                if (controller.hasTorch()) {
                                    torchEnabled = !torchEnabled
                                    controller.setTorch(torchEnabled)
                                }
                            }, contentAlignment = Alignment.Center) {
                            Icon(if (torchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                                contentDescription = "Toggle torch",
                                tint = if (torchEnabled) accent else Color.White,
                                modifier = Modifier.size(21.dp))
                        }
                        Box(Modifier.size(38.dp)
                            .background(Color(0x991E2027), CircleShape)
                            .clickable {
                                gridEnabled = !gridEnabled
                                preferences.edit().putBoolean("grid", gridEnabled).apply()
                            }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.GridOn, contentDescription = "Toggle grid",
                                tint = if (gridEnabled) accent else Color.White,
                                modifier = Modifier.size(21.dp))
                        }
                        Box(Modifier.size(38.dp)
                            .background(Color(0x991E2027), CircleShape)
                            .clickable { showMore = false; showSettings = true },
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Tune, contentDescription = "Open settings page",
                                tint = Color.White, modifier = Modifier.size(21.dp))
                        }
                    }
                }
                AnimatedVisibility(
                    visible = showMore,
                    enter = fadeIn(tween(145)) + slideInVertically(tween(175)) { -it / 6 },
                    exit = fadeOut(tween(100)) + slideOutVertically(tween(130)) { -it / 6 }
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("QUICK CONTROLS", color = PocamPalette.Orange,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            @Composable
                            fun Quick(label: String, selected: Boolean,
                                icon: androidx.compose.ui.graphics.vector.ImageVector,
                                onClick: () -> Unit) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(Modifier.size(52.dp)
                                        .background(if(selected) PocamPalette.Orange.copy(alpha=.18f)
                                            else PocamPalette.Graphite,CircleShape)
                                        .clickable(onClick=onClick),
                                        contentAlignment = Alignment.Center) {
                                        Icon(icon, contentDescription=label,
                                            tint=if(selected)PocamPalette.Orange else Color.White,
                                            modifier=Modifier.size(23.dp))
                                    }
                                    Text(label,color=PocamPalette.Silver,fontSize=10.sp)
                                }
                            }
                            Quick("Torch",torchEnabled,Icons.Rounded.FlashOn) {
                                if(controller.hasTorch()) {
                                    torchEnabled=!torchEnabled
                                    controller.setTorch(torchEnabled)
                                }
                            }
                            Quick("Grid",gridEnabled,Icons.Rounded.GridOn) {
                                gridEnabled=!gridEnabled
                                preferences.edit().putBoolean("grid",gridEnabled).apply()
                            }
                            Quick("Timer ${timerSeconds}s",timerSeconds>0,Icons.Rounded.Timer) {
                                val values=listOf(0,3,5,10)
                                timerSeconds=values[(values.indexOf(timerSeconds)+1)
                                    .coerceAtLeast(0)%values.size]
                                preferences.edit().putInt("timer",timerSeconds).apply()
                            }
                            Quick("EV",exposureIndex!=0,Icons.Rounded.Tune) {
                                showMoreExposure=!showMoreExposure
                            }
                        }
                        AnimatedVisibility(showMoreExposure && exposureRange.first<exposureRange.last) {
                            StraightRuler(
                                value=exposureIndex.toFloat(),
                                valueRange=exposureRange.first.toFloat()..exposureRange.last.toFloat(),
                                onValueChange={
                                    exposureIndex=it.toInt().coerceIn(exposureRange)
                                    controller.setExposure(exposureIndex)
                                },
                                label="${exposureIndex} EV steps",
                                onDetent={playUiTone()}
                            )
                        }
                    }
                }
            }
            Box(Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                // Compact outlined badge directly below the header, like the approved design.
                .padding(top = 77.dp)
                .background(Color(0xD4181A20), CircleShape)
                .border(.75.dp, PocamPalette.Orange.copy(alpha=.62f), CircleShape)
                .padding(horizontal = 14.dp, vertical = 4.dp)) {
                Text(mode.name, color = accent, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

        }
        // Render settings as a fixed opaque page. Sliding and fading the entire
        // settings tree over a live camera SurfaceView causes large GPU overdraw
        // and frame skips on some Android devices. Immediate navigation is cheaper.
        if (showSettings) {
            Column(Modifier.fillMaxSize().zIndex(30f)
                .background(PocamPalette.Carbon)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)).navigationBarsPadding()) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)) {
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
                shutterSound = shutterSound,
                onShutterSound = { value -> shutterSound = value
                    preferences.edit().putBoolean("shutter_sound", value).apply() },
                videoStabilization = videoStabilization,
                onVideoStabilization = { value ->
                    videoStabilization = value
                    controller.setVideoStabilizationEnabled(value)
                    preferences.edit().putBoolean("video_stabilization",value).apply()
                    if (mode == CameraMode.VIDEO) pendingVideoRebind = true
                },
                headerOpacity = headerOpacity,
                onHeaderOpacity = { value ->
                    headerOpacity = value
                    preferences.edit().putFloat("header_opacity",value).apply()
                },
                exposure = exposureIndex,
                exposureRange = exposureRange,
                onExposure = { exposureIndex = it; controller.setExposure(it) },
                onVideoQuality = { selected ->
                    videoQuality = selected
                    preferences.edit().putString("video_quality", selected).apply()
                    controller.setVideoQuality(when (selected) {
                        "4K" -> Quality.UHD
                        "720p" -> Quality.HD
                        else -> Quality.FHD
                    })
                    if (mode == CameraMode.VIDEO) pendingVideoRebind = true
                },
                onTimer = {
                    timerSeconds = it
                    preferences.edit().putInt("timer", it).apply()
                },
                motionEnabled = motionEnabled,
                onMotionEnabled = { enabled ->
                    motionEnabled = enabled
                    preferences.edit().putBoolean("motion_enabled", enabled).apply()
                },
                onClose = { showSettings = false }
            )
                }
            }
        }
        // Keep the curved zoom control clear of the shutter and gallery/flip buttons.
        if (!showSettings && !shutterPressed && editPhoto == null) {
            Box(Modifier.align(Alignment.BottomCenter).zIndex(12f)
                .navigationBarsPadding().padding(bottom = if (mode == CameraMode.PRO && showProPanel) 290.dp else 208.dp, start = 17.dp, end = 17.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                if (!controller.isFrontCamera() && controller.rearLenses.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        controller.rearLenses.forEach { lens ->
                            val selected = activeLensId == lens.id
                            Box(Modifier.background(
                                if (selected) accent.copy(alpha = .9f) else Color(0x88303842),
                                CircleShape).clickable(enabled = !captureBusy && !recordingVideo) {
                                controller.selectRearLens(lens.id)

                                proState.reset()

                                showProPanel = false
                                zoom = 1f
                                previewView?.let { view ->
                                    controller.bind(owner, view, { error = it }, {
                                        cameraGeneration++
                                        maxZoom = controller.maxZoom()
                                        minZoom = controller.minZoom()
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
                // Keep high-frequency dragging inside ZoomRail, not the whole screen.
                ZoomRail(zoom, minZoom = minZoom, maxZoom = maxZoom,
                    onSelect = { value -> controller.setZoom(value) },
                    onCommit = { value -> zoom = value },
                    onTick = { })
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
        // Compact right-side rail: live camera preview remains visible.
        if (mode == CameraMode.PRO && !showSettings && editPhoto == null) {
            ProControlRail(
                state = proState,
                controller = controller,
                cameraGeneration = cameraGeneration,
                exposureIndex = exposureIndex,
                exposureStepEv = controller.exposureStepEv(),
                expanded = showProPanel,
                onExpanded = { showProPanel = it },
                modifier = Modifier.align(Alignment.TopEnd).zIndex(14f)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    // Slim 51dp rail starts directly below the compact top header.
                    .padding(top = if (showMore) 210.dp else 90.dp, end = 12.dp),
                motionEnabled = motionEnabled
            )
        }
        // A compact Pro tray should respond immediately, not animate a translucent
        // layout across the live preview while rebuilding Camera2 controls.
        if (mode == CameraMode.PRO && showProPanel && !showSettings && editPhoto == null) {
            ProParameterTray(
                state = proState,
                controller = controller,
                cameraGeneration = cameraGeneration,
                exposureIndex = exposureIndex,
                exposureRange = exposureRange,
                exposureStepEv = controller.exposureStepEv(),
                onExposure = { next -> exposureIndex = next; controller.setExposure(next) },
                onCollapse = { showProPanel = false },
                modifier = Modifier.align(Alignment.BottomCenter).zIndex(15f)
                    .navigationBarsPadding()
                    .padding(start = 13.dp, end = 13.dp, bottom = 111.dp)
                    .heightIn(max = 167.dp)
            )
        }

        // A single renderer owns the background, curve, selected pill and labels.
        // This avoids overlap and ensures the highlight follows the exact same arc.
        if (!showSettings && editPhoto == null && !(mode == CameraMode.PRO && showProPanel)) {
            CurvedModeSelector(
                selected = mode,
                motionEnabled = motionEnabled,
                modifier = Modifier.align(Alignment.BottomCenter).zIndex(13f)
                    .navigationBarsPadding()
                    .padding(start = 9.dp, end = 9.dp, bottom = 122.dp),
                onSelect = { candidate ->
                    if (candidate != mode && !captureBusy && !recordingVideo) {
                        previewView?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        if (mode == CameraMode.PRO) controller.resetManual()
                        mode = candidate
                        focusLocked = false
                        focusLockPoint = null
                        showProPanel = false
                        proState.reset()
                        showMore = false
                        previewView?.let { view ->
                            controller.bind(owner, view, { error = it }, {
                                cameraGeneration++
                                exposureRange = controller.exposureRange()
                                maxZoom = controller.maxZoom()
                                minZoom = controller.minZoom()
                            }, candidate)
                        }
                    }
                }
            )
        }
        // Transparent floating controls over the edge-to-edge camera preview.
        // Keep the shutter's footprint fixed so the wheel cannot resize the dock.
        if (!showSettings && editPhoto == null) {
        Row(Modifier.align(Alignment.BottomCenter).zIndex(10f).fillMaxWidth()
            .navigationBarsPadding().padding(start = 17.dp, end = 17.dp, top = 8.dp, bottom = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(43.dp)
                .background(Color(0xCE25272E), CircleShape)
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
                if (thumbnail != null) {
                    Image(bitmap = thumbnail!!, contentDescription = "Last captured photo",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape))
                } else {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = "View last photo",
                        tint = if (lastPhoto == null) Color.LightGray else Color.White)
                }
            }
            // Keep shutter fixed and separate from the curved, tappable mode selector.
            Box(Modifier.size(74.dp),
                contentAlignment = Alignment.Center) {

                ShutterButton(busy = captureBusy, recording = recordingVideo,
                    motionEnabled = motionEnabled,
                    onSound = { if (shutterSound) playUiTone() },
                    onCapture = {
                        if (captureBusy) return@ShutterButton
                        shutterPressed = true
                        if (mode == CameraMode.VIDEO) {
                            controller.toggleVideo(
                                onSaved = { uri -> rememberMedia(uri) },
                                onError = { error = it; shutterPressed = false },
                                onState = { recordingVideo = it; shutterPressed = false })
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
                                    shutterPressed = false
                                    rememberMedia(uri)
                                    Toast.makeText(context, "Saved to Pocam photos",
                                        Toast.LENGTH_SHORT).show()
                                },
                                onError = {
                                    countdown = null
                                    countdownJob = null
                                    captureBusy = false
                                    shutterPressed = false
                                    error = it
                                })
                            }
                        } else {
                            shutterPressed = false
                            Toast.makeText(context, "${mode.name} coming soon",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                if (captureBusy) {
                    Text(if (countdown != null) "TIMER" else "SAVING",
                        color = accent, fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.TopCenter)
                            .offset(y = (-14).dp))
                }
            }
            Box(Modifier.size(43.dp)
                .background(Color(0xCE25272E), CircleShape)
                .clickable(enabled = !captureBusy && !recordingVideo) {
                    showSettings = false
                    showProPanel = false
                    previewView?.let { view ->
                        controller.flip(owner, view, { error = it }) {
                            cameraGeneration++
                            exposureRange = controller.exposureRange()
                            exposureIndex = 0
                            maxZoom = controller.maxZoom()
                            minZoom = controller.minZoom()
                        }
                        zoom = 1f

                        proState.reset()
                        focusLocked = false
                        focusLockPoint = null
                        torchEnabled = false
                    }
                }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Cameraswitch, contentDescription = "Switch camera",
                    tint = Color.White)
            }
        }
        }
        if (recordingVideo && !showSettings) {
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
            Box(Modifier.fillMaxSize().zIndex(100f)) {
            PhotoEditor(uri = uri, onClose = { editPhoto = null }, onSaved = {
                rememberMedia(it)
                editPhoto = null
                Toast.makeText(context, "Edited photo saved", Toast.LENGTH_SHORT).show()
            }, onError = { error = it })
            }
        }
        if (editPhoto == null) error?.let {
            Text(it, color = Color.White, modifier = Modifier.align(Alignment.Center)
                .background(Color(0xCC550000), RoundedCornerShape(12.dp))
                .clickable { error = null }.padding(12.dp))
        }
    }
}
