package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.AccessManager
import com.example.billing.AccessUnlockRecord
import com.example.billing.FeatureGate
import com.example.billing.FeatureGuard
import com.example.billing.GooglePlayPurchaseManager
import com.example.data.PreferencesRepository
import com.example.data.UserPreferences
import com.example.domain.FeatureRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccessSystemVerificationTest {

    @Test
    fun `1_per_feature_unlock_isolation_and_print_table`() {
        val grantWallMs = 1_750_000_000_000L
        val grantElapsedMs = 50_000_000L
        val checkWallMs = grantWallMs + 10 * 60_000L // 10 minutes into 1h unlock
        val checkElapsedMs = grantElapsedMs + 10 * 60_000L

        // Grant 1-hour unlock ONLY for "flashlight_strobe"
        val strobeRecord = AccessUnlockRecord(
            key = "flashlight_strobe",
            expiryWallMs = grantWallMs + AccessManager.ONE_HOUR_MS,
            grantWallMs = grantWallMs,
            grantElapsedMs = grantElapsedMs
        )
        val prefsWithStrobeOnly = UserPreferences(
            accessUnlockRecordsSerialized = AccessManager.serializeRecords(mapOf("flashlight_strobe" to strobeRecord)),
            lastObservedWallMs = grantWallMs,
            lastObservedElapsedMs = grantElapsedMs
        )

        val probeKeys = listOf(
            "flashlight_strobe" to true,
            "flashlight_morse" to false,
            "flashlight_brightness" to false,
            "screen_colors" to false,
            "compass_sun_moon_direction" to false,
            "outdoor_sky_dome" to false,
            "outdoor_sun_moon" to false,
            "outdoor_solunar_7day" to false,
            "outdoor_weather_7day" to false,
            "outdoor_air_quality_uv" to false,
            "outdoor_planner_7day" to false,
            "theme:ocean" to false,
            "customize" to false
        )

        println("=== PER-FEATURE UNLOCK ISOLATION TEST TABLE (Active Unlock: flashlight_strobe) ===")
        println(String.format("%-30s | %-14s | %-12s | %-14s", "Feature Key", "Granted Key", "Expected", "Actual hasAccess"))
        println("-".repeat(78))

        for ((key, expectedAccess) in probeKeys) {
            val actualAccess = AccessManager.hasAccess(
                featureKey = key,
                isPremium = false,
                prefs = prefsWithStrobeOnly,
                nowWallMs = checkWallMs,
                nowElapsedMs = checkElapsedMs
            )
            println(
                String.format(
                    "%-30s | %-14s | %-12s | %-14s",
                    key,
                    "flashlight_strobe",
                    expectedAccess,
                    actualAccess
                )
            )
            assertEquals(
                "Unlocking 'flashlight_strobe' must NOT unlock '$key'",
                expectedAccess,
                actualAccess
            )
        }
        println("=".repeat(78))
    }

    @Test
    fun `2_clock_rollback_over_5_minutes_expires_all_unlocks`() {
        val grantWallMs = 1_750_000_000_000L
        val grantElapsedMs = 100_000_000L
        val record = AccessUnlockRecord(
            key = "flashlight_brightness",
            expiryWallMs = grantWallMs + AccessManager.ONE_HOUR_MS,
            grantWallMs = grantWallMs,
            grantElapsedMs = grantElapsedMs
        )
        val prefs = UserPreferences(
            accessUnlockRecordsSerialized = AccessManager.serializeRecords(mapOf("flashlight_brightness" to record)),
            lastObservedWallMs = grantWallMs + 10 * 60_000L,
            lastObservedElapsedMs = grantElapsedMs + 10 * 60_000L
        )

        // Normal progression (+12 min wall, +12 min elapsed) -> active
        assertTrue(
            AccessManager.hasAccess(
                featureKey = "flashlight_brightness",
                isPremium = false,
                prefs = prefs,
                nowWallMs = grantWallMs + 12 * 60_000L,
                nowElapsedMs = grantElapsedMs + 12 * 60_000L
            )
        )

        // Move device wall clock backwards by 6 minutes (> 5 min tolerance) while elapsed advances +1 min
        val rolledBackWallMs = (grantWallMs + 10 * 60_000L) - (6 * 60_000L)
        val normalNextElapsedMs = grantElapsedMs + 11 * 60_000L
        assertFalse(
            "Expected clock rollback > 5 minutes to invalidate unlock immediately",
            AccessManager.hasAccess(
                featureKey = "flashlight_brightness",
                isPremium = false,
                prefs = prefs,
                nowWallMs = rolledBackWallMs,
                nowElapsedMs = normalNextElapsedMs
            )
        )
    }

    @Test
    fun `3_audit_all_premium_features_require_hasAccess_in_FeatureGuard`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = PreferencesRepository(context)
        val purchaseManager = GooglePlayPurchaseManager(context, prefsRepo)
        purchaseManager.setSimulatePro(false)
        val featureGate = FeatureGate(purchaseManager, prefsRepo)

        // Verify Flashlight Brightness, Compass Sun & Moon Direction, and Outdoors sub-views are registered as Premium
        val requiredPremiumIds = listOf(
            "flashlight_brightness",
            "flashlight_strobe",
            "flashlight_morse",
            "screen_colors",
            "compass_sun_moon_direction",
            "outdoor_solunar_7day",
            "outdoor_sky_dome",
            "outdoor_sun_moon",
            "outdoor_sun_moon_calendar",
            "outdoor_weather_7day",
            "outdoor_air_quality_uv",
            "outdoor_planner_7day",
            "outdoor_alerts"
        )
        for (reqId in requiredPremiumIds) {
            val feat = FeatureRegistry.getFeatureById(reqId)
            assertNotNull("Expected '$reqId' in FeatureRegistry", feat)
            assertTrue("Expected '$reqId' to be marked isPremium = true", feat!!.isPremium)
        }

        // Verify Free Outdoors views are registered as Free
        assertFalse(FeatureRegistry.getFeatureById("outdoor_solunar_today")!!.isPremium)
        assertFalse(FeatureRegistry.getFeatureById("outdoor_weather_current")!!.isPremium)
        assertFalse(FeatureRegistry.getFeatureById("flashlight_normal")!!.isPremium)

        val lockedPrefs = UserPreferences()
        val nowWall = 1_750_000_000_000L
        val nowElapsed = 50_000_000L

        // Audit every Premium feature in FeatureRegistry: none may be reached when hasAccess == false
        val allPremium = FeatureRegistry.premiumFeatures
        assertTrue("Expected at least 30 Premium features in FeatureRegistry", allPremium.size >= 30)

        for (feature in allPremium) {
            var allowedReached = false
            var lockedSheetOpened = false
            val navigated = FeatureGuard.navigate(
                featureKey = feature.id,
                isPremium = false,
                prefs = lockedPrefs,
                nowWallMs = nowWall,
                nowElapsedMs = nowElapsed,
                onAllowed = { allowedReached = true },
                onLocked = { lockedSheetOpened = true }
            )
            assertFalse(
                "SECURITY AUDIT FAILED: Premium feature '${feature.id}' was reached without hasAccess!",
                navigated || allowedReached
            )
            assertTrue(
                "Expected Unlock sheet to open for locked Premium feature '${feature.id}'",
                lockedSheetOpened
            )

            // Also verify via FeatureGate.guardNavigate
            var gateAllowed = false
            val gateResult = featureGate.guardNavigate(
                featureKey = feature.id,
                prefs = lockedPrefs,
                nowMs = nowWall,
                nowElapsedMs = nowElapsed,
                onAllowed = { gateAllowed = true }
            )
            assertFalse("FeatureGate.guardNavigate allowed locked feature '${feature.id}'", gateResult || gateAllowed)
            assertNotNull("Expected activeUnlockRequest to be populated for '${feature.id}'", featureGate.activeUnlockRequest.value)
            assertEquals(feature.id, featureGate.activeUnlockRequest.value?.effectiveUnlockKey)
            featureGate.dismissUnlockSheet()
        }
    }

    @Test
    fun `4_theme_and_customization_temporary_unlock_expires_and_restores_default_look`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = PreferencesRepository(context)
        prefsRepo.resetAllPreferencesToDefault(preserveProState = false)

        val t0Wall = 1_750_000_000_000L
        val t0Elapsed = 80_000_000L

        // 1. Free user tries to set a Premium theme directly -> ignored, stays "Lime Night"
        prefsRepo.setSelectedThemeName("Ocean", forceEntitledPro = false)
        val initialPrefs = prefsRepo.preferencesFlow.first()
        assertEquals("Lime Night", initialPrefs.selectedThemeName)
        assertEquals("Lime Night", initialPrefs.effectiveThemeName(isPro = false, nowMs = t0Wall, nowElapsedMs = t0Elapsed))

        // 2. Grant 1-hour unlock for "theme:Ocean"
        prefsRepo.unlockFeatureFor1Hour(
            featureKey = AccessManager.themeKey("Ocean"),
            durationMs = AccessManager.ONE_HOUR_MS,
            nowMs = t0Wall,
            nowElapsedMs = t0Elapsed
        )
        val unlockedThemePrefs = prefsRepo.preferencesFlow.first()
        // Saved preference selectedThemeName MUST remain "Lime Night" (never written for temporary unlock)
        assertEquals("Lime Night", unlockedThemePrefs.selectedThemeName)
        // EffectiveTheme while 1h unlock is active = "Ocean"
        assertEquals(
            "Ocean",
            unlockedThemePrefs.effectiveThemeName(
                isPro = false,
                nowMs = t0Wall + 15 * 60_000L,
                nowElapsedMs = t0Elapsed + 15 * 60_000L
            )
        )

        // 3. Grant 1-hour unlock for "customize" and change tile size & accent color under temporary unlock
        prefsRepo.unlockFeatureFor1Hour(
            featureKey = AccessManager.KEY_CUSTOMIZE,
            durationMs = AccessManager.ONE_HOUR_MS,
            nowMs = t0Wall,
            nowElapsedMs = t0Elapsed
        )
        prefsRepo.setHomeTileSize("L")
        prefsRepo.setCustomAccentHex("#FF3B30")
        val unlockedCustomizePrefs = prefsRepo.preferencesFlow.first()
        // Permanent store untouched ("M" and ""), pending store holds "L" and "#FF3B30"
        assertEquals("M", unlockedCustomizePrefs.homeTileSize)
        assertEquals("L", unlockedCustomizePrefs.pendingHomeTileSize)
        assertEquals(
            "L",
            unlockedCustomizePrefs.effectiveHomeTileSize(
                isPro = false,
                nowMs = t0Wall + 20 * 60_000L,
                nowElapsedMs = t0Elapsed + 20 * 60_000L
            )
        )
        assertEquals(
            "#FF3B30",
            unlockedCustomizePrefs.effectiveCustomAccentHex(
                isPro = false,
                nowMs = t0Wall + 20 * 60_000L,
                nowElapsedMs = t0Elapsed + 20 * 60_000L
            )
        )

        // 4. Advance clock past 1 hour (+61 minutes), return to app / relaunch -> default theme & layout restored
        val expiredWall = t0Wall + 61 * 60_000L
        val expiredElapsed = t0Elapsed + 61 * 60_000L

        assertEquals(
            "Lime Night",
            unlockedCustomizePrefs.effectiveThemeName(
                isPro = false,
                nowMs = expiredWall,
                nowElapsedMs = expiredElapsed
            )
        )
        assertEquals(
            "M",
            unlockedCustomizePrefs.effectiveHomeTileSize(
                isPro = false,
                nowMs = expiredWall,
                nowElapsedMs = expiredElapsed
            )
        )
        assertEquals(
            "",
            unlockedCustomizePrefs.effectiveCustomAccentHex(
                isPro = false,
                nowMs = expiredWall,
                nowElapsedMs = expiredElapsed
            )
        )

        // Verify verifyClockAndEvaluateExpiries purges expired records and returns lookUnlockExpired = true
        val lookExpired = prefsRepo.verifyClockAndEvaluateExpiries(
            isPro = false,
            nowWallMs = expiredWall,
            nowElapsedMs = expiredElapsed
        )
        assertTrue("Expected verifyClockAndEvaluateExpiries to report look unlock expiry", lookExpired)

        val afterExpiryPrefs = prefsRepo.preferencesFlow.first()
        assertEquals("Lime Night", afterExpiryPrefs.effectiveThemeName(isPro = false, nowMs = expiredWall, nowElapsedMs = expiredElapsed))
        assertEquals("M", afterExpiryPrefs.effectiveHomeTileSize(isPro = false, nowMs = expiredWall, nowElapsedMs = expiredElapsed))

        // 5. If user later buys Premium, pending customizations are promoted and applied
        prefsRepo.setSimulateProEnabled(true)
        val proPrefs = prefsRepo.preferencesFlow.first()
        assertEquals("L", proPrefs.effectiveHomeTileSize(isPro = true, nowMs = expiredWall, nowElapsedMs = expiredElapsed))
        assertEquals("#FF3B30", proPrefs.effectiveCustomAccentHex(isPro = true, nowMs = expiredWall, nowElapsedMs = expiredElapsed))
        prefsRepo.setSimulateProEnabled(false)
    }
}
