package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.PreferencesRepository
import com.example.data.UserPreferences
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

enum class AdFormatType(val label: String) {
    BANNER("Adaptive Banner"),
    NATIVE("Native Ad"),
    INTERSTITIAL("Interstitial"),
    REWARDED("Rewarded Video"),
    APP_OPEN("App Open")
}

enum class AdLoadStage {
    IDLE,
    LOADING,
    LOADED,
    FAILED,
    DISABLED_PRO
}

data class AdFormatStatus(
    val format: AdFormatType,
    val stage: AdLoadStage = AdLoadStage.IDLE,
    val errorCode: Int? = null,
    val errorMessage: String? = null,
    val adUnitId: String = "",
    val lastUpdatedMs: Long = System.currentTimeMillis()
) {
    val summaryLabel: String
        get() = when (stage) {
            AdLoadStage.IDLE -> "Idle"
            AdLoadStage.LOADING -> "Loading..."
            AdLoadStage.LOADED -> "Loaded"
            AdLoadStage.FAILED -> "Failed (code ${errorCode ?: -1})"
            AdLoadStage.DISABLED_PRO -> "Disabled (Dyne Pro)"
        }
}

data class NativeAdDisplayModel(
    val headline: String,
    val body: String,
    val callToAction: String,
    val advertiser: String,
    val starRating: Double = 4.9,
    val isTestAd: Boolean = true,
    val sdkNativeAd: NativeAd? = null
)

sealed class RewardUnlockPromptTarget {
    data class LockedTool(
        val toolId: String,
        val toolName: String,
        val onUnlocked: () -> Unit
    ) : RewardUnlockPromptTarget()

    data class LockedTheme(
        val themeName: String,
        val onUnlocked: () -> Unit
    ) : RewardUnlockPromptTarget()

    data class ExtraDocumentSave(
        val onUnlocked: () -> Unit
    ) : RewardUnlockPromptTarget()

    data class OneCsvExport(
        val onUnlocked: () -> Unit
    ) : RewardUnlockPromptTarget()
}

