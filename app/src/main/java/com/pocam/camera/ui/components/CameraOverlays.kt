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
    val safeMax = maxZoom.coerceAtLeast(1f)
    val latestSelect by rememberUpdatedState(onSelect)
    val latestTick by rememberUpdatedState(onTick)
    val latestMax by rememberUpdatedState(safeMax)
    var lastTick by remember { androidx.compose.runtime.mutableIntStateOf(-1) }
    val fraction = ((selected - 1f) / (safeMax - 1f).coerceAtLeast(.001f)).coerceIn(0f, 1f)
    val animated by animateFloatAsState(fraction, tween(90), label = "continuous zoom")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.background(Color(0xE91B1F28), RoundedCornerShape(30.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            listOf(1f, 2f, 4f).filter { it <= safeMax + .01f }.forEach { value ->
                val active = kotlin.math.abs(selected - value) < .12f
                Box(Modifier.size(40.dp)
                    .background(if (active) accent else Color.Transparent, CircleShape)
                    .clickable { onSelect(value) }, contentAlignment = Alignment.Center) {
                    Text("${value.toInt()}×", color = if (active) Color.Black else Color.White,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(String.format(java.util.Locale.US, "%.1f×", selected),
                color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 7.dp))
        }
        Box(Modifier.width(260.dp).height(52.dp)
            .background(Color(0xE91B1F28), RoundedCornerShape(26.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun update(x: Float) {
                        val normalized = ((x - 22.dp.toPx()) /
                            (size.width - 44.dp.toPx()).coerceAtLeast(1f)).coerceIn(0f, 1f)
                        val next = 1f + normalized * (latestMax - 1f)
                        val tick = (normalized * 24).toInt()
                        if (tick != lastTick) {
                            lastTick = tick
                            latestTick()
                        }
                        latestSelect(next)
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
            Canvas(Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 22.dp)) {
                val center = size.height / 2f
                drawLine(Color.White.copy(alpha = .28f),
                    Offset(0f, center), Offset(size.width, center), 3.dp.toPx())
                drawLine(accent, Offset(0f, center),
                    Offset(size.width * animated, center), 3.dp.toPx())
                for (i in 0..24) {
                    val x = size.width * i / 24f
                    drawLine(Color.White.copy(alpha = if (i / 24f <= animated) .85f else .28f),
                        Offset(x, center - if (i % 4 == 0) 8.dp.toPx() else 4.dp.toPx()),
                        Offset(x, center + if (i % 4 == 0) 8.dp.toPx() else 4.dp.toPx()),
                        1.dp.toPx())
                }
                drawCircle(accent, 9.dp.toPx(),
                    Offset(size.width * animated, center))
                drawCircle(Color.White, 3.dp.toPx(),
                    Offset(size.width * animated, center))
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
