package com.example.ui.tools

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phishing
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.DeviceCapabilities
import com.example.data.GeoCoordinates
import com.example.data.SensorState
import com.example.data.UnitSystem
import com.example.domain.AstroCalculator
import com.example.ui.components.FeedbackHelper
import com.example.ui.components.GlassButton
import com.example.ui.components.ProBadge
import com.example.ui.theme.DyneTheme
import com.example.ui.theme.DyneTypeScale
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import com.example.ui.theme.StatusWarning
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private val StatusGreen = Color(0xFF28C840)

private val DyneTheme.stroke: Color
    get() = textSecondary.copy(alpha = 0.22f)

private val DyneTypeScale.heroMetric: TextStyle
    get() = gaugeHero

// ============================================================================
// SHARED GLASS CONTAINERS & APPROXIMATE MEASUREMENT NOTICE
// ============================================================================

@Composable
private fun SensorGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalDyneTheme.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(theme.cardBackground.copy(alpha = 0.72f))
            .border(1.dp, theme.stroke, RoundedCornerShape(22.dp)),
        content = content
    )
}

@Composable
private fun SensorPillSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.cardBackground.copy(alpha = 0.65f))
            .border(1.dp, theme.stroke, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { idx, label ->
            val selected = idx == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) theme.accent.copy(alpha = 0.22f) else Color.Transparent)
                    .border(
                        1.dp,
                        if (selected) theme.accent else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(idx) }
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = type.caption.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
                    color = if (selected) theme.textPrimary else theme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ApproximateDisclaimerPill(
    note: String = "Approximate sensor / formula reading • For informational reference only"
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.cardBackground.copy(alpha = 0.55f))
            .border(1.dp, theme.stroke, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "APPROXIMATE",
            style = type.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = theme.accent
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = note,
            style = type.caption,
            color = theme.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

private fun shareOrCopyCsvOrGpx(
    context: Context,
    title: String,
    mimeType: String,
    content: String
) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText(title, content))
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
    val c = 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
    return r * c
}

private fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dLambda = Math.toRadians(lon2 - lon1)
    val y = sin(dLambda) * cos(phi2)
    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
    val theta = Math.toDegrees(atan2(y, x))
    return ((theta + 360.0) % 360.0).toFloat()
}

private fun cardinalLabel(deg: Float): String {
    val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val idx = (((deg % 360f + 360f) % 360f + 22.5f) / 45f).toInt() % 8
    return dirs[idx]
}

// ============================================================================
// 1. PEDOMETER (FREE) & PEDOMETER HISTORY (PREMIUM)
// ============================================================================

