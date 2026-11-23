package com.example.billing

import android.os.SystemClock
import com.example.data.PreferencesRepository
import com.example.data.UserPreferences
import com.example.domain.Feature
import com.example.domain.FeatureRegistry
import java.util.Locale

/**
 * Per-key 1-hour temporary unlock record persisted in DataStore.
 * Stores both the absolute wall-clock expiry ([expiryWallMs], [grantWallMs]) and
 * the monotonic [SystemClock.elapsedRealtime] baseline ([grantElapsedMs]).
 */
data class AccessUnlockRecord(
    val key: String,
    val expiryWallMs: Long,
    val grantWallMs: Long,
    val grantElapsedMs: Long
) {
    fun isExpired(
        nowWallMs: Long,
        nowElapsedMs: Long,
        lastObservedWallMs: Long
    ): Boolean {
        // 1. If device wall clock moved backwards by more than 5 minutes relative to either
        //    the grant timestamp or the latest observed wall clock, treat every unlock as expired.
        val baselineWallMs = maxOf(grantWallMs, lastObservedWallMs)
        if (baselineWallMs > 0L && nowWallMs < baselineWallMs - AccessManager.CLOCK_ROLLBACK_TOLERANCE_MS) {
            return true
        }
        // 2. Absolute wall-clock expiry check (continues counting down while app is closed or clock moves forward).
        if (nowWallMs >= expiryWallMs) {
            return true
        }
        // 3. Monotonic elapsedRealtime check within the same boot session.
        if (grantElapsedMs > 0L && nowElapsedMs >= grantElapsedMs) {
            val durationMs = (expiryWallMs - grantWallMs).coerceAtLeast(UserPreferences.ONE_HOUR_MS)
            if (nowElapsedMs - grantElapsedMs >= durationMs) {
                return true
            }
        }
        return false
    }

    fun remainingMs(
        nowWallMs: Long,
        nowElapsedMs: Long,
        lastObservedWallMs: Long
    ): Long {
        if (isExpired(nowWallMs, nowElapsedMs, lastObservedWallMs)) return 0L
        val wallRem = (expiryWallMs - nowWallMs).coerceAtLeast(0L)
        return if (grantElapsedMs > 0L && nowElapsedMs >= grantElapsedMs) {
            val durationMs = (expiryWallMs - grantWallMs).coerceAtLeast(UserPreferences.ONE_HOUR_MS)
            val elapsedRem = (durationMs - (nowElapsedMs - grantElapsedMs)).coerceAtLeast(0L)
            minOf(wallRem, elapsedRem)
        } else {
            wallRem
        }
    }
}

/**
 * Single source of truth for feature, theme, and customization access in Dyne.
 *
 * Rules:
 * - `hasAccess(featureKey) = isPremium OR unlockExpiry[featureKey] > now`.
 * - Keys are strictly per feature (`feature.id`), per theme (`"theme:<id>"`), and `"customize"`.
 * - An unlock grants ONLY its own key; there is no global unlock flag, no shared tool/enum timer,
 *   and no 10-second preview state.
 * - Expiries are persisted per key in DataStore as an absolute wall time plus an `elapsedRealtime`
 *   baseline; if the device clock moves backwards by more than 5 minutes, every unlock is treated
 *   as expired immediately.
 */
object AccessManager {
    const val ONE_HOUR_MS: Long = 60L * 60L * 1000L
    const val CLOCK_ROLLBACK_TOLERANCE_MS: Long = 5L * 60L * 1000L
    const val KEY_CUSTOMIZE: String = "customize"
    const val THEME_PREFIX: String = "theme:"
    const val DEFAULT_FREE_THEME: String = "Lime Night"

    fun safeElapsedRealtime(): Long {
        return runCatching { SystemClock.elapsedRealtime() }.getOrDefault(0L)
    }

    fun currentElapsedRealtimeMs(): Long = safeElapsedRealtime()

