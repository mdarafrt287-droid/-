package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Futuristic Void & Dark Backgrounds
val VoidBlack = Color(0xFF06040A)
val SurfaceDark = Color(0xFF0F0A1C)
val SurfaceVariantDark = Color(0xFF19112E)
val SurfaceCard = Color(0xFF130E24)
val SurfaceCardBorder = Color(0xFF2D1B4E)

// Neon Cyber Accents
val NeonPurple = Color(0xFFA855F7)
val NeonPurpleGlow = Color(0xFFC084FC)
val DeepViolet = Color(0xFF7C3AED)
val NeonCyan = Color(0xFF06B6D4)
val NeonCyanGlow = Color(0xFF22D3EE)
val NeonCyanLight = Color(0xFF67E8F9)
val NeonPink = Color(0xFFEC4899)
val NeonPinkGlow = Color(0xFFF43F5E)
val ElectricBlue = Color(0xFF3B82F6)

// Functional & Text Colors
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextTertiary = Color(0xFF64748B)
val OnlineGreen = Color(0xFF10B981)
val OfflineGray = Color(0xFF6B7280)
val ErrorRed = Color(0xFFEF4444)

// Gradient Brushes
val NeonGradientPrimary = Brush.linearGradient(
    colors = listOf(NeonPurple, NeonPink)
)

val NeonGradientCyber = Brush.linearGradient(
    colors = listOf(NeonCyan, NeonPurple, NeonPink)
)

val NeonGradientCyanBlue = Brush.linearGradient(
    colors = listOf(NeonCyan, DeepViolet)
)

val NeonGradientCardBorder = Brush.linearGradient(
    colors = listOf(
        NeonPurple.copy(alpha = 0.6f),
        NeonCyan.copy(alpha = 0.6f),
        NeonPink.copy(alpha = 0.6f)
    )
)

val VoidGradientBackground = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0B0618),
        Color(0xFF06040A),
        Color(0xFF020105)
    )
)
