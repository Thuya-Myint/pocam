package com.pocam.camera.camera

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.content.ContentValues
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.MeteringPointFactory
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

    private var zoom = 1f
    private val capture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()

    fun bind(owner: LifecycleOwner, view: PreviewView, onError: (String) -> Unit, onReady: () -> Unit = {}) {
        onCameraReady = onReady
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val cameraProvider = future.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                cameraProvider.unbindAll()
                val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                camera = cameraProvider.bindToLifecycle(owner, selector, preview, capture)
                provider = cameraProvider
                setZoom(zoom)
                applyManual()
                onCameraReady?.invoke()
            } catch (e: Exception) {
                onError(e.message ?: "Unable to start camera")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun flip(owner: LifecycleOwner, view: PreviewView, onError: (String) -> Unit, onReady: () -> Unit = {}) {
        front = !front
        zoom = 1f
        manualIso = null
        manualExposureNs = null
        manualFocusDiopters = null
        whiteBalanceMode = CaptureRequest.CONTROL_AWB_MODE_AUTO
        bind(owner, view, onError, onReady)
    }

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
        provider?.unbindAll()
        camera = null
        provider = null
    }
}
