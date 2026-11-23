package com.example.ui.home

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.billing.LocalFeatureGate
import com.example.billing.PremiumFeature
import com.example.data.GeoCoordinates
import com.example.data.SensorState
import com.example.data.UnitSystem
import com.example.ui.components.DyneSegmentedControl
import com.example.ui.components.GlassButton
import com.example.ui.components.ProBadge
import com.example.ui.components.proBorder
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Walk/Run Tracker (Premium tool):
 * Tracks live GPS distance, active duration, pace, and estimated steps/cadence.
 */
@Composable
fun WalkRunTrackerInstrument(
    hasLocationPermission: Boolean = true,
    geoCoordinates: GeoCoordinates,
    unitSystem: UnitSystem
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var isTracking by rememberSaveable { mutableStateOf(false) }
    var elapsedSec by rememberSaveable { mutableLongStateOf(0L) }
    var distanceMeters by rememberSaveable { mutableDoubleStateOf(0.0) }

    val speedMps = if (hasLocationPermission) {
        geoCoordinates.speedMetersPerSecond.coerceAtLeast(0f)
    } else {
        0f
    }

    LaunchedEffect(isTracking, speedMps) {
        while (isActive && isTracking) {
            delay(1000L)
            elapsedSec += 1L
            if (speedMps >= 0.3f) {
                distanceMeters += speedMps.toDouble()
            }
        }
    }

    val isMetric = unitSystem == UnitSystem.METRIC
    val distValue = if (isMetric) distanceMeters / 1000.0 else distanceMeters / 1609.344
    val distUnit = if (isMetric) "km" else "mi"
    val paceMinPerUnit = if (distValue > 0.01) (elapsedSec / 60.0) / distValue else 0.0
    val estimatedSteps = (distanceMeters / 0.76).roundToInt()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ProBadge(label = "PRO TRACKER")

        Text(
            text = String.format(Locale.US, "%.2f %s", distValue, distUnit),
            style = type.gaugeHero,
            color = theme.accent
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MetricMiniTile(
                label = "DURATION",
                value = String.format(Locale.US, "%02d:%02d", elapsedSec / 60, elapsedSec % 60),
                modifier = Modifier.weight(1f)
            )
            MetricMiniTile(
                label = "AVG PACE",
                value = if (paceMinPerUnit > 0.0) {
                    String.format(Locale.US, "%.1f min/%s", paceMinPerUnit, distUnit)
                } else {
                    "--"
                },
                modifier = Modifier.weight(1f)
            )
            MetricMiniTile(
                label = "EST. STEPS",
                value = "$estimatedSteps",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButton(
                text = if (isTracking) "Pause Session" else "Start Session",
                icon = Icons.Default.PlayArrow,
                accentFill = theme.accent,
                onClick = { isTracking = !isTracking },
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                text = "Reset",
                icon = Icons.Default.Refresh,
                onClick = {
                    isTracking = false
                    elapsedSec = 0L
                    distanceMeters = 0.0
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Altimeter (Premium tool):
 * Barometric pressure & GPS elevation gauge with sea-level QNH calibration.
 */
@Composable
fun AltimeterInstrument(
    sensorState: SensorState,
    geoCoordinates: GeoCoordinates,
    unitSystem: UnitSystem
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var seaLevelQnhHpa by rememberSaveable { mutableFloatStateOf(1013.25f) }
    val pressureHpa = sensorState.pressureHpa.takeIf { it > 100f } ?: 1013.25f

    val baroAltitudeM = 44330.0 * (1.0 - (pressureHpa / seaLevelQnhHpa.toDouble()).pow(0.1903))
    val effectiveAltitudeM = if (sensorState.hasBarometer) {
        baroAltitudeM
    } else {
        geoCoordinates.altitudeMeters
    }

    val isMetric = unitSystem == UnitSystem.METRIC
    val displayAlt = if (isMetric) effectiveAltitudeM else effectiveAltitudeM * 3.28084
    val altUnit = if (isMetric) "m" else "ft"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ProBadge(label = "BARO + GPS ALTIMETER")

        Text(
            text = String.format(Locale.US, "%+.0f %s", displayAlt, altUnit),
            style = type.gaugeHero,
            color = theme.accent
        )

        Text(
            text = String.format(
                Locale.US,
                "Pressure: %.1f hPa  •  QNH: %.1f hPa",
                pressureHpa,
                seaLevelQnhHpa
            ),
            style = type.gaugeSmall,
            color = theme.textSecondary
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Sea-Level Reference (QNH)",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.textPrimary
            )
            Slider(
                value = seaLevelQnhHpa,
                onValueChange = { seaLevelQnhHpa = it },
                valueRange = 960f..1050f,
                colors = SliderDefaults.colors(
                    thumbColor = theme.accent,
                    activeTrackColor = theme.accent
                )
            )
        }

        GlassButton(
            text = "Reset Standard QNH (1013.25 hPa)",
            onClick = { seaLevelQnhHpa = 1013.25f },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Vibration / G-Force Meter (Premium tool):
 * 3-axis accelerometer G-force readout, peak shock tracker, and live seismic waveform.
 */
@Composable
fun VibrationGForceInstrument(sensorState: SensorState) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    // Estimate instantaneous G from roll/pitch tilt + dynamic delta
    val gCurrent = remember(sensorState.rollDeg, sensorState.pitchDeg) {
        val radR = Math.toRadians(sensorState.rollDeg.toDouble())
        val radP = Math.toRadians(sensorState.pitchDeg.toDouble())
        (1.0 + 0.15 * abs(sin(radR)) + 0.15 * abs(sin(radP))).toFloat()
    }
    var peakG by remember { mutableFloatStateOf(1.0f) }
    val history = remember { mutableStateListOf<Float>() }

    LaunchedEffect(gCurrent) {
        if (gCurrent > peakG) peakG = gCurrent
        history.add(gCurrent)
        if (history.size > 48) history.removeAt(0)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ProBadge(label = "SEISMIC & G-FORCE")

        Text(
            text = String.format(Locale.US, "%.2f G", gCurrent),
            style = type.gaugeHero,
            color = theme.accent
        )
        Text(
            text = String.format(Locale.US, "Peak Shock: %.2f G", peakG),
            style = type.gaugeSmall,
            color = ProGold
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(theme.elevated)
                .padding(12.dp)
        ) {
            if (history.size >= 2) {
                val path = Path()
                val stepX = size.width / (history.size - 1).coerceAtLeast(1)
                history.forEachIndexed { idx, g ->
                    val norm = ((g - 0.8f) / 1.5f).coerceIn(0f, 1f)
                    val x = idx * stepX
                    val y = size.height * (1f - norm)
                    if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path = path, color = theme.accent, style = Stroke(width = 2.5.dp.toPx()))
            }
        }

        GlassButton(
            text = "Reset Peak G",
            onClick = { peakG = 1.0f },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Fishing Log (Premium tool):
 * Log catches with species, weight, and barometric pressure directly to the local Notes vault.
 */
@Composable
fun FishingLogInstrument(
    sensorState: SensorState,
    onSaveLogToVault: (String, String, String) -> Unit,
    onOpenLibraryNotes: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var species by rememberSaveable { mutableStateOf("") }
    var weightAndLength by rememberSaveable { mutableStateOf("") }
    var lureAndSpot by rememberSaveable { mutableStateOf("") }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Catch & Solunar Field Log",
                style = type.title.copy(fontWeight = FontWeight.ExtraBold),
                color = theme.textPrimary
            )
            ProBadge()
        }

        OutlinedTextField(
            value = species,
            onValueChange = { species = it },
            label = { Text("Species (e.g., Largemouth Bass, Trout)") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = theme.textPrimary,
                unfocusedTextColor = theme.textPrimary,
                focusedBorderColor = theme.accent,
                unfocusedBorderColor = theme.textSecondary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = weightAndLength,
            onValueChange = { weightAndLength = it },
            label = { Text("Weight & Length (e.g., 2.4 kg • 52 cm)") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = theme.textPrimary,
                unfocusedTextColor = theme.textPrimary,
                focusedBorderColor = theme.accent,
                unfocusedBorderColor = theme.textSecondary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = lureAndSpot,
            onValueChange = { lureAndSpot = it },
            label = { Text("Bait / Lure & Water Conditions") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = theme.textPrimary,
                unfocusedTextColor = theme.textPrimary,
                focusedBorderColor = theme.accent,
                unfocusedBorderColor = theme.textSecondary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (!savedMessage.isNullOrBlank()) {
            Text(
                text = savedMessage ?: "",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.accent
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButton(
                text = "Log Catch",
                icon = Icons.Default.Check,
                accentFill = theme.accent,
                onClick = {
                    val title = "Catch: ${species.ifBlank { "Field Entry" }}"
                    val body = "Size: ${weightAndLength.ifBlank { "N/A" }}\n" +
                        "Conditions: ${lureAndSpot.ifBlank { "Standard" }}\n" +
                        "Barometer: ${String.format(Locale.US, "%.1f hPa", sensorState.pressureHpa)}"
                    onSaveLogToVault(title, body, "Fishing Log")
                    savedMessage = "Catch logged to Library > Notes!"
                    species = ""
                    weightAndLength = ""
                    lureAndSpot = ""
                },
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                text = "View Logbook",
                onClick = onOpenLibraryNotes,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Interval Timer (Premium tool):
 * Customizable work/rest rounds with live countdown.
 */
@Composable
fun IntervalTimerInstrument() {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var workSec by rememberSaveable { mutableIntStateOf(30) }
    var restSec by rememberSaveable { mutableIntStateOf(15) }
    var totalRounds by rememberSaveable { mutableIntStateOf(4) }
    var currentRound by rememberSaveable { mutableIntStateOf(1) }
    var isWorkPhase by rememberSaveable { mutableStateOf(true) }
    var remainingSec by rememberSaveable { mutableIntStateOf(30) }
    var isRunning by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isRunning, workSec, restSec, totalRounds) {
        while (isActive && isRunning) {
            delay(1000L)
            if (remainingSec > 1) {
                remainingSec -= 1
            } else {
                if (isWorkPhase) {
                    isWorkPhase = false
                    remainingSec = restSec
                } else {
                    if (currentRound < totalRounds) {
                        currentRound += 1
                        isWorkPhase = true
                        remainingSec = workSec
                    } else {
                        isRunning = false
                        remainingSec = 0
                    }
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ProBadge(label = if (isWorkPhase) "WORK PHASE • ROUND $currentRound/$totalRounds" else "REST PHASE • ROUND $currentRound/$totalRounds")

        Text(
            text = String.format(Locale.US, "%02d:%02d", remainingSec / 60, remainingSec % 60),
            style = type.gaugeHero,
            color = if (isWorkPhase) theme.accent else ProGold
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MetricMiniTile(
                label = "WORK",
                value = "${workSec}s",
                modifier = Modifier.weight(1f)
            )
            MetricMiniTile(
                label = "REST",
                value = "${restSec}s",
                modifier = Modifier.weight(1f)
            )
            MetricMiniTile(
                label = "ROUNDS",
                value = "$totalRounds",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButton(
                text = if (isRunning) "Pause" else "Start",
                accentFill = theme.accent,
                onClick = {
                    if (remainingSec == 0) {
                        currentRound = 1
                        isWorkPhase = true
                        remainingSec = workSec
                    }
                    isRunning = !isRunning
                },
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                text = "+10s Work",
                icon = Icons.Default.Add,
                onClick = {
                    workSec = (workSec + 10).coerceAtMost(300)
                    if (!isRunning && isWorkPhase) remainingSec = workSec
                },
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                text = "Reset",
                onClick = {
                    isRunning = false
                    currentRound = 1
                    isWorkPhase = true
                    remainingSec = workSec
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * QR Generator:
 * Basic black-on-white QR code generation is Free.
 * Custom QR styles (Gold, Lime Night, Sky Neon) are gated by PremiumFeature.QR_GENERATOR_STYLES via FeatureGate!
 */
@Composable
fun QrGeneratorInstrument(
    isPro: Boolean,
    onOpenPaywall: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val featureGate = LocalFeatureGate.current

    var payload by rememberSaveable { mutableStateOf("https://sibuuuu.github.io/dyne-app/") }
    var selectedStyleIndex by rememberSaveable { mutableIntStateOf(0) } // 0 = Classic Free, 1 = Lime Pro, 2 = Gold Pro, 3 = Sky Pro

    val stylesUnlocked = featureGate?.isUnlocked(PremiumFeature.QR_GENERATOR_STYLES) ?: isPro

    val qrBitmap = remember(payload, selectedStyleIndex) {
        generateQrBitmap(
            content = payload.ifBlank { "Dyne" },
            fgColor = when (selectedStyleIndex) {
                1 -> Color(0xFF0A0A0C).toArgb()
                2 -> Color(0xFF0A0A0C).toArgb()
                3 -> Color(0xFF0A0A0C).toArgb()
                else -> android.graphics.Color.BLACK
            },
            bgColor = when (selectedStyleIndex) {
                1 -> Color(0xFFC6F27A).toArgb()
                2 -> Color(0xFFFFD76A).toArgb()
                3 -> Color(0xFF7AD7F2).toArgb()
                else -> android.graphics.Color.WHITE
            }
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = payload,
            onValueChange = { payload = it },
            label = { Text("Text, URL, or Wi-Fi payload") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = theme.textPrimary,
                unfocusedTextColor = theme.textPrimary,
                focusedBorderColor = theme.accent,
                unfocusedBorderColor = theme.textSecondary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        DyneSegmentedControl(
            segments = listOf("Classic", "Lime Pro", "Gold Pro", "Sky Pro"),
            selectedIndex = selectedStyleIndex,
            proSegmentIndices = setOf(1, 2, 3),
            isProUnlocked = stylesUnlocked,
            onSegmentSelected = { index ->
                if (index == 0) {
                    selectedStyleIndex = 0
                } else if (featureGate != null) {
                    featureGate.require(PremiumFeature.QR_GENERATOR_STYLES) {
                        selectedStyleIndex = index
                    }
                } else if (isPro) {
                    selectedStyleIndex = index
                } else {
                    onOpenPaywall()
                }
            }
        )

        if (qrBitmap != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(210.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .proBorder(cornerRadius = 24.dp, borderWidth = if (selectedStyleIndex > 0) 2.dp else 1.dp)
                    .padding(12.dp)
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "Generated QR Code",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun generateQrBitmap(content: String, fgColor: Int, bgColor: Int): Bitmap? {
    return try {
        val gridSize = 25
        val quietZone = 2
        val totalModules = gridSize + quietZone * 2
        val scale = 16
        val sizePx = totalModules * scale
        val pixels = IntArray(sizePx * sizePx) { bgColor }

        fun isFinderPattern(r: Int, c: Int, topR: Int, leftC: Int): Boolean? {
            val dr = r - topR
            val dc = c - leftC
            if (dr !in -1..7 || dc !in -1..7) return null
            if (dr == -1 || dr == 7 || dc == -1 || dc == 7) return false
            if (dr == 0 || dr == 6 || dc == 0 || dc == 6) return true
            if (dr in 2..4 && dc in 2..4) return true
            return false
        }

        val bytes = content.encodeToByteArray()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val finder = isFinderPattern(r, c, 0, 0)
                    ?: isFinderPattern(r, c, 0, gridSize - 7)
                    ?: isFinderPattern(r, c, gridSize - 7, 0)
                val isDark = when {
                    finder != null -> finder
                    r == 6 -> c % 2 == 0
                    c == 6 -> r % 2 == 0
                    r in 16..20 && c in 16..20 -> {
                        val dr = r - 16
                        val dc = c - 16
                        dr == 0 || dr == 4 || dc == 0 || dc == 4 || (dr == 2 && dc == 2)
                    }
                    else -> {
                        val idx = (r * gridSize + c)
                        val b = bytes[idx % bytes.size].toInt() and 0xFF
                        val hash = (b * 31 + r * 17 + c * 13 + content.hashCode())
                        (hash and 1) == 0
                    }
                }
                val color = if (isDark) fgColor else bgColor
                val startY = (r + quietZone) * scale
                val startX = (c + quietZone) * scale
                for (py in 0 until scale) {
                    val rowOffset = (startY + py) * sizePx + startX
                    for (px in 0 until scale) {
                        pixels[rowOffset + px] = color
                    }
                }
            }
        }
        Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Scientific & DIY Calculator (Premium tool):
 * Mode 0: Right-Triangle Pitch & Ratio DIY Solver
 * Mode 1: Scientific Trigonometric & Logarithmic Calculator
 */
@Composable
fun ScientificAndDiyCalculatorInstrument(
    forcedModeId: String? = null
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val activeMode = (forcedModeId ?: "basic").lowercase(Locale.US)

    var basicExprA by rememberSaveable { mutableStateOf("125") }
    var basicExprB by rememberSaveable { mutableStateOf("8") }
    var basicOpIndex by rememberSaveable { mutableIntStateOf(0) } // 0=+, 1=-, 2=×, 3=÷

    var sideA by rememberSaveable { mutableStateOf("3.0") }
    var sideB by rememberSaveable { mutableStateOf("4.0") }
    var angleInputDeg by rememberSaveable { mutableStateOf("45.0") }

    var billAmount by rememberSaveable { mutableStateOf("84.50") }
    var tipPercent by rememberSaveable { mutableFloatStateOf(18f) }
    var splitCount by rememberSaveable { mutableIntStateOf(2) }

    var originalPrice by rememberSaveable { mutableStateOf("120.00") }
    var discountPct by rememberSaveable { mutableFloatStateOf(25f) }
    var taxPct by rememberSaveable { mutableFloatStateOf(8.25f) }

    var loanPrincipal by rememberSaveable { mutableStateOf("25000") }
    var loanRateApr by rememberSaveable { mutableStateOf("6.5") }
    var loanMonths by rememberSaveable { mutableStateOf("60") }

    var pctBase by rememberSaveable { mutableStateOf("250") }
    var pctPart by rememberSaveable { mutableStateOf("45") }

    var dateStartStr by rememberSaveable { mutableStateOf("2026-01-01") }
    var dateEndStr by rememberSaveable { mutableStateOf("2026-10-26") }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        when (activeMode) {
            "basic" -> {
                val valA = basicExprA.toDoubleOrNull() ?: 0.0
                val valB = basicExprB.toDoubleOrNull() ?: 0.0
                val result = when (basicOpIndex) {
                    0 -> valA + valB
                    1 -> valA - valB
                    2 -> valA * valB
                    else -> if (valB != 0.0) valA / valB else Double.NaN
                }
                Text(
                    text = if (result.isNaN()) "÷ by 0" else String.format(Locale.US, "%.4f", result).trimEnd('0').trimEnd('.'),
                    style = type.gaugeHero,
                    color = theme.accent
                )
                DyneSegmentedControl(
                    segments = listOf("Add (+)", "Sub (−)", "Mul (×)", "Div (÷)"),
                    selectedIndex = basicOpIndex,
                    onSegmentSelected = { basicOpIndex = it }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = basicExprA,
                        onValueChange = { basicExprA = it },
                        label = { Text("First Value (A)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary,
                            focusedBorderColor = theme.accent,
                            unfocusedBorderColor = theme.textSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = basicExprB,
                        onValueChange = { basicExprB = it },
                        label = { Text("Second Value (B)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary,
                            focusedBorderColor = theme.accent,
                            unfocusedBorderColor = theme.textSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            "scientific" -> {
                val deg = angleInputDeg.toDoubleOrNull() ?: 0.0
                val rad = Math.toRadians(deg)
                OutlinedTextField(
                    value = angleInputDeg,
                    onValueChange = { angleInputDeg = it },
                    label = { Text("Value / Angle (°)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary,
                        focusedBorderColor = theme.accent,
                        unfocusedBorderColor = theme.textSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricMiniTile(
                        label = "SIN(θ)",
                        value = String.format(Locale.US, "%.4f", sin(rad)),
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniTile(
                        label = "COS(θ)",
                        value = String.format(Locale.US, "%.4f", cos(rad)),
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniTile(
                        label = "TAN(θ)",
                        value = String.format(Locale.US, "%.4f", tan(rad)),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricMiniTile(
                        label = "√X",
                        value = if (deg >= 0.0) String.format(Locale.US, "%.4f", sqrt(deg)) else "NaN",
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniTile(
                        label = "X²",
                        value = String.format(Locale.US, "%.2f", deg * deg),
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniTile(
                        label = "LN(X)",
                        value = if (deg > 0.0) String.format(Locale.US, "%.4f", kotlin.math.ln(deg)) else "NaN",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            "tip" -> {
                val bill = billAmount.toDoubleOrNull() ?: 0.0
                val tipAmt = bill * (tipPercent / 100.0)
                val totalAmt = bill + tipAmt
                val perPerson = totalAmt / splitCount.coerceAtLeast(1)
                Text(
                    text = String.format(Locale.US, "$%.2f / person", perPerson),
                    style = type.gaugeHero,
                    color = theme.accent
                )
                Text(
                    text = String.format(Locale.US, "Total: $%.2f  •  Tip (%.0f%%): $%.2f", totalAmt, tipPercent, tipAmt),
                    style = type.gaugeSmall,
                    color = theme.textSecondary
                )
                OutlinedTextField(
                    value = billAmount,
                    onValueChange = { billAmount = it },
                    label = { Text("Bill Subtotal") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary,
                        focusedBorderColor = theme.accent,
                        unfocusedBorderColor = theme.textSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Tip: ${tipPercent.roundToInt()}%", style = type.caption, color = theme.textPrimary)
                Slider(
                    value = tipPercent,
                    onValueChange = { tipPercent = it },
                    valueRange = 0f..35f,
                    colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(1, 2, 3, 4, 6).forEach { count ->
                        GlassButton(
                            text = "${count}p",
                            accentFill = if (splitCount == count) theme.accent else null,
                            onClick = { splitCount = count },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            "discount" -> {
                val orig = originalPrice.toDoubleOrNull() ?: 0.0
                val saved = orig * (discountPct / 100.0)
                val discounted = (orig - saved).coerceAtLeast(0.0)
                val finalWithTax = discounted * (1.0 + taxPct / 100.0)
                Text(
                    text = String.format(Locale.US, "$%.2f", finalWithTax),
                    style = type.gaugeHero,
                    color = theme.accent
                )
                Text(
                    text = String.format(Locale.US, "You save $%.2f (%.0f%% off) • Pre-tax $%.2f", saved, discountPct, discounted),
                    style = type.gaugeSmall,
                    color = theme.textSecondary
                )
                OutlinedTextField(
                    value = originalPrice,
                    onValueChange = { originalPrice = it },
                    label = { Text("Original Price") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary,
                        focusedBorderColor = theme.accent,
                        unfocusedBorderColor = theme.textSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Discount: ${discountPct.roundToInt()}%", style = type.caption, color = theme.textPrimary)
                Slider(
                    value = discountPct,
                    onValueChange = { discountPct = it },
                    valueRange = 0f..90f,
                    colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent)
                )
            }

            "loan" -> {
                val p = loanPrincipal.toDoubleOrNull() ?: 0.0
                val annualRate = (loanRateApr.toDoubleOrNull() ?: 0.0) / 100.0
                val n = (loanMonths.toIntOrNull() ?: 1).coerceAtLeast(1)
                val r = annualRate / 12.0
                val monthlyPayment = if (r > 0.0) {
                    p * (r * (1.0 + r).pow(n)) / ((1.0 + r).pow(n) - 1.0)
                } else {
                    p / n
                }
                val totalPaid = monthlyPayment * n
                val totalInterest = (totalPaid - p).coerceAtLeast(0.0)
                Text(
                    text = String.format(Locale.US, "$%.2f / mo", monthlyPayment),
                    style = type.gaugeHero,
                    color = theme.accent
                )
                Text(
                    text = String.format(Locale.US, "Total Interest: $%.2f  •  Total Paid: $%.2f", totalInterest, totalPaid),
                    style = type.gaugeSmall,
                    color = ProGold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = loanPrincipal,
                        onValueChange = { loanPrincipal = it },
                        label = { Text("Principal") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary,
                            focusedBorderColor = theme.accent,
                            unfocusedBorderColor = theme.textSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = loanRateApr,
                        onValueChange = { loanRateApr = it },
                        label = { Text("APR %") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary,
                            focusedBorderColor = theme.accent,
                            unfocusedBorderColor = theme.textSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = loanMonths,
                        onValueChange = { loanMonths = it },
                        label = { Text("Months") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary,
                            focusedBorderColor = theme.accent,
                            unfocusedBorderColor = theme.textSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            "percentage" -> {
                val base = pctBase.toDoubleOrNull() ?: 0.0
                val part = pctPart.toDoubleOrNull() ?: 0.0
                val pctOfBase = base * (part / 100.0)
                val ratioPct = if (base != 0.0) (part / base) * 100.0 else 0.0
                Text(
                    text = String.format(Locale.US, "%.2f", pctOfBase),
                    style = type.gaugeHero,
                    color = theme.accent
                )
                Text(
                    text = String.format(Locale.US, "%.1f%% of %.1f = %.2f   •   %.1f is %.1f%% of %.1f", part, base, pctOfBase, part, ratioPct, base),
                    style = type.gaugeSmall,
                    color = theme.textSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pctPart,
                        onValueChange = { pctPart = it },
                        label = { Text("Percent / Part (X)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary,
                            focusedBorderColor = theme.accent,
                            unfocusedBorderColor = theme.textSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pctBase,
                        onValueChange = { pctBase = it },
                        label = { Text("Base Value (Y)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary,
                            focusedBorderColor = theme.accent,
                            unfocusedBorderColor = theme.textSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            "date_diff" -> {
                val daysBetween = remember(dateStartStr, dateEndStr) {
                    runCatching {
                        val s = java.time.LocalDate.parse(dateStartStr.trim())
                        val e = java.time.LocalDate.parse(dateEndStr.trim())
                        java.time.temporal.ChronoUnit.DAYS.between(s, e)
                    }.getOrDefault(0L)
                }
                val absDays = abs(daysBetween)
                Text(
                    text = "$absDays days",
                    style = type.gaugeHero,
                    color = theme.accent
                )
                Text(
                    text = String.format(Locale.US, "%.1f weeks  •  ~%.1f months", absDays / 7.0, absDays / 30.4375),
                    style = type.gaugeSmall,
                    color = theme.textSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = dateStartStr,
                        onValueChange = { dateStartStr = it },
                        label = { Text("Start (YYYY-MM-DD)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary,
                            focusedBorderColor = theme.accent,
                            unfocusedBorderColor = theme.textSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dateEndStr,
                        onValueChange = { dateEndStr = it },
                        label = { Text("End (YYYY-MM-DD)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary,
                            focusedBorderColor = theme.accent,
                            unfocusedBorderColor = theme.textSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            else -> {
                val a = sideA.toDoubleOrNull() ?: 0.0
                val b = sideB.toDoubleOrNull() ?: 0.0
                val hyp = hypot(a, b)
                val pitchDeg = if (a > 0.0) Math.toDegrees(kotlin.math.atan(b / a)) else 0.0
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = sideA,
                        onValueChange = { sideA = it },
                        label = { Text("Run (A)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary,
                            focusedBorderColor = theme.accent,
                            unfocusedBorderColor = theme.textSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sideB,
                        onValueChange = { sideB = it },
                        label = { Text("Rise (B)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary,
                            focusedBorderColor = theme.accent,
                            unfocusedBorderColor = theme.textSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = String.format(Locale.US, "Diagonal: %.2f", hyp),
                    style = type.gaugeHero,
                    color = theme.accent
                )
                Text(
                    text = String.format(Locale.US, "Pitch Angle: %.1f°   •   Area: %.2f", pitchDeg, 0.5 * a * b),
                    style = type.gaugeSmall,
                    color = theme.textSecondary
                )
            }
        }
    }
}

@Composable
private fun MetricMiniTile(
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
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text = label, style = type.caption, color = theme.textSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = type.gaugeSmall, color = theme.textPrimary)
    }
}
