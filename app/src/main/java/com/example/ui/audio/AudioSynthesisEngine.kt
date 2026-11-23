package com.example.ui.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class SynthWaveform(val label: String, val isProInToneGen: Boolean) {
    SINE("Sine", false),
    SQUARE("Square", true),
    SAW("Saw", true),
    TRIANGLE("Triangle", true),
    NOISE("Noise", true)
}

enum class SynthPlayMode(val label: String) {
    CONTINUOUS("Continuous"),
    PULSE("Pulse"),
    SWEEP("Sweep"),
    LOOP("Loop")
}

enum class StereoRouteMode(val label: String) {
    BOTH("Both (Stereo)"),
    LEFT_ONLY("Left Channel"),
    RIGHT_ONLY("Right Channel"),
    EARPIECE("Earpiece")
}

data class SleepMixerLevels(
    val white: Float = 0.0f,
    val pink: Float = 0.35f,
    val brown: Float = 0.45f,
    val rain: Float = 0.30f,
    val wind: Float = 0.20f
)

data class PitchDetectionResult(
    val frequencyHz: Float = 0f,
    val noteName: String = "--",
    val centsOff: Float = 0f,
    val targetHz: Float = 440f,
    val isLocked: Boolean = false
)

/**
 * Pure Kotlin 48 kHz AudioTrack synthesizer & audio hardware monitor.
 * - Generates all sound in code with AudioTrack at 48,000 Hz (no audio assets).
 * - Monitors Headphones / Bluetooth / USB audio output with AudioManager.getDevices and AudioDeviceCallback.
 * - Manages AudioFocus and stops automatically on focus loss or incoming/active phone call.
 * - Exposes live oscilloscope samples and spectrum bars for Compose Canvas rendering.
 */
