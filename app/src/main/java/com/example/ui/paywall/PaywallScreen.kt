package com.example.ui.paywall

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billing.PlanOption
import com.example.domain.FeatureRegistry
import com.example.domain.ToolCategory
import com.example.ui.components.HazeState
import com.example.ui.components.dyneClickable
import com.example.ui.components.proBorder
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.DesignTokens
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import com.example.ui.theme.ProGoldDeep
import com.example.ui.theme.ProGoldGradient

private data class PaywallWhatYouGetGroup(
    val id: String,
    val title: String,
    val items: List<Pair<String, String>>
)

/**
 * Full-screen Paywall route (replaces small popup).
 * Verified at 360x640dp and fontScale 1.3 so that both the plan prices and the sticky buy button
 * are immediately visible WITHOUT scrolling.
 * - Top: always-visible close button (48dp touch target), DyneLogo(56dp), "Dyne Premium" (28sp),
 *   and one line "No ads. Every feature."
 * - Three plan cards right below, each at least 72dp high:
 *   Yearly (pre-selected, larger, "BEST VALUE", "Save X%", "just Y per month" computed from Play Billing),
 *   Lifetime ("Pay once"), Monthly.
 *   Prices come strictly from ProductDetails (formattedPrice, priceAmountMicros, priceCurrencyCode),
 *   showing a skeleton until loaded and a clear retry message if Play Billing is unavailable.
 * - Remove Ads one-time card below the plans, small.
 * - Scrollable area: "What you get" in 9 collapsible groups (collapsed by default, with counts):
 *   Tools, Outdoors, Documents and PDF, Media Studio, Notes and Drawing, Alerts and Widgets,
 *   Themes and Customization, Backup and Privacy, and No ads. Plus a "See all features" link.
 * - Sticky bottom bar: selected plan's price and period in one line, 52dp "Subscribe" or "Get Lifetime"
 *   button, "Maybe later", "Restore  -  Terms  -  Privacy", and "Cancel anytime in Google Play".
 * - Premium users never see it (auto-dismisses if isPro becomes true).
 */
