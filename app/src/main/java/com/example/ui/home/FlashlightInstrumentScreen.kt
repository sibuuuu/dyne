package com.example.ui.home

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.LocalFeatureGate
import com.example.billing.PremiumFeature
import com.example.service.FlashlightTileService
import com.example.ui.components.DyneSegmentedControl
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassSurface
import com.example.ui.components.ProBadge
import com.example.ui.components.SharedTopBar
import com.example.ui.components.pressScaleClickable
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.components.FeedbackHelper
import com.example.ui.components.HazeState
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val TrackGreenStart = Color(0xFF16A34A)
private val TrackGreenMid = Color(0xFF22C55E)
private val TrackYellow = Color(0xFFFACC15)
private val TrackOrange = Color(0xFFFB923C)
private val TrackRedMax = Color(0xFFEF4444)

enum class ScreenLightPreset(
    val id: String,
    val label: String,
    val kelvinLabel: String,
    val color: Color,
    val isPremiumPreset: Boolean
) {
    READING("reading", "Reading", "4500K Crisp", Color(0xFFFFF8EA), true),
    WARM("warm", "Warm", "2700K Cozy", Color(0xFFFFD180), true),
    NIGHT_RED("night_red", "Night red", "630nm Red", Color(0xFFFF2424), true),
    CAMPING("camping", "Camping", "2200K Lantern", Color(0xFFFFA726), true)
}

enum class BlinkPatternPreset(
    val id: String,
    val label: String,
    val steps: List<Boolean>
) {
    DOUBLE_PULSE("double", "Double Pulse", listOf(true, false, true, false, false, false, false, false)),
    BEACON("beacon", "Beacon", listOf(true, true, false, false, false, false, false, false)),
    HEARTBEAT("heartbeat", "Heartbeat", listOf(true, false, true, true, false, false, false, false)),
    TACTICAL("tactical", "Tactical Burst", listOf(true, false, true, false, true, false, false, false)),
    CUSTOM("custom", "Custom 8-Step", listOf(true, false, true, true, false, true, false, false))
}

private val MORSE_CODE_MAP: Map<Char, String> = mapOf(
    'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
    'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
    'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
    'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
    'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
    'Z' to "--..",
    '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
    '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----."
)

fun encodeToMorseDisplay(text: String): String {
    return text.uppercase(Locale.US).mapNotNull { ch ->
        if (ch == ' ') "/" else MORSE_CODE_MAP[ch]
    }.joinToString(" ")
}

/**
 * Rebuilt FLASHLIGHT instrument:
 * - Uses CameraManager.turnOnTorchWithStrengthLevel with FLASH_INFO_STRENGTH_MAXIMUM_LEVEL.
 * - Starts at the MIDDLE (level = max(1, max/2)) with thumb in the middle, labeled "Medium".
 * - Color-coded track: left half GREEN (very dim up to medium), right half blends green, yellow, orange, and RED toward a red "MAX" label.
 * - Free users slide freely from lowest level to the middle; at the middle a gold lock stops them and dragging past it opens the 1-hour Unlock sheet.
 * - Premium and 1-hour unlocks get the full range, a haptic "MAX" detent, and a battery note.
 * - Animated beam glow follows the intensity; haptic ticks at detents.
 * - If the phone has no strength control (maxLevel <= 1): hides the hardware slider and offers On/Off, Soft Screen Light (free up to 50%), Strobe, SOS, and an auto-off timer. Never fakes dimming by rapid flashing.
 * - Free extras: SOS, auto-off timer, keep-screen-on.
 * - Premium extras: Strobe with speed slider & BPM sync, Morse sender (type text), custom blink patterns, shake-to-toggle, color-temperature Screen Light with presets (Reading, Warm, Night red, Camping), Quick Settings tile (TileService).
 */
