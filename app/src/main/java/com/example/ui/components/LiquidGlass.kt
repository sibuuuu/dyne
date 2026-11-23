package com.example.ui.components

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SoundEffectConstants
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HapticStrength
import com.example.domain.ToolMode
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.DesignTokens
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import com.example.ui.theme.ProGoldDark
import com.example.ui.theme.ProGoldDeep
import com.example.ui.theme.ProGoldGradient

/**
 * Shared bottom inset equal to tab bar height + 24.dp so every scrollable screen
 * clears the floating glass tab bar.
 */
val LocalBottomBarInset = staticCompositionLocalOf { 116.dp }

/**
 * Self-contained, binary-safe Haze backdrop state for Compose 1.7+.
 */
@Stable
class HazeState

fun Modifier.haze(state: HazeState): Modifier = this

/**
 * TapSound manager preloaded at startup using SoundPool with USAGE_ASSISTANCE_SONIFICATION
 * and CONTENT_TYPE_SONIFICATION, maxStreams 4, released in onDestroy.
 */
@Stable
class TapSound(private val appContext: Context) {

    private var soundPool: SoundPool? = null
    private var rawResId: Int = 0
    private var loadedSoundId: Int = 0
    @Volatile
    private var isSoundReady: Boolean = false
    private var lastTapRealtimeMs: Long = 0L

    @Volatile
    var tapSoundsEnabled: Boolean = true
    @Volatile
    var tapVolume: Float = 0.80f
    @Volatile
    var scanBeepEnabled: Boolean = true
    @Volatile
    var hapticsEnabled: Boolean = true
    @Volatile
    var hapticStrength: HapticStrength = HapticStrength.MEDIUM
    @Volatile
    var isCameraScannerOpen: Boolean = false
    @Volatile
    var isSoundMeterActive: Boolean = false

    init {
        runCatching {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val pool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attrs)
                .build()
            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0 && sampleId == loadedSoundId) {
                    isSoundReady = true
                }
            }
            soundPool = pool
            rawResId = appContext.resources.getIdentifier("sfx_tap", "raw", appContext.packageName)
            if (rawResId != 0) {
                loadedSoundId = pool.load(appContext, rawResId, 1)
            }
        }
    }

    fun playTap(view: View? = null, forcePreview: Boolean = false, customVolume: Float? = null) {
        if (isCameraScannerOpen || isSoundMeterActive) return

        val now = SystemClock.elapsedRealtime()
        if (!forcePreview && (now - lastTapRealtimeMs) < 60L) {
            return
        }
        lastTapRealtimeMs = now

        if (hapticsEnabled) {
            FeedbackHelper.vibrateTick(
                context = appContext,
                durationMs = hapticStrength.durationMs,
                amplitude = hapticStrength.amplitude
            )
        }

        if (!tapSoundsEnabled && !forcePreview) return

        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
        if (ringerMode == AudioManager.RINGER_MODE_SILENT || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            return
        }

        val vol = (customVolume ?: tapVolume).coerceIn(0.05f, 1.0f)
        val pool = soundPool
        if (rawResId != 0 && loadedSoundId != 0 && isSoundReady && pool != null) {
            runCatching {
                pool.play(loadedSoundId, vol, vol, 1, 0, 1.0f)
            }
        } else {
            runCatching {
                view?.playSoundEffect(SoundEffectConstants.CLICK)
            }
        }
    }

    fun release() {
        runCatching {
            soundPool?.release()
            soundPool = null
            isSoundReady = false
        }
    }
}

val LocalTapSound = staticCompositionLocalOf<TapSound?> { null }

private const val AGSL_LIQUID_GLASS_REFRACTION = """
    uniform shader content;
    uniform float2 resolution;

    half4 main(float2 coord) {
        float2 safeRes = float2(max(resolution.x, 1.0), max(resolution.y, 1.0));
        float2 uv = coord / safeRes;
        float2 centered = uv - float2(0.5, 0.5);
        float distSq = dot(centered, centered);
        float edgeFactor = smoothstep(0.14, 0.25, distSq);
        float2 offset = centered * edgeFactor * 3.0;
        half4 base = content.eval(coord - offset);
        float topSheen = smoothstep(0.30, 0.0, uv.y) * 0.035;
        return half4(base.rgb + half3(topSheen), base.a);
    }
"""

