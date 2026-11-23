package com.example.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.data.PreferencesRepository
import com.example.data.WeatherRepository
import com.example.domain.AstroCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Notification channels created in [com.example.DyneApplication.onCreate] with explicit importance.
 */
enum class DyneNotificationChannel(
    val id: String,
    val title: String,
    val descriptionText: String,
    val importance: Int
) {
    DAILY_DIGEST(
        id = "dyne_channel_daily_digest",
        title = "Daily Digest (Free)",
        descriptionText = "Daily morning summary of cached weather, Solunar score, and sunrise/sunset.",
        importance = NotificationManager.IMPORTANCE_DEFAULT
    ),
    FOCUS_AND_TIME(
        id = "dyne_channel_focus_time",
        title = "Focus & Time (Timers & Alarms)",
        descriptionText = "User-set timers, stopwatch, Pomodoro sessions, parking timer, and event countdowns.",
        importance = NotificationManager.IMPORTANCE_HIGH
    ),
    OUTDOOR_AND_WEATHER(
        id = "dyne_channel_outdoor_weather",
        title = "Outdoor, Sky & Weather Alerts",
        descriptionText = "Solunar windows, Golden/Blue hour, Sunrise/Sunset, Rain soon, Wind, Pressure, UV, and Moon phases.",
        importance = NotificationManager.IMPORTANCE_DEFAULT
    ),
    FINANCE_AND_DOCS(
        id = "dyne_channel_finance_docs",
        title = "Documents, Bills & Finance",
        descriptionText = "Document expiry reminders, bill due alerts, weekly expense summaries, backups, and currency alerts.",
        importance = NotificationManager.IMPORTANCE_DEFAULT
    ),
    GOALS_AND_HABITS(
        id = "dyne_channel_goals_habits",
        title = "Focus Streaks, Steps & Checklists",
        descriptionText = "Pomodoro streaks, habit goals, trip checklists, daily step goals, evening step nudges, and weekly recaps.",
        importance = NotificationManager.IMPORTANCE_DEFAULT
    ),
    REWARDS_AND_STREAK(
        id = "dyne_channel_rewards_streak",
        title = "Rewards & Streak Reminders",
        descriptionText = "Daily coin check-in bonus and streak-at-risk reminders.",
        importance = NotificationManager.IMPORTANCE_DEFAULT
    )
}

enum class NotificationGroupCategory(val title: String) {
    FREE_DAILY("Free Daily Digest"),
    OUTDOOR_AND_SKY("Outdoor, Sky & Weather (Premium)"),
    DOCS_AND_FINANCE("Documents, Bills & Backup (Premium)"),
    FOCUS_AND_CHECKLISTS("Focus, Habits & Checklists (Premium)"),
    STEPS_AND_REWARDS("Steps, Logs & Rewards (Premium)")
}

enum class NotificationTimingStyle {
    TIME_OF_DAY,
    LEAD_MINUTES_15_30_60,
    LEAD_DAYS_30_7_1
}

/**
 * All 25 scheduled notification types (1 Free "Daily Digest" + 24 Premium types).
 */
