package com.example.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ads.AdConfig
import com.example.ads.AdFormatType
import com.example.ads.AdLoadStage
import com.example.ads.LocalAdManager
import com.example.ads.RewardUnlockPromptTarget
import com.example.billing.LocalFeatureGate
import com.example.billing.PremiumFeature
import com.example.data.LaunchAnimationMode
import com.example.data.ScanRepository
import com.example.data.ThemeMode
import com.example.data.UnitSystem
import com.example.domain.HomeNeedPreset
import com.example.domain.ToolRegistry
import com.example.ui.components.DesignedEmptyStateCard
import com.example.ui.components.DyneSegmentedControl
import com.example.ui.components.DyneSwitch
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassSheet
import com.example.ui.components.GlassSurface
import com.example.ui.components.HazeState
import com.example.ui.components.LocalBottomBarInset
import com.example.ui.components.ProBadge
import com.example.ui.components.SharedTopBar
import com.example.ui.components.dyneClickable
import com.example.ui.components.horizontalChipRowEdgeFade
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.DyneTheme
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import com.example.ui.theme.ProGoldDeep
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.ThemeEngine
import java.util.Locale
import kotlin.math.roundToInt

private data class ScanShortcutOption(
    val id: String,
    val title: String,
    val subtitle: String
)

private val AvailableScanShortcuts = listOf(
    ScanShortcutOption("scan_doc", "Scan Document", "Multi-page document scanner with edge crop & PDF export"),
    ScanShortcutOption("scan_qr", "Scan QR / Barcode", "Live camera QR, EAN, UPC & Wi-Fi code reader"),
    ScanShortcutOption("quick_note", "Quick Note", "Instant field note or sketch in Library"),
    ScanShortcutOption("flashlight", "Flashlight", "Instant LED torch, SOS & strobe"),
    ScanShortcutOption("unit_converter", "Unit Converter", "Offline length, weight, volume & currency"),
    ScanShortcutOption("world_clock", "World Clock", "Multi-timezone reference & meeting planner"),
    ScanShortcutOption("ruler", "Screen Ruler", "DPI-calibrated inch & cm ruler"),
    ScanShortcutOption("sound_db", "Sound Meter", "Live decibel gauge & peak hold")
)

enum class SettingsSubScreen {
    ROOT,
    THEMES, // Appearance
    SOUNDS_AND_HAPTICS,
    CUSTOMIZE_HOME, // Home & Tools
    LOCATION,
    UNITS_AND_FORMATS,
    REWARDS_AND_COINS,
    NOTIFICATIONS,
    PRIVACY_AND_SECURITY,
    DATA_AND_BACKUP,
    SUPPORT_AND_ABOUT,
    DEVELOPER_OPTIONS,
    FRAME_STATS
}

private data class SettingsMainCategoryRowSpec(
    val id: String,
    val title: String,
    val summary: String,
    val icon: ImageVector,
    val iconBg: Color,
    val subScreen: SettingsSubScreen,
    val testTag: String,
    val searchKeywords: List<String>
)

