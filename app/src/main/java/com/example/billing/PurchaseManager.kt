package com.example.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.BuildConfig
import com.example.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency
import kotlin.math.roundToInt

enum class PlanOption {
    YEARLY, LIFETIME, MONTHLY
}

enum class SoundPack(
    val productId: String,
    val title: String,
    val subtitle: String,
    val coinCost1h: Int = 10
) {
    PETS(
        productId = "dyne_pack_pets",
        title = "Pets & Animals Pack",
        subtitle = "Dog whistle (16–20 kHz), Dog & Cat attention pulses, Bird deterrent bursts, Distress-free Pet Test"
    ),
    RELAX(
        productId = "dyne_pack_relax",
        title = "Relax & Focus Pack",
        subtitle = "Calm layered drones (174, 285, 432, 528 Hz), Deep Focus drone, Sleep Hum, Binaural Focus"
    ),
    PRO_TONES(
        productId = "dyne_pack_pro",
        title = "Pro Tones & Full Generator Pack",
        subtitle = "20 Hz–20 kHz range, all waveforms, Sweep/Pulse/Loop, 100% volume, Subwoofer & Burn-In sweeps, Presets"
    );

    val shortBenefit: String
        get() = subtitle

    companion object {
        fun fromProductId(id: String): SoundPack? =
            entries.firstOrNull { it.productId.equals(id.trim(), ignoreCase = true) }
    }
}

/**
 * Product pricing read strictly from Google Play Billing ProductDetails
 * (formattedPrice, priceAmountMicros, priceCurrencyCode). Never hardcoded.
 */
data class BillingPlanInfo(
    val planOption: PlanOption,
    val productId: String,
    val basePlanId: String?,
    val formattedPrice: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val offerToken: String? = null
) {
    val hasValidPlayPrice: Boolean
        get() = priceAmountMicros > 0L && formattedPrice.isNotBlank()
}

data class PaywallPricingState(
    val yearlyPlan: BillingPlanInfo,
    val lifetimePlan: BillingPlanInfo,
    val monthlyPlan: BillingPlanInfo,
    val isBillingAvailable: Boolean = false,
    val isLoading: Boolean = false,
    val statusMessage: String? = null
) {
    /**
     * Dynamically computes "Save X%" comparing the Yearly plan against 12x the Monthly plan
     * using priceAmountMicros from Google Play Billing ProductDetails.
     */
    val yearlySavingsPercent: Int
        get() {
            val fullYearMonthlyMicros = monthlyPlan.priceAmountMicros * 12L
            if (fullYearMonthlyMicros <= 0L || yearlyPlan.priceAmountMicros <= 0L) return 0
            val diff = fullYearMonthlyMicros - yearlyPlan.priceAmountMicros
            return ((diff.toDouble() / fullYearMonthlyMicros.toDouble()) * 100.0)
                .roundToInt()
                .coerceIn(0, 95)
        }

    /**
     * Dynamically formats the equivalent monthly cost of the Yearly plan using NumberFormat
     * and the product's priceCurrencyCode from Play Billing ProductDetails.
     */
    val yearlyPerMonthFormatted: String
        get() {
            if (yearlyPlan.priceAmountMicros <= 0L || yearlyPlan.priceCurrencyCode.isBlank()) {
                return ""
            }
            val perMonthAmount = (yearlyPlan.priceAmountMicros / 12.0) / 1_000_000.0
            return formatCurrency(perMonthAmount, yearlyPlan.priceCurrencyCode)
        }

    companion object {
        const val PLAY_UNAVAILABLE_MESSAGE =
            "Purchases work when Dyne is installed from Google Play"

        fun formatCurrency(amount: Double, currencyCode: String): String {
            return try {
                val format = NumberFormat.getCurrencyInstance()
                format.currency = Currency.getInstance(currencyCode)
                format.maximumFractionDigits = 2
                format.minimumFractionDigits = 2
                format.format(amount)
            } catch (_: Exception) {
                String.format("%.2f %s", amount, currencyCode)
            }
        }

        fun createUnloadedState(isLoading: Boolean = false): PaywallPricingState {
            return PaywallPricingState(
                yearlyPlan = BillingPlanInfo(
                    planOption = PlanOption.YEARLY,
                    productId = GooglePlayPurchaseManager.PRODUCT_SUB_DYNE_PRO,
                    basePlanId = GooglePlayPurchaseManager.BASE_PLAN_YEARLY,
                    formattedPrice = "",
                    priceAmountMicros = 0L,
                    priceCurrencyCode = ""
                ),
                lifetimePlan = BillingPlanInfo(
                    planOption = PlanOption.LIFETIME,
                    productId = GooglePlayPurchaseManager.PRODUCT_INAPP_LIFETIME,
                    basePlanId = null,
                    formattedPrice = "",
                    priceAmountMicros = 0L,
                    priceCurrencyCode = ""
                ),
                monthlyPlan = BillingPlanInfo(
                    planOption = PlanOption.MONTHLY,
                    productId = GooglePlayPurchaseManager.PRODUCT_SUB_DYNE_PRO,
                    basePlanId = GooglePlayPurchaseManager.BASE_PLAN_MONTHLY,
                    formattedPrice = "",
                    priceAmountMicros = 0L,
                    priceCurrencyCode = ""
                ),
                isBillingAvailable = false,
                isLoading = isLoading
            )
        }
    }
}