    /**
     * Normalizes a raw feature, theme, or customization key to its canonical form:
     * - `"customize"` for customization keys
     * - `"theme:<lowercase_name>"` for theme keys
     * - `<lowercase_feature_id>` for catalog feature keys
     */
    fun normalizeKey(rawKey: String): String {
        val cleaned = rawKey.trim()
            .removePrefix("feature/")
            .lowercase(Locale.US)
        return when (cleaned) {
            "customize",
            "home_customization",
            "customization_pro" -> KEY_CUSTOMIZE
            else -> cleaned
        }
    }

    fun themeKey(themeName: String): String {
        val clean = themeName.trim().removePrefix("theme:").lowercase(Locale.US)
        return "theme:$clean"
    }

    /**
     * Serializes a map of [AccessUnlockRecord] to a compact string for DataStore persistence.
     * Format per entry: `key|expiryWallMs|grantWallMs|grantElapsedMs` separated by `;`
     */
    fun serializeRecords(records: Map<String, AccessUnlockRecord>): String {
        if (records.isEmpty()) return ""
        return records.values.joinToString(";") { rec ->
            "${rec.key}|${rec.expiryWallMs}|${rec.grantWallMs}|${rec.grantElapsedMs}"
        }
    }

    /**
     * Parses serialized [AccessUnlockRecord] entries from DataStore, also supporting legacy `key:expiry` CSV entries.
     */
    fun parseRecords(
        serialized: String,
        fallbackCsv: String = ""
    ): Map<String, AccessUnlockRecord> {
        val result = linkedMapOf<String, AccessUnlockRecord>()
        if (serialized.isNotBlank()) {
            serialized.split(";").forEach { token ->
                val parts = token.split("|")
                if (parts.size >= 4) {
                    val key = normalizeKey(parts[0])
                    val expWall = parts[1].trim().toLongOrNull()
                    val grantWall = parts[2].trim().toLongOrNull()
                    val grantElapsed = parts[3].trim().toLongOrNull() ?: 0L
                    if (key.isNotEmpty() && expWall != null && grantWall != null) {
                        result[key] = AccessUnlockRecord(
                            key = key,
                            expiryWallMs = expWall,
                            grantWallMs = grantWall,
                            grantElapsedMs = grantElapsed
                        )
                    }
                }
            }
        }
        if (result.isEmpty() && fallbackCsv.isNotBlank()) {
            UserPreferences.parseUnlockMap(fallbackCsv).forEach { (rawK, expWall) ->
                val key = normalizeKey(rawK)
                result[key] = AccessUnlockRecord(
                    key = key,
                    expiryWallMs = expWall,
                    grantWallMs = expWall - ONE_HOUR_MS,
                    grantElapsedMs = 0L
                )
            }
        }
        return result
    }

    /**
     * Returns true if the device clock moved backwards by more than 5 minutes relative to
     * [lastObservedWallMs] or any stored record's [AccessUnlockRecord.grantWallMs].
     */
    fun isClockRolledBack(
        prefs: UserPreferences,
        nowWallMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (prefs.clockRollbackInvalidated) return true
        if (prefs.lastObservedWallMs > 0L &&
            nowWallMs < prefs.lastObservedWallMs - CLOCK_ROLLBACK_TOLERANCE_MS
        ) {
            return true
        }
        val records = parseRecords(prefs.accessUnlockRecordsSerialized, prefs.featureUnlocksCsv)
        return records.values.any { rec ->
            rec.grantWallMs > 0L && nowWallMs < rec.grantWallMs - CLOCK_ROLLBACK_TOLERANCE_MS
        }
    }

