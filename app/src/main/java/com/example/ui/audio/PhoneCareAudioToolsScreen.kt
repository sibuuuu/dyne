package com.example.ui.audio

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.LocalAdManager
import com.example.billing.LocalFeatureGate
import com.example.billing.PremiumFeature
import com.example.data.DeviceCapabilities
import com.example.ui.components.DyneSegmentedControl
import com.example.ui.components.DyneSwitch
import com.example.ui.components.FeedbackHelper
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassPermissionCard
import com.example.ui.components.HazeState
import com.example.ui.components.ProBadge
import com.example.ui.components.dyneClickable
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import com.example.ui.theme.StatusWarning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 2) SPEAKER TEST (Free):
 * Left, right and both channel test with animated indicators, a bass test,
 * a frequency sweep, and an earpiece test using USAGE_VOICE_COMMUNICATION.
 */
@Composable
fun SpeakerTestInstrumentScreen(
    showFirstUseHearingTip: Boolean = false,
    onDismissHearingTip: () -> Unit = {}
) {
    val context = LocalContext.current
    val adManager = LocalAdManager.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val engine = remember(context) {
        AudioSynthesisEngine(
            context = context,
            onAudioPlayingStateChanged = { adManager?.setSoundPlaying(it) }
        )
    }
    val isPlaying by engine.isPlaying.collectAsStateWithLifecycle()
    val liveHz by engine.liveCurrentHz.collectAsStateWithLifecycle()
    var activeTestLabel by rememberSaveable { mutableStateOf("Idle") }

    DisposableEffect(engine) {
        onDispose {
            engine.release()
            adManager?.setSoundPlaying(false)
        }
    }

    fun startTest(
        label: String,
        route: StereoRouteMode,
        mode: SynthPlayMode = SynthPlayMode.CONTINUOUS,
        freq: Float = 440f,
        sweepStart: Float = 80f,
        sweepEnd: Float = 8000f
    ) {
        if (isPlaying && activeTestLabel == label) {
            engine.stopImmediate()
            activeTestLabel = "Idle"
            return
        }
        engine.stopImmediate()
        activeTestLabel = label
        engine.stereoRoute = route
        engine.waveform = SynthWaveform.SINE
        engine.playMode = mode
        engine.targetFrequencyHz = freq
        engine.sweepStartHz = sweepStart
        engine.sweepEndHz = sweepEnd
        engine.sweepDurationSec = 4f
        engine.amplitude = 0.65f
        engine.start(
            fadeInSeconds = 0.35f,
            maxDurationSeconds = 5.0f,
            onAutoStopped = { activeTestLabel = "Idle" }
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (showFirstUseHearingTip) {
            HearingSafetyGlassTipCard(onDismiss = onDismissHearingTip)
        }

        // Animated Left / Right Channel Indicator Canvas
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val leftActive = isPlaying && (engine.stereoRoute == StereoRouteMode.LEFT_ONLY || engine.stereoRoute == StereoRouteMode.BOTH)
            val rightActive = isPlaying && (engine.stereoRoute == StereoRouteMode.RIGHT_ONLY || engine.stereoRoute == StereoRouteMode.BOTH)

            ChannelSpeakerIndicatorCard(
                label = "LEFT CHANNEL",
                active = leftActive,
                onClick = { startTest("Left Channel", StereoRouteMode.LEFT_ONLY, freq = 440f) },
                modifier = Modifier.weight(1f)
            )
            ChannelSpeakerIndicatorCard(
                label = "RIGHT CHANNEL",
                active = rightActive,
                onClick = { startTest("Right Channel", StereoRouteMode.RIGHT_ONLY, freq = 554.37f) },
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = if (isPlaying) "Active: $activeTestLabel (${liveHz.roundToInt()} Hz)" else "Tap a speaker test below",
            style = type.body.copy(fontWeight = FontWeight.Bold),
            color = if (isPlaying) theme.accent else theme.textSecondary
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButton(
                text = "Both (Stereo)",
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                accentFill = if (isPlaying && activeTestLabel == "Both Channels") theme.accent else null,
                onClick = { startTest("Both Channels", StereoRouteMode.BOTH, freq = 528f) },
                modifier = Modifier.weight(1f),
                testTag = "speaker_test_both"
            )
            GlassButton(
                text = "Bass Test (65 Hz)",
                icon = Icons.Default.GraphicEq,
                accentFill = if (isPlaying && activeTestLabel == "Bass Test") theme.accent else null,
                onClick = { startTest("Bass Test", StereoRouteMode.BOTH, freq = 65f) },
                modifier = Modifier.weight(1f),
                testTag = "speaker_test_bass"
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButton(
                text = "Frequency Sweep",
                icon = Icons.Default.PlayArrow,
                accentFill = if (isPlaying && activeTestLabel == "Frequency Sweep") theme.accent else null,
                onClick = {
                    startTest(
                        label = "Frequency Sweep",
                        route = StereoRouteMode.BOTH,
                        mode = SynthPlayMode.SWEEP,
                        sweepStart = 80f,
                        sweepEnd = 10000f
                    )
                },
                modifier = Modifier.weight(1f),
                testTag = "speaker_test_sweep"
            )
            GlassButton(
                text = "Earpiece Test",
                icon = Icons.Default.PhoneInTalk,
                accentFill = if (isPlaying && activeTestLabel == "Earpiece") theme.accent else null,
                onClick = { startTest("Earpiece", StereoRouteMode.EARPIECE, freq = 600f) },
                modifier = Modifier.weight(1f),
                testTag = "speaker_test_earpiece"
            )
        }

        if (isPlaying) {
            GlassButton(
                text = "Stop Playback",
                icon = Icons.Default.Stop,
                accentFill = StatusWarning,
                onClick = {
                    engine.stopImmediate()
                    activeTestLabel = "Idle"
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ChannelSpeakerIndicatorCard(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (active) theme.accent.copy(alpha = 0.18f) else theme.elevated)
            .border(
                1.5.dp,
                if (active) theme.accent else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(22.dp)
            )
            .dyneClickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (active) theme.accent else theme.surface)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = label,
                tint = if (active) BentoTextBlack else theme.textPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = label,
            style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
            color = if (active) theme.accent else theme.textPrimary
        )
        Text(
            text = if (active) "PLAYING" else "TAP TO TEST",
            style = type.caption,
            color = theme.textSecondary
        )
    }
}

/**
 * 3) MIC TEST (Free):
 * Live level meter, record 5 seconds and play back from memory only (never saved to disk).
 * Requests RECORD_AUDIO only inside Mic Test at use.
 */
@SuppressLint("MissingPermission")
@Composable
fun MicTestInstrumentScreen(hazeState: HazeState?) {
    val context = LocalContext.current
    val adManager = LocalAdManager.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val scope = rememberCoroutineScope()

    var hasAudioPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPerm = granted
    }

    var liveLevelFraction by remember { mutableFloatStateOf(0f) }
    var isRecording5s by remember { mutableStateOf(false) }
    var isPlayingBack by remember { mutableStateOf(false) }
    var recordStatusText by remember { mutableStateOf("Tap \"Record 5s (RAM Only)\" to test microphone clarity.") }
    var memoryPcmBuffer by remember { mutableStateOf<ShortArray?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            memoryPcmBuffer = null
            adManager?.setSoundPlaying(false)
        }
    }

    // Live input level meter when permission is granted and not playing back
    LaunchedEffect(hasAudioPerm, isRecording5s, isPlayingBack) {
        if (!hasAudioPerm || isRecording5s || isPlayingBack) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val sampleRate = 48_000
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)
            val recorder = runCatching {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf
                )
            }.getOrNull() ?: return@withContext

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                runCatching { recorder.release() }
                return@withContext
            }
            val buf = ShortArray(1024)
            try {
                recorder.startRecording()
                while (isActive && !isRecording5s && !isPlayingBack) {
                    val read = recorder.read(buf, 0, buf.size)
                    if (read > 0) {
                        var sum = 0.0
                        for (i in 0 until read) {
                            val norm = buf[i].toDouble() / Short.MAX_VALUE
                            sum += norm * norm
                        }
                        val rms = sqrt(sum / read).toFloat()
                        liveLevelFraction = (rms * 4.5f).coerceIn(0f, 1f)
                    }
                    delay(40L)
                }
            } finally {
                runCatching {
                    recorder.stop()
                    recorder.release()
                }
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!hasAudioPerm) {
            GlassPermissionCard(
                icon = Icons.Default.Mic,
                title = "Microphone Access for Mic Test",
                explanation = "Dyne samples microphone audio strictly in RAM for 5 seconds so you can hear playback clarity. Nothing is ever saved to disk.",
                buttonText = "Allow Microphone",
                hazeState = hazeState,
                onRequestPermission = { permLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )
            return@Column
        }

        // Live Level Meter Bar
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(theme.elevated)
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "LIVE INPUT LEVEL METER",
                    style = type.section,
                    color = theme.accent
                )
                Text(
                    text = "${(liveLevelFraction * 100).roundToInt()}%",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.textPrimary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(CircleShape)
                    .background(theme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(liveLevelFraction.coerceIn(0.03f, 1f))
                        .height(16.dp)
                        .clip(CircleShape)
                        .background(theme.accent)
                )
            }
            Text(
                text = recordStatusText,
                style = type.caption,
                color = theme.textSecondary
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButton(
                text = if (isRecording5s) "Recording 5s..." else "Record 5s (RAM)",
                icon = Icons.Default.Mic,
                accentFill = theme.accent,
                enabled = !isRecording5s && !isPlayingBack,
                onClick = {
                    scope.launch {
                        isRecording5s = true
                        recordStatusText = "Recording 5 seconds into memory (RAM only)..."
                        val captured = withContext(Dispatchers.IO) {
                            val sr = 48_000
                            val totalSamples = sr * 5
                            val out = ShortArray(totalSamples)
                            val minBuf = AudioRecord.getMinBufferSize(
                                sr,
                                AudioFormat.CHANNEL_IN_MONO,
                                AudioFormat.ENCODING_PCM_16BIT
                            ).coerceAtLeast(4096)
                            val rec = runCatching {
                                AudioRecord(
                                    MediaRecorder.AudioSource.MIC,
                                    sr,
                                    AudioFormat.CHANNEL_IN_MONO,
                                    AudioFormat.ENCODING_PCM_16BIT,
                                    minBuf
                                )
                            }.getOrNull()
                            if (rec == null || rec.state != AudioRecord.STATE_INITIALIZED) {
                                runCatching { rec?.release() }
                                null
                            } else {
                                try {
                                    rec.startRecording()
                                    var offset = 0
                                    while (offset < totalSamples && isActive) {
                                        val chunk = minOf(2048, totalSamples - offset)
                                        val r = rec.read(out, offset, chunk)
                                        if (r <= 0) break
                                        var sum = 0.0
                                        for (i in offset until (offset + r)) {
                                            val v = out[i].toDouble() / Short.MAX_VALUE
                                            sum += v * v
                                        }
                                        liveLevelFraction = (sqrt(sum / r).toFloat() * 4.5f).coerceIn(0f, 1f)
                                        offset += r
                                    }
                                    out
                                } finally {
                                    runCatching {
                                        rec.stop()
                                        rec.release()
                                    }
                                }
                            }
                        }
                        memoryPcmBuffer = captured
                        isRecording5s = false
                        recordStatusText = if (captured != null) {
                            "5-second sample ready in memory (never written to storage). Tap Play Back."
                        } else {
                            "Could not capture microphone audio."
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                testTag = "mic_test_record_5s_button"
            )

            GlassButton(
                text = if (isPlayingBack) "Playing..." else "Play Back",
                icon = Icons.Default.PlayArrow,
                enabled = memoryPcmBuffer != null && !isRecording5s && !isPlayingBack,
                onClick = {
                    val sample = memoryPcmBuffer ?: return@GlassButton
                    scope.launch {
                        isPlayingBack = true
                        adManager?.setSoundPlaying(true)
                        recordStatusText = "Playing back 5-second memory buffer..."
                        withContext(Dispatchers.IO) {
                            val sr = 48_000
                            runCatching {
                                val track = AudioTrack.Builder()
                                    .setAudioAttributes(
                                        AudioAttributes.Builder()
                                            .setUsage(AudioAttributes.USAGE_MEDIA)
                                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                            .build()
                                    )
                                    .setAudioFormat(
                                        AudioFormat.Builder()
                                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                            .setSampleRate(sr)
                                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                            .build()
                                    )
                                    .setBufferSizeInBytes(sample.size * 2)
                                    .setTransferMode(AudioTrack.MODE_STATIC)
                                    .build()
                                track.write(sample, 0, sample.size)
                                track.play()
                                delay(5100L)
                                track.stop()
                                track.release()
                            }
                        }
                        adManager?.setSoundPlaying(false)
                        isPlayingBack = false
                        recordStatusText = "Playback finished. Memory buffer is cleared when you leave this screen."
                    }
                },
                modifier = Modifier.weight(1f),
                testTag = "mic_test_playback_button"
            )
        }
    }
}

/**
 * 4) TONE GENERATOR (Free sine; Premium waveforms: square, saw, triangle, sweep, dual tone):
 * Log frequency slider 20 Hz–20 kHz, presets, safe volume limit, fade, and a warning above 8 kHz.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToneGeneratorInstrumentScreen() {
    val context = LocalContext.current
    val adManager = LocalAdManager.current
    val featureGate = LocalFeatureGate.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val isProWaveformUnlocked = featureGate?.isUnlocked(PremiumFeature.TONE_GENERATOR_PRO) == true

    val engine = remember(context) {
        AudioSynthesisEngine(
            context = context,
            onAudioPlayingStateChanged = { adManager?.setSoundPlaying(it) }
        )
    }
    val isPlaying by engine.isPlaying.collectAsStateWithLifecycle()

    // Logarithmic slider position 0..1 -> 20 Hz..20,000 Hz
    var logPos by rememberSaveable { mutableFloatStateOf(0.448f) } // ~440 Hz
    val freqHz = remember(logPos) {
        val minLog = ln(20.0)
        val maxLog = ln(20000.0)
        exp(minLog + logPos.toDouble() * (maxLog - minLog)).toFloat().coerceIn(20f, 20000f)
    }

    var selectedWave by rememberSaveable { mutableStateOf(SynthWaveform.SINE) }
    var sweepMode by rememberSaveable { mutableStateOf(false) }
    var dualToneMode by rememberSaveable { mutableStateOf(false) }
    var safeVolumeCap by rememberSaveable { mutableFloatStateOf(0.60f) }

    DisposableEffect(engine) {
        onDispose {
            engine.release()
            adManager?.setSoundPlaying(false)
        }
    }

    LaunchedEffect(freqHz, selectedWave, sweepMode, dualToneMode, safeVolumeCap) {
        engine.targetFrequencyHz = freqHz
        engine.waveform = selectedWave
        engine.amplitude = safeVolumeCap.coerceIn(0.05f, 0.85f)
        engine.playMode = if (sweepMode) SynthPlayMode.SWEEP else SynthPlayMode.CONTINUOUS
        engine.sweepStartHz = 100f
        engine.sweepEndHz = freqHz.coerceAtLeast(400f)
        engine.secondaryFrequencyHz = if (dualToneMode) (freqHz * 1.25f).coerceAtMost(18000f) else 0f
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (freqHz >= 1000f) {
                String.format(Locale.US, "%.2f kHz (%d Hz)", freqHz / 1000f, freqHz.roundToInt())
            } else {
                String.format(Locale.US, "%.1f Hz", freqHz)
            },
            style = type.gaugeHero,
            color = theme.accent
        )

        if (freqHz > 8000f) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(StatusWarning.copy(alpha = 0.16f))
                    .border(1.dp, StatusWarning, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = StatusWarning,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "High frequency (> 8 kHz): keep volume low and keep the phone away from your ears and pets.",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.textPrimary
                )
            }
        }

        // Logarithmic Frequency Slider 20 Hz - 20 kHz
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("20 Hz", style = type.caption, color = theme.textSecondary)
                Text("Logarithmic Scale", style = type.caption, color = theme.textSecondary)
                Text("20 kHz", style = type.caption, color = theme.textSecondary)
            }
            Slider(
                value = logPos,
                onValueChange = { logPos = it },
                colors = SliderDefaults.colors(
                    thumbColor = theme.accent,
                    activeTrackColor = theme.accent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Quick Frequency Presets
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(60f, 165f, 432f, 440f, 528f, 1000f, 4000f, 10000f).forEach { pHz ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.elevated)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                        .dyneClickable {
                            val minL = ln(20.0)
                            val maxL = ln(20000.0)
                            logPos = ((ln(pHz.toDouble()) - minL) / (maxL - minL)).toFloat()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (pHz >= 1000f) "${(pHz / 1000f).toInt()} kHz" else "${pHz.toInt()} Hz",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.textPrimary
                    )
                }
            }
        }

        // Waveforms (Sine Free; Square, Saw, Triangle, Sweep, Dual Tone Premium)
        Text("WAVEFORM & MODES", style = type.section, color = theme.accent)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(SynthWaveform.SINE, SynthWaveform.SQUARE, SynthWaveform.SAW, SynthWaveform.TRIANGLE).forEach { wf ->
                val isSelected = selectedWave == wf && !sweepMode && !dualToneMode
                val locked = wf.isProInToneGen && !isProWaveformUnlocked
                WaveformModeChip(
                    label = wf.label,
                    selected = isSelected,
                    locked = locked,
                    onClick = {
                        if (locked && featureGate != null) {
                            featureGate.require(PremiumFeature.TONE_GENERATOR_PRO) {
                                selectedWave = wf
                                sweepMode = false
                                dualToneMode = false
                            }
                        } else {
                            selectedWave = wf
                            sweepMode = false
                            dualToneMode = false
                        }
                    }
                )
            }
            WaveformModeChip(
                label = "Sweep",
                selected = sweepMode,
                locked = !isProWaveformUnlocked,
                onClick = {
                    if (!isProWaveformUnlocked && featureGate != null) {
                        featureGate.require(PremiumFeature.TONE_GENERATOR_PRO) {
                            sweepMode = !sweepMode
                            dualToneMode = false
                        }
                    } else {
                        sweepMode = !sweepMode
                        dualToneMode = false
                    }
                }
            )
            WaveformModeChip(
                label = "Dual Tone",
                selected = dualToneMode,
                locked = !isProWaveformUnlocked,
                onClick = {
                    if (!isProWaveformUnlocked && featureGate != null) {
                        featureGate.require(PremiumFeature.TONE_GENERATOR_PRO) {
                            dualToneMode = !dualToneMode
                            sweepMode = false
                        }
                    } else {
                        dualToneMode = !dualToneMode
                        sweepMode = false
                    }
                }
            )
        }

        // Safe Volume Limit Slider
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Safe Output Level (Fade In/Out)", style = type.caption, color = theme.textSecondary)
                Text("${(safeVolumeCap * 100).roundToInt()}%", style = type.caption.copy(fontWeight = FontWeight.Bold), color = theme.accent)
            }
            Slider(
                value = safeVolumeCap,
                onValueChange = { safeVolumeCap = it.coerceIn(0.05f, 0.85f) },
                valueRange = 0.05f..0.85f,
                colors = SliderDefaults.colors(
                    thumbColor = theme.accent,
                    activeTrackColor = theme.accent
                )
            )
        }

        GlassButton(
            text = if (isPlaying) "Stop Tone (Fade Out)" else "Play Tone (Fade In)",
            icon = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
            accentFill = if (isPlaying) StatusWarning else theme.accent,
            onClick = {
                if (isPlaying) engine.stopWithFadeOut() else engine.start(fadeInSeconds = 1.0f)
            },
            modifier = Modifier.fillMaxWidth(),
            testTag = "tone_generator_play_button"
        )
    }
}

@Composable
private fun WaveformModeChip(
    label: String,
    selected: Boolean,
    locked: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) theme.accent else theme.elevated)
            .border(
                1.dp,
                if (selected) theme.accent else if (locked) ProGold.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(16.dp)
            )
            .dyneClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            style = type.caption.copy(fontWeight = FontWeight.Bold),
            color = if (selected) BentoTextBlack else theme.textPrimary
        )
        if (locked) {
            ProBadge()
        }
    }
}

/**
 * 5) NOISE & SLEEP MIXER (Premium):
 * Synthesized white, pink and brown noise plus rain-like and wind-like layers with separate sliders,
 * a timer (15/30/60 min) with fade-out, keep-screen-on option, and a sleep-friendly dark mode.
 */
@Composable
fun NoiseSleepMixerInstrumentScreen() {
    val context = LocalContext.current
    val adManager = LocalAdManager.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val engine = remember(context) {
        AudioSynthesisEngine(
            context = context,
            onAudioPlayingStateChanged = { adManager?.setSoundPlaying(it) }
        )
    }
    val isPlaying by engine.isPlaying.collectAsStateWithLifecycle()

    var whiteLevel by rememberSaveable { mutableFloatStateOf(0.0f) }
    var pinkLevel by rememberSaveable { mutableFloatStateOf(0.35f) }
    var brownLevel by rememberSaveable { mutableFloatStateOf(0.50f) }
    var rainLevel by rememberSaveable { mutableFloatStateOf(0.35f) }
    var windLevel by rememberSaveable { mutableFloatStateOf(0.25f) }

    var timerMinutes by rememberSaveable { mutableIntStateOf(30) } // 15, 30, 60
    var keepScreenOn by rememberSaveable { mutableStateOf(false) }
    var sleepDarkMode by rememberSaveable { mutableStateOf(true) }

    DisposableEffect(isPlaying, keepScreenOn) {
        val window = (context as? Activity)?.window
        if (isPlaying && keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(engine) {
        onDispose {
            engine.release()
            adManager?.setSoundPlaying(false)
        }
    }

    LaunchedEffect(whiteLevel, pinkLevel, brownLevel, rainLevel, windLevel) {
        engine.sleepMixerLevels = SleepMixerLevels(
            white = whiteLevel,
            pink = pinkLevel,
            brown = brownLevel,
            rain = rainLevel,
            wind = windLevel
        )
        engine.amplitude = 0.75f
    }

    val containerBg = if (sleepDarkMode) Color(0xFF07090E) else theme.elevated

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(containerBg)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NightlightRound,
                    contentDescription = null,
                    tint = theme.accent
                )
                Text(
                    text = "SYNTHESIZED SLEEP LAYERS",
                    style = type.section,
                    color = theme.accent
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Sleep Dim", style = type.caption, color = theme.textSecondary)
                DyneSwitch(checked = sleepDarkMode, onCheckedChange = { sleepDarkMode = it })
            }
        }

        MixerLayerSlider("White Noise", whiteLevel) { whiteLevel = it }
        MixerLayerSlider("Pink Noise", pinkLevel) { pinkLevel = it }
        MixerLayerSlider("Brown Noise", brownLevel) { brownLevel = it }
        MixerLayerSlider("Rain-like Layer", rainLevel) { rainLevel = it }
        MixerLayerSlider("Wind-like Layer", windLevel) { windLevel = it }

        // Sleep Timer (15 / 30 / 60 min) with Fade-Out
        Text("SLEEP TIMER (WITH FADE-OUT)", style = type.section, color = theme.accent)
        DyneSegmentedControl(
            segments = listOf("15 min", "30 min", "60 min"),
            selectedIndex = when (timerMinutes) {
                15 -> 0
                60 -> 2
                else -> 1
            },
            onSegmentSelected = { idx ->
                timerMinutes = when (idx) {
                    0 -> 15
                    2 -> 60
                    else -> 30
                }
            }
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Keep screen on while mixer plays", style = type.body, color = Color.White)
            DyneSwitch(checked = keepScreenOn, onCheckedChange = { keepScreenOn = it })
        }

        GlassButton(
            text = if (isPlaying) "Stop Sleep Mixer" else "Start Sleep Mixer (${timerMinutes}m)",
            icon = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
            accentFill = theme.accent,
            onClick = {
                if (isPlaying) {
                    engine.stopImmediate()
                } else {
                    engine.start(
                        fadeInSeconds = 1.5f,
                        maxDurationSeconds = timerMinutes * 60f
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            testTag = "noise_sleep_mixer_start_button"
        )
    }
}

@Composable
private fun MixerLayerSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(title, style = type.caption.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Text("${(value * 100).roundToInt()}%", style = type.caption, color = theme.accent)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = theme.accent,
                activeTrackColor = theme.accent
            )
        )
    }
}

