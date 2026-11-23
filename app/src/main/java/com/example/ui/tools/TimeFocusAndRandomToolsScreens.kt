package com.example.ui.tools

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.billing.LocalFeatureGate
import com.example.billing.PremiumFeature
import com.example.ui.components.FeedbackHelper
import com.example.ui.components.ProBadge
import com.example.ui.theme.DyneTheme
import com.example.ui.theme.LocalDyneTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.*
import kotlin.random.Random

internal val DyneTheme.cardBackground: Color
    get() = elevated

internal val DyneTheme.backgroundTop: Color
    get() = surface

internal fun FeedbackHelper.hapticTick(view: android.view.View) {
    view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
}

internal fun FeedbackHelper.hapticClick(view: android.view.View) {
    view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
}

internal fun FeedbackHelper.hapticSuccess(view: android.view.View) {
    view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
}

// Helper to play a short, clean acoustic click or chime offline via ToneGenerator
private fun playToolTone(toneType: Int = ToneGenerator.TONE_PROP_BEEP, durationMs: Int = 80) {
    runCatching {
        val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
        tg.startTone(toneType, durationMs)
    }
}

// ================================================================================================
// 1. TALLY COUNTER (FREE: haptic, sound, goal, several named counters)
// ================================================================================================

data class NamedTallyCounter(
    val id: Long,
    val name: String,
    val count: Int,
    val goal: Int,
    val step: Int = 1
)

