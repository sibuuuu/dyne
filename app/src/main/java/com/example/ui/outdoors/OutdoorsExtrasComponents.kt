package com.example.ui.outdoors

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.billing.FeatureGate
import com.example.billing.PremiumFeature
import com.example.data.DailyForecastItem
import com.example.data.PreferencesRepository
import com.example.data.UserPreferences
import com.example.data.WeatherSnapshot
import com.example.domain.AstroCalculator
import com.example.domain.DailyAstroReport
import com.example.service.DyneAlertsScheduler
import com.example.ui.components.GlassCard
import com.example.ui.components.ProBadge
import com.example.ui.components.ProCountdownRing
import com.example.ui.theme.LocalDyneTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt

enum class PlannerActivity(val id: String, val label: String) {
    FISHING("FISHING", "Fishing"),
    HUNTING("HUNTING", "Hunting"),
    PHOTOGRAPHY("PHOTOGRAPHY", "Photography")
}

data class RankedOutdoorDay(
    val date: LocalDate,
    val dayLabel: String,
    val report: DailyAstroReport,
    val dailyWeather: DailyForecastItem?,
    val score: Int,
    val bestWindowText: String,
    val summaryReason: String
)

fun compute7DayRankedPlan(
    activity: PlannerActivity,
    reports: List<DailyAstroReport>,
    weatherSnapshot: WeatherSnapshot?,
    timeFmt: DateTimeFormatter
): List<RankedOutdoorDay> {
    val dailyByIso = weatherSnapshot?.daily?.associateBy { it.dateIso }.orEmpty()
    val dayNameFmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)

    return reports.mapIndexed { idx, rep ->
        val w = dailyByIso[rep.date.toString()] ?: weatherSnapshot?.daily?.getOrNull(idx)
        val rainPenalty = ((w?.precipProbMaxPercent ?: 15) * 0.28).roundToInt()
        val windPenalty = (((w?.windMaxKmh ?: 12.0) - 14.0).coerceAtLeast(0.0) * 0.7).roundToInt()

        val rawScore = when (activity) {
            PlannerActivity.FISHING -> {
                rep.fishingScore - (rainPenalty / 2) - windPenalty +
                    (if (weatherSnapshot?.solunarWeatherBoost != null && idx == 0) weatherSnapshot.solunarWeatherBoost else 4)
            }
            PlannerActivity.HUNTING -> {
                rep.huntingScore - (rainPenalty / 2) - windPenalty + 3
            }
            PlannerActivity.PHOTOGRAPHY -> {
                // Clear/partly cloudy skies + golden hour + low rain probability = high photography score
                val codeBonus = when (w?.weatherCode ?: 1) {
                    0, 1, 2 -> 18
                    3 -> 6
                    else -> -10
                }
                val moonDrama = if (rep.moonIlluminationPercent in 85..100 || rep.moonIlluminationPercent <= 12) 10 else 4
                (72 + codeBonus + moonDrama - rainPenalty - (windPenalty / 2))
            }
        }.coerceIn(32, 99)

        val windowText = when (activity) {
            PlannerActivity.FISHING, PlannerActivity.HUNTING -> {
                val maj = rep.majorPeriods.firstOrNull()
                if (maj != null) {
                    "Major ${maj.startTime.format(timeFmt)}–${maj.endTime.format(timeFmt)}"
                } else {
                    "Sunrise ${rep.sunrise?.format(timeFmt) ?: "--:--"}"
                }
            }
            PlannerActivity.PHOTOGRAPHY -> {
                val eveGold = rep.eveningGoldenHourStart?.format(timeFmt) ?: "17:30"
                val sunset = rep.sunset?.format(timeFmt) ?: "18:15"
                "Golden Hr $eveGold–$sunset"
            }
        }

        val reason = buildString {
            append(rep.moonPhaseName).append(" (${rep.moonIlluminationPercent}%)")
            if (w != null) {
                append(" • ").append(w.conditionLabel)
                append(" • Wind ${w.windMaxKmh.roundToInt()} km/h")
            }
        }

        RankedOutdoorDay(
            date = rep.date,
            dayLabel = if (idx == 0) "Today (${rep.date.format(dayNameFmt)})" else rep.date.format(dayNameFmt),
            report = rep,
            dailyWeather = w,
            score = rawScore,
            bestWindowText = windowText,
            summaryReason = reason
        )
    }.sortedByDescending { it.score }
}