/**
 * 6) VIBRATION LAB (Free):
 * Pattern buttons (tick, buzz, heartbeat, SOS) and an intensity slider when amplitude control
 * is supported; otherwise hide the slider.
 */
@Composable
fun VibrationLabInstrumentScreen() {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val capabilities = remember(context) { DeviceCapabilities.initialize(context) }
    val hasAmplitudeControl = capabilities.hasVibratorAmplitudeControl
    var intensity by rememberSaveable { mutableFloatStateOf(0.80f) }
    var lastPatternLabel by rememberSaveable { mutableStateOf("Tap a haptic pattern below") }

    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun playPattern(name: String, timings: LongArray, amplitudes: IntArray) {
        lastPatternLabel = "Triggered: $name"
        val vib = vibrator ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (hasAmplitudeControl) {
                    val scaledAmps = amplitudes.map { amp ->
                        if (amp == 0) 0 else (amp * intensity).roundToInt().coerceIn(1, 255)
                    }.toIntArray()
                    vib.vibrate(VibrationEffect.createWaveform(timings, scaledAmps, -1))
                } else {
                    vib.vibrate(VibrationEffect.createWaveform(timings, -1))
                }
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(timings, -1)
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = lastPatternLabel,
            style = type.body.copy(fontWeight = FontWeight.Bold),
            color = theme.accent
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButton(
                text = "Tick",
                icon = Icons.Default.Vibration,
                onClick = {
                    playPattern("Crisp Tick", longArrayOf(0, 22), intArrayOf(0, 200))
                },
                modifier = Modifier.weight(1f),
                testTag = "vibration_lab_tick"
            )
            GlassButton(
                text = "Buzz",
                icon = Icons.Default.Vibration,
                onClick = {
                    playPattern("Continuous Buzz", longArrayOf(0, 320), intArrayOf(0, 255))
                },
                modifier = Modifier.weight(1f),
                testTag = "vibration_lab_buzz"
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButton(
                text = "Heartbeat",
                icon = Icons.Default.Vibration,
                onClick = {
                    playPattern(
                        "Double Heartbeat",
                        longArrayOf(0, 75, 95, 110, 420, 75, 95, 110),
                        intArrayOf(0, 180, 0, 255, 0, 180, 0, 255)
                    )
                },
                modifier = Modifier.weight(1f),
                testTag = "vibration_lab_heartbeat"
            )
            GlassButton(
                text = "SOS Pattern",
                icon = Icons.Default.Vibration,
                onClick = {
                    playPattern(
                        "SOS Morse (... --- ...)",
                        longArrayOf(0, 90, 80, 90, 80, 90, 180, 260, 100, 260, 100, 260, 180, 90, 80, 90, 80, 90),
                        intArrayOf(0, 240, 0, 240, 0, 240, 0, 255, 0, 255, 0, 255, 0, 240, 0, 240, 0, 240)
                    )
                },
                modifier = Modifier.weight(1f),
                testTag = "vibration_lab_sos"
            )
        }

        // Intensity slider shown ONLY when hardware amplitude control is supported
        if (hasAmplitudeControl) {
            Column {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Haptic Amplitude Intensity", style = type.caption, color = theme.textSecondary)
                    Text("${(intensity * 100).roundToInt()}%", style = type.caption.copy(fontWeight = FontWeight.Bold), color = theme.accent)
                }
                Slider(
                    value = intensity,
                    onValueChange = { intensity = it.coerceIn(0.1f, 1.0f) },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = theme.accent,
                        activeTrackColor = theme.accent
                    )
                )
            }
        }
    }
}

