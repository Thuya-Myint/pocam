package com.pocam.camera.camera

import android.content.Context
import android.net.Uri
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraController(private val context: Context) {
    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var front = false
    private var zoom = 1f
    private val capture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()

    fun bind(owner: LifecycleOwner, view: PreviewView, onError: (String) -> Unit) {
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
            } catch (e: Exception) {
                onError(e.message ?: "Unable to start camera")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun flip(owner: LifecycleOwner, view: PreviewView, onError: (String) -> Unit) {
        front = !front
        zoom = 1f
        bind(owner, view, onError)
    }

    fun setZoom(ratio: Float) {
        val state = camera?.cameraInfo?.zoomState?.value
        val clamped = if (state == null) ratio.coerceAtLeast(1f)
            else ratio.coerceIn(state.minZoomRatio, state.maxZoomRatio)
        zoom = clamped
        camera?.cameraControl?.setZoomRatio(clamped)
    }

    fun takePhoto(onSaved: (Uri) -> Unit, onError: (String) -> Unit) {
        val dir = context.getExternalFilesDir("Pictures") ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "POCAM_" +
            SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date()) + ".jpg")
        val output = ImageCapture.OutputFileOptions.Builder(file).build()
        capture.takePicture(output, ContextCompat.getMainExecutor(context),
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