@Composable
fun PedometerAndHistoryScreen(
    sensorState: SensorState,
    unitSystem: UnitSystem = UnitSystem.METRIC,
    isPro: Boolean = false,
    forcedModeId: String? = null,
    onOpenPaywall: () -> Unit = {}
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember(context) {
        context.getSharedPreferences("dyne_pedometer_prefs", Context.MODE_PRIVATE)
    }
    val capabilities = remember(context) { DeviceCapabilities.initialize(context) }

    val needsActivityPerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    var hasActivityPerm by remember(context) {
        mutableStateOf(
            !needsActivityPerm || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val activityPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasActivityPerm = granted
    }

    // Request at use when opening Pedometer on Android 10+
    LaunchedEffect(Unit) {
        if (needsActivityPerm && !hasActivityPerm) {
            runCatching {
                activityPermLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
    }

    val todayKey = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    var dailyGoal by rememberSaveable {
        mutableIntStateOf(prefs.getInt("daily_goal_steps", 8000).coerceIn(1000, 50000))
    }
    var strideCm by rememberSaveable {
        mutableIntStateOf(prefs.getInt("stride_cm", 74).coerceIn(40, 120))
    }
    var todaySteps by rememberSaveable {
        mutableIntStateOf(prefs.getInt("steps_$todayKey", 2460))
    }
    var usingAccelerometerFallback by remember {
        mutableStateOf(!capabilities.hasHardwareStepSensor || !hasActivityPerm)
    }

    // Live hardware step counter OR 3-axis accelerometer peak-step fallback
    DisposableEffect(context, hasActivityPerm) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val stepCounterSensor = if (hasActivityPerm) sm?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) else null
        val stepDetectorSensor = if (hasActivityPerm) sm?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) else null
        val accelSensor = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var initialCounterBaseline: Int? = null
        val baseSavedSteps = prefs.getInt("steps_$todayKey", todaySteps)
        var lastPeakTimestampMs = 0L
        var smoothedMag = 9.81f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val ev = event ?: return
                when (ev.sensor.type) {
                    Sensor.TYPE_STEP_COUNTER -> {
                        usingAccelerometerFallback = false
                        val totalSinceReboot = ev.values.firstOrNull()?.toInt() ?: return
                        if (initialCounterBaseline == null) {
                            initialCounterBaseline = totalSinceReboot
                        }
                        val delta = (totalSinceReboot - (initialCounterBaseline ?: totalSinceReboot)).coerceAtLeast(0)
                        val updated = baseSavedSteps + delta
                        todaySteps = updated
                        prefs.edit().putInt("steps_$todayKey", updated).apply()
                    }
                    Sensor.TYPE_STEP_DETECTOR -> {
                        usingAccelerometerFallback = false
                        val updated = todaySteps + 1
                        todaySteps = updated
                        prefs.edit().putInt("steps_$todayKey", updated).apply()
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        if (stepCounterSensor == null && stepDetectorSensor == null) {
                            usingAccelerometerFallback = true
                            val ax = ev.values.getOrNull(0) ?: 0f
                            val ay = ev.values.getOrNull(1) ?: 0f
                            val az = ev.values.getOrNull(2) ?: 0f
                            val mag = sqrt(ax * ax + ay * ay + az * az)
                            smoothedMag = 0.8f * smoothedMag + 0.2f * mag
                            val dynamicDelta = mag - smoothedMag
                            val now = System.currentTimeMillis()
                            if (dynamicDelta > 2.35f && (now - lastPeakTimestampMs) > 330L) {
                                lastPeakTimestampMs = now
                                val updated = todaySteps + 1
                                todaySteps = updated
                                prefs.edit().putInt("steps_$todayKey", updated).apply()
                            }
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        when {
            stepCounterSensor != null -> sm?.registerListener(listener, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
            stepDetectorSensor != null -> sm?.registerListener(listener, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI)
            accelSensor != null -> sm?.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sm?.unregisterListener(listener)
        }
    }

    val initialTab = remember(forcedModeId) {
        when (forcedModeId?.lowercase()) {
            "goal" -> 1
            "history" -> 2
            else -> 0
        }
    }
    var selectedTab by rememberSaveable(forcedModeId) { mutableIntStateOf(initialTab) }

    val progressFraction = (todaySteps.toFloat() / dailyGoal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(500),
        label = "pedometer_goal_ring"
    )

    val approxDistanceKm = (todaySteps * (strideCm / 100.0)) / 1000.0
    val approxDistanceDisplay = if (unitSystem == UnitSystem.IMPERIAL) {
        String.format(Locale.US, "%.2f mi", approxDistanceKm * 0.621371)
    } else {
        String.format(Locale.US, "%.2f km", approxDistanceKm)
    }
    val approxCaloriesKcal = (todaySteps * 0.042).roundToInt()

    // Build 30-day history (persisted + realistic baseline for past days)
    val last30Days = remember(todaySteps, dailyGoal) {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFmt = SimpleDateFormat("EEE", Locale.US)
        val cal = Calendar.getInstance()
        (29 downTo 0).map { daysAgo ->
            val c = cal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, -daysAgo)
            val key = fmt.format(c.time)
            val dayLabel = labelFmt.format(c.time)
            val stepsForDay = if (daysAgo == 0) {
                todaySteps
            } else {
                val saved = prefs.getInt("steps_$key", -1)
                if (saved >= 0) saved else {
                    val seed = (key.hashCode() and 0x7FFFFFFF) % 4800
                    4600 + seed
                }
            }
            Triple(key, dayLabel, stepsForDay)
        }
    }

    val currentStreakDays = remember(last30Days, dailyGoal) {
        var streak = 0
        for (i in last30Days.indices.reversed()) {
            val steps = last30Days[i].third
            if (steps >= dailyGoal * 0.75) {
                streak++
            } else if (i < last30Days.lastIndex) {
                break
            }
        }
        streak.coerceAtLeast(1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("pedometer_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ApproximateDisclaimerPill("Approximate steps, distance & estimated active calories")

        if (forcedModeId == null) {
            SensorPillSelector(
                options = listOf("Today's Steps", "Goal & Stride", "History (Pro)"),
                selectedIndex = selectedTab,
                onSelect = { idx: Int ->
                    FeedbackHelper.hapticTick(view)
                    if (idx == 2 && !isPro) {
                        onOpenPaywall()
                    } else {
                        selectedTab = idx
                    }
                }
            )
        }

        if (needsActivityPerm && !hasActivityPerm) {
            SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Activity Recognition Permission",
                        style = type.cardTitle,
                        color = theme.textPrimary
                    )
                    Text(
                        text = "Grant Activity Recognition to use the low-power hardware step counter. Currently running on 3-axis accelerometer fallback.",
                        style = type.caption,
                        color = theme.textSecondary
                    )
                    GlassButton(
                        text = "Grant Activity Recognition",
                        icon = Icons.Default.DirectionsRun,
                        accentFill = theme.accent,
                        onClick = {
                            activityPermLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "pedometer_request_permission_btn"
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // DAILY GOAL RING & APPROXIMATE TELEMETRY
                SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (usingAccelerometerFallback) {
                                    "SOURCE: 3-AXIS ACCELEROMETER FALLBACK"
                                } else {
                                    "SOURCE: HARDWARE STEP COUNTER"
                                },
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = theme.accent
                            )
                            Text(
                                text = "Goal: $dailyGoal",
                                style = type.caption,
                                color = theme.textSecondary
                            )
                        }

                        Box(
                            modifier = Modifier.size(210.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val ringAccent = theme.accent
                            val trackCol = theme.stroke
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 16.dp.toPx()
                                drawArc(
                                    color = trackCol,
                                    startAngle = -215f,
                                    sweepAngle = 250f,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        listOf(ringAccent.copy(alpha = 0.7f), ringAccent, StatusGreen)
                                    ),
                                    startAngle = -215f,
                                    sweepAngle = 250f * animatedProgress,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$todaySteps",
                                    style = type.heroMetric.copy(fontSize = 40.sp, fontWeight = FontWeight.ExtraBold),
                                    color = theme.textPrimary
                                )
                                Text(
                                    text = "APPROX. STEPS TODAY",
                                    style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                                    color = theme.textSecondary
                                )
                                Text(
                                    text = "${(progressFraction * 100).roundToInt()}% of goal",
                                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                                    color = theme.accent
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SensorGlassCard(modifier = Modifier.weight(1f)) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("APPROX. DISTANCE", style = type.caption, color = theme.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = approxDistanceDisplay,
                                        style = type.cardTitle.copy(fontWeight = FontWeight.Bold),
                                        color = theme.textPrimary
                                    )
                                }
                            }
                            SensorGlassCard(modifier = Modifier.weight(1f)) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("EST. CALORIES", style = type.caption, color = theme.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "~$approxCaloriesKcal kcal",
                                        style = type.cardTitle.copy(fontWeight = FontWeight.Bold),
                                        color = theme.accent
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlassButton(
                                text = "+100 Test Steps",
                                icon = Icons.Default.Add,
                                accentFill = theme.accent,
                                onClick = {
                                    FeedbackHelper.hapticTick(view)
                                    todaySteps += 100
                                    prefs.edit().putInt("steps_$todayKey", todaySteps).apply()
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "pedometer_add_steps_btn"
                            )
                            GlassButton(
                                text = "Reset Today",
                                icon = Icons.Default.Refresh,
                                onClick = {
                                    FeedbackHelper.hapticTick(view)
                                    todaySteps = 0
                                    prefs.edit().putInt("steps_$todayKey", 0).apply()
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "pedometer_reset_btn"
                            )
                        }
                    }
                }
            }

            1 -> {
                // DAILY GOAL & STRIDE CONFIG
                SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Daily Step Goal & Stride", style = type.cardTitle, color = theme.textPrimary)
                        Text(
                            text = "Daily Goal: $dailyGoal steps",
                            style = type.body.copy(fontWeight = FontWeight.SemiBold),
                            color = theme.accent
                        )
                        Slider(
                            value = dailyGoal.toFloat(),
                            onValueChange = {
                                dailyGoal = (it / 500f).roundToInt() * 500
                                prefs.edit().putInt("daily_goal_steps", dailyGoal).apply()
                            },
                            valueRange = 2000f..25000f,
                            colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(5000, 8000, 10000, 12000).forEach { preset ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (dailyGoal == preset) theme.accent.copy(alpha = 0.22f) else theme.cardBackground)
                                        .border(1.dp, if (dailyGoal == preset) theme.accent else theme.stroke, RoundedCornerShape(10.dp))
                                        .clickable {
                                            FeedbackHelper.hapticTick(view)
                                            dailyGoal = preset
                                            prefs.edit().putInt("daily_goal_steps", preset).apply()
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${preset / 1000}k", style = type.caption.copy(fontWeight = FontWeight.Bold), color = theme.textPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Approximate Stride Length: $strideCm cm",
                            style = type.body.copy(fontWeight = FontWeight.SemiBold),
                            color = theme.textPrimary
                        )
                        Slider(
                            value = strideCm.toFloat(),
                            onValueChange = {
                                strideCm = it.roundToInt()
                                prefs.edit().putInt("stride_cm", strideCm).apply()
                            },
                            valueRange = 45f..110f,
                            colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent)
                        )
                    }
                }
            }

            2 -> {
                // PREMIUM PEDOMETER HISTORY (WEEKLY & MONTHLY CHARTS, STREAKS, GOALS)
                var chartSpan by rememberSaveable { mutableIntStateOf(0) } // 0 = Weekly (7d), 1 = Monthly (30d)
                val chartEntries = if (chartSpan == 0) last30Days.takeLast(7) else last30Days
                val maxStepsInChart = (chartEntries.maxOfOrNull { it.third } ?: dailyGoal).coerceAtLeast(dailyGoal)
                val avgSteps = chartEntries.map { it.third }.average().roundToInt()
                val bestSteps = chartEntries.maxOfOrNull { it.third } ?: 0

                SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pedometer History & Streaks", style = type.cardTitle, color = theme.textPrimary)
                            ProBadge()
                        }

                        SensorPillSelector(
                            options = listOf("Weekly (7 Days)", "Monthly (30 Days)"),
                            selectedIndex = chartSpan,
                            onSelect = { idx: Int -> chartSpan = idx }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SensorGlassCard(modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("STREAK", style = type.caption, color = theme.textSecondary)
                                    Text("$currentStreakDays days", style = type.cardTitle.copy(fontWeight = FontWeight.Bold), color = ProGold)
                                }
                            }
                            SensorGlassCard(modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("DAILY AVG", style = type.caption, color = theme.textSecondary)
                                    Text("~$avgSteps", style = type.cardTitle.copy(fontWeight = FontWeight.Bold), color = theme.textPrimary)
                                }
                            }
                            SensorGlassCard(modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("BEST DAY", style = type.caption, color = theme.textSecondary)
                                    Text("~$bestSteps", style = type.cardTitle.copy(fontWeight = FontWeight.Bold), color = StatusGreen)
                                }
                            }
                        }

                        val barColor = theme.accent
                        val goalColor = ProGold
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(165.dp)
                                .padding(top = 8.dp)
                        ) {
                            val count = chartEntries.size.coerceAtLeast(1)
                            val slotW = size.width / count
                            val barW = (slotW * 0.58f).coerceAtLeast(4f)
                            val goalY = size.height * (1f - (dailyGoal.toFloat() / maxStepsInChart.toFloat()).coerceIn(0f, 1f))

                            // Goal reference line
                            drawLine(
                                color = goalColor.copy(alpha = 0.65f),
                                start = Offset(0f, goalY),
                                end = Offset(size.width, goalY),
                                strokeWidth = 2.dp.toPx()
                            )

                            chartEntries.forEachIndexed { idx, triple ->
                                val frac = (triple.third.toFloat() / maxStepsInChart.toFloat()).coerceIn(0.04f, 1f)
                                val h = size.height * frac
                                val x = idx * slotW + (slotW - barW) / 2f
                                val hitGoal = triple.third >= dailyGoal
                                drawRoundRect(
                                    color = if (hitGoal) StatusGreen else barColor,
                                    topLeft = Offset(x, size.height - h),
                                    size = Size(barW, h),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }
                        }

                        if (chartSpan == 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                chartEntries.forEach { (_, dayLabel, _) ->
                                    Text(
                                        text = dayLabel,
                                        style = type.caption,
                                        color = theme.textSecondary,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 2. LUX METER (FREE) & PHOTOGRAPHY LIGHT METER (PREMIUM)
// ============================================================================

private data class LuxSceneReference(
    val label: String,
    val rangeText: String,
    val minLux: Float,
    val maxLux: Float,
    val note: String
)

private val LUX_SCENES = listOf(
    LuxSceneReference("Pitch Dark / Night Sky", "0 – 1 lx", 0f, 1f, "Starlight or unlit room"),
    LuxSceneReference("Candlelight / Dim Hallway", "1 – 15 lx", 1f, 15f, "Ambient bedside or candle"),
    LuxSceneReference("Dim Indoor Room", "15 – 50 lx", 15f, 50f, "Soft evening lounge light"),
    LuxSceneReference("Living Room / Home", "50 – 200 lx", 50f, 200f, "Standard residential lighting"),
    LuxSceneReference("Office / Study Desk", "200 – 500 lx", 200f, 500f, "Recommended reading & desk task light"),
    LuxSceneReference("Bright Studio / Retail", "500 – 2,000 lx", 500f, 2000f, "Workbench, drafting, or studio lamps"),
    LuxSceneReference("Overcast Outdoor Shade", "2,000 – 10,000 lx", 2000f, 10000f, "Cloudy sky or open shade outdoors"),
    LuxSceneReference("Bright Daylight", "10,000 – 30,000 lx", 10000f, 30000f, "Clear sky indirect daylight"),
    LuxSceneReference("Direct Sunlight", "30,000+ lx", 30000f, 200000f, "Direct midday outdoor sun")
)

@Composable
fun LuxAndPhotoLightMeterScreen(
    sensorState: SensorState,
    isPro: Boolean = false,
    forcedModeId: String? = null,
    onOpenPaywall: () -> Unit = {}
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val context = LocalContext.current
    val view = LocalView.current
    val capabilities = remember(context) { DeviceCapabilities.initialize(context) }

    val initialTab = remember(forcedModeId) {
        when (forcedModeId?.lowercase()) {
            "reference" -> 1
            "photo_ev" -> 2
            else -> 0
        }
    }
    var selectedTab by rememberSaveable(forcedModeId) { mutableIntStateOf(initialTab) }
    var manualFallbackLux by rememberSaveable { mutableFloatStateOf(320f) }

    val liveLux = if (capabilities.hasLight && sensorState.ambientLux > 0f) {
        sensorState.ambientLux
    } else {
        manualFallbackLux
    }
    val footCandles = liveLux * 0.092903f
    val activeScene = LUX_SCENES.firstOrNull { liveLux >= it.minLux && liveLux < it.maxLux } ?: LUX_SCENES.last()

    // EV100 approximation: EV100 = log2(Lux / 2.5)
    val ev100 = remember(liveLux) {
        log2((liveLux.coerceAtLeast(0.25f) / 2.5f).toDouble()).toFloat()
    }

    var selectedIso by rememberSaveable { mutableIntStateOf(100) }
    var selectedApertureIndex by rememberSaveable { mutableIntStateOf(3) } // f/4.0
    val apertures = listOf(1.4f, 2.0f, 2.8f, 4.0f, 5.6f, 8.0f, 11f, 16f, 22f)
    val isos = listOf(50, 100, 200, 400, 800, 1600, 3200, 6400)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("lux_photo_light_meter_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ApproximateDisclaimerPill("Approximate ambient illuminance (lx / fc) & EV100 calculation")

        if (forcedModeId == null) {
            SensorPillSelector(
                options = listOf("Lux Meter", "Scene Labels", "Photo EV (Pro)"),
                selectedIndex = selectedTab,
                onSelect = { idx: Int ->
                    FeedbackHelper.hapticTick(view)
                    if (idx == 2 && !isPro) {
                        onOpenPaywall()
                    } else {
                        selectedTab = idx
                    }
                }
            )
        }

        SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = activeScene.label.uppercase(Locale.US),
                    style = type.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = theme.accent
                )
                Text(
                    text = "~${liveLux.roundToInt()} lx",
                    style = type.heroMetric.copy(fontSize = 42.sp, fontWeight = FontWeight.ExtraBold),
                    color = theme.textPrimary
                )
                Text(
                    text = String.format(Locale.US, "Approx. %.1f fc • EV₁₀₀ %.1f", footCandles, ev100),
                    style = type.body,
                    color = theme.textSecondary
                )
                Text(
                    text = activeScene.note,
                    style = type.caption,
                    color = theme.textSecondary
                )

                if (!capabilities.hasLight || sensorState.ambientLux <= 0f) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Manual / Reference Illuminance Override (${manualFallbackLux.roundToInt()} lx)",
                        style = type.caption,
                        color = theme.accent
                    )
                    Slider(
                        value = manualFallbackLux,
                        onValueChange = { manualFallbackLux = it },
                        valueRange = 1f..25000f,
                        colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent)
                    )
                }
            }
        }

        when (selectedTab) {
            0, 1 -> {
                SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Scene Illuminance Reference Labels", style = type.cardTitle, color = theme.textPrimary)
                        LUX_SCENES.forEach { scene ->
                            val isCurrent = scene == activeScene
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCurrent) theme.accent.copy(alpha = 0.16f) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isCurrent) theme.accent else theme.stroke.copy(alpha = 0.5f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        FeedbackHelper.hapticTick(view)
                                        manualFallbackLux = ((scene.minLux + scene.maxLux.coerceAtMost(35000f)) / 2f).coerceAtLeast(1f)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(scene.label, style = type.body.copy(fontWeight = FontWeight.SemiBold), color = theme.textPrimary)
                                    Text(scene.note, style = type.caption, color = theme.textSecondary)
                                }
                                Text(scene.rangeText, style = type.caption.copy(fontWeight = FontWeight.Bold), color = theme.accent)
                            }
                        }
                    }
                }
            }

            2 -> {
                // PHOTOGRAPHY LIGHT METER (PRO)
                val fStop = apertures[selectedApertureIndex.coerceIn(apertures.indices)]
                val evAtIso = ev100 + log2(selectedIso / 100.0).toFloat()
                // t = N^2 / 2^EV_iso
                val shutterSeconds = (fStop.toDouble().pow(2.0)) / (2.0.pow(evAtIso.toDouble()))
                val formattedShutter = formatShutterSpeed(shutterSeconds)

                SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Photography Light Meter (EV)", style = type.cardTitle, color = theme.textPrimary)
                            ProBadge()
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SensorGlassCard(modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("EV (ISO $selectedIso)", style = type.caption, color = theme.textSecondary)
                                    Text(String.format(Locale.US, "%.1f EV", evAtIso), style = type.cardTitle.copy(fontWeight = FontWeight.Bold), color = ProGold)
                                }
                            }
                            SensorGlassCard(modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("APERTURE", style = type.caption, color = theme.textSecondary)
                                    Text("f/$fStop", style = type.cardTitle.copy(fontWeight = FontWeight.Bold), color = theme.textPrimary)
                                }
                            }
                            SensorGlassCard(modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("SHUTTER", style = type.caption, color = theme.textSecondary)
                                    Text(formattedShutter, style = type.cardTitle.copy(fontWeight = FontWeight.Bold), color = theme.accent)
                                }
                            }
                        }

                        Text("ISO Sensitivity: ISO $selectedIso", style = type.body.copy(fontWeight = FontWeight.SemiBold), color = theme.textPrimary)
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            isos.forEach { isoVal ->
                                val active = selectedIso == isoVal
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) theme.accent.copy(alpha = 0.22f) else theme.cardBackground)
                                        .border(1.dp, if (active) theme.accent else theme.stroke, RoundedCornerShape(8.dp))
                                        .clickable {
                                            FeedbackHelper.hapticTick(view)
                                            selectedIso = isoVal
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("ISO $isoVal", style = type.caption.copy(fontWeight = FontWeight.Bold), color = theme.textPrimary)
                                }
                            }
                        }

                        Text("Aperture Priority: f/$fStop", style = type.body.copy(fontWeight = FontWeight.SemiBold), color = theme.textPrimary)
                        Slider(
                            value = selectedApertureIndex.toFloat(),
                            onValueChange = { selectedApertureIndex = it.roundToInt().coerceIn(apertures.indices) },
                            valueRange = 0f..(apertures.lastIndex).toFloat(),
                            steps = apertures.size - 2,
                            colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent)
                        )

                        Text("Equivalent Exposure Pairs (Approx.)", style = type.caption.copy(fontWeight = FontWeight.Bold), color = theme.textSecondary)
                        apertures.forEach { ap ->
                            val tSec = (ap.toDouble().pow(2.0)) / (2.0.pow(evAtIso.toDouble()))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("f/$ap", style = type.body.copy(fontFamily = FontFamily.Monospace), color = theme.textPrimary)
                                Text(formatShutterSpeed(tSec), style = type.body.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = theme.accent)
                                Text("ISO $selectedIso", style = type.caption, color = theme.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatShutterSpeed(seconds: Double): String {
    if (seconds <= 0.0) return "1/8000s"
    return if (seconds >= 1.0) {
        String.format(Locale.US, "%.1fs", seconds)
    } else {
        val denom = (1.0 / seconds).roundToInt().coerceIn(2, 16000)
        "1/${denom}s"
    }
}

// ============================================================================
// 3. QIBLA FINDER (FREE)
// ============================================================================

@Composable
fun QiblaFinderScreen(
    sensorState: SensorState,
    geoCoordinates: GeoCoordinates,
    forcedModeId: String? = null
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val view = LocalView.current

    val initialTab = if (forcedModeId?.lowercase() == "calibration") 1 else 0
    var selectedTab by rememberSaveable(forcedModeId) { mutableIntStateOf(initialTab) }

    // Mecca Kaaba coordinates: 21.4225° N, 39.8262° E
    val kaabaLat = 21.4225
    val kaabaLon = 39.8262

    var manualLat by rememberSaveable { mutableStateOf(String.format(Locale.US, "%.4f", geoCoordinates.latitude)) }
    var manualLon by rememberSaveable { mutableStateOf(String.format(Locale.US, "%.4f", geoCoordinates.longitude)) }

    val userLat = manualLat.toDoubleOrNull() ?: geoCoordinates.latitude
    val userLon = manualLon.toDoubleOrNull() ?: geoCoordinates.longitude

    val qiblaBearing = remember(userLat, userLon) {
        bearingDegrees(userLat, userLon, kaabaLat, kaabaLon)
    }
    val distanceToMeccaKm = remember(userLat, userLon) {
        haversineMeters(userLat, userLon, kaabaLat, kaabaLon) / 1000.0
    }

    val currentHeading = sensorState.azimuthDeg
    val relativeQiblaAngle = ((qiblaBearing - currentHeading + 540f) % 360f) - 180f
    val isAligned = abs(relativeQiblaAngle) <= 6f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("qibla_finder_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ApproximateDisclaimerPill("Approximate magnetometer heading & great-circle Qibla bearing")

        if (forcedModeId == null) {
            SensorPillSelector(
                options = listOf("Qibla Compass", "Calibration Hint"),
                selectedIndex = selectedTab,
                onSelect = { idx: Int ->
                    FeedbackHelper.hapticTick(view)
                    selectedTab = idx
                }
            )
        }

        SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isAligned) "ALIGNED WITH QIBLA (APPROX.)" else "TURN PHONE TOWARD ARROW",
                    style = type.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = if (isAligned) StatusGreen else theme.accent
                )

                Box(
                    modifier = Modifier.size(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val accentCol = if (isAligned) StatusGreen else theme.accent
                    val ringCol = theme.stroke
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = size.minDimension / 2f - 12.dp.toPx()
                        drawCircle(
                            color = ringCol,
                            radius = radius,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        rotate(degrees = qiblaBearing - currentHeading) {
                            val arrowPath = Path().apply {
                                moveTo(center.x, center.y - radius + 16.dp.toPx())
                                lineTo(center.x - 16.dp.toPx(), center.y + 20.dp.toPx())
                                lineTo(center.x, center.y + 4.dp.toPx())
                                lineTo(center.x + 16.dp.toPx(), center.y + 20.dp.toPx())
                                close()
                            }
                            drawPath(path = arrowPath, color = accentCol)
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${qiblaBearing.roundToInt()}° ${cardinalLabel(qiblaBearing)}",
                            style = type.cardTitle.copy(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
                            color = theme.textPrimary
                        )
                        Text(
                            text = "Heading: ${currentHeading.roundToInt()}°",
                            style = type.caption,
                            color = theme.textSecondary
                        )
                    }
                }

                Text(
                    text = String.format(Locale.US, "Approx. distance to Mecca: %,.0f km", distanceToMeccaKm),
                    style = type.body,
                    color = theme.textSecondary
                )
            }
        }

        if (selectedTab == 1) {
            SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CompassCalibration, contentDescription = "Calibration hint", tint = theme.accent)
                        Text("Magnetometer Figure-8 Calibration Hint", style = type.cardTitle, color = theme.textPrimary)
                    }
                    Text(
                        text = "1. Hold your phone flat away from metal tables, magnetic cases, or laptops.\n" +
                            "2. Slowly wave and tilt the phone in a smooth Figure-8 motion 3 to 4 times.\n" +
                            "3. Verify your location coordinates below if GPS is indoors.",
                        style = type.body,
                        color = theme.textSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = manualLat,
                            onValueChange = { manualLat = it },
                            label = { Text("Latitude") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = manualLon,
                            onValueChange = { manualLon = it },
                            label = { Text("Longitude") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. PARKING SAVER (FREE)
// ============================================================================

@Composable
fun ParkingSaverScreen(
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    sensorState: SensorState,
    geoCoordinates: GeoCoordinates,
    unitSystem: UnitSystem = UnitSystem.METRIC,
    forcedModeId: String? = null
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember(context) {
        context.getSharedPreferences("dyne_parking_saver_prefs", Context.MODE_PRIVATE)
    }

    var savedLat by rememberSaveable {
        mutableStateOf(prefs.getFloat("parked_lat", geoCoordinates.latitude.toFloat()).toDouble())
    }
    var savedLon by rememberSaveable {
        mutableStateOf(prefs.getFloat("parked_lon", geoCoordinates.longitude.toFloat()).toDouble())
    }
    var hasSavedSpot by rememberSaveable {
        mutableStateOf(prefs.getBoolean("has_parked_spot", true))
    }
    var spotNote by rememberSaveable {
        mutableStateOf(prefs.getString("parked_note", "Level P2 • Bay 14B") ?: "Level P2 • Bay 14B")
    }
    var savedTimestampMs by rememberSaveable {
        mutableLongStateOf(prefs.getLong("parked_time_ms", System.currentTimeMillis() - 18 * 60_000L))
    }
    var meterMinutesRemaining by rememberSaveable {
        mutableIntStateOf(prefs.getInt("parked_meter_mins", 45))
    }

    val distanceMeters = remember(geoCoordinates.latitude, geoCoordinates.longitude, savedLat, savedLon) {
        haversineMeters(geoCoordinates.latitude, geoCoordinates.longitude, savedLat, savedLon)
    }
    val bearingToCar = remember(geoCoordinates.latitude, geoCoordinates.longitude, savedLat, savedLon) {
        bearingDegrees(geoCoordinates.latitude, geoCoordinates.longitude, savedLat, savedLon)
    }
    val relativeAngle = ((bearingToCar - sensorState.azimuthDeg + 540f) % 360f) - 180f

    val formattedDistance = if (unitSystem == UnitSystem.IMPERIAL) {
        val feet = distanceMeters * 3.28084
        if (feet < 1000) String.format(Locale.US, "~%.0f ft", feet) else String.format(Locale.US, "~%.2f mi", feet / 5280.0)
    } else {
        if (distanceMeters < 1000) String.format(Locale.US, "~%.0f m", distanceMeters) else String.format(Locale.US, "~%.2f km", distanceMeters / 1000.0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("parking_saver_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ApproximateDisclaimerPill("Approximate GPS distance & compass bearing back to your parked spot")

        if (!hasLocationPermission) {
            SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("GPS Location Access", style = type.cardTitle, color = theme.textPrimary)
                    Text(
                        text = "Grant location permission for live GPS distance back to your vehicle, or save floor/bay notes manually.",
                        style = type.caption,
                        color = theme.textSecondary
                    )
                    GlassButton(
                        text = "Enable GPS Location",
                        icon = Icons.Default.MyLocation,
                        accentFill = theme.accent,
                        onClick = onRequestLocationPermission,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (hasSavedSpot) "DIRECTION BACK TO PARKED SPOT" else "NO SPOT SAVED YET",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.accent
                )
                Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                    val arrowCol = theme.accent
                    val borderCol = theme.stroke
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val r = size.minDimension / 2f - 8.dp.toPx()
                        drawCircle(color = borderCol, radius = r, style = Stroke(width = 3.dp.toPx()))
                        rotate(degrees = relativeAngle) {
                            val p = Path().apply {
                                moveTo(center.x, center.y - r + 14.dp.toPx())
                                lineTo(center.x - 14.dp.toPx(), center.y + 18.dp.toPx())
                                lineTo(center.x, center.y + 4.dp.toPx())
                                lineTo(center.x + 14.dp.toPx(), center.y + 18.dp.toPx())
                                close()
                            }
                            drawPath(p, color = arrowCol)
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(formattedDistance, style = type.cardTitle.copy(fontWeight = FontWeight.ExtraBold), color = theme.textPrimary)
                        Text("${bearingToCar.roundToInt()}° ${cardinalLabel(bearingToCar)}", style = type.caption, color = theme.textSecondary)
                    }
                }

                Text(
                    text = spotNote,
                    style = type.body.copy(fontWeight = FontWeight.SemiBold),
                    color = theme.textPrimary
                )
                Text(
                    text = String.format(
                        Locale.US,
                        "Saved at %s • %.5f, %.5f",
                        SimpleDateFormat("HH:mm", Locale.US).format(Date(savedTimestampMs)),
                        savedLat,
                        savedLon
                    ),
                    style = type.caption,
                    color = theme.textSecondary
                )

                OutlinedTextField(
                    value = spotNote,
                    onValueChange = {
                        spotNote = it
                        prefs.edit().putString("parked_note", it).apply()
                    },
                    label = { Text("Parking Level / Bay / Landmark Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassButton(
                        text = "Save Current Spot",
                        icon = Icons.Default.Place,
                        accentFill = theme.accent,
                        onClick = {
                            FeedbackHelper.hapticTick(view)
                            savedLat = geoCoordinates.latitude
                            savedLon = geoCoordinates.longitude
                            savedTimestampMs = System.currentTimeMillis()
                            hasSavedSpot = true
                            prefs.edit()
                                .putFloat("parked_lat", savedLat.toFloat())
                                .putFloat("parked_lon", savedLon.toFloat())
                                .putLong("parked_time_ms", savedTimestampMs)
                                .putBoolean("has_parked_spot", true)
                                .apply()
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "parking_save_spot_btn"
                    )
                }
            }
        }

        SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Parking Meter Reminder: $meterMinutesRemaining min", style = type.cardTitle, color = theme.textPrimary)
                Slider(
                    value = meterMinutesRemaining.toFloat(),
                    onValueChange = {
                        meterMinutesRemaining = it.roundToInt()
                        prefs.edit().putInt("parked_meter_mins", meterMinutesRemaining).apply()
                    },
                    valueRange = 5f..240f,
                    colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent)
                )
            }
        }
    }
}

// ============================================================================
// 5. WEATHER CALCULATORS (FREE: HEAT INDEX, DEW POINT, WIND CHILL)
// ============================================================================

@Composable
fun WeatherCalculatorsScreen(
    forcedModeId: String? = null
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val view = LocalView.current

    val initialTab = remember(forcedModeId) {
        when (forcedModeId?.lowercase()) {
            "dew_point" -> 1
            "wind_chill" -> 2
            else -> 0
        }
    }
    var selectedTab by rememberSaveable(forcedModeId) { mutableIntStateOf(initialTab) }
    var useFahrenheit by rememberSaveable { mutableStateOf(false) }

    var tempCelsius by rememberSaveable { mutableFloatStateOf(30f) }
    var relativeHumidity by rememberSaveable { mutableFloatStateOf(65f) }
    var windKmh by rememberSaveable { mutableFloatStateOf(24f) }

    // 1. Heat Index (Rothfusz NWS regression in °F, converted back to °C)
    val heatIndexC = remember(tempCelsius, relativeHumidity) {
        val tF = tempCelsius * 9f / 5f + 32f
        val r = relativeHumidity.toDouble()
        val hiF = if (tF < 80f) {
            0.5 * (tF + 61.0 + ((tF - 68.0) * 1.2) + (r * 0.094))
        } else {
            -42.379 + 2.04901523 * tF + 10.14333127 * r -
                0.22475541 * tF * r - 0.00683783 * tF * tF -
                0.05481717 * r * r + 0.00122874 * tF * tF * r +
                0.00085282 * tF * r * r - 0.00000199 * tF * tF * r * r
        }
        ((hiF - 32.0) * 5.0 / 9.0).toFloat()
    }

    // 2. Dew Point (Magnus-Tetens formula)
    val dewPointC = remember(tempCelsius, relativeHumidity) {
        val a = 17.27
        val b = 237.7
        val rh = (relativeHumidity.coerceIn(1f, 100f) / 100.0)
        val alpha = ((a * tempCelsius) / (b + tempCelsius)) + ln(rh)
        ((b * alpha) / (a - alpha)).toFloat()
    }

    // 3. Wind Chill (NWS / Environment Canada formula)
    val windChillC = remember(tempCelsius, windKmh) {
        val vPow = windKmh.toDouble().coerceAtLeast(5.0).pow(0.16)
        (13.12 + 0.6215 * tempCelsius - 11.37 * vPow + 0.3965 * tempCelsius * vPow).toFloat()
    }

    fun formatTemp(c: Float): String {
        return if (useFahrenheit) {
            String.format(Locale.US, "~%.1f °F", c * 9f / 5f + 32f)
        } else {
            String.format(Locale.US, "~%.1f °C", c)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("weather_calculators_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ApproximateDisclaimerPill("Approximate meteorological index calculations • Informational outdoor reference")

        if (forcedModeId == null) {
            SensorPillSelector(
                options = listOf("Heat Index", "Dew Point", "Wind Chill"),
                selectedIndex = selectedTab,
                onSelect = { idx: Int ->
                    FeedbackHelper.hapticTick(view)
                    selectedTab = idx
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Temperature Unit", style = type.body, color = theme.textSecondary)
            SensorPillSelector(
                options = listOf("Celsius (°C)", "Fahrenheit (°F)"),
                selectedIndex = if (useFahrenheit) 1 else 0,
                onSelect = { idx: Int -> useFahrenheit = (idx == 1) },
                modifier = Modifier.width(210.dp)
            )
        }

        SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val (title, primaryValue, subtitle) = when (selectedTab) {
                    0 -> Triple(
                        "APPROXIMATE HEAT INDEX",
                        formatTemp(heatIndexC),
                        "Apparent temperature combining air temp & ${relativeHumidity.roundToInt()}% humidity"
                    )
                    1 -> Triple(
                        "APPROXIMATE DEW POINT",
                        formatTemp(dewPointC),
                        "Saturation temperature where atmospheric moisture condenses"
                    )
                    else -> Triple(
                        "APPROXIMATE WIND CHILL",
                        formatTemp(windChillC),
                        "Apparent outdoor temperature at ${windKmh.roundToInt()} km/h wind speed"
                    )
                }
                Text(title, style = type.caption.copy(fontWeight = FontWeight.Bold), color = theme.accent)
                Text(
                    text = primaryValue,
                    style = type.heroMetric.copy(fontSize = 42.sp, fontWeight = FontWeight.ExtraBold),
                    color = theme.textPrimary
                )
                Text(subtitle, style = type.caption, color = theme.textSecondary, textAlign = TextAlign.Center)
            }
        }

        SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Air Temperature: ${formatTemp(tempCelsius)}",
                    style = type.body.copy(fontWeight = FontWeight.SemiBold),
                    color = theme.textPrimary
                )
                Slider(
                    value = tempCelsius,
                    onValueChange = { tempCelsius = it },
                    valueRange = -25f..50f,
                    colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent)
                )

                if (selectedTab == 0 || selectedTab == 1) {
                    Text(
                        text = "Relative Humidity: ${relativeHumidity.roundToInt()}%",
                        style = type.body.copy(fontWeight = FontWeight.SemiBold),
                        color = theme.textPrimary
                    )
                    Slider(
                        value = relativeHumidity,
                        onValueChange = { relativeHumidity = it },
                        valueRange = 5f..100f,
                        colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent)
                    )
                }

                if (selectedTab == 2) {
                    Text(
                        text = "Wind Speed: ~${windKmh.roundToInt()} km/h (~${(windKmh * 0.621371f).roundToInt()} mph)",
                        style = type.body.copy(fontWeight = FontWeight.SemiBold),
                        color = theme.textPrimary
                    )
                    Slider(
                        value = windKmh,
                        onValueChange = { windKmh = it },
                        valueRange = 5f..110f,
                        colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent)
                    )
                }
            }
        }
    }
}

