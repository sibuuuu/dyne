package com.example.data

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioManager
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import androidx.biometric.BiometricManager

enum class HardwareCapability(
    val displayName: String,
    val missingReason: String,
    val fallbackDescription: String
) {
    ACCELEROMETER(
        "Accelerometer",
        "Requires a 3-axis accelerometer sensor",
        "Fallback: Manual numeric tilt/motion slider input"
    ),
    GYROSCOPE(
        "Gyroscope",
        "Requires a gyroscope sensor",
        "Fallback: Accelerometer + magnetometer orientation fusion"
    ),
    MAGNETOMETER(
        "Magnetometer / Compass",
        "Requires a magnetic field sensor",
        "Fallback: Manual heading & reference GPS coordinate bearing"
    ),
    ROTATION_VECTOR(
        "Rotation Vector",
        "Requires a rotation vector or orientation sensor",
        "Fallback: 3-axis accelerometer pitch & roll calculation"
    ),
    LIGHT(
        "Light Sensor (Lux)",
        "Requires an ambient light sensor",
        "Fallback: Manual scene illuminance selector & EV reference table"
    ),
    PROXIMITY(
        "Proximity Sensor",
        "Requires a proximity sensor",
        "Fallback: Touch-to-count manual trigger"
    ),
    BAROMETER(
        "Barometer",
        "Requires a barometric pressure sensor",
        "Fallback: GPS elevation & manual QNH sea-level pressure entry"
    ),
    STEP_COUNTER(
        "Step Counter / Detector",
        "Requires a hardware step counter or 3-axis accelerometer",
        "Fallback: 3-axis accelerometer peak-step detection"
    ),
    FLASH(
        "Rear LED Flash",
        "Requires a camera flash LED",
        "Fallback: Full-screen Soft Screen Light with brightness & warmth control"
    ),
    FLASH_STRENGTH(
        "Flash Strength Control",
        "Requires Android 13+ variable torch strength hardware",
        "Fallback: Standard LED On/Off + Soft Screen Light (dimmable)"
    ),
    CAMERA(
        "Camera",
        "Requires a rear or front camera",
        "Fallback: Pick image from Android Photo Picker gallery"
    ),
    MICROPHONE(
        "Microphone",
        "Requires a built-in or connected microphone",
        "Fallback: Reference acoustic dB chart & manual tone generator"
    ),
    GPS(
        "GPS Location",
        "Requires GPS location hardware",
        "Fallback: Manual latitude/longitude & saved spot coordinates"
    ),
    VIBRATOR(
        "Vibrator / Haptics",
        "Requires a vibration motor",
        "Fallback: Visual pulse flash & audio click feedback"
    ),
    VIBRATOR_AMPLITUDE(
        "Vibration Amplitude Control",
        "Requires variable-amplitude haptics",
        "Fallback: Pulse-width duty cycle haptic patterns"
    ),
    BIOMETRIC(
        "Biometric Hardware",
        "Requires fingerprint or biometric authentication hardware",
        "Fallback: Local PIN / passphrase lock"
    ),
    AUDIO_OUTPUT(
        "Audio Output / Speaker",
        "Requires speaker or audio output hardware",
        "Fallback: Visual waveform & haptic beat indicator"
    ),
    MULTITOUCH(
        "Multi-Touch Screen",
        "Requires a multi-touch display",
        "Fallback: Single-touch grid inspector"
    ),
    MLKIT_SEGMENTATION(
        "On-Device AI Segmentation",
        "Requires on-device subject segmentation support",
        "Fallback: Portrait cutout & interactive Refine Brush"
    ),
    MEDIA3_TRANSFORMER(
        "Media3 Video Transformer",
        "Requires Media3 Transformer & hardware H.264 video encoder",
        "Fallback: Lossless Video Trim, Extract Audio & Mute via MediaExtractor/MediaMuxer"
    )
}

