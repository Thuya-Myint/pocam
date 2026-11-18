package com.pocam.camera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import android.os.Build
import androidx.compose.runtime.Composable
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
    val context = LocalContext.current
    val accent = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(context).primary
        else Color(0xFFFFD88C)
    }
    Column(modifier.widthIn(max = 340.dp).fillMaxWidth()
        .background(Color(0xF21B1E26), RoundedCornerShape(24.dp))
        .border(1.dp, Color.White.copy(alpha = .14f), RoundedCornerShape(24.dp))
        .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("SLIDE TO CHOOSE MODE", color = accent,
            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically) {
            wheelModes.forEach { mode ->
                val active = mode == selected
                Column(Modifier.weight(1f).background(
                    if (active) accent else Color(0xFF303540),
                    RoundedCornerShape(12.dp))
                    .padding(horizontal = 1.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(modeSymbol(mode), color = if (active) Color.Black else Color.White,
                        fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(mode.name.take(5), color = if (active) Color.Black else Color.White,
                        fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
