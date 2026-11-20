package com.pocam.camera.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocam.camera.domain.model.CameraMode
import kotlin.math.atan

/**
 * Pocam reference selector: a thin continuous quadratic ribbon with five
 * equally spaced modes. Its selected pill glides on the cached path,
 * without animating the preview or remeasuring the screen.
 */
@Composable
fun CurvedModeSelector(
    selected: CameraMode,
    hovered: CameraMode? = null,
    onSelect: (CameraMode) -> Unit,
    modifier: Modifier = Modifier,
    motionEnabled: Boolean = true
) {
    val modes = remember {
        listOf(CameraMode.VIDEO, CameraMode.NIGHT, CameraMode.PHOTO,
            CameraMode.PRO, CameraMode.PORTRAIT)
    }
    val active = hovered ?: selected
    val activeIndex = modes.indexOf(active).coerceAtLeast(0)
    // Only the orange selection pill glides; the expensive ribbon geometry
    // stays cached and the camera preview is never animated or rebound.
    val animatedIndex by animateFloatAsState(
        targetValue = activeIndex.toFloat(),
        animationSpec = if (motionEnabled) tween(190, easing = FastOutSlowInEasing) else snap(),
        label = "pocam-mode-glide"
    )
    val latestSelect by rememberUpdatedState(onSelect)
    val latestSelected by rememberUpdatedState(selected)
    val haptics = LocalHapticFeedback.current
    val labelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
    }

    Box(
        modifier.fillMaxWidth().height(77.dp)
            .pointerInput(Unit) {
                detectTapGestures { point ->
                    val i = ((point.x / size.width.coerceAtLeast(1).toFloat()) *
                        modes.size).toInt().coerceIn(0, modes.lastIndex)
                    val next = modes[i]
                    if (next != latestSelected) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        latestSelect(next)
                    }
                }
            }
            .drawWithCache {
                val pad = 4.dp.toPx()
                val width = (size.width - pad * 2f).coerceAtLeast(1f)
                val rise = 17.dp.toPx()
                val edge = 28.dp.toPx()
                val band = 33.dp.toPx()
                fun y(f: Float) = edge - rise * 4f * f * (1f - f)
                fun center(index: Int): Offset {
                    val f = (index + .5f) / modes.size
                    return Offset(pad + width * f, y(f) + band/2f)
                }
                fun rotation(index: Float): Float {
                    val f = (index + .5f) / modes.size
                    return Math.toDegrees(
                        atan((rise * 8f * (f - .5f) / width).toDouble())
                    ).toFloat()
                }
                val ribbon = Path().apply {
                    moveTo(pad, edge)
                    quadraticBezierTo(size.width/2f, edge - 2f * rise, pad + width, edge)
                    lineTo(pad + width, edge + band)
                    quadraticBezierTo(size.width/2f, edge - 2f * rise + band, pad, edge + band)
                    close()
                }
                val centers = modes.indices.map(::center)
                val angles = modes.indices.map { rotation(it.toFloat()) }
                val brush = Brush.horizontalGradient(
                    0f to Color(0x2D101115),
                    .12f to Color(0xB313151B),
                    .5f to Color(0xE013151B),
                    .88f to Color(0xB313151B),
                    1f to Color(0x2D101115))
                val nativePaint = labelPaint.apply { textSize = 10.sp.toPx() }

                onDrawBehind {
                    drawPath(ribbon, brush)
                    val movingFraction = ((animatedIndex + .5f) / modes.size).coerceIn(0f, 1f)
                    val selectedCenter = Offset(
                        pad + width * movingFraction,
                        y(movingFraction) + band / 2f
                    )
                    val pillWidth = (width / modes.size * .76f).coerceAtMost(59.dp.toPx())
                    val pillHeight = 27.dp.toPx()
                    rotate(rotation(animatedIndex), selectedCenter) {
                        drawRoundRect(
                            color = PocamPalette.Orange,
                            topLeft = Offset(selectedCenter.x - pillWidth / 2f,
                                selectedCenter.y - pillHeight / 2f),
                            size = Size(pillWidth, pillHeight),
                            cornerRadius = CornerRadius(pillHeight/2f)
                        )
                    }
                    val native = drawContext.canvas.nativeCanvas
                    modes.forEachIndexed { i, mode ->
                        nativePaint.color = if (mode == active) Color.Black.toArgb()
                            else Color.White.toArgb()
                        nativePaint.alpha = if (i == 0 || i == modes.lastIndex) 195 else 255
                        val point = centers[i]
                        val save = native.save()
                        native.rotate(angles[i], point.x, point.y)
                        native.drawText(mode.name, point.x,
                            point.y - (nativePaint.ascent() + nativePaint.descent())/2f,
                            nativePaint)
                        native.restoreToCount(save)
                    }
                }
            }
    )
}
