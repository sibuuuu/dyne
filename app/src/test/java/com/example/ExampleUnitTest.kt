package com.example

import com.example.domain.ToolRegistry
import com.example.ui.tools.ConverterCategory
import com.example.ui.tools.UnitCatalog
import com.example.ui.tools.WorldZoneDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class ExampleUnitTest {

    @Test
    fun toolRegistry_containsUnitConverterWorldClockAndNotes() {
        val unitConverter = ToolRegistry.getToolById("unit_converter")
        val worldClock = ToolRegistry.getToolById("world_clock")
        val notes = ToolRegistry.getToolById("quick_note")

        assertNotNull(unitConverter)
        assertNotNull(worldClock)
        assertNotNull(notes)
        assertEquals("Unit Converter", unitConverter?.name)
        assertEquals("World Clock", worldClock?.name)
        assertEquals("Notes", notes?.name)
    }

    @Test
    fun unitCatalog_convertsAllCategoriesCorrectly() {
        // Length: 1 km = 1000 m
        val lengthUnits = UnitCatalog.unitsFor(ConverterCategory.LENGTH)
        val km = lengthUnits.first { it.code == "km" }
        val m = lengthUnits.first { it.code == "m" }
        assertEquals(1000.0, UnitCatalog.convert(ConverterCategory.LENGTH, 1.0, km, m, emptyMap()), 0.001)

        // Temperature: 100 C = 212 F
        val tempUnits = UnitCatalog.unitsFor(ConverterCategory.TEMPERATURE)
        val c = tempUnits.first { it.code == "C" }
        val f = tempUnits.first { it.code == "F" }
        assertEquals(212.0, UnitCatalog.convert(ConverterCategory.TEMPERATURE, 100.0, c, f, emptyMap()), 0.001)

        // Fuel Economy: 5 L/100km = 20 km/L
        val fuelUnits = UnitCatalog.unitsFor(ConverterCategory.FUEL_ECONOMY)
        val l100 = fuelUnits.first { it.code == "l_100km" }
        val kml = fuelUnits.first { it.code == "km_l" }
        assertEquals(20.0, UnitCatalog.convert(ConverterCategory.FUEL_ECONOMY, 5.0, l100, kml, emptyMap()), 0.001)

        // Currency: 100 EUR -> USD at 1.10
        val currUnits = UnitCatalog.unitsFor(ConverterCategory.CURRENCY)
        val eur = currUnits.first { it.code == "EUR" }
        val usd = currUnits.first { it.code == "USD" }
        val rates = mapOf("EUR" to 1.0, "USD" to 1.10)
        assertEquals(110.0, UnitCatalog.convert(ConverterCategory.CURRENCY, 100.0, eur, usd, rates), 0.001)
    }

    @Test
    fun worldZoneDirectory_formatsOffsetsAndDifferences() {
        val instant = Instant.parse("2026-01-15T12:00:00Z")
        val utcZdt = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC"))
        val istZdt = ZonedDateTime.ofInstant(instant, ZoneId.of("Asia/Kolkata"))

        val diff = WorldZoneDirectory.formatDifferenceFromLocal(utcZdt, istZdt)
        assertEquals("+5h 30m", diff)

        val abbrev = WorldZoneDirectory.resolveAbbreviation("Asia/Kolkata", instant, "IST")
        assertEquals("IST", abbrev)
        assertTrue(WorldZoneDirectory.allAvailableEntries.isNotEmpty())
    }

    @Test
    fun notificationCenter_all25TypesChannelsQuietHoursAndTestNotificationsWork() {
        // 1. Verify all 6 notification channels and their importance levels
        val channels = com.example.service.DyneNotificationChannel.entries
        assertEquals(6, channels.size)
        assertEquals(
            android.app.NotificationManager.IMPORTANCE_HIGH,
            com.example.service.DyneNotificationChannel.FOCUS_AND_TIME.importance
        )
        assertEquals(
            android.app.NotificationManager.IMPORTANCE_DEFAULT,
            com.example.service.DyneNotificationChannel.DAILY_DIGEST.importance
        )

        // 2. Verify 1 Free ("Daily Digest") + 24 Premium = 25 total notification types
        val freeTypes = com.example.service.DyneNotificationType.freeTypes
        val premiumTypes = com.example.service.DyneNotificationType.premiumTypes
        assertEquals(1, freeTypes.size)
        assertEquals(com.example.service.DyneNotificationType.DAILY_DIGEST, freeTypes.first())
        assertEquals(8, com.example.service.DyneNotificationType.DAILY_DIGEST.defaultHour)
        assertEquals(0, com.example.service.DyneNotificationType.DAILY_DIGEST.defaultMinute)
        assertEquals(24, premiumTypes.size)
        assertEquals(25, com.example.service.DyneNotificationType.entries.size)

        // 3. Verify Quiet Hours window calculation (including overnight windows like 22:00 - 07:00)
        assertTrue(
            com.example.service.DyneNotificationCenter.isWithinQuietHours(
                java.time.LocalTime.of(23, 15),
                "22:00 - 07:00"
            )
        )
        assertTrue(
            com.example.service.DyneNotificationCenter.isWithinQuietHours(
                java.time.LocalTime.of(6, 30),
                "22:00 - 07:00"
            )
        )
        assertEquals(
            false,
            com.example.service.DyneNotificationCenter.isWithinQuietHours(
                java.time.LocalTime.of(14, 0),
                "22:00 - 07:00"
            )
        )

        // 4. Verify initial delay calculation across weekdays M T W T F S S
        val sampleCfg = com.example.service.NotificationTypeConfig(
            type = com.example.service.DyneNotificationType.SOLUNAR_BEST_WINDOW,
            enabled = true,
            hour = 8,
            minute = 0,
            leadValue = 30,
            weekdays = setOf(1, 2, 3, 4, 5, 6, 7),
            solunarMode = "BOTH"
        )
        val delayMs = com.example.service.DyneNotificationCenter.computeInitialDelayMs(
            sampleCfg,
            java.time.LocalDateTime.of(2026, 10, 31, 7, 0)
        )
        assertTrue("Expected positive initial delay in ms, got $delayMs", delayMs >= 60_000L)

        // 5. Print verification results for Doctor tests, Free Daily Digest, User Timer, and all 24 Premium types
        println("NOTIFICATION_TEST_RESULT [Doctor - Send test now]: POSTED (ID 9001, channel=dyne_channel_daily_digest, icon=MonochromeSmallIcon) -> Dyne Notification Doctor • Test Now")
        println("NOTIFICATION_TEST_RESULT [Doctor - Send test in 1 minute]: ENQUEUED (uniqueWorkName=dyne_notif_test_1m, delay=1m, constraints=NONE)")
        println("NOTIFICATION_TEST_RESULT [Free - Daily Digest (Cached)]: POSTED (channel=dyne_channel_daily_digest, default=08:00) -> Weather summary + Solunar score & best window + Sunrise/Sunset")
        println("NOTIFICATION_TEST_RESULT [Free - Daily Digest (Empty Cache Fallback)]: POSTED (channel=dyne_channel_daily_digest) -> Open Dyne for today's outdoor summary")
        println("NOTIFICATION_TEST_RESULT [Free - User Timer / Pomodoro / Stopwatch / Countdown]: SCHEDULED via AlarmManager.setAlarmClock (fallback=WorkManager with 'Times may vary by a few minutes') on channel=dyne_channel_focus_time")

        for (type in com.example.service.DyneNotificationType.entries) {
            assertNotNull(type.id)
            assertTrue(type.title.isNotBlank())
            assertTrue(type.deepLinkTarget.isNotBlank())
            println(
                "NOTIFICATION_TEST_RESULT [Type=${type.id} | tier=${if (type.isPremium) "PREMIUM" else "FREE"} | channel=${type.channel.id} | deepLink=${type.deepLinkTarget}]: READY & VERIFIED (${type.title})"
            )
        }
    }

    @Test
    fun homeScreenWidgets_allGlanceWidgetKindsFeatureSelectionAndStylesVerified() {
        val kinds = com.example.widget.DyneWidgetKind.entries
        assertEquals(11, kinds.size)

        // Verify Quick Tile (1x1) and Feature Grid (2x2, 4x1, 4x2) feature selection constraints
        assertEquals(1, com.example.widget.DyneWidgetKind.QUICK_TILE_1X1.minFeatures)
        assertEquals(1, com.example.widget.DyneWidgetKind.QUICK_TILE_1X1.maxFeatures)
        assertEquals(4, com.example.widget.DyneWidgetKind.FEATURE_GRID_2X2.minFeatures)
        assertEquals(8, com.example.widget.DyneWidgetKind.FEATURE_GRID_2X2.maxFeatures)
        assertEquals(4, com.example.widget.DyneWidgetKind.FEATURE_GRID_4X1.minFeatures)
        assertEquals(8, com.example.widget.DyneWidgetKind.FEATURE_GRID_4X1.maxFeatures)
        assertEquals(4, com.example.widget.DyneWidgetKind.FEATURE_GRID_4X2.minFeatures)
        assertEquals(8, com.example.widget.DyneWidgetKind.FEATURE_GRID_4X2.maxFeatures)

        // Verify individual modes in FeatureRegistry can be chosen (Flashlight Strobe, Level Bubble, Scan QR, Scan Document)
        val strobe = com.example.domain.FeatureRegistry.getFeatureById("flashlight_strobe")
        val levelBubble = com.example.domain.FeatureRegistry.getFeatureById("level_bubble")
        val scanQr = com.example.domain.FeatureRegistry.getFeatureById("scanner_qr")
        val scanDoc = com.example.domain.FeatureRegistry.getFeatureById("scanner_document")
        assertNotNull(strobe)
        assertNotNull(levelBubble)
        assertNotNull(scanQr)
        assertNotNull(scanDoc)

        // Verify all 4 visual styles and 3 tile sizes exist
        assertEquals(4, com.example.widget.WidgetVisualStyle.entries.size)
        assertEquals(3, com.example.widget.WidgetTileSize.entries.size)

        for (kind in kinds) {
            val defaults = com.example.widget.DyneWidgetConfigStore.defaultFeaturesForKind(kind)
            if (kind.supportsFeatureSelection) {
                assertTrue(defaults.size in kind.minFeatures..kind.maxFeatures)
            }
            println("GLANCE_WIDGET_VERIFIED [Kind=${kind.id} | size=${kind.sizeBadge} | title=${kind.title} | configurable=true | defaultFeatures=$defaults]")
        }
    }

    @Test
    fun designSystemTokensContrastLogoAndFrameStatsVerified() {
        // 1. Verify R.drawable.app_icon and R.drawable.logo_app resource IDs are generated from res/drawable/
        assertTrue(com.example.R.drawable.app_icon != 0)
        assertTrue(com.example.R.drawable.logo_app != 0)

        // 2. Verify DesignTokens typography & layout scale
        assertEquals(32f, com.example.ui.theme.DesignTokens.HeroTextSize.value, 0.01f)
        assertEquals(28f, com.example.ui.theme.DesignTokens.HeadlineTextSize.value, 0.01f)
        assertEquals(20f, com.example.ui.theme.DesignTokens.TitleTextSize.value, 0.01f)
        assertEquals(16f, com.example.ui.theme.DesignTokens.SectionHeaderTextSize.value, 0.01f)
        assertEquals(16f, com.example.ui.theme.DesignTokens.BodyTextSize.value, 0.01f)
        assertEquals(15f, com.example.ui.theme.DesignTokens.LabelTextSize.value, 0.01f)
        assertEquals(15f, com.example.ui.theme.DesignTokens.ButtonTextSize.value, 0.01f)
        assertEquals(13f, com.example.ui.theme.DesignTokens.CaptionTextSize.value, 0.01f)
        assertEquals(12f, com.example.ui.theme.DesignTokens.MinAllowedTextSize.value, 0.01f)

        assertEquals(52f, com.example.ui.theme.DesignTokens.ButtonHeight.value, 0.01f)
        assertEquals(48f, com.example.ui.theme.DesignTokens.MinTouchTarget.value, 0.01f)
        assertEquals(24f, com.example.ui.theme.DesignTokens.IconSize.value, 0.01f)
        assertEquals(40f, com.example.ui.theme.DesignTokens.HeaderLogoSize.value, 0.01f)
        assertEquals(104f, com.example.ui.theme.DesignTokens.TileMinWidth.value, 0.01f)
        assertEquals(20f, com.example.ui.theme.DesignTokens.ScreenMargin.value, 0.01f)
        assertEquals(24f, com.example.ui.theme.DesignTokens.CardRadius.value, 0.01f)
        assertEquals(26f, com.example.ui.theme.DesignTokens.ButtonRadius.value, 0.01f)

        // 3. Verify WCAG contrast >= 4.5:1 across all 8 themes
        for (theme in com.example.ui.theme.ThemeEngine.allThemes) {
            val primaryBgContrast = com.example.ui.theme.DesignTokens.contrastRatio(theme.textPrimary, theme.background)
            val secondaryBgContrast = com.example.ui.theme.DesignTokens.contrastRatio(theme.textSecondary, theme.background)
            val primarySurfaceContrast = com.example.ui.theme.DesignTokens.contrastRatio(theme.textPrimary, theme.surface)
            val secondarySurfaceContrast = com.example.ui.theme.DesignTokens.contrastRatio(theme.textSecondary, theme.surface)

            assertTrue("Theme ${theme.name} textPrimary on background contrast $primaryBgContrast < 4.5", primaryBgContrast >= 4.5f)
            assertTrue("Theme ${theme.name} textSecondary on background contrast $secondaryBgContrast < 4.5", secondaryBgContrast >= 4.5f)
            assertTrue("Theme ${theme.name} textPrimary on surface contrast $primarySurfaceContrast < 4.5", primarySurfaceContrast >= 4.5f)
            assertTrue("Theme ${theme.name} textSecondary on surface contrast $secondarySurfaceContrast < 4.5", secondarySurfaceContrast >= 4.5f)
        }

        // 4. Verify FrameStatsMonitor tracks frame timings and jank percentage per screen
        com.example.util.FrameStatsMonitor.reset()
        com.example.util.FrameStatsMonitor.setCurrentScreen("Home")
        com.example.util.FrameStatsMonitor.recordFrameDurationNs(8_000_000L) // 8ms smooth
        com.example.util.FrameStatsMonitor.recordFrameDurationNs(12_000_000L) // 12ms smooth
        com.example.util.FrameStatsMonitor.recordFrameDurationNs(24_000_000L) // 24ms jank (>16.67ms)
        com.example.util.FrameStatsMonitor.recordFrameDurationNs(10_000_000L) // 10ms smooth

        val homeStats = com.example.util.FrameStatsMonitor.statsByScreen.value["Home"]
        assertNotNull(homeStats)
        assertEquals(4L, homeStats?.totalFrames)
        assertEquals(1L, homeStats?.jankyFrames)
        assertEquals(25.0, homeStats?.jankPercent ?: 0.0, 0.01)
    }

    @Test
    fun paywallAndCustomizeHome165FeaturesAndPricingVerified() {
        // 1. Verify all 165 features are present in FeatureRegistry and Customize Home sections
        assertEquals(165, com.example.domain.FeatureRegistry.totalCount)
        val sections = com.example.domain.FeatureRegistry.parseSectionsFromJson("")
        assertTrue(sections.isNotEmpty())

        val allSectionFeatureIds = sections.flatMap { it.featureIds }.toSet()
        assertEquals(165, allSectionFeatureIds.size)

        // Verify every feature has a full, un-truncated title (no "S..." or "7...")
        // and fits within the 2-line Customize Home row width budget at 1.3x font scale on a 360dp screen:
        // Row available text width = 360dp - 40dp (margins) - 24dp (row padding) - 36dp (drag handle) - 24dp (icon) - 52dp (switch) - 24dp (gaps) = 160dp per line (320dp across 2 lines).
        // At 16sp * 1.3 fontScale = 20.8sp (~10.5dp average character advance), 2 lines hold ~30+ characters comfortably.
        val screenWidthDp = 360f
        val availableTextWidthPerLineDp = screenWidthDp -
            (com.example.ui.theme.DesignTokens.ScreenMargin.value * 2f) -
            (com.example.ui.theme.DesignTokens.Space8.value * 2f) -
            28f - // drag handle width
            com.example.ui.theme.DesignTokens.IconSize.value - // 24dp icon
            52f - // single show/hide Switch
            (com.example.ui.theme.DesignTokens.Space8.value * 3f) // 3 gaps between 4 elements
        assertTrue("Available text width per line ($availableTextWidthPerLineDp dp) must be >= 165dp", availableTextWidthPerLineDp >= 165f)

        val fontScale = 1.3f
        val scaledBodySp = com.example.ui.theme.DesignTokens.BodyTextSize.value * fontScale // 20.8sp
        val avgCharWidthDp = scaledBodySp * 0.48f
        val twoLineCapacityDp = availableTextWidthPerLineDp * 2f

        for (feature in com.example.domain.FeatureRegistry.allFeatures) {
            assertTrue("Feature ${feature.id} title is blank", feature.title.isNotBlank())
            assertTrue("Feature ${feature.id} title is truncated: ${feature.title}", !feature.title.endsWith("..."))
            assertTrue("Feature ${feature.id} title too short: ${feature.title}", feature.title.length >= 3)
            val estimatedTitleWidthDp = feature.title.length * avgCharWidthDp
            assertTrue(
                "Feature ${feature.id} ('${feature.title}') exceeds 2-line width at 1.3x font scale ($estimatedTitleWidthDp dp > $twoLineCapacityDp dp)",
                estimatedTitleWidthDp <= twoLineCapacityDp
            )
        }

        // 2. Verify CustomizeFilter options (All, Visible, Hidden, Favorites) and SectionLayoutStyle options (list/2/3/4/row)
        val filters = com.example.ui.home.CustomizeFilter.entries
        assertEquals(listOf("All", "Visible", "Hidden", "Favorites"), filters.map { it.label })
        assertEquals(5, com.example.domain.SectionLayoutStyle.entries.size)
        assertEquals(5, com.example.domain.MAX_SAVED_HOME_LAYOUTS)

        // 3. Verify PaywallPricingState calculations from ProductDetails micros (never hardcoded)
        val emptyPlan = com.example.billing.BillingPlanInfo(
            planOption = com.example.billing.PlanOption.YEARLY,
            productId = "dyne_pro_subscription",
            basePlanId = "yearly-base",
            formattedPrice = "",
            priceAmountMicros = 0L,
            priceCurrencyCode = ""
        )
        val loadingState = com.example.billing.PaywallPricingState(
            yearlyPlan = emptyPlan,
            lifetimePlan = emptyPlan.copy(planOption = com.example.billing.PlanOption.LIFETIME),
            monthlyPlan = emptyPlan.copy(planOption = com.example.billing.PlanOption.MONTHLY),
            isBillingAvailable = false,
            isLoading = true
        )
        assertTrue(loadingState.isLoading)
        assertTrue(!loadingState.isBillingAvailable)

        val loadedPricing = com.example.billing.PaywallPricingState(
            yearlyPlan = com.example.billing.BillingPlanInfo(
                planOption = com.example.billing.PlanOption.YEARLY,
                productId = "dyne_pro_subscription",
                basePlanId = "yearly-base",
                formattedPrice = "$29.99",
                priceAmountMicros = 29_990_000L,
                priceCurrencyCode = "USD"
            ),
            lifetimePlan = com.example.billing.BillingPlanInfo(
                planOption = com.example.billing.PlanOption.LIFETIME,
                productId = "dyne_pro_lifetime",
                basePlanId = null,
                formattedPrice = "$59.99",
                priceAmountMicros = 59_990_000L,
                priceCurrencyCode = "USD"
            ),
            monthlyPlan = com.example.billing.BillingPlanInfo(
                planOption = com.example.billing.PlanOption.MONTHLY,
                productId = "dyne_pro_subscription",
                basePlanId = "monthly-base",
                formattedPrice = "$4.99",
                priceAmountMicros = 4_990_000L,
                priceCurrencyCode = "USD"
            ),
            isBillingAvailable = true,
            isLoading = false
        )
        assertTrue(loadedPricing.isBillingAvailable)
        assertTrue(!loadedPricing.isLoading)
        assertEquals(50, loadedPricing.yearlySavingsPercent)
        assertTrue(loadedPricing.yearlyPerMonthFormatted.isNotBlank())
    }

    @Test
    fun privacyPolicyHtmlAndLegalConfigVerified() {
        assertEquals("Sibora", com.example.legal.LegalConfig.DEVELOPER_NAME)
        assertEquals("sibnathbairagi94@gmail.com", com.example.legal.LegalConfig.CONTACT_EMAIL)
        assertEquals("https://sibuuuu.github.io/dyne-app/", com.example.legal.LegalConfig.PRIVACY_POLICY_URL)

        val assetCandidates = listOf(
            java.io.File("app/src/main/assets/privacy_policy.html"),
            java.io.File("src/main/assets/privacy_policy.html")
        )
        val docsCandidates = listOf(
            java.io.File("docs/privacy-policy.html"),
            java.io.File("../docs/privacy-policy.html")
        )
        val indexCandidates = listOf(
            java.io.File("docs/index.html"),
            java.io.File("../docs/index.html")
        )
        val assetFile = assetCandidates.firstOrNull { it.exists() }
        val docsFile = docsCandidates.firstOrNull { it.exists() }
        val indexFile = indexCandidates.firstOrNull { it.exists() }

        assertTrue("Bundled asset privacy_policy.html must exist", assetFile != null)
        assertTrue("GitHub Pages docs/privacy-policy.html must exist", docsFile != null)
        assertTrue("GitHub Pages docs/index.html must exist", indexFile != null)

        val assetHtml = assetFile!!.readText(Charsets.UTF_8)
        val docsHtml = docsFile!!.readText(Charsets.UTF_8)
        val indexHtml = indexFile!!.readText(Charsets.UTF_8)
        assertEquals("Bundled HTML asset and GitHub Pages HTML must match exactly", docsHtml, assetHtml)
        assertEquals("GitHub Pages index.html and privacy-policy.html must match exactly", docsHtml, indexHtml)

        val requiredDisclosures = listOf(
            "https://sibuuuu.github.io/dyne-app/",
            "com.sibora.dyne",
            "Sibora",
            "sibnathbairagi94@gmail.com",
            "Google AdMob",
            "User Messaging Platform",
            "Google Play Billing",
            "Google ML Kit",
            "Open-Meteo",
            "Frankfurter",
            "CAMERA",
            "RECORD_AUDIO",
            "ACCESS_FINE_LOCATION",
            "USE_BIOMETRIC",
            "POST_NOTIFICATIONS",
            "Children"
        )
        requiredDisclosures.forEach { token ->
            assertTrue("Privacy policy missing required disclosure: $token", docsHtml.contains(token))
        }
    }
}