@Composable
fun FlashlightInstrumentScreen(
    hasCameraPermission: Boolean,
    isPro: Boolean = false,
    onOpenPaywall: () -> Unit = {},
    onRequestCameraPermission: () -> Unit,
    forcedModeId: String? = null,
    hazeState: HazeState? = null
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val featureGate = LocalFeatureGate.current
    val resolvedMode = (forcedModeId ?: "normal").lowercase(Locale.US)
    val isBrightnessMode = resolvedMode == "brightness" || resolvedMode == "flashlight_brightness"
    val isHighUnlocked = isPro || (
        featureGate?.hasAccess(
            when (resolvedMode) {
                "brightness", "flashlight_brightness" -> "flashlight_brightness"
                "strobe", "flashlight_strobe" -> "flashlight_strobe"
                "morse", "flashlight_morse" -> "flashlight_morse"
                "colors", "screen_colors" -> "screen_colors"
                else -> "flashlight_brightness"
            }
        ) == true
        )

    val cameraManager = remember {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }

    val (torchCameraId, hardwareMaxStrengthLevel) = remember(cameraManager) {
        var foundId: String? = null
        var maxLvl = 1
        runCatching {
            cameraManager?.cameraIdList?.forEach { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                if (hasFlash && foundId == null) {
                    foundId = id
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        maxLvl = (chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1)
                            .coerceAtLeast(1)
                    }
                }
            }
        }
        foundId to maxLvl
    }

    val hasHardwareStrengthControl = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && hardwareMaxStrengthLevel > 1
    val maxLevel = hardwareMaxStrengthLevel.coerceAtLeast(1)
    val middleLevel = remember(maxLevel) { max(1, maxLevel / 2) }

    // On open the intensity starts at the MIDDLE (level = max(1, max/2)) and the thumb sits in the middle (0.5f), labeled "Medium".
    var sliderFraction by rememberSaveable { mutableFloatStateOf(0.5f) }
    var currentStrengthLevel by rememberSaveable(maxLevel) { mutableIntStateOf(middleLevel) }

    var isTorchOn by rememberSaveable { mutableStateOf(false) }
    var pulseState by remember { mutableStateOf(false) }

    // Active mode: 0 = Steady/Normal, 1 = SOS (Free), 2 = Strobe (Pro), 3 = Morse (Pro), 4 = Patterns (Pro)
    var activeModeIndex by rememberSaveable(resolvedMode) {
        mutableIntStateOf(
            when (resolvedMode) {
                "sos", "flashlight_sos" -> 1
                "strobe", "flashlight_strobe" -> 2
                "morse", "flashlight_morse" -> 3
                "patterns" -> 4
                else -> 0
            }
        )
    }

    // Free extras: Keep screen on + Auto-off timer
    var keepScreenOn by rememberSaveable { mutableStateOf(true) }
    val timerOptionsMinutes = remember { listOf(0, 1, 2, 5, 10, 15, 30) }
    var selectedTimerMinutes by rememberSaveable(resolvedMode) {
        mutableIntStateOf(if (resolvedMode == "timer" || resolvedMode == "flashlight_timer") 5 else 0)
    }
    var remainingTimerSeconds by rememberSaveable { mutableIntStateOf(0) }

    // Full-screen Soft Screen Light overlay state
    var showFullScreenLight by rememberSaveable(resolvedMode) {
        mutableStateOf(false)
    }
    var selectedScreenPreset by rememberSaveable { mutableStateOf(ScreenLightPreset.READING.id) }
    var screenColorTempKelvin by rememberSaveable { mutableFloatStateOf(4500f) }
    var screenBrightnessFraction by rememberSaveable { mutableFloatStateOf(0.5f) }

    // Premium Strobe + BPM sync state
    var strobeHz by rememberSaveable { mutableFloatStateOf(2.0f) }
    var bpmSyncEnabled by rememberSaveable { mutableStateOf(false) }
    var strobeBpm by rememberSaveable { mutableFloatStateOf(120f) }
    val tapTimestamps = remember { mutableStateListOf<Long>() }

    // Premium Morse sender state
    var morseText by rememberSaveable { mutableStateOf("SOS") }
    var activeMorseCharIndex by remember { mutableIntStateOf(-1) }
    var activeMorseSymbol by remember { mutableStateOf("") }

    // Premium Custom Blink Patterns state
    var selectedPatternId by rememberSaveable { mutableStateOf(BlinkPatternPreset.DOUBLE_PULSE.id) }
    val customPatternSteps = remember {
        mutableStateListOf(true, false, true, true, false, true, false, false)
    }
    var patternStepDurationMs by rememberSaveable { mutableLongStateOf(180L) }
    var currentPatternStepIndex by remember { mutableIntStateOf(0) }

    // Premium Shake-to-toggle state
    var shakeToToggleEnabled by rememberSaveable { mutableStateOf(false) }
    var qsTileFeedbackMessage by remember { mutableStateOf<String?>(null) }

    // Keep screen on effect
    DisposableEffect(keepScreenOn) {
        val window = (context as? Activity)?.window
        if (keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    fun fractionToStrengthLevel(fraction: Float): Int {
        if (maxLevel <= 1) return 1
        val clamped = fraction.coerceIn(0f, 1f)
        return if (clamped <= 0.5f) {
            val t = clamped / 0.5f
            (1 + t * (middleLevel - 1)).roundToInt().coerceIn(1, middleLevel)
        } else {
            val t = (clamped - 0.5f) / 0.5f
            (middleLevel + t * (maxLevel - middleLevel)).roundToInt().coerceIn(middleLevel, maxLevel)
        }
    }

    fun applyHardwareTorch(active: Boolean, level: Int = currentStrengthLevel) {
        val camId = torchCameraId ?: return
        runCatching {
            if (!active) {
                cameraManager?.setTorchMode(camId, false)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && hardwareMaxStrengthLevel > 1) {
                val safeLevel = if (isBrightnessMode && isHighUnlocked) {
                    level.coerceIn(1, hardwareMaxStrengthLevel)
                } else {
                    middleLevel.coerceIn(1, hardwareMaxStrengthLevel)
                }
                cameraManager?.turnOnTorchWithStrengthLevel(camId, safeLevel)
            } else {
                // Never fake dimming by rapid flashing when maxLevel <= 1
                cameraManager?.setTorchMode(camId, true)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            applyHardwareTorch(false)
        }
    }

    // Shake-to-toggle accelerometer listener (Premium)
    DisposableEffect(shakeToToggleEnabled, isHighUnlocked, hasCameraPermission) {
        if (!shakeToToggleEnabled || !isHighUnlocked) {
            return@DisposableEffect onDispose {}
        }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastShakeMs = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val values = event?.values ?: return
                if (values.size < 3) return
                val mag = sqrt(
                    values[0] * values[0] +
                        values[1] * values[1] +
                        values[2] * values[2]
                )
                val delta = kotlin.math.abs(mag - SensorManager.GRAVITY_EARTH)
                val now = System.currentTimeMillis()
                if (delta > 13.5f && now - lastShakeMs > 700L) {
                    lastShakeMs = now
                    if (hasCameraPermission) {
                        FeedbackHelper.vibrateTick(context, durationMs = 40L, amplitude = 220)
                        isTorchOn = !isTorchOn
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    // Auto-off countdown timer
    LaunchedEffect(isTorchOn, selectedTimerMinutes) {
        if (!isTorchOn || selectedTimerMinutes <= 0) {
            remainingTimerSeconds = selectedTimerMinutes * 60
            return@LaunchedEffect
        }
        remainingTimerSeconds = selectedTimerMinutes * 60
        while (isActive && isTorchOn && remainingTimerSeconds > 0) {
            delay(1000L)
            remainingTimerSeconds -= 1
            if (remainingTimerSeconds <= 0) {
                isTorchOn = false
                FeedbackHelper.vibrateTick(context, durationMs = 50L, amplitude = 200)
            }
        }
    }

    // Torch output loop (Steady, SOS, Strobe + BPM, Morse sender, Custom Blink Patterns)
    LaunchedEffect(
        isTorchOn,
        activeModeIndex,
        currentStrengthLevel,
        strobeHz,
        bpmSyncEnabled,
        strobeBpm,
        morseText,
        selectedPatternId,
        patternStepDurationMs,
        customPatternSteps.toList(),
        isHighUnlocked
    ) {
        if (!isTorchOn) {
            pulseState = false
            activeMorseCharIndex = -1
            activeMorseSymbol = ""
            applyHardwareTorch(false)
            return@LaunchedEffect
        }

        when (activeModeIndex) {
            0 -> { // Steady
                pulseState = true
                applyHardwareTorch(true, currentStrengthLevel)
            }
            1 -> { // Free SOS (... --- ...)
                val timings = listOf(
                    180L to true, 180L to false, 180L to true, 180L to false, 180L to true, 420L to false,
                    540L to true, 180L to false, 540L to true, 180L to false, 540L to true, 420L to false,
                    180L to true, 180L to false, 180L to true, 180L to false, 180L to true, 1000L to false
                )
                while (isActive && isTorchOn) {
                    for ((dur, on) in timings) {
                        if (!isActive || !isTorchOn) break
                        pulseState = on
                        applyHardwareTorch(on, currentStrengthLevel)
                        delay(dur)
                    }
                }
            }
            2 -> { // Premium Strobe (Hz or BPM sync)
                val effectiveHz = if (bpmSyncEnabled) {
                    (strobeBpm / 60f).coerceIn(0.5f, 20f)
                } else {
                    strobeHz.coerceIn(1f, 20f)
                }
                while (isActive && isTorchOn) {
                    val halfPeriodMs = (500f / effectiveHz).toLong().coerceAtLeast(25L)
                    pulseState = !pulseState
                    applyHardwareTorch(pulseState, currentStrengthLevel)
                    delay(halfPeriodMs)
                }
            }
            3 -> { // Premium Morse Sender (type text)
                val cleanText = morseText.uppercase(Locale.US).ifBlank { "SOS" }
                val unitMs = 160L
                while (isActive && isTorchOn) {
                    for ((charIdx, ch) in cleanText.withIndex()) {
                        if (!isActive || !isTorchOn) break
                        activeMorseCharIndex = charIdx
                        if (ch == ' ') {
                            activeMorseSymbol = "SPACE"
                            pulseState = false
                            applyHardwareTorch(false)
                            delay(unitMs * 7)
                            continue
                        }
                        val code = MORSE_CODE_MAP[ch] ?: continue
                        activeMorseSymbol = "$ch ($code)"
                        for ((sIdx, sym) in code.withIndex()) {
                            if (!isActive || !isTorchOn) break
                            val onDur = if (sym == '.') unitMs else unitMs * 3
                            pulseState = true
                            applyHardwareTorch(true, currentStrengthLevel)
                            delay(onDur)
                            pulseState = false
                            applyHardwareTorch(false)
                            if (sIdx < code.lastIndex) {
                                delay(unitMs)
                            }
                        }
                        delay(unitMs * 3) // inter-letter gap
                    }
                    activeMorseCharIndex = -1
                    activeMorseSymbol = "LOOP PAUSE"
                    delay(unitMs * 7)
                }
            }
            4 -> { // Premium Custom Blink Patterns
                val preset = BlinkPatternPreset.entries.firstOrNull { it.id == selectedPatternId }
                    ?: BlinkPatternPreset.DOUBLE_PULSE
                val steps = if (preset == BlinkPatternPreset.CUSTOM) {
                    customPatternSteps.toList()
                } else {
                    preset.steps
                }
                var idx = 0
                while (isActive && isTorchOn) {
                    currentPatternStepIndex = idx
                    val on = steps.getOrElse(idx) { false }
                    pulseState = on
                    applyHardwareTorch(on, currentStrengthLevel)
                    delay(patternStepDurationMs.coerceIn(60L, 600L))
                    idx = (idx + 1) % steps.size.coerceAtLeast(1)
                }
            }
        }
    }

    if (showFullScreenLight) {
        ScreenTorchFullScreenScreen(
            hazeState = hazeState,
            isHighUnlocked = isHighUnlocked,
            initialPresetId = selectedScreenPreset,
            initialBrightness = if (isHighUnlocked) screenBrightnessFraction else screenBrightnessFraction.coerceAtMost(0.5f),
            initialKelvin = screenColorTempKelvin,
            onRequestUnlockHigh = { onGranted ->
                if (featureGate != null) {
                    featureGate.require(PremiumFeature.FLASHLIGHT_HIGH) {
                        onGranted()
                    }
                } else {
                    onOpenPaywall()
                }
            },
            onBack = {
                showFullScreenLight = false
            }
        )
        return
    }

    val intensityPercent = (sliderFraction * 100).roundToInt().coerceIn(5, 100)
    val intensityLabel = when {
        sliderFraction < 0.22f -> "Very Dim"
        sliderFraction < 0.45f -> "Low"
        sliderFraction in 0.45f..0.55f -> "Medium"
        sliderFraction < 0.80f -> "High"
        sliderFraction < 0.96f -> "Very High"
        else -> "MAX"
    }
    val singleValueDisplay = "$intensityLabel - $intensityPercent%"

    val toggleTorchAction: () -> Unit = {
        if (!hasCameraPermission) {
            onRequestCameraPermission()
        }
        FeedbackHelper.vibrateTick(context, durationMs = 30L, amplitude = 200)
        isTorchOn = !isTorchOn
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("flashlight_instrument_root")
    ) {
        when (resolvedMode) {
            "brightness", "flashlight_brightness" -> {
                FlashlightBrightnessScreen(
                    isTorchOn = isTorchOn,
                    pulseState = pulseState,
                    sliderFraction = sliderFraction,
                    singleValueDisplay = singleValueDisplay,
                    remainingTimerSeconds = if (isTorchOn && selectedTimerMinutes > 0) remainingTimerSeconds else 0,
                    hasHardwareStrengthControl = hasHardwareStrengthControl,
                    isHighUnlocked = isHighUnlocked,
                    shakeToToggleEnabled = shakeToToggleEnabled,
                    qsTileFeedbackMessage = qsTileFeedbackMessage,
                    onToggleTorch = toggleTorchAction,
                    onFractionChanged = { candidate ->
                        val newLevel = fractionToStrengthLevel(candidate)
                        if (newLevel != currentStrengthLevel) {
                            if (newLevel == maxLevel && isHighUnlocked) {
                                FeedbackHelper.vibrateTick(context, durationMs = 38L, amplitude = 255)
                            } else {
                                FeedbackHelper.hapticTick(context)
                            }
                        }
                        sliderFraction = candidate
                        currentStrengthLevel = newLevel
                        if (isTorchOn && activeModeIndex == 0) {
                            applyHardwareTorch(true, newLevel)
                        }
                    },
                    onLockedPastMiddle = {
                        sliderFraction = 0.5f
                        currentStrengthLevel = middleLevel
                        FeedbackHelper.vibrateTick(context, durationMs = 25L, amplitude = 180)
                        if (featureGate != null) {
                            featureGate.require(PremiumFeature.FLASHLIGHT_HIGH)
                        } else {
                            onOpenPaywall()
                        }
                    },
                    onOpenSoftScreenLight = { showFullScreenLight = true },
                    onToggleShake = { enabled ->
                        if (!isHighUnlocked) {
                            if (featureGate != null) {
                                featureGate.require(PremiumFeature.FLASHLIGHT_HIGH) {
                                    shakeToToggleEnabled = true
                                }
                            } else {
                                onOpenPaywall()
                            }
                        } else {
                            FeedbackHelper.hapticTick(context)
                            shakeToToggleEnabled = enabled
                        }
                    },
                    onAddQuickSettingsTile = {
                        if (!isHighUnlocked) {
                            if (featureGate != null) {
                                featureGate.require(PremiumFeature.FLASHLIGHT_HIGH) {
                                    FlashlightTileService.requestAddQuickSettingsTile(context) { msg ->
                                        qsTileFeedbackMessage = msg
                                    }
                                }
                            } else {
                                onOpenPaywall()
                            }
                        } else {
                            FlashlightTileService.requestAddQuickSettingsTile(context) { msg ->
                                qsTileFeedbackMessage = msg
                            }
                        }
                    }
                )
            }

            "strobe", "flashlight_strobe" -> {
                FlashlightStrobeScreen(
                    isTorchOn = isTorchOn,
                    pulseState = pulseState,
                    strobeHz = strobeHz,
                    bpmSyncEnabled = bpmSyncEnabled,
                    strobeBpm = strobeBpm,
                    remainingTimerSeconds = if (isTorchOn && selectedTimerMinutes > 0) remainingTimerSeconds else 0,
                    onToggleTorch = toggleTorchAction,
                    onStrobeHzChange = {
                        bpmSyncEnabled = false
                        strobeHz = it
                    },
                    onToggleBpmSync = { bpmSyncEnabled = it },
                    onBpmChange = {
                        bpmSyncEnabled = true
                        strobeBpm = it
                        strobeHz = (it / 60f).coerceIn(1f, 20f)
                    },
                    onTapTempo = {
                        val now = System.currentTimeMillis()
                        if (tapTimestamps.isNotEmpty() && now - tapTimestamps.last() > 2500L) {
                            tapTimestamps.clear()
                        }
                        tapTimestamps.add(now)
                        if (tapTimestamps.size > 5) {
                            tapTimestamps.removeAt(0)
                        }
                        if (tapTimestamps.size >= 2) {
                            val intervals = tapTimestamps.zipWithNext { a, b -> b - a }
                            val avgMs = intervals.average().toFloat().coerceAtLeast(250f)
                            val computedBpm = (60_000f / avgMs).coerceIn(60f, 240f)
                            bpmSyncEnabled = true
                            strobeBpm = computedBpm
                            strobeHz = (computedBpm / 60f).coerceIn(1f, 20f)
                        }
                        FeedbackHelper.hapticTick(context)
                    }
                )
            }

            "sos", "flashlight_sos" -> {
                FlashlightSosScreen(
                    isTorchOn = isTorchOn,
                    pulseState = pulseState,
                    keepScreenOn = keepScreenOn,
                    timerOptionsMinutes = timerOptionsMinutes,
                    selectedTimerMinutes = selectedTimerMinutes,
                    remainingTimerSeconds = remainingTimerSeconds,
                    onToggleSos = toggleTorchAction,
                    onSelectTimerMinutes = { mins ->
                        FeedbackHelper.hapticTick(context)
                        selectedTimerMinutes = mins
                    },
                    onToggleKeepScreenOn = { enabled ->
                        FeedbackHelper.hapticTick(context)
                        keepScreenOn = enabled
                    }
                )
            }

            "morse", "flashlight_morse" -> {
                FlashlightMorseScreen(
                    isTorchOn = isTorchOn,
                    pulseState = pulseState,
                    morseText = morseText,
                    activeMorseCharIndex = activeMorseCharIndex,
                    activeMorseSymbol = activeMorseSymbol,
                    remainingTimerSeconds = if (isTorchOn && selectedTimerMinutes > 0) remainingTimerSeconds else 0,
                    onToggleTorch = toggleTorchAction,
                    onMorseTextChange = { morseText = it.take(36) }
                )
            }

            "screen_light" -> {
                ScreenLightScreen(
                    brightnessFraction = screenBrightnessFraction,
                    isHighUnlocked = isHighUnlocked,
                    onBrightnessFractionChanged = { screenBrightnessFraction = it },
                    onLockedPastMiddle = {
                        screenBrightnessFraction = 0.5f
                        FeedbackHelper.vibrateTick(context, durationMs = 25L, amplitude = 180)
                        if (featureGate != null) {
                            featureGate.require(PremiumFeature.FLASHLIGHT_HIGH)
                        } else {
                            onOpenPaywall()
                        }
                    },
                    onOpenFullScreenLight = {
                        selectedScreenPreset = ScreenLightPreset.READING.id
                        showFullScreenLight = true
                    }
                )
            }

            "colors", "screen_colors" -> {
                ScreenColorsScreen(
                    selectedPresetId = selectedScreenPreset,
                    brightnessFraction = screenBrightnessFraction,
                    kelvin = screenColorTempKelvin,
                    isHighUnlocked = isHighUnlocked,
                    onSelectPreset = { preset ->
                        selectedScreenPreset = preset.id
                        FeedbackHelper.hapticTick(context)
                    },
                    onKelvinChanged = { screenColorTempKelvin = it },
                    onBrightnessFractionChanged = { screenBrightnessFraction = it },
                    onLockedPastMiddle = {
                        screenBrightnessFraction = 0.5f
                        FeedbackHelper.vibrateTick(context, durationMs = 25L, amplitude = 180)
                        if (featureGate != null) {
                            featureGate.require(PremiumFeature.FLASHLIGHT_HIGH)
                        } else {
                            onOpenPaywall()
                        }
                    },
                    onOpenFullScreenLight = { showFullScreenLight = true }
                )
            }

            "timer", "flashlight_timer" -> {
                FlashlightTimerScreen(
                    isTorchOn = isTorchOn,
                    pulseState = pulseState,
                    timerOptionsMinutes = timerOptionsMinutes.filter { it > 0 },
                    selectedTimerMinutes = if (selectedTimerMinutes > 0) selectedTimerMinutes else 5,
                    remainingTimerSeconds = remainingTimerSeconds,
                    keepScreenOn = keepScreenOn,
                    onToggleTorch = {
                        if (selectedTimerMinutes <= 0) selectedTimerMinutes = 5
                        toggleTorchAction()
                    },
                    onSelectTimerMinutes = { mins ->
                        FeedbackHelper.hapticTick(context)
                        selectedTimerMinutes = mins
                    },
                    onToggleKeepScreenOn = { enabled ->
                        FeedbackHelper.hapticTick(context)
                        keepScreenOn = enabled
                    }
                )
            }

            else -> {
                // Default: FlashlightNormalScreen (power button, single status chip, optional timer)
                FlashlightNormalScreen(
                    isTorchOn = isTorchOn,
                    pulseState = pulseState,
                    timerOptionsMinutes = timerOptionsMinutes,
                    selectedTimerMinutes = selectedTimerMinutes,
                    remainingTimerSeconds = remainingTimerSeconds,
                    keepScreenOn = keepScreenOn,
                    onToggleTorch = toggleTorchAction,
                    onSelectTimerMinutes = { mins ->
                        FeedbackHelper.hapticTick(context)
                        selectedTimerMinutes = mins
                    },
                    onToggleKeepScreenOn = { enabled ->
                        FeedbackHelper.hapticTick(context)
                        keepScreenOn = enabled
                    }
                )
            }
        }
    }
}

@Composable
fun PhotosensitivityWarningDialog(
    onDismiss: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.elevated,
        icon = {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "Photosensitivity Warning",
                tint = StatusWarning,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Photosensitivity Warning",
                style = type.cardTitle,
                color = theme.textPrimary
            )
        },
        text = {
            Text(
                text = "Strobe, SOS, and Morse flashing lights may trigger seizures in individuals with photosensitive epilepsy. Avoid pointing flashing LED light directly at eyes.",
                style = type.body,
                color = theme.textSecondary
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("photosensitivity_dialog_confirm")
            ) {
                Text(
                    text = "I Understand",
                    style = type.body.copy(fontWeight = FontWeight.Bold),
                    color = theme.accent
                )
            }
        }
    )
}

@Composable
fun FlashlightNormalScreen(
    isTorchOn: Boolean,
    pulseState: Boolean,
    timerOptionsMinutes: List<Int>,
    selectedTimerMinutes: Int,
    remainingTimerSeconds: Int,
    keepScreenOn: Boolean,
    onToggleTorch: () -> Unit,
    onSelectTimerMinutes: (Int) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("flashlight_normal_screen")
    ) {
        AnimatedBeamTorchHero(
            isTorchOn = isTorchOn,
            pulseState = pulseState,
            intensityFraction = 0.5f,
            intensityLabel = if (isTorchOn) "Active • ON" else "Standby • OFF",
            remainingTimerSeconds = if (isTorchOn && selectedTimerMinutes > 0) remainingTimerSeconds else 0,
            onToggleTorch = onToggleTorch
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(theme.elevated.copy(alpha = 0.72f))
                .border(1.dp, theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Optional Auto-Off Timer", style = type.caption.copy(fontWeight = FontWeight.Bold), color = theme.textPrimary)
                if (selectedTimerMinutes > 0 && remainingTimerSeconds > 0) {
                    val m = remainingTimerSeconds / 60
                    val s = remainingTimerSeconds % 60
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", m, s),
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.accent
                    )
                } else {
                    Text(
                        text = if (selectedTimerMinutes == 0) "Off" else "${selectedTimerMinutes}m",
                        style = type.caption,
                        color = theme.textSecondary
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                timerOptionsMinutes.forEach { mins ->
                    val selected = selectedTimerMinutes == mins
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) theme.accent.copy(alpha = 0.22f) else theme.surface)
                            .border(1.dp, if (selected) theme.accent else theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .clickable { onSelectTimerMinutes(mins) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("flashlight_timer_${mins}m")
                    ) {
                        Text(
                            text = if (mins == 0) "Off" else "${mins}m",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) theme.accent else theme.textPrimary
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LightMode,
                        contentDescription = null,
                        tint = theme.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text("Keep Screen On", style = type.body.copy(fontWeight = FontWeight.SemiBold), color = theme.textPrimary)
                        Text("Prevent sleep while torch is open", style = type.caption, color = theme.textSecondary)
                    }
                }
                Switch(
                    checked = keepScreenOn,
                    onCheckedChange = onToggleKeepScreenOn,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BentoTextBlack,
                        checkedTrackColor = theme.accent
                    ),
                    modifier = Modifier.testTag("flashlight_keep_screen_on_switch")
                )
            }
        }
    }
}

@Composable
fun FlashlightBrightnessScreen(
    isTorchOn: Boolean,
    pulseState: Boolean,
    sliderFraction: Float,
    singleValueDisplay: String,
    remainingTimerSeconds: Int,
    hasHardwareStrengthControl: Boolean,
    isHighUnlocked: Boolean,
    shakeToToggleEnabled: Boolean,
    qsTileFeedbackMessage: String?,
    onToggleTorch: () -> Unit,
    onFractionChanged: (Float) -> Unit,
    onLockedPastMiddle: () -> Unit,
    onOpenSoftScreenLight: () -> Unit,
    onToggleShake: (Boolean) -> Unit,
    onAddQuickSettingsTile: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("flashlight_brightness_screen")
    ) {
        AnimatedBeamTorchHero(
            isTorchOn = isTorchOn,
            pulseState = pulseState,
            intensityFraction = sliderFraction,
            intensityLabel = singleValueDisplay,
            remainingTimerSeconds = remainingTimerSeconds,
            onToggleTorch = onToggleTorch
        )

        if (hasHardwareStrengthControl) {
            ColorCodedIntensitySliderCard(
                title = "Torch Strength",
                subtitle = singleValueDisplay,
                fraction = sliderFraction,
                isHighUnlocked = isHighUnlocked,
                showBatteryNote = isHighUnlocked && sliderFraction > 0.5f,
                onFractionChanged = onFractionChanged,
                onLockedPastMiddle = onLockedPastMiddle,
                testTag = "flashlight_strength_slider"
            )
        } else {
            NoStrengthControlFallbackCard(
                isTorchOn = isTorchOn,
                activeModeIndex = 0,
                isHighUnlocked = isHighUnlocked,
                onToggleTorch = onToggleTorch,
                onOpenSoftScreenLight = onOpenSoftScreenLight,
                onTriggerSos = {},
                onTriggerStrobe = {}
            )
        }

        PremiumFlashlightExtrasCard(
            isHighUnlocked = isHighUnlocked,
            shakeToToggleEnabled = shakeToToggleEnabled,
            qsTileFeedbackMessage = qsTileFeedbackMessage,
            onToggleShake = onToggleShake,
            onAddQuickSettingsTile = onAddQuickSettingsTile
        )
    }
}

