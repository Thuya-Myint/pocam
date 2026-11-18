package com.pocam.camera.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocam.camera.domain.model.CameraMode
import kotlin.math.cos
import kotlin.math.sin

val wheelModes = listOf(CameraMode.PHOTO, CameraMode.VIDEO, CameraMode.PORTRAIT,
    CameraMode.NIGHT, CameraMode.MORE, CameraMode.PRO)

// Icons occupy six equal sectors of a circular dial, starting at twelve o'clock.
@Composable
fun RadialModeWheel(selected: CameraMode, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val accent = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(context).primary
        else Color(0xFFFFD88C)
    }
    val selectedIndex = wheelModes.indexOf(selected).coerceAtLeast(0)
    var targetRotation by remember { mutableFloatStateOf(-selectedIndex * 60f) }
    LaunchedEffect(selectedIndex) {
        val currentSector = ((-targetRotation / 60f).toInt() % wheelModes.size + wheelModes.size) % wheelModes.size
        val forward = (selectedIndex - currentSector + wheelModes.size) % wheelModes.size
        val steps = if (forward <= wheelModes.size / 2) forward else forward - wheelModes.size
        targetRotation -= steps * 60f
    }
    val rotation by animateFloatAsState(targetRotation, spring(stiffness = 260f), label = "mode carousel rotation")
    Box(modifier.requiredSize(252.dp),
        contentAlignment = Alignment.Center) {
        wheelModes.forEachIndexed { index, mode ->
            val angle = Math.toRadians((index * 60 - 90 + rotation).toDouble())
            val x = (88.0 * cos(angle)).toFloat().dp
            val y = (88.0 * sin(angle)).toFloat().dp
            val active = selected == mode
            val visibility = (1.0 - 0.35 * ((sin(angle) + 0.15) / 1.15).coerceIn(0.0, 1.0)).toFloat()
            Box(Modifier.offset(x = x, y = y).size(52.dp).alpha(visibility)
                .background(if (active) accent else Color(0xFF343944), CircleShape)
                .border(1.dp, if (active) accent else Color.White.copy(alpha = .18f), CircleShape),
                contentAlignment = Alignment.Center) {
                Text(modeSymbol(mode),
                    color = if (active) Color.Black else Color.White,
                    fontSize = 25.sp, fontWeight = FontWeight.Bold)
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
