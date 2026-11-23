package com.example.ads

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.billing.PurchaseManager
import kotlinx.coroutines.flow.StateFlow

/**
 * AdGate exposes isPro from PurchaseManager so that every ad composable and manager
 * returns immediately when isPro is true, and ads disappear instantly after purchase or restore.
 */
class AdGate(
    private val purchaseManager: PurchaseManager
) {
    val isPro: StateFlow<Boolean> = purchaseManager.isPro
    val isRemoveAds: StateFlow<Boolean> = purchaseManager.isRemoveAds

    val isProActiveNow: Boolean
        get() = purchaseManager.isPro.value

    val isRemoveAdsActiveNow: Boolean
        get() = purchaseManager.isRemoveAds.value

    fun shouldShowAds(): Boolean {
        if (isProActiveNow || isRemoveAdsActiveNow) return false
        return AdConfig.isAppIdConfigured()
    }
}

val LocalAdGate = staticCompositionLocalOf<AdGate?> { null }
val LocalAdManager = staticCompositionLocalOf<AdManager?> { null }