@Composable
fun FlashlightStrobeScreen(
    isTorchOn: Boolean,
    pulseState: Boolean,
    strobeHz: Float,
    bpmSyncEnabled: Boolean,
    strobeBpm: Float,
    remainingTimerSeconds: Int,
    onToggleTorch: () -> Unit,
    onStrobeHzChange: (Float) -> Unit,
    onToggleBpmSync: (Boolean) -> Unit,
    onBpmChange: (Float) -> Unit,
    onTapTempo: () -> Unit
) {
    val readout = if (bpmSyncEnabled) {
        String.format(Locale.US, "%.0f BPM - %.1f Hz", strobeBpm, strobeBpm / 60f)
    } else {
        String.format(Locale.US, "%.1f Hz - %.0f BPM", strobeHz, strobeHz * 60f)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("flashlight_strobe_screen")
    ) {
        AnimatedBeamTorchHero(
            isTorchOn = isTorchOn,
            pulseState = pulseState,
            intensityFraction = 0.75f,
            intensityLabel = readout,
            remainingTimerSeconds = remainingTimerSeconds,
            onToggleTorch = onToggleTorch
        )

        StrobeBpmControlCard(
            strobeHz = strobeHz,
            bpmSyncEnabled = bpmSyncEnabled,
            strobeBpm = strobeBpm,
            onStrobeHzChange = onStrobeHzChange,
            onToggleBpmSync = onToggleBpmSync,
            onBpmChange = onBpmChange,
            onTapTempo = onTapTempo
        )
    }
}

