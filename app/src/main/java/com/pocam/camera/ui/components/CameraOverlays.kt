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
import androidx.compose.ui.draw.drawWithCache
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

/**
 * Lightweight Pro zoom arc. The quadratic track and the tick geometry are
 * cached per size; zoom changes only invalidate drawing of the indicator.
 * Avoids thousands of small Path allocations while dragging.
 */
@Composable
fun ZoomRail(
    selected: Float, minZoom: Float = 1f, maxZoom: Float, onSelect: (Float) -> Unit,
    onTick: () -> Unit = {}, modifier: Modifier = Modifier,
    onCommit: (Float) -> Unit = {}
) {
    val latestSelect by rememberUpdatedState(onSelect)
    val latestCommit by rememberUpdatedState(onCommit)
    val latestTick by rememberUpdatedState(onTick)
    val haptics = LocalHapticFeedback.current
    var displayZoom by remember { mutableFloatStateOf(selected) }
    var previousDetent by remember { androidx.compose.runtime.mutableIntStateOf(-1) }
    LaunchedEffect(selected, minZoom, maxZoom) {
        displayZoom = selected.coerceIn(minZoom, maxZoom.coerceAtLeast(minZoom))
    }
    val maximum = maxZoom.coerceAtLeast(minZoom + 0.01f)

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Box(
            Modifier.background(Color(0xDB17191F), CircleShape)
                .border(.6.dp, Color.White.copy(alpha=.28f), CircleShape)
                .padding(horizontal = 12.dp, vertical = 3.dp)
        ) {
            Text(String.format(java.util.Locale.US, "%.1f×", displayZoom),
                color = Color.White, fontWeight = FontWeight.Medium, fontSize = 11.sp)
        }
        Box(
            Modifier.fillMaxWidth().height(72.dp)
                .pointerInput(minZoom, maximum) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        fun update(x: Float) {
                            val pad = 29.dp.toPx()
                            val f = ((x - pad) /
                                (size.width - 2f * pad).coerceAtLeast(1f)).coerceIn(0f, 1f)
                            val value = minZoom + (maximum - minZoom) * f
                            val next = value.coerceIn(minZoom, maximum)
                            if (kotlin.math.abs(next - displayZoom) >= .008f) {
                                displayZoom = next
                                latestSelect(next)
                            }
                            val detent = (next * 2f).toInt()
                            if (detent != previousDetent) {
                                previousDetent = detent
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                latestTick()
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
                }
                .drawWithCache {
                    val pad = 29.dp.toPx()
                    val usable = (size.width - pad * 2f).coerceAtLeast(1f)
                    val edge = size.height * .71f
                    val rise = 21.dp.toPx()
                    val halfBand = 11.dp.toPx()
                    fun y(f: Float) = edge - rise * 4f * f * (1f - f)
                    val bandPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(pad, edge - halfBand)
                        quadraticBezierTo(size.width/2f, edge - rise*2f - halfBand,
                            pad + usable, edge - halfBand)
                        lineTo(pad + usable, edge + halfBand)
                        quadraticBezierTo(size.width/2f, edge - rise*2f + halfBand,
                            pad, edge + halfBand)
                        close()
                    }
                    val ticks = (0..60).map { i ->
                        val f = i / 60f
                        Offset(pad + usable * f, y(f))
                    }
                    val glass = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        0f to Color.Transparent,
                        .13f to Color(0xB014151B),
                        .5f to Color(0xDB14151B),
                        .87f to Color(0xB014151B),
                        1f to Color.Transparent)
                    onDrawBehind {
                        drawPath(bandPath, glass)
                        ticks.forEachIndexed { i, pos ->
                            val major = i % 10 == 0
                            val length = (if (major) 15 else 7).dp.toPx()
                            drawLine(Color.White.copy(alpha=if (major) .88f else .67f),
                                Offset(pos.x, pos.y - length / 2f),
                                Offset(pos.x, pos.y + length / 2f),
                                (if (major) 1.45f else .85f).dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        }
                        val f = ((displayZoom - minZoom) / (maximum - minZoom))
                            .coerceIn(0f, 1f)
                        val point = Offset(pad + usable * f, y(f))
                        drawCircle(PocamPalette.Orange, 8.dp.toPx(), point)
                        drawCircle(Color.White, 4.8.dp.toPx(), point)
                    }
                }
        )
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