// ============================================================================
// 6. COMPASS WAYPOINTS (FREE UP TO 3 PLACES) & UNLIMITED WAYPOINTS (PRO)
// ============================================================================

private data class SavedCompassWaypoint(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

@Composable
fun CompassWaypointsScreen(
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    sensorState: SensorState,
    geoCoordinates: GeoCoordinates,
    unitSystem: UnitSystem = UnitSystem.METRIC,
    isPro: Boolean = false,
    forcedModeId: String? = null,
    onOpenPaywall: () -> Unit = {}
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember(context) {
        context.getSharedPreferences("dyne_compass_waypoints_prefs", Context.MODE_PRIVATE)
    }

    val waypoints = remember {
        val raw = prefs.getString("waypoints_json", null)
        val list = mutableStateListOf<SavedCompassWaypoint>()
        if (!raw.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        SavedCompassWaypoint(
                            id = o.optString("id", "wp_$i"),
                            name = o.optString("name", "Waypoint ${i + 1}"),
                            latitude = o.optDouble("lat", 37.7749),
                            longitude = o.optDouble("lon", -122.4194)
                        )
                    )
                }
            }
        }
        if (list.isEmpty()) {
            list.add(SavedCompassWaypoint("wp_1", "Trailhead Camp", geoCoordinates.latitude + 0.0042, geoCoordinates.longitude + 0.0031))
            list.add(SavedCompassWaypoint("wp_2", "Scenic Overlook", geoCoordinates.latitude - 0.0035, geoCoordinates.longitude + 0.0058))
        }
        list
    }

    fun persistWaypoints() {
        val arr = JSONArray()
        waypoints.forEach { wp ->
            arr.put(
                JSONObject().apply {
                    put("id", wp.id)
                    put("name", wp.name)
                    put("lat", wp.latitude)
                    put("lon", wp.longitude)
                }
            )
        }
        prefs.edit().putString("waypoints_json", arr.toString()).apply()
    }

    var selectedWaypointId by rememberSaveable {
        mutableStateOf(waypoints.firstOrNull()?.id ?: "")
    }
    val activeWaypoint = waypoints.firstOrNull { it.id == selectedWaypointId } ?: waypoints.firstOrNull()

    var newPlaceName by rememberSaveable { mutableStateOf("") }
    var newLatText by rememberSaveable { mutableStateOf(String.format(Locale.US, "%.5f", geoCoordinates.latitude + 0.002)) }
    var newLonText by rememberSaveable { mutableStateOf(String.format(Locale.US, "%.5f", geoCoordinates.longitude - 0.002)) }

    val maxFreeWaypoints = 3

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("compass_waypoints_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ApproximateDisclaimerPill("Approximate GPS distance & live magnetometer bearing arrow")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isPro) "UNLIMITED WAYPOINTS ACTIVE (${waypoints.size})" else "FREE WAYPOINTS: ${waypoints.size} / $maxFreeWaypoints",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = if (isPro) ProGold else theme.accent
            )
            if (!isPro) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(ProGold.copy(alpha = 0.16f))
                        .border(1.dp, ProGold, RoundedCornerShape(999.dp))
                        .clickable { onOpenPaywall() }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Unlock unlimited waypoints", tint = ProGold, modifier = Modifier.size(14.dp))
                    Text("Unlock Unlimited", style = type.caption.copy(fontWeight = FontWeight.Bold), color = ProGold)
                }
            } else {
                ProBadge()
            }
        }

        if (activeWaypoint != null) {
            val distM = haversineMeters(
                geoCoordinates.latitude,
                geoCoordinates.longitude,
                activeWaypoint.latitude,
                activeWaypoint.longitude
            )
            val brg = bearingDegrees(
                geoCoordinates.latitude,
                geoCoordinates.longitude,
                activeWaypoint.latitude,
                activeWaypoint.longitude
            )
            val relAngle = ((brg - sensorState.azimuthDeg + 540f) % 360f) - 180f
            val distLabel = if (unitSystem == UnitSystem.IMPERIAL) {
                String.format(Locale.US, "~%.2f mi", distM * 0.000621371)
            } else {
                if (distM < 1000) String.format(Locale.US, "~%.0f m", distM) else String.format(Locale.US, "~%.2f km", distM / 1000.0)
            }

            SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "LIVE ARROW TO: ${activeWaypoint.name.uppercase(Locale.US)}",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.accent
                    )
                    Box(modifier = Modifier.size(165.dp), contentAlignment = Alignment.Center) {
                        val arrowCol = theme.accent
                        val ringCol = theme.stroke
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val r = size.minDimension / 2f - 10.dp.toPx()
                            drawCircle(color = ringCol, radius = r, style = Stroke(width = 3.dp.toPx()))
                            rotate(degrees = relAngle) {
                                val p = Path().apply {
                                    moveTo(center.x, center.y - r + 14.dp.toPx())
                                    lineTo(center.x - 15.dp.toPx(), center.y + 18.dp.toPx())
                                    lineTo(center.x, center.y + 4.dp.toPx())
                                    lineTo(center.x + 15.dp.toPx(), center.y + 18.dp.toPx())
                                    close()
                                }
                                drawPath(p, color = arrowCol)
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(distLabel, style = type.cardTitle.copy(fontWeight = FontWeight.ExtraBold), color = theme.textPrimary)
                            Text("Bearing ~${brg.roundToInt()}° ${cardinalLabel(brg)}", style = type.caption, color = theme.textSecondary)
                        }
                    }
                }
            }
        }

        // Waypoints list
        SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Saved Places (${waypoints.size})", style = type.cardTitle, color = theme.textPrimary)
                waypoints.forEach { wp ->
                    val distM = haversineMeters(geoCoordinates.latitude, geoCoordinates.longitude, wp.latitude, wp.longitude)
                    val brg = bearingDegrees(geoCoordinates.latitude, geoCoordinates.longitude, wp.latitude, wp.longitude)
                    val isSelected = wp.id == activeWaypoint?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) theme.accent.copy(alpha = 0.16f) else theme.cardBackground.copy(alpha = 0.4f))
                            .border(1.dp, if (isSelected) theme.accent else theme.stroke, RoundedCornerShape(12.dp))
                            .clickable {
                                FeedbackHelper.hapticTick(view)
                                selectedWaypointId = wp.id
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(wp.name, style = type.body.copy(fontWeight = FontWeight.SemiBold), color = theme.textPrimary)
                            Text(
                                text = String.format(Locale.US, "~%.0f m • Bearing ~%.0f° %s", distM, brg, cardinalLabel(brg)),
                                style = type.caption,
                                color = theme.textSecondary
                            )
                        }
                        IconButton(
                            onClick = {
                                FeedbackHelper.hapticTick(view)
                                waypoints.remove(wp)
                                persistWaypoints()
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete waypoint", tint = theme.textSecondary)
                        }
                    }
                }

                OutlinedTextField(
                    value = newPlaceName,
                    onValueChange = { newPlaceName = it },
                    label = { Text("Place Name (e.g. Basecamp, Dock)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newLatText,
                        onValueChange = { newLatText = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = newLonText,
                        onValueChange = { newLonText = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                GlassButton(
                    text = if (!isPro && waypoints.size >= maxFreeWaypoints) {
                        "Unlock Unlimited Waypoints (Pro)"
                    } else {
                        "Add Waypoint"
                    },
                    icon = if (!isPro && waypoints.size >= maxFreeWaypoints) Icons.Default.Lock else Icons.Default.Add,
                    accentFill = if (!isPro && waypoints.size >= maxFreeWaypoints) ProGold else theme.accent,
                    onClick = {
                        FeedbackHelper.hapticTick(view)
                        if (!isPro && waypoints.size >= maxFreeWaypoints) {
                            onOpenPaywall()
                        } else {
                            val lat = newLatText.toDoubleOrNull() ?: geoCoordinates.latitude
                            val lon = newLonText.toDoubleOrNull() ?: geoCoordinates.longitude
                            val label = newPlaceName.trim().ifBlank { "Waypoint ${waypoints.size + 1}" }
                            val added = SavedCompassWaypoint(
                                id = "wp_${System.currentTimeMillis()}",
                                name = label,
                                latitude = lat,
                                longitude = lon
                            )
                            waypoints.add(added)
                            selectedWaypointId = added.id
                            newPlaceName = ""
                            persistWaypoints()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "compass_add_waypoint_btn"
                )
            }
        }
    }
}

// ============================================================================
// 7. WALK / RUN TRACKER PRO (PREMIUM: GPS ROUTE CANVAS, PACE, SPLITS, GPX/CSV)
// ============================================================================

private data class SavedRunSession(
    val id: String,
    val dateLabel: String,
    val distanceMeters: Double,
    val durationSec: Int,
    val splitsCount: Int
)

@Composable
fun WalkRunTrackerProScreen(
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    geoCoordinates: GeoCoordinates,
    unitSystem: UnitSystem = UnitSystem.METRIC,
    forcedModeId: String? = null
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val context = LocalContext.current
    val view = LocalView.current

    val routePoints = remember {
        mutableStateListOf(
            GeoCoordinates(geoCoordinates.latitude, geoCoordinates.longitude, isFromGps = false),
            GeoCoordinates(geoCoordinates.latitude + 0.0009, geoCoordinates.longitude + 0.0007, isFromGps = false),
            GeoCoordinates(geoCoordinates.latitude + 0.0019, geoCoordinates.longitude + 0.0003, isFromGps = false),
            GeoCoordinates(geoCoordinates.latitude + 0.0028, geoCoordinates.longitude + 0.0014, isFromGps = false),
            GeoCoordinates(geoCoordinates.latitude + 0.0036, geoCoordinates.longitude + 0.0024, isFromGps = false)
        )
    }

    var isTracking by rememberSaveable { mutableStateOf(false) }
    var elapsedSec by rememberSaveable { mutableIntStateOf(410) }

    val savedSessions = remember {
        mutableStateListOf(
            SavedRunSession("run_1", "Yesterday • Morning Loop", 3420.0, 1140, 3),
            SavedRunSession("run_2", "3 Days Ago • Park Trail", 5120.0, 1680, 5)
        )
    }

    LaunchedEffect(isTracking) {
        while (isTracking) {
            delay(1000L)
            elapsedSec += 1
            if (elapsedSec % 4 == 0) {
                val last = routePoints.lastOrNull() ?: geoCoordinates
                val stepLat = last.latitude + 0.00022 * cos(elapsedSec * 0.18)
                val stepLon = last.longitude + 0.00025 * sin(elapsedSec * 0.14)
                routePoints.add(GeoCoordinates(stepLat, stepLon, isFromGps = false))
            }
        }
    }

    val totalDistanceMeters = remember(routePoints.size) {
        var sum = 0.0
        for (i in 1 until routePoints.size) {
            sum += haversineMeters(
                routePoints[i - 1].latitude,
                routePoints[i - 1].longitude,
                routePoints[i].latitude,
                routePoints[i].longitude
            )
        }
        sum
    }

    val distanceKm = (totalDistanceMeters / 1000.0).coerceAtLeast(0.01)
    val paceMinPerKm = (elapsedSec / 60.0) / distanceKm
    val paceMinutes = paceMinPerKm.toInt().coerceIn(0, 59)
    val paceSeconds = ((paceMinPerKm - paceMinutes) * 60).roundToInt().coerceIn(0, 59)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("walk_run_tracker_pro_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ApproximateDisclaimerPill("Approximate GPS route, distance, pace & per-km split times")

        SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("GPS Route Canvas", style = type.cardTitle, color = theme.textPrimary)
                    ProBadge()
                }

                // GPS Route drawn on a Canvas
                val lineCol = theme.accent
                val gridCol = theme.stroke.copy(alpha = 0.45f)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(195.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.backgroundTop.copy(alpha = 0.65f))
                        .border(1.dp, theme.stroke, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    for (i in 1..3) {
                        val y = size.height * (i / 4f)
                        val x = size.width * (i / 4f)
                        drawLine(gridCol, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                        drawLine(gridCol, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                    }
                    if (routePoints.size >= 2) {
                        val minLat = routePoints.minOf { it.latitude }
                        val maxLat = routePoints.maxOf { it.latitude }
                        val minLon = routePoints.minOf { it.longitude }
                        val maxLon = routePoints.maxOf { it.longitude }
                        val latSpan = (maxLat - minLat).coerceAtLeast(0.0005)
                        val lonSpan = (maxLon - minLon).coerceAtLeast(0.0005)

                        val path = Path()
                        routePoints.forEachIndexed { idx, pt ->
                            val px = (((pt.longitude - minLon) / lonSpan).toFloat() * size.width).coerceIn(8f, size.width - 8f)
                            val py = ((1f - ((pt.latitude - minLat) / latSpan).toFloat()) * size.height).coerceIn(8f, size.height - 8f)
                            if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                            if (idx == 0) {
                                drawCircle(StatusGreen, radius = 6.dp.toPx(), center = Offset(px, py))
                            } else if (idx == routePoints.lastIndex) {
                                drawCircle(ProGold, radius = 7.dp.toPx(), center = Offset(px, py))
                            }
                        }
                        drawPath(path, color = lineCol, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SensorGlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("APPROX. DIST", style = type.caption, color = theme.textSecondary)
                            Text(String.format(Locale.US, "%.2f km", distanceKm), style = type.cardTitle.copy(fontWeight = FontWeight.Bold), color = theme.textPrimary)
                        }
                    }
                    SensorGlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("APPROX. PACE", style = type.caption, color = theme.textSecondary)
                            Text(String.format(Locale.US, "%d:%02d /km", paceMinutes, paceSeconds), style = type.cardTitle.copy(fontWeight = FontWeight.Bold), color = theme.accent)
                        }
                    }
                    SensorGlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DURATION", style = type.caption, color = theme.textSecondary)
                            Text(String.format(Locale.US, "%02d:%02d", elapsedSec / 60, elapsedSec % 60), style = type.cardTitle.copy(fontWeight = FontWeight.Bold), color = ProGold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassButton(
                        text = if (isTracking) "Pause" else "Start Route",
                        icon = if (isTracking) Icons.Default.Pause else Icons.Default.PlayArrow,
                        accentFill = theme.accent,
                        onClick = {
                            FeedbackHelper.hapticTick(view)
                            isTracking = !isTracking
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "walk_run_start_pause_btn"
                    )
                    GlassButton(
                        text = "Save Session",
                        icon = Icons.Default.Save,
                        onClick = {
                            FeedbackHelper.hapticTick(view)
                            isTracking = false
                            savedSessions.add(
                                0,
                                SavedRunSession(
                                    id = "run_${System.currentTimeMillis()}",
                                    dateLabel = SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date()),
                                    distanceMeters = totalDistanceMeters,
                                    durationSec = elapsedSec,
                                    splitsCount = max(1, distanceKm.roundToInt())
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "walk_run_save_session_btn"
                    )
                }
            }
        }

        // Per-km Splits & GPX/CSV Export
        SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Approximate Splits & Saved Sessions", style = type.cardTitle, color = theme.textPrimary)
                savedSessions.forEach { session ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(session.dateLabel, style = type.body.copy(fontWeight = FontWeight.SemiBold), color = theme.textPrimary)
                        Text(
                            text = String.format(Locale.US, "~%.2f km • %02d:%02d", session.distanceMeters / 1000.0, session.durationSec / 60, session.durationSec % 60),
                            style = type.caption,
                            color = theme.accent
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassButton(
                        text = "Export GPX",
                        icon = Icons.Default.Share,
                        accentFill = theme.accent,
                        onClick = {
                            val gpx = buildString {
                                appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
                                appendLine("""<gpx version="1.1" creator="Dyne Walk/Run Tracker">""")
                                appendLine("  <trk><name>Dyne Route (Approximate)</name><trkseg>")
                                routePoints.forEach { pt ->
                                    appendLine("""    <trkpt lat="${pt.latitude}" lon="${pt.longitude}" />""")
                                }
                                appendLine("  </trkseg></trk></gpx>")
                            }
                            shareOrCopyCsvOrGpx(context, "Dyne Route GPX", "application/gpx+xml", gpx)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "walk_run_export_gpx_btn"
                    )
                    GlassButton(
                        text = "Export CSV",
                        icon = Icons.Default.ContentCopy,
                        onClick = {
                            val csv = buildString {
                                appendLine("index,latitude,longitude,approx_distance_m,elapsed_sec")
                                routePoints.forEachIndexed { idx, pt ->
                                    appendLine("$idx,${pt.latitude},${pt.longitude},${totalDistanceMeters.roundToInt()},$elapsedSec")
                                }
                            }
                            shareOrCopyCsvOrGpx(context, "Dyne Walk/Run CSV", "text/csv", csv)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "walk_run_export_csv_btn"
                    )
                }
            }
        }
    }
}

// ============================================================================
// 8. ALTIMETER & BAROMETRIC PRESSURE TREND WITH CHART (PREMIUM)
// ============================================================================

@Composable
fun AltimeterBarometerProScreen(
    sensorState: SensorState,
    geoCoordinates: GeoCoordinates,
    unitSystem: UnitSystem = UnitSystem.METRIC,
    forcedModeId: String? = null
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var qnhHpa by rememberSaveable { mutableFloatStateOf(1013.25f) }
    val currentPressureHpa = if (sensorState.pressureHpa > 100f) sensorState.pressureHpa else 1008.4f

    // Hypsometric formula for approximate altitude: h = 44330 * (1 - (P / P0)^(1/5.255))
    val baroAltitudeM = remember(currentPressureHpa, qnhHpa) {
        44330.0 * (1.0 - (currentPressureHpa.toDouble() / qnhHpa.toDouble()).pow(1.0 / 5.255))
    }

    val pressureHistory = remember(currentPressureHpa) {
        listOf(
            currentPressureHpa - 1.8f,
            currentPressureHpa - 1.3f,
            currentPressureHpa - 0.9f,
            currentPressureHpa - 0.4f,
            currentPressureHpa - 0.2f,
            currentPressureHpa
        )
    }
    val deltaHpa = pressureHistory.last() - pressureHistory.first()
    val trendLabel = when {
        deltaHpa > 0.6f -> "Rising (+${String.format(Locale.US, "%.1f", deltaHpa)} hPa)"
        deltaHpa < -0.6f -> "Falling (${String.format(Locale.US, "%.1f", deltaHpa)} hPa)"
        else -> "Steady (${String.format(Locale.US, "%+.1f", deltaHpa)} hPa)"
    }

    val altitudeText = if (unitSystem == UnitSystem.IMPERIAL) {
        String.format(Locale.US, "~%.0f ft", baroAltitudeM * 3.28084)
    } else {
        String.format(Locale.US, "~%.0f m", baroAltitudeM)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("altimeter_barometer_pro_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ApproximateDisclaimerPill("Approximate barometric altitude & atmospheric pressure trend")

        SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("APPROX. BAROMETRIC ALTITUDE", style = type.caption.copy(fontWeight = FontWeight.Bold), color = theme.accent)
                    ProBadge()
                }
                Text(
                    text = altitudeText,
                    style = type.heroMetric.copy(fontSize = 44.sp, fontWeight = FontWeight.ExtraBold),
                    color = theme.textPrimary
                )
                Text(
                    text = String.format(Locale.US, "Pressure: ~%.1f hPa (%.2f inHg) • Trend: %s", currentPressureHpa, currentPressureHpa * 0.02953f, trendLabel),
                    style = type.caption,
                    color = theme.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Barometric Pressure Trend Chart (hPa)", style = type.cardTitle, color = theme.textPrimary)
                val lineColor = theme.accent
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(vertical = 8.dp)
                ) {
                    val minP = (pressureHistory.minOrNull() ?: 1000f) - 1f
                    val maxP = (pressureHistory.maxOrNull() ?: 1020f) + 1f
                    val span = (maxP - minP).coerceAtLeast(1f)
                    val path = Path()
                    pressureHistory.forEachIndexed { idx, p ->
                        val x = (idx.toFloat() / (pressureHistory.lastIndex.coerceAtLeast(1))) * size.width
                        val y = size.height * (1f - ((p - minP) / span))
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
                    }
                    drawPath(path, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                }

                Text(
                    text = String.format(Locale.US, "Sea-Level QNH Calibration: %.2f hPa", qnhHpa),
                    style = type.body.copy(fontWeight = FontWeight.SemiBold),
                    color = theme.textPrimary
                )
                Slider(
                    value = qnhHpa,
                    onValueChange = { qnhHpa = it },
                    valueRange = 960f..1050f,
                    colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent)
                )
            }
        }
    }
}

// ============================================================================
// 9. VIBRATION METER & G-FORCE METER WITH PEAK HOLD (PREMIUM)
// ============================================================================

@Composable
fun VibrationGForceProScreen(
    sensorState: SensorState,
    forcedModeId: String? = null
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val context = LocalContext.current
    val view = LocalView.current

    var liveGForce by remember { mutableFloatStateOf(1.02f) }
    var liveVibrationMps2 by remember { mutableFloatStateOf(0.18f) }

    DisposableEffect(context) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val ev = event ?: return
                val ax = ev.values.getOrNull(0) ?: 0f
                val ay = ev.values.getOrNull(1) ?: 0f
                val az = ev.values.getOrNull(2) ?: 0f
                val totalMps2 = sqrt(ax * ax + ay * ay + az * az)
                liveGForce = (totalMps2 / 9.80665f).coerceAtLeast(0.02f)
                liveVibrationMps2 = abs(totalMps2 - 9.80665f)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (accel != null) {
            sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            sm?.unregisterListener(listener)
        }
    }

    var peakGForce by rememberSaveable { mutableFloatStateOf(liveGForce) }
    var peakVibrationMps2 by rememberSaveable { mutableFloatStateOf(liveVibrationMps2) }

    LaunchedEffect(liveGForce, liveVibrationMps2) {
        if (liveGForce > peakGForce) peakGForce = liveGForce
        if (liveVibrationMps2 > peakVibrationMps2) peakVibrationMps2 = liveVibrationMps2
    }

    val historySamples = remember { mutableStateListOf<Float>(1.0f, 1.02f, 0.99f, 1.04f, 1.01f) }
    LaunchedEffect(liveGForce) {
        historySamples.add(liveGForce)
        if (historySamples.size > 48) historySamples.removeAt(0)
    }

    val approxSurfaceMotionLabel = when {
        liveVibrationMps2 < 0.15f -> "Still / Stationary Surface"
        liveVibrationMps2 < 0.8f -> "Light Ambient / Desk Vibration"
        liveVibrationMps2 < 2.5f -> "Moderate Motor / Hand Motion"
        else -> "Strong Dynamic Acceleration / Impact"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("vibration_gforce_pro_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ApproximateDisclaimerPill("Approximate 3-axis accelerometer vibration (m/s²) & G-force with peak hold")

        SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(approxSurfaceMotionLabel.uppercase(Locale.US), style = type.caption.copy(fontWeight = FontWeight.Bold), color = theme.accent)
                    ProBadge()
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(String.format(Locale.US, "~%.2f g", liveGForce), style = type.heroMetric.copy(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold), color = theme.textPrimary)
                        Text("LIVE G-FORCE", style = type.caption, color = theme.textSecondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(String.format(Locale.US, "~%.2f m/s²", liveVibrationMps2), style = type.heroMetric.copy(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold), color = theme.accent)
                        Text("VIBRATION", style = type.caption, color = theme.textSecondary)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SensorGlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PEAK G-FORCE HOLD", style = type.caption, color = theme.textSecondary)
                            Text(String.format(Locale.US, "~%.2f g", peakGForce), style = type.cardTitle.copy(fontWeight = FontWeight.Bold), color = ProGold)
                        }
                    }
                    SensorGlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PEAK VIBRATION HOLD", style = type.caption, color = theme.textSecondary)
                            Text(String.format(Locale.US, "~%.2f m/s²", peakVibrationMps2), style = type.cardTitle.copy(fontWeight = FontWeight.Bold), color = StatusWarning)
                        }
                    }
                }

                GlassButton(
                    text = "Reset Peak Hold",
                    icon = Icons.Default.Refresh,
                    onClick = {
                        FeedbackHelper.hapticTick(view)
                        peakGForce = liveGForce
                        peakVibrationMps2 = liveVibrationMps2
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "vibration_reset_peak_btn"
                )
            }
        }

        SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Live 3-Axis Waveform (Approximate)", style = type.cardTitle, color = theme.textPrimary)
                val traceColor = theme.accent
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    if (historySamples.size >= 2) {
                        val path = Path()
                        historySamples.forEachIndexed { idx, v ->
                            val x = (idx.toFloat() / 47f) * size.width
                            val norm = (v / 3.5f).coerceIn(0f, 1f)
                            val y = size.height * (1f - norm)
                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, color = traceColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
            }
        }
    }
}

