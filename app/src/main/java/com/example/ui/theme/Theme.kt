package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * ThemeEngine data class:
 * - One accent color per theme.
 * - Guaranteed >= 4.5:1 contrast ratio across textPrimary, textSecondary, and accent/onAccent in every theme.
 */
@Immutable
data class DyneTheme(
    val name: String,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val bentoColors: List<Color>,
    val isPro: Boolean,
    val elevated: Color = LimeNightElevated,
    val textPrimary: Color = Color.White,
    val textSecondary: Color = Color(0xFFC8CCD6),
    val onAccent: Color = OnLimeAccent,
    val bentoCoral: Color = accent,
    val bentoSky: Color = accent,
    val bentoLime: Color = accent,
    val bentoLavender: Color = accent,
    val isDark: Boolean = true,
    val reduceGlassEffects: Boolean = false,
    val reduceMotion: Boolean = false,
    val subtitle: String = "Signature dark liquid glass"
)

object ThemeEngine {
    // 1. Lime Night (Free) - One accent (#C6F27A), >= 4.5:1 contrast everywhere
    val LimeNight = DyneTheme(
        name = "Lime Night",
        accent = LimeAccent,
        background = LimeNightBackground,
        surface = LimeNightSurface,
        bentoColors = listOf(LimeAccent, LimeAccent, LimeAccent, LimeAccent),
        isPro = false,
        elevated = LimeNightElevated,
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFC8CCD6),
        onAccent = Color(0xFF0A0A0C),
        bentoCoral = LimeAccent,
        bentoSky = LimeAccent,
        bentoLime = LimeAccent,
        bentoLavender = LimeAccent,
        isDark = true,
        subtitle = "Electric lime on obsidian glass"
    )

    // 2. Classic Light (Free) - Deep emerald-lime accent (#256300) for >= 7.0:1 contrast on white & white on accent
    val ClassicLight = DyneTheme(
        name = "Classic Light",
        accent = Color(0xFF256300),
        background = LightBackground,
        surface = LightSurface,
        bentoColors = listOf(Color(0xFF256300), Color(0xFF256300), Color(0xFF256300), Color(0xFF256300)),
        isPro = false,
        elevated = LightElevated,
        textPrimary = Color(0xFF0A0A0C),
        textSecondary = Color(0xFF3A3D47),
        onAccent = Color(0xFFFFFFFF),
        bentoCoral = Color(0xFF256300),
        bentoSky = Color(0xFF256300),
        bentoLime = Color(0xFF256300),
        bentoLavender = Color(0xFF256300),
        isDark = false,
        subtitle = "Crisp daylight studio glass"
    )

    // 3. Ocean (Pro) - Single accent (#38BDF8)
    val OceanPro = DyneTheme(
        name = "Ocean",
        accent = Color(0xFF38BDF8),
        background = Color(0xFF061320),
        surface = Color(0xFF0D2136),
        bentoColors = listOf(Color(0xFF38BDF8), Color(0xFF38BDF8), Color(0xFF38BDF8), Color(0xFF38BDF8)),
        isPro = true,
        elevated = Color(0xFF15304B),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFC4D7EA),
        onAccent = Color(0xFF04101C),
        bentoCoral = Color(0xFF38BDF8),
        bentoSky = Color(0xFF38BDF8),
        bentoLime = Color(0xFF38BDF8),
        bentoLavender = Color(0xFF38BDF8),
        isDark = true,
        subtitle = "Deep marine cyan"
    )

    // 4. Sunset (Pro) - Single accent (#FF8A6B)
    val SunsetPro = DyneTheme(
        name = "Sunset",
        accent = Color(0xFFFF8A6B),
        background = Color(0xFF180B10),
        surface = Color(0xFF28131B),
        bentoColors = listOf(Color(0xFFFF8A6B), Color(0xFFFF8A6B), Color(0xFFFF8A6B), Color(0xFFFF8A6B)),
        isPro = true,
        elevated = Color(0xFF381B26),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFE2C6CF),
        onAccent = Color(0xFF14060A),
        bentoCoral = Color(0xFFFF8A6B),
        bentoSky = Color(0xFFFF8A6B),
        bentoLime = Color(0xFFFF8A6B),
        bentoLavender = Color(0xFFFF8A6B),
        isDark = true,
        subtitle = "Warm horizon coral"
    )

    // 5. Aurora (Pro) - Single accent (#34D399)
    val AuroraPro = DyneTheme(
        name = "Aurora",
        accent = Color(0xFF34D399),
        background = Color(0xFF071714),
        surface = Color(0xFF0E2923),
        bentoColors = listOf(Color(0xFF34D399), Color(0xFF34D399), Color(0xFF34D399), Color(0xFF34D399)),
        isPro = true,
        elevated = Color(0xFF163A32),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFC0DDD5),
        onAccent = Color(0xFF041410),
        bentoCoral = Color(0xFF34D399),
        bentoSky = Color(0xFF34D399),
        bentoLime = Color(0xFF34D399),
        bentoLavender = Color(0xFF34D399),
        isDark = true,
        subtitle = "Polar emerald"
    )

    // 6. Sakura (Pro) - Single accent (#F990C8)
    val SakuraPro = DyneTheme(
        name = "Sakura",
        accent = Color(0xFFF990C8),
        background = Color(0xFF190D16),
        surface = Color(0xFF291624),
        bentoColors = listOf(Color(0xFFF990C8), Color(0xFFF990C8), Color(0xFFF990C8), Color(0xFFF990C8)),
        isPro = true,
        elevated = Color(0xFF391F33),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFE0C6D9),
        onAccent = Color(0xFF1A0814),
        bentoCoral = Color(0xFFF990C8),
        bentoSky = Color(0xFFF990C8),
        bentoLime = Color(0xFFF990C8),
        bentoLavender = Color(0xFFF990C8),
        isDark = true,
        subtitle = "Soft cherry blossom"
    )

    // 7. AMOLED Mono (Pro) - Single accent (#F8FAFC)
    val AmoledMonoPro = DyneTheme(
        name = "AMOLED Mono",
        accent = Color(0xFFF8FAFC),
        background = Color(0xFF000000),
        surface = Color(0xFF111114),
        bentoColors = listOf(Color(0xFFF8FAFC), Color(0xFFF8FAFC), Color(0xFFF8FAFC), Color(0xFFF8FAFC)),
        isPro = true,
        elevated = Color(0xFF1C1C21),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFCBD5E1),
        onAccent = Color(0xFF000000),
        bentoCoral = Color(0xFFF8FAFC),
        bentoSky = Color(0xFFF8FAFC),
        bentoLime = Color(0xFFF8FAFC),
        bentoLavender = Color(0xFFF8FAFC),
        isDark = true,
        subtitle = "True pitch black monochrome"
    )

    // 8. Midnight Violet (Pro) - Single accent (#B79CFF)
    val MidnightVioletPro = DyneTheme(
        name = "Midnight Violet",
        accent = Color(0xFFB79CFF),
        background = Color(0xFF0D0A17),
        surface = Color(0xFF18132B),
        bentoColors = listOf(Color(0xFFB79CFF), Color(0xFFB79CFF), Color(0xFFB79CFF), Color(0xFFB79CFF)),
        isPro = true,
        elevated = Color(0xFF231C3E),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFD1C8EA),
        onAccent = Color(0xFF0E091A),
        bentoCoral = Color(0xFFB79CFF),
        bentoSky = Color(0xFFB79CFF),
        bentoLime = Color(0xFFB79CFF),
        bentoLavender = Color(0xFFB79CFF),
        isDark = true,
        subtitle = "Deep ultraviolet"
    )

    // Legacy alias kept for compatibility if referenced
    val MidnightGoldPro: DyneTheme = MidnightVioletPro

    val allThemes: List<DyneTheme> = listOf(
        LimeNight,
        ClassicLight,
        OceanPro,
        SunsetPro,
        AuroraPro,
        SakuraPro,
        AmoledMonoPro,
        MidnightVioletPro
    )

    fun parseCustomHexColor(hex: String): Color? {
        val cleaned = hex.trim().removePrefix("#")
        if (cleaned.length != 6 && cleaned.length != 8) return null
        return runCatching {
            val argb = if (cleaned.length == 6) {
                (0xFF000000L or cleaned.toLong(16)).toInt()
            } else {
                cleaned.toLong(16).toInt()
            }
            Color(argb)
        }.getOrNull()
    }

    val customAccentPresets: List<Pair<String, String>> = listOf(
        "Electric Lime" to "#CCFF00",
        "Cyber Cyan" to "#00E5FF",
        "Solar Gold" to "#F59E0B",
        "Coral Pulse" to "#FF6B57",
        "Emerald Glow" to "#10B981",
        "Sakura Pink" to "#F472B6",
        "Ultraviolet" to "#A78BFA",
        "Cobalt Sky" to "#38BDF8",
        "Crimson Neon" to "#FB7185",
        "Pure Ice" to "#E2E8F0"
    )

    fun resolveTheme(
        themeName: String,
        darkTheme: Boolean,
        reduceGlassEffects: Boolean = false,
        reduceMotion: Boolean = false,
        customAccentHex: String = ""
    ): DyneTheme {
        val normalizedName = if (themeName.equals("Midnight Gold", ignoreCase = true)) {
            "Midnight Violet"
        } else {
            themeName
        }
        val base = allThemes.firstOrNull { it.name.equals(normalizedName, ignoreCase = true) } ?: LimeNight
        val resolved = if (base.name == "Classic Light" && !darkTheme) {
            base
        } else if (darkTheme) {
            if (base.name == "Classic Light") {
                LimeNight
            } else {
                base
            }
        } else {
            // Light mode adaptation with >= 4.5:1 contrast guaranteed
            val lightAccent = if (DesignTokens.contrastRatio(base.accent, LightSurface) >= 4.5) {
                base.accent
            } else {
                Color(0xFF1D4ED8)
            }
            val lightOnAccent = if (DesignTokens.contrastRatio(Color.White, lightAccent) >= 4.5) {
                Color.White
            } else {
                Color(0xFF0A0A0C)
            }
            base.copy(
                accent = lightAccent,
                onAccent = lightOnAccent,
                bentoColors = listOf(lightAccent, lightAccent, lightAccent, lightAccent),
                bentoCoral = lightAccent,
                bentoSky = lightAccent,
                bentoLime = lightAccent,
                bentoLavender = lightAccent,
                background = LightBackground,
                surface = LightSurface,
                elevated = LightElevated,
                textPrimary = Color(0xFF0A0A0C),
                textSecondary = Color(0xFF3A3D47),
                isDark = false
            )
        }
        val customAccent = parseCustomHexColor(customAccentHex)
        val withAccent = if (customAccent != null) {
            val onCustom = if (DesignTokens.contrastRatio(Color(0xFF0A0A0C), customAccent) >= 4.5) {
                Color(0xFF0A0A0C)
            } else {
                Color.White
            }
            resolved.copy(
                accent = customAccent,
                onAccent = onCustom,
                bentoColors = listOf(customAccent, customAccent, customAccent, customAccent),
                bentoCoral = customAccent,
                bentoSky = customAccent,
                bentoLime = customAccent,
                bentoLavender = customAccent
            )
        } else {
            resolved
        }
        return withAccent.copy(
            reduceGlassEffects = reduceGlassEffects,
            reduceMotion = reduceMotion
        )
    }
}

