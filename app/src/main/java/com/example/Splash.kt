package com.example

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LaunchAnimationMode
import com.example.ui.components.LocalTapSound
import com.example.ui.theme.PlusJakartaSansFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

enum class SplashVariant {
    LEVEL_BUBBLE,
    COMPASS_SNAP,
    GLASS_DROP,
    SONAR_POP
}

enum class SplashPalette {
    COLOR,
    MONO_DARK,
    MONO_LIGHT
}

data class SplashCombo(
    val variant: SplashVariant,
    val palette: SplashPalette
) {
    val key: String get() = "${variant.name}_${palette.name}"
}

/**
 * Picks a random (Variant x Palette) combination according to [mode],
 * guaranteeing that the exact same combination is never picked twice in a row.
 */
fun pickNextSplashCombo(
    mode: LaunchAnimationMode,
    lastComboKey: String,
    random: Random = Random.Default
): SplashCombo {
    val allowedPalettes = when (mode) {
        LaunchAnimationMode.COLOR_ONLY -> listOf(SplashPalette.COLOR)
        LaunchAnimationMode.BLACK_AND_WHITE_ONLY -> listOf(SplashPalette.MONO_DARK, SplashPalette.MONO_LIGHT)
        LaunchAnimationMode.RANDOM, LaunchAnimationMode.MINIMAL -> listOf(
            SplashPalette.COLOR,
            SplashPalette.COLOR, // 50% weight for COLOR, 50% split between MONO_DARK and MONO_LIGHT
            SplashPalette.MONO_DARK,
            SplashPalette.MONO_LIGHT
        )
    }

    val allVariants = SplashVariant.entries
    val candidates = mutableListOf<SplashCombo>()
    for (v in allVariants) {
        for (p in allowedPalettes.distinct()) {
            val combo = SplashCombo(v, p)
            if (combo.key != lastComboKey) {
                candidates.add(combo)
            }
        }
    }

    if (candidates.isEmpty()) {
        return SplashCombo(SplashVariant.LEVEL_BUBBLE, SplashPalette.COLOR)
    }

    // Respect 50/50 COLOR vs MONO in RANDOM mode
    val targetPalette = allowedPalettes[random.nextInt(allowedPalettes.size)]
    val matchingPaletteCandidates = candidates.filter { it.palette == targetPalette }
    val pool = matchingPaletteCandidates.ifEmpty { candidates }
    return pool[random.nextInt(pool.size)]
}

/**
 * Composable launch animation for Dyne.
 * - Uses springs with DampingRatioMediumBouncy, 0.5 -> 1.0 scale, ~50ms staggered delays,
 *   150ms fade-in tween, rotation with bounce, and slide + fade on exit.
 * - Tapping anywhere skips immediately.
 * - Respects Reduce Motion (app setting or system animator scale 0) and LaunchAnimationMode.MINIMAL
 *   by replacing the animation with a clean 300ms fade.
 * - Final reveal renders R.drawable.logo (logo.png) directly with ContentScale.Fit (never code-drawn).
 */
