package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "dyne_prefs")

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class LaunchAnimationMode(val label: String) {
    RANDOM("Random"),
    COLOR_ONLY("Color only"),
    BLACK_AND_WHITE_ONLY("Black & White only"),
    MINIMAL("Minimal")
}

enum class UnitSystem {
    METRIC, IMPERIAL
}

enum class HapticStrength(val label: String, val durationMs: Long, val amplitude: Int) {
    LIGHT("Light", 10L, 65),
    MEDIUM("Medium", 18L, 140),
    STRONG("Strong", 28L, 240)
}

data class UserPreferences(
    // Appearance
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val selectedThemeName: String = "Lime Night",
    val launchAnimationMode: LaunchAnimationMode = LaunchAnimationMode.RANDOM,
    val lastSplashCombo: String = "",
    val selectedAppIconStyle: String = "Orange",
    val textScale: Float = 1.0f,
    val reduceGlassEffects: Boolean = false,
    val reduceMotion: Boolean = false,

    // Sounds & Haptics
    val tapSoundsEnabled: Boolean = true,
    val tapSoundVolume: Float = 0.80f,
    val scanBeepEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val hapticStrength: HapticStrength = HapticStrength.MEDIUM,

    // Customize Home
    val selectedHomePreset: String = "",
    val homePresetDismissed: Boolean = false,
    val enabledToolIdsCsv: String = "",
    val orderedToolIdsCsv: String = "",
    val orderedSectionsCsv: String = "MEDIA_STUDIO,PHONE_CARE_AND_AUDIO,MEASURE,LIGHT_AND_SOUND,TRAVEL_AND_TIME,CONVERT_AND_WRITE",
    val outdoorsSegmentsCsv: String = "SOLUNAR:true,SUN_MOON:true,WEATHER:true",
    val scanMenuShortcutsCsv: String = "scan_doc,scan_qr,quick_note,flashlight",
    val favoriteToolIdsCsv: String = "",
    val recentToolIdsCsv: String = "",
    val homeBlockOrderCsv: String = "HERO,FAVORITES,RECENTLY_USED,SKY_NOW,DYNE_PREMIUM,CATEGORIES,QUICK_ACCESS",
    val disabledHomeBlocksCsv: String = "",
    val homeTileSize: String = "M",
    val homeLayoutColumns: String = "3",
    val sectionLayoutOverridesCsv: String = "",
    val showToolNames: Boolean = true,
    val outdoorInterestsCsv: String = "FISHING,HUNTING,CAMPING_PHOTO",

    // Units & Formats
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val distanceUnit: String = "km / m",
    val speedUnit: String = "km/h",
    val temperatureUnit: String = "°C",
    val pressureUnit: String = "hPa",
    val use24HourTime: Boolean = true,
    val defaultTimeZone: String = "System Default",
    val defaultCurrency: String = "USD",

    // Privacy & Security
    val lockDocumentsWithBiometrics: Boolean = false,

    // Pro & Location
    val cachedIsPro: Boolean = false,
    val cachedProSource: String = "None",
    val simulateProEnabled: Boolean = false,
    val launchCount: Int = 0,
    val thirdLaunchPaywallDismissed: Boolean = false,
    val manualLat: Double = 37.7749,
    val manualLng: Double = -122.4194,
    val useManualLocation: Boolean = false,
    val locationName: String = "Local Coordinates",

    // AdMob Frequency Caps & 1-Hour Temporary Unlocks
    val firstSessionStartEpochMs: Long = 0L,
    val lastInterstitialShownEpochMs: Long = 0L,
    val interstitialDailyDate: String = "",
    val interstitialDailyCount: Int = 0,
    val lastAppOpenShownEpochMs: Long = 0L,
    val featureUnlocksCsv: String = "",
    val rewardedToolUnlocksCsv: String = "",
    val rewardedThemeUnlocksCsv: String = "",
    val bonusDocumentSaves: Int = 0,
    val bonusCsvExports: Int = 0,

    // Coins & Daily Check-in Streak (DataStore balance + Room ledger)
    val coinBalance: Int = 0,
    val lastAdCoinEpochMs: Long = 0L,
    val adCoinDailyDate: String = "",
    val adCoinDailyCount: Int = 0,
    val lastCheckInDate: String = "",
    val checkInStreak: Int = 0,

    // Sound Packs, Frequency Presets & One-Time Audio Safety Notices
    val ownedSoundPacksCsv: String = "",
    val frequencyPresetsCsv: String = "",
    val audioHearingSafetyTipDismissed: Boolean = false,
    val frequencySafetyCardDismissed: Boolean = false,
    val lastToolModesCsv: String = "",
    val customToolModesConfigCsv: String = "",

    // Media Studio Preferences & Daily Quotas
    val mediaSaveLocationTipDismissed: Boolean = false,
    val bgRemoverDailyDate: String = "",
    val bgRemoverDailyCount: Int = 0,
    val bgRemoverBonusCredits: Int = 0,
    val passportPhotoFreeTryUsed: Boolean = false,

    // Notifications (Solunar alerts, expiry reminders, daily bonus; each with a switch and quiet hours)
    val solunarAlertsEnabled: Boolean = true,
    val solunarQuietHoursEnabled: Boolean = true,
    val solunarQuietHours: String = "22:00 - 06:00",
    val expiryRemindersEnabled: Boolean = true,
    val expiryQuietHoursEnabled: Boolean = true,
    val expiryQuietHours: String = "22:00 - 07:00",
    val dailyBonusNotificationsEnabled: Boolean = true,
    val dailyBonusQuietHoursEnabled: Boolean = true,
    val dailyBonusQuietHours: String = "21:00 - 08:00",

    // Privacy & Security (Premium App Lock, Recents Preview FLAG_SECURE, Per-Feature Locks)
    val appLockEnabled: Boolean = false,
    val appLockMethod: String = "BIOMETRIC_OR_PIN",
    val appLockOnLaunch: Boolean = true,
    val appLockTimeoutMinutes: Int = 5,
    val hideAppPreviewInRecents: Boolean = false,
    val lockNotesWithBiometrics: Boolean = false,

    // Premium Customization (Custom Accent, Tile Style, Hero Carousel Style, Icon Set)
    val customAccentHex: String = "",
    val homeTileStyle: String = "GLASS", // GLASS, SOLID, OUTLINE
    val heroCarouselStyle: String = "EDITORIAL", // EDITORIAL, COMPACT_PILL, MINIMAL_SPLIT
    val iconSetStyle: String = "FILLED", // FILLED, OUTLINED, DUOTONE, MINIMAL

    // Premium WorkManager Local Alerts (Solunar mode/lead time, Golden hour, Daily weather, Rain-soon)
    val solunarAlertMode: String = "FISHING", // FISHING, HUNTING
    val solunarAlertLeadMinutes: Int = 30, // 15, 30, 60
    val goldenHourAlertsEnabled: Boolean = false,
    val dailyWeatherSummaryEnabled: Boolean = false,
    val rainSoonAlertsEnabled: Boolean = false,

    // Remove Ads Product Cache (from Google Play Billing "dyne_remove_ads")
    val cachedRemoveAds: Boolean = false,

    // QuotaManager Daily Allowances (resets at local midnight) & Bonus +1 Credits
    val quotaDailyDate: String = "",
    val quotaCountsCsv: String = "",
    val quotaBonusCsv: String = "",

    // Daily Premium Spotlight (once a day one random Premium tool is free for 1h, ad-free during it; 7-day streak gives +1h)
    val spotlightDailyDate: String = "",
    val spotlightToolId: String = "",
    val spotlightStartEpochMs: Long = 0L,
    val spotlightExpiryEpochMs: Long = 0L,

    // Smart Upsell (at most once every 3 days, 7-day pause on "Maybe later", tracks 5th scan)
    val lastSmartUpsellShownEpochMs: Long = 0L,
    val smartUpsellPausedUntilEpochMs: Long = 0L,
    val completedScanCount: Int = 0,

    // Value Meter in Settings > Dyne Premium ("This week you used N Premium features with ads or coins")
    val weeklyAdOrCoinUsesCsv: String = "",

    // Premium Feature Catalog & Home Layout Customization
    val customSectionsJson: String = "",
    val hiddenFeatureIdsCsv: String = "",
    val perTileSizeCsv: String = "",
    val pinnedTopFeatureIdsCsv: String = "",
    val startTabIndex: Int = 0,
    val homeFirstFeatureId: String = "",
    val savedHomeLayoutsJson: String = "",
    val customLayoutSavedBannerDismissed: Boolean = false,
    val photosensitivityWarningShown: Boolean = false,

    // AccessManager Per-Key Unlock Records & Clock-Rollback Baseline
    val accessUnlockRecordsSerialized: String = "",
    val lastObservedWallMs: Long = 0L,
    val lastObservedElapsedMs: Long = 0L,
    val clockRollbackInvalidated: Boolean = false,
    val hadActiveLookUnlockSession: Boolean = false,

    // Pending Customization Store (written under temporary 1-hour "customize" unlock; never applied after expiry; promoted if Premium is bought)
    val pendingHomeTileSize: String = "",
    val pendingPerTileSizeCsv: String = "",
    val pendingCustomSectionsJson: String = "",
    val pendingHiddenFeatureIdsCsv: String = "",
    val pendingFavoriteToolIdsCsv: String = "",
    val pendingPinnedTopFeatureIdsCsv: String = "",
    val pendingStartTabIndex: Int = -1,
    val pendingHomeFirstFeatureId: String = "",
    val pendingCustomAccentHex: String = "",
    val pendingHomeTileStyle: String = "",
    val pendingHeroCarouselStyle: String = "",
    val pendingIconSetStyle: String = "",
    val pendingHomeBlockOrderCsv: String = "",
    val pendingDisabledHomeBlocksCsv: String = "",
    val pendingHomeLayoutColumns: String = "",
    val pendingSectionLayoutOverridesCsv: String = ""
) {
    val hiddenFeatureIdsSet: Set<String>
        get() = hiddenFeatureIdsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    val pendingHiddenFeatureIdsSet: Set<String>
        get() = pendingHiddenFeatureIdsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    val pinnedTopFeatureIdsList: List<String>
        get() = pinnedTopFeatureIdsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    val hasCustomHomeConfiguration: Boolean
        get() = customSectionsJson.isNotBlank() ||
            hiddenFeatureIdsCsv.isNotBlank() ||
            perTileSizeCsv.isNotBlank() ||
            pinnedTopFeatureIdsCsv.isNotBlank() ||
            homeFirstFeatureId.isNotBlank() ||
            savedHomeLayoutsJson.isNotBlank() ||
            disabledHomeBlocksCsv.isNotBlank()

    val scanShortcutsList: List<String>
        get() = scanMenuShortcutsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(4)
            .ifEmpty { listOf("scan_doc", "scan_qr", "quick_note", "flashlight") }

    val orderedSectionsList: List<String>
        get() {
            val defaultSections = listOf(
                "MEDIA_STUDIO",
                "PHONE_CARE_AND_AUDIO",
                "MEASURE",
                "LIGHT_AND_SOUND",
                "TRAVEL_AND_TIME",
                "CONVERT_AND_WRITE"
            )
            val parsed = orderedSectionsCsv.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .toMutableList()
            defaultSections.forEach { sec ->
                if (sec !in parsed) parsed.add(sec)
            }
            return parsed
        }

    val favoriteToolIdsList: List<String>
        get() = favoriteToolIdsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    val recentToolIdsList: List<String>
        get() = recentToolIdsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    val homeBlockOrderList: List<String>
        get() {
            val defaultBlocks = listOf(
                "HERO", "FAVORITES", "RECENTLY_USED", "SKY_NOW", "DYNE_PREMIUM", "CATEGORIES", "QUICK_ACCESS"
            )
            val parsed = homeBlockOrderCsv.split(",")
                .map { it.trim().uppercase() }
                .filter { it in defaultBlocks }
                .distinct()
                .toMutableList()
            defaultBlocks.forEach { b -> if (b !in parsed) parsed.add(b) }
            return parsed
        }

    val disabledHomeBlocksSet: Set<String>
        get() = disabledHomeBlocksCsv.split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }
            .toSet()

    val outdoorInterestsSet: Set<String>
        get() = outdoorInterestsCsv.split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }
            .toSet()

    val ownedSoundPacksSet: Set<String>
        get() = ownedSoundPacksCsv.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()

    val sectionLayoutOverridesMap: Map<String, Int>
        get() {
            if (sectionLayoutOverridesCsv.isBlank()) return emptyMap()
            val map = mutableMapOf<String, Int>()
            sectionLayoutOverridesCsv.split(",").forEach { token ->
                val parts = token.split(":")
                if (parts.size >= 2) {
                    val key = parts[0].trim().uppercase()
                    val colVal = parts[1].trim().toIntOrNull()
                    if (key.isNotEmpty() && colVal != null) {
                        map[key] = colVal
                    }
                }
            }
            return map
        }

    val homeLayoutColumnsInt: Int
        get() = homeLayoutColumns.trim().toIntOrNull() ?: 0

    fun getSectionLayoutOverride(sectionKey: String): Pair<String, String>? {
        val target = sectionKey.trim().uppercase()
        sectionLayoutOverridesCsv.split(",").forEach { token ->
            val parts = token.split(":")
            if (parts.size >= 3 && parts[0].trim().uppercase() == target) {
                return parts[1].trim().uppercase() to parts[2].trim().uppercase()
            }
        }
        return null
    }

    fun isFeatureUnlockedTemporarily(
        featureKey: String,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): Boolean {
        return com.example.billing.AccessManager.hasAccess(
            featureKey = featureKey,
            isPremium = false,
            prefs = this,
            nowWallMs = nowMs,
            nowElapsedMs = nowElapsedMs
        )
    }

    fun getFeatureUnlockExpiryMs(
        featureKey: String,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): Long? {
        return com.example.billing.AccessManager.getUnlockExpiryWallMs(
            featureKey = featureKey,
            prefs = this,
            nowWallMs = nowMs,
            nowElapsedMs = nowElapsedMs
        )
    }

    fun isToolUnlockedByReward(
        toolId: String,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): Boolean {
        return com.example.billing.AccessManager.hasAccess(
            featureKey = toolId,
            isPremium = false,
            prefs = this,
            nowWallMs = nowMs,
            nowElapsedMs = nowElapsedMs
        )
    }

    fun isThemeUnlockedByReward(
        themeName: String,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): Boolean {
        return com.example.billing.AccessManager.hasAccess(
            featureKey = com.example.billing.AccessManager.themeKey(themeName),
            isPremium = false,
            prefs = this,
            nowWallMs = nowMs,
            nowElapsedMs = nowElapsedMs
        )
    }

    fun getToolRewardExpiryMs(
        toolId: String,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): Long? {
        return com.example.billing.AccessManager.getUnlockExpiryWallMs(toolId, this, nowMs, nowElapsedMs)
    }

    fun getThemeRewardExpiryMs(
        themeName: String,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): Long? {
        return com.example.billing.AccessManager.getUnlockExpiryWallMs(
            com.example.billing.AccessManager.themeKey(themeName),
            this,
            nowMs,
            nowElapsedMs
        )
    }

    fun getAllActiveTemporaryUnlocks(
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): Map<String, Long> {
        return com.example.billing.AccessManager.getAllActiveUnlocks(this, nowMs, nowElapsedMs)
    }

    /**
     * Returns the nearest future expiry timestamp (ms) across all active 1-hour unlocks,
     * or null if no temporary unlocks are currently active.
     */
    fun nearestTemporaryUnlockExpiryMs(
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): Long? {
        val candidates = mutableListOf<Long>()
        getAllActiveTemporaryUnlocks(nowMs, nowElapsedMs).values.forEach { exp ->
            if (exp > nowMs) candidates.add(exp)
        }
        return candidates.minOrNull()
    }

    /**
     * True if the user currently has access to Premium customization (either via Dyne Premium
     * or an active 1-hour temporary unlock on key "customize"). Computed dynamically on every read.
     */
    fun isCustomizationAccessActive(
        isPro: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): Boolean {
        val entitledPro = isPro || cachedIsPro || simulateProEnabled
        return com.example.billing.AccessManager.hasAccess(
            featureKey = com.example.billing.AccessManager.KEY_CUSTOMIZE,
            isPremium = entitledPro,
            prefs = this,
            nowWallMs = nowMs,
            nowElapsedMs = nowElapsedMs
        )
    }

    /**
     * True if the user currently has access to [themeName] (either via Dyne Premium,
     * default "Lime Night", or an active 1-hour unlock on key "theme:<id>").
     */
    fun isThemeAccessActive(
        themeName: String,
        isPro: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): Boolean {
        val entitledPro = isPro || cachedIsPro || simulateProEnabled
        if (entitledPro) return true
        if (themeName.equals("Lime Night", ignoreCase = true)) return true
        return com.example.billing.AccessManager.hasAccess(
            featureKey = com.example.billing.AccessManager.themeKey(themeName),
            isPremium = false,
            prefs = this,
            nowWallMs = nowMs,
            nowElapsedMs = nowElapsedMs
        )
    }

    fun isThemeAccessActive(
        isPro: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): Boolean {
        val entitledPro = isPro || cachedIsPro || simulateProEnabled
        if (entitledPro) return true
        val activeTempTheme = com.example.billing.AccessManager.getActiveTemporaryThemeName(this, nowMs, nowElapsedMs)
        if (activeTempTheme != null) return true
        return isThemeAccessActive(selectedThemeName, false, nowMs, nowElapsedMs)
    }

    /**
     * Effective Theme computed on every read from `(selectedTheme, hasAccess)`:
     * - For entitled Premium users (`isPro`): returns [selectedThemeName].
     * - For Free users with an active 1-hour `theme:<id>` unlock: returns that unlocked theme.
     * - Otherwise: returns `"Lime Night"`.
     */
    fun effectiveThemeName(
        isPro: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): String {
        val entitledPro = isPro || cachedIsPro || simulateProEnabled
        if (entitledPro) return selectedThemeName
        val activeTempTheme = com.example.billing.AccessManager.getActiveTemporaryThemeName(this, nowMs, nowElapsedMs)
        if (activeTempTheme != null) return activeTempTheme
        return if (isThemeAccessActive(selectedThemeName, false, nowMs, nowElapsedMs)) {
            selectedThemeName
        } else {
            "Lime Night"
        }
    }

    /**
     * Effective Custom Accent Hex: permanent value for Pro, pending value during 1h "customize" unlock, otherwise "".
     */
    fun effectiveCustomAccentHex(
        isPro: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): String {
        val entitledPro = isPro || cachedIsPro || simulateProEnabled
        return when {
            entitledPro -> customAccentHex.ifBlank { pendingCustomAccentHex }
            isCustomizationAccessActive(false, nowMs, nowElapsedMs) -> pendingCustomAccentHex.ifBlank { customAccentHex }
            else -> ""
        }
    }

    /**
     * Effective Tile Style: permanent value for Pro, pending value during 1h "customize" unlock, otherwise "GLASS".
     */
    fun effectiveHomeTileStyle(
        isPro: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): String {
        val entitledPro = isPro || cachedIsPro || simulateProEnabled
        return when {
            entitledPro -> homeTileStyle.ifBlank { pendingHomeTileStyle }.ifBlank { "GLASS" }
            isCustomizationAccessActive(false, nowMs, nowElapsedMs) -> pendingHomeTileStyle.ifBlank { homeTileStyle }.ifBlank { "GLASS" }
            else -> "GLASS"
        }
    }

    /**
     * Effective Hero Carousel Style: permanent value for Pro, pending value during 1h "customize" unlock, otherwise "EDITORIAL".
     */
    fun effectiveHeroCarouselStyle(
        isPro: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): String {
        val entitledPro = isPro || cachedIsPro || simulateProEnabled
        return when {
            entitledPro -> heroCarouselStyle.ifBlank { pendingHeroCarouselStyle }.ifBlank { "EDITORIAL" }
            isCustomizationAccessActive(false, nowMs, nowElapsedMs) -> pendingHeroCarouselStyle.ifBlank { heroCarouselStyle }.ifBlank { "EDITORIAL" }
            else -> "EDITORIAL"
        }
    }

    /**
     * Effective Icon Set Style: permanent value for Pro, pending value during 1h "customize" unlock, otherwise "FILLED".
     */
    fun effectiveIconSetStyle(
        isPro: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): String {
        val entitledPro = isPro || cachedIsPro || simulateProEnabled
        return when {
            entitledPro -> iconSetStyle.ifBlank { pendingIconSetStyle }.ifBlank { "FILLED" }
            isCustomizationAccessActive(false, nowMs, nowElapsedMs) -> pendingIconSetStyle.ifBlank { iconSetStyle }.ifBlank { "FILLED" }
            else -> "FILLED"
        }
    }

    /**
     * Effective Home Tile Size: permanent value for Pro, pending value during 1h "customize" unlock, otherwise "M".
     */
    fun effectiveHomeTileSize(
        isPro: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): String {
        val entitledPro = isPro || cachedIsPro || simulateProEnabled
        return when {
            entitledPro -> if (pendingHomeTileSize.isNotBlank() && homeTileSize == "M") pendingHomeTileSize else homeTileSize
            isCustomizationAccessActive(false, nowMs, nowElapsedMs) -> pendingHomeTileSize.ifBlank { homeTileSize }
            else -> "M"
        }
    }

    /**
     * Effective Per-Tile Size CSV: permanent value for Pro, pending value during 1h "customize" unlock, otherwise "".
     */
    fun effectivePerTileSizeCsv(
        isPro: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): String {
        val entitledPro = isPro || cachedIsPro || simulateProEnabled
        return when {
            entitledPro -> perTileSizeCsv.ifBlank { pendingPerTileSizeCsv }
            isCustomizationAccessActive(false, nowMs, nowElapsedMs) -> pendingPerTileSizeCsv.ifBlank { perTileSizeCsv }
            else -> ""
        }
    }

    /**
     * Effective Custom Sections JSON: permanent value for Pro, pending value during 1h "customize" unlock, otherwise "".
     */
    fun effectiveCustomSectionsJson(
        isPro: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): String {
        val entitledPro = isPro || cachedIsPro || simulateProEnabled
        return when {
            entitledPro -> customSectionsJson.ifBlank { pendingCustomSectionsJson }
            isCustomizationAccessActive(false, nowMs, nowElapsedMs) -> pendingCustomSectionsJson.ifBlank { customSectionsJson }
            else -> ""
        }
    }

    /**
     * Effective Hidden Features Set: permanent value for Pro, pending value during 1h "customize" unlock, otherwise emptySet().
     */
    fun effectiveHiddenFeatureIdsSet(
        isPro: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): Set<String> {
        val entitledPro = isPro || cachedIsPro || simulateProEnabled
        return when {
            entitledPro -> if (hiddenFeatureIdsSet.isNotEmpty()) hiddenFeatureIdsSet else pendingHiddenFeatureIdsSet
            isCustomizationAccessActive(false, nowMs, nowElapsedMs) ->
                if (pendingHiddenFeatureIdsCsv.isNotBlank()) pendingHiddenFeatureIdsSet else hiddenFeatureIdsSet
            else -> emptySet()
        }
    }

    /**
     * Effective Favorites List: permanent value for Pro, pending value during 1h "customize" unlock, otherwise emptyList().
     */
    fun effectiveFavoriteIds(
        isPro: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): List<String> {
        val entitledPro = isPro || cachedIsPro || simulateProEnabled
        val legacyDefault = "speaker_cleaner,frequency_tool,flashlight,compass"
        val cleanSaved = if (favoriteToolIdsCsv == legacyDefault) {
            emptyList()
        } else {
            favoriteToolIdsList
        }
        val pendingFavs = pendingFavoriteToolIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        return when {
            entitledPro -> if (cleanSaved.isNotEmpty()) cleanSaved else pendingFavs
            isCustomizationAccessActive(false, nowMs, nowElapsedMs) -> if (pendingFavs.isNotEmpty()) pendingFavs else cleanSaved
            else -> emptyList()
        }
    }

    /**
     * Effective Saved Home Layouts JSON: permanent value for Pro, or during 1h "customize" unlock, otherwise "".
     */
    fun effectiveSavedHomeLayoutsJson(
        isPro: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): String {
        val entitledPro = isPro || cachedIsPro || simulateProEnabled
        return when {
            entitledPro -> savedHomeLayoutsJson
            isCustomizationAccessActive(false, nowMs, nowElapsedMs) -> savedHomeLayoutsJson
            else -> ""
        }
    }

    /**
     * Effective Home Block Order List: permanent value for Pro, pending value during 1h "customize" unlock, otherwise default order.
     */
    fun effectiveHomeBlockOrderList(
        isPro: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): List<String> {
        val defaultBlocks = listOf(
            "HERO", "FAVORITES", "RECENTLY_USED", "SKY_NOW", "DYNE_PREMIUM", "CATEGORIES", "QUICK_ACCESS"
        )
        val entitledPro = isPro || cachedIsPro || simulateProEnabled
        val rawCsv = when {
            entitledPro -> if (pendingHomeBlockOrderCsv.isNotBlank() && homeBlockOrderCsv.isBlank()) pendingHomeBlockOrderCsv else homeBlockOrderCsv
            isCustomizationAccessActive(false, nowMs, nowElapsedMs) -> pendingHomeBlockOrderCsv.ifBlank { homeBlockOrderCsv }
            else -> ""
        }
        if (rawCsv.isBlank()) return defaultBlocks
        val parsed = rawCsv.split(",")
            .map { it.trim().uppercase() }
            .filter { it in defaultBlocks }
            .distinct()
            .toMutableList()
        defaultBlocks.forEach { b -> if (b !in parsed) parsed.add(b) }
        return parsed
    }

    /**
     * Effective Pinned Top Feature IDs: permanent value for Pro, pending value during 1h "customize" unlock, otherwise emptyList().
     */
    fun effectivePinnedTopFeatureIdsList(
        isPro: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): List<String> {
        val entitledPro = isPro || cachedIsPro || simulateProEnabled
        val pendingList = pendingPinnedTopFeatureIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        return when {
            entitledPro -> if (pinnedTopFeatureIdsList.isNotEmpty()) pinnedTopFeatureIdsList else pendingList
            isCustomizationAccessActive(false, nowMs, nowElapsedMs) -> if (pendingList.isNotEmpty()) pendingList else pinnedTopFeatureIdsList
            else -> emptyList()
        }
    }

    /**
     * Effective Home First Feature ID: permanent value for Pro, pending value during 1h "customize" unlock, otherwise "".
     */
    fun effectiveHomeFirstFeatureId(
        isPro: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): String {
        val entitledPro = isPro || cachedIsPro || simulateProEnabled
        return when {
            entitledPro -> homeFirstFeatureId.ifBlank { pendingHomeFirstFeatureId }
            isCustomizationAccessActive(false, nowMs, nowElapsedMs) -> pendingHomeFirstFeatureId.ifBlank { homeFirstFeatureId }
            else -> ""
        }
    }

    /**
     * Effective Start Tab Index: permanent value for Pro, pending value during 1h "customize" unlock, otherwise 0 (Home).
     */
    fun effectiveStartTabIndex(
        isPro: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): Int {
        val entitledPro = isPro || cachedIsPro || simulateProEnabled
        return when {
            entitledPro -> (if (startTabIndex != 0) startTabIndex else if (pendingStartTabIndex >= 0) pendingStartTabIndex else 0).coerceIn(0, 3)
            isCustomizationAccessActive(false, nowMs, nowElapsedMs) ->
                (if (pendingStartTabIndex >= 0) pendingStartTabIndex else startTabIndex).coerceIn(0, 3)
            else -> 0
        }
    }

    fun getLastModeForTool(toolId: String): String? {
        val cleanId = toolId.trim().lowercase()
        if (lastToolModesCsv.isBlank()) return null
        lastToolModesCsv.split(",").forEach { token ->
            val parts = token.split(":")
            if (parts.size >= 2 && parts[0].trim().lowercase() == cleanId) {
                return parts[1].trim()
            }
        }
        return null
    }

    fun isModePinned(toolId: String, modeId: String): Boolean {
        val target = "${toolId.trim()}:${modeId.trim()}".lowercase()
        return favoriteToolIdsList.any { it.lowercase() == target }
    }

    fun isSpotlightActiveNow(nowMs: Long = System.currentTimeMillis()): Boolean {
        val todayStr = java.time.LocalDate.now().toString()
        return spotlightDailyDate == todayStr &&
            spotlightToolId.isNotBlank() &&
            spotlightExpiryEpochMs > nowMs
    }

    fun isToolSpotlightUnlocked(toolId: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!isSpotlightActiveNow(nowMs)) return false
        return spotlightToolId.equals(toolId.trim(), ignoreCase = true)
    }

    fun getWeeklyAdOrCoinUsesCount(nowMs: Long = System.currentTimeMillis()): Int {
        if (weeklyAdOrCoinUsesCsv.isBlank()) return 0
        val sevenDaysAgo = nowMs - (7L * 24L * 60L * 60L * 1000L)
        return weeklyAdOrCoinUsesCsv.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .count { it >= sevenDaysAgo }
    }

    fun getQuotaUsedCountToday(quotaKey: String, todayDate: String = java.time.LocalDate.now().toString()): Int {
        val keyUpper = quotaKey.trim().uppercase()
        if (keyUpper == "BACKGROUND_REMOVER" && bgRemoverDailyDate == todayDate && quotaDailyDate != todayDate) {
            return bgRemoverDailyCount.coerceAtLeast(0)
        }
        if (quotaDailyDate != todayDate) return 0
        return parseIntMap(quotaCountsCsv)[keyUpper] ?: 0
    }

    fun getQuotaBonusCount(quotaKey: String): Int {
        val keyUpper = quotaKey.trim().uppercase()
        val fromMap = parseIntMap(quotaBonusCsv)[keyUpper] ?: 0
        return if (keyUpper == "BACKGROUND_REMOVER") {
            maxOf(fromMap, bgRemoverBonusCredits.coerceAtLeast(0))
        } else {
            fromMap
        }
    }

    fun canShowSmartUpsell(nowMs: Long = System.currentTimeMillis(), shownInCurrentSession: Boolean = false): Boolean {
        if (shownInCurrentSession) return false
        if (cachedIsPro || simulateProEnabled) return false
        if (smartUpsellPausedUntilEpochMs > nowMs) return false
        val threeDaysMs = 3L * 24L * 60L * 60L * 1000L
        if (lastSmartUpsellShownEpochMs > 0L && (nowMs - lastSmartUpsellShownEpochMs) < threeDaysMs) return false
        return true
    }

    companion object {
        const val ONE_HOUR_MS: Long = 60L * 60L * 1000L
        const val FIVE_MINUTES_MS: Long = 5L * 60L * 1000L
        const val AD_COIN_COOLDOWN_MS: Long = 60_000L
        const val AD_COIN_MAX_PER_DAY: Int = 12

        fun parseUnlockMap(csv: String): Map<String, Long> {
            if (csv.isBlank()) return emptyMap()
            return csv.split(",")
                .mapNotNull { entry ->
                    val sepIdx = entry.lastIndexOf(':')
                    if (sepIdx > 0 && sepIdx < entry.length - 1) {
                        val key = entry.substring(0, sepIdx).trim().lowercase()
                        val exp = entry.substring(sepIdx + 1).trim().toLongOrNull()
                        if (key.isNotEmpty() && exp != null) key to exp else null
                    } else null
                }
                .toMap()
        }

        fun parseIntMap(csv: String): Map<String, Int> {
            if (csv.isBlank()) return emptyMap()
            return csv.split(",")
                .mapNotNull { entry ->
                    val sepIdx = entry.lastIndexOf(':')
                    if (sepIdx > 0 && sepIdx < entry.length - 1) {
                        val key = entry.substring(0, sepIdx).trim().uppercase()
                        val v = entry.substring(sepIdx + 1).trim().toIntOrNull()
                        if (key.isNotEmpty() && v != null) key to v.coerceAtLeast(0) else null
                    } else null
                }
                .toMap()
        }
    }
}

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SELECTED_THEME_NAME = stringPreferencesKey("selected_theme_name")
        val LAUNCH_ANIMATION_MODE = stringPreferencesKey("launch_animation_mode")
        val LAST_SPLASH_COMBO = stringPreferencesKey("last_splash_combo")
        val SELECTED_APP_ICON_STYLE = stringPreferencesKey("selected_app_icon_style")
        val TEXT_SCALE = floatPreferencesKey("text_scale")
        val REDUCE_GLASS = booleanPreferencesKey("reduce_glass_effects")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")

        val TAP_SOUNDS_ENABLED = booleanPreferencesKey("tap_sounds_enabled")
        val TAP_SOUND_VOLUME = floatPreferencesKey("tap_sound_volume")
        val SCAN_BEEP_ENABLED = booleanPreferencesKey("scan_beep_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val HAPTIC_STRENGTH = stringPreferencesKey("haptic_strength")

        val SELECTED_HOME_PRESET = stringPreferencesKey("selected_home_preset")
        val HOME_PRESET_DISMISSED = booleanPreferencesKey("home_preset_dismissed")
        val ENABLED_TOOL_IDS_CSV = stringPreferencesKey("enabled_tool_ids_csv")
        val ORDERED_TOOL_IDS_CSV = stringPreferencesKey("ordered_tool_ids_csv")
        val ORDERED_SECTIONS_CSV = stringPreferencesKey("ordered_sections_csv")
        val OUTDOORS_SEGMENTS_CSV = stringPreferencesKey("outdoors_segments_csv")
        val SCAN_SHORTCUTS_CSV = stringPreferencesKey("scan_shortcuts_csv")
        val FAVORITE_TOOL_IDS_CSV = stringPreferencesKey("favorite_tool_ids_csv")
        val RECENT_TOOL_IDS_CSV = stringPreferencesKey("recent_tool_ids_csv")
        val HOME_BLOCK_ORDER_CSV = stringPreferencesKey("home_block_order_csv")
        val DISABLED_HOME_BLOCKS_CSV = stringPreferencesKey("disabled_home_blocks_csv")
        val HOME_TILE_SIZE = stringPreferencesKey("home_tile_size")
        val HOME_LAYOUT_COLUMNS = stringPreferencesKey("home_layout_columns")
        val SECTION_LAYOUT_OVERRIDES_CSV = stringPreferencesKey("section_layout_overrides_csv")
        val SHOW_TOOL_NAMES = booleanPreferencesKey("show_tool_names")
        val OUTDOOR_INTERESTS_CSV = stringPreferencesKey("outdoor_interests_csv")

        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
        val SPEED_UNIT = stringPreferencesKey("speed_unit")
        val TEMPERATURE_UNIT = stringPreferencesKey("temperature_unit")
        val PRESSURE_UNIT = stringPreferencesKey("pressure_unit")
        val USE_24_HOUR = booleanPreferencesKey("use_24_hour")
        val DEFAULT_TIME_ZONE = stringPreferencesKey("default_time_zone")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")

        val LOCK_DOCS_BIOMETRICS = booleanPreferencesKey("lock_docs_biometrics")

        val CACHED_IS_PRO = booleanPreferencesKey("cached_is_pro")
        val CACHED_PRO_SOURCE = stringPreferencesKey("cached_pro_source")
        val SIMULATE_PRO_ENABLED = booleanPreferencesKey("simulate_pro_enabled")
        val LAUNCH_COUNT = intPreferencesKey("launch_count")
        val THIRD_LAUNCH_DISMISSED = booleanPreferencesKey("third_launch_dismissed")
        val MANUAL_LAT = doublePreferencesKey("manual_lat")
        val MANUAL_LNG = doublePreferencesKey("manual_lng")
        val USE_MANUAL_LOC = booleanPreferencesKey("use_manual_loc")
        val LOCATION_NAME = stringPreferencesKey("location_name")

        val FIRST_SESSION_START_MS = longPreferencesKey("first_session_start_ms")
        val LAST_INTERSTITIAL_MS = longPreferencesKey("last_interstitial_ms")
        val INTERSTITIAL_DAILY_DATE = stringPreferencesKey("interstitial_daily_date")
        val INTERSTITIAL_DAILY_COUNT = intPreferencesKey("interstitial_daily_count")
        val LAST_APP_OPEN_MS = longPreferencesKey("last_app_open_ms")
        val FEATURE_UNLOCKS_CSV = stringPreferencesKey("feature_unlocks_csv")
        val REWARDED_TOOL_UNLOCKS_CSV = stringPreferencesKey("rewarded_tool_unlocks_csv")
        val REWARDED_THEME_UNLOCKS_CSV = stringPreferencesKey("rewarded_theme_unlocks_csv")
        val BONUS_DOCUMENT_SAVES = intPreferencesKey("bonus_document_saves")
        val BONUS_CSV_EXPORTS = intPreferencesKey("bonus_csv_exports")

        val COIN_BALANCE = intPreferencesKey("coin_balance")
        val LAST_AD_COIN_MS = longPreferencesKey("last_ad_coin_ms")
        val AD_COIN_DAILY_DATE = stringPreferencesKey("ad_coin_daily_date")
        val AD_COIN_DAILY_COUNT = intPreferencesKey("ad_coin_daily_count")
        val LAST_CHECK_IN_DATE = stringPreferencesKey("last_check_in_date")
        val CHECK_IN_STREAK = intPreferencesKey("check_in_streak")

        val OWNED_SOUND_PACKS_CSV = stringPreferencesKey("owned_sound_packs_csv")
        val FREQUENCY_PRESETS_CSV = stringPreferencesKey("frequency_presets_csv")
        val AUDIO_HEARING_SAFETY_DISMISSED = booleanPreferencesKey("audio_hearing_safety_dismissed")
        val FREQUENCY_SAFETY_CARD_DISMISSED = booleanPreferencesKey("frequency_safety_card_dismissed")
        val LAST_TOOL_MODES_CSV = stringPreferencesKey("last_tool_modes_csv")
        val CUSTOM_TOOL_MODES_CONFIG_CSV = stringPreferencesKey("custom_tool_modes_config_csv")

        val MEDIA_SAVE_LOCATION_TIP_DISMISSED = booleanPreferencesKey("media_save_location_tip_dismissed")
        val BG_REMOVER_DAILY_DATE = stringPreferencesKey("bg_remover_daily_date")
        val BG_REMOVER_DAILY_COUNT = intPreferencesKey("bg_remover_daily_count")
        val BG_REMOVER_BONUS_CREDITS = intPreferencesKey("bg_remover_bonus_credits")
        val PASSPORT_PHOTO_FREE_TRY_USED = booleanPreferencesKey("passport_photo_free_try_used")

        val SOLUNAR_ALERTS_ENABLED = booleanPreferencesKey("solunar_alerts_enabled")
        val SOLUNAR_QUIET_HOURS_ENABLED = booleanPreferencesKey("solunar_quiet_hours_enabled")
        val SOLUNAR_QUIET_HOURS = stringPreferencesKey("solunar_quiet_hours")
        val EXPIRY_REMINDERS_ENABLED = booleanPreferencesKey("expiry_reminders_enabled")
        val EXPIRY_QUIET_HOURS_ENABLED = booleanPreferencesKey("expiry_quiet_hours_enabled")
        val EXPIRY_QUIET_HOURS = stringPreferencesKey("expiry_quiet_hours")
        val DAILY_BONUS_NOTIFS_ENABLED = booleanPreferencesKey("daily_bonus_notifs_enabled")
        val DAILY_BONUS_QUIET_HOURS_ENABLED = booleanPreferencesKey("daily_bonus_quiet_hours_enabled")
        val DAILY_BONUS_QUIET_HOURS = stringPreferencesKey("daily_bonus_quiet_hours")

        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_METHOD = stringPreferencesKey("app_lock_method")
        val APP_LOCK_ON_LAUNCH = booleanPreferencesKey("app_lock_on_launch")
        val APP_LOCK_TIMEOUT_MINUTES = intPreferencesKey("app_lock_timeout_minutes")
        val HIDE_APP_PREVIEW_RECENTS = booleanPreferencesKey("hide_app_preview_recents")
        val LOCK_NOTES_BIOMETRICS = booleanPreferencesKey("lock_notes_biometrics")

        val CUSTOM_ACCENT_HEX = stringPreferencesKey("custom_accent_hex")
        val HOME_TILE_STYLE = stringPreferencesKey("home_tile_style")
        val HERO_CAROUSEL_STYLE = stringPreferencesKey("hero_carousel_style")
        val ICON_SET_STYLE = stringPreferencesKey("icon_set_style")

        val SOLUNAR_ALERT_MODE = stringPreferencesKey("solunar_alert_mode")
        val SOLUNAR_ALERT_LEAD_MINS = intPreferencesKey("solunar_alert_lead_mins")
        val GOLDEN_HOUR_ALERTS_ENABLED = booleanPreferencesKey("golden_hour_alerts_enabled")
        val DAILY_WEATHER_SUMMARY_ENABLED = booleanPreferencesKey("daily_weather_summary_enabled")
        val RAIN_SOON_ALERTS_ENABLED = booleanPreferencesKey("rain_soon_alerts_enabled")

        val CACHED_REMOVE_ADS = booleanPreferencesKey("cached_remove_ads")
        val QUOTA_DAILY_DATE = stringPreferencesKey("quota_daily_date")
        val QUOTA_COUNTS_CSV = stringPreferencesKey("quota_counts_csv")
        val QUOTA_BONUS_CSV = stringPreferencesKey("quota_bonus_csv")
        val SPOTLIGHT_DAILY_DATE = stringPreferencesKey("spotlight_daily_date")
        val SPOTLIGHT_TOOL_ID = stringPreferencesKey("spotlight_tool_id")
        val SPOTLIGHT_START_MS = longPreferencesKey("spotlight_start_ms")
        val SPOTLIGHT_EXPIRY_MS = longPreferencesKey("spotlight_expiry_ms")
        val LAST_SMART_UPSELL_MS = longPreferencesKey("last_smart_upsell_ms")
        val SMART_UPSELL_PAUSED_UNTIL_MS = longPreferencesKey("smart_upsell_paused_until_ms")
        val COMPLETED_SCAN_COUNT = intPreferencesKey("completed_scan_count")
        val WEEKLY_AD_COIN_USES_CSV = stringPreferencesKey("weekly_ad_coin_uses_csv")
        val CUSTOM_SECTIONS_JSON = stringPreferencesKey("custom_sections_json")
        val HIDDEN_FEATURE_IDS_CSV = stringPreferencesKey("hidden_feature_ids_csv")
        val PER_TILE_SIZE_CSV = stringPreferencesKey("per_tile_size_csv")
        val PINNED_TOP_FEATURE_IDS_CSV = stringPreferencesKey("pinned_top_feature_ids_csv")
        val START_TAB_INDEX = intPreferencesKey("start_tab_index")
        val HOME_FIRST_FEATURE_ID = stringPreferencesKey("home_first_feature_id")
        val SAVED_HOME_LAYOUTS_JSON = stringPreferencesKey("saved_home_layouts_json")
        val CUSTOM_LAYOUT_SAVED_BANNER_DISMISSED = booleanPreferencesKey("custom_layout_saved_banner_dismissed")
        val PHOTOSENSITIVITY_WARNING_SHOWN = booleanPreferencesKey("photosensitivity_warning_shown")

        val ACCESS_UNLOCK_RECORDS = stringPreferencesKey("access_unlock_records_serialized")
        val LAST_OBSERVED_WALL_MS = longPreferencesKey("last_observed_wall_ms")
        val LAST_OBSERVED_ELAPSED_MS = longPreferencesKey("last_observed_elapsed_ms")
        val CLOCK_ROLLBACK_INVALIDATED = booleanPreferencesKey("clock_rollback_invalidated")
        val HAD_ACTIVE_LOOK_UNLOCK_SESSION = booleanPreferencesKey("had_active_look_unlock_session")

        val PENDING_HOME_TILE_SIZE = stringPreferencesKey("pending_home_tile_size")
        val PENDING_PER_TILE_SIZE_CSV = stringPreferencesKey("pending_per_tile_size_csv")
        val PENDING_CUSTOM_SECTIONS_JSON = stringPreferencesKey("pending_custom_sections_json")
        val PENDING_HIDDEN_FEATURE_IDS_CSV = stringPreferencesKey("pending_hidden_feature_ids_csv")
        val PENDING_FAVORITE_TOOL_IDS_CSV = stringPreferencesKey("pending_favorite_tool_ids_csv")
        val PENDING_PINNED_TOP_FEATURE_IDS_CSV = stringPreferencesKey("pending_pinned_top_feature_ids_csv")
        val PENDING_START_TAB_INDEX = intPreferencesKey("pending_start_tab_index")
        val PENDING_HOME_FIRST_FEATURE_ID = stringPreferencesKey("pending_home_first_feature_id")
        val PENDING_CUSTOM_ACCENT_HEX = stringPreferencesKey("pending_custom_accent_hex")
        val PENDING_HOME_TILE_STYLE = stringPreferencesKey("pending_home_tile_style")
        val PENDING_HERO_CAROUSEL_STYLE = stringPreferencesKey("pending_hero_carousel_style")
        val PENDING_ICON_SET_STYLE = stringPreferencesKey("pending_icon_set_style")
        val PENDING_HOME_BLOCK_ORDER_CSV = stringPreferencesKey("pending_home_block_order_csv")
        val PENDING_DISABLED_HOME_BLOCKS_CSV = stringPreferencesKey("pending_disabled_home_blocks_csv")
        val PENDING_HOME_LAYOUT_COLUMNS = stringPreferencesKey("pending_home_layout_columns")
        val PENDING_SECTION_LAYOUT_OVERRIDES_CSV = stringPreferencesKey("pending_section_layout_overrides_csv")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val launchAnimStr = prefs[Keys.LAUNCH_ANIMATION_MODE] ?: LaunchAnimationMode.RANDOM.name
        val unitStr = prefs[Keys.UNIT_SYSTEM] ?: UnitSystem.METRIC.name
        val strengthStr = prefs[Keys.HAPTIC_STRENGTH] ?: HapticStrength.MEDIUM.name
        val unitSys = runCatching { UnitSystem.valueOf(unitStr) }.getOrDefault(UnitSystem.METRIC)

        UserPreferences(
            themeMode = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.SYSTEM),
            selectedThemeName = prefs[Keys.SELECTED_THEME_NAME] ?: "Lime Night",
            launchAnimationMode = runCatching { LaunchAnimationMode.valueOf(launchAnimStr) }
                .getOrDefault(LaunchAnimationMode.RANDOM),
            lastSplashCombo = prefs[Keys.LAST_SPLASH_COMBO] ?: "",
            selectedAppIconStyle = prefs[Keys.SELECTED_APP_ICON_STYLE] ?: "Orange",
            textScale = (prefs[Keys.TEXT_SCALE] ?: 1.0f).coerceIn(0.85f, 1.25f),
            reduceGlassEffects = prefs[Keys.REDUCE_GLASS] ?: false,
            reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,

            tapSoundsEnabled = prefs[Keys.TAP_SOUNDS_ENABLED] ?: true,
            tapSoundVolume = (prefs[Keys.TAP_SOUND_VOLUME] ?: 0.80f).coerceIn(0.05f, 1.0f),
            scanBeepEnabled = prefs[Keys.SCAN_BEEP_ENABLED] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            hapticStrength = runCatching { HapticStrength.valueOf(strengthStr) }.getOrDefault(HapticStrength.MEDIUM),

            selectedHomePreset = prefs[Keys.SELECTED_HOME_PRESET] ?: "",
            homePresetDismissed = prefs[Keys.HOME_PRESET_DISMISSED] ?: false,
            enabledToolIdsCsv = prefs[Keys.ENABLED_TOOL_IDS_CSV] ?: "",
            orderedToolIdsCsv = prefs[Keys.ORDERED_TOOL_IDS_CSV] ?: "",
            orderedSectionsCsv = prefs[Keys.ORDERED_SECTIONS_CSV]
                ?: "PHONE_CARE_AND_AUDIO,MEASURE,LIGHT_AND_SOUND,TRAVEL_AND_TIME,CONVERT_AND_WRITE",
            outdoorsSegmentsCsv = prefs[Keys.OUTDOORS_SEGMENTS_CSV]
                ?: "SOLUNAR:true,SUN_MOON:true,WEATHER:true",
            scanMenuShortcutsCsv = prefs[Keys.SCAN_SHORTCUTS_CSV]
                ?: "scan_doc,scan_qr,quick_note,flashlight",
            favoriteToolIdsCsv = (prefs[Keys.FAVORITE_TOOL_IDS_CSV] ?: "")
                .let { if (it == "speaker_cleaner,frequency_tool,flashlight,compass") "" else it },
            recentToolIdsCsv = prefs[Keys.RECENT_TOOL_IDS_CSV] ?: "",
            homeBlockOrderCsv = prefs[Keys.HOME_BLOCK_ORDER_CSV]
                ?: "HERO,FAVORITES,RECENTLY_USED,SKY_NOW,DYNE_PREMIUM,CATEGORIES,QUICK_ACCESS",
            disabledHomeBlocksCsv = prefs[Keys.DISABLED_HOME_BLOCKS_CSV] ?: "",
            homeTileSize = prefs[Keys.HOME_TILE_SIZE] ?: "M",
            homeLayoutColumns = prefs[Keys.HOME_LAYOUT_COLUMNS] ?: "3",
            sectionLayoutOverridesCsv = prefs[Keys.SECTION_LAYOUT_OVERRIDES_CSV] ?: "",
            showToolNames = prefs[Keys.SHOW_TOOL_NAMES] ?: true,
            outdoorInterestsCsv = prefs[Keys.OUTDOOR_INTERESTS_CSV]
                ?: "FISHING,HUNTING,CAMPING_PHOTO",

            unitSystem = unitSys,
            distanceUnit = prefs[Keys.DISTANCE_UNIT] ?: if (unitSys == UnitSystem.METRIC) "km / m" else "mi / ft",
            speedUnit = prefs[Keys.SPEED_UNIT] ?: if (unitSys == UnitSystem.METRIC) "km/h" else "mph",
            temperatureUnit = prefs[Keys.TEMPERATURE_UNIT] ?: if (unitSys == UnitSystem.METRIC) "°C" else "°F",
            pressureUnit = prefs[Keys.PRESSURE_UNIT] ?: if (unitSys == UnitSystem.METRIC) "hPa" else "inHg",
            use24HourTime = prefs[Keys.USE_24_HOUR] ?: true,
            defaultTimeZone = prefs[Keys.DEFAULT_TIME_ZONE] ?: "System Default",
            defaultCurrency = prefs[Keys.DEFAULT_CURRENCY] ?: "USD",

            lockDocumentsWithBiometrics = prefs[Keys.LOCK_DOCS_BIOMETRICS] ?: false,

            cachedIsPro = prefs[Keys.CACHED_IS_PRO] ?: false,
            cachedProSource = prefs[Keys.CACHED_PRO_SOURCE] ?: "None",
            simulateProEnabled = prefs[Keys.SIMULATE_PRO_ENABLED] ?: false,
            launchCount = prefs[Keys.LAUNCH_COUNT] ?: 0,
            thirdLaunchPaywallDismissed = prefs[Keys.THIRD_LAUNCH_DISMISSED] ?: false,
            manualLat = prefs[Keys.MANUAL_LAT] ?: 37.7749,
            manualLng = prefs[Keys.MANUAL_LNG] ?: -122.4194,
            useManualLocation = prefs[Keys.USE_MANUAL_LOC] ?: false,
            locationName = prefs[Keys.LOCATION_NAME] ?: "Local Coordinates",

            firstSessionStartEpochMs = prefs[Keys.FIRST_SESSION_START_MS] ?: 0L,
            lastInterstitialShownEpochMs = prefs[Keys.LAST_INTERSTITIAL_MS] ?: 0L,
            interstitialDailyDate = prefs[Keys.INTERSTITIAL_DAILY_DATE] ?: "",
            interstitialDailyCount = prefs[Keys.INTERSTITIAL_DAILY_COUNT] ?: 0,
            lastAppOpenShownEpochMs = prefs[Keys.LAST_APP_OPEN_MS] ?: 0L,
            featureUnlocksCsv = prefs[Keys.FEATURE_UNLOCKS_CSV] ?: "",
            rewardedToolUnlocksCsv = prefs[Keys.REWARDED_TOOL_UNLOCKS_CSV] ?: "",
            rewardedThemeUnlocksCsv = prefs[Keys.REWARDED_THEME_UNLOCKS_CSV] ?: "",
            bonusDocumentSaves = prefs[Keys.BONUS_DOCUMENT_SAVES] ?: 0,
            bonusCsvExports = prefs[Keys.BONUS_CSV_EXPORTS] ?: 0,

            coinBalance = (prefs[Keys.COIN_BALANCE] ?: 0).coerceAtLeast(0),
            lastAdCoinEpochMs = prefs[Keys.LAST_AD_COIN_MS] ?: 0L,
            adCoinDailyDate = prefs[Keys.AD_COIN_DAILY_DATE] ?: "",
            adCoinDailyCount = prefs[Keys.AD_COIN_DAILY_COUNT] ?: 0,
            lastCheckInDate = prefs[Keys.LAST_CHECK_IN_DATE] ?: "",
            checkInStreak = (prefs[Keys.CHECK_IN_STREAK] ?: 0).coerceIn(0, 7),

            ownedSoundPacksCsv = prefs[Keys.OWNED_SOUND_PACKS_CSV] ?: "",
            frequencyPresetsCsv = prefs[Keys.FREQUENCY_PRESETS_CSV] ?: "",
            audioHearingSafetyTipDismissed = prefs[Keys.AUDIO_HEARING_SAFETY_DISMISSED] ?: false,
            frequencySafetyCardDismissed = prefs[Keys.FREQUENCY_SAFETY_CARD_DISMISSED] ?: false,
            lastToolModesCsv = prefs[Keys.LAST_TOOL_MODES_CSV] ?: "",
            customToolModesConfigCsv = prefs[Keys.CUSTOM_TOOL_MODES_CONFIG_CSV] ?: "",

            mediaSaveLocationTipDismissed = prefs[Keys.MEDIA_SAVE_LOCATION_TIP_DISMISSED] ?: false,
            bgRemoverDailyDate = prefs[Keys.BG_REMOVER_DAILY_DATE] ?: "",
            bgRemoverDailyCount = prefs[Keys.BG_REMOVER_DAILY_COUNT] ?: 0,
            bgRemoverBonusCredits = prefs[Keys.BG_REMOVER_BONUS_CREDITS] ?: 0,
            passportPhotoFreeTryUsed = prefs[Keys.PASSPORT_PHOTO_FREE_TRY_USED] ?: false,

            solunarAlertsEnabled = prefs[Keys.SOLUNAR_ALERTS_ENABLED] ?: true,
            solunarQuietHoursEnabled = prefs[Keys.SOLUNAR_QUIET_HOURS_ENABLED] ?: true,
            solunarQuietHours = prefs[Keys.SOLUNAR_QUIET_HOURS] ?: "22:00 - 06:00",
            expiryRemindersEnabled = prefs[Keys.EXPIRY_REMINDERS_ENABLED] ?: true,
            expiryQuietHoursEnabled = prefs[Keys.EXPIRY_QUIET_HOURS_ENABLED] ?: true,
            expiryQuietHours = prefs[Keys.EXPIRY_QUIET_HOURS] ?: "22:00 - 07:00",
            dailyBonusNotificationsEnabled = prefs[Keys.DAILY_BONUS_NOTIFS_ENABLED] ?: true,
            dailyBonusQuietHoursEnabled = prefs[Keys.DAILY_BONUS_QUIET_HOURS_ENABLED] ?: true,
            dailyBonusQuietHours = prefs[Keys.DAILY_BONUS_QUIET_HOURS] ?: "21:00 - 08:00",

            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            appLockMethod = prefs[Keys.APP_LOCK_METHOD] ?: "BIOMETRIC_OR_PIN",
            appLockOnLaunch = prefs[Keys.APP_LOCK_ON_LAUNCH] ?: true,
            appLockTimeoutMinutes = (prefs[Keys.APP_LOCK_TIMEOUT_MINUTES] ?: 5).let { if (it in listOf(1, 5, 15)) it else 5 },
            hideAppPreviewInRecents = prefs[Keys.HIDE_APP_PREVIEW_RECENTS] ?: false,
            lockNotesWithBiometrics = prefs[Keys.LOCK_NOTES_BIOMETRICS] ?: false,

            customAccentHex = prefs[Keys.CUSTOM_ACCENT_HEX] ?: "",
            homeTileStyle = prefs[Keys.HOME_TILE_STYLE] ?: "GLASS",
            heroCarouselStyle = prefs[Keys.HERO_CAROUSEL_STYLE] ?: "EDITORIAL",
            iconSetStyle = prefs[Keys.ICON_SET_STYLE] ?: "FILLED",

            solunarAlertMode = prefs[Keys.SOLUNAR_ALERT_MODE] ?: "FISHING",
            solunarAlertLeadMinutes = (prefs[Keys.SOLUNAR_ALERT_LEAD_MINS] ?: 30).let { if (it in listOf(15, 30, 60)) it else 30 },
            goldenHourAlertsEnabled = prefs[Keys.GOLDEN_HOUR_ALERTS_ENABLED] ?: false,
            dailyWeatherSummaryEnabled = prefs[Keys.DAILY_WEATHER_SUMMARY_ENABLED] ?: false,
            rainSoonAlertsEnabled = prefs[Keys.RAIN_SOON_ALERTS_ENABLED] ?: false,

            cachedRemoveAds = prefs[Keys.CACHED_REMOVE_ADS] ?: false,
            quotaDailyDate = prefs[Keys.QUOTA_DAILY_DATE] ?: "",
            quotaCountsCsv = prefs[Keys.QUOTA_COUNTS_CSV] ?: "",
            quotaBonusCsv = prefs[Keys.QUOTA_BONUS_CSV] ?: "",
            spotlightDailyDate = prefs[Keys.SPOTLIGHT_DAILY_DATE] ?: "",
            spotlightToolId = prefs[Keys.SPOTLIGHT_TOOL_ID] ?: "",
            spotlightStartEpochMs = prefs[Keys.SPOTLIGHT_START_MS] ?: 0L,
            spotlightExpiryEpochMs = prefs[Keys.SPOTLIGHT_EXPIRY_MS] ?: 0L,
            lastSmartUpsellShownEpochMs = prefs[Keys.LAST_SMART_UPSELL_MS] ?: 0L,
            smartUpsellPausedUntilEpochMs = prefs[Keys.SMART_UPSELL_PAUSED_UNTIL_MS] ?: 0L,
            completedScanCount = prefs[Keys.COMPLETED_SCAN_COUNT] ?: 0,
            weeklyAdOrCoinUsesCsv = prefs[Keys.WEEKLY_AD_COIN_USES_CSV] ?: "",
            customSectionsJson = prefs[Keys.CUSTOM_SECTIONS_JSON] ?: "",
            hiddenFeatureIdsCsv = prefs[Keys.HIDDEN_FEATURE_IDS_CSV] ?: "",
            perTileSizeCsv = prefs[Keys.PER_TILE_SIZE_CSV] ?: "",
            pinnedTopFeatureIdsCsv = prefs[Keys.PINNED_TOP_FEATURE_IDS_CSV] ?: "",
            startTabIndex = (prefs[Keys.START_TAB_INDEX] ?: 0).coerceIn(0, 3),
            homeFirstFeatureId = prefs[Keys.HOME_FIRST_FEATURE_ID] ?: "",
            savedHomeLayoutsJson = prefs[Keys.SAVED_HOME_LAYOUTS_JSON] ?: "",
            customLayoutSavedBannerDismissed = prefs[Keys.CUSTOM_LAYOUT_SAVED_BANNER_DISMISSED] ?: false,
            photosensitivityWarningShown = prefs[Keys.PHOTOSENSITIVITY_WARNING_SHOWN] ?: false,
            accessUnlockRecordsSerialized = prefs[Keys.ACCESS_UNLOCK_RECORDS] ?: "",
            lastObservedWallMs = prefs[Keys.LAST_OBSERVED_WALL_MS] ?: 0L,
            lastObservedElapsedMs = prefs[Keys.LAST_OBSERVED_ELAPSED_MS] ?: 0L,
            clockRollbackInvalidated = prefs[Keys.CLOCK_ROLLBACK_INVALIDATED] ?: false,
            hadActiveLookUnlockSession = prefs[Keys.HAD_ACTIVE_LOOK_UNLOCK_SESSION] ?: false,
            pendingHomeTileSize = prefs[Keys.PENDING_HOME_TILE_SIZE] ?: "",
            pendingPerTileSizeCsv = prefs[Keys.PENDING_PER_TILE_SIZE_CSV] ?: "",
            pendingCustomSectionsJson = prefs[Keys.PENDING_CUSTOM_SECTIONS_JSON] ?: "",
            pendingHiddenFeatureIdsCsv = prefs[Keys.PENDING_HIDDEN_FEATURE_IDS_CSV] ?: "",
            pendingFavoriteToolIdsCsv = prefs[Keys.PENDING_FAVORITE_TOOL_IDS_CSV] ?: "",
            pendingPinnedTopFeatureIdsCsv = prefs[Keys.PENDING_PINNED_TOP_FEATURE_IDS_CSV] ?: "",
            pendingStartTabIndex = prefs[Keys.PENDING_START_TAB_INDEX] ?: -1,
            pendingHomeFirstFeatureId = prefs[Keys.PENDING_HOME_FIRST_FEATURE_ID] ?: "",
            pendingCustomAccentHex = prefs[Keys.PENDING_CUSTOM_ACCENT_HEX] ?: "",
            pendingHomeTileStyle = prefs[Keys.PENDING_HOME_TILE_STYLE] ?: "",
            pendingHeroCarouselStyle = prefs[Keys.PENDING_HERO_CAROUSEL_STYLE] ?: "",
            pendingIconSetStyle = prefs[Keys.PENDING_ICON_SET_STYLE] ?: "",
            pendingHomeBlockOrderCsv = prefs[Keys.PENDING_HOME_BLOCK_ORDER_CSV] ?: "",
            pendingDisabledHomeBlocksCsv = prefs[Keys.PENDING_DISABLED_HOME_BLOCKS_CSV] ?: "",
            pendingHomeLayoutColumns = prefs[Keys.PENDING_HOME_LAYOUT_COLUMNS] ?: "",
            pendingSectionLayoutOverridesCsv = prefs[Keys.PENDING_SECTION_LAYOUT_OVERRIDES_CSV] ?: ""
        )
    }

    private fun isEntitledProInPrefs(prefs: androidx.datastore.preferences.core.Preferences): Boolean {
        return (prefs[Keys.CACHED_IS_PRO] == true) || (prefs[Keys.SIMULATE_PRO_ENABLED] == true)
    }

    private fun promotePendingCustomizationsInPrefs(prefs: androidx.datastore.preferences.core.MutablePreferences) {
        prefs[Keys.PENDING_HOME_TILE_SIZE]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.HOME_TILE_SIZE] = it }
        prefs[Keys.PENDING_PER_TILE_SIZE_CSV]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.PER_TILE_SIZE_CSV] = it }
        prefs[Keys.PENDING_CUSTOM_SECTIONS_JSON]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.CUSTOM_SECTIONS_JSON] = it }
        prefs[Keys.PENDING_HIDDEN_FEATURE_IDS_CSV]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.HIDDEN_FEATURE_IDS_CSV] = it }
        prefs[Keys.PENDING_FAVORITE_TOOL_IDS_CSV]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.FAVORITE_TOOL_IDS_CSV] = it }
        prefs[Keys.PENDING_PINNED_TOP_FEATURE_IDS_CSV]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.PINNED_TOP_FEATURE_IDS_CSV] = it }
        prefs[Keys.PENDING_START_TAB_INDEX]?.takeIf { it >= 0 }?.let { prefs[Keys.START_TAB_INDEX] = it.coerceIn(0, 3) }
        prefs[Keys.PENDING_HOME_FIRST_FEATURE_ID]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.HOME_FIRST_FEATURE_ID] = it }
        prefs[Keys.PENDING_CUSTOM_ACCENT_HEX]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.CUSTOM_ACCENT_HEX] = it }
        prefs[Keys.PENDING_HOME_TILE_STYLE]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.HOME_TILE_STYLE] = it }
        prefs[Keys.PENDING_HERO_CAROUSEL_STYLE]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.HERO_CAROUSEL_STYLE] = it }
        prefs[Keys.PENDING_ICON_SET_STYLE]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.ICON_SET_STYLE] = it }
        prefs[Keys.PENDING_HOME_BLOCK_ORDER_CSV]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.HOME_BLOCK_ORDER_CSV] = it }
        prefs[Keys.PENDING_DISABLED_HOME_BLOCKS_CSV]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.DISABLED_HOME_BLOCKS_CSV] = it }
        prefs[Keys.PENDING_HOME_LAYOUT_COLUMNS]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.HOME_LAYOUT_COLUMNS] = it }
        prefs[Keys.PENDING_SECTION_LAYOUT_OVERRIDES_CSV]?.takeIf { it.isNotBlank() }?.let { prefs[Keys.SECTION_LAYOUT_OVERRIDES_CSV] = it }

        prefs.remove(Keys.PENDING_HOME_TILE_SIZE)
        prefs.remove(Keys.PENDING_PER_TILE_SIZE_CSV)
        prefs.remove(Keys.PENDING_CUSTOM_SECTIONS_JSON)
        prefs.remove(Keys.PENDING_HIDDEN_FEATURE_IDS_CSV)
        prefs.remove(Keys.PENDING_FAVORITE_TOOL_IDS_CSV)
        prefs.remove(Keys.PENDING_PINNED_TOP_FEATURE_IDS_CSV)
        prefs.remove(Keys.PENDING_START_TAB_INDEX)
        prefs.remove(Keys.PENDING_HOME_FIRST_FEATURE_ID)
        prefs.remove(Keys.PENDING_CUSTOM_ACCENT_HEX)
        prefs.remove(Keys.PENDING_HOME_TILE_STYLE)
        prefs.remove(Keys.PENDING_HERO_CAROUSEL_STYLE)
        prefs.remove(Keys.PENDING_ICON_SET_STYLE)
        prefs.remove(Keys.PENDING_HOME_BLOCK_ORDER_CSV)
        prefs.remove(Keys.PENDING_DISABLED_HOME_BLOCKS_CSV)
        prefs.remove(Keys.PENDING_HOME_LAYOUT_COLUMNS)
        prefs.remove(Keys.PENDING_SECTION_LAYOUT_OVERRIDES_CSV)
    }

    suspend fun incrementLaunchCount(): Int {
        var updated = 1
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.LAUNCH_COUNT] ?: 0
            updated = current + 1
            prefs[Keys.LAUNCH_COUNT] = updated
        }
        return updated
    }

    suspend fun markThirdLaunchPaywallShown() {
        context.dataStore.edit { prefs ->
            prefs[Keys.THIRD_LAUNCH_DISMISSED] = true
        }
    }

    suspend fun setCachedProFromPlayBilling(isPro: Boolean, source: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CACHED_IS_PRO] = isPro
            prefs[Keys.CACHED_PRO_SOURCE] = if (isPro) source else "None"
            if (isPro) {
                promotePendingCustomizationsInPrefs(prefs)
            }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    /**
     * Writes [SELECTED_THEME_NAME] ONLY for entitled (Premium) users or when restoring the
     * default free theme ("Lime Night"). NEVER writes [SELECTED_THEME_NAME] for a temporary unlock.
     */
    suspend fun setSelectedThemeName(name: String, forceEntitledPro: Boolean = false) {
        context.dataStore.edit { prefs ->
            val isEntitled = forceEntitledPro ||
                isEntitledProInPrefs(prefs) ||
                name.equals("Lime Night", ignoreCase = true)
            if (isEntitled) {
                prefs[Keys.SELECTED_THEME_NAME] = name
            }
        }
    }

    suspend fun setLaunchAnimationMode(mode: LaunchAnimationMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAUNCH_ANIMATION_MODE] = mode.name
        }
    }

    suspend fun setLastSplashCombo(combo: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SPLASH_COMBO] = combo
        }
    }

    suspend fun setSelectedAppIconStyle(style: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_APP_ICON_STYLE] = style
        }
    }

    suspend fun setTextScale(scale: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TEXT_SCALE] = scale.coerceIn(0.85f, 1.25f)
        }
    }

    suspend fun setReduceGlassEffects(reduce: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REDUCE_GLASS] = reduce
        }
    }

    suspend fun setReduceMotion(reduce: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REDUCE_MOTION] = reduce
        }
    }

    suspend fun setTapSoundsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TAP_SOUNDS_ENABLED] = enabled
        }
    }

    suspend fun setTapSoundVolume(volume: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TAP_SOUND_VOLUME] = volume.coerceIn(0.05f, 1.0f)
        }
    }

    suspend fun setScanBeepEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SCAN_BEEP_ENABLED] = enabled
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun setHapticStrength(strength: HapticStrength) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAPTIC_STRENGTH] = strength.name
        }
    }

    suspend fun setHomeCustomization(
        preset: String? = null,
        presetDismissed: Boolean? = null,
        enabledToolIdsCsv: String? = null,
        orderedToolIdsCsv: String? = null,
        orderedSectionsCsv: String? = null,
        outdoorsSegmentsCsv: String? = null,
        scanShortcutsCsv: String? = null,
        favoriteToolIdsCsv: String? = null,
        recentToolIdsCsv: String? = null,
        homeBlockOrderCsv: String? = null,
        disabledHomeBlocksCsv: String? = null,
        homeTileSize: String? = null,
        homeLayoutColumns: String? = null,
        sectionLayoutOverridesCsv: String? = null,
        showToolNames: Boolean? = null,
        outdoorInterestsCsv: String? = null
    ) {
        context.dataStore.edit { prefs ->
            if (preset != null) prefs[Keys.SELECTED_HOME_PRESET] = preset
            if (presetDismissed != null) prefs[Keys.HOME_PRESET_DISMISSED] = presetDismissed
            if (enabledToolIdsCsv != null) prefs[Keys.ENABLED_TOOL_IDS_CSV] = enabledToolIdsCsv
            if (orderedToolIdsCsv != null) prefs[Keys.ORDERED_TOOL_IDS_CSV] = orderedToolIdsCsv
            if (orderedSectionsCsv != null) prefs[Keys.ORDERED_SECTIONS_CSV] = orderedSectionsCsv
            if (outdoorsSegmentsCsv != null) prefs[Keys.OUTDOORS_SEGMENTS_CSV] = outdoorsSegmentsCsv
            if (scanShortcutsCsv != null) prefs[Keys.SCAN_SHORTCUTS_CSV] = scanShortcutsCsv
            if (favoriteToolIdsCsv != null) prefs[Keys.FAVORITE_TOOL_IDS_CSV] = favoriteToolIdsCsv
            if (recentToolIdsCsv != null) prefs[Keys.RECENT_TOOL_IDS_CSV] = recentToolIdsCsv
            if (homeBlockOrderCsv != null) prefs[Keys.HOME_BLOCK_ORDER_CSV] = homeBlockOrderCsv
            if (disabledHomeBlocksCsv != null) prefs[Keys.DISABLED_HOME_BLOCKS_CSV] = disabledHomeBlocksCsv
            if (homeTileSize != null) prefs[Keys.HOME_TILE_SIZE] = homeTileSize
            if (homeLayoutColumns != null) prefs[Keys.HOME_LAYOUT_COLUMNS] = homeLayoutColumns
            if (sectionLayoutOverridesCsv != null) prefs[Keys.SECTION_LAYOUT_OVERRIDES_CSV] = sectionLayoutOverridesCsv
            if (showToolNames != null) prefs[Keys.SHOW_TOOL_NAMES] = showToolNames
            if (outdoorInterestsCsv != null) prefs[Keys.OUTDOOR_INTERESTS_CSV] = outdoorInterestsCsv
        }
    }

    suspend fun toggleFavoriteTool(toolId: String): Boolean {
        var added = false
        context.dataStore.edit { prefs ->
            val raw = (prefs[Keys.FAVORITE_TOOL_IDS_CSV] ?: "")
                .let { if (it == "speaker_cleaner,frequency_tool,flashlight,compass") "" else it }
            val current = raw
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .toMutableList()
            val resolvedFeature = com.example.domain.FeatureRegistry.resolveFeatureOrPrimaryForTool(toolId)
            val canonicalId = resolvedFeature?.id ?: toolId
            val existingIndex = current.indexOfFirst {
                it == canonicalId || it == toolId ||
                    com.example.domain.FeatureRegistry.resolveFeatureOrPrimaryForTool(it)?.id == canonicalId
            }
            if (existingIndex >= 0) {
                current.removeAt(existingIndex)
                added = false
            } else {
                current.add(0, canonicalId)
                added = true
            }
            prefs[Keys.FAVORITE_TOOL_IDS_CSV] = current.joinToString(",")
        }
        return added
    }

    /**
     * Toggles a feature or tool in Favorites while respecting [maxLimit] for Free users.
     * Returns Pair(changed: Boolean, limitReached: Boolean).
     */
    suspend fun toggleFavoriteWithLimit(featureOrToolId: String, maxLimit: Int?): Pair<Boolean, Boolean> {
        var changed = false
        var limitReached = false
        context.dataStore.edit { prefs ->
            val isPro = isEntitledProInPrefs(prefs)
            val legacyDefault = "speaker_cleaner,frequency_tool,flashlight,compass"
            val rawSaved = (prefs[Keys.FAVORITE_TOOL_IDS_CSV] ?: "")
                .let { if (it == legacyDefault) "" else it }
            val sourceCsv = if (!isPro && maxLimit == null && !(prefs[Keys.PENDING_FAVORITE_TOOL_IDS_CSV] ?: "").isBlank()) {
                prefs[Keys.PENDING_FAVORITE_TOOL_IDS_CSV] ?: ""
            } else {
                rawSaved
            }
            val current = sourceCsv
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .toMutableList()
            val resolvedFeature = com.example.domain.FeatureRegistry.resolveFeatureOrPrimaryForTool(featureOrToolId)
            val canonicalId = resolvedFeature?.id ?: featureOrToolId
            val existingIndex = current.indexOfFirst {
                it == canonicalId || it == featureOrToolId ||
                    com.example.domain.FeatureRegistry.resolveFeatureOrPrimaryForTool(it)?.id == canonicalId
            }
            if (existingIndex >= 0) {
                current.removeAt(existingIndex)
                changed = true
            } else {
                if (maxLimit != null && current.size >= maxLimit) {
                    limitReached = true
                    changed = false
                } else {
                    current.add(0, canonicalId)
                    changed = true
                }
            }
            if (changed) {
                val joined = current.joinToString(",")
                if (!isPro && maxLimit == null) {
                    prefs[Keys.PENDING_FAVORITE_TOOL_IDS_CSV] = joined
                } else {
                    prefs[Keys.FAVORITE_TOOL_IDS_CSV] = joined
                }
            }
        }
        return changed to limitReached
    }

    suspend fun reorderFavoriteTools(newOrder: List<String>) {
        context.dataStore.edit { prefs ->
            val joined = newOrder.distinct().joinToString(",")
            if (isEntitledProInPrefs(prefs)) {
                prefs[Keys.FAVORITE_TOOL_IDS_CSV] = joined
            } else {
                prefs[Keys.PENDING_FAVORITE_TOOL_IDS_CSV] = joined
            }
        }
    }

    suspend fun moveFavoriteTool(fromIndex: Int, toIndex: Int) {
        context.dataStore.edit { prefs ->
            val isPro = isEntitledProInPrefs(prefs)
            val legacyDefault = "speaker_cleaner,frequency_tool,flashlight,compass"
            val rawSaved = (prefs[Keys.FAVORITE_TOOL_IDS_CSV] ?: "")
                .let { if (it == legacyDefault) "" else it }
            val raw = if (isPro) {
                rawSaved
            } else {
                (prefs[Keys.PENDING_FAVORITE_TOOL_IDS_CSV]?.takeIf { it.isNotBlank() } ?: rawSaved)
            }
            val current = raw
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .toMutableList()
            if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
                val moved = current.removeAt(fromIndex)
                current.add(toIndex, moved)
                val joined = current.joinToString(",")
                if (isPro) {
                    prefs[Keys.FAVORITE_TOOL_IDS_CSV] = joined
                } else {
                    prefs[Keys.PENDING_FAVORITE_TOOL_IDS_CSV] = joined
                }
            }
        }
    }

    suspend fun recordRecentToolUsed(toolId: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.RECENT_TOOL_IDS_CSV] ?: "")
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() && it != toolId }
                .toMutableList()
            current.add(0, toolId)
            prefs[Keys.RECENT_TOOL_IDS_CSV] = current.take(10).joinToString(",")
        }
    }

    suspend fun recordRecentTool(toolId: String) {
        recordRecentToolUsed(toolId)
    }

    suspend fun toggleHomeBlock(blockId: String, enabled: Boolean) {
        val norm = blockId.trim().uppercase()
        context.dataStore.edit { prefs ->
            val isPro = isEntitledProInPrefs(prefs)
            val key = if (isPro) Keys.DISABLED_HOME_BLOCKS_CSV else Keys.PENDING_DISABLED_HOME_BLOCKS_CSV
            val disabled = (prefs[key] ?: prefs[Keys.DISABLED_HOME_BLOCKS_CSV] ?: "")
                .split(",")
                .map { it.trim().uppercase() }
                .filter { it.isNotEmpty() }
                .toMutableSet()
            if (enabled) disabled.remove(norm) else disabled.add(norm)
            prefs[key] = disabled.joinToString(",")
        }
    }

    suspend fun setHomeBlockOrder(order: List<String>) {
        context.dataStore.edit { prefs ->
            val joined = order.map { it.trim().uppercase() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(",")
            if (isEntitledProInPrefs(prefs)) {
                prefs[Keys.HOME_BLOCK_ORDER_CSV] = joined
            } else {
                prefs[Keys.PENDING_HOME_BLOCK_ORDER_CSV] = joined
            }
        }
    }

    suspend fun setHomeTileSize(size: String) {
        context.dataStore.edit { prefs ->
            val clean = size.trim().uppercase()
            if (isEntitledProInPrefs(prefs)) {
                prefs[Keys.HOME_TILE_SIZE] = clean
            } else {
                prefs[Keys.PENDING_HOME_TILE_SIZE] = clean
            }
        }
    }

    suspend fun setHomeLayoutColumns(columns: Int) {
        context.dataStore.edit { prefs ->
            if (isEntitledProInPrefs(prefs)) {
                prefs[Keys.HOME_LAYOUT_COLUMNS] = columns.toString()
            } else {
                prefs[Keys.PENDING_HOME_LAYOUT_COLUMNS] = columns.toString()
            }
        }
    }

    suspend fun setSectionLayoutOverride(categoryName: String, columns: Int?) {
        val normKey = categoryName.trim().uppercase()
        context.dataStore.edit { prefs ->
            val isPro = isEntitledProInPrefs(prefs)
            val targetKey = if (isPro) Keys.SECTION_LAYOUT_OVERRIDES_CSV else Keys.PENDING_SECTION_LAYOUT_OVERRIDES_CSV
            val currentMap = mutableMapOf<String, Int>()
            (prefs[targetKey] ?: prefs[Keys.SECTION_LAYOUT_OVERRIDES_CSV] ?: "").split(",").forEach { token ->
                val parts = token.split(":")
                if (parts.size >= 2) {
                    val k = parts[0].trim().uppercase()
                    val v = parts[1].trim().toIntOrNull()
                    if (k.isNotEmpty() && v != null) currentMap[k] = v
                }
            }
            if (columns == null) {
                currentMap.remove(normKey)
            } else {
                currentMap[normKey] = columns
            }
            prefs[targetKey] = currentMap.entries.joinToString(",") { "${it.key}:${it.value}" }
        }
    }

    suspend fun setShowToolNames(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_TOOL_NAMES] = show
        }
    }

    suspend fun toggleOutdoorInterest(interest: String) {
        val norm = interest.trim().uppercase()
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.OUTDOOR_INTERESTS_CSV] ?: "FISHING,HUNTING,CAMPING_PHOTO")
                .split(",")
                .map { it.trim().uppercase() }
                .filter { it.isNotEmpty() }
                .toMutableSet()
            if (current.contains(norm)) {
                current.remove(norm)
            } else {
                current.add(norm)
            }
            prefs[Keys.OUTDOOR_INTERESTS_CSV] = current.joinToString(",")
        }
    }

    suspend fun resetHomeAndToolsCustomization() {
        context.dataStore.edit { prefs ->
            prefs[Keys.HOME_BLOCK_ORDER_CSV] =
                "HERO,FAVORITES,RECENTLY_USED,SKY_NOW,DYNE_PREMIUM,CATEGORIES,QUICK_ACCESS"
            prefs[Keys.DISABLED_HOME_BLOCKS_CSV] = ""
            prefs[Keys.HOME_TILE_SIZE] = "M"
            prefs[Keys.HOME_LAYOUT_COLUMNS] = "0"
            prefs[Keys.SECTION_LAYOUT_OVERRIDES_CSV] = ""
            prefs[Keys.SHOW_TOOL_NAMES] = true
            prefs[Keys.OUTDOOR_INTERESTS_CSV] = "FISHING,HUNTING,CAMPING_PHOTO"
            prefs[Keys.FAVORITE_TOOL_IDS_CSV] = "flashlight_normal,speaker_cleaner_water,compass_main"
            prefs[Keys.CUSTOM_TOOL_MODES_CONFIG_CSV] = ""
            prefs[Keys.CUSTOM_SECTIONS_JSON] = ""
            prefs[Keys.HIDDEN_FEATURE_IDS_CSV] = ""
            prefs[Keys.PER_TILE_SIZE_CSV] = ""
            prefs[Keys.PINNED_TOP_FEATURE_IDS_CSV] = ""
            prefs[Keys.START_TAB_INDEX] = 0
            prefs[Keys.HOME_FIRST_FEATURE_ID] = ""

            prefs.remove(Keys.PENDING_HOME_TILE_SIZE)
            prefs.remove(Keys.PENDING_PER_TILE_SIZE_CSV)
            prefs.remove(Keys.PENDING_CUSTOM_SECTIONS_JSON)
            prefs.remove(Keys.PENDING_HIDDEN_FEATURE_IDS_CSV)
            prefs.remove(Keys.PENDING_FAVORITE_TOOL_IDS_CSV)
            prefs.remove(Keys.PENDING_PINNED_TOP_FEATURE_IDS_CSV)
            prefs.remove(Keys.PENDING_START_TAB_INDEX)
            prefs.remove(Keys.PENDING_HOME_FIRST_FEATURE_ID)
            prefs.remove(Keys.PENDING_CUSTOM_ACCENT_HEX)
            prefs.remove(Keys.PENDING_HOME_TILE_STYLE)
            prefs.remove(Keys.PENDING_HERO_CAROUSEL_STYLE)
            prefs.remove(Keys.PENDING_ICON_SET_STYLE)
            prefs.remove(Keys.PENDING_HOME_BLOCK_ORDER_CSV)
            prefs.remove(Keys.PENDING_DISABLED_HOME_BLOCKS_CSV)
            prefs.remove(Keys.PENDING_HOME_LAYOUT_COLUMNS)
            prefs.remove(Keys.PENDING_SECTION_LAYOUT_OVERRIDES_CSV)
        }
    }

    suspend fun saveCustomSectionsJson(json: String) {
        context.dataStore.edit { prefs ->
            if (isEntitledProInPrefs(prefs)) {
                prefs[Keys.CUSTOM_SECTIONS_JSON] = json
            } else {
                prefs[Keys.PENDING_CUSTOM_SECTIONS_JSON] = json
            }
            prefs[Keys.CUSTOM_LAYOUT_SAVED_BANNER_DISMISSED] = false
        }
    }

    suspend fun setHiddenFeatureIds(ids: Set<String>) {
        context.dataStore.edit { prefs ->
            val joined = ids.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(",")
            if (isEntitledProInPrefs(prefs)) {
                prefs[Keys.HIDDEN_FEATURE_IDS_CSV] = joined
            } else {
                prefs[Keys.PENDING_HIDDEN_FEATURE_IDS_CSV] = joined
            }
            prefs[Keys.CUSTOM_LAYOUT_SAVED_BANNER_DISMISSED] = false
        }
    }

    suspend fun setPerTileSize(featureId: String, size: com.example.domain.FeatureTileSize) {
        context.dataStore.edit { prefs ->
            val isPro = isEntitledProInPrefs(prefs)
            val sourceCsv = if (isPro) {
                prefs[Keys.PER_TILE_SIZE_CSV] ?: ""
            } else {
                (prefs[Keys.PENDING_PER_TILE_SIZE_CSV]?.takeIf { it.isNotBlank() } ?: prefs[Keys.PER_TILE_SIZE_CSV] ?: "")
            }
            val map = com.example.domain.FeatureRegistry.parseTileSizes(sourceCsv).toMutableMap()
            if (size == com.example.domain.FeatureTileSize.MEDIUM) {
                map.remove(featureId)
            } else {
                map[featureId] = size
            }
            val serialized = com.example.domain.FeatureRegistry.serializeTileSizes(map)
            if (isPro) {
                prefs[Keys.PER_TILE_SIZE_CSV] = serialized
            } else {
                prefs[Keys.PENDING_PER_TILE_SIZE_CSV] = serialized
            }
            prefs[Keys.CUSTOM_LAYOUT_SAVED_BANNER_DISMISSED] = false
        }
    }

    suspend fun setPinnedTopFeatureIds(ids: List<String>) {
        context.dataStore.edit { prefs ->
            val joined = ids.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(",")
            if (isEntitledProInPrefs(prefs)) {
                prefs[Keys.PINNED_TOP_FEATURE_IDS_CSV] = joined
            } else {
                prefs[Keys.PENDING_PINNED_TOP_FEATURE_IDS_CSV] = joined
            }
            prefs[Keys.CUSTOM_LAYOUT_SAVED_BANNER_DISMISSED] = false
        }
    }

    suspend fun setStartTabIndex(index: Int) {
        context.dataStore.edit { prefs ->
            val clamped = index.coerceIn(0, 3)
            if (isEntitledProInPrefs(prefs)) {
                prefs[Keys.START_TAB_INDEX] = clamped
            } else {
                prefs[Keys.PENDING_START_TAB_INDEX] = clamped
            }
        }
    }

    suspend fun setHomeFirstFeatureId(featureId: String) {
        context.dataStore.edit { prefs ->
            val clean = featureId.trim()
            if (isEntitledProInPrefs(prefs)) {
                prefs[Keys.HOME_FIRST_FEATURE_ID] = clean
            } else {
                prefs[Keys.PENDING_HOME_FIRST_FEATURE_ID] = clean
            }
            prefs[Keys.CUSTOM_LAYOUT_SAVED_BANNER_DISMISSED] = false
        }
    }

    suspend fun setCustomLayoutSavedBannerDismissed(dismissed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_LAYOUT_SAVED_BANNER_DISMISSED] = dismissed
        }
    }

    suspend fun saveNamedHomeLayout(name: String): List<com.example.domain.SavedHomeLayout> {
        val cleanName = name.trim().ifEmpty { "Custom Layout" }
        var updatedList: List<com.example.domain.SavedHomeLayout> = emptyList()
        context.dataStore.edit { prefs ->
            val current = com.example.domain.FeatureRegistry.parseSavedLayouts(prefs[Keys.SAVED_HOME_LAYOUTS_JSON] ?: "")
                .toMutableList()
            val existingIdx = current.indexOfFirst { it.name.equals(cleanName, ignoreCase = true) }
            val entry = com.example.domain.SavedHomeLayout(
                id = if (existingIdx >= 0) current[existingIdx].id else "layout_${System.currentTimeMillis()}",
                name = cleanName,
                createdAtMs = System.currentTimeMillis(),
                sectionsJson = prefs[Keys.CUSTOM_SECTIONS_JSON] ?: "",
                hiddenFeatureIdsCsv = prefs[Keys.HIDDEN_FEATURE_IDS_CSV] ?: "",
                perTileSizeCsv = prefs[Keys.PER_TILE_SIZE_CSV] ?: "",
                pinnedTopFeatureIdsCsv = prefs[Keys.PINNED_TOP_FEATURE_IDS_CSV] ?: "",
                startTabIndex = prefs[Keys.START_TAB_INDEX] ?: 0,
                homeFirstFeatureId = prefs[Keys.HOME_FIRST_FEATURE_ID] ?: ""
            )
            if (existingIdx >= 0) {
                current[existingIdx] = entry
            } else {
                current.add(0, entry)
            }
            updatedList = current.take(com.example.domain.MAX_SAVED_HOME_LAYOUTS)
            prefs[Keys.SAVED_HOME_LAYOUTS_JSON] = com.example.domain.FeatureRegistry.serializeSavedLayouts(updatedList)
        }
        return updatedList
    }

    suspend fun applyNamedHomeLayout(layout: com.example.domain.SavedHomeLayout) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_SECTIONS_JSON] = layout.sectionsJson
            prefs[Keys.HIDDEN_FEATURE_IDS_CSV] = layout.hiddenFeatureIdsCsv
            prefs[Keys.PER_TILE_SIZE_CSV] = layout.perTileSizeCsv
            prefs[Keys.PINNED_TOP_FEATURE_IDS_CSV] = layout.pinnedTopFeatureIdsCsv
            prefs[Keys.START_TAB_INDEX] = layout.startTabIndex.coerceIn(0, 3)
            prefs[Keys.HOME_FIRST_FEATURE_ID] = layout.homeFirstFeatureId
        }
    }

    suspend fun deleteNamedHomeLayout(layoutId: String) {
        context.dataStore.edit { prefs ->
            val current = com.example.domain.FeatureRegistry.parseSavedLayouts(prefs[Keys.SAVED_HOME_LAYOUTS_JSON] ?: "")
                .filterNot { it.id == layoutId }
            prefs[Keys.SAVED_HOME_LAYOUTS_JSON] = com.example.domain.FeatureRegistry.serializeSavedLayouts(current)
        }
    }

    suspend fun setLastModeForTool(toolId: String, modeId: String) {
        val cleanTool = toolId.trim().lowercase()
        val cleanMode = modeId.trim()
        if (cleanTool.isEmpty() || cleanMode.isEmpty() || cleanMode == "more") return
        context.dataStore.edit { prefs ->
            val map = linkedMapOf<String, String>()
            (prefs[Keys.LAST_TOOL_MODES_CSV] ?: "").split(",").forEach { token ->
                val parts = token.split(":")
                if (parts.size >= 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    map[parts[0].trim().lowercase()] = parts[1].trim()
                }
            }
            map[cleanTool] = cleanMode
            prefs[Keys.LAST_TOOL_MODES_CSV] = map.entries.joinToString(",") { "${it.key}:${it.value}" }
        }
    }

    suspend fun setCustomToolModesForTool(toolId: String, modeSpecs: List<Pair<String, Boolean>>) {
        val cleanTool = toolId.trim().lowercase()
        if (cleanTool.isEmpty()) return
        context.dataStore.edit { prefs ->
            val map = linkedMapOf<String, String>()
            (prefs[Keys.CUSTOM_TOOL_MODES_CONFIG_CSV] ?: "").split(";").forEach { entry ->
                val parts = entry.split("=")
                if (parts.size == 2 && parts[0].isNotBlank()) {
                    map[parts[0].trim().lowercase()] = parts[1].trim()
                }
            }
            val encoded = modeSpecs.joinToString(",") { (mId, enabled) ->
                "${mId.trim()}:${if (enabled) "1" else "0"}"
            }
            map[cleanTool] = encoded
            prefs[Keys.CUSTOM_TOOL_MODES_CONFIG_CSV] = map.entries.joinToString(";") { "${it.key}=${it.value}" }
        }
    }

    suspend fun setOwnedSoundPacks(packIds: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OWNED_SOUND_PACKS_CSV] = packIds.map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(",")
        }
    }

    suspend fun setFrequencyPresetsCsv(csv: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FREQUENCY_PRESETS_CSV] = csv
        }
    }

    suspend fun setAudioHearingSafetyTipDismissed(dismissed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUDIO_HEARING_SAFETY_DISMISSED] = dismissed
        }
    }

    suspend fun dismissAudioHearingSafetyTip() {
        setAudioHearingSafetyTipDismissed(true)
    }

    suspend fun setFrequencySafetyCardDismissed(dismissed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FREQUENCY_SAFETY_CARD_DISMISSED] = dismissed
        }
    }

    suspend fun dismissFrequencySafetyCard() {
        setFrequencySafetyCardDismissed(true)
    }

    suspend fun dismissMediaSaveLocationTip() {
        context.dataStore.edit { prefs ->
            prefs[Keys.MEDIA_SAVE_LOCATION_TIP_DISMISSED] = true
        }
    }

    suspend fun consumeBgRemoverQuota(todayKey: String = java.time.LocalDate.now().toString()): Boolean {
        var allowed = false
        context.dataStore.edit { prefs ->
            val savedDate = prefs[Keys.BG_REMOVER_DAILY_DATE] ?: ""
            val currentCount = if (savedDate == todayKey) (prefs[Keys.BG_REMOVER_DAILY_COUNT] ?: 0) else 0
            val bonus = (prefs[Keys.BG_REMOVER_BONUS_CREDITS] ?: 0).coerceAtLeast(0)
            if (currentCount < 3) {
                prefs[Keys.BG_REMOVER_DAILY_DATE] = todayKey
                prefs[Keys.BG_REMOVER_DAILY_COUNT] = currentCount + 1
                allowed = true
            } else if (bonus > 0) {
                prefs[Keys.BG_REMOVER_BONUS_CREDITS] = bonus - 1
                allowed = true
            }
        }
        return allowed
    }

    suspend fun grantBgRemoverBonusCredit(count: Int = 3) {
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.BG_REMOVER_BONUS_CREDITS] ?: 0).coerceAtLeast(0)
            prefs[Keys.BG_REMOVER_BONUS_CREDITS] = current + count
        }
    }

    suspend fun consumePassportPhotoFreeTry(): Boolean {
        var allowed = false
        context.dataStore.edit { prefs ->
            val used = prefs[Keys.PASSPORT_PHOTO_FREE_TRY_USED] ?: false
            if (!used) {
                prefs[Keys.PASSPORT_PHOTO_FREE_TRY_USED] = true
                allowed = true
            }
        }
        return allowed
    }

    suspend fun setUnitSystem(units: UnitSystem) {
        context.dataStore.edit { prefs ->
            prefs[Keys.UNIT_SYSTEM] = units.name
            if (units == UnitSystem.METRIC) {
                prefs[Keys.DISTANCE_UNIT] = "km / m"
                prefs[Keys.SPEED_UNIT] = "km/h"
                prefs[Keys.TEMPERATURE_UNIT] = "°C"
                prefs[Keys.PRESSURE_UNIT] = "hPa"
            } else {
                prefs[Keys.DISTANCE_UNIT] = "mi / ft"
                prefs[Keys.SPEED_UNIT] = "mph"
                prefs[Keys.TEMPERATURE_UNIT] = "°F"
                prefs[Keys.PRESSURE_UNIT] = "inHg"
            }
        }
    }

    suspend fun setDistanceUnit(unit: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DISTANCE_UNIT] = unit
            prefs[Keys.UNIT_SYSTEM] = if (unit.contains("km")) UnitSystem.METRIC.name else UnitSystem.IMPERIAL.name
        }
    }

    suspend fun setSpeedUnit(unit: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SPEED_UNIT] = unit
        }
    }

    suspend fun setTemperatureUnit(unit: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TEMPERATURE_UNIT] = unit
        }
    }

    suspend fun setPressureUnit(unit: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PRESSURE_UNIT] = unit
        }
    }

    suspend fun setUse24HourTime(use24h: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USE_24_HOUR] = use24h
        }
    }

    suspend fun setDefaultTimeZone(zone: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_TIME_ZONE] = zone
        }
    }

    suspend fun setDefaultCurrency(currency: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_CURRENCY] = currency
        }
    }

    suspend fun setLockDocumentsWithBiometrics(locked: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOCK_DOCS_BIOMETRICS] = locked
        }
    }

    suspend fun setLockNotesWithBiometrics(locked: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOCK_NOTES_BIOMETRICS] = locked
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setAppLockMethod(method: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_LOCK_METHOD] = method
        }
    }

    suspend fun setAppLockOnLaunch(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_LOCK_ON_LAUNCH] = enabled
        }
    }

    suspend fun setAppLockTimeoutMinutes(minutes: Int) {
        val valid = if (minutes in listOf(1, 5, 15)) minutes else 5
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_LOCK_TIMEOUT_MINUTES] = valid
        }
    }

    suspend fun setHideAppPreviewInRecents(hide: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIDE_APP_PREVIEW_RECENTS] = hide
        }
    }

    suspend fun setSolunarNotificationConfig(
        enabled: Boolean? = null,
        quietHoursEnabled: Boolean? = null,
        quietHours: String? = null
    ) {
        context.dataStore.edit { prefs ->
            if (enabled != null) prefs[Keys.SOLUNAR_ALERTS_ENABLED] = enabled
            if (quietHoursEnabled != null) prefs[Keys.SOLUNAR_QUIET_HOURS_ENABLED] = quietHoursEnabled
            if (quietHours != null) prefs[Keys.SOLUNAR_QUIET_HOURS] = quietHours
        }
    }

    suspend fun setExpiryNotificationConfig(
        enabled: Boolean? = null,
        quietHoursEnabled: Boolean? = null,
        quietHours: String? = null
    ) {
        context.dataStore.edit { prefs ->
            if (enabled != null) prefs[Keys.EXPIRY_REMINDERS_ENABLED] = enabled
            if (quietHoursEnabled != null) prefs[Keys.EXPIRY_QUIET_HOURS_ENABLED] = quietHoursEnabled
            if (quietHours != null) prefs[Keys.EXPIRY_QUIET_HOURS] = quietHours
        }
    }

    suspend fun setDailyBonusNotificationConfig(
        enabled: Boolean? = null,
        quietHoursEnabled: Boolean? = null,
        quietHours: String? = null
    ) {
        context.dataStore.edit { prefs ->
            if (enabled != null) prefs[Keys.DAILY_BONUS_NOTIFS_ENABLED] = enabled
            if (quietHoursEnabled != null) prefs[Keys.DAILY_BONUS_QUIET_HOURS_ENABLED] = quietHoursEnabled
            if (quietHours != null) prefs[Keys.DAILY_BONUS_QUIET_HOURS] = quietHours
        }
    }

    suspend fun setCustomAccentHex(hex: String) {
        context.dataStore.edit { prefs ->
            val clean = hex.trim()
            if (isEntitledProInPrefs(prefs) || clean.isEmpty()) {
                prefs[Keys.CUSTOM_ACCENT_HEX] = clean
                if (clean.isEmpty()) prefs.remove(Keys.PENDING_CUSTOM_ACCENT_HEX)
            } else {
                prefs[Keys.PENDING_CUSTOM_ACCENT_HEX] = clean
            }
        }
    }

    suspend fun setHomeTileStyle(style: String) {
        val valid = style.trim().uppercase().let { if (it in listOf("GLASS", "SOLID", "OUTLINE")) it else "GLASS" }
        context.dataStore.edit { prefs ->
            if (isEntitledProInPrefs(prefs) || valid == "GLASS") {
                prefs[Keys.HOME_TILE_STYLE] = valid
                if (valid == "GLASS") prefs.remove(Keys.PENDING_HOME_TILE_STYLE)
            } else {
                prefs[Keys.PENDING_HOME_TILE_STYLE] = valid
            }
        }
    }

    suspend fun setHeroCarouselStyle(style: String) {
        val valid = style.trim().uppercase().let { if (it in listOf("EDITORIAL", "COMPACT_PILL", "MINIMAL_SPLIT")) it else "EDITORIAL" }
        context.dataStore.edit { prefs ->
            if (isEntitledProInPrefs(prefs) || valid == "EDITORIAL") {
                prefs[Keys.HERO_CAROUSEL_STYLE] = valid
                if (valid == "EDITORIAL") prefs.remove(Keys.PENDING_HERO_CAROUSEL_STYLE)
            } else {
                prefs[Keys.PENDING_HERO_CAROUSEL_STYLE] = valid
            }
        }
    }

    suspend fun setIconSetStyle(style: String) {
        val valid = style.trim().uppercase().let { if (it in listOf("FILLED", "OUTLINED", "DUOTONE", "MINIMAL")) it else "FILLED" }
        context.dataStore.edit { prefs ->
            if (isEntitledProInPrefs(prefs) || valid == "FILLED") {
                prefs[Keys.ICON_SET_STYLE] = valid
                if (valid == "FILLED") prefs.remove(Keys.PENDING_ICON_SET_STYLE)
            } else {
                prefs[Keys.PENDING_ICON_SET_STYLE] = valid
            }
        }
    }

    suspend fun setOutdoorAlertsConfig(
        solunarMode: String? = null,
        solunarLeadMinutes: Int? = null,
        goldenHourEnabled: Boolean? = null,
        dailyWeatherEnabled: Boolean? = null,
        rainSoonEnabled: Boolean? = null
    ) {
        context.dataStore.edit { prefs ->
            if (solunarMode != null) {
                val m = solunarMode.trim().uppercase()
                prefs[Keys.SOLUNAR_ALERT_MODE] = if (m == "HUNTING") "HUNTING" else "FISHING"
            }
            if (solunarLeadMinutes != null) {
                prefs[Keys.SOLUNAR_ALERT_LEAD_MINS] = if (solunarLeadMinutes in listOf(15, 30, 60)) solunarLeadMinutes else 30
            }
            if (goldenHourEnabled != null) prefs[Keys.GOLDEN_HOUR_ALERTS_ENABLED] = goldenHourEnabled
            if (dailyWeatherEnabled != null) prefs[Keys.DAILY_WEATHER_SUMMARY_ENABLED] = dailyWeatherEnabled
            if (rainSoonEnabled != null) prefs[Keys.RAIN_SOON_ALERTS_ENABLED] = rainSoonEnabled
        }
    }

    suspend fun setManualLocation(lat: Double, lng: Double, useManual: Boolean, label: String = "Local Coordinates") {
        context.dataStore.edit { prefs ->
            prefs[Keys.MANUAL_LAT] = lat.coerceIn(-90.0, 90.0)
            prefs[Keys.MANUAL_LNG] = lng.coerceIn(-180.0, 180.0)
            prefs[Keys.USE_MANUAL_LOC] = useManual
            prefs[Keys.LOCATION_NAME] = label
        }
    }

    suspend fun exportSettingsToJson(includeCoins: Boolean): org.json.JSONObject {
        val current = preferencesFlow.first()
        return org.json.JSONObject().apply {
            put("themeMode", current.themeMode.name)
            put("selectedThemeName", current.selectedThemeName)
            put("launchAnimationMode", current.launchAnimationMode.name)
            put("selectedAppIconStyle", current.selectedAppIconStyle)
            put("textScale", current.textScale.toDouble())
            put("reduceGlassEffects", current.reduceGlassEffects)
            put("reduceMotion", current.reduceMotion)
            put("tapSoundsEnabled", current.tapSoundsEnabled)
            put("tapSoundVolume", current.tapSoundVolume.toDouble())
            put("scanBeepEnabled", current.scanBeepEnabled)
            put("hapticsEnabled", current.hapticsEnabled)
            put("hapticStrength", current.hapticStrength.name)
            put("selectedHomePreset", current.selectedHomePreset)
            put("homePresetDismissed", current.homePresetDismissed)
            put("enabledToolIdsCsv", current.enabledToolIdsCsv)
            put("orderedToolIdsCsv", current.orderedToolIdsCsv)
            put("orderedSectionsCsv", current.orderedSectionsCsv)
            put("outdoorsSegmentsCsv", current.outdoorsSegmentsCsv)
            put("scanMenuShortcutsCsv", current.scanMenuShortcutsCsv)
            put("favoriteToolIdsCsv", current.favoriteToolIdsCsv)
            put("homeBlockOrderCsv", current.homeBlockOrderCsv)
            put("disabledHomeBlocksCsv", current.disabledHomeBlocksCsv)
            put("homeTileSize", current.homeTileSize)
            put("homeLayoutColumns", current.homeLayoutColumns)
            put("sectionLayoutOverridesCsv", current.sectionLayoutOverridesCsv)
            put("showToolNames", current.showToolNames)
            put("outdoorInterestsCsv", current.outdoorInterestsCsv)
            put("customToolModesConfigCsv", current.customToolModesConfigCsv)
            put("unitSystem", current.unitSystem.name)
            put("distanceUnit", current.distanceUnit)
            put("speedUnit", current.speedUnit)
            put("temperatureUnit", current.temperatureUnit)
            put("pressureUnit", current.pressureUnit)
            put("use24HourTime", current.use24HourTime)
            put("defaultTimeZone", current.defaultTimeZone)
            put("defaultCurrency", current.defaultCurrency)
            put("manualLat", current.manualLat)
            put("manualLng", current.manualLng)
            put("useManualLocation", current.useManualLocation)
            put("locationName", current.locationName)
            put("solunarAlertsEnabled", current.solunarAlertsEnabled)
            put("solunarQuietHoursEnabled", current.solunarQuietHoursEnabled)
            put("solunarQuietHours", current.solunarQuietHours)
            put("expiryRemindersEnabled", current.expiryRemindersEnabled)
            put("expiryQuietHoursEnabled", current.expiryQuietHoursEnabled)
            put("expiryQuietHours", current.expiryQuietHours)
            put("dailyBonusNotificationsEnabled", current.dailyBonusNotificationsEnabled)
            put("dailyBonusQuietHoursEnabled", current.dailyBonusQuietHoursEnabled)
            put("dailyBonusQuietHours", current.dailyBonusQuietHours)
            put("lockDocumentsWithBiometrics", current.lockDocumentsWithBiometrics)
            put("lockNotesWithBiometrics", current.lockNotesWithBiometrics)
            put("appLockEnabled", current.appLockEnabled)
            put("appLockMethod", current.appLockMethod)
            put("appLockOnLaunch", current.appLockOnLaunch)
            put("appLockTimeoutMinutes", current.appLockTimeoutMinutes)
            put("hideAppPreviewInRecents", current.hideAppPreviewInRecents)
            put("ownedSoundPacksCsv", current.ownedSoundPacksCsv)
            put("frequencyPresetsCsv", current.frequencyPresetsCsv)
            put("customAccentHex", current.customAccentHex)
            put("homeTileStyle", current.homeTileStyle)
            put("heroCarouselStyle", current.heroCarouselStyle)
            put("iconSetStyle", current.iconSetStyle)
            put("solunarAlertMode", current.solunarAlertMode)
            put("solunarAlertLeadMinutes", current.solunarAlertLeadMinutes)
            put("goldenHourAlertsEnabled", current.goldenHourAlertsEnabled)
            put("dailyWeatherSummaryEnabled", current.dailyWeatherSummaryEnabled)
            put("rainSoonAlertsEnabled", current.rainSoonAlertsEnabled)
            put("customSectionsJson", current.customSectionsJson)
            put("hiddenFeatureIdsCsv", current.hiddenFeatureIdsCsv)
            put("perTileSizeCsv", current.perTileSizeCsv)
            put("pinnedTopFeatureIdsCsv", current.pinnedTopFeatureIdsCsv)
            put("startTabIndex", current.startTabIndex)
            put("homeFirstFeatureId", current.homeFirstFeatureId)
            put("savedHomeLayoutsJson", current.savedHomeLayoutsJson)
            if (includeCoins) {
                put("coinBalance", current.coinBalance)
                put("checkInStreak", current.checkInStreak)
                put("lastCheckInDate", current.lastCheckInDate)
                put("bonusDocumentSaves", current.bonusDocumentSaves)
                put("bonusCsvExports", current.bonusCsvExports)
                put("featureUnlocksCsv", current.featureUnlocksCsv)
                put("rewardedToolUnlocksCsv", current.rewardedToolUnlocksCsv)
                put("rewardedThemeUnlocksCsv", current.rewardedThemeUnlocksCsv)
            }
        }
    }

    suspend fun restoreSettingsFromJson(json: org.json.JSONObject, includeCoins: Boolean) {
        context.dataStore.edit { prefs ->
            if (json.has("themeMode")) prefs[Keys.THEME_MODE] = json.optString("themeMode", ThemeMode.SYSTEM.name)
            if (json.has("selectedThemeName")) prefs[Keys.SELECTED_THEME_NAME] = json.optString("selectedThemeName", "Lime Night")
            if (json.has("launchAnimationMode")) prefs[Keys.LAUNCH_ANIMATION_MODE] = json.optString("launchAnimationMode", LaunchAnimationMode.RANDOM.name)
            if (json.has("selectedAppIconStyle")) prefs[Keys.SELECTED_APP_ICON_STYLE] = json.optString("selectedAppIconStyle", "Orange")
            if (json.has("textScale")) prefs[Keys.TEXT_SCALE] = json.optDouble("textScale", 1.0).toFloat().coerceIn(0.85f, 1.25f)
            if (json.has("reduceGlassEffects")) prefs[Keys.REDUCE_GLASS] = json.optBoolean("reduceGlassEffects", false)
            if (json.has("reduceMotion")) prefs[Keys.REDUCE_MOTION] = json.optBoolean("reduceMotion", false)
            if (json.has("tapSoundsEnabled")) prefs[Keys.TAP_SOUNDS_ENABLED] = json.optBoolean("tapSoundsEnabled", true)
            if (json.has("tapSoundVolume")) prefs[Keys.TAP_SOUND_VOLUME] = json.optDouble("tapSoundVolume", 0.8).toFloat().coerceIn(0.05f, 1.0f)
            if (json.has("scanBeepEnabled")) prefs[Keys.SCAN_BEEP_ENABLED] = json.optBoolean("scanBeepEnabled", true)
            if (json.has("hapticsEnabled")) prefs[Keys.HAPTICS_ENABLED] = json.optBoolean("hapticsEnabled", true)
            if (json.has("hapticStrength")) prefs[Keys.HAPTIC_STRENGTH] = json.optString("hapticStrength", HapticStrength.MEDIUM.name)
            if (json.has("selectedHomePreset")) prefs[Keys.SELECTED_HOME_PRESET] = json.optString("selectedHomePreset", "")
            if (json.has("homePresetDismissed")) prefs[Keys.HOME_PRESET_DISMISSED] = json.optBoolean("homePresetDismissed", false)
            if (json.has("enabledToolIdsCsv")) prefs[Keys.ENABLED_TOOL_IDS_CSV] = json.optString("enabledToolIdsCsv", "")
            if (json.has("orderedToolIdsCsv")) prefs[Keys.ORDERED_TOOL_IDS_CSV] = json.optString("orderedToolIdsCsv", "")
            if (json.has("orderedSectionsCsv")) prefs[Keys.ORDERED_SECTIONS_CSV] = json.optString("orderedSectionsCsv", "")
            if (json.has("outdoorsSegmentsCsv")) prefs[Keys.OUTDOORS_SEGMENTS_CSV] = json.optString("outdoorsSegmentsCsv", "")
            if (json.has("scanMenuShortcutsCsv")) prefs[Keys.SCAN_SHORTCUTS_CSV] = json.optString("scanMenuShortcutsCsv", "")
            if (json.has("favoriteToolIdsCsv")) prefs[Keys.FAVORITE_TOOL_IDS_CSV] = json.optString("favoriteToolIdsCsv", "")
            if (json.has("homeBlockOrderCsv")) prefs[Keys.HOME_BLOCK_ORDER_CSV] = json.optString("homeBlockOrderCsv", "")
            if (json.has("disabledHomeBlocksCsv")) prefs[Keys.DISABLED_HOME_BLOCKS_CSV] = json.optString("disabledHomeBlocksCsv", "")
            if (json.has("homeTileSize")) prefs[Keys.HOME_TILE_SIZE] = json.optString("homeTileSize", "M")
            if (json.has("homeLayoutColumns")) prefs[Keys.HOME_LAYOUT_COLUMNS] = json.optString("homeLayoutColumns", "3")
            if (json.has("sectionLayoutOverridesCsv")) prefs[Keys.SECTION_LAYOUT_OVERRIDES_CSV] = json.optString("sectionLayoutOverridesCsv", "")
            if (json.has("showToolNames")) prefs[Keys.SHOW_TOOL_NAMES] = json.optBoolean("showToolNames", true)
            if (json.has("outdoorInterestsCsv")) prefs[Keys.OUTDOOR_INTERESTS_CSV] = json.optString("outdoorInterestsCsv", "FISHING,HUNTING,CAMPING_PHOTO")
            if (json.has("customToolModesConfigCsv")) prefs[Keys.CUSTOM_TOOL_MODES_CONFIG_CSV] = json.optString("customToolModesConfigCsv", "")
            if (json.has("unitSystem")) prefs[Keys.UNIT_SYSTEM] = json.optString("unitSystem", UnitSystem.METRIC.name)
            if (json.has("distanceUnit")) prefs[Keys.DISTANCE_UNIT] = json.optString("distanceUnit", "km / m")
            if (json.has("speedUnit")) prefs[Keys.SPEED_UNIT] = json.optString("speedUnit", "km/h")
            if (json.has("temperatureUnit")) prefs[Keys.TEMPERATURE_UNIT] = json.optString("temperatureUnit", "°C")
            if (json.has("pressureUnit")) prefs[Keys.PRESSURE_UNIT] = json.optString("pressureUnit", "hPa")
            if (json.has("use24HourTime")) prefs[Keys.USE_24_HOUR] = json.optBoolean("use24HourTime", true)
            if (json.has("defaultTimeZone")) prefs[Keys.DEFAULT_TIME_ZONE] = json.optString("defaultTimeZone", "System Default")
            if (json.has("defaultCurrency")) prefs[Keys.DEFAULT_CURRENCY] = json.optString("defaultCurrency", "USD")
            if (json.has("manualLat")) prefs[Keys.MANUAL_LAT] = json.optDouble("manualLat", 37.7749)
            if (json.has("manualLng")) prefs[Keys.MANUAL_LNG] = json.optDouble("manualLng", -122.4194)
            if (json.has("useManualLocation")) prefs[Keys.USE_MANUAL_LOC] = json.optBoolean("useManualLocation", false)
            if (json.has("locationName")) prefs[Keys.LOCATION_NAME] = json.optString("locationName", "Local Coordinates")
            if (json.has("solunarAlertsEnabled")) prefs[Keys.SOLUNAR_ALERTS_ENABLED] = json.optBoolean("solunarAlertsEnabled", true)
            if (json.has("solunarQuietHoursEnabled")) prefs[Keys.SOLUNAR_QUIET_HOURS_ENABLED] = json.optBoolean("solunarQuietHoursEnabled", true)
            if (json.has("solunarQuietHours")) prefs[Keys.SOLUNAR_QUIET_HOURS] = json.optString("solunarQuietHours", "22:00 - 06:00")
            if (json.has("expiryRemindersEnabled")) prefs[Keys.EXPIRY_REMINDERS_ENABLED] = json.optBoolean("expiryRemindersEnabled", true)
            if (json.has("expiryQuietHoursEnabled")) prefs[Keys.EXPIRY_QUIET_HOURS_ENABLED] = json.optBoolean("expiryQuietHoursEnabled", true)
            if (json.has("expiryQuietHours")) prefs[Keys.EXPIRY_QUIET_HOURS] = json.optString("expiryQuietHours", "22:00 - 07:00")
            if (json.has("dailyBonusNotificationsEnabled")) prefs[Keys.DAILY_BONUS_NOTIFS_ENABLED] = json.optBoolean("dailyBonusNotificationsEnabled", true)
            if (json.has("dailyBonusQuietHoursEnabled")) prefs[Keys.DAILY_BONUS_QUIET_HOURS_ENABLED] = json.optBoolean("dailyBonusQuietHoursEnabled", true)
            if (json.has("dailyBonusQuietHours")) prefs[Keys.DAILY_BONUS_QUIET_HOURS] = json.optString("dailyBonusQuietHours", "21:00 - 08:00")
            if (json.has("lockDocumentsWithBiometrics")) prefs[Keys.LOCK_DOCS_BIOMETRICS] = json.optBoolean("lockDocumentsWithBiometrics", false)
            if (json.has("lockNotesWithBiometrics")) prefs[Keys.LOCK_NOTES_BIOMETRICS] = json.optBoolean("lockNotesWithBiometrics", false)
            if (json.has("appLockEnabled")) prefs[Keys.APP_LOCK_ENABLED] = json.optBoolean("appLockEnabled", false)
            if (json.has("appLockMethod")) prefs[Keys.APP_LOCK_METHOD] = json.optString("appLockMethod", "BIOMETRIC_OR_PIN")
            if (json.has("appLockOnLaunch")) prefs[Keys.APP_LOCK_ON_LAUNCH] = json.optBoolean("appLockOnLaunch", true)
            if (json.has("appLockTimeoutMinutes")) prefs[Keys.APP_LOCK_TIMEOUT_MINUTES] = json.optInt("appLockTimeoutMinutes", 5)
            if (json.has("hideAppPreviewInRecents")) prefs[Keys.HIDE_APP_PREVIEW_RECENTS] = json.optBoolean("hideAppPreviewInRecents", false)
            if (json.has("ownedSoundPacksCsv")) prefs[Keys.OWNED_SOUND_PACKS_CSV] = json.optString("ownedSoundPacksCsv", "")
            if (json.has("frequencyPresetsCsv")) prefs[Keys.FREQUENCY_PRESETS_CSV] = json.optString("frequencyPresetsCsv", "")
            if (json.has("customAccentHex")) prefs[Keys.CUSTOM_ACCENT_HEX] = json.optString("customAccentHex", "")
            if (json.has("homeTileStyle")) prefs[Keys.HOME_TILE_STYLE] = json.optString("homeTileStyle", "GLASS")
            if (json.has("heroCarouselStyle")) prefs[Keys.HERO_CAROUSEL_STYLE] = json.optString("heroCarouselStyle", "EDITORIAL")
            if (json.has("iconSetStyle")) prefs[Keys.ICON_SET_STYLE] = json.optString("iconSetStyle", "FILLED")
            if (json.has("solunarAlertMode")) prefs[Keys.SOLUNAR_ALERT_MODE] = json.optString("solunarAlertMode", "FISHING")
            if (json.has("solunarAlertLeadMinutes")) prefs[Keys.SOLUNAR_ALERT_LEAD_MINS] = json.optInt("solunarAlertLeadMinutes", 30)
            if (json.has("goldenHourAlertsEnabled")) prefs[Keys.GOLDEN_HOUR_ALERTS_ENABLED] = json.optBoolean("goldenHourAlertsEnabled", false)
            if (json.has("dailyWeatherSummaryEnabled")) prefs[Keys.DAILY_WEATHER_SUMMARY_ENABLED] = json.optBoolean("dailyWeatherSummaryEnabled", false)
            if (json.has("rainSoonAlertsEnabled")) prefs[Keys.RAIN_SOON_ALERTS_ENABLED] = json.optBoolean("rainSoonAlertsEnabled", false)
            if (json.has("customSectionsJson")) prefs[Keys.CUSTOM_SECTIONS_JSON] = json.optString("customSectionsJson", "")
            if (json.has("hiddenFeatureIdsCsv")) prefs[Keys.HIDDEN_FEATURE_IDS_CSV] = json.optString("hiddenFeatureIdsCsv", "")
            if (json.has("perTileSizeCsv")) prefs[Keys.PER_TILE_SIZE_CSV] = json.optString("perTileSizeCsv", "")
            if (json.has("pinnedTopFeatureIdsCsv")) prefs[Keys.PINNED_TOP_FEATURE_IDS_CSV] = json.optString("pinnedTopFeatureIdsCsv", "")
            if (json.has("startTabIndex")) prefs[Keys.START_TAB_INDEX] = json.optInt("startTabIndex", 0).coerceIn(0, 3)
            if (json.has("homeFirstFeatureId")) prefs[Keys.HOME_FIRST_FEATURE_ID] = json.optString("homeFirstFeatureId", "")
            if (json.has("savedHomeLayoutsJson")) prefs[Keys.SAVED_HOME_LAYOUTS_JSON] = json.optString("savedHomeLayoutsJson", "")
            if (includeCoins) {
                if (json.has("coinBalance")) prefs[Keys.COIN_BALANCE] = json.optInt("coinBalance", 0).coerceAtLeast(0)
                if (json.has("checkInStreak")) prefs[Keys.CHECK_IN_STREAK] = json.optInt("checkInStreak", 0).coerceIn(0, 7)
                if (json.has("lastCheckInDate")) prefs[Keys.LAST_CHECK_IN_DATE] = json.optString("lastCheckInDate", "")
                if (json.has("bonusDocumentSaves")) prefs[Keys.BONUS_DOCUMENT_SAVES] = json.optInt("bonusDocumentSaves", 0)
                if (json.has("bonusCsvExports")) prefs[Keys.BONUS_CSV_EXPORTS] = json.optInt("bonusCsvExports", 0)
                if (json.has("featureUnlocksCsv")) prefs[Keys.FEATURE_UNLOCKS_CSV] = json.optString("featureUnlocksCsv", "")
                if (json.has("rewardedToolUnlocksCsv")) prefs[Keys.REWARDED_TOOL_UNLOCKS_CSV] = json.optString("rewardedToolUnlocksCsv", "")
                if (json.has("rewardedThemeUnlocksCsv")) prefs[Keys.REWARDED_THEME_UNLOCKS_CSV] = json.optString("rewardedThemeUnlocksCsv", "")
            }
        }
    }

    suspend fun setSimulateProEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SIMULATE_PRO_ENABLED] = enabled
            if (enabled) {
                promotePendingCustomizationsInPrefs(prefs)
            }
        }
    }

    suspend fun ensureFirstSessionStartRecorded(nowMs: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FIRST_SESSION_START_MS] ?: 0L
            if (current <= 0L) {
                prefs[Keys.FIRST_SESSION_START_MS] = nowMs
            }
        }
    }

    suspend fun recordInterstitialShown(nowMs: Long, dateKey: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_INTERSTITIAL_MS] = nowMs
            val savedDate = prefs[Keys.INTERSTITIAL_DAILY_DATE] ?: ""
            val currentCount = if (savedDate == dateKey) (prefs[Keys.INTERSTITIAL_DAILY_COUNT] ?: 0) else 0
            prefs[Keys.INTERSTITIAL_DAILY_DATE] = dateKey
            prefs[Keys.INTERSTITIAL_DAILY_COUNT] = currentCount + 1
        }
    }

    suspend fun recordAppOpenShown(nowMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_APP_OPEN_MS] = nowMs
        }
    }

    suspend fun resetAdFrequencyCaps() {
        context.dataStore.edit { prefs ->
            prefs[Keys.FIRST_SESSION_START_MS] = System.currentTimeMillis() - 10 * 60 * 1000L
            prefs[Keys.LAST_INTERSTITIAL_MS] = 0L
            prefs[Keys.INTERSTITIAL_DAILY_DATE] = ""
            prefs[Keys.INTERSTITIAL_DAILY_COUNT] = 0
            prefs[Keys.LAST_APP_OPEN_MS] = 0L
        }
    }

    suspend fun resetAllPreferencesToDefault(preserveProState: Boolean = false) {
        context.dataStore.edit { prefs ->
            val savedPro = prefs[Keys.CACHED_IS_PRO] ?: false
            val savedSimPro = prefs[Keys.SIMULATE_PRO_ENABLED] ?: false
            prefs.clear()
            if (preserveProState) {
                prefs[Keys.CACHED_IS_PRO] = savedPro
                prefs[Keys.SIMULATE_PRO_ENABLED] = savedSimPro
            }
        }
    }

    /**
     * Unlocks ONLY [featureKey] for 1 hour (UserPreferences.ONE_HOUR_MS).
     * Never writes shared toolId or enum keys. Persists absolute wall expiry + elapsedRealtime baseline.
     */
    suspend fun unlockFeatureFor1Hour(
        featureKey: String,
        toolId: String? = null,
        themeName: String? = null,
        durationMs: Long = UserPreferences.ONE_HOUR_MS,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): Long {
        val expiry = nowMs + durationMs
        val targetKey = when {
            !themeName.isNullOrBlank() -> com.example.billing.AccessManager.themeKey(themeName)
            else -> com.example.billing.AccessManager.normalizeKey(featureKey)
        }
        context.dataStore.edit { prefs ->
            prefs[Keys.CLOCK_ROLLBACK_INVALIDATED] = false
            val prevLastWall = prefs[Keys.LAST_OBSERVED_WALL_MS] ?: 0L
            prefs[Keys.LAST_OBSERVED_WALL_MS] = maxOf(prevLastWall, nowMs)
            prefs[Keys.LAST_OBSERVED_ELAPSED_MS] = nowElapsedMs

            val records = com.example.billing.AccessManager.parseRecords(
                serialized = prefs[Keys.ACCESS_UNLOCK_RECORDS] ?: "",
                fallbackCsv = prefs[Keys.FEATURE_UNLOCKS_CSV] ?: ""
            ).toMutableMap()

            records[targetKey] = com.example.billing.AccessUnlockRecord(
                key = targetKey,
                expiryWallMs = expiry,
                grantWallMs = nowMs,
                grantElapsedMs = nowElapsedMs
            )

            prefs[Keys.ACCESS_UNLOCK_RECORDS] = com.example.billing.AccessManager.serializeRecords(records)
            prefs[Keys.FEATURE_UNLOCKS_CSV] = records.values.joinToString(",") { "${it.key}:${it.expiryWallMs}" }
            prefs[Keys.REWARDED_TOOL_UNLOCKS_CSV] = ""

            if (targetKey.startsWith("theme:") || targetKey == com.example.billing.AccessManager.KEY_CUSTOMIZE) {
                prefs[Keys.HAD_ACTIVE_LOOK_UNLOCK_SESSION] = true
            }
        }
        return expiry
    }

    suspend fun unlockToolFor1Hour(
        toolId: String,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): Long {
        return unlockFeatureFor1Hour(featureKey = toolId, nowMs = nowMs, nowElapsedMs = nowElapsedMs)
    }

    suspend fun unlockThemeFor1Hour(
        themeName: String,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): Long {
        return unlockFeatureFor1Hour(
            featureKey = com.example.billing.AccessManager.themeKey(themeName),
            themeName = themeName,
            nowMs = nowMs,
            nowElapsedMs = nowElapsedMs
        )
    }

    /**
     * Evaluates clock rollback (> 5 minutes backwards) and purges expired temporary unlocks.
     * Returns `true` if an active 1-hour theme or customization unlock just ended so the UI
     * can display the one-time snackbar "Your 1-hour unlock ended - default look restored".
     */
    suspend fun verifyClockAndEvaluateExpiries(
        isPro: Boolean,
        nowWallMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = com.example.billing.AccessManager.safeElapsedRealtime()
    ): Boolean {
        var lookUnlockJustEnded = false
        context.dataStore.edit { prefs ->
            val entitledPro = isPro || isEntitledProInPrefs(prefs)
            val lastWall = prefs[Keys.LAST_OBSERVED_WALL_MS] ?: 0L
            val records = com.example.billing.AccessManager.parseRecords(
                serialized = prefs[Keys.ACCESS_UNLOCK_RECORDS] ?: "",
                fallbackCsv = prefs[Keys.FEATURE_UNLOCKS_CSV] ?: ""
            )

            val rolledBack = (lastWall > 0L && nowWallMs < lastWall - com.example.billing.AccessManager.CLOCK_ROLLBACK_TOLERANCE_MS) ||
                records.values.any { it.grantWallMs > 0L && nowWallMs < it.grantWallMs - com.example.billing.AccessManager.CLOCK_ROLLBACK_TOLERANCE_MS }

            if (rolledBack) {
                // Device clock moved backwards by more than 5 minutes: treat every unlock as expired!
                prefs[Keys.CLOCK_ROLLBACK_INVALIDATED] = true
                prefs[Keys.ACCESS_UNLOCK_RECORDS] = ""
                prefs[Keys.FEATURE_UNLOCKS_CSV] = ""
                prefs[Keys.REWARDED_TOOL_UNLOCKS_CSV] = ""
                prefs[Keys.REWARDED_THEME_UNLOCKS_CSV] = ""
                prefs[Keys.LAST_OBSERVED_WALL_MS] = nowWallMs
                prefs[Keys.LAST_OBSERVED_ELAPSED_MS] = nowElapsedMs
                if (!entitledPro && (prefs[Keys.HAD_ACTIVE_LOOK_UNLOCK_SESSION] == true)) {
                    prefs[Keys.HAD_ACTIVE_LOOK_UNLOCK_SESSION] = false
                    lookUnlockJustEnded = true
                }
            } else {
                prefs[Keys.LAST_OBSERVED_WALL_MS] = maxOf(lastWall, nowWallMs)
                prefs[Keys.LAST_OBSERVED_ELAPSED_MS] = nowElapsedMs

                val surviving = records.filterValues { rec ->
                    !rec.isExpired(nowWallMs, nowElapsedMs, lastWall)
                }
                if (surviving.size != records.size) {
                    prefs[Keys.ACCESS_UNLOCK_RECORDS] = com.example.billing.AccessManager.serializeRecords(surviving)
                    prefs[Keys.FEATURE_UNLOCKS_CSV] = surviving.values.joinToString(",") { "${it.key}:${it.expiryWallMs}" }
                }

                val hasActiveLookUnlock = surviving.keys.any {
                    it.startsWith("theme:") || it == com.example.billing.AccessManager.KEY_CUSTOMIZE
                }
                if (!entitledPro && (prefs[Keys.HAD_ACTIVE_LOOK_UNLOCK_SESSION] == true) && !hasActiveLookUnlock) {
                    prefs[Keys.HAD_ACTIVE_LOOK_UNLOCK_SESSION] = false
                    lookUnlockJustEnded = true
                }
            }
        }
        return lookUnlockJustEnded
    }

    suspend fun grantOneExtraDocumentSave() {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.BONUS_DOCUMENT_SAVES] ?: 0
            prefs[Keys.BONUS_DOCUMENT_SAVES] = current + 1
        }
    }

    suspend fun grantOneExtraCsvExport() {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.BONUS_CSV_EXPORTS] ?: 0
            prefs[Keys.BONUS_CSV_EXPORTS] = current + 1
        }
    }

    suspend fun consumeOneBonusCsvExport(): Boolean {
        var consumed = false
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.BONUS_CSV_EXPORTS] ?: 0
            if (current > 0) {
                prefs[Keys.BONUS_CSV_EXPORTS] = current - 1
                consumed = true
                }
        }
        return consumed
    }

    /**
     * Awards +10 coins for a completed RewardedAd, enforcing 60s cooldown and max 12/day.
     * Returns Pair(granted: Boolean, newBalanceOrReason: Pair<Int, String>).
     */
    suspend fun claimRewardedAdCoins(
        nowMs: Long = System.currentTimeMillis(),
        todayKey: String = java.time.LocalDate.now().toString()
    ): Pair<Boolean, Pair<Int, String>> {
        var granted = false
        var finalBalance = 0
        var message = ""
        context.dataStore.edit { prefs ->
            val lastAdMs = prefs[Keys.LAST_AD_COIN_MS] ?: 0L
            val savedDate = prefs[Keys.AD_COIN_DAILY_DATE] ?: ""
            val countToday = if (savedDate == todayKey) (prefs[Keys.AD_COIN_DAILY_COUNT] ?: 0) else 0
            val currentBal = (prefs[Keys.COIN_BALANCE] ?: 0).coerceAtLeast(0)

            val elapsedSinceLast = nowMs - lastAdMs
            when {
                lastAdMs > 0L && elapsedSinceLast < UserPreferences.AD_COIN_COOLDOWN_MS -> {
                    val waitSec = ((UserPreferences.AD_COIN_COOLDOWN_MS - elapsedSinceLast) / 1000L).coerceAtLeast(1L)
                    finalBalance = currentBal
                    message = "Wait ${waitSec}s before earning coins from another ad"
                }
                countToday >= UserPreferences.AD_COIN_MAX_PER_DAY -> {
                    finalBalance = currentBal
                    message = "Daily limit reached (${UserPreferences.AD_COIN_MAX_PER_DAY}/day)"
                }
                else -> {
                    granted = true
                    finalBalance = currentBal + 10
                    prefs[Keys.COIN_BALANCE] = finalBalance
                    prefs[Keys.LAST_AD_COIN_MS] = nowMs
                    prefs[Keys.AD_COIN_DAILY_DATE] = todayKey
                    prefs[Keys.AD_COIN_DAILY_COUNT] = countToday + 1
                    message = "+10 coins earned (${countToday + 1}/${UserPreferences.AD_COIN_MAX_PER_DAY} today)"
                }
            }
        }
        return granted to (finalBalance to message)
    }

    /**
     * Claims the daily check-in reward:
     * +5 coins on Days 1..6, +25 coins on Day 7 of a consecutive 7-day streak.
     * Returns Triple(claimed, deltaAwarded, newBalance).
     */
    suspend fun claimDailyCheckIn(
        todayDate: java.time.LocalDate = java.time.LocalDate.now()
    ): Triple<Boolean, Int, Int> {
        val todayStr = todayDate.toString()
        val yesterdayStr = todayDate.minusDays(1).toString()
        var claimed = false
        var rewardDelta = 0
        var updatedBalance = 0

        context.dataStore.edit { prefs ->
            val lastDate = prefs[Keys.LAST_CHECK_IN_DATE] ?: ""
            val prevStreak = (prefs[Keys.CHECK_IN_STREAK] ?: 0).coerceIn(0, 7)
            val currentBal = (prefs[Keys.COIN_BALANCE] ?: 0).coerceAtLeast(0)

            if (lastDate == todayStr) {
                claimed = false
                rewardDelta = 0
                updatedBalance = currentBal
            } else {
                val nextStreak = if (lastDate == yesterdayStr) {
                    if (prevStreak >= 7) 1 else prevStreak + 1
                } else {
                    1
                }
                rewardDelta = if (nextStreak == 7) 25 else 5
                updatedBalance = currentBal + rewardDelta
                prefs[Keys.COIN_BALANCE] = updatedBalance
                prefs[Keys.LAST_CHECK_IN_DATE] = todayStr
                prefs[Keys.CHECK_IN_STREAK] = nextStreak
                claimed = true
            }
        }
        return Triple(claimed, rewardDelta, updatedBalance)
    }

    /**
     * Spends [cost] coins if the user's balance is sufficient.
     * Returns Pair(spent: Boolean, newBalance: Int).
     */
    suspend fun spendCoins(cost: Int): Pair<Boolean, Int> {
        var success = false
        var updatedBalance = 0
        context.dataStore.edit { prefs ->
            val currentBal = (prefs[Keys.COIN_BALANCE] ?: 0).coerceAtLeast(0)
            if (currentBal >= cost && cost > 0) {
                updatedBalance = currentBal - cost
                prefs[Keys.COIN_BALANCE] = updatedBalance
                success = true
            } else {
                updatedBalance = currentBal
                success = false
            }
        }
        return success to updatedBalance
    }

    /**
     * Used by the DEBUG "Pro debug" card's "Clear" button to reset simulated Pro and all 1h unlocks.
     */
    suspend fun clearProDebugStateAndUnlocks() {
        context.dataStore.edit { prefs ->
            prefs[Keys.SIMULATE_PRO_ENABLED] = false
            prefs[Keys.CACHED_IS_PRO] = false
            prefs[Keys.CACHED_PRO_SOURCE] = "None"
            prefs[Keys.ACCESS_UNLOCK_RECORDS] = ""
            prefs[Keys.FEATURE_UNLOCKS_CSV] = ""
            prefs[Keys.REWARDED_TOOL_UNLOCKS_CSV] = ""
            prefs[Keys.REWARDED_THEME_UNLOCKS_CSV] = ""
            prefs[Keys.CLOCK_ROLLBACK_INVALIDATED] = false
            prefs[Keys.HAD_ACTIVE_LOOK_UNLOCK_SESSION] = false
            prefs[Keys.BONUS_DOCUMENT_SAVES] = 0
            prefs[Keys.BONUS_CSV_EXPORTS] = 0
        }
    }

    suspend fun clearAllPreferences() {
        context.dataStore.edit { prefs ->
            val wasPro = prefs[Keys.CACHED_IS_PRO] ?: false
            val proSource = prefs[Keys.CACHED_PRO_SOURCE] ?: "None"
            prefs.clear()
            prefs[Keys.CACHED_IS_PRO] = wasPro
            prefs[Keys.CACHED_PRO_SOURCE] = proSource
        }
    }

    suspend fun setCachedRemoveAdsFromPlayBilling(removeAdsOwned: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CACHED_REMOVE_ADS] = removeAdsOwned
        }
    }

    /**
     * Records a timestamp when the user unlocks a Premium feature or quota use with an ad or coins,
     * keeping entries from the last 7 days for the Value Meter in Settings > Dyne Premium.
     */
    suspend fun recordAdOrCoinPremiumUse(nowMs: Long = System.currentTimeMillis()) {
        val sevenDaysAgo = nowMs - (7L * 24L * 60L * 60L * 1000L)
        context.dataStore.edit { prefs ->
            val currentCsv = prefs[Keys.WEEKLY_AD_COIN_USES_CSV] ?: ""
            val updated = currentCsv.split(",")
                .mapNotNull { it.trim().toLongOrNull() }
                .filter { it >= sevenDaysAgo }
                .toMutableList()
            updated.add(nowMs)
            prefs[Keys.WEEKLY_AD_COIN_USES_CSV] = updated.takeLast(200).joinToString(",")
        }
    }

    /**
     * Consumes 1 use of [quotaKey] for today (resetting automatically at local midnight).
     * Uses daily free allowance [dailyAllowance] first, then falls back to bonus +1 credits.
     * Returns true if allowed & consumed, false if at the limit.
     */
    suspend fun consumeQuotaUse(
        quotaKey: String,
        dailyAllowance: Int,
        todayDate: String = java.time.LocalDate.now().toString()
    ): Boolean {
        val keyUpper = quotaKey.trim().uppercase()
        var consumed = false
        context.dataStore.edit { prefs ->
            val savedDate = prefs[Keys.QUOTA_DAILY_DATE] ?: ""
            val countsMap = if (savedDate == todayDate) {
                UserPreferences.parseIntMap(prefs[Keys.QUOTA_COUNTS_CSV] ?: "").toMutableMap()
            } else {
                mutableMapOf()
            }
            val bonusMap = UserPreferences.parseIntMap(prefs[Keys.QUOTA_BONUS_CSV] ?: "").toMutableMap()

            val usedToday = countsMap[keyUpper] ?: 0
            val bonusAvailable = bonusMap[keyUpper] ?: 0

            if (usedToday < dailyAllowance) {
                countsMap[keyUpper] = usedToday + 1
                prefs[Keys.QUOTA_DAILY_DATE] = todayDate
                prefs[Keys.QUOTA_COUNTS_CSV] = countsMap.entries.joinToString(",") { "${it.key}:${it.value}" }
                if (keyUpper == "BACKGROUND_REMOVER") {
                    prefs[Keys.BG_REMOVER_DAILY_DATE] = todayDate
                    prefs[Keys.BG_REMOVER_DAILY_COUNT] = usedToday + 1
                }
                consumed = true
            } else if (bonusAvailable > 0) {
                val nextBonus = (bonusAvailable - 1).coerceAtLeast(0)
                if (nextBonus > 0) bonusMap[keyUpper] = nextBonus else bonusMap.remove(keyUpper)
                prefs[Keys.QUOTA_BONUS_CSV] = bonusMap.entries.joinToString(",") { "${it.key}:${it.value}" }
                if (keyUpper == "BACKGROUND_REMOVER") {
                    prefs[Keys.BG_REMOVER_BONUS_CREDITS] = nextBonus
                }
                consumed = true
            }
        }
        return consumed
    }

    /**
     * Grants +[delta] bonus use(s) for [quotaKey] (e.g. from watching a rewarded ad or spending 10 coins).
     */
    suspend fun grantQuotaBonusUse(quotaKey: String, delta: Int = 1) {
        val keyUpper = quotaKey.trim().uppercase()
        context.dataStore.edit { prefs ->
            val bonusMap = UserPreferences.parseIntMap(prefs[Keys.QUOTA_BONUS_CSV] ?: "").toMutableMap()
            val updated = ((bonusMap[keyUpper] ?: 0) + delta).coerceAtLeast(0)
            bonusMap[keyUpper] = updated
            prefs[Keys.QUOTA_BONUS_CSV] = bonusMap.entries.joinToString(",") { "${it.key}:${it.value}" }
            if (keyUpper == "BACKGROUND_REMOVER") {
                prefs[Keys.BG_REMOVER_BONUS_CREDITS] = updated
            }
        }
    }

    /**
     * Initializes or activates today's Daily Premium Spotlight tool.
     * Once a day, one random Premium tool is free for 1 hour (or 2 hours if 7-day streak is active).
     */
    suspend fun ensureOrActivateDailySpotlight(
        candidateToolIds: List<String>,
        activateNow: Boolean = false,
        nowMs: Long = System.currentTimeMillis(),
        todayDate: String = java.time.LocalDate.now().toString()
    ): Pair<String, Long> {
        var chosenToolId = ""
        var expiryMs = 0L
        context.dataStore.edit { prefs ->
            val savedDate = prefs[Keys.SPOTLIGHT_DAILY_DATE] ?: ""
            val streak = (prefs[Keys.CHECK_IN_STREAK] ?: 0).coerceIn(0, 7)
            val durationMs = if (streak >= 7) 2L * UserPreferences.ONE_HOUR_MS else UserPreferences.ONE_HOUR_MS

            if (savedDate != todayDate || (prefs[Keys.SPOTLIGHT_TOOL_ID] ?: "").isBlank()) {
                val pool = candidateToolIds.ifEmpty {
                    listOf("background_remover", "photo_editor", "scientific_calc", "fishing_log", "altimeter", "walk_run_tracker")
                }
                val seed = todayDate.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
                chosenToolId = pool[seed % pool.size]
                prefs[Keys.SPOTLIGHT_DAILY_DATE] = todayDate
                prefs[Keys.SPOTLIGHT_TOOL_ID] = chosenToolId
                if (activateNow) {
                    prefs[Keys.SPOTLIGHT_START_MS] = nowMs
                    expiryMs = nowMs + durationMs
                    prefs[Keys.SPOTLIGHT_EXPIRY_MS] = expiryMs
                } else {
                    prefs[Keys.SPOTLIGHT_START_MS] = 0L
                    prefs[Keys.SPOTLIGHT_EXPIRY_MS] = 0L
                    expiryMs = 0L
                }
            } else {
                chosenToolId = prefs[Keys.SPOTLIGHT_TOOL_ID] ?: "background_remover"
                val existingStart = prefs[Keys.SPOTLIGHT_START_MS] ?: 0L
                val existingExpiry = prefs[Keys.SPOTLIGHT_EXPIRY_MS] ?: 0L
                if (activateNow && existingStart == 0L) {
                    prefs[Keys.SPOTLIGHT_START_MS] = nowMs
                    expiryMs = nowMs + durationMs
                    prefs[Keys.SPOTLIGHT_EXPIRY_MS] = expiryMs
                } else {
                    expiryMs = existingExpiry
                }
            }
        }
        return chosenToolId to expiryMs
    }

    suspend fun recordSmartUpsellShown(nowMs: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SMART_UPSELL_MS] = nowMs
        }
    }

    suspend fun pauseSmartUpsellFor7Days(nowMs: Long = System.currentTimeMillis()) {
        val sevenDaysMs = 7L * 24L * 60L * 60L * 1000L
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SMART_UPSELL_MS] = nowMs
            prefs[Keys.SMART_UPSELL_PAUSED_UNTIL_MS] = nowMs + sevenDaysMs
        }
    }

    suspend fun incrementCompletedScanCount(): Int {
        var next = 1
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.COMPLETED_SCAN_COUNT] ?: 0
            next = current + 1
            prefs[Keys.COMPLETED_SCAN_COUNT] = next
        }
        return next
    }

    suspend fun setPhotosensitivityWarningShown(shown: Boolean = true) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PHOTOSENSITIVITY_WARNING_SHOWN] = shown
        }
    }

    companion object {
        const val AD_COIN_COOLDOWN_MS = UserPreferences.AD_COIN_COOLDOWN_MS
        const val AD_COIN_MAX_PER_DAY = UserPreferences.AD_COIN_MAX_PER_DAY
        const val COST_TOOL_1H = 10
        const val COST_THEME_1H = 15
        const val COST_PACK_1H = 10
        const val COST_EXTRA_DOC_SAVE = 10
        const val COST_CSV_EXPORT = 10
        const val COST_PDF_EXPORT = 10
        const val COST_QUOTA_PLUS_ONE = 10
    }
}