@Composable
fun PaywallScreen(
    viewModel: PaywallViewModel,
    hazeState: HazeState? = null,
    onDismiss: () -> Unit,
    onDismissMaybeLater: () -> Unit = onDismiss,
    onOpenAllFeatures: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val pricing by viewModel.pricingState.collectAsStateWithLifecycle()
    val selectedPlan by viewModel.selectedPlan.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val isRemoveAds by viewModel.isRemoveAds.collectAsStateWithLifecycle()
    val removeAdsPrice by viewModel.removeAdsPrice.collectAsStateWithLifecycle()
    val purchaseMessage by viewModel.purchaseMessage.collectAsStateWithLifecycle()

    // Premium users never see the Paywall; Manage subscription lives in Settings.
    LaunchedEffect(isPro) {
        if (isPro) {
            onDismiss()
        }
    }
    if (isPro) return

    BackHandler(onBack = onDismiss)

    var activeLegalSection by remember { mutableStateOf<String?>(null) }
    var showPrivacyPolicyViewer by remember { mutableStateOf(false) }
    var expandedGroups by remember { mutableStateOf(setOf<String>()) }

    val whatYouGetGroups = remember {
        buildWhatYouGetGroups()
    }

    val yearlyHeadline = when {
        pricing.isLoading -> "Loading…"
        pricing.yearlyPlan.hasValidPlayPrice -> "${pricing.yearlyPlan.formattedPrice} / yr"
        else -> "—"
    }
    val yearlyPerMonthLine = when {
        pricing.isLoading -> "Checking Google Play price…"
        pricing.yearlyPerMonthFormatted.isNotBlank() -> "just ${pricing.yearlyPerMonthFormatted} per month"
        else -> "Billed yearly via Google Play"
    }
    val lifetimePriceText = when {
        pricing.isLoading -> "Loading…"
        pricing.lifetimePlan.hasValidPlayPrice -> pricing.lifetimePlan.formattedPrice
        else -> "—"
    }
    val monthlyPriceText = when {
        pricing.isLoading -> "Loading…"
        pricing.monthlyPlan.hasValidPlayPrice -> "${pricing.monthlyPlan.formattedPrice} / mo"
        else -> "—"
    }

    val selectedPlanStickySummary = when (selectedPlan) {
        PlanOption.YEARLY -> {
            val pricePart = when {
                pricing.isLoading -> "Loading price…"
                pricing.yearlyPlan.hasValidPlayPrice -> "${pricing.yearlyPlan.formattedPrice} / year"
                else -> "Price via Google Play / year"
            }
            val perMoSuffix = if (pricing.yearlyPerMonthFormatted.isNotBlank()) {
                " (${pricing.yearlyPerMonthFormatted}/mo)"
            } else ""
            "Yearly • $pricePart$perMoSuffix"
        }
        PlanOption.LIFETIME -> {
            val pricePart = when {
                pricing.isLoading -> "Loading price…"
                pricing.lifetimePlan.hasValidPlayPrice -> "${pricing.lifetimePlan.formattedPrice} one-time"
                else -> "One-time purchase via Google Play"
            }
            "Lifetime • $pricePart • Pay once"
        }
        PlanOption.MONTHLY -> {
            val pricePart = when {
                pricing.isLoading -> "Loading price…"
                pricing.monthlyPlan.hasValidPlayPrice -> "${pricing.monthlyPlan.formattedPrice} / month"
                else -> "Price via Google Play / month"
            }
            "Monthly • $pricePart"
        }
    }

    val ctaLabel = when (selectedPlan) {
        PlanOption.YEARLY -> "Subscribe"
        PlanOption.MONTHLY -> "Subscribe"
        PlanOption.LIFETIME -> "Get Lifetime"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("paywall_fullscreen_route")
    ) {
        // 1. PINNED TOP HEADER: Logo (56dp), "Dyne Premium" (28sp), "No ads. Every feature.", always-visible Close button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignTokens.ScreenMargin,
                    vertical = DesignTokens.Space8
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Space12),
                modifier = Modifier.weight(1f)
            ) {
                com.example.DyneLogo(size = 56.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dyne Premium",
                        style = type.headline.copy(
                            fontSize = DesignTokens.HeadlineTextSize,
                            fontWeight = DesignTokens.TitleWeight,
                            brush = ProGoldGradient
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("paywall_headline_title")
                    )
                    Text(
                        text = "No ads. Every feature.",
                        style = type.label.copy(
                            fontSize = DesignTokens.LabelTextSize,
                            fontWeight = DesignTokens.LabelWeight
                        ),
                        color = theme.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("paywall_subtitle_line")
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(DesignTokens.MinTouchTarget)
                    .clip(CircleShape)
                    .background(theme.elevated)
                    .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                    .dyneClickable(
                        testTag = "paywall_close_button",
                        onClick = onDismiss
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Paywall",
                    tint = theme.textPrimary,
                    modifier = Modifier.size(DesignTokens.IconSize)
                )
            }
        }

        // 2. MIDDLE SCROLLABLE AREA (Plan cards right at the top, then Remove Ads card, then "What you get" groups)
        Column(
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Space8),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = DesignTokens.ScreenMargin,
                    vertical = DesignTokens.Space8
                )
        ) {
            if (!purchaseMessage.isNullOrBlank()) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(DesignTokens.Space16))
                        .background(theme.elevated)
                        .border(1.dp, ProGold.copy(alpha = 0.65f), RoundedCornerShape(DesignTokens.Space16))
                        .padding(horizontal = DesignTokens.Space12, vertical = DesignTokens.Space8)
                        .testTag("paywall_status_message")
                ) {
                    Text(
                        text = purchaseMessage ?: "",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = ProGold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss message",
                        tint = theme.textSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .dyneClickable(onClick = viewModel::clearMessage)
                    )
                }
            }

            // PLAN CARD 1: YEARLY (pre-selected, larger, >= 72dp high, "BEST VALUE", "Save X%", "just Y per month")
            YearlyPlanCard(
                headlinePricePerYear = yearlyHeadline,
                perMonthLine = yearlyPerMonthLine,
                savingsPercent = pricing.yearlySavingsPercent,
                isLoading = pricing.isLoading,
                selected = selectedPlan == PlanOption.YEARLY,
                onClick = { viewModel.selectPlan(PlanOption.YEARLY) }
            )

            // PLAN CARD 2: LIFETIME ("Pay once", >= 72dp high)
            StandardPlanCard(
                title = "Lifetime",
                subtitle = "Pay once",
                price = lifetimePriceText,
                isLoading = pricing.isLoading,
                selected = selectedPlan == PlanOption.LIFETIME,
                testTag = "plan_card_lifetime",
                onClick = { viewModel.selectPlan(PlanOption.LIFETIME) }
            )

            // PLAN CARD 3: MONTHLY (>= 72dp high)
            StandardPlanCard(
                title = "Monthly",
                subtitle = "Flexible monthly billing",
                price = monthlyPriceText,
                isLoading = pricing.isLoading,
                selected = selectedPlan == PlanOption.MONTHLY,
                testTag = "plan_card_monthly",
                onClick = { viewModel.selectPlan(PlanOption.MONTHLY) }
            )

            // Clear retry message if Play Billing is unavailable
            if (!pricing.isLoading && !pricing.isBillingAvailable) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(DesignTokens.Space16))
                        .background(theme.elevated)
                        .border(1.dp, ProGold.copy(alpha = 0.45f), RoundedCornerShape(DesignTokens.Space16))
                        .padding(horizontal = DesignTokens.Space12, vertical = DesignTokens.Space8)
                        .testTag("paywall_billing_unavailable_card")
                ) {
                    Text(
                        text = "Play Billing unavailable. Connect to Google Play to load local prices.",
                        style = type.caption,
                        color = theme.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.Space8))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(DesignTokens.Space12))
                            .background(ProGold.copy(alpha = 0.18f))
                            .border(1.dp, ProGold.copy(alpha = 0.6f), RoundedCornerShape(DesignTokens.Space12))
                            .dyneClickable(
                                testTag = "paywall_retry_billing_button",
                                onClick = { viewModel.notifyPlayBillingUnavailable() }
                            )
                            .padding(horizontal = DesignTokens.Space12, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry Play Billing",
                            tint = ProGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Retry",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = ProGold
                        )
                    }
                }
            }

            // Small Remove Ads one-time card below the plans
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DesignTokens.Space16))
                    .background(theme.surface)
                    .border(
                        width = 1.dp,
                        color = if (isRemoveAds) ProGold.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(DesignTokens.Space16)
                    )
                    .dyneClickable(
                        testTag = "plan_card_remove_ads",
                        onClick = {
                            if (!isRemoveAds) {
                                if (activity != null) {
                                    viewModel.launchRemoveAdsPurchase(activity)
                                } else {
                                    viewModel.notifyPlayBillingUnavailable()
                                }
                            }
                        }
                    )
                    .padding(horizontal = DesignTokens.Space12, vertical = DesignTokens.Space8)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Remove Ads (One-time)",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.textPrimary
                    )
                    Text(
                        text = if (isRemoveAds) "Active on this device"
                        else "Removes ads only • Dyne Premium already includes No ads",
                        style = type.caption,
                        color = theme.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(DesignTokens.Space8))
                Text(
                    text = when {
                        isRemoveAds -> "Owned ✓"
                        pricing.isLoading -> "…"
                        removeAdsPrice.isNotBlank() -> removeAdsPrice
                        else -> "Google Play"
                    },
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = ProGold
                )
            }

            // "What you get" in 9 collapsible groups (collapsed by default, with counts) + "See all features"
            Column(
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Space8),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DesignTokens.CardShape)
                    .background(theme.surface)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), DesignTokens.CardShape)
                    .padding(DesignTokens.Space16)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "What you get",
                        style = type.section.copy(
                            fontSize = DesignTokens.SectionHeaderTextSize,
                            fontWeight = DesignTokens.SectionHeaderWeight
                        ),
                        color = theme.textPrimary
                    )
                    Text(
                        text = "See all features",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = ProGold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(DesignTokens.Space8))
                            .dyneClickable(
                                testTag = "paywall_see_all_features_button",
                                onClick = onOpenAllFeatures
                            )
                            .padding(horizontal = DesignTokens.Space8, vertical = 4.dp)
                    )
                }

                whatYouGetGroups.forEach { group ->
                    val isExpanded = group.id in expandedGroups
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(DesignTokens.Space12))
                            .background(theme.elevated.copy(alpha = 0.75f))
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = DesignTokens.MinTouchTarget)
                                .clickable {
                                    expandedGroups = if (isExpanded) {
                                        expandedGroups - group.id
                                    } else {
                                        expandedGroups + group.id
                                    }
                                }
                                .padding(horizontal = DesignTokens.Space12, vertical = DesignTokens.Space8)
                                .testTag("paywall_group_${group.id}")
                        ) {
                            Text(
                                text = "${group.title} (${group.items.size})",
                                style = type.label.copy(
                                    fontSize = DesignTokens.LabelTextSize,
                                    fontWeight = DesignTokens.LabelWeight
                                ),
                                color = theme.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse ${group.title}" else "Expand ${group.title}",
                                tint = theme.textSecondary,
                                modifier = Modifier.size(DesignTokens.IconSize)
                            )
                        }
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(DesignTokens.Space8),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = DesignTokens.Space12,
                                        end = DesignTokens.Space12,
                                        bottom = DesignTokens.Space12
                                    )
                            ) {
                                group.items.forEach { (title, benefit) ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = ProGold,
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(DesignTokens.Space8))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = title,
                                                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                                                    color = theme.textPrimary
                                                )
                                                Text(
                                                    text = benefit,
                                                    style = type.caption,
                                                    color = theme.textSecondary
                                                )
                                            }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (activeLegalSection != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Space8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(DesignTokens.Space16))
                        .background(theme.elevated)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(DesignTokens.Space16))
                        .padding(DesignTokens.Space12)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (activeLegalSection == "terms") "Terms of Use" else "Privacy Policy",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = ProGold
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = theme.textSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .dyneClickable(onClick = { activeLegalSection = null })
                        )
                    }
                    Text(
                        text = if (activeLegalSection == "terms") {
                            "Subscriptions auto-renew through your Google Play account unless canceled at least 24 hours before the end of the current billing period. Manage or cancel anytime in Google Play > Subscriptions. Privacy Policy: ${com.example.legal.LegalConfig.PRIVACY_POLICY_URL}"
                        } else {
                            "No account or login is required. All sensor readings, documents, scans, and notes stay locally on your device. Full Privacy Policy: ${com.example.legal.LegalConfig.PRIVACY_POLICY_URL}"
                        },
                        style = type.caption,
                        color = theme.textSecondary
                    )
                }
            }
        }

        // 3. STICKY BOTTOM BAR: selected plan's price & period in one line, 52dp CTA button, Restore - Terms - Privacy, Cancel anytime
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.surface)
                .padding(
                    horizontal = DesignTokens.ScreenMargin,
                    vertical = DesignTokens.Space8
                )
        ) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Text(
                text = selectedPlanStickySummary,
                style = type.caption.copy(
                    fontSize = DesignTokens.CaptionTextSize,
                    fontWeight = FontWeight.SemiBold
                ),
                color = theme.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("paywall_sticky_price_line")
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DesignTokens.ButtonHeight)
                    .shadow(
                        elevation = 8.dp,
                        shape = DesignTokens.ButtonShape,
                        ambientColor = ProGold,
                        spotColor = ProGoldDeep
                    )
                    .clip(DesignTokens.ButtonShape)
                    .background(brush = ProGoldGradient)
                    .border(1.dp, ProGold, DesignTokens.ButtonShape)
                    .dyneClickable(
                        targetScale = 0.97f,
                        testTag = "paywall_unlock_button",
                        onClick = {
                            if (activity != null) {
                                viewModel.launchPurchase(activity)
                            } else {
                                viewModel.notifyPlayBillingUnavailable()
                            }
                        }
                    )
                    .padding(horizontal = DesignTokens.Space16)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = BentoTextBlack,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.Space8))
                    Text(
                        text = ctaLabel,
                        style = type.button.copy(
                            fontSize = DesignTokens.ButtonTextSize,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = BentoTextBlack,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Restore",
                    style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = ProGold,
                    modifier = Modifier
                        .dyneClickable(
                            testTag = "paywall_restore_button",
                            onClick = { viewModel.restorePurchases() }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Text(" - ", style = type.caption, color = theme.textSecondary)
                Text(
                    text = "Terms",
                    style = type.caption,
                    color = theme.textSecondary,
                    modifier = Modifier
                        .dyneClickable(
                            testTag = "paywall_terms_button",
                            onClick = {
                                activeLegalSection = if (activeLegalSection == "terms") null else "terms"
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Text(" - ", style = type.caption, color = theme.textSecondary)
                Text(
                    text = "Privacy",
                    style = type.caption,
                    color = theme.textSecondary,
                    modifier = Modifier
                        .dyneClickable(
                            testTag = "paywall_privacy_button",
                            onClick = { showPrivacyPolicyViewer = true }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Text(" - ", style = type.caption, color = theme.textSecondary)
                Text(
                    text = "Maybe later",
                    style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = theme.textSecondary,
                    modifier = Modifier
                        .dyneClickable(
                            testTag = "paywall_maybe_later_button",
                            onClick = onDismissMaybeLater
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Text(
                text = "Cancel anytime in Google Play",
                style = type.caption.copy(fontSize = DesignTokens.CaptionTextSize),
                color = theme.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("paywall_renewal_line")
            )
        }
    }

    if (showPrivacyPolicyViewer) {
        com.example.legal.PrivacyPolicyViewerDialog(
            onDismiss = { showPrivacyPolicyViewer = false }
        )
    }
}

/**
 * Alias for compatibility with existing call sites that invoke PaywallSheet.
 */
@Composable
fun PaywallSheet(
    viewModel: PaywallViewModel,
    hazeState: HazeState?,
    onDismiss: () -> Unit,
    onDismissMaybeLater: () -> Unit = onDismiss,
    onOpenAllFeatures: () -> Unit = {}
) {
    PaywallScreen(
        viewModel = viewModel,
        hazeState = hazeState,
        onDismiss = onDismiss,
        onDismissMaybeLater = onDismissMaybeLater,
        onOpenAllFeatures = onOpenAllFeatures
    )
}

@Composable
private fun YearlyPlanCard(
    headlinePricePerYear: String,
    perMonthLine: String,
    savingsPercent: Int,
    isLoading: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .shadow(
                elevation = if (selected) 12.dp else 0.dp,
                shape = DesignTokens.CardShape,
                ambientColor = ProGold,
                spotColor = ProGoldDeep
            )
            .clip(DesignTokens.CardShape)
            .background(if (selected) theme.elevated else theme.surface)
            .proBorder(
                cornerRadius = DesignTokens.CardRadius,
                borderWidth = if (selected) 2.dp else 1.dp,
                enableShimmer = selected
            )
            .dyneClickable(
                targetScale = 0.98f,
                testTag = "plan_card_yearly",
                onClick = onClick
            )
            .padding(horizontal = DesignTokens.Space16, vertical = DesignTokens.Space12)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Space8),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(DesignTokens.Space8))
                    .background(brush = ProGoldGradient)
                    .padding(horizontal = DesignTokens.Space8, vertical = 2.dp)
            ) {
                Text(
                    text = "BEST VALUE",
                    style = type.caption.copy(
                        fontSize = DesignTokens.MinAllowedTextSize,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = BentoTextBlack,
                    maxLines = 1,
                    softWrap = false
                )
            }
            if (savingsPercent > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(DesignTokens.Space8))
                        .background(ProGold.copy(alpha = 0.16f))
                        .border(1.dp, ProGold.copy(alpha = 0.55f), RoundedCornerShape(DesignTokens.Space8))
                        .padding(horizontal = DesignTokens.Space8, vertical = 2.dp)
                ) {
                    Text(
                        text = "Save $savingsPercent%",
                        style = type.caption.copy(
                            fontSize = DesignTokens.MinAllowedTextSize,
                            fontWeight = FontWeight.Bold
                        ),
                        color = ProGold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Yearly",
                    style = type.title.copy(
                        fontSize = DesignTokens.TitleTextSize,
                        fontWeight = DesignTokens.TitleWeight
                    ),
                    color = theme.textPrimary
                )
                Text(
                    text = perMonthLine,
                    style = type.caption.copy(
                        fontSize = DesignTokens.CaptionTextSize,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = theme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .width(88.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(DesignTokens.Space8))
                        .background(Color.White.copy(alpha = 0.12f))
                        .testTag("paywall_price_skeleton")
                )
            } else {
                Text(
                    text = headlinePricePerYear,
                    style = type.title.copy(
                        fontSize = DesignTokens.TitleTextSize,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = if (selected) ProGold else theme.textPrimary
                )
            }
        }
    }
}

@Composable
private fun StandardPlanCard(
    title: String,
    subtitle: String,
    price: String,
    isLoading: Boolean,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val borderMod = if (selected) {
        Modifier.proBorder(cornerRadius = DesignTokens.CardRadius, borderWidth = 1.5.dp)
    } else {
        Modifier.border(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.10f),
            shape = DesignTokens.CardShape
        )
    }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(DesignTokens.CardShape)
            .background(if (selected) theme.elevated else theme.surface)
            .then(borderMod)
            .dyneClickable(
                targetScale = 0.98f,
                testTag = testTag,
                onClick = onClick
            )
            .padding(horizontal = DesignTokens.Space16, vertical = DesignTokens.Space12)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = type.title.copy(
                    fontSize = DesignTokens.TitleTextSize,
                    fontWeight = DesignTokens.TitleWeight
                ),
                color = theme.textPrimary
            )
            Text(
                text = subtitle,
                style = type.caption.copy(fontSize = DesignTokens.CaptionTextSize),
                color = theme.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isLoading) {
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(DesignTokens.Space8))
                    .background(Color.White.copy(alpha = 0.12f))
            )
        } else {
            Text(
                text = price,
                style = type.title.copy(
                    fontSize = DesignTokens.TitleTextSize,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = if (selected) ProGold else theme.textPrimary
            )
        }
    }
}

