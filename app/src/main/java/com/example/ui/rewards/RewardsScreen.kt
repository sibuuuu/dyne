package com.example.ui.rewards

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.AdManager
import com.example.billing.FeatureGate
import com.example.billing.PremiumFeature
import com.example.data.CoinLedgerRecord
import com.example.data.PreferencesRepository
import com.example.data.ScanRepository
import com.example.data.UserPreferences
import com.example.ui.components.GlassButton
import com.example.ui.components.HazeState
import com.example.ui.components.LocalBottomBarInset
import com.example.ui.components.ProBadge
import com.example.ui.components.SharedTopBar
import com.example.ui.components.dyneClickable
import com.example.ui.components.proBorder
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import com.example.ui.theme.ProGoldDeep
import com.example.ui.theme.ProGoldGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@Composable
fun RewardsScreen(
    preferences: UserPreferences,
    preferencesRepository: PreferencesRepository,
    scanRepository: ScanRepository,
    adManager: AdManager,
    featureGate: FeatureGate,
    hazeState: HazeState?,
    onBack: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenSpotlightTool: (com.example.domain.RegisteredTool) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val activity = context as? Activity
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val bottomInset = LocalBottomBarInset.current
    val scope = rememberCoroutineScope()

    val ledgerEntries by scanRepository.allCoinLedger.collectAsState(initial = emptyList())
    val animatedBalance by animateIntAsState(
        targetValue = preferences.coinBalance,
        animationSpec = tween(durationMillis = if (theme.reduceMotion) 0 else 450),
        label = "animated_coin_balance"
    )

    var nowEpochMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowEpochMs = System.currentTimeMillis()
        }
    }

    var statusBanner by remember { mutableStateOf<String?>(null) }

    val todayIso = remember(nowEpochMs) { LocalDate.now().toString() }
    val adsWatchedToday = if (preferences.adCoinDailyDate == todayIso) {
        preferences.adCoinDailyCount
    } else {
        0
    }
    val remainingCooldownSec = remember(preferences.lastAdCoinEpochMs, nowEpochMs) {
        val elapsed = nowEpochMs - preferences.lastAdCoinEpochMs
        val rem = PreferencesRepository.AD_COIN_COOLDOWN_MS - elapsed
        if (preferences.lastAdCoinEpochMs > 0L && rem > 0L) {
            ((rem + 999L) / 1000L).toInt()
        } else {
            0
        }
    }
    val checkedInToday = preferences.lastCheckInDate == todayIso

    val spotlightTool = remember(preferences.spotlightToolId, preferences.spotlightDailyDate) {
        val today = LocalDate.now().toString()
        val allPro = com.example.domain.ToolRegistry.tools.value.filter { it.isPro && it.isEnabled }
        if (preferences.spotlightDailyDate == today && preferences.spotlightToolId.isNotBlank()) {
            allPro.firstOrNull { it.id == preferences.spotlightToolId } ?: allPro.firstOrNull()
        } else {
            allPro.firstOrNull()
        }
    }
    val isSpotlightRunning = remember(preferences.spotlightStartEpochMs, preferences.spotlightExpiryEpochMs, nowEpochMs) {
        preferences.isSpotlightActiveNow(nowEpochMs)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SharedTopBar(
            title = "Coins & Rewards",
            subtitle = "Earn & unlock 1-hour passes",
            hazeState = hazeState,
            onBackClick = onBack,
            rightActionIcon = Icons.Default.WorkspacePremium,
            rightActionDescription = "Unlock Dyne Premium",
            rightActionTestTag = "rewards_crown_button",
            onRightActionClick = onOpenPaywall
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = bottomInset + 32.dp)
        ) {
            // 1. Animated Coin Balance Hero Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(theme.surface)
                    .proBorder(cornerRadius = 28.dp, borderWidth = 1.5.dp, enableShimmer = false)
                    .padding(horizontal = 22.dp, vertical = 20.dp)
                    .testTag("rewards_balance_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(brush = ProGoldGradient)
                    ) {
                        Text(
                            text = "C",
                            style = type.caption.copy(fontWeight = FontWeight.ExtraBold, fontSize = 14.sp),
                            color = BentoTextBlack
                        )
                    }
                    Text(
                        text = "AVAILABLE BALANCE",
                        style = type.section.copy(fontWeight = FontWeight.ExtraBold),
                        color = ProGold
                    )
                }

                Text(
                    text = "$animatedBalance",
                    style = type.gaugeLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        brush = ProGoldGradient
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("rewards_animated_balance_text")
                )

                Text(
                    text = "Use coins to unlock any Premium tool or theme for 1 hour, or unlock extra document saves and CSV or PDF exports.",
                    style = type.caption,
                    color = theme.textSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // 1B. Compact single-line Daily Spotlight row ("Free for 1h today: <Title>")
            if (spotlightTool != null) {
                val totalHours = if (preferences.checkInStreak >= 7) 2 else 1
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(theme.surface)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("rewards_daily_spotlight_row")
                ) {
                    Text(
                        text = "Free for ${totalHours}h today: ${spotlightTool.name}",
                        style = type.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = theme.textPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.accent.copy(alpha = 0.16f))
                            .border(1.dp, theme.accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .dyneClickable(
                                testTag = "rewards_daily_spotlight_button",
                                onClick = {
                                    onOpenSpotlightTool(spotlightTool)
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isSpotlightRunning) "Open" else "Try ${totalHours}h",
                            style = type.caption.copy(fontWeight = FontWeight.ExtraBold, fontSize = 11.sp),
                            color = theme.accent,
                            maxLines = 1
                        )
                    }
                }
            }

            if (!statusBanner.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ProGold.copy(alpha = 0.14f))
                        .border(1.dp, ProGold.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("rewards_status_banner")
                ) {
                    Text(
                        text = statusBanner ?: "",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = ProGold
                    )
                }
            }

            // 2. Watch Rewarded Ad (+10 coins, 60s cooldown, max 12/day)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(theme.surface)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Watch Rewarded Ad",
                            style = type.title.copy(fontWeight = FontWeight.ExtraBold),
                            color = theme.textPrimary
                        )
                        Text(
                            text = "+10 coins per completed ad • 60s cooldown • $adsWatchedToday/${PreferencesRepository.AD_COIN_MAX_PER_DAY} today",
                            style = type.caption,
                            color = theme.textSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(ProGold.copy(alpha = 0.18f))
                            .border(1.dp, ProGold.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+10",
                            style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                            color = ProGold
                        )
                    }
                }

                val watchButtonLabel = when {
                    adsWatchedToday >= PreferencesRepository.AD_COIN_MAX_PER_DAY ->
                        "Daily ad limit reached (12/12)"
                    remainingCooldownSec > 0 ->
                        "Cooldown: ${remainingCooldownSec}s"
                    else ->
                        "Watch ad (+10 coins)"
                }

                GlassButton(
                    text = watchButtonLabel,
                    icon = Icons.Default.PlayCircleFilled,
                    accentFill = ProGold,
                    textColor = BentoTextBlack,
                    enabled = adsWatchedToday < PreferencesRepository.AD_COIN_MAX_PER_DAY && remainingCooldownSec == 0,
                    onClick = {
                        statusBanner = null
                        if (adsWatchedToday >= PreferencesRepository.AD_COIN_MAX_PER_DAY) {
                            statusBanner = "Daily limit of 12 rewarded ads reached. Come back tomorrow!"
                            return@GlassButton
                        }
                        if (remainingCooldownSec > 0) {
                            statusBanner = "Please wait ${remainingCooldownSec}s before watching another ad."
                            return@GlassButton
                        }
                        adManager.showRewardedAd(
                            activity = activity,
                            rewardDescription = "+10 Dyne Coins",
                            onAdUnavailable = { msg ->
                                statusBanner = msg
                            },
                            onRewardEarned = {
                                scope.launch {
                                    val (granted, balAndMsg) = preferencesRepository.claimRewardedAdCoins()
                                    val (newBalance, message) = balAndMsg
                                    if (granted) {
                                        scanRepository.recordCoinTransaction(
                                            delta = 10,
                                            balanceAfter = newBalance,
                                            reason = "Completed rewarded ad (+10 coins)"
                                        )
                                    }
                                    statusBanner = message
                                }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "rewards_watch_ad_button"
                )
            }

            // 3. 7-Day Daily Check-In Streak (+5 daily, Day 7 = +25)
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(theme.surface)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Check-In Streak",
                            style = type.title.copy(fontWeight = FontWeight.ExtraBold),
                            color = theme.textPrimary
                        )
                        Text(
                            text = "+5 coins daily • Day 7 bonus = +25 coins",
                            style = type.caption,
                            color = theme.textSecondary
                        )
                    }
                    ProBadge(label = "STREAK ${preferences.checkInStreak}/7")
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (day in 1..7) {
                        val completed = day <= preferences.checkInStreak
                        val isDay7 = day == 7
                        val rewardAmount = if (isDay7) "+25" else "+5"
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (completed) ProGold.copy(alpha = 0.22f)
                                    else theme.elevated
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (completed || isDay7) ProGold.copy(alpha = 0.65f)
                                    else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "D$day",
                                style = type.caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = if (completed) ProGold else theme.textSecondary
                            )
                            if (completed) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Claimed",
                                    tint = ProGold,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Text(
                                    text = rewardAmount,
                                    style = type.caption.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isDay7) ProGold else theme.textPrimary
                                )
                            }
                        }
                    }
                }

                GlassButton(
                    text = if (checkedInToday) "Checked in today" else "Claim daily check-in",
                    accentFill = if (checkedInToday) theme.elevated else theme.accent,
                    textColor = if (checkedInToday) theme.textSecondary else theme.onAccent,
                    enabled = !checkedInToday,
                    onClick = {
                        scope.launch {
                            val (claimed, delta, newBalance) = preferencesRepository.claimDailyCheckIn()
                            if (claimed) {
                                scanRepository.recordCoinTransaction(
                                    delta = delta,
                                    balanceAfter = newBalance,
                                    reason = "Daily check-in (+$delta coins)"
                                )
                                statusBanner = "+$delta coins claimed! Balance: $newBalance coins."
                            } else {
                                statusBanner = "Already checked in today."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "rewards_daily_checkin_button"
                )
            }

            // 4. Spend Grid (Tool 1h = 10, Theme 1h = 15, Extra Document Save = 10, CSV or PDF Export = 10)
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(theme.surface)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Text(
                    text = "Spend Coins",
                    style = type.title.copy(fontWeight = FontWeight.ExtraBold),
                    color = theme.textPrimary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SpendGridCard(
                        title = "Tool 1h Pass",
                        subtitle = "Unlock any Pro tool for 1 hour",
                        cost = PreferencesRepository.COST_TOOL_1H,
                        icon = Icons.Default.LockOpen,
                        testTag = "spend_card_tool_1h",
                        onClick = {
                            scope.launch {
                                val (spent, newBal) = preferencesRepository.spendCoins(PreferencesRepository.COST_TOOL_1H)
                                if (spent) {
                                    preferencesRepository.unlockFeatureFor1Hour(PremiumFeature.ANGLE.key, toolId = "angle")
                                    scanRepository.recordCoinTransaction(
                                        delta = -PreferencesRepository.COST_TOOL_1H,
                                        balanceAfter = newBal,
                                        reason = "Unlocked Pro Tool for 1 hour"
                                    )
                                    statusBanner = "Unlocked Angle for 1 hour! Tap any locked tool tile to unlock specific tools."
                                } else {
                                    statusBanner = "Need ${PreferencesRepository.COST_TOOL_1H} coins (you have ${preferences.coinBalance})."
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    SpendGridCard(
                        title = "Theme 1h Pass",
                        subtitle = "Unlock any Pro theme for 1 hour",
                        cost = PreferencesRepository.COST_THEME_1H,
                        icon = Icons.Default.Palette,
                        testTag = "spend_card_theme_1h",
                        onClick = {
                            scope.launch {
                                val (spent, newBal) = preferencesRepository.spendCoins(PreferencesRepository.COST_THEME_1H)
                                if (spent) {
                                    preferencesRepository.unlockFeatureFor1Hour(PremiumFeature.PREMIUM_THEMES.key)
                                    scanRepository.recordCoinTransaction(
                                        delta = -PreferencesRepository.COST_THEME_1H,
                                        balanceAfter = newBal,
                                        reason = "Unlocked Premium Themes for 1 hour"
                                    )
                                    statusBanner = "Unlocked all Premium Themes for 1 hour!"
                                } else {
                                    statusBanner = "Need ${PreferencesRepository.COST_THEME_1H} coins (you have ${preferences.coinBalance})."
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SpendGridCard(
                        title = "Extra Doc Save",
                        subtitle = "+1 saved PDF document slot",
                        cost = PreferencesRepository.COST_EXTRA_DOC_SAVE,
                        icon = Icons.Default.Description,
                        testTag = "spend_card_extra_doc",
                        onClick = {
                            scope.launch {
                                val (spent, newBal) = preferencesRepository.spendCoins(PreferencesRepository.COST_EXTRA_DOC_SAVE)
                                if (spent) {
                                    preferencesRepository.grantOneExtraDocumentSave()
                                    scanRepository.recordCoinTransaction(
                                        delta = -PreferencesRepository.COST_EXTRA_DOC_SAVE,
                                        balanceAfter = newBal,
                                        reason = "Purchased +1 Extra Document Save"
                                    )
                                    statusBanner = "+1 Extra Document Save added!"
                                } else {
                                    statusBanner = "Need ${PreferencesRepository.COST_EXTRA_DOC_SAVE} coins (you have ${preferences.coinBalance})."
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    SpendGridCard(
                        title = "CSV / PDF Export",
                        subtitle = "+1 CSV or PDF export pass",
                        cost = PreferencesRepository.COST_CSV_EXPORT,
                        icon = Icons.Default.TableView,
                        testTag = "spend_card_csv_export",
                        onClick = {
                            scope.launch {
                                val (spent, newBal) = preferencesRepository.spendCoins(PreferencesRepository.COST_CSV_EXPORT)
                                if (spent) {
                                    preferencesRepository.grantOneExtraCsvExport()
                                    scanRepository.recordCoinTransaction(
                                        delta = -PreferencesRepository.COST_CSV_EXPORT,
                                        balanceAfter = newBal,
                                        reason = "Purchased +1 CSV / PDF Export"
                                    )
                                    statusBanner = "+1 CSV / PDF Export unlocked!"
                                } else {
                                    statusBanner = "Need ${PreferencesRepository.COST_CSV_EXPORT} coins (you have ${preferences.coinBalance})."
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4b. Sound Packs (One-time purchase via Play Billing or 10 coins for 1h)
            val ownedPacks by featureGate.ownedSoundPacks.collectAsState()
            val packPrices by featureGate.soundPackPrices.collectAsState()
            val isPro by featureGate.isPro.collectAsState()
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(theme.surface)
                    .proBorder(cornerRadius = 24.dp, borderWidth = 1.dp)
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Frequency Sound Packs",
                            style = type.title.copy(fontWeight = FontWeight.ExtraBold),
                            color = ProGold
                        )
                        Text(
                            text = "Included in Dyne Premium, or unlock individually with 10 coins (1h) or one-time Play purchase",
                            style = type.caption,
                            color = theme.textSecondary
                        )
                    }
                }

                com.example.billing.SoundPack.entries.forEach { pack ->
                    val permanentlyOwned = isPro || ownedPacks.contains(pack.productId.lowercase())
                    val tempUnlocked = featureGate.isPackUnlocked(pack, nowMs = nowEpochMs)
                    val priceLabel = packPrices[pack.productId]?.takeIf { it.isNotBlank() } ?: "Google Play"
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.elevated)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pack.title,
                                    style = type.body.copy(fontWeight = FontWeight.ExtraBold),
                                    color = theme.textPrimary
                                )
                                Text(
                                    text = pack.shortBenefit,
                                    style = type.caption,
                                    color = theme.textSecondary
                                )
                            }
                            if (permanentlyOwned) {
                                ProBadge(label = "OWNED")
                            } else if (tempUnlocked) {
                                ProBadge(label = "1H ACTIVE")
                            }
                        }

                        if (!permanentlyOwned) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ProGold.copy(alpha = 0.18f))
                                        .border(1.dp, ProGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .dyneClickable {
                                            featureGate.unlockWithCoins(
                                                request = com.example.billing.UnlockSheetRequest(
                                                    feature = com.example.billing.PremiumFeature.FREQUENCY_PRO,
                                                    soundPack = pack
                                                ),
                                                onResult = { _, msg ->
                                                    statusBanner = msg
                                                }
                                            )
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "10 coins (1h)",
                                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                                        color = ProGold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(brush = ProGoldGradient)
                                        .dyneClickable {
                                            if (activity != null) {
                                                featureGate.launchSoundPackPurchase(activity, pack)
                                                statusBanner = "Opening Google Play for ${pack.title}..."
                                            } else {
                                                statusBanner = "Purchases work when Dyne is installed from Google Play"
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Buy ($priceLabel)",
                                        style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                                        color = BentoTextBlack
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Transaction History (Room Ledger)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(theme.surface)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Text(
                    text = "Transaction History",
                    style = type.title.copy(fontWeight = FontWeight.ExtraBold),
                    color = theme.textPrimary
                )

                if (ledgerEntries.isEmpty()) {
                    Text(
                        text = "No coin transactions yet. Claim your daily check-in or watch a rewarded ad to start earning.",
                        style = type.caption,
                        color = theme.textSecondary
                    )
                } else {
                    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }
                    ledgerEntries.take(25).forEach { entry ->
                        LedgerRow(entry = entry, dateFormat = dateFormat)
                    }
                }
            }

            // 6. Device storage disclosure
            Text(
                text = "Coins are stored on this device",
                style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                color = theme.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .testTag("coins_stored_on_device_note")
            )
        }
    }
}

@Composable
private fun SpendGridCard(
    title: String,
    subtitle: String,
    cost: Int,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(theme.elevated)
            .proBorder(cornerRadius = 20.dp, borderWidth = 1.dp)
            .dyneClickable(targetScale = 0.96f, testTag = testTag, onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ProGold.copy(alpha = 0.18f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = ProGold,
                    modifier = Modifier.size(16.dp)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(brush = ProGoldGradient)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$cost coins",
                    style = type.caption.copy(fontWeight = FontWeight.ExtraBold, fontSize = 11.sp),
                    color = BentoTextBlack
                )
            }
        }

        Text(
            text = title,
            style = type.caption.copy(fontWeight = FontWeight.ExtraBold, fontSize = 14.sp),
            color = theme.textPrimary
        )
        Text(
            text = subtitle,
            style = type.caption.copy(fontSize = 11.sp),
            color = theme.textSecondary
        )
    }
}

@Composable
private fun LedgerRow(
    entry: CoinLedgerRecord,
    dateFormat: SimpleDateFormat
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val positive = entry.delta >= 0
    val sign = if (positive) "+" else ""

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.elevated.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.reason,
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.textPrimary
            )
            Text(
                text = "${dateFormat.format(Date(entry.timestamp))} • Balance: ${entry.balanceAfter}",
                style = type.caption.copy(fontSize = 11.sp),
                color = theme.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$sign${entry.delta}",
            style = type.caption.copy(fontWeight = FontWeight.ExtraBold, fontSize = 14.sp),
            color = if (positive) theme.accent else ProGold
        )
    }
}