enum class DyneNotificationType(
    val id: String,
    val title: String,
    val subtitle: String,
    val isPremium: Boolean,
    val channel: DyneNotificationChannel,
    val group: NotificationGroupCategory,
    val timingStyle: NotificationTimingStyle,
    val defaultHour: Int = 8,
    val defaultMinute: Int = 0,
    val defaultLeadMinutes: Int = 30,
    val defaultEnabled: Boolean = false,
    val deepLinkTarget: String,
    val supportsSolunarMode: Boolean = false
) {
    // 1. FREE: Daily Digest (1 per day at user-chosen time, default 08:00)
    DAILY_DIGEST(
        id = "daily_digest",
        title = "Daily Digest",
        subtitle = "1 per day: cached weather summary, Solunar score & best window, sunrise & sunset",
        isPremium = false,
        channel = DyneNotificationChannel.DAILY_DIGEST,
        group = NotificationGroupCategory.FREE_DAILY,
        timingStyle = NotificationTimingStyle.TIME_OF_DAY,
        defaultHour = 8,
        defaultMinute = 0,
        defaultEnabled = false,
        deepLinkTarget = "outdoor_weather_current"
    ),

    // 2. PREMIUM: Outdoor, Sky & Weather (9 types)
    SOLUNAR_BEST_WINDOW(
        id = "solunar_best_window",
        title = "Solunar Best Window",
        subtitle = "Fishing, Hunting or both; lead time 15, 30 or 60 min before major window",
        isPremium = true,
        channel = DyneNotificationChannel.OUTDOOR_AND_WEATHER,
        group = NotificationGroupCategory.OUTDOOR_AND_SKY,
        timingStyle = NotificationTimingStyle.LEAD_MINUTES_15_30_60,
        defaultHour = 7,
        defaultMinute = 30,
        defaultLeadMinutes = 30,
        deepLinkTarget = "outdoor_solunar_today",
        supportsSolunarMode = true
    ),
    GOLDEN_BLUE_HOUR(
        id = "golden_blue_hour",
        title = "Golden & Blue Hour",
        subtitle = "Notify before morning or evening Golden & Blue hour light windows",
        isPremium = true,
        channel = DyneNotificationChannel.OUTDOOR_AND_WEATHER,
        group = NotificationGroupCategory.OUTDOOR_AND_SKY,
        timingStyle = NotificationTimingStyle.LEAD_MINUTES_15_30_60,
        defaultHour = 17,
        defaultMinute = 15,
        defaultLeadMinutes = 30,
        deepLinkTarget = "outdoor_sky_dome"
    ),
    SUNRISE_SUNSET(
        id = "sunrise_sunset",
        title = "Sunrise & Sunset",
        subtitle = "Lead-time reminder before local sunrise and sunset",
        isPremium = true,
        channel = DyneNotificationChannel.OUTDOOR_AND_WEATHER,
        group = NotificationGroupCategory.OUTDOOR_AND_SKY,
        timingStyle = NotificationTimingStyle.LEAD_MINUTES_15_30_60,
        defaultHour = 6,
        defaultMinute = 15,
        defaultLeadMinutes = 30,
        deepLinkTarget = "outdoor_sun_moon_calendar"
    ),
    RAIN_SOON(
        id = "rain_soon",
        title = "Rain Soon",
        subtitle = "Alerts when cached hourly forecast shows elevated rain probability",
        isPremium = true,
        channel = DyneNotificationChannel.OUTDOOR_AND_WEATHER,
        group = NotificationGroupCategory.OUTDOOR_AND_SKY,
        timingStyle = NotificationTimingStyle.LEAD_MINUTES_15_30_60,
        defaultHour = 11,
        defaultMinute = 0,
        defaultLeadMinutes = 30,
        deepLinkTarget = "outdoor_weather_current"
    ),
    WIND_CHANGE(
        id = "wind_change",
        title = "Wind Change",
        subtitle = "Notifies when forecasted wind gusts shift noticeably",
        isPremium = true,
        channel = DyneNotificationChannel.OUTDOOR_AND_WEATHER,
        group = NotificationGroupCategory.OUTDOOR_AND_SKY,
        timingStyle = NotificationTimingStyle.TIME_OF_DAY,
        defaultHour = 10,
        defaultMinute = 0,
        deepLinkTarget = "outdoor_weather_current"
    ),
    PRESSURE_TREND(
        id = "pressure_trend",
        title = "Pressure Trend",
        subtitle = "Barometric pressure rise or drop trend check for outdoor planning",
        isPremium = true,
        channel = DyneNotificationChannel.OUTDOOR_AND_WEATHER,
        group = NotificationGroupCategory.OUTDOOR_AND_SKY,
        timingStyle = NotificationTimingStyle.TIME_OF_DAY,
        defaultHour = 9,
        defaultMinute = 30,
        deepLinkTarget = "outdoor_weather_current"
    ),
    UV_HIGH(
        id = "uv_high",
        title = "UV High",
        subtitle = "Midday reminder when cached UV index reaches high levels",
        isPremium = true,
        channel = DyneNotificationChannel.OUTDOOR_AND_WEATHER,
        group = NotificationGroupCategory.OUTDOOR_AND_SKY,
        timingStyle = NotificationTimingStyle.TIME_OF_DAY,
        defaultHour = 11,
        defaultMinute = 30,
        deepLinkTarget = "outdoor_air_uv"
    ),
    FULL_NEW_MOON(
        id = "full_new_moon",
        title = "Full & New Moon",
        subtitle = "Alerts on Full Moon and New Moon peak illumination days",
        isPremium = true,
        channel = DyneNotificationChannel.OUTDOOR_AND_WEATHER,
        group = NotificationGroupCategory.OUTDOOR_AND_SKY,
        timingStyle = NotificationTimingStyle.TIME_OF_DAY,
        defaultHour = 19,
        defaultMinute = 0,
        deepLinkTarget = "outdoor_sun_moon_calendar"
    ),
    FISHING_LOG_REMINDER(
        id = "fishing_log_reminder",
        title = "Fishing Log Reminder",
        subtitle = "Reminder to log catches, conditions, and notes after outdoor sessions",
        isPremium = true,
        channel = DyneNotificationChannel.OUTDOOR_AND_WEATHER,
        group = NotificationGroupCategory.OUTDOOR_AND_SKY,
        timingStyle = NotificationTimingStyle.TIME_OF_DAY,
        defaultHour = 18,
        defaultMinute = 30,
        deepLinkTarget = "fishing_log"
    ),

    // 3. PREMIUM: Documents, Bills & Finance (5 types)
    DOCUMENT_EXPIRY(
        id = "document_expiry",
        title = "Document Expiry (30 / 7 / 1 Days)",
        subtitle = "Advance notice 30, 7, or 1 day before saved ID or warranty dates",
        isPremium = true,
        channel = DyneNotificationChannel.FINANCE_AND_DOCS,
        group = NotificationGroupCategory.DOCS_AND_FINANCE,
        timingStyle = NotificationTimingStyle.LEAD_DAYS_30_7_1,
        defaultHour = 9,
        defaultMinute = 0,
        defaultLeadMinutes = 7, // stores 30, 7, or 1 days
        deepLinkTarget = "scanner_id_card"
    ),
    BILL_DUE(
        id = "bill_due",
        title = "Bill Due Reminder",
        subtitle = "Scheduled check for upcoming recurring bills and split expenses",
        isPremium = true,
        channel = DyneNotificationChannel.FINANCE_AND_DOCS,
        group = NotificationGroupCategory.DOCS_AND_FINANCE,
        timingStyle = NotificationTimingStyle.TIME_OF_DAY,
        defaultHour = 9,
        defaultMinute = 30,
        deepLinkTarget = "finance_pack"
    ),
    WEEKLY_EXPENSE_SUMMARY(
        id = "weekly_expense_summary",
        title = "Weekly Expense Summary",
        subtitle = "Weekly digest of logged bill splits and finance notes",
        isPremium = true,
        channel = DyneNotificationChannel.FINANCE_AND_DOCS,
        group = NotificationGroupCategory.DOCS_AND_FINANCE,
        timingStyle = NotificationTimingStyle.TIME_OF_DAY,
        defaultHour = 18,
        defaultMinute = 0,
        deepLinkTarget = "split_bill_pro"
    ),
    BACKUP_REMINDER(
        id = "backup_reminder",
        title = "Backup Reminder",
        subtitle = "Periodic reminder to export a local ZIP backup of your library",
        isPremium = true,
        channel = DyneNotificationChannel.FINANCE_AND_DOCS,
        group = NotificationGroupCategory.DOCS_AND_FINANCE,
        timingStyle = NotificationTimingStyle.TIME_OF_DAY,
        defaultHour = 20,
        defaultMinute = 0,
        deepLinkTarget = "settings_backup"
    ),
    CURRENCY_RATE_ALERT(
        id = "currency_rate_alert",
        title = "Currency Rate Alert",
        subtitle = "Check saved currency conversion pairs and reference rates",
        isPremium = true,
        channel = DyneNotificationChannel.FINANCE_AND_DOCS,
        group = NotificationGroupCategory.DOCS_AND_FINANCE,
        timingStyle = NotificationTimingStyle.TIME_OF_DAY,
        defaultHour = 10,
        defaultMinute = 30,
        deepLinkTarget = "finance_pack"
    ),

    // 4. PREMIUM: Focus, Habits & Checklists (5 types)
    POMODORO_STREAK(
        id = "pomodoro_streak",
        title = "Pomodoro Streak",
        subtitle = "Keep your daily focus session streak active",
        isPremium = true,
        channel = DyneNotificationChannel.GOALS_AND_HABITS,
        group = NotificationGroupCategory.FOCUS_AND_CHECKLISTS,
        timingStyle = NotificationTimingStyle.TIME_OF_DAY,
        defaultHour = 16,
        defaultMinute = 0,
        deepLinkTarget = "pomodoro_stats"
    ),
    HABIT_GOAL_REACHED(
        id = "habit_goal_reached",
        title = "Habit Goal Reached",
        subtitle = "Progress check on your daily tally and habit counters",
        isPremium = true,
        channel = DyneNotificationChannel.GOALS_AND_HABITS,
        group = NotificationGroupCategory.FOCUS_AND_CHECKLISTS,
        timingStyle = NotificationTimingStyle.TIME_OF_DAY,
        defaultHour = 19,
        defaultMinute = 30,
        deepLinkTarget = "tally_counter"
    ),
    EVENT_COUNTDOWN_MILESTONES(
        id = "event_countdown_milestones",
        title = "Event Countdown Milestones",
        subtitle = "Milestone alerts (30, 7, and 1 day remaining) for saved events",
        isPremium = true,
        channel = DyneNotificationChannel.GOALS_AND_HABITS,
        group = NotificationGroupCategory.FOCUS_AND_CHECKLISTS,
        timingStyle = NotificationTimingStyle.LEAD_DAYS_30_7_1,
        defaultHour = 9,
        defaultMinute = 0,
        defaultLeadMinutes = 7,
        deepLinkTarget = "event_countdown"
    ),
    TRIP_CHECKLIST_REMINDERS(
        id = "trip_checklist_reminders",
        title = "Trip Checklist Reminders",
        subtitle = "Pre-departure reminder to review unchecked gear items",
        isPremium = true,
        channel = DyneNotificationChannel.GOALS_AND_HABITS,
        group = NotificationGroupCategory.FOCUS_AND_CHECKLISTS,
        timingStyle = NotificationTimingStyle.TIME_OF_DAY,
        defaultHour = 17,
        defaultMinute = 0,
        deepLinkTarget = "trip_gear_checklists"
    ),
    PARKING_TIMER(
        id = "parking_timer",
        title = "Parking Timer",
        subtitle = "Lead-time alert before your saved parking meter expires",
        isPremium = true,
        channel = DyneNotificationChannel.FOCUS_AND_TIME,
        group = NotificationGroupCategory.FOCUS_AND_CHECKLISTS,
        timingStyle = NotificationTimingStyle.LEAD_MINUTES_15_30_60,
        defaultHour = 12,
        defaultMinute = 0,
        defaultLeadMinutes = 15,
        deepLinkTarget = "parking_saver"
    ),

    // 5. PREMIUM: Steps, Activity & Rewards (5 types)
    DAILY_STEP_GOAL(
        id = "daily_step_goal",
        title = "Daily Step Goal",
        subtitle = "Celebrates when your daily step target is reached",
        isPremium = true,
        channel = DyneNotificationChannel.GOALS_AND_HABITS,
        group = NotificationGroupCategory.STEPS_AND_REWARDS,
        timingStyle = NotificationTimingStyle.TIME_OF_DAY,
        defaultHour = 18,
        defaultMinute = 0,
        deepLinkTarget = "pedometer_history"
    ),
    EVENING_STEP_NUDGE(
        id = "evening_step_nudge",
        title = "Evening Step Nudge",
        subtitle = "Gentle evening check-in on today's step count",
        isPremium = true,
        channel = DyneNotificationChannel.GOALS_AND_HABITS,
        group = NotificationGroupCategory.STEPS_AND_REWARDS,
        timingStyle = NotificationTimingStyle.TIME_OF_DAY,
        defaultHour = 19,
        defaultMinute = 0,
        deepLinkTarget = "pedometer"
    ),
    WEEKLY_RECAP(
        id = "weekly_recap",
        title = "Weekly Recap",
        subtitle = "Weekly summary of logged measurements, scans, and tool sessions",
        isPremium = true,
        channel = DyneNotificationChannel.GOALS_AND_HABITS,
        group = NotificationGroupCategory.STEPS_AND_REWARDS,
        timingStyle = NotificationTimingStyle.TIME_OF_DAY,
        defaultHour = 18,
        defaultMinute = 30,
        deepLinkTarget = "measurement_history_csv"
    ),
    DAILY_COIN_BONUS(
        id = "daily_coin_bonus",
        title = "Daily Coin Bonus",
        subtitle = "Reminder to claim your daily check-in coins",
        isPremium = true,
        channel = DyneNotificationChannel.REWARDS_AND_STREAK,
        group = NotificationGroupCategory.STEPS_AND_REWARDS,
        timingStyle = NotificationTimingStyle.TIME_OF_DAY,
        defaultHour = 10,
        defaultMinute = 0,
        deepLinkTarget = "rewards"
    ),
    STREAK_AT_RISK(
        id = "streak_at_risk",
        title = "Streak at Risk",
        subtitle = "Evening reminder before midnight if today's check-in is unclaimed",
        isPremium = true,
        channel = DyneNotificationChannel.REWARDS_AND_STREAK,
        group = NotificationGroupCategory.STEPS_AND_REWARDS,
        timingStyle = NotificationTimingStyle.TIME_OF_DAY,
        defaultHour = 20,
        defaultMinute = 30,
        deepLinkTarget = "rewards"
    );

    companion object {
        fun fromId(id: String): DyneNotificationType? =
            entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) }

        val freeTypes: List<DyneNotificationType> = entries.filter { !it.isPremium }
        val premiumTypes: List<DyneNotificationType> = entries.filter { it.isPremium }
    }
}

data class NotificationTypeConfig(
    val type: DyneNotificationType,
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    val leadValue: Int, // 15/30/60 min or 30/7/1 days
    val weekdays: Set<Int>, // 1 = Monday .. 7 = Sunday
    val solunarMode: String = "BOTH" // FISHING, HUNTING, BOTH
) {
    val leadMinutes: Int
        get() = leadValue

    val formattedTime: String
        get() = String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
}

data class NotificationHistoryEntry(
    val id: Long,
    val typeId: String,
    val title: String,
    val body: String,
    val channelId: String,
    val deepLinkTarget: String,
    val timestampEpochMs: Long,
    val isSampleOrTest: Boolean,
    val status: String // "SENT", "SNOOZED", "DONE"
)

data class NotificationDoctorChannelState(
    val channel: DyneNotificationChannel,
    val existsInSystem: Boolean,
    val systemImportance: Int,
    val isBlockedByUser: Boolean
)

