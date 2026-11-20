package com.pocam.camera.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocam.camera.domain.model.CameraMode

/**
 * Flat mode strip. Retains the previous function signature for CameraScreen,
 * but intentionally removes the curved Canvas ribbon: native Compose text and
 * capsules are predictable at any density and font scale.
 */
@Composable
fun CurvedModeSelector(
    selected: CameraMode,
    hovered: CameraMode? = null,
    onSelect: (CameraMode) -> Unit,
    modifier: Modifier = Modifier,
    motionEnabled: Boolean = true
) {
    val modes = listOf(
        CameraMode.VIDEO, CameraMode.NIGHT, CameraMode.PHOTO,
        CameraMode.PRO, CameraMode.PORTRAIT
    )
    val current = hovered ?: selected
    val haptics = LocalHapticFeedback.current

    Row(
        modifier.fillMaxWidth()
            .height(52.dp)
            .background(Color(0xF7F8F9FA), RoundedCornerShape(24.dp))
            .padding(horizontal = 4.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        modes.forEach { mode ->
            val active = mode == current
            val fill by animateColorAsState(
                targetValue = if (active) Color(0xFF24272D) else Color.Transparent,
                animationSpec = if (motionEnabled) tween(140) else snap(),
                label = "flat-mode-selection"
            )
            val foreground by animateColorAsState(
                targetValue = if (active) Color.White else Color(0xFF252932),
                animationSpec = if (motionEnabled) tween(140) else snap(),
                label = "flat-mode-text"
            )
            Box(
                Modifier.weight(1f).height(42.dp)
                    .background(fill, RoundedCornerShape(20.dp))
                    .clickable {
                        if (mode != selected) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(mode)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    mode.name,
                    color = foreground,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}
