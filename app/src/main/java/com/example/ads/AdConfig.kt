package com.example.ads

import com.example.BuildConfig

/**
 * Central AdMob configuration.
 * - DEBUG builds strictly use official Google test IDs.
 * - RELEASE builds read REAL_* fields from BuildConfig placeholders.
 * - If any REAL_* ID is empty in a release build, that format safely disables itself
 *   rather than crashing or requesting an invalid ad unit.
 */
object AdConfig {

    // Official Google Test IDs for Android
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/9214589741"
    const val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"

    val isDebugBuild: Boolean
        get() = BuildConfig.DEBUG

    val APP_ID: String
        get() = if (BuildConfig.DEBUG) TEST_APP_ID else BuildConfig.REAL_APP_ID.trim()

    val BANNER: String
        get() = if (BuildConfig.DEBUG) TEST_BANNER_ID else BuildConfig.REAL_BANNER_ID.trim()

    val NATIVE: String
        get() = if (BuildConfig.DEBUG) TEST_NATIVE_ID else BuildConfig.REAL_NATIVE_ID.trim()

    val INTERSTITIAL: String
        get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL_ID else BuildConfig.REAL_INTERSTITIAL_ID.trim()

    val REWARDED: String
        get() = if (BuildConfig.DEBUG) TEST_REWARDED_ID else BuildConfig.REAL_REWARDED_ID.trim()

    val APP_OPEN: String
        get() = if (BuildConfig.DEBUG) TEST_APP_OPEN_ID else BuildConfig.REAL_APP_OPEN_ID.trim()

    fun isAppIdConfigured(): Boolean = APP_ID.isNotBlank()

    fun isBannerEnabled(): Boolean = isAppIdConfigured() && BANNER.isNotBlank()

    fun isNativeEnabled(): Boolean = isAppIdConfigured() && NATIVE.isNotBlank()

    fun isInterstitialEnabled(): Boolean = isAppIdConfigured() && INTERSTITIAL.isNotBlank()

    fun isRewardedEnabled(): Boolean = isAppIdConfigured() && REWARDED.isNotBlank()

    fun isAppOpenEnabled(): Boolean = isAppIdConfigured() && APP_OPEN.isNotBlank()
}
