package com.example.ads

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billing.FeatureGate
import com.example.billing.UnlockPromptState
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassSheet
import com.example.ui.components.HazeState
import com.example.ui.components.ProBadge
import com.example.ui.components.proBorder
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import com.example.ui.theme.ProGoldGradient
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Inline reserved Banner Ad slot inside a scroll layout:
 * - Uses only the real Google Mobile Ads SDK [AdView] with Anchored Adaptive size.
 * - Immediately vanishes with zero height (no gap) when Pro is active or when not loaded.
 * - Contains no UI debug labels or fake ad containers.
 */
@Composable
fun InlineBannerAdSlot(
    placementTag: String,
    modifier: Modifier = Modifier
) {
    val adGate = LocalAdGate.current ?: return
    val adManager = LocalAdManager.current ?: return
    val isPro by adGate.isPro.collectAsStateWithLifecycle()
    val isRemoveAds by adGate.isRemoveAds.collectAsStateWithLifecycle()
    val isSoundPlaying by adManager.isSoundPlaying.collectAsStateWithLifecycle()

    if (isPro || isRemoveAds || isSoundPlaying || !AdConfig.isBannerEnabled()) return

    var sdkBannerLoaded by remember(placementTag) { mutableStateOf(false) }
    var adViewRef by remember(placementTag) { mutableStateOf<AdView?>(null) }

    DisposableEffect(placementTag) {
        onDispose {
            adViewRef?.destroy()
            adViewRef = null
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (sdkBannerLoaded) {
                    Modifier
                        .padding(vertical = 8.dp)
                        .wrapContentHeight()
                } else {
                    Modifier.height(0.dp)
                }
            )
            .clipToBounds()
            .testTag("inline_banner_$placementTag")
    ) {
        val adWidthDp = maxWidth.value.toInt().coerceAtLeast(320)

        AndroidView(
            factory = { ctx ->
                AdView(ctx).apply {
                    adViewRef = this
                    val adaptiveSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                        ctx,
                        adWidthDp
                    )
                    setAdSize(adaptiveSize)
                    adUnitId = AdConfig.BANNER
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            sdkBannerLoaded = true
                            adManager.notifyBannerLoaded(placementTag)
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            sdkBannerLoaded = false
                            adManager.notifyBannerFailed(error.code, error.message, placementTag)
                        }

                        override fun onAdClicked() {
                            adManager.logEvent("Banner clicked [$placementTag]")
                        }

                        override fun onAdImpression() {
                            adManager.logEvent("Banner impression [$placementTag]")
                        }
                    }
                    adManager.notifyBannerLoading(placementTag)
                    loadAd(AdRequest.Builder().build())
                }
            },
            modifier = if (sdkBannerLoaded) {
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(0.dp)
            }
        )
    }
}

/**
 * Unified 1-Hour Unlock Sheet (glass, gold accents):
 * - "Watch ad: unlock for 1 hour" (real RewardedAd, grant only in onUserEarnedReward;
 *   if no ad is ready show "No ad available, try again soon" and never unlock)
 * - "Use 10 coins: unlock for 1 hour" (or 15 coins for themes, 10 coins for extra doc save / CSV or PDF export)
 * - "Go Pro"
 */