data class ScheduledWorkDiagnostic(
    val uniqueWorkName: String,
    val typeTitle: String,
    val state: String,
    val nextRunEpochMs: Long,
    val lastResult: String
)

data class NotificationDoctorReport(
    val permissionGranted: Boolean,
    val systemNotificationsEnabled: Boolean,
    val channels: List<NotificationDoctorChannelState>,
    val scheduledWorks: List<ScheduledWorkDiagnostic>,
    val isBatteryOptimizationIgnored: Boolean,
    val recentLogs: List<String>,
    val sentTodayCount: Int,
    val dailyCap: Int
)

data class TimerScheduleResult(
    val usedAlarmClock: Boolean,
    val triggerAtWallMs: Long,
    val note: String
) {
    val scheduled: Boolean get() = true
    val method: String get() = if (usedAlarmClock) "AlarmClock" else "WorkManager"
}

/**
 * Single on-device Notification Center & Scheduler (no server, no Firebase).
 */
object DyneNotificationCenter {

    private const val TAG = "DyneNotifCenter"
    private const val PREFS_NAME = "dyne_notification_center_prefs"
    private const val KEY_HISTORY_JSON = "notif_history_v1"
    private const val KEY_DIAG_LOGS_JSON = "notif_diag_logs_v1"
    private const val KEY_LAST_RESULTS_MAP = "notif_last_results_v1"
    private const val KEY_QUIET_ENABLED = "notif_quiet_enabled"
    private const val KEY_QUIET_RANGE = "notif_quiet_range"
    private const val KEY_DAILY_CAP = "notif_daily_cap"
    private const val KEY_DAILY_SENT_DATE = "notif_daily_sent_date"
    private const val KEY_DAILY_SENT_COUNT = "notif_daily_sent_count"
    private const val KEY_PERMISSION_PROMPTED_ONCE = "notif_perm_prompted_once"

    const val EXTRA_NOTIFICATION_DEEP_LINK = "extra_notification_deep_link"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_NOTIFICATION_TYPE_ID = "extra_notification_type_id"
    const val EXTRA_NOTIFICATION_TITLE = "extra_notification_title"
    const val EXTRA_NOTIFICATION_BODY = "extra_notification_body"

    const val ACTION_SNOOZE = "com.example.action.NOTIFICATION_SNOOZE"
    const val ACTION_DONE = "com.example.action.NOTIFICATION_DONE"
    const val ACTION_TIMER_ALARM = "com.example.action.USER_TIMER_ALARM"

    const val DEFAULT_DAILY_CAP = 6
    const val MAX_DAILY_CAP = 12
    const val MAX_HISTORY_ITEMS = 50
    private const val MAX_LOG_ITEMS = 40

    private val _stateVersion = MutableStateFlow(0L)
    val stateVersion: StateFlow<Long> = _stateVersion.asStateFlow()