data class DeviceCapabilitiesSnapshot(
    val hasAccelerometer: Boolean = true,
    val hasGyroscope: Boolean = true,
    val hasMagnetometer: Boolean = true,
    val hasRotationVector: Boolean = true,
    val hasLight: Boolean = true,
    val hasProximity: Boolean = true,
    val hasBarometer: Boolean = true,
    val hasStepCounter: Boolean = true,
    val hasHardwareStepSensor: Boolean = true,
    val hasFlash: Boolean = true,
    val hasFlashStrengthControl: Boolean = false,
    val maxTorchStrengthLevel: Int = 1,
    val hasCamera: Boolean = true,
    val hasMicrophone: Boolean = true,
    val hasGps: Boolean = true,
    val hasVibrator: Boolean = true,
    val hasVibratorAmplitudeControl: Boolean = false,
    val hasBiometric: Boolean = false,
    val hasAudioOutput: Boolean = true,
    val hasMultitouch: Boolean = true,
    val hasSubjectSegmentation: Boolean = true,
    val hasSelfieSegmentation: Boolean = true,
    val hasMedia3Transformer: Boolean = true,
    val hasAacEncoder: Boolean = true,
    val hasFlacEncoder: Boolean = false,
    val hasOpusEncoder: Boolean = false
) {
    fun supports(capability: HardwareCapability): Boolean = when (capability) {
        HardwareCapability.ACCELEROMETER -> hasAccelerometer
        HardwareCapability.GYROSCOPE -> hasGyroscope
        HardwareCapability.MAGNETOMETER -> hasMagnetometer
        HardwareCapability.ROTATION_VECTOR -> hasRotationVector
        HardwareCapability.LIGHT -> hasLight
        HardwareCapability.PROXIMITY -> hasProximity
        HardwareCapability.BAROMETER -> hasBarometer
        HardwareCapability.STEP_COUNTER -> hasStepCounter || hasAccelerometer
        HardwareCapability.FLASH -> hasFlash
        HardwareCapability.FLASH_STRENGTH -> hasFlashStrengthControl
        HardwareCapability.CAMERA -> hasCamera
        HardwareCapability.MICROPHONE -> hasMicrophone
        HardwareCapability.GPS -> hasGps
        HardwareCapability.VIBRATOR -> hasVibrator
        HardwareCapability.VIBRATOR_AMPLITUDE -> hasVibratorAmplitudeControl
        HardwareCapability.BIOMETRIC -> hasBiometric
        HardwareCapability.AUDIO_OUTPUT -> hasAudioOutput
        HardwareCapability.MULTITOUCH -> hasMultitouch
        HardwareCapability.MLKIT_SEGMENTATION -> hasSubjectSegmentation || hasSelfieSegmentation
        HardwareCapability.MEDIA3_TRANSFORMER -> hasMedia3Transformer
    }

    fun supportsAll(required: Set<HardwareCapability>): Boolean =
        required.all { supports(it) }

    fun firstMissingReason(required: Set<HardwareCapability>): String? =
        required.firstOrNull { !supports(it) }?.missingReason

    fun firstFallbackReason(required: Set<HardwareCapability>): String? =
        required.firstOrNull { !supports(it) }?.fallbackDescription
            ?: required.firstOrNull()?.fallbackDescription
}

object DeviceCapabilities {
    @Volatile
    private var cachedSnapshot: DeviceCapabilitiesSnapshot? = null

    val current: DeviceCapabilitiesSnapshot
        get() = cachedSnapshot ?: DeviceCapabilitiesSnapshot()