// ============================================================================
// 1. TIDE-FREE "BEST DAY" CARD & SHAREABLE DAY CARD IMAGE
// ============================================================================

@Composable
fun TideFreeBestDayAndShareCard(
    reports: List<DailyAstroReport>,
    selectedReport: DailyAstroReport,
    weatherSnapshot: WeatherSnapshot?,
    locationLabel: String,
    use24Hour: Boolean,
    isTempFahrenheit: Boolean,
    featureGate: FeatureGate?,
    isPro: Boolean,
    userPreferences: UserPreferences,
    onRequestPaywall: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val timeFmt = remember(use24Hour) {
        DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a", Locale.US)
    }

    val featureKey = "outdoor_planner_7day"
    val isUnlocked = isPro ||
        (featureGate?.hasAccess(featureKey, prefs = userPreferences) == true)
    val remMs = featureGate?.getRemainingMsForKey(featureKey, prefs = userPreferences) ?: 0L

    val rankedFishing = remember(reports, weatherSnapshot, timeFmt) {
        compute7DayRankedPlan(PlannerActivity.FISHING, reports, weatherSnapshot, timeFmt)
    }
    val bestDay = rankedFishing.firstOrNull()
    var isSharingImage by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "BEST DAY OF THE WEEK (TIDE-FREE)",
                        color = theme.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
                if (!isPro && remMs > 0L && featureGate != null) {
                    ProCountdownRing(
                        progress = featureGate.getCountdownProgressForKey(featureKey, prefs = userPreferences),
                        label = featureGate.formatRemainingRingBadge(remMs)
                    )
                } else if (!isPro) {
                    ProBadge()
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isUnlocked && bestDay != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFF59E0B).copy(alpha = 0.16f),
                                    theme.accent.copy(alpha = 0.12f)
                                )
                            )
                        )
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.40f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bestDay.dayLabel,
                            color = theme.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = bestDay.bestWindowText,
                            color = theme.accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${bestDay.summaryReason} • Barometer & lunar aligned",
                            color = theme.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B).copy(alpha = 0.20f))
                            .border(2.dp, Color(0xFFF59E0B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${bestDay.score}",
                                color = Color(0xFFF59E0B),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "/100",
                                color = theme.textSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (isSharingImage) return@Button
                        isSharingImage = true
                        scope.launch {
                            shareOutdoorsDayCardImage(
                                context = context,
                                report = selectedReport,
                                weatherSnapshot = weatherSnapshot,
                                locationLabel = locationLabel,
                                timeFmt = timeFmt,
                                isTempFahrenheit = isTempFahrenheit
                            )
                            isSharingImage = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.accent,
                        contentColor = theme.onAccent
                    )
                ) {
                    if (isSharingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = theme.onAccent
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Day Card Image",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share Day Card Image (PNG)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            } else {
                Text(
                    text = "See the #1 ranked tide-free day in the next 7 days (combining lunar phase, Solunar peaks, wind & barometric pressure) and export a high-res shareable day card image.",
                    color = theme.textSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (featureGate != null) {
                                featureGate.guardNavigate(featureKey, prefs = userPreferences) {}
                            } else {
                                onRequestPaywall()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color(0xFF0A0A0C)
                        )
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Unlock Best Day & Share Card", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

suspend fun shareOutdoorsDayCardImage(
    context: Context,
    report: DailyAstroReport,
    weatherSnapshot: WeatherSnapshot?,
    locationLabel: String,
    timeFmt: DateTimeFormatter,
    isTempFahrenheit: Boolean
) = withContext(Dispatchers.IO) {
    runCatching {
        val width = 1080
        val height = 1320
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bmp)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(0xFF0B101B.toInt(), 0xFF131D31.toInt(), 0xFF0A0E17.toInt()),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF172238.toInt()
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFCCFF00.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(RectF(56f, 56f, width - 56f, height - 56f), 48f, 48f, cardPaint)
        canvas.drawRoundRect(RectF(56f, 56f, width - 56f, height - 56f), 48f, 48f, borderPaint)

        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFCCFF00.toInt()
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFB0BDD0.toInt()
            textSize = 32f
        }
        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 42f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val dateStr = report.date.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.US))
        canvas.drawText("DYNE OUTDOORS • FIELD SUMMARY CARD", 108f, 140f, accentPaint)
        canvas.drawText(locationLabel.take(28), 108f, 212f, titlePaint)
        canvas.drawText("$dateStr • Approximate Ephemeris", 108f, 264f, subPaint)

        // Score boxes
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1E2D4A.toInt() }
        canvas.drawRoundRect(RectF(108f, 310f, 520f, 510f), 32f, 32f, boxPaint)
        canvas.drawRoundRect(RectF(560f, 310f, 972f, 510f), 32f, 32f, boxPaint)

        canvas.drawText("FISHING SCORE", 144f, 368f, accentPaint)
        canvas.drawText("${report.fishingScore} / 100", 144f, 450f, titlePaint)

        val goldPaint = Paint(accentPaint).apply { color = 0xFFF59E0B.toInt() }
        canvas.drawText("HUNTING SCORE", 596f, 368f, goldPaint)
        canvas.drawText("${report.huntingScore} / 100", 596f, 450f, titlePaint)

        // Moon & Sun section
        canvas.drawRoundRect(RectF(108f, 546f, 972f, 820f), 32f, 32f, boxPaint)
        canvas.drawText("MOON & GOLDEN HOUR", 144f, 604f, accentPaint)
        canvas.drawText("Moon: ${report.moonPhaseName} (${report.moonIlluminationPercent}%)", 144f, 668f, valPaint)
        val sr = report.sunrise?.format(timeFmt) ?: "--:--"
        val ss = report.sunset?.format(timeFmt) ?: "--:--"
        val gh = report.eveningGoldenHourStart?.format(timeFmt) ?: "--:--"
        canvas.drawText("Sunrise: $sr   •   Sunset: $ss", 144f, 730f, subPaint)
        canvas.drawText("Evening Golden Hour: $gh", 144f, 782f, subPaint)

        // Major windows & Weather/AQI
        canvas.drawRoundRect(RectF(108f, 856f, 972f, 1140f), 32f, 32f, boxPaint)
        canvas.drawText("MAJOR SOLUNAR WINDOWS & WEATHER", 144f, 914f, accentPaint)
        val majText = report.majorPeriods.joinToString("  |  ") {
            "${it.startTime.format(timeFmt)}–${it.endTime.format(timeFmt)}"
        }.ifBlank { "Standard diurnal cycle" }
        canvas.drawText(majText, 144f, 976f, valPaint)

        if (weatherSnapshot != null) {
            val tempVal = if (isTempFahrenheit) {
                "${((weatherSnapshot.currentTempC * 9.0 / 5.0) + 32.0).roundToInt()}°F"
            } else {
                "${weatherSnapshot.currentTempC.roundToInt()}°C"
            }
            canvas.drawText(
                "$tempVal • ${weatherSnapshot.conditionLabel} • Wind ${weatherSnapshot.windSpeedKmh.roundToInt()} km/h",
                144f,
                1040f,
                subPaint
            )
            canvas.drawText(
                "US AQI: ${weatherSnapshot.usAqi} (${weatherSnapshot.aqiCategoryLabel}) • UV Index: ${weatherSnapshot.uvIndex.roundToInt()} (${weatherSnapshot.uvCategoryLabel})",
                144f,
                1094f,
                subPaint
            )
        } else {
            canvas.drawText("Tide-free Solunar & Astronomical ephemeris calculated on-device", 144f, 1045f, subPaint)
        }

        canvas.drawText("Generated by Dyne • All measurements approximate", 108f, 1215f, subPaint)

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outFile = File(exportDir, "dyne_day_card_${report.date}.png")
        FileOutputStream(outFile).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Dyne Outdoor Day Card • ${report.date}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        withContext(Dispatchers.Main) {
            context.startActivity(Intent.createChooser(shareIntent, "Share Outdoor Day Card"))
        }
    }
}