@Composable
fun FlashlightSosScreen(
    isTorchOn: Boolean,
    pulseState: Boolean,
    keepScreenOn: Boolean,
    timerOptionsMinutes: List<Int>,
    selectedTimerMinutes: Int,
    remainingTimerSeconds: Int,
    onToggleSos: () -> Unit,
    onSelectTimerMinutes: (Int) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("flashlight_sos_screen")
    ) {
        AnimatedBeamTorchHero(
            isTorchOn = isTorchOn,
            pulseState = pulseState,
            intensityFraction = 0.85f,
            intensityLabel = if (isTorchOn) "SOS (... --- ...) • Active" else "SOS (... --- ...) • Standby",
            remainingTimerSeconds = if (isTorchOn && selectedTimerMinutes > 0) remainingTimerSeconds else 0,
            onToggleTorch = onToggleSos
        )

        FreeFlashlightExtrasCard(
            isSosActive = isTorchOn,
            timerOptionsMinutes = timerOptionsMinutes,
            selectedTimerMinutes = selectedTimerMinutes,
            remainingTimerSeconds = remainingTimerSeconds,
            keepScreenOn = keepScreenOn,
            onToggleSos = onToggleSos,
            onSelectTimerMinutes = onSelectTimerMinutes,
            onToggleKeepScreenOn = onToggleKeepScreenOn
        )
    }
}

@Composable
fun FlashlightMorseScreen(
    isTorchOn: Boolean,
    pulseState: Boolean,
    morseText: String,
    activeMorseCharIndex: Int,
    activeMorseSymbol: String,
    remainingTimerSeconds: Int,
    onToggleTorch: () -> Unit,
    onMorseTextChange: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("flashlight_morse_screen")
    ) {
        AnimatedBeamTorchHero(
            isTorchOn = isTorchOn,
            pulseState = pulseState,
            intensityFraction = 0.75f,
            intensityLabel = if (isTorchOn && activeMorseSymbol.isNotEmpty()) {
                "Sending: $activeMorseSymbol"
            } else {
                "Morse Ready"
            },
            remainingTimerSeconds = remainingTimerSeconds,
            onToggleTorch = onToggleTorch
        )

        MorseSenderCard(
            morseText = morseText,
            activeMorseCharIndex = activeMorseCharIndex,
            activeMorseSymbol = activeMorseSymbol,
            isTorchOn = isTorchOn,
            onMorseTextChange = onMorseTextChange,
            onToggleTransmit = onToggleTorch
        )
    }
}

