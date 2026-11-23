package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * UI font: "Plus Jakarta Sans" mapped explicitly to Medium 500, SemiBold 600, Bold 700, ExtraBold 800
 * bundled offline in res/font/plus_jakarta_sans_*.ttf. Never use Light or Normal.
 */
val PlusJakartaSansFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_500, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_600, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_700, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_800, FontWeight.ExtraBold)
)

/**
 * Numbers on gauges, clocks, speed and dB: "Space Grotesk" mapped explicitly to 500/600/700 with tabular figures ("tnum").
 */
val SpaceGroteskTabularFamily = FontFamily(
    Font(R.font.space_grotesk_500, FontWeight.Medium),
    Font(R.font.space_grotesk_600, FontWeight.SemiBold),
    Font(R.font.space_grotesk_700, FontWeight.Bold),
    Font(R.font.space_grotesk_700, FontWeight.ExtraBold)
)

@Immutable
data class DyneTypeScale(
    val hero: TextStyle,
    val headline: TextStyle,
    val largeTitle: TextStyle,
    val title: TextStyle,
    val section: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val button: TextStyle,
    val caption: TextStyle,
    val gaugeHero: TextStyle,
    val gaugeLarge: TextStyle,
    val gaugeMedium: TextStyle,
    val gaugeSmall: TextStyle,
    val cardTitle: TextStyle = title,
    val subtitle: TextStyle = body
)

/**
 * DESIGN TOKENS TYPOGRAPHY RULES:
 * - Hero: 32sp ExtraBold
 * - Headline / largeTitle: 28sp ExtraBold
 * - Title: 20sp ExtraBold
 * - Section header: 16sp Bold
 * - Body: 16sp Medium
 * - Label & Button: 15sp SemiBold
 * - Caption: 13sp Medium
 * - NOTHING below 12sp anywhere. Default text-size setting = 100% (1.0f).
 */
fun createScaledDyneTypeScale(scale: Float = DesignTokens.DefaultTextScaleFactor): DyneTypeScale {
    val s = scale.coerceIn(0.92f, 1.30f)
    val headlineStyle = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = DesignTokens.TitleWeight,
        fontSize = DesignTokens.clampSp(28f, s),
        lineHeight = DesignTokens.clampSp(34f, s),
        letterSpacing = (-0.5).sp
    )
    val labelStyle = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = DesignTokens.LabelWeight,
        fontSize = DesignTokens.clampSp(15f, s),
        lineHeight = DesignTokens.clampSp(20f, s),
        letterSpacing = 0.sp
    )
    return DyneTypeScale(
        hero = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = DesignTokens.TitleWeight,
            fontSize = DesignTokens.clampSp(32f, s),
            lineHeight = DesignTokens.clampSp(38f, s),
            letterSpacing = (-0.8).sp
        ),
        headline = headlineStyle,
        largeTitle = headlineStyle,
        title = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = DesignTokens.TitleWeight,
            fontSize = DesignTokens.clampSp(20f, s),
            lineHeight = DesignTokens.clampSp(26f, s),
            letterSpacing = (-0.2).sp
        ),
        section = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = DesignTokens.SectionHeaderWeight,
            fontSize = DesignTokens.clampSp(16f, s),
            lineHeight = DesignTokens.clampSp(22f, s),
            letterSpacing = 0.2.sp
        ),
        body = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = DesignTokens.BodyWeight,
            fontSize = DesignTokens.clampSp(16f, s),
            lineHeight = DesignTokens.clampSp(22f, s),
            letterSpacing = 0.sp
        ),
        label = labelStyle,
        button = labelStyle.copy(fontWeight = DesignTokens.ButtonWeight),
        caption = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = DesignTokens.CaptionWeight,
            fontSize = DesignTokens.clampSp(13f, s),
            lineHeight = DesignTokens.clampSp(18f, s),
            letterSpacing = 0.sp
        ),
        gaugeHero = TextStyle(
            fontFamily = SpaceGroteskTabularFamily,
            fontWeight = FontWeight.Bold,
            fontSize = DesignTokens.clampSp(32f, s),
            lineHeight = DesignTokens.clampSp(38f, s),
            letterSpacing = (-0.8).sp,
            fontFeatureSettings = "tnum"
        ),
        gaugeLarge = TextStyle(
            fontFamily = SpaceGroteskTabularFamily,
            fontWeight = FontWeight.Bold,
            fontSize = DesignTokens.clampSp(28f, s),
            lineHeight = DesignTokens.clampSp(34f, s),
            letterSpacing = (-0.5).sp,
            fontFeatureSettings = "tnum"
        ),
        gaugeMedium = TextStyle(
            fontFamily = SpaceGroteskTabularFamily,
            fontWeight = FontWeight.Bold,
            fontSize = DesignTokens.clampSp(20f, s),
            lineHeight = DesignTokens.clampSp(26f, s),
            letterSpacing = (-0.2).sp,
            fontFeatureSettings = "tnum"
        ),
        gaugeSmall = TextStyle(
            fontFamily = SpaceGroteskTabularFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = DesignTokens.clampSp(15f, s),
            lineHeight = DesignTokens.clampSp(20f, s),
            fontFeatureSettings = "tnum"
        )
    )
}

val DefaultDyneTypeScale = createScaledDyneTypeScale(DesignTokens.DefaultTextScaleFactor)

val LocalDyneType = staticCompositionLocalOf { DefaultDyneTypeScale }

val Typography = Typography(
    displayLarge = DefaultDyneTypeScale.hero,
    displayMedium = DefaultDyneTypeScale.headline,
    headlineLarge = DefaultDyneTypeScale.headline,
    headlineMedium = DefaultDyneTypeScale.title,
    titleLarge = DefaultDyneTypeScale.title,
    titleMedium = DefaultDyneTypeScale.section,
    bodyLarge = DefaultDyneTypeScale.body,
    bodyMedium = DefaultDyneTypeScale.caption,
    labelLarge = DefaultDyneTypeScale.button,
    labelSmall = DefaultDyneTypeScale.caption
)