// ============================================================================
// 2. 7-DAY OUTDOOR PLANNER (FISHING, HUNTING, PHOTOGRAPHY)
// ============================================================================

@Composable
fun OutdoorPlanner7DayCard(
    reports: List<DailyAstroReport>,
    weatherSnapshot: WeatherSnapshot?,
    use24Hour: Boolean,
    featureGate: FeatureGate?,
    isPro: Boolean,
    userPreferences: UserPreferences,
    onRequestPaywall: () -> Unit
) {
    val theme = LocalDyneTheme.current
    var selectedActivity by remember { mutableStateOf(PlannerActivity.FISHING) }
    val timeFmt = remember(use24Hour) {
        DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a", Locale.US)
    }

    val featureKey = "outdoor_planner_7day"
    val isUnlocked = isPro ||
        (featureGate?.hasAccess(featureKey, prefs = userPreferences) == true)
    val remMs = featureGate?.getRemainingMsForKey(featureKey, prefs = userPreferences) ?: 0L

    val rankedDays = remember(selectedActivity, reports, weatherSnapshot, timeFmt) {
        compute7DayRankedPlan(selectedActivity, reports, weatherSnapshot, timeFmt)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = theme.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "7-DAY OUTDOOR PLANNER",
                        color = theme.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
                if (!isPro && remMs > 0L && featureGate != null) {
                    ProCountdownRing(
                        progress = featureGate.getCountdownProgressForKey(featureKey, prefs = userPreferences),
                        label = featureGate.formatRemainingRingBadge(remMs)
                    )
                } else if (!isPro) {
                    ProBadge()
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Activity selector pills: Fishing / Hunting / Photography
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlannerActivity.entries.forEach { act ->
                    val selected = selectedActivity == act
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) theme.accent else theme.elevated)
                            .border(
                                width = 1.dp,
                                color = if (selected) theme.accent else theme.textSecondary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedActivity = act }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = act.label,
                            color = if (selected) theme.onAccent else theme.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val visibleDays = if (isUnlocked) rankedDays else rankedDays.take(2)
            visibleDays.forEachIndexed { idx, item ->
                val rankColor = when (idx) {
                    0 -> Color(0xFFF59E0B)
                    1 -> theme.accent
                    else -> theme.textSecondary
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.elevated.copy(alpha = 0.65f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(rankColor.copy(alpha = 0.18f))
                                .border(1.dp, rankColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#${idx + 1}",
                                color = rankColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.dayLabel,
                                color = theme.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.bestWindowText,
                                color = theme.accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = item.summaryReason,
                                color = theme.textSecondary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = "${item.score}/100",
                        color = rankColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (!isUnlocked) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.14f))
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .clickable {
                            if (featureGate != null) {
                                featureGate.guardNavigate(featureKey, prefs = userPreferences) {}
                            } else {
                                onRequestPaywall()
                            }
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Unlock Full 7-Day Ranking (${selectedActivity.label})",
                            color = theme.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Watch ad or use 10 coins for 1h • Dyne Premium",
                            color = theme.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    ProBadge()
                }
            }
        }
    }
}

