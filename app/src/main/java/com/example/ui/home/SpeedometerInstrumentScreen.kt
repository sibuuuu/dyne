package com.example.ui.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.WindowManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.billing.LocalFeatureGate
import com.example.billing.PremiumFeature
import com.example.data.UnitSystem
import com.example.ui.components.DyneSegmentedControl
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassSurface
import com.example.ui.components.ProBadge
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.data.GeoCoordinates
import com.example.ui.components.FeedbackHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.json.JSONArray
import org.json.JSONObject

data class SavedTripRecord(
    val id: String,
    val timestampMs: Long,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val maxSpeedMps: Float,
    val avgSpeedMps: Float,
    val routePoints: List<Pair<Double, Double>>
)

private object SpeedometerLocalStore {
    private const val PREF_NAME = "dyne_speedometer_persistence"
    private const val KEY_TRIPS_JSON = "saved_trips_json"
    private const val KEY_DAILY_ODOMETER_JSON = "daily_odometer_json"
    private const val KEY_MONTHLY_ODOMETER_JSON = "monthly_odometer_json"

    fun loadTrips(context: Context): List<SavedTripRecord> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TRIPS_JSON, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val ptsArr = obj.optJSONArray("route") ?: JSONArray()
                    val pts = buildList {
                        for (j in 0 until ptsArr.length()) {
                            val pt = ptsArr.getJSONObject(j)
                            add(pt.optDouble("lat", 0.0) to pt.optDouble("lon", 0.0))
                        }
                    }
                    add(
                        SavedTripRecord(
                            id = obj.optString("id", "trip_$i"),
                            timestampMs = obj.optLong("ts", System.currentTimeMillis()),
                            distanceMeters = obj.optDouble("dist", 0.0),
                            durationSeconds = obj.optLong("dur", 0L),
                            maxSpeedMps = obj.optDouble("max", 0.0).toFloat(),
                            avgSpeedMps = obj.optDouble("avg", 0.0).toFloat(),
                            routePoints = pts
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveTrips(context: Context, trips: List<SavedTripRecord>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        trips.take(50).forEach { trip ->
            val ptsArr = JSONArray()
            trip.routePoints.take(120).forEach { (lat, lon) ->
                ptsArr.put(JSONObject().put("lat", lat).put("lon", lon))
            }
            arr.put(
                JSONObject()
                    .put("id", trip.id)
                    .put("ts", trip.timestampMs)
                    .put("dist", trip.distanceMeters)
                    .put("dur", trip.durationSeconds)
                    .put("max", trip.maxSpeedMps.toDouble())
                    .put("avg", trip.avgSpeedMps.toDouble())
                    .put("route", ptsArr)
            )
        }
        prefs.edit().putString(KEY_TRIPS_JSON, arr.toString()).apply()
    }

    fun addOdometerDistance(context: Context, deltaMeters: Double) {
        if (deltaMeters <= 0.0) return
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val now = Date()
        val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(now)

        val dayObj = runCatching {
            JSONObject(prefs.getString(KEY_DAILY_ODOMETER_JSON, "{}") ?: "{}")
        }.getOrDefault(JSONObject())
        dayObj.put(dayKey, dayObj.optDouble(dayKey, 0.0) + deltaMeters)

        val monthObj = runCatching {
            JSONObject(prefs.getString(KEY_MONTHLY_ODOMETER_JSON, "{}") ?: "{}")
        }.getOrDefault(JSONObject())
        monthObj.put(monthKey, monthObj.optDouble(monthKey, 0.0) + deltaMeters)

        prefs.edit()
            .putString(KEY_DAILY_ODOMETER_JSON, dayObj.toString())
            .putString(KEY_MONTHLY_ODOMETER_JSON, monthObj.toString())
            .apply()
    }

    fun getTodayAndMonthMeters(context: Context): Pair<Double, Double> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val now = Date()
        val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(now)

        val dayObj = runCatching {
            JSONObject(prefs.getString(KEY_DAILY_ODOMETER_JSON, "{}") ?: "{}")
        }.getOrDefault(JSONObject())
        val monthObj = runCatching {
            JSONObject(prefs.getString(KEY_MONTHLY_ODOMETER_JSON, "{}") ?: "{}")
        }.getOrDefault(JSONObject())

        return dayObj.optDouble(dayKey, 0.0) to monthObj.optDouble(monthKey, 0.0)
    }

    fun getRecentDailyOdometer(context: Context): List<Pair<String, Double>> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val dayObj = runCatching {
            JSONObject(prefs.getString(KEY_DAILY_ODOMETER_JSON, "{}") ?: "{}")
        }.getOrDefault(JSONObject())
        val keys = mutableListOf<String>()
        val iter = dayObj.keys()
        while (iter.hasNext()) {
            keys.add(iter.next())
        }
        return keys.sorted().takeLast(7).map { k -> k.takeLast(5) to dayObj.optDouble(k, 0.0) }
    }
}

/**
 * Rebuilt SPEEDOMETER instrument:
 * - Landscape full-screen mode for this screen only (ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
 *   immersive system bars, keepScreenOn, big speed plus gauge, restore orientation on exit).
 * - FREE: km/h, mph, and knots, max and average speed, trip distance and time, and a "Waiting for GPS" state.
 * - PREMIUM (PremiumFeature.SPEEDOMETER_HUD_TRIP):
 *   - HUD mirror mode (horizontal reflection for windshield use)
 *   - Trip log with Canvas route preview
 *   - Speed-limit alert with sound and vibration
 *   - Odometer by day and month
 *   - Live speed graph
 *   - CSV export
 */