fun Modifier.liquidGlassEdgeRefraction(enabled: Boolean = true): Modifier = composed {
    val theme = LocalDyneTheme.current
    if (!enabled || theme.reduceGlassEffects || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        this
    } else {
        applyAgslRefractionImpl()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun Modifier.applyAgslRefractionImpl(): Modifier {
    val shader = remember {
        runCatching {
            RuntimeShader(AGSL_LIQUID_GLASS_REFRACTION).apply {
                setFloatUniform("resolution", 400f, 120f)
            }
        }.getOrNull()
    }
    if (shader == null) return this
    return this.graphicsLayer {
        runCatching {
            val w = size.width.coerceAtLeast(1f)
            val h = size.height.coerceAtLeast(1f)
            shader.setFloatUniform("resolution", w, h)
            renderEffect = RenderEffect
                .createRuntimeShaderEffect(shader, "content")
                .asComposeRenderEffect()
        }
    }
}

fun Modifier.dyneClickable(
    enabled: Boolean = true,
    targetScale: Float = 0.96f,
    testTag: String? = null,
    onClick: () -> Unit
): Modifier = composed {
    val tapSound = LocalTapSound.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val theme = LocalDyneTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val noAnim = theme.reduceMotion || theme.reduceGlassEffects
    val effectiveScaleTarget = if (noAnim) 1f else targetScale
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) effectiveScaleTarget else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = if (noAnim) Spring.StiffnessHigh else Spring.StiffnessMedium
        ),
        label = "dyne_clickable_scale"
    )

    this
        .let { if (testTag != null) it.testTag(testTag) else it }
        .scale(scale)
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null
        ) {
            if (tapSound != null) {
                tapSound.playTap(view = view)
            } else {
                runCatching {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
            onClick()
        }
}

fun Modifier.pressScaleClickable(
    enabled: Boolean = true,
    targetScale: Float = 0.96f,
    testTag: String? = null,
    onClick: () -> Unit
): Modifier = dyneClickable(
    enabled = enabled,
    targetScale = targetScale,
    testTag = testTag,
    onClick = onClick
)

/**
 * Edge fade modifier for horizontal chip LazyRows so clipped edges fade smoothly.
 */
fun Modifier.horizontalChipRowEdgeFade(fadeWidth: Dp = 20.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fw = fadeWidth.toPx().coerceAtMost(size.width * 0.15f)
        if (fw > 1f) {
            // Left edge fade
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startX = 0f,
                    endX = fw
                ),
                size = Size(fw, size.height),
                blendMode = BlendMode.DstIn
            )
            // Right edge fade
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = size.width - fw,
                    endX = size.width
                ),
                topLeft = Offset(size.width - fw, 0f),
                size = Size(fw, size.height),
                blendMode = BlendMode.DstIn
            )
        }
    }

/**
 * ProBorder modifier: 1dp gold gradient border (#FFD76A -> #E8A93B -> #7A5A12),
 * faint gold glow, and a slow shimmer sweep every 4 seconds (disabled when Reduce Motion is on).
 */
fun Modifier.proBorder(
    shape: Shape,
    borderWidth: Dp = 1.dp,
    @Suppress("UNUSED_PARAMETER") enableShimmer: Boolean = false
): Modifier = proBorder(
    cornerRadius = 24.dp,
    borderWidth = borderWidth,
    enableShimmer = false,
    customShape = shape
)

fun Modifier.proBorder(
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.dp,
    @Suppress("UNUSED_PARAMETER") enableShimmer: Boolean = false,
    customShape: Shape? = null
): Modifier = composed {
    val shape = customShape ?: RoundedCornerShape(cornerRadius)
    val gradientBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                ProGold.copy(alpha = 0.90f),
                ProGoldDeep.copy(alpha = 0.75f),
                ProGoldDark.copy(alpha = 0.70f),
                ProGold.copy(alpha = 0.85f)
            )
        )
    }

    this
        .border(
            width = borderWidth,
            brush = gradientBrush,
            shape = shape
        )
        .drawWithCache {
            val rPx = cornerRadius.toPx()
            val glowBrush = Brush.radialGradient(
                colors = listOf(
                    ProGold.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.8f, size.height * 0.2f),
                radius = size.maxDimension * 0.75f
            )
            onDrawWithContent {
                drawRoundRect(
                    brush = glowBrush,
                    cornerRadius = CornerRadius(rPx, rPx)
                )
                drawContent()
            }
        }
}

