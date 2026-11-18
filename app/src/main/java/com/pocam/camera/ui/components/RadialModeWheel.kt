package com.pocam.camera.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocam.camera.domain.model.CameraMode

val wheelModes = listOf(CameraMode.PHOTO, CameraMode.VIDEO, CameraMode.PORTRAIT,
    CameraMode.NIGHT, CameraMode.MORE, CameraMode.PRO)

@Composable
fun RadialModeWheel(selected: CameraMode, modifier: Modifier = Modifier) {
    Column(modifier.widthIn(max = 340.dp)
        .background(Color(0xF21B1E26), RoundedCornerShape(24.dp))
        .border(1.dp, Color.White.copy(alpha = .14f), RoundedCornerShape(24.dp))
        .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("SLIDE TO CHOOSE MODE", color = Color(0xFFFFD88C),
            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            wheelModes.forEach { mode ->
                val active = mode == selected
                val emphasis by animateFloatAsState(if (active) 1f else .75f,
                    spring(), label = "mode emphasis")
                Column(Modifier.background(
                    if (active) Color(0xFFFFD88C) else Color(0xFF303540),
                    RoundedCornerShape(12.dp))
                    .padding(horizontal = 7.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(modeSymbol(mode), color = if (active) Color.Black else Color.White,
                        fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(mode.name.take(5), color = if (active) Color.Black else Color.White,
                        fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text("RELEASE TO SELECT  ·  HOLD TO KEEP", color = Color(0xFFB6BAC5),
            fontSize = 9.sp)
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