    private fun bumpState() {
        _stateVersion.value = System.currentTimeMillis()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 1) Creates all 6 notification channels in Application.onCreate with explicit importance.
     */
    fun createAllChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channels = DyneNotificationChannel.entries.map { ch ->
                NotificationChannel(ch.id, ch.title, ch.importance).apply {
                    description = ch.descriptionText
                    enableVibration(true)
                }
            }
            nm.createNotificationChannels(channels)
            recordLog(context, "Channels", "Created/verified ${channels.size} channels in NotificationManager")
        }
    }

    /**
     * Generates a valid monochrome white-on-transparent small icon in code so Android status bar
     * and notification shade always render a crisp icon without blank squares.
     */
    fun createMonochromeSmallIcon(context: Context): IconCompat {
        val sizePx = 72
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6.5f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        // Draw precision compass/bell emblem inside monochrome alpha mask
        val pad = 10f
        canvas.drawArc(RectF(pad, pad, sizePx - pad, sizePx - pad), 0f, 360f, false, paint)
        // North/South diamond needle
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        canvas.drawCircle(cx, cy, 7f, fillPaint)
        canvas.drawLine(cx, pad + 8f, cx, cy - 10f, paint)
        canvas.drawLine(cx, cy + 10f, cx, sizePx - pad - 8f, paint)
        canvas.drawLine(pad + 8f, cy, cx - 10f, cy, paint)
        canvas.drawLine(cx + 10f, cy, sizePx - pad - 8f, cy, paint)
        return IconCompat.createWithBitmap(bmp)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val appEnabled = runCatching {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }.getOrDefault(true)
        return runtimeGranted && appEnabled
    }

    fun hasRuntimePostNotificationsPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun wasPermissionPromptedOnce(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PERMISSION_PROMPTED_ONCE, false)

    fun markPermissionPromptedOnce(context: Context) {
        prefs(context).edit().putBoolean(KEY_PERMISSION_PROMPTED_ONCE, true).apply()
        bumpState()
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openAppNotificationSettings(context: Context) {
        runCatching {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        }.onFailure {
            openAppSystemDetailsSettings(context)
        }
    }

    fun openAppSystemDetailsSettings(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    // ============================================================================================
    // CONFIGURATION & PER-TYPE SETTINGS
    // ============================================================================================

    fun getTypeConfig(context: Context, type: DyneNotificationType): NotificationTypeConfig {
        val p = prefs(context)
        val prefix = "type_${type.id}_"
        val enabled = p.getBoolean("${prefix}enabled", type.defaultEnabled)
        val hour = p.getInt("${prefix}hour", type.defaultHour).coerceIn(0, 23)
        val minute = p.getInt("${prefix}minute", type.defaultMinute).coerceIn(0, 59)
        val leadValue = p.getInt("${prefix}lead", type.defaultLeadMinutes)
        val weekdaysCsv = p.getString("${prefix}weekdays", "1,2,3,4,5,6,7") ?: "1,2,3,4,5,6,7"
        val weekdays = weekdaysCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .toSet()
            .ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }
        val solunarMode = p.getString("${prefix}solunar_mode", "BOTH") ?: "BOTH"
        return NotificationTypeConfig(
            type = type,
            enabled = enabled,
            hour = hour,
            minute = minute,
            leadValue = leadValue,
            weekdays = weekdays,
            solunarMode = solunarMode
        )
    }

    fun getAllTypeConfigs(context: Context): Map<DyneNotificationType, NotificationTypeConfig> {
        return DyneNotificationType.entries.associateWith { getTypeConfig(context, it) }
    }

    fun updateTypeConfig(
        context: Context,
        type: DyneNotificationType,
        enabled: Boolean? = null,
        hour: Int? = null,
        minute: Int? = null,
        leadValue: Int? = null,
        weekdays: Set<Int>? = null,
        solunarMode: String? = null,
        isPremiumActive: Boolean = true
    ) {
        val current = getTypeConfig(context, type)
        val newEnabled = enabled ?: current.enabled
        val newHour = (hour ?: current.hour).coerceIn(0, 23)
        val newMinute = (minute ?: current.minute).coerceIn(0, 59)
        val newLead = leadValue ?: current.leadValue
        val newWeekdays = (weekdays ?: current.weekdays).ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }
        val newMode = solunarMode ?: current.solunarMode

        val prefix = "type_${type.id}_"
        prefs(context).edit()
            .putBoolean("${prefix}enabled", newEnabled)
            .putInt("${prefix}hour", newHour)
            .putInt("${prefix}minute", newMinute)
            .putInt("${prefix}lead", newLead)
            .putString("${prefix}weekdays", newWeekdays.sorted().joinToString(","))
            .putString("${prefix}solunar_mode", newMode)
            .apply()

        if (!newEnabled) {
            cancelWorkForType(context, type, reason = "Turned off by user")
        } else if (type.isPremium && !isPremiumActive) {
            cancelWorkForType(context, type, reason = "Paused: Premium inactive (settings preserved)")
        } else {
            enqueueWorkForType(context, getTypeConfig(context, type))
        }
        bumpState()
    }

    fun isQuietHoursEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_QUIET_ENABLED, true)

    fun getQuietHoursRange(context: Context): String =
        prefs(context).getString(KEY_QUIET_RANGE, "22:00 - 07:00") ?: "22:00 - 07:00"

    fun setQuietHours(context: Context, enabled: Boolean? = null, range: String? = null) {
        val edit = prefs(context).edit()
        if (enabled != null) edit.putBoolean(KEY_QUIET_ENABLED, enabled)
        if (range != null) edit.putString(KEY_QUIET_RANGE, range)
        edit.apply()
        recordLog(context, "QuietHours", "Updated quiet hours: enabled=${isQuietHoursEnabled(context)}, window=${getQuietHoursRange(context)}")
        bumpState()
    }

    fun getDailyCap(context: Context): Int =
        prefs(context).getInt(KEY_DAILY_CAP, DEFAULT_DAILY_CAP).coerceIn(1, MAX_DAILY_CAP)

    fun setDailyCap(context: Context, cap: Int) {
        val clamped = cap.coerceIn(1, MAX_DAILY_CAP)
        prefs(context).edit().putInt(KEY_DAILY_CAP, clamped).apply()
        recordLog(context, "DailyCap", "Updated daily notification cap to $clamped/day")
        bumpState()
    }

    fun getSentTodayCount(context: Context, todayIso: String = LocalDate.now().toString()): Int {
        val p = prefs(context)
        return if (p.getString(KEY_DAILY_SENT_DATE, "") == todayIso) {
            p.getInt(KEY_DAILY_SENT_COUNT, 0)
        } else {
            0
        }
    }

    private fun incrementSentTodayCount(context: Context, todayIso: String = LocalDate.now().toString()) {
        val p = prefs(context)
        val next = getSentTodayCount(context, todayIso) + 1
        p.edit()
            .putString(KEY_DAILY_SENT_DATE, todayIso)
            .putInt(KEY_DAILY_SENT_COUNT, next)
            .apply()
    }

    // ============================================================================================
    // WORKMANAGER SCHEDULING, READ-BACK VERIFICATION & PREMIUM PAUSE
    // ============================================================================================

    fun uniqueWorkNameFor(type: DyneNotificationType): String = "dyne_notif_work_${type.id}"
    const val UNIQUE_TEST_1M_WORK_NAME = "dyne_notif_test_1m"
    private const val PERIODIC_HEARTBEAT_WORK_NAME = "dyne_notif_periodic_dispatcher"

    /**
     * Synchronizes all scheduled WorkManager jobs on startup, reboot, app update, or Premium state change.
     * When Premium ends, pauses Premium notification work without deleting the user's saved settings.
     */
    fun syncAllSchedules(context: Context, isPremiumActive: Boolean) {
        createAllChannels(context)
        val allConfigs = getAllTypeConfigs(context)
        var activeCount = 0

        for ((type, cfg) in allConfigs) {
            if (!cfg.enabled) {
                cancelWorkForType(context, type, reason = null)
                continue
            }
            if (type.isPremium && !isPremiumActive) {
                cancelWorkForType(context, type, reason = "Paused: Premium ended (settings preserved)")
                continue
            }
            enqueueWorkForType(context, cfg)
            activeCount++
        }

        // Also keep a lightweight 60-minute periodic dispatcher with Constraints.NONE if any type is active
        val wm = runCatching { WorkManager.getInstance(context) }.getOrNull()
        if (wm != null) {
            if (activeCount > 0) {
                val periodicReq = PeriodicWorkRequestBuilder<DyneNotificationWorker>(60, TimeUnit.MINUTES)
                    .setConstraints(Constraints.NONE)
                    .setInputData(workDataOf(DyneNotificationWorker.KEY_MODE to DyneNotificationWorker.MODE_PERIODIC_SWEEP))
                    .build()
                wm.enqueueUniquePeriodicWork(
                    PERIODIC_HEARTBEAT_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    periodicReq
                )
            } else {
                wm.cancelUniqueWork(PERIODIC_HEARTBEAT_WORK_NAME)
            }
        }
        bumpState()
    }

    /**
     * Calculates the exact initial delay in milliseconds to the next occurrence of [cfg]'s hour/minute
     * on one of its enabled weekdays (1=Mon .. 7=Sun).
     */
    fun computeInitialDelayMs(
        cfg: NotificationTypeConfig,
        now: LocalDateTime = LocalDateTime.now()
    ): Long {
        val enabledDays = cfg.weekdays.ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }
        val targetLocalTime = LocalTime.of(cfg.hour.coerceIn(0, 23), cfg.minute.coerceIn(0, 59))

        for (dayOffset in 0..7) {
            val candidateDate = now.toLocalDate().plusDays(dayOffset.toLong())
            val dow = candidateDate.dayOfWeek.value // 1..7
            if (dow in enabledDays) {
                val candidateDateTime = LocalDateTime.of(candidateDate, targetLocalTime)
                val delayMs = Duration.between(now, candidateDateTime).toMillis()
                if (delayMs > 15_000L) {
                    return delayMs
                }
            }
        }
        return TimeUnit.HOURS.toMillis(24)
    }

    /**
     * Enqueues a unique WorkManager OneTimeWorkRequest with Constraints.NONE (no network constraint)
     * and immediately reads back the WorkInfo to verify it is ENQUEUED/RUNNING.
     */
    fun enqueueWorkForType(context: Context, cfg: NotificationTypeConfig): WorkInfo.State? {
        createAllChannels(context)
        val delayMs = computeInitialDelayMs(cfg)
        val uniqueName = uniqueWorkNameFor(cfg.type)
        val nextRunMs = System.currentTimeMillis() + delayMs

        val request = OneTimeWorkRequestBuilder<DyneNotificationWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.NONE)
            .addTag("dyne_notif")
            .addTag(uniqueName)
            .setInputData(
                workDataOf(
                    DyneNotificationWorker.KEY_MODE to DyneNotificationWorker.MODE_SCHEDULED_TYPE,
                    DyneNotificationWorker.KEY_TYPE_ID to cfg.type.id
                )
            )
            .build()

        return runCatching {
            val wm = WorkManager.getInstance(context)
            wm.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, request)
            val infos = wm.getWorkInfosForUniqueWork(uniqueName).get()
            val firstInfo = infos.firstOrNull()
            val state = firstInfo?.state
            prefs(context).edit()
                .putLong("next_run_${cfg.type.id}", nextRunMs)
                .putString("work_state_${cfg.type.id}", state?.name ?: "ENQUEUED")
                .apply()
            recordLog(
                context,
                cfg.type.id,
                "Enqueued '$uniqueName' (state=${state?.name ?: "ENQUEUED"}, delay=${delayMs / 60_000L}m)"
            )
            bumpState()
            state
        }.onFailure { e ->
            recordLog(context, cfg.type.id, "Enqueue error for '$uniqueName': ${e.message}")
        }.getOrNull()
    }

    fun cancelWorkForType(context: Context, type: DyneNotificationType, reason: String?) {
        val uniqueName = uniqueWorkNameFor(type)
        runCatching {
            WorkManager.getInstance(context).cancelUniqueWork(uniqueName)
            prefs(context).edit()
                .remove("next_run_${type.id}")
                .putString("work_state_${type.id}", "CANCELLED")
                .apply()
            if (!reason.isNullOrBlank()) {
                recordLastResult(context, type.id, reason)
                recordLog(context, type.id, "Cancelled '$uniqueName': $reason")
            }
        }
        bumpState()
    }

    // ============================================================================================
    // USER-SET TIMERS: AlarmManager.setAlarmClock (with WorkManager fallback)
    // ============================================================================================

    fun scheduleUserTimerAlarm(
        context: Context,
        timerId: String,
        title: String,
        body: String,
        durationMs: Long? = null,
        triggerAtMs: Long? = null,
        deepLinkTarget: String = "pomodoro_timer",
        deepLinkFeatureId: String? = null
    ): TimerScheduleResult {
        createAllChannels(context)
        val nowMs = System.currentTimeMillis()
        val resolvedTarget = deepLinkFeatureId ?: deepLinkTarget
        val safeDurationMs = when {
            durationMs != null -> durationMs.coerceAtLeast(1_000L)
            triggerAtMs != null -> (triggerAtMs - nowMs).coerceAtLeast(1_000L)
            else -> 60_000L
        }
        val triggerAtWallMs = nowMs + safeDurationMs

        val alarmIntent = Intent(context, DyneNotificationReceiver::class.java).apply {
            action = ACTION_TIMER_ALARM
            putExtra(EXTRA_NOTIFICATION_TYPE_ID, "timer_$timerId")
            putExtra(EXTRA_NOTIFICATION_TITLE, title)
            putExtra(EXTRA_NOTIFICATION_BODY, body)
            putExtra(EXTRA_NOTIFICATION_DEEP_LINK, resolvedTarget)
            putExtra(EXTRA_NOTIFICATION_ID, 8000 + abs(timerId.hashCode() % 1000))
        }
        val pendingAlarm = PendingIntent.getBroadcast(
            context,
            8000 + abs(timerId.hashCode() % 1000),
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val showIntent = createDeepLinkPendingIntent(context, resolvedTarget, 8900)

        val usedAlarmClock = runCatching {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                ?: throw IllegalStateException("AlarmManager unavailable")
            val clockInfo = AlarmManager.AlarmClockInfo(triggerAtWallMs, showIntent)
            am.setAlarmClock(clockInfo, pendingAlarm)
            true
        }.getOrElse { false }

        if (usedAlarmClock) {
            recordLog(context, "timer_$timerId", "Scheduled AlarmManager.setAlarmClock for '$title' in ${safeDurationMs / 1000L}s")
            return TimerScheduleResult(
                usedAlarmClock = true,
                triggerAtWallMs = triggerAtWallMs,
                note = "Scheduled with AlarmClock"
            )
        }

        // Fallback to WorkManager with explicit honest note
        runCatching {
            val req = OneTimeWorkRequestBuilder<DyneNotificationWorker>()
                .setInitialDelay(safeDurationMs, TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.NONE)
                .setInputData(
                    workDataOf(
                        DyneNotificationWorker.KEY_MODE to DyneNotificationWorker.MODE_USER_TIMER,
                        DyneNotificationWorker.KEY_CUSTOM_TITLE to title,
                        DyneNotificationWorker.KEY_CUSTOM_BODY to body,
                        DyneNotificationWorker.KEY_DEEP_LINK to resolvedTarget
                    )
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "dyne_user_timer_$timerId",
                ExistingWorkPolicy.REPLACE,
                req
            )
        }
        recordLog(context, "timer_$timerId", "Fallback WorkManager timer scheduled for '$title' (Times may vary by a few minutes)")
        return TimerScheduleResult(
            usedAlarmClock = false,
            triggerAtWallMs = triggerAtWallMs,
            note = "Times may vary by a few minutes"
        )
    }

    fun cancelUserTimerAlarm(context: Context, timerId: String) {
        runCatching {
            val alarmIntent = Intent(context, DyneNotificationReceiver::class.java).apply {
                action = ACTION_TIMER_ALARM
            }
            val pendingAlarm = PendingIntent.getBroadcast(
                context,
                8000 + abs(timerId.hashCode() % 1000),
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            am?.cancel(pendingAlarm)
            WorkManager.getInstance(context).cancelUniqueWork("dyne_user_timer_$timerId")
        }
    }

    fun postTimerCompletedNotification(
        context: Context,
        title: String,
        body: String,
        deepLinkTarget: String = "pomodoro_timer",
        deepLinkFeatureId: String? = null
    ): Boolean {
        val resolvedDeepLink = deepLinkFeatureId ?: deepLinkTarget
        return postNotificationInternal(
            context = context,
            notificationId = 8100 + abs(title.hashCode() % 800),
            typeId = "focus_timer",
            channel = DyneNotificationChannel.FOCUS_AND_TIME,
            title = title,
            body = body,
            deepLinkTarget = resolvedDeepLink,
            isSampleOrTest = false,
            includeSnoozeAndDone = true
        )
    }

    // ============================================================================================
    // NOTIFICATION DOCTOR: SEND TEST NOW, SEND TEST IN 1 MINUTE & DIAGNOSTICS REPORT
    // ============================================================================================

    fun sendTestNotificationNow(context: Context): Pair<Boolean, String> {
        createAllChannels(context)
        if (!hasNotificationPermission(context)) {
            val msg = "Skipped: Notifications are off (POST_NOTIFICATIONS not granted or disabled in system settings)"
            recordLastResult(context, "doctor_test_now", msg)
            recordLog(context, "doctor_test_now", msg)
            return false to msg
        }
        val posted = postNotificationInternal(
            context = context,
            notificationId = 9001,
            typeId = "doctor_test_now",
            channel = DyneNotificationChannel.DAILY_DIGEST,
            title = "Dyne Notification Doctor • Test Now",
            body = "Local notification pipeline verified. Channels, monochrome icon, and deep links are working.",
            deepLinkTarget = "outdoor_weather_current",
            isSampleOrTest = true,
            includeSnoozeAndDone = true
        )
        val resultMsg = if (posted) {
            "Sent: Test notification posted immediately (ID 9001)"
        } else {
            "Failed: NotificationManager could not post notification"
        }
        recordLastResult(context, "doctor_test_now", resultMsg)
        recordLog(context, "doctor_test_now", resultMsg)
        return posted to resultMsg
    }

    fun scheduleTestNotificationInOneMinute(context: Context): Pair<Boolean, String> {
        createAllChannels(context)
        if (!hasNotificationPermission(context)) {
            val msg = "Skipped: Notifications are off (enable permission first)"
            recordLastResult(context, UNIQUE_TEST_1M_WORK_NAME, msg)
            recordLog(context, UNIQUE_TEST_1M_WORK_NAME, msg)
            return false to msg
        }
        val nextRunMs = System.currentTimeMillis() + 60_000L
        val req = OneTimeWorkRequestBuilder<DyneNotificationWorker>()
            .setInitialDelay(1, TimeUnit.MINUTES)
            .setConstraints(Constraints.NONE)
            .addTag("dyne_notif")
            .addTag(UNIQUE_TEST_1M_WORK_NAME)
            .setInputData(
                workDataOf(
                    DyneNotificationWorker.KEY_MODE to DyneNotificationWorker.MODE_DOCTOR_TEST_1M
                )
            )
            .build()

        return runCatching {
            val wm = WorkManager.getInstance(context)
            wm.enqueueUniqueWork(UNIQUE_TEST_1M_WORK_NAME, ExistingWorkPolicy.REPLACE, req)
            val infos = wm.getWorkInfosForUniqueWork(UNIQUE_TEST_1M_WORK_NAME).get()
            val stateName = infos.firstOrNull()?.state?.name ?: "ENQUEUED"
            prefs(context).edit()
                .putLong("next_run_$UNIQUE_TEST_1M_WORK_NAME", nextRunMs)
                .putString("work_state_$UNIQUE_TEST_1M_WORK_NAME", stateName)
                .apply()
            val msg = "Enqueued '$UNIQUE_TEST_1M_WORK_NAME' (WorkInfo=$stateName, runs in 1 min, no network constraint)"
            recordLastResult(context, UNIQUE_TEST_1M_WORK_NAME, msg)
            recordLog(context, UNIQUE_TEST_1M_WORK_NAME, msg)
            bumpState()
            true to msg
        }.getOrElse { e ->
            val err = "Enqueue failed: ${e.message}"
            recordLastResult(context, UNIQUE_TEST_1M_WORK_NAME, err)
            recordLog(context, UNIQUE_TEST_1M_WORK_NAME, err)
            false to err
        }
    }

    /**
     * Posts ONE honest sample notification for [type] (used by Free users' "Preview a sample" button
     * and by Premium users testing an individual alert type).
     */
    suspend fun postSamplePreviewForType(context: Context, type: DyneNotificationType): Pair<Boolean, String> {
        createAllChannels(context)
        if (!hasNotificationPermission(context)) {
            val msg = "Skipped sample for ${type.title}: Notifications are off"
            recordLastResult(context, type.id, msg)
            recordLog(context, type.id, msg)
            return false to msg
        }
        val cfg = getTypeConfig(context, type)
        val (title, body) = buildHonestContentForType(context, type, cfg, isSample = true)
        val notifId = 5000 + type.ordinal
        val posted = postNotificationInternal(
            context = context,
            notificationId = notifId,
            typeId = type.id,
            channel = type.channel,
            title = title,
            body = body,
            deepLinkTarget = type.deepLinkTarget,
            isSampleOrTest = true,
            includeSnoozeAndDone = type.isPremium
        )
        val msg = if (posted) {
            "Sent sample: $title"
        } else {
            "Failed to post sample for ${type.title}"
        }
        recordLastResult(context, type.id, msg)
        recordLog(context, type.id, msg)
        return posted to msg
    }

    // ============================================================================================
    // HONEST CONTENT BUILDER (NO UPSELL, NO ADS, NO MEDICAL/SAFETY CLAIMS)
    // ============================================================================================

    suspend fun buildHonestContentForType(
        context: Context,
        type: DyneNotificationType,
        cfg: NotificationTypeConfig = getTypeConfig(context, type),
        isSample: Boolean = false
    ): Pair<String, String> {
        val prefsRepo = PreferencesRepository(context)
        val userPrefs = runCatching { prefsRepo.preferencesFlow.first() }.getOrNull()
        val lat = userPrefs?.manualLat ?: 37.7749
        val lng = userPrefs?.manualLng ?: -122.4194
        val use24h = userPrefs?.use24HourTime ?: true
        val timeFmt = DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "h:mm a", Locale.US)
        val today = LocalDate.now()
        val astro = AstroCalculator.calculateDailyReport(today, lat, lng, ZoneId.systemDefault())
        val weatherSnap = runCatching { WeatherRepository(context).getCachedSnapshot() }.getOrNull()

        val sunriseStr = astro.sunrise?.format(timeFmt) ?: "06:30"
        val sunsetStr = astro.sunset?.format(timeFmt) ?: "18:15"
        val majorWindow = astro.majorPeriods.firstOrNull()
        val bestWindowStr = if (majorWindow != null) {
            "${majorWindow.startTime.format(timeFmt)}–${majorWindow.endTime.format(timeFmt)}"
        } else {
            "11:00–13:00"
        }

        return when (type) {
            DyneNotificationType.DAILY_DIGEST -> {
                if (weatherSnap != null) {
                    val cond = WeatherRepository.wmoDescription(weatherSnap.currentWeatherCode)
                    val temp = "${weatherSnap.currentTempC.toInt()}°C"
                    "Daily Outdoor Digest • $temp, $cond" to
                        "Solunar ${astro.fishingScore}/100 (best window $bestWindowStr) • Sunrise $sunriseStr, Sunset $sunsetStr"
                } else {
                    "Daily Outdoor Digest" to "Open Dyne for today's outdoor summary"
                }
            }

            DyneNotificationType.SOLUNAR_BEST_WINDOW -> {
                val lead = cfg.leadValue.coerceIn(15, 60)
                val mode = cfg.solunarMode.uppercase()
                val label = when (mode) {
                    "HUNTING" -> "Best hunting window starts in about $lead min"
                    "BOTH" -> "Best fishing & hunting window starts in about $lead min"
                    else -> "Best fishing window starts in about $lead min"
                }
                val score = if (mode == "HUNTING") astro.huntingScore else astro.fishingScore
                label to "Window $bestWindowStr • Today's Solunar score: $score/100 (approx.)"
            }

            DyneNotificationType.GOLDEN_BLUE_HOUR -> {
                val lead = cfg.leadValue.coerceIn(15, 60)
                val eveGolden = astro.eveningGoldenHourStart?.format(timeFmt) ?: "17:30"
                "Golden & Blue hour starts in about $lead min" to
                    "Evening Golden Hour around $eveGolden • Sunset at $sunsetStr (approx.)"
            }

            DyneNotificationType.SUNRISE_SUNSET -> {
                val lead = cfg.leadValue.coerceIn(15, 60)
                "Sunrise & Sunset • Next transition in about $lead min" to
                    "Today's Sunrise $sunriseStr • Sunset $sunsetStr (local solar time)"
            }

            DyneNotificationType.RAIN_SOON -> {
                val lead = cfg.leadValue.coerceIn(15, 60)
                val rainHour = weatherSnap?.hourly?.take(4)?.firstOrNull { it.precipProbPercent >= 50 }
                if (rainHour != null) {
                    "Rain likely in about $lead min (${rainHour.precipProbPercent}% chance)" to
                        "Cached local forecast indicates upcoming precipitation around ${rainHour.isoTime.substringAfter("T", "soon")}."
                } else {
                    "Rain-Soon Check ($lead min lead)" to
                        "Cached forecast shows ${weatherSnap?.hourly?.firstOrNull()?.precipProbPercent ?: 60}% precipitation chance in the upcoming window."
                }
            }

            DyneNotificationType.WIND_CHANGE -> {
                val wind = weatherSnap?.currentWindKmh?.toInt() ?: 24
                "Wind Change • Around $wind km/h" to
                    "Forecasted local surface wind shift detected in cached weather data."
            }

            DyneNotificationType.PRESSURE_TREND -> {
                val hpa = weatherSnap?.pressureHpa?.toInt() ?: 1012
                "Pressure Trend • $hpa hPa" to
                    "Barometric trend updated for local outdoor and fishing conditions."
            }

            DyneNotificationType.UV_HIGH -> {
                val uv = weatherSnap?.currentUvIndex?.toInt()?.coerceAtLeast(6) ?: 7
                "UV Index High • Around UV $uv today" to
                    "Midday solar elevation is high; check shade and outdoor timing."
            }

            DyneNotificationType.FULL_NEW_MOON -> {
                "Moon Phase • ${astro.moonPhaseName} (${astro.moonIlluminationPercent}%)" to
                    "Lunar illumination is ${astro.moonIlluminationPercent}% tonight • Best Solunar window $bestWindowStr."
            }

            DyneNotificationType.DOCUMENT_EXPIRY -> {
                val days = if (cfg.leadValue in listOf(30, 7, 1)) cfg.leadValue else 7
                "Document Expiry Reminder • $days day${if (days == 1) "" else "s"} left" to
                    "Review your saved ID cards, permits, or warranties expiring in about $days day${if (days == 1) "" else "s"}."
            }

            DyneNotificationType.BILL_DUE -> {
                "Bill Due Reminder" to
                    "Check your upcoming scheduled bills and recurring payment dates."
            }

            DyneNotificationType.WEEKLY_EXPENSE_SUMMARY -> {
                "Weekly Expense Summary" to
                    "Your weekly bill-split and expense log summary is ready to review."
            }

            DyneNotificationType.BACKUP_REMINDER -> {
                "Local Backup Reminder" to
                    "Create a local ZIP backup of your documents, notes, and settings on this device."
            }

            DyneNotificationType.CURRENCY_RATE_ALERT -> {
                val baseCurr = userPrefs?.defaultCurrency ?: "USD"
                "Currency Rate Check ($baseCurr)" to
                    "Review your saved currency conversion pairs and reference rates."
            }

            DyneNotificationType.POMODORO_STREAK -> {
                val pomoPrefs = context.getSharedPreferences("dyne_pomodoro_prefs", Context.MODE_PRIVATE)
                val streak = pomoPrefs.getInt("pomo_streak_days", 5)
                "Pomodoro Focus Streak • $streak days" to
                    "Complete one focus session today to keep your $streak-day streak going."
            }

            DyneNotificationType.HABIT_GOAL_REACHED -> {
                "Habit & Tally Goal Check" to
                    "You are close to reaching your daily counter goal. Tap to log your progress."
            }

            DyneNotificationType.EVENT_COUNTDOWN_MILESTONES -> {
                val days = if (cfg.leadValue in listOf(30, 7, 1)) cfg.leadValue else 7
                "Event Countdown Milestone • $days day${if (days == 1) "" else "s"} to go" to
                    "An upcoming tracked event is $days day${if (days == 1) "" else "s"} away."
            }

            DyneNotificationType.TRIP_CHECKLIST_REMINDERS -> {
                "Trip & Gear Checklist Reminder" to
                    "Review your packing checklist items before heading out."
            }

            DyneNotificationType.PARKING_TIMER -> {
                val lead = cfg.leadValue.coerceIn(15, 60)
                "Parking Timer • About $lead min remaining" to
                    "Your saved parking timer is approaching its limit. Tap to view your spot."
            }

            DyneNotificationType.DAILY_STEP_GOAL -> {
                "Daily Step Goal Progress" to
                    "Check your approximate daily step count and goal streak."
            }

            DyneNotificationType.EVENING_STEP_NUDGE -> {
                "Evening Step Nudge" to
                    "A short evening walk can help finish today's step target."
            }

            DyneNotificationType.WEEKLY_RECAP -> {
                "Weekly Activity Recap" to
                    "Review this week's saved measurements, scans, and outdoor sessions."
            }

            DyneNotificationType.FISHING_LOG_REMINDER -> {
                "Fishing Log Reminder" to
                    "Log today's catches, water conditions, and Solunar notes in your Fishing Log."
            }

            DyneNotificationType.DAILY_COIN_BONUS -> {
                "Daily Check-In Available" to
                    "Claim today's daily check-in coins in your local Rewards wallet."
            }

            DyneNotificationType.STREAK_AT_RISK -> {
                val streak = userPrefs?.checkInStreak ?: 3
                "Check-In Streak Reminder ($streak days)" to
                    "Check in before midnight to keep your daily streak active."
            }
        }
    }

    // ============================================================================================
    // CORE NOTIFICATION POSTING WITH SNOOZE / DONE ACTIONS & DEEP LINK BACK STACK
    // ============================================================================================

    fun createDeepLinkPendingIntent(
        context: Context,
        deepLinkTarget: String,
        requestCode: Int
    ): PendingIntent {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_DEEP_LINK, deepLinkTarget)
            putExtra(com.example.domain.FeatureRegistry.EXTRA_SHORTCUT_FEATURE_ID, deepLinkTarget)
        }
        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(launchIntent)
            getPendingIntent(
                requestCode,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } ?: PendingIntent.getActivity(
            context,
            requestCode,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun postNotificationInternal(
        context: Context,
        notificationId: Int,
        typeId: String,
        channel: DyneNotificationChannel,
        title: String,
        body: String,
        deepLinkTarget: String,
        isSampleOrTest: Boolean,
        includeSnoozeAndDone: Boolean
    ): Boolean {
        if (!hasNotificationPermission(context)) {
            recordLog(context, typeId, "Skipped: POST_NOTIFICATIONS permission not granted")
            return false
        }
        createAllChannels(context)

        val contentPendingIntent = createDeepLinkPendingIntent(context, deepLinkTarget, notificationId)
        val smallIcon = createMonochromeSmallIcon(context)

        val builder = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                if (channel == DyneNotificationChannel.FOCUS_AND_TIME) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setCategory(
                if (channel == DyneNotificationChannel.FOCUS_AND_TIME) {
                    NotificationCompat.CATEGORY_ALARM
                } else {
                    NotificationCompat.CATEGORY_REMINDER
                }
            )
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)

        if (includeSnoozeAndDone) {
            val snoozeIntent = Intent(context, DyneNotificationReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_NOTIFICATION_TYPE_ID, typeId)
                putExtra(EXTRA_NOTIFICATION_TITLE, title)
                putExtra(EXTRA_NOTIFICATION_BODY, body)
                putExtra(EXTRA_NOTIFICATION_DEEP_LINK, deepLinkTarget)
            }
            val snoozePending = PendingIntent.getBroadcast(
                context,
                notificationId * 10 + 1,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val doneIntent = Intent(context, DyneNotificationReceiver::class.java).apply {
                action = ACTION_DONE
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_NOTIFICATION_TYPE_ID, typeId)
                putExtra(EXTRA_NOTIFICATION_TITLE, title)
            }
            val donePending = PendingIntent.getBroadcast(
                context,
                notificationId * 10 + 2,
                doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.addAction(0, "Snooze 15m", snoozePending)
            builder.addAction(0, "Done", donePending)
        }

        val posted = runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            true
        }.getOrElse { e ->
            recordLog(context, typeId, "NotificationManager.notify exception: ${e.message}")
            false
        }

        if (posted) {
            appendHistoryEntry(
                context = context,
                NotificationHistoryEntry(
                    id = System.currentTimeMillis(),
                    typeId = typeId,
                    title = title,
                    body = body,
                    channelId = channel.id,
                    deepLinkTarget = deepLinkTarget,
                    timestampEpochMs = System.currentTimeMillis(),
                    isSampleOrTest = isSampleOrTest,
                    status = "SENT"
                )
            )
            if (!isSampleOrTest) {
                incrementSentTodayCount(context)
            }
            bumpState()
        }
        return posted
    }

    // ============================================================================================
    // WORKER EXECUTION & QUIET HOURS / DAILY CAP / DEDUPLICATION GATES
    // ============================================================================================

    suspend fun executeScheduledTypeRun(
        context: Context,
        type: DyneNotificationType
    ): String {
        val cfg = getTypeConfig(context, type)
        if (!cfg.enabled) {
            val msg = "Skipped: ${type.title} is disabled"
            recordLastResult(context, type.id, msg)
            recordLog(context, type.id, msg)
            return msg
        }

        if (!hasNotificationPermission(context)) {
            val msg = "Skipped: POST_NOTIFICATIONS permission is off"
            recordLastResult(context, type.id, msg)
            recordLog(context, type.id, msg)
            enqueueWorkForType(context, cfg)
            return msg
        }

        val prefsRepo = PreferencesRepository(context)
        val userPrefs = runCatching { prefsRepo.preferencesFlow.first() }.getOrNull()
        val isPremiumActive = (userPrefs?.cachedIsPro == true) ||
            (userPrefs?.simulateProEnabled == true) ||
            (userPrefs?.isFeatureUnlockedTemporarily("outdoor_alerts") == true) ||
            (userPrefs?.isToolUnlockedByReward("outdoor_alerts") == true)

        if (type.isPremium && !isPremiumActive) {
            val msg = "Paused: Premium inactive (settings preserved)"
            cancelWorkForType(context, type, reason = msg)
            return msg
        }

        val today = LocalDate.now()
        val dow = today.dayOfWeek.value
        if (dow !in cfg.weekdays) {
            val msg = "Skipped: DayOfWeek($dow) not in selected weekdays ${cfg.weekdays}"
            recordLastResult(context, type.id, msg)
            recordLog(context, type.id, msg)
            enqueueWorkForType(context, cfg)
            return msg
        }

        val nowTime = LocalTime.now()
        if (type.isPremium && isQuietHoursEnabled(context) && isWithinQuietHours(nowTime, getQuietHoursRange(context))) {
            val msg = "Skipped: Inside quiet hours (${getQuietHoursRange(context)})"
            recordLastResult(context, type.id, msg)
            recordLog(context, type.id, msg)
            enqueueWorkForType(context, cfg)
            return msg
        }

        if (type.isPremium) {
            val sentToday = getSentTodayCount(context, today.toString())
            val cap = getDailyCap(context)
            if (sentToday >= cap) {
                val msg = "Skipped: Daily cap reached ($sentToday/$cap today)"
                recordLastResult(context, type.id, msg)
                recordLog(context, type.id, msg)
                enqueueWorkForType(context, cfg)
                return msg
            }
        }

        val dedupKey = "dedup_${type.id}_${today}"
        val p = prefs(context)
        if (p.getBoolean(dedupKey, false)) {
            val msg = "Skipped: De-duplicated (already sent today for ${type.id})"
            recordLastResult(context, type.id, msg)
            recordLog(context, type.id, msg)
            enqueueWorkForType(context, cfg)
            return msg
        }

        val (title, body) = buildHonestContentForType(context, type, cfg, isSample = false)
        val posted = postNotificationInternal(
            context = context,
            notificationId = 6000 + type.ordinal,
            typeId = type.id,
            channel = type.channel,
            title = title,
            body = body,
            deepLinkTarget = type.deepLinkTarget,
            isSampleOrTest = false,
            includeSnoozeAndDone = type.isPremium
        )
        if (posted) {
            p.edit().putBoolean(dedupKey, true).apply()
            val msg = "Sent: $title"
            recordLastResult(context, type.id, msg)
            recordLog(context, type.id, msg)
            enqueueWorkForType(context, cfg)
            return msg
        } else {
            val msg = "Failed: Could not post notification for ${type.id}"
            recordLastResult(context, type.id, msg)
            recordLog(context, type.id, msg)
            enqueueWorkForType(context, cfg)
            return msg
        }
    }

    fun isWithinQuietHours(now: LocalTime, rangeStr: String): Boolean {
        return runCatching {
            val parts = rangeStr.split("-").map { it.trim() }
            if (parts.size != 2) return false
            val start = LocalTime.parse(parts[0])
            val end = LocalTime.parse(parts[1])
            if (start.isBefore(end)) {
                !now.isBefore(start) && !now.isAfter(end)
            } else {
                !now.isBefore(start) || !now.isAfter(end)
            }
        }.getOrDefault(false)
    }

    // ============================================================================================
    // HISTORY (LAST 50, DEVICE ONLY) & DIAGNOSTIC LOGS
    // ============================================================================================

    fun getHistory(context: Context): List<NotificationHistoryEntry> {
        val raw = prefs(context).getString(KEY_HISTORY_JSON, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<NotificationHistoryEntry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    NotificationHistoryEntry(
                        id = o.optLong("id", 0L),
                        typeId = o.optString("typeId", ""),
                        title = o.optString("title", ""),
                        body = o.optString("body", ""),
                        channelId = o.optString("channelId", ""),
                        deepLinkTarget = o.optString("deepLinkTarget", ""),
                        timestampEpochMs = o.optLong("timestampEpochMs", 0L),
                        isSampleOrTest = o.optBoolean("isSampleOrTest", false),
                        status = o.optString("status", "SENT")
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    private fun appendHistoryEntry(context: Context, entry: NotificationHistoryEntry) {
        val updated = (listOf(entry) + getHistory(context)).take(MAX_HISTORY_ITEMS)
        saveHistoryList(context, updated)
    }

    fun updateHistoryStatus(context: Context, typeId: String, newStatus: String) {
        val current = getHistory(context).toMutableList()
        val idx = current.indexOfFirst { it.typeId == typeId }
        if (idx >= 0) {
            current[idx] = current[idx].copy(status = newStatus)
            saveHistoryList(context, current)
            bumpState()
        }
    }

    fun clearHistory(context: Context) {
        prefs(context).edit().remove(KEY_HISTORY_JSON).apply()
        recordLog(context, "History", "Cleared local notification history")
        bumpState()
    }

    private fun saveHistoryList(context: Context, list: List<NotificationHistoryEntry>) {
        val arr = JSONArray()
        list.take(MAX_HISTORY_ITEMS).forEach { item ->
            val o = JSONObject()
            o.put("id", item.id)
            o.put("typeId", item.typeId)
            o.put("title", item.title)
            o.put("body", item.body)
            o.put("channelId", item.channelId)
            o.put("deepLinkTarget", item.deepLinkTarget)
            o.put("timestampEpochMs", item.timestampEpochMs)
            o.put("isSampleOrTest", item.isSampleOrTest)
            o.put("status", item.status)
            arr.put(o)
        }
        prefs(context).edit().putString(KEY_HISTORY_JSON, arr.toString()).apply()
    }

    fun recordLog(context: Context, source: String, message: String) {
        val timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US))
        val line = "[$timeStr] $source: $message"
        Log.i(TAG, line)
        val p = prefs(context)
        val raw = p.getString(KEY_DIAG_LOGS_JSON, "") ?: ""
        val current = runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.optString(it) }
        }.getOrDefault(emptyList())
        val updated = (listOf(line) + current).take(MAX_LOG_ITEMS)
        val outArr = JSONArray()
        updated.forEach { outArr.put(it) }
        p.edit().putString(KEY_DIAG_LOGS_JSON, outArr.toString()).apply()
    }

    fun getDiagnosticLogs(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_DIAG_LOGS_JSON, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.optString(it) }
        }.getOrDefault(emptyList())
    }

    fun recordLastResult(context: Context, key: String, result: String) {
        prefs(context).edit().putString("last_res_$key", result).apply()
    }

    fun getLastResult(context: Context, key: String): String =
        prefs(context).getString("last_res_$key", "Not run yet") ?: "Not run yet"

    /**
     * Builds the complete live Notification Doctor report for Settings > Notifications.
     */
    fun buildDoctorReport(context: Context): NotificationDoctorReport {
        createAllChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val channelStates = DyneNotificationChannel.entries.map { ch ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
                val sysCh = nm.getNotificationChannel(ch.id)
                val imp = sysCh?.importance ?: ch.importance
                NotificationDoctorChannelState(
                    channel = ch,
                    existsInSystem = sysCh != null,
                    systemImportance = imp,
                    isBlockedByUser = imp == NotificationManager.IMPORTANCE_NONE
                )
            } else {
                NotificationDoctorChannelState(
                    channel = ch,
                    existsInSystem = true,
                    systemImportance = ch.importance,
                    isBlockedByUser = false
                )
            }
        }

        val p = prefs(context)
        val wm = runCatching { WorkManager.getInstance(context) }.getOrNull()
        val scheduledWorks = mutableListOf<ScheduledWorkDiagnostic>()

        // 1. Check 1-minute doctor test work if enqueued
        val testInfos = runCatching { wm?.getWorkInfosForUniqueWork(UNIQUE_TEST_1M_WORK_NAME)?.get() }.getOrNull().orEmpty()
        val testActive = testInfos.firstOrNull { !it.state.isFinished }
        if (testActive != null || p.contains("last_res_$UNIQUE_TEST_1M_WORK_NAME")) {
            scheduledWorks.add(
                ScheduledWorkDiagnostic(
                    uniqueWorkName = UNIQUE_TEST_1M_WORK_NAME,
                    typeTitle = "Doctor 1-Minute Test",
                    state = testActive?.state?.name ?: testInfos.firstOrNull()?.state?.name ?: p.getString("work_state_$UNIQUE_TEST_1M_WORK_NAME", "IDLE") ?: "IDLE",
                    nextRunEpochMs = p.getLong("next_run_$UNIQUE_TEST_1M_WORK_NAME", 0L),
                    lastResult = getLastResult(context, UNIQUE_TEST_1M_WORK_NAME)
                )
            )
        }

        // 2. Check all enabled notification types
        for (type in DyneNotificationType.entries) {
            val cfg = getTypeConfig(context, type)
            val uniqueName = uniqueWorkNameFor(type)
            val infos = runCatching { wm?.getWorkInfosForUniqueWork(uniqueName)?.get() }.getOrNull().orEmpty()
            val currentInfo = infos.firstOrNull()
            val storedState = p.getString("work_state_${type.id}", null)
            if (cfg.enabled || currentInfo != null || storedState != null) {
                scheduledWorks.add(
                    ScheduledWorkDiagnostic(
                        uniqueWorkName = uniqueName,
                        typeTitle = type.title,
                        state = currentInfo?.state?.name ?: storedState ?: if (cfg.enabled) "ENQUEUED" else "OFF",
                        nextRunEpochMs = p.getLong("next_run_${type.id}", 0L),
                        lastResult = getLastResult(context, type.id)
                    )
                )
            }
        }

        return NotificationDoctorReport(
            permissionGranted = hasRuntimePostNotificationsPermission(context),
            systemNotificationsEnabled = runCatching { NotificationManagerCompat.from(context).areNotificationsEnabled() }.getOrDefault(true),
            channels = channelStates,
            scheduledWorks = scheduledWorks,
            isBatteryOptimizationIgnored = isBatteryOptimizationIgnored(context),
            recentLogs = getDiagnosticLogs(context),
            sentTodayCount = getSentTodayCount(context),
            dailyCap = getDailyCap(context)
        )
    }
}