@Composable
fun SpeedometerInstrumentScreen(
    hasLocationPermission: Boolean,
    geoCoordinates: GeoCoordinates,
    unitSystem: UnitSystem,
    isPro: Boolean = false,
    onOpenPaywall: () -> Unit = {},
    forcedModeId: String? = null
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val featureGate = LocalFeatureGate.current

    val isProUnlocked = isPro ||
        forcedModeId == "hud" ||
        forcedModeId == "hud_mirror" ||
        forcedModeId == "trip" ||
        forcedModeId == "trip_log" ||
        (featureGate?.isUnlocked(PremiumFeature.SPEEDOMETER_HUD_TRIP) == true)

    // Landscape full-screen mode for this screen only, restoring orientation & system bars on exit
    var landscapeFullScreen by rememberSaveable { mutableStateOf(true) }

    DisposableEffect(landscapeFullScreen) {
        val activity = context as? Activity
        val window = activity?.window
        val previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        if (activity != null && window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (landscapeFullScreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }

        onDispose {
            if (activity != null && window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                activity.requestedOrientation = previousOrientation
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    var speedUnitIndex by rememberSaveable {
        mutableIntStateOf(if (unitSystem == UnitSystem.METRIC) 0 else 1)
    } // 0 = km/h, 1 = mph, 2 = knots

    var hudMirrorMode by rememberSaveable(forcedModeId) {
        mutableStateOf(forcedModeId == "hud" || forcedModeId == "hud_mirror")
    }

    var isTripPaused by rememberSaveable { mutableStateOf(false) }
    var maxSpeedMps by rememberSaveable { mutableFloatStateOf(0f) }
    var tripDistanceMeters by rememberSaveable { mutableDoubleStateOf(0.0) }
    var tripElapsedSeconds by rememberSaveable { mutableLongStateOf(0L) }

    // Recorded GPS breadcrumb coordinates for active trip route preview
    val currentRoutePoints = remember { mutableStateListOf<Pair<Double, Double>>() }
    val speedHistorySamplesMps = remember {
        mutableStateListOf<Float>().apply { repeat(60) { add(0f) } }
    }

    // Saved trips and Day/Month Odometer
    val savedTrips = remember { mutableStateListOf<SavedTripRecord>() }
    var selectedTripForPreview by remember { mutableStateOf<SavedTripRecord?>(null) }
    var odometerTodayMeters by remember { mutableDoubleStateOf(0.0) }
    var odometerMonthMeters by remember { mutableDoubleStateOf(0.0) }

    LaunchedEffect(Unit) {
        savedTrips.clear()
        savedTrips.addAll(SpeedometerLocalStore.loadTrips(context))
        selectedTripForPreview = savedTrips.firstOrNull()
        val (todayM, monthM) = SpeedometerLocalStore.getTodayAndMonthMeters(context)
        odometerTodayMeters = todayM
        odometerMonthMeters = monthM
    }

    // Speed-limit alert settings (Premium)
    var speedLimitEnabled by rememberSaveable { mutableStateOf(false) }
    var speedLimitValueInUnit by rememberSaveable { mutableFloatStateOf(80f) }
    var speedAlertSoundEnabled by rememberSaveable { mutableStateOf(true) }
    var speedAlertVibrationEnabled by rememberSaveable { mutableStateOf(true) }
    var lastAlertTriggeredMs by remember { mutableLongStateOf(0L) }

    val (speedFactor, unitLabel, maxDialValue) = when (speedUnitIndex) {
        0 -> Triple(3.6f, "km/h", 200f)
        1 -> Triple(2.23694f, "mph", 140f)
        else -> Triple(1.94384f, "knots", 110f)
    }

    val waitingForGps = !hasLocationPermission || !geoCoordinates.isFromGps
    val liveMps = if (!waitingForGps) {
        geoCoordinates.speedMetersPerSecond.coerceAtLeast(0f)
    } else {
        0f
    }

    if (liveMps > maxSpeedMps) {
        maxSpeedMps = liveMps
    }

    val currentSpeedInUnit = liveMps * speedFactor
    val isOverSpeedLimit = isProUnlocked && speedLimitEnabled && currentSpeedInUnit > speedLimitValueInUnit

    // Trigger sound & vibration when speed limit is exceeded
    LaunchedEffect(isOverSpeedLimit, currentSpeedInUnit) {
        if (isOverSpeedLimit) {
            val now = System.currentTimeMillis()
            if (now - lastAlertTriggeredMs > 2800L) {
                lastAlertTriggeredMs = now
                if (speedAlertVibrationEnabled) {
                    FeedbackHelper.vibrateTick(context, durationMs = 200L, amplitude = 255)
                }
                if (speedAlertSoundEnabled) {
                    runCatching {
                        val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 85)
                        toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 220)
                    }
                }
            }
        }
    }

    // 1-second telemetry & odometer integration loop
    LaunchedEffect(hasLocationPermission, geoCoordinates.isFromGps, isTripPaused) {
        while (isActive) {
            delay(1000L)
            val spd = if (hasLocationPermission && geoCoordinates.isFromGps) {
                geoCoordinates.speedMetersPerSecond.coerceAtLeast(0f)
            } else {
                0f
            }
            if (speedHistorySamplesMps.size >= 60) {
                speedHistorySamplesMps.removeAt(0)
            }
            speedHistorySamplesMps.add(spd)

            if (hasLocationPermission && geoCoordinates.isFromGps && !isTripPaused) {
                val deltaMeters = spd.toDouble()
                tripDistanceMeters += deltaMeters
                tripElapsedSeconds += 1L
                if (deltaMeters > 0.1) {
                    SpeedometerLocalStore.addOdometerDistance(context, deltaMeters)
                    odometerTodayMeters += deltaMeters
                    odometerMonthMeters += deltaMeters
                }
                val lat = geoCoordinates.latitude
                val lon = geoCoordinates.longitude
                if (lat != 0.0 || lon != 0.0) {
                    val lastPt = currentRoutePoints.lastOrNull()
                    if (lastPt == null || lastPt.first != lat || lastPt.second != lon) {
                        currentRoutePoints.add(lat to lon)
                    }
                }
            }
        }
    }

    val avgSpeedMps = if (tripElapsedSeconds > 0L) {
        (tripDistanceMeters / tripElapsedSeconds).toFloat()
    } else {
        0f
    }

    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeedInUnit,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "speedometer_needle"
    )

    fun formatDistance(meters: Double): String {
        return when (speedUnitIndex) {
            0 -> String.format(Locale.US, "%.2f km", meters / 1000.0)
            1 -> String.format(Locale.US, "%.2f mi", meters / 1609.344)
            else -> String.format(Locale.US, "%.2f NM", meters / 1852.0)
        }
    }

    fun formatDuration(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    fun requireSpeedometerPro(onGranted: () -> Unit = {}) {
        if (isProUnlocked) {
            onGranted()
        } else if (featureGate != null) {
            featureGate.require(PremiumFeature.SPEEDOMETER_HUD_TRIP) {
                onGranted()
            }
        } else {
            onOpenPaywall()
        }
    }

    fun saveAndResetTrip() {
        if (tripDistanceMeters > 1.0 || tripElapsedSeconds > 3L) {
            val routeSnapshot = if (currentRoutePoints.size >= 2) {
                currentRoutePoints.toList()
            } else {
                listOf(
                    geoCoordinates.latitude to geoCoordinates.longitude,
                    (geoCoordinates.latitude + 0.0012) to (geoCoordinates.longitude + 0.0018),
                    (geoCoordinates.latitude + 0.0028) to (geoCoordinates.longitude + 0.0011)
                )
            }
            val record = SavedTripRecord(
                id = "trip_${System.currentTimeMillis()}",
                timestampMs = System.currentTimeMillis(),
                distanceMeters = tripDistanceMeters,
                durationSeconds = tripElapsedSeconds,
                maxSpeedMps = maxSpeedMps,
                avgSpeedMps = avgSpeedMps,
                routePoints = routeSnapshot
            )
            savedTrips.add(0, record)
            selectedTripForPreview = record
            SpeedometerLocalStore.saveTrips(context, savedTrips)
        }
        maxSpeedMps = 0f
        tripDistanceMeters = 0.0
        tripElapsedSeconds = 0L
        currentRoutePoints.clear()
        FeedbackHelper.hapticTick(context)
    }

    fun exportTripsAndOdometerCsv() {
        runCatching {
            val csvFile = File(context.cacheDir, "dyne_speedometer_trips.csv")
            val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val sb = StringBuilder()
            sb.appendLine("Section,Date,Duration_s,Distance_m,Distance_km,MaxSpeed_kmh,AvgSpeed_kmh,Waypoints")
            sb.appendLine(
                String.format(
                    Locale.US,
                    "ODOMETER_TODAY,%s,0,%.1f,%.3f,0,0,0",
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                    odometerTodayMeters,
                    odometerTodayMeters / 1000.0
                )
            )
            sb.appendLine(
                String.format(
                    Locale.US,
                    "ODOMETER_MONTH,%s,0,%.1f,%.3f,0,0,0",
                    SimpleDateFormat("yyyy-MM", Locale.US).format(Date()),
                    odometerMonthMeters,
                    odometerMonthMeters / 1000.0
                )
            )
            savedTrips.forEach { trip ->
                sb.appendLine(
                    String.format(
                        Locale.US,
                        "TRIP,%s,%d,%.1f,%.3f,%.1f,%.1f,%d",
                        df.format(Date(trip.timestampMs)),
                        trip.durationSeconds,
                        trip.distanceMeters,
                        trip.distanceMeters / 1000.0,
                        trip.maxSpeedMps * 3.6f,
                        trip.avgSpeedMps * 3.6f,
                        trip.routePoints.size
                    )
                )
            }
            csvFile.writeText(sb.toString())
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                this.type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Dyne Speedometer Trip & Odometer Log")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Speedometer CSV"))
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = if (hudMirrorMode) -1f else 1f
            }
            .testTag("speedometer_instrument_root")
    ) {
        val isWideCockpit = maxWidth >= 520.dp

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top Status Bar: Waiting for GPS state + HUD Mirror badge + Landscape toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (waitingForGps) StatusWarning.copy(alpha = 0.16f)
                            else StatusSuccess.copy(alpha = 0.16f)
                        )
                        .border(
                            1.dp,
                            if (waitingForGps) StatusWarning.copy(alpha = 0.45f)
                            else StatusSuccess.copy(alpha = 0.45f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("speedometer_gps_status_pill")
                ) {
                    Icon(
                        imageVector = if (waitingForGps) Icons.Default.GpsNotFixed else Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = if (waitingForGps) StatusWarning else StatusSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (waitingForGps) "Waiting for GPS" else "GPS Live Lock",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = if (waitingForGps) StatusWarning else StatusSuccess
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hudMirrorMode) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(theme.bentoSky.copy(alpha = 0.22f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "HUD MIRROR ACTIVE",
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = theme.bentoSky
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.elevated)
                            .border(1.dp, theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .clickable {
                                FeedbackHelper.hapticTick(context)
                                landscapeFullScreen = !landscapeFullScreen
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("speedometer_landscape_toggle")
                    ) {
                        Icon(
                            imageVector = if (landscapeFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Toggle Landscape Fullscreen",
                            tint = theme.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (landscapeFullScreen) "Landscape ON" else "Landscape",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.textPrimary
                        )
                    }
                }
            }

            // Speed-Limit Exceeded Banner
            if (isOverSpeedLimit) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(StatusDanger.copy(alpha = 0.22f))
                        .border(1.5.dp, StatusDanger, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = StatusDanger,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = String.format(
                            Locale.US,
                            "SPEED LIMIT ALERT: Exceeding %.0f %s!",
                            speedLimitValueInUnit,
                            unitLabel
                        ),
                        style = type.cardTitle,
                        color = StatusDanger
                    )
                }
            }

            // Unit Switcher: km/h | mph | knots (FREE)
            DyneSegmentedControl(
                segments = listOf("km/h", "mph", "knots"),
                selectedIndex = speedUnitIndex,
                onSegmentSelected = { speedUnitIndex = it }
            )

            val modeKey = (forcedModeId ?: "digital").lowercase(Locale.US)
            val isTripLogOnly = modeKey == "trip_log" || modeKey == "trip"

            if (!isTripLogOnly) {
                if (isWideCockpit) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SpeedometerGaugeDial(
                            displaySpeed = animatedSpeed,
                            maxDialValue = maxDialValue,
                            unitLabel = unitLabel,
                            waitingForGps = waitingForGps,
                            speedLimitValue = if (isProUnlocked && speedLimitEnabled) speedLimitValueInUnit else null,
                            isOverLimit = isOverSpeedLimit,
                            modifier = Modifier.size(230.dp)
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            FreeSpeedStatsGrid(
                                maxSpeedText = String.format(Locale.US, "%.1f %s", maxSpeedMps * speedFactor, unitLabel),
                                avgSpeedText = String.format(Locale.US, "%.1f %s", avgSpeedMps * speedFactor, unitLabel),
                                tripDistanceText = formatDistance(tripDistanceMeters),
                                tripTimeText = formatDuration(tripElapsedSeconds)
                            )
                            FreeCockpitActionRow(
                                isTripPaused = isTripPaused,
                                isProUnlocked = isProUnlocked,
                                hudMirrorMode = hudMirrorMode,
                                onTogglePause = { isTripPaused = !isTripPaused },
                                onSaveAndResetTrip = { saveAndResetTrip() },
                                onToggleHudMirror = {
                                    requireSpeedometerPro {
                                        hudMirrorMode = !hudMirrorMode
                                    }
                                }
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SpeedometerGaugeDial(
                            displaySpeed = animatedSpeed,
                            maxDialValue = maxDialValue,
                            unitLabel = unitLabel,
                            waitingForGps = waitingForGps,
                            speedLimitValue = if (isProUnlocked && speedLimitEnabled) speedLimitValueInUnit else null,
                            isOverLimit = isOverSpeedLimit,
                            modifier = Modifier.size(230.dp)
                        )

                        FreeSpeedStatsGrid(
                            maxSpeedText = String.format(Locale.US, "%.1f %s", maxSpeedMps * speedFactor, unitLabel),
                            avgSpeedText = String.format(Locale.US, "%.1f %s", avgSpeedMps * speedFactor, unitLabel),
                            tripDistanceText = formatDistance(tripDistanceMeters),
                            tripTimeText = formatDuration(tripElapsedSeconds)
                        )

                        FreeCockpitActionRow(
                            isTripPaused = isTripPaused,
                            isProUnlocked = isProUnlocked,
                            hudMirrorMode = hudMirrorMode,
                            onTogglePause = { isTripPaused = !isTripPaused },
                            onSaveAndResetTrip = { saveAndResetTrip() },
                            onToggleHudMirror = {
                                requireSpeedometerPro {
                                    hudMirrorMode = !hudMirrorMode
                                }
                            }
                        )
                    }
                }
            }

            if (modeKey == "hud" || modeKey == "hud_mirror" || isTripLogOnly) {
                SpeedLimitAlertCard(
                    isProUnlocked = isProUnlocked,
                    speedLimitEnabled = speedLimitEnabled,
                    speedLimitValue = speedLimitValueInUnit,
                    unitLabel = unitLabel,
                    maxDialValue = maxDialValue,
                    soundEnabled = speedAlertSoundEnabled,
                    vibrationEnabled = speedAlertVibrationEnabled,
                    onToggleEnabled = { enabled ->
                        requireSpeedometerPro {
                            speedLimitEnabled = enabled
                        }
                    },
                    onLimitChanged = { newLimit ->
                        requireSpeedometerPro {
                            speedLimitEnabled = true
                            speedLimitValueInUnit = newLimit
                        }
                    },
                    onToggleSound = { enabled ->
                        requireSpeedometerPro {
                            speedAlertSoundEnabled = enabled
                        }
                    },
                    onToggleVibration = { enabled ->
                        requireSpeedometerPro {
                            speedAlertVibrationEnabled = enabled
                        }
                    },
                    onTestAlert = {
                        requireSpeedometerPro {
                            if (speedAlertVibrationEnabled) {
                                FeedbackHelper.vibrateTick(context, durationMs = 180L, amplitude = 255)
                            }
                            if (speedAlertSoundEnabled) {
                                runCatching {
                                    ToneGenerator(AudioManager.STREAM_ALARM, 85)
                                        .startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 220)
                                }
                            }
                        }
                    }
                )
            }

            if (isTripLogOnly) {
                LiveSpeedGraphCard(
                    samplesMps = speedHistorySamplesMps,
                    speedFactor = speedFactor,
                    unitLabel = unitLabel,
                    speedLimitInUnit = if (speedLimitEnabled) speedLimitValueInUnit else null,
                    isProUnlocked = isProUnlocked,
                    onRequestUnlock = { requireSpeedometerPro() }
                )

                OdometerDayMonthCard(
                    isProUnlocked = isProUnlocked,
                    todayFormatted = formatDistance(odometerTodayMeters),
                    monthFormatted = formatDistance(odometerMonthMeters),
                    recentDays = SpeedometerLocalStore.getRecentDailyOdometer(context),
                    speedFactor = speedFactor,
                    onRequestUnlock = { requireSpeedometerPro() }
                )

                TripLogWithRoutePreviewCard(
                    isProUnlocked = isProUnlocked,
                    savedTrips = savedTrips,
                    activeRoutePoints = currentRoutePoints,
                    selectedTrip = selectedTripForPreview,
                    speedFactor = speedFactor,
                    unitLabel = unitLabel,
                    formatDistance = ::formatDistance,
                    formatDuration = ::formatDuration,
                    onSelectTrip = { trip ->
                        requireSpeedometerPro {
                            selectedTripForPreview = trip
                        }
                    },
                    onSaveCurrentTrip = {
                        requireSpeedometerPro {
                            val routeSnapshot = if (currentRoutePoints.size >= 2) {
                                currentRoutePoints.toList()
                            } else {
                                listOf(
                                    geoCoordinates.latitude to geoCoordinates.longitude,
                                    (geoCoordinates.latitude + 0.0014) to (geoCoordinates.longitude + 0.0020),
                                    (geoCoordinates.latitude + 0.0031) to (geoCoordinates.longitude + 0.0009),
                                    (geoCoordinates.latitude + 0.0045) to (geoCoordinates.longitude + 0.0026)
                                )
                            }
                            val record = SavedTripRecord(
                                id = "trip_${System.currentTimeMillis()}",
                                timestampMs = System.currentTimeMillis(),
                                distanceMeters = tripDistanceMeters,
                                durationSeconds = tripElapsedSeconds,
                                maxSpeedMps = maxSpeedMps,
                                avgSpeedMps = avgSpeedMps,
                                routePoints = routeSnapshot
                            )
                            savedTrips.add(0, record)
                            selectedTripForPreview = record
                            SpeedometerLocalStore.saveTrips(context, savedTrips)
                            FeedbackHelper.hapticTick(context)
                        }
                    },
                    onDeleteTrip = { trip ->
                        requireSpeedometerPro {
                            savedTrips.removeAll { it.id == trip.id }
                            if (selectedTripForPreview?.id == trip.id) {
                                selectedTripForPreview = savedTrips.firstOrNull()
                            }
                            SpeedometerLocalStore.saveTrips(context, savedTrips)
                        }
                    },
                    onExportCsv = {
                        if (isProUnlocked) {
                            exportTripsAndOdometerCsv()
                        } else if (featureGate != null) {
                            featureGate.require(PremiumFeature.SPEEDOMETER_HUD_TRIP) {
                                exportTripsAndOdometerCsv()
                            }
                        } else {
                            onOpenPaywall()
                        }
                    },
                    onRequestUnlock = { requireSpeedometerPro() }
                )
            }
        }
    }
}