@Composable
fun TallyCounterScreen(forcedModeId: String? = null) {
    val theme = LocalDyneTheme.current
    val view = LocalView.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dyne_tally_prefs", Context.MODE_PRIVATE) }

    val counters = remember {
        mutableStateListOf<NamedTallyCounter>().apply {
            val saved = prefs.getString("named_counters_v1", null)
            if (!saved.isNullOrBlank()) {
                saved.split("\n").forEach { line ->
                    val p = line.split("|")
                    if (p.size >= 4) {
                        add(
                            NamedTallyCounter(
                                id = p[0].toLongOrNull() ?: System.currentTimeMillis(),
                                name = p[1],
                                count = p[2].toIntOrNull() ?: 0,
                                goal = p[3].toIntOrNull() ?: 100
                            )
                        )
                    }
                }
            }
            if (isEmpty()) {
                add(NamedTallyCounter(1L, "Daily Reps", 0, 50))
                add(NamedTallyCounter(2L, "Water Glasses", 3, 8))
                add(NamedTallyCounter(3L, "Inventory / Headcount", 12, 100))
            }
        }
    }

    var activeIndex by remember { mutableIntStateOf(0) }
    var hapticEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var newCounterName by remember { mutableStateOf("") }
    var newCounterGoalStr by remember { mutableStateOf("50") }

    fun saveCounters() {
        val raw = counters.joinToString("\n") { "${it.id}|${it.name}|${it.count}|${it.goal}" }
        prefs.edit().putString("named_counters_v1", raw).apply()
    }

    val current = counters.getOrNull(activeIndex.coerceIn(0, (counters.size - 1).coerceAtLeast(0)))
        ?: NamedTallyCounter(1L, "Default", 0, 50)

    fun updateCount(delta: Int) {
        val idx = activeIndex.coerceIn(0, counters.size - 1)
        val updatedCount = (counters[idx].count + delta).coerceAtLeast(0)
        val hitGoal = updatedCount == counters[idx].goal && delta > 0
        counters[idx] = counters[idx].copy(count = updatedCount)
        saveCounters()
        if (hapticEnabled) {
            if (hitGoal) FeedbackHelper.hapticSuccess(view) else FeedbackHelper.hapticClick(view)
        }
        if (soundEnabled) {
            playToolTone(
                if (hitGoal) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_BEEP,
                if (hitGoal) 180 else 55
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Named counter selector chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(counters) { index, item ->
                val selected = index == activeIndex
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) theme.accent.copy(alpha = 0.22f) else theme.cardBackground.copy(alpha = 0.65f),
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (selected) theme.accent else theme.textSecondary.copy(alpha = 0.2f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            FeedbackHelper.hapticTick(view)
                            activeIndex = index
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${item.name} (${item.count}/${item.goal})",
                            color = if (selected) theme.accent else theme.textPrimary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Main Interactive Tally Ring + Tap Target
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = theme.cardBackground.copy(alpha = 0.85f),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(26.dp))
                .clickable { updateCount(1) }
                .testTag("tally_main_tap_area")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = current.name.uppercase(),
                    color = theme.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                val progress = (current.count.toFloat() / current.goal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(190.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 14.dp.toPx()
                        drawArc(
                            color = theme.textSecondary.copy(alpha = 0.16f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = if (progress >= 1f) Color(0xFF00E676) else theme.accent,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = current.count.toString(),
                            color = theme.textPrimary,
                            fontSize = 54.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("tally_count_value")
                        )
                        Text(
                            text = "Goal: ${current.goal} (${(progress * 100).roundToInt()}%)",
                            color = if (progress >= 1f) Color(0xFF00E676) else theme.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { updateCount(-1) },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("tally_decrement")
                    ) {
                        Text("-1", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Button(
                        onClick = { updateCount(1) },
                        modifier = Modifier.weight(1.6f).height(48.dp).testTag("tally_increment")
                    ) {
                        Text("+1 COUNT", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val idx = activeIndex.coerceIn(0, counters.size - 1)
                            counters[idx] = counters[idx].copy(count = 0)
                            saveCounters()
                            if (hapticEnabled) FeedbackHelper.hapticTick(view)
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("tally_reset")
                    ) {
                        Text("Reset", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Haptic, Sound, and Goal Configuration
        GlassInputCard(title = "Feedback &Named Counters") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Vibration, contentDescription = null, tint = theme.accent, modifier = Modifier.size(18.dp))
                    Text("Haptic Feedback", color = theme.textPrimary, fontSize = 13.sp)
                    Switch(checked = hapticEnabled, onCheckedChange = { hapticEnabled = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = theme.accent, modifier = Modifier.size(18.dp))
                    Text("Click Sound", color = theme.textPrimary, fontSize = 13.sp)
                    Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newCounterName,
                    onValueChange = { newCounterName = it },
                    label = { Text("New Counter Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1.3f)
                )
                OutlinedTextField(
                    value = newCounterGoalStr,
                    onValueChange = { newCounterGoalStr = it },
                    label = { Text("Goal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.8f)
                )
                Button(
                    onClick = {
                        val goal = (newCounterGoalStr.toIntOrNull() ?: 50).coerceAtLeast(1)
                        val title = newCounterName.trim().ifEmpty { "Counter ${counters.size + 1}" }
                        counters.add(NamedTallyCounter(System.currentTimeMillis(), title, 0, goal))
                        activeIndex = counters.lastIndex
                        newCounterName = ""
                        saveCounters()
                    },
                    modifier = Modifier.testTag("tally_add_counter")
                ) {
                    Text("Add")
                }
            }
        }
    }
}

// ================================================================================================
// 2. POMODORO FOCUS TIMER (FREE: work/break cycles, sound) +
//    POMODORO STATISTICS & STREAKS (PREMIUM: PremiumFeature.POMODORO_STATS)
// ================================================================================================

@Composable
fun PomodoroFocusAndStatsScreen(
    forcedModeId: String? = null,
    isPro: Boolean = false,
    onOpenPaywall: () -> Unit = {}
) {
    val theme = LocalDyneTheme.current
    val view = LocalView.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dyne_pomodoro_prefs", Context.MODE_PRIVATE) }

    val featureGate = LocalFeatureGate.current
    val statsUnlocked = isPro || (featureGate?.isUnlocked(PremiumFeature.POMODORO_STATS) == true)
    var activeTab by remember(forcedModeId) {
        mutableStateOf(if (forcedModeId == "stats") "stats" else "focus")
    }

    var workMinutes by remember { mutableIntStateOf(25) }
    var shortBreakMinutes by remember { mutableIntStateOf(5) }
    var longBreakMinutes by remember { mutableIntStateOf(15) }

    var phase by remember { mutableStateOf("Work") } // "Work", "Short Break", "Long Break"
    var remainingSeconds by remember { mutableIntStateOf(25 * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(true) }

    var completedSessionsToday by remember { mutableIntStateOf(prefs.getInt("pomo_today_sessions", 3)) }
    var currentStreakDays by remember { mutableIntStateOf(prefs.getInt("pomo_streak_days", 5)) }
    var totalFocusMinutes by remember { mutableIntStateOf(prefs.getInt("pomo_total_minutes", 425)) }

    val totalPhaseSeconds = when (phase) {
        "Short Break" -> shortBreakMinutes * 60
        "Long Break" -> longBreakMinutes * 60
        else -> workMinutes * 60
    }.coerceAtLeast(1)

    var timerScheduleNote by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isRunning, remainingSeconds, phase) {
        if (isRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
            if (remainingSeconds == 0) {
                isRunning = false
                com.example.service.DyneNotificationCenter.cancelUserTimerAlarm(context, "pomodoro_timer")
                com.example.service.DyneNotificationCenter.postTimerCompletedNotification(
                    context = context,
                    title = "Pomodoro $phase Complete",
                    body = if (phase == "Work") "Focus session complete! Time for a break." else "Break complete! Ready for your next focus session.",
                    deepLinkFeatureId = "pomodoro_timer"
                )
                FeedbackHelper.hapticSuccess(view)
                if (soundEnabled) {
                    playToolTone(ToneGenerator.TONE_PROP_ACK, 300)
                }
                if (phase == "Work") {
                    completedSessionsToday += 1
                    totalFocusMinutes += workMinutes
                    prefs.edit()
                        .putInt("pomo_today_sessions", completedSessionsToday)
                        .putInt("pomo_total_minutes", totalFocusMinutes)
                        .apply()
                    phase = if (completedSessionsToday % 4 == 0) "Long Break" else "Short Break"
                    remainingSeconds = if (phase == "Long Break") longBreakMinutes * 60 else shortBreakMinutes * 60
                } else {
                    phase = "Work"
                    remainingSeconds = workMinutes * 60
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tab bar: Focus Timer (Free) | Statistics & Streaks (Pro)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("focus" to "Focus Timer", "stats" to "Stats & Streaks").forEach { (id, title) ->
                val selected = activeTab == id
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) theme.accent.copy(alpha = 0.22f) else theme.cardBackground.copy(alpha = 0.65f),
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.dp,
                            if (selected) theme.accent else theme.textSecondary.copy(alpha = 0.2f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            FeedbackHelper.hapticTick(view)
                            if (id == "stats" && !statsUnlocked) {
                                onOpenPaywall()
                            } else {
                                activeTab = id
                            }
                        }
                        .testTag("pomo_tab_$id")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            color = if (selected) theme.accent else theme.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (id == "stats" && !statsUnlocked) {
                            Spacer(Modifier.width(6.dp))
                            ProBadge()
                        }
                    }
                }
            }
        }

        if (activeTab == "focus") {
            // Phase selector chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Work" to workMinutes,
                    "Short Break" to shortBreakMinutes,
                    "Long Break" to longBreakMinutes
                ).forEach { (phaseName, mins) ->
                    FilterChip(
                        selected = phase == phaseName,
                        onClick = {
                            isRunning = false
                            phase = phaseName
                            remainingSeconds = mins * 60
                        },
                        label = { Text("$phaseName (${mins}m)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Timer ring card
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = theme.cardBackground.copy(alpha = 0.85f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(26.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val progress = remainingSeconds.toFloat() / totalPhaseSeconds.toFloat()
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 14.dp.toPx()
                            drawArc(
                                color = theme.textSecondary.copy(alpha = 0.16f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = if (phase == "Work") theme.accent else Color(0xFF00E676),
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val mins = remainingSeconds / 60
                            val secs = remainingSeconds % 60
                            Text(
                                text = "%02d:%02d".format(mins, secs),
                                color = theme.textPrimary,
                                fontSize = 46.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.testTag("pomodoro_time_display")
                            )
                            Text(
                                text = "$phase • Cycle ${(completedSessionsToday % 4) + 1}/4",
                                color = theme.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                FeedbackHelper.hapticClick(view)
                                val nextRunning = !isRunning
                                isRunning = nextRunning
                                if (nextRunning && remainingSeconds > 0) {
                                    val res = com.example.service.DyneNotificationCenter.scheduleUserTimerAlarm(
                                        context = context,
                                        timerId = "pomodoro_timer",
                                        title = "Pomodoro $phase Complete",
                                        body = if (phase == "Work") "Focus session complete! Time for a break." else "Break complete! Ready for your next focus session.",
                                        triggerAtMs = System.currentTimeMillis() + remainingSeconds * 1000L,
                                        deepLinkFeatureId = "pomodoro_timer"
                                    )
                                    timerScheduleNote = res.note
                                } else {
                                    com.example.service.DyneNotificationCenter.cancelUserTimerAlarm(context, "pomodoro_timer")
                                }
                            },
                            modifier = Modifier.weight(1.5f).height(48.dp).testTag("pomodoro_start_pause")
                        ) {
                            Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (isRunning) "Pause" else "Start Focus", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                FeedbackHelper.hapticTick(view)
                                isRunning = false
                                remainingSeconds = totalPhaseSeconds
                                com.example.service.DyneNotificationCenter.cancelUserTimerAlarm(context, "pomodoro_timer")
                            },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("pomodoro_reset")
                        ) {
                            Text("Reset")
                        }
                    }
                    if (!timerScheduleNote.isNullOrBlank()) {
                        Text(
                            text = timerScheduleNote!!,
                            color = theme.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            GlassInputCard(title = "Cycle Durations & Sound") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Completion Sound Chime", color = theme.textPrimary, fontSize = 13.sp)
                    Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 25, 45, 50).forEach { w ->
                        FilterChip(
                            selected = workMinutes == w,
                            onClick = {
                                workMinutes = w
                                if (!isRunning && phase == "Work") remainingSeconds = w * 60
                            },
                            label = { Text("${w}m Work") }
                        )
                    }
                }
            }
        } else {
            // Pomodoro Statistics & Streaks (PRO)
            val weeklyBars = remember(completedSessionsToday) {
                listOf(4, 5, 3, 6, 4, 5, completedSessionsToday.coerceAtLeast(1))
            }
            val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Today")

            GlassResultCard(
                headlineLabel = "Active Focus Streak",
                headlineValue = "$currentStreakDays Days 🔥",
                rows = listOf(
                    "Pomodoros Completed Today" to "$completedSessionsToday sessions",
                    "Total Focus Time Logged" to "${totalFocusMinutes / 60}h ${totalFocusMinutes % 60}m",
                    "Daily Completion Goal (4 sessions)" to "${((completedSessionsToday / 4.0) * 100).roundToInt().coerceAtMost(100)}%"
                )
            )

            GlassInputCard(title = "7-Day Focus Sessions Chart") {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val maxBar = (weeklyBars.maxOrNull() ?: 1).coerceAtLeast(1)
                    val slotW = size.width / weeklyBars.size
                    val barW = slotW * 0.55f
                    weeklyBars.forEachIndexed { index, count ->
                        val h = (count.toFloat() / maxBar) * (size.height - 20.dp.toPx())
                        val x = index * slotW + (slotW - barW) / 2f
                        drawRoundRect(
                            color = if (index == weeklyBars.lastIndex) Color(0xFFFFD54F) else theme.accent,
                            topLeft = Offset(x, size.height - h),
                            size = Size(barW, h),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dayLabels.forEach { d ->
                        Text(d, fontSize = 11.sp, color = theme.textSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ================================================================================================
// 3. INTERVAL TIMER PRO (PREMIUM: rounds, rest, presets)
// ================================================================================================

data class IntervalPreset(
    val id: String,
    val name: String,
    val rounds: Int,
    val workSec: Int,
    val restSec: Int,
    val warmupSec: Int
)

@Composable
fun IntervalTimerProScreen(forcedModeId: String? = null) {
    val theme = LocalDyneTheme.current
    val view = LocalView.current

    val presets = remember {
        listOf(
            IntervalPreset("tabata", "Tabata HIIT", 8, 20, 10, 10),
            IntervalPreset("hiit_30", "30/30 Circuit", 10, 30, 30, 15),
            IntervalPreset("boxing", "Boxing 3m/1m", 6, 180, 60, 15),
            IntervalPreset("emom", "EMOM 45/15", 12, 45, 15, 10)
        )
    }

    var rounds by remember { mutableIntStateOf(8) }
    var workSec by remember { mutableIntStateOf(20) }
    var restSec by remember { mutableIntStateOf(10) }
    var warmupSec by remember { mutableIntStateOf(10) }

    var currentRound by remember { mutableIntStateOf(1) }
    var currentStage by remember { mutableStateOf("Warmup") } // Warmup, Work, Rest, Complete
    var stageRemainingSec by remember { mutableIntStateOf(10) }
    var isRunning by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var intervalScheduleNote by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isRunning, stageRemainingSec, currentStage, currentRound) {
        if (isRunning && stageRemainingSec > 0) {
            delay(1000L)
            stageRemainingSec -= 1
            if (stageRemainingSec in 1..3) {
                playToolTone(ToneGenerator.TONE_PROP_BEEP, 60)
            } else if (stageRemainingSec == 0) {
                FeedbackHelper.hapticSuccess(view)
                playToolTone(ToneGenerator.TONE_PROP_ACK, 200)
                when (currentStage) {
                    "Warmup" -> {
                        currentStage = "Work"
                        stageRemainingSec = workSec
                    }
                    "Work" -> {
                        if (currentRound < rounds) {
                            currentStage = "Rest"
                            stageRemainingSec = restSec
                        } else {
                            currentStage = "Complete"
                            isRunning = false
                            com.example.service.DyneNotificationCenter.cancelUserTimerAlarm(context, "interval_timer")
                            com.example.service.DyneNotificationCenter.postTimerCompletedNotification(
                                context = context,
                                title = "Interval Workout Complete",
                                body = "Completed all $rounds rounds ($workSec s work / $restSec s rest).",
                                deepLinkFeatureId = "interval_timer"
                            )
                        }
                    }
                    "Rest" -> {
                        currentRound += 1
                        currentStage = "Work"
                        stageRemainingSec = workSec
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Built-in Presets
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets) { p ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = theme.cardBackground.copy(alpha = 0.75f),
                    modifier = Modifier
                        .border(1.dp, theme.accent.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable {
                            FeedbackHelper.hapticTick(view)
                            isRunning = false
                            rounds = p.rounds
                            workSec = p.workSec
                            restSec = p.restSec
                            warmupSec = p.warmupSec
                            currentRound = 1
                            currentStage = "Warmup"
                            stageRemainingSec = p.warmupSec
                        }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(p.name, color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${p.rounds}x • ${p.workSec}s / ${p.restSec}s", color = theme.accent, fontSize = 11.sp)
                    }
                }
            }
        }

        // Main stage readout
        val stageColor = when (currentStage) {
            "Work" -> Color(0xFF00E676)
            "Rest" -> Color(0xFF29B6F6)
            "Complete" -> Color(0xFFFFD54F)
            else -> theme.accent
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.cardBackground.copy(alpha = 0.88f),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, stageColor.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${currentStage.uppercase()} • ROUND $currentRound / $rounds",
                    color = stageColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "%02d:%02d".format(stageRemainingSec / 60, stageRemainingSec % 60),
                    color = theme.textPrimary,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("interval_timer_display")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            FeedbackHelper.hapticClick(view)
                            if (currentStage == "Complete") {
                                currentRound = 1
                                currentStage = "Warmup"
                                stageRemainingSec = warmupSec
                            }
                            val nextRunning = !isRunning
                            isRunning = nextRunning
                            if (nextRunning) {
                                val totalRemainingSec = stageRemainingSec + (rounds - currentRound).coerceAtLeast(0) * (workSec + restSec)
                                val res = com.example.service.DyneNotificationCenter.scheduleUserTimerAlarm(
                                    context = context,
                                    timerId = "interval_timer",
                                    title = "Interval Workout Complete",
                                    body = "Completed all $rounds rounds ($workSec s work / $restSec s rest).",
                                    triggerAtMs = System.currentTimeMillis() + totalRemainingSec.coerceAtLeast(1) * 1000L,
                                    deepLinkFeatureId = "interval_timer"
                                )
                                intervalScheduleNote = res.note
                            } else {
                                com.example.service.DyneNotificationCenter.cancelUserTimerAlarm(context, "interval_timer")
                            }
                        },
                        modifier = Modifier.weight(1.4f).height(48.dp).testTag("interval_start_pause")
                    ) {
                        Text(if (isRunning) "Pause" else "Start Interval", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            isRunning = false
                            currentRound = 1
                            currentStage = "Warmup"
                            stageRemainingSec = warmupSec
                            com.example.service.DyneNotificationCenter.cancelUserTimerAlarm(context, "interval_timer")
                        },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Reset")
                    }
                }
                if (!intervalScheduleNote.isNullOrBlank()) {
                    Text(
                        text = intervalScheduleNote!!,
                        color = theme.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        GlassInputCard(title = "Custom Rounds, Work & Rest") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rounds: $rounds", color = theme.textPrimary, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { if (rounds > 1) rounds-- }) { Text("-") }
                    OutlinedButton(onClick = { rounds++ }) { Text("+") }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Work Duration: ${workSec}s", color = theme.textPrimary, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { if (workSec > 5) workSec -= 5 }) { Text("-5s") }
                    OutlinedButton(onClick = { workSec += 5 }) { Text("+5s") }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rest Duration: ${restSec}s", color = theme.textPrimary, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { if (restSec > 5) restSec -= 5 }) { Text("-5s") }
                    OutlinedButton(onClick = { restSec += 5 }) { Text("+5s") }
                }
            }
        }
    }
}

// ================================================================================================
// 4. COUNTDOWN & EVENT TIMERS WITH NOTIFICATIONS (PREMIUM: PremiumFeature.EVENT_COUNTDOWN)
// ================================================================================================

data class CountdownEventItem(
    val id: Long,
    val title: String,
    val targetDateStr: String,
    val category: String
)

@Composable
fun EventCountdownProScreen(forcedModeId: String? = null) {
    val theme = LocalDyneTheme.current
    val context = LocalContext.current
    val view = LocalView.current

    val events = remember {
        mutableStateListOf(
            CountdownEventItem(1L, "Summer Expedition", LocalDate.now().plusDays(18).toString(), "Travel"),
            CountdownEventItem(2L, "Product Launch Milestone", LocalDate.now().plusDays(45).toString(), "Work"),
            CountdownEventItem(3L, "Annual Renewal & Filing", LocalDate.now().plusDays(90).toString(), "Finance")
        )
    }

    var newTitle by remember { mutableStateOf("") }
    var newDateStr by remember { mutableStateOf(LocalDate.now().plusDays(14).toString()) }
    var notificationStatus by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            postCountdownNotification(context, events.firstOrNull()?.title ?: "Upcoming Event")
            notificationStatus = "Event reminder notification posted!"
        } else {
            notificationStatus = "Notification permission denied."
        }
    }

    fun triggerEventNotification(title: String) {
        FeedbackHelper.hapticClick(view)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        postCountdownNotification(context, title)
        notificationStatus = "Notification alert sent for \"$title\""
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(events, key = { it.id }) { ev ->
            val targetDate = runCatching { LocalDate.parse(ev.targetDateStr) }.getOrNull() ?: LocalDate.now().plusDays(7)
            val now = LocalDateTime.now()
            val targetDateTime = targetDate.atTime(9, 0)
            val totalSeconds = ChronoUnit.SECONDS.between(now, targetDateTime).coerceAtLeast(0L)
            val days = totalSeconds / 86400
            val hours = (totalSeconds % 86400) / 3600
            val mins = (totalSeconds % 3600) / 60

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = theme.cardBackground.copy(alpha = 0.85f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, theme.accent.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ev.title, color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Target Date: ${ev.targetDateStr} • ${ev.category}", color = theme.textSecondary, fontSize = 12.sp)
                        }
                        Row {
                            IconButton(
                                onClick = { triggerEventNotification(ev.title) },
                                modifier = Modifier.testTag("notify_event_${ev.id}")
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = "Notify", tint = theme.accent)
                            }
                            IconButton(onClick = { events.remove(ev) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = theme.textSecondary)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            days.toString() to "DAYS",
                            hours.toString() to "HOURS",
                            mins.toString() to "MINS"
                        ).forEach { (num, unit) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = theme.backgroundTop.copy(alpha = 0.55f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(num, color = theme.accent, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, fontFamily = FontFamily.Monospace)
                                    Text(unit, color = theme.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            if (notificationStatus != null) {
                Text(
                    text = notificationStatus!!,
                    color = Color(0xFF00E676),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            GlassInputCard(title = "Add Countdown Event") {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Event Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newDateStr,
                    onValueChange = { newDateStr = it },
                    label = { Text("Target Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            FeedbackHelper.hapticClick(view)
                            events.add(0, CountdownEventItem(System.currentTimeMillis(), newTitle.trim(), newDateStr.trim(), "Event"))
                            newTitle = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("add_countdown_event")
                ) {
                    Icon(Icons.Default.AddAlarm, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create Event Timer")
                }
            }
        }
    }
}

private fun postCountdownNotification(context: Context, eventTitle: String) {
    com.example.service.DyneNotificationCenter.postTimerCompletedNotification(
        context = context,
        title = "Countdown Reminder: $eventTitle",
        body = "Your tracked milestone \"$eventTitle\" is active in Dyne.",
        deepLinkFeatureId = "event_countdown"
    )
}

// ================================================================================================
// 5. DICE, COIN FLIP, SPINNER AND DECISION WHEEL (FREE: randomizer_suite)
// ================================================================================================

@Composable
fun RandomizerSuiteScreen(forcedModeId: String? = null) {
    val theme = LocalDyneTheme.current
    val view = LocalView.current

    val modes = remember {
        listOf(
            "dice" to "Dice Roller",
            "coin" to "Coin Flip",
            "spinner" to "Spinner",
            "wheel" to "Decision Wheel"
        )
    }
    var activeMode by remember(forcedModeId) { mutableStateOf(forcedModeId ?: "dice") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(modes) { (id, label) ->
                val selected = activeMode == id
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) theme.accent.copy(alpha = 0.22f) else theme.cardBackground.copy(alpha = 0.65f),
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (selected) theme.accent else theme.textSecondary.copy(alpha = 0.2f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            FeedbackHelper.hapticTick(view)
                            activeMode = id
                        }
                        .testTag("random_mode_$id")
                ) {
                    Text(
                        text = label,
                        color = if (selected) theme.accent else theme.textPrimary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeMode) {
                "dice" -> DiceRollerView()
                "coin" -> CoinFlipView()
                "spinner" -> PointerSpinnerView()
                "wheel" -> DecisionWheelView()
                else -> DiceRollerView()
            }
        }
    }
}

@Composable
private fun DiceRollerView() {
    val theme = LocalDyneTheme.current
    val view = LocalView.current
    var diceCount by remember { mutableIntStateOf(2) }
    var diceValues by remember { mutableStateOf(listOf(4, 6)) }
    var rollCount by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassInputCard(title = "Dice Count (1 to 6)") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..6).forEach { count ->
                    FilterChip(
                        selected = diceCount == count,
                        onClick = {
                            diceCount = count
                            diceValues = List(count) { Random.nextInt(1, 7) }
                        },
                        label = { Text("${count}D6") }
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.cardBackground.copy(alpha = 0.85f),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    diceValues.take(3).forEach { face ->
                        DieFaceCanvas(face = face)
                    }
                }
                if (diceValues.size > 3) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        diceValues.drop(3).forEach { face ->
                            DieFaceCanvas(face = face)
                        }
                    }
                }

                Text(
                    text = "Total Sum: ${diceValues.sum()} (Roll #$rollCount)",
                    color = theme.accent,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )

                Button(
                    onClick = {
                        FeedbackHelper.hapticClick(view)
                        playToolTone(ToneGenerator.TONE_PROP_BEEP, 60)
                        diceValues = List(diceCount) { Random.nextInt(1, 7) }
                        rollCount += 1
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("roll_dice_button")
                ) {
                    Icon(Icons.Default.Casino, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("ROLL DICE", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun DieFaceCanvas(face: Int) {
    val theme = LocalDyneTheme.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = theme.backgroundTop.copy(alpha = 0.85f),
        modifier = Modifier
            .size(72.dp)
            .border(1.5.dp, theme.accent, RoundedCornerShape(16.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            val r = 5.dp.toPx()
            val w = size.width
            val h = size.height
            val tl = Offset(w * 0.2f, h * 0.2f)
            val tr = Offset(w * 0.8f, h * 0.2f)
            val ml = Offset(w * 0.2f, h * 0.5f)
            val mc = Offset(w * 0.5f, h * 0.5f)
            val mr = Offset(w * 0.8f, h * 0.5f)
            val bl = Offset(w * 0.2f, h * 0.8f)
            val br = Offset(w * 0.8f, h * 0.8f)

            val pips = when (face) {
                1 -> listOf(mc)
                2 -> listOf(tl, br)
                3 -> listOf(tl, mc, br)
                4 -> listOf(tl, tr, bl, br)
                5 -> listOf(tl, tr, mc, bl, br)
                else -> listOf(tl, tr, ml, mr, bl, br)
            }
            pips.forEach { pt ->
                drawCircle(color = theme.accent, radius = r, center = pt)
            }
        }
    }
}

@Composable
private fun CoinFlipView() {
    val theme = LocalDyneTheme.current
    val view = LocalView.current
    var side by remember { mutableStateOf("HEADS") }
    var headsCount by remember { mutableIntStateOf(0) }
    var tailsCount by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFFFD54F).copy(alpha = 0.22f),
            modifier = Modifier
                .size(180.dp)
                .border(3.dp, Color(0xFFFFD54F), CircleShape)
                .clickable {
                    FeedbackHelper.hapticClick(view)
                    playToolTone(ToneGenerator.TONE_PROP_BEEP, 70)
                    val isHeads = Random.nextBoolean()
                    side = if (isHeads) "HEADS" else "TAILS"
                    if (isHeads) headsCount++ else tailsCount++
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = side,
                    color = Color(0xFFFFD54F),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Button(
            onClick = {
                FeedbackHelper.hapticClick(view)
                playToolTone(ToneGenerator.TONE_PROP_BEEP, 70)
                val isHeads = Random.nextBoolean()
                side = if (isHeads) "HEADS" else "TAILS"
                if (isHeads) headsCount++ else tailsCount++
            },
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("flip_coin_button")
        ) {
            Text("FLIP COIN", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }

        GlassResultCard(
            headlineLabel = "Session Tally",
            headlineValue = "Heads: $headsCount  •  Tails: $tailsCount",
            rows = listOf(
                "Total Flips" to "${headsCount + tailsCount}",
                "Heads Share" to if (headsCount + tailsCount > 0) "${(headsCount * 100) / (headsCount + tailsCount)}%" else "50%"
            )
        )
    }
}

@Composable
private fun PointerSpinnerView() {
    val theme = LocalDyneTheme.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val angleAnim = remember { Animatable(0f) }

    val compassPoints = listOf("North", "North-East", "East", "South-East", "South", "South-West", "West", "North-West")
    val normalizedAngle = ((angleAnim.value % 360f) + 360f) % 360f
    val sectorIndex = (((normalizedAngle + 22.5f) % 360f) / 45f).toInt().coerceIn(0, 7)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.cardBackground.copy(alpha = 0.85f),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Canvas(modifier = Modifier.size(210.dp)) {
                    val radius = size.minDimension / 2f
                    drawCircle(
                        color = theme.textSecondary.copy(alpha = 0.25f),
                        radius = radius,
                        style = Stroke(width = 6.dp.toPx())
                    )
                    for (i in 0 until 8) {
                        rotate(i * 45f) {
                            drawLine(
                                color = theme.textSecondary.copy(alpha = 0.35f),
                                start = Offset(center.x, center.y - radius),
                                end = Offset(center.x, center.y - radius + 16.dp.toPx()),
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                    }
                    rotate(normalizedAngle) {
                        val arrowPath = Path().apply {
                            moveTo(center.x, center.y - radius * 0.78f)
                            lineTo(center.x - 14.dp.toPx(), center.y + 18.dp.toPx())
                            lineTo(center.x + 14.dp.toPx(), center.y + 18.dp.toPx())
                            close()
                        }
                        drawPath(arrowPath, color = theme.accent)
                        drawCircle(color = Color.White, radius = 8.dp.toPx(), center = center)
                    }
                }

                Text(
                    text = "${compassPoints[sectorIndex]} (${normalizedAngle.roundToInt()}°)",
                    color = theme.accent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Button(
                    onClick = {
                        FeedbackHelper.hapticClick(view)
                        scope.launch {
                            val spinDelta = 720f + Random.nextFloat() * 1080f
                            angleAnim.animateTo(
                                targetValue = angleAnim.value + spinDelta,
                                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
                            )
                            FeedbackHelper.hapticSuccess(view)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("spin_pointer_button")
                ) {
                    Text("SPIN POINTER", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun DecisionWheelView() {
    val theme = LocalDyneTheme.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val wheelRotation = remember { Animatable(0f) }

    val options = remember {
        mutableStateListOf("Pizza", "Sushi", "Tacos", "Burgers", "Salad", "Thai")
    }
    var newOption by remember { mutableStateOf("") }
    var selectedWinner by remember { mutableStateOf(options.first()) }

    val sliceColors = listOf(
        Color(0xFF29B6F6),
        Color(0xFFAB47BC),
        Color(0xFF26A69A),
        Color(0xFFFFCA28),
        Color(0xFFEF5350),
        Color(0xFF66BB6A),
        Color(0xFFFF7043),
        Color(0xFF5C6BC0)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.cardBackground.copy(alpha = 0.85f),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(contentAlignment = Alignment.TopCenter) {
                    Canvas(modifier = Modifier.size(210.dp)) {
                        val sweep = 360f / options.size.coerceAtLeast(1)
                        rotate(wheelRotation.value) {
                            options.forEachIndexed { idx, _ ->
                                drawArc(
                                    color = sliceColors[idx % sliceColors.size],
                                    startAngle = idx * sweep,
                                    sweepAngle = sweep,
                                    useCenter = true
                                )
                            }
                        }
                        drawCircle(color = Color(0xFF101824), radius = 18.dp.toPx(), center = center)
                    }
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "Winner: $selectedWinner",
                    color = Color(0xFFFFD54F),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Button(
                    onClick = {
                        if (options.isNotEmpty()) {
                            FeedbackHelper.hapticClick(view)
                            scope.launch {
                                val delta = 1080f + Random.nextFloat() * 1080f
                                wheelRotation.animateTo(
                                    targetValue = wheelRotation.value + delta,
                                    animationSpec = tween(durationMillis = 1350, easing = FastOutSlowInEasing)
                                )
                                selectedWinner = options.random()
                                FeedbackHelper.hapticSuccess(view)
                                playToolTone(ToneGenerator.TONE_PROP_ACK, 160)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("spin_decision_wheel")
                ) {
                    Text("SPIN DECISION WHEEL", fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        GlassInputCard(title = "Customize Wheel Choices (${options.size})") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(options) { choice ->
                    InputChip(
                        selected = true,
                        onClick = { if (options.size > 2) options.remove(choice) },
                        label = { Text(choice) },
                        trailingIcon = {
                            if (options.size > 2) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newOption,
                    onValueChange = { newOption = it },
                    label = { Text("Add Option") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (newOption.isNotBlank()) {
                            options.add(newOption.trim())
                            newOption = ""
                        }
                    }
                ) {
                    Text("Add")
                }
            }
        }
    }
}