@Composable
fun ScreenLightScreen(
    brightnessFraction: Float,
    isHighUnlocked: Boolean,
    onBrightnessFractionChanged: (Float) -> Unit,
    onLockedPastMiddle: () -> Unit,
    onOpenFullScreenLight: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val pct = (brightnessFraction * 100).roundToInt().coerceIn(5, 100)
    val levelName = when {
        brightnessFraction < 0.25f -> "Soft"
        brightnessFraction <= 0.55f -> "Medium"
        brightnessFraction < 0.85f -> "High"
        else -> "MAX"
    }
    val singleReadout = "$levelName - $pct%"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("screen_light_screen")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Color(0xFFFFF8EA).copy(
                        alpha = (0.28f + 0.72f * if (isHighUnlocked) brightnessFraction else brightnessFraction.coerceAtMost(0.5f))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
                .clickable(onClick = onOpenFullScreenLight)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = "Open Full Screen Lamp",
                    tint = BentoTextBlack,
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = "Tap for Full-Screen Lamp",
                    style = type.cardTitle,
                    color = BentoTextBlack
                )
                Text(
                    text = singleReadout,
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = BentoTextBlack.copy(alpha = 0.75f)
                )
            }
        }

        ColorCodedIntensitySliderCard(
            title = "Screen Light Brightness",
            subtitle = singleReadout,
            fraction = brightnessFraction,
            isHighUnlocked = isHighUnlocked,
            showBatteryNote = false,
            onFractionChanged = onBrightnessFractionChanged,
            onLockedPastMiddle = onLockedPastMiddle,
            testTag = "screen_light_inline_slider"
        )
    }
}

@Composable
fun ScreenColorsScreen(
    selectedPresetId: String,
    brightnessFraction: Float,
    kelvin: Float,
    isHighUnlocked: Boolean,
    onSelectPreset: (ScreenLightPreset) -> Unit,
    onKelvinChanged: (Float) -> Unit,
    onBrightnessFractionChanged: (Float) -> Unit,
    onLockedPastMiddle: () -> Unit,
    onOpenFullScreenLight: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val activePreset = ScreenLightPreset.entries.firstOrNull { it.id == selectedPresetId }
        ?: ScreenLightPreset.READING
    val pct = (brightnessFraction * 100).roundToInt().coerceIn(5, 100)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("screen_colors_screen")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(activePreset.color.copy(alpha = (0.35f + 0.65f * brightnessFraction)))
                .border(1.dp, activePreset.color, RoundedCornerShape(28.dp))
                .clickable(onClick = onOpenFullScreenLight)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Open Full Screen Color Lamp",
                    tint = BentoTextBlack,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = "${activePreset.label} • ${activePreset.kelvinLabel}",
                    style = type.cardTitle,
                    color = BentoTextBlack
                )
                Text(
                    text = "Tap for Full Screen",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = BentoTextBlack.copy(alpha = 0.78f)
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(theme.elevated.copy(alpha = 0.72f))
                .border(1.dp, theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Text("Color Presets", style = type.cardTitle, color = theme.textPrimary)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                ScreenLightPreset.entries.forEach { preset ->
                    val isSelected = selectedPresetId == preset.id
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) preset.color.copy(alpha = 0.24f) else theme.surface)
                            .border(
                                1.dp,
                                if (isSelected) preset.color else theme.textSecondary.copy(alpha = 0.25f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectPreset(preset) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("screen_light_preset_${preset.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(preset.color)
                        )
                        Text(
                            text = preset.label,
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.textPrimary
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Color Temperature", style = type.caption.copy(fontWeight = FontWeight.SemiBold), color = theme.textSecondary)
                    Text("${kelvin.roundToInt()}K", style = type.caption.copy(fontWeight = FontWeight.Bold), color = activePreset.color)
                }
                Slider(
                    value = kelvin,
                    onValueChange = onKelvinChanged,
                    valueRange = 2000f..6500f,
                    colors = SliderDefaults.colors(
                        thumbColor = activePreset.color,
                        activeTrackColor = activePreset.color
                    )
                )
            }
        }

        ColorCodedIntensitySliderCard(
            title = "Screen Brightness",
            subtitle = "${activePreset.label} - $pct%",
            fraction = brightnessFraction,
            isHighUnlocked = isHighUnlocked,
            showBatteryNote = false,
            onFractionChanged = onBrightnessFractionChanged,
            onLockedPastMiddle = onLockedPastMiddle,
            testTag = "screen_colors_brightness_slider"
        )
    }
}

@Composable
fun FlashlightTimerScreen(
    isTorchOn: Boolean,
    pulseState: Boolean,
    timerOptionsMinutes: List<Int>,
    selectedTimerMinutes: Int,
    remainingTimerSeconds: Int,
    keepScreenOn: Boolean,
    onToggleTorch: () -> Unit,
    onSelectTimerMinutes: (Int) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val timerDisplay = if (isTorchOn && remainingTimerSeconds > 0) {
        val m = remainingTimerSeconds / 60
        val s = remainingTimerSeconds % 60
        String.format(Locale.US, "Auto-off in %02d:%02d", m, s)
    } else {
        "Preset - ${selectedTimerMinutes}m"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("flashlight_timer_screen")
    ) {
        AnimatedBeamTorchHero(
            isTorchOn = isTorchOn,
            pulseState = pulseState,
            intensityFraction = 0.5f,
            intensityLabel = timerDisplay,
            remainingTimerSeconds = if (isTorchOn) remainingTimerSeconds else 0,
            onToggleTorch = onToggleTorch
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(theme.elevated.copy(alpha = 0.72f))
                .border(1.dp, theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Text("Countdown Duration", style = type.cardTitle, color = theme.textPrimary)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                timerOptionsMinutes.forEach { mins ->
                    val selected = selectedTimerMinutes == mins
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) theme.accent.copy(alpha = 0.22f) else theme.surface)
                            .border(1.dp, if (selected) theme.accent else theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .clickable { onSelectTimerMinutes(mins) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("flashlight_timer_preset_${mins}m")
                    ) {
                        Text(
                            text = "${mins} min",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) theme.accent else theme.textPrimary
                        )
                    }
                }
            }

            GlassButton(
                text = if (isTorchOn) "Stop Timer Torch" else "Start ${selectedTimerMinutes}m Torch",
                icon = Icons.Default.Timer,
                accentFill = if (isTorchOn) StatusDanger.copy(alpha = 0.28f) else theme.accent.copy(alpha = 0.24f),
                onClick = onToggleTorch,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FlashlightModeBar(
    selectedModeIndex: Int,
    isHighUnlocked: Boolean,
    onSelectMode: (Int) -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val items = listOf(
        Triple(0, "Steady", false),
        Triple(1, "SOS", false),
        Triple(2, "Strobe", true),
        Triple(3, "Morse", true),
        Triple(4, "Patterns", true)
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        items.forEach { (idx, label, isPremium) ->
            val selected = selectedModeIndex == idx
            val locked = isPremium && !isHighUnlocked
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selected) theme.accent.copy(alpha = 0.22f)
                        else theme.elevated.copy(alpha = 0.75f)
                    )
                    .border(
                        1.dp,
                        when {
                            selected -> theme.accent
                            locked -> ProGold.copy(alpha = 0.45f)
                            else -> theme.textSecondary.copy(alpha = 0.25f)
                        },
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelectMode(idx) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("flashlight_mode_chip_$idx")
            ) {
                Text(
                    text = label,
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = if (selected) theme.accent else theme.textPrimary
                )
                if (locked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked Pro Mode",
                        tint = ProGold,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedBeamTorchHero(
    isTorchOn: Boolean,
    pulseState: Boolean,
    intensityFraction: Float,
    intensityLabel: String,
    remainingTimerSeconds: Int,
    onToggleTorch: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val activeBeam = isTorchOn && pulseState
    val animatedBeamAlpha by animateFloatAsState(
        targetValue = if (activeBeam) (0.25f + 0.75f * intensityFraction).coerceIn(0.25f, 1f) else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "beam_glow_alpha"
    )
    val animatedBeamSpread by animateFloatAsState(
        targetValue = if (activeBeam) (0.55f + 0.45f * intensityFraction) else 0.35f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "beam_spread"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "beam_pulse")
    val haloBreath by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_breath"
    )

    val beamColor = remember(intensityFraction) {
        when {
            intensityFraction <= 0.5f -> TrackGreenMid
            intensityFraction <= 0.72f -> TrackYellow
            intensityFraction <= 0.88f -> TrackOrange
            else -> TrackRedMax
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(225.dp)
    ) {
        // Animated Beam Glow Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.56f)
            if (animatedBeamAlpha > 0.01f) {
                val beamTopWidth = size.width * 0.82f * animatedBeamSpread
                val beamBottomWidth = 84.dp.toPx()
                val beamPath = Path().apply {
                    moveTo(center.x - beamBottomWidth / 2f, center.y)
                    lineTo(center.x - beamTopWidth / 2f, 0f)
                    lineTo(center.x + beamTopWidth / 2f, 0f)
                    lineTo(center.x + beamBottomWidth / 2f, center.y)
                    close()
                }
                drawPath(
                    path = beamPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            beamColor.copy(alpha = 0.02f),
                            beamColor.copy(alpha = 0.22f * animatedBeamAlpha),
                            beamColor.copy(alpha = 0.48f * animatedBeamAlpha)
                        ),
                        startY = 0f,
                        endY = center.y
                    )
                )

                val outerRadius = 112.dp.toPx() * animatedBeamSpread * haloBreath
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            beamColor.copy(alpha = 0.46f * animatedBeamAlpha),
                            beamColor.copy(alpha = 0.16f * animatedBeamAlpha),
                            Color.Transparent
                        ),
                        center = center,
                        radius = outerRadius
                    ),
                    radius = outerRadius,
                    center = center
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassSurface(
                shape = CircleShape,
                cornerRadius = 80.dp,
                tintColor = if (activeBeam) beamColor.copy(alpha = 0.88f) else null,
                shadowElevation = if (isTorchOn) 24.dp else 10.dp,
                modifier = Modifier
                    .size(152.dp)
                    .pressScaleClickable(
                        targetScale = 0.94f,
                        testTag = "flashlight_big_toggle_button",
                        onClick = onToggleTorch
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        contentDescription = "Toggle Flashlight",
                        tint = if (activeBeam) BentoTextBlack else theme.accent,
                        modifier = Modifier.size(46.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isTorchOn) "ON" else "OFF",
                        style = type.gaugeMedium,
                        color = if (activeBeam) BentoTextBlack else theme.textPrimary
                    )
                    if (remainingTimerSeconds > 0) {
                        val m = remainingTimerSeconds / 60
                        val s = remainingTimerSeconds % 60
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", m, s),
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = if (activeBeam) BentoTextBlack.copy(alpha = 0.8f) else theme.accent
                        )
                    }
                }
            }

            // Current intensity pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.elevated.copy(alpha = 0.85f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isTorchOn) beamColor else theme.textSecondary)
                )
                Text(
                    text = intensityLabel,
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.textPrimary
                )
            }
        }
    }
}

