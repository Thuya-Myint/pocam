package com.pocam.camera.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pocam.camera.ui.components.ShutterButton

@Composable
fun CameraScreen() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Text("pocam.", modifier = Modifier.align(Alignment.TopCenter), color = MaterialTheme.colorScheme.onSurface)
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ShutterButton(onCapture = {})
        }
    }
}