private data class SearchableSubOptionSpec(
    val title: String,
    val subtitle: String,
    val categoryTitle: String,
    val icon: ImageVector,
    val iconBg: Color,
    val targetSubScreen: SettingsSubScreen
)

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    hazeState: HazeState? = null,
    nestedScrollConnection: NestedScrollConnection? = null,
    scrollToTopTrigger: Int = 0,
    onOpenPaywall: () -> Unit = {},
    onOpenRewards: () -> Unit = {},
    onOpenAllFeatures: () -> Unit = {},
    initialSubScreen: SettingsSubScreen = SettingsSubScreen.ROOT,
    onConsumedInitialSubScreen: () -> Unit = {}
) {
    var activeSubScreen by rememberSaveable { mutableStateOf(SettingsSubScreen.ROOT) }
    var showClearDataConfirmSheet by remember { mutableStateOf(false) }
    var activeModalSheet by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    LaunchedEffect(initialSubScreen) {
        if (initialSubScreen != SettingsSubScreen.ROOT) {
            activeSubScreen = initialSubScreen
            onConsumedInitialSubScreen()
        }
    }

    if (activeSubScreen != SettingsSubScreen.ROOT) {
        BackHandler {
            activeSubScreen = if (activeSubScreen == SettingsSubScreen.FRAME_STATS) {
                SettingsSubScreen.DEVELOPER_OPTIONS
            } else {
                SettingsSubScreen.ROOT
            }
        }
    }

    AnimatedContent(
        targetState = activeSubScreen,
        transitionSpec = {
            fadeIn(tween(220)) togetherWith fadeOut(tween(180))
        },
        label = "SettingsSubScreenTransition"
    ) { subScreen ->
        when (subScreen) {
            SettingsSubScreen.ROOT -> SettingsRootContent(
                viewModel = viewModel,
                hazeState = hazeState,
                nestedScrollConnection = nestedScrollConnection,
                scrollToTopTrigger = scrollToTopTrigger,
                onOpenPaywall = onOpenPaywall,
                onOpenRewards = onOpenRewards,
                onOpenAllFeatures = onOpenAllFeatures,
                onNavigateSubScreen = { activeSubScreen = it }
            )
            SettingsSubScreen.THEMES -> AppearanceSubScreen(
                viewModel = viewModel,
                hazeState = hazeState,
                onOpenPaywall = onOpenPaywall,
                onBack = { activeSubScreen = SettingsSubScreen.ROOT }
            )
            SettingsSubScreen.SOUNDS_AND_HAPTICS -> SoundsAndHapticsSubScreen(
                viewModel = viewModel,
                hazeState = hazeState,
                onOpenPaywall = onOpenPaywall,
                onBack = { activeSubScreen = SettingsSubScreen.ROOT }
            )
            SettingsSubScreen.CUSTOMIZE_HOME -> CustomizeHomeSubScreen(
                viewModel = viewModel,
                hazeState = hazeState,
                onOpenPaywall = onOpenPaywall,
                onOpenAllFeatures = onOpenAllFeatures,
                onBack = { activeSubScreen = SettingsSubScreen.ROOT }
            )
            SettingsSubScreen.LOCATION -> LocationSubScreen(
                viewModel = viewModel,
                hazeState = hazeState,
                onBack = { activeSubScreen = SettingsSubScreen.ROOT }
            )
            SettingsSubScreen.UNITS_AND_FORMATS -> UnitsAndFormatsSubScreen(
                viewModel = viewModel,
                hazeState = hazeState,
                onBack = { activeSubScreen = SettingsSubScreen.ROOT }
            )
            SettingsSubScreen.REWARDS_AND_COINS -> RewardsAndCoinsSubScreen(
                viewModel = viewModel,
                hazeState = hazeState,
                onOpenRewards = onOpenRewards,
                onOpenPaywall = onOpenPaywall,
                onBack = { activeSubScreen = SettingsSubScreen.ROOT }
            )
            SettingsSubScreen.NOTIFICATIONS -> NotificationsSubScreen(
                viewModel = viewModel,
                hazeState = hazeState,
                onBack = { activeSubScreen = SettingsSubScreen.ROOT }
            )
            SettingsSubScreen.PRIVACY_AND_SECURITY -> PrivacyAndSecuritySubScreen(
                viewModel = viewModel,
                hazeState = hazeState,
                onOpenPaywall = onOpenPaywall,
                onRequestClearAllData = { showClearDataConfirmSheet = true },
                onRequestAdPrivacySheet = { activeModalSheet = "ad_privacy" },
                onOpenPrivacyPolicy = { activeModalSheet = "privacy" },
                onBack = { activeSubScreen = SettingsSubScreen.ROOT }
            )
            SettingsSubScreen.DATA_AND_BACKUP -> DataAndBackupSubScreen(
                viewModel = viewModel,
                hazeState = hazeState,
                onOpenPaywall = onOpenPaywall,
                onBack = { activeSubScreen = SettingsSubScreen.ROOT }
            )
            SettingsSubScreen.SUPPORT_AND_ABOUT -> SupportAndAboutSubScreen(
                viewModel = viewModel,
                hazeState = hazeState,
                onOpenModalSheet = { sheet -> activeModalSheet = sheet },
                onOpenDeveloperOptions = { activeSubScreen = SettingsSubScreen.DEVELOPER_OPTIONS },
                onBack = { activeSubScreen = SettingsSubScreen.ROOT }
            )
            SettingsSubScreen.DEVELOPER_OPTIONS -> DeveloperOptionsSubScreen(
                viewModel = viewModel,
                hazeState = hazeState,
                onOpenFrameStats = { activeSubScreen = SettingsSubScreen.FRAME_STATS },
                onBack = { activeSubScreen = SettingsSubScreen.ROOT }
            )
            SettingsSubScreen.FRAME_STATS -> FrameStatsSubScreen(
                hazeState = hazeState,
                onBack = { activeSubScreen = SettingsSubScreen.DEVELOPER_OPTIONS }
            )
        }
    }

    // Confirmation Sheet for "Clear all local data"
    if (showClearDataConfirmSheet) {
        GlassSheet(
            onDismissRequest = { showClearDataConfirmSheet = false },
            hazeState = hazeState
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Clear All Local Data?",
                    style = type.title,
                    color = StatusDanger
                )
                Text(
                    text = "This permanently deletes all scanned PDFs, QR/barcode history, text notes, PNG drawings, and resets all custom preferences on this device. Your Google Play Pro entitlement is preserved.",
                    style = type.body,
                    color = theme.textSecondary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassButton(
                        text = "Cancel",
                        onClick = { showClearDataConfirmSheet = false },
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Delete Everything",
                        accentFill = StatusDanger,
                        textColor = Color.White,
                        onClick = {
                            viewModel.clearAllLocalData(context)
                            showClearDataConfirmSheet = false
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "confirm_clear_all_data_button"
                    )
                }
            }
        }
    }

    // Info / Legal Sheets & Full HTML Privacy Policy Viewer
    if (activeModalSheet == "privacy") {
        com.example.legal.PrivacyPolicyViewerDialog(
            onDismiss = { activeModalSheet = null },
            onBrowserLaunchError = { msg -> viewModel.showStatusMessage(msg) }
        )
    } else if (activeModalSheet != null) {
        GlassSheet(
            onDismissRequest = { activeModalSheet = null },
            hazeState = hazeState
        ) {
            val (sheetTitle, sheetBody) = when (activeModalSheet) {
                "ad_privacy" -> "Ad Privacy Choices (UMP)" to
                    "Dyne uses Google User Messaging Platform (UMP) to manage your ad consent choices. In DEBUG builds, only official Google AdMob test IDs are used. Upgrade to Dyne Premium anytime to remove all ads completely."
                "contact" -> "Contact Support" to
                    "For support, feedback, or privacy inquiries, reach out to ${com.example.legal.LegalConfig.CONTACT_EMAIL}."
                else -> "Terms of Use" to
                    "Dyne provides precision sensor, optical, and ephemeris instruments for everyday utility and field reference. Do not rely solely on barometric or solunar estimates for life-safety navigation. Subscriptions and Lifetime unlocks are managed through Google Play. Privacy Policy: ${com.example.legal.LegalConfig.PRIVACY_POLICY_URL}"
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = sheetTitle, style = type.title, color = theme.textPrimary)
                Text(text = sheetBody, style = type.body, color = theme.textSecondary)
                GlassButton(
                    text = "Done",
                    accentFill = theme.accent,
                    onClick = { activeModalSheet = null },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SettingsRootContent(
    viewModel: SettingsViewModel,
    hazeState: HazeState?,
    nestedScrollConnection: NestedScrollConnection?,
    scrollToTopTrigger: Int,
    onOpenPaywall: () -> Unit,
    onOpenRewards: () -> Unit,
    onOpenAllFeatures: () -> Unit = {},
    onNavigateSubScreen: (SettingsSubScreen) -> Unit
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val isRemoveAds by viewModel.isRemoveAds.collectAsStateWithLifecycle()
    val removeAdsPrice by viewModel.removeAdsPrice.collectAsStateWithLifecycle()
    val proSource by viewModel.proSource.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val temporaryCacheSizeBytes by viewModel.temporaryCacheSizeBytes.collectAsStateWithLifecycle()
    val storageBreakdown by viewModel.storageBreakdown.collectAsStateWithLifecycle()
    val registeredTools by ToolRegistry.tools.collectAsState()
    val hostActivity = remember(context) { context as? Activity }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var versionTapCounter by remember { mutableIntStateOf(0) }

    val scrollState = rememberScrollState()
    val collapseFraction by remember {
        derivedStateOf { (scrollState.value / 150f).coerceIn(0f, 1f) }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshTemporaryCacheSize(context)
    }

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            scrollState.animateScrollTo(0)
        }
    }

    val q = searchQuery.trim()

    // The grouped rows on the main Settings page (Rewards & Coins hidden when isPro)
    val tenCategoryRows = remember(
        prefs,
        isPro,
        registeredTools,
        storageBreakdown,
        temporaryCacheSizeBytes,
        theme.accent
    ) {
        val enabledToolCount = registeredTools.count { it.isEnabled }
        val totalToolCount = registeredTools.size
        listOfNotNull(
            SettingsMainCategoryRowSpec(
                id = "appearance",
                title = "Appearance",
                summary = "${prefs.effectiveThemeName(isPro)} • ${prefs.themeMode.label} • ${(prefs.textScale * 100).roundToInt()}%",
                icon = Icons.Default.Palette,
                iconBg = theme.accent,
                subScreen = SettingsSubScreen.THEMES,
                testTag = "settings_row_themes",
                searchKeywords = listOf(
                    "appearance", "theme", "lime night", "classic light", "ocean", "sunset", "aurora",
                    "sakura", "amoled", "midnight gold", "dark", "light", "system", "text size",
                    "font", "reduce glass", "reduce motion", "launch animation", "splash", "app icon"
                )
            ),
            SettingsMainCategoryRowSpec(
                id = "sounds_haptics",
                title = "Sounds & Haptics",
                summary = "Tap ${if (prefs.tapSoundsEnabled) "${(prefs.tapSoundVolume * 100).roundToInt()}%" else "Off"} • Haptics ${if (prefs.hapticsEnabled) prefs.hapticStrength.label else "Off"}",
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                iconBg = Color(0xFF38BDF8),
                subScreen = SettingsSubScreen.SOUNDS_AND_HAPTICS,
                testTag = "settings_row_sounds_haptics",
                searchKeywords = listOf(
                    "sounds", "haptics", "tap sounds", "tap volume", "play preview", "scan beep",
                    "vibration", "haptic strength", "sound packs", "acoustic"
                )
            ),
            SettingsMainCategoryRowSpec(
                id = "home_tools",
                title = "Home & Tools",
                summary = "$enabledToolCount/$totalToolCount tools • Tile ${prefs.effectiveHomeTileSize(isPro)} • ${prefs.outdoorInterestsSet.size} interests",
                icon = Icons.Default.DashboardCustomize,
                iconBg = Color(0xFF34D399),
                subScreen = SettingsSubScreen.CUSTOMIZE_HOME,
                testTag = "settings_row_customize_home",
                searchKeywords = listOf(
                    "home", "tools", "customize home", "widgets", "blocks", "tile sizes", "columns",
                    "outdoor interests", "fishing", "hunting", "camping", "scan menu shortcuts",
                    "modes", "not available on this phone", "reset"
                )
            ),
            SettingsMainCategoryRowSpec(
                id = "location",
                title = "Location",
                summary = if (prefs.useManualLocation) {
                    "${prefs.locationName} (${String.format(Locale.US, "%.2f°, %.2f°", prefs.manualLat, prefs.manualLng)})"
                } else {
                    "Automatic GPS (${prefs.locationName})"
                },
                icon = Icons.Default.MyLocation,
                iconBg = Color(0xFF60A5FA),
                subScreen = SettingsSubScreen.LOCATION,
                testTag = "settings_row_location",
                searchKeywords = listOf(
                    "location", "gps", "manual coordinates", "latitude", "longitude", "city", "ephemeris"
                )
            ),
            SettingsMainCategoryRowSpec(
                id = "units_formats",
                title = "Units & Formats",
                summary = "${prefs.distanceUnit} • ${prefs.temperatureUnit} • ${if (prefs.use24HourTime) "24h" else "12h"} • ${prefs.defaultCurrency}",
                icon = Icons.Default.Straighten,
                iconBg = Color(0xFFA78BFA),
                subScreen = SettingsSubScreen.UNITS_AND_FORMATS,
                testTag = "settings_row_units_formats",
                searchKeywords = listOf(
                    "units", "formats", "metric", "imperial", "distance", "speed", "temperature",
                    "pressure", "24-hour", "12-hour", "time zone", "currency", "km", "mph", "celsius", "fahrenheit", "hpa"
                )
            ),
            if (!isPro) {
                SettingsMainCategoryRowSpec(
                    id = "rewards_coins",
                    title = "Rewards & Coins",
                    summary = "${prefs.coinBalance} coins • ${prefs.checkInStreak}/7 day streak",
                    icon = Icons.Default.MonetizationOn,
                    iconBg = ProGold,
                    subScreen = SettingsSubScreen.REWARDS_AND_COINS,
                    testTag = "settings_row_rewards",
                    searchKeywords = listOf(
                        "rewards", "coins", "streak", "check-in", "1-hour unlock", "temporary unlock", "watch ad"
                    )
                )
            } else null,
            SettingsMainCategoryRowSpec(
                id = "notifications",
                title = "Notifications",
                summary = "Solunar ${if (prefs.solunarAlertsEnabled) "On" else "Off"} • Expiry ${if (prefs.expiryRemindersEnabled) "On" else "Off"} • Bonus ${if (prefs.dailyBonusNotificationsEnabled) "On" else "Off"}",
                icon = Icons.Default.Notifications,
                iconBg = Color(0xFFFB7185),
                subScreen = SettingsSubScreen.NOTIFICATIONS,
                testTag = "settings_row_notifications",
                searchKeywords = listOf(
                    "notifications", "solunar alerts", "expiry reminders", "daily bonus", "quiet hours", "alerts"
                )
            ),
            SettingsMainCategoryRowSpec(
                id = "privacy_security",
                title = "Privacy & Security",
                summary = when {
                    prefs.appLockEnabled -> "App Lock (${prefs.appLockTimeoutMinutes}m) • ${if (prefs.hideAppPreviewInRecents) "FLAG_SECURE" else "Standard"}"
                    prefs.lockDocumentsWithBiometrics || prefs.lockNotesWithBiometrics -> "Per-Feature Biometric Lock"
                    else -> "Biometrics, UMP & Permissions"
                },
                icon = Icons.Default.Security,
                iconBg = Color(0xFF818CF8),
                subScreen = SettingsSubScreen.PRIVACY_AND_SECURITY,
                testTag = "settings_row_privacy_security",
                searchKeywords = listOf(
                    "privacy", "security", "app lock", "biometrics", "device pin", "background",
                    "hide app preview in recents", "flag_secure", "documents lock", "notes lock",
                    "ad privacy choices", "ump", "permission shortcuts", "clear all local data"
                )
            ),
            SettingsMainCategoryRowSpec(
                id = "data_backup",
                title = "Data & Backup",
                summary = "Used ${ScanRepository.formatFileSize(storageBreakdown.totalBytes)} • Cache ${ScanRepository.formatFileSize(temporaryCacheSizeBytes)}",
                icon = Icons.Default.FolderOpen,
                iconBg = Color(0xFF4ADE80),
                subScreen = SettingsSubScreen.DATA_AND_BACKUP,
                testTag = "settings_row_data_backup",
                searchKeywords = listOf(
                    "data", "backup", "restore", "zip", "export", "storage used by dyne",
                    "clear cache", "cache cleaner", "documents", "notes", "sketches", "scans"
                )
            ),
            SettingsMainCategoryRowSpec(
                id = "support_about",
                title = "Support & About",
                summary = "Dyne v${BuildConfig.VERSION_NAME} • Built by Sibora",
                icon = Icons.Default.Explore,
                iconBg = Color(0xFFF472B6),
                subScreen = SettingsSubScreen.SUPPORT_AND_ABOUT,
                testTag = "settings_row_support_about",
                searchKeywords = listOf(
                    "support", "about", "dyne", "version", "sibora", "sibnath bairagi", "privacy policy",
                    "terms of use", "rate", "share", "contact"
                )
            )
        )
    }

    // Sub-options catalog for instant search filtering with highlighted matches
    val allSearchableSubOptions = remember(prefs, isPro, temporaryCacheSizeBytes, storageBreakdown) {
        listOfNotNull(
            SearchableSubOptionSpec("Theme Grid (8 Glass Themes)", prefs.effectiveThemeName(isPro), "Appearance", Icons.Default.Palette, Color(0xFF34D399), SettingsSubScreen.THEMES),
            SearchableSubOptionSpec("Appearance Mode (System / Light / Dark)", prefs.themeMode.label, "Appearance", Icons.Default.Tune, Color(0xFF60A5FA), SettingsSubScreen.THEMES),
            SearchableSubOptionSpec("Text Size & Live Preview", "${(prefs.textScale * 100).roundToInt()}%", "Appearance", Icons.Default.FormatSize, Color(0xFF34D399), SettingsSubScreen.THEMES),
            SearchableSubOptionSpec("Reduce Glass & Reduce Motion", if (prefs.reduceGlassEffects || prefs.reduceMotion) "Active" else "Off", "Appearance", Icons.Default.BlurOn, Color(0xFF818CF8), SettingsSubScreen.THEMES),
            SearchableSubOptionSpec("Launch Animation", prefs.launchAnimationMode.label, "Appearance", Icons.Default.Animation, Color(0xFFF472B6), SettingsSubScreen.THEMES),
            SearchableSubOptionSpec("Tap Sounds & Tap Volume", "${(prefs.tapSoundVolume * 100).roundToInt()}%", "Sounds & Haptics", Icons.AutoMirrored.Filled.VolumeUp, Color(0xFF38BDF8), SettingsSubScreen.SOUNDS_AND_HAPTICS),
            SearchableSubOptionSpec("Scan Beep & Haptic Strength", prefs.hapticStrength.label, "Sounds & Haptics", Icons.Default.Vibration, Color(0xFFFB7185), SettingsSubScreen.SOUNDS_AND_HAPTICS),
            SearchableSubOptionSpec("Widgets, Home Blocks & Tile Sizes", "Tile ${prefs.effectiveHomeTileSize(isPro)} • Cols ${prefs.homeLayoutColumns}", "Home & Tools", Icons.Default.DashboardCustomize, Color(0xFF34D399), SettingsSubScreen.CUSTOMIZE_HOME),
            SearchableSubOptionSpec("Outdoor Interests (Fishing, Hunting, Camping)", "${prefs.outdoorInterestsSet.size} active", "Home & Tools", Icons.Default.Explore, Color(0xFF4ADE80), SettingsSubScreen.CUSTOMIZE_HOME),
            SearchableSubOptionSpec("GPS & Manual Coordinates", prefs.locationName, "Location", Icons.Default.MyLocation, Color(0xFF60A5FA), SettingsSubScreen.LOCATION),
            SearchableSubOptionSpec("Distance, Speed, Temperature & Pressure Units", "${prefs.distanceUnit} • ${prefs.temperatureUnit}", "Units & Formats", Icons.Default.Straighten, Color(0xFFA78BFA), SettingsSubScreen.UNITS_AND_FORMATS),
            SearchableSubOptionSpec("24-Hour Time, Default Time Zone & Currency", "${prefs.defaultTimeZone} • ${prefs.defaultCurrency}", "Units & Formats", Icons.Default.CurrencyExchange, Color(0xFF4ADE80), SettingsSubScreen.UNITS_AND_FORMATS),
            if (!isPro) SearchableSubOptionSpec("Coin Balance, Streak & 1-Hour Unlocks", "${prefs.coinBalance} coins", "Rewards & Coins", Icons.Default.MonetizationOn, ProGold, SettingsSubScreen.REWARDS_AND_COINS) else null,
            SearchableSubOptionSpec("Solunar Alerts & Quiet Hours", "${if (prefs.solunarAlertsEnabled) "On" else "Off"} • ${prefs.solunarQuietHours}", "Notifications", Icons.Default.Notifications, Color(0xFF34D399), SettingsSubScreen.NOTIFICATIONS),
            SearchableSubOptionSpec("Expiry Reminders & Quiet Hours", "${if (prefs.expiryRemindersEnabled) "On" else "Off"} • ${prefs.expiryQuietHours}", "Notifications", Icons.Default.AccessTime, Color(0xFF38BDF8), SettingsSubScreen.NOTIFICATIONS),
            SearchableSubOptionSpec("Daily Bonus Alerts & Quiet Hours", "${if (prefs.dailyBonusNotificationsEnabled) "On" else "Off"} • ${prefs.dailyBonusQuietHours}", "Notifications", Icons.Default.MonetizationOn, ProGold, SettingsSubScreen.NOTIFICATIONS),
            SearchableSubOptionSpec("App Lock (Biometrics / PIN & 1/5/15m Timeout)", if (prefs.appLockEnabled) "On (${prefs.appLockTimeoutMinutes}m)" else "Off", "Privacy & Security", Icons.Default.Security, ProGoldDeep, SettingsSubScreen.PRIVACY_AND_SECURITY),
            SearchableSubOptionSpec("Hide App Preview in Recents (FLAG_SECURE)", if (prefs.hideAppPreviewInRecents) "Enabled" else "Off", "Privacy & Security", Icons.Default.PrivacyTip, ProGoldDeep, SettingsSubScreen.PRIVACY_AND_SECURITY),
            SearchableSubOptionSpec("Lock Documents & Notes with Biometrics", if (prefs.lockDocumentsWithBiometrics || prefs.lockNotesWithBiometrics) "Active" else "Off", "Privacy & Security", Icons.Default.Security, ProGoldDeep, SettingsSubScreen.PRIVACY_AND_SECURITY),
            SearchableSubOptionSpec("Ad Privacy Choices (UMP), Permissions & Clear Data", "Free Controls", "Privacy & Security", Icons.Default.PrivacyTip, Color(0xFFA78BFA), SettingsSubScreen.PRIVACY_AND_SECURITY),
            SearchableSubOptionSpec("Privacy Policy", "View HTML & Public URL", "Privacy & Security", Icons.Default.Security, Color(0xFF818CF8), SettingsSubScreen.PRIVACY_AND_SECURITY),
            SearchableSubOptionSpec("Export & Restore Full ZIP Backup (Documents, Notes, Scans, Coins)", "ZIP Archive", "Data & Backup", Icons.Default.FolderOpen, ProGold, SettingsSubScreen.DATA_AND_BACKUP),
            SearchableSubOptionSpec("Storage Used by Dyne & Clear Cache", ScanRepository.formatFileSize(temporaryCacheSizeBytes), "Data & Backup", Icons.Default.Restore, Color(0xFFFF2D55), SettingsSubScreen.DATA_AND_BACKUP),
            SearchableSubOptionSpec("Built by Sibora, Privacy Policy, Terms, Rate & Share", "v${BuildConfig.VERSION_NAME}", "Support & About", Icons.Default.Explore, Color(0xFFF472B6), SettingsSubScreen.SUPPORT_AND_ABOUT)
        )
    }

    val filteredRows = remember(tenCategoryRows, q) {
        if (q.isEmpty()) {
            tenCategoryRows
        } else {
            tenCategoryRows.filter { row ->
                row.title.contains(q, ignoreCase = true) ||
                    row.summary.contains(q, ignoreCase = true) ||
                    row.searchKeywords.any { kw -> kw.contains(q, ignoreCase = true) }
            }
        }
    }

    val filteredSubOptions = remember(allSearchableSubOptions, q) {
        if (q.isEmpty()) {
            emptyList()
        } else {
            allSearchableSubOptions.filter { sub ->
                sub.title.contains(q, ignoreCase = true) ||
                    sub.subtitle.contains(q, ignoreCase = true) ||
                    sub.categoryTitle.contains(q, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .let { base -> if (nestedScrollConnection != null) base.nestedScroll(nestedScrollConnection) else base }
    ) {
        SharedTopBar(
            title = "Settings",
            subtitle = "DYNE CONFIGURATION",
            hazeState = hazeState
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Search Bar ("Search settings" — filters rows and sub-options with highlighted matches)
            GlassSurface(
                hazeState = hazeState,
                shape = RoundedCornerShape(24.dp),
                cornerRadius = 24.dp,
                shadowElevation = 8.dp,
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_search_bar")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search settings",
                    tint = theme.accent,
                    modifier = Modifier.size(20.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search settings, tools, backup or locks",
                            style = type.body,
                            color = theme.textSecondary
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = type.body.copy(color = theme.textPrimary),
                        cursorBrush = SolidColor(theme.accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_search_input")
                    )
                }
                if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = theme.textSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .dyneClickable(onClick = { searchQuery = "" })
                        )
                    }
                }
            }

            // Status Banner Feedback
            if (statusBanner != null) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(theme.surface)
                        .border(1.dp, theme.accent.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = statusBanner ?: "",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.accent,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Dismiss",
                        style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                        color = theme.textSecondary,
                        modifier = Modifier.dyneClickable(onClick = viewModel::clearStatusBanner)
                    )
                }
            }

            if (q.isEmpty()) {
                // 1. TOP CARD: Logo, "Dyne", and Version (plus 7-tap Developer Options unlock)
                GlassSurface(
                    hazeState = hazeState,
                    shape = RoundedCornerShape(24.dp),
                    cornerRadius = 24.dp,
                    shadowElevation = 10.dp,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .dyneClickable(
                            testTag = "settings_row_version",
                            onClick = {
                                if (BuildConfig.DEBUG) {
                                    versionTapCounter += 1
                                    if (versionTapCounter >= 7) {
                                        versionTapCounter = 0
                                        onNavigateSubScreen(SettingsSubScreen.DEVELOPER_OPTIONS)
                                    } else if (versionTapCounter >= 4) {
                                        val rem = 7 - versionTapCounter
                                        viewModel.showStatusMessage("$rem more tap${if (rem == 1) "" else "s"} to open Developer options")
                                    }
                                }
                            }
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            com.example.DyneLogo(size = 56.dp)
                            Column {
                                Text(
                                    text = "Dyne",
                                    style = type.title.copy(fontWeight = FontWeight.ExtraBold),
                                    color = theme.textPrimary
                                )
                                Text(
                                    text = "Version ${BuildConfig.VERSION_NAME} • Precision Utility Suite",
                                    style = type.caption,
                                    color = theme.textSecondary
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(theme.elevated)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                                color = theme.accent
                            )
                        }
                    }
                }

                // 2. STATUS CARD (Dyne Premium + Value Meter + Remove Ads one-time product):
                // Free: gold gradient "Upgrade to Dyne Premium" with 3 short benefits
                // Premium: "Dyne Premium active" with renewal info
                val weeklyAdCoinUses = prefs.getWeeklyAdOrCoinUsesCount()
                if (!isPro) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(26.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            ProGold,
                                            ProGoldDeep
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(26.dp)
                                )
                                .dyneClickable(
                                    testTag = "settings_upgrade_pro_card",
                                    onClick = onOpenPaywall
                                )
                                .padding(horizontal = 20.dp, vertical = 18.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(BentoTextBlack.copy(alpha = 0.14f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WorkspacePremium,
                                            contentDescription = "Dyne Premium",
                                            tint = BentoTextBlack,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Upgrade to Dyne Premium",
                                            style = type.title.copy(fontWeight = FontWeight.ExtraBold),
                                            color = BentoTextBlack
                                        )
                                        Text(
                                            text = com.example.domain.FeatureRegistry.liveCounterText,
                                            style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                                            color = BentoTextBlack.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Open Paywall",
                                    tint = BentoTextBlack
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "✓ ${com.example.domain.FeatureRegistry.liveCounterText} • All 8 glass themes & custom Home",
                                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                                    color = BentoTextBlack.copy(alpha = 0.88f)
                                )
                                Text(
                                    text = "✓ Full ZIP vault backup, App Lock & biometric vault",
                                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                                    color = BentoTextBlack.copy(alpha = 0.88f)
                                )
                                Text(
                                    text = "✓ 100% ad-free precision experience",
                                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                                    color = BentoTextBlack.copy(alpha = 0.88f)
                                )
                            }
                        }

                        // VALUE METER in Settings > Dyne Premium:
                        // "This week you used N Premium features with ads or coins" with a gentle "Get them all" button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(theme.surface)
                                .border(1.dp, ProGold.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("settings_value_meter_card")
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.weight(1f).padding(end = 10.dp)
                            ) {
                                Text(
                                    text = "DYNE PREMIUM VALUE METER",
                                    style = type.section.copy(fontWeight = FontWeight.ExtraBold),
                                    color = ProGold
                                )
                                Text(
                                    text = "This week you used $weeklyAdCoinUses Premium feature${if (weeklyAdCoinUses == 1) "" else "s"} with ads or coins",
                                    style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                                    color = theme.textPrimary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(ProGold.copy(alpha = 0.18f))
                                    .border(1.dp, ProGold.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                                    .dyneClickable(
                                        testTag = "settings_value_meter_get_all_button",
                                        onClick = onOpenPaywall
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Get them all",
                                    style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                                    color = ProGold
                                )
                            }
                        }

                        // Compact One-Time "Remove Ads" Card in Settings (Premium includes it)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(theme.surface)
                                .border(
                                    1.dp,
                                    if (isRemoveAds) theme.accent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.12f),
                                    RoundedCornerShape(20.dp)
                                )
                                .dyneClickable(
                                    testTag = "settings_remove_ads_card",
                                    onClick = {
                                        if (!isRemoveAds) {
                                            if (hostActivity != null) {
                                                viewModel.launchRemoveAdsPurchase(hostActivity)
                                            } else {
                                                onOpenPaywall()
                                            }
                                        }
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.weight(1f).padding(end = 10.dp)
                            ) {
                                Text(
                                    text = if (isRemoveAds) "Remove Ads Active ✓" else "Remove Ads (One-Time)",
                                    style = type.body.copy(fontWeight = FontWeight.Bold),
                                    color = if (isRemoveAds) theme.accent else theme.textPrimary
                                )
                                Text(
                                    text = "Removes banners & interstitials only • Included free in Dyne Premium",
                                    style = type.caption,
                                    color = theme.textSecondary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isRemoveAds) theme.accent.copy(alpha = 0.16f) else theme.elevated)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = when {
                                        isRemoveAds -> "Owned ✓"
                                        removeAdsPrice.isNotBlank() -> removeAdsPrice
                                        else -> "Google Play"
                                    },
                                    style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                                    color = if (isRemoveAds) theme.accent else ProGold
                                )
                            }
                        }
                    }
                } else {
                    // Clean single-row mark for Premium users: "Dyne Premium - active" + "Manage subscription"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(theme.surface)
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 13.dp)
                            .testTag("settings_pro_active_card")
                    ) {
                        Text(
                            text = "Dyne Premium - active",
                            style = type.body.copy(fontWeight = FontWeight.SemiBold),
                            color = theme.textPrimary
                        )
                        Text(
                            text = "Manage subscription",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.accent,
                            modifier = Modifier
                                .testTag("settings_manage_subscription_button")
                                .dyneClickable(
                                    onClick = {
                                        runCatching {
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://play.google.com/store/account/subscriptions?package=${context.packageName}")
                                            )
                                            context.startActivity(intent)
                                        }
                                    }
                                )
                        )
                    }
                }

                // Compact "All features (N)" row with live feature count ("164 features, 63 Premium") in Settings
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(theme.surface)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                        .dyneClickable(
                            testTag = "settings_root_all_features_row",
                            onClick = onOpenAllFeatures
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "All features (${com.example.domain.FeatureRegistry.totalCount})",
                            style = type.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                            color = theme.textPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = if (isPro) {
                                "${com.example.domain.FeatureRegistry.totalCount} features unlocked"
                            } else {
                                com.example.domain.FeatureRegistry.liveCounterText
                            },
                            style = type.caption,
                            color = theme.textSecondary,
                            maxLines = 1
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "All",
                            style = type.caption.copy(fontWeight = FontWeight.Bold, color = theme.accent)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "All features",
                            tint = theme.accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // 3. 2x2 GRID OF QUICK TOGGLES WITH LARGE ICONS:
                // Tap sounds, Haptics, Dark/Light, Reduce glass
                val isCurrentlyDark = theme.isDark
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickToggleTile(
                            title = "Tap sounds",
                            stateLabel = if (prefs.tapSoundsEnabled) "On" else "Off",
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            active = prefs.tapSoundsEnabled,
                            accentColor = theme.accent,
                            onClick = { viewModel.setTapSoundsEnabled(!prefs.tapSoundsEnabled) },
                            testTag = "quick_toggle_tap_sounds",
                            modifier = Modifier.weight(1f)
                        )
                        QuickToggleTile(
                            title = "Haptics",
                            stateLabel = if (prefs.hapticsEnabled) prefs.hapticStrength.label else "Off",
                            icon = Icons.Default.Vibration,
                            active = prefs.hapticsEnabled,
                            accentColor = Color(0xFFFB7185),
                            onClick = { viewModel.setHapticsEnabled(!prefs.hapticsEnabled) },
                            testTag = "quick_toggle_haptics",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickToggleTile(
                            title = "Dark/Light",
                            stateLabel = when (prefs.themeMode) {
                                ThemeMode.DARK -> "Dark"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.SYSTEM -> if (isCurrentlyDark) "System (Dark)" else "System (Light)"
                            },
                            icon = if (isCurrentlyDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                            active = isCurrentlyDark,
                            accentColor = Color(0xFF38BDF8),
                            onClick = {
                                val nextMode = if (isCurrentlyDark) ThemeMode.LIGHT else ThemeMode.DARK
                                viewModel.setThemeMode(nextMode)
                            },
                            testTag = "quick_toggle_dark_light",
                            modifier = Modifier.weight(1f)
                        )
                        QuickToggleTile(
                            title = "Reduce glass",
                            stateLabel = if (prefs.reduceGlassEffects) "Reduced" else "Full Glass",
                            icon = Icons.Default.BlurOn,
                            active = prefs.reduceGlassEffects,
                            accentColor = Color(0xFF818CF8),
                            onClick = { viewModel.setReduceGlassEffects(!prefs.reduceGlassEffects) },
                            testTag = "quick_toggle_reduce_glass",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. ONE GROUPED LIST OF 10 ROWS:
            // (a colored rounded-square icon, a SemiBold title, a one-line current-value summary, a chevron)
            if (filteredRows.isNotEmpty()) {
                InsetSettingsGroup(header = if (q.isEmpty()) "" else "CATEGORIES (${filteredRows.size})") {
                    filteredRows.forEachIndexed { idx, rowSpec ->
                        if (idx > 0) InsetDivider()
                        SettingsMainSummaryRow(
                            icon = rowSpec.icon,
                            iconBg = rowSpec.iconBg,
                            title = rowSpec.title,
                            summary = rowSpec.summary,
                            query = q,
                            testTag = rowSpec.testTag,
                            onClick = { onNavigateSubScreen(rowSpec.subScreen) }
                        )
                    }
                }
            }

            // Matching sub-options when searching
            if (q.isNotEmpty() && filteredSubOptions.isNotEmpty()) {
                InsetSettingsGroup(header = "MATCHING SUB-OPTIONS (${filteredSubOptions.size})") {
                    filteredSubOptions.forEachIndexed { idx, sub ->
                        if (idx > 0) InsetDivider()
                        SettingsMainSummaryRow(
                            icon = sub.icon,
                            iconBg = sub.iconBg,
                            title = sub.title,
                            summary = "${sub.categoryTitle} • ${sub.subtitle}",
                            query = q,
                            testTag = null,
                            onClick = { onNavigateSubScreen(sub.targetSubScreen) }
                        )
                    }
                }
            }

            if (q.isNotEmpty() && filteredRows.isEmpty() && filteredSubOptions.isEmpty()) {
                DesignedEmptyStateCard(
                    title = "No matching settings",
                    description = "No settings rows or sub-options matched \"$searchQuery\". Try searching for Theme, Tap Sounds, Solunar, ZIP, or Biometrics.",
                    accentColor = theme.accent,
                    actionLabel = "Clear Search",
                    onActionClick = { searchQuery = "" }
                )
            }

            // Quiet centered credit block at the very bottom of Settings
            DyneQuietCreditBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
                    .testTag("settings_bottom_credit_block")
            )

            Spacer(modifier = Modifier.height(LocalBottomBarInset.current))
        }
    }
}