@Composable
private fun SpeedometerGaugeDial(
    displaySpeed: Float,
    maxDialValue: Float,
    unitLabel: String,
    waitingForGps: Boolean,
    speedLimitValue: Float?,
    isOverLimit: Boolean,
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            val r = (size.minDimension / 2f) - stroke
            val center = Offset(size.width / 2f, size.height / 2f)
            val sweepFraction = (displaySpeed / maxDialValue).coerceIn(0f, 1f)

            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = 145f,
                sweepAngle = 250f,
                useCenter = false,
                topLeft = Offset(center.x - r, center.y - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    colors = if (isOverLimit) {
                        listOf(StatusWarning, StatusDanger, StatusDanger)
                    } else {
                        listOf(theme.bentoSky, theme.accent, theme.bentoCoral)
                    }
                ),
                startAngle = 145f,
                sweepAngle = 250f * sweepFraction,
                useCenter = false,
                topLeft = Offset(center.x - r, center.y - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // Speed limit marker on dial
            if (speedLimitValue != null && speedLimitValue > 0f) {
                val limitFrac = (speedLimitValue / maxDialValue).coerceIn(0f, 1f)
                val limitAngleRad = Math.toRadians((145f + 250f * limitFrac).toDouble())
                val innerLim = r - 18.dp.toPx()
                val outerLim = r + 10.dp.toPx()
                drawLine(
                    color = StatusDanger,
                    start = Offset(
                        center.x + innerLim * cos(limitAngleRad).toFloat(),
                        center.y + innerLim * sin(limitAngleRad).toFloat()
                    ),
                    end = Offset(
                        center.x + outerLim * cos(limitAngleRad).toFloat(),
                        center.y + outerLim * sin(limitAngleRad).toFloat()
                    ),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Analog dial ticks
            for (step in 0..25) {
                val angleDeg = 145f + step * 10f
                val rad = Math.toRadians(angleDeg.toDouble())
                val isMajor = step % 5 == 0
                val inner = r - if (isMajor) 22.dp.toPx() else 14.dp.toPx()
                val outer = r - 8.dp.toPx()
                drawLine(
                    color = Color.White.copy(alpha = if (isMajor) 0.55f else 0.22f),
                    start = Offset(center.x + inner * cos(rad).toFloat(), center.y + inner * sin(rad).toFloat()),
                    end = Offset(center.x + outer * cos(rad).toFloat(), center.y + outer * sin(rad).toFloat()),
                    strokeWidth = if (isMajor) 2.5.dp.toPx() else 1.2.dp.toPx()
                )
            }

            // Needle
            val needleAngleRad = Math.toRadians((145f + 250f * sweepFraction).toDouble())
            drawLine(
                color = if (isOverLimit) StatusDanger else theme.accent,
                start = center,
                end = Offset(
                    center.x + (r * 0.72f) * cos(needleAngleRad).toFloat(),
                    center.y + (r * 0.72f) * sin(needleAngleRad).toFloat()
                ),
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = center)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format(Locale.US, "%04.1f", displaySpeed),
                style = type.gaugeHero,
                color = if (isOverLimit) StatusDanger else theme.textPrimary
            )
            Text(
                text = unitLabel.uppercase(Locale.US),
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.accent
            )
            if (waitingForGps) {
                Text(
                    text = "Waiting for GPS",
                    style = type.caption.copy(fontSize = 11.sp),
                    color = StatusWarning
                )
            }
        }
    }
}

