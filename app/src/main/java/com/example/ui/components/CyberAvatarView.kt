package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.VoiceState
import com.example.data.repository.GyroscopeOffset
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.SurfaceDark
import kotlin.math.roundToInt

@Composable
fun CyberAvatarView(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 160.dp,
    voiceState: VoiceState = VoiceState.IDLE,
    isOnline: Boolean = true,
    gyroOffset: GyroscopeOffset = GyroscopeOffset(),
    gyroEnabled: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_anim")

    // Slow rotation of cybernetic orbit ring
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )

    // Breathing pulse
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = if (voiceState == VoiceState.SPEAKING) 1.05f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // 3D Parallax Tilt Offsets from Gyroscope
    val tiltX = if (gyroEnabled) (gyroOffset.roll * 16f).roundToInt() else 0
    val tiltY = if (gyroEnabled) (gyroOffset.pitch * 16f).roundToInt() else 0

    val ringBorderColors = when (voiceState) {
        VoiceState.LISTENING -> listOf(NeonCyan, NeonPurple, NeonCyan)
        VoiceState.SPEAKING -> listOf(NeonPink, NeonPurple, NeonCyan)
        VoiceState.THINKING -> listOf(NeonPurple, NeonPink, NeonPurple)
        VoiceState.ERROR -> listOf(Color(0xFFEF4444), NeonPink)
        VoiceState.IDLE -> listOf(NeonPurple, NeonCyan, NeonPink, NeonPurple)
    }

    Box(
        modifier = modifier
            .size(sizeDp + 40.dp)
            .testTag("cyber_avatar_view"),
        contentAlignment = Alignment.Center
    ) {
        // Outer holographic ambient glow
        Box(
            modifier = Modifier
                .size(sizeDp + 24.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonPurple.copy(alpha = 0.35f),
                            NeonCyan.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Rotating cybernetic ring with gradient border
        Box(
            modifier = Modifier
                .size(sizeDp + 12.dp)
                .rotate(orbitAngle)
                .clip(CircleShape)
                .border(
                    width = 2.5.dp,
                    brush = Brush.sweepGradient(ringBorderColors),
                    shape = CircleShape
                )
        )

        // Main Parallax Container
        Box(
            modifier = Modifier
                .offset { IntOffset(tiltX, tiltY) }
                .size(sizeDp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(SurfaceDark)
                .border(
                    width = 3.dp,
                    brush = Brush.linearGradient(
                        listOf(NeonCyan, NeonPurple, NeonPink)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_arifa_avatar),
                contentDescription = "ARIFA AI Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(sizeDp)
                    .clip(CircleShape)
            )
        }

        // Online / AI Status Indicator Pip
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(if (isOnline) OnlineGreen else Color(0xFF64748B))
                .border(2.5.dp, MaterialTheme.colorScheme.background, CircleShape)
        )
    }
}
