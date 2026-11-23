package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Default "Lime Night" Dark Palette
val LimeNightBackground = Color(0xFF0A0A0C)
val LimeNightSurface = Color(0xFF151518)
val LimeNightElevated = Color(0xFF1E1E22)
val LimeAccent = Color(0xFFC6F27A)
val OnLimeAccent = Color(0xFF0A0A0C)

// Light Mode Palette
val LightBackground = Color(0xFFF5F5F7)
val LightSurface = Color(0xFFFFFFFF)
val LightElevated = Color(0xFFEAEAEF)

// Four Pastel Bento Colors (with bold black text #0A0A0C)
val BentoCoral = Color(0xFFF58A6E)
val BentoSky = Color(0xFF7FCBFF)
val BentoLime = Color(0xFFC6F27A)
val BentoLavender = Color(0xFFB7A9F7)
val BentoTextBlack = Color(0xFF0A0A0C)

// GOLD IS RESERVED EXCLUSIVELY FOR PRO
val ProGold = Color(0xFFFFD76A)
val ProGoldDeep = Color(0xFFE8A93B)
val ProGoldDark = Color(0xFF7A5A12)
val ProGoldGradient = Brush.linearGradient(
    colors = listOf(ProGold, ProGoldDeep)
)

// Midnight Violet Palette (replaces Midnight Gold so gold is reserved strictly for Pro)
val MidnightVioletAccent = Color(0xFFA78BFA)

// Additional Semantic Colors (no amber/gold in standard UI)
val StatusWarning = Color(0xFFF58A6E)
val StatusDanger = Color(0xFFFB7185)
val StatusSuccess = Color(0xFFC6F27A)