interface PurchaseManager {
    val isPro: StateFlow<Boolean>
    val proSource: StateFlow<String>
    val pricingState: StateFlow<PaywallPricingState>
    val purchaseMessage: StateFlow<String?>
    val ownedSoundPacks: StateFlow<Set<String>>
    val soundPackPrices: StateFlow<Map<String, String>>
    val isRemoveAds: StateFlow<Boolean>
        get() = MutableStateFlow(false)
    val removeAdsPrice: StateFlow<String>
        get() = MutableStateFlow("")

    fun refreshProductsAndPurchases()
    fun launchPurchase(activity: Activity, plan: PlanOption)
    fun launchSoundPackPurchase(activity: Activity, productId: String)
    fun launchRemoveAdsPurchase(activity: Activity) {}
    fun restorePurchases()
    fun setSimulatePro(enabled: Boolean)
    fun clearProDebugState()
    fun clearMessage()
}

/**
 * Real GooglePlayPurchaseManager implementing Google Play Billing Library v7.
 *
 * ENTITLEMENT RULES:
 * - isPro defaults to false.
 * - It becomes true ONLY via Google Play Billing (active subscription "dyne_pro" or owned
 *   "dyne_lifetime", checked on launch and resume, cached in DataStore) or the DEBUG-only
 *   "Simulate Pro" switch.
 * - One-time (INAPP) Sound Packs ("dyne_pack_pets", "dyne_pack_relax", "dyne_pack_pro")
 *   are queried on launch and resume, acknowledged, cached in DataStore, and restored with
 *   "Restore purchases".
 * - If Play Billing is unavailable, plan buttons say
 *   "Purchases work when Dyne is installed from Google Play" and NEVER grant Pro or packs.
 * - No free trial anywhere.
 */
