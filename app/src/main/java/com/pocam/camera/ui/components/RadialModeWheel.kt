package com.pocam.camera.ui.components

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocam.camera.domain.model.CameraMode
import kotlin.math.*

val wheelModes = listOf(CameraMode.PHOTO, CameraMode.VIDEO, CameraMode.PORTRAIT,
    CameraMode.NIGHT, CameraMode.MORE, CameraMode.PRO)

@Composable
fun RadialModeWheel(selected: CameraMode, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val accent = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(context).primary
        else Color(0xFFFFD88C)
    }
    val index = wheelModes.indexOf(selected).coerceAtLeast(0)
    var target by remember { mutableFloatStateOf(0f) }
    var previous by remember { mutableIntStateOf(index) }
    LaunchedEffect(index) {
        val raw = (index - previous + wheelModes.size) % wheelModes.size
        val steps = if (raw <= wheelModes.size / 2) raw else raw - wheelModes.size
        target -= steps * 60f
        previous = index
    }
    val rotation by animateFloatAsState(target,
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 180f),
        label = "smooth radial wheel")
    Box(modifier.requiredSize(300.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.requiredSize(300.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val outer = 144.dp.toPx()
            val inner = 66.dp.toPx()
            for (i in wheelModes.indices) {
                val mid = -90f + i * 60f + rotation
                val start = mid - 30f
                val path = Path()
                fun point(radius: Float, angle: Float): Offset {
                    val rad = Math.toRadians(angle.toDouble())
                    return Offset(center.x + radius * cos(rad).toFloat(),
                        center.y + radius * sin(rad).toFloat())
                }
                path.moveTo(point(inner, start).x, point(inner, start).y)
                for (j in 0..24) {
                    val a = start + j * 60f / 24f
                    val pt = point(outer, a)
                    path.lineTo(pt.x, pt.y)
                }
                for (j in 24 downTo 0) {
                    val a = start + j * 60f / 24f
                    val pt = point(inner, a)
                    path.lineTo(pt.x, pt.y)
                }
                path.close()
                val isActive = i == index
                drawPath(path, if (isActive) accent.copy(alpha = .62f)
                    else Color(0xEE202733))
                drawPath(path, if (isActive) accent else Color.White.copy(alpha = .16f),
                    style = Stroke(1.2.dp.toPx()))
            }
            drawCircle(Color(0xFF101720), inner)
            drawCircle(accent.copy(alpha = .85f), inner, style = Stroke(2.dp.toPx()))
        }
        wheelModes.forEachIndexed { i, mode ->
            val rad = Math.toRadians((-90f + i * 60f + rotation).toDouble())
            val x = (105 * cos(rad)).dp
            val y = (105 * sin(rad)).dp
            Text(modeSymbol(mode), color = if (i == index) Color.Black else Color.White,
                fontSize = 23.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
                    .then(Modifier.offset(x = x, y = y)))
        }
        Text(selected.name, color = Color.White, fontSize = 13.sp,
            fontWeight = FontWeight.Bold)
    }
}

fun modeSymbol(mode: CameraMode): String = when (mode) {
    CameraMode.PHOTO -> "◎"
    CameraMode.PRO -> "☷"
    CameraMode.VIDEO -> "▣"
    CameraMode.PORTRAIT -> "♙"
    CameraMode.NIGHT -> "☾"
    CameraMode.MORE -> "⊞"
}