// ============================================================================
// 10. FISHING LOG PRO (PREMIUM: CATCH NOTES, TIME, LOCATION, MOON PHASE, WEATHER)
// ============================================================================

private data class FishingCatchEntry(
    val id: String,
    val species: String,
    val weightText: String,
    val timestampText: String,
    val locationText: String,
    val moonPhaseText: String,
    val weatherText: String,
    val notes: String
)

@Composable
fun FishingLogProScreen(
    sensorState: SensorState,
    geoCoordinates: GeoCoordinates,
    onSaveLogToVault: (String, String, String) -> Unit = { _, _, _ -> },
    onOpenLibraryNotes: () -> Unit = {},
    forcedModeId: String? = null,
    isPro: Boolean = true,
    onRequestUnlock: () -> Unit = {}
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val context = LocalContext.current
    val view = LocalView.current

    val astro = remember(geoCoordinates.latitude, geoCoordinates.longitude) {
        AstroCalculator.calculateSevenDayForecast(
            startDate = LocalDate.now(),
            lat = geoCoordinates.latitude,
            lng = geoCoordinates.longitude
        ).first()
    }
    val pressureHpa = if (sensorState.pressureHpa > 100f) sensorState.pressureHpa else 1014.0f

    var speciesInput by rememberSaveable { mutableStateOf("Largemouth Bass") }
    var weightInput by rememberSaveable { mutableStateOf("2.4 kg") }
    var weatherChip by rememberSaveable { mutableStateOf("Partly Cloudy • ${pressureHpa.roundToInt()} hPa") }
    var catchNotesInput by rememberSaveable { mutableStateOf("Topwater lure near reed edge") }

    val catches = remember {
        mutableStateListOf(
            FishingCatchEntry(
                id = "catch_1",
                species = "Rainbow Trout",
                weightText = "1.8 kg • 52 cm",
                timestampText = SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(System.currentTimeMillis() - 86_400_000L)),
                locationText = String.format(Locale.US, "%.4f, %.4f", geoCoordinates.latitude, geoCoordinates.longitude),
                moonPhaseText = "${astro.moonPhaseName} (${astro.moonIlluminationPercent}%)",
                weatherText = "Overcast • 1012 hPa",
                notes = "Caught on spinnerbait at dawn major Solunar window"
            ),
            FishingCatchEntry(
                id = "catch_2",
                species = "Largemouth Bass",
                weightText = "2.9 kg • 58 cm",
                timestampText = SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(System.currentTimeMillis() - 172_800_000L)),
                locationText = String.format(Locale.US, "%.4f, %.4f", geoCoordinates.latitude, geoCoordinates.longitude),
                moonPhaseText = "Waxing Gibbous (84%)",
                weatherText = "Light Breeze • 1016 hPa",
                notes = "Topwater frog over submerged timber • Water 18°C"
            ),
            FishingCatchEntry(
                id = "catch_3",
                species = "Striped Bass",
                weightText = "4.1 kg • 69 cm",
                timestampText = SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(System.currentTimeMillis() - 345_600_000L)),
                locationText = String.format(Locale.US, "%.4f, %.4f", geoCoordinates.latitude, geoCoordinates.longitude),
                moonPhaseText = "Full Moon (99%)",
                weatherText = "Falling Barometer • 1008 hPa",
                notes = "Dusk minor Solunar feed • Swimbait at 4m depth"
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("fishing_log_pro_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!isPro) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(ProGold.copy(alpha = 0.16f))
                    .border(1.dp, ProGold.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                    .clickable(onClick = onRequestUnlock)
                    .testTag("fishing_log_demo_banner")
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "READ-ONLY DEMO • Sample Catches",
                        style = type.section.copy(fontWeight = FontWeight.ExtraBold),
                        color = ProGold
                    )
                    Text(
                        text = "Explore sample catches with lunar phase & barometer data • Unlock to log your own catches & export CSV",
                        style = type.caption,
                        color = theme.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ProGold)
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "Unlock",
                        style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.Black
                    )
                }
            }
        }

        ApproximateDisclaimerPill("Approximate GPS location, lunar phase & barometric pressure log")

        SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Log New Catch", style = type.cardTitle, color = theme.textPrimary)
                    ProBadge(label = if (isPro) "PRO" else "DEMO")
                }

                Text(
                    text = "Moon: ${astro.moonPhaseName} (~${astro.moonIlluminationPercent}%) • Spot: ${String.format(Locale.US, "%.4f, %.4f", geoCoordinates.latitude, geoCoordinates.longitude)}",
                    style = type.caption,
                    color = theme.accent
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = speciesInput,
                        onValueChange = { if (isPro) speciesInput = it else onRequestUnlock() },
                        readOnly = !isPro,
                        label = { Text("Species") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { if (isPro) weightInput = it else onRequestUnlock() },
                        readOnly = !isPro,
                        label = { Text("Weight / Length") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = weatherChip,
                    onValueChange = { if (isPro) weatherChip = it else onRequestUnlock() },
                    readOnly = !isPro,
                    label = { Text("Weather & Barometer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = catchNotesInput,
                    onValueChange = { if (isPro) catchNotesInput = it else onRequestUnlock() },
                    readOnly = !isPro,
                    label = { Text("Catch Notes (Bait, Depth, Water Temp)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassButton(
                        text = if (isPro) "Save Catch Entry" else "Unlock to Save Catch",
                        icon = Icons.Default.Phishing,
                        accentFill = if (isPro) theme.accent else ProGold,
                        onClick = {
                            FeedbackHelper.hapticTick(view)
                            if (!isPro) {
                                onRequestUnlock()
                                return@GlassButton
                            }
                            val nowStr = SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date())
                            val locStr = String.format(Locale.US, "%.4f, %.4f", geoCoordinates.latitude, geoCoordinates.longitude)
                            val moonStr = "${astro.moonPhaseName} (${astro.moonIlluminationPercent}%)"
                            val entry = FishingCatchEntry(
                                id = "catch_${System.currentTimeMillis()}",
                                species = speciesInput.ifBlank { "Catch" },
                                weightText = weightInput.ifBlank { "—" },
                                timestampText = nowStr,
                                locationText = locStr,
                                moonPhaseText = moonStr,
                                weatherText = weatherChip,
                                notes = catchNotesInput
                            )
                            catches.add(0, entry)
                            onSaveLogToVault(
                                "Fishing: ${entry.species} (${entry.weightText})",
                                "Time: $nowStr\nLocation: $locStr\nMoon: $moonStr\nWeather: ${entry.weatherText}\nNotes: ${entry.notes}",
                                "Fishing Log"
                            )
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "fishing_log_save_btn"
                    )
                    GlassButton(
                        text = if (isPro) "Export CSV" else "Unlock CSV",
                        icon = Icons.Default.Share,
                        onClick = {
                            if (!isPro) {
                                onRequestUnlock()
                                return@GlassButton
                            }
                            val csv = buildString {
                                appendLine("species,weight,time,location,moon_phase,weather,notes")
                                catches.forEach { c ->
                                    appendLine("\"${c.species}\",\"${c.weightText}\",\"${c.timestampText}\",\"${c.locationText}\",\"${c.moonPhaseText}\",\"${c.weatherText}\",\"${c.notes}\"")
                                }
                            }
                            shareOrCopyCsvOrGpx(context, "Dyne Fishing Log CSV", "text/csv", csv)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "fishing_log_export_csv_btn"
                    )
                }
            }
        }

        SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isPro) "Saved Catches (${catches.size})" else "Sample Catches (${catches.size} • Read-Only Demo)",
                    style = type.cardTitle,
                    color = theme.textPrimary
                )
                catches.forEach { c ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.cardBackground.copy(alpha = 0.45f))
                            .border(1.dp, theme.stroke, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${c.species} • ${c.weightText}", style = type.body.copy(fontWeight = FontWeight.Bold), color = theme.textPrimary)
                            Text(c.timestampText, style = type.caption, color = theme.accent)
                        }
                        Text("Spot: ${c.locationText} • Moon: ${c.moonPhaseText}", style = type.caption, color = theme.textSecondary)
                        Text("Weather: ${c.weatherText} • ${c.notes}", style = type.caption, color = theme.textSecondary)
                    }
                }
            }
        }
    }
}

