package com.pocam.camera.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocam.camera.domain.model.CameraMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val wheelModes = listOf(CameraMode.PHOTO, CameraMode.VIDEO, CameraMode.PORTRAIT,
    CameraMode.NIGHT, CameraMode.MORE, CameraMode.PRO)

@Composable
fun RadialModeWheel(selected: CameraMode, modifier: Modifier = Modifier) {
    val entrance = animateFloatAsState(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "wheel entrance")
    Box(modifier.scale(entrance.value).size(248.dp)
        .shadow(18.dp, CircleShape)
        .background(Brush.radialGradient(listOf(Color(0xF12C303A), Color(0xF813151A))), CircleShape)
        .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape),
        contentAlignment = Alignment.Center) {
        wheelModes.forEachIndexed { index, mode ->
            val angle = (index * 60.0 - 90.0) * PI / 180.0
            val active = mode == selected
            val selectedScale = animateFloatAsState(if (active) 1.18f else 1f, spring(dampingRatio = Spring.DampingRatioLowBouncy), label = "mode highlight")
            Box(
                Modifier.offset(x = (91 * cos(angle)).dp, y = (91 * sin(angle)).dp)
                    .scale(selectedScale.value)
                    .size(49.dp)
                    .background(if (active) Color.White else Color(0xFF343945), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(modeSymbol(mode), color = if (active) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Text(mode.name.take(5), color = if (active) Color.Black else Color.White,
                        fontSize = 7.sp)
                }
            }
        }
    }
}

fun modeSymbol(mode: CameraMode): String = when (mode) {
    CameraMode.PHOTO -> "◎"
    CameraMode.PRO -> "☷"
    CameraMode.VIDEO -> "▣"
    CameraMode.PORTRAIT -> "♙"
    CameraMode.NIGHT -> "☾"
    CameraMode.MORE -> "⊞"
}
