package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ads.AdGate
import com.example.ads.AdManager
import com.example.ads.FeatureUnlockGlassSheet
import com.example.ads.LocalAdGate
import com.example.ads.LocalAdManager
import com.example.ads.RewardUnlockPromptTarget
import com.example.ads.RewardedUnlockGlassSheet
import com.example.billing.FeatureGate
import com.example.billing.GooglePlayPurchaseManager
import com.example.billing.LocalFeatureGate
import com.example.billing.PremiumFeature
import com.example.billing.PurchaseManager
import com.example.data.GeoCoordinates
import com.example.data.LocationProvider
import com.example.data.OpenMeteoWeatherRepository
import com.example.data.PreferencesRepository
import com.example.data.ScanDatabase
import com.example.data.ScanRepository
import com.example.data.SensorRepository
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.example.data.ThemeMode
import com.example.data.UserPreferences
import com.example.data.WeatherRepository
import com.example.domain.RegisteredTool
import com.example.domain.ToolRegistry
import com.example.ui.components.FeedbackHelper
import com.example.ui.components.GlassTabBar
import com.example.ui.components.GlassTabItem
import com.example.ui.components.HazeState
import com.example.ui.notes.FullScreenDrawingPadScreen
import com.example.ui.notes.FullScreenNoteEditorScreen
import com.example.ui.components.LocalBottomBarInset
import com.example.ui.components.LocalTapSound
import com.example.ui.components.ScanPlusMenuOverlay
import com.example.ui.components.TapSound
import com.example.ui.components.haze
import com.example.ui.home.HomeScreen
import com.example.ui.home.QuickAccessTarget
import com.example.ui.home.ToolDetailScreen
import com.example.ui.library.LibraryScreen
import com.example.ui.outdoors.OutdoorsScreen
import com.example.ui.paywall.PaywallSheet
import com.example.ui.paywall.PaywallViewModel
import com.example.ui.rewards.RewardsScreen
import com.example.ui.scan.DocumentScannerScreen
import com.example.ui.scan.QrBarcodeScannerScreen
import com.example.ui.scan.ScanViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.solunar.SolunarViewModel
import com.example.ui.sunmoon.SunMoonViewModel
import com.example.ui.theme.DyneAppTheme
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.tools.ToolsViewModel
import kotlinx.coroutines.launch

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

private enum class ActiveScannerOverlay {
    NONE,
    QR_BARCODE,
    DOCUMENT
}

class MainActivity : FragmentActivity() {

    companion object {
        // Ensures the Compose launch splash only plays once per process cold start, never on resume
        @Volatile
        private var hasShownColdStartSplash: Boolean = false
    }

    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var sensorRepository: SensorRepository
    private lateinit var locationProvider: LocationProvider
    private lateinit var scanRepository: ScanRepository
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var purchaseManager: PurchaseManager
    private lateinit var featureGate: FeatureGate
    private lateinit var adGate: AdGate
    private lateinit var adManager: AdManager
    private lateinit var tapSound: TapSound

    @Volatile
    private var isFirstComposeFrameDrawn: Boolean = false

    private val incomingIntentState = kotlinx.coroutines.flow.MutableStateFlow<android.content.Intent?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingIntentState.value = intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        incomingIntentState.value = intent
        splashScreen.setKeepOnScreenCondition { !isFirstComposeFrameDrawn }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val shouldShowColdSplash = savedInstanceState == null && !hasShownColdStartSplash
        if (shouldShowColdStartSplashFlag(shouldShowColdSplash)) {
            hasShownColdStartSplash = true
        }

        com.example.data.DeviceCapabilities.initialize(applicationContext)
        preferencesRepository = PreferencesRepository(applicationContext)
        sensorRepository = SensorRepository(applicationContext)
        locationProvider = LocationProvider(applicationContext)
        val database = ScanDatabase.getInstance(applicationContext)
        scanRepository = ScanRepository(database.scanDao())
        weatherRepository = OpenMeteoWeatherRepository(applicationContext)
        purchaseManager = GooglePlayPurchaseManager(applicationContext, preferencesRepository)
        featureGate = FeatureGate(purchaseManager, preferencesRepository, scanRepository)
        adGate = AdGate(purchaseManager)
        adManager = AdManager(applicationContext, adGate, preferencesRepository)
        tapSound = TapSound(applicationContext)

        // Request UMP consent on first launch and initialize Mobile Ads SDK on a background thread
        adManager.requestUmpConsentOnLaunch(this)

