package com.example.ui.tools

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DeviceCapabilities
import com.example.data.HardwareCapability
import com.example.ui.components.GlassButton
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.StatusWarning
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.sqrt

private data class LiveSensorItem(
    val type: Int,
    val label: String,
    val unit: String,
    val sensor: Sensor?
)

@Composable
fun DeviceSensorsInstrumentScreen() {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val capabilities = remember(context) { DeviceCapabilities.initialize(context) }
    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }

    val monitoredSensors = remember(sensorManager) {
        listOf(
            LiveSensorItem(Sensor.TYPE_ACCELEROMETER, "Accelerometer", "m/s²", sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)),
            LiveSensorItem(Sensor.TYPE_GYROSCOPE, "Gyroscope", "rad/s", sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)),
            LiveSensorItem(Sensor.TYPE_MAGNETIC_FIELD, "Magnetometer", "µT", sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)),
            LiveSensorItem(Sensor.TYPE_ROTATION_VECTOR, "Rotation Vector", "vec", sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)),
            LiveSensorItem(Sensor.TYPE_LIGHT, "Ambient Light", "lx", sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)),
            LiveSensorItem(Sensor.TYPE_PROXIMITY, "Proximity", "cm", sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)),
            LiveSensorItem(Sensor.TYPE_PRESSURE, "Barometer", "hPa", sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE)),
            LiveSensorItem(Sensor.TYPE_STEP_COUNTER, "Step Counter", "steps", sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR))
        )
    }

    val allHardwareSensors = remember(sensorManager) {
        sensorManager?.getSensorList(Sensor.TYPE_ALL).orEmpty()
    }

    val liveValues = remember { mutableStateMapOf<Int, String>() }
    val liveHistories = remember { mutableStateMapOf<Int, List<Float>>() }

    DisposableEffect(sensorManager) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val ev = event ?: return
                val sType = ev.sensor.type
                val v0 = ev.values.getOrNull(0) ?: 0f
                val v1 = ev.values.getOrNull(1) ?: 0f
                val v2 = ev.values.getOrNull(2) ?: 0f

                val (formatted, scalar) = when (sType) {
                    Sensor.TYPE_ACCELEROMETER,
                    Sensor.TYPE_GYROSCOPE,
                    Sensor.TYPE_MAGNETIC_FIELD,
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        val mag = sqrt(v0 * v0 + v1 * v1 + v2 * v2)
                        String.format(Locale.US, "X:%.1f Y:%.1f Z:%.1f (|%.1f|)", v0, v1, v2, mag) to mag
                    }
                    else -> {
                        String.format(Locale.US, "%.2f", v0) to v0
                    }
                }
                liveValues[sType] = formatted
                val prev = liveHistories[sType].orEmpty()
                liveHistories[sType] = (prev + scalar).takeLast(36)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        monitoredSensors.forEach { item ->
            item.sensor?.let { s ->
                runCatching {
                    sensorManager?.registerListener(listener, s, SensorManager.SENSOR_DELAY_UI)
                }
            }
        }

        onDispose {
            runCatching { sensorManager?.unregisterListener(listener) }
        }
    }

    // System specs: Battery, Storage, RAM, Android version, Screen specs
    val systemSummary = remember(context) { readDeviceSystemSpecs(context) }

    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Share as Text CTA at top
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "HARDWARE & TELEMETRY",
                    style = type.section,
                    color = theme.accent
                )
                Text(
                    text = "${allHardwareSensors.size} hardware sensors detected",
                    style = type.caption,
                    color = theme.textSecondary
                )
            }
            GlassButton(
                text = "Share as text",
                icon = Icons.Default.Share,
                accentFill = theme.accent,
                onClick = {
                    val report = buildShareableDeviceReport(
                        systemSummary = systemSummary,
                        monitored = monitoredSensors,
                        liveValues = liveValues,
                        allSensors = allHardwareSensors
                    )
                    runCatching {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            this.type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Dyne Device & Sensors Report")
                            putExtra(Intent.EXTRA_TEXT, report)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Device & Sensors"))
                    }
                },
                testTag = "device_sensors_share_button"
            )
        }

        // System Specs Cards (Battery, Storage, RAM, Android, Display)
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
                text = "PHONE SPECIFICATIONS",
                style = type.section,
                color = theme.accent
            )
            SpecRowItem(
                icon = Icons.Default.PhoneAndroid,
                title = "Android & Device",
                value = "${systemSummary.deviceModel} • Android ${systemSummary.androidRelease} (API ${systemSummary.sdkInt})"
            )
            SpecRowItem(
                icon = Icons.Default.Memory,
                title = "RAM Memory",
                value = "${systemSummary.ramAvailableGb} free of ${systemSummary.ramTotalGb}"
            )
            SpecRowItem(
                icon = Icons.Default.Storage,
                title = "Internal Storage",
                value = "${systemSummary.storageFreeGb} free of ${systemSummary.storageTotalGb}"
            )
            SpecRowItem(
                icon = Icons.Default.BatteryChargingFull,
                title = "Battery",
                value = "${systemSummary.batteryLevelPct}% • ${systemSummary.batteryTempC}°C • ${systemSummary.batteryVoltageMv} mV"
            )
            SpecRowItem(
                icon = Icons.Default.Sensors,
                title = "Screen Specs",
                value = "${systemSummary.screenResolution} • ${systemSummary.screenDpi} dpi • ${systemSummary.refreshRateHz} Hz"
            )
        }

        // Capabilities checklist
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(theme.elevated)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "CAPABILITY MATRIX",
                style = type.section,
                color = theme.accent
            )
            HardwareCapability.entries.chunked(2).forEach { pair ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    pair.forEach { cap ->
                        val supported = capabilities.supports(cap)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(theme.surface)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (supported) theme.accent else StatusWarning)
                            )
                            Text(
                                text = cap.displayName,
                                style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = if (supported) theme.textPrimary else theme.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Live Monitored Sensors with Name, Range, Live Value, and Mini Graph
        Text(
            text = "LIVE SENSORS & MINI GRAPHS",
            style = type.section,
            color = theme.accent
        )

        monitoredSensors.forEach { item ->
            val hw = item.sensor
            val currentVal = liveValues[hw?.type ?: item.type]
                ?: if (hw != null) "Waiting for sample..." else "Not present on this phone"
            val history = liveHistories[hw?.type ?: item.type].orEmpty()

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.elevated)
                    .border(
                        1.dp,
                        if (hw != null) theme.accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.label,
                            style = type.body.copy(fontWeight = FontWeight.ExtraBold),
                            color = theme.textPrimary
                        )
                        Text(
                            text = if (hw != null) {
                                "${hw.name} • Max range: ${String.format(Locale.US, "%.1f", hw.maximumRange)} ${item.unit}"
                            } else {
                                "Hardware sensor unavailable"
                            },
                            style = type.caption,
                            color = theme.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (hw != null) theme.accent else theme.textSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (hw != null) "$currentVal ${item.unit}" else currentVal,
                        style = type.gaugeSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (hw != null) theme.accent else theme.textSecondary,
                        modifier = Modifier.weight(1f)
                    )

                    // Mini live sparkline graph
                    Canvas(
                        modifier = Modifier
                            .size(width = 110.dp, height = 34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.surface)
                            .padding(4.dp)
                    ) {
                        if (history.size >= 2) {
                            val minV = history.minOrNull() ?: 0f
                            val maxV = history.maxOrNull() ?: 1f
                            val span = (maxV - minV).coerceAtLeast(0.001f)
                            val path = Path()
                            history.forEachIndexed { idx, v ->
                                val x = (idx.toFloat() / (history.size - 1).coerceAtLeast(1)) * size.width
                                val norm = ((v - minV) / span).coerceIn(0f, 1f)
                                val y = size.height - norm * size.height
                                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(
                                path = path,
                                color = theme.accent,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        } else {
                            val midY = size.height / 2f
                            drawLine(
                                color = theme.textSecondary.copy(alpha = 0.25f),
                                start = Offset(0f, midY),
                                end = Offset(size.width, midY),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = theme.accent,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = type.caption,
                color = theme.textSecondary
            )
            Text(
                text = value,
                style = type.body.copy(fontWeight = FontWeight.SemiBold),
                color = theme.textPrimary
            )
        }
    }
}

private data class DeviceSystemSpecs(
    val deviceModel: String,
    val androidRelease: String,
    val sdkInt: Int,
    val ramTotalGb: String,
    val ramAvailableGb: String,
    val storageTotalGb: String,
    val storageFreeGb: String,
    val batteryLevelPct: Int,
    val batteryTempC: String,
    val batteryVoltageMv: Int,
    val screenResolution: String,
    val screenDpi: Int,
    val refreshRateHz: Int
)

private fun readDeviceSystemSpecs(context: Context): DeviceSystemSpecs {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    runCatching { am?.getMemoryInfo(memInfo) }
    val ramTotal = memInfo.totalMem.toDouble() / (1024.0 * 1024.0 * 1024.0)
    val ramAvail = memInfo.availMem.toDouble() / (1024.0 * 1024.0 * 1024.0)

    val statFs = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
    val totalBytes = statFs?.totalBytes ?: 0L
    val freeBytes = statFs?.availableBytes ?: 0L
    val storageTotal = totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    val storageFree = freeBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)

    val battIntent = runCatching {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }.getOrNull()
    val level = battIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = battIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val pct = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else 100
    val tempTenths = battIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280
    val voltage = battIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4000) ?: 4000

    val dm: DisplayMetrics = context.resources.displayMetrics
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    @Suppress("DEPRECATION")
    val refresh = runCatching { wm?.defaultDisplay?.refreshRate?.toInt() ?: 60 }.getOrDefault(60)
    val physInches = hypot(
        dm.widthPixels.toDouble() / dm.xdpi.coerceAtLeast(1f),
        dm.heightPixels.toDouble() / dm.ydpi.coerceAtLeast(1f)
    )

    return DeviceSystemSpecs(
        deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
        androidRelease = Build.VERSION.RELEASE ?: "14",
        sdkInt = Build.VERSION.SDK_INT,
        ramTotalGb = String.format(Locale.US, "%.1f GB", ramTotal),
        ramAvailableGb = String.format(Locale.US, "%.1f GB", ramAvail),
        storageTotalGb = String.format(Locale.US, "%.1f GB", storageTotal),
        storageFreeGb = String.format(Locale.US, "%.1f GB", storageFree),
        batteryLevelPct = pct,
        batteryTempC = String.format(Locale.US, "%.1f", tempTenths / 10.0),
        batteryVoltageMv = voltage,
        screenResolution = "${dm.widthPixels}×${dm.heightPixels} (${String.format(Locale.US, "%.1f\"", physInches)})",
        screenDpi = dm.densityDpi,
        refreshRateHz = refresh
    )
}

