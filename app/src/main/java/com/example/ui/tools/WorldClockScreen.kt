package com.example.ui.tools

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.billing.LocalFeatureGate
import com.example.billing.PremiumFeature
import com.example.ui.components.FeedbackHelper
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassSheet
import com.example.ui.components.HazeState
import com.example.ui.components.horizontalChipRowEdgeFade
import com.example.ui.components.pressScaleClickable
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class WorldClockEntry(
    val zoneId: String,
    val city: String,
    val country: String,
    val customAbbrev: String = ""
)

object WorldZoneDirectory {

    // Curated rich directory of major cities, countries (including multi-zone countries), and common abbreviations
    private val curatedEntries = listOf(
        WorldClockEntry("America/New_York", "New York", "United States", "EST"),
        WorldClockEntry("America/Chicago", "Chicago", "United States", "CST"),
        WorldClockEntry("America/Denver", "Denver", "United States", "MST"),
        WorldClockEntry("America/Los_Angeles", "Los Angeles", "United States", "PST"),
        WorldClockEntry("America/Phoenix", "Phoenix", "United States", "MST"),
        WorldClockEntry("America/Anchorage", "Anchorage", "United States", "AKST"),
        WorldClockEntry("Pacific/Honolulu", "Honolulu", "United States", "HST"),
        WorldClockEntry("America/Toronto", "Toronto", "Canada", "EST"),
        WorldClockEntry("America/Vancouver", "Vancouver", "Canada", "PST"),
        WorldClockEntry("America/Halifax", "Halifax", "Canada", "AST"),
        WorldClockEntry("America/Winnipeg", "Winnipeg", "Canada", "CST"),
        WorldClockEntry("America/Edmonton", "Edmonton", "Canada", "MST"),
        WorldClockEntry("America/St_Johns", "St. John's", "Canada", "NST"),
        WorldClockEntry("Europe/London", "London", "United Kingdom", "GMT"),
        WorldClockEntry("Europe/Paris", "Paris", "France", "CET"),
        WorldClockEntry("Europe/Berlin", "Berlin", "Germany", "CET"),
        WorldClockEntry("Europe/Madrid", "Madrid", "Spain", "CET"),
        WorldClockEntry("Atlantic/Canary", "Canary Islands", "Spain", "WET"),
        WorldClockEntry("Europe/Rome", "Rome", "Italy", "CET"),
        WorldClockEntry("Europe/Zurich", "Zurich", "Switzerland", "CET"),
        WorldClockEntry("Europe/Amsterdam", "Amsterdam", "Netherlands", "CET"),
        WorldClockEntry("Europe/Stockholm", "Stockholm", "Sweden", "CET"),
        WorldClockEntry("Europe/Warsaw", "Warsaw", "Poland", "CET"),
        WorldClockEntry("Europe/Athens", "Athens", "Greece", "EET"),
        WorldClockEntry("Europe/Istanbul", "Istanbul", "Turkey", "TRT"),
        WorldClockEntry("Europe/Moscow", "Moscow", "Russia", "MSK"),
        WorldClockEntry("Asia/Yekaterinburg", "Yekaterinburg", "Russia", "YEKT"),
        WorldClockEntry("Asia/Novosibirsk", "Novosibirsk", "Russia", "NOVT"),
        WorldClockEntry("Asia/Vladivostok", "Vladivostok", "Russia", "VLAT"),
        WorldClockEntry("Asia/Kolkata", "Mumbai / New Delhi", "India", "IST"),
        WorldClockEntry("Asia/Dubai", "Dubai", "United Arab Emirates", "GST"),
        WorldClockEntry("Asia/Riyadh", "Riyadh", "Saudi Arabia", "AST"),
        WorldClockEntry("Asia/Singapore", "Singapore", "Singapore", "SGT"),
        WorldClockEntry("Asia/Tokyo", "Tokyo", "Japan", "JST"),
        WorldClockEntry("Asia/Seoul", "Seoul", "South Korea", "KST"),
        WorldClockEntry("Asia/Shanghai", "Shanghai / Beijing", "China", "CST"),
        WorldClockEntry("Asia/Hong_Kong", "Hong Kong", "China", "HKT"),
        WorldClockEntry("Asia/Taipei", "Taipei", "Taiwan", "CST"),
        WorldClockEntry("Asia/Bangkok", "Bangkok", "Thailand", "ICT"),
        WorldClockEntry("Asia/Jakarta", "Jakarta", "Indonesia", "WIB"),
        WorldClockEntry("Asia/Makassar", "Bali / Makassar", "Indonesia", "WITA"),
        WorldClockEntry("Asia/Jayapura", "Jayapura", "Indonesia", "WIT"),
        WorldClockEntry("Asia/Manila", "Manila", "Philippines", "PHT"),
        WorldClockEntry("Asia/Dhaka", "Dhaka", "Bangladesh", "BST"),
        WorldClockEntry("Asia/Karachi", "Karachi", "Pakistan", "PKT"),
        WorldClockEntry("Australia/Sydney", "Sydney", "Australia", "AEST"),
        WorldClockEntry("Australia/Melbourne", "Melbourne", "Australia", "AEST"),
        WorldClockEntry("Australia/Brisbane", "Brisbane", "Australia", "AEST"),
        WorldClockEntry("Australia/Adelaide", "Adelaide", "Australia", "ACST"),
        WorldClockEntry("Australia/Perth", "Perth", "Australia", "AWST"),
        WorldClockEntry("Australia/Darwin", "Darwin", "Australia", "ACST"),
        WorldClockEntry("Pacific/Auckland", "Auckland", "New Zealand", "NZST"),
        WorldClockEntry("America/Sao_Paulo", "São Paulo", "Brazil", "BRT"),
        WorldClockEntry("America/Manaus", "Manaus", "Brazil", "AMT"),
        WorldClockEntry("America/Noronha", "Fernando de Noronha", "Brazil", "FNT"),
        WorldClockEntry("America/Mexico_City", "Mexico City", "Mexico", "CST"),
        WorldClockEntry("America/Cancun", "Cancún", "Mexico", "EST"),
        WorldClockEntry("America/Tijuana", "Tijuana", "Mexico", "PST"),
        WorldClockEntry("America/Buenos_Aires", "Buenos Aires", "Argentina", "ART"),
        WorldClockEntry("America/Santiago", "Santiago", "Chile", "CLT"),
        WorldClockEntry("America/Bogota", "Bogotá", "Colombia", "COT"),
        WorldClockEntry("Africa/Cairo", "Cairo", "Egypt", "EET"),
        WorldClockEntry("Africa/Johannesburg", "Johannesburg", "South Africa", "SAST"),
        WorldClockEntry("Africa/Lagos", "Lagos", "Nigeria", "WAT"),
        WorldClockEntry("Africa/Nairobi", "Nairobi", "Kenya", "EAT"),
        WorldClockEntry("UTC", "Coordinated Universal Time", "Global Reference", "UTC")
    )

