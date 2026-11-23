package com.example.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

data class SensorState(
    val hasAccelerometer: Boolean = true,
    val hasMagnetometer: Boolean = true,
    val hasLightSensor: Boolean = true,
    val hasBarometer: Boolean = true,
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val tiltAngleDeg: Float = 0f,
    val azimuthDeg: Float = 0f,
    val compassAccuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val magneticMagnitudeUt: Float = 45f,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f,
    val ambientLux: Float = 240f,
    val pressureHpa: Float = 1013.25f,
    val ambientTempCelsius: Float = 21.5f
) {
    val isLowCompassAccuracy: Boolean
        get() = compassAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
            compassAccuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW

    val cardinalDirection: String
        get() {
            val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
            val index = (((azimuthDeg + 22.5f) % 360f) / 45f).toInt().coerceIn(0, 7)
            return dirs[index]
        }

    fun computeBarometricAltitudeMeters(seaLevelHpa: Float = 1013.25f): Float {
        if (pressureHpa <= 0f || seaLevelHpa <= 0f) return 0f
        return (44330.0 * (1.0 - (pressureHpa / seaLevelHpa).toDouble().pow(1.0 / 5.255))).toFloat()
    }
}

class SensorRepository(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val lightSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val pressureSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE)
    private val tempSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)

    val hasAccelerometer: Boolean = accelerometer != null
    val hasMagnetometer: Boolean = magnetometer != null
    val hasLightSensor: Boolean = lightSensor != null
    val hasBarometer: Boolean = pressureSensor != null

    val sensorFlow: Flow<SensorState> = callbackFlow {
        val sm = sensorManager
        if (sm == null) {
            trySend(
                SensorState(
                    hasAccelerometer = false,
                    hasMagnetometer = false,
                    hasLightSensor = false,
                    hasBarometer = false
                )
            )
            awaitClose { }
            return@callbackFlow
        }

        val alpha = 0.14f
        val accelFiltered = FloatArray(3) { 0f }
        val magFiltered = FloatArray(3) { 0f }
        var accelInitialized = false
        var magInitialized = false
        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)

        var currentPitch = 0f
        var currentRoll = 0f
        var currentTilt = 0f
        var currentAzimuth = 0f
        var currentMagUt = 45f
        var currentLux = 240f
        var currentPressure = 1013.25f
        var currentTemp = 21.5f
        var currentAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH

        fun emitCurrent() {
            trySend(
                SensorState(
                    hasAccelerometer = hasAccelerometer,
                    hasMagnetometer = hasMagnetometer,
                    hasLightSensor = hasLightSensor,
                    hasBarometer = hasBarometer,
                    pitchDeg = currentPitch,
                    rollDeg = currentRoll,
                    tiltAngleDeg = currentTilt,
                    azimuthDeg = currentAzimuth,
                    compassAccuracy = currentAccuracy,
                    magneticMagnitudeUt = currentMagUt,
                    magX = magFiltered[0],
                    magY = magFiltered[1],
                    magZ = magFiltered[2],
                    ambientLux = currentLux,
                    pressureHpa = currentPressure,
                    ambientTempCelsius = currentTemp
                )
            )
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        if (!accelInitialized) {
                            accelFiltered[0] = event.values[0]
                            accelFiltered[1] = event.values[1]
                            accelFiltered[2] = event.values[2]
                            accelInitialized = true
                        } else {
                            accelFiltered[0] += alpha * (event.values[0] - accelFiltered[0])
                            accelFiltered[1] += alpha * (event.values[1] - accelFiltered[1])
                            accelFiltered[2] += alpha * (event.values[2] - accelFiltered[2])
                        }

                        val ax = accelFiltered[0]
                        val ay = accelFiltered[1]
                        val az = accelFiltered[2]
                        val norm = sqrt(ax * ax + ay * ay + az * az).coerceAtLeast(0.001f)

                        currentRoll = Math.toDegrees(atan2(ax.toDouble(), sqrt((ay * ay + az * az).toDouble()))).toFloat()
                        currentPitch = Math.toDegrees(atan2(ay.toDouble(), sqrt((ax * ax + az * az).toDouble()))).toFloat()

                        val edgeAngle = Math.toDegrees(atan2(ay.toDouble(), ax.toDouble())).toFloat()
                        val flatTilt = Math.toDegrees(acos((az / norm).coerceIn(-1f, 1f).toDouble())).toFloat()
                        currentTilt = if (kotlin.math.abs(az) < 6.0f) {
                            val raw = kotlin.math.abs(edgeAngle)
                            if (raw > 90f) kotlin.math.abs(180f - raw) else raw
                        } else {
                            flatTilt
                        }
                    }

                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        if (!magInitialized) {
                            magFiltered[0] = event.values[0]
                            magFiltered[1] = event.values[1]
                            magFiltered[2] = event.values[2]
                            magInitialized = true
                        } else {
                            magFiltered[0] += alpha * (event.values[0] - magFiltered[0])
                            magFiltered[1] += alpha * (event.values[1] - magFiltered[1])
                            magFiltered[2] += alpha * (event.values[2] - magFiltered[2])
                        }
                        val mx = magFiltered[0]
                        val my = magFiltered[1]
                        val mz = magFiltered[2]
                        currentMagUt = sqrt(mx * mx + my * my + mz * mz)
                        if (event.accuracy != SensorManager.SENSOR_STATUS_NO_CONTACT) {
                            currentAccuracy = event.accuracy
                        }
                    }

                    Sensor.TYPE_LIGHT -> {
                        val rawLux = event.values.firstOrNull() ?: 0f
                        currentLux += alpha * (rawLux - currentLux)
                    }

                    Sensor.TYPE_PRESSURE -> {
                        val rawP = event.values.firstOrNull() ?: 1013.25f
                        currentPressure += alpha * (rawP - currentPressure)
                    }

                    Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                        val rawT = event.values.firstOrNull() ?: 21.5f
                        currentTemp += alpha * (rawT - currentTemp)
                    }
                }

                if (accelInitialized && magInitialized) {
                    if (SensorManager.getRotationMatrix(rotationMatrix, null, accelFiltered, magFiltered)) {
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        val azimuthRad = orientationAngles[0]
                        val deg = ((Math.toDegrees(azimuthRad.toDouble()).toFloat() + 360f) % 360f)
                        val diff = ((deg - currentAzimuth + 540f) % 360f) - 180f
                        currentAzimuth = (currentAzimuth + alpha * diff + 360f) % 360f
                    }
                }
                emitCurrent()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    currentAccuracy = accuracy
                    emitCurrent()
                }
            }
        }

        emitCurrent()

        runCatching {
            accelerometer?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
            magnetometer?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
            lightSensor?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            pressureSensor?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            tempSensor?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        }

        awaitClose {
            runCatching { sm.unregisterListener(listener) }
        }
    }
}
