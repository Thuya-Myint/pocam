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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val gold = Color(0xFFFFD88C)

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
    performance: Boolean,
    onPerformance: (Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.background(Color(0xF722252D), RoundedCornerShape(22.dp))
        .animateContentSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("CAMERA SETTINGS", color = Color.White, fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp, fontSize = 12.sp)
            Text("DONE", color = gold, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                modifier = Modifier.clickable(onClick = onClose))
        }
        HorizontalDivider(color = Color.White.copy(alpha = .12f))
        SettingsSwitch("Composition grid", grid) { onGrid(!grid) }
        SettingsSwitch("Center crosshair", level) { onLevel(!level) }
        SettingsSwitch("Live torch", torch, torchAvailable) { onTorch(!torch) }
        SettingsSwitch("Performance HUD", performance) { onPerformance(!performance) }
        Text("CAPTURE TIMER", color = Color.LightGray, fontSize = 11.sp,
            letterSpacing = 1.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 3, 5, 10).forEach { seconds ->
                Text(if (seconds == 0) "OFF" else "${seconds}s",
                    color = if (timer == seconds) Color.Black else Color.White,
                    fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                    modifier = Modifier.background(
                        if (timer == seconds) gold else Color(0xFF383C46),
                        RoundedCornerShape(12.dp))
                        .clickable { onTimer(seconds) }
                        .padding(horizontal = 15.dp, vertical = 10.dp))
            }
        }
    }
}

@Composable
private fun SettingsSwitch(label: String, enabled: Boolean,
                           available: Boolean = true, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled = available, onClick = onClick)
        .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (available) Color.White else Color.Gray, fontSize = 13.sp)
        Text(if (!available) "N/A" else if (enabled) "ON" else "OFF",
            color = if (enabled && available) gold else Color.Gray,
            fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