    val allAvailableEntries: List<WorldClockEntry> by lazy {
        val byZone = LinkedHashMap<String, WorldClockEntry>()
        curatedEntries.forEach { byZone[it.zoneId] = it }

        ZoneId.getAvailableZoneIds()
            .filter { id ->
                id.contains("/") &&
                    !id.startsWith("Etc/") &&
                    !id.startsWith("SystemV/") &&
                    !id.startsWith("US/") &&
                    !id.startsWith("Canada/") &&
                    !id.startsWith("Brazil/") &&
                    !id.startsWith("Mexico/")
            }
            .sorted()
            .forEach { zoneId ->
                if (!byZone.containsKey(zoneId)) {
                    val parts = zoneId.split("/")
                    val region = parts.firstOrNull()?.replace('_', ' ') ?: "World"
                    val city = parts.lastOrNull()?.replace('_', ' ') ?: zoneId
                    byZone[zoneId] = WorldClockEntry(
                        zoneId = zoneId,
                        city = city,
                        country = region
                    )
                }
            }
        byZone.values.toList()
    }

    fun resolveAbbreviation(zoneId: String, instant: Instant, customAbbrev: String = ""): String {
        val zone = runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault())
        val zdt = ZonedDateTime.ofInstant(instant, zone)
        // Prefer common recognizable abbreviations if Java returns a generic GMT+XX:XX
        if (customAbbrev.isNotBlank()) return customAbbrev
        val curated = curatedEntries.firstOrNull { it.zoneId == zoneId }?.customAbbrev
        if (!curated.isNullOrBlank()) return curated