    fun initialize(context: Context): DeviceCapabilitiesSnapshot {
        cachedSnapshot?.let { return it }
        val appContext = context.applicationContext
        val pm = appContext.packageManager
        val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        val hasAccel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        val hasGyro = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
        val hasMag = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
        val hasRot = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
            sensorManager?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) != null ||
            (hasAccel && hasMag)
        val hasLight = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT) != null
        val hasProx = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null
        val hasBaro = sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE) != null
        val hasSteps = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null ||
            sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null

        var flashAvailable = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        var maxTorchLevel = 1
        var flashStrengthSupported = false
        runCatching {
            cameraManager?.cameraIdList?.forEach { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasUnit = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                if (hasUnit) {
                    flashAvailable = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val maxLevel = chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                        if (maxLevel > 1) {
                            flashStrengthSupported = true
                            maxTorchLevel = maxOf(maxTorchLevel, maxLevel)
                        }
                    }
                }
            }
        }

        val hasCam = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) ||
            runCatching { (cameraManager?.cameraIdList?.isNotEmpty() == true) }.getOrDefault(true)
        val hasMic = pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
        val hasGps = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS) ||
            runCatching { locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) != null }.getOrDefault(true)

        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        val hasVib = vibrator?.hasVibrator() == true
        val hasVibAmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.hasAmplitudeControl() == true
        } else {
            false
        }

        val hasBio = runCatching {
            val bm = BiometricManager.from(appContext)
            val res = bm.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            res == BiometricManager.BIOMETRIC_SUCCESS ||
                res == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ||
                pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        }.getOrDefault(false)

        val hasAudioOut = pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT) ||
            runCatching {
                audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.isNotEmpty() == true
            }.getOrDefault(true)

        val hasMultiTouch = pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH) ||
            pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)

        val hasSubjectSeg = runCatching {
            Class.forName("com.google.mlkit.vision.segmentation.subject.SubjectSegmentation")
            true
        }.getOrDefault(false)

        val hasSelfieSeg = runCatching {
            Class.forName("com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions")
            true
        }.getOrElse {
            runCatching {
                Class.forName("com.google.mlkit.vision.common.InputImage")
                true
            }.getOrDefault(true)
        }

        val hasMedia3 = runCatching {
            Class.forName("androidx.media3.transformer.Transformer")
            true
        }.getOrDefault(false)

        var aacEnc = true
        var flacEnc = false
        var opusEnc = false
        runCatching {
            val codecList = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS)
            val infos = codecList.codecInfos
            var foundAac = false
            for (info in infos) {
                if (!info.isEncoder) continue
                for (type in info.supportedTypes) {
                    when {
                        type.equals("audio/mp4a-latm", ignoreCase = true) -> foundAac = true
                        type.equals("audio/flac", ignoreCase = true) -> flacEnc = true
                        type.equals("audio/opus", ignoreCase = true) -> opusEnc = true
                    }
                }
            }
            if (infos.isNotEmpty()) {
                aacEnc = foundAac || true
            }
        }

        return DeviceCapabilitiesSnapshot(
            hasAccelerometer = hasAccel,
            hasGyroscope = hasGyro,
            hasMagnetometer = hasMag,
            hasRotationVector = hasRot,
            hasLight = hasLight,
            hasProximity = hasProx,
            hasBarometer = hasBaro,
            hasStepCounter = hasSteps || hasAccel,
            hasHardwareStepSensor = hasSteps,
            hasFlash = flashAvailable,
            hasFlashStrengthControl = flashStrengthSupported,
            maxTorchStrengthLevel = maxTorchLevel,
            hasCamera = hasCam,
            hasMicrophone = hasMic,
            hasGps = hasGps,
            hasVibrator = hasVib,
            hasVibratorAmplitudeControl = hasVibAmp,
            hasBiometric = hasBio,
            hasAudioOutput = hasAudioOut,
            hasMultitouch = hasMultiTouch,
            hasSubjectSegmentation = hasSubjectSeg,
            hasSelfieSegmentation = hasSelfieSeg,
            hasMedia3Transformer = hasMedia3,
            hasAacEncoder = aacEnc,
            hasFlacEncoder = flacEnc,
            hasOpusEncoder = opusEnc
        ).also {
            cachedSnapshot = it
        }
    }

    fun isToolSupported(
        requiredCapabilities: Set<HardwareCapability>,
        context: Context? = null
    ): Boolean {
        val snap = if (context != null) initialize(context) else current
        return snap.supportsAll(requiredCapabilities)
    }

    fun getMissingReason(
        requiredCapabilities: Set<HardwareCapability>,
        context: Context? = null
    ): String? {
        val snap = if (context != null) initialize(context) else current
        return snap.firstMissingReason(requiredCapabilities)
    }

    fun getFallbackReason(
        requiredCapabilities: Set<HardwareCapability>,
        context: Context? = null
    ): String? {
        val snap = if (context != null) initialize(context) else current
        return snap.firstFallbackReason(requiredCapabilities)
    }

    fun hasSubjectSegmentationClass(): Boolean = current.hasSubjectSegmentation

    fun hasSelfieSegmentationClass(): Boolean = current.hasSelfieSegmentation

    fun hasMedia3TransformerClass(): Boolean = current.hasMedia3Transformer
}