// ============================================================================
// 3. MONTHLY SUN & MOON CALENDAR (MOON PHASES & BEST SOLUNAR DAYS)
// ============================================================================

@Composable
fun MonthlySunMoonCalendarCard(
    lat: Double,
    lng: Double,
    use24Hour: Boolean,
    featureGate: FeatureGate?,
    isPro: Boolean,
    userPreferences: UserPreferences,
    onRequestPaywall: () -> Unit
) {
    val theme = LocalDyneTheme.current
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val featureKey = "outdoor_sun_moon_calendar"
    val isUnlocked = isPro ||
        (featureGate?.hasAccess(featureKey, prefs = userPreferences) == true)
    val remMs = featureGate?.getRemainingMsForKey(featureKey, prefs = userPreferences) ?: 0L

    val monthReports = remember(currentMonth, lat, lng) {
        val daysInMonth = currentMonth.lengthOfMonth()
        (1..daysInMonth).map { day ->
            AstroCalculator.calculateDailyReport(
                currentMonth.atDay(day),
                lat,
                lng,
                ZoneId.systemDefault()
            )
        }
    }
    val selectedDayReport = remember(selectedDate, monthReports, lat, lng) {
        monthReports.firstOrNull { it.date == selectedDate }
            ?: AstroCalculator.calculateDailyReport(selectedDate, lat, lng, ZoneId.systemDefault())
    }
    val timeFmt = remember(use24Hour) {
        DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a", Locale.US)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = theme.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "MONTHLY SUN & MOON CALENDAR",
                        color = theme.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
                if (!isPro && remMs > 0L && featureGate != null) {
                    ProCountdownRing(
                        progress = featureGate.getCountdownProgressForKey(featureKey, prefs = userPreferences),
                        label = featureGate.formatRemainingRingBadge(remMs)
                    )
                } else if (!isPro) {
                    ProBadge()
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isUnlocked) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        currentMonth = currentMonth.minusMonths(1)
                        selectedDate = currentMonth.atDay(1)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Month",
                            tint = theme.textPrimary
                        )
                    }
                    Text(
                        text = "${currentMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.US)} ${currentMonth.year}",
                        color = theme.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = {
                        currentMonth = currentMonth.plusMonths(1)
                        selectedDate = currentMonth.atDay(1)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Month",
                            tint = theme.textPrimary
                        )
                    }
                }

                // Weekday headers
                val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekdays.forEach { wd ->
                        Text(
                            text = wd,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = theme.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                val firstDayOffset = currentMonth.atDay(1).dayOfWeek.value % 7
                val totalCells = firstDayOffset + monthReports.size
                val rows = (totalCells + 6) / 7

                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (c in 0 until 7) {
                            val cellIndex = r * 7 + c
                            val dayIdx = cellIndex - firstDayOffset
                            if (dayIdx in monthReports.indices) {
                                val rep = monthReports[dayIdx]
                                val isSelected = rep.date == selectedDate
                                val isBestDay = rep.fishingScore >= 82
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                isSelected -> theme.accent.copy(alpha = 0.24f)
                                                isBestDay -> Color(0xFFF59E0B).copy(alpha = 0.14f)
                                                else -> theme.elevated.copy(alpha = 0.45f)
                                            }
                                        )
                                        .border(
                                            width = if (isSelected || isBestDay) 1.dp else 0.dp,
                                            color = when {
                                                isSelected -> theme.accent
                                                isBestDay -> Color(0xFFF59E0B).copy(alpha = 0.7f)
                                                else -> Color.Transparent
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedDate = rep.date }
                                        .padding(3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${rep.date.dayOfMonth}",
                                            color = theme.textPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        MiniMoonPhaseDisc(
                                            phaseFraction = rep.moonPhaseFraction,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "${rep.fishingScore}",
                                            color = if (isBestDay) Color(0xFFF59E0B) else theme.textSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f).height(54.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Selected date summary bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.elevated.copy(alpha = 0.75f))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedDayReport.date} • ${selectedDayReport.moonPhaseName} (${selectedDayReport.moonIlluminationPercent}%)",
                            color = theme.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Solunar ${selectedDayReport.fishingScore}/100",
                            color = if (selectedDayReport.fishingScore >= 82) Color(0xFFF59E0B) else theme.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sunrise ${selectedDayReport.sunrise?.format(timeFmt) ?: "--:--"} • Sunset ${selectedDayReport.sunset?.format(timeFmt) ?: "--:--"} • Golden Hr ${selectedDayReport.eveningGoldenHourStart?.format(timeFmt) ?: "--:--"}",
                        color = theme.textSecondary,
                        fontSize = 11.sp
                    )
                }
            } else {
                Text(
                    text = "Explore any month's daily moon phases, lunar illumination %, and gold-highlighted best Solunar days.",
                    color = theme.textSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (featureGate != null) {
                            featureGate.guardNavigate(featureKey, prefs = userPreferences) {}
                        } else {
                            onRequestPaywall()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B),
                        contentColor = Color(0xFF0A0A0C)
                    )
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Unlock Monthly Sun & Moon Calendar", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun MiniMoonPhaseDisc(
    phaseFraction: Double,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = Color(0xFF283248), radius = r, center = center)
        val illum = ((1.0 - cos(phaseFraction * 2.0 * Math.PI)) / 2.0).toFloat().coerceIn(0.08f, 1f)
        drawArc(
            color = Color(0xFFFDE68A),
            startAngle = if (phaseFraction <= 0.5) -90f else 90f,
            sweepAngle = 180f * illum * 1.4f,
            useCenter = true,
            topLeft = Offset(center.x - r, center.y - r),
            size = Size(r * 2f, r * 2f)
        )
    }
}

