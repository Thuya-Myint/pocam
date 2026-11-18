package com.pocam.camera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.pocam.camera.domain.model.CameraMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val wheelModes = listOf(CameraMode.PHOTO, CameraMode.VIDEO, CameraMode.PORTRAIT,
    CameraMode.NIGHT, CameraMode.MORE, CameraMode.PRO)

@Composable
fun RadialModeWheel(selected: CameraMode, modifier: Modifier = Modifier) {
    Box(modifier.size(248.dp).background(Color(0xD91A1B1F), CircleShape)
        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center) {
        wheelModes.forEachIndexed { index, mode ->
            val angle = (index * 60.0 - 90.0) * PI / 180.0
            val active = mode == selected
            Box(
                Modifier.offset(x = (91 * cos(angle)).dp, y = (91 * sin(angle)).dp)
                    .size(49.dp)
                    .background(if (active) Color.White else Color(0xFF303137), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(modeSymbol(mode), color = if (active) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold)
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