/**
 * Color-coded intensity slider (Dim to MAX, green-to-red track, starting at Medium):
 * - Left half (0%..50%) is GREEN (very dim up to "Medium" at 50%).
 * - Right half (50%..100%) blends green, yellow, orange, and RED toward a red "MAX" label.
 * - Works across the full 0%..100% range during a 1-hour unlock of flashlight_brightness or when isPro.
 */
@Composable
fun ColorCodedIntensitySliderCard(
    title: String,
    subtitle: String,
    fraction: Float,
    isHighUnlocked: Boolean,
    showBatteryNote: Boolean,
    onFractionChanged: (Float) -> Unit,
    onLockedPastMiddle: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "color_coded_intensity_slider"
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(theme.elevated.copy(alpha = 0.72f))
            .border(1.dp, theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = type.cardTitle,
                    color = theme.textPrimary
                )
                Text(
                    text = subtitle,
                    style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = when {
                        fraction >= 0.95f -> TrackRedMax
                        fraction > 0.5f -> TrackOrange
                        else -> TrackGreenMid
                    }
                )
            }
        }

        // Interactive Color-Coded Track (Dim to MAX, Green-to-Red, starting at Medium)
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag(testTag)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val raw = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onFractionChanged(raw)
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val raw = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            onFractionChanged(raw)
                        }
                    )
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                val trackHeight = 14.dp.toPx()
                val trackY = (size.height - trackHeight) / 2f
                val midX = size.width * 0.5f

                // Left half: GREEN (very dim up to medium)
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(TrackGreenStart.copy(alpha = 0.55f), TrackGreenMid),
                        startX = 0f,
                        endX = midX
                    ),
                    topLeft = Offset(0f, trackY),
                    size = Size(midX, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                )

                // Right half: blends green, yellow, orange and RED toward MAX
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            TrackGreenMid.copy(alpha = 0.95f),
                            TrackYellow.copy(alpha = 0.95f),
                            TrackOrange.copy(alpha = 0.95f),
                            TrackRedMax.copy(alpha = 0.95f)
                        ),
                        startX = midX - 6.dp.toPx(),
                        endX = size.width
                    ),
                    topLeft = Offset(midX - 6.dp.toPx(), trackY),
                    size = Size(size.width - midX + 6.dp.toPx(), trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                )

                // Detent ticks along the track
                for (step in 1..9) {
                    val x = size.width * (step / 10f)
                    val isMiddle = step == 5
                    drawLine(
                        color = if (isMiddle) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.45f),
                        start = Offset(x, trackY - if (isMiddle) 5.dp.toPx() else 2.dp.toPx()),
                        end = Offset(x, trackY + trackHeight + if (isMiddle) 5.dp.toPx() else 2.dp.toPx()),
                        strokeWidth = if (isMiddle) 2.5.dp.toPx() else 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Active Thumb
                val thumbX = (size.width * fraction.coerceIn(0f, 1f))
                    .coerceIn(14.dp.toPx(), size.width - 14.dp.toPx())
                val thumbColor = when {
                    fraction <= 0.5f -> TrackGreenMid
                    fraction <= 0.72f -> TrackYellow
                    fraction <= 0.88f -> TrackOrange
                    else -> TrackRedMax
                }
                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = 15.dp.toPx(),
                    center = Offset(thumbX, size.height / 2f + 2.dp.toPx())
                )
                drawCircle(
                    color = Color.White,
                    radius = 13.dp.toPx(),
                    center = Offset(thumbX, size.height / 2f)
                )
                drawCircle(
                    color = thumbColor,
                    radius = 9.dp.toPx(),
                    center = Offset(thumbX, size.height / 2f)
                )
            }
        }

        // Labels Row: Dim (Green) | Medium (50%) | Red "MAX" Label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Dim",
                style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                color = TrackGreenMid
            )
            Text(
                text = "Medium",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = if (fraction in 0.45f..0.55f) TrackGreenMid else theme.textSecondary
            )
            Text(
                text = "MAX",
                style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                color = TrackRedMax,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(TrackRedMax.copy(alpha = 0.16f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        // Battery & thermal note when above Medium
        if (showBatteryNote) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TrackOrange.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryAlert,
                    contentDescription = null,
                    tint = TrackOrange,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Battery note: High LED intensity above Medium increases battery & thermal draw.",
                    style = type.caption,
                    color = theme.textPrimary
                )
            }
        }
    }
}