// ============================================================================
// 4. INTERACTIVE SKY DOME TIME SCRUBBER & GOLDEN / BLUE HOUR BAND
// ============================================================================

@Composable
fun InteractiveSkyDomeScrubberPanel(
    selectedDate: LocalDate,
    lat: Double,
    lng: Double,
    report: DailyAstroReport,
    use24Hour: Boolean,
    isUnlocked: Boolean,
    onUnlockClick: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val nowMinutes = remember { LocalTime.now().let { it.hour * 60 + it.minute } }
    var scrubbedMinutes by remember { mutableFloatStateOf(nowMinutes.toFloat()) }

    val timeFmt = remember(use24Hour) {
        DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a", Locale.US)
    }
    val scrubbedTime = remember(scrubbedMinutes.roundToInt()) {
        val m = scrubbedMinutes.roundToInt().coerceIn(0, 1439)
        LocalTime.of(m / 60, m % 60)
    }
    val scrubbedZdt = remember(selectedDate, scrubbedTime) {
        ZonedDateTime.of(selectedDate, scrubbedTime, ZoneId.systemDefault())
    }
    val sunPos = remember(scrubbedZdt, lat, lng) {
        AstroCalculator.getSunPosition(scrubbedZdt, lat, lng)
    }
    val moonPos = remember(scrubbedZdt, lat, lng) {
        AstroCalculator.getMoonPosition(scrubbedZdt, lat, lng)
    }

    val lightPhaseLabel = when {
        sunPos.altitudeDeg in -6.0..-4.0 -> "Blue Hour (Soft Twilight)"
        sunPos.altitudeDeg in -4.0..6.0 -> "Golden Hour (Warm Directional Sun)"
        sunPos.altitudeDeg > 6.0 -> "Daylight (${sunPos.altitudeDeg.roundToInt()}° Sun Elevation)"
        else -> "Night Sky (${moonPos.altitudeDeg.roundToInt()}° Moon Elevation)"
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "INTERACTIVE SUN PATH & GOLDEN/BLUE HOUR",
                        color = theme.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }
                ProBadge()
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isUnlocked) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scrub Time: ${scrubbedTime.format(timeFmt)}",
                        color = theme.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = lightPhaseLabel,
                        color = Color(0xFFF59E0B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = scrubbedMinutes,
                    onValueChange = { scrubbedMinutes = it },
                    valueRange = 0f..1439f,
                    colors = SliderDefaults.colors(
                        thumbColor = theme.accent,
                        activeTrackColor = theme.accent
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Sun: Az ${sunPos.azimuthDeg.roundToInt()}° / Alt ${sunPos.altitudeDeg.roundToInt()}°",
                        color = theme.textSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Moon: Az ${moonPos.azimuthDeg.roundToInt()}° / Alt ${moonPos.altitudeDeg.roundToInt()}°",
                        color = theme.textSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = theme.textSecondary.copy(alpha = 0.14f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("AM Blue / Golden", color = theme.textSecondary, fontSize = 10.sp)
                        Text(
                            text = "${report.morningBlueHourStart?.format(timeFmt) ?: "--:--"} – ${report.morningGoldenHourEnd?.format(timeFmt) ?: "--:--"}",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("PM Golden / Blue", color = theme.textSecondary, fontSize = 10.sp)
                        Text(
                            text = "${report.eveningGoldenHourStart?.format(timeFmt) ?: "--:--"} – ${report.eveningBlueHourEnd?.format(timeFmt) ?: "--:--"}",
                            color = Color(0xFFF59E0B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = "Scrub through all 24 hours on the interactive sun path to inspect exact Sun & Moon azimuth/altitude, Golden Hour, and Blue Hour windows.",
                    color = theme.textSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onUnlockClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B),
                        contentColor = Color(0xFF0A0A0C)
                    )
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Unlock Interactive Sky Dome Scrubber", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// ============================================================================
// 5. AIR QUALITY (AQI) & UV INDEX CARD (OPEN-METEO, CACHED)
// ============================================================================

@Composable
fun AirQualityAndUvCard(
    weatherSnapshot: WeatherSnapshot?,
    featureGate: FeatureGate?,
    isPro: Boolean,
    userPreferences: UserPreferences,
    onRequestPaywall: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val featureKey = "outdoor_air_quality_uv"
    val isUnlocked = isPro ||
        (featureGate?.hasAccess(featureKey, prefs = userPreferences) == true)
    val remMs = featureGate?.getRemainingMsForKey(featureKey, prefs = userPreferences) ?: 0L

    val usAqi = weatherSnapshot?.usAqi ?: 28
    val euAqi = weatherSnapshot?.europeanAqi ?: 22
    val pm25 = weatherSnapshot?.pm25UgM3 ?: 6.8
    val pm10 = weatherSnapshot?.pm10UgM3 ?: 13.5
    val uv = weatherSnapshot?.uvIndex ?: 4.5
    val aqiLabel = weatherSnapshot?.aqiCategoryLabel ?: "Good"
    val uvLabel = weatherSnapshot?.uvCategoryLabel ?: "Moderate"

    val aqiColor = when {
        usAqi <= 50 -> Color(0xFF10B981)
        usAqi <= 100 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Air,
                        contentDescription = null,
                        tint = aqiColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AIR QUALITY (AQI) & UV INDEX (OPEN-METEO)",
                        color = theme.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }
                if (!isPro && remMs > 0L && featureGate != null) {
                    ProCountdownRing(
                        progress = featureGate.getCountdownProgressForKey(featureKey, prefs = userPreferences),
                        label = featureGate.formatRemainingRingBadge(remMs)
                    )
                } else if (!isPro) {
                    ProBadge()
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isUnlocked) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.elevated.copy(alpha = 0.7f))
                            .padding(12.dp)
                    ) {
                        Text("US AQI (Approx.)", color = theme.textSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$usAqi • $aqiLabel",
                            color = aqiColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "PM2.5: ${String.format(Locale.US, "%.1f", pm25)} µg/m³ • PM10: ${String.format(Locale.US, "%.1f", pm10)} • EU AQI $euAqi",
                            color = theme.textSecondary,
                            fontSize = 10.sp
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.elevated.copy(alpha = 0.7f))
                            .padding(12.dp)
                    ) {
                        Text("UV Index (Approx.)", color = theme.textSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.1f", uv)} • $uvLabel",
                            color = Color(0xFFF59E0B),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uv >= 6.0) "Sunglasses & SPF 30+ recommended outdoors" else "Low-to-moderate solar UV exposure",
                            color = theme.textSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            } else {
                Text(
                    text = "View cached Open-Meteo US & European Air Quality Index (AQI), PM2.5, PM10 particulates, and UV exposure guidance.",
                    color = theme.textSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (featureGate != null) {
                            featureGate.guardNavigate(featureKey, prefs = userPreferences) {}
                        } else {
                            onRequestPaywall()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B),
                        contentColor = Color(0xFF0A0A0C)
                    )
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Unlock Air Quality & UV Index", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// ============================================================================
// 6. WORKMANAGER LOCAL ALERTS CONFIG CARD (SOLUNAR, GOLDEN HOUR, WEATHER, RAIN)
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OutdoorAlertsConfigCard(
    userPreferences: UserPreferences,
    preferencesRepository: PreferencesRepository,
    featureGate: FeatureGate?,
    isPro: Boolean,
    onRequestPaywall: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasNotifPerm by remember {
        mutableStateOf(DyneAlertsScheduler.hasNotificationPermission(context))
    }
    var pendingActionAfterPerm by remember { mutableStateOf<(() -> Unit)?>(null) }

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPerm = granted
        if (granted) {
            DyneAlertsScheduler.scheduleOrUpdateAlerts(context)
            pendingActionAfterPerm?.invoke()
        }
        pendingActionAfterPerm = null
    }

    fun ensurePermThenRun(block: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !DyneAlertsScheduler.hasNotificationPermission(context)) {
            pendingActionAfterPerm = block
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            hasNotifPerm = true
            block()
            DyneAlertsScheduler.scheduleOrUpdateAlerts(context)
        }
    }

    val featureKey = "outdoor_alerts"
    val isUnlocked = isPro ||
        (featureGate?.hasAccess(featureKey, prefs = userPreferences) == true)
    val remMs = featureGate?.getRemainingMsForKey(featureKey, prefs = userPreferences) ?: 0L

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "OUTDOOR & WEATHER ALERTS (WORKMANAGER)",
                        color = theme.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }
                if (!isPro && remMs > 0L && featureGate != null) {
                    ProCountdownRing(
                        progress = featureGate.getCountdownProgressForKey(featureKey, prefs = userPreferences),
                        label = featureGate.formatRemainingRingBadge(remMs)
                    )
                } else if (!isPro) {
                    ProBadge()
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "100% on-device notifications scheduled via Android WorkManager (no server). POST_NOTIFICATIONS requested only when enabled.",
                color = theme.textSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (isUnlocked) {
                // 1) Solunar Best-Window Alerts (Fishing or Hunting, lead time 15/30/60m)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Solunar Best-Window Alerts",
                            color = theme.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${userPreferences.solunarAlertMode.lowercase().replaceFirstChar { it.uppercase() }} mode • ${userPreferences.solunarAlertLeadMinutes} min lead time",
                            color = theme.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = userPreferences.solunarAlertsEnabled,
                        onCheckedChange = { enabled ->
                            ensurePermThenRun {
                                scope.launch {
                                    preferencesRepository.setSolunarNotificationConfig(enabled = enabled)
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = theme.onAccent, checkedTrackColor = theme.accent)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("FISHING" to "Fishing", "HUNTING" to "Hunting").forEach { (id, label) ->
                        val active = userPreferences.solunarAlertMode.equals(id, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) theme.accent else theme.elevated)
                                .clickable {
                                    scope.launch { preferencesRepository.setOutdoorAlertsConfig(solunarMode = id) }
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (active) theme.onAccent else theme.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    listOf(15, 30, 60).forEach { mins ->
                        val active = userPreferences.solunarAlertLeadMinutes == mins
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) Color(0xFFF59E0B) else theme.elevated)
                                .clickable {
                                    scope.launch { preferencesRepository.setOutdoorAlertsConfig(solunarLeadMinutes = mins) }
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${mins}m lead",
                                color = if (active) Color(0xFF0A0A0C) else theme.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = theme.textSecondary.copy(alpha = 0.14f))
                Spacer(modifier = Modifier.height(8.dp))

                // 2) Golden-Hour Alerts
                AlertToggleRow(
                    title = "Golden-Hour Alerts",
                    subtitle = "Notifies 20 min before evening Golden Hour light",
                    checked = userPreferences.goldenHourAlertsEnabled,
                    onCheckedChange = { enabled ->
                        ensurePermThenRun {
                            scope.launch { preferencesRepository.setOutdoorAlertsConfig(goldenHourEnabled = enabled) }
                        }
                    }
                )

                // 3) Daily Weather Summary
                AlertToggleRow(
                    title = "Daily Weather Summary",
                    subtitle = "Morning high/low, wind, UV & Solunar score",
                    checked = userPreferences.dailyWeatherSummaryEnabled,
                    onCheckedChange = { enabled ->
                        ensurePermThenRun {
                            scope.launch { preferencesRepository.setOutdoorAlertsConfig(dailyWeatherEnabled = enabled) }
                        }
                    }
                )

                // 4) Rain-Soon Alerts
                AlertToggleRow(
                    title = "Rain-Soon Alerts",
                    subtitle = "Alerts when upcoming hourly precipitation chance ≥ 55%",
                    checked = userPreferences.rainSoonAlertsEnabled,
                    onCheckedChange = { enabled ->
                        ensurePermThenRun {
                            scope.launch { preferencesRepository.setOutdoorAlertsConfig(rainSoonEnabled = enabled) }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        ensurePermThenRun {
                            DyneAlertsScheduler.triggerPreviewAlert(context, "SOLUNAR")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.elevated,
                        contentColor = theme.accent
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send Test Outdoor Notification Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        if (featureGate != null) {
                            featureGate.guardNavigate(featureKey, prefs = userPreferences) {}
                        } else {
                            onRequestPaywall()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B),
                        contentColor = Color(0xFF0A0A0C)
                    )
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Unlock Local Outdoor & Weather Alerts", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun AlertToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val theme = LocalDyneTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = theme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = theme.textSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = theme.onAccent, checkedTrackColor = theme.accent)
        )
    }
}