private fun buildShareableDeviceReport(
    systemSummary: DeviceSystemSpecs,
    monitored: List<LiveSensorItem>,
    liveValues: Map<Int, String>,
    allSensors: List<Sensor>
): String {
    return buildString {
        appendLine("=== Dyne Device & Sensors Report ===")
        appendLine("Device: ${systemSummary.deviceModel}")
        appendLine("Android: ${systemSummary.androidRelease} (SDK ${systemSummary.sdkInt})")
        appendLine("RAM: ${systemSummary.ramAvailableGb} free / ${systemSummary.ramTotalGb}")
        appendLine("Storage: ${systemSummary.storageFreeGb} free / ${systemSummary.storageTotalGb}")
        appendLine("Battery: ${systemSummary.batteryLevelPct}% (${systemSummary.batteryTempC} °C, ${systemSummary.batteryVoltageMv} mV)")
        appendLine("Display: ${systemSummary.screenResolution} @ ${systemSummary.screenDpi} dpi (${systemSummary.refreshRateHz} Hz)")
        appendLine()
        appendLine("--- Monitored Sensors ---")
        monitored.forEach { item ->
            val hw = item.sensor
            val valueStr = liveValues[hw?.type ?: item.type] ?: "N/A"
            if (hw != null) {
                appendLine("• ${item.label}: $valueStr ${item.unit} (${hw.name}, max=${hw.maximumRange})")
            } else {
                appendLine("• ${item.label}: Not available on this phone")
            }
        }
        appendLine()
        appendLine("Total Hardware Sensors Reported by OS: ${allSensors.size}")
    }
}
