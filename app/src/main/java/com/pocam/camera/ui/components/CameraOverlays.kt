package com.pocam.camera.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale

@Composable
private fun cameraAccent(): Color {
    val context = LocalContext.current
    return remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(context).primary
        else Color(0xFFFFD88C)
    }
}

@Composable
fun CameraGrid(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 1.dp.toPx()
        for (index in 1..2) {
            val fraction = index / 3f
            drawLine(Color.White.copy(alpha = .23f), Offset(size.width * fraction, 0f),
                Offset(size.width * fraction, size.height), stroke)
            drawLine(Color.White.copy(alpha = .23f), Offset(0f, size.height * fraction),
                Offset(size.width, size.height * fraction), stroke)
        }
    }
}

@Composable
fun FocusReticle(xFraction: Float, yFraction: Float, modifier: Modifier = Modifier) {
    val accent = cameraAccent()
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.size(64.dp).border(1.5.dp, accent,
            RoundedCornerShape(12.dp)))
        Box(Modifier.size(4.dp).background(accent, CircleShape))
    }
}

@Composable
fun ZoomRail(
    selected: Float, maxZoom: Float, onSelect: (Float) -> Unit,
    onTick: () -> Unit = {}, modifier: Modifier = Modifier
) {
    val accent = cameraAccent()
    val safeMax = maxZoom.coerceAtLeast(1.01f)
    val latestSelect by rememberUpdatedState(onSelect)
    val latestTick by rememberUpdatedState(onTick)
    val latestMax by rememberUpdatedState(safeMax)
    var lastTick by remember { androidx.compose.runtime.mutableIntStateOf(-1) }
    val fraction = ((selected - 1f) / (safeMax - 1f)).coerceIn(0f, 1f)
    val animated by animateFloatAsState(fraction,
        androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = 320f), label = "zoom dial")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(String.format(java.util.Locale.US, "%.1f×", selected),
            color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.background(Color(0xD91B1F28), RoundedCornerShape(20.dp))
                .padding(horizontal = 18.dp, vertical = 5.dp))
        Box(Modifier.width(230.dp).height(82.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun update(x: Float) {
                        val normalized = (x / size.width).coerceIn(0f, 1f)
                        val tick = (normalized * 48).toInt()
                        if (tick != lastTick) {
                            lastTick = tick
                            latestTick()
                        }
                        latestSelect(1f + normalized * (latestMax - 1f))
                    }
                    update(down.position.x)
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change != null && change.pressed) {
                            update(change.position.x)
                            change.consume()
                        }
                    } while (event.changes.any { it.id == down.id && it.pressed })
                }
            }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val radius = size.width * .48f
                val cy = size.height + radius * .27f
                for (i in 0..48) {
                    val theta = Math.toRadians((198f + 144f * i / 48f).toDouble())
                    val major = i % 8 == 0
                    val r1 = radius - if (major) 24.dp.toPx() else 17.dp.toPx()
                    val r2 = radius - 7.dp.toPx()
                    val x1 = cx + r1 * kotlin.math.cos(theta).toFloat()
                    val y1 = cy + r1 * kotlin.math.sin(theta).toFloat()
                    val x2 = cx + r2 * kotlin.math.cos(theta).toFloat()
                    val y2 = cy + r2 * kotlin.math.sin(theta).toFloat()
                    // Feather the outer edges rather than clipping or hiding ticks.
                    val edge = kotlin.math.min(i, 48 - i) / 9f
                    val opacity = (edge.coerceIn(0f, 1f) * edge.coerceIn(0f, 1f) * (3f - 2f * edge.coerceIn(0f, 1f)))
                        .coerceAtLeast(.05f)
                    drawLine(if (i / 48f <= animated) accent.copy(alpha = opacity)
                        else Color.White.copy(alpha = .52f * opacity),
                        Offset(x1, y1), Offset(x2, y2),
                        if (major) 2.dp.toPx() else 1.dp.toPx())
                }
                val theta = Math.toRadians((198f + 144f * animated).toDouble())
                val x = cx + (radius - 17.dp.toPx()) * kotlin.math.cos(theta).toFloat()
                val y = cy + (radius - 17.dp.toPx()) * kotlin.math.sin(theta).toFloat()
                val markerFade = (kotlin.math.min(animated, 1f - animated) * 12f).coerceIn(.15f, 1f)
                drawCircle(accent.copy(alpha = markerFade), 5.dp.toPx(), Offset(x, y))
                drawCircle(Color.White.copy(alpha = markerFade), 2.dp.toPx(), Offset(x, y))
            }
        }
    }
}

@Composable
fun CaptureFlash(active: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(active, modifier = modifier, enter = fadeIn(tween(45)),
        exit = fadeOut(tween(240))) {
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = .70f)))
    }
}