        setContent {
            val prefs by preferencesRepository.preferencesFlow.collectAsStateWithLifecycle(
                initialValue = UserPreferences()
            )
            val isPro by purchaseManager.isPro.collectAsStateWithLifecycle()
            val resumeTick by resumeTickState.collectAsStateWithLifecycle()
            var nowTickMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
            var hadActiveLookUnlock by remember { mutableStateOf(false) }
            var showUnlockEndedSnackbar by remember { mutableStateOf(false) }

            // Re-evaluate immediately on launch, onStart, onResume, or preference change
            LaunchedEffect(resumeTick, prefs, isPro) {
                val now = System.currentTimeMillis()
                nowTickMs = now
                val lookUnlockExpired = preferencesRepository.verifyClockAndEvaluateExpiries(
                    isPro = isPro,
                    nowWallMs = now
                )
                if (lookUnlockExpired) {
                    showUnlockEndedSnackbar = true
                }
            }

            // Ticker that wakes at the exact moment the nearest temporary unlock expires
            LaunchedEffect(prefs, isPro, nowTickMs) {
                if (!isPro) {
                    val nearestExpiry = prefs.nearestTemporaryUnlockExpiryMs(nowTickMs)
                    if (nearestExpiry != null) {
                        val waitMs = (nearestExpiry - System.currentTimeMillis() + 60L).coerceAtLeast(60L)
                        kotlinx.coroutines.delay(waitMs)
                        val wakeNow = System.currentTimeMillis()
                        nowTickMs = wakeNow
                        val lookUnlockExpired = preferencesRepository.verifyClockAndEvaluateExpiries(
                            isPro = false,
                            nowWallMs = wakeNow
                        )
                        if (lookUnlockExpired) {
                            showUnlockEndedSnackbar = true
                        }
                    }
                }
            }

            val isCustomLookTemporarilyActive = !isPro && (
                prefs.isCustomizationAccessActive(false, nowTickMs) ||
                    !prefs.effectiveThemeName(false, nowTickMs).equals("Lime Night", ignoreCase = true)
                )

            LaunchedEffect(isCustomLookTemporarilyActive, isPro) {
                if (isPro) {
                    hadActiveLookUnlock = false
                    showUnlockEndedSnackbar = false
                } else if (isCustomLookTemporarilyActive) {
                    hadActiveLookUnlock = true
                } else if (hadActiveLookUnlock) {
                    hadActiveLookUnlock = false
                    showUnlockEndedSnackbar = true
                }
            }

            LaunchedEffect(showUnlockEndedSnackbar) {
                if (showUnlockEndedSnackbar) {
                    kotlinx.coroutines.delay(4500L)
                    showUnlockEndedSnackbar = false
                }
            }

            val effectiveThemeName = prefs.effectiveThemeName(isPro, nowTickMs)
            val effectiveAccentHex = prefs.effectiveCustomAccentHex(isPro, nowTickMs)

            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (prefs.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            SideEffect {
                tapSound.tapSoundsEnabled = prefs.tapSoundsEnabled
                tapSound.tapVolume = prefs.tapSoundVolume
                tapSound.scanBeepEnabled = prefs.scanBeepEnabled
                tapSound.hapticsEnabled = prefs.hapticsEnabled
                tapSound.hapticStrength = prefs.hapticStrength
                FeedbackHelper.hapticsEnabledGlobal = prefs.hapticsEnabled
                FeedbackHelper.hapticStrengthGlobal = prefs.hapticStrength
                FeedbackHelper.scanBeepEnabledGlobal = prefs.scanBeepEnabled
                if (prefs.hideAppPreviewInRecents) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            LaunchedEffect(Unit) {
                withFrameNanos {
                    isFirstComposeFrameDrawn = true
                }
            }

            CompositionLocalProvider(
                LocalTapSound provides tapSound,
                LocalAdGate provides adGate,
                LocalAdManager provides adManager,
                LocalFeatureGate provides featureGate
            ) {
                DyneAppTheme(
                    themeName = effectiveThemeName,
                    darkTheme = useDarkTheme,
                    textScale = prefs.textScale,
                    reduceGlassEffects = prefs.reduceGlassEffects,
                    reduceMotion = prefs.reduceMotion,
                    customAccentHex = effectiveAccentHex
                ) {
                    val currentIncomingIntent by incomingIntentState.collectAsStateWithLifecycle()
                    DyneRootApp(
                        preferencesRepository = preferencesRepository,
                        sensorRepository = sensorRepository,
                        locationProvider = locationProvider,
                        scanRepository = scanRepository,
                        weatherRepository = weatherRepository,
                        purchaseManager = purchaseManager,
                        featureGate = featureGate,
                        adGate = adGate,
                        adManager = adManager,
                        tapSound = tapSound,
                        userPreferences = prefs,
                        initialShowSplash = shouldShowColdSplash,
                        incomingIntent = currentIncomingIntent,
                        onConsumeIncomingIntent = { incomingIntentState.value = null },
                        onFirstSplashFrame = { isFirstComposeFrameDrawn = true },
                        showUnlockEndedSnackbar = showUnlockEndedSnackbar,
                        onDismissUnlockEndedSnackbar = { showUnlockEndedSnackbar = false }
                    )
                }
            }
        }
    }

    private val resumeTickState = kotlinx.coroutines.flow.MutableStateFlow(0L)

    private fun shouldShowColdStartSplashFlag(flag: Boolean): Boolean = flag

    override fun onStart() {
        super.onStart()
        resumeTickState.value = System.currentTimeMillis()
    }

    override fun onStop() {
        super.onStop()
        if (::adManager.isInitialized) {
            adManager.onAppBackgrounded()
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTickState.value = System.currentTimeMillis()
        if (::purchaseManager.isInitialized) {
            purchaseManager.refreshProductsAndPurchases()
        }
        if (::adManager.isInitialized) {
            adManager.onAppForegrounded(this)
        }
    }

    override fun onDestroy() {
        if (::tapSound.isInitialized) {
            tapSound.release()
        }
        super.onDestroy()
    }
}

@Composable
fun DyneRootApp(
    preferencesRepository: PreferencesRepository,
    sensorRepository: SensorRepository,
    locationProvider: LocationProvider,
    scanRepository: ScanRepository,
    weatherRepository: WeatherRepository,
    purchaseManager: PurchaseManager,
    featureGate: FeatureGate? = null,
    adGate: AdGate? = null,
    adManager: AdManager? = null,
    tapSound: TapSound? = null,
    userPreferences: UserPreferences,
    initialShowSplash: Boolean = false,
    incomingIntent: android.content.Intent? = null,
    onConsumeIncomingIntent: () -> Unit = {},
    onFirstSplashFrame: () -> Unit = {},
    showUnlockEndedSnackbar: Boolean = false,
    onDismissUnlockEndedSnackbar: () -> Unit = {}
) {
    val theme = LocalDyneTheme.current
    val context = LocalContext.current
    val activity = remember(context) { context.findHostActivity() }
    val scope = rememberCoroutineScope()
    val isPro by purchaseManager.isPro.collectAsStateWithLifecycle()
    val resolvedAdManager = adManager ?: LocalAdManager.current
    val resolvedFeatureGate = remember(featureGate, purchaseManager, preferencesRepository, scanRepository) {
        featureGate ?: FeatureGate(purchaseManager, preferencesRepository, scanRepository)
    }
    val hazeState = remember { HazeState() }

    var showSplash by rememberSaveable { mutableStateOf(initialShowSplash) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var outdoorsSegment by rememberSaveable { mutableIntStateOf(0) }
    var librarySegment by rememberSaveable { mutableIntStateOf(0) }
    var activeFeatureId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeToolId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeToolInitialModeId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeToolEnteredAtMs by rememberSaveable { mutableLongStateOf(0L) }
    var activeScannerOverlay by rememberSaveable { mutableStateOf(ActiveScannerOverlay.NONE) }

    // Per-tab SaveableStateHolder + scroll-to-top triggers + per-tab back stack
    val tabStateHolder = rememberSaveableStateHolder()
    var homeScrollToTopTrigger by rememberSaveable { mutableIntStateOf(0) }
    var outdoorsScrollToTopTrigger by rememberSaveable { mutableIntStateOf(0) }
    var libraryScrollToTopTrigger by rememberSaveable { mutableIntStateOf(0) }
    var settingsScrollToTopTrigger by rememberSaveable { mutableIntStateOf(0) }

    var showScanMenu by rememberSaveable { mutableStateOf(false) }
    var showPaywallSheet by rememberSaveable { mutableStateOf(false) }
    var showRewardsScreen by rememberSaveable { mutableStateOf(false) }
    var showAllFeaturesScreen by rememberSaveable { mutableStateOf(false) }
    var showCustomizeHomeScreen by rememberSaveable { mutableStateOf(false) }
    var isTabBarVisible by rememberSaveable { mutableStateOf(true) }
    var pendingSettingsSubScreen by rememberSaveable {
        mutableStateOf(com.example.ui.settings.SettingsSubScreen.ROOT)
    }

    // Record tool session start timestamp whenever a feature/tool is opened
    LaunchedEffect(activeFeatureId, activeToolId) {
        val recordId = activeFeatureId ?: activeToolId
        if (recordId != null) {
            activeToolEnteredAtMs = System.currentTimeMillis()
            preferencesRepository.recordRecentTool(recordId)
        }
    }

    // Mute tap sound while camera scanner is open or inside Sound Meter / Mic tools so audio is never polluted
    val currentTapSound = tapSound ?: LocalTapSound.current
    SideEffect {
        currentTapSound?.isCameraScannerOpen = (activeScannerOverlay != ActiveScannerOverlay.NONE)
        currentTapSound?.isSoundMeterActive = (activeToolId in setOf("sound_db", "mic_test", "tuner", "frequency_tool", "speaker_cleaner"))
    }

    // Hides the floating glass tab bar on scroll down and returns it on scroll up,
    // snapping cleanly fully hidden or fully shown at scroll end (never staying half-way).
    val scrollConnection = remember {
        object : NestedScrollConnection {
            private var accumulatedDy = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (showScanMenu) {
                    isTabBarVisible = true
                    accumulatedDy = 0f
                    return Offset.Zero
                }
                if ((accumulatedDy > 0f && available.y < 0f) || (accumulatedDy < 0f && available.y > 0f)) {
                    accumulatedDy = 0f
                }
                accumulatedDy += available.y
                if (accumulatedDy < -24f) {
                    isTabBarVisible = false
                    accumulatedDy = 0f
                } else if (accumulatedDy > 18f) {
                    isTabBarVisible = true
                    accumulatedDy = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: androidx.compose.ui.unit.Velocity,
                available: androidx.compose.ui.unit.Velocity
            ): androidx.compose.ui.unit.Velocity {
                if (showScanMenu) {
                    isTabBarVisible = true
                } else if (consumed.y < -180f) {
                    isTabBarVisible = false
                } else if (consumed.y > 140f) {
                    isTabBarVisible = true
                }
                accumulatedDy = 0f
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }

    val toolsViewModel: ToolsViewModel = viewModel(
        factory = ToolsViewModel.Factory(sensorRepository)
    )
    val sensorState by toolsViewModel.sensorState.collectAsStateWithLifecycle()

    val solunarViewModel: SolunarViewModel = viewModel(
        factory = SolunarViewModel.Factory(locationProvider, preferencesRepository, weatherRepository)
    )
    val solunarState by solunarViewModel.uiState.collectAsStateWithLifecycle()

    val sunMoonViewModel: SunMoonViewModel = viewModel(
        factory = SunMoonViewModel.Factory(sensorRepository, locationProvider, preferencesRepository)
    )
    val sunMoonState by sunMoonViewModel.uiState.collectAsStateWithLifecycle()
    val ownedSoundPacks by purchaseManager.ownedSoundPacks.collectAsStateWithLifecycle()
    val soundPackPrices by purchaseManager.soundPackPrices.collectAsStateWithLifecycle()
    val scanViewModel: ScanViewModel = viewModel(
        factory = ScanViewModel.Factory(scanRepository)
    )
    val allDocuments by scanViewModel.allDocuments.collectAsStateWithLifecycle()
    val activeViewerDoc by scanViewModel.activeViewerDocument.collectAsStateWithLifecycle()
    val isDrawingPadOpen by scanViewModel.isDrawingPadOpen.collectAsStateWithLifecycle()
    val activeDrawingPadNote by scanViewModel.activeDrawingPadNote.collectAsStateWithLifecycle()
    val isFullScreenNoteEditorOpen by scanViewModel.isFullScreenNoteEditorOpen.collectAsStateWithLifecycle()
    val activeEditorNote by scanViewModel.activeEditorNote.collectAsStateWithLifecycle()
    val paywallViewModel: PaywallViewModel = viewModel(
        factory = PaywallViewModel.Factory(purchaseManager)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(preferencesRepository, purchaseManager, scanRepository)
    )

    val currentTrackedScreenName = when {
        showAllFeaturesScreen -> "All Features"
        activeFeatureId != null || activeToolId != null -> "Tool Screen"
        selectedTabIndex == 1 -> "Outdoors"
        selectedTabIndex == 2 -> "Library"
        selectedTabIndex == 3 -> "Settings"
        else -> "Home"
    }
    com.example.util.AttachFrameStatsObserver(currentScreenName = currentTrackedScreenName)

    fun openToolOrDestination(toolId: String, modeId: String? = null) {
        showAllFeaturesScreen = false
        showScanMenu = false
        when (toolId) {
            "outdoor_planner", "outdoor_alerts", "open_outdoors_solunar" -> {
                activeFeatureId = null
                activeToolId = null
                activeToolInitialModeId = null
                outdoorsSegment = 0
                selectedTabIndex = 1
                isTabBarVisible = true
            }
            "sun_moon_calendar", "open_outdoors_sun_moon" -> {
                activeFeatureId = null
                activeToolId = null
                activeToolInitialModeId = null
                outdoorsSegment = 1
                selectedTabIndex = 1
                isTabBarVisible = true
            }
            "open_outdoors_weather" -> {
                activeFeatureId = null
                activeToolId = null
                activeToolInitialModeId = null
                outdoorsSegment = 2
                selectedTabIndex = 1
                isTabBarVisible = true
            }
            "open_library_notes" -> {
                activeFeatureId = null
                activeToolId = null
                activeToolInitialModeId = null
                librarySegment = 1
                selectedTabIndex = 2
                isTabBarVisible = true
            }
            "home_widgets" -> {
                activeFeatureId = null
                activeToolId = null
                activeToolInitialModeId = null
                pendingSettingsSubScreen = com.example.ui.settings.SettingsSubScreen.CUSTOMIZE_HOME
                selectedTabIndex = 3
                isTabBarVisible = true
            }
            "custom_style_studio" -> {
                activeFeatureId = null
                activeToolId = null
                activeToolInitialModeId = null
                pendingSettingsSubScreen = com.example.ui.settings.SettingsSubScreen.THEMES
                selectedTabIndex = 3
                isTabBarVisible = true
            }
            else -> {
                val resolvedFeature = com.example.domain.FeatureRegistry.resolveFeatureForToolAndMode(toolId, modeId)
                    ?: com.example.domain.FeatureRegistry.resolveFeatureOrPrimaryForTool(toolId)
                activeFeatureId = resolvedFeature?.id
                activeToolInitialModeId = modeId ?: resolvedFeature?.let { com.example.domain.FeatureRegistry.resolveModeIdForFeature(it) }
                activeToolId = ToolRegistry.getToolById(resolvedFeature?.toolId ?: toolId)?.id ?: toolId
            }
        }
    }

    fun openFeatureDestination(feature: com.example.domain.Feature) {
        showAllFeaturesScreen = false
        showScanMenu = false
        scope.launch {
            preferencesRepository.recordRecentTool(feature.id)
        }
        when (feature.toolId) {
            "open_qr_scanner" -> {
                activeScannerOverlay = ActiveScannerOverlay.QR_BARCODE
            }
            "open_doc_scanner" -> {
                if (feature.id == "scanner_document" &&
                    !isPro &&
                    !resolvedFeatureGate.isUnlocked(PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE) &&
                    allDocuments.size >= 5 + userPreferences.bonusDocumentSaves
                ) {
                    resolvedFeatureGate.require(PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE) {
                        activeScannerOverlay = ActiveScannerOverlay.DOCUMENT
                    }
                } else {
                    activeScannerOverlay = ActiveScannerOverlay.DOCUMENT
                }
            }
            "open_outdoors_solunar", "outdoor_planner", "outdoor_alerts" -> {
                activeFeatureId = null
                activeToolId = null
                activeToolInitialModeId = null
                outdoorsSegment = 0
                selectedTabIndex = 1
                isTabBarVisible = true
            }
            "open_outdoors_sun_moon", "sun_moon_calendar" -> {
                activeFeatureId = null
                activeToolId = null
                activeToolInitialModeId = null
                outdoorsSegment = 1
                selectedTabIndex = 1
                isTabBarVisible = true
            }
            "open_outdoors_weather" -> {
                activeFeatureId = null
                activeToolId = null
                activeToolInitialModeId = null
                outdoorsSegment = 2
                selectedTabIndex = 1
                isTabBarVisible = true
            }
            "open_library_notes" -> {
                activeFeatureId = null
                activeToolId = null
                activeToolInitialModeId = null
                librarySegment = 1
                selectedTabIndex = 2
                isTabBarVisible = true
            }
            "home_widgets", "custom_style_studio" -> {
                openToolOrDestination(feature.toolId, feature.modeId)
            }
            else -> {
                activeFeatureId = feature.id
                activeToolInitialModeId = com.example.domain.FeatureRegistry.resolveModeIdForFeature(feature)
                activeToolId = ToolRegistry.getToolById(feature.toolId)?.id ?: feature.toolId
            }
        }
    }

    /**
     * Route-level FeatureGuard: every navigation to `"feature/{id}"` (from Home tiles, hero slides,
     * Favorites, Recently used, Quick Access, search results, All Features, Scan menu, Daily Spotlight,
     * pinned and dynamic launcher shortcuts, Quick Settings tiles, Glance widgets, notification deep links,
     * and cold-start or onNewIntent intents) calls `FeatureGuard.navigate(featureKey)`:
     * - Allowed -> opens the feature screen.
     * - Locked -> opens the Unlock sheet and does NOT navigate.
     */
    fun navigateToFeature(featureIdOrToolId: String, modeId: String? = null) {
        val cleanedId = featureIdOrToolId.removePrefix("feature/").trim()
        val feature = com.example.domain.FeatureRegistry.getFeatureById(cleanedId)
            ?: com.example.domain.FeatureRegistry.resolveFeatureForToolAndMode(cleanedId, modeId)
            ?: com.example.domain.FeatureRegistry.resolveFeatureOrPrimaryForTool(cleanedId)
        val targetKey = feature?.id ?: cleanedId
        resolvedFeatureGate.guardNavigate(
            featureKey = targetKey,
            prefs = userPreferences,
            onAllowed = {
                if (feature != null) {
                    openFeatureDestination(feature)
                } else {
                    openToolOrDestination(cleanedId, modeId)
                }
            }
        )
    }

    fun openCatalogFeature(feature: com.example.domain.Feature) {
        navigateToFeature(feature.id)
    }

    // SMART UPSELL: show the Paywall only after a success moment (after an export, the 5th scan,
    // a finished tool session), at most once every 3 days, dismissible with "Maybe later" (7-day pause).
    // Never during a task, never on launch, never more than one prompt per session.
    var hasRecordedSessionLaunch by rememberSaveable { mutableStateOf(false) }
    var smartUpsellShownThisSession by rememberSaveable { mutableStateOf(false) }

    fun triggerSmartUpsellAfterSuccess() {
        if (!isPro &&
            !showPaywallSheet &&
            userPreferences.canShowSmartUpsell(
                nowMs = System.currentTimeMillis(),
                shownInCurrentSession = smartUpsellShownThisSession
            )
        ) {
            smartUpsellShownThisSession = true
            scope.launch {
                preferencesRepository.recordSmartUpsellShown()
            }
            showPaywallSheet = true
        }
    }

    fun handleIncomingIntent(intentToHandle: android.content.Intent?): Boolean {
        if (intentToHandle == null) return false
        var handled = false

        val notifDeepLink = intentToHandle.getStringExtra(com.example.service.DyneNotificationCenter.EXTRA_NOTIFICATION_DEEP_LINK)
        if (!notifDeepLink.isNullOrBlank()) {
            intentToHandle.removeExtra(com.example.service.DyneNotificationCenter.EXTRA_NOTIFICATION_DEEP_LINK)
            showSplash = false
            handled = true
            when (notifDeepLink) {
                "rewards" -> {
                    activeFeatureId = null
                    activeToolId = null
                    showRewardsScreen = true
                }
                "settings_backup" -> {
                    activeFeatureId = null
                    activeToolId = null
                    pendingSettingsSubScreen = com.example.ui.settings.SettingsSubScreen.DATA_AND_BACKUP
                    selectedTabIndex = 3
                    isTabBarVisible = true
                }
                "settings_notifications" -> {
                    activeFeatureId = null
                    activeToolId = null
                    pendingSettingsSubScreen = com.example.ui.settings.SettingsSubScreen.NOTIFICATIONS
                    selectedTabIndex = 3
                    isTabBarVisible = true
                }
                else -> {
                    navigateToFeature(notifDeepLink)
                }
            }
        }

        val widgetTarget = intentToHandle.getStringExtra(com.example.widget.DyneWidgetHelper.EXTRA_WIDGET_TARGET)
        if (!widgetTarget.isNullOrBlank()) {
            intentToHandle.removeExtra(com.example.widget.DyneWidgetHelper.EXTRA_WIDGET_TARGET)
            showSplash = false
            handled = true
            val mappedFeatureId = when (widgetTarget) {
                "flashlight" -> "flashlight_normal"
                "compass" -> "compass"
                "solunar", "outdoors_solunar" -> "outdoor_solunar_today"
                "scan_doc" -> "scanner_document"
                "scan_qr" -> "scanner_qr"
                "quick_note" -> "note_text"
                "home_widgets" -> "home_widgets"
                "weather", "outdoors_weather" -> "outdoor_weather_current"
                else -> widgetTarget
            }
            // Widgets are a Premium capability; if user's widget unlock expired, gate via FeatureGuard
            if (!isPro && !resolvedFeatureGate.hasAccess("home_widgets", prefs = userPreferences)) {
                navigateToFeature("home_widgets")
            } else {
                navigateToFeature(mappedFeatureId)
            }
        }

        val shortcutFeatureId = intentToHandle.getStringExtra(com.example.domain.FeatureRegistry.EXTRA_SHORTCUT_FEATURE_ID)
            ?: intentToHandle.getStringExtra("shortcut_feature_id")
            ?: intentToHandle.getStringExtra("feature_id")
            ?: intentToHandle.data?.let { uri ->
                if (uri.path?.contains("feature/") == true) uri.lastPathSegment else null
            }
        if (!shortcutFeatureId.isNullOrBlank()) {
            intentToHandle.removeExtra(com.example.domain.FeatureRegistry.EXTRA_SHORTCUT_FEATURE_ID)
            intentToHandle.removeExtra("shortcut_feature_id")
            intentToHandle.removeExtra("feature_id")
            showSplash = false
            handled = true
            navigateToFeature(shortcutFeatureId)
        }
        return handled
    }

    LaunchedEffect(incomingIntent) {
        if (incomingIntent != null) {
            handleIncomingIntent(incomingIntent)
            onConsumeIncomingIntent()
        }
    }

    LaunchedEffect(isPro, userPreferences.selectedThemeName, userPreferences.accessUnlockRecordsSerialized) {
        com.example.service.DyneNotificationCenter.syncAllSchedules(context, isPremiumActive = isPro)
        com.example.widget.DyneWidgetHelper.refreshAllWidgets(context)
    }

    LaunchedEffect(Unit) {
        com.example.service.DyneAlertsScheduler.scheduleOrUpdateAlerts(context)
        val proToolIds = ToolRegistry.tools.value.filter { it.isPro && it.isEnabled }.map { it.id }
        preferencesRepository.ensureOrActivateDailySpotlight(proToolIds, activateNow = false)

        val handledInitialIntent = handleIncomingIntent(activity?.intent)
        if (!hasRecordedSessionLaunch) {
            hasRecordedSessionLaunch = true
            preferencesRepository.incrementLaunchCount()
            val configuredTab = userPreferences.effectiveStartTabIndex(isPro).coerceIn(0, 3)
            if (configuredTab != 0 && !handledInitialIntent) {
                selectedTabIndex = configuredTab
            }
        }
    }

    val liveGeoFlow = remember(userPreferences.manualLat, userPreferences.manualLng) {
        locationProvider.locationUpdatesFlow(userPreferences.manualLat, userPreferences.manualLng)
    }
    val geoCoordinates by liveGeoFlow.collectAsStateWithLifecycle(
        initialValue = GeoCoordinates(
            latitude = userPreferences.manualLat,
            longitude = userPreferences.manualLng,
            isFromGps = false,
            label = userPreferences.locationName
        )
    )

    val tabs = remember {
        listOf(
            GlassTabItem("Home", Icons.Default.Home, "nav_tab_home"),
            GlassTabItem("Outdoors", Icons.Default.Explore, "nav_tab_outdoors"),
            GlassTabItem("Library", Icons.Default.FolderOpen, "nav_tab_library"),
            GlassTabItem("Settings", Icons.Default.Settings, "nav_tab_settings")
        )
    }

    val selectedTool: RegisteredTool? = remember(activeToolId) {
        activeToolId?.let { ToolRegistry.getToolById(it) }
    }

    // Home composes behind the splash and cross-fades in (scale 1.04 to 1.0 plus fade) when the splash ends
    val homeCrossfadeScale by animateFloatAsState(
        targetValue = if (showSplash) 1.04f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "home_splash_scale"
    )
    val homeCrossfadeAlpha by animateFloatAsState(
        targetValue = if (showSplash) 0f else 1f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "home_splash_alpha"
    )

    // Subtle content blur when the Scan PlusMenu is open
    val backgroundBlurRadius by animateDpAsState(
        targetValue = if (showScanMenu && !theme.reduceGlassEffects) 6.dp else 0.dp,
        animationSpec = tween(150),
        label = "scan_menu_bg_blur"
    )

    val maxFreeDocuments = 5 + userPreferences.bonusDocumentSaves

    CompositionLocalProvider(LocalFeatureGate provides resolvedFeatureGate) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        // Main App Layer (composes behind Splash and cross-fades in when Splash finishes)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = homeCrossfadeScale
                    scaleY = homeCrossfadeScale
                    alpha = homeCrossfadeAlpha
                }
        ) {
            // Main Content Layer with Haze source and slight blur when Scan PlusMenu is open
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(state = hazeState)
                    .blur(backgroundBlurRadius)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                when {
                    showRewardsScreen && resolvedAdManager != null -> {
                        RewardsScreen(
                            preferences = userPreferences,
                            preferencesRepository = preferencesRepository,
                            scanRepository = scanRepository,
                            adManager = resolvedAdManager,
                            featureGate = resolvedFeatureGate,
                            hazeState = hazeState,
                            onOpenPaywall = {
                                showRewardsScreen = false
                                showPaywallSheet = true
                            },
                            onOpenSpotlightTool = { tool ->
                                scope.launch {
                                    val proToolIds = ToolRegistry.tools.value.filter { it.isPro && it.isEnabled }.map { it.id }
                                    preferencesRepository.ensureOrActivateDailySpotlight(proToolIds, activateNow = true)
                                    showRewardsScreen = false
                                    navigateToFeature(tool.id)
                                }
                            },
                            onBack = {
                                showRewardsScreen = false
                            }
                        )
                    }

                    activeScannerOverlay == ActiveScannerOverlay.QR_BARCODE -> {
                        QrBarcodeScannerScreen(
                            viewModel = scanViewModel,
                            isPro = isPro,
                            hazeState = hazeState,
                            onNavigateToHistory = {
                                activeScannerOverlay = ActiveScannerOverlay.NONE
                                librarySegment = 1
                                selectedTabIndex = 2
                                isTabBarVisible = true
                                scope.launch {
                                    val totalScans = preferencesRepository.incrementCompletedScanCount()
                                    if (totalScans >= 5) {
                                        triggerSmartUpsellAfterSuccess()
                                    }
                                }
                            },
                            onClose = {
                                activeScannerOverlay = ActiveScannerOverlay.NONE
                                isTabBarVisible = true
                            }
                        )
                    }

                    activeScannerOverlay == ActiveScannerOverlay.DOCUMENT -> {
                        DocumentScannerScreen(
                            viewModel = scanViewModel,
                            hazeState = hazeState,
                            isDocsUnlocked = isPro || resolvedFeatureGate.isUnlocked(PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE),
                            onDocumentSaved = {
                                activeScannerOverlay = ActiveScannerOverlay.NONE
                                librarySegment = 0
                                selectedTabIndex = 2
                                isTabBarVisible = true
                                scope.launch {
                                    val totalScans = preferencesRepository.incrementCompletedScanCount()
                                    if (totalScans >= 5) {
                                        triggerSmartUpsellAfterSuccess()
                                    }
                                }
                                // Natural break: after saving a document
                                resolvedAdManager?.maybeShowInterstitialAtNaturalBreak(
                                    activity = activity,
                                    reason = "After saving document"
                                )
                            },
                            onClose = {
                                activeScannerOverlay = ActiveScannerOverlay.NONE
                                isTabBarVisible = true
                            }
                        )
                    }

                    isDrawingPadOpen -> {
                        FullScreenDrawingPadScreen(
                            existingNote = activeDrawingPadNote,
                            scanViewModel = scanViewModel,
                            isPro = isPro,
                            hazeState = hazeState,
                            onOpenPaywall = { showPaywallSheet = true },
                            onClose = {
                                scanViewModel.closeDrawingPad()
                            }
                        )
                    }

                    isFullScreenNoteEditorOpen -> {
                        FullScreenNoteEditorScreen(
                            existingNote = activeEditorNote,
                            scanViewModel = scanViewModel,
                            isPro = isPro,
                            hazeState = hazeState,
                            onOpenPaywall = { showPaywallSheet = true },
                            onClose = {
                                scanViewModel.closeFullScreenNoteEditor()
                            }
                        )
                    }

                    selectedTool != null -> {
                        val activeCatalogFeature = remember(activeFeatureId, selectedTool.id, activeToolInitialModeId) {
                            activeFeatureId?.let { com.example.domain.FeatureRegistry.getFeatureById(it) }
                                ?: com.example.domain.FeatureRegistry.resolveFeatureForToolAndMode(selectedTool.id, activeToolInitialModeId)
                                ?: com.example.domain.FeatureRegistry.resolveFeatureOrPrimaryForTool(selectedTool.id)
                        }
                        val isToolUnlocked = if (activeCatalogFeature != null) {
                            resolvedFeatureGate.isCatalogFeatureUnlocked(
                                catalogFeature = activeCatalogFeature,
                                prefs = userPreferences,
                                nowMs = System.currentTimeMillis()
                            )
                        } else {
                            resolvedFeatureGate.isToolUnlocked(
                                toolId = selectedTool.id,
                                isProTool = selectedTool.isPro,
                                nowMs = System.currentTimeMillis()
                            )
                        }
                        val favTargetId = activeCatalogFeature?.id ?: selectedTool.id
                        val isFav = userPreferences.effectiveFavoriteIds(isPro).any {
                            it == favTargetId ||
                                com.example.domain.FeatureRegistry.resolveFeatureOrPrimaryForTool(it)?.id == favTargetId
                        }
                        val isNoAdTool = selectedTool.category == com.example.domain.ToolCategory.PHONE_CARE_AND_AUDIO ||
                            selectedTool.category == com.example.domain.ToolCategory.MEDIA_STUDIO ||
                            selectedTool.id in setOf("speedometer", "sound_db", "angle", "level", "compass", "metal_finder", "vibration_gforce", "altimeter")
                        ToolDetailScreen(
                            tool = selectedTool,
                            catalogFeature = activeCatalogFeature,
                            initialModeId = activeToolInitialModeId,
                            sensorState = sensorState,
                            geoCoordinates = geoCoordinates,
                            unitSystem = userPreferences.unitSystem,
                            scanViewModel = scanViewModel,
                            isPro = isToolUnlocked,
                            isAccountPro = isPro,
                            isFavorite = isFav,
                            onToggleFavorite = {
                                settingsViewModel.toggleFavoriteFeature(
                                    context = context,
                                    featureId = favTargetId,
                                    isCustomizationUnlocked = isPro || resolvedFeatureGate.isHomeCustomizationUnlocked(userPreferences)
                                )
                            },
                            onTogglePinMode = { modeId ->
                                val modeFeat = com.example.domain.FeatureRegistry.resolveFeatureForToolAndMode(selectedTool.id, modeId)
                                settingsViewModel.toggleFavoriteFeature(
                                    context = context,
                                    featureId = modeFeat?.id ?: "${selectedTool.id}:$modeId",
                                    isCustomizationUnlocked = isPro || resolvedFeatureGate.isHomeCustomizationUnlocked(userPreferences)
                                )
                            },
                            userPreferences = userPreferences,
                            preferencesRepository = preferencesRepository,
                            ownedSoundPacks = ownedSoundPacks,
                            soundPackPrices = soundPackPrices,
                            onSwitchTool = { targetToolId ->
                                val baseId = targetToolId.substringBefore(":")
                                val subMode = targetToolId.substringAfter(":", "").ifEmpty { null }
                                navigateToFeature(baseId, subMode)
                            },
                            onLaunchScannerMode = { scannerModeId ->
                                activeFeatureId = null
                                activeToolId = null
                                activeToolInitialModeId = null
                                activeScannerOverlay = if (scannerModeId == "document" || scannerModeId == "id_card") {
                                    ActiveScannerOverlay.DOCUMENT
                                } else {
                                    ActiveScannerOverlay.QR_BARCODE
                                }
                            },
                            hazeState = hazeState,
                            onOpenPaywall = { showPaywallSheet = true },
                            onSaveNoteToVault = { title, content, categoryTag ->
                                scanViewModel.saveNote(
                                    title = title,
                                    content = content,
                                    categoryTag = categoryTag
                                )
                            },
                            onOpenLibraryNotes = {
                                val usedDurationMs = if (activeToolEnteredAtMs > 0L) {
                                    System.currentTimeMillis() - activeToolEnteredAtMs
                                } else 0L
                                val exitedToolName = activeCatalogFeature?.title ?: selectedTool.name
                                activeFeatureId = null
                                activeToolId = null
                                activeToolInitialModeId = null
                                librarySegment = 2
                                selectedTabIndex = 2
                                isTabBarVisible = true
                                if (usedDurationMs >= 20_000L) {
                                    triggerSmartUpsellAfterSuccess()
                                    if (!isNoAdTool) {
                                        resolvedAdManager?.maybeShowInterstitialAtNaturalBreak(
                                            activity = activity,
                                            reason = "Left $exitedToolName after ${usedDurationMs / 1000L}s"
                                        )
                                    }
                                }
                            },
                            onBack = {
                                val usedDurationMs = if (activeToolEnteredAtMs > 0L) {
                                    System.currentTimeMillis() - activeToolEnteredAtMs
                                } else 0L
                                val exitedToolName = activeCatalogFeature?.title ?: selectedTool.name
                                activeFeatureId = null
                                activeToolId = null
                                activeToolInitialModeId = null
                                isTabBarVisible = true
                                // Natural break & Smart Upsell after a finished tool session (20+ seconds)
                                if (usedDurationMs >= 20_000L) {
                                    triggerSmartUpsellAfterSuccess()
                                    if (!isNoAdTool) {
                                        resolvedAdManager?.maybeShowInterstitialAtNaturalBreak(
                                            activity = activity,
                                            reason = "Left $exitedToolName after ${usedDurationMs / 1000L}s"
                                        )
                                    }
                                } else {
                                    resolvedAdManager?.logEvent(
                                        "Interstitial skipped [Left $exitedToolName]: used ${usedDurationMs / 1000L}s"
                                    )
                                }
                            }
                        )
                    }

                    else -> {
                        if (selectedTabIndex != 0 && activeViewerDoc == null) {
                            BackHandler {
                                selectedTabIndex = 0
                                isTabBarVisible = true
                            }
                        }
                        val isFloatingBarShown = selectedTool == null &&
                            activeScannerOverlay == ActiveScannerOverlay.NONE &&
                            !showRewardsScreen &&
                            !showAllFeaturesScreen &&
                            !showCustomizeHomeScreen &&
                            activeViewerDoc == null &&
                            !isDrawingPadOpen &&
                            !isFullScreenNoteEditorOpen
                        CompositionLocalProvider(
                            LocalBottomBarInset provides if (isFloatingBarShown) 116.dp else 16.dp
                        ) {
                            tabStateHolder.SaveableStateProvider(key = "tab_$selectedTabIndex") {
                                when (selectedTabIndex) {
                                    0 -> HomeScreen(
                                        isPro = isPro,
                                        userPreferences = userPreferences,
                                        sunMoonState = sunMoonState,
                                        coinBalance = userPreferences.coinBalance,
                                        solunarState = solunarState,
                                        hazeState = hazeState,
                                        nestedScrollConnection = scrollConnection,
                                        scrollToTopTrigger = homeScrollToTopTrigger,
                                        featureGate = resolvedFeatureGate,
                                        settingsViewModel = settingsViewModel,
                                        showFirstRunPresetPicker = !userPreferences.homePresetDismissed,
                                        selectedPresetName = userPreferences.selectedHomePreset,
                                        isToolRewardUnlocked = { idOrFeatId ->
                                            val feat = com.example.domain.FeatureRegistry.getFeatureById(idOrFeatId)
                                                ?: com.example.domain.FeatureRegistry.resolveFeatureOrPrimaryForTool(idOrFeatId.substringBefore(":"))
                                            val key = feat?.id ?: idOrFeatId.substringBefore(":")
                                            !isPro && (
                                                resolvedFeatureGate.getRemainingMsForKey(key, prefs = userPreferences) > 0L ||
                                                    userPreferences.isToolSpotlightUnlocked(feat?.toolId ?: key, System.currentTimeMillis())
                                                )
                                        },
                                        getToolCountdownProgress = { idOrFeatId ->
                                            val feat = com.example.domain.FeatureRegistry.getFeatureById(idOrFeatId)
                                                ?: com.example.domain.FeatureRegistry.resolveFeatureOrPrimaryForTool(idOrFeatId.substringBefore(":"))
                                            val key = feat?.id ?: idOrFeatId.substringBefore(":")
                                            resolvedFeatureGate.getCountdownProgressForKey(key, prefs = userPreferences)
                                        },
                                        getToolCountdownLabel = { idOrFeatId ->
                                            val feat = com.example.domain.FeatureRegistry.getFeatureById(idOrFeatId)
                                                ?: com.example.domain.FeatureRegistry.resolveFeatureOrPrimaryForTool(idOrFeatId.substringBefore(":"))
                                            val key = feat?.id ?: idOrFeatId.substringBefore(":")
                                            val rem = resolvedFeatureGate.getRemainingMsForKey(key, prefs = userPreferences)
                                            resolvedFeatureGate.formatRemainingRingBadge(rem)
                                        },
                                        expiringSoonNotice = resolvedFeatureGate.getExpiringSoonNotice(prefs = userPreferences),
                                        onApplyPreset = { preset ->
                                            settingsViewModel.applyHomePreset(preset)
                                        },
                                        onDismissPresetPicker = {
                                            settingsViewModel.dismissHomePresetBanner()
                                        },
                                        onOpenPaywall = { showPaywallSheet = true },
                                        onOpenRewards = { showRewardsScreen = true },
                                        onOpenCustomizeHome = {
                                            showCustomizeHomeScreen = true
                                        },
                                        onOpenAllFeatures = {
                                            showAllFeaturesScreen = true
                                        },
                                        onOpenCatalogFeature = { feat ->
                                            openCatalogFeature(feat)
                                        },
                                        onToggleFavoriteTool = { toolId ->
                                            settingsViewModel.toggleFavoriteFeature(
                                                context = context,
                                                featureId = toolId,
                                                isCustomizationUnlocked = isPro || resolvedFeatureGate.isHomeCustomizationUnlocked(userPreferences)
                                            )
                                        },
                                        onMoveFavoriteTool = { toolId, delta ->
                                            settingsViewModel.moveFavoriteTool(toolId, delta)
                                        },
                                        onHideTool = { toolId ->
                                            settingsViewModel.setToolEnabled(toolId.substringBefore(":"), false)
                                        },
                                        onSkyNowClick = {
                                            navigateToFeature("outdoor_sky_dome")
                                        },
                                        onActivateSpotlightTool = { spotlightTool ->
                                            scope.launch {
                                                val proToolIds = ToolRegistry.tools.value.filter { it.isPro && it.isEnabled }.map { it.id }
                                                preferencesRepository.ensureOrActivateDailySpotlight(proToolIds, activateNow = true)
                                                navigateToFeature(spotlightTool.id)
                                            }
                                        },
                                        onLockedToolClick = { lockedTool ->
                                            navigateToFeature(lockedTool.id)
                                        },
                                        onSelectTool = { tool ->
                                            navigateToFeature(tool.id)
                                        },
                                        onSelectToolWithMode = { tool, modeId ->
                                            navigateToFeature(tool.id, modeId)
                                        },
                                        onQuickAccessClick = { target ->
                                            when (target) {
                                                QuickAccessTarget.DOCUMENTS -> navigateToFeature("scanner_document")
                                                QuickAccessTarget.QR_BARCODE -> navigateToFeature("scanner_qr")
                                                QuickAccessTarget.SOLUNAR -> navigateToFeature("outdoor_solunar_today")
                                                QuickAccessTarget.WEATHER -> navigateToFeature("outdoor_weather_current")
                                                QuickAccessTarget.SPEAKER_CLEANER -> navigateToFeature("speaker_cleaner")
                                                QuickAccessTarget.FREQUENCY -> navigateToFeature("frequency_generator")
                                                QuickAccessTarget.MEDIA_STUDIO -> navigateToFeature("photo_converter")
                                                QuickAccessTarget.OUTDOOR_PLANNER -> navigateToFeature("outdoor_planner_7day")
                                                QuickAccessTarget.SUN_MOON_CALENDAR -> navigateToFeature("outdoor_sun_moon_calendar")
                                                QuickAccessTarget.OUTDOOR_ALERTS -> navigateToFeature("outdoor_alerts")
                                                QuickAccessTarget.HOME_WIDGETS -> navigateToFeature("home_widgets")
                                                QuickAccessTarget.CUSTOM_STYLE_STUDIO -> navigateToFeature("custom_style_studio")
                                            }
                                        },
                                        onHeroActionClick = {
                                            navigateToFeature("outdoor_solunar_today")
                                        }
                                    )

                                    1 -> OutdoorsScreen(
                                        selectedSegment = outdoorsSegment,
                                        onSegmentChange = { outdoorsSegment = it },
                                        solunarViewModel = solunarViewModel,
                                        sunMoonViewModel = sunMoonViewModel,
                                        sensorState = sensorState,
                                        unitSystem = userPreferences.unitSystem,
                                        isPro = isPro,
                                        hazeState = hazeState,
                                        nestedScrollConnection = scrollConnection,
                                        scrollToTopTrigger = outdoorsScrollToTopTrigger,
                                        onOpenPaywall = { showPaywallSheet = true }
                                    )

                                    2 -> LibraryScreen(
                                        selectedSegment = librarySegment,
                                        onSegmentChange = { librarySegment = it },
                                        scanViewModel = scanViewModel,
                                        isPro = isPro,
                                        bonusDocumentSaves = userPreferences.bonusDocumentSaves,
                                        hazeState = hazeState,
                                        nestedScrollConnection = scrollConnection,
                                        scrollToTopTrigger = libraryScrollToTopTrigger,
                                        onLaunchDocumentScanner = {
                                            navigateToFeature("scanner_document")
                                        },
                                        onLaunchQrScanner = {
                                            navigateToFeature("scanner_qr")
                                        },
                                        onOpenPaywall = { showPaywallSheet = true },
                                        onOpenPdfTools = {
                                            navigateToFeature("pdf_merge")
                                        }
                                    )

                                    else -> SettingsScreen(
                                        viewModel = settingsViewModel,
                                        hazeState = hazeState,
                                        nestedScrollConnection = scrollConnection,
                                        scrollToTopTrigger = settingsScrollToTopTrigger,
                                        onOpenPaywall = { showPaywallSheet = true },
                                        onOpenRewards = { showRewardsScreen = true },
                                        onOpenAllFeatures = { showAllFeaturesScreen = true },
                                        initialSubScreen = pendingSettingsSubScreen,
                                        onConsumedInitialSubScreen = {
                                            pendingSettingsSubScreen = com.example.ui.settings.SettingsSubScreen.ROOT
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Animated Scan PlusMenu Overlay (scrim + stacked glass pills above the center Scan button)
            ScanPlusMenuOverlay(
                visible = showScanMenu,
                shortcutIds = userPreferences.scanShortcutsList,
                hazeState = hazeState,
                onRequestClose = { showScanMenu = false },
                onSelectDocumentScan = {
                    showScanMenu = false
                    navigateToFeature("scanner_document")
                },
                onSelectQrBarcodeScan = {
                    showScanMenu = false
                    navigateToFeature("scanner_qr")
                },
                onOpenToolShortcut = { toolId ->
                    showScanMenu = false
                    navigateToFeature(toolId)
                }
            )

            // Soft 32.dp fade-to-background gradient at the bottom edge (never above the pill, no hard edge)
            // + Floating Pill Glass Bottom Bar (hidden in tools, scanners, rewards, and Document Viewer)
            val showFloatingBottomBar = selectedTool == null &&
                activeScannerOverlay == ActiveScannerOverlay.NONE &&
                !showRewardsScreen &&
                !showAllFeaturesScreen &&
                !showCustomizeHomeScreen &&
                !showPaywallSheet &&
                activeViewerDoc == null &&
                !isDrawingPadOpen &&
                !isFullScreenNoteEditorOpen

            if (showFloatingBottomBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(32.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        theme.background
                                    )
                                )
                            )
                    )

                    GlassTabBar(
                        tabs = tabs,
                        selectedIndex = selectedTabIndex,
                        visible = isTabBarVisible || showScanMenu,
                        hazeState = hazeState,
                        isScanMenuOpen = showScanMenu,
                        onTabSelected = { index ->
                            showScanMenu = false
                            if (selectedTabIndex == index) {
                                when (index) {
                                    0 -> homeScrollToTopTrigger++
                                    1 -> outdoorsScrollToTopTrigger++
                                    2 -> libraryScrollToTopTrigger++
                                    3 -> settingsScrollToTopTrigger++
                                }
                            } else {
                                selectedTabIndex = index
                            }
                            isTabBarVisible = true
                        },
                        onScanClicked = {
                            showScanMenu = !showScanMenu
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            // Full-screen "All Features" directory (accessible from Home and Paywall)
            if (showAllFeaturesScreen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(theme.background)
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    com.example.ui.home.AllFeaturesScreen(
                        capabilities = com.example.data.DeviceCapabilities,
                        preferences = userPreferences,
                        featureGate = resolvedFeatureGate,
                        isPro = isPro,
                        onBack = { showAllFeaturesScreen = false },
                        onOpenFeature = { feat ->
                            openCatalogFeature(feat)
                        },
                        onToggleFavorite = { feat ->
                            settingsViewModel.toggleFavoriteFeature(
                                context = context,
                                featureId = feat.id,
                                isCustomizationUnlocked = isPro || resolvedFeatureGate.isHomeCustomizationUnlocked(userPreferences)
                            )
                        }
                    )
                }
            }

            // Full-screen "Customize Home" editor (accessible from Home "Customize" button)
            if (showCustomizeHomeScreen) {
                BackHandler { showCustomizeHomeScreen = false }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(theme.background)
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    com.example.ui.home.CustomizeHomeEditorScreen(
                        viewModel = settingsViewModel,
                        preferences = userPreferences,
                        capabilities = com.example.data.DeviceCapabilities,
                        featureGate = resolvedFeatureGate,
                        isPro = isPro,
                        showTopBar = true,
                        onBack = { showCustomizeHomeScreen = false },
                        onOpenPaywall = { showPaywallSheet = true },
                        onOpenAllFeatures = {
                            showCustomizeHomeScreen = false
                            showAllFeaturesScreen = true
                        }
                    )
                }
            }

            // Unified 1-hour Unlock Sheet (Glass, Gold Accents: "Watch ad: unlock for 1 hour", "Use 10 coins: unlock for 1 hour", "Go Pro")
            val activeUnlockReq by resolvedFeatureGate.activeUnlockRequest.collectAsStateWithLifecycle()
            if (activeUnlockReq != null && resolvedAdManager != null) {
                FeatureUnlockGlassSheet(
                    prompt = activeUnlockReq!!,
                    featureGate = resolvedFeatureGate,
                    adManager = resolvedAdManager,
                    coinBalance = userPreferences.coinBalance,
                    hazeState = hazeState,
                    onOpenPaywall = {
                        resolvedFeatureGate.dismissUnlockSheet()
                        showPaywallSheet = true
                    },
                    onDismiss = {
                        resolvedFeatureGate.dismissUnlockSheet()
                    }
                )
            }

            // Legacy Rewarded Ad Glass Sheet
            if (resolvedAdManager != null) {
                val promptTarget by resolvedAdManager.activeRewardPrompt.collectAsStateWithLifecycle()
                val currentTarget = promptTarget
                if (currentTarget != null) {
                    RewardedUnlockGlassSheet(
                        target = currentTarget,
                        adManager = resolvedAdManager,
                        hazeState = hazeState,
                        onOpenPaywall = {
                            resolvedAdManager.dismissRewardPrompt()
                            showPaywallSheet = true
                        },
                        onDismiss = {
                            resolvedAdManager.dismissRewardPrompt()
                        }
                    )
                }
            }

            // Full-screen Paywall route ("Dyne Premium" — never shown to Premium users)
            if (showPaywallSheet) {
                if (isPro) {
                    showPaywallSheet = false
                } else {
                    com.example.ui.paywall.PaywallScreen(
                        viewModel = paywallViewModel,
                        hazeState = hazeState,
                        onDismissMaybeLater = {
                            scope.launch {
                                preferencesRepository.pauseSmartUpsellFor7Days()
                            }
                            showPaywallSheet = false
                        },
                        onDismiss = { showPaywallSheet = false },
                        onOpenAllFeatures = {
                            showPaywallSheet = false
                            showAllFeaturesScreen = true
                        }
                    )
                }
            }

            // One-time snackbar when a 1-hour theme or layout unlock expires in the foreground
            if (showUnlockEndedSnackbar) {
                androidx.compose.material3.Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .height(56.dp),
                    containerColor = theme.elevated,
                    contentColor = theme.textPrimary,
                    action = {
                        androidx.compose.material3.TextButton(onClick = onDismissUnlockEndedSnackbar) {
                            androidx.compose.material3.Text(
                                text = "OK",
                                color = theme.accent
                            )
                        }
                    }
                ) {
                    androidx.compose.material3.Text(
                        text = "Your 1-hour unlock ended - default look restored"
                    )
                }
            }
        }

        // Cold-Start Launch Animation Overlay (Splash.kt -> DyneSplash(onFinished))
        if (showSplash) {
            DyneSplash(
                onFinished = { showSplash = false },
                launchMode = userPreferences.launchAnimationMode,
                lastComboKey = userPreferences.lastSplashCombo,
                reduceMotionSetting = userPreferences.reduceMotion,
                onSaveComboKey = { comboKey ->
                    scope.launch {
                        preferencesRepository.setLastSplashCombo(comboKey)
                    }
                },
                onFirstFrameDrawn = onFirstSplashFrame
            )
        }

        // App Lock Overlay (Premium: lock on launch & after 1/5/15 minutes in background)
        com.example.ui.settings.DyneAppLockOverlay(
            appLockEnabled = userPreferences.appLockEnabled,
            appLockMethod = userPreferences.appLockMethod,
            lockOnLaunch = userPreferences.appLockOnLaunch,
            timeoutMinutes = userPreferences.appLockTimeoutMinutes,
            isSplashActive = showSplash
        )
    }
    }
}