class GooglePlayPurchaseManager(
    context: Context,
    private val preferencesRepository: PreferencesRepository
) : PurchaseManager, PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isPro = MutableStateFlow(false)
    override val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _proSource = MutableStateFlow("None")
    override val proSource: StateFlow<String> = _proSource.asStateFlow()

    private val _pricingState = MutableStateFlow(PaywallPricingState.createUnloadedState())
    override val pricingState: StateFlow<PaywallPricingState> = _pricingState.asStateFlow()

    private val _purchaseMessage = MutableStateFlow<String?>(null)
    override val purchaseMessage: StateFlow<String?> = _purchaseMessage.asStateFlow()

    private val _ownedSoundPacks = MutableStateFlow<Set<String>>(emptySet())
    override val ownedSoundPacks: StateFlow<Set<String>> = _ownedSoundPacks.asStateFlow()

    private val _soundPackPrices = MutableStateFlow<Map<String, String>>(emptyMap())
    override val soundPackPrices: StateFlow<Map<String, String>> = _soundPackPrices.asStateFlow()

    private val _isRemoveAds = MutableStateFlow(false)
    override val isRemoveAds: StateFlow<Boolean> = _isRemoveAds.asStateFlow()

    private val _removeAdsPrice = MutableStateFlow("")
    override val removeAdsPrice: StateFlow<String> = _removeAdsPrice.asStateFlow()

    private var isBillingConnected = false
    private var subProductDetails: ProductDetails? = null
    private var lifetimeProductDetails: ProductDetails? = null
    private var removeAdsProductDetails: ProductDetails? = null
    private val packProductDetailsMap = mutableMapOf<String, ProductDetails>()

    private val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .build()

    init {
        scope.launch {
            preferencesRepository.preferencesFlow.collectLatest { prefs ->
                val debugSim = BuildConfig.DEBUG && prefs.simulateProEnabled
                val playVerified = prefs.cachedIsPro &&
                    (prefs.cachedProSource.startsWith("Google Play"))
                val effectivePro = playVerified || debugSim
                _isPro.value = effectivePro
                _proSource.value = when {
                    playVerified -> prefs.cachedProSource
                    debugSim -> "DEBUG Simulate Pro"
                    else -> "None"
                }
                _ownedSoundPacks.value = prefs.ownedSoundPacksSet
                // Premium includes Remove Ads; also true during active 1h Daily Premium Spotlight
                _isRemoveAds.value = effectivePro || prefs.cachedRemoveAds || prefs.isSpotlightActiveNow()
            }
        }
        connectBilling()
    }

    private fun connectBilling() {
        _pricingState.value = _pricingState.value.copy(isLoading = true)
        runCatching {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        isBillingConnected = true
                        queryProductDetails()
                        queryActivePurchases()
                    } else {
                        isBillingConnected = false
                        _pricingState.value = PaywallPricingState.createUnloadedState(isLoading = false)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isBillingConnected = false
                    _pricingState.value = _pricingState.value.copy(
                        isBillingAvailable = false,
                        isLoading = false
                    )
                }
            })
        }.onFailure {
            isBillingConnected = false
            _pricingState.value = PaywallPricingState.createUnloadedState(isLoading = false)
        }
    }

    override fun refreshProductsAndPurchases() {
        if (isBillingConnected) {
            queryProductDetails()
            queryActivePurchases()
        } else {
            connectBilling()
        }
    }

    private fun queryProductDetails() {
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_SUB_DYNE_PRO)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        val inAppProductIds = listOf(
            PRODUCT_INAPP_LIFETIME,
            PRODUCT_INAPP_REMOVE_ADS,
            PRODUCT_PACK_PETS,
            PRODUCT_PACK_RELAX,
            PRODUCT_PACK_PRO
        )
        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                inAppProductIds.map { prodId ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(prodId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
            )
            .build()

        billingClient.queryProductDetailsAsync(subsParams) { result, queryProductDetailsResult ->
            val productDetailsList = queryProductDetailsResult.productDetailsList
            if (result.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                subProductDetails = productDetailsList.firstOrNull { it.productId == PRODUCT_SUB_DYNE_PRO }
            }
            rebuildPricingFromPlayStore()
        }

        billingClient.queryProductDetailsAsync(inAppParams) { result, queryProductDetailsResult ->
            val productDetailsList = queryProductDetailsResult.productDetailsList
            if (result.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                lifetimeProductDetails = productDetailsList.firstOrNull { it.productId == PRODUCT_INAPP_LIFETIME }
                removeAdsProductDetails = productDetailsList.firstOrNull { it.productId == PRODUCT_INAPP_REMOVE_ADS }
                _removeAdsPrice.value = removeAdsProductDetails?.oneTimePurchaseOfferDetails?.formattedPrice.orEmpty()
                val updatedPackPrices = mutableMapOf<String, String>()
                SoundPack.entries.forEach { pack ->
                    val details = productDetailsList.firstOrNull { it.productId == pack.productId }
                    if (details != null) {
                        packProductDetailsMap[pack.productId] = details
                        val formatted = details.oneTimePurchaseOfferDetails?.formattedPrice.orEmpty()
                        if (formatted.isNotBlank()) {
                            updatedPackPrices[pack.productId] = formatted
                        }
                    }
                }
                _soundPackPrices.value = updatedPackPrices
            }
            rebuildPricingFromPlayStore()
        }
    }

    private fun rebuildPricingFromPlayStore() {
        val sub = subProductDetails
        val lifetime = lifetimeProductDetails
        val unloaded = PaywallPricingState.createUnloadedState()
        val offers = sub?.subscriptionOfferDetails.orEmpty()

        val yearlyOffer = offers.firstOrNull { it.basePlanId == BASE_PLAN_YEARLY }
        val yearlyRecurringPhase = yearlyOffer?.pricingPhases?.pricingPhaseList
            ?.lastOrNull { it.priceAmountMicros > 0L }

        val yearlyPlan = if (yearlyRecurringPhase != null && yearlyOffer != null) {
            BillingPlanInfo(
                planOption = PlanOption.YEARLY,
                productId = PRODUCT_SUB_DYNE_PRO,
                basePlanId = BASE_PLAN_YEARLY,
                formattedPrice = yearlyRecurringPhase.formattedPrice,
                priceAmountMicros = yearlyRecurringPhase.priceAmountMicros,
                priceCurrencyCode = yearlyRecurringPhase.priceCurrencyCode,
                offerToken = yearlyOffer.offerToken
            )
        } else {
            unloaded.yearlyPlan
        }

        val monthlyOffer = offers.firstOrNull { it.basePlanId == BASE_PLAN_MONTHLY }
        val monthlyRecurringPhase = monthlyOffer?.pricingPhases?.pricingPhaseList
            ?.lastOrNull { it.priceAmountMicros > 0L }

        val monthlyPlan = if (monthlyRecurringPhase != null && monthlyOffer != null) {
            BillingPlanInfo(
                planOption = PlanOption.MONTHLY,
                productId = PRODUCT_SUB_DYNE_PRO,
                basePlanId = BASE_PLAN_MONTHLY,
                formattedPrice = monthlyRecurringPhase.formattedPrice,
                priceAmountMicros = monthlyRecurringPhase.priceAmountMicros,
                priceCurrencyCode = monthlyRecurringPhase.priceCurrencyCode,
                offerToken = monthlyOffer.offerToken
            )
        } else {
            unloaded.monthlyPlan
        }

        val oneTimeOffer = lifetime?.oneTimePurchaseOfferDetails
        val lifetimePlan = if (oneTimeOffer != null) {
            BillingPlanInfo(
                planOption = PlanOption.LIFETIME,
                productId = PRODUCT_INAPP_LIFETIME,
                basePlanId = null,
                formattedPrice = oneTimeOffer.formattedPrice,
                priceAmountMicros = oneTimeOffer.priceAmountMicros,
                priceCurrencyCode = oneTimeOffer.priceCurrencyCode
            )
        } else {
            unloaded.lifetimePlan
        }

        val hasAnyConfiguredProduct = yearlyPlan.hasValidPlayPrice ||
            monthlyPlan.hasValidPlayPrice ||
            lifetimePlan.hasValidPlayPrice

        _pricingState.value = PaywallPricingState(
            yearlyPlan = yearlyPlan,
            lifetimePlan = lifetimePlan,
            monthlyPlan = monthlyPlan,
            isBillingAvailable = isBillingConnected && hasAnyConfiguredProduct,
            isLoading = false
        )
    }

    override fun launchPurchase(activity: Activity, plan: PlanOption) {
        val currentState = _pricingState.value
        if (!isBillingConnected || !currentState.isBillingAvailable) {
            // Never grant Pro when Google Play Billing is unavailable
            _purchaseMessage.value = PaywallPricingState.PLAY_UNAVAILABLE_MESSAGE
            return
        }

        val productDetailsParamsList = when (plan) {
            PlanOption.YEARLY -> {
                val sub = subProductDetails
                val token = currentState.yearlyPlan.offerToken
                if (sub != null && token != null) {
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(sub)
                            .setOfferToken(token)
                            .build()
                    )
                } else null
            }
            PlanOption.MONTHLY -> {
                val sub = subProductDetails
                val token = currentState.monthlyPlan.offerToken
                if (sub != null && token != null) {
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(sub)
                            .setOfferToken(token)
                            .build()
                    )
                } else null
            }
            PlanOption.LIFETIME -> {
                val life = lifetimeProductDetails
                if (life != null) {
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(life)
                            .build()
                    )
                } else null
            }
        }

        if (productDetailsParamsList == null) {
            _purchaseMessage.value = PaywallPricingState.PLAY_UNAVAILABLE_MESSAGE
            return
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun launchSoundPackPurchase(activity: Activity, productId: String) {
        val details = packProductDetailsMap[productId]
        if (!isBillingConnected || details == null) {
            // Never grant Sound Pack when Google Play Billing is unavailable
            _purchaseMessage.value = PaywallPricingState.PLAY_UNAVAILABLE_MESSAGE
            return
        }
        val params = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(params)
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun launchRemoveAdsPurchase(activity: Activity) {
        val details = removeAdsProductDetails
        if (!isBillingConnected || details == null) {
            // Never grant Remove Ads when Google Play Billing is unavailable
            _purchaseMessage.value = PaywallPricingState.PLAY_UNAVAILABLE_MESSAGE
            return
        }
        val params = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(params)
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseMessage.value = "Purchase canceled."
            }
            else -> {
                _purchaseMessage.value = billingResult.debugMessage.ifBlank {
                    PaywallPricingState.PLAY_UNAVAILABLE_MESSAGE
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        val isSub = purchase.products.contains(PRODUCT_SUB_DYNE_PRO)
        val isLifetime = purchase.products.contains(PRODUCT_INAPP_LIFETIME)
        val isRemoveAdsProd = purchase.products.contains(PRODUCT_INAPP_REMOVE_ADS)
        val matchedPacks = purchase.products.filter { prod ->
            SoundPack.fromProductId(prod) != null
        }

        if (!isSub && !isLifetime && !isRemoveAdsProd && matchedPacks.isEmpty()) return

        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                val applyEntitlements = {
                    if (isSub || isLifetime) {
                        val sourceLabel = if (isLifetime) {
                            "Google Play ($PRODUCT_INAPP_LIFETIME)"
                        } else {
                            "Google Play ($PRODUCT_SUB_DYNE_PRO)"
                        }
                        grantVerifiedPlayBillingEntitlement(sourceLabel)
                    }
                    if (isRemoveAdsProd) {
                        grantVerifiedRemoveAdsEntitlement()
                    }
                    if (matchedPacks.isNotEmpty()) {
                        grantVerifiedSoundPacks(matchedPacks)
                    }
                }
                if (!purchase.isAcknowledged) {
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(ackParams) { ackResult ->
                        if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            applyEntitlements()
                        }
                    }
                } else {
                    applyEntitlements()
                }
            }
            Purchase.PurchaseState.PENDING -> {
                _purchaseMessage.value = "Payment pending in Google Play."
                _pricingState.value = _pricingState.value.copy(statusMessage = "Payment pending")
            }
            else -> {}
        }
    }

    private fun grantVerifiedPlayBillingEntitlement(source: String) {
        scope.launch {
            preferencesRepository.setCachedProFromPlayBilling(true, source)
            _pricingState.value = _pricingState.value.copy(statusMessage = null)
            _purchaseMessage.value = "Dyne Premium unlocked via $source."
        }
    }

    private fun grantVerifiedRemoveAdsEntitlement() {
        scope.launch {
            _isRemoveAds.value = true
            preferencesRepository.setCachedRemoveAdsFromPlayBilling(true)
            _purchaseMessage.value = "Remove Ads unlocked via Google Play."
        }
    }

    private fun grantVerifiedSoundPacks(packIds: List<String>) {
        scope.launch {
            val updated = (_ownedSoundPacks.value + packIds.map { it.lowercase() }).toSet()
            _ownedSoundPacks.value = updated
            preferencesRepository.setOwnedSoundPacks(updated)
            val titles = packIds.mapNotNull { SoundPack.fromProductId(it)?.title }.joinToString(", ")
            _purchaseMessage.value = "Sound pack unlocked: $titles"
        }
    }

    private fun queryActivePurchases(notifyUser: Boolean = false) {
        if (!isBillingConnected) {
            if (notifyUser) {
                _purchaseMessage.value = PaywallPricingState.PLAY_UNAVAILABLE_MESSAGE
            }
            return
        }

        val subsQuery = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val inAppQuery = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(subsQuery) { subResult, subPurchases ->
            var hasActiveSub = false
            var hasPending = false
            if (subResult.responseCode == BillingClient.BillingResponseCode.OK) {
                subPurchases.forEach { handlePurchase(it) }
                hasActiveSub = subPurchases.any { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        purchase.products.contains(PRODUCT_SUB_DYNE_PRO)
                }
                if (subPurchases.any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
                    hasPending = true
                }
            }

            billingClient.queryPurchasesAsync(inAppQuery) { inAppResult, inAppPurchases ->
                var hasLifetime = false
                var hasRemoveAds = false
                val activePacks = mutableSetOf<String>()
                if (inAppResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    inAppPurchases.forEach { handlePurchase(it) }
                    hasLifetime = inAppPurchases.any { purchase ->
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            purchase.products.contains(PRODUCT_INAPP_LIFETIME)
                    }
                    hasRemoveAds = inAppPurchases.any { purchase ->
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            purchase.products.contains(PRODUCT_INAPP_REMOVE_ADS)
                    }
                    inAppPurchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                        .forEach { purchase ->
                            purchase.products.forEach { prod ->
                                if (SoundPack.fromProductId(prod) != null) {
                                    activePacks.add(prod.lowercase())
                                }
                            }
                        }
                    if (inAppPurchases.any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
                        hasPending = true
                    }
                }

                val bothQueriesSucceeded =
                    subResult.responseCode == BillingClient.BillingResponseCode.OK &&
                        inAppResult.responseCode == BillingClient.BillingResponseCode.OK
                val foundActive = hasActiveSub || hasLifetime

                scope.launch {
                    if (inAppResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _ownedSoundPacks.value = activePacks
                        preferencesRepository.setOwnedSoundPacks(activePacks)
                        preferencesRepository.setCachedRemoveAdsFromPlayBilling(hasRemoveAds)
                    }

                    if (hasPending) {
                        _purchaseMessage.value = "Payment pending in Google Play."
                        _pricingState.value = _pricingState.value.copy(statusMessage = "Payment pending")
                    }

                    if (foundActive) {
                        val source = if (hasLifetime) {
                            "Google Play ($PRODUCT_INAPP_LIFETIME)"
                        } else {
                            "Google Play ($PRODUCT_SUB_DYNE_PRO)"
                        }
                        preferencesRepository.setCachedProFromPlayBilling(true, source)
                        if (notifyUser) {
                            _purchaseMessage.value = "Purchases restored — Dyne Premium is active."
                        }
                    } else if (bothQueriesSucceeded) {
                        preferencesRepository.setCachedProFromPlayBilling(false, "None")
                        if (notifyUser && !hasPending) {
                            _purchaseMessage.value = when {
                                hasRemoveAds && activePacks.isNotEmpty() ->
                                    "Restored Remove Ads and ${activePacks.size} Sound Pack(s) from Google Play."
                                hasRemoveAds ->
                                    "Purchases restored — Remove Ads is active."
                                activePacks.isNotEmpty() ->
                                    "Restored ${activePacks.size} Sound Pack(s) from Google Play."
                                else ->
                                    "No active dyne_pro subscription, dyne_lifetime, Remove Ads, or Sound Pack purchase found in Google Play."
                            }
                        }
                    }
                }
            }
        }
    }

    override fun restorePurchases() {
        queryActivePurchases(notifyUser = true)
    }

    override fun setSimulatePro(enabled: Boolean) {
        if (!BuildConfig.DEBUG) return
        _isPro.value = enabled
        _proSource.value = if (enabled) "DEBUG Simulate Pro" else "None (Free)"
        scope.launch {
            preferencesRepository.setSimulateProEnabled(enabled)
        }
    }

    override fun clearProDebugState() {
        if (!BuildConfig.DEBUG) return
        _isPro.value = false
        _proSource.value = "None (Free)"
        _ownedSoundPacks.value = emptySet()
        _isRemoveAds.value = false
        scope.launch {
            preferencesRepository.clearProDebugStateAndUnlocks()
            preferencesRepository.setCachedRemoveAdsFromPlayBilling(false)
            preferencesRepository.setOwnedSoundPacks(emptySet())
            _purchaseMessage.value = "Cleared Pro debug state & 1-hour temporary unlocks."
        }
    }

    override fun clearMessage() {
        _purchaseMessage.value = null
    }

    companion object {
        const val PRODUCT_SUB_DYNE_PRO = "dyne_pro"
        const val BASE_PLAN_MONTHLY = "monthly"
        const val BASE_PLAN_YEARLY = "yearly"
        const val PRODUCT_INAPP_LIFETIME = "dyne_lifetime"
        const val PRODUCT_INAPP_REMOVE_ADS = "dyne_remove_ads"
        const val PRODUCT_PACK_PETS = "dyne_pack_pets"
        const val PRODUCT_PACK_RELAX = "dyne_pack_relax"
        const val PRODUCT_PACK_PRO = "dyne_pack_pro"
    }
}