/**
 * 7) TOUCH SCREEN TEST (Free):
 * Multi-touch dots with a live touch-point count, and a draw-trail grid that must be fully covered
 * to confirm there are no dead zones.
 */
@Composable
fun TouchScreenTestInstrumentScreen() {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val gridCols = 6
    val gridRows = 8
    val totalCells = gridCols * gridRows

    val coveredCells = remember { mutableStateListOf<Int>() }
    val activePointers = remember { mutableStateListOf<Offset>() }
    var maxSimultaneousTouches by rememberSaveable { mutableIntStateOf(0) }

    val coveragePct = ((coveredCells.size.toFloat() / totalCells.toFloat()) * 100f).roundToInt()
    val allCovered = coveredCells.size >= totalCells

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Live Touch Points: ${activePointers.size} (Max: $maxSimultaneousTouches)",
                    style = type.body.copy(fontWeight = FontWeight.ExtraBold),
                    color = theme.accent
                )
                Text(
                    text = if (allCovered) {
                        "100% Grid Covered — Zero Dead Zones Confirmed!"
                    } else {
                        "Coverage: ${coveredCells.size}/$totalCells cells ($coveragePct%) — Drag across all cells"
                    },
                    style = type.caption,
                    color = if (allCovered) theme.accent else theme.textSecondary
                )
            }
            GlassButton(
                text = "Reset Grid",
                icon = Icons.Default.Refresh,
                onClick = {
                    coveredCells.clear()
                    activePointers.clear()
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(22.dp))
                .background(theme.elevated)
                .border(1.5.dp, if (allCovered) theme.accent else Color.White.copy(alpha = 0.14f), RoundedCornerShape(22.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        do {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            activePointers.clear()
                            pressed.forEach { change ->
                                activePointers.add(change.position)
                                val col = ((change.position.x / size.width) * gridCols).toInt().coerceIn(0, gridCols - 1)
                                val row = ((change.position.y / size.height) * gridRows).toInt().coerceIn(0, gridRows - 1)
                                val idx = row * gridCols + col
                                if (idx !in coveredCells) {
                                    coveredCells.add(idx)
                                }
                                change.consume()
                            }
                            if (activePointers.size > maxSimultaneousTouches) {
                                maxSimultaneousTouches = activePointers.size
                            }
                        } while (event.changes.any { it.pressed })
                        activePointers.clear()
                    }
                }
                .testTag("touch_screen_test_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellW = size.width / gridCols
                val cellH = size.height / gridRows
                for (r in 0 until gridRows) {
                    for (c in 0 until gridCols) {
                        val idx = r * gridCols + c
                        val isCovered = idx in coveredCells
                        drawRoundRect(
                            color = if (isCovered) theme.accent.copy(alpha = 0.36f) else theme.surface,
                            topLeft = Offset(c * cellW + 2.dp.toPx(), r * cellH + 2.dp.toPx()),
                            size = Size(cellW - 4.dp.toPx(), cellH - 4.dp.toPx()),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }

                // Multi-touch dots
                activePointers.forEachIndexed { idx, pt ->
                    drawCircle(
                        color = theme.accent,
                        radius = 28.dp.toPx(),
                        center = pt,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 10.dp.toPx(),
                        center = pt
                    )
                }
            }
        }
    }
}