/**
 * ProBadge: small gold gradient pill with crown + "PRO".
 */
@Composable
fun ProBadge(
    modifier: Modifier = Modifier,
    label: String = "PRO"
) {
    val type = LocalDyneType.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(brush = ProGoldGradient)
            .border(1.dp, ProGold.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Icon(
            imageVector = Icons.Default.WorkspacePremium,
            contentDescription = label,
            tint = BentoTextBlack,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = type.caption.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.4.sp
            ),
            color = BentoTextBlack,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Gold crown chip for the corner of locked Pro tiles.
 */
@Composable
fun ProCrownChip(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(brush = ProGoldGradient)
            .border(1.dp, ProGold, CircleShape)
    ) {
        Icon(
            imageVector = Icons.Default.WorkspacePremium,
            contentDescription = "Pro Locked",
            tint = BentoTextBlack,
            modifier = Modifier.size(15.dp)
        )
    }
}

/**
 * 1-hour reward unlock gold countdown ring for temporarily unlocked Pro tiles.
 */
@Composable
fun ProCountdownRing(
    progress: Float = 0.85f,
    label: String = "1h",
    modifier: Modifier = Modifier
) {
    val type = LocalDyneType.current
    val clampedProgress = progress.coerceIn(0.08f, 1f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(32.dp)
            .drawWithCache {
                val stroke = 2.5.dp.toPx()
                val trackStyle = Stroke(width = stroke)
                val arcStyle = Stroke(width = stroke, cap = StrokeCap.Round)
                val arcBrush = Brush.sweepGradient(listOf(ProGoldDeep, ProGold, ProGoldDeep))
                onDrawBehind {
                    drawCircle(
                        color = ProGoldDark.copy(alpha = 0.45f),
                        style = trackStyle
                    )
                    drawArc(
                        brush = arcBrush,
                        startAngle = -90f,
                        sweepAngle = 360f * clampedProgress,
                        useCenter = false,
                        style = arcStyle
                    )
                }
            }
    ) {
        Text(
            text = label,
            style = type.caption.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 14.sp
            ),
            color = ProGold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = DesignTokens.CardRadius,
    content: @Composable () -> Unit
) {
    val theme = LocalDyneTheme.current
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(theme.surface)
            .border(
                width = 1.dp,
                color = if (theme.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
                shape = shape
            )
            .padding(DesignTokens.Space16)
    ) {
        content()
    }
}

/**
 * Crown button in the top bar: clean static gold glass button (no infinite pulse).
 */
@Composable
fun ProCrownTopBarButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    contentDescription: String = "Dyne Pro",
    testTag: String? = null
) {
    GlassSurface(
        hazeState = hazeState,
        shape = CircleShape,
        cornerRadius = 24.dp,
        tintColor = ProGold.copy(alpha = 0.14f),
        shadowElevation = 10.dp,
        modifier = modifier
            .size(48.dp)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(ProGold, ProGoldDeep)),
                shape = CircleShape
            )
            .dyneClickable(targetScale = 0.95f, testTag = testTag, onClick = onClick)
    ) {
        Icon(
            imageVector = Icons.Default.WorkspacePremium,
            contentDescription = contentDescription,
            tint = ProGold,
            modifier = Modifier
                .size(22.dp)
                .align(Alignment.Center)
        )
    }
}