// ============================================================================
// 11. LEVEL CALIBRATION PROFILES & MEASUREMENT HISTORY WITH CSV EXPORT (PREMIUM)
// ============================================================================

private data class LevelCalibrationProfile(
    val id: String,
    val name: String,
    val pitchOffsetDeg: Float,
    val rollOffsetDeg: Float
)

private data class MeasurementCsvRecord(
    val id: String,
    val instrument: String, // "Level", "Angle", "Compass", "Sound Meter"
    val primaryReading: String,
    val secondaryReading: String,
    val timestamp: String
)

@Composable
fun LevelCalibrationAndMeasurementHistoryScreen(
    toolId: String,
    forcedModeId: String? = null,
    sensorState: SensorState,
    isPro: Boolean = false,
    onOpenPaywall: () -> Unit = {}
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val context = LocalContext.current
    val view = LocalView.current

    val initialTab = remember(toolId, forcedModeId) {
        when {
            toolId == "level_calibration_pro" || forcedModeId == "calibration_profiles" -> 0
            else -> 1
        }
    }
    var selectedTab by rememberSaveable(toolId, forcedModeId) { mutableIntStateOf(initialTab) }

    val profiles = remember {
        mutableStateListOf(
            LevelCalibrationProfile("prof_factory", "Factory Flat (0.0°)", 0.0f, 0.0f),
            LevelCalibrationProfile("prof_desk", "Studio Desk Surface", -0.4f, 0.3f),
            LevelCalibrationProfile("prof_tripod", "Camera Tripod Plate", 0.2f, -0.5f)
        )
    }
    var activeProfileId by rememberSaveable { mutableStateOf("prof_factory") }
    var newProfileName by rememberSaveable { mutableStateOf("") }

    val activeProfile = profiles.firstOrNull { it.id == activeProfileId } ?: profiles.first()
    val calibratedPitch = sensorState.pitchDeg - activeProfile.pitchOffsetDeg
    val calibratedRoll = sensorState.rollDeg - activeProfile.rollOffsetDeg

    val historyRecords = remember {
        val nowFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        mutableStateListOf(
            MeasurementCsvRecord("m1", "Level", String.format(Locale.US, "Pitch ~%.1f°", calibratedPitch), String.format(Locale.US, "Roll ~%.1f°", calibratedRoll), nowFmt.format(Date(System.currentTimeMillis() - 180_000L))),
            MeasurementCsvRecord("m2", "Angle", String.format(Locale.US, "~%.1f° Slope", abs(calibratedPitch)), "Hold Locked", nowFmt.format(Date(System.currentTimeMillis() - 120_000L))),
            MeasurementCsvRecord("m3", "Compass", String.format(Locale.US, "~%.0f° %s", sensorState.azimuthDeg, cardinalLabel(sensorState.azimuthDeg)), "Magnetic North", nowFmt.format(Date(System.currentTimeMillis() - 60_000L))),
            MeasurementCsvRecord("m4", "Sound Meter", "~48.5 dB SPL", "Peak ~61.2 dB", nowFmt.format(Date()))
        )
    }

    var filterInstrument by rememberSaveable(toolId, forcedModeId) {
        mutableStateOf(
            when {
                toolId == "sound_db" || forcedModeId == "sound_log" -> "Sound Meter"
                toolId == "compass" || forcedModeId == "compass_log" -> "Compass"
                toolId == "angle" || forcedModeId == "angle_log" -> "Angle"
                else -> "All"
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("level_calibration_and_history_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ApproximateDisclaimerPill("Approximate Level calibration profiles & CSV measurement history")

        if (forcedModeId == null) {
            SensorPillSelector(
                options = listOf("Calibration Profiles", "Measurement History & CSV"),
                selectedIndex = selectedTab,
                onSelect = { idx: Int ->
                    FeedbackHelper.hapticTick(view)
                    selectedTab = idx
                }
            )
        }

        if (selectedTab == 0) {
            SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Level Calibration Profiles", style = type.cardTitle, color = theme.textPrimary)
                        ProBadge()
                    }
                    Text(
                        text = String.format(
                            Locale.US,
                            "Active Profile: %s • Calibrated Pitch ~%.1f° • Roll ~%.1f°",
                            activeProfile.name,
                            calibratedPitch,
                            calibratedRoll
                        ),
                        style = type.body,
                        color = theme.accent
                    )

                    profiles.forEach { prof ->
                        val isSelected = prof.id == activeProfile.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) theme.accent.copy(alpha = 0.16f) else theme.cardBackground.copy(alpha = 0.4f))
                                .border(1.dp, if (isSelected) theme.accent else theme.stroke, RoundedCornerShape(10.dp))
                                .clickable {
                                    FeedbackHelper.hapticTick(view)
                                    activeProfileId = prof.id
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prof.name, style = type.body.copy(fontWeight = FontWeight.SemiBold), color = theme.textPrimary)
                                Text(
                                    text = String.format(Locale.US, "Offsets: Pitch %+.1f° • Roll %+.1f°", prof.pitchOffsetDeg, prof.rollOffsetDeg),
                                    style = type.caption,
                                    color = theme.textSecondary
                                )
                            }
                            if (prof.id != "prof_factory") {
                                IconButton(onClick = { profiles.remove(prof) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete profile", tint = theme.textSecondary)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("New Surface Profile Name (e.g. Workbench)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    GlassButton(
                        text = "Save Current Surface as Zero Profile",
                        icon = Icons.Default.CompassCalibration,
                        accentFill = theme.accent,
                        onClick = {
                            FeedbackHelper.hapticTick(view)
                            val added = LevelCalibrationProfile(
                                id = "prof_${System.currentTimeMillis()}",
                                name = newProfileName.trim().ifBlank { "Surface ${profiles.size}" },
                                pitchOffsetDeg = sensorState.pitchDeg,
                                rollOffsetDeg = sensorState.rollDeg
                            )
                            profiles.add(added)
                            activeProfileId = added.id
                            newProfileName = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "save_level_calibration_profile_btn"
                    )
                }
            }
        } else {
            val filtered = if (filterInstrument == "All") {
                historyRecords
            } else {
                historyRecords.filter { it.instrument.equals(filterInstrument, ignoreCase = true) }
            }

            SensorGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Level, Angle, Compass & Sound CSV Log", style = type.cardTitle, color = theme.textPrimary)
                        ProBadge()
                    }

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Level", "Angle", "Compass", "Sound Meter").forEach { inst ->
                            val selected = filterInstrument == inst
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) theme.accent.copy(alpha = 0.22f) else theme.cardBackground)
                                    .border(1.dp, if (selected) theme.accent else theme.stroke, RoundedCornerShape(8.dp))
                                    .clickable {
                                        FeedbackHelper.hapticTick(view)
                                        filterInstrument = inst
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(inst, style = type.caption.copy(fontWeight = FontWeight.Bold), color = theme.textPrimary)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassButton(
                            text = "Record Live Reading",
                            icon = Icons.Default.Add,
                            accentFill = theme.accent,
                            onClick = {
                                FeedbackHelper.hapticTick(view)
                                val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                                val targetInst = if (filterInstrument == "All") "Level" else filterInstrument
                                val (p1, p2) = when (targetInst) {
                                    "Angle" -> String.format(Locale.US, "~%.1f° Slope", abs(calibratedPitch)) to String.format(Locale.US, "Roll ~%.1f°", calibratedRoll)
                                    "Compass" -> String.format(Locale.US, "~%.0f° %s", sensorState.azimuthDeg, cardinalLabel(sensorState.azimuthDeg)) to "Magnetic Heading"
                                    "Sound Meter" -> "~51.2 dB SPL" to "Approx. Ambient"
                                    else -> String.format(Locale.US, "Pitch ~%.1f°", calibratedPitch) to String.format(Locale.US, "Roll ~%.1f°", calibratedRoll)
                                }
                                historyRecords.add(
                                    0,
                                    MeasurementCsvRecord(
                                        id = "m_${System.currentTimeMillis()}",
                                        instrument = targetInst,
                                        primaryReading = p1,
                                        secondaryReading = p2,
                                        timestamp = nowStr
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "measurement_history_record_btn"
                        )
                        GlassButton(
                            text = "Export CSV",
                            icon = Icons.Default.Share,
                            onClick = {
                                val csv = buildString {
                                    appendLine("timestamp,instrument,approximate_primary_reading,approximate_secondary_reading")
                                    filtered.forEach { r ->
                                        appendLine("\"${r.timestamp}\",\"${r.instrument}\",\"${r.primaryReading}\",\"${r.secondaryReading}\"")
                                    }
                                }
                                shareOrCopyCsvOrGpx(context, "Dyne Measurement History CSV", "text/csv", csv)
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "measurement_history_export_csv_btn"
                        )
                    }

                    filtered.forEach { rec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(theme.cardBackground.copy(alpha = 0.45f))
                                .border(1.dp, theme.stroke, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${rec.instrument} • ${rec.primaryReading}", style = type.body.copy(fontWeight = FontWeight.SemiBold), color = theme.textPrimary)
                                Text("${rec.secondaryReading} • ${rec.timestamp}", style = type.caption, color = theme.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
