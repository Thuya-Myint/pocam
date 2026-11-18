package com.pocam.camera.camera

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.content.ContentValues
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.content.Context.CAMERA_SERVICE
import androidx.camera.core.CameraInfo
import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.extensions.ExtensionMode
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.FallbackStrategy
import com.pocam.camera.domain.model.CameraMode
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCamera2Interop::class)
class CameraController(private val context: Context) {
    private var provider: ProcessCameraProvider? = null
    private var onCameraReady: (() -> Unit)? = null
    private var camera: Camera? = null
    private var front = false
    data class RearLens(val id: String, val focalMm: Float, val label: String)
    private val cameraManager by lazy { context.getSystemService(CAMERA_SERVICE) as CameraManager }
    val rearLenses: List<RearLens> by lazy {
        try {
            val candidates = cameraManager.cameraIdList.mapNotNull { id ->
                val ch = cameraManager.getCameraCharacteristics(id)
                if (ch.get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_BACK) null
                else {
                    val focal = ch.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                        ?.minOrNull()
                    if (focal == null || focal <= 0f) null else id to focal
                }
            }.sortedBy { it.second }
            // Only list distinct cameras actually exposed by the Android camera service.
            val mainFocal = candidates.getOrNull(candidates.size / 2)?.second ?: 1f
            candidates.map { (id, focal) ->
                val ratio = mainFocal / focal
                RearLens(id, focal, String.format(Locale.US, "%.1f×", ratio))
            }
        } catch (_: Exception) { emptyList() }
    }
    private var requestedLensId: String? = null
    fun selectedLensId(): String? = requestedLensId
    fun isFrontCamera(): Boolean = front
    fun selectRearLens(id: String) {
        if (rearLenses.any { it.id == id }) {
            front = false
            requestedLensId = id
            zoom = 1f
        }
    }
    fun activeCameraId(): String? = camera?.cameraInfo?.let {
        Camera2CameraInfo.from(it).cameraId
    }
    fun activeFocalLength(): Float? = camera?.cameraInfo?.let {
        Camera2CameraInfo.from(it).getCameraCharacteristic(
            CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull()
    }
    private var activeMode = CameraMode.PHOTO
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var recordingActive = false
    fun isRecording(): Boolean = recordingActive
    // Diagnostics remain available to developers, not shown in the viewfinder.
    fun cameraDiagnostics(): String {
        val id = activeCameraId() ?: "unbound"
        val focal = activeFocalLength()?.let { String.format(Locale.US, "%.2f mm", it) } ?: "unknown"
        val ratio = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
        return "camera=$id focal=$focal zoom=$ratio recording=$recordingActive"
    }
    fun setPhotoQuality(highQuality: Boolean) {
        capture.flashMode = ImageCapture.FLASH_MODE_OFF
        // ImageCapture mode is configured at construction; use a quality-first default.
    }
    private var manualIso: Int? = null
    private var manualExposureNs: Long? = null
    private var manualFocusDiopters: Float? = null
    private var whiteBalanceMode: Int = CaptureRequest.CONTROL_AWB_MODE_AUTO

    data class ManualCapabilities(
        val isoRange: IntRange?,
        val exposureRangeNs: LongRange?,
        val minFocusDistance: Float,
        val supportedAwbModes: Set<Int>
    )

    fun manualCapabilities(): ManualCapabilities? {
        val info = camera?.cameraInfo ?: return null
        val chars = Camera2CameraInfo.from(info)
        val iso = chars.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val exposure = chars.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val focus = chars.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
        val modes = chars.getCameraCharacteristic(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)
            ?.toSet() ?: emptySet()
        val capabilities = chars.getCameraCharacteristic(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) == true
        return ManualCapabilities(
            if (capabilities && iso != null) iso.lower..iso.upper else null,
            if (capabilities && exposure != null) exposure.lower..exposure.upper else null,
            focus, modes
        )
    }

    fun setManualIso(value: Int?) {
        manualIso = value
        applyManual()
    }

    fun setManualExposureNs(value: Long?) {
        manualExposureNs = value
        applyManual()
    }

    fun setManualFocus(diopters: Float?) {
        manualFocusDiopters = diopters
        applyManual()
    }

    fun setWhiteBalance(mode: Int) {
        whiteBalanceMode = mode
        applyManual()
    }

    fun resetManual() {
        manualIso = null
        manualExposureNs = null
        manualFocusDiopters = null
        whiteBalanceMode = CaptureRequest.CONTROL_AWB_MODE_AUTO
        applyManual()
    }

    private fun applyManual() {
        val activeCamera = camera ?: return
        val caps = manualCapabilities() ?: return
        val builder = CaptureRequestOptions.Builder()
        val manualSensor = caps.isoRange != null && caps.exposureRangeNs != null &&
            (manualIso != null || manualExposureNs != null)
        if (manualSensor) {
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            builder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY,
                (manualIso ?: caps.isoRange!!.first).coerceIn(caps.isoRange!!.first, caps.isoRange.last))
            builder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME,
                (manualExposureNs ?: 10_000_000L).coerceIn(caps.exposureRangeNs!!.first, caps.exposureRangeNs.last))
        }
        if (manualFocusDiopters != null && caps.minFocusDistance > 0f) {
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            builder.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE,
                manualFocusDiopters!!.coerceIn(0f, caps.minFocusDistance))
        }
        if (whiteBalanceMode in caps.supportedAwbModes) {
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, whiteBalanceMode)
        }
        Camera2CameraControl.from(activeCamera.cameraControl).setCaptureRequestOptions(builder.build())
    }

    private var torchEnabled = false
    fun hasTorch(): Boolean = camera?.cameraInfo?.hasFlashUnit() == true
    fun setTorch(enabled: Boolean) {
        torchEnabled = enabled && hasTorch()
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    private var zoom = 1f
    private val capture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        .setResolutionSelector(ResolutionSelector.Builder()
            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
            .build())
        .build()

    fun bind(owner: LifecycleOwner, view: PreviewView, onError: (String) -> Unit,
             onReady: () -> Unit = {}, mode: CameraMode = activeMode) {
        activeMode = mode
        onCameraReady = onReady
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val cameraProvider = future.get()
                val baseSelector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA
                    else CameraSelector.DEFAULT_BACK_CAMERA
                val requestedId = if (front) null else requestedLensId
                val chosenBase = if (requestedId == null) baseSelector else CameraSelector.Builder()
                    .addCameraFilter { infos ->
                        infos.filter { info ->
                            Camera2CameraInfo.from(info).cameraId == requestedId
                        }
                    }.build()
                val extensionMode = when (mode) {
                    CameraMode.NIGHT -> ExtensionMode.NIGHT
                    CameraMode.PORTRAIT -> ExtensionMode.BOKEH
                    else -> ExtensionMode.HDR
                }
                // Extensions are optional: unsupported devices retain normal CameraX capture.
                val extensionFuture = ExtensionsManager.getInstanceAsync(context, cameraProvider)
                extensionFuture.addListener({
                    try {
                        val extensions = extensionFuture.get()
                        val selectedExtension = if (mode == CameraMode.NIGHT ||
                            mode == CameraMode.PORTRAIT || mode == CameraMode.PHOTO) extensionMode
                            else -1
                        val selector = if (selectedExtension != -1 &&
                            requestedId == null && extensions.isExtensionAvailable(chosenBase, selectedExtension)) {
                            extensions.getExtensionEnabledCameraSelector(chosenBase, selectedExtension)
                        } else chosenBase
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = view.surfaceProvider
                        }
                        recording?.stop()
                        recording = null
                        recordingActive = false
                        cameraProvider.unbindAll()
                        if (mode == CameraMode.VIDEO) {
                            val recorder = Recorder.Builder()
                                .setQualitySelector(QualitySelector.from(Quality.HIGHEST, FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD))).build()
                            val video = VideoCapture.withOutput(recorder)
                            camera = cameraProvider.bindToLifecycle(owner, selector, preview, video)
                            videoCapture = video
                        } else {
                            videoCapture = null
                            camera = cameraProvider.bindToLifecycle(owner, selector, preview, capture)
                        }
                        provider = cameraProvider
                        setZoom(zoom)
                        setTorch(torchEnabled)
                        if (mode == CameraMode.PRO) applyManual()
                        onCameraReady?.invoke()
                    } catch (e: Exception) {
                        onError(e.message ?: "Unable to configure camera")
                    }
                }, ContextCompat.getMainExecutor(context))
            } catch (e: Exception) {
                onError(e.message ?: "Unable to start camera")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun toggleVideo(onSaved: (Uri) -> Unit, onError: (String) -> Unit,
                    onState: (Boolean) -> Unit) {
        if (recordingActive) {
            recording?.stop()
            return
        }
        val video = videoCapture ?: run { onError("Video camera is not ready"); return }
        val name = "POCAM_" +
            SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date()) + ".mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/Pocam")
        }
        val options = MediaStoreOutputOptions.Builder(
            context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(values).build()
        try {
            recording = video.output.prepareRecording(context, options)
                .start(ContextCompat.getMainExecutor(context)) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> {
                            recordingActive = true
                            onState(true)
                        }
                        is VideoRecordEvent.Finalize -> {
                            recordingActive = false
                            recording = null
                            onState(false)
                            if (event.hasError()) onError("Video recording failed: ${event.error}")
                            else onSaved(event.outputResults.outputUri)
                        }
                    }
                }
        } catch (e: Exception) {
            recordingActive = false
            onState(false)
            onError(e.message ?: "Could not start video recording")
        }
    }

    fun flip(owner: LifecycleOwner, view: PreviewView, onError: (String) -> Unit, onReady: () -> Unit = {}) {
        front = !front
        requestedLensId = null
        zoom = 1f
        torchEnabled = false
        manualIso = null
        manualExposureNs = null
        manualFocusDiopters = null
        whiteBalanceMode = CaptureRequest.CONTROL_AWB_MODE_AUTO
        bind(owner, view, onError, onReady, activeMode)
    }

    fun maxZoom(): Float = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 4f

    fun setZoom(ratio: Float) {
        val state = camera?.cameraInfo?.zoomState?.value
        val clamped = if (state == null) ratio.coerceAtLeast(1f)
            else ratio.coerceIn(state.minZoomRatio, state.maxZoomRatio)
        zoom = clamped
        camera?.cameraControl?.setZoomRatio(clamped)
    }

    fun exposureRange(): IntRange {
        val range = camera?.cameraInfo?.exposureState?.exposureCompensationRange
        return if (range == null) 0..0 else range.lower..range.upper
    }

    fun exposureStepEv(): Float =
        camera?.cameraInfo?.exposureState?.exposureCompensationStep?.toFloat() ?: 0f

    fun setExposure(index: Int) {
        val range = exposureRange()
        camera?.cameraControl?.setExposureCompensationIndex(index.coerceIn(range.first, range.last))
    }

    fun focusAt(view: PreviewView, x: Float, y: Float) {
        val point = view.meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(3, TimeUnit.SECONDS).build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    // Request a center autofocus pass before capture, but do not block shooting
    // if the device reports unsupported AF or the focus future fails.
    fun focusThenTakePhoto(onSaved: (Uri) -> Unit, onError: (String) -> Unit) {
        val activeCamera = camera
        if (activeCamera == null) {
            onError("Camera is not ready")
            return
        }
        val point = SurfaceOrientedMeteringPointFactory(1f, 1f).createPoint(.5f, .5f)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(3, TimeUnit.SECONDS).build()
        try {
            val future = activeCamera.cameraControl.startFocusAndMetering(action)
            future.addListener({
                // A completed AF attempt is not proof of focus lock; capture
                // regardless to avoid indefinite shutter delays on fixed-focus lenses.
                takePhoto(onSaved, onError)
            }, ContextCompat.getMainExecutor(context))
        } catch (_: Exception) {
            takePhoto(onSaved, onError)
        }
    }

    fun takePhoto(onSaved: (Uri) -> Unit, onError: (String) -> Unit) {
        val name = "POCAM_" +
            SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date()) + ".jpg"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            saveLegacyPhoto(name, onSaved, onError)
            return
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Pocam")
            }
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) {
            onError("Could not create photo in gallery")
            return
        }
        val output = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver, uri, values).build()
        capture.takePicture(output, ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    onSaved(result.savedUri ?: uri)
                }
                override fun onError(exception: ImageCaptureException) {
                    context.contentResolver.delete(uri, null, null)
                    onError(exception.message ?: "Photo capture failed")
                }
            })
    }

    private fun saveLegacyPhoto(name: String, onSaved: (Uri) -> Unit,
                                onError: (String) -> Unit) {
        val dir = context.getExternalFilesDir("Pictures") ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, name)
        capture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    onSaved(result.savedUri ?: Uri.fromFile(file))
                }
                override fun onError(exception: ImageCaptureException) {
                    onError(exception.message ?: "Photo capture failed")
                }
            })
    }

    fun release() {
        recording?.stop()
        recording = null
        recordingActive = false
        provider?.unbindAll()
        camera = null
        provider = null
    }
}
