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
    val safeMax = maxZoom.coerceAtLeast(1.01f)
    val latestSelect by rememberUpdatedState(onSelect)
    val latestTick by rememberUpdatedState(onTick)
    val latestHaptics by rememberUpdatedState(haptics)
    val latestMax by rememberUpdatedState(safeMax)
    var lastTick by remember { androidx.compose.runtime.mutableIntStateOf(-1) }
    var lastWhole by remember { androidx.compose.runtime.mutableIntStateOf(-1) }
    val fraction = ((selected - 1f) / (safeMax - 1f)).coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        fraction,
        androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = 320f
        ),
        label = "zoom dial"
    )

    // Transparent zoom ruler: fixed accent pointer, no dark label pill or dot.
    // The scale is shortened to 80% of its previous visual width.
    Box(modifier.fillMaxWidth().height(116.dp), contentAlignment = Alignment.TopCenter) {
        Canvas(
            Modifier.fillMaxWidth(.64f).height(100.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        fun update(x: Float) {
                            val edge = 18.dp.toPx()
                            val normalized = ((x - edge) /
                                (size.width - edge * 2f).coerceAtLeast(1f)).coerceIn(0f, 1f)
                            val rawZoom = 1f + normalized * (latestMax - 1f)
                            val nearest = kotlin.math.round(rawZoom).toInt()
                            // A small magnetic detent at whole-number zoom levels.
                            // Decimals remain selectable once the finger moves past the detent.
                            val nearWhole = nearest >= 1 && nearest <= latestMax &&
                                kotlin.math.abs(rawZoom - nearest) <= .055f
                            val next = if (nearWhole) nearest.toFloat() else rawZoom
                            val tick = (normalized * 100f).toInt()
                            if (nearWhole && nearest != lastWhole) {
                                latestHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                lastWhole = nearest
                            } else if (!nearWhole && tick != lastTick && tick % 4 == 0) {
                                latestHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            if (tick != lastTick) {
                                lastTick = tick
                                if (tick % 4 == 0 || nearWhole) latestTick()
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
                }
        ) {
            val cx = size.width / 2f
            val radius = size.width * .49f
            val cy = radius + 9.dp.toPx()
            // Draw white tick marks on the arc, fading gradually at either edge.
            for (i in 0..100) {
                val angle = Math.toRadians((198f + 144f * i / 100f).toDouble())
                val edge = (kotlin.math.min(i, 100 - i) / 17f).coerceIn(0f, 1f)
                val fade = (.07f + .93f * edge * edge * (3f - 2f * edge))
                val major = i % 10 == 0
                val distance = kotlin.math.abs(i / 100f - animated)
                // Only the nearest five rods grow smoothly around the current zoom.
                val proximity = (1f - distance * 100f / 3f).coerceIn(0f, 1f)
                val length = (if (major) 21f else 12f) + 9f * proximity
                val rOuter = radius
                val rInner = radius - length.dp.toPx()
                val cosine = kotlin.math.cos(angle).toFloat()
                val sine = kotlin.math.sin(angle).toFloat()
                val tint = if (proximity > .01f) {
                    accent.copy(alpha = (.15f + .25f * proximity) * fade)
                } else Color.White.copy(alpha = .65f * fade)
                drawLine(
                    tint,
                    Offset(cx + rInner * cosine, cy + rInner * sine),
                    Offset(cx + rOuter * cosine, cy + rOuter * sine),
                    (if (major) 1.9f else 1.1f).dp.toPx() + 1.1.dp.toPx() * proximity,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
            // Fixed center pointer, independent of the scrolling zoom position.
            val pointerY = cy - radius
            val triangle = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx - 6.dp.toPx(), pointerY - 10.dp.toPx())
                lineTo(cx + 6.dp.toPx(), pointerY - 10.dp.toPx())
                lineTo(cx, pointerY - 2.dp.toPx())
                close()
            }
            drawPath(triangle, accent)
            drawLine(
                accent,
                Offset(cx, pointerY + 1.dp.toPx()),
                Offset(cx, pointerY + 20.dp.toPx()),
                2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
        Text(
            String.format(java.util.Locale.US, "%.1f×", selected),
            color = accent,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 53.dp)
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