        val shortName = zone.getDisplayName(TextStyle.SHORT, Locale.US)
        if (!shortName.startsWith("GMT") && !shortName.startsWith("UTC") && shortName.length <= 6) {
            return shortName
        }
        val fmt = DateTimeFormatter.ofPattern("zzz", Locale.US)
        val formatted = zdt.format(fmt)
        return if (formatted.length <= 6) formatted else formatUtcOffset(zdt)
    }

    fun formatUtcOffset(zdt: ZonedDateTime): String {
        val totalSeconds = zdt.offset.totalSeconds
        if (totalSeconds == 0) return "UTC±00:00"
        val sign = if (totalSeconds >= 0) "+" else "-"
        val absSec = abs(totalSeconds)
        val hours = absSec / 3600
        val mins = (absSec % 3600) / 60
        return String.format(Locale.US, "UTC%s%02d:%02d", sign, hours, mins)
    }

    fun formatDifferenceFromLocal(localZdt: ZonedDateTime, targetZdt: ZonedDateTime): String {
        val diffSeconds = targetZdt.offset.totalSeconds - localZdt.offset.totalSeconds
        if (diffSeconds == 0) return "Same time"
        val sign = if (diffSeconds > 0) "+" else "-"
        val absMinutes = abs(diffSeconds) / 60
        val h = absMinutes / 60
        val m = absMinutes % 60
        return if (m == 0) {
            "$sign${h}h"
        } else {
            "$sign${h}h ${m}m"
        }
    }
}

private object WorldClockStorage {
    private const val FILE_NAME = "world_clocks_prefs.json"

