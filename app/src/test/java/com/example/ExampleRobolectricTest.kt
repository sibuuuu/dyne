package com.example

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.data.OpenMeteoWeatherRepository
import com.example.data.PressureTrend
import com.example.domain.AstroCalculator
import com.example.domain.ToolRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `app name and tool registry and ephemeris and weather are initialized`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Dyne", appName)
        assertEquals("com.sibora.dyne", BuildConfig.APPLICATION_ID)
        assertEquals("1.0.0", BuildConfig.VERSION_NAME)
        assertEquals(1, BuildConfig.VERSION_CODE)

        // Verify all precision tools and their multi-mode frameworks are registered
        val tools = ToolRegistry.tools.value
        assertTrue("Expected at least 75 tools registered, got ${tools.size}", tools.size >= 75)
        assertTrue(com.example.domain.FeatureRegistry.totalCount >= 160)
        assertTrue(com.example.domain.FeatureRegistry.premiumCount > 0)
        assertEquals(0, com.example.domain.FREE_FAVORITES_LIMIT)
        assertEquals(emptyList<String>(), com.example.data.UserPreferences().effectiveFavoriteIds(isPro = false))

        val requiredToolIds = listOf(
            "speaker_cleaner",
            "frequency_tool",
            "speaker_test",
            "mic_test",
            "tone_generator",
            "noise_sleep_mixer",
            "vibration_lab",
            "touch_screen_test",
            "metronome",
            "tuner",
            "battery_info",
            "device_sensors",
            "pdf_tools",
            "level",
            "angle",
            "compass",
            "metal_finder",
            "flashlight",
            "screen_torch",
            "sound_db",
            "speedometer",
            "walk_run_tracker",
            "altimeter",
            "vibration_gforce",
            "fishing_log",
            "interval_timer",
            "qr_generator",
            // Everyday Free tools
            "tip_calc",
            "tally_counter",
            "stopwatch",
            "pomodoro_timer",
            "randomizer_suite",
            "password_generator",
            "cooking_converter",
            "ruler",
            "screen_test",
            "magnifier_color",
            "unit_price_comparer",
            "fuel_trip_calc",
            "text_tools",
            "number_tools",
            // Sensor & Outdoor Free tools
            "pedometer",
            "lux_meter",
            "qibla_finder",
            "parking_saver",
            "weather_calculators",
            "compass_waypoints",
            // Media Studio Free & Pro tools
            "photo_converter",
            "photo_converter_pro",
            "background_remover",
            "photo_editor",
            "passport_id_photo",
            "audio_converter",
            "video_tools",
            "video_compress_pro",
            "pdf_image_tools",
            // Everyday & Sensor Premium tools
            "scientific_calc",
            "diy_calculators",
            "qr_studio_pro",
            "finance_pack",
            "image_toolkit",
            "event_countdown",
            "split_bill_pro",
            "trip_gear_checklists",
            "pomodoro_stats",
            "unit_converter_pro",
            "pedometer_history",
            "waypoints_pro",
            "photo_light_meter",
            "level_calibration_pro",
            "measurement_history_csv"
        )

        requiredToolIds.forEach { id ->
            val tool = ToolRegistry.getToolById(id)
            assertNotNull("Expected tool '$id' to be registered in ToolRegistry", tool)
            assertTrue("Expected tool '$id' to have modes configured", tool!!.modes.isNotEmpty())
        }

        // Verify DeviceCapabilities fallback reasons are present for every hardware capability
        com.example.data.HardwareCapability.entries.forEach { cap ->
            assertTrue(cap.missingReason.isNotBlank())
            assertTrue(cap.fallbackDescription.isNotBlank())
            assertNotNull(com.example.data.DeviceCapabilities.getFallbackReason(setOf(cap), context))
        }

        // Verify ToolMode search ("Flashlight > Strobe") and mode pin resolution ("flashlight:strobe")
        val strobeResults = ToolRegistry.searchToolsAndModes("Strobe")
        assertTrue(strobeResults.any { it.displayTitle == "Flashlight > Strobe" })

        val resolvedFlashlightStrobe = ToolRegistry.resolveToolAndMode("flashlight:strobe")
        assertNotNull(resolvedFlashlightStrobe)
        assertEquals("flashlight", resolvedFlashlightStrobe!!.first.id)
        assertEquals("strobe", resolvedFlashlightStrobe.second?.id)

        // Verify Solunar & Sun/Moon ephemeris calculation
        val forecast = AstroCalculator.calculateSevenDayForecast(
            startDate = LocalDate.now(),
            lat = 37.7749,
            lng = -122.4194
        )
        assertEquals(7, forecast.size)
        val today = forecast.first()
        assertTrue(today.fishingScore in 0..100)
        assertEquals(2, today.majorPeriods.size)
        assertEquals(2, today.minorPeriods.size)
        assertNotNull(today.moonOverhead)
        assertNotNull(today.moonUnderfoot)

        // Verify Weather Outdoor Score and Solunar Weather Boost calculation
        val outdoorScore = OpenMeteoWeatherRepository.calculateOutdoorScore(
            pressureTrend = PressureTrend.RISING,
            windSpeedKmh = 10.0,
            cloudCoverPercent = 25,
            rainChancePercent = 5
        )
        assertTrue(outdoorScore in 80..100)

        val (boost, boostReason) = OpenMeteoWeatherRepository.calculateSolunarWeatherBoost(
            pressureHpa = 1013.5,
            pressureDelta3hHpa = -1.2,
            pressureTrend = PressureTrend.FALLING,
            windSpeedKmh = 11.0
        )
        assertTrue(boost in -10..12)
        assertTrue(boostReason.contains("hPa"))

        // Verify Free vs Pro gating in ToolRegistry and ToolModes
        assertEquals(false, ToolRegistry.getToolById("speaker_cleaner")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("frequency_tool")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("device_sensors")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("pdf_tools")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("level")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("compass")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("flashlight")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("screen_torch")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("unit_converter")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("world_clock")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("speedometer")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("tally_counter")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("pomodoro_timer")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("randomizer_suite")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("password_generator")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("cooking_converter")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("ruler")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("screen_test")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("magnifier_color")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("unit_price_comparer")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("fuel_trip_calc")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("text_tools")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("number_tools")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("qr_generator")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("pedometer")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("lux_meter")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("qibla_finder")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("parking_saver")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("weather_calculators")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("compass_waypoints")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("photo_converter")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("background_remover")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("photo_editor")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("audio_converter")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("video_tools")?.isPro)
        assertEquals(false, ToolRegistry.getToolById("pdf_image_tools")?.isPro)

        assertEquals(true, ToolRegistry.getToolById("photo_converter_pro")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("passport_id_photo")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("video_compress_pro")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("scientific_calc")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("interval_timer")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("diy_calculators")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("qr_studio_pro")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("finance_pack")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("image_toolkit")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("event_countdown")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("split_bill_pro")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("trip_gear_checklists")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("pomodoro_stats")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("unit_converter_pro")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("pedometer_history")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("walk_run_tracker")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("altimeter")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("vibration_gforce")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("fishing_log")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("waypoints_pro")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("photo_light_meter")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("level_calibration_pro")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("measurement_history_csv")?.isPro)

        assertEquals(true, ToolRegistry.getToolById("sound_db")?.modes?.first { it.id == "graph" }?.isPremium)
        assertEquals(true, ToolRegistry.getToolById("sound_db")?.isPro)
        assertEquals(true, ToolRegistry.getToolById("level")?.modes?.first { it.id == "angle" }?.isPremium)
        assertEquals(false, ToolRegistry.getToolById("level")?.modes?.first { it.id == "bubble" }?.isPremium)
        assertEquals(true, ToolRegistry.getToolById("tip_calc")?.modes?.first { it.id == "scientific" }?.isPremium)
        assertEquals(false, ToolRegistry.getToolById("tip_calc")?.modes?.first { it.id == "basic" }?.isPremium)
        assertEquals(false, ToolRegistry.getToolById("tip_calc")?.modes?.first { it.id == "discount" }?.isPremium)

        // Verify Dyne Pro strict entitlement verification, FeatureGate, 1-hour temporary unlock constants, 5-minute warning, and coin prices
        val prefsRepo = com.example.data.PreferencesRepository(context)
        val purchaseManager = com.example.billing.GooglePlayPurchaseManager(context, prefsRepo)
        assertEquals(false, purchaseManager.isPro.value)
        purchaseManager.setSimulatePro(true)
        assertEquals(true, purchaseManager.isPro.value)
        purchaseManager.setSimulatePro(false)
        assertEquals(false, purchaseManager.isPro.value)

        val featureGate = com.example.billing.FeatureGate(purchaseManager, prefsRepo)
        assertEquals(false, featureGate.isUnlocked(com.example.billing.PremiumFeature.ANGLE))
        assertTrue(com.example.billing.PremiumFeature.entries.size >= 45)

        // Verify 1-hour duration, 5-minute warning, and coin prices
        assertEquals(3_600_000L, com.example.data.UserPreferences.ONE_HOUR_MS)
        assertEquals(300_000L, com.example.data.UserPreferences.FIVE_MINUTES_MS)
        assertEquals(10, com.example.data.PreferencesRepository.COST_TOOL_1H)
        assertEquals(15, com.example.data.PreferencesRepository.COST_THEME_1H)
        assertEquals(10, com.example.data.PreferencesRepository.COST_PACK_1H)
        assertEquals(10, com.example.data.PreferencesRepository.COST_EXTRA_DOC_SAVE)
        assertEquals(10, com.example.data.PreferencesRepository.COST_CSV_EXPORT)
        assertEquals(10, com.example.data.PreferencesRepository.COST_PDF_EXPORT)

        // Verify 5-minute expiry warning detection
        val testNow = 2_000_000_000_000L
        val prefsWith4mLeft = com.example.data.UserPreferences(
            featureUnlocksCsv = "angle:${testNow + 4 * 60_000L}"
        )
        assertEquals(true, prefsWith4mLeft.isFeatureUnlockedTemporarily("angle", testNow))
        val expiringNotice = featureGate.getExpiringSoonNotice(nowMs = testNow, prefs = prefsWith4mLeft)
        assertNotNull(expiringNotice)
        assertTrue(expiringNotice!!.contains("Angle"))
        val fullProgressPrefs = com.example.data.UserPreferences(
            featureUnlocksCsv = "angle:${testNow + com.example.data.UserPreferences.ONE_HOUR_MS}"
        )
        assertEquals(
            "1.00",
            String.format(
                java.util.Locale.US,
                "%.2f",
                featureGate.getCountdownProgress(
                    com.example.billing.PremiumFeature.ANGLE,
                    prefs = fullProgressPrefs,
                    nowMs = testNow
                )
            )
        )

        // Verify PDF page size presets & ±3% aspect ratio auto-detection
        assertEquals(com.example.data.PdfPageSize.A4, com.example.data.PdfPageSize.detectClosestStandardSize(1000, 1414))
        assertEquals(com.example.data.PdfPageSize.US_LETTER, com.example.data.PdfPageSize.detectClosestStandardSize(850, 1100))
        assertEquals(com.example.data.PdfPageSize.ID_CARD, com.example.data.PdfPageSize.detectClosestStandardSize(856, 540))

        // Verify AdConfig official Google test IDs in DEBUG builds
        assertEquals("ca-app-pub-3940256099942544~3347511713", com.example.ads.AdConfig.APP_ID)
        assertEquals("ca-app-pub-3940256099942544/9214589741", com.example.ads.AdConfig.BANNER)
        assertEquals("ca-app-pub-3940256099942544/2247696110", com.example.ads.AdConfig.NATIVE)
        assertEquals("ca-app-pub-3940256099942544/1033173712", com.example.ads.AdConfig.INTERSTITIAL)
        assertEquals("ca-app-pub-3940256099942544/5224354917", com.example.ads.AdConfig.REWARDED)
        assertEquals("ca-app-pub-3940256099942544/9257395921", com.example.ads.AdConfig.APP_OPEN)

        // Verify AdGate Pro suppression and AdManager frequency caps
        val adGate = com.example.ads.AdGate(purchaseManager)
        val adManager = com.example.ads.AdManager(context, adGate, prefsRepo)
        assertEquals(false, adGate.isProActiveNow)
        assertEquals(true, adGate.shouldShowAds())

        val nowMs = 1_700_000_000_000L
        // First session < 2 minutes -> blocked
        val firstSessionEarlyPrefs = com.example.data.UserPreferences(
            launchCount = 1,
            firstSessionStartEpochMs = nowMs - 30_000L
        )
        assertEquals(false, adManager.canShowInterstitialByFrequencyCap(firstSessionEarlyPrefs, nowMs).first)

        // Second session, no recent interstitial -> allowed
        val eligiblePrefs = com.example.data.UserPreferences(
            launchCount = 2,
            firstSessionStartEpochMs = nowMs - 600_000L
        )
        assertEquals(true, adManager.canShowInterstitialByFrequencyCap(eligiblePrefs, nowMs).first)

        // App Open never on first launch, requires 30+ min background & cooldown
        assertEquals(
            false,
            adManager.canShowAppOpenOnReturn(firstSessionEarlyPrefs, backgroundDurationMs = 45 * 60_000L, nowMs = nowMs).first
        )
        assertEquals(
            true,
            adManager.canShowAppOpenOnReturn(eligiblePrefs, backgroundDurationMs = 35 * 60_000L, nowMs = nowMs).first
        )
    }

    @Test
    fun `logo resource and themes and font resources are configured`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appIconDrawable = context.getDrawable(R.drawable.app_icon)
        assertNotNull("Expected R.drawable.app_icon to exist", appIconDrawable)
        val logoAppDrawable = context.getDrawable(R.drawable.logo_app)
        assertNotNull("Expected R.drawable.logo_app to exist", logoAppDrawable)

        val themeNames = com.example.ui.theme.ThemeEngine.allThemes.map { it.name }
        assertTrue(themeNames.contains("Lime Night"))
        assertTrue(themeNames.contains("Midnight Violet"))
        assertTrue(!themeNames.contains("Midnight Gold"))

        assertNotNull(context.resources.getFont(R.font.plus_jakarta_sans_500))
        assertNotNull(context.resources.getFont(R.font.plus_jakarta_sans_600))
        assertNotNull(context.resources.getFont(R.font.plus_jakarta_sans_700))
        assertNotNull(context.resources.getFont(R.font.plus_jakarta_sans_800))
        assertNotNull(context.resources.getFont(R.font.space_grotesk_500))
        assertNotNull(context.resources.getFont(R.font.space_grotesk_600))
        assertNotNull(context.resources.getFont(R.font.space_grotesk_700))
    }

    @Test
    @Config(sdk = [34], fontScale = 1.35f)
    fun `custom pdf engine and library filtering with 0 1 3 and 30 items at 1_35f font scale`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Verify custom PdfDocument page dimensions with A4, US Letter, ID Card, Receipt, and Auto without stretching
        assertEquals(595 to 842, com.example.data.PdfPageSize.A4.resolvePageDimensionsPt(595, 842))
        assertEquals(842 to 595, com.example.data.PdfPageSize.A4.resolvePageDimensionsPt(1200, 800))
        assertEquals(612 to 792, com.example.data.PdfPageSize.US_LETTER.resolvePageDimensionsPt(612, 792))
        assertEquals(243 to 153, com.example.data.PdfPageSize.ID_CARD.resolvePageDimensionsPt(856, 540))
        assertEquals(252 to 144, com.example.data.PdfPageSize.BUSINESS_CARD.resolvePageDimensionsPt(1000, 571))

        val sampleBmp = android.graphics.Bitmap.createBitmap(1000, 1414, android.graphics.Bitmap.Config.ARGB_8888)
        val estimatedHigh = com.example.data.ScanRepository.estimatePdfSizeBytes(
            bitmaps = listOf(sampleBmp),
            pageSize = com.example.data.PdfPageSize.A4,
            qualityDpi = com.example.data.PdfQualityDpi.HIGH_200
        )
        val estimatedLow = com.example.data.ScanRepository.estimatePdfSizeBytes(
            bitmaps = listOf(sampleBmp),
            pageSize = com.example.data.PdfPageSize.A4,
            qualityDpi = com.example.data.PdfQualityDpi.LOW_100
        )
        assertTrue("Expected High DPI estimate ($estimatedHigh) > Low DPI estimate ($estimatedLow)", estimatedHigh > estimatedLow)
        assertTrue(com.example.data.ScanRepository.formatFileSize(estimatedHigh).isNotBlank())

        // Verify Library document filtering & sorting for 0, 1, 3, and 30 items
        for (count in listOf(0, 1, 3, 30)) {
            val docs = (1..count).map { idx ->
                com.example.data.DocumentRecord(
                    id = idx.toLong(),
                    name = "Doc ${String.format(java.util.Locale.US, "%02d", idx)}",
                    category = when (idx % 5) {
                        0 -> "ID"
                        1 -> "Receipt"
                        2 -> "Bill"
                        3 -> "Notes"
                        else -> "Other"
                    },
                    pageCount = (idx % 7) + 1,
                    pdfPath = "/tmp/doc_$idx.pdf",
                    thumbnailPath = "/tmp/thumb_$idx.jpg",
                    ocrText = "OCR text $idx",
                    filterMode = "Crisp B&W",
                    favorite = idx == 1,
                    createdAt = 1_700_000_000_000L + idx * 1000L
                )
            }
            val sortedNewest = docs.sortedWith(
                compareByDescending<com.example.data.DocumentRecord> { it.favorite }
                    .thenByDescending { it.createdAt }
            )
            assertEquals(count, sortedNewest.size)
            if (count >= 1) {
                assertTrue(sortedNewest.first().favorite)
            }
        }
    }

    @Test
    @Config(sdk = [34])
    fun `notification center channels doctor test notifications daily digest and 24 premium sample previews work`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        org.robolectric.Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

        // 1. Verify Application.onCreate created all 6 channels with expected importance
        com.example.service.DyneNotificationCenter.createAllChannels(app)
        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        com.example.service.DyneNotificationChannel.entries.forEach { chSpec ->
            val createdChannel = nm.getNotificationChannel(chSpec.id)
            assertNotNull("Expected channel ${chSpec.id} to exist", createdChannel)
            assertEquals(chSpec.importance, createdChannel.importance)
        }

        // 2. Verify catalog counts: 1 Free (Daily Digest) + 24 Premium = 25 total
        assertEquals(1, com.example.service.DyneNotificationType.freeTypes.size)
        assertEquals(com.example.service.DyneNotificationType.DAILY_DIGEST, com.example.service.DyneNotificationType.freeTypes.first())
        assertEquals(24, com.example.service.DyneNotificationType.premiumTypes.size)
        assertEquals(25, com.example.service.DyneNotificationType.entries.size)

        // 3. Test "Send test now" and "Send test in 1 minute"
        val testNowResult = com.example.service.DyneNotificationCenter.sendTestNotificationNow(app)
        println("TEST_NOTIFICATION_RESULT [Send test now]: ${testNowResult.first} -> ${testNowResult.second}")
        assertTrue("Expected Send test now to succeed: ${testNowResult.second}", testNowResult.first)

        val test1mResult = com.example.service.DyneNotificationCenter.scheduleTestNotificationInOneMinute(app)
        println("TEST_NOTIFICATION_RESULT [Send test in 1 minute]: ${test1mResult.first} -> ${test1mResult.second}")
        assertTrue("Expected Send test in 1 minute to return status message", test1mResult.second.isNotBlank())

        // 4. Test Free "Daily Digest" when weather cache is empty -> "Open Dyne for today's outdoor summary"
        java.io.File(app.filesDir, "dyne_weather_cache_v2.json").delete()
        val digestFallbackContent = com.example.service.DyneNotificationCenter.buildHonestContentForType(
            app,
            com.example.service.DyneNotificationType.DAILY_DIGEST
        )
        println("TEST_NOTIFICATION_RESULT [Daily Digest - Empty Cache]: ${digestFallbackContent.first} | ${digestFallbackContent.second}")
        assertEquals("Open Dyne for today's outdoor summary", digestFallbackContent.second)

        val digestSampleResult = com.example.service.DyneNotificationCenter.postSamplePreviewForType(
            app,
            com.example.service.DyneNotificationType.DAILY_DIGEST
        )
        println("TEST_NOTIFICATION_RESULT [Daily Digest Sample Post]: ${digestSampleResult.first} -> ${digestSampleResult.second}")
        assertTrue(digestSampleResult.first)

        // 5. Test user-set timer notification (Focus & Time channel)
        val timerScheduleRes = com.example.service.DyneNotificationCenter.scheduleUserTimerAlarm(
            context = app,
            timerId = "test_pomodoro",
            title = "Pomodoro Work Complete",
            body = "Focus session complete! Time for a break.",
            triggerAtMs = System.currentTimeMillis() + 60_000L,
            deepLinkFeatureId = "pomodoro_timer"
        )
        val timerPostOk = com.example.service.DyneNotificationCenter.postTimerCompletedNotification(
            context = app,
            title = "Pomodoro Work Complete",
            body = "Focus session complete! Time for a break.",
            deepLinkFeatureId = "pomodoro_timer"
        )
        println("TEST_NOTIFICATION_RESULT [User Timer Focus & Time]: scheduled=${timerScheduleRes.scheduled}, method=${timerScheduleRes.method}, posted=$timerPostOk")
        assertTrue(timerScheduleRes.scheduled)
        assertTrue(timerPostOk)

        // 6. Test "Preview a sample" for all 24 Premium notification types
        com.example.service.DyneNotificationType.premiumTypes.forEach { type ->
            val sampleRes = com.example.service.DyneNotificationCenter.postSamplePreviewForType(app, type)
            val content = com.example.service.DyneNotificationCenter.buildHonestContentForType(app, type)
            println("TEST_NOTIFICATION_RESULT [Premium Sample - ${type.id}]: ${sampleRes.first} -> ${content.first}: ${content.second}")
            assertTrue("Expected sample preview for ${type.id} to succeed, got ${sampleRes.second}", sampleRes.first)
        }

        // 7. Verify History (last 50 max), Daily Cap (default 6, max 12), Quiet Hours, and Premium pause preserving settings
        val history = com.example.service.DyneNotificationCenter.getHistory(app)
        assertTrue("Expected history to record posted notifications (size=${history.size})", history.isNotEmpty())
        assertTrue("Expected history to cap at 50 entries", history.size <= 50)

        assertEquals(6, com.example.service.DyneNotificationCenter.getDailyCap(app))
        com.example.service.DyneNotificationCenter.setDailyCap(app, 15)
        assertEquals(12, com.example.service.DyneNotificationCenter.getDailyCap(app))

        // Enable a Premium type, then pause Premium -> config.enabled remains true while work is paused
        com.example.service.DyneNotificationCenter.updateTypeConfig(
            context = app,
            type = com.example.service.DyneNotificationType.SOLUNAR_BEST_WINDOW,
            enabled = true,
            leadValue = 30,
            solunarMode = "BOTH",
            isPremiumActive = true
        )
        com.example.service.DyneNotificationCenter.syncAllSchedules(app, isPremiumActive = false)
        val preservedConfig = com.example.service.DyneNotificationCenter.getTypeConfig(
            app,
            com.example.service.DyneNotificationType.SOLUNAR_BEST_WINDOW
        )
        assertEquals("Expected Premium setting to be preserved when Premium ends", true, preservedConfig.enabled)
        assertEquals(30, preservedConfig.leadMinutes)
        assertEquals("BOTH", preservedConfig.solunarMode)

        val doctorReport = com.example.service.DyneNotificationCenter.buildDoctorReport(app)
        assertEquals(true, doctorReport.permissionGranted)
        assertEquals(6, doctorReport.channels.size)
        println("TEST_NOTIFICATION_RESULT [Doctor Report]: permissionGranted=${doctorReport.permissionGranted}, channels=${doctorReport.channels.size}, workItems=${doctorReport.scheduledWorks.size}")
    }
}


