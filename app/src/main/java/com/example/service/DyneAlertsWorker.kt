package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.R
import com.example.data.PreferencesRepository
import com.example.data.WeatherRepository
import com.example.domain.AstroCalculator
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * On-device local notifications worker powered by AndroidX WorkManager (no server required):
 * 1) Solunar best-window alerts (Fishing or Hunting mode, lead time 15 / 30 / 60 minutes)
 * 2) Golden-hour alerts (morning & evening golden hour windows)
 * 3) Daily weather summary notification
 * 4) Rain-soon alerts (upcoming hourly precipitation probability / rain codes)
 */
class DyneAlertsWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!DyneAlertsScheduler.hasNotificationPermission(context)) {
            return Result.success()
        }

        val prefsRepo = PreferencesRepository(context)
        val prefs = prefsRepo.preferencesFlow.first()

        // Ensure user has Pro or temporary unlock for Outdoor Alerts (unless triggered as preview)
        val isPreview = inputData.getBoolean(KEY_IS_PREVIEW, false)
        val previewType = inputData.getString(KEY_PREVIEW_TYPE) ?: ""
        val isUnlocked = prefs.cachedIsPro ||
            prefs.simulateProEnabled ||
            prefs.isFeatureUnlockedTemporarily("outdoor_alerts") ||
            prefs.isToolUnlockedByReward("outdoor_alerts")

        if (!isPreview && !isUnlocked) {
            return Result.success()
        }

        // Respect quiet hours unless user explicitly tapped a preview/test button
        val nowTime = LocalTime.now()
        if (!isPreview && prefs.solunarQuietHoursEnabled && isWithinQuietHours(nowTime, prefs.solunarQuietHours)) {
            return Result.success()
        }

        val lat = prefs.manualLat
        val lng = prefs.manualLng
        val today = LocalDate.now()
        val report = AstroCalculator.calculateDailyReport(today, lat, lng, ZoneId.systemDefault())
        val timeFmt = if (prefs.use24HourTime) {
            DateTimeFormatter.ofPattern("HH:mm")
        } else {
            DateTimeFormatter.ofPattern("h:mm a")
        }

        if (isPreview && previewType.isNotBlank()) {
            firePreviewNotification(context, previewType, prefs.solunarAlertMode, prefs.solunarAlertLeadMinutes, report, lat, lng, timeFmt)
            return Result.success()
        }

        val statePrefs = context.getSharedPreferences(PREFS_SENT_TRACKER, Context.MODE_PRIVATE)
        val nowMinutes = nowTime.hour * 60 + nowTime.minute

        // 1. Solunar best-window alert (Fishing or Hunting, lead time 15 / 30 / 60 min)
        if (prefs.solunarAlertsEnabled) {
            val leadMins = prefs.solunarAlertLeadMinutes.coerceIn(15, 60)
            val isFishing = !prefs.solunarAlertMode.equals("HUNTING", ignoreCase = true)
            val modeLabel = if (isFishing) "Fishing" else "Hunting"
            val score = report.scoreForMode(isFishing)
            val allWindows = report.majorPeriods + report.minorPeriods
            for (period in allWindows) {
                val targetAlertMin = (period.startMinutesOfDay - leadMins).coerceAtLeast(0)
                if (abs(nowMinutes - targetAlertMin) <= 20) {
                    val dedupKey = "solunar_${today}_${period.title}_${period.startMinutesOfDay}"
                    if (!statePrefs.getBoolean(dedupKey, false)) {
                        statePrefs.edit().putBoolean(dedupKey, true).apply()
                        DyneAlertsScheduler.postNotification(
                            context = context,
                            notificationId = 4101 + period.startMinutesOfDay,
                            title = "$modeLabel ${period.title} in $leadMins min",
                            body = "${period.startTime.format(timeFmt)} – ${period.endTime.format(timeFmt)} • Today's $modeLabel Solunar score: $score/100 (approx.)"
                        )
                    }
                }
            }
        }

        // 2. Golden-hour alert
        if (prefs.goldenHourAlertsEnabled) {
            val eveningGolden = report.eveningGoldenHourStart
            if (eveningGolden != null) {
                val goldenMin = eveningGolden.hour * 60 + eveningGolden.minute
                val triggerMin = (goldenMin - 20).coerceAtLeast(0)
                if (abs(nowMinutes - triggerMin) <= 20) {
                    val dedupKey = "golden_eve_$today"
                    if (!statePrefs.getBoolean(dedupKey, false)) {
                        statePrefs.edit().putBoolean(dedupKey, true).apply()
                        val sunsetStr = report.sunset?.format(timeFmt) ?: "--:--"
                        DyneAlertsScheduler.postNotification(
                            context = context,
                            notificationId = 4201,
                            title = "Golden Hour starts at ${eveningGolden.format(timeFmt)}",
                            body = "Warm directional sunlight ahead • Sunset at $sunsetStr (approx.)"
                        )
                    }
                }
            }
        }

        // 3. Daily Weather Summary & 4. Rain-Soon Alert (Open-Meteo cached/live)
        if (prefs.dailyWeatherSummaryEnabled || prefs.rainSoonAlertsEnabled) {
            val weatherRepo = WeatherRepository(context)
            val snapshot = weatherRepo.getCachedSnapshot()
                ?: weatherRepo.fetchWeather(lat, lng, prefs.locationName).getOrNull()

            if (snapshot != null) {
                if (prefs.dailyWeatherSummaryEnabled && nowTime.hour in 7..10) {
                    val dedupKey = "weather_daily_$today"
                    if (!statePrefs.getBoolean(dedupKey, false)) {
                        statePrefs.edit().putBoolean(dedupKey, true).apply()
                        val todayDaily = snapshot.daily.firstOrNull()
                        val hiLo = if (todayDaily != null) {
                            "H: ${todayDaily.tempMaxC.toInt()}°C / L: ${todayDaily.tempMinC.toInt()}°C"
                        } else {
                            "${snapshot.currentTempC.toInt()}°C"
                        }
                        val desc = WeatherRepository.wmoDescription(snapshot.currentWeatherCode)
                        DyneAlertsScheduler.postNotification(
                            context = context,
                            notificationId = 4301,
                            title = "Daily Outdoor Weather • $desc",
                            body = "$hiLo • Wind ${snapshot.currentWindKmh.toInt()} km/h • UV ${snapshot.currentUvIndex.toInt()} • Solunar ${report.fishingScore}/100"
                        )
                    }
                }

                if (prefs.rainSoonAlertsEnabled) {
                    val upcomingRain = snapshot.hourly.take(3).firstOrNull { h ->
                        h.precipProbPercent >= 55 || h.weatherCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99)
                    }
                    if (upcomingRain != null) {
                        val dedupKey = "rain_${today}_${nowTime.hour / 3}"
                        if (!statePrefs.getBoolean(dedupKey, false)) {
                            statePrefs.edit().putBoolean(dedupKey, true).apply()
                            val hourLabel = upcomingRain.isoTime.substringAfter("T", "soon")
                            DyneAlertsScheduler.postNotification(
                                context = context,
                                notificationId = 4401,
                                title = "Rain-Soon Alert • ${upcomingRain.precipProbPercent}% chance",
                                body = "Precipitation expected around $hourLabel (${WeatherRepository.wmoDescription(upcomingRain.weatherCode)}, approx.)"
                            )
                        }
                    }
                }
            }
        }

        return Result.success()
    }

    private suspend fun firePreviewNotification(
        context: Context,
        previewType: String,
        solunarMode: String,
        leadMins: Int,
        report: com.example.domain.DailyAstroReport,
        lat: Double,
        lng: Double,
        timeFmt: DateTimeFormatter
    ) {
        when (previewType.uppercase()) {
            "SOLUNAR" -> {
                val isFishing = !solunarMode.equals("HUNTING", ignoreCase = true)
                val modeLabel = if (isFishing) "Fishing" else "Hunting"
                val nextMajor = report.majorPeriods.firstOrNull()
                val windowText = if (nextMajor != null) {
                    "${nextMajor.startTime.format(timeFmt)} – ${nextMajor.endTime.format(timeFmt)}"
                } else {
                    "Peak window today"
                }
                DyneAlertsScheduler.postNotification(
                    context = context,
                    notificationId = 4501,
                    title = "Solunar $modeLabel Alert ($leadMins min lead)",
                    body = "Next Major Window: $windowText • $modeLabel score ${report.scoreForMode(isFishing)}/100 (approx.)"
                )
            }
            "GOLDEN_HOUR" -> {
                val eve = report.eveningGoldenHourStart?.format(timeFmt) ?: "17:45"
                val sunset = report.sunset?.format(timeFmt) ?: "18:30"
                DyneAlertsScheduler.postNotification(
                    context = context,
                    notificationId = 4502,
                    title = "Golden Hour Alert • Starts at $eve",
                    body = "Soft low-angle light for photography & outdoors • Sunset $sunset (approx.)"
                )
            }
            "DAILY_WEATHER" -> {
                val weatherRepo = WeatherRepository(context)
                val snap = weatherRepo.getCachedSnapshot()
                    ?: weatherRepo.fetchWeather(lat, lng, "Local").getOrNull()
                val tempText = if (snap != null) "${snap.currentTempC.toInt()}°C • ${WeatherRepository.wmoDescription(snap.currentWeatherCode)}" else "Clear skies"
                DyneAlertsScheduler.postNotification(
                    context = context,
                    notificationId = 4503,
                    title = "Daily Outdoor Weather Summary",
                    body = "$tempText • Moon: ${report.moonPhaseName} (${report.moonIlluminationPercent}%) • Solunar ${report.fishingScore}/100"
                )
            }
            "RAIN_SOON" -> {
                DyneAlertsScheduler.postNotification(
                    context = context,
                    notificationId = 4504,
                    title = "Rain-Soon Alert Active",
                    body = "WorkManager monitors cached & hourly Open-Meteo precipitation probability locally on your device."
                )
            }
        }
    }

    private fun isWithinQuietHours(now: LocalTime, rangeStr: String): Boolean {
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

    companion object {
        const val KEY_IS_PREVIEW = "is_preview"
        const val KEY_PREVIEW_TYPE = "preview_type"
        private const val PREFS_SENT_TRACKER = "dyne_alerts_sent_tracker"
    }
}

