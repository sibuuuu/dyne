package com.example.ui.audio

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ads.LocalAdManager
import com.example.billing.LocalFeatureGate
import com.example.billing.PremiumFeature
import com.example.billing.QuotaAllowanceChip
import com.example.billing.QuotaType
import com.example.billing.SoundPack
import com.example.ui.theme.ProGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

enum class FrequencyTemplateCategory(val label: String, val soundPack: SoundPack?) {
    BASIC("Basic", null),
    PETS("Pets & Animals", SoundPack.PETS),
    RELAX("Relax", SoundPack.RELAX),
    PRO_TONES("Pro Tones", SoundPack.PRO_TONES)
}

enum class FrequencyPlayMode(val label: String) {
    CONTINUOUS("Continuous"),
    PULSE("Pulse"),
    SWEEP("Sweep"),
    LOOP("Loop")
}

data class FrequencyTemplateItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: FrequencyTemplateCategory,
    val baseFreqHz: Float,
    val endFreqHz: Float = baseFreqHz,
    val mode: FrequencyPlayMode = FrequencyPlayMode.CONTINUOUS,
    val waveform: SynthWaveform = SynthWaveform.SINE,
    val isCalmDrone: Boolean = false,
    val isBinaural: Boolean = false,
    val binauralBeatHz: Float = 10f,
    val isBirdBursts: Boolean = false,
    val isDualTone: Boolean = false,
    val secondFreqHz: Float = 660f,
    val pulseOnMs: Int = 250,
    val pulseOffMs: Int = 250,
    val sweepDurationSec: Float = 8f
)

data class SavedFrequencyPreset(
    val id: String,
    val name: String,
    val freqHz: Float,
    val waveform: SynthWaveform,
    val mode: FrequencyPlayMode
) {
    fun serialize(): String = "${id.replace("|", "")}|${name.replace("|", "")}|$freqHz|${waveform.name}|${mode.name}"

    companion object {
        fun deserialize(raw: String): SavedFrequencyPreset? {
            val parts = raw.split("|")
            if (parts.size < 5) return null
            val freq = parts[2].toFloatOrNull() ?: return null
            val wave = runCatching { SynthWaveform.valueOf(parts[3]) }.getOrDefault(SynthWaveform.SINE)
            val mode = runCatching { FrequencyPlayMode.valueOf(parts[4]) }.getOrDefault(FrequencyPlayMode.CONTINUOUS)
            return SavedFrequencyPreset(parts[0], parts[1], freq, wave, mode)
        }
    }
}