private fun buildWhatYouGetGroups(): List<PaywallWhatYouGetGroup> {
    val premium = FeatureRegistry.premiumFeatures

    val outdoorToolIds = setOf(
        "compass", "compass_waypoints", "waypoints_pro", "qibla_finder", "parking_saver",
        "weather_calculators", "pedometer", "pedometer_history", "speedometer", "altimeter",
        "walk_run_tracker", "fishing_log", "sky_dome", "solunar", "weather", "sun_moon"
    )
    val docsPdfToolIds = setOf(
        "scanner_document", "scanner_id_card", "scanner_ocr", "scanner_qr", "qr_generator",
        "pdf_tools", "pdf_merge", "pdf_split", "pdf_compress", "pdf_sign", "pdf_watermark",
        "pdf_reorder", "pdf_annotate"
    )
    val notesDrawingToolIds = setOf(
        "quick_note", "drawing_pad", "trip_gear_checklists", "markdown_notes"
    )

    val outdoorsItems = premium
        .filter { it.toolId in outdoorToolIds || it.id.startsWith("outdoor_") || it.id.startsWith("sky_") || it.id.startsWith("solunar_") || it.id.startsWith("weather_") }
        .map { it.title to it.shortBenefit }
        .ifEmpty {
            listOf(
                "7-Day Outdoor Planner" to "Fishing, Hunting & Photography ranking with Best Day card",
                "Monthly Sun & Moon Calendar" to "Full month moon phases and top Solunar days",
                "Interactive Sky Dome & 24h Scrubber" to "Live sun/moon path, golden hour, and air quality"
            )
        }

    val docsPdfItems = premium
        .filter { it.toolId in docsPdfToolIds || it.id.startsWith("pdf_") || it.id.startsWith("scanner_") }
        .map { it.title to it.shortBenefit }
        .ifEmpty {
            listOf(
                "Unlimited Document & ID Scans" to "Multi-page document vault, OCR text extraction, and PDF export",
                "Full PDF Suite" to "Merge, split, compress, sign, reorder, and watermark PDFs without limits"
            )
        }

    val mediaStudioItems = premium
        .filter { it.category == ToolCategory.MEDIA_STUDIO && it.toolId !in docsPdfToolIds }
        .map { it.title to it.shortBenefit }
        .ifEmpty {
            listOf(
                "Batch Photo Converter & Editor" to "Up to 50 photos at full resolution (JPG, PNG, WebP, HEIC, AVIF)",
                "HD Background Remover & Passport ID" to "On-device subject cutout and printable passport sheets",
                "Audio & 1080p Video Tools" to "Unlimited audio trimming/conversion and 1080p video compression"
            )
        }

    val notesDrawingItems = premium
        .filter { it.toolId in notesDrawingToolIds || it.id.contains("note") || it.id.contains("drawing") || it.id.contains("checklist") }
        .map { it.title to it.shortBenefit }
        .ifEmpty {
            listOf(
                "Unlimited Notes & Checklists" to "Markdown notes, voice dictation, and pinned checklists",
                "Pro Drawing Pad & PDF Export" to "Multi-brush canvas, grid templates, photo markup, and vector/PDF export"
            )
        }

    val usedTitles = (outdoorsItems + docsPdfItems + mediaStudioItems + notesDrawingItems).map { it.first }.toSet()

    val toolsItems = premium
        .filter {
            it.category in setOf(
                ToolCategory.MEASURE,
                ToolCategory.LIGHT_AND_SOUND,
                ToolCategory.PHONE_CARE_AND_AUDIO,
                ToolCategory.TRAVEL_AND_TIME,
                ToolCategory.CONVERT_AND_WRITE
            ) && it.title !in usedTitles
        }
        .map { it.title to it.shortBenefit }
        .ifEmpty {
            listOf(
                "Precision Instruments" to "Angle Protractor, Metal Finder, Sound dB, Tuner, Metronome & Focus Mixer",
                "Photography Light Meter & Level Profiles" to "EV/ISO/Aperture/Shutter calculator and saved calibration profiles",
                "Unlimited World Clocks & Sound Packs" to "All time zones and Pets, Relax & Pro Audio tone packs"
            )
        }

    val alertsWidgetsItems = listOf(
        "24 Smart Outdoor & Tool Alerts" to "Solunar windows, Golden Hour, Rain-Soon, UV, Barometer drop, and custom reminders",
        "11 Home-Screen Glance Widgets" to "Quick Tile (1×1), Feature Grids (2×2, 4×1, 4×2), Torch, Solunar, Weather, Sky, Scan & Compass"
    )

    val themesCustomizeItems = listOf(
        "All 8 Liquid Glass Themes" to "Lime Night, Ocean Glass, Sunset Amber, Forest Canopy, Midnight Violet & True Black AMOLED",
        "Customize Home & Folders" to "Drag-and-drop sections, custom folders, S/M/L tile sizes, and 5 saved layouts",
        "Custom Style Studio" to "Custom accent colors, tile styles, hero styles, and icon sets"
    )

    val backupPrivacyItems = listOf(
        "Full ZIP Vault Backup & Restore" to "One-file backup of all documents, notes, drawings, scans, and settings",
        "App Lock & Privacy Guard" to "Biometric / PIN lock with timeout and Hide in Recents (FLAG_SECURE)"
    )

    val noAdsItems = listOf(
        "Zero Banner, Interstitial, or App-Open Ads" to "Completely ad-free experience across every screen and tool forever"
    )

    return listOf(
        PaywallWhatYouGetGroup("tools", "Tools", toolsItems),
        PaywallWhatYouGetGroup("outdoors", "Outdoors", outdoorsItems),
        PaywallWhatYouGetGroup("docs_pdf", "Documents and PDF", docsPdfItems),
        PaywallWhatYouGetGroup("media_studio", "Media Studio", mediaStudioItems),
        PaywallWhatYouGetGroup("notes_drawing", "Notes and Drawing", notesDrawingItems),
        PaywallWhatYouGetGroup("alerts_widgets", "Alerts and Widgets", alertsWidgetsItems),
        PaywallWhatYouGetGroup("themes_customization", "Themes and Customization", themesCustomizeItems),
        PaywallWhatYouGetGroup("backup_privacy", "Backup and Privacy", backupPrivacyItems),
        PaywallWhatYouGetGroup("no_ads", "No ads", noAdsItems)
    )
}