class AudioSynthesisEngine(
    context: Context,
    private val onAudioPlayingStateChanged: (Boolean) -> Unit = {}
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var playbackJob: Job? = null
    private var focusRequest: AudioFocusRequest? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _externalHeadsetConnected = MutableStateFlow(false)
    val externalHeadsetConnected: StateFlow<Boolean> = _externalHeadsetConnected.asStateFlow()

    private val _externalDeviceWarning = MutableStateFlow<String?>(null)
    val externalDeviceWarning: StateFlow<String?> = _externalDeviceWarning.asStateFlow()

    private val _liveCurrentHz = MutableStateFlow(440f)
    val liveCurrentHz: StateFlow<Float> = _liveCurrentHz.asStateFlow()

    private val _oscilloscopeSamples = MutableStateFlow(FloatArray(128))
    val oscilloscopeSamples: StateFlow<FloatArray> = _oscilloscopeSamples.asStateFlow()

    private val _spectrumBars = MutableStateFlow(FloatArray(16))
    val spectrumBars: StateFlow<FloatArray> = _spectrumBars.asStateFlow()

    @Volatile
    var targetFrequencyHz: Float = 440f

    @Volatile
    var secondaryFrequencyHz: Float = 0f // > 0 enables dual-tone or binaural offset

    @Volatile
    var binauralBeatOffsetHz: Float = 0f // > 0 plays targetFrequencyHz in Left and +offset in Right

    @Volatile
    var waveform: SynthWaveform = SynthWaveform.SINE

    @Volatile
    var playMode: SynthPlayMode = SynthPlayMode.CONTINUOUS

    @Volatile
    var amplitude: Float = 0.5f // 0.0..1.0 digital amplitude, clamped so it never clips

    @Volatile
    var pulseOnMs: Int = 250

    @Volatile
    var pulseOffMs: Int = 250

    @Volatile
    var sweepStartHz: Float = 100f

    @Volatile
    var sweepEndHz: Float = 1000f

    @Volatile
    var sweepDurationSec: Float = 5f

    @Volatile
    var blockExternalAudioOutputs: Boolean = false

    @Volatile
    var stereoRoute: StereoRouteMode = StereoRouteMode.BOTH

    @Volatile
    var birdDeterrentMode: Boolean = false

    @Volatile
    var layeredDroneFrequencies: FloatArray? = null

    @Volatile
    var sleepMixerLevels: SleepMixerLevels? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                stopImmediate()
            }
        }
    }

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refreshExternalAudioDevices()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshExternalAudioDevices()
        }
    }

    init {
        refreshExternalAudioDevices()
        runCatching {
            audioManager?.registerAudioDeviceCallback(deviceCallback, mainHandler)
        }
    }

    fun refreshExternalAudioDevices(): Boolean {
        val outputs = runCatching {
            audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty()
        }.getOrDefault(emptyArray())

        val hasExternal = outputs.any { info ->
            when (info.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_ACCESSORY,
                AudioDeviceInfo.TYPE_HEARING_AID -> true
                else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    info.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        info.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                } else false
            }
        }
        _externalHeadsetConnected.value = hasExternal
        if (hasExternal && blockExternalAudioOutputs && _isPlaying.value) {
            _externalDeviceWarning.value =
                "Playback paused: disconnect headphones, Bluetooth, or USB audio before playing speaker or high-frequency tones."
            stopImmediate()
        } else if (!hasExternal) {
            _externalDeviceWarning.value = null
        }
        return hasExternal
    }

    fun clearExternalWarning() {
        _externalDeviceWarning.value = null
    }

    fun getMediaVolumeFraction(): Float {
        val am = audioManager ?: return 0.7f
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return (cur.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    }

    fun getRawMediaVolume(): Int =
        audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0

    fun setRawMediaVolume(vol: Int) {
        val am = audioManager ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        runCatching {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, vol.coerceIn(0, max), 0)
        }
    }

    fun setMediaVolumeToMax(): Int {
        val am = audioManager ?: return 0
        val previous = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        runCatching {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, max, 0)
        }
        return previous
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return true
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(
                        if (stereoRoute == StereoRouteMode.EARPIECE)
                            AudioAttributes.USAGE_VOICE_COMMUNICATION
                        else
                            AudioAttributes.USAGE_MEDIA
                    )
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener(focusChangeListener, mainHandler)
                    .build()
                focusRequest = req
                am.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    focusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        }.getOrDefault(true)
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(focusChangeListener)
            }
        }
        focusRequest = null
    }

    fun start(
        fadeInSeconds: Float = 1.5f,
        maxDurationSeconds: Float? = null,
        onAutoStopped: () -> Unit = {}
    ) {
        if (blockExternalAudioOutputs && refreshExternalAudioDevices()) {
            _externalDeviceWarning.value =
                "Headphones, Bluetooth, or USB audio detected. Disconnect them to use this speaker mode safely."
            return
        }
        if (!requestAudioFocus()) return

        playbackJob?.cancel()
        playbackJob = scope.launch {
            runSynthesisLoop(
                fadeInSeconds = fadeInSeconds,
                maxDurationSeconds = maxDurationSeconds,
                onAutoStopped = onAutoStopped
            )
        }
    }

    fun stopWithFadeOut() {
        stopImmediate()
    }

    fun stopImmediate() {
        playbackJob?.cancel()
        playbackJob = null
        if (_isPlaying.value) {
            _isPlaying.value = false
            mainHandler.post { onAudioPlayingStateChanged(false) }
        }
        abandonAudioFocus()
    }

    fun release() {
        stopImmediate()
        runCatching {
            audioManager?.unregisterAudioDeviceCallback(deviceCallback)
        }
    }

    /**
     * Plays a single percussive click for the Metronome (1200 Hz accented downbeat, 820 Hz normal beat).
     */
    fun playMetronomeClick(accented: Boolean) {
        scope.launch {
            val sampleRate = SAMPLE_RATE_HZ
            val durationSamples = (sampleRate * 0.032f).toInt() // 32 ms crisp click
            val freq = if (accented) 1240.0 else 840.0
            val buffer = ShortArray(durationSamples)
            var phase = 0.0
            val phaseInc = 2.0 * PI * freq / sampleRate
            for (i in 0 until durationSamples) {
                val env = (1.0 - i.toDouble() / durationSamples).pow(2.2)
                val sample = sin(phase) * env * 0.85
                buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                phase += phaseInc
            }
            runCatching {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(buffer, 0, buffer.size)
                track.play()
                delay(45L)
                track.stop()
                track.release()
            }
        }
    }

    private suspend fun runSynthesisLoop(
        fadeInSeconds: Float,
        maxDurationSeconds: Float?,
        onAutoStopped: () -> Unit
    ) {
        val sampleRate = SAMPLE_RATE_HZ
        val framesPerChunk = 1024 // ~21.3 ms per chunk at 48 kHz
        val stereoShortBuffer = ShortArray(framesPerChunk * 2)
        val oscPreview = FloatArray(128)

        val minBufBytes = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(framesPerChunk * 4)

        val useEarpiece = stereoRoute == StereoRouteMode.EARPIECE
        if (useEarpiece) {
            runCatching {
                audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
                @Suppress("DEPRECATION")
                audioManager?.isSpeakerphoneOn = false
            }
        }

        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(
                            if (useEarpiece) AudioAttributes.USAGE_VOICE_COMMUNICATION
                            else AudioAttributes.USAGE_MEDIA
                        )
                        .setContentType(
                            if (useEarpiece) AudioAttributes.CONTENT_TYPE_SPEECH
                            else AudioAttributes.CONTENT_TYPE_MUSIC
                        )
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(minBufBytes * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: return

        _isPlaying.value = true
        mainHandler.post { onAudioPlayingStateChanged(true) }

        var phasePrimary = 0.0
        var phaseSecondary = 0.0
        val dronePhases = DoubleArray(4)
        var elapsedSamples = 0L
        val fadeInTotalSamples = (fadeInSeconds.coerceAtLeast(0.05f) * sampleRate).toLong()
        val maxTotalSamples = maxDurationSeconds?.let { (it * sampleRate).toLong() }

        // Pink / Brown noise state variables
        var b0 = 0.0
        var b1 = 0.0
        var b2 = 0.0
        var brownLast = 0.0
        var windLpf = 0.0
        var rainLpf = 0.0

        try {
            track.play()
            while (scope.isActive && _isPlaying.value) {
                // Stop immediately if a phone call becomes active
                val mode = audioManager?.mode
                if (mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_RINGTONE) {
                    break
                }
                if (blockExternalAudioOutputs && _externalHeadsetConnected.value) {
                    _externalDeviceWarning.value =
                        "Playback paused: headphones or Bluetooth audio connected."
                    break
                }
                if (maxTotalSamples != null && elapsedSamples >= maxTotalSamples) {
                    withContext(Dispatchers.Main) { onAutoStopped() }
                    break
                }

                val currentMixer = sleepMixerLevels
                val currentDrones = layeredDroneFrequencies
                val baseAmp = amplitude.coerceIn(0f, 0.96f).toDouble()

                for (frame in 0 until framesPerChunk) {
                    val sampleIndex = elapsedSamples + frame
                    val timeSec = sampleIndex.toDouble() / sampleRate.toDouble()

                    // Fade-in & optional end-of-timer fade-out envelope
                    val fadeInEnv = if (fadeInTotalSamples > 0) {
                        (sampleIndex.toDouble() / fadeInTotalSamples.toDouble()).coerceIn(0.0, 1.0)
                    } else 1.0

                    val fadeOutEnv = if (maxTotalSamples != null && maxTotalSamples - sampleIndex < sampleRate * 2) {
                        ((maxTotalSamples - sampleIndex).toDouble() / (sampleRate * 2.0)).coerceIn(0.0, 1.0)
                    } else 1.0

                    // Pulse gate envelope
                    val pulseEnv = if (playMode == SynthPlayMode.PULSE) {
                        val cycleMs = (pulseOnMs + pulseOffMs).coerceAtLeast(40)
                        val posMs = ((timeSec * 1000.0).toLong() % cycleMs).toInt()
                        if (posMs < pulseOnMs) 1.0 else 0.0
                    } else 1.0

                    val env = baseAmp * fadeInEnv * fadeOutEnv * pulseEnv

                    var leftSample = 0.0
                    var rightSample = 0.0

                    if (currentMixer != null) {
                        val white = Random.nextDouble(-1.0, 1.0)
                        b0 = 0.99765 * b0 + white * 0.0990460
                        b1 = 0.96300 * b1 + white * 0.2965164
                        b2 = 0.57000 * b2 + white * 1.0526913
                        val pink = (b0 + b1 + b2 + white * 0.1848) * 0.12
                        brownLast = (brownLast + 0.02 * white).coerceIn(-1.0, 1.0)
                        val brown = brownLast * 0.7

                        // Rain-like pattering modulated noise & wind-like slow LFO filtered noise
                        rainLpf = 0.75 * rainLpf + 0.25 * white * (0.6 + 0.4 * sin(timeSec * 19.0))
                        val windSweep = 0.015 + 0.012 * sin(2.0 * PI * 0.14 * timeSec)
                        windLpf += windSweep * (white - windLpf)

                        val mix = (
                            white * currentMixer.white * 0.25 +
                                pink * currentMixer.pink * 0.55 +
                                brown * currentMixer.brown * 0.65 +
                                rainLpf * currentMixer.rain * 0.40 +
                                windLpf * currentMixer.wind * 0.80
                            ).coerceIn(-0.95, 0.95) * fadeInEnv * fadeOutEnv * baseAmp
                        leftSample = mix
                        rightSample = mix
                    } else if (currentDrones != null && currentDrones.isNotEmpty()) {
                        // Layered calm drones with slow breathing envelope
                        val breathing = 0.72 + 0.28 * sin(2.0 * PI * 0.12 * timeSec)
                        var sum = 0.0
                        for (dIdx in currentDrones.indices) {
                            val dFreq = currentDrones[dIdx].toDouble()
                            dronePhases[dIdx] = (dronePhases[dIdx] + 2.0 * PI * dFreq / sampleRate) % (2.0 * PI)
                            sum += sin(dronePhases[dIdx])
                        }
                        val mono = (sum / currentDrones.size.coerceAtLeast(1)) * env * breathing
                        leftSample = mono
                        rightSample = mono
                    } else {
                        val instantaneousHz = when {
                            birdDeterrentMode -> {
                                // Varied 2–8 kHz bursts with pseudo-random intervals
                                val slot = (timeSec * 6.5).toInt()
                                val activeBurst = (slot % 3 != 0)
                                if (activeBurst) {
                                    2200.0 + ((slot * 1373) % 5600).toDouble()
                                } else 0.0
                            }
                            playMode == SynthPlayMode.SWEEP || playMode == SynthPlayMode.LOOP -> {
                                val dur = sweepDurationSec.coerceAtLeast(0.5f).toDouble()
                                val frac = (timeSec % dur) / dur
                                sweepStartHz.toDouble() + (sweepEndHz - sweepStartHz).toDouble() * frac
                            }
                            else -> targetFrequencyHz.toDouble()
                        }

                        if (frame == 0) {
                            _liveCurrentHz.value = instantaneousHz.toFloat()
                        }

                        if (instantaneousHz <= 1.0) {
                            leftSample = 0.0
                            rightSample = 0.0
                        } else {
                            phasePrimary = (phasePrimary + 2.0 * PI * instantaneousHz / sampleRate) % (2.0 * PI)
                            val primaryWave = sampleWaveform(waveform, phasePrimary)

                            if (binauralBeatOffsetHz > 0.1f) {
                                val rightHz = instantaneousHz + binauralBeatOffsetHz
                                phaseSecondary = (phaseSecondary + 2.0 * PI * rightHz / sampleRate) % (2.0 * PI)
                                leftSample = primaryWave * env
                                rightSample = sin(phaseSecondary) * env
                            } else if (secondaryFrequencyHz > 10f) {
                                phaseSecondary = (phaseSecondary + 2.0 * PI * secondaryFrequencyHz / sampleRate) % (2.0 * PI)
                                val combined = 0.5 * (primaryWave + sampleWaveform(waveform, phaseSecondary)) * env
                                leftSample = combined
                                rightSample = combined
                            } else {
                                val mono = primaryWave * env
                                leftSample = mono
                                rightSample = mono
                            }
                        }
                    }

                    val routedLeft = if (stereoRoute == StereoRouteMode.RIGHT_ONLY) 0.0 else leftSample
                    val routedRight = if (stereoRoute == StereoRouteMode.LEFT_ONLY) 0.0 else rightSample

                    val sL = (routedLeft * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    val sR = (routedRight * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                    stereoShortBuffer[frame * 2] = sL
                    stereoShortBuffer[frame * 2 + 1] = sR

                    if (frame < oscPreview.size) {
                        oscPreview[frame] = ((routedLeft + routedRight) * 0.5).toFloat()
                    }
                }

                elapsedSamples += framesPerChunk
                _oscilloscopeSamples.value = oscPreview.copyOf()
                updateSpectrumPreview( _liveCurrentHz.value, amplitude)

                val written = track.write(stereoShortBuffer, 0, stereoShortBuffer.size)
                if (written < 0) break
            }

            // Short 30 ms anti-pop fade-out before stopping track
            val tailFrames = 512
            val tailBuf = ShortArray(tailFrames * 2)
            for (i in 0 until tailFrames) {
                val scale = 1.0f - (i.toFloat() / tailFrames)
                val lastL = stereoShortBuffer.getOrElse(0) { 0 }
                val lastR = stereoShortBuffer.getOrElse(1) { 0 }
                tailBuf[i * 2] = (lastL * scale).toInt().toShort()
                tailBuf[i * 2 + 1] = (lastR * scale).toInt().toShort()
            }
            runCatching { track.write(tailBuf, 0, tailBuf.size) }
        } finally {
            runCatching {
                track.stop()
                track.release()
            }
            if (useEarpiece) {
                runCatching { audioManager?.mode = AudioManager.MODE_NORMAL }
            }
            _oscilloscopeSamples.value = FloatArray(128)
            _spectrumBars.value = FloatArray(16)
            _isPlaying.value = false
            mainHandler.post { onAudioPlayingStateChanged(false) }
            abandonAudioFocus()
        }
    }

    private fun sampleWaveform(wf: SynthWaveform, phase: Double): Double {
        return when (wf) {
            SynthWaveform.SINE -> sin(phase)
            SynthWaveform.SQUARE -> if (sin(phase) >= 0.0) 0.85 else -0.85
            SynthWaveform.SAW -> ((phase / PI) - 1.0) * 0.85
            SynthWaveform.TRIANGLE -> (2.0 / PI) * kotlin.math.asin(sin(phase)) * 0.92
            SynthWaveform.NOISE -> Random.nextDouble(-0.85, 0.85)
        }
    }

    private fun updateSpectrumPreview(freqHz: Float, amp: Float) {
        val bars = FloatArray(16)
        val logMin = ln(20.0)
        val logMax = ln(20000.0)
        val normPos = ((ln(freqHz.coerceIn(20f, 20000f).toDouble()) - logMin) / (logMax - logMin))
            .coerceIn(0.0, 1.0)
        val centerBin = (normPos * 15.0).roundToInt().coerceIn(0, 15)
        for (i in 0 until 16) {
            val dist = abs(i - centerBin)
            bars[i] = when {
                waveform == SynthWaveform.NOISE -> (amp * (0.45f + Random.nextFloat() * 0.45f)).coerceIn(0.05f, 1f)
                dist == 0 -> amp.coerceIn(0.15f, 1f)
                dist == 1 -> (amp * 0.55f).coerceIn(0.06f, 0.85f)
                dist == 2 && waveform != SynthWaveform.SINE -> (amp * 0.28f).coerceIn(0.04f, 0.5f)
                else -> 0.04f
            }
        }
        _spectrumBars.value = bars
    }

    companion object {
        const val SAMPLE_RATE_HZ = 48_000

        /**
         * Uses the Goertzel algorithm on a live microphone buffer to verify whether the speaker
         * is acoustically producing [targetHz].
         * Returns Pair(detectedMagnitudeDb, humanReadableVerdict).
         */
        @SuppressLint("MissingPermission")
        suspend fun measureTargetFrequencyWithGoertzel(
            targetHz: Float
        ): Pair<Float, String> = withContext(Dispatchers.IO) {
            val sampleRate = SAMPLE_RATE_HZ
            val numSamples = 4096
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(numSamples * 2)

            val recorder = runCatching {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf
                )
            }.getOrNull() ?: return@withContext (-90f to "Microphone unavailable on this device.")

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                runCatching { recorder.release() }
                return@withContext (-90f to "Microphone could not be initialized.")
            }

            val buffer = ShortArray(numSamples)
            try {
                recorder.startRecording()
                delay(120L)
                val read = recorder.read(buffer, 0, numSamples)
                if (read <= 0) {
                    return@withContext (-90f to "No acoustic signal captured from microphone.")
                }

                // Goertzel filter at targetHz
                val k = (0.5 + (read * targetHz) / sampleRate).toInt()
                val omega = (2.0 * PI * k) / read
                val coeff = 2.0 * cos(omega)
                var q0: Double
                var q1 = 0.0
                var q2 = 0.0
                for (i in 0 until read) {
                    val window = 0.54 - 0.46 * cos(2.0 * PI * i / (read - 1).coerceAtLeast(1))
                    val sample = (buffer[i].toDouble() / Short.MAX_VALUE) * window
                    q0 = coeff * q1 - q2 + sample
                    q2 = q1
                    q1 = q0
                }
                val magnitudeSquared = q1 * q1 + q2 * q2 - q1 * q2 * coeff
                val mag = sqrt(magnitudeSquared.coerceAtLeast(1e-12)) / (read * 0.5)
                val db = (20.0 * log10(mag.coerceAtLeast(1e-6))).toFloat()

                val verdict = when {
                    db > -34f ->
                        "Strong output detected at ${targetHz.roundToInt()} Hz (${String.format("%.1f", db)} dBFS)."
                    db > -52f ->
                        "Moderate output detected at ${targetHz.roundToInt()} Hz (${String.format("%.1f", db)} dBFS)."
                    targetHz >= 15000f ->
                        "Signal is weak (${String.format("%.1f", db)} dBFS). Phone speakers roll off sharply above 15–18 kHz."
                    else ->
                        "Weak output (${String.format("%.1f", db)} dBFS). Try raising media volume or unblocking the speaker grille."
                }
                db to verdict
            } finally {
                runCatching {
                    recorder.stop()
                    recorder.release()
                }
            }
        }

        /**
         * Autocorrelation pitch detector for the Tuner instrument.
         */
        fun detectPitchAutocorrelation(samples: ShortArray, count: Int, sampleRate: Int): PitchDetectionResult {
            if (count < 512) return PitchDetectionResult()
            var rms = 0.0
            for (i in 0 until count) {
                val v = samples[i].toDouble() / Short.MAX_VALUE
                rms += v * v
            }
            rms = sqrt(rms / count)
            if (rms < 0.012) return PitchDetectionResult()

            val minLag = (sampleRate / 1100.0).toInt().coerceAtLeast(16) // ~1100 Hz max
            val maxLag = (sampleRate / 55.0).toInt().coerceAtMost(count / 2) // ~55 Hz min

            var bestLag = -1
            var bestCorr = 0.0
            for (lag in minLag..maxLag) {
                var sum = 0.0
                for (i in 0 until count - lag) {
                    sum += (samples[i].toDouble() / Short.MAX_VALUE) *
                        (samples[i + lag].toDouble() / Short.MAX_VALUE)
                }
                val normCorr = sum / (count - lag)
                if (normCorr > bestCorr) {
                    bestCorr = normCorr
                    bestLag = lag
                }
            }
            if (bestLag <= 0) return PitchDetectionResult()
            val freq = sampleRate.toFloat() / bestLag.toFloat()
            if (freq !in 50f..1200f) return PitchDetectionResult()

            val midiFloat = 69.0 + 12.0 * (ln(freq / 440.0) / ln(2.0))
            val midiRounded = midiFloat.roundToInt()
            val cents = ((midiFloat - midiRounded) * 100.0).toFloat().coerceIn(-50f, 50f)
            val targetHz = (440.0 * 2.0.pow((midiRounded - 69) / 12.0)).toFloat()
            val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
            val noteIdx = ((midiRounded % 12) + 12) % 12
            val octave = (midiRounded / 12) - 1
            return PitchDetectionResult(
                frequencyHz = freq,
                noteName = "${noteNames[noteIdx]}$octave",
                centsOff = cents,
                targetHz = targetHz,
                isLocked = abs(cents) <= 5f
            )
        }
    }
}
