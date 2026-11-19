package com.pocam.camera.ui.components

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Offline non-destructive photo editor with the Pocam studio design. */
@Composable
fun PhotoEditor(uri: Uri, onClose: () -> Unit, onSaved: (Uri) -> Unit, onError: (String) -> Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var brightness by remember(uri) { mutableFloatStateOf(0f) }
    var contrast by remember(uri) { mutableFloatStateOf(1f) }
    var rotation by remember(uri) { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf("Brightness") }
    var saving by remember { mutableStateOf(false) }
    val source by produceState<Bitmap?>(initialValue=null,key1=uri) {
        value=withContext(Dispatchers.IO) {
            try { context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream) }
            catch (_:Exception) { null }
        }
    }
    val preview=remember(source,brightness,contrast,rotation) {
        source?.let { processPhoto(it,brightness,contrast,rotation) }
    }
    Column(
        Modifier.fillMaxSize()
            .background(PocamPalette.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(PocamPalette.Graphite,CircleShape)
                .clickable(onClick=onClose),contentAlignment=Alignment.Center) {
                Text("×",color=Color.White,fontSize=28.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Edit photo",color=Color.White,
                    fontSize=22.sp,fontWeight=FontWeight.Bold)
                Text("Non-destructive  ·  Save a copy",
                    color=PocamPalette.Silver,fontSize=11.sp)
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f)
            .background(PocamPalette.Carbon,RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp)),contentAlignment=Alignment.Center) {
            if(preview!=null) Image(preview.asImageBitmap(),"Live edited preview",
                Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
            else CircularProgressIndicator(color=PocamPalette.Orange)
        }
        Column(
            Modifier.fillMaxWidth().background(
                PocamPalette.GlassLight,RoundedCornerShape(25.dp))
                .padding(14.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                listOf("Brightness","Contrast","Rotate").forEach { tab ->
                    Box(Modifier.weight(1f)
                        .background(if(selected==tab)PocamPalette.Orange
                            else PocamPalette.Graphite,CircleShape)
                        .clickable { selected=tab }.padding(vertical=10.dp),
                        contentAlignment=Alignment.Center) {
                        Text(tab,color=if(selected==tab)PocamPalette.Black else Color.White,
                            fontSize=11.sp,fontWeight=FontWeight.Bold)
                    }
                }
            }
            when(selected) {
                "Brightness" -> {
                    Text("Brightness  ${brightness.toInt()}",color=Color.White)
                    Slider(brightness,{brightness=it},valueRange=-80f..80f)
                }
                "Contrast" -> {
                    Text("Contrast  ${String.format(Locale.US,"%.2f",contrast)}",color=Color.White)
                    Slider(contrast,{contrast=it},valueRange=.5f..1.5f)
                }
                else -> {
                    Text("Rotation  $rotation°",color=Color.White)
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick={rotation=(rotation+270)%360}){ Text("↺ 90°") }
                        OutlinedButton(onClick={rotation=(rotation+90)%360}){ Text("↻ 90°") }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                OutlinedButton(onClick={brightness=0f;contrast=1f;rotation=0}) {
                    Text("Reset")
                }
                Button(
                    enabled=preview!=null&&!saving,
                    onClick={
                        val bitmap=preview?:return@Button
                        saving=true
                        scope.launch {
                            val result=withContext(Dispatchers.IO) {
                                try { saveEditedPhoto(context,bitmap) }
                                catch (_:Exception) { null }
                            }
                            saving=false
                            if(result!=null)onSaved(result)
                            else onError("Unable to save edited photo")
                        }
                    }
                ) {
                    Text(if(saving)"Saving…" else "Save copy",
                        color=PocamPalette.Black,fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}

private fun processPhoto(source: Bitmap, brightness: Float, contrast: Float, degrees: Int): Bitmap {
    val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
    val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    val output = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
    val offset = brightness + 128f * (1f - contrast)
    val values = floatArrayOf(
        contrast,0f,0f,0f,offset,
        0f,contrast,0f,0f,offset,
        0f,0f,contrast,0f,offset,
        0f,0f,0f,1f,0f
    )
    Canvas(output).drawBitmap(rotated, 0f, 0f,
        Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(values))
        })
    return output
}

private fun saveEditedPhoto(context: android.content.Context, bitmap: Bitmap): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "POCAM_EDIT_" +
            SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date()) + ".jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Pocam")
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    return try {
        val written = resolver.openOutputStream(uri)?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it)
        } == true
        if (written) uri else { resolver.delete(uri, null, null); null }
    } catch (_: Exception) {
        resolver.delete(uri, null, null)
        null
    }
}