@Composable
private fun NoStrengthControlFallbackCard(
    isTorchOn: Boolean,
    activeModeIndex: Int,
    isHighUnlocked: Boolean,
    onToggleTorch: () -> Unit,
    onOpenSoftScreenLight: () -> Unit,
    onTriggerSos: () -> Unit,
    onTriggerStrobe: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(theme.elevated.copy(alpha = 0.72f))
            .border(1.dp, theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Single-Level LED Hardware",
            style = type.cardTitle,
            color = theme.textPrimary
        )
        Text(
            text = "This phone provides On/Off rear LED control without variable strength levels. Use Soft Screen Light for smooth brightness & color temperature (never faked by rapid LED flashing).",
            style = type.caption,
            color = theme.textSecondary
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButton(
                text = if (isTorchOn) "Turn Off" else "Turn On",
                icon = if (isTorchOn) Icons.Default.FlashlightOff else Icons.Default.FlashlightOn,
                accentFill = if (isTorchOn) theme.accent else null,
                onClick = onToggleTorch,
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                text = "Soft Screen Light",
                icon = Icons.Default.Smartphone,
                onClick = onOpenSoftScreenLight,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButton(
                text = "SOS Beacon",
                icon = Icons.Default.Bolt,
                accentFill = if (isTorchOn && activeModeIndex == 1) StatusDanger.copy(alpha = 0.3f) else null,
                onClick = onTriggerSos,
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                text = if (isHighUnlocked) "Strobe" else "Strobe • PRO",
                icon = Icons.Default.AutoAwesome,
                accentFill = if (!isHighUnlocked) ProGold.copy(alpha = 0.2f) else null,
                textColor = if (!isHighUnlocked) ProGold else null,
                onClick = onTriggerStrobe,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StrobeBpmControlCard(
    strobeHz: Float,
    bpmSyncEnabled: Boolean,
    strobeBpm: Float,
    onStrobeHzChange: (Float) -> Unit,
    onToggleBpmSync: (Boolean) -> Unit,
    onBpmChange: (Float) -> Unit,
    onTapTempo: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(theme.elevated.copy(alpha = 0.72f))
            .border(1.dp, theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text("Strobe Speed & BPM Sync", style = type.cardTitle, color = theme.textPrimary)
                Text(
                    text = if (bpmSyncEnabled) {
                        String.format(Locale.US, "%.0f BPM (%.2f Hz)", strobeBpm, strobeBpm / 60f)
                    } else {
                        String.format(Locale.US, "%.1f Hz (%.0f BPM)", strobeHz, strobeHz * 60f)
                    },
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.accent
                )
            }
            GlassButton(
                text = "Tap BPM",
                icon = Icons.Default.MusicNote,
                accentFill = if (bpmSyncEnabled) theme.accent.copy(alpha = 0.25f) else null,
                onClick = onTapTempo,
                testTag = "flashlight_strobe_tap_bpm"
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Frequency (Hz)", style = type.caption.copy(fontWeight = FontWeight.SemiBold), color = theme.textSecondary)
                Text(String.format(Locale.US, "%.1f Hz", strobeHz), style = type.caption, color = theme.accent)
            }
            Slider(
                value = strobeHz,
                onValueChange = onStrobeHzChange,
                valueRange = 1.0f..20.0f,
                colors = SliderDefaults.colors(
                    thumbColor = theme.accent,
                    activeTrackColor = theme.accent
                )
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("BPM Sync (60 – 240 BPM)", style = type.caption.copy(fontWeight = FontWeight.SemiBold), color = theme.textSecondary)
                Text(String.format(Locale.US, "%.0f BPM", strobeBpm), style = type.caption, color = ProGold)
            }
            Slider(
                value = strobeBpm,
                onValueChange = onBpmChange,
                valueRange = 60f..240f,
                colors = SliderDefaults.colors(
                    thumbColor = ProGold,
                    activeTrackColor = ProGold
                )
            )
        }
    }
}

@Composable
private fun MorseSenderCard(
    morseText: String,
    activeMorseCharIndex: Int,
    activeMorseSymbol: String,
    isTorchOn: Boolean,
    onMorseTextChange: (String) -> Unit,
    onToggleTransmit: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val encodedPreview = remember(morseText) { encodeToMorseDisplay(morseText) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(theme.elevated.copy(alpha = 0.72f))
            .border(1.dp, theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text("Morse Code Sender", style = type.cardTitle, color = theme.textPrimary)
                Text(
                    text = if (isTorchOn && activeMorseSymbol.isNotEmpty()) {
                        "Transmitting: $activeMorseSymbol"
                    } else {
                        "Type message to transmit via LED torch"
                    },
                    style = type.caption,
                    color = if (isTorchOn) theme.accent else theme.textSecondary
                )
            }
            GlassButton(
                text = if (isTorchOn) "Stop" else "Send",
                icon = Icons.Default.GraphicEq,
                accentFill = if (isTorchOn) StatusDanger.copy(alpha = 0.28f) else theme.accent.copy(alpha = 0.24f),
                onClick = onToggleTransmit,
                testTag = "flashlight_morse_send_button"
            )
        }

        OutlinedTextField(
            value = morseText,
            onValueChange = onMorseTextChange,
            label = { Text("Message to send in Morse") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = theme.textPrimary,
                unfocusedTextColor = theme.textPrimary,
                focusedBorderColor = theme.accent,
                unfocusedBorderColor = theme.textSecondary.copy(alpha = 0.25f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("flashlight_morse_input")
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(theme.surface.copy(alpha = 0.65f))
                .padding(10.dp)
        ) {
            Text(
                text = encodedPreview.ifBlank { "... --- ..." },
                style = type.caption.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = theme.accent
            )
        }
    }
}

@Composable
private fun CustomBlinkPatternCard(
    selectedPatternId: String,
    customSteps: List<Boolean>,
    stepDurationMs: Long,
    activeStepIndex: Int,
    onSelectPreset: (BlinkPatternPreset) -> Unit,
    onToggleCustomStep: (Int) -> Unit,
    onStepDurationChange: (Long) -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(theme.elevated.copy(alpha = 0.72f))
            .border(1.dp, theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Text("Custom Blink Patterns", style = type.cardTitle, color = theme.textPrimary)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            BlinkPatternPreset.entries.forEach { preset ->
                val selected = preset.id == selectedPatternId
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) theme.accent.copy(alpha = 0.22f) else theme.surface)
                        .border(1.dp, if (selected) theme.accent else theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .clickable { onSelectPreset(preset) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = preset.label,
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) theme.accent else theme.textPrimary
                    )
                }
            }
        }

        // 8-Step interactive pattern sequencer
        val activePreset = BlinkPatternPreset.entries.firstOrNull { it.id == selectedPatternId }
            ?: BlinkPatternPreset.DOUBLE_PULSE
        val steps = if (activePreset == BlinkPatternPreset.CUSTOM) customSteps else activePreset.steps

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            steps.forEachIndexed { idx, isOn ->
                val isCurrentStep = activeStepIndex == idx
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isOn) theme.accent.copy(alpha = if (isCurrentStep) 0.95f else 0.35f)
                            else theme.surface
                        )
                        .border(
                            width = if (isCurrentStep) 2.dp else 1.dp,
                            color = if (isCurrentStep) Color.White else if (isOn) theme.accent else theme.textSecondary.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onToggleCustomStep(idx) }
                ) {
                    Text(
                        text = if (isOn) "ON" else "·",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = if (isOn && isCurrentStep) BentoTextBlack else theme.textPrimary
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Step Tempo", style = type.caption, color = theme.textSecondary)
                Text("${stepDurationMs} ms / step", style = type.caption, color = theme.accent)
            }
            Slider(
                value = stepDurationMs.toFloat(),
                onValueChange = { onStepDurationChange(it.roundToInt().toLong()) },
                valueRange = 70f..500f,
                colors = SliderDefaults.colors(
                    thumbColor = theme.accent,
                    activeTrackColor = theme.accent
                )
            )
        }
    }
}

@Composable
private fun SoftScreenLightCard(
    selectedPresetId: String,
    brightnessFraction: Float,
    kelvin: Float,
    isHighUnlocked: Boolean,
    onSelectPreset: (ScreenLightPreset) -> Unit,
    onBrightnessFractionChanged: (Float) -> Unit,
    onLockedPastMiddle: () -> Unit,
    onOpenFullScreenLight: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(theme.elevated.copy(alpha = 0.72f))
            .border(1.dp, theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Soft Screen Light", style = type.cardTitle, color = theme.textPrimary)
                    ProBadge()
                }
                Text(
                    text = "Full-screen soft panel light • Free up to 50% brightness",
                    style = type.caption,
                    color = theme.textSecondary
                )
            }
            GlassButton(
                text = "Open Full Screen",
                icon = Icons.Default.Smartphone,
                onClick = onOpenFullScreenLight,
                testTag = "flashlight_open_screen_light_button"
            )
        }

        // Color-Temperature Presets: Reading, Warm, Night red, Camping
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            ScreenLightPreset.entries.forEach { preset ->
                val isSelected = selectedPresetId == preset.id
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) preset.color.copy(alpha = 0.24f) else theme.surface)
                        .border(
                            1.dp,
                            if (isSelected) preset.color else theme.textSecondary.copy(alpha = 0.25f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelectPreset(preset) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("screen_light_preset_${preset.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(preset.color)
                    )
                    Text(
                        text = preset.label,
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.textPrimary
                    )
                    if (!isHighUnlocked && preset.isPremiumPreset) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Pro Preset",
                            tint = ProGold,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // Apply the same 50% rule to Screen Torch / Soft Screen Light
        ColorCodedIntensitySliderCard(
            title = "Screen Light Brightness",
            subtitle = "${(brightnessFraction * 100).roundToInt().coerceIn(5, 100)}% • ${if (brightnessFraction <= 0.5f) "Medium Free Range" else "Pro High Brightness"}",
            fraction = brightnessFraction,
            isHighUnlocked = isHighUnlocked,
            showBatteryNote = false,
            onFractionChanged = onBrightnessFractionChanged,
            onLockedPastMiddle = onLockedPastMiddle,
            testTag = "screen_light_inline_slider"
        )
    }
}

@Composable
private fun FreeFlashlightExtrasCard(
    isSosActive: Boolean,
    timerOptionsMinutes: List<Int>,
    selectedTimerMinutes: Int,
    remainingTimerSeconds: Int,
    keepScreenOn: Boolean,
    onToggleSos: () -> Unit,
    onSelectTimerMinutes: (Int) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(theme.elevated.copy(alpha = 0.72f))
            .border(1.dp, theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text("Free Essentials", style = type.cardTitle, color = theme.textPrimary)
                Text("SOS beacon, auto-off timer & keep-screen-on", style = type.caption, color = theme.textSecondary)
            }
            GlassButton(
                text = if (isSosActive) "Stop SOS" else "SOS",
                icon = Icons.Default.Bolt,
                accentFill = if (isSosActive) StatusDanger.copy(alpha = 0.32f) else null,
                textColor = if (isSosActive) StatusDanger else null,
                onClick = onToggleSos,
                testTag = "flashlight_free_sos_button"
            )
        }

        // Auto-off Timer chips
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Auto-Off Timer", style = type.caption.copy(fontWeight = FontWeight.Bold), color = theme.textPrimary)
                if (selectedTimerMinutes > 0 && remainingTimerSeconds > 0) {
                    val m = remainingTimerSeconds / 60
                    val s = remainingTimerSeconds % 60
                    Text(
                        text = String.format(Locale.US, "Auto-off in %02d:%02d", m, s),
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.accent
                    )
                } else {
                    Text(
                        text = if (selectedTimerMinutes == 0) "Always On" else "${selectedTimerMinutes}m",
                        style = type.caption,
                        color = theme.textSecondary
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                timerOptionsMinutes.forEach { mins ->
                    val selected = selectedTimerMinutes == mins
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) theme.accent.copy(alpha = 0.22f) else theme.surface)
                            .border(1.dp, if (selected) theme.accent else theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .clickable { onSelectTimerMinutes(mins) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("flashlight_timer_${mins}m")
                    ) {
                        Text(
                            text = if (mins == 0) "Off" else "${mins}m",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) theme.accent else theme.textPrimary
                        )
                    }
                }
            }
        }

        // Keep-screen-on toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LightMode,
                    contentDescription = null,
                    tint = theme.accent,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text("Keep Screen On", style = type.body.copy(fontWeight = FontWeight.SemiBold), color = theme.textPrimary)
                    Text("Prevent screen sleep while torch screen is open", style = type.caption, color = theme.textSecondary)
                }
            }
            Switch(
                checked = keepScreenOn,
                onCheckedChange = onToggleKeepScreenOn,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BentoTextBlack,
                    checkedTrackColor = theme.accent
                ),
                modifier = Modifier.testTag("flashlight_keep_screen_on_switch")
            )
        }
    }
}