@Composable
internal fun DyneQuietCreditBlock(modifier: Modifier = Modifier) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.padding(vertical = 12.dp)
    ) {
        com.example.DyneLogo(size = 40.dp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Dyne • Version ${BuildConfig.VERSION_NAME}",
            style = type.caption.copy(fontWeight = FontWeight.Bold),
            color = theme.textPrimary.copy(alpha = 0.85f)
        )
        Text(
            text = "Built by Sibora",
            style = type.caption,
            color = theme.textSecondary
        )
        Text(
            text = "Founded by Sibnath Bairagi",
            style = type.caption,
            color = theme.textSecondary
        )
        Text(
            text = "Made with care",
            style = type.caption,
            color = theme.textSecondary.copy(alpha = 0.8f)
        )
        Text(
            text = "(c) 2026 Sibora",
            style = type.caption.copy(fontSize = 11.sp),
            color = theme.textSecondary.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun QuickToggleTile(
    title: String,
    stateLabel: String,
    icon: ImageVector,
    active: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (active) accentColor.copy(alpha = 0.16f) else theme.surface)
            .border(
                width = 1.dp,
                color = if (active) accentColor.copy(alpha = 0.65f)
                else if (theme.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.07f),
                shape = RoundedCornerShape(22.dp)
            )
            .dyneClickable(
                targetScale = 0.97f,
                testTag = testTag,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (active) accentColor else theme.elevated)
        ) {
            val useDarkTint = active && (accentColor == Color(0xFFCCFF00) || accentColor == Color(0xFFC6F27A) || accentColor == ProGold)
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (useDarkTint) BentoTextBlack else if (active) Color.White else theme.textSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = type.body.copy(fontWeight = FontWeight.SemiBold),
                color = theme.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stateLabel,
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = if (active) accentColor else theme.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsMainSummaryRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    summary: String,
    query: String,
    testTag: String?,
    onClick: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .dyneClickable(
                targetScale = 0.98f,
                testTag = testTag,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            RoundedSquareIconBadge(icon = icon, bgColor = iconBg)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = highlightMatch(title, query, theme.accent),
                    style = type.body.copy(fontWeight = FontWeight.SemiBold),
                    color = theme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = highlightMatch(summary, query, theme.accent),
                    style = type.caption,
                    color = theme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = theme.textSecondary.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * 1. APPEARANCE SUB-SCREEN:
 * Theme grid, mode (System/Light/Dark), text size with live preview,
 * reduce glass and motion, launch animation, and app icon style.
 */
@Composable
private fun AppearanceSubScreen(
    viewModel: SettingsViewModel,
    hazeState: HazeState?,
    onOpenPaywall: () -> Unit,
    onBack: () -> Unit
) {
    val adManager = LocalAdManager.current
    val featureGate = LocalFeatureGate.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()

    val rows = remember { ThemeEngine.allThemes.chunked(2) }
    val iconStyles = remember { listOf("Orange", "Lime Night", "AMOLED Mono", "Midnight Gold") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SubScreenTopBar(
            title = "Appearance",
            subtitle = "THEMES, MODE, TEXT SIZE & GLASS ENGINE",
            hazeState = hazeState,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Mode, Text Size with Live Preview, Reduce Glass & Motion, Launch Animation
            InsetSettingsGroup(header = "DISPLAY MODE & TEXT SIZE") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RoundedSquareIconBadge(
                            icon = Icons.Default.Tune,
                            bgColor = Color(0xFF60A5FA)
                        )
                        Text(
                            text = "Appearance Mode (System / Light / Dark)",
                            style = type.body.copy(fontWeight = FontWeight.SemiBold),
                            color = theme.textPrimary
                        )
                    }
                    DyneSegmentedControl(
                        segments = ThemeMode.entries.map { it.label },
                        selectedIndex = ThemeMode.entries.indexOf(prefs.themeMode).coerceAtLeast(0),
                        onSegmentSelected = { idx ->
                            viewModel.setThemeMode(ThemeMode.entries[idx])
                        }
                    )
                }

                InsetDivider()

                // Text Size Slider with Live Preview
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RoundedSquareIconBadge(
                                icon = Icons.Default.FormatSize,
                                bgColor = Color(0xFF34D399)
                            )
                            Text(
                                text = "Text Size",
                                style = type.body.copy(fontWeight = FontWeight.SemiBold),
                                color = theme.textPrimary
                            )
                        }
                        Text(
                            text = "${(prefs.textScale * 100).roundToInt()}%",
                            style = type.gaugeSmall,
                            color = theme.accent
                        )
                    }

                    Slider(
                        value = prefs.textScale,
                        onValueChange = { viewModel.setTextScale(it) },
                        valueRange = 0.85f..1.25f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = theme.accent,
                            activeTrackColor = theme.accent,
                            inactiveTrackColor = theme.elevated
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.elevated)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Live preview: Plus Jakarta Sans & Space Grotesk 1013.2 hPa • 0.0°",
                            style = type.body,
                            color = theme.textPrimary
                        )
                    }
                }

                InsetDivider()

                InsetSettingsSwitchRow(
                    icon = Icons.Default.BlurOn,
                    iconBg = Color(0xFF818CF8),
                    title = "Reduce Glass Effects",
                    subtitle = "Higher opacity surfaces & reduced lens refraction",
                    query = "",
                    checked = prefs.reduceGlassEffects,
                    onCheckedChange = viewModel::setReduceGlassEffects
                )

                InsetDivider()

                InsetSettingsSwitchRow(
                    icon = Icons.Default.Animation,
                    iconBg = Color(0xFFF472B6),
                    title = "Reduce Motion",
                    subtitle = "Minimize spring bounce & scale transitions",
                    query = "",
                    checked = prefs.reduceMotion,
                    onCheckedChange = viewModel::setReduceMotion
                )

                InsetDivider()

                // Launch animation row
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RoundedSquareIconBadge(
                                icon = Icons.Default.Animation,
                                bgColor = theme.accent
                            )
                            Column {
                                Text(
                                    text = "Launch Animation",
                                    style = type.body.copy(fontWeight = FontWeight.SemiBold),
                                    color = theme.textPrimary
                                )
                                Text(
                                    text = "Cold-start splash variant & palette mode",
                                    style = type.caption,
                                    color = theme.textSecondary
                                )
                            }
                        }
                        Text(
                            text = prefs.launchAnimationMode.label,
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.accent
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalChipRowEdgeFade()
                    ) {
                        items(LaunchAnimationMode.entries) { mode ->
                            val selected = prefs.launchAnimationMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) theme.accent else theme.elevated)
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) theme.accent else Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .dyneClickable(
                                        testTag = "launch_anim_mode_${mode.name.lowercase()}",
                                        onClick = { viewModel.setLaunchAnimationMode(mode) }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = mode.label,
                                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                                    color = if (selected) theme.onAccent else theme.textPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                InsetDivider()

                // App Icon Style
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "App Badge Style (${prefs.selectedAppIconStyle})",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.textSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(iconStyles) { style ->
                            val selected = prefs.selectedAppIconStyle == style
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) theme.accent else theme.elevated)
                                    .dyneClickable(onClick = { viewModel.setSelectedAppIconStyle(style) })
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = style,
                                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                                    color = if (selected) theme.onAccent else theme.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            // 2. Theme Grid (8 Live Preview Cards)
            Text(
                text = "THEME GRID (8 LIVE GLASS PALETTES)",
                style = type.section,
                color = theme.accent,
                modifier = Modifier.padding(start = 12.dp)
            )
            Text(
                text = if (isPro) {
                    "Tap any theme card for a smooth live crossfade."
                } else {
                    "Tap any theme card for a smooth live crossfade. Free users include Lime Night; other themes can be unlocked for 1 hour via an ad or coins, or permanently with Dyne Premium."
                },
                style = type.caption,
                color = theme.textSecondary
            )

            val effectiveSelectedTheme = prefs.effectiveThemeName(isPro)
            rows.forEach { pair ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    pair.forEach { candidate ->
                        val isDefaultFreeTheme = candidate.name.equals("Lime Night", ignoreCase = true)
                        val isSelected = effectiveSelectedTheme.equals(candidate.name, ignoreCase = true)
                        val isThemeUnlocked = isDefaultFreeTheme ||
                            isPro ||
                            prefs.isThemeAccessActive(candidate.name, false) ||
                            (featureGate?.isThemeUnlocked(candidate.name, true, prefs) == true)
                        val isLocked = !isThemeUnlocked
                        ThemeLivePreviewCard(
                            candidate = candidate,
                            isSelected = isSelected,
                            isLocked = isLocked,
                            isAccountPro = isPro,
                            onClick = {
                                if (isLocked) {
                                    if (featureGate != null) {
                                        featureGate.require(
                                            feature = PremiumFeature.PREMIUM_THEMES,
                                            themeName = candidate.name,
                                            onUnlocked = {
                                                if (isPro) {
                                                    viewModel.setSelectedThemeName(candidate.name)
                                                } else if (candidate.name == "Classic Light") {
                                                    viewModel.setThemeMode(ThemeMode.LIGHT)
                                                } else if (prefs.themeMode == ThemeMode.LIGHT) {
                                                    viewModel.setThemeMode(ThemeMode.DARK)
                                                }
                                            }
                                        )
                                    } else if (adManager != null) {
                                        adManager.promptRewardedUnlock(
                                            RewardUnlockPromptTarget.LockedTheme(
                                                themeName = candidate.name,
                                                onUnlocked = {
                                                    if (isPro) {
                                                        viewModel.setSelectedThemeName(candidate.name)
                                                    }
                                                }
                                            )
                                        )
                                    } else {
                                        onOpenPaywall()
                                    }
                                } else {
                                    viewModel.setSelectedThemeName(candidate.name)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Custom Accent Color Picker, Tile Styles, Hero Carousel Styles & Icon Sets (Premium)
            PremiumStyleAndAccentStudioCard(
                userPreferences = prefs,
                preferencesRepository = viewModel.preferencesRepository,
                featureGate = featureGate,
                isPro = isPro,
                onRequestPaywall = onOpenPaywall
            )

            // Home Screen Glance Widgets Gallery (Premium)
            HomeScreenWidgetsGalleryCard(
                userPreferences = prefs,
                featureGate = featureGate,
                isPro = isPro,
                onRequestPaywall = onOpenPaywall
            )

            Spacer(modifier = Modifier.height(118.dp))
        }
    }
}

@Composable
private fun ThemeLivePreviewCard(
    candidate: DyneTheme,
    isSelected: Boolean,
    isLocked: Boolean,
    isAccountPro: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(theme.surface)
            .let { base ->
                when {
                    isSelected -> base.border(2.dp, theme.accent, RoundedCornerShape(26.dp))
                    isLocked && !isAccountPro -> base.border(
                        1.dp,
                        Brush.linearGradient(listOf(ProGold, ProGoldDeep)),
                        RoundedCornerShape(26.dp)
                    )
                    else -> base.border(
                        1.dp,
                        if (theme.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
                        RoundedCornerShape(26.dp)
                    )
                }
            }
            .dyneClickable(
                testTag = "theme_card_${candidate.name.lowercase().replace(" ", "_")}",
                onClick = onClick
            )
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(candidate.background)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pad = 10.dp.toPx()
                val w = size.width - pad * 2f

                // Hero Card Mockup
                drawRoundRect(
                    color = candidate.surface,
                    topLeft = Offset(pad, pad),
                    size = Size(w, 34.dp.toPx()),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
                drawCircle(
                    color = candidate.accent,
                    radius = 7.dp.toPx(),
                    center = Offset(pad + 16.dp.toPx(), pad + 17.dp.toPx())
                )

                // 3 Mini Tool Tiles
                val tileY = pad + 42.dp.toPx()
                val tileW = (w - 12.dp.toPx()) / 3f
                for (i in 0..2) {
                    val tx = pad + i * (tileW + 6.dp.toPx())
                    drawRoundRect(
                        color = candidate.elevated,
                        topLeft = Offset(tx, tileY),
                        size = Size(tileW, 28.dp.toPx()),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }

                // 4 Mini Bento pills at the bottom in candidate's 4 bentoColors
                val bentoY = tileY + 36.dp.toPx()
                val bentoW = (w - 18.dp.toPx()) / 4f
                candidate.bentoColors.take(4).forEachIndexed { idx, bColor ->
                    val bx = pad + idx * (bentoW + 6.dp.toPx())
                    drawRoundRect(
                        color = bColor,
                        topLeft = Offset(bx, bentoY),
                        size = Size(bentoW, 24.dp.toPx()),
                        cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                    )
                }
            }

            if (isSelected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(theme.accent)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected Theme",
                        tint = BentoTextBlack,
                        modifier = Modifier.size(15.dp)
                    )
                }
            } else if (isLocked && !isAccountPro) {
                ProBadge(
                    label = "PRO",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = candidate.name,
                    style = type.body.copy(fontWeight = FontWeight.Bold),
                    color = theme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = candidate.subtitle,
                    style = type.caption,
                    color = theme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 3. HOME & TOOLS SUB-SCREEN (CUSTOMIZE HOME)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomizeHomeSubScreen(
    viewModel: SettingsViewModel,
    hazeState: HazeState?,
    onOpenPaywall: () -> Unit = {},
    onOpenAllFeatures: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val featureGate = LocalFeatureGate.current
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val tools by ToolRegistry.tools.collectAsState()
    val orderedCategories by ToolRegistry.orderedCategories.collectAsState()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val outdoorsConfigs = remember(prefs.outdoorsSegmentsCsv) {
        viewModel.getOutdoorsSegmentConfigs(prefs.outdoorsSegmentsCsv)
    }
    val selectedScanShortcuts = prefs.scanShortcutsList

    val supportedTools = remember(tools, context) {
        tools.filter { com.example.data.DeviceCapabilities.isToolSupported(it.requiredCapabilities, context) }
    }
    val unsupportedTools = remember(tools, context) {
        tools.filterNot { com.example.data.DeviceCapabilities.isToolSupported(it.requiredCapabilities, context) }
    }
    var unavailableSectionExpanded by rememberSaveable { mutableStateOf(false) }

    val blockTitles = remember {
        mapOf(
            "hero" to "Hero Carousel",
            "favorites" to "Favorites & Recently Used",
            "sky_now" to "Sky Now Mini Dome",
            "dyne_premium" to "Dyne Premium Section",
            "categories" to "Free Tool Categories",
            "quick_access" to "Quick Access Bento"
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (statusBanner != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.surface)
                    .border(1.dp, theme.accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = statusBanner ?: "",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.accent
                )
            }
        }

        // 1. UNIFIED HOME & FEATURE CATALOG CUSTOMIZER (Free sees locked preview with 1-hour ad unlock; Premium/1h sees full editor)
        com.example.ui.home.CustomizeHomeEditorScreen(
            viewModel = viewModel,
            preferences = prefs,
            capabilities = com.example.data.DeviceCapabilities,
            featureGate = featureGate,
            isPro = isPro,
            showTopBar = true,
            onBack = onBack,
            onOpenPaywall = onOpenPaywall,
            onOpenAllFeatures = onOpenAllFeatures,
            modifier = Modifier.weight(1f),
            extraFooterContent = null
        )
        if (false) {
                // 1B. HOME SCREEN WIDGETS (PREMIUM, JETPACK GLANCE)
                HomeScreenWidgetsGalleryCard(
                    userPreferences = prefs,
                    featureGate = featureGate,
                    isPro = isPro,
                    onRequestPaywall = onOpenPaywall
                )

            // 1C. CUSTOMIZATION STUDIO (PREMIUM: ACCENT PICKER, TILE STYLES, HERO STYLES, ICON SETS)
            PremiumStyleAndAccentStudioCard(
                userPreferences = prefs,
                preferencesRepository = viewModel.preferencesRepository,
                featureGate = featureGate,
                isPro = isPro,
                onRequestPaywall = onOpenPaywall
            )

            // 2. OUTDOOR INTERESTS
            InsetSettingsGroup(header = "OUTDOOR INTERESTS") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Select your outdoor activities. Deselecting all hides the Solunar slide from the Home hero carousel.",
                        style = type.caption,
                        color = theme.textSecondary
                    )
                    val interests = listOf("Fishing", "Hunting", "Camping/Photography")
                    val activeSet = prefs.outdoorInterestsSet
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        interests.forEach { interest ->
                            val selected = activeSet.any { it.equals(interest, ignoreCase = true) }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selected) theme.accent else theme.elevated)
                                    .border(
                                        1.dp,
                                        if (selected) theme.accent else Color.White.copy(alpha = 0.12f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .dyneClickable(
                                        testTag = "outdoor_interest_${interest.lowercase().replace("/", "_")}",
                                        onClick = { viewModel.toggleOutdoorInterest(interest) }
                                    )
                                    .padding(horizontal = 14.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = interest,
                                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                                    color = if (selected) BentoTextBlack else theme.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            // 3. HOME WIDGETS & BLOCKS ORDER
            InsetSettingsGroup(header = "HOME WIDGETS & BLOCKS (TOGGLE & DRAG TO REORDER)") {
                val orderedBlocks = prefs.homeBlockOrderList
                val disabledBlocks = prefs.disabledHomeBlocksSet
                orderedBlocks.forEachIndexed { index, blockId ->
                    if (index > 0) InsetDivider()
                    var blockDragAccum by remember { mutableFloatStateOf(0f) }
                    val isEnabled = !disabledBlocks.contains(blockId)
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(index, orderedBlocks.size) {
                                detectVerticalDragGestures(
                                    onVerticalDrag = { _, dragAmount ->
                                        blockDragAccum += dragAmount
                                        if (blockDragAccum > 42f && index < orderedBlocks.lastIndex) {
                                            viewModel.moveHomeBlock(index, index + 1)
                                            blockDragAccum = 0f
                                        } else if (blockDragAccum < -42f && index > 0) {
                                            viewModel.moveHomeBlock(index, index - 1)
                                            blockDragAccum = 0f
                                        }
                                    },
                                    onDragEnd = { blockDragAccum = 0f }
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Drag block",
                                tint = theme.textSecondary
                            )
                            Text(
                                text = blockTitles[blockId.lowercase()] ?: blockId,
                                style = type.body.copy(fontWeight = FontWeight.Bold),
                                color = theme.textPrimary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (index > 0) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Move block up",
                                    tint = theme.accent,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .dyneClickable(onClick = { viewModel.moveHomeBlock(index, index - 1) })
                                )
                            }
                            if (index < orderedBlocks.lastIndex) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Move block down",
                                    tint = theme.accent,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .dyneClickable(onClick = { viewModel.moveHomeBlock(index, index + 1) })
                                )
                            }
                            DyneSwitch(
                                checked = isEnabled,
                                onCheckedChange = { viewModel.toggleHomeBlock(blockId, it) }
                            )
                        }
                    }
                }
            }

            // 4. "WHAT DO YOU NEED?" CHIP PICKER
            InsetSettingsGroup(header = "WHAT DO YOU NEED? (QUICK PRESETS)") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Text(
                        text = "Pick a profile to pre-select relevant tools on your Home screen. You can change this anytime or toggle individual tools below.",
                        style = type.caption,
                        color = theme.textSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalChipRowEdgeFade()
                    ) {
                        items(HomeNeedPreset.entries) { preset ->
                            val isSelected = prefs.selectedHomePreset.equals(preset.label, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) theme.accent else theme.elevated)
                                    .border(
                                        1.dp,
                                        if (isSelected) theme.accent else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .dyneClickable(
                                        onClick = { viewModel.applyHomePreset(preset) },
                                        testTag = "preset_chip_${preset.label.lowercase()}"
                                    )
                                    .padding(horizontal = 14.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = preset.label,
                                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) BentoTextBlack else theme.textPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 5. SCAN MENU SHORTCUTS (UP TO 4 ITEMS)
            InsetSettingsGroup(header = "SCAN MENU SHORTCUTS (${selectedScanShortcuts.size}/4 SELECTED)") {
                AvailableScanShortcuts.forEachIndexed { idx, option ->
                    val isChecked = selectedScanShortcuts.contains(option.id)
                    if (idx > 0) InsetDivider()
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .dyneClickable(onClick = { viewModel.toggleScanShortcut(option.id) })
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            RoundedSquareIconBadge(
                                icon = Icons.Default.CenterFocusStrong,
                                bgColor = if (isChecked) theme.accent else theme.elevated
                            )
                            Column {
                                Text(
                                    text = option.title,
                                    style = type.body.copy(fontWeight = FontWeight.SemiBold),
                                    color = theme.textPrimary
                                )
                                Text(
                                    text = option.subtitle,
                                    style = type.caption,
                                    color = theme.textSecondary
                                )
                            }
                        }
                        DyneSwitch(
                            checked = isChecked,
                            onCheckedChange = { viewModel.toggleScanShortcut(option.id) }
                        )
                    }
                }
            }

            // 6. HOME SECTION REORDER & PER-SECTION LAYOUT OVERRIDE
            InsetSettingsGroup(header = "CATEGORY ORDER & PER-SECTION LAYOUT") {
                orderedCategories.forEachIndexed { index, category ->
                    if (index > 0) InsetDivider()
                    var dragAccum by remember { mutableFloatStateOf(0f) }
                    val overrideCol = prefs.sectionLayoutOverridesMap[category.name]
                    val nextOverrideLabel = when (overrideCol) {
                        null -> "Global"
                        0 -> "Row"
                        2 -> "2 Col"
                        3 -> "3 Col"
                        4 -> "4 Col"
                        1 -> "List"
                        else -> "Global"
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(index, orderedCategories.size) {
                                detectVerticalDragGestures(
                                    onVerticalDrag = { _, dragAmount ->
                                        dragAccum += dragAmount
                                        if (dragAccum > 42f && index < orderedCategories.lastIndex) {
                                            viewModel.moveCategorySection(index, index + 1)
                                            dragAccum = 0f
                                        } else if (dragAccum < -42f && index > 0) {
                                            viewModel.moveCategorySection(index, index - 1)
                                            dragAccum = 0f
                                        }
                                    },
                                    onDragEnd = { dragAccum = 0f }
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Drag section",
                                tint = theme.textSecondary
                            )
                            Text(
                                text = category.title,
                                style = type.body.copy(fontWeight = FontWeight.Bold),
                                color = theme.textPrimary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(theme.elevated)
                                    .dyneClickable {
                                        val cycle = listOf<Int?>(null, 0, 2, 3, 4, 1)
                                        val nextIdx = (cycle.indexOf(overrideCol) + 1) % cycle.size
                                        viewModel.setSectionLayoutOverride(category.name, cycle[nextIdx])
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = nextOverrideLabel,
                                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                                    color = theme.accent
                                )
                            }
                            if (index > 0) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Move section up",
                                    tint = theme.accent,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .dyneClickable(onClick = { viewModel.moveCategorySection(index, index - 1) })
                                )
                            }
                            if (index < orderedCategories.lastIndex) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Move section down",
                                    tint = theme.accent,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .dyneClickable(onClick = { viewModel.moveCategorySection(index, index + 1) })
                                )
                            }
                        }
                    }
                }
            }

            // 7. OUTDOORS SEGMENTS
            InsetSettingsGroup(header = "OUTDOORS TAB SEGMENTS") {
                outdoorsConfigs.forEachIndexed { index, seg ->
                    if (index > 0) InsetDivider()
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Reorder segment",
                                tint = theme.textSecondary
                            )
                            Text(
                                text = seg.title,
                                style = type.body.copy(fontWeight = FontWeight.SemiBold),
                                color = theme.textPrimary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (index > 0) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Move up",
                                    tint = theme.textSecondary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .dyneClickable(onClick = { viewModel.moveOutdoorsSegment(index, index - 1) })
                                )
                            }
                            if (index < outdoorsConfigs.lastIndex) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Move down",
                                    tint = theme.textSecondary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .dyneClickable(onClick = { viewModel.moveOutdoorsSegment(index, index + 1) })
                                )
                            }
                            DyneSwitch(
                                checked = seg.enabled,
                                onCheckedChange = { viewModel.toggleOutdoorsSegment(seg.id, it) }
                            )
                        }
                    }
                }
            }

            // 8. ALL AVAILABLE HOME TOOLS
            var expandedToolModesId by rememberSaveable { mutableStateOf<String?>(null) }
            InsetSettingsGroup(header = "AVAILABLE TOOLS & MODES (${supportedTools.count { it.isEnabled }}/${supportedTools.size} ENABLED)") {
                supportedTools.forEachIndexed { index, tool ->
                    if (index > 0) InsetDivider()
                    val globalIndex = tools.indexOfFirst { it.id == tool.id }
                    var toolDragAccum by remember { mutableFloatStateOf(0f) }
                    val configurableModes = remember(tool.id, prefs.customToolModesConfigCsv) {
                        ToolRegistry.getConfigurableModesForTool(tool, prefs.customToolModesConfigCsv)
                    }
                    val isModesExpanded = expandedToolModesId == tool.id

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(globalIndex, tools.size) {
                                    detectVerticalDragGestures(
                                        onVerticalDrag = { _, dragAmount ->
                                            toolDragAccum += dragAmount
                                            if (toolDragAccum > 42f && globalIndex < tools.lastIndex) {
                                                viewModel.moveToolOrder(globalIndex, globalIndex + 1)
                                                toolDragAccum = 0f
                                            } else if (toolDragAccum < -42f && globalIndex > 0) {
                                                viewModel.moveToolOrder(globalIndex, globalIndex - 1)
                                                toolDragAccum = 0f
                                            }
                                        },
                                        onDragEnd = { toolDragAccum = 0f }
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .dyneClickable(
                                        onClick = {
                                            expandedToolModesId = if (isModesExpanded) null else tool.id
                                        }
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Drag to reorder",
                                    tint = theme.textSecondary
                                )
                                RoundedSquareIconBadge(
                                    icon = tool.icon,
                                    bgColor = theme.accent
                                )
                                Column {
                                    Text(
                                        text = tool.name,
                                        style = type.body.copy(fontWeight = FontWeight.SemiBold),
                                        color = theme.textPrimary
                                    )
                                    Text(
                                        text = "${tool.category.title} • ${configurableModes.count { it.second }}/${configurableModes.size} modes",
                                        style = type.caption,
                                        color = theme.textSecondary
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(theme.elevated)
                                        .dyneClickable(
                                            testTag = "customize_modes_${tool.id}",
                                            onClick = {
                                                expandedToolModesId = if (isModesExpanded) null else tool.id
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isModesExpanded) "Hide modes" else "Modes",
                                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                                        color = theme.accent
                                    )
                                }
                                if (globalIndex > 0) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Move Up",
                                        tint = theme.textSecondary,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .dyneClickable(onClick = { viewModel.moveToolOrder(globalIndex, globalIndex - 1) })
                                    )
                                }
                                if (globalIndex < tools.lastIndex) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Move Down",
                                        tint = theme.textSecondary,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .dyneClickable(onClick = { viewModel.moveToolOrder(globalIndex, globalIndex + 1) })
                                    )
                                }
                                DyneSwitch(
                                    checked = tool.isEnabled,
                                    onCheckedChange = { viewModel.setToolEnabled(tool.id, it) }
                                )
                            }
                        }

                        if (isModesExpanded && configurableModes.isNotEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 42.dp, end = 16.dp, bottom = 12.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(theme.elevated.copy(alpha = 0.7f))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "Show, hide & reorder ${tool.name} modes",
                                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                                    color = theme.accent
                                )
                                configurableModes.forEachIndexed { modeIdx, (mode, isVisible) ->
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = mode.icon,
                                                contentDescription = mode.title,
                                                tint = if (mode.isPremium) ProGold else theme.accent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = mode.title,
                                                style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                                                color = theme.textPrimary
                                            )
                                            if (mode.isPremium) {
                                                Icon(
                                                    imageVector = Icons.Default.WorkspacePremium,
                                                    contentDescription = "Pro Mode",
                                                    tint = ProGold,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (modeIdx > 0) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowUp,
                                                    contentDescription = "Move mode up",
                                                    tint = theme.textSecondary,
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .dyneClickable {
                                                            val list = configurableModes.toMutableList()
                                                            val item = list.removeAt(modeIdx)
                                                            list.add(modeIdx - 1, item)
                                                            viewModel.setCustomToolModesForTool(
                                                                tool.id,
                                                                list.map { it.first.id to it.second }
                                                            )
                                                        }
                                                )
                                            }
                                            if (modeIdx < configurableModes.lastIndex) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Move mode down",
                                                    tint = theme.textSecondary,
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .dyneClickable {
                                                            val list = configurableModes.toMutableList()
                                                            val item = list.removeAt(modeIdx)
                                                            list.add(modeIdx + 1, item)
                                                            viewModel.setCustomToolModesForTool(
                                                                tool.id,
                                                                list.map { it.first.id to it.second }
                                                            )
                                                        }
                                                )
                                            }
                                            DyneSwitch(
                                                checked = isVisible,
                                                onCheckedChange = { checked ->
                                                    val updated = configurableModes.map { (m, vis) ->
                                                        m.id to (if (m.id == mode.id) checked else vis)
                                                    }
                                                    if (updated.any { it.second }) {
                                                        viewModel.setCustomToolModesForTool(tool.id, updated)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 9. COLLAPSED "NOT AVAILABLE ON THIS PHONE" SECTION
            InsetSettingsGroup(header = "HARDWARE CAPABILITIES") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .dyneClickable(
                                testTag = "not_available_on_phone_header",
                                onClick = { unavailableSectionExpanded = !unavailableSectionExpanded }
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Not available on this phone (${unsupportedTools.size})",
                                style = type.body.copy(fontWeight = FontWeight.Bold),
                                color = theme.textPrimary
                            )
                            Text(
                                text = if (unsupportedTools.isEmpty()) {
                                    "All registered tools are supported by this phone's sensors"
                                } else {
                                    "Tap to see hidden tools and missing hardware sensors"
                                },
                                style = type.caption,
                                color = theme.textSecondary
                            )
                        }
                        Icon(
                            imageVector = if (unavailableSectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Not available on this phone",
                            tint = theme.accent
                        )
                    }

                    if (unavailableSectionExpanded) {
                        InsetDivider()
                        val capSnap = remember(context) { com.example.data.DeviceCapabilities.initialize(context) }
                        if (!capSnap.hasHardwareStepSensor && capSnap.hasAccelerometer) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Pedometer • Hardware Step Sensor Fallback Active",
                                    style = type.body.copy(fontWeight = FontWeight.SemiBold),
                                    color = theme.textPrimary
                                )
                                Text(
                                    text = "Reason: Dedicated hardware step counter not detected on this device.",
                                    style = type.caption,
                                    color = theme.textSecondary
                                )
                                Text(
                                    text = "Fallback: Using 3-axis accelerometer approximate step detection.",
                                    style = type.caption,
                                    color = theme.accent
                                )
                            }
                            InsetDivider()
                        }
                        if (unsupportedTools.isEmpty()) {
                            Text(
                                text = "Every tool in Dyne is supported on your hardware (or active via sensor fallback).",
                                style = type.caption,
                                color = theme.textSecondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            unsupportedTools.forEachIndexed { idx, tool ->
                                if (idx > 0) InsetDivider()
                                val reason = com.example.data.DeviceCapabilities.getMissingReason(tool.requiredCapabilities, context)
                                    ?: "Required hardware sensor not present on this device"
                                val fallback = com.example.data.DeviceCapabilities.getFallbackReason(tool.requiredCapabilities, context)
                                    ?: "Fallback: Manual numeric/reference mode available"
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = tool.name,
                                        style = type.body.copy(fontWeight = FontWeight.SemiBold),
                                        color = theme.textPrimary
                                    )
                                    Text(
                                        text = "Reason: $reason",
                                        style = type.caption,
                                        color = StatusDanger
                                    )
                                    Text(
                                        text = fallback,
                                        style = type.caption,
                                        color = theme.accent
                                    )
                                }
                            }
                        }
                    }
                }
            }

                // Reset to Default Button
                GlassButton(
                    text = "Reset to Default",
                    icon = Icons.Default.Restore,
                    accentFill = theme.accent,
                    onClick = { viewModel.resetHomeCustomizationToDefault() },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "customize_home_reset_button"
                )

                Spacer(modifier = Modifier.height(118.dp))
            }
    }
}