@Composable
fun DyneSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val theme = LocalDyneTheme.current
    val tapSound = LocalTapSound.current
    val view = LocalView.current

    Switch(
        checked = checked,
        onCheckedChange = { newVal ->
            tapSound?.playTap(view = view)
            onCheckedChange(newVal)
        },
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = theme.onAccent,
            checkedTrackColor = theme.accent,
            uncheckedThumbColor = theme.textSecondary,
            uncheckedTrackColor = theme.elevated
        ),
        modifier = modifier
    )
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") hazeState: HazeState? = null,
    shape: Shape = RoundedCornerShape(28.dp),
    cornerRadius: Dp = 28.dp,
    tintColor: Color? = null,
    fillAlpha: Float = 0.15f,
    enableEdgeRefraction: Boolean = false,
    shadowElevation: Dp = 16.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val theme = LocalDyneTheme.current
    val baseBackdropColor = if (theme.isDark) {
        Color(0xFF17171C).copy(alpha = if (theme.reduceGlassEffects) 0.98f else 0.88f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = if (theme.reduceGlassEffects) 0.98f else 0.88f)
    }
    val whiteOverlay = tintColor ?: Color.White.copy(
        alpha = if (theme.isDark) {
            if (theme.reduceGlassEffects) 0.06f else fillAlpha.coerceIn(0.12f, 0.18f)
        } else {
            0.72f
        }
    )

    val topLeftBorder = if (theme.isDark) {
        Color.White.copy(alpha = 0.48f)
    } else {
        Color.White.copy(alpha = 0.95f)
    }
    val bottomRightBorder = Color.White.copy(alpha = 0.03f)

    Box(
        modifier = modifier
            .shadow(
                elevation = if (theme.reduceGlassEffects) 4.dp else shadowElevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.45f),
                spotColor = Color.Black.copy(alpha = 0.45f)
            )
            .clip(shape)
            .liquidGlassEdgeRefraction(enabled = enableEdgeRefraction && !theme.reduceGlassEffects)
            .background(baseBackdropColor)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        whiteOverlay.copy(alpha = (whiteOverlay.alpha + 0.05f).coerceAtMost(0.95f)),
                        whiteOverlay
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        topLeftBorder,
                        Color.White.copy(alpha = 0.16f),
                        bottomRightBorder
                    )
                ),
                shape = shape
            )
            .drawWithContent {
                drawContent()
                if (!theme.reduceGlassEffects) {
                    val rPx = cornerRadius.toPx()
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (theme.isDark) 0.28f else 0.65f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = (size.height * 0.28f).coerceAtLeast(8f)
                        ),
                        cornerRadius = CornerRadius(rPx, rPx),
                        style = Stroke(width = 1.25.dp.toPx())
                    )
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = if (theme.isDark) 0.24f else 0.08f)
                            ),
                            startY = size.height * 0.65f,
                            endY = size.height
                        ),
                        cornerRadius = CornerRadius(rPx, rPx),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
            .padding(contentPadding),
        content = content
    )
}

/**
 * Reusable Liquid Glass Button:
 * - 52dp height, 26dp corner radius, 24dp icon, 15sp SemiBold button label.
 * - widthIn(min = 96.dp), maxLines = 1, softWrap = false so buttons beside weighted elements never squeeze.
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    hazeState: HazeState? = null,
    accentFill: Color? = null,
    textColor: Color? = null,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val resolvedTextColor = textColor ?: if (accentFill != null) {
        theme.onAccent
    } else {
        theme.textPrimary
    }

    GlassSurface(
        hazeState = hazeState,
        shape = DesignTokens.ButtonShape,
        cornerRadius = DesignTokens.ButtonRadius,
        tintColor = accentFill,
        shadowElevation = 6.dp,
        contentPadding = PaddingValues(horizontal = DesignTokens.Space16, vertical = DesignTokens.Space12),
        modifier = modifier
            .height(DesignTokens.ButtonHeight)
            .widthIn(min = 96.dp)
            .dyneClickable(
                enabled = enabled,
                targetScale = 0.96f,
                testTag = testTag,
                onClick = onClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.align(Alignment.Center)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = resolvedTextColor,
                    modifier = Modifier.size(DesignTokens.IconSize)
                )
                Spacer(modifier = Modifier.width(DesignTokens.Space8))
            }
            Text(
                text = text,
                style = type.button,
                color = resolvedTextColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    iconTint: Color? = null,
    testTag: String? = null
) {
    val theme = LocalDyneTheme.current
    GlassSurface(
        hazeState = hazeState,
        shape = CircleShape,
        cornerRadius = DesignTokens.CardRadius,
        shadowElevation = 4.dp,
        modifier = modifier
            .size(DesignTokens.MinTouchTarget)
            .dyneClickable(targetScale = 0.96f, testTag = testTag, onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint ?: theme.accent,
            modifier = Modifier
                .size(DesignTokens.IconSize)
                .align(Alignment.Center)
        )
    }
}

data class GlassTabItem(
    val title: String,
    val icon: ImageVector,
    val tag: String
)

/**
 * Floating pill glass bottom bar with 4 tabs (Home, Outdoors, Library, Settings)
 * plus a raised circular lime glass Scan button in the center.
 * Design kept untouched per specification.
 */
