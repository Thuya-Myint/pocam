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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.abs

/**
 * The five Pocam modes sit inside a genuinely rounded curved ribbon.
 * Unlike the earlier version, there are NO vertical/square end walls:
 * both ends are smoothly capped with quadratic segments.
 * Only the active pill animates; geometry and text paint remain cached.
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
    val animatedIndex by animateFloatAsState(
        targetValue = activeIndex.toFloat(),
        animationSpec = if (motionEnabled) tween(190, easing = FastOutSlowInEasing)
            else snap(),
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
        modifier.fillMaxWidth().height(86.dp)
            .pointerInput(Unit) {
                detectTapGestures { point ->
                    val fraction = (point.x / size.width.coerceAtLeast(1).toFloat())
                        .coerceIn(0f, 1f)
                    val index = (fraction * modes.size).toInt()
                        .coerceIn(0, modes.lastIndex)
                    val next = modes[index]
                    if (next != latestSelected) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        latestSelect(next)
                    }
                }
            }
            .drawWithCache {
                val pad = 7.dp.toPx()
                val width = (size.width - 2f * pad).coerceAtLeast(1f)
                val rise = 21.dp.toPx()
                val edge = 25.dp.toPx()
                val band = 43.dp.toPx()
                val capRadius = 13.dp.toPx().coerceAtMost(width / 8f)
                val capFraction = capRadius / width
                val left = pad + capRadius
                val right = pad + width - capRadius
                val mid = pad + width / 2f

                // Arc is a quadratic in normalized x, rising smoothly at center.
                fun arc(f: Float) = edge - rise * 4f * f * (1f - f)
                val topAtCap = arc(capFraction)
                val topControl = 2f * arc(.5f) - topAtCap
                val ribbon = Path().apply {
                    moveTo(left, topAtCap)
                    quadraticBezierTo(mid, topControl, right, topAtCap)
                    // Round right end instead of a hard vertical closing edge.
                    quadraticBezierTo(pad + width, edge + band / 2f,
                        right, topAtCap + band)
                    quadraticBezierTo(mid, topControl + band,
                        left, topAtCap + band)
                    // Round left end as well.
                    quadraticBezierTo(pad, edge + band / 2f, left, topAtCap)
                    close()
                }
                val topEdge = Path().apply {
                    moveTo(left, topAtCap)
                    quadraticBezierTo(mid, topControl, right, topAtCap)
                }
                val bottomEdge = Path().apply {
                    moveTo(left, topAtCap + band)
                    quadraticBezierTo(mid, topControl + band,
                        right, topAtCap + band)
                }
                fun center(index: Float): Offset {
                    val f = ((index + .5f) / modes.size).coerceIn(0f, 1f)
                    return Offset(pad + width * f, arc(f) + band / 2f)
                }
                fun tangent(index: Float): Float {
                    val f = ((index + .5f) / modes.size).coerceIn(0f, 1f)
                    return Math.toDegrees(
                        atan((rise * 8f * (f - .5f) / width).toDouble())
                    ).toFloat()
                }
                val centers = modes.indices.map { center(it.toFloat()) }
                val angles = modes.indices.map { tangent(it.toFloat()) }
                val ribbonBrush = Brush.horizontalGradient(
                    0f to Color(0x5C101115),
                    .10f to Color(0xDC121319),
                    .5f to Color(0xF0111319),
                    .90f to Color(0xDC121319),
                    1f to Color(0x5C101115)
                )
                val topLineBrush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    .20f to Color.White.copy(alpha = .42f),
                    .5f to Color.White.copy(alpha = .68f),
                    .80f to Color.White.copy(alpha = .42f),
                    1f to Color.Transparent
                )
                val selectedBrush = Brush.verticalGradient(
                    listOf(Color(0xFFFF9637), PocamPalette.Orange)
                )
                val paint = labelPaint.apply { textSize = 10.sp.toPx() }
                val pillWidth = (width / modes.size * .79f).coerceAtMost(61.dp.toPx())
                val pillHeight = 30.dp.toPx()

                onDrawBehind {
                    drawPath(ribbon, ribbonBrush)
                    // Two shallow silver contours reproduce the camera dial.
                    drawPath(topEdge, topLineBrush,
                        style = Stroke(width = .85.dp.toPx(), cap = StrokeCap.Round))
                    drawPath(bottomEdge, Color.White.copy(alpha = .12f),
                        style = Stroke(width = .65.dp.toPx(), cap = StrokeCap.Round))

                    // The selection remains on the same parabola at every frame.
                    val center = center(animatedIndex)
                    rotate(tangent(animatedIndex), center) {
                        drawRoundRect(
                            brush = selectedBrush,
                            topLeft = Offset(center.x - pillWidth / 2f,
                                center.y - pillHeight / 2f),
                            size = Size(pillWidth, pillHeight),
                            cornerRadius = CornerRadius(pillHeight / 2f)
                        )
                    }

                    val native = drawContext.canvas.nativeCanvas
                    modes.forEachIndexed { i, mode ->
                        paint.color = if (abs(animatedIndex - i.toFloat()) < .43f)
                            Color.Black.toArgb() else Color.White.toArgb()
                        paint.alpha = if (i == 0 || i == modes.lastIndex) 205 else 255
                        val point = centers[i]
                        val save = native.save()
                        native.rotate(angles[i], point.x, point.y)
                        native.drawText(mode.name, point.x,
                            point.y - (paint.ascent() + paint.descent()) / 2f,
                            paint)
                        native.restoreToCount(save)
                    }
                }
            }
    )
}