@Composable
fun DyneSplash(
    onFinished: () -> Unit,
    launchMode: LaunchAnimationMode = LaunchAnimationMode.RANDOM,
    lastComboKey: String = "",
    reduceMotionSetting: Boolean = false,
    onSaveComboKey: (String) -> Unit = {},
    onFirstFrameDrawn: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val tapSound = LocalTapSound.current

    val systemAnimatorDisabled = remember(context) {
        runCatching {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f
        }.getOrDefault(false)
    }

    val isMinimalOrReduced = reduceMotionSetting ||
        systemAnimatorDisabled ||
        launchMode == LaunchAnimationMode.MINIMAL

    val chosenCombo = remember {
        pickNextSplashCombo(launchMode, lastComboKey)
    }

    var finishedTriggered by remember { mutableStateOf(false) }
    val triggerFinish = {
        if (!finishedTriggered) {
            finishedTriggered = true
            onFinished()
        }
    }

    // Overall splash exit state
    val exitAlpha = remember { Animatable(1f) }
    val exitSlideY = remember { Animatable(0f) }
    val monoFlashAlpha = remember { Animatable(0f) }

    // Shared animation channels
    val introAlpha = remember { Animatable(0f) }
    val primaryScale = remember { Animatable(0.5f) }
    val primaryRotation = remember { Animatable(0f) }

    // Variant-specific channels
    val bubbleOffset = remember { Animatable(-0.68f) } // -1..1 inside level vial
    val morphToHex = remember { Animatable(0f) } // 0 = instrument, 1 = final logo reveal
    val logoDScale = remember { Animatable(0.5f) }
    val logoDAlpha = remember { Animatable(0f) }

    val compassSweep = remember { Animatable(0f) }
    val needleAngle = remember { Animatable(-145f) }

    val dropFallProgress = remember { Animatable(0f) } // 0 = top, 1 = center impact
    val rippleProgress = remember { Animatable(0f) }

    val sonarProgress = remember { Animatable(0f) }

    // Wordmark "D-y-n-e" staggered channels (4 letters)
    val letterAlphas = remember { List(4) { Animatable(0f) } }
    val letterScales = remember { List(4) { Animatable(0.5f) } }
    val letterSlides = remember { List(4) { Animatable(24f) } }

    LaunchedEffect(Unit) {
        withFrameNanos {
            onFirstFrameDrawn()
        }
        onSaveComboKey(chosenCombo.key)

        if (isMinimalOrReduced) {
            // Simple 300ms fade for Reduce Motion or Minimal mode
            morphToHex.snapTo(1f)
            logoDAlpha.snapTo(1f)
            logoDScale.snapTo(1f)
            letterAlphas.forEach { it.snapTo(1f) }
            letterScales.forEach { it.snapTo(1f) }
            letterSlides.forEach { it.snapTo(0f) }
            introAlpha.animateTo(1f, tween(150))
            delay(180)
            exitAlpha.animateTo(0f, tween(150))
            triggerFinish()
            return@LaunchedEffect
        }

        // Entrance fade 150ms + bouncy scale from 0.5 to 1.0
        launch {
            introAlpha.animateTo(1f, tween(150, easing = FastOutSlowInEasing))
        }
        launch {
            primaryScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        when (chosenCombo.variant) {
            SplashVariant.LEVEL_BUBBLE -> {
                // Drift left -> right -> center settle with bounce
                bubbleOffset.animateTo(
                    targetValue = 0.46f,
                    animationSpec = tween(420, easing = FastOutSlowInEasing)
                )
                bubbleOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                // Key snap/land beat: play one tap sound & haptic tick
                tapSound?.playTap(view)

                // Morph vial into final logo reveal
                launch {
                    morphToHex.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
                launch {
                    logoDAlpha.animateTo(1f, tween(150))
                }
                logoDScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }

            SplashVariant.COMPASS_SNAP -> {
                // Tick marks sweep around ring while needle spins and snaps to North
                launch {
                    compassSweep.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
                }
                needleAngle.animateTo(
                    targetValue = 38f,
                    animationSpec = tween(380, easing = FastOutSlowInEasing)
                )
                needleAngle.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                // Key snap beat at North
                tapSound?.playTap(view)

                // Ring closes and logo scales in
                launch {
                    morphToHex.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
                launch {
                    logoDAlpha.animateTo(1f, tween(150))
                }
                logoDScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }

            SplashVariant.GLASS_DROP -> {
                // Glass drop falls to center
                dropFallProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(420, easing = FastOutSlowInEasing)
                )
                // Key impact/land beat
                tapSound?.playTap(view)

                // Impact ripple + splash into final logo reveal
                launch {
                    rippleProgress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
                }
                launch {
                    morphToHex.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
                launch {
                    primaryRotation.snapTo(-14f)
                    primaryRotation.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
                launch {
                    logoDAlpha.animateTo(1f, tween(150))
                }
                logoDScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }

            SplashVariant.SONAR_POP -> {
                // Concentric sonar rings expand outward
                launch {
                    sonarProgress.animateTo(1f, tween(780, easing = FastOutSlowInEasing))
                }
                delay(260)
                // Key beat as central Dyne badge locks in
                tapSound?.playTap(view)
                launch {
                    morphToHex.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                launch {
                    logoDAlpha.animateTo(1f, tween(150))
                }
                logoDScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }

        // Staggered wordmark "Dyne" letters (50ms delay per letter, 150ms fade, 0.5 -> 1.0 bouncy scale + slide)
        val letters = listOf("D", "y", "n", "e")
        for (i in letters.indices) {
            launch {
                delay(i * 50L)
                launch {
                    letterAlphas[i].animateTo(1f, tween(150))
                }
                launch {
                    letterSlides[i].animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                letterScales[i].animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }

        // Hold wordmark "Dyne" for ~320ms
        delay(440)

        // Optional quick white flash at end of MONO_DARK palette
        if (chosenCombo.palette == SplashPalette.MONO_DARK) {
            monoFlashAlpha.animateTo(0.28f, tween(70))
            monoFlashAlpha.animateTo(0f, tween(110))
        }

        // Slide + fade exit
        launch {
            exitSlideY.animateTo(-28f, tween(200, easing = FastOutSlowInEasing))
        }
        exitAlpha.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
        triggerFinish()
    }

    // Palette colors
    val bgColor = when (chosenCombo.palette) {
        SplashPalette.COLOR -> Color(0xFF0A0A0C)
        SplashPalette.MONO_DARK -> Color(0xFF000000)
        SplashPalette.MONO_LIGHT -> Color(0xFFFFFFFF)
    }
    val limeColor = when (chosenCombo.palette) {
        SplashPalette.COLOR -> Color(0xFFC6F27A)
        SplashPalette.MONO_DARK -> Color(0xFFFFFFFF)
        SplashPalette.MONO_LIGHT -> Color(0xFF0A0A0C)
    }
    val skyColor = when (chosenCombo.palette) {
        SplashPalette.COLOR -> Color(0xFF7FCBFF)
        SplashPalette.MONO_DARK -> Color(0xFFD4D4D8)
        SplashPalette.MONO_LIGHT -> Color(0xFF3F3F46)
    }
    val lavenderColor = when (chosenCombo.palette) {
        SplashPalette.COLOR -> Color(0xFFB7A9F7)
        SplashPalette.MONO_DARK -> Color(0xFFA1A1AA)
        SplashPalette.MONO_LIGHT -> Color(0xFF52525B)
    }
    val coralColor = when (chosenCombo.palette) {
        SplashPalette.COLOR -> Color(0xFFFF8B7B)
        SplashPalette.MONO_DARK -> Color(0xFFE4E4E7)
        SplashPalette.MONO_LIGHT -> Color(0xFF27272A)
    }
    val textColor = when (chosenCombo.palette) {
        SplashPalette.COLOR, SplashPalette.MONO_DARK -> Color(0xFFFFFFFF)
        SplashPalette.MONO_LIGHT -> Color(0xFF0A0A0C)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = exitAlpha.value
                translationY = exitSlideY.value
            }
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = triggerFinish
            )
            .testTag("dyne_splash_screen")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(220.dp)
                    .graphicsLayer {
                        alpha = introAlpha.value
                        scaleX = primaryScale.value
                        scaleY = primaryScale.value
                        rotationZ = primaryRotation.value
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val minDim = min(size.width, size.height)

                    // Soft radial ambient glow
                    if (chosenCombo.palette == SplashPalette.COLOR) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    limeColor.copy(alpha = 0.20f),
                                    skyColor.copy(alpha = 0.10f),
                                    Color.Transparent
                                ),
                                center = Offset(cx, cy),
                                radius = minDim * 0.56f
                            ),
                            radius = minDim * 0.56f,
                            center = Offset(cx, cy)
                        )
                    }

                    val morph = morphToHex.value.coerceIn(0f, 1f)

                    when (chosenCombo.variant) {
                        SplashVariant.LEVEL_BUBBLE -> {
                            val vialAlpha = (1f - morph * 0.85f).coerceIn(0f, 1f)
                            if (vialAlpha > 0.01f) {
                                val vialWidth = minDim * (0.82f - morph * 0.34f)
                                val vialHeight = minDim * (0.28f + morph * 0.20f)
                                val corner = vialHeight / 2f
                                val topLeft = Offset(cx - vialWidth / 2f, cy - vialHeight / 2f)

                                drawRoundRect(
                                    color = skyColor.copy(alpha = 0.14f * vialAlpha),
                                    topLeft = topLeft,
                                    size = Size(vialWidth, vialHeight),
                                    cornerRadius = CornerRadius(corner, corner)
                                )
                                drawRoundRect(
                                    color = textColor.copy(alpha = 0.38f * vialAlpha),
                                    topLeft = topLeft,
                                    size = Size(vialWidth, vialHeight),
                                    cornerRadius = CornerRadius(corner, corner),
                                    style = Stroke(width = 2.dp.toPx())
                                )

                                // Center calibration hairlines
                                val lineOffset = vialHeight * 0.36f
                                drawLine(
                                    color = textColor.copy(alpha = 0.45f * vialAlpha),
                                    start = Offset(cx - lineOffset, topLeft.y + 6.dp.toPx()),
                                    end = Offset(cx - lineOffset, topLeft.y + vialHeight - 6.dp.toPx()),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                                drawLine(
                                    color = textColor.copy(alpha = 0.45f * vialAlpha),
                                    start = Offset(cx + lineOffset, topLeft.y + 6.dp.toPx()),
                                    end = Offset(cx + lineOffset, topLeft.y + vialHeight - 6.dp.toPx()),
                                    strokeWidth = 1.5.dp.toPx()
                                )

                                // Glowing level bubble
                                val maxTravel = (vialWidth - vialHeight) * 0.44f
                                val bubbleCenterX = cx + bubbleOffset.value * maxTravel * (1f - morph)
                                val bubbleRadius = vialHeight * 0.33f
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            limeColor.copy(alpha = 0.95f * vialAlpha),
                                            limeColor.copy(alpha = 0.25f * vialAlpha),
                                            Color.Transparent
                                        ),
                                        center = Offset(bubbleCenterX, cy),
                                        radius = bubbleRadius * 1.45f
                                    ),
                                    radius = bubbleRadius * 1.45f,
                                    center = Offset(bubbleCenterX, cy)
                                )
                                drawCircle(
                                    color = limeColor.copy(alpha = vialAlpha),
                                    radius = bubbleRadius * 0.82f,
                                    center = Offset(bubbleCenterX, cy)
                                )
                            }
                        }

                        SplashVariant.COMPASS_SNAP -> {
                            val ringAlpha = (1f - morph * 0.85f).coerceIn(0f, 1f)
                            if (ringAlpha > 0.01f) {
                                val ringRadius = minDim * (0.40f - morph * 0.12f)
                                val totalTicks = 36
                                val activeTicks = (totalTicks * compassSweep.value).roundToInt()
                                for (i in 0 until activeTicks) {
                                    val angleDeg = i * 10f - 90f
                                    val rad = angleDeg * (PI.toFloat() / 180f)
                                    val isMajor = i % 9 == 0
                                    val innerR = ringRadius - (if (isMajor) 12.dp.toPx() else 6.dp.toPx())
                                    val start = Offset(
                                        cx + cos(rad) * innerR,
                                        cy + sin(rad) * innerR
                                    )
                                    val end = Offset(
                                        cx + cos(rad) * ringRadius,
                                        cy + sin(rad) * ringRadius
                                    )
                                    drawLine(
                                        color = if (i == 0) coralColor.copy(alpha = ringAlpha)
                                        else textColor.copy(alpha = (if (isMajor) 0.75f else 0.35f) * ringAlpha),
                                        start = start,
                                        end = end,
                                        strokeWidth = (if (isMajor) 2.5.dp else 1.2.dp).toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }

                                // Spinning & snapping needle
                                rotate(degrees = needleAngle.value, pivot = Offset(cx, cy)) {
                                    val needleLen = ringRadius * 0.68f
                                    drawLine(
                                        color = coralColor.copy(alpha = ringAlpha),
                                        start = Offset(cx, cy),
                                        end = Offset(cx, cy - needleLen),
                                        strokeWidth = 4.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                    drawLine(
                                        color = skyColor.copy(alpha = 0.65f * ringAlpha),
                                        start = Offset(cx, cy),
                                        end = Offset(cx, cy + needleLen * 0.65f),
                                        strokeWidth = 3.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                    drawCircle(
                                        color = limeColor.copy(alpha = ringAlpha),
                                        radius = 5.dp.toPx(),
                                        center = Offset(cx, cy)
                                    )
                                }
                            }
                        }

                        SplashVariant.GLASS_DROP -> {
                            val dropProg = dropFallProgress.value
                            if (dropProg < 1f) {
                                val startY = cy - minDim * 0.42f
                                val curY = startY + (cy - startY) * dropProg
                                val dropR = 14.dp.toPx()
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(skyColor, lavenderColor.copy(alpha = 0.3f)),
                                        center = Offset(cx, curY - dropR * 0.2f),
                                        radius = dropR * 1.4f
                                    ),
                                    radius = dropR,
                                    center = Offset(cx, curY)
                                )
                            }
                            val rip = rippleProgress.value
                            if (rip > 0f && rip < 1f) {
                                for (rIdx in 0..2) {
                                    val localRip = ((rip - rIdx * 0.14f) / 0.72f).coerceIn(0f, 1f)
                                    if (localRip > 0f) {
                                        drawCircle(
                                            color = skyColor.copy(alpha = (1f - localRip) * 0.55f),
                                            radius = minDim * (0.12f + localRip * 0.38f),
                                            center = Offset(cx, cy),
                                            style = Stroke(width = (3f * (1f - localRip) + 1f).dp.toPx())
                                        )
                                    }
                                }
                            }
                        }

                        SplashVariant.SONAR_POP -> {
                            val sonar = sonarProgress.value
                            for (ringIdx in 0..2) {
                                val p = ((sonar - ringIdx * 0.16f) / 0.68f).coerceIn(0f, 1f)
                                if (p > 0f) {
                                    val ringColor = when (ringIdx) {
                                        0 -> limeColor
                                        1 -> skyColor
                                        else -> lavenderColor
                                    }
                                    drawCircle(
                                        color = ringColor.copy(alpha = (1f - p * 0.75f) * 0.55f),
                                        radius = minDim * (0.14f + p * 0.34f),
                                        center = Offset(cx, cy),
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                }

                // Final logo reveal at the end of the Splash using DyneLogo(size = 160.dp) -> R.drawable.logo_app
                if (logoDAlpha.value > 0.01f) {
                    DyneLogo(
                        size = 160.dp,
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = logoDAlpha.value
                                scaleX = logoDScale.value
                                scaleY = logoDScale.value
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Wordmark "Dyne" (Plus Jakarta Sans 800) with staggered bouncy letters
            val letters = listOf("D", "y", "n", "e")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                letters.forEachIndexed { index, ch ->
                    Text(
                        text = ch,
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 40.sp,
                        letterSpacing = (-1.0).sp,
                        color = textColor,
                        modifier = Modifier
                            .offset { IntOffset(0, letterSlides[index].value.roundToInt()) }
                            .graphicsLayer {
                                alpha = letterAlphas[index].value
                                scaleX = letterScales[index].value
                                scaleY = letterScales[index].value
                            }
                    )
                }
            }
        }

        // Quick white flash overlay for MONO_DARK ending
        if (monoFlashAlpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = monoFlashAlpha.value))
            )
        }
    }
}

/**
 * Shared composable that renders the official in-app Dyne logo (`R.drawable.logo_app` -> `res/drawable/logo_app.png`)
 * with ContentScale.Fit, preserving the original aspect ratio (never stretched, skewed, or cropped),
 * with no tint and no background box.
 * Fixed sizes:
 * - Home header: 40.dp
 * - Settings header: 56.dp
 * - Paywall: 56.dp
 * - About: 96.dp
 * - Splash final reveal: 160.dp
 */
@Composable
fun DyneLogo(
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.logo_app),
        contentDescription = "Dyne",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}

@Composable
fun DyneLogoBadge(
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    DyneLogo(size = size, modifier = modifier)
}

