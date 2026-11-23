package com.example.ui.settings

import android.Manifest
import android.app.NotificationManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billing.LocalFeatureGate
import com.example.billing.PremiumFeature
import com.example.service.DyneNotificationCenter
import com.example.service.DyneNotificationType
import com.example.service.NotificationDoctorReport
import com.example.service.NotificationGroupCategory
import com.example.service.NotificationHistoryEntry
import com.example.service.NotificationTimingStyle
import com.example.service.NotificationTypeConfig
import com.example.ui.components.HazeState
import com.example.ui.components.ProCrownChip
import com.example.ui.components.dyneClickable
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_PRESETS: List<Pair<Int, Int>> = listOf(
    6 to 0,
    7 to 0,
    8 to 0,
    9 to 0,
    12 to 0,
    17 to 30,
    19 to 0,
    20 to 30
)

private val QUIET_HOUR_PRESETS: List<String> = listOf(
    "21:00 - 07:00",
    "22:00 - 06:00",
    "22:00 - 07:00",
    "23:00 - 07:00",
    "00:00 - 08:00"
)

private val WEEKDAY_SHORT_LABELS: List<Pair<Int, String>> = listOf(
    1 to "M",
    2 to "T",
    3 to "W",
    4 to "T",
    5 to "F",
    6 to "S",
    7 to "S"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationCenterSubScreenContent(
    viewModel: SettingsViewModel,
    hazeState: HazeState?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val scope = rememberCoroutineScope()
    val featureGate = LocalFeatureGate.current

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val centerVersion by DyneNotificationCenter.stateVersion.collectAsState()

    val isPremiumAlertsUnlocked = isPro ||
        (featureGate?.hasAccess("outdoor_alerts", prefs = prefs) == true) ||
        prefs.isFeatureUnlockedTemporarily("outdoor_alerts")

    // Sync schedules when Premium status changes (pausing Premium types when Premium ends without deleting settings)
    LaunchedEffect(isPremiumAlertsUnlocked) {
        DyneNotificationCenter.syncAllSchedules(context, isPremiumActive = isPremiumAlertsUnlocked)
    }

    var showHistoryScreen by rememberSaveable { mutableStateOf(false) }
    var showLogsExpanded by rememberSaveable { mutableStateOf(false) }
    var statusFeedback by remember { mutableStateOf<String?>(null) }

    // Explanation card state before requesting POST_NOTIFICATIONS on Android 13+
    var showPermissionExplanationDialog by remember { mutableStateOf(false) }
    var pendingActionAfterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val doctorReport: NotificationDoctorReport = remember(centerVersion, refreshTrigger, isPremiumAlertsUnlocked) {
        DyneNotificationCenter.buildDoctorReport(context)
    }
    val typeConfigs: Map<DyneNotificationType, NotificationTypeConfig> = remember(centerVersion, refreshTrigger) {
        DyneNotificationCenter.getAllTypeConfigs(context)
    }
    val historyItems: List<NotificationHistoryEntry> = remember(centerVersion, refreshTrigger) {
        DyneNotificationCenter.getHistory(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        DyneNotificationCenter.markPermissionPromptedOnce(context)
        refreshTrigger++
        if (granted) {
            DyneNotificationCenter.syncAllSchedules(context, isPremiumActive = isPremiumAlertsUnlocked)
            pendingActionAfterPermission?.invoke()
            statusFeedback = "Notifications enabled and scheduled."
        } else {
            statusFeedback = "Notifications are off. Use 'Open settings' whenever you want to enable them."
        }
        pendingActionAfterPermission = null
    }

    /**
     * Requests POST_NOTIFICATIONS at the moment a notification feature is turned on or tested:
     * - Shows the explanation card first on Android 13+ if not yet prompted.
     * - Never loops permission requests if already prompted and denied.
     */
    fun runWithNotificationPermission(action: () -> Unit) {
        if (DyneNotificationCenter.hasNotificationPermission(context)) {
            action()
            refreshTrigger++
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !DyneNotificationCenter.hasRuntimePostNotificationsPermission(context)
        ) {
            if (!DyneNotificationCenter.wasPermissionPromptedOnce(context)) {
                pendingActionAfterPermission = action
                showPermissionExplanationDialog = true
            } else {
                statusFeedback = "Notifications are off. Tap 'Open settings' in Notification Doctor to enable."
            }
        } else {
            statusFeedback = "Notifications are off in system settings. Tap 'Open settings' to enable."
        }
    }

    if (showHistoryScreen) {
        BackHandler { showHistoryScreen = false }
        NotificationHistorySubScreen(
            history = historyItems,
            hazeState = hazeState,
            onClearHistory = {
                DyneNotificationCenter.clearHistory(context)
                refreshTrigger++
            },
            onBack = { showHistoryScreen = false }
        )
        return
    }

    if (showPermissionExplanationDialog) {
        Dialog(
            onDismissRequest = {
                showPermissionExplanationDialog = false
                pendingActionAfterPermission = null
            }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(theme.surface)
                    .border(1.dp, theme.accent.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
                    .testTag("notif_permission_explanation_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(theme.accent.copy(alpha = 0.18f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = theme.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Allow Local Notifications?",
                        style = type.title.copy(fontWeight = FontWeight.ExtraBold, fontSize = 17.sp),
                        color = theme.textPrimary
                    )
                }

                Text(
                    text = "Dyne schedules Daily Digests, outdoor windows, and timers 100% locally on your phone using Android WorkManager and AlarmClock. There is no server, no account, and never any ads or upsells inside notifications.",
                    style = type.body.copy(fontSize = 13.sp),
                    color = theme.textSecondary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            showPermissionExplanationDialog = false
                            pendingActionAfterPermission = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("notif_explanation_cancel_button")
                    ) {
                        Text("Not now", color = theme.textSecondary)
                    }
                    Button(
                        onClick = {
                            showPermissionExplanationDialog = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                pendingActionAfterPermission?.invoke()
                                pendingActionAfterPermission = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.accent,
                            contentColor = BentoTextBlack
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("notif_explanation_continue_button")
                    ) {
                        Text("Continue", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("notifications_center_list"),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "top_bar") {
            SubScreenTopBar(
                title = "Notifications",
                subtitle = "DOCTOR, DAILY DIGEST, TIMERS & 24 ALERTS",
                hazeState = hazeState,
                onBack = onBack
            )
        }

        // Immediate status banner when a test or sample is triggered
        if (!statusFeedback.isNullOrBlank()) {
            item(key = "status_feedback") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(theme.accent.copy(alpha = 0.14f))
                        .border(1.dp, theme.accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("notif_status_feedback_banner")
                ) {
                    Text(
                        text = statusFeedback ?: "",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.accent,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Dismiss",
                        style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                        color = theme.textSecondary,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable { statusFeedback = null }
                    )
                }
            }
        }

        // ========================================================================================
        // 1. NOTIFICATION DOCTOR (FOR EVERYONE)
        // ========================================================================================
        item(key = "notification_doctor") {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                NotificationDoctorCard(
                    report = doctorReport,
                    showLogsExpanded = showLogsExpanded,
                    onToggleLogs = { showLogsExpanded = !showLogsExpanded },
                    onOpenNotificationSettings = {
                        DyneNotificationCenter.openAppNotificationSettings(context)
                    },
                    onOpenBatterySettings = {
                        DyneNotificationCenter.openAppSystemDetailsSettings(context)
                    },
                    onSendTestNow = {
                        runWithNotificationPermission {
                            val (_, msg) = DyneNotificationCenter.sendTestNotificationNow(context)
                            statusFeedback = msg
                            refreshTrigger++
                        }
                    },
                    onSendTestIn1Min = {
                        runWithNotificationPermission {
                            val (_, msg) = DyneNotificationCenter.scheduleTestNotificationInOneMinute(context)
                            statusFeedback = msg
                            refreshTrigger++
                        }
                    }
                )
            }
        }

        // ========================================================================================
        // 2. FREE: DAILY DIGEST (1 PER DAY) & USER-SET TIMERS
        // ========================================================================================
        item(key = "free_daily_digest") {
            val digestCfg = typeConfigs[DyneNotificationType.DAILY_DIGEST]
                ?: DyneNotificationCenter.getTypeConfig(context, DyneNotificationType.DAILY_DIGEST)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "FREE • DAILY DIGEST & FOCUS TIMERS",
                    style = type.section,
                    color = theme.textSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(theme.surface)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Digest (1 per day)",
                                style = type.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                                color = theme.textPrimary
                            )
                            Text(
                                text = "Weather summary from cached data, Solunar score & best window, sunrise & sunset. Falls back to 'Open Dyne for today's outdoor summary' if cache is empty.",
                                style = type.caption,
                                color = theme.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = digestCfg.enabled,
                            onCheckedChange = { wantEnabled ->
                                if (wantEnabled) {
                                    runWithNotificationPermission {
                                        DyneNotificationCenter.updateTypeConfig(
                                            context = context,
                                            type = DyneNotificationType.DAILY_DIGEST,
                                            enabled = true,
                                            isPremiumActive = isPremiumAlertsUnlocked
                                        )
                                        statusFeedback = "Daily Digest scheduled for ${digestCfg.formattedTime}"
                                    }
                                } else {
                                    DyneNotificationCenter.updateTypeConfig(
                                        context = context,
                                        type = DyneNotificationType.DAILY_DIGEST,
                                        enabled = false,
                                        isPremiumActive = isPremiumAlertsUnlocked
                                    )
                                    statusFeedback = "Daily Digest turned off"
                                }
                                refreshTrigger++
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BentoTextBlack,
                                checkedTrackColor = theme.accent
                            ),
                            modifier = Modifier.testTag("notif_switch_daily_digest")
                        )
                    }

                    // Delivery time chips (default 08:00)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Delivery time",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.textSecondary
                        )
                        Text(
                            text = digestCfg.formattedTime,
                            style = type.caption.copy(fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace),
                            color = theme.accent
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        TIME_PRESETS.forEach { (h, m) ->
                            val label = String.format(Locale.US, "%02d:%02d", h, m)
                            val selected = digestCfg.hour == h && digestCfg.minute == m
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) theme.accent else theme.elevated)
                                    .clickable {
                                        DyneNotificationCenter.updateTypeConfig(
                                            context = context,
                                            type = DyneNotificationType.DAILY_DIGEST,
                                            hour = h,
                                            minute = m,
                                            isPremiumActive = isPremiumAlertsUnlocked
                                        )
                                        refreshTrigger++
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = type.caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                    color = if (selected) BentoTextBlack else theme.textPrimary
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Last result: ${DyneNotificationCenter.getLastResult(context, DyneNotificationType.DAILY_DIGEST.id)}",
                            style = type.caption.copy(fontSize = 11.sp),
                            color = theme.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(theme.elevated)
                                .border(1.dp, theme.accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                .dyneClickable(
                                    testTag = "notif_sample_daily_digest",
                                    onClick = {
                                        runWithNotificationPermission {
                                            scope.launch {
                                                val (_, msg) = DyneNotificationCenter.postSamplePreviewForType(
                                                    context,
                                                    DyneNotificationType.DAILY_DIGEST
                                                )
                                                statusFeedback = msg
                                                refreshTrigger++
                                            }
                                        }
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Preview a sample",
                                style = type.caption.copy(fontWeight = FontWeight.ExtraBold, fontSize = 11.sp),
                                color = theme.accent
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    // Focus & Time always-on note
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = theme.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "User-set timers, stopwatch, Pomodoro, parking timer and event countdowns always notify on the Focus & Time channel via AlarmClock (or WorkManager if restricted).",
                            style = type.caption.copy(fontSize = 11.sp),
                            color = theme.textSecondary
                        )
                    }
                }
            }
        }

        // ========================================================================================
        // 3. PREMIUM CONTROLS: QUIET HOURS, DAILY CAP (DEFAULT 6, MAX 12) & HISTORY (LAST 50)
        // ========================================================================================
        item(key = "premium_global_controls") {
            val quietEnabled = remember(centerVersion, refreshTrigger) {
                DyneNotificationCenter.isQuietHoursEnabled(context)
            }
            val quietRange = remember(centerVersion, refreshTrigger) {
                DyneNotificationCenter.getQuietHoursRange(context)
            }
            val dailyCap = remember(centerVersion, refreshTrigger) {
                DyneNotificationCenter.getDailyCap(context)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "PREMIUM NOTIFICATION CONTROLS",
                        style = type.section,
                        color = theme.textSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    if (!isPro) {
                        ProCrownChip()
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(theme.surface)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    if (!isPremiumAlertsUnlocked) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ProGold.copy(alpha = 0.12f))
                                .border(1.dp, ProGold.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "24 Smart Alert Types, Quiet Hours & History",
                                    style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                                    color = theme.textPrimary
                                )
                                Text(
                                    text = "Free users can tap 'Preview a sample' on any alert below. Unlock for 1 hour or Go Premium to schedule automatic background runs.",
                                    style = type.caption.copy(fontSize = 11.sp),
                                    color = theme.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ProGold)
                                    .dyneClickable(
                                        testTag = "notif_unlock_premium_alerts_button",
                                        onClick = {
                                            featureGate?.require(
                                                feature = PremiumFeature.OUTDOOR_ALERTS,
                                                featureKey = "outdoor_alerts"
                                            )
                                        }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "Unlock",
                                    style = type.caption.copy(fontWeight = FontWeight.ExtraBold, fontSize = 11.sp),
                                    color = BentoTextBlack
                                )
                            }
                        }
                    }

                    // Quiet Hours
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Quiet Hours",
                                style = type.body.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                color = theme.textPrimary
                            )
                            Text(
                                text = if (quietEnabled) "Pause scheduled alerts during $quietRange" else "Alerts allowed at any hour",
                                style = type.caption,
                                color = theme.textSecondary
                            )
                        }
                        Switch(
                            checked = quietEnabled,
                            onCheckedChange = { enabled ->
                                DyneNotificationCenter.setQuietHours(context, enabled = enabled)
                                refreshTrigger++
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BentoTextBlack,
                                checkedTrackColor = theme.accent
                            ),
                            modifier = Modifier.testTag("notif_quiet_hours_switch")
                        )
                    }

                    if (quietEnabled) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            QUIET_HOUR_PRESETS.forEach { windowStr ->
                                val selected = quietRange == windowStr
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) theme.accent else theme.elevated)
                                        .clickable {
                                            DyneNotificationCenter.setQuietHours(context, range = windowStr)
                                            refreshTrigger++
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = windowStr,
                                        style = type.caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                        color = if (selected) BentoTextBlack else theme.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    // Daily Cap (default 6, max 12)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Notification Cap",
                                style = type.body.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                color = theme.textPrimary
                            )
                            Text(
                                text = "${doctorReport.sentTodayCount} of $dailyCap sent today (default 6, max 12)",
                                style = type.caption,
                                color = theme.textSecondary
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(3, 6, 9, 12).forEach { capOption ->
                                val selected = dailyCap == capOption
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) theme.accent else theme.elevated)
                                        .clickable {
                                            DyneNotificationCenter.setDailyCap(context, capOption)
                                            refreshTrigger++
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("notif_cap_$capOption")
                                ) {
                                    Text(
                                        text = "$capOption",
                                        style = type.caption.copy(fontWeight = FontWeight.ExtraBold, fontSize = 11.sp),
                                        color = if (selected) BentoTextBlack else theme.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    // Notification History Row (last 50, device-only)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.elevated)
                            .dyneClickable(
                                testTag = "notif_open_history_button",
                                onClick = { showHistoryScreen = true }
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = theme.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Notification History (${historyItems.size}/50)",
                                    style = type.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                    color = theme.textPrimary
                                )
                                Text(
                                    text = "Device-only log with Snooze (15m) & Done status",
                                    style = type.caption.copy(fontSize = 11.sp),
                                    color = theme.textSecondary
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open History",
                            tint = theme.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // ========================================================================================
        // 4. ALL 24 PREMIUM NOTIFICATION TYPES BY CATEGORY
        // ========================================================================================
        val groupedPremium = DyneNotificationType.premiumTypes.groupBy { it.group }
        groupedPremium.forEach { (group, typesInGroup) ->
            item(key = "group_header_${group.name}") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = group.title.uppercase(Locale.US),
                        style = type.section,
                        color = theme.textSecondary
                    )
                    Text(
                        text = "${typesInGroup.count { typeConfigs[it]?.enabled == true }}/${typesInGroup.size} active",
                        style = type.caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = theme.accent
                    )
                }
            }

            items(
                items = typesInGroup,
                key = { it.id },
                contentType = { "premium_notif_type_card" }
            ) { notifType ->
                val cfg = typeConfigs[notifType] ?: DyneNotificationCenter.getTypeConfig(context, notifType)
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    PremiumNotificationTypeCard(
                        cfg = cfg,
                        isPremiumAlertsUnlocked = isPremiumAlertsUnlocked,
                        isPro = isPro,
                        lastResult = DyneNotificationCenter.getLastResult(context, notifType.id),
                        onToggleEnabled = { wantEnabled ->
                            if (!isPremiumAlertsUnlocked) {
                                // Save setting if turning off; if turning on while locked, prompt unlock sheet
                                if (!wantEnabled) {
                                    DyneNotificationCenter.updateTypeConfig(
                                        context = context,
                                        type = notifType,
                                        enabled = false,
                                        isPremiumActive = false
                                    )
                                    refreshTrigger++
                                } else {
                                    featureGate?.require(
                                        feature = PremiumFeature.OUTDOOR_ALERTS,
                                        featureKey = "outdoor_alerts",
                                        onUnlocked = {
                                            runWithNotificationPermission {
                                                DyneNotificationCenter.updateTypeConfig(
                                                    context = context,
                                                    type = notifType,
                                                    enabled = true,
                                                    isPremiumActive = true
                                                )
                                                statusFeedback = "${notifType.title} enabled"
                                                refreshTrigger++
                                            }
                                        }
                                    )
                                }
                            } else {
                                if (wantEnabled) {
                                    runWithNotificationPermission {
                                        DyneNotificationCenter.updateTypeConfig(
                                            context = context,
                                            type = notifType,
                                            enabled = true,
                                            isPremiumActive = true
                                        )
                                        statusFeedback = "${notifType.title} scheduled"
                                        refreshTrigger++
                                    }
                                } else {
                                    DyneNotificationCenter.updateTypeConfig(
                                        context = context,
                                        type = notifType,
                                        enabled = false,
                                        isPremiumActive = true
                                    )
                                    statusFeedback = "${notifType.title} turned off"
                                    refreshTrigger++
                                }
                            }
                        },
                        onUpdateTime = { h, m ->
                            DyneNotificationCenter.updateTypeConfig(
                                context = context,
                                type = notifType,
                                hour = h,
                                minute = m,
                                isPremiumActive = isPremiumAlertsUnlocked
                            )
                            refreshTrigger++
                        },
                        onUpdateLead = { leadVal ->
                            DyneNotificationCenter.updateTypeConfig(
                                context = context,
                                type = notifType,
                                leadValue = leadVal,
                                isPremiumActive = isPremiumAlertsUnlocked
                            )
                            refreshTrigger++
                        },
                        onUpdateSolunarMode = { mode ->
                            DyneNotificationCenter.updateTypeConfig(
                                context = context,
                                type = notifType,
                                solunarMode = mode,
                                isPremiumActive = isPremiumAlertsUnlocked
                            )
                            refreshTrigger++
                        },
                        onToggleWeekday = { dayInt ->
                            val nextDays = if (dayInt in cfg.weekdays && cfg.weekdays.size > 1) {
                                cfg.weekdays - dayInt
                            } else {
                                cfg.weekdays + dayInt
                            }
                            DyneNotificationCenter.updateTypeConfig(
                                context = context,
                                type = notifType,
                                weekdays = nextDays,
                                isPremiumActive = isPremiumAlertsUnlocked
                            )
                            refreshTrigger++
                        },
                        onPreviewSample = {
                            runWithNotificationPermission {
                                scope.launch {
                                    val (_, msg) = DyneNotificationCenter.postSamplePreviewForType(context, notifType)
                                    statusFeedback = msg
                                    refreshTrigger++
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationDoctorCard(
    report: NotificationDoctorReport,
    showLogsExpanded: Boolean,
    onToggleLogs: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onSendTestNow: () -> Unit,
    onSendTestIn1Min: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val isAllPermissionOk = report.permissionGranted && report.systemNotificationsEnabled

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(theme.surface)
            .border(
                width = 1.dp,
                color = if (isAllPermissionOk) theme.accent.copy(alpha = 0.35f) else Color(0xFFFB7185).copy(alpha = 0.55f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp)
            .testTag("notification_doctor_card")
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
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = theme.accent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "NOTIFICATION DOCTOR",
                    style = type.section.copy(fontWeight = FontWeight.ExtraBold),
                    color = theme.accent
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isAllPermissionOk) Color(0xFF34D399).copy(alpha = 0.16f)
                        else Color(0xFFFB7185).copy(alpha = 0.18f)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isAllPermissionOk) "HEALTHY" else "NOTIFICATIONS OFF",
                    style = type.caption.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.sp),
                    color = if (isAllPermissionOk) Color(0xFF34D399) else Color(0xFFFB7185)
                )
            }
        }

        // 1. Permission status (if off, show "Notifications are off" with "Open settings" button)
        if (!isAllPermissionOk) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFB7185).copy(alpha = 0.14f))
                    .border(1.dp, Color(0xFFFB7185).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("notif_doctor_off_banner")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = Color(0xFFFB7185),
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "Notifications are off",
                            style = type.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 13.sp),
                            color = theme.textPrimary
                        )
                        Text(
                            text = "POST_NOTIFICATIONS permission is off or muted in Android Settings.",
                            style = type.caption.copy(fontSize = 11.sp),
                            color = theme.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFB7185))
                        .dyneClickable(
                            testTag = "notif_doctor_open_settings",
                            onClick = onOpenNotificationSettings
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "Open settings",
                        style = type.caption.copy(fontWeight = FontWeight.ExtraBold, fontSize = 11.sp),
                        color = BentoTextBlack
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Permission: POST_NOTIFICATIONS Granted",
                    style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF34D399)
                )
                Text(
                    text = "System Settings",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.accent,
                    modifier = Modifier
                        .testTag("notif_doctor_open_settings")
                        .clickable(onClick = onOpenNotificationSettings)
                )
            }
        }

        // 2. Battery-optimization status with button to open app's system settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(theme.elevated)
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = if (report.isBatteryOptimizationIgnored) Color(0xFF34D399) else Color(0xFFFBBF24),
                    modifier = Modifier.size(16.dp)
                )
                Column {
                    Text(
                        text = if (report.isBatteryOptimizationIgnored) {
                            "Battery Optimization: Unrestricted"
                        } else {
                            "Battery Optimization: Optimized (Standard)"
                        },
                        style = type.caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = theme.textPrimary
                    )
                    Text(
                        text = "If background alerts are delayed by OEM battery savers, set Dyne to Unrestricted.",
                        style = type.caption.copy(fontSize = 10.sp),
                        color = theme.textSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.surface)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .dyneClickable(
                        testTag = "notif_doctor_battery_button",
                        onClick = onOpenBatterySettings
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "App Settings",
                    style = type.caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    color = theme.accent
                )
            }
        }

        // 3. Channel States (all 6 channels)
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(theme.elevated.copy(alpha = 0.7f))
                .padding(10.dp)
        ) {
            Text(
                text = "NOTIFICATION CHANNELS (${report.channels.size})",
                style = type.caption.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.sp),
                color = theme.textSecondary
            )
            report.channels.forEach { chState ->
                val impLabel = when (chState.systemImportance) {
                    NotificationManager.IMPORTANCE_HIGH, NotificationManager.IMPORTANCE_MAX -> "HIGH"
                    NotificationManager.IMPORTANCE_DEFAULT -> "DEFAULT"
                    NotificationManager.IMPORTANCE_LOW -> "LOW"
                    NotificationManager.IMPORTANCE_NONE -> "MUTED"
                    else -> "ACTIVE"
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = chState.channel.title,
                        style = type.caption.copy(fontSize = 11.sp),
                        color = theme.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (chState.isBlockedByUser) "BLOCKED" else "OK ($impLabel)",
                        style = type.caption.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (chState.isBlockedByUser) Color(0xFFFB7185) else Color(0xFF34D399)
                    )
                }
            }
        }

        // 4. Scheduled Work List with next run time and last result
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(theme.elevated.copy(alpha = 0.7f))
                .padding(10.dp)
        ) {
            Text(
                text = "SCHEDULED WORK (${report.scheduledWorks.size})",
                style = type.caption.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.sp),
                color = theme.textSecondary
            )
            if (report.scheduledWorks.isEmpty()) {
                Text(
                    text = "No background work scheduled yet. Turn on Daily Digest, any alert type, or tap 'Send test in 1 minute'.",
                    style = type.caption.copy(fontSize = 11.sp),
                    color = theme.textSecondary
                )
            } else {
                val fmt = remember { DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.US) }
                report.scheduledWorks.take(8).forEach { w ->
                    val nextStr = if (w.nextRunEpochMs > 0L) {
                        runCatching {
                            Instant.ofEpochMilli(w.nextRunEpochMs)
                                .atZone(ZoneId.systemDefault())
                                .format(fmt)
                        }.getOrDefault("--:--")
                    } else {
                        "—"
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.surface.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = w.typeTitle,
                                style = type.caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = theme.textPrimary
                            )
                            Text(
                                text = "${w.state} • Next: $nextStr",
                                style = type.caption.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = theme.accent
                            )
                        }
                        Text(
                            text = "Last: ${w.lastResult}",
                            style = type.caption.copy(fontSize = 10.sp),
                            color = theme.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 5. "Send test now" and "Send test in 1 minute" buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onSendTestNow,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.accent,
                    contentColor = BentoTextBlack
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("notif_doctor_send_test_now")
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send test now", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            }

            OutlinedButton(
                onClick = onSendTestIn1Min,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("notif_doctor_send_test_1m")
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = theme.accent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send test in 1 minute", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary, maxLines = 1)
            }
        }

        // 6. Collapsible Run & Skip Log
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleLogs)
                .padding(vertical = 2.dp)
                .testTag("notif_doctor_toggle_logs")
        ) {
            Text(
                text = "Run & Skip Diagnostic Log (${report.recentLogs.size})",
                style = type.caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = theme.textSecondary
            )
            Icon(
                imageVector = if (showLogsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Toggle logs",
                tint = theme.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        if (showLogsExpanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.28f))
                    .padding(10.dp)
            ) {
                if (report.recentLogs.isEmpty()) {
                    Text(
                        text = "No log entries yet.",
                        style = type.caption.copy(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                        color = theme.textSecondary
                    )
                } else {
                    report.recentLogs.take(15).forEach { logLine ->
                        Text(
                            text = logLine,
                            style = type.caption.copy(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                            color = theme.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PremiumNotificationTypeCard(
    cfg: NotificationTypeConfig,
    isPremiumAlertsUnlocked: Boolean,
    isPro: Boolean,
    lastResult: String,
    onToggleEnabled: (Boolean) -> Unit,
    onUpdateTime: (Int, Int) -> Unit,
    onUpdateLead: (Int) -> Unit,
    onUpdateSolunarMode: (String) -> Unit,
    onToggleWeekday: (Int) -> Unit,
    onPreviewSample: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val notifType = cfg.type

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(theme.surface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(15.dp)
            .testTag("notif_card_${notifType.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = notifType.title,
                        style = type.body.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = theme.textPrimary
                    )
                    if (!isPro) {
                        ProCrownChip()
                    }
                }
                Text(
                    text = notifType.subtitle,
                    style = type.caption.copy(fontSize = 11.sp),
                    color = theme.textSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = cfg.enabled && isPremiumAlertsUnlocked,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BentoTextBlack,
                    checkedTrackColor = theme.accent
                ),
                modifier = Modifier.testTag("notif_switch_${notifType.id}")
            )
        }

        // Solunar mode selector (Fishing, Hunting, Both)
        if (notifType.supportsSolunarMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Mode:",
                    style = type.caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = theme.textSecondary
                )
                listOf("FISHING" to "Fishing", "HUNTING" to "Hunting", "BOTH" to "Both").forEach { (modeKey, modeLabel) ->
                    val selected = cfg.solunarMode.equals(modeKey, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) theme.accent else theme.elevated)
                            .clickable { onUpdateSolunarMode(modeKey) }
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = modeLabel,
                            style = type.caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = if (selected) BentoTextBlack else theme.textPrimary
                        )
                    }
                }
            }
        }

        // Lead time or time-of-day chips
        when (notifType.timingStyle) {
            NotificationTimingStyle.LEAD_MINUTES_15_30_60 -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Lead time:",
                        style = type.caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = theme.textSecondary
                    )
                    listOf(15, 30, 60).forEach { mins ->
                        val selected = cfg.leadValue == mins
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) theme.accent else theme.elevated)
                                .clickable { onUpdateLead(mins) }
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$mins min",
                                style = type.caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = if (selected) BentoTextBlack else theme.textPrimary
                            )
                        }
                    }
                }
            }

            NotificationTimingStyle.LEAD_DAYS_30_7_1 -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Advance notice:",
                        style = type.caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = theme.textSecondary
                    )
                    listOf(30, 7, 1).forEach { days ->
                        val selected = cfg.leadValue == days
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) theme.accent else theme.elevated)
                                .clickable { onUpdateLead(days) }
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$days day${if (days == 1) "" else "s"}",
                                style = type.caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = if (selected) BentoTextBlack else theme.textPrimary
                            )
                        }
                    }
                }
            }

            NotificationTimingStyle.TIME_OF_DAY -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Time:",
                        style = type.caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = theme.textSecondary
                    )
                    TIME_PRESETS.forEach { (h, m) ->
                        val label = String.format(Locale.US, "%02d:%02d", h, m)
                        val selected = cfg.hour == h && cfg.minute == m
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) theme.accent else theme.elevated)
                                .clickable { onUpdateTime(h, m) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                style = type.caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = if (selected) BentoTextBlack else theme.textPrimary
                            )
                        }
                    }
                }
            }
        }

        // Weekday chips (M T W T F S S)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                WEEKDAY_SHORT_LABELS.forEach { (dayInt, dayLabel) ->
                    val selected = dayInt in cfg.weekdays
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (selected) theme.accent.copy(alpha = 0.22f) else theme.elevated)
                            .border(
                                1.dp,
                                if (selected) theme.accent else Color.White.copy(alpha = 0.08f),
                                CircleShape
                            )
                            .clickable { onToggleWeekday(dayInt) }
                    ) {
                        Text(
                            text = dayLabel,
                            style = type.caption.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.sp),
                            color = if (selected) theme.accent else theme.textSecondary
                        )
                    }
                }
            }

            // "Preview a sample" button (posts ONE sample notification for Free or Premium users)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.elevated)
                    .border(1.dp, theme.accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .dyneClickable(
                        testTag = "notif_sample_${notifType.id}",
                        onClick = onPreviewSample
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Preview a sample",
                    style = type.caption.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.sp),
                    color = theme.accent
                )
            }
        }

        if (cfg.enabled && !isPremiumAlertsUnlocked) {
            Text(
                text = "Paused while Premium is inactive (your settings are saved).",
                style = type.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                color = ProGold
            )
        } else if (lastResult != "Not run yet") {
            Text(
                text = "Last: $lastResult",
                style = type.caption.copy(fontSize = 10.sp),
                color = theme.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NotificationHistorySubScreen(
    history: List<NotificationHistoryEntry>,
    hazeState: HazeState?,
    onClearHistory: () -> Unit,
    onBack: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val fmt = remember { DateTimeFormatter.ofPattern("MMM d, HH:mm:ss", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("notification_history_screen")
    ) {
        SubScreenTopBar(
            title = "Notification History",
            subtitle = "LAST 50 ON-DEVICE ALERTS (DEVICE ONLY)",
            hazeState = hazeState,
            onBack = onBack
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "${history.size} of 50 stored locally",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.textSecondary
            )
            if (history.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(theme.elevated)
                        .clickable(onClick = onClearHistory)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("notif_history_clear_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = theme.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Clear",
                        style = type.caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = theme.textSecondary
                    )
                }
            }
        }

        if (history.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = theme.textSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "No notifications recorded yet",
                        style = type.body.copy(fontWeight = FontWeight.Bold),
                        color = theme.textPrimary
                    )
                    Text(
                        text = "Notifications sent by Daily Digest, timers, Doctor tests, or sample previews appear here (kept 100% on this device).",
                        style = type.caption,
                        color = theme.textSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history, key = { it.id }) { item ->
                    val timeLabel = runCatching {
                        Instant.ofEpochMilli(item.timestampEpochMs)
                            .atZone(ZoneId.systemDefault())
                            .format(fmt)
                    }.getOrDefault("")

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.surface)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.title,
                                style = type.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                color = theme.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = item.status,
                                style = type.caption.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.sp),
                                color = when {
                                    item.status.startsWith("DONE") -> Color(0xFF34D399)
                                    item.status.startsWith("SNOOZE") -> ProGold
                                    else -> theme.accent
                                }
                            )
                        }
                        Text(
                            text = item.body,
                            style = type.caption.copy(fontSize = 11.sp),
                            color = theme.textSecondary
                        )
                        Text(
                            text = "$timeLabel • ${if (item.isSampleOrTest) "Sample / Test" else "Scheduled"} • Deep link: ${item.deepLinkTarget}",
                            style = type.caption.copy(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                            color = theme.textSecondary.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}
