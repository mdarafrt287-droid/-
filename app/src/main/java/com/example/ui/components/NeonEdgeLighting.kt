package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple

@Composable
fun NeonEdgeLighting(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    strokeWidthDp: Float = 3.5f
) {
    if (!enabled) return

    val infiniteTransition = rememberInfiniteTransition(label = "edge_neon")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "edge_phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val colors = listOf(
            NeonPurple,
            NeonPink,
            NeonCyan,
            NeonPurple
        )

        // Gradient shifting smoothly around the perimeter
        val brush = Brush.sweepGradient(
            colors = colors,
            center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)
        )

        val strokePx = strokeWidthDp.dp.toPx()

        drawRect(
            brush = brush,
            topLeft = androidx.compose.ui.geometry.Offset(strokePx / 2f, strokePx / 2f),
            size = androidx.compose.ui.geometry.Size(width - strokePx, height - strokePx),
            style = Stroke(width = strokePx)
        )
    }
}