private val ALL_FREQUENCY_TEMPLATES = listOf(
    // BASIC (Free)
    FrequencyTemplateItem(
        id = "basic_440",
        title = "440 Hz A4 Reference",
        subtitle = "Standard concert pitch tuning reference",
        category = FrequencyTemplateCategory.BASIC,
        baseFreqHz = 440f
    ),
    FrequencyTemplateItem(
        id = "basic_1000",
        title = "1 kHz Calibration",
        subtitle = "Audio level & line check tone",
        category = FrequencyTemplateCategory.BASIC,
        baseFreqHz = 1000f
    ),
    FrequencyTemplateItem(
        id = "basic_50_bass",
        title = "50 Hz Bass Check",
        subtitle = "Low-end speaker resonance test",
        category = FrequencyTemplateCategory.BASIC,
        baseFreqHz = 50f
    ),
    FrequencyTemplateItem(
        id = "basic_hearing_sweep",
        title = "Hearing-Range Sweep",
        subtitle = "20 Hz – 18 kHz sweep (not a medical test)",
        category = FrequencyTemplateCategory.BASIC,
        baseFreqHz = 20f,
        endFreqHz = 18000f,
        mode = FrequencyPlayMode.SWEEP,
        sweepDurationSec = 12f
    ),

    // PETS & ANIMALS (SoundPack.PETS)
    FrequencyTemplateItem(
        id = "pets_dog_whistle",
        title = "Dog Whistle (16–20 kHz)",
        subtitle = "High-frequency sweep & attention call",
        category = FrequencyTemplateCategory.PETS,
        baseFreqHz = 16500f,
        endFreqHz = 19500f,
        mode = FrequencyPlayMode.SWEEP,
        sweepDurationSec = 4f
    ),
    FrequencyTemplateItem(
        id = "pets_dog_pulse",
        title = "Dog Attention Pulse",
        subtitle = "17.5 kHz rhythmic training pulse",
        category = FrequencyTemplateCategory.PETS,
        baseFreqHz = 17500f,
        mode = FrequencyPlayMode.PULSE,
        pulseOnMs = 220,
        pulseOffMs = 280
    ),
    FrequencyTemplateItem(
        id = "pets_cat_attention",
        title = "Cat Attention",
        subtitle = "14.2 kHz gentle curiosity chirp",
        category = FrequencyTemplateCategory.PETS,
        baseFreqHz = 14200f,
        mode = FrequencyPlayMode.PULSE,
        pulseOnMs = 160,
        pulseOffMs = 340
    ),
    FrequencyTemplateItem(
        id = "pets_bird_bursts",
        title = "Bird Deterrent Bursts",
        subtitle = "Varied 2–8 kHz randomized sonic bursts",
        category = FrequencyTemplateCategory.PETS,
        baseFreqHz = 4500f,
        isBirdBursts = true
    ),
    FrequencyTemplateItem(
        id = "pets_gentle_test",
        title = "Distress-Free Pet Test",
        subtitle = "Low-volume 12 kHz soft check tone",
        category = FrequencyTemplateCategory.PETS,
        baseFreqHz = 12000f
    ),

    // RELAX (SoundPack.RELAX)
    FrequencyTemplateItem(
        id = "relax_174",
        title = "174 Hz Grounding Drone",
        subtitle = "Warm harmonic drone with slow breathing",
        category = FrequencyTemplateCategory.RELAX,
        baseFreqHz = 174f,
        isCalmDrone = true
    ),
    FrequencyTemplateItem(
        id = "relax_285",
        title = "285 Hz Serene Pad",
        subtitle = "Layered fifth & octave ambient tone",
        category = FrequencyTemplateCategory.RELAX,
        baseFreqHz = 285f,
        isCalmDrone = true
    ),
    FrequencyTemplateItem(
        id = "relax_432",
        title = "432 Hz Natural Calm",
        subtitle = "Soft breathing harmonic meditation tone",
        category = FrequencyTemplateCategory.RELAX,
        baseFreqHz = 432f,
        isCalmDrone = true
    ),
    FrequencyTemplateItem(
        id = "relax_528",
        title = "528 Hz Clarity Tone",
        subtitle = "Bright harmonic drone with slow LFO",
        category = FrequencyTemplateCategory.RELAX,
        baseFreqHz = 528f,
        isCalmDrone = true
    ),
    FrequencyTemplateItem(
        id = "relax_deep_focus",
        title = "Deep Focus Hum",
        subtitle = "136.1 Hz warm ambient foundation",
        category = FrequencyTemplateCategory.RELAX,
        baseFreqHz = 136.1f,
        isCalmDrone = true
    ),
    FrequencyTemplateItem(
        id = "relax_sleep_hum",
        title = "Sleep Hum (110 Hz)",
        subtitle = "Low soothing harmonic bed for rest",
        category = FrequencyTemplateCategory.RELAX,
        baseFreqHz = 110f,
        isCalmDrone = true
    ),
    FrequencyTemplateItem(
        id = "relax_binaural",
        title = "Binaural Focus (Headphones)",
        subtitle = "200 Hz L / 210 Hz R (10 Hz Alpha beat)",
        category = FrequencyTemplateCategory.RELAX,
        baseFreqHz = 200f,
        isBinaural = true,
        binauralBeatHz = 10f
    ),

    // PRO TONES (SoundPack.PRO_TONES)
    FrequencyTemplateItem(
        id = "pro_sub_sweep",
        title = "Subwoofer Sweep (20–120 Hz)",
        subtitle = "Deep bass linearity & rattle finder",
        category = FrequencyTemplateCategory.PRO_TONES,
        baseFreqHz = 20f,
        endFreqHz = 120f,
        mode = FrequencyPlayMode.SWEEP,
        sweepDurationSec = 10f
    ),
    FrequencyTemplateItem(
        id = "pro_full_sweep",
        title = "Full 20 Hz – 20 kHz Sweep",
        subtitle = "Complete audible spectrum sweep",
        category = FrequencyTemplateCategory.PRO_TONES,
        baseFreqHz = 20f,
        endFreqHz = 20000f,
        mode = FrequencyPlayMode.SWEEP,
        sweepDurationSec = 15f
    ),
    FrequencyTemplateItem(
        id = "pro_burn_in",
        title = "Speaker Burn-In Loop",
        subtitle = "Safe-level warm-up sweep loop",
        category = FrequencyTemplateCategory.PRO_TONES,
        baseFreqHz = 60f,
        endFreqHz = 6000f,
        mode = FrequencyPlayMode.LOOP,
        sweepDurationSec = 8f
    ),
    FrequencyTemplateItem(
        id = "pro_dual_tone",
        title = "Dual-Tone IMD Test",
        subtitle = "440 Hz + 660 Hz intermodulation check",
        category = FrequencyTemplateCategory.PRO_TONES,
        baseFreqHz = 440f,
        isDualTone = true,
        secondFreqHz = 660f
    ),
    FrequencyTemplateItem(
        id = "pro_square_set",
        title = "Square-Wave Transient Set",
        subtitle = "500 Hz rich odd-harmonic square wave",
        category = FrequencyTemplateCategory.PRO_TONES,
        baseFreqHz = 500f,
        waveform = SynthWaveform.SQUARE
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FrequencyInstrumentScreen(
    isPro: Boolean,
    ownedSoundPacks: Set<String>,
    soundPackPrices: Map<String, String>,
    safetyDismissed: Boolean,
    onDismissSafety: () -> Unit,
    presetsCsv: String,
    onSavePresetsCsv: (String) -> Unit,
    onOpenSpeakerCleaner: () -> Unit,
    forcedModeId: String? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val featureGate = LocalFeatureGate.current
    val adManager = LocalAdManager.current

    val isFreqProUnlocked = isPro ||
        (featureGate?.isUnlocked(PremiumFeature.FREQUENCY_PRO) == true) ||
        (featureGate?.isPackUnlocked(SoundPack.PRO_TONES) == true) ||
        ownedSoundPacks.contains(SoundPack.PRO_TONES.productId.lowercase())

    var freqHz by remember { mutableFloatStateOf(440f) }
    var secondFreqHz by remember { mutableFloatStateOf(660f) }
    var isPlaying by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(FrequencyTemplateCategory.BASIC) }
    var selectedTemplate by remember { mutableStateOf<FrequencyTemplateItem?>(null) }
    var waveform by remember { mutableStateOf(SynthWaveform.SINE) }
    var playMode by remember { mutableStateOf(FrequencyPlayMode.CONTINUOUS) }

    // Volume: Free users capped at 0.50f unless FREQUENCY_PRO or PRO_TONES pack is unlocked
    var volume by remember { mutableFloatStateOf(0.45f) }
    val maxAllowedVolume = if (isFreqProUnlocked) 0.90f else 0.50f

    // Mode parameters
    var pulseOnMs by remember { mutableIntStateOf(250) }
    var pulseOffMs by remember { mutableIntStateOf(250) }
    var sweepStartHz by remember { mutableFloatStateOf(100f) }
    var sweepEndHz by remember { mutableFloatStateOf(4000f) }
    var sweepDurationSec by remember { mutableFloatStateOf(8f) }

    LaunchedEffect(forcedModeId) {
        when (forcedModeId?.lowercase(Locale.US)) {
            "single_tone" -> {
                selectedCategory = FrequencyTemplateCategory.BASIC
                selectedTemplate = null
                playMode = FrequencyPlayMode.CONTINUOUS
                freqHz = 440f
            }
            "sweep" -> {
                selectedCategory = FrequencyTemplateCategory.PRO_TONES
                selectedTemplate = ALL_FREQUENCY_TEMPLATES.firstOrNull { it.id == "pro_full_sweep" }
                playMode = FrequencyPlayMode.SWEEP
                sweepStartHz = 20f
                sweepEndHz = 20000f
                sweepDurationSec = 15f
            }
            "binaural" -> {
                selectedCategory = FrequencyTemplateCategory.RELAX
                selectedTemplate = ALL_FREQUENCY_TEMPLATES.firstOrNull { it.id == "relax_binaural" }
                playMode = FrequencyPlayMode.CONTINUOUS
                freqHz = 200f
            }
            "dog_whistle" -> {
                selectedCategory = FrequencyTemplateCategory.PETS
                selectedTemplate = ALL_FREQUENCY_TEMPLATES.firstOrNull { it.id == "pets_dog_whistle" }
                playMode = FrequencyPlayMode.SWEEP
                freqHz = 16500f
                sweepStartHz = 16500f
                sweepEndHz = 19500f
                sweepDurationSec = 4f
            }
            "hearing_check" -> {
                selectedCategory = FrequencyTemplateCategory.BASIC
                selectedTemplate = ALL_FREQUENCY_TEMPLATES.firstOrNull { it.id == "basic_hearing_sweep" }
                playMode = FrequencyPlayMode.SWEEP
                freqHz = 20f
                sweepStartHz = 20f
                sweepEndHz = 18000f
                sweepDurationSec = 12f
            }
            "presets" -> {
                selectedCategory = FrequencyTemplateCategory.BASIC
            }
        }
    }

    // Timer: 0 = Off, 1, 5, 10, 30 min (Free up to 10 min; 30 min requires Pro)
    var timerMinutes by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableIntStateOf(0) }
    var keepScreenOn by remember { mutableStateOf(false) }

    // Exact frequency input dialog
    var showExactInputDialog by remember { mutableStateOf(false) }
    var exactInputText by remember { mutableStateOf("440") }

    val tonePlayer = remember(context) {
        AudioSynthesisEngine(context) { playing ->
            isPlaying = playing
            adManager?.setSoundPlaying(playing)
        }
    }

    // External headset / Bluetooth state
    var hasExternalAudio by remember {
        mutableStateOf(tonePlayer.refreshExternalAudioDevices())
    }
    var externalAudioWarning by remember { mutableStateOf<String?>(null) }

    // Goertzel Check Output state
    var isCheckingOutput by remember { mutableStateOf(false) }
    var goertzelResult by remember { mutableStateOf<Pair<Float, String>?>(null) }
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            isCheckingOutput = true
            scope.launch {
                val res = AudioSynthesisEngine.measureTargetFrequencyWithGoertzel(
                    targetHz = freqHz
                )
                goertzelResult = res
                isCheckingOutput = false
            }
        }
    }

    // Saved presets
    val savedPresets = remember(presetsCsv) {
        presetsCsv.split(";")
            .mapNotNull { SavedFrequencyPreset.deserialize(it) }
    }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }
    var editingPreset by remember { mutableStateOf<SavedFrequencyPreset?>(null) }

    // Press-and-hold fine tune delta
    var holdDeltaHz by remember { mutableFloatStateOf(0f) }

    // Keep screen on when requested and playing
    DisposableEffect(isPlaying, keepScreenOn) {
        val window = (context as? android.app.Activity)?.window
        if (isPlaying && keepScreenOn) {
            view.keepScreenOn = true
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            view.keepScreenOn = false
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            view.keepScreenOn = false
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Monitor external audio devices (Headphones / Bluetooth / USB)
    DisposableEffect(context, selectedCategory, isPlaying) {
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
        val callback = object : android.media.AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out android.media.AudioDeviceInfo>?) {
                val ext = tonePlayer.refreshExternalAudioDevices()
                hasExternalAudio = ext
                if (ext && isPlaying && selectedCategory == FrequencyTemplateCategory.PETS) {
                    tonePlayer.stopImmediate()
                    isPlaying = false
                    adManager?.setSoundPlaying(false)
                    externalAudioWarning = "Stopped: Disconnect headphones or Bluetooth before playing high-frequency Pet tones."
                }
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out android.media.AudioDeviceInfo>?) {
                hasExternalAudio = tonePlayer.refreshExternalAudioDevices()
            }
        }
        audioManager.registerAudioDeviceCallback(callback, null)
        onDispose {
            audioManager.unregisterAudioDeviceCallback(callback)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tonePlayer.release()
            adManager?.setSoundPlaying(false)
        }
    }

    // Clamp volume if Free
    LaunchedEffect(maxAllowedVolume) {
        if (volume > maxAllowedVolume) {
            volume = maxAllowedVolume
            tonePlayer.amplitude = volume
        }
    }

    // Handle press-and-hold fine tuning
    LaunchedEffect(holdDeltaHz) {
        if (holdDeltaHz != 0f) {
            delay(320L)
            while (holdDeltaHz != 0f) {
                val updated = (freqHz + holdDeltaHz).coerceIn(20f, 20000f)
                freqHz = updated
                tonePlayer.targetFrequencyHz = updated
                delay(75L)
            }
        }
    }

    // Sync active tone parameters
    fun applySynthParamsToEngine() {
        tonePlayer.targetFrequencyHz = freqHz
        tonePlayer.waveform = waveform
        tonePlayer.amplitude = volume.coerceAtMost(maxAllowedVolume)
        val tpl = selectedTemplate
        tonePlayer.layeredDroneFrequencies = if (tpl?.isCalmDrone == true) {
            floatArrayOf(freqHz, freqHz * 1.5f, freqHz * 2f)
        } else null
        tonePlayer.binauralBeatOffsetHz = if (tpl?.isBinaural == true) tpl.binauralBeatHz else 0f
        tonePlayer.birdDeterrentMode = tpl?.isBirdBursts == true
        tonePlayer.secondaryFrequencyHz = if (tpl?.isDualTone == true) secondFreqHz else 0f
        tonePlayer.blockExternalAudioOutputs = selectedCategory == FrequencyTemplateCategory.PETS
        tonePlayer.pulseOnMs = pulseOnMs
        tonePlayer.pulseOffMs = pulseOffMs
        tonePlayer.sweepStartHz = sweepStartHz
        tonePlayer.sweepEndHz = sweepEndHz
        tonePlayer.sweepDurationSec = sweepDurationSec
        tonePlayer.playMode = when (playMode) {
            FrequencyPlayMode.CONTINUOUS -> SynthPlayMode.CONTINUOUS
            FrequencyPlayMode.PULSE -> SynthPlayMode.PULSE
            FrequencyPlayMode.SWEEP -> SynthPlayMode.SWEEP
            FrequencyPlayMode.LOOP -> SynthPlayMode.LOOP
        }
    }

    fun startTone() {
        if (selectedCategory == FrequencyTemplateCategory.PETS && tonePlayer.refreshExternalAudioDevices()) {
            externalAudioWarning = "Disconnect earphones or Bluetooth before playing high-frequency Pet tones."
            return
        }
        externalAudioWarning = null
        applySynthParamsToEngine()
        if (timerMinutes > 0) {
            remainingSeconds = timerMinutes * 60
        } else {
            remainingSeconds = 0
        }
        isPlaying = true
        adManager?.setSoundPlaying(true)
        tonePlayer.start(fadeInSeconds = 1.5f)
    }

    fun stopTone() {
        tonePlayer.stopImmediate()
        isPlaying = false
        remainingSeconds = 0
        adManager?.setSoundPlaying(false)
    }

    // Timer countdown loop
    LaunchedEffect(isPlaying, remainingSeconds, playMode) {
        while (isPlaying) {
            delay(200L)
            if (playMode == FrequencyPlayMode.SWEEP || playMode == FrequencyPlayMode.LOOP) {
                freqHz = tonePlayer.liveCurrentHz.value
            }
            // Check every 1 second
            delay(800L)
            if (!isPlaying) break
            if (remainingSeconds > 0) {
                remainingSeconds -= 1
                if (remainingSeconds <= 0) {
                    stopTone()
                    break
                }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "freq_osc")
    val oscPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "osc_phase"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // First-use dismissible hearing safety notice
        AnimatedVisibility(visible = !safetyDismissed) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF162032))
                    .border(1.dp, ProGold.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ProGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hearing & Speaker Safety", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    IconButton(onClick = onDismissSafety, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss safety notice", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    text = "Start at a low volume and never hold the speaker directly against your ear or a pet's ear. Phone speakers usually cannot play above about 18-20 kHz.",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        // High frequency (>8 kHz) or Hardware Ceiling (>18 kHz) live reminder
        if (freqHz >= 8000f) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFB300).copy(alpha = 0.14f))
                    .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (freqHz >= 18000f) {
                        "Phone speakers usually cannot play above about 18-20 kHz. Keep volume low even if you cannot hear the tone."
                    } else {
                        "High frequency (>8 kHz): Keep volume low to protect hearing and small speaker drivers."
                    },
                    color = Color(0xFFFFE082),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        // External audio warning banner
        externalAudioWarning?.let { warn ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFF5252).copy(alpha = 0.16f))
                    .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Headphones, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = warn, color = Color(0xFFFFCDD2), fontSize = 12.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { externalAudioWarning = null }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }

        // Giant Frequency Readout Card + Live Oscilloscope & 16-bar Spectrum
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF141D2B), Color(0xFF0B1018))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        exactInputText = String.format(Locale.US, "%.1f", freqHz)
                        showExactInputDialog = true
                    }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formattedReadout = if (freqHz >= 1000f) {
                    String.format(Locale.US, "%.2f kHz", freqHz / 1000f)
                } else {
                    String.format(Locale.US, "%.1f Hz", freqHz)
                }
                Text(
                    text = formattedReadout,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 40.sp,
                    color = if (isPlaying) ProGold else Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Type exact frequency",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "${String.format(Locale.US, "%.1f", freqHz)} Hz • Tap readout to type exact value",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Oscilloscope + 16-bar Spectrum Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF070B10))
                    .padding(8.dp)
            ) {
                val w = size.width
                val h = size.height
                val midY = h * 0.42f

                // Top portion: Live waveform oscilloscope
                val path = Path()
                val cycles = (ln(freqHz.coerceAtLeast(20f) / 20f) / ln(1000f) * 8f + 2f).coerceIn(2f, 12f)
                val ampPx = if (isPlaying) (h * 0.30f * (volume / 0.9f).coerceIn(0.2f, 1f)) else h * 0.08f
                val steps = 120
                for (i in 0..steps) {
                    val x = (i.toFloat() / steps) * w
                    val t = (i.toFloat() / steps) * cycles * 2f * PI.toFloat() + (if (isPlaying) oscPhase else 0f)
                    val sample = when (waveform) {
                        SynthWaveform.SINE -> sin(t)
                        SynthWaveform.SQUARE -> if (sin(t) >= 0f) 1f else -1f
                        SynthWaveform.SAW -> 2f * ((t / (2f * PI.toFloat())) - kotlin.math.floor(0.5f + t / (2f * PI.toFloat())))
                        SynthWaveform.TRIANGLE -> (2f / PI.toFloat()) * kotlin.math.asin(sin(t))
                        SynthWaveform.NOISE -> sin(t * 3.7f) * cos(t * 7.1f)
                    }
                    val y = midY - sample * ampPx
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = if (isPlaying) ProGold else Color(0xFF4FC3F7).copy(alpha = 0.55f),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Bottom portion: 16-bar logarithmic spectrum indicator
                val barCount = 16
                val spacing = 4.dp.toPx()
                val barWidth = (w - spacing * (barCount - 1)) / barCount
                val logNorm = (ln(freqHz.coerceIn(20f, 20000f) / 20f) / ln(1000f)).toFloat().coerceIn(0f, 1f)
                val targetBin = (logNorm * (barCount - 1)).roundToInt()

                for (b in 0 until barCount) {
                    val dist = kotlin.math.abs(b - targetBin)
                    val baseRatio = when {
                        waveform == SynthWaveform.NOISE -> 0.65f
                        dist == 0 -> 1f
                        dist == 1 -> 0.55f
                        dist == 2 && waveform != SynthWaveform.SINE -> 0.35f
                        else -> 0.12f
                    }
                    val activeRatio = if (isPlaying) baseRatio else baseRatio * 0.25f
                    val barH = (h * 0.26f * activeRatio).coerceAtLeast(3.dp.toPx())
                    val bx = b * (barWidth + spacing)
                    val by = h - barH
                    drawRoundRect(
                        color = if (b == targetBin && isPlaying) ProGold else Color(0xFF4FC3F7).copy(alpha = 0.45f),
                        topLeft = Offset(bx, by),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Precision Rotary Dial + Logarithmic Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Precision Rotary Dial
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val pos = change.position
                            val angleRad = atan2(pos.y - center.y, pos.x - center.x)
                            var deg = Math.toDegrees(angleRad.toDouble()).toFloat() + 90f
                            if (deg < 0f) deg += 360f
                            val norm = ((deg - 30f) / 300f).coerceIn(0f, 1f)
                            val newHz = (20.0 * 1000.0.pow(norm.toDouble())).toFloat().coerceIn(20f, 20000f)
                            freqHz = newHz
                            tonePlayer.targetFrequencyHz = newHz
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val logNorm = (ln(freqHz.coerceIn(20f, 20000f) / 20f) / ln(1000f)).toFloat().coerceIn(0f, 1f)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 8.dp.toPx()
                    drawArc(
                        color = Color.White.copy(alpha = 0.1f),
                        startAngle = 120f,
                        sweepAngle = 300f,
                        useCenter = false,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = ProGold,
                        startAngle = 120f,
                        sweepAngle = 300f * logNorm,
                        useCenter = false,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                    val knobAngleRad = Math.toRadians((120f + 300f * logNorm).toDouble())
                    val r = size.minDimension / 2f - stroke
                    val kx = center.x + r * cos(knobAngleRad).toFloat()
                    val ky = center.y + r * sin(knobAngleRad).toFloat()
                    drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(kx, ky))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = ProGold, modifier = Modifier.size(20.dp))
                    Text("DIAL", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Wide Logarithmic Slider + Range labels
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Logarithmic Frequency (20 Hz – 20 kHz)",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                val sliderPos = (ln(freqHz.coerceIn(20f, 20000f) / 20f) / ln(1000f)).toFloat().coerceIn(0f, 1f)
                Slider(
                    value = sliderPos,
                    onValueChange = { norm ->
                        val newHz = (20.0 * 1000.0.pow(norm.toDouble())).toFloat().coerceIn(20f, 20000f)
                        freqHz = newHz
                        tonePlayer.targetFrequencyHz = newHz
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = ProGold,
                        activeTrackColor = ProGold
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("20 Hz", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
                    Text("200 Hz", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
                    Text("2 kHz", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
                    Text("20 kHz", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Fine-tune step buttons (-100, -10, -1, +1, +10, +100 Hz) with press-and-hold
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val steps = listOf(-100f, -10f, -1f, 1f, 10f, 100f)
            steps.forEach { delta ->
                val label = if (delta > 0) "+${delta.toInt()}" else "${delta.toInt()}"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                        .pointerInput(delta) {
                            detectTapGestures(
                                onPress = {
                                    val updated = (freqHz + delta).coerceIn(20f, 20000f)
                                    freqHz = updated
                                    tonePlayer.targetFrequencyHz = updated
                                    holdDeltaHz = delta
                                    tryAwaitRelease()
                                    holdDeltaHz = 0f
                                }
                            )
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Large Glowing Play / Stop Button
        Button(
            onClick = {
                if (isPlaying) {
                    stopTone()
                } else {
                    startTone()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPlaying) Color(0xFFFF5252) else ProGold,
                contentColor = if (isPlaying) Color.White else Color.Black
            )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isPlaying) {
                    if (remainingSeconds > 0) {
                        "STOP (${remainingSeconds / 60}:${String.format(Locale.US, "%02d", remainingSeconds % 60)})"
                    } else {
                        "STOP TONE"
                    }
                } else {
                    "PLAY TONE (1.5S FADE-IN)"
                },
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Waveform Selector Chips (Sine free; Square, Saw, Triangle, Noise require FREQUENCY_PRO or PRO_TONES)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(14.dp)
        ) {
            Text("Waveform & Output Level", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SynthWaveform.entries.forEach { wf ->
                    val requiresPro = wf != SynthWaveform.SINE
                    val selected = waveform == wf
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (requiresPro && !isFreqProUnlocked) {
                                featureGate?.require(PremiumFeature.FREQUENCY_PRO) {
                                    waveform = wf
                                    tonePlayer.waveform = wf
                                }
                            } else {
                                waveform = wf
                                tonePlayer.waveform = wf
                            }
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(wf.label, fontWeight = FontWeight.SemiBold)
                                if (requiresPro && !isFreqProUnlocked) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Lock, contentDescription = "Pro", modifier = Modifier.size(12.dp), tint = ProGold)
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ProGold.copy(alpha = 0.22f),
                            selectedLabelColor = ProGold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Digital Gain: ${(volume * 100).roundToInt()}% ${if (!isFreqProUnlocked) "(Free safe cap 50%)" else ""}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
                if (!isFreqProUnlocked) {
                    Text(
                        text = "Unlock 90% Gain",
                        color = ProGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            featureGate?.require(PremiumFeature.FREQUENCY_PRO) {}
                        }
                    )
                }
            }
            Slider(
                value = volume,
                onValueChange = { v ->
                    if (v > maxAllowedVolume && !isFreqProUnlocked) {
                        volume = maxAllowedVolume
                        tonePlayer.amplitude = maxAllowedVolume
                        featureGate?.require(PremiumFeature.FREQUENCY_PRO) {}
                    } else {
                        volume = v.coerceIn(0.05f, maxAllowedVolume)
                        tonePlayer.amplitude = volume
                    }
                },
                valueRange = 0.05f..0.90f,
                colors = SliderDefaults.colors(thumbColor = ProGold, activeTrackColor = ProGold)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Modes (Continuous, Pulse, Sweep, Loop) + Timer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(14.dp)
        ) {
            Text("Mode & Session Timer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FrequencyPlayMode.entries.forEach { m ->
                    val requiresPro = m != FrequencyPlayMode.CONTINUOUS && selectedTemplate?.category != FrequencyTemplateCategory.BASIC
                    FilterChip(
                        selected = playMode == m,
                        onClick = {
                            if (requiresPro && !isFreqProUnlocked && m != FrequencyPlayMode.CONTINUOUS) {
                                featureGate?.require(PremiumFeature.FREQUENCY_PRO) {
                                    playMode = m
                                    applySynthParamsToEngine()
                                }
                            } else {
                                playMode = m
                                applySynthParamsToEngine()
                            }
                        },
                        label = { Text(m.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ProGold.copy(alpha = 0.22f),
                            selectedLabelColor = ProGold
                        )
                    )
                }
            }

            if (playMode == FrequencyPlayMode.PULSE) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pulse On: ${pulseOnMs}ms • Off: ${pulseOffMs}ms",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
                Slider(
                    value = pulseOnMs.toFloat(),
                    onValueChange = {
                        pulseOnMs = it.roundToInt()
                        pulseOffMs = it.roundToInt()
                        tonePlayer.pulseOnMs = pulseOnMs
                        tonePlayer.pulseOffMs = pulseOffMs
                    },
                    valueRange = 80f..1000f
                )
            } else if (playMode == FrequencyPlayMode.SWEEP || playMode == FrequencyPlayMode.LOOP) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sweep ${sweepStartHz.roundToInt()} Hz → ${sweepEndHz.roundToInt()} Hz over ${sweepDurationSec.roundToInt()}s",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
                Slider(
                    value = sweepDurationSec,
                    onValueChange = {
                        sweepDurationSec = it
                        tonePlayer.sweepDurationSec = it
                    },
                    valueRange = 2f..20f
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Timer:", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                listOf(0, 1, 5, 10, 30).forEach { min ->
                    val locked30 = min == 30 && !isFreqProUnlocked
                    FilterChip(
                        selected = timerMinutes == min,
                        onClick = {
                            if (locked30) {
                                featureGate?.require(PremiumFeature.FREQUENCY_PRO) {
                                    timerMinutes = min
                                }
                            } else {
                                timerMinutes = min
                                if (isPlaying) {
                                    remainingSeconds = if (min > 0) min * 60 else 0
                                }
                            }
                        },
                        label = {
                            Text(if (min == 0) "Off" else "${min}m", fontSize = 11.sp)
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Keep screen on during playback", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                Switch(checked = keepScreenOn, onCheckedChange = { keepScreenOn = it })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sound Packs & Category Template Tabs
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF121926))
                .border(1.dp, ProGold.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = ProGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Templates & Sound Packs", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal Category Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FrequencyTemplateCategory.entries.forEach { cat ->
                    val pack = cat.soundPack
                    val unlocked = pack == null ||
                        (featureGate?.isPackUnlocked(pack) == true) ||
                        ownedSoundPacks.contains(pack.productId.lowercase()) ||
                        isPro
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(cat.label, fontWeight = FontWeight.Bold)
                                if (!unlocked) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = ProGold, modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ProGold.copy(alpha = 0.24f),
                            selectedLabelColor = ProGold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category-specific honest disclaimer cards
            if (selectedCategory == FrequencyTemplateCategory.PETS) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Pets, contentDescription = null, tint = ProGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Experimental sound templates for pet training and attention. Every animal reacts differently; stop immediately if an animal seems uncomfortable. This tool does not guarantee pest or animal deterrence and is not a safety device.",
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else if (selectedCategory == FrequencyTemplateCategory.RELAX) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = Color(0xFF80CBC4), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "For relaxation and focus only. Not medical or therapeutic treatment.",
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            val templatesForCategory = remember(selectedCategory) {
                ALL_FREQUENCY_TEMPLATES.filter { it.category == selectedCategory }
            }

            templatesForCategory.forEach { tpl ->
                val pack = tpl.category.soundPack
                val isUnlocked = pack == null ||
                    (featureGate?.isPackUnlocked(pack) == true) ||
                    ownedSoundPacks.contains(pack.productId.lowercase()) ||
                    isPro
                val isSelected = selectedTemplate?.id == tpl.id

                val applyAndPlay: () -> Unit = {
                    selectedTemplate = tpl
                    freqHz = tpl.baseFreqHz
                    secondFreqHz = tpl.secondFreqHz
                    waveform = tpl.waveform
                    playMode = tpl.mode
                    pulseOnMs = tpl.pulseOnMs
                    pulseOffMs = tpl.pulseOffMs
                    sweepStartHz = tpl.baseFreqHz
                    sweepEndHz = tpl.endFreqHz
                    sweepDurationSec = tpl.sweepDurationSec
                    if (tpl.isBinaural && !tonePlayer.refreshExternalAudioDevices()) {
                        externalAudioWarning = "Binaural Focus works best with stereo headphones connected."
                    }
                    startTone()
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) ProGold.copy(alpha = 0.16f)
                            else Color.White.copy(alpha = 0.04f)
                        )
                        .border(
                            1.dp,
                            if (isSelected) ProGold else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            if (isUnlocked) {
                                applyAndPlay()
                            } else if (pack != null) {
                                featureGate?.requirePack(
                                    pack = pack,
                                    onUnlocked = { applyAndPlay() }
                                )
                            }
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tpl.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (!isUnlocked && pack != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = soundPackPrices[pack.productId] ?: "PACK",
                                    color = ProGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ProGold.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(tpl.subtitle, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
                    }
                    Icon(
                        imageVector = if (isUnlocked) Icons.Default.PlayArrow else Icons.Default.Lock,
                        contentDescription = null,
                        tint = ProGold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Linked Speaker Cleaner (165 Hz) Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF00363A), Color(0xFF006064))
                    )
                )
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                .clickable {
                    stopTone()
                    onOpenSpeakerCleaner()
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Speaker Cleaner (165 Hz)", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Text(
                        "Open Water Eject & Dust Shake with guided checklist",
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 12.sp
                    )
                }
            }
            Text("OPEN", color = Color(0xFF00E5FF), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Goertzel "Check Output" Microphone Verification Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Check Output (Goertzel Detector)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "Listens briefly with the microphone to confirm whether your speaker is producing ${freqHz.roundToInt()} Hz",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        if (!isPlaying) {
                            startTone()
                        }
                        if (!hasMicPermission) {
                            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else if (!isCheckingOutput) {
                            isCheckingOutput = true
                            scope.launch {
                                val res = AudioSynthesisEngine.measureTargetFrequencyWithGoertzel(
                                    targetHz = freqHz
                                )
                                goertzelResult = res
                                isCheckingOutput = false
                            }
                        }
                    },
                    enabled = !isCheckingOutput
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isCheckingOutput) "Listening..." else "Check output", fontSize = 12.sp)
                }
            }

            if (isCheckingOutput) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ProGold)
            }

            goertzelResult?.let { (db, verdict) ->
                val detected = db > -52f
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (detected) Color(0xFF00E676).copy(alpha = 0.14f)
                            else Color(0xFFFFB300).copy(alpha = 0.14f)
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (detected) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (detected) Color(0xFF00E676) else Color(0xFFFFB300),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = verdict,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // My Presets (Save, Rename, Delete; gated by FREQUENCY_PRO)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("My Presets", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                TextButton(
                    onClick = {
                        if (isFreqProUnlocked) {
                            editingPreset = null
                            presetNameInput = "${freqHz.roundToInt()} Hz ${waveform.label}"
                            showSavePresetDialog = true
                        } else {
                            featureGate?.require(PremiumFeature.FREQUENCY_PRO) {
                                editingPreset = null
                                presetNameInput = "${freqHz.roundToInt()} Hz ${waveform.label}"
                                showSavePresetDialog = true
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = ProGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Current", color = ProGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            if (savedPresets.isEmpty()) {
                Text(
                    text = "Save your favorite frequency, waveform, and mode combinations for instant recall.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp
                )
            } else {
                savedPresets.forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .clickable {
                                freqHz = preset.freqHz
                                waveform = preset.waveform
                                playMode = preset.mode
                                startTone()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(preset.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                "${preset.freqHz.roundToInt()} Hz • ${preset.waveform.label} • ${preset.mode.label}",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                editingPreset = preset
                                presetNameInput = preset.name
                                showSavePresetDialog = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename preset", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                val updated = savedPresets.filterNot { it.id == preset.id }
                                    .joinToString(";") { it.serialize() }
                                onSavePresetsCsv(updated)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete preset", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

    if (showExactInputDialog) {
        AlertDialog(
            onDismissRequest = { showExactInputDialog = false },
            title = { Text("Enter Exact Frequency (Hz)") },
            text = {
                OutlinedTextField(
                    value = exactInputText,
                    onValueChange = { exactInputText = it },
                    label = { Text("20.0 – 20000.0 Hz") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = exactInputText.toFloatOrNull()
                        if (parsed != null) {
                            val clamped = parsed.coerceIn(20f, 20000f)
                            freqHz = clamped
                            tonePlayer.targetFrequencyHz = clamped
                        }
                        showExactInputDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExactInputDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text(if (editingPreset == null) "Save Frequency Preset" else "Rename Preset") },
            text = {
                OutlinedTextField(
                    value = presetNameInput,
                    onValueChange = { presetNameInput = it },
                    label = { Text("Preset Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cleanName = presetNameInput.trim().ifEmpty { "${freqHz.roundToInt()} Hz" }
                        val target = editingPreset
                        val nextList = if (target == null) {
                            savedPresets + SavedFrequencyPreset(
                                id = System.currentTimeMillis().toString(),
                                name = cleanName,
                                freqHz = freqHz,
                                waveform = waveform,
                                mode = playMode
                            )
                        } else {
                            savedPresets.map {
                                if (it.id == target.id) it.copy(name = cleanName) else it
                            }
                        }
                        onSavePresetsCsv(nextList.joinToString(";") { it.serialize() })
                        showSavePresetDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