/**
 * 5. UNITS & FORMATS SUB-SCREEN:
 * Metric/Imperial preset, Distance, Speed, Temperature, Pressure,
 * 24-Hour Time, Default Time Zone, Default Currency.
 */
@Composable
private fun UnitsAndFormatsSubScreen(
    viewModel: SettingsViewModel,
    hazeState: HazeState?,
    onBack: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()

    val timeZones = remember {
        listOf(
            "System Default",
            "UTC",
            "America/New_York (EST)",
            "America/Los_Angeles (PST)",
            "Europe/London (GMT)",
            "Europe/Paris (CET)",
            "Asia/Kolkata (IST)",
            "Asia/Tokyo (JST)",
            "Asia/Singapore (SGT)",
            "Australia/Sydney (AEST)"
        )
    }

    val currencies = remember {
        listOf("USD", "EUR", "GBP", "INR", "JPY", "AUD", "CAD", "CHF", "CNY", "SGD")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SubScreenTopBar(
            title = "Units & Formats",
            subtitle = "MEASUREMENT UNITS, 24H CLOCK & CURRENCY",
            hazeState = hazeState,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            InsetSettingsGroup(header = "MEASUREMENT UNITS & CLOCK") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Unit System Preset",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.textSecondary
                    )
                    DyneSegmentedControl(
                        segments = listOf("Metric (SI)", "Imperial (US)"),
                        selectedIndex = if (prefs.unitSystem == UnitSystem.METRIC) 0 else 1,
                        onSegmentSelected = { idx ->
                            viewModel.setUnitSystem(if (idx == 0) UnitSystem.METRIC else UnitSystem.IMPERIAL)
                        }
                    )
                }

                InsetDivider()

                InsetSettingsRow(
                    icon = Icons.Default.Straighten,
                    iconBg = Color(0xFF38BDF8),
                    title = "Distance",
                    query = "",
                    valueText = prefs.distanceUnit,
                    onClick = {
                        val next = if (prefs.distanceUnit == "km / m") "mi / ft" else "km / m"
                        viewModel.setDistanceUnit(next)
                    }
                )

                InsetDivider()

                InsetSettingsRow(
                    icon = Icons.Default.Speed,
                    iconBg = Color(0xFF34D399),
                    title = "Speed",
                    query = "",
                    valueText = prefs.speedUnit,
                    onClick = {
                        val options = listOf("km/h", "mph", "knots", "m/s")
                        val nextIdx = (options.indexOf(prefs.speedUnit) + 1) % options.size
                        viewModel.setSpeedUnit(options[nextIdx])
                    }
                )

                InsetDivider()

                InsetSettingsRow(
                    icon = Icons.Default.Thermostat,
                    iconBg = Color(0xFFFB7185),
                    title = "Temperature",
                    query = "",
                    valueText = prefs.temperatureUnit,
                    onClick = {
                        val options = listOf("°C", "°F", "K")
                        val nextIdx = (options.indexOf(prefs.temperatureUnit) + 1) % options.size
                        viewModel.setTemperatureUnit(options[nextIdx])
                    }
                )

                InsetDivider()

                InsetSettingsRow(
                    icon = Icons.Default.Compress,
                    iconBg = Color(0xFFA78BFA),
                    title = "Pressure",
                    query = "",
                    valueText = prefs.pressureUnit,
                    onClick = {
                        val options = listOf("hPa", "inHg", "mmHg", "psi")
                        val nextIdx = (options.indexOf(prefs.pressureUnit) + 1) % options.size
                        viewModel.setPressureUnit(options[nextIdx])
                    }
                )

                InsetDivider()

                InsetSettingsSwitchRow(
                    icon = Icons.Default.AccessTime,
                    iconBg = theme.accent,
                    title = "24-Hour Time",
                    subtitle = "Use 24h clock across World Clock, Solunar & Weather",
                    query = "",
                    checked = prefs.use24HourTime,
                    onCheckedChange = viewModel::setUse24HourTime
                )
            }

            InsetSettingsGroup(header = "DEFAULT CURRENCY") {
                currencies.forEachIndexed { idx, code ->
                    if (idx > 0) InsetDivider()
                    InsetSettingsRow(
                        icon = Icons.Default.CurrencyExchange,
                        iconBg = Color(0xFF4ADE80),
                        title = code,
                        query = "",
                        valueText = if (prefs.defaultCurrency == code) "Selected" else "",
                        showChevron = false,
                        onClick = { viewModel.setDefaultCurrency(code) }
                    )
                }
            }

            InsetSettingsGroup(header = "DEFAULT TIME ZONE") {
                timeZones.forEachIndexed { idx, tz ->
                    if (idx > 0) InsetDivider()
                    InsetSettingsRow(
                        icon = Icons.Default.Public,
                        iconBg = Color(0xFF38BDF8),
                        title = tz,
                        query = "",
                        valueText = if (prefs.defaultTimeZone == tz) "Selected" else "",
                        showChevron = false,
                        onClick = { viewModel.setDefaultTimeZone(tz) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(118.dp))
        }
    }
}

