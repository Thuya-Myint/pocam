package com.pocam.camera.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocam.camera.domain.model.CameraMode
import kotlin.math.atan
import kotlin.math.min

/**
 * One geometry for the ribbon, its labels, and its selection. Both ends gently
 * fade out. The active indicator follows the curve instead of sitting on a Row.
 */
@Composable
fun CurvedModeSelector(
    selected: CameraMode,
    hovered: CameraMode? = null,
    onSelect: (CameraMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = PocamPalette.Orange
    val modes = remember {
        listOf(CameraMode.VIDEO, CameraMode.NIGHT, CameraMode.PHOTO,
            CameraMode.PRO, CameraMode.PORTRAIT)
    }
    val activeIndex = modes.indexOf(hovered ?: selected).coerceAtLeast(0)
    val animatedIndex by animateFloatAsState(
        targetValue = activeIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "mode-selection"
    )
    val latestSelect by rememberUpdatedState(onSelect)
    val latestSelected by rememberUpdatedState(selected)
    val haptics = LocalHapticFeedback.current
    // Native Paint and Typeface are expensive to allocate every animation frame.
    val labelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
    }

    Canvas(modifier.fillMaxWidth().height(116.dp)
        .pointerInput(Unit) {
            detectTapGestures { offset ->
                val index = ((offset.x / size.width.toFloat().coerceAtLeast(1f)) * modes.size)
                    .toInt().coerceIn(0, modes.lastIndex)
                if (modes[index] != latestSelected) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    latestSelect(modes[index])
                }
            }
        }) {
        val pad = 4.dp.toPx()
        val trackWidth = (size.width - pad * 2f).coerceAtLeast(1f)
        val curveHeight = 41.dp.toPx()
        val top = 7.dp.toPx()
        val band = 48.dp.toPx()
        fun arc(t: Float): Float =
            top + curveHeight * 4f * (t - .5f) * (t - .5f)

        val ribbon = Path().apply {
            moveTo(pad, arc(0f))
            for (i in 1..80) {
                val fraction = i / 80f
                lineTo(pad + trackWidth * fraction, arc(fraction))
            }
            for (i in 80 downTo 0) {
                val fraction = i / 80f
                lineTo(pad + trackWidth * fraction, arc(fraction) + band)
            }
            close()
        }
        drawPath(ribbon, Brush.horizontalGradient(
            0f to Color.Transparent,
            .12f to Color(0xB11A1C24),
            .50f to Color(0xC61A1C24),
            .88f to Color(0xB11A1C24),
            1f to Color.Transparent
        ))

        fun itemCenter(index: Float): Offset {
            val fraction = ((index + .5f) / modes.size).coerceIn(0f, 1f)
            return Offset(pad + trackWidth * fraction, arc(fraction) + band / 2f)
        }
        fun itemAngle(index: Float): Float {
            val fraction = ((index + .5f) / modes.size).coerceIn(0f, 1f)
            return Math.toDegrees(
                atan((8f * curveHeight * (fraction - .5f) / trackWidth).toDouble())
            ).toFloat()
        }

        val selectionCenter = itemCenter(animatedIndex)
        val pillWidth = (trackWidth / modes.size * 0.93f)
            .coerceAtMost(86.dp.toPx())
        val pillHeight = 36.dp.toPx()
        rotate(itemAngle(animatedIndex), selectionCenter) {
            drawRoundRect(
                color = accent.copy(alpha = .95f),
                topLeft = Offset(
                    selectionCenter.x - pillWidth / 2f,
                    selectionCenter.y - pillHeight / 2f
                ),
                size = Size(pillWidth, pillHeight),
                cornerRadius = CornerRadius(pillHeight / 2f)
            )
        }

        val paint = labelPaint
        paint.textSize = 11.sp.toPx()
        val native = drawContext.canvas.nativeCanvas
        modes.forEachIndexed { index, candidate ->
            val center = itemCenter(index.toFloat())
            val fade = (min((index + .5f) / modes.size,
                1f - (index + .5f) / modes.size) * 6f).coerceIn(.35f, 1f)
            paint.color =
                if (candidate == (hovered ?: selected)) Color.Black.toArgb()
                else Color.White.toArgb()
            paint.alpha = (255f * fade).toInt()
            val save = native.save()
            native.rotate(itemAngle(index.toFloat()), center.x, center.y)
            native.drawText(
                candidate.name,
                center.x,
                center.y - (paint.ascent() + paint.descent()) / 2f,
                paint
            )
            native.restoreToCount(save)
        }
    }
}