    fun load(context: Context): Pair<Boolean, List<WorldClockEntry>> {
        val file = File(context.filesDir, FILE_NAME)
        val defaultClocks = listOf(
            WorldClockEntry("Europe/London", "London", "United Kingdom", "GMT"),
            WorldClockEntry("America/New_York", "New York", "United States", "EST"),
            WorldClockEntry("Asia/Tokyo", "Tokyo", "Japan", "JST")
        )
        if (!file.exists()) return true to defaultClocks
        return runCatching {
            val root = JSONObject(file.readText())
            val use24h = root.optBoolean("use24h", true)
            val arr = root.optJSONArray("clocks") ?: JSONArray()
            val list = mutableListOf<WorldClockEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    WorldClockEntry(
                        zoneId = obj.optString("zoneId", "UTC"),
                        city = obj.optString("city", "UTC"),
                        country = obj.optString("country", "World"),
                        customAbbrev = obj.optString("customAbbrev", "")
                    )
                )
            }
            use24h to (if (list.isEmpty()) defaultClocks else list)
        }.getOrElse {
            true to defaultClocks
        }
    }

    fun save(context: Context, use24h: Boolean, clocks: List<WorldClockEntry>) {
        runCatching {
            val root = JSONObject()
            root.put("use24h", use24h)
            val arr = JSONArray()
            clocks.forEach { c ->
                arr.put(
                    JSONObject().apply {
                        put("zoneId", c.zoneId)
                        put("city", c.city)
                        put("country", c.country)
                        put("customAbbrev", c.customAbbrev)
                    }
                )
            }
            root.put("clocks", arr)
            File(context.filesDir, FILE_NAME).writeText(root.toString())
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorldClockInstrumentScreen(
    isPro: Boolean = false,
    onOpenPaywall: () -> Unit = {},
    hazeState: HazeState? = null
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var liveInstant by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            liveInstant = Instant.now()
            delay(1000L)
        }
    }

    var use24Hour by rememberSaveable { mutableStateOf(true) }
    val savedClocks = remember { mutableStateListOf<WorldClockEntry>() }

    LaunchedEffect(Unit) {
        val (loaded24h, loadedClocks) = WorldClockStorage.load(context)
        use24Hour = loaded24h
        savedClocks.clear()
        savedClocks.addAll(loadedClocks)
    }

    // Meeting planner slider: offset in minutes from current local time, or null when live
    var plannerMinutesOfDay by rememberSaveable { mutableFloatStateOf(-1f) }
    val isPlannerActive = plannerMinutesOfDay >= 0f

    val localZone = ZoneId.systemDefault()
    val effectiveInstant = remember(liveInstant, plannerMinutesOfDay, localZone) {
        if (plannerMinutesOfDay < 0f) {
            liveInstant
        } else {
            val localToday = ZonedDateTime.ofInstant(liveInstant, localZone)
            val totalMins = plannerMinutesOfDay.roundToInt().coerceIn(0, 1439)
            val h = totalMins / 60
            val m = totalMins % 60
            localToday.withHour(h).withMinute(m).withSecond(0).toInstant()
        }
    }

    val localZdt = remember(effectiveInstant, localZone) {
        ZonedDateTime.ofInstant(effectiveInstant, localZone)
    }
    val localAbbrev = remember(localZone, effectiveInstant) {
        WorldZoneDirectory.resolveAbbreviation(localZone.id, effectiveInstant)
    }
    val localUtcOffset = remember(localZdt) {
        WorldZoneDirectory.formatUtcOffset(localZdt)
    }
    val isLocalDaytime = localZdt.hour in 6..18

    val mainTimeFormatter = remember(use24Hour) {
        DateTimeFormatter.ofPattern(
            if (use24Hour) "HH:mm:ss" else "hh:mm:ss a",
            Locale.US
        )
    }
    val cardTimeFormatter = remember(use24Hour) {
        DateTimeFormatter.ofPattern(
            if (use24Hour) "HH:mm" else "hh:mm a",
            Locale.US
        )
    }
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)
    }

    var showAddZoneSheet by remember { mutableStateOf(false) }
    var selectedMultiZoneCountry by remember { mutableStateOf<String?>(null) }
    var activeManageClock by remember { mutableStateOf<WorldClockEntry?>(null) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // TOP HEADER ROW: 12/24H TOGGLE + ADD CLOCK BUTTON
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.elevated)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .pressScaleClickable(
                        onClick = {
                            FeedbackHelper.tick(context)
                            use24Hour = !use24Hour
                            WorldClockStorage.save(context, use24Hour, savedClocks)
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp)
                    .testTag("world_clock_format_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Toggle 12/24h",
                    tint = theme.accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (use24Hour) "24-Hour Format" else "12-Hour (AM/PM)",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.textPrimary
                )
            }

            val featureGate = LocalFeatureGate.current
            val isUnlimitedClocksUnlocked = isPro || (featureGate?.isUnlocked(PremiumFeature.UNLIMITED_WORLD_CLOCKS) == true)
            val isProGate = !isUnlimitedClocksUnlocked && savedClocks.size >= 3
            GlassButton(
                text = if (isProGate) "Add Clock • PRO" else "Add Clock",
                icon = Icons.Default.Add,
                accentFill = if (isProGate) ProGold else theme.accent,
                textColor = BentoTextBlack,
                onClick = {
                    if (isProGate) {
                        if (featureGate != null) {
                            featureGate.require(PremiumFeature.UNLIMITED_WORLD_CLOCKS) {
                                selectedMultiZoneCountry = null
                                showAddZoneSheet = true
                            }
                        } else {
                            onOpenPaywall()
                        }
                    } else {
                        selectedMultiZoneCountry = null
                        showAddZoneSheet = true
                    }
                },
                testTag = "world_clock_add_button"
            )
        }

        // TOP CARD: "YOUR TIME" FROM DEVICE TIME ZONE
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(theme.elevated)
                .border(1.5.dp, theme.accent.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
                .padding(18.dp)
                .testTag("world_clock_your_time_card")
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
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isLocalDaytime) theme.bentoLime.copy(alpha = 0.22f)
                                else theme.bentoSky.copy(alpha = 0.22f)
                            )
                    ) {
                        Icon(
                            imageVector = if (isLocalDaytime) Icons.Default.WbSunny else Icons.Default.NightsStay,
                            contentDescription = if (isLocalDaytime) "Daytime" else "Nighttime",
                            tint = if (isLocalDaytime) theme.bentoLime else theme.bentoSky,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "YOUR TIME",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.accent
                        )
                        Text(
                            text = localZone.id.replace('_', ' '),
                            style = type.caption,
                            color = theme.textSecondary
                        )
                    }
                }

                // Abbreviation + UTC Offset Pill
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(theme.accent.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = localAbbrev,
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.accent
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(theme.surface)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = localUtcOffset,
                            style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = theme.textPrimary
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = localZdt.format(mainTimeFormatter),
                        style = type.gaugeHero,
                        color = theme.textPrimary
                    )
                    Text(
                        text = "${localZdt.format(dateFormatter)} • ${if (isLocalDaytime) "Daylight" else "Night"}",
                        style = type.body,
                        color = theme.textSecondary
                    )
                }

                MiniAnalogClockCanvas(
                    zdt = localZdt,
                    isDaytime = isLocalDaytime,
                    accentColor = theme.accent,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        // TIME SLIDER (MEETING PLANNER)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(theme.elevated)
                .border(
                    1.dp,
                    if (isPlannerActive) theme.bentoSky.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.08f),
                    RoundedCornerShape(22.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "MEETING PLANNER TIME SLIDER",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = if (isPlannerActive) theme.bentoSky else theme.textSecondary
                    )
                    Text(
                        text = if (isPlannerActive) {
                            "Previewing ${localZdt.format(cardTimeFormatter)} across all clocks"
                        } else {
                            "Drag slider to preview a chosen time across all world clocks"
                        },
                        style = type.caption,
                        color = theme.textPrimary
                    )
                }

                if (isPlannerActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.surface)
                            .pressScaleClickable(
                                onClick = {
                                    FeedbackHelper.tick(context)
                                    plannerMinutesOfDay = -1f
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = "Reset to live",
                            tint = theme.accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Live",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.accent
                        )
                    }
                }
            }

            val currentSliderVal = if (isPlannerActive) {
                plannerMinutesOfDay
            } else {
                (localZdt.hour * 60 + localZdt.minute).toFloat()
            }

            Slider(
                value = currentSliderVal,
                onValueChange = { newMins ->
                    // Snap to 15-minute increments for clean meeting planning
                    val snapped = ((newMins / 15f).roundToInt() * 15).coerceIn(0, 1425).toFloat()
                    plannerMinutesOfDay = snapped
                },
                valueRange = 0f..1425f,
                colors = SliderDefaults.colors(
                    thumbColor = theme.bentoSky,
                    activeTrackColor = theme.bentoSky,
                    inactiveTrackColor = theme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("world_clock_time_slider")
            )
        }

        // WORLD CLOCKS LIST HEADER
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "WORLD CLOCKS (${savedClocks.size})",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.textSecondary
            )
            Text(
                text = "Long-press or tap arrows to reorder/remove",
                style = type.caption,
                color = theme.textSecondary
            )
        }

        // ADDED WORLD CLOCK CARDS
        savedClocks.forEachIndexed { index, clock ->
            val zone = runCatching { ZoneId.of(clock.zoneId) }.getOrDefault(ZoneId.of("UTC"))
            val targetZdt = ZonedDateTime.ofInstant(effectiveInstant, zone)
            val abbrev = WorldZoneDirectory.resolveAbbreviation(clock.zoneId, effectiveInstant, clock.customAbbrev)
            val diffText = WorldZoneDirectory.formatDifferenceFromLocal(localZdt, targetZdt)
            val isDay = targetZdt.hour in 6..18

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(theme.elevated)
                    .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(24.dp))
                    .combinedClickable(
                        onClick = { activeManageClock = clock },
                        onLongClick = {
                            FeedbackHelper.heavyClick(context)
                            activeManageClock = clock
                        }
                    )
                    .padding(16.dp)
                    .testTag("world_clock_card_${clock.city.lowercase().replace(' ', '_')}")
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Left: City, Country, Abbreviation, Time difference, Day/Night badge
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isDay) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                contentDescription = if (isDay) "Day" else "Night",
                                tint = if (isDay) theme.bentoLime else theme.bentoSky,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = clock.city,
                                style = type.section.copy(fontWeight = FontWeight.Bold),
                                color = theme.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = "${clock.country} • $abbrev",
                            style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = theme.textSecondary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(theme.surface)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = diffText,
                                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                                    color = theme.accent
                                )
                            }
                            Text(
                                text = if (isDay) "Day" else "Night",
                                style = type.caption,
                                color = if (isDay) theme.bentoLime else theme.bentoSky
                            )
                        }
                    }

                    // Center: Mini Analog Clock Canvas
                    MiniAnalogClockCanvas(
                        zdt = targetZdt,
                        isDaytime = isDay,
                        accentColor = if (isDay) theme.bentoLime else theme.bentoSky,
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .size(50.dp)
                    )

                    // Right: Digital Time + Quick Reorder/Delete Controls
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = targetZdt.format(cardTimeFormatter),
                            style = type.gaugeMedium,
                            color = theme.textPrimary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (index > 0) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Move Up",
                                    tint = theme.textSecondary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .pressScaleClickable(
                                            onClick = {
                                                val item = savedClocks.removeAt(index)
                                                savedClocks.add(index - 1, item)
                                                WorldClockStorage.save(context, use24Hour, savedClocks)
                                            }
                                        )
                                )
                            }
                            if (index < savedClocks.lastIndex) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Move Down",
                                    tint = theme.textSecondary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .pressScaleClickable(
                                            onClick = {
                                                val item = savedClocks.removeAt(index)
                                                savedClocks.add(index + 1, item)
                                                WorldClockStorage.save(context, use24Hour, savedClocks)
                                            }
                                        )
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove Clock",
                                tint = theme.bentoCoral,
                                modifier = Modifier
                                    .size(20.dp)
                                    .pressScaleClickable(
                                        onClick = {
                                            savedClocks.removeAt(index)
                                            WorldClockStorage.save(context, use24Hour, savedClocks)
                                        }
                                    )
                            )
                        }
                    }
                }
            }

            // Reserved banner slot after the 3rd clock
            if (index == 2) {
                com.example.ads.InlineBannerAdSlot(placementTag = "world_clock_after_3rd")
            }
        }
    }

    // LONG-PRESS / TAP MANAGE CLOCK SHEET (REORDER / REMOVE)
    if (activeManageClock != null) {
        val target = activeManageClock!!
        val idx = savedClocks.indexOfFirst { it.zoneId == target.zoneId && it.city == target.city }
        GlassSheet(
            onDismissRequest = { activeManageClock = null },
            hazeState = hazeState
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${target.city}, ${target.country}",
                    style = type.title,
                    color = theme.textPrimary
                )
                Text(
                    text = "Time Zone: ${target.zoneId}",
                    style = type.caption,
                    color = theme.textSecondary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassButton(
                        text = "Move Up",
                        icon = Icons.Default.KeyboardArrowUp,
                        onClick = {
                            if (idx > 0) {
                                val item = savedClocks.removeAt(idx)
                                savedClocks.add(idx - 1, item)
                                WorldClockStorage.save(context, use24Hour, savedClocks)
                            }
                            activeManageClock = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Move Down",
                        icon = Icons.Default.KeyboardArrowDown,
                        onClick = {
                            if (idx in 0 until savedClocks.lastIndex) {
                                val item = savedClocks.removeAt(idx)
                                savedClocks.add(idx + 1, item)
                                WorldClockStorage.save(context, use24Hour, savedClocks)
                            }
                            activeManageClock = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                GlassButton(
                    text = "Remove Clock",
                    icon = Icons.Default.DeleteOutline,
                    accentFill = theme.bentoCoral,
                    onClick = {
                        if (idx >= 0) {
                            savedClocks.removeAt(idx)
                            WorldClockStorage.save(context, use24Hour, savedClocks)
                        }
                        activeManageClock = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // ADD WORLD TIME ZONE SHEET (SEARCH BY COUNTRY, CITY, OR ABBREVIATION + MULTI-ZONE COUNTRY PICKER)
    if (showAddZoneSheet) {
        var query by remember { mutableStateOf("") }

        val allEntries = WorldZoneDirectory.allAvailableEntries
        val multiZoneCountries = remember(allEntries) {
            allEntries.groupBy { it.country }
                .filter { it.value.size > 1 }
        }

        val filteredEntries = remember(query, allEntries, selectedMultiZoneCountry, effectiveInstant) {
            if (selectedMultiZoneCountry != null) {
                allEntries.filter { it.country.equals(selectedMultiZoneCountry, ignoreCase = true) }
            } else if (query.isBlank()) {
                allEntries.take(35)
            } else {
                val q = query.trim()
                allEntries.filter { entry ->
                    val abbrev = WorldZoneDirectory.resolveAbbreviation(entry.zoneId, effectiveInstant, entry.customAbbrev)
                    entry.city.contains(q, ignoreCase = true) ||
                        entry.country.contains(q, ignoreCase = true) ||
                        entry.zoneId.contains(q, ignoreCase = true) ||
                        abbrev.contains(q, ignoreCase = true)
                }.take(40)
            }
        }

        val matchingMultiZoneCountries = remember(query, multiZoneCountries) {
            if (query.isBlank()) {
                listOf("United States", "Canada", "Australia", "Brazil", "Russia", "Indonesia", "Mexico")
            } else {
                multiZoneCountries.keys.filter { it.contains(query.trim(), ignoreCase = true) }
            }
        }

        GlassSheet(
            onDismissRequest = {
                selectedMultiZoneCountry = null
                showAddZoneSheet = false
            },
            hazeState = hazeState
        ) {
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
                        text = if (selectedMultiZoneCountry != null) {
                            "$selectedMultiZoneCountry Time Zones"
                        } else {
                            "Add World Clock"
                        },
                        style = type.title,
                        color = theme.textPrimary
                    )
                    if (selectedMultiZoneCountry != null) {
                        Text(
                            text = "All Countries",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.accent,
                            modifier = Modifier.pressScaleClickable(
                                onClick = { selectedMultiZoneCountry = null }
                            )
                        )
                    }
                }

                if (selectedMultiZoneCountry == null) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text(
                                "Search cities",
                                style = type.body,
                                color = theme.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = theme.accent)
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = theme.textSecondary,
                                    modifier = Modifier.pressScaleClickable(onClick = { query = "" })
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.accent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedContainerColor = theme.elevated,
                            unfocusedContainerColor = theme.elevated,
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    )

                    // Multi-zone country picker pills (LazyRow with 20.dp side padding and edge fade)
                    if (matchingMultiZoneCountries.isNotEmpty()) {
                        Text(
                            text = "POPULAR COUNTRIES",
                            style = type.section,
                            color = theme.accent
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalChipRowEdgeFade()
                        ) {
                            items(matchingMultiZoneCountries) { countryName ->
                                val count = multiZoneCountries[countryName]?.size ?: 2
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(theme.elevated)
                                        .border(1.dp, theme.accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                        .pressScaleClickable(
                                            onClick = { selectedMultiZoneCountry = countryName }
                                        )
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = null,
                                        tint = theme.accent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "$countryName ($count)",
                                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                                        color = theme.textPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Matching Zone Rows
                filteredEntries.take(12).forEach { entry ->
                    val z = runCatching { ZoneId.of(entry.zoneId) }.getOrDefault(ZoneId.of("UTC"))
                    val zdt = ZonedDateTime.ofInstant(effectiveInstant, z)
                    val abbrev = WorldZoneDirectory.resolveAbbreviation(entry.zoneId, effectiveInstant, entry.customAbbrev)
                    val diff = WorldZoneDirectory.formatDifferenceFromLocal(localZdt, zdt)

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.elevated)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .pressScaleClickable(
                                onClick = {
                                    FeedbackHelper.confirmClick(context)
                                    if (savedClocks.none { it.zoneId == entry.zoneId && it.city == entry.city }) {
                                        savedClocks.add(entry)
                                        WorldClockStorage.save(context, use24Hour, savedClocks)
                                    }
                                    selectedMultiZoneCountry = null
                                    showAddZoneSheet = false
                                }
                            )
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${entry.city}, ${entry.country}",
                                style = type.body.copy(fontWeight = FontWeight.Bold),
                                color = theme.textPrimary
                            )
                            Text(
                                text = "${entry.zoneId} • $abbrev ($diff)",
                                style = type.caption,
                                color = theme.textSecondary
                            )
                        }
                        Text(
                            text = zdt.format(cardTimeFormatter),
                            style = type.gaugeSmall,
                            color = theme.accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniAnalogClockCanvas(
    zdt: ZonedDateTime,
    isDaytime: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Dial face
        drawCircle(
            color = if (isDaytime) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.35f),
            radius = radius,
            center = center
        )
        drawCircle(
            color = accentColor.copy(alpha = 0.65f),
            radius = radius,
            center = center,
            style = Stroke(width = 1.8.dp.toPx())
        )

        // 12 hour tick marks
        for (i in 0 until 12) {
            val angleRad = (i * 30.0 - 90.0) * PI / 180.0
            val outerR = radius * 0.88f
            val innerR = if (i % 3 == 0) radius * 0.70f else radius * 0.78f
            val start = Offset(
                x = center.x + (innerR * cos(angleRad)).toFloat(),
                y = center.y + (innerR * sin(angleRad)).toFloat()
            )
            val end = Offset(
                x = center.x + (outerR * cos(angleRad)).toFloat(),
                y = center.y + (outerR * sin(angleRad)).toFloat()
            )
            drawLine(
                color = theme.textSecondary.copy(alpha = if (i % 3 == 0) 0.9f else 0.45f),
                start = start,
                end = end,
                strokeWidth = if (i % 3 == 0) 1.8.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        val sec = zdt.second
        val min = zdt.minute + sec / 60f
        val hr = (zdt.hour % 12) + min / 60f

        val hrAngle = (hr * 30.0 - 90.0) * PI / 180.0
        val minAngle = (min * 6.0 - 90.0) * PI / 180.0
        val secAngle = (sec * 6.0 - 90.0) * PI / 180.0

        // Hour hand
        drawLine(
            color = theme.textPrimary,
            start = center,
            end = Offset(
                x = center.x + (radius * 0.48f * cos(hrAngle)).toFloat(),
                y = center.y + (radius * 0.48f * sin(hrAngle)).toFloat()
            ),
            strokeWidth = 2.4.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Minute hand
        drawLine(
            color = theme.textPrimary,
            start = center,
            end = Offset(
                x = center.x + (radius * 0.70f * cos(minAngle)).toFloat(),
                y = center.y + (radius * 0.70f * sin(minAngle)).toFloat()
            ),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Second hand
        drawLine(
            color = accentColor,
            start = center,
            end = Offset(
                x = center.x + (radius * 0.76f * cos(secAngle)).toFloat(),
                y = center.y + (radius * 0.76f * sin(secAngle)).toFloat()
            ),
            strokeWidth = 1.1.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Center cap
        drawCircle(
            color = accentColor,
            radius = 2.6.dp.toPx(),
            center = center
        )
    }
}
