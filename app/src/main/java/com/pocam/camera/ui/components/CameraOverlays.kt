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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
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
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.size(64.dp).border(1.5.dp, Color(0xFFFFD36A),
            RoundedCornerShape(12.dp)))
        Box(Modifier.size(4.dp).background(Color(0xFFFFD36A), CircleShape))
    }
}

@Composable
fun ZoomRail(selected: Float, onSelect: (Float) -> Unit, modifier: Modifier = Modifier) {
    var dragAccumulated by remember { mutableFloatStateOf(0f) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(Modifier.background(Color(0xE91B1F28), RoundedCornerShape(30.dp))
        .padding(horizontal = 5.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        listOf(1f, 2f, 4f).forEach { value ->
            val active = value == selected
            val scale by animateFloatAsState(if (active) 1.04f else 1f,
                tween(180, easing = FastOutSlowInEasing), label = "zoom pill")
            Box(Modifier.size(38.dp).scale(scale)
                .background(if (active) Color(0xFFFFD88C) else Color.Transparent, CircleShape)
                .clickable { onSelect(value) }, contentAlignment = Alignment.Center) {
                Text("${value.toInt()}×", color = if (active) Color.Black else Color.White,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    Row(Modifier.width(150.dp).height(20.dp)
        .pointerInput(selected) {
            detectHorizontalDragGestures(
                onDragStart = { dragAccumulated = 0f },
                onDragEnd = { dragAccumulated = 0f },
                onHorizontalDrag = { change, amount ->
                    change.consume()
                    dragAccumulated += amount
                    if (kotlin.math.abs(dragAccumulated) > 28.dp.toPx()) {
                        val values = listOf(1f, 2f, 4f)
                        val current = values.indexOf(selected).coerceAtLeast(0)
                        val direction = if (dragAccumulated > 0f) 1 else -1
                        onSelect(values[(current + direction).coerceIn(0, values.lastIndex)])
                        dragAccumulated = 0f
                    }
                })
        }, horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically) {
        Text("SWIPE TO ZOOM", color = Color.White.copy(alpha = .8f),
            fontSize = 9.sp, letterSpacing = 1.sp)
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
