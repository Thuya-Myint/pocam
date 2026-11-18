package com.pocam.camera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pocam.camera.performance.AppPerformance
import com.pocam.camera.performance.AppPerformanceMonitor
import kotlinx.coroutines.delay

@Composable
fun PerformanceHud(modifier: Modifier = Modifier) {
    val monitor = remember { AppPerformanceMonitor() }
    var metrics by remember { mutableStateOf(AppPerformance(null, 0)) }
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            metrics = monitor.sample()
            delay(1500)
        }
    }
    Column(modifier.background(Color(0xD9222327), RoundedCornerShape(22.dp))
        .clickable { expanded = !expanded }.padding(horizontal = 14.dp, vertical = 9.dp)) {
        Text("CPU ${metrics.cpuPercent?.let { "${it}%" } ?: "—"}  ·  RAM ${metrics.memoryMb} MB",
            color = Color.White)
        if (expanded) Text("Pocam process only", color = Color.LightGray)
    }
}
