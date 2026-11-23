package com.example.ui.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.billing.PaywallPricingState
import com.example.billing.PlanOption
import com.example.billing.PurchaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PaywallViewModel(
    private val purchaseManager: PurchaseManager
) : ViewModel() {

    val pricingState: StateFlow<PaywallPricingState> = purchaseManager.pricingState
    val isPro: StateFlow<Boolean> = purchaseManager.isPro
    val isRemoveAds: StateFlow<Boolean> = purchaseManager.isRemoveAds
    val removeAdsPrice: StateFlow<String> = purchaseManager.removeAdsPrice
    val purchaseMessage: StateFlow<String?> = purchaseManager.purchaseMessage

    // Yearly is pre-selected by default per specification
    private val _selectedPlan = MutableStateFlow(PlanOption.YEARLY)
    val selectedPlan: StateFlow<PlanOption> = _selectedPlan.asStateFlow()

    fun selectPlan(plan: PlanOption) {
        _selectedPlan.value = plan
    }

    fun launchPurchase(activity: Activity) {
        purchaseManager.launchPurchase(activity, _selectedPlan.value)
    }

    fun launchRemoveAdsPurchase(activity: Activity) {
        purchaseManager.launchRemoveAdsPurchase(activity)
    }

    fun notifyPlayBillingUnavailable() {
        // Trigger refresh or message if no Activity is present
        purchaseManager.refreshProductsAndPurchases()
    }

    fun restorePurchases() {
        purchaseManager.restorePurchases()
    }

    fun clearMessage() {
        purchaseManager.clearMessage()
    }

    class Factory(private val purchaseManager: PurchaseManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PaywallViewModel(purchaseManager) as T
        }
    }
}