    /**
     * Evaluates whether [featureKey] is currently accessible:
     * `hasAccess(featureKey) = isPremium OR unlockExpiry[featureKey] > now`.
     *
     * - Free catalog features (`feature.isPremium == false`) and the default free theme (`"theme:lime night"`)
     *   always return `true`.
     * - Locked features, themes (`"theme:<id>"`), and `"customize"` require either [isPremium]
     *   or an active, non-expired per-key entry in `unlockExpiry[featureKey]`.
     * - Unlocking one key NEVER unlocks any other key.
     */
    fun hasAccess(
        featureKey: String,
        isPremium: Boolean = false,
        prefs: UserPreferences,
        nowWallMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = safeElapsedRealtime()
    ): Boolean {
        val key = normalizeKey(featureKey)
        if (key.isEmpty()) return false

        // Default free theme is always accessible
        if (key == themeKey(DEFAULT_FREE_THEME)) return true

        // Free catalog features are always accessible
        val catalogFeature = FeatureRegistry.getFeatureById(key)
        if (catalogFeature != null && !catalogFeature.isPremium) {
            return true
        }

        // Entitled Premium users have access to everything
        if (isPremium || prefs.cachedIsPro || prefs.simulateProEnabled) {
            return true
        }

        // If device clock moved backwards by more than 5 minutes, all temporary unlocks are expired
        if (isClockRolledBack(prefs, nowWallMs)) {
            return false
        }

        val records = parseRecords(prefs.accessUnlockRecordsSerialized, prefs.featureUnlocksCsv)
        val record = records[key] ?: return false
        return !record.isExpired(
            nowWallMs = nowWallMs,
            nowElapsedMs = nowElapsedMs,
            lastObservedWallMs = prefs.lastObservedWallMs
        )
    }

    /**
     * Returns the remaining milliseconds on [featureKey]'s 1-hour unlock, or 0L if not unlocked / expired.
     */
    fun getRemainingUnlockMs(
        featureKey: String,
        isPremium: Boolean = false,
        prefs: UserPreferences,
        nowWallMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = safeElapsedRealtime()
    ): Long {
        if (isPremium || prefs.cachedIsPro || prefs.simulateProEnabled) return 0L
        if (isClockRolledBack(prefs, nowWallMs)) return 0L
        val key = normalizeKey(featureKey)
        val records = parseRecords(prefs.accessUnlockRecordsSerialized, prefs.featureUnlocksCsv)
        val record = records[key] ?: return 0L
        return record.remainingMs(
            nowWallMs = nowWallMs,
            nowElapsedMs = nowElapsedMs,
            lastObservedWallMs = prefs.lastObservedWallMs
        )
    }

    /**
     * Returns the absolute wall-clock expiry timestamp for [featureKey], or null if not active.
     */
    fun getUnlockExpiryWallMs(
        featureKey: String,
        prefs: UserPreferences,
        nowWallMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = safeElapsedRealtime()
    ): Long? {
        if (isClockRolledBack(prefs, nowWallMs)) return null
        val key = normalizeKey(featureKey)
        val records = parseRecords(prefs.accessUnlockRecordsSerialized, prefs.featureUnlocksCsv)
        val record = records[key] ?: return null
        if (record.isExpired(nowWallMs, nowElapsedMs, prefs.lastObservedWallMs)) return null
        return record.expiryWallMs
    }

    /**
     * Returns all currently active per-key unlocks (`key -> expiryWallMs`).
     */
    fun getAllActiveUnlocks(
        prefs: UserPreferences,
        nowWallMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = safeElapsedRealtime()
    ): Map<String, Long> {
        if (isClockRolledBack(prefs, nowWallMs)) return emptyMap()
        val records = parseRecords(prefs.accessUnlockRecordsSerialized, prefs.featureUnlocksCsv)
        val active = linkedMapOf<String, Long>()
        records.forEach { (k, rec) ->
            if (!rec.isExpired(nowWallMs, nowElapsedMs, prefs.lastObservedWallMs)) {
                active[k] = rec.expiryWallMs
            }
        }
        return active
    }

    /**
     * Returns the active temporary theme name (e.g. "Ocean") if a `theme:<id>` key is currently unlocked,
     * or null if no temporary theme unlock is active.
     */
    fun getActiveTemporaryThemeName(
        prefs: UserPreferences,
        nowWallMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = safeElapsedRealtime()
    ): String? {
        if (isClockRolledBack(prefs, nowWallMs)) return null
        val records = parseRecords(prefs.accessUnlockRecordsSerialized, prefs.featureUnlocksCsv)
        val activeThemeRecord = records.values
            .filter { it.key.startsWith("theme:") && !it.isExpired(nowWallMs, nowElapsedMs, prefs.lastObservedWallMs) }
            .maxByOrNull { it.grantWallMs }
            ?: return null
        val rawThemeId = activeThemeRecord.key.removePrefix("theme:").trim()
        return canonicalThemeDisplayName(rawThemeId)
    }