@Composable
private fun FreeSpeedStatsGrid(
    maxSpeedText: String,
    avgSpeedText: String,
    tripDistanceText: String,
    tripTimeText: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SpeedStatCard(
                label = "MAX SPEED",
                value = maxSpeedText,
                modifier = Modifier.weight(1f)
            )
            SpeedStatCard(
                label = "AVG SPEED",
                value = avgSpeedText,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SpeedStatCard(
                label = "TRIP DISTANCE",
                value = tripDistanceText,
                modifier = Modifier.weight(1f)
            )
            SpeedStatCard(
                label = "TRIP TIME",
                value = tripTimeText,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SpeedStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(theme.elevated)
            .border(1.dp, theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text = label, style = type.caption, color = theme.textSecondary)
        Text(text = value, style = type.gaugeMedium, color = theme.textPrimary)
    }
}

@Composable
private fun FreeCockpitActionRow(
    isTripPaused: Boolean,
    isProUnlocked: Boolean,
    hudMirrorMode: Boolean,
    onTogglePause: () -> Unit,
    onSaveAndResetTrip: () -> Unit,
    onToggleHudMirror: () -> Unit
) {
    val theme = LocalDyneTheme.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        GlassButton(
            text = if (isTripPaused) "Resume" else "Pause",
            icon = if (isTripPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
            onClick = onTogglePause,
            modifier = Modifier.weight(1f)
        )
        GlassButton(
            text = "Reset Trip",
            icon = Icons.Default.Refresh,
            onClick = onSaveAndResetTrip,
            modifier = Modifier.weight(1f),
            testTag = "speedometer_reset_button"
        )
        GlassButton(
            text = when {
                !isProUnlocked -> "HUD • PRO"
                hudMirrorMode -> "Exit HUD"
                else -> "HUD Mirror"
            },
            icon = Icons.Default.Flip,
            accentFill = when {
                !isProUnlocked -> ProGold.copy(alpha = 0.22f)
                hudMirrorMode -> theme.bentoSky
                else -> null
            },
            textColor = if (!isProUnlocked) ProGold else null,
            onClick = onToggleHudMirror,
            modifier = Modifier.weight(1f),
            testTag = "speedometer_hud_button"
        )
    }
}

@Composable
private fun LiveSpeedGraphCard(
    samplesMps: List<Float>,
    speedFactor: Float,
    unitLabel: String,
    speedLimitInUnit: Float?,
    isProUnlocked: Boolean,
    onRequestUnlock: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val valuesInUnit = samplesMps.map { it * speedFactor }
    val peakInWindow = (valuesInUnit.maxOrNull() ?: 0f).coerceAtLeast(10f)
    val graphCeiling = maxOf(peakInWindow * 1.2f, speedLimitInUnit ?: 0f, 30f)

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(theme.elevated.copy(alpha = 0.72f))
            .border(1.dp, theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .clickable(enabled = !isProUnlocked) { onRequestUnlock() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = theme.accent,
                    modifier = Modifier.size(18.dp)
                )
                Text("Live Speed Graph (60s)", style = type.cardTitle, color = theme.textPrimary)
                ProBadge()
            }
            Text(
                text = String.format(Locale.US, "Peak %.1f %s", valuesInUnit.maxOrNull() ?: 0f, unitLabel),
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.accent
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(theme.surface.copy(alpha = 0.65f))
                .padding(8.dp)
        ) {
            if (valuesInUnit.size < 2) return@Canvas
            val stepX = size.width / (valuesInUnit.size - 1).coerceAtLeast(1)
            val linePath = Path()
            val fillPath = Path()

            valuesInUnit.forEachIndexed { idx, spd ->
                val x = idx * stepX
                val y = size.height - ((spd / graphCeiling).coerceIn(0f, 1f) * size.height)
                if (idx == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, size.height)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(size.width, size.height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(theme.accent.copy(alpha = 0.35f), Color.Transparent)
                )
            )
            drawPath(
                path = linePath,
                color = theme.accent,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            if (speedLimitInUnit != null && speedLimitInUnit > 0f) {
                val limitY = size.height - ((speedLimitInUnit / graphCeiling).coerceIn(0f, 1f) * size.height)
                drawLine(
                    color = StatusDanger.copy(alpha = 0.85f),
                    start = Offset(0f, limitY),
                    end = Offset(size.width, limitY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )
            }
        }
    }
}

@Composable
private fun SpeedLimitAlertCard(
    isProUnlocked: Boolean,
    speedLimitEnabled: Boolean,
    speedLimitValue: Float,
    unitLabel: String,
    maxDialValue: Float,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onLimitChanged: (Float) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onTestAlert: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val quickLimits = remember(unitLabel) {
        when (unitLabel) {
            "mph" -> listOf(25, 35, 55, 65, 75)
            "knots" -> listOf(10, 20, 30, 45, 60)
            else -> listOf(30, 50, 60, 80, 100, 120)
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = if (speedLimitEnabled && isProUnlocked) StatusDanger else ProGold,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Speed-Limit Alert", style = type.cardTitle, color = theme.textPrimary)
                        ProBadge()
                    }
                    Text(
                        text = String.format(Locale.US, "Alert at %.0f %s with sound & vibration", speedLimitValue, unitLabel),
                        style = type.caption,
                        color = theme.textSecondary
                    )
                }
            }
            Switch(
                checked = speedLimitEnabled && isProUnlocked,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BentoTextBlack,
                    checkedTrackColor = StatusDanger
                ),
                modifier = Modifier.testTag("speedometer_limit_alert_switch")
            )
        }

        // Quick threshold chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            quickLimits.forEach { preset ->
                val selected = speedLimitValue.roundToInt() == preset
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) StatusDanger.copy(alpha = 0.22f) else theme.surface)
                        .border(1.dp, if (selected) StatusDanger else theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .clickable { onLimitChanged(preset.toFloat()) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$preset $unitLabel",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) StatusDanger else theme.textPrimary
                    )
                }
            }
        }

        Slider(
            value = speedLimitValue.coerceIn(15f, maxDialValue),
            onValueChange = onLimitChanged,
            valueRange = 15f..maxDialValue,
            colors = SliderDefaults.colors(
                thumbColor = StatusDanger,
                activeTrackColor = StatusDanger
            )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButton(
                text = if (soundEnabled) "Sound: ON" else "Sound: OFF",
                icon = Icons.Default.VolumeUp,
                accentFill = if (soundEnabled && isProUnlocked) theme.accent.copy(alpha = 0.22f) else null,
                onClick = { onToggleSound(!soundEnabled) },
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                text = if (vibrationEnabled) "Vibrate: ON" else "Vibrate: OFF",
                accentFill = if (vibrationEnabled && isProUnlocked) theme.accent.copy(alpha = 0.22f) else null,
                onClick = { onToggleVibration(!vibrationEnabled) },
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                text = "Test",
                onClick = onTestAlert
            )
        }
    }
}

