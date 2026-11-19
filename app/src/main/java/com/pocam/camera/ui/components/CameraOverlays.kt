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
import androidx.compose.runtime.LaunchedEffect
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
    return PocamPalette.Orange
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
    selected: Float, minZoom: Float = 1f, maxZoom: Float, onSelect: (Float) -> Unit,
    onTick: () -> Unit = {}, modifier: Modifier = Modifier,
    onCommit: (Float) -> Unit = {}
) {
    val accent = cameraAccent()
    val haptics = LocalHapticFeedback.current
    val latestSelect by rememberUpdatedState(onSelect)
    val latestCommit by rememberUpdatedState(onCommit)
    val latestTick by rememberUpdatedState(onTick)
    var displayZoom by remember { mutableFloatStateOf(selected) }
    LaunchedEffect(selected, minZoom, maxZoom) {
        displayZoom = selected.coerceIn(minZoom, maxZoom.coerceAtLeast(minZoom))
    }
    val latestMax by rememberUpdatedState(maxZoom.coerceAtLeast(minZoom))
    val latestMin by rememberUpdatedState(minZoom)
    var lastStep by remember { androidx.compose.runtime.mutableIntStateOf(-1) }
    var lastWhole by remember { androidx.compose.runtime.mutableIntStateOf(-1) }
    val max = maxZoom.coerceAtLeast(minZoom + .01f)
    val fraction = ((displayZoom - minZoom) / (max - minZoom)).coerceIn(0f, 1f)
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.background(PocamPalette.Graphite.copy(alpha = .86f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 4.dp)) {
            Text(String.format(java.util.Locale.US, "%.1f×", displayZoom),
                color = accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Canvas(Modifier.fillMaxWidth().height(96.dp)
            
            .pointerInput(minZoom,maxZoom) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun update(x: Float) {
                        val pad = 46.dp.toPx()
                        val f = ((x - pad) / (size.width - 2f * pad).coerceAtLeast(1f))
                            .coerceIn(0f, 1f)
                        val raw = latestMin + f * (latestMax - latestMin)
                        val whole = kotlin.math.round(raw).toInt()
                        val snap = kotlin.math.abs(raw - whole) <= .055f
                        val next = if (snap) whole.toFloat() else raw
                        val step = (f * 70).toInt()
                        if (snap && whole != lastWhole) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            lastWhole = whole
                            latestTick()
                        } else if (step != lastStep && step % 10 == 0) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            latestTick()
                        }
                        lastStep = step
                        val clamped = next.coerceIn(latestMin, latestMax)
                        if (kotlin.math.abs(clamped - displayZoom) >= .002f) {
                            displayZoom = clamped
                            latestSelect(clamped)
                        }
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
                    latestCommit(displayZoom)
                }
            }) {
            val pad = 46.dp.toPx()
            val usable = (size.width - pad * 2f).coerceAtLeast(1f)
            // A true arched ruler: ticks, indicator and glass backing share the same path.
            val arch = 34.dp.toPx()
            fun arcY(f: Float): Float = size.height * .80f -
                arch * (1f - 4f * (f - .5f) * (f - .5f))
            val bandHalf = 14.dp.toPx()
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(pad, arcY(0f) - bandHalf)
                for (i in 1..80) {
                    val f = i / 80f
                    lineTo(pad + usable * f, arcY(f) - bandHalf)
                }
                for (i in 80 downTo 0) {
                    val f = i / 80f
                    lineTo(pad + usable * f, arcY(f) + bandHalf)
                }
                close()
            }
            drawPath(path, androidx.compose.ui.graphics.Brush.horizontalGradient(
                0f to Color.Transparent,
                .13f to Color(0xA51B1B23),
                .5f to Color(0xB51B1B23),
                .87f to Color(0xA51B1B23),
                1f to Color.Transparent
            ))
            for (i in 0..70) {
                val f = i / 70f
                val x = pad + usable * f
                val y = arcY(f)
                val major = i % 10 == 0
                val nearby = (1f - kotlin.math.abs(f - fraction) * 15f).coerceIn(0f, 1f)
                val h = (if (major) 17f else 8f) + nearby * 6f
                val edgeFade = (kotlin.math.min(f, 1f - f) * 6f).coerceIn(.12f, 1f)
                drawLine(
                    if (f <= fraction) accent.copy(alpha = (.5f + nearby * .4f) * edgeFade)
                    else Color.White.copy(alpha = .65f * edgeFade),
                    Offset(x, y - h.dp.toPx() / 2f),
                    Offset(x, y + h.dp.toPx() / 2f),
                    (if (major) 1.8f else 1.1f).dp.toPx()
                )
            }
            val x = pad + usable * fraction
            val y = arcY(fraction)
            drawCircle(accent, radius = 9.dp.toPx(), center = Offset(x, y))
            drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(x, y))}
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
    onDetent: () -> Unit = {},
    showLabel: Boolean = true,
    compact: Boolean = false
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
        if (showLabel) Text(label, color = accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Canvas(Modifier.fillMaxWidth().height(if (compact) 36.dp else 46.dp)
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