    fun canonicalThemeDisplayName(rawThemeId: String): String {
        val knownThemes = listOf(
            "Lime Night",
            "Classic Light",
            "Ocean",
            "Cyberpunk",
            "Ember",
            "Arctic",
            "Forest",
            "Royal"
        )
        return knownThemes.firstOrNull { it.equals(rawThemeId.trim(), ignoreCase = true) }
            ?: rawThemeId.trim().split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { ch -> ch.uppercase(Locale.US) }
            }
    }
}

/**
 * Central navigation guard required for every feature entry point:
 * Home tiles, hero slides, Favorites, Recently used, Quick Access, search, All Features,
 * Scan menu, Daily Spotlight, pinned and dynamic launcher shortcuts, Quick Settings tiles,
 * widgets, notification deep links, Outdoors segments/sub-views, Compass sub-views,
 * and cold-start or `onNewIntent` intents.
 *
 * - If `hasAccess(featureKey)` is true (or the feature is Free), runs [onAllowed] / [onNavigate] immediately and returns `true`.
 * - If locked, opens the Unlock sheet for that exact `featureKey` and does NOT navigate (`returns false`).
 */
object FeatureGuard {

    /**
     * Callback-driven guarded navigation used by [FeatureGate.guardNavigate].
     */
    fun navigate(
        featureKey: String,
        hasAccess: (String) -> Boolean,
        onOpenUnlockSheet: (String) -> Unit,
        onNavigate: (String) -> Unit
    ): Boolean {
        val normalizedKey = AccessManager.normalizeKey(featureKey)
        val catalogFeature = FeatureRegistry.getFeatureById(normalizedKey)
            ?: FeatureRegistry.resolveFeatureOrPrimaryForTool(normalizedKey)
        val effectiveKey = catalogFeature?.id ?: normalizedKey

        val isPremiumTarget = when {
            effectiveKey == AccessManager.KEY_CUSTOMIZE -> true
            effectiveKey.startsWith(AccessManager.THEME_PREFIX) ->
                !effectiveKey.equals(AccessManager.themeKey(AccessManager.DEFAULT_FREE_THEME), ignoreCase = true)
            catalogFeature != null -> catalogFeature.isPremium
            else -> true
        }

        if (!isPremiumTarget || hasAccess(effectiveKey)) {
            onNavigate(effectiveKey)
            return true
        }

        onOpenUnlockSheet(effectiveKey)
        return false
    }

    /**
     * Direct guarded navigation check using [AccessManager.hasAccess] and [UserPreferences].
     */
    fun navigate(
        featureKey: String,
        isPremium: Boolean = false,
        prefs: UserPreferences = UserPreferences(),
        nowWallMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = AccessManager.safeElapsedRealtime(),
        onLocked: (String) -> Unit = {},
        onAllowed: (Feature?) -> Unit
    ): Boolean {
        val normalizedKey = AccessManager.normalizeKey(featureKey)
        val catalogFeature = FeatureRegistry.getFeatureById(normalizedKey)
            ?: FeatureRegistry.resolveFeatureOrPrimaryForTool(normalizedKey)
        val effectiveKey = catalogFeature?.id ?: normalizedKey

        val isPremiumTarget = when {
            effectiveKey == AccessManager.KEY_CUSTOMIZE -> true
            effectiveKey.startsWith(AccessManager.THEME_PREFIX) ->
                !effectiveKey.equals(AccessManager.themeKey(AccessManager.DEFAULT_FREE_THEME), ignoreCase = true)
            catalogFeature != null -> catalogFeature.isPremium
            else -> true
        }

        val entitledPro = isPremium || prefs.cachedIsPro || prefs.simulateProEnabled
        if (!isPremiumTarget || AccessManager.hasAccess(effectiveKey, entitledPro, prefs, nowWallMs, nowElapsedMs)) {
            onAllowed(catalogFeature)
            return true
        }

        onLocked(effectiveKey)
        return false
    }
}