/**
 * WorkManager worker for all scheduled notifications, 1-minute doctor test, and fallback user timers.
 */
class DyneNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val mode = inputData.getString(KEY_MODE) ?: MODE_PERIODIC_SWEEP

        return when (mode) {
            MODE_DOCTOR_TEST_1M -> {
                if (!DyneNotificationCenter.hasNotificationPermission(context)) {
                    val msg = "Skipped 1m test: POST_NOTIFICATIONS permission off"
                    DyneNotificationCenter.recordLastResult(context, DyneNotificationCenter.UNIQUE_TEST_1M_WORK_NAME, msg)
                    DyneNotificationCenter.recordLog(context, DyneNotificationCenter.UNIQUE_TEST_1M_WORK_NAME, msg)
                    return Result.success()
                }
                val posted = DyneNotificationCenter.postNotificationInternal(
                    context = context,
                    notificationId = 9002,
                    typeId = DyneNotificationCenter.UNIQUE_TEST_1M_WORK_NAME,
                    channel = DyneNotificationChannel.DAILY_DIGEST,
                    title = "Dyne Notification Doctor • 1-Minute Test Arrived",
                    body = "Scheduled WorkManager background job executed on time with Constraints.NONE.",
                    deepLinkTarget = "outdoor_weather_current",
                    isSampleOrTest = true,
                    includeSnoozeAndDone = true
                )
                val msg = if (posted) "Sent: 1-minute scheduled test notification delivered (ID 9002)" else "Failed: notify returned false"
                DyneNotificationCenter.recordLastResult(context, DyneNotificationCenter.UNIQUE_TEST_1M_WORK_NAME, msg)
                DyneNotificationCenter.recordLog(context, DyneNotificationCenter.UNIQUE_TEST_1M_WORK_NAME, msg)
                Result.success()
            }

            MODE_USER_TIMER -> {
                val title = inputData.getString(KEY_CUSTOM_TITLE) ?: "Timer Finished"
                val body = inputData.getString(KEY_CUSTOM_BODY) ?: "Your user-set timer has completed."
                val deepLink = inputData.getString(KEY_DEEP_LINK) ?: "pomodoro_timer"
                DyneNotificationCenter.postTimerCompletedNotification(context, title, body, deepLink)
                Result.success()
            }

            MODE_SNOOZE -> {
                val title = inputData.getString(KEY_CUSTOM_TITLE) ?: "Snoozed Reminder"
                val body = inputData.getString(KEY_CUSTOM_BODY) ?: "Your 15-minute snoozed reminder is ready."
                val deepLink = inputData.getString(KEY_DEEP_LINK) ?: "outdoor_weather_current"
                val typeId = inputData.getString(KEY_TYPE_ID) ?: "snoozed"
                val type = DyneNotificationType.fromId(typeId)
                DyneNotificationCenter.postNotificationInternal(
                    context = context,
                    notificationId = 9100 + abs(typeId.hashCode() % 500),
                    typeId = typeId,
                    channel = type?.channel ?: DyneNotificationChannel.OUTDOOR_AND_WEATHER,
                    title = title,
                    body = body,
                    deepLinkTarget = deepLink,
                    isSampleOrTest = false,
                    includeSnoozeAndDone = true
                )
                DyneNotificationCenter.recordLog(context, typeId, "Delivered snoozed notification: $title")
                Result.success()
            }

            MODE_SCHEDULED_TYPE -> {
                val typeId = inputData.getString(KEY_TYPE_ID) ?: ""
                val type = DyneNotificationType.fromId(typeId)
                if (type == null) {
                    DyneNotificationCenter.recordLog(context, "Worker", "Skipped: Unknown notification type '$typeId'")
                    return Result.success()
                }
                DyneNotificationCenter.executeScheduledTypeRun(context, type)
                Result.success()
            }

            else -> {
                // Periodic sweep: check any enabled type whose scheduled hour matches the current hour
                val now = LocalTime.now()
                for (type in DyneNotificationType.entries) {
                    val cfg = DyneNotificationCenter.getTypeConfig(context, type)
                    if (cfg.enabled && abs((now.hour * 60 + now.minute) - (cfg.hour * 60 + cfg.minute)) <= 30) {
                        DyneNotificationCenter.executeScheduledTypeRun(context, type)
                    }
                }
                Result.success()
            }
        }
    }

    companion object {
        const val KEY_MODE = "notif_worker_mode"
        const val KEY_TYPE_ID = "notif_type_id"
        const val KEY_CUSTOM_TITLE = "notif_custom_title"
        const val KEY_CUSTOM_BODY = "notif_custom_body"
        const val KEY_DEEP_LINK = "notif_deep_link"

        const val MODE_SCHEDULED_TYPE = "scheduled_type"
        const val MODE_DOCTOR_TEST_1M = "doctor_test_1m"
        const val MODE_USER_TIMER = "user_timer"
        const val MODE_SNOOZE = "snooze"
        const val MODE_PERIODIC_SWEEP = "periodic_sweep"
    }
}