/**
 * Hidden "Developer options" screen in Settings (DEBUG builds only),
 * opened by tapping the Version card 7 times.
 */
@Composable
private fun DeveloperOptionsSubScreen(
    viewModel: SettingsViewModel,
    hazeState: HazeState?,
    onOpenFrameStats: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adManager = LocalAdManager.current
    val featureGate = LocalFeatureGate.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val proSource by viewModel.proSource.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val statuses = adManager?.formatStatuses?.collectAsStateWithLifecycle()?.value ?: emptyMap()
    val umpStatus = adManager?.umpConsentStatusText?.collectAsStateWithLifecycle()?.value ?: "Unknown"
    val logs = adManager?.adEventLogs?.collectAsStateWithLifecycle()?.value ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("developer_options_screen")
    ) {
        SubScreenTopBar(
            title = "Developer options",
            subtitle = "Diagnostics & Frame Stats",
            hazeState = hazeState,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            InsetSettingsGroup(header = "PERFORMANCE & JANKSTATS") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .dyneClickable(
                            testTag = "dev_open_frame_stats_button",
                            onClick = onOpenFrameStats
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Frame stats",
                            style = type.body.copy(fontWeight = FontWeight.Bold),
                            color = theme.textPrimary
                        )
                        Text(
                            text = "Jank percentage and frame timing per screen",
                            style = type.caption,
                            color = theme.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open Frame stats",
                        tint = theme.accent
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(theme.surface)
                    .border(1.dp, theme.accent.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Official Google Test IDs Active",
                        style = type.body.copy(fontWeight = FontWeight.Bold),
                        color = theme.textPrimary
                    )
                    Text(
                        text = "App ID: ${AdConfig.APP_ID}\nUMP Consent: $umpStatus",
                        style = type.caption,
                        color = theme.textSecondary
                    )
                }
            }

            if (BuildConfig.DEBUG) {
                val active1hUnlocks = remember(
                    prefs.featureUnlocksCsv,
                    prefs.rewardedToolUnlocksCsv,
                    prefs.rewardedThemeUnlocksCsv
                ) {
                    featureGate?.getActive1hUnlockDescriptions(System.currentTimeMillis()) ?: emptyList()
                }
                InsetSettingsGroup(header = "PRO DEBUG") {
                    InsetSettingsSwitchRow(
                        icon = Icons.Default.WorkspacePremium,
                        iconBg = ProGold,
                        title = "Simulate Pro",
                        subtitle = "DEBUG-only switch — toggles isPro without Play Billing",
                        query = "",
                        checked = isPro,
                        testTag = "dev_simulate_pro_switch",
                        onCheckedChange = { enabled ->
                            viewModel.setSimulatePro(enabled)
                        }
                    )

                    InsetDivider()

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("pro_debug_card")
                    ) {
                        Text(
                            text = "isPro: $isPro",
                            style = type.body.copy(fontWeight = FontWeight.Bold),
                            color = if (isPro) ProGold else theme.textPrimary
                        )
                        Text(
                            text = "Source: $proSource",
                            style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = theme.accent
                        )
                        Text(
                            text = "Coin Balance: ${prefs.coinBalance} coins • Bonus Doc Saves: +${prefs.bonusDocumentSaves}",
                            style = type.caption,
                            color = theme.textSecondary
                        )

                        Text(
                            text = "Active 1h Temporary Unlocks (${active1hUnlocks.size}):",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.textPrimary
                        )
                        if (active1hUnlocks.isEmpty()) {
                            Text(
                                text = "None active",
                                style = type.caption,
                                color = theme.textSecondary
                            )
                        } else {
                            active1hUnlocks.forEach { desc ->
                                Text(
                                    text = "• $desc",
                                    style = type.caption,
                                    color = ProGold
                                )
                            }
                        }

                        GlassButton(
                            text = "Clear",
                            icon = Icons.Default.DeleteForever,
                            accentFill = StatusDanger,
                            textColor = Color.White,
                            onClick = {
                                viewModel.clearProDebugStateAndUnlocks()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "pro_debug_clear_button"
                        )
                    }
                }
            }

            InsetSettingsGroup(header = "AD FORMAT STATUS (5 FORMATS)") {
                AdFormatType.entries.forEachIndexed { index, format ->
                    if (index > 0) InsetDivider()
                    val status = statuses[format]
                    val badgeColor = when (status?.stage) {
                        AdLoadStage.LOADED -> Color(0xFF34D399)
                        AdLoadStage.LOADING -> theme.accent
                        AdLoadStage.FAILED -> StatusDanger
                        AdLoadStage.DISABLED_PRO -> ProGold
                        else -> theme.textSecondary
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = format.label,
                                style = type.body.copy(fontWeight = FontWeight.Bold),
                                color = theme.textPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(badgeColor.copy(alpha = 0.2f))
                                    .border(1.dp, badgeColor, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = status?.summaryLabel ?: "Idle",
                                    style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                                    color = badgeColor
                                )
                            }
                        }
                        Text(
                            text = "Unit: ${status?.adUnitId ?: ""}",
                            style = type.caption,
                            color = theme.textSecondary
                        )
                        if (!status?.errorMessage.isNullOrBlank()) {
                            Text(
                                text = "Note: ${status?.errorMessage}",
                                style = type.caption,
                                color = theme.accent
                            )
                        }
                    }
                }
            }

            InsetSettingsGroup(header = "TEST AD TRIGGERS & CONTROLS") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    GlassButton(
                        text = "Show Interstitial Now",
                        icon = Icons.Default.PlayArrow,
                        accentFill = theme.accent,
                        onClick = {
                            adManager?.showInterstitialNowForDebug(activity)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "dev_show_interstitial_button"
                    )

                    GlassButton(
                        text = "Show Rewarded Ad Now",
                        icon = Icons.Default.PlayArrow,
                        accentFill = theme.bentoSky,
                        onClick = {
                            adManager?.showRewardedAd(
                                activity = activity,
                                rewardDescription = "Developer Options Test Reward",
                                onAdUnavailable = { msg ->
                                    viewModel.showStatusMessage(msg)
                                }
                            ) {
                                viewModel.showStatusMessage("Rewarded Ad completed (onUserEarnedReward)")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "dev_show_rewarded_button"
                    )

                    GlassButton(
                        text = "Show App Open Ad Now",
                        icon = Icons.Default.PlayArrow,
                        accentFill = theme.bentoLavender,
                        onClick = {
                            adManager?.showAppOpenNowForDebug(activity)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "dev_show_app_open_button"
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GlassButton(
                            text = "Reload All Ads",
                            icon = Icons.Default.Restore,
                            onClick = {
                                adManager?.reloadAllAds()
                                viewModel.showStatusMessage("Reloaded all 5 AdMob formats")
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "dev_reload_all_ads_button"
                        )

                        GlassButton(
                            text = "Reset Freq Caps",
                            icon = Icons.Default.Tune,
                            onClick = {
                                adManager?.resetFrequencyCaps()
                                viewModel.showStatusMessage("Reset Interstitial & App Open frequency caps")
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "dev_reset_caps_button"
                        )
                    }

                    GlassButton(
                        text = "Reset UMP Consent",
                        icon = Icons.Default.PrivacyTip,
                        onClick = {
                            if (activity != null) {
                                adManager?.resetUmpConsent(activity)
                                viewModel.showStatusMessage("UMP consent reset & re-requested")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "dev_reset_ump_button"
                    )
                }
            }

            InsetSettingsGroup(header = "FREQUENCY CAPS & REWARDED UNLOCKS") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Interstitial count today (${prefs.interstitialDailyDate.ifEmpty { "today" }}): ${prefs.interstitialDailyCount} / 6",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.textPrimary
                    )
                    Text(
                        text = "Bonus Document Saves: +${prefs.bonusDocumentSaves} (Free limit: ${5 + prefs.bonusDocumentSaves})",
                        style = type.caption,
                        color = theme.textSecondary
                    )
                    Text(
                        text = "1h Feature Unlocks: ${prefs.featureUnlocksCsv.ifEmpty { "None" }}",
                        style = type.caption,
                        color = theme.textSecondary
                    )
                }
            }

            InsetSettingsGroup(header = "AD EVENT LOG (${logs.size} EVENTS)") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (logs.isEmpty()) {
                        Text(
                            text = "No ad events recorded yet.",
                            style = type.caption,
                            color = theme.textSecondary
                        )
                    } else {
                        logs.take(30).forEach { logLine ->
                            Text(
                                text = logLine,
                                style = type.caption,
                                color = theme.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(118.dp))
        }
    }
}

@Composable
private fun FrameStatsSubScreen(
    hazeState: HazeState?,
    onBack: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val statsByScreen by com.example.util.FrameStatsMonitor.statsByScreen.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("frame_stats_screen")
    ) {
        SubScreenTopBar(
            title = "Frame stats",
            subtitle = "Jank percentage per screen",
            hazeState = hazeState,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InsetSettingsGroup(header = "JANK PERCENTAGE PER SCREEN") {
                val entries = statsByScreen.values.toList()
                entries.forEachIndexed { idx, stat ->
                    if (idx > 0) InsetDivider()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("frame_stats_row_${stat.screenName.lowercase().replace(" ", "_")}")
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stat.screenName,
                                style = type.body.copy(fontWeight = FontWeight.Bold),
                                color = theme.textPrimary
                            )
                            Text(
                                text = "${stat.totalFrames} frames • ${stat.jankyFrames} janky • avg ${stat.formattedAvgFrameMs}",
                                style = type.caption,
                                color = theme.textSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (stat.jankPercent <= 2.0) theme.accent.copy(alpha = 0.16f)
                                    else StatusDanger.copy(alpha = 0.18f)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = stat.formattedJankPercent,
                                style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (stat.jankPercent <= 2.0) theme.accent else StatusDanger
                            )
                        }
                    }
                }
            }

            GlassButton(
                text = "Reset",
                icon = Icons.Default.Restore,
                accentFill = theme.accent,
                onClick = { com.example.util.FrameStatsMonitor.reset() },
                modifier = Modifier.fillMaxWidth(),
                testTag = "frame_stats_reset_button"
            )

            Spacer(modifier = Modifier.height(118.dp))
        }
    }
}

