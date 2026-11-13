package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.VoiceState
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.VoidBlack

@Composable
fun GlowingMicButton(
    modifier: Modifier = Modifier,
    voiceState: VoiceState = VoiceState.IDLE,
    amplitude: Float = 0f,
    buttonSize: Dp = 80.dp,
    onClick: () -> Unit
) {
    val isListening = voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING

    val infiniteTransition = rememberInfiniteTransition(label = "mic_glow")

    // Pulsing outer wave 1
    val wave1Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.6f else 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 1000 else 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave1"
    )

    val wave1Alpha by infiniteTransition.animateFloat(
        initialValue = if (isListening) 0.5f else 0.25f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 1000 else 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave1_alpha"
    )

    // Pulsing outer wave 2
    val wave2Scale by infiniteTransition.animateFloat(
        initialValue = 1.1f,
        targetValue = if (isListening) 1.9f else 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 1200 else 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave2"
    )

    val wave2Alpha by infiniteTransition.animateFloat(
        initialValue = if (isListening) 0.35f else 0.15f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 1200 else 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave2_alpha"
    )

    val dynamicAudioScale = if (isListening) (1f + (amplitude * 0.35f)).coerceIn(1f, 1.4f) else 1f

    val coreGradient = when (voiceState) {
        VoiceState.LISTENING -> Brush.linearGradient(listOf(NeonCyan, NeonPurple))
        VoiceState.SPEAKING -> Brush.linearGradient(listOf(NeonPink, NeonPurple))
        VoiceState.THINKING -> Brush.linearGradient(listOf(NeonPurple, Color(0xFF6366F1)))
        VoiceState.ERROR -> Brush.linearGradient(listOf(Color(0xFFEF4444), NeonPink))
        VoiceState.IDLE -> Brush.linearGradient(listOf(NeonPurple, NeonPink))
    }

    Box(
        modifier = modifier
            .size(buttonSize * 2f)
            .testTag("glowing_mic_container"),
        contentAlignment = Alignment.Center
    ) {
        // Outer wave 2
        Box(
            modifier = Modifier
                .size(buttonSize)
                .scale(wave2Scale * dynamicAudioScale)
                .clip(CircleShape)
                .background(NeonCyan.copy(alpha = wave2Alpha))
        )

        // Outer wave 1
        Box(
            modifier = Modifier
                .size(buttonSize)
                .scale(wave1Scale * dynamicAudioScale)
                .clip(CircleShape)
                .background(NeonPink.copy(alpha = wave1Alpha))
        )

        // Main Button Core
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .background(coreGradient)
                .clickable(onClick = onClick)
                .testTag("glowing_mic_button"),
            contentAlignment = Alignment.Center
        ) {
            // Inner dark inset
            Box(
                modifier = Modifier
                    .size(buttonSize - 10.dp)
                    .clip(CircleShape)
                    .background(VoidBlack.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (voiceState == VoiceState.ERROR) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Microphone action",
                    tint = Color.White,
                    modifier = Modifier.size(buttonSize * 0.48f)
                )
            }
        }
    }
}
