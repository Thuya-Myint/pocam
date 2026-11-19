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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

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
    val haptics = LocalHapticFeedback.current
    val latestSelect by rememberUpdatedState(onSelect)
    val latestTick by rememberUpdatedState(onTick)
    val latestMax by rememberUpdatedState(maxZoom.coerceAtLeast(1f))
    var lastStep by remember { androidx.compose.runtime.mutableIntStateOf(-1) }
    var lastWhole by remember { androidx.compose.runtime.mutableIntStateOf(-1) }
    val minZoom = 1f
    val max = maxZoom.coerceAtLeast(minZoom + .01f)
    val fraction = ((selected - minZoom) / (max - minZoom)).coerceIn(0f, 1f)
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.background(Color(0xE833293D), CircleShape)
            .padding(horizontal = 14.dp, vertical = 4.dp)) {
            Text(String.format(java.util.Locale.US, "%.1f×", selected),
                color = accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Canvas(Modifier.fillMaxWidth().height(44.dp)
            .background(Color(0xD91B1B23), CircleShape)
            .pointerInput(maxZoom) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun update(x: Float) {
                        val pad = 46.dp.toPx()
                        val f = ((x - pad) / (size.width - 2f * pad).coerceAtLeast(1f))
                            .coerceIn(0f, 1f)
                        val raw = 1f + f * (latestMax - 1f)
                        val whole = kotlin.math.round(raw).toInt()
                        val snap = kotlin.math.abs(raw - whole) <= .055f
                        val next = if (snap) whole.toFloat() else raw
                        val step = (f * 70).toInt()
                        if (snap && whole != lastWhole) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            lastWhole = whole
                            latestTick()
                        } else if (step != lastStep && step % 4 == 0) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            latestTick()
                        }
                        lastStep = step
                        latestSelect(next.coerceIn(1f, latestMax))
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
            }) {
            val pad = 46.dp.toPx()
            val usable = (size.width - pad * 2f).coerceAtLeast(1f)
            val cy = size.height / 2f
            for (i in 0..70) {
                val f = i / 70f
                val x = pad + usable * f
                val major = i % 10 == 0
                val nearby = (1f - kotlin.math.abs(f - fraction) * 15f).coerceIn(0f, 1f)
                val h = (if (major) 18f else 9f) + nearby * 7f
                drawLine(if (f <= fraction) accent.copy(alpha = .55f + nearby * .4f)
                    else Color.White.copy(alpha = .6f),
                    Offset(x, cy - h.dp.toPx() / 2f),
                    Offset(x, cy + h.dp.toPx() / 2f),
                    (if (major) 1.8f else 1.1f).dp.toPx())
            }
            val x = pad + usable * fraction
            drawCircle(accent, radius = 9.dp.toPx(), center = Offset(x, cy))
            drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(x, cy))
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

/** Flat, zoom-inspired ruler for exposure and Pro controls. */
@Composable
fun StraightRuler(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onDetent: () -> Unit = {}
) {
    val accent = cameraAccent()
    val haptics = LocalHapticFeedback.current
    val currentSelect by rememberUpdatedState(onValueChange)
    val currentDetent by rememberUpdatedState(onDetent)
    val currentHaptics by rememberUpdatedState(haptics)
    val min = valueRange.start
    val max = valueRange.endInclusive.coerceAtLeast(min + .0001f)
    val fraction = ((value - min) / (max - min)).coerceIn(0f, 1f)
    var previousDetent by remember { androidx.compose.runtime.mutableIntStateOf(-1) }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Canvas(Modifier.fillMaxWidth().height(46.dp)
            .pointerInput(min, max) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun update(x: Float) {
                        val margin = 12.dp.toPx()
                        val f = ((x - margin) /
                            (size.width - margin * 2f).coerceAtLeast(1f)).coerceIn(0f, 1f)
                        val raw = min + f * (max - min)
                        val nearest = kotlin.math.round(raw)
                        val snap = kotlin.math.abs(raw - nearest) <= (max - min) * .012f
                        val next = if (snap) nearest.coerceIn(min, max) else raw
                        val detent = kotlin.math.round(f * 20f).toInt()
                        if (detent != previousDetent) {
                            previousDetent = detent
                            currentHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentDetent()
                        }
                        currentSelect(next)
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
            }) {
            val pad = 12.dp.toPx()
            val width = size.width - 2f * pad
            val centerY = size.height * .58f
            for (i in 0..60) {
                val x = pad + width * i / 60f
                val near = (1f - kotlin.math.abs(i / 60f - fraction) * 12f).coerceIn(0f, 1f)
                val major = i % 10 == 0
                val height = (if (major) 19f else 11f) + 8f * near
                drawLine(
                    if (near > 0f) accent.copy(alpha = .4f + .6f * near)
                    else Color.White.copy(alpha = .58f),
                    Offset(x, centerY - height.dp.toPx() / 2f),
                    Offset(x, centerY + height.dp.toPx() / 2f),
                    (if (major) 2f else 1.2f).dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
            val selectedX = pad + width * fraction
            drawLine(accent, Offset(selectedX, 2.dp.toPx()),
                Offset(selectedX, size.height - 2.dp.toPx()),
                2.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}