val LocalDyneTheme = staticCompositionLocalOf { ThemeEngine.LimeNight }

@Composable
fun DyneAppTheme(
    themeName: String = "Lime Night",
    darkTheme: Boolean = isSystemInDarkTheme(),
    textScale: Float = DesignTokens.DefaultTextScaleFactor,
    reduceGlassEffects: Boolean = false,
    reduceMotion: Boolean = false,
    customAccentHex: String = "",
    content: @Composable () -> Unit
) {
    val targetTheme = ThemeEngine.resolveTheme(
        themeName = themeName,
        darkTheme = darkTheme,
        reduceGlassEffects = reduceGlassEffects,
        reduceMotion = reduceMotion,
        customAccentHex = customAccentHex
    )

    val animSpec = spring<Color>(stiffness = if (reduceMotion) Spring.StiffnessHigh else Spring.StiffnessMediumLow)
    val animAccent by animateColorAsState(targetTheme.accent, animSpec, label = "theme_accent")
    val animBg by animateColorAsState(targetTheme.background, animSpec, label = "theme_bg")
    val animSurface by animateColorAsState(targetTheme.surface, animSpec, label = "theme_surface")
    val animElevated by animateColorAsState(targetTheme.elevated, animSpec, label = "theme_elevated")

    val activeTheme = remember(
        targetTheme,
        animAccent,
        animBg,
        animSurface,
        animElevated
    ) {
        targetTheme.copy(
            accent = animAccent,
            background = animBg,
            surface = animSurface,
            elevated = animElevated,
            bentoCoral = animAccent,
            bentoSky = animAccent,
            bentoLime = animAccent,
            bentoLavender = animAccent,
            bentoColors = listOf(animAccent, animAccent, animAccent, animAccent)
        )
    }

    val typeScale = remember(textScale) { createScaledDyneTypeScale(textScale) }
    val sharedSecondTick by rememberSharedSecondTicker()

    val colorScheme = if (activeTheme.isDark) {
        darkColorScheme(
            primary = activeTheme.accent,
            onPrimary = activeTheme.onAccent,
            secondary = activeTheme.accent,
            onSecondary = activeTheme.onAccent,
            tertiary = activeTheme.accent,
            onTertiary = activeTheme.onAccent,
            background = activeTheme.background,
            onBackground = activeTheme.textPrimary,
            surface = activeTheme.surface,
            onSurface = activeTheme.textPrimary,
            surfaceVariant = activeTheme.elevated,
            onSurfaceVariant = activeTheme.textSecondary
        )
    } else {
        lightColorScheme(
            primary = activeTheme.accent,
            onPrimary = activeTheme.onAccent,
            secondary = activeTheme.accent,
            onSecondary = activeTheme.onAccent,
            tertiary = activeTheme.accent,
            onTertiary = activeTheme.onAccent,
            background = activeTheme.background,
            onBackground = activeTheme.textPrimary,
            surface = activeTheme.surface,
            onSurface = activeTheme.textPrimary,
            surfaceVariant = activeTheme.elevated,
            onSurfaceVariant = activeTheme.textSecondary
        )
    }

    CompositionLocalProvider(
        LocalDyneTheme provides activeTheme,
        LocalDyneType provides typeScale,
        LocalSharedSecondTicker provides sharedSecondTick
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