object DyneAlertsScheduler {
    private const val PERIODIC_WORK_NAME = "dyne_outdoor_alerts_periodic_work"

    fun hasNotificationPermission(context: Context): Boolean {
        return DyneNotificationCenter.hasNotificationPermission(context)
    }

    fun ensureChannel(context: Context) {
        DyneNotificationCenter.createAllChannels(context)
    }

    fun scheduleOrUpdateAlerts(context: Context, isPremiumActive: Boolean = false) {
        DyneNotificationCenter.createAllChannels(context)
        val cachePrefs = context.applicationContext.getSharedPreferences("dyne_user_prefs_cache", Context.MODE_PRIVATE)
        val effectivePro = isPremiumActive || cachePrefs.getBoolean("cached_is_pro", false)
        DyneNotificationCenter.syncAllSchedules(context, isPremiumActive = effectivePro)
    }

    fun triggerPreviewAlert(context: Context, type: String) {
        DyneNotificationCenter.createAllChannels(context)
        val mappedType = when (type.uppercase()) {
            "SOLUNAR" -> DyneNotificationType.SOLUNAR_BEST_WINDOW
            "GOLDEN_HOUR" -> DyneNotificationType.GOLDEN_BLUE_HOUR
            "DAILY_WEATHER", "DAILY_DIGEST" -> DyneNotificationType.DAILY_DIGEST
            "RAIN_SOON" -> DyneNotificationType.RAIN_SOON
            else -> DyneNotificationType.fromId(type) ?: DyneNotificationType.SOLUNAR_BEST_WINDOW
        }
        val req = OneTimeWorkRequestBuilder<DyneAlertsWorker>()
            .setConstraints(androidx.work.Constraints.NONE)
            .setInputData(
                workDataOf(
                    DyneAlertsWorker.KEY_IS_PREVIEW to true,
                    DyneAlertsWorker.KEY_PREVIEW_TYPE to mappedType.id
                )
            )
            .build()
        runCatching {
            WorkManager.getInstance(context).enqueue(req)
        }
    }

    fun postNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        deepLinkTarget: String = "outdoor_solunar_today"
    ) {
        DyneNotificationCenter.postNotificationInternal(
            context = context,
            notificationId = notificationId,
            typeId = "outdoor_alert",
            channel = DyneNotificationChannel.OUTDOOR_AND_WEATHER,
            title = title,
            body = body,
            deepLinkTarget = deepLinkTarget,
            isSampleOrTest = false,
            includeSnoozeAndDone = true
        )
    }
}
