package com.example.ui.audio

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HeadsetOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.LocalAdManager
import com.example.ui.components.DyneSegmentedControl
import com.example.ui.components.DyneSwitch
import com.example.ui.components.FeedbackHelper
import com.example.ui.components.GlassButton
import com.example.ui.components.dyneClickable
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusWarning
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class CleanerMode(val label: String) {
    WATER_EJECT("Water Eject"),
    DUST_SHAKE("Dust Shake")
}

@Composable
fun SpeakerCleanerInstrumentScreen(
    showFirstUseHearingTip: Boolean = false,
    onDismissHearingTip: () -> Unit = {},
    forcedModeId: String? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val adManager = LocalAdManager.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val engine = remember(context) {
        AudioSynthesisEngine(
            context = context,
            onAudioPlayingStateChanged = { playing ->
                adManager?.setSoundPlaying(playing)
            }
        ).apply {
            blockExternalAudioOutputs = true
        }
    }

    val isPlaying by engine.isPlaying.collectAsStateWithLifecycle()
    val externalHeadsetConnected by engine.externalHeadsetConnected.collectAsStateWithLifecycle()
    val externalWarning by engine.externalDeviceWarning.collectAsStateWithLifecycle()
    val liveCurrentHz by engine.liveCurrentHz.collectAsStateWithLifecycle()

    var modeIndex by rememberSaveable { mutableIntStateOf(0) } // 0 = Water Eject, 1 = Dust Shake
    var waterSweepEnabled by rememberSaveable { mutableStateOf(false) }
    var syncVibration by rememberSaveable { mutableStateOf(true) }
    var repeatEnabled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(forcedModeId) {
        when (forcedModeId?.lowercase(Locale.US)) {
            "auto_eject" -> {
                modeIndex = 0
                waterSweepEnabled = false
            }
            "manual_hz" -> {
                modeIndex = 0
                waterSweepEnabled = true
            }
            "dust" -> {
                modeIndex = 1
            }
        }
    }

    var sessionProgress by remember { mutableFloatStateOf(0f) }
    var remainingSeconds by remember { mutableIntStateOf(30) }
    var consecutiveSessionsCount by rememberSaveable { mutableIntStateOf(0) }
    var cooldownRemainingSeconds by rememberSaveable { mutableIntStateOf(0) }
    var sessionCompletedOfferCheck by rememberSaveable { mutableStateOf(false) }
    var previousRawVolume by rememberSaveable { mutableIntStateOf(-1) }
    var liveVolumeFraction by remember { mutableFloatStateOf(engine.getMediaVolumeFraction()) }

    // Keep screen on during active Speaker Cleaner session
    DisposableEffect(isPlaying) {
        val window = (context as? Activity)?.window
        if (isPlaying) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Stop playback immediately if app goes to background or screen is disposed
    DisposableEffect(lifecycleOwner, engine) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                engine.stopImmediate()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            engine.release()
            adManager?.setSoundPlaying(false)
        }
    }

    // Poll live media volume bar
    LaunchedEffect(Unit) {
        while (isActive) {
            liveVolumeFraction = engine.getMediaVolumeFraction()
            engine.refreshExternalAudioDevices()
            delay(500L)
        }
    }

    // 60-second cool-down timer after 2 sessions in a row
    LaunchedEffect(cooldownRemainingSeconds) {
        if (cooldownRemainingSeconds > 0) {
            delay(1000L)
            cooldownRemainingSeconds -= 1
            if (cooldownRemainingSeconds == 0) {
                consecutiveSessionsCount = 0
            }
        }
    }

    // Configure engine parameters dynamically when mode/options change
    LaunchedEffect(modeIndex, waterSweepEnabled) {
        val mode = CleanerMode.entries[modeIndex]
        when (mode) {
            CleanerMode.WATER_EJECT -> {
                engine.waveform = SynthWaveform.SINE
                engine.amplitude = 0.88f
                if (waterSweepEnabled) {
                    engine.playMode = SynthPlayMode.SWEEP
                    engine.sweepStartHz = 100f
                    engine.sweepEndHz = 500f
                    engine.sweepDurationSec = 6f
                } else {
                    engine.playMode = SynthPlayMode.CONTINUOUS
                    engine.targetFrequencyHz = 165f
                }
            }
            CleanerMode.DUST_SHAKE -> {
                engine.waveform = SynthWaveform.SINE
                engine.amplitude = 0.62f // Lower amplitude pulsing sweep 300-1500 Hz
                engine.playMode = SynthPlayMode.SWEEP
                engine.sweepStartHz = 300f
                engine.sweepEndHz = 1500f
                engine.sweepDurationSec = 2.4f
                engine.pulseOnMs = 180
                engine.pulseOffMs = 90
            }
        }
    }

    // 30-second session progress & synchronized vibration loop
    LaunchedEffect(isPlaying, syncVibration) {
        if (!isPlaying) {
            sessionProgress = 0f
            remainingSeconds = 30
            return@LaunchedEffect
        }
        val startMs = System.currentTimeMillis()
        val totalMs = 30_000L
        var lastVibTick = 0L
        while (isActive && isPlaying) {
            val elapsed = System.currentTimeMillis() - startMs
            sessionProgress = (elapsed.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
            remainingSeconds = ((totalMs - elapsed) / 1000L).toInt().coerceIn(0, 30)

            if (syncVibration && elapsed - lastVibTick >= 600L) {
                lastVibTick = elapsed
                FeedbackHelper.vibrateTick(context, durationMs = 45L, amplitude = 210)
            }

            if (elapsed >= totalMs) {
                val nextCount = consecutiveSessionsCount + 1
                consecutiveSessionsCount = nextCount
                if (nextCount >= 2) {
                    engine.stopImmediate()
                    cooldownRemainingSeconds = 60
                    sessionCompletedOfferCheck = true
                } else if (repeatEnabled) {
                    // Restart 30s session automatically (up to 2 sessions in a row)
                    engine.start(fadeInSeconds = 1.0f)
                } else {
                    engine.stopImmediate()
                    sessionCompletedOfferCheck = true
                }
                break
            }
            delay(80L)
        }
    }

    val waveTransition = rememberInfiniteTransition(label = "speaker_cleaner_waves")
    val wavePhase by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "speaker_wave_phase"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // One-time glass hearing safety tip on first use
        if (showFirstUseHearingTip) {
            HearingSafetyGlassTipCard(onDismiss = onDismissHearingTip)
        }

        // Headphone / Bluetooth / USB Audio Blocker Warning
        if (externalHeadsetConnected || !externalWarning.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(StatusDanger.copy(alpha = 0.16f))
                    .border(1.dp, StatusDanger, RoundedCornerShape(18.dp))
                    .padding(14.dp)
                    .testTag("speaker_cleaner_headset_warning")
            ) {
                Icon(
                    imageVector = Icons.Default.HeadsetOff,
                    contentDescription = "Headphones connected",
                    tint = StatusDanger,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Earphones / Bluetooth / USB Audio Connected",
                        style = type.body.copy(fontWeight = FontWeight.ExtraBold),
                        color = StatusDanger
                    )
                    Text(
                        text = externalWarning
                            ?: "Disconnect headphones, earbuds, Bluetooth, or USB audio before starting Speaker Cleaner. Tones never play through external audio.",
                        style = type.caption,
                        color = theme.textPrimary
                    )
                }
            }
        }

        // Mode selector: Water Eject vs Dust Shake
        DyneSegmentedControl(
            segments = CleanerMode.entries.map { it.label },
            selectedIndex = modeIndex,
            onSegmentSelected = { newIdx ->
                if (isPlaying) engine.stopImmediate()
                modeIndex = newIdx
            }
        )

        if (modeIndex == 0) {
            // Optional slow sweep 100-500 Hz switch for Water Eject
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(theme.elevated)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (waterSweepEnabled) "Slow Sweep (100–500 Hz)" else "Continuous 165 Hz Sine (Default)",
                        style = type.body.copy(fontWeight = FontWeight.Bold),
                        color = theme.textPrimary
                    )
                    Text(
                        text = "Optional 100–500 Hz sweep for stubborn water droplets",
                        style = type.caption,
                        color = theme.textSecondary
                    )
                }
                DyneSwitch(
                    checked = waterSweepEnabled,
                    onCheckedChange = { waterSweepEnabled = it }
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(theme.elevated)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Dust Shake: Pulsing 300–1500 Hz sweep at lower amplitude.",
                    style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = theme.textSecondary
                )
            }
        }

        // Animated Canvas with 30s Progress Ring, Water Droplets, Waves, and Big Glowing Start/Stop Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.08f)
                .clip(RoundedCornerShape(28.dp))
                .background(theme.elevated)
                .border(1.dp, theme.accent.copy(alpha = if (isPlaying) 0.65f else 0.2f), RoundedCornerShape(28.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxR = size.minDimension * 0.42f

                // Animated water waves at the bottom of the canvas reacting to the tone
                val waveBaseY = size.height * 0.78f
                val waveAmp = if (isPlaying) 14.dp.toPx() else 4.dp.toPx()
                val wavePath = Path().apply {
                    moveTo(0f, size.height)
                    lineTo(0f, waveBaseY)
                    val steps = 40
                    for (i in 0..steps) {
                        val x = (i.toFloat() / steps) * size.width
                        val y = waveBaseY + sin(wavePhase + i * 0.35f) * waveAmp
                        lineTo(x, y)
                    }
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(
                    path = wavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            theme.accent.copy(alpha = if (isPlaying) 0.26f else 0.08f),
                            theme.accent.copy(alpha = 0.03f)
                        ),
                        startY = waveBaseY - waveAmp,
                        endY = size.height
                    )
                )

                // Animated water droplets flying outward when playing
                if (isPlaying) {
                    val dropletCount = 12
                    for (d in 0 until dropletCount) {
                        val angle = (d * (360f / dropletCount) + (wavePhase * 30f)) * (PI / 180.0)
                        val radialFrac = ((wavePhase / (2f * PI).toFloat() + d * 0.13f) % 1f)
                        val dist = maxR * (0.55f + radialFrac * 0.55f)
                        val dx = center.x + (cos(angle) * dist).toFloat()
                        val dy = center.y + (sin(angle) * dist).toFloat()
                        drawCircle(
                            color = theme.accent.copy(alpha = (1f - radialFrac) * 0.85f),
                            radius = (3.5f + (1f - radialFrac) * 4.5f).dp.toPx(),
                            center = Offset(dx, dy)
                        )
                    }
                }

                // Outer 30-second session progress ring
                val ringStroke = 8.dp.toPx()
                drawCircle(
                    color = theme.surface,
                    radius = maxR,
                    center = center,
                    style = Stroke(width = ringStroke)
                )
                if (isPlaying || sessionProgress > 0f) {
                    drawArc(
                        color = theme.accent,
                        startAngle = -90f,
                        sweepAngle = 360f * sessionProgress,
                        useCenter = false,
                        topLeft = Offset(center.x - maxR, center.y - maxR),
                        size = androidx.compose.ui.geometry.Size(maxR * 2f, maxR * 2f),
                        style = Stroke(width = ringStroke, cap = StrokeCap.Round)
                    )
                }
            }

            // Big Glowing Start / Stop Button in the center
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val canStart = cooldownRemainingSeconds == 0 && !externalHeadsetConnected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(144.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = if (isPlaying) {
                                    listOf(StatusDanger, StatusDanger.copy(alpha = 0.78f))
                                } else if (canStart) {
                                    listOf(theme.accent, theme.accent.copy(alpha = 0.78f))
                                } else {
                                    listOf(theme.surface, theme.elevated)
                                }
                            )
                        )
                        .border(
                            width = 3.dp,
                            color = Color.White.copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                        .dyneClickable(
                            enabled = isPlaying || canStart,
                            targetScale = 0.94f,
                            testTag = "speaker_cleaner_start_stop_button",
                            onClick = {
                                if (isPlaying) {
                                    engine.stopImmediate()
                                } else if (canStart) {
                                    sessionCompletedOfferCheck = false
                                    // Fade sound in over 1 second
                                    engine.start(fadeInSeconds = 1.0f)
                                }
                            }
                        )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Stop Speaker Cleaner" else "Start Speaker Cleaner",
                            tint = if (isPlaying) Color.White else if (canStart) BentoTextBlack else theme.textSecondary,
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = if (isPlaying) "STOP" else "START",
                            style = type.title.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (isPlaying) Color.White else if (canStart) BentoTextBlack else theme.textSecondary
                        )
                        Text(
                            text = if (isPlaying) "${remainingSeconds}s • ${liveCurrentHz.roundToInt()} Hz" else "30s Session",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = if (isPlaying) Color.White.copy(alpha = 0.9f) else if (canStart) BentoTextBlack.copy(alpha = 0.8f) else theme.textSecondary
                        )
                    }
                }
            }
        }

        // Synchronized Vibration Switch & Repeat Chip
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(theme.elevated)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = null,
                        tint = theme.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Sync Vibration",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.textPrimary
                    )
                }
                DyneSwitch(
                    checked = syncVibration,
                    onCheckedChange = { syncVibration = it }
                )
            }

            // "Repeat" chip
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (repeatEnabled) theme.accent else theme.elevated)
                    .border(
                        1.dp,
                        if (repeatEnabled) theme.accent else Color.White.copy(alpha = 0.12f),
                        RoundedCornerShape(18.dp)
                    )
                    .dyneClickable(
                        testTag = "speaker_cleaner_repeat_chip",
                        onClick = { repeatEnabled = !repeatEnabled }
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatEnabled) BentoTextBlack else theme.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Repeat",
                        style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (repeatEnabled) BentoTextBlack else theme.textPrimary
                    )
                }
            }
        }

        // 60-second cool-down message after 2 sessions in a row
        if (cooldownRemainingSeconds > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(StatusWarning.copy(alpha = 0.16f))
                    .border(1.dp, StatusWarning, RoundedCornerShape(18.dp))
                    .padding(14.dp)
                    .testTag("speaker_cleaner_cooldown_banner")
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = StatusWarning,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Speaker cool-down active (${cooldownRemainingSeconds}s remaining). After 2 consecutive sessions, let the speaker coil rest for 60 seconds.",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.textPrimary
                )
            }
        }

        // Post-session "Test your speaker" check tone + "Restore previous volume" offer
        if (sessionCompletedOfferCheck || previousRawVolume >= 0) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.elevated)
                    .border(1.dp, theme.accent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "SESSION COMPLETE",
                    style = type.section,
                    color = theme.accent
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassButton(
                        text = "Test your speaker",
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        accentFill = theme.accent,
                        onClick = {
                            engine.playMode = SynthPlayMode.SWEEP
                            engine.sweepStartHz = 260f
                            engine.sweepEndHz = 880f
                            engine.sweepDurationSec = 1.6f
                            engine.start(
                                fadeInSeconds = 0.25f,
                                maxDurationSeconds = 1.8f
                            )
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "speaker_cleaner_test_speaker_button"
                    )
                    if (previousRawVolume >= 0) {
                        GlassButton(
                            text = "Restore volume",
                            onClick = {
                                engine.setRawMediaVolume(previousRawVolume)
                                liveVolumeFraction = engine.getMediaVolumeFraction()
                                previousRawVolume = -1
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "speaker_cleaner_restore_volume_button"
                        )
                    }
                }
            }
        }

        // Glass Pre-Start Checklist with live volume bar & "Set volume to max" button (acts only on user tap)
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(theme.elevated)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "PRE-START CHECKLIST",
                style = type.section,
                color = theme.accent
            )

            ChecklistRow("1. Remove earphones, headphones, and Bluetooth audio")
            ChecklistRow("2. Turn up media volume for full speaker excursion")

            // Live Volume Bar + "Set volume to max" button (acts only on user tap)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Live Media Volume",
                            style = type.caption,
                            color = theme.textSecondary
                        )
                        Text(
                            text = "${(liveVolumeFraction * 100).roundToInt()}%",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.accent
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(theme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(liveVolumeFraction.coerceIn(0.04f, 1f))
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(theme.accent)
                        )
                    }
                }
                GlassButton(
                    text = "Set volume to max",
                    onClick = {
                        val prev = engine.setMediaVolumeToMax()
                        if (previousRawVolume < 0) {
                            previousRawVolume = prev
                        }
                        liveVolumeFraction = engine.getMediaVolumeFraction()
                    },
                    testTag = "speaker_cleaner_max_volume_button"
                )
            }

            ChecklistRow("3. Place the phone with the speaker facing down")
            ChecklistRow("4. Never hold the phone near your ear or animals")
        }

        // Required Honest Disclaimer under the button
        Text(
            text = "Uses sound vibration to help push water and dust out of the speaker opening. Results vary and nothing is guaranteed. This is not a repair. Do not use if the phone was submerged or liquid may be inside the device; let it dry first.",
            style = type.caption,
            color = theme.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .testTag("speaker_cleaner_disclaimer")
        )
    }
}

@Composable
private fun ChecklistRow(text: String) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = theme.accent,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = type.body,
            color = theme.textPrimary
        )
    }
}

@Composable
fun HearingSafetyGlassTipCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(theme.elevated)
            .border(1.dp, theme.accent.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(14.dp)
            .testTag("hearing_safety_first_use_tip")
    ) {
        Icon(
            imageVector = Icons.Default.WarningAmber,
            contentDescription = "Hearing Safety Tip",
            tint = theme.accent,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hearing Safety Tip",
                style = type.body.copy(fontWeight = FontWeight.ExtraBold),
                color = theme.textPrimary
            )
            Text(
                text = "Start at a moderate volume and keep the speaker away from your ears and pets. Synthesized tones can be loud.",
                style = type.caption,
                color = theme.textSecondary
            )
        }
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Dismiss tip",
            tint = theme.textSecondary,
            modifier = Modifier
                .size(20.dp)
                .dyneClickable(onClick = onDismiss)
        )
    }
}