/**
 * 8) METRONOME (Free):
 * 30-300 BPM, time signatures, tap tempo, an accented click synthesized in code,
 * visual pulse and haptic tick.
 */
@Composable
fun MetronomeInstrumentScreen() {
    val context = LocalContext.current
    val adManager = LocalAdManager.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val engine = remember(context) {
        AudioSynthesisEngine(context = context)
    }

    var bpm by rememberSaveable { mutableIntStateOf(120) }
    var beatsPerBar by rememberSaveable { mutableIntStateOf(4) } // 2/4, 3/4, 4/4, 6/8
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var currentBeat by remember { mutableIntStateOf(0) }
    var hapticTickEnabled by rememberSaveable { mutableStateOf(true) }
    var lastTapEpochMs by remember { mutableLongStateOf(0L) }

    DisposableEffect(isRunning) {
        adManager?.setSoundPlaying(isRunning)
        onDispose {
            adManager?.setSoundPlaying(false)
            engine.release()
        }
    }

    LaunchedEffect(isRunning, bpm, beatsPerBar, hapticTickEnabled) {
        if (!isRunning) {
            currentBeat = 0
            return@LaunchedEffect
        }
        var beatIndex = 0
        while (isActive && isRunning) {
            val accented = (beatIndex == 0)
            currentBeat = beatIndex + 1
            engine.playMetronomeClick(accented = accented)
            if (hapticTickEnabled) {
                FeedbackHelper.vibrateTick(
                    context,
                    durationMs = if (accented) 26L else 14L,
                    amplitude = if (accented) 230 else 130
                )
            }
            val intervalMs = (60_000L / bpm.coerceIn(30, 300)).coerceAtLeast(180L)
            delay(intervalMs)
            beatIndex = (beatIndex + 1) % beatsPerBar.coerceAtLeast(1)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "$bpm BPM",
            style = type.gaugeHero,
            color = theme.accent
        )

        // Visual Beat Pulse Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (b in 1..beatsPerBar) {
                val active = isRunning && currentBeat == b
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(if (active) 38.dp else 30.dp)
                        .clip(CircleShape)
                        .background(if (active) theme.accent else theme.elevated)
                        .border(1.5.dp, if (b == 1) theme.accent else Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Text(
                        text = "$b",
                        style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (active) BentoTextBlack else theme.textPrimary
                    )
                }
            }
        }

        // BPM Slider 30-300
        Slider(
            value = bpm.toFloat(),
            onValueChange = { bpm = it.roundToInt().coerceIn(30, 300) },
            valueRange = 30f..300f,
            colors = SliderDefaults.colors(
                thumbColor = theme.accent,
                activeTrackColor = theme.accent
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Time signatures
        DyneSegmentedControl(
            segments = listOf("2/4", "3/4", "4/4", "6/8"),
            selectedIndex = when (beatsPerBar) {
                2 -> 0
                3 -> 1
                6 -> 3
                else -> 2
            },
            onSegmentSelected = { idx ->
                beatsPerBar = when (idx) {
                    0 -> 2
                    1 -> 3
                    3 -> 6
                    else -> 4
                }
            }
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButton(
                text = if (isRunning) "Stop" else "Start",
                icon = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                accentFill = theme.accent,
                onClick = { isRunning = !isRunning },
                modifier = Modifier.weight(1f),
                testTag = "metronome_start_stop_button"
            )
            GlassButton(
                text = "Tap Tempo",
                icon = Icons.Default.TouchApp,
                onClick = {
                    val now = System.currentTimeMillis()
                    val delta = now - lastTapEpochMs
                    if (delta in 200L..2000L) {
                        bpm = (60_000L / delta).toInt().coerceIn(30, 300)
                    }
                    lastTapEpochMs = now
                },
                modifier = Modifier.weight(1f),
                testTag = "metronome_tap_tempo_button"
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Haptic Beat Tick", style = type.body, color = theme.textPrimary)
            DyneSwitch(checked = hapticTickEnabled, onCheckedChange = { hapticTickEnabled = it })
        }
    }
}

/**
 * 8b) TUNER (Premium):
 * Microphone pitch detection by autocorrelation, chromatic mode plus guitar, bass and ukulele
 * presets, needle gauge and cents readout. Requests RECORD_AUDIO only inside Tuner at use.
 */
@SuppressLint("MissingPermission")
@Composable
fun TunerInstrumentScreen(hazeState: HazeState?) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var hasAudioPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPerm = granted
    }

    var modeIndex by rememberSaveable { mutableIntStateOf(0) } // 0=Chromatic, 1=Guitar, 2=Bass, 3=Ukulele
    var pitchResult by remember { mutableStateOf(PitchDetectionResult()) }

    val presetReferenceText = remember(modeIndex) {
        when (modeIndex) {
            1 -> "Guitar Standard: E2 (82.4) • A2 (110.0) • D3 (146.8) • G3 (196.0) • B3 (246.9) • E4 (329.6)"
            2 -> "Bass 4-String: E1 (41.2) • A1 (55.0) • D2 (73.4) • G2 (98.0)"
            3 -> "Ukulele Standard: G4 (392.0) • C4 (261.6) • E4 (329.6) • A4 (440.0)"
            else -> "Chromatic 12-TET Autocorrelation (A4 = 440.0 Hz)"
        }
    }

    LaunchedEffect(hasAudioPerm) {
        if (!hasAudioPerm) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val sr = 48_000
            val bufSize = 4096
            val minBuf = AudioRecord.getMinBufferSize(
                sr,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(bufSize * 2)
            val recorder = runCatching {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sr,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf
                )
            }.getOrNull() ?: return@withContext

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                runCatching { recorder.release() }
                return@withContext
            }
            val buffer = ShortArray(bufSize)
            try {
                recorder.startRecording()
                while (isActive) {
                    val read = recorder.read(buffer, 0, bufSize)
                    if (read > 512) {
                        val detected = AudioSynthesisEngine.detectPitchAutocorrelation(buffer, read, sr)
                        if (detected.frequencyHz > 0f) {
                            pitchResult = detected
                        }
                    }
                    delay(65L)
                }
            } finally {
                runCatching {
                    recorder.stop()
                    recorder.release()
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!hasAudioPerm) {
            GlassPermissionCard(
                icon = Icons.Default.Mic,
                title = "Microphone Access for Tuner",
                explanation = "Dyne uses real-time autocorrelation in memory to detect note pitch and cents deviation.",
                buttonText = "Allow Microphone",
                hazeState = hazeState,
                onRequestPermission = { permLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )
            return@Column
        }

        DyneSegmentedControl(
            segments = listOf("Chromatic", "Guitar", "Bass", "Ukulele"),
            selectedIndex = modeIndex,
            onSegmentSelected = { modeIndex = it }
        )

        Text(
            text = pitchResult.noteName,
            style = type.gaugeHero,
            color = if (pitchResult.isLocked) theme.accent else theme.textPrimary
        )
        Text(
            text = if (pitchResult.frequencyHz > 0f) {
                String.format(
                    Locale.US,
                    "%.1f Hz  •  %+.1f cents (Target %.1f Hz)",
                    pitchResult.frequencyHz,
                    pitchResult.centsOff,
                    pitchResult.targetHz
                )
            } else {
                "Play a note near the microphone..."
            },
            style = type.gaugeSmall,
            color = if (pitchResult.isLocked) theme.accent else theme.textSecondary
        )

        // Needle Gauge Canvas (-50 to +50 cents)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(theme.elevated)
                .padding(16.dp)
        ) {
            val pivot = Offset(size.width / 2f, size.height * 0.90f)
            val radius = size.height * 0.76f
            drawArc(
                color = theme.surface,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(pivot.x - radius, pivot.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
            val normCents = (pitchResult.centsOff / 50f).coerceIn(-1f, 1f)
            val angleRad = (-90f + normCents * 75f) * (PI / 180.0)
            val tip = Offset(
                pivot.x + (sin(angleRad) * radius).toFloat(),
                pivot.y - (cos(angleRad) * radius).toFloat()
            )
            drawLine(
                color = if (abs(pitchResult.centsOff) <= 5f) theme.accent else StatusWarning,
                start = pivot,
                end = tip,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = theme.accent,
                radius = 6.dp.toPx(),
                center = pivot
            )
        }

        Text(
            text = presetReferenceText,
            style = type.caption,
            color = theme.textSecondary
        )
    }
}

/**
 * 9) BATTERY INFO (Free):
 * Level, status, temperature, voltage, health text, and current in mA when reported.
 * Do not claim battery capacity or battery repair.
 */
@Composable
fun BatteryInfoInstrumentScreen() {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var levelPct by remember { mutableIntStateOf(100) }
    var statusText by remember { mutableStateOf("Discharging") }
    var tempC by remember { mutableFloatStateOf(28.0f) }
    var voltageMv by remember { mutableIntStateOf(4000) }
    var healthText by remember { mutableStateOf("Good") }
    var currentMa by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(context) {
        while (isActive) {
            val intent = runCatching {
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            }.getOrNull()
            if (intent != null) {
                val lvl = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scl = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (lvl >= 0 && scl > 0) {
                    levelPct = ((lvl * 100f) / scl).roundToInt()
                }
                val st = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
                statusText = when (st) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                    BatteryManager.BATTERY_STATUS_FULL -> "Full"
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                    else -> "Active"
                }
                val t = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280)
                tempC = t / 10f
                voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4000)
                val h = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)
                healthText = when (h) {
                    BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                    BatteryManager.BATTERY_HEALTH_DEAD -> "Depleted"
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                    BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                    else -> "Normal"
                }
            }
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val rawUa = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            currentMa = if (rawUa != null && rawUa != Int.MIN_VALUE && rawUa != 0) {
                if (abs(rawUa) > 10_000) rawUa / 1000 else rawUa
            } else null

            delay(1500L)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "$levelPct%",
                    style = type.gaugeHero,
                    color = theme.accent
                )
                Text(
                    text = "Status: $statusText",
                    style = type.body.copy(fontWeight = FontWeight.Bold),
                    color = theme.textPrimary
                )
            }
            Icon(
                imageVector = Icons.Default.BatteryChargingFull,
                contentDescription = null,
                tint = theme.accent,
                modifier = Modifier.size(48.dp)
            )
        }

        BatteryMetricRow("Temperature", String.format(Locale.US, "%.1f °C (%.1f °F)", tempC, tempC * 1.8f + 32f))
        BatteryMetricRow("Voltage", "$voltageMv mV")
        BatteryMetricRow("Reported Health State", healthText)
        BatteryMetricRow(
            "Instantaneous Current",
            currentMa?.let { "$it mA" } ?: "Not reported by hardware controller"
        )
    }
}

@Composable
private fun BatteryMetricRow(label: String, value: String) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.elevated)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(label, style = type.body, color = theme.textSecondary)
        Text(value, style = type.body.copy(fontWeight = FontWeight.ExtraBold), color = theme.textPrimary)
    }
}