@Composable
fun FeatureUnlockGlassSheet(
    prompt: UnlockPromptState,
    featureGate: FeatureGate,
    adManager: AdManager,
    coinBalance: Int,
    hazeState: HazeState?,
    onOpenPaywall: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var statusMessage by remember(prompt) { mutableStateOf<String?>(null) }
    val packPrices by featureGate.purchaseManager.soundPackPrices.collectAsStateWithLifecycle()
    val purchaseMessage by featureGate.purchaseManager.purchaseMessage.collectAsStateWithLifecycle()

    val isPackRequest = prompt.soundPack != null
    val isQuotaRequest = prompt.quotaType != null
    val isThemeRequest = !prompt.themeName.isNullOrBlank()
    val watchAdLabel = when {
        isQuotaRequest -> "Watch an ad (+1 use)"
        isThemeRequest -> "Watch ad: try this theme for 1 hour"
        else -> "Watch ad: unlock for 1 hour"
    }
    val coinButtonLabel = if (isQuotaRequest) {
        "Use 10 coins (+1 use)"
    } else {
        "Use 10 coins: unlock for 1 hour"
    }

    GlassSheet(
        onDismissRequest = onDismiss,
        hazeState = hazeState
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .proBorder(cornerRadius = 24.dp, borderWidth = 1.dp)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(brush = ProGoldGradient)
                        .border(1.dp, ProGold, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = BentoTextBlack,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProBadge()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(ProGold.copy(alpha = 0.16f))
                                .border(1.dp, ProGold.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "$coinBalance coins",
                                style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                                color = ProGold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = prompt.displayTitle,
                        style = type.title.copy(fontWeight = FontWeight.ExtraBold),
                        color = theme.textPrimary
                    )
                }
            }

            Text(
                text = prompt.displayDescription,
                style = type.body,
                color = theme.textSecondary
            )

            val activeMsg = statusMessage ?: purchaseMessage
            if (!activeMsg.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ProGold.copy(alpha = 0.14f))
                        .border(1.dp, ProGold.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("unlock_sheet_status_message")
                ) {
                    Text(
                        text = activeMsg,
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = ProGold
                    )
                }
            }

            // 1. Watch ad: unlock for 1 hour (or +1 use at daily quota limit)
            GlassButton(
                text = watchAdLabel,
                icon = Icons.Default.PlayCircleFilled,
                accentFill = ProGold.copy(alpha = 0.22f),
                textColor = ProGold,
                onClick = {
                    statusMessage = null
                    featureGate.purchaseManager.clearMessage()
                    adManager.showRewardedAd(
                        activity = activity,
                        rewardDescription = if (isQuotaRequest) "+1 Use (${prompt.displayTitle})" else "Unlock ${prompt.displayTitle} (1h)",
                        onAdUnavailable = { msg ->
                            statusMessage = msg
                        },
                        onRewardEarned = {
                            featureGate.grant1HourUnlockFromRewardedAd(prompt)
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "unlock_sheet_watch_ad_button"
            )

            // 2. Use 10 coins: unlock for 1 hour (or 15 for theme)
            GlassButton(
                text = coinButtonLabel,
                icon = Icons.Default.LockOpen,
                accentFill = ProGold.copy(alpha = 0.18f),
                textColor = ProGold,
                onClick = {
                    statusMessage = null
                    featureGate.purchaseManager.clearMessage()
                    featureGate.unlockWithCoins(prompt) { _, msg ->
                        statusMessage = msg
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "unlock_sheet_use_coins_button"
            )

            // Optional 3. "Buy this pack" (price from Play Billing, never hardcoded)
            val targetPack = prompt.soundPack
            if (targetPack != null) {
                val playPrice = packPrices[targetPack.productId].orEmpty()
                val buyPackLabel = if (playPrice.isNotBlank()) {
                    "Buy this pack • $playPrice"
                } else {
                    "Buy this pack"
                }
                GlassButton(
                    text = buyPackLabel,
                    icon = Icons.Default.WorkspacePremium,
                    accentFill = ProGold.copy(alpha = 0.28f),
                    textColor = ProGold,
                    onClick = {
                        statusMessage = null
                        if (activity != null) {
                            featureGate.purchaseManager.launchSoundPackPurchase(
                                activity = activity,
                                productId = targetPack.productId
                            )
                        } else {
                            statusMessage = "Purchases work when Dyne is installed from Google Play"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "unlock_sheet_buy_pack_button"
                )
            }

            // 4. Go Premium
            GlassButton(
                text = "Go Premium",
                icon = Icons.Default.WorkspacePremium,
                accentFill = ProGold,
                textColor = BentoTextBlack,
                onClick = {
                    onDismiss()
                    onOpenPaywall()
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "unlock_sheet_go_pro_button"
            )

            if (isQuotaRequest) {
                Text(
                    text = "Resets automatically at local midnight • Core free tools are never blocked.",
                    style = type.caption,
                    color = theme.textSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/**
 * Legacy compatibility wrapper routing to 1-hour unlock behavior.
 */
@Composable
fun RewardedUnlockGlassSheet(
    target: RewardUnlockPromptTarget,
    adManager: AdManager,
    hazeState: HazeState?,
    onOpenPaywall: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    var statusMessage by remember(target) { mutableStateOf<String?>(null) }

    val (title, subtitle) = when (target) {
        is RewardUnlockPromptTarget.LockedTool -> Pair(
            "Unlock ${target.toolName}",
            "Watch a rewarded ad to unlock ${target.toolName} for 1 hour, or upgrade to Dyne Premium."
        )
        is RewardUnlockPromptTarget.LockedTheme -> Pair(
            "Unlock ${target.themeName} Theme",
            "Watch a rewarded ad to unlock ${target.themeName} for 1 hour, or upgrade to Dyne Premium."
        )
        is RewardUnlockPromptTarget.ExtraDocumentSave -> Pair(
            "Free Document Limit Reached",
            "Watch a rewarded ad to unlock 1 extra document save, or upgrade to Dyne Premium."
        )
        is RewardUnlockPromptTarget.OneCsvExport -> Pair(
            "Unlock CSV Export",
            "Watch a rewarded ad to unlock 1 CSV export, or upgrade to Dyne Premium."
        )
    }

    GlassSheet(
        onDismissRequest = onDismiss,
        hazeState = hazeState
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .proBorder(cornerRadius = 24.dp, borderWidth = 1.dp)
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = type.title.copy(fontWeight = FontWeight.ExtraBold),
                color = theme.textPrimary
            )
            Text(
                text = subtitle,
                style = type.body,
                color = theme.textSecondary
            )

            if (!statusMessage.isNullOrBlank()) {
                Text(
                    text = statusMessage ?: "",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = ProGold
                )
            }

            GlassButton(
                text = "Watch ad: unlock for 1 hour",
                icon = Icons.Default.PlayCircleFilled,
                accentFill = ProGold.copy(alpha = 0.22f),
                textColor = ProGold,
                onClick = {
                    statusMessage = null
                    adManager.showRewardedAd(
                        activity = activity,
                        rewardDescription = title,
                        onAdUnavailable = { msg -> statusMessage = msg },
                        onRewardEarned = {
                            when (target) {
                                is RewardUnlockPromptTarget.LockedTool ->
                                    adManager.grantTool1hUnlock(target.toolId, target.toolName) {
                                        target.onUnlocked()
                                    }
                                is RewardUnlockPromptTarget.LockedTheme ->
                                    adManager.grantTheme1hUnlock(target.themeName) {
                                        target.onUnlocked()
                                    }
                                is RewardUnlockPromptTarget.ExtraDocumentSave ->
                                    adManager.grantOneExtraDocumentSave { target.onUnlocked() }
                                is RewardUnlockPromptTarget.OneCsvExport ->
                                    adManager.grantOneCsvExport { target.onUnlocked() }
                            }
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "rewarded_watch_ad_button"
            )

            GlassButton(
                text = "Go Pro",
                icon = Icons.Default.WorkspacePremium,
                accentFill = ProGold,
                textColor = BentoTextBlack,
                onClick = {
                    onDismiss()
                    onOpenPaywall()
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "rewarded_open_paywall_button"
            )
        }
    }
}