class AdManager(
    context: Context,
    val adGate: AdGate,
    private val preferencesRepository: PreferencesRepository
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private var sdkInitialized = false
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var lastBackgroundTimestampMs: Long = 0L
    private var isShowingFullscreenAd: Boolean = false

    private val _isSoundPlaying = MutableStateFlow(false)
    val isSoundPlaying: StateFlow<Boolean> = _isSoundPlaying.asStateFlow()
    val isSoundPlayingNow: Boolean
        get() = _isSoundPlaying.value

    fun setSoundPlaying(playing: Boolean) {
        _isSoundPlaying.value = playing
    }

    private val _formatStatuses = MutableStateFlow<Map<AdFormatType, AdFormatStatus>>(
        AdFormatType.entries.associateWith { fmt ->
            AdFormatStatus(format = fmt, adUnitId = getUnitIdForFormat(fmt))
        }
    )
    val formatStatuses: StateFlow<Map<AdFormatType, AdFormatStatus>> = _formatStatuses.asStateFlow()

    private val _nativeAdModel = MutableStateFlow<NativeAdDisplayModel?>(null)
    val nativeAdModel: StateFlow<NativeAdDisplayModel?> = _nativeAdModel.asStateFlow()

    private val _bannerReady = MutableStateFlow(false)
    val bannerReady: StateFlow<Boolean> = _bannerReady.asStateFlow()

    private val _umpConsentStatusText = MutableStateFlow("Checking UMP...")
    val umpConsentStatusText: StateFlow<String> = _umpConsentStatusText.asStateFlow()

    private val _adEventLogs = MutableStateFlow<List<String>>(emptyList())
    val adEventLogs: StateFlow<List<String>> = _adEventLogs.asStateFlow()

    private val _activeRewardPrompt = MutableStateFlow<RewardUnlockPromptTarget?>(null)
    val activeRewardPrompt: StateFlow<RewardUnlockPromptTarget?> = _activeRewardPrompt.asStateFlow()

    init {
        scope.launch {
            preferencesRepository.ensureFirstSessionStartRecorded()
        }
        scope.launch {
            adGate.isPro.collect { isPro ->
                if (isPro) {
                    logEvent("PRO active -> disabling all AdMob formats & clearing preloaded cache")
                    clearAllAdsForPro()
                } else {
                    logEvent("Free tier active -> preloading AdMob formats")
                    initializeMobileAdsInBackgroundAndPreload()
                }
            }
        }
    }

    private fun getUnitIdForFormat(format: AdFormatType): String = when (format) {
        AdFormatType.BANNER -> AdConfig.BANNER
        AdFormatType.NATIVE -> AdConfig.NATIVE
        AdFormatType.INTERSTITIAL -> AdConfig.INTERSTITIAL
        AdFormatType.REWARDED -> AdConfig.REWARDED
        AdFormatType.APP_OPEN -> AdConfig.APP_OPEN
    }

    fun logEvent(message: String) {
        val timestamp = timeFormat.format(Date())
        val line = "[$timestamp] $message"
        Log.d(TAG, line)
        _adEventLogs.value = (listOf(line) + _adEventLogs.value).take(120)
    }

    private fun updateStatus(
        format: AdFormatType,
        stage: AdLoadStage,
        errorCode: Int? = null,
        errorMessage: String? = null
    ) {
        val current = _formatStatuses.value.toMutableMap()
        current[format] = AdFormatStatus(
            format = format,
            stage = stage,
            errorCode = errorCode,
            errorMessage = errorMessage,
            adUnitId = getUnitIdForFormat(format),
            lastUpdatedMs = System.currentTimeMillis()
        )
        _formatStatuses.value = current
    }

    private val currentSessionStartMs: Long = System.currentTimeMillis()

    private fun clearAllAdsForPro() {
        interstitialAd = null
        rewardedAd = null
        appOpenAd = null
        _nativeAdModel.value?.sdkNativeAd?.destroy()
        _nativeAdModel.value = null
        _bannerReady.value = false
        _activeRewardPrompt.value = null
        AdFormatType.entries.forEach { fmt ->
            updateStatus(fmt, AdLoadStage.DISABLED_PRO)
        }
    }

    /**
     * Requests UMP consent on first launch (and whenever required) and initializes Mobile Ads SDK
     * on a background thread.
     */
    fun requestUmpConsentOnLaunch(activity: Activity) {
        if (adGate.isProActiveNow) {
            _umpConsentStatusText.value = "Pro Active (No Ads)"
            return
        }
        runCatching {
            val params = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
            logEvent("UMP: requesting consent info update...")
            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    val statusLabel = consentStatusName(consentInformation.consentStatus)
                    _umpConsentStatusText.value = statusLabel
                    logEvent("UMP: consent info updated ($statusLabel)")
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        if (formError != null) {
                            logEvent("UMP form note: ${formError.errorCode} - ${formError.message}")
                        } else {
                            val updatedStatus = consentStatusName(consentInformation.consentStatus)
                            _umpConsentStatusText.value = updatedStatus
                            logEvent("UMP: consent flow complete ($updatedStatus)")
                        }
                        initializeMobileAdsInBackgroundAndPreload()
                    }
                },
                { requestError ->
                    _umpConsentStatusText.value = "Standard (${requestError.errorCode})"
                    logEvent("UMP request fallback (${requestError.errorCode}): ${requestError.message}")
                    initializeMobileAdsInBackgroundAndPreload()
                }
            )
        }.onFailure { err ->
            _umpConsentStatusText.value = "Standard (Offline)"
            logEvent("UMP exception handled: ${err.message}")
            initializeMobileAdsInBackgroundAndPreload()
        }
    }

    /**
     * Re-opens the UMP Privacy Options form from Settings > Privacy.
     */
    fun openPrivacyOptionsForm(activity: Activity, onResultMessage: (String) -> Unit) {
        runCatching {
            logEvent("UMP: opening Privacy Options form from Settings > Privacy")
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
                if (formError != null) {
                    logEvent("UMP privacy form info (${formError.errorCode}): ${formError.message}")
                    onResultMessage("UMP Privacy Choices checked (${_umpConsentStatusText.value}).")
                } else {
                    val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
                    val updated = consentStatusName(consentInfo.consentStatus)
                    _umpConsentStatusText.value = updated
                    logEvent("UMP privacy form updated -> $updated")
                    onResultMessage("Ad privacy preferences updated ($updated).")
                }
            }
        }.onFailure { err ->
            logEvent("UMP privacy form fallback: ${err.message}")
            onResultMessage("UMP Privacy Choices verified (${_umpConsentStatusText.value}).")
        }
    }

    /**
     * Resets UMP consent state (Developer Options in DEBUG builds).
     */
    fun resetUmpConsent(activity: Activity) {
        runCatching {
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
            consentInformation.reset()
            _umpConsentStatusText.value = "Reset (UNKNOWN)"
            logEvent("UMP: consent state reset via Developer Options")
            requestUmpConsentOnLaunch(activity)
        }.onFailure { err ->
            logEvent("UMP reset error: ${err.message}")
        }
    }

    private fun consentStatusName(status: Int): String = when (status) {
        ConsentInformation.ConsentStatus.REQUIRED -> "Required"
        ConsentInformation.ConsentStatus.NOT_REQUIRED -> "Not Required"
        ConsentInformation.ConsentStatus.OBTAINED -> "Obtained"
        else -> "Unknown"
    }

    /**
     * Initializes the Google Mobile Ads SDK on a background thread (Dispatchers.IO)
     * and preloads ad formats.
     */
    fun initializeMobileAdsInBackgroundAndPreload() {
        if (!adGate.shouldShowAds()) {
            clearAllAdsForPro()
            return
        }
        scope.launch {
            if (!sdkInitialized) {
                logEvent("Initializing Google Mobile Ads SDK on Dispatchers.IO...")
                withContext(Dispatchers.IO) {
                    runCatching {
                        MobileAds.initialize(appContext) { initStatus ->
                            Log.d(TAG, "MobileAds initialized: ${initStatus.adapterStatusMap.keys}")
                        }
                    }.onFailure { err ->
                        Log.w(TAG, "MobileAds.initialize warning: ${err.message}")
                    }
                }
                sdkInitialized = true
                logEvent("Google Mobile Ads SDK initialized (App ID: ${AdConfig.APP_ID})")
            }
            preloadAllFormats()
        }
    }

    fun preloadAllFormats() {
        if (!adGate.shouldShowAds()) {
            clearAllAdsForPro()
            return
        }
        preloadBanner()
        preloadNativeAd()
        preloadInterstitialAd()
        preloadRewardedAd()
        preloadAppOpenAd()
    }

    fun reloadAllAds() {
        logEvent("Reloading all AdMob formats...")
        interstitialAd = null
        rewardedAd = null
        appOpenAd = null
        _nativeAdModel.value?.sdkNativeAd?.destroy()
        _nativeAdModel.value = null
        _bannerReady.value = false
        preloadAllFormats()
    }

    fun notifyBannerLoading(placementTag: String = "") {
        if (!adGate.shouldShowAds()) return
        updateStatus(AdFormatType.BANNER, AdLoadStage.LOADING)
        if (placementTag.isNotEmpty()) {
            logEvent("Banner loading [$placementTag] (${AdConfig.BANNER})")
        }
    }

    fun notifyBannerLoaded(placementTag: String = "") {
        if (!adGate.shouldShowAds()) return
        _bannerReady.value = true
        updateStatus(AdFormatType.BANNER, AdLoadStage.LOADED)
        val suffix = if (placementTag.isNotEmpty()) " [$placementTag]" else ""
        logEvent("Banner LOADED$suffix (${AdConfig.BANNER})")
    }

    fun notifyBannerFailed(errorCode: Int, errorMessage: String, placementTag: String = "") {
        if (!adGate.shouldShowAds()) return
        _bannerReady.value = false
        updateStatus(AdFormatType.BANNER, AdLoadStage.FAILED, errorCode, errorMessage)
        val suffix = if (placementTag.isNotEmpty()) " [$placementTag]" else ""
        logEvent("Banner SDK load failed$suffix (code $errorCode: $errorMessage)")
    }

    private fun preloadBanner() {
        if (!AdConfig.isBannerEnabled() || !adGate.shouldShowAds()) {
            _bannerReady.value = false
            updateStatus(
                AdFormatType.BANNER,
                if (adGate.isProActiveNow) AdLoadStage.DISABLED_PRO else AdLoadStage.IDLE
            )
            return
        }
        updateStatus(AdFormatType.BANNER, AdLoadStage.IDLE)
    }

    fun preloadNativeAd() {
        if (!AdConfig.isNativeEnabled() || !adGate.shouldShowAds()) {
            _nativeAdModel.value = null
            updateStatus(
                AdFormatType.NATIVE,
                if (adGate.isProActiveNow) AdLoadStage.DISABLED_PRO else AdLoadStage.IDLE
            )
            return
        }
        updateStatus(AdFormatType.NATIVE, AdLoadStage.LOADING)
        logEvent("Preloading Native Ad (${AdConfig.NATIVE})...")

        runCatching {
            val adLoader = AdLoader.Builder(appContext, AdConfig.NATIVE)
                .forNativeAd { nativeAd ->
                    if (!adGate.shouldShowAds()) {
                        nativeAd.destroy()
                        return@forNativeAd
                    }
                    _nativeAdModel.value?.sdkNativeAd?.destroy()
                    _nativeAdModel.value = NativeAdDisplayModel(
                        headline = nativeAd.headline ?: "",
                        body = nativeAd.body ?: "",
                        callToAction = nativeAd.callToAction ?: "",
                        advertiser = nativeAd.advertiser ?: "",
                        starRating = nativeAd.starRating ?: 0.0,
                        isTestAd = BuildConfig.DEBUG,
                        sdkNativeAd = nativeAd
                    )
                    updateStatus(AdFormatType.NATIVE, AdLoadStage.LOADED)
                    logEvent("Native Ad LOADED (${AdConfig.NATIVE})")
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        _nativeAdModel.value = null
                        updateStatus(AdFormatType.NATIVE, AdLoadStage.FAILED, error.code, error.message)
                        logEvent("Native Ad load failed (code ${error.code}: ${error.message})")
                    }

                    override fun onAdImpression() {
                        logEvent("Native Ad impression recorded")
                    }

                    override fun onAdClicked() {
                        logEvent("Native Ad clicked")
                    }
                })
                .withNativeAdOptions(NativeAdOptions.Builder().build())
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        }.onFailure { err ->
            _nativeAdModel.value = null
            logEvent("Native Ad exception: ${err.message}")
            updateStatus(AdFormatType.NATIVE, AdLoadStage.FAILED, -1, err.message)
        }
    }

    fun preloadInterstitialAd() {
        if (!AdConfig.isInterstitialEnabled() || !adGate.shouldShowAds()) {
            interstitialAd = null
            updateStatus(
                AdFormatType.INTERSTITIAL,
                if (adGate.isProActiveNow) AdLoadStage.DISABLED_PRO else AdLoadStage.IDLE
            )
            return
        }
        updateStatus(AdFormatType.INTERSTITIAL, AdLoadStage.LOADING)
        logEvent("Preloading Interstitial Ad (${AdConfig.INTERSTITIAL})...")

        runCatching {
            InterstitialAd.load(
                appContext,
                AdConfig.INTERSTITIAL,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        if (!adGate.shouldShowAds()) return
                        interstitialAd = ad
                        updateStatus(AdFormatType.INTERSTITIAL, AdLoadStage.LOADED)
                        logEvent("Interstitial Ad LOADED (${AdConfig.INTERSTITIAL})")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                        logEvent("Interstitial load failed (code ${error.code}: ${error.message})")
                        updateStatus(
                            AdFormatType.INTERSTITIAL,
                            AdLoadStage.FAILED,
                            error.code,
                            error.message
                        )
                    }
                }
            )
        }.onFailure { err ->
            interstitialAd = null
            logEvent("Interstitial load exception: ${err.message}")
            updateStatus(AdFormatType.INTERSTITIAL, AdLoadStage.FAILED, -1, err.message)
        }
    }

    fun preloadRewardedAd() {
        if (!AdConfig.isRewardedEnabled() || !adGate.shouldShowAds()) {
            rewardedAd = null
            updateStatus(
                AdFormatType.REWARDED,
                if (adGate.isProActiveNow) AdLoadStage.DISABLED_PRO else AdLoadStage.IDLE
            )
            return
        }
        updateStatus(AdFormatType.REWARDED, AdLoadStage.LOADING)
        logEvent("Preloading Rewarded Ad (${AdConfig.REWARDED})...")

        runCatching {
            RewardedAd.load(
                appContext,
                AdConfig.REWARDED,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        if (!adGate.shouldShowAds()) return
                        rewardedAd = ad
                        updateStatus(AdFormatType.REWARDED, AdLoadStage.LOADED)
                        logEvent("Rewarded Ad LOADED (${AdConfig.REWARDED})")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedAd = null
                        logEvent("Rewarded load failed (code ${error.code}: ${error.message})")
                        updateStatus(
                            AdFormatType.REWARDED,
                            AdLoadStage.FAILED,
                            error.code,
                            error.message
                        )
                    }
                }
            )
        }.onFailure { err ->
            rewardedAd = null
            logEvent("Rewarded load exception: ${err.message}")
            updateStatus(AdFormatType.REWARDED, AdLoadStage.FAILED, -1, err.message)
        }
    }

    fun preloadAppOpenAd() {
        if (!AdConfig.isAppOpenEnabled() || !adGate.shouldShowAds()) {
            appOpenAd = null
            updateStatus(
                AdFormatType.APP_OPEN,
                if (adGate.isProActiveNow) AdLoadStage.DISABLED_PRO else AdLoadStage.IDLE
            )
            return
        }
        updateStatus(AdFormatType.APP_OPEN, AdLoadStage.LOADING)
        logEvent("Preloading App Open Ad (${AdConfig.APP_OPEN})...")

        runCatching {
            AppOpenAd.load(
                appContext,
                AdConfig.APP_OPEN,
                AdRequest.Builder().build(),
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        if (!adGate.shouldShowAds()) return
                        appOpenAd = ad
                        updateStatus(AdFormatType.APP_OPEN, AdLoadStage.LOADED)
                        logEvent("App Open Ad LOADED (${AdConfig.APP_OPEN})")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        appOpenAd = null
                        logEvent("App Open load failed (code ${error.code}: ${error.message})")
                        updateStatus(
                            AdFormatType.APP_OPEN,
                            AdLoadStage.FAILED,
                            error.code,
                            error.message
                        )
                    }
                }
            )
        }.onFailure { err ->
            appOpenAd = null
            logEvent("App Open load exception: ${err.message}")
            updateStatus(AdFormatType.APP_OPEN, AdLoadStage.FAILED, -1, err.message)
        }
    }

    /**
     * Checks if an Interstitial Ad is permitted by all frequency caps:
     * - Never when isPro
     * - Never in the first 2 minutes
     * - Max 1 per 3 minutes (180_000 ms)
     * - Max 6 per day
     */
    fun canShowInterstitialByFrequencyCap(
        prefs: UserPreferences,
        nowMs: Long = System.currentTimeMillis(),
        todayKey: String = LocalDate.now().toString()
    ): Pair<Boolean, String> {
        if (!adGate.shouldShowAds()) {
            return false to "Dyne Pro active or ads disabled"
        }
        if (prefs.launchCount <= 1) {
            val startMs = if (prefs.firstSessionStartEpochMs > 0L) prefs.firstSessionStartEpochMs else currentSessionStartMs
            val elapsedFirstSessionMs = nowMs - startMs
            if (elapsedFirstSessionMs < FIRST_SESSION_GRACE_MS) {
                val remainSec = ((FIRST_SESSION_GRACE_MS - elapsedFirstSessionMs) / 1000L).coerceAtLeast(1L)
                return false to "First-session 2-min protection (${remainSec}s left)"
            }
        }
        val sinceLastMs = nowMs - prefs.lastInterstitialShownEpochMs
        if (prefs.lastInterstitialShownEpochMs > 0L && sinceLastMs < INTERSTITIAL_COOLDOWN_MS) {
            val remainSec = ((INTERSTITIAL_COOLDOWN_MS - sinceLastMs) / 1000L).coerceAtLeast(1L)
            return false to "3-minute cooldown active (${remainSec}s left)"
        }
        val countToday = if (prefs.interstitialDailyDate == todayKey) prefs.interstitialDailyCount else 0
        if (countToday >= INTERSTITIAL_MAX_PER_DAY) {
            return false to "Daily cap reached ($countToday/$INTERSTITIAL_MAX_PER_DAY today)"
        }
        return true to "Eligible"
    }

    /**
     * Shows an Interstitial Ad at a natural break:
     * - After saving a document
     * - After leaving a tool used for 20+ seconds
     */
    fun maybeShowInterstitialAtNaturalBreak(activity: Activity?, reason: String) {
        if (!adGate.shouldShowAds() || isShowingFullscreenAd || _isSoundPlaying.value) {
            if (_isSoundPlaying.value) {
                logEvent("Interstitial skipped [$reason]: Sound is currently playing")
            }
            return
        }
        scope.launch {
            val prefs = preferencesRepository.preferencesFlow.first()
            val nowMs = System.currentTimeMillis()
            if (nowMs - currentSessionStartMs < FIRST_SESSION_GRACE_MS) {
                val remainSec = ((FIRST_SESSION_GRACE_MS - (nowMs - currentSessionStartMs)) / 1000L).coerceAtLeast(1L)
                logEvent("Interstitial skipped [$reason]: First 2-min protection (${remainSec}s left)")
                return@launch
            }
            val todayKey = LocalDate.now().toString()
            val (allowed, reasonMsg) = canShowInterstitialByFrequencyCap(prefs, nowMs, todayKey)
            if (!allowed) {
                logEvent("Interstitial skipped [$reason]: $reasonMsg")
                return@launch
            }
            showInterstitialInternal(activity, triggerReason = reason, recordCap = true)
        }
    }

    /**
     * Developer Options button: Show Interstitial Now.
     */
    fun showInterstitialNowForDebug(activity: Activity?) {
        if (!adGate.shouldShowAds()) {
            logEvent("Cannot show Interstitial: Pro is active")
            return
        }
        showInterstitialInternal(activity, triggerReason = "Developer Options", recordCap = true)
    }

    private fun showInterstitialInternal(
        activity: Activity?,
        triggerReason: String,
        recordCap: Boolean
    ) {
        val nowMs = System.currentTimeMillis()
        val todayKey = LocalDate.now().toString()
        val sdkAd = interstitialAd

        if (sdkAd != null && activity != null) {
            isShowingFullscreenAd = true
            sdkAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    logEvent("Interstitial SHOWN [$triggerReason]")
                    if (recordCap) {
                        scope.launch {
                            preferencesRepository.recordInterstitialShown(nowMs, todayKey)
                        }
                    }
                }

                override fun onAdDismissedFullScreenContent() {
                    isShowingFullscreenAd = false
                    interstitialAd = null
                    logEvent("Interstitial dismissed -> reloading")
                    preloadInterstitialAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    isShowingFullscreenAd = false
                    interstitialAd = null
                    logEvent("Interstitial failed to show (${adError.code}): ${adError.message}")
                    preloadInterstitialAd()
                }
            }
            sdkAd.show(activity)
        } else {
            logEvent("Interstitial not ready [$triggerReason] — skipping with zero UI gap")
            preloadInterstitialAd()
        }
    }

    /**
     * Opens the Rewarded Unlock choice sheet ("Watch ad to unlock" vs "Unlock Dyne Pro").
     */
    val isRewardedAdReady: Boolean
        get() = rewardedAd != null

    fun promptRewardedUnlock(target: RewardUnlockPromptTarget) {
        if (adGate.isProActiveNow) {
            when (target) {
                is RewardUnlockPromptTarget.LockedTool -> target.onUnlocked()
                is RewardUnlockPromptTarget.LockedTheme -> target.onUnlocked()
                is RewardUnlockPromptTarget.ExtraDocumentSave -> target.onUnlocked()
                is RewardUnlockPromptTarget.OneCsvExport -> target.onUnlocked()
            }
            return
        }
        _activeRewardPrompt.value = target
    }

    fun dismissRewardPrompt() {
        _activeRewardPrompt.value = null
    }

    /**
     * Shows a real Google AdMob RewardedAd and executes [onRewardEarned] strictly inside
     * the SDK's onUserEarnedReward callback. If no ad is ready, invokes [onAdUnavailable]
     * with "No ad available, try again soon" and NEVER grants the reward.
     */
    fun showRewardedAd(
        activity: Activity?,
        rewardDescription: String,
        onAdUnavailable: (String) -> Unit = {},
        onRewardEarned: () -> Unit
    ) {
        val sdkAd = rewardedAd
        if (sdkAd != null && activity != null) {
            _activeRewardPrompt.value = null
            isShowingFullscreenAd = true
            sdkAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    logEvent("Rewarded Ad SHOWN [$rewardDescription]")
                }

                override fun onAdDismissedFullScreenContent() {
                    isShowingFullscreenAd = false
                    rewardedAd = null
                    logEvent("Rewarded Ad dismissed -> reloading")
                    preloadRewardedAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    isShowingFullscreenAd = false
                    rewardedAd = null
                    logEvent("Rewarded Ad failed to show (${adError.code}): ${adError.message}")
                    preloadRewardedAd()
                    onAdUnavailable("No ad available, try again soon")
                }
            }
            sdkAd.show(activity) { rewardItem ->
                logEvent("Reward earned (${rewardItem.amount} ${rewardItem.type}) -> $rewardDescription")
                onRewardEarned()
            }
        } else {
            logEvent("Rewarded Ad not ready for [$rewardDescription] -> reloading")
            preloadRewardedAd()
            onAdUnavailable("No ad available, try again soon")
        }
    }

    /**
     * Tracks when the app goes to background so App Open ads know how long the user was away.
     */
    fun onAppBackgrounded(nowMs: Long = System.currentTimeMillis()) {
        lastBackgroundTimestampMs = nowMs
    }

    /**
     * Evaluates App Open eligibility when returning from background:
     * - Never when isPro
     * - Never on first launch (launchCount > 1)
     * - Only when returning from background after 30+ minutes
     * - Enforces APP_OPEN_COOLDOWN_MS between shows
     */
    fun canShowAppOpenOnReturn(
        prefs: UserPreferences,
        backgroundDurationMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): Pair<Boolean, String> {
        if (!adGate.shouldShowAds()) {
            return false to "Dyne Pro active or ads disabled"
        }
        if (prefs.launchCount <= 1) {
            return false to "Never on first launch"
        }
        if (backgroundDurationMs < APP_OPEN_MIN_BACKGROUND_MS) {
            return false to "Backgrounded ${backgroundDurationMs / 1000L}s (< 30 min)"
        }
        val sinceLastAppOpen = nowMs - prefs.lastAppOpenShownEpochMs
        if (prefs.lastAppOpenShownEpochMs > 0L && sinceLastAppOpen < APP_OPEN_COOLDOWN_MS) {
            return false to "App Open cooldown active"
        }
        return true to "Eligible"
    }

    fun onAppForegrounded(activity: Activity?, nowMs: Long = System.currentTimeMillis()) {
        val bgStart = lastBackgroundTimestampMs
        lastBackgroundTimestampMs = 0L
        if (bgStart <= 0L || !adGate.shouldShowAds() || isShowingFullscreenAd || _isSoundPlaying.value) return

        val backgroundDurationMs = nowMs - bgStart
        scope.launch {
            val prefs = preferencesRepository.preferencesFlow.first()
            val (allowed, reason) = canShowAppOpenOnReturn(prefs, backgroundDurationMs, nowMs)
            if (!allowed) {
                logEvent("App Open skipped on resume: $reason")
                return@launch
            }
            showAppOpenInternal(activity, triggerReason = "Returned after ${backgroundDurationMs / 60000L}m")
        }
    }

    fun showAppOpenNowForDebug(activity: Activity?) {
        if (!adGate.shouldShowAds()) {
            logEvent("Cannot show App Open: Pro is active")
            return
        }
        showAppOpenInternal(activity, triggerReason = "Developer Options")
    }

    private fun showAppOpenInternal(activity: Activity?, triggerReason: String) {
        val nowMs = System.currentTimeMillis()
        val sdkAd = appOpenAd

        if (sdkAd != null && activity != null) {
            isShowingFullscreenAd = true
            sdkAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    logEvent("App Open Ad SHOWN [$triggerReason]")
                    scope.launch {
                        preferencesRepository.recordAppOpenShown(nowMs)
                    }
                }

                override fun onAdDismissedFullScreenContent() {
                    isShowingFullscreenAd = false
                    appOpenAd = null
                    logEvent("App Open Ad dismissed -> reloading")
                    preloadAppOpenAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    isShowingFullscreenAd = false
                    appOpenAd = null
                    logEvent("App Open failed to show (${adError.code}): ${adError.message}")
                    preloadAppOpenAd()
                }
            }
            sdkAd.show(activity)
        } else {
            logEvent("App Open Ad not ready [$triggerReason] -> reloading")
            preloadAppOpenAd()
        }
    }

    fun resetFrequencyCaps() {
        scope.launch {
            preferencesRepository.resetAdFrequencyCaps()
            logEvent("Frequency caps reset (Interstitial 3m/6-day & App Open cooldown timers cleared)")
        }
    }

    fun grantTool1hUnlock(toolId: String, toolName: String, onDone: () -> Unit) {
        scope.launch {
            preferencesRepository.unlockToolFor1Hour(toolId)
            logEvent("Reward granted: Unlocked tool '$toolName' ($toolId) for 1h")
            onDone()
        }
    }

    fun grantTheme1hUnlock(themeName: String, onDone: () -> Unit) {
        scope.launch {
            preferencesRepository.unlockThemeFor1Hour(themeName)
            logEvent("Reward granted: Unlocked theme '$themeName' for 1h")
            onDone()
        }
    }

    fun grantFeature1hUnlock(featureKey: String, featureTitle: String, onDone: () -> Unit) {
        scope.launch {
            preferencesRepository.unlockFeatureFor1Hour(featureKey)
            logEvent("Reward granted: Unlocked feature '$featureTitle' ($featureKey) for 1h")
            onDone()
        }
    }

    fun grantOneExtraDocumentSave(onDone: () -> Unit) {
        scope.launch {
            preferencesRepository.grantOneExtraDocumentSave()
            logEvent("Reward granted: +1 Extra Document Save")
            onDone()
        }
    }

    fun grantOneCsvExport(onDone: () -> Unit) {
        scope.launch {
            preferencesRepository.grantOneExtraCsvExport()
            logEvent("Reward granted: +1 CSV Export unlocked")
            onDone()
        }
    }

    companion object {
        private const val TAG = "DyneAds"
        const val FIRST_SESSION_GRACE_MS = 2L * 60L * 1000L // 2 minutes
        const val INTERSTITIAL_COOLDOWN_MS = 3L * 60L * 1000L // 3 minutes
        const val INTERSTITIAL_MAX_PER_DAY = 6
        const val APP_OPEN_MIN_BACKGROUND_MS = 30L * 60L * 1000L // 30 minutes
        const val APP_OPEN_COOLDOWN_MS = 240L * 60L * 1000L // 240 minutes
    }
}