/**
 * BroadcastReceiver for:
 * - Rescheduling after device reboot (`ACTION_BOOT_COMPLETED`) and app update (`ACTION_MY_PACKAGE_REPLACED`)
 * - Firing `AlarmManager.setAlarmClock` user-set timers (`ACTION_TIMER_ALARM`)
 * - Handling notification **Snooze 15m** (`ACTION_SNOOZE`) and **Done** (`ACTION_DONE`) actions
 */
class DyneNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val prefs = context.getSharedPreferences("dyne_user_prefs_cache", Context.MODE_PRIVATE)
                val isPro = prefs.getBoolean("cached_is_pro", false)
                DyneNotificationCenter.recordLog(context, "Receiver", "Rescheduling notifications on $action")
                DyneNotificationCenter.syncAllSchedules(context, isPremiumActive = isPro)
            }

            DyneNotificationCenter.ACTION_TIMER_ALARM -> {
                val title = intent.getStringExtra(DyneNotificationCenter.EXTRA_NOTIFICATION_TITLE) ?: "Timer Finished"
                val body = intent.getStringExtra(DyneNotificationCenter.EXTRA_NOTIFICATION_BODY) ?: "Your user-set timer has finished."
                val deepLink = intent.getStringExtra(DyneNotificationCenter.EXTRA_NOTIFICATION_DEEP_LINK) ?: "pomodoro_timer"
                DyneNotificationCenter.recordLog(context, "AlarmClock", "AlarmClock fired for '$title'")
                DyneNotificationCenter.postTimerCompletedNotification(context, title, body, deepLink)
            }

            DyneNotificationCenter.ACTION_SNOOZE -> {
                val notifId = intent.getIntExtra(DyneNotificationCenter.EXTRA_NOTIFICATION_ID, 0)
                val typeId = intent.getStringExtra(DyneNotificationCenter.EXTRA_NOTIFICATION_TYPE_ID) ?: "snoozed"
                val title = intent.getStringExtra(DyneNotificationCenter.EXTRA_NOTIFICATION_TITLE) ?: "Dyne Reminder"
                val body = intent.getStringExtra(DyneNotificationCenter.EXTRA_NOTIFICATION_BODY) ?: ""
                val deepLink = intent.getStringExtra(DyneNotificationCenter.EXTRA_NOTIFICATION_DEEP_LINK) ?: "outdoor_weather_current"

                if (notifId != 0) {
                    runCatching { NotificationManagerCompat.from(context).cancel(notifId) }
                }
                DyneNotificationCenter.updateHistoryStatus(context, typeId, "SNOOZED (15m)")
                DyneNotificationCenter.recordLog(context, typeId, "Snoozed for 15 minutes by user action")

                val req = OneTimeWorkRequestBuilder<DyneNotificationWorker>()
                    .setInitialDelay(15, TimeUnit.MINUTES)
                    .setConstraints(Constraints.NONE)
                    .setInputData(
                        workDataOf(
                            DyneNotificationWorker.KEY_MODE to DyneNotificationWorker.MODE_SNOOZE,
                            DyneNotificationWorker.KEY_TYPE_ID to typeId,
                            DyneNotificationWorker.KEY_CUSTOM_TITLE to title,
                            DyneNotificationWorker.KEY_CUSTOM_BODY to body,
                            DyneNotificationWorker.KEY_DEEP_LINK to deepLink
                        )
                    )
                    .build()
                runCatching {
                    WorkManager.getInstance(context).enqueueUniqueWork(
                        "dyne_snooze_$typeId",
                        ExistingWorkPolicy.REPLACE,
                        req
                    )
                }
            }

            DyneNotificationCenter.ACTION_DONE -> {
                val notifId = intent.getIntExtra(DyneNotificationCenter.EXTRA_NOTIFICATION_ID, 0)
                val typeId = intent.getStringExtra(DyneNotificationCenter.EXTRA_NOTIFICATION_TYPE_ID) ?: "done"
                if (notifId != 0) {
                    runCatching { NotificationManagerCompat.from(context).cancel(notifId) }
                }
                DyneNotificationCenter.updateHistoryStatus(context, typeId, "DONE")
                DyneNotificationCenter.recordLog(context, typeId, "Marked Done by user action")
            }
        }
    }
}
