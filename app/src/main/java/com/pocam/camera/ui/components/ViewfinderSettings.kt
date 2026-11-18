package com.pocam.camera.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ViewfinderSettings(
    grid: Boolean,
    onGrid: (Boolean) -> Unit,
    level: Boolean,
    onLevel: (Boolean) -> Unit,
    torch: Boolean,
    torchAvailable: Boolean,
    onTorch: (Boolean) -> Unit,
    timer: Int,
    onTimer: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accent = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(context).primary
        else Color(0xFFFFD88C)
    }
    Column(modifier.background(Color(0xF722252D), RoundedCornerShape(22.dp))
        .animateContentSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("CAMERA SETTINGS", color = Color.White, fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp, fontSize = 12.sp)
            Text("DONE", color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                modifier = Modifier.clickable(onClick = onClose))
        }
        HorizontalDivider(color = Color.White.copy(alpha = .12f))
        SettingsSwitch("Composition grid", grid, accent = accent) { onGrid(!grid) }
        SettingsSwitch("Center crosshair", level, accent = accent) { onLevel(!level) }
        SettingsSwitch("Live torch", torch, torchAvailable, accent) { onTorch(!torch) }
        Text("CAPTURE TIMER", color = Color.LightGray, fontSize = 11.sp,
            letterSpacing = 1.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 3, 5, 10).forEach { seconds ->
                Text(if (seconds == 0) "OFF" else "${seconds}s",
                    color = if (timer == seconds) Color.Black else Color.White,
                    fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                    modifier = Modifier.background(
                        if (timer == seconds) accent else Color(0xFF383C46),
                        RoundedCornerShape(12.dp))
                        .clickable { onTimer(seconds) }
                        .padding(horizontal = 15.dp, vertical = 10.dp))
            }
        }
    }
}

@Composable
private fun SettingsSwitch(label: String, enabled: Boolean,
                           available: Boolean = true, accent: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled = available, onClick = onClick)
        .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (available) Color.White else Color.Gray, fontSize = 13.sp)
        Text(if (!available) "N/A" else if (enabled) "ON" else "OFF",
            color = if (enabled && available) accent else Color.Gray,
            fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
