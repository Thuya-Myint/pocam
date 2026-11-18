package com.pocam.camera.camera

import android.content.Context
import android.net.Uri
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
    private val capture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    fun bind(owner: LifecycleOwner, view: PreviewView, onError: (String) -> Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val cameraProvider = future.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                provider = cameraProvider
            } catch (e: Exception) {
                onError(e.message ?: "Unable to start camera")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun takePhoto(onSaved: (Uri) -> Unit, onError: (String) -> Unit) {
        val dir = context.getExternalFilesDir("Pictures") ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val output = ImageCapture.OutputFileOptions.Builder(File(dir, "POCAM_$stamp.jpg")).build()
        capture.takePicture(output, ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    result.savedUri?.let(onSaved) ?: onSaved(Uri.fromFile(File(dir, "POCAM_$stamp.jpg")))
                }
                override fun onError(exception: ImageCaptureException) {
                    onError(exception.message ?: "Photo capture failed")
                }
            })
    }

    fun release() {
        provider?.unbindAll()
        provider = null
    }
}