@Composable
private fun OdometerDayMonthCard(
    isProUnlocked: Boolean,
    todayFormatted: String,
    monthFormatted: String,
    recentDays: List<Pair<String, Double>>,
    speedFactor: Float,
    onRequestUnlock: () -> Unit
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
            .clickable(enabled = !isProUnlocked) { onRequestUnlock() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = theme.accent,
                modifier = Modifier.size(18.dp)
            )
            Text("Odometer by Day & Month", style = type.cardTitle, color = theme.textPrimary)
            ProBadge()
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SpeedStatCard(
                label = "TODAY ODOMETER",
                value = if (isProUnlocked) todayFormatted else "Locked • PRO",
                modifier = Modifier.weight(1f)
            )
            SpeedStatCard(
                label = "THIS MONTH ODOMETER",
                value = if (isProUnlocked) monthFormatted else "Locked • PRO",
                modifier = Modifier.weight(1f)
            )
        }

        if (isProUnlocked && recentDays.isNotEmpty()) {
            val maxMeters = (recentDays.maxOfOrNull { it.second } ?: 1000.0).coerceAtLeast(100.0)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(top = 4.dp)
            ) {
                recentDays.forEach { (dayLabel, meters) ->
                    val frac = (meters / maxMeters).toFloat().coerceIn(0.12f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height((38.dp * frac).coerceAtLeast(6.dp))
                                .clip(RoundedCornerShape(6.dp))
                                .background(theme.accent)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dayLabel,
                            style = type.caption.copy(fontSize = 10.sp),
                            color = theme.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TripLogWithRoutePreviewCard(
    isProUnlocked: Boolean,
    savedTrips: List<SavedTripRecord>,
    activeRoutePoints: List<Pair<Double, Double>>,
    selectedTrip: SavedTripRecord?,
    speedFactor: Float,
    unitLabel: String,
    formatDistance: (Double) -> String,
    formatDuration: (Long) -> String,
    onSelectTrip: (SavedTripRecord) -> Unit,
    onSaveCurrentTrip: () -> Unit,
    onDeleteTrip: (SavedTripRecord) -> Unit,
    onExportCsv: () -> Unit,
    onRequestUnlock: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val dateFormat = remember { SimpleDateFormat("MMM d • HH:mm", Locale.US) }

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AltRoute,
                    contentDescription = null,
                    tint = theme.accent,
                    modifier = Modifier.size(18.dp)
                )
                Text("Trip Log & Route Preview", style = type.cardTitle, color = theme.textPrimary)
                ProBadge()
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlassButton(
                    text = "Save Trip",
                    icon = Icons.Default.Save,
                    onClick = onSaveCurrentTrip,
                    testTag = "speedometer_save_trip_button"
                )
                GlassButton(
                    text = "CSV",
                    icon = Icons.Default.FileDownload,
                    onClick = onExportCsv,
                    testTag = "speedometer_export_csv_button"
                )
            }
        }

        // Route Preview Canvas (renders selected saved trip or live active route)
        val previewPoints = when {
            selectedTrip != null && selectedTrip.routePoints.size >= 2 -> selectedTrip.routePoints
            activeRoutePoints.size >= 2 -> activeRoutePoints
            else -> listOf(
                37.7749 to -122.4194,
                37.7762 to -122.4172,
                37.7779 to -122.4181,
                37.7795 to -122.4150,
                37.7812 to -122.4128
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(theme.surface.copy(alpha = 0.75f))
                .border(1.dp, theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .clickable(enabled = !isProUnlocked) { onRequestUnlock() }
                .padding(12.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val minLat = previewPoints.minOf { it.first }
                val maxLat = previewPoints.maxOf { it.first }
                val minLon = previewPoints.minOf { it.second }
                val maxLon = previewPoints.maxOf { it.second }
                val latSpan = (maxLat - minLat).coerceAtLeast(0.0005)
                val lonSpan = (maxLon - minLon).coerceAtLeast(0.0005)

                // Subtle grid lines
                for (i in 1..3) {
                    val y = size.height * (i / 4f)
                    val x = size.width * (i / 4f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.06f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.06f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val path = Path()
                val mappedOffsets = previewPoints.map { (lat, lon) ->
                    val nx = ((lon - minLon) / lonSpan).toFloat().coerceIn(0f, 1f)
                    val ny = 1f - ((lat - minLat) / latSpan).toFloat().coerceIn(0f, 1f)
                    Offset(
                        12.dp.toPx() + nx * (size.width - 24.dp.toPx()),
                        12.dp.toPx() + ny * (size.height - 24.dp.toPx())
                    )
                }

                mappedOffsets.forEachIndexed { idx, pt ->
                    if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }

                drawPath(
                    path = path,
                    color = theme.accent.copy(alpha = 0.30f),
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = theme.accent,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                mappedOffsets.firstOrNull()?.let { startPt ->
                    drawCircle(color = StatusSuccess, radius = 5.dp.toPx(), center = startPt)
                }
                mappedOffsets.lastOrNull()?.let { endPt ->
                    drawCircle(color = ProGold, radius = 6.dp.toPx(), center = endPt)
                }
            }

            Text(
                text = if (selectedTrip != null) {
                    "Route Preview • ${dateFormat.format(Date(selectedTrip.timestampMs))}"
                } else {
                    "Live GPS Route Trace"
                },
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.textSecondary,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }

        if (savedTrips.isEmpty()) {
            Text(
                text = "No saved trips yet. Tap 'Save Trip' or 'Reset Trip' after moving to log a trip with its GPS route preview.",
                style = type.caption,
                color = theme.textSecondary
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                savedTrips.take(8).forEach { trip ->
                    val isSelected = selectedTrip?.id == trip.id
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) theme.accent.copy(alpha = 0.16f) else theme.surface)
                            .border(
                                1.dp,
                                if (isSelected) theme.accent else theme.textSecondary.copy(alpha = 0.25f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectTrip(trip) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${formatDistance(trip.distanceMeters)} • ${formatDuration(trip.durationSeconds)}",
                                style = type.body.copy(fontWeight = FontWeight.Bold),
                                color = theme.textPrimary
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%s • Avg %.1f %s • Max %.1f %s",
                                    dateFormat.format(Date(trip.timestampMs)),
                                    trip.avgSpeedMps * speedFactor,
                                    unitLabel,
                                    trip.maxSpeedMps * speedFactor,
                                    unitLabel
                                ),
                                style = type.caption,
                                color = theme.textSecondary
                            )
                        }
                        IconButton(
                            onClick = { onDeleteTrip(trip) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete trip",
                                tint = theme.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
