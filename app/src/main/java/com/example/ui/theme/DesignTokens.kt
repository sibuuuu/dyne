package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Single source of truth for Dyne's unified Design System tokens.
 * Replaces ad-hoc sizes across all screens:
 * - Text: hero 32sp, headline 28sp, title 20sp, section header 16sp bold,
 *   body 16sp, label and button 15sp, caption 13sp. Nothing below 12sp anywhere.
 * - Weights: titles ExtraBold, labels SemiBold, body Medium. Default text-size = 100% (1.0f).
 * - Sizes: buttons 52dp high (min touch target 48dp), icons 24dp, tiles >= 104dp wide (up to 2 lines),
 *   spacing scale 8/12/16/24dp, screen margins 20dp, card radius 24dp, button radius 26dp.
 */
@Immutable
object DesignTokens {
    // Typography sizes (never below 12sp)
    val HeroTextSize: TextUnit = 32.sp
    val HeadlineTextSize: TextUnit = 28.sp
    val TitleTextSize: TextUnit = 20.sp
    val SectionHeaderTextSize: TextUnit = 16.sp
    val BodyTextSize: TextUnit = 16.sp
    val LabelTextSize: TextUnit = 15.sp
    val ButtonTextSize: TextUnit = 15.sp
    val CaptionTextSize: TextUnit = 13.sp
    val MinAllowedTextSize: TextUnit = 12.sp

    // Typography weights
    val TitleWeight: FontWeight = FontWeight.ExtraBold
    val SectionHeaderWeight: FontWeight = FontWeight.Bold
    val LabelWeight: FontWeight = FontWeight.SemiBold
    val ButtonWeight: FontWeight = FontWeight.SemiBold
    val BodyWeight: FontWeight = FontWeight.Medium
    val CaptionWeight: FontWeight = FontWeight.Medium

    // Default text scale (100%)
    const val DefaultTextScalePercent: Int = 100
    const val DefaultTextScaleFactor: Float = 1.0f

    // Component dimensions
    val ButtonHeight: Dp = 52.dp
    val MinTouchTarget: Dp = 48.dp
    val IconSize: Dp = 24.dp
    val HeaderLogoSize: Dp = 40.dp
    val TileMinWidth: Dp = 104.dp
    const val TileTitleMaxLines: Int = 2
    const val LogoCornerPercent: Int = 22

    // Spacing scale: 8 / 12 / 16 / 24
    val Space8: Dp = 8.dp
    val Space12: Dp = 12.dp
    val Space16: Dp = 16.dp
    val Space24: Dp = 24.dp

    // Margins & Radii
    val ScreenMargin: Dp = 20.dp
    val CardRadius: Dp = 24.dp
    val ButtonRadius: Dp = 26.dp

    val CardShape = RoundedCornerShape(CardRadius)
    val ButtonShape = RoundedCornerShape(ButtonRadius)
    val LogoTileShape = RoundedCornerShape(percent = LogoCornerPercent)

    /**
     * Clamps any scaled font size so nothing is ever below 12sp.
     */
    fun clampSp(baseSp: Float, scale: Float = DefaultTextScaleFactor): TextUnit {
        return max(12f, baseSp * scale.coerceIn(0.85f, 1.30f)).sp
    }

    /**
     * Computes WCAG 2.1 contrast ratio between two colors (always >= 1.0, up to 21.0).
     * Used to verify >= 4.5:1 contrast in every theme.
     */
    fun contrastRatio(foreground: Color, background: Color): Double {
        val l1 = relativeLuminance(foreground)
        val l2 = relativeLuminance(background)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        fun channel(c: Float): Double {
            val v = c.toDouble().coerceIn(0.0, 1.0)
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }
}

/**
 * Shared 1-second wall-clock ticker for visible countdown rings and banners.
 * Avoids per-item coroutines or infinite transitions while scrolling.
 */
val LocalSharedSecondTicker = staticCompositionLocalOf { 0L }

@Composable
fun rememberSharedSecondTicker(enabled: Boolean = true): State<Long> {
    val state = remember { mutableLongStateOf(System.currentTimeMillis() / 1000L) }
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        while (true) {
            state.longValue = System.currentTimeMillis() / 1000L
            delay(1_000L)
        }
    }
    return state
}