@Composable
fun GlassTabBar(
    tabs: List<GlassTabItem>,
    selectedIndex: Int,
    visible: Boolean,
    hazeState: HazeState?,
    onTabSelected: (Int) -> Unit,
    onScanClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isScanMenuOpen: Boolean = false
) {
    val theme = LocalDyneTheme.current

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
        ) {
            GlassSurface(
                hazeState = hazeState,
                shape = RoundedCornerShape(36.dp),
                cornerRadius = 36.dp,
                fillAlpha = 0.15f,
                enableEdgeRefraction = true,
                shadowElevation = 22.dp,
                contentPadding = PaddingValues(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val slotWidth = maxWidth / 5f
                    val targetSlotIndex = if (selectedIndex < 2) selectedIndex else selectedIndex + 1

                    val capsuleOffset by animateDpAsState(
                        targetValue = slotWidth * targetSlotIndex,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = if (theme.reduceMotion) Spring.StiffnessHigh else Spring.StiffnessMediumLow
                        ),
                        label = "tab_capsule_spring"
                    )

                    Box(
                        modifier = Modifier
                            .offset(x = capsuleOffset)
                            .width(slotWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = if (theme.isDark) 0.22f else 0.85f),
                                        theme.accent.copy(alpha = 0.18f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.65f),
                                        theme.accent.copy(alpha = 0.25f)
                                    )
                                ),
                                shape = RoundedCornerShape(30.dp)
                            )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        tabs.take(2).forEachIndexed { idx, tab ->
                            TabSlotItem(
                                tab = tab,
                                isSelected = selectedIndex == idx,
                                onClick = { onTabSelected(idx) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        tabs.drop(2).forEachIndexed { offsetIdx, tab ->
                            val actualIdx = offsetIdx + 2
                            TabSlotItem(
                                tab = tab,
                                isSelected = selectedIndex == actualIdx,
                                onClick = { onTabSelected(actualIdx) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Raised Circular Glass Scan Button in the Center ("+" rotates to "x" with bounce spring)
            val plusRotation by animateFloatAsState(
                targetValue = if (isScanMenuOpen) 45f else 0f,
                animationSpec = if (theme.reduceMotion) {
                    spring(stiffness = Spring.StiffnessHigh)
                } else {
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                },
                label = "plus_to_x_rotation"
            )

            GlassSurface(
                hazeState = hazeState,
                shape = CircleShape,
                cornerRadius = 32.dp,
                tintColor = theme.accent.copy(alpha = 0.96f),
                shadowElevation = 20.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(62.dp)
                    .dyneClickable(
                        targetScale = 0.94f,
                        testTag = "center_lime_scan_button",
                        onClick = onScanClicked
                    )
            ) {
                Canvas(
                    modifier = Modifier
                        .size(26.dp)
                        .align(Alignment.Center)
                        .graphicsLayer {
                            rotationZ = plusRotation
                        }
                ) {
                    val strokeW = 3.2.dp.toPx()
                    val half = size.minDimension * 0.42f
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawLine(
                        color = theme.onAccent,
                        start = Offset(cx - half, cy),
                        end = Offset(cx + half, cy),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = theme.onAccent,
                        start = Offset(cx, cy - half),
                        end = Offset(cx, cy + half),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
private fun TabSlotItem(
    tab: GlassTabItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val color = if (isSelected) theme.textPrimary else theme.textSecondary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .dyneClickable(
                targetScale = 0.96f,
                testTag = tab.tag,
                onClick = onClick
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                tint = if (isSelected) theme.accent else color,
                modifier = Modifier.size(21.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tab.title,
                style = type.caption.copy(
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = color,
                maxLines = 1
            )
        }
    }
}

/**
 * Sheets and pickers get an opaque surface (95% alpha) so the screen behind never shows through.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassSheet(
    onDismissRequest: () -> Unit,
    hazeState: HazeState? = null,
    content: @Composable () -> Unit
) {
    val theme = LocalDyneTheme.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val opaqueSheetBg = theme.surface.copy(alpha = 0.95f)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(opaqueSheetBg)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = if (theme.isDark) 0.14f else 0.45f),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
                .padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(theme.textSecondary.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.height(16.dp))
                content()
            }
        }
    }
}

@Composable
fun CoinBalanceTopBarChip(
    coinBalance: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    testTag: String = "home_coin_chip"
) {
    val type = LocalDyneType.current
    GlassSurface(
        hazeState = hazeState,
        shape = RoundedCornerShape(22.dp),
        cornerRadius = 22.dp,
        tintColor = ProGold.copy(alpha = 0.16f),
        shadowElevation = 8.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = modifier
            .height(42.dp)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(ProGold, ProGoldDeep)),
                shape = RoundedCornerShape(22.dp)
            )
            .dyneClickable(targetScale = 0.95f, testTag = testTag, onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(brush = ProGoldGradient)
            ) {
                Text(
                    text = "C",
                    style = type.caption.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        lineHeight = 14.sp
                    ),
                    color = BentoTextBlack
                )
            }
            Text(
                text = coinBalance.toString(),
                style = type.caption.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                ),
                color = ProGold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun SharedTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    hazeState: HazeState? = null,
    leadingLogo: (@Composable () -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    rightExtraContent: (@Composable () -> Unit)? = null,
    rightActionIcon: ImageVector? = null,
    rightActionDescription: String = "",
    rightActionTestTag: String? = null,
    onRightActionClick: (() -> Unit)? = null
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.ScreenMargin, vertical = DesignTokens.Space12)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (onBackClick != null) {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    hazeState = hazeState,
                    onClick = onBackClick,
                    testTag = "top_bar_back_button"
                )
                Spacer(modifier = Modifier.width(DesignTokens.Space12))
            } else if (leadingLogo != null) {
                leadingLogo()
                Spacer(modifier = Modifier.width(DesignTokens.Space12))
            }
            Column(modifier = Modifier.weight(1f)) {
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = type.caption,
                        color = theme.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = title,
                    style = type.headline,
                    color = theme.textPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(DesignTokens.Space8))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Space8)
        ) {
            if (rightExtraContent != null) {
                rightExtraContent()
            }
            if (rightActionIcon != null && onRightActionClick != null) {
                val isCrownIcon = rightActionIcon == Icons.Default.WorkspacePremium ||
                    rightActionDescription.contains("Pro", ignoreCase = true)
                if (isCrownIcon) {
                    ProCrownTopBarButton(
                        onClick = onRightActionClick,
                        hazeState = hazeState,
                        contentDescription = rightActionDescription,
                        testTag = rightActionTestTag
                    )
                } else {
                    GlassIconButton(
                        icon = rightActionIcon,
                        contentDescription = rightActionDescription,
                        hazeState = hazeState,
                        iconTint = theme.accent,
                        onClick = onRightActionClick,
                        testTag = rightActionTestTag
                    )
                }
            }
        }
    }
}

@Composable
fun DyneSegmentedControl(
    segments: List<String>,
    selectedIndex: Int,
    onSegmentSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    proSegmentIndices: Set<Int> = emptySet(),
    proIndices: Set<Int> = emptySet(),
    isProUnlocked: Boolean = false
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val effectiveProIndices = proSegmentIndices + proIndices

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(22.dp))
            .background(theme.surface)
            .border(1.dp, Color.White.copy(alpha = if (theme.isDark) 0.07f else 0.25f), RoundedCornerShape(22.dp))
            .padding(4.dp)
    ) {
        val segWidth = maxWidth / segments.size.coerceAtLeast(1)
        val offset by animateDpAsState(
            targetValue = segWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = if (theme.reduceMotion) Spring.StiffnessHigh else Spring.StiffnessMediumLow
            ),
            label = "segment_slide"
        )

        Box(
            modifier = Modifier
                .offset(x = offset)
                .width(segWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(theme.accent)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            segments.forEachIndexed { idx, label ->
                val selected = idx == selectedIndex
                val isProSeg = idx in effectiveProIndices
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .dyneClickable(
                            targetScale = 0.96f,
                            testTag = "segment_${label.lowercase().replace(" ", "_")}",
                            onClick = { onSegmentSelected(idx) }
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = label,
                            style = type.caption.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = if (selected) theme.onAccent else theme.textSecondary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isProSeg && !isProUnlocked) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Pro",
                                tint = if (selected) BentoTextBlack else ProGold,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Glass Mode Switcher under the top bar for multi-mode tools:
 * - Scrollable chip row with a sliding glass indicator
 * - Locked modes show a gold crown and trigger onLockedModeSelected (opens the 1h Unlock sheet)
 * - Long-pressing a mode chip pins/unpins it as its own Home favorite with haptic feedback
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassModeSwitcherBar(
    modes: List<ToolMode>,
    selectedIndex: Int,
    isModeUnlocked: (ToolMode) -> Boolean,
    isModePinned: (ToolMode) -> Boolean,
    onModeSelected: (Int, ToolMode) -> Unit,
    onLockedModeSelected: (Int, ToolMode) -> Unit,
    onTogglePinMode: (ToolMode) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    if (modes.isEmpty()) return
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    GlassSurface(
        hazeState = hazeState,
        shape = RoundedCornerShape(24.dp),
        cornerRadius = 24.dp,
        fillAlpha = 0.14f,
        shadowElevation = 8.dp,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 5.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalChipRowEdgeFade(14.dp)
                .horizontalScroll(scrollState)
                .padding(horizontal = 4.dp)
        ) {
            modes.forEachIndexed { idx, mode ->
                val isSelected = idx == selectedIndex
                val unlocked = !mode.isPremium || isModeUnlocked(mode)
                val pinned = isModePinned(mode)

                val bgAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "mode_chip_bg_$idx"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (bgAlpha > 0.01f) {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        theme.accent.copy(alpha = 0.92f * bgAlpha),
                                        theme.accent.copy(alpha = 0.78f * bgAlpha)
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, Color.Transparent)
                                )
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = when {
                                isSelected -> Color.White.copy(alpha = 0.55f)
                                mode.isPremium && !unlocked -> ProGold.copy(alpha = 0.45f)
                                else -> Color.White.copy(alpha = if (theme.isDark) 0.08f else 0.24f)
                            },
                            shape = RoundedCornerShape(18.dp)
                        )
                        .combinedClickable(
                            onClick = {
                                FeedbackHelper.tick(context)
                                if (unlocked) {
                                    onModeSelected(idx, mode)
                                } else {
                                    onLockedModeSelected(idx, mode)
                                }
                            },
                            onLongClick = {
                                if (mode.id != "more") {
                                    FeedbackHelper.heavyClick(context)
                                    onTogglePinMode(mode)
                                }
                            }
                        )
                        .testTag("tool_mode_chip_${mode.id}")
                        .padding(horizontal = 13.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = mode.icon,
                            contentDescription = mode.title,
                            tint = when {
                                isSelected -> theme.onAccent
                                mode.isPremium && !unlocked -> ProGold
                                else -> theme.textSecondary
                            },
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = mode.title,
                            style = type.caption.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = if (isSelected) theme.onAccent else theme.textPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (mode.isPremium && !unlocked) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Premium Mode",
                                tint = if (isSelected) BentoTextBlack else ProGold,
                                modifier = Modifier.size(14.dp)
                            )
                        } else if (pinned && mode.id != "more") {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned to Home",
                                tint = if (isSelected) theme.onAccent else theme.accent,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassPermissionCard(
    icon: ImageVector,
    title: String,
    explanation: String,
    buttonText: String,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    secondaryActionText: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    GlassSurface(
        hazeState = hazeState,
        shape = RoundedCornerShape(28.dp),
        cornerRadius = 28.dp,
        fillAlpha = 0.16f,
        enableEdgeRefraction = true,
        contentPadding = PaddingValues(22.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(theme.accent.copy(alpha = 0.18f))
                    .border(1.dp, theme.accent.copy(alpha = 0.55f), CircleShape)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = theme.accent,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = type.title,
                color = theme.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = explanation,
                style = type.body,
                color = theme.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            GlassButton(
                text = buttonText,
                accentFill = theme.accent,
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
                testTag = "permission_grant_button"
            )
            if (secondaryActionText != null && onSecondaryAction != null) {
                Spacer(modifier = Modifier.height(10.dp))
                GlassButton(
                    text = secondaryActionText,
                    onClick = onSecondaryAction,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun DesignedEmptyStateCard(
    title: String,
    description: String,
    accentColor: Color,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(theme.surface)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = if (theme.isDark) 0.07f else 0.25f),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Canvas(modifier = Modifier.size(88.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val r = size.minDimension / 2f
                drawCircle(color = accentColor.copy(alpha = 0.10f), radius = r, center = center)
                drawCircle(color = accentColor.copy(alpha = 0.22f), radius = r * 0.68f, center = center)
                drawCircle(color = accentColor, radius = r * 0.36f, center = center, style = Stroke(width = 3.dp.toPx()))
                drawCircle(color = accentColor, radius = 5.dp.toPx(), center = center)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = type.title,
                color = theme.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = type.body,
                color = theme.textSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(18.dp))
                GlassButton(
                    text = actionLabel,
                    accentFill = accentColor,
                    onClick = onActionClick
                )
            }
        }
    }
}

@Composable
fun DesignedStateBanner(
    badge: String,
    title: String,
    message: String,
    accentColor: Color,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(theme.surface)
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(10.dp)) {
                    drawCircle(color = accentColor)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = badge.uppercase(),
                    style = type.section,
                    color = accentColor
                )
            }
            Text(text = title, style = type.title, color = theme.textPrimary)
            Text(text = message, style = type.body, color = theme.textSecondary)
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                GlassButton(
                    text = actionText,
                    accentFill = accentColor,
                    onClick = onAction
                )
            }
        }
    }
}

@Composable
fun DesignedLoadingCard(
    label: String = "Calibrating local sensors...",
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(theme.surface)
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                color = theme.accent,
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(text = label, style = type.body, color = theme.textSecondary)
        }
    }
}

object FeedbackHelper {
    private var toneGenerator: ToneGenerator? = null

    @Volatile
    var hapticsEnabledGlobal: Boolean = true
    @Volatile
    var hapticStrengthGlobal: HapticStrength = HapticStrength.MEDIUM
    @Volatile
    var scanBeepEnabledGlobal: Boolean = true

    private fun getToneGen(): ToneGenerator? {
        if (toneGenerator == null) {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
            } catch (_: Exception) {
                toneGenerator = null
            }
        }
        return toneGenerator
    }

    fun vibrateTick(
        context: Context,
        durationMs: Long = hapticStrengthGlobal.durationMs,
        amplitude: Int = hapticStrengthGlobal.amplitude
    ) {
        if (!hapticsEnabledGlobal) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        durationMs.coerceAtLeast(8L),
                        amplitude.coerceIn(1, 255)
                    )
                )
            }
        } catch (_: Exception) {
        }
    }

    fun tick(context: Context) {
        vibrateTick(context, durationMs = hapticStrengthGlobal.durationMs, amplitude = hapticStrengthGlobal.amplitude)
    }

    fun hapticTick(context: Context) {
        tick(context)
    }

    fun confirmClick(context: Context) {
        vibrateTick(
            context,
            durationMs = (hapticStrengthGlobal.durationMs + 6L),
            amplitude = (hapticStrengthGlobal.amplitude + 30).coerceAtMost(255)
        )
    }

    fun heavyClick(context: Context) {
        vibrateTick(
            context,
            durationMs = (hapticStrengthGlobal.durationMs + 14L),
            amplitude = (hapticStrengthGlobal.amplitude + 65).coerceAtMost(255)
        )
    }

    fun playBeep(durationMs: Int = 45, toneType: Int = ToneGenerator.TONE_PROP_BEEP) {
        if (!scanBeepEnabledGlobal) return
        try {
            getToneGen()?.startTone(toneType, durationMs)
        } catch (_: Exception) {
        }
    }
}