@Composable
private fun PremiumFlashlightExtrasCard(
    isHighUnlocked: Boolean,
    shakeToToggleEnabled: Boolean,
    qsTileFeedbackMessage: String?,
    onToggleShake: (Boolean) -> Unit,
    onAddQuickSettingsTile: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(theme.elevated.copy(alpha = 0.72f))
            .border(1.dp, theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Pro Hardware Shortcuts", style = type.cardTitle, color = theme.textPrimary)
            ProBadge()
        }

        // Shake to toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Vibration,
                    contentDescription = null,
                    tint = if (isHighUnlocked) theme.accent else ProGold,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text("Shake to Toggle", style = type.body.copy(fontWeight = FontWeight.SemiBold), color = theme.textPrimary)
                    Text("Firm wrist shake toggles the LED torch on/off", style = type.caption, color = theme.textSecondary)
                }
            }
            Switch(
                checked = shakeToToggleEnabled && isHighUnlocked,
                onCheckedChange = onToggleShake,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BentoTextBlack,
                    checkedTrackColor = theme.accent
                ),
                modifier = Modifier.testTag("flashlight_shake_switch")
            )
        }

        // Quick Settings Tile (TileService)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = null,
                    tint = if (isHighUnlocked) theme.accent else ProGold,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text("Quick Settings Tile", style = type.body.copy(fontWeight = FontWeight.SemiBold), color = theme.textPrimary)
                    Text("Toggle Dyne Torch at Medium strength from Android Quick Settings", style = type.caption, color = theme.textSecondary)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            GlassButton(
                text = if (isHighUnlocked) "Add Tile" else "Add Tile • PRO",
                accentFill = if (!isHighUnlocked) ProGold.copy(alpha = 0.2f) else null,
                textColor = if (!isHighUnlocked) ProGold else null,
                onClick = onAddQuickSettingsTile,
                testTag = "flashlight_add_qs_tile_button"
            )
        }

        if (!qsTileFeedbackMessage.isNullOrBlank()) {
            Text(
                text = qsTileFeedbackMessage,
                style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                color = theme.accent
            )
        }
    }
}

/**
 * Full-screen Screen Torch / Soft Screen Light:
 * - Applies the same 50% rule (starts at 50% "Medium", color-coded Green->Yellow->Orange->Red track,
 *   gold lock at 50% that stops free users and opens the 1-hour Unlock sheet when dragged past 50%).
 * - Color-temperature presets: Reading, Warm, Night red, Camping, plus continuous Kelvin warmth slider.
 */
@Composable
fun ScreenTorchFullScreenScreen(
    hazeState: HazeState?,
    isHighUnlocked: Boolean,
    initialPresetId: String = ScreenLightPreset.READING.id,
    initialBrightness: Float = 0.5f,
    initialKelvin: Float = 4500f,
    onRequestUnlockHigh: (onGranted: () -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var selectedPresetId by rememberSaveable { mutableStateOf(initialPresetId) }
    var brightnessFraction by rememberSaveable { mutableFloatStateOf(initialBrightness.coerceIn(0.05f, 1.0f)) }
    var kelvin by rememberSaveable { mutableFloatStateOf(initialKelvin) }
    var useCustomKelvin by rememberSaveable { mutableStateOf(false) }
    var controlsVisible by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(isHighUnlocked) {
        if (!isHighUnlocked && brightnessFraction > 0.5f) {
            brightnessFraction = 0.5f
        }
    }

    // Keep screen on & apply hardware window brightness
    DisposableEffect(brightnessFraction, isHighUnlocked) {
        val window = (context as? Activity)?.window
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val effectiveBrightness = if (isHighUnlocked) {
                brightnessFraction.coerceIn(0.05f, 1.0f)
            } else {
                brightnessFraction.coerceIn(0.05f, 0.5f)
            }
            val params = window.attributes
            params.screenBrightness = effectiveBrightness
            window.attributes = params
        }
        onDispose {}
    }

    DisposableEffect(Unit) {
        onDispose {
            val window = (context as? Activity)?.window
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val params = window.attributes
                params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = params
            }
        }
    }

    val preset = ScreenLightPreset.entries.firstOrNull { it.id == selectedPresetId }
        ?: ScreenLightPreset.READING

    val activeColor = if (useCustomKelvin) {
        // Interpolate 2000K (amber) -> 4500K (reading) -> 6500K (daylight white)
        val t = ((kelvin - 2000f) / 4500f).coerceIn(0f, 1f)
        Color(
            red = 1.0f,
            green = (0.68f + 0.32f * t).coerceIn(0f, 1f),
            blue = (0.35f + 0.65f * t).coerceIn(0f, 1f)
        )
    } else {
        preset.color
    }

    val effectiveAlpha = (0.25f + 0.75f * if (isHighUnlocked) brightnessFraction else brightnessFraction.coerceAtMost(0.5f))
        .coerceIn(0.25f, 1.0f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(activeColor.copy(alpha = effectiveAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { controlsVisible = !controlsVisible }
            )
            .testTag("screen_torch_fullscreen_root")
    ) {
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                SharedTopBar(
                    title = "Screen Light",
                    subtitle = "TAP ANYWHERE TO HIDE CONTROLS",
                    hazeState = hazeState,
                    onBackClick = onBack,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                GlassSurface(
                    hazeState = hazeState,
                    shape = RoundedCornerShape(30.dp),
                    cornerRadius = 30.dp,
                    tintColor = Color(0xFF151518).copy(alpha = 0.90f),
                    contentPadding = PaddingValues(20.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 28.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Presets: Reading, Warm, Night red, Camping
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            ScreenLightPreset.entries.forEach { item ->
                                val selected = !useCustomKelvin && selectedPresetId == item.id
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (selected) item.color.copy(alpha = 0.25f)
                                            else Color.White.copy(alpha = 0.08f)
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) item.color else Color.White.copy(alpha = 0.2f),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            if (item.isPremiumPreset && !isHighUnlocked) {
                                                onRequestUnlockHigh {
                                                    useCustomKelvin = false
                                                    selectedPresetId = item.id
                                                }
                                            } else {
                                                useCustomKelvin = false
                                                selectedPresetId = item.id
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(item.color)
                                    )
                                    Text(
                                        text = item.label,
                                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    if (!isHighUnlocked && item.isPremiumPreset) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Pro Preset",
                                            tint = ProGold,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Color Temperature Slider (2000K – 6500K)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Color Temperature",
                                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = if (useCustomKelvin) "${kelvin.roundToInt()}K" else preset.kelvinLabel,
                                    style = type.caption,
                                    color = activeColor
                                )
                            }
                            Slider(
                                value = kelvin,
                                onValueChange = { candidate ->
                                    if (isHighUnlocked) {
                                        useCustomKelvin = true
                                        kelvin = candidate
                                    } else {
                                        onRequestUnlockHigh {
                                            useCustomKelvin = true
                                            kelvin = candidate
                                        }
                                    }
                                },
                                valueRange = 2000f..6500f,
                                colors = SliderDefaults.colors(
                                    thumbColor = activeColor,
                                    activeTrackColor = activeColor
                                )
                            )
                        }

                        // Brightness Slider with the same 50% rule (Green left half, Yellow/Orange/Red right half, Gold Lock at 50%)
                        ColorCodedIntensitySliderCard(
                            title = "Screen Torch Brightness",
                            subtitle = "${(brightnessFraction * 100).roundToInt().coerceIn(5, 100)}% • ${if (brightnessFraction <= 0.5f) "Medium (Free up to 50%)" else "High Output"}",
                            fraction = brightnessFraction,
                            isHighUnlocked = isHighUnlocked,
                            showBatteryNote = isHighUnlocked && brightnessFraction > 0.5f,
                            onFractionChanged = { candidate ->
                                FeedbackHelper.hapticTick(context)
                                brightnessFraction = candidate
                            },
                            onLockedPastMiddle = {
                                brightnessFraction = 0.5f
                                FeedbackHelper.vibrateTick(context, durationMs = 25L, amplitude = 180)
                                onRequestUnlockHigh {}
                            },
                            testTag = "screen_torch_brightness_slider"
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Screen stays awake • Tap anywhere to hide/show controls",
                                style = type.caption,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
        }
    }
}
