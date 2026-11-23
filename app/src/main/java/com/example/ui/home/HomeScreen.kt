package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DeviceCapabilities
import com.example.data.UserPreferences
import com.example.domain.AstroCalculator
import com.example.domain.HomeNeedPreset
import com.example.domain.RegisteredTool
import com.example.domain.ToolMode
import com.example.domain.ToolRegistry
import androidx.activity.compose.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.example.ui.components.CoinBalanceTopBarChip
import com.example.ui.components.FeedbackHelper
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassSurface
import com.example.ui.components.HazeState
import com.example.ui.components.LocalBottomBarInset
import com.example.ui.components.ProBadge
import com.example.ui.components.ProCountdownRing
import com.example.ui.components.ProCrownChip
import com.example.ui.components.ProCrownTopBarButton
import com.example.ui.components.SharedTopBar
import com.example.ui.components.dyneClickable
import com.example.ui.components.horizontalChipRowEdgeFade
import com.example.ui.components.proBorder
import com.example.ui.solunar.SolunarUiState
import com.example.ui.sunmoon.SunMoonUiState
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.DesignTokens
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class QuickAccessTarget {
    DOCUMENTS,
    QR_BARCODE,
    SOLUNAR,
    WEATHER,
    SPEAKER_CLEANER,
    FREQUENCY,
    MEDIA_STUDIO,
    OUTDOOR_PLANNER,
    SUN_MOON_CALENDAR,
    OUTDOOR_ALERTS,
    HOME_WIDGETS,
    CUSTOM_STYLE_STUDIO
}

@Immutable
data class FavoriteTileItem(
    val favoriteKey: String,
    val tool: RegisteredTool,
    val mode: ToolMode? = null
) {
    val displayTitle: String
        get() = if (mode != null) "${tool.name} • ${mode.title}" else tool.name
    val displayIcon: ImageVector
        get() = mode?.icon ?: tool.icon
    val isLockedPro: Boolean
        get() = tool.isPro || (mode?.isPremium == true)
}

private data class LongPressContextTarget(
    val favoriteKey: String,
    val tool: RegisteredTool,
    val mode: ToolMode? = null,
    val fromFavorites: Boolean,
    val anchorRootOffset: Offset
)

@Immutable
private sealed class HeroSlideItem {
    data class ToolSlide(val tool: RegisteredTool) : HeroSlideItem()
    data class SpotlightSlide(
        val tool: RegisteredTool,
        val shortLabel: String,
        val buttonLabel: String,
        val isExpiredToday: Boolean
    ) : HeroSlideItem()
    data class SolunarSlide(
        val bestWindow: String,
        val score: Int,
        val moonPhase: String
    ) : HeroSlideItem()
    data class WeatherSlide(
        val tempLabel: String,
        val conditionLabel: String
    ) : HeroSlideItem()
}

@Immutable
private data class CategorySectionUiModel(
    val id: String,
    val name: String,
    val layoutColumns: Int,
    val features: List<com.example.domain.Feature>,
    val synthTools: List<RegisteredTool>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    isPro: Boolean,
    userPreferences: UserPreferences = UserPreferences(),
    sunMoonState: SunMoonUiState = SunMoonUiState(),
    isToolRewardUnlocked: (String) -> Boolean = { false },
    getToolCountdownProgress: (String) -> Float = { 0.85f },
    getToolCountdownLabel: (String) -> String = { "1h" },
    expiringSoonNotice: String? = null,
    coinBalance: Int = 0,
    scrollToTopTrigger: Int = 0,
    onOpenRewards: () -> Unit = {},
    onOpenCustomizeHome: () -> Unit = {},
    onToggleFavoriteTool: (String) -> Unit = {},
    onMoveFavoriteTool: (String, Int) -> Unit = { _, _ -> },
    onHideTool: (String) -> Unit = {},
    onSkyNowClick: () -> Unit = {},
    solunarState: SolunarUiState,
    hazeState: HazeState?,
    nestedScrollConnection: NestedScrollConnection,
    showFirstRunPresetPicker: Boolean = false,
    selectedPresetName: String = "",
    onApplyPreset: (HomeNeedPreset) -> Unit = {},
    onDismissPresetPicker: () -> Unit = {},
    onOpenPaywall: () -> Unit,
    onLockedToolClick: (RegisteredTool) -> Unit = { onOpenPaywall() },
    onSelectTool: (RegisteredTool) -> Unit,
    onSelectToolWithMode: (RegisteredTool, String?) -> Unit = { tool, _ -> onSelectTool(tool) },
    onActivateSpotlightTool: (RegisteredTool) -> Unit = { tool -> onSelectTool(tool) },
    onQuickAccessClick: (QuickAccessTarget) -> Unit,
    onHeroActionClick: () -> Unit,
    featureGate: com.example.billing.FeatureGate? = null,
    settingsViewModel: com.example.ui.settings.SettingsViewModel? = null,
    onOpenAllFeatures: () -> Unit = {},
    onOpenCatalogFeature: (com.example.domain.Feature) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val bottomInset = LocalBottomBarInset.current
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val animationsDisabled = userPreferences.reduceMotion || userPreferences.reduceGlassEffects

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            if (animationsDisabled) {
                lazyListState.scrollToItem(0)
            } else {
                lazyListState.animateScrollToItem(0)
            }
        }
    }

    val allTools by ToolRegistry.tools.collectAsState()
    val capabilitiesSnapshot = remember(context) { DeviceCapabilities.initialize(context) }

    val isCustomizationUnlocked = userPreferences.isCustomizationAccessActive(isPro)

    // Filter tools by hardware capability support and enabled state
    val supportedEnabledTools = remember(allTools, capabilitiesSnapshot) {
        allTools.filter { tool ->
            tool.isEnabled && DeviceCapabilities.isToolSupported(tool.requiredCapabilities, context)
        }
    }

    val rawFavoriteIds = userPreferences.favoriteToolIdsList
    val favoriteIds = remember(rawFavoriteIds, isCustomizationUnlocked, isPro) {
        userPreferences.effectiveFavoriteIds(isPro)
    }
    val favoriteTileItems = remember(favoriteIds, supportedEnabledTools, capabilitiesSnapshot) {
        favoriteIds.mapNotNull { entry ->
            val catalogFeature = com.example.domain.FeatureRegistry.resolveFeatureOrPrimaryForTool(entry)
            if (catalogFeature != null && com.example.domain.FeatureRegistry.isSupported(catalogFeature, capabilitiesSnapshot)) {
                val parentTool = allTools.find { it.id == catalogFeature.toolId } ?: supportedEnabledTools.firstOrNull()
                if (parentTool != null) {
                    val mode = com.example.domain.ToolMode(
                        id = catalogFeature.modeId ?: catalogFeature.id,
                        title = catalogFeature.title,
                        icon = catalogFeature.icon,
                        isPremium = catalogFeature.isPremium
                    )
                    return@mapNotNull FavoriteTileItem(
                        favoriteKey = catalogFeature.id,
                        tool = parentTool.copy(
                            id = catalogFeature.id,
                            name = catalogFeature.title,
                            shortBenefit = catalogFeature.shortBenefit,
                            icon = catalogFeature.icon,
                            isPro = catalogFeature.isPremium
                        ),
                        mode = mode
                    )
                }
            }
            val resolved = ToolRegistry.resolveToolAndMode(entry) ?: return@mapNotNull null
            val (tool, mode) = resolved
            if (supportedEnabledTools.any { it.id == tool.id }) {
                FavoriteTileItem(favoriteKey = entry, tool = tool, mode = mode)
            } else {
                null
            }
        }
    }
    val favoriteTools = remember(favoriteTileItems) {
        favoriteTileItems.map { it.tool }.distinctBy { it.id }
    }

    val recentIds = userPreferences.recentToolIdsList
    val recentTools = remember(recentIds, supportedEnabledTools) {
        recentIds.mapNotNull { id ->
            val catFeat = com.example.domain.FeatureRegistry.getFeatureById(id)
            if (catFeat != null) {
                val baseTool = allTools.find { it.id == catFeat.toolId }
                baseTool?.copy(
                    id = catFeat.id,
                    name = catFeat.title,
                    shortBenefit = catFeat.shortBenefit,
                    icon = catFeat.icon,
                    isPro = catFeat.isPremium
                )
            } else {
                supportedEnabledTools.find { it.id == id }
            }
        }
    }

    // Search bar is toggled via the header search icon button
    var isSearchBarVisible by rememberSaveable { mutableStateOf(false) }
    var homeSearchQuery by rememberSaveable { mutableStateOf("") }
    val featureSearchResults by remember(homeSearchQuery, capabilitiesSnapshot) {
        derivedStateOf {
            if (homeSearchQuery.isBlank()) {
                emptyList()
            } else {
                com.example.domain.FeatureRegistry.searchFeatures(
                    query = homeSearchQuery,
                    capabilities = DeviceCapabilities,
                    includeUnsupported = false
                )
            }
        }
    }

    // Active "See all (N)" or "See all (M)" dialog state
    var seeAllDialogState by remember {
        mutableStateOf<Triple<String, String, List<com.example.domain.Feature>>?>(null)
    }
    var seeAllIsGoldSection by remember { mutableStateOf(false) }

    // Active Long-press Feature dialog state
    var activeLongPressFeature by remember { mutableStateOf<com.example.domain.Feature?>(null) }

    // Long-press compact glass context menu state
    var longPressTarget by remember { mutableStateOf<LongPressContextTarget?>(null) }
    var removingFavoriteId by remember { mutableStateOf<String?>(null) }

    // Flying star animation state when adding a tool to Favorites
    var showFlyingStar by remember { mutableStateOf(false) }
    val flyingStarProgress = remember { Animatable(0f) }

    val triggerFlyingStar = {
        if (!animationsDisabled) {
            scope.launch {
                showFlyingStar = true
                flyingStarProgress.snapTo(0f)
                flyingStarProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                delay(250L)
                showFlyingStar = false
            }
        }
    }

    val greeting = remember {
        val hour = LocalTime.now().hour
        when {
            hour < 12 -> "Good morning"
            hour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    val weatherSnap = solunarState.weatherSnapshot
    val weatherBadge = remember(weatherSnap, solunarState.useCelsius) {
        if (weatherSnap == null) {
            "LIVE"
        } else {
            val t = if (solunarState.useCelsius) {
                weatherSnap.currentTempC.roundToInt()
            } else {
                (weatherSnap.currentTempC * 9.0 / 5.0 + 32.0).roundToInt()
            }
            "${t}°${if (solunarState.useCelsius) "C" else "F"}"
        }
    }

    // Resolve Daily Spotlight tool for Hero Carousel slide (Free users only)
    val spotlightTool = remember(isPro, userPreferences.spotlightToolId, supportedEnabledTools) {
        if (isPro) {
            null
        } else {
            val proTools = supportedEnabledTools.filter { it.isPro }
            proTools.find { it.id == userPreferences.spotlightToolId } ?: proTools.firstOrNull()
        }
    }

    // Build Hero Carousel slides: Favorites first, Daily Spotlight (normal hero slide for Free users),
    // Speaker Cleaner, Frequency, Solunar, Weather, then remaining featured tools
    val heroSlides = remember(
        favoriteTools,
        spotlightTool,
        isPro,
        userPreferences.spotlightStartEpochMs,
        userPreferences.spotlightExpiryEpochMs,
        userPreferences.spotlightDailyDate,
        userPreferences.checkInStreak,
        supportedEnabledTools,
        userPreferences.outdoorInterestsSet,
        solunarState.todayBestWindowLabel,
        solunarState.todayBestScore,
        weatherBadge,
        weatherSnap
    ) {
        val list = mutableListOf<HeroSlideItem>()
        val addedToolIds = mutableSetOf<String>()

        // 1. Favorites first
        favoriteTools.forEach { fav ->
            if (addedToolIds.add(fav.id)) {
                list.add(HeroSlideItem.ToolSlide(fav))
            }
        }

        // 2. Daily Spotlight as a normal Hero slide for Free users ("Free for 1h today: <Title>")
        if (!isPro && spotlightTool != null) {
            val nowMs = System.currentTimeMillis()
            val totalHours = if (userPreferences.checkInStreak >= 7) 2 else 1
            val isRunning = userPreferences.isSpotlightActiveNow(nowMs)
            val isExpiredToday = userPreferences.spotlightDailyDate == java.time.LocalDate.now().toString() &&
                userPreferences.spotlightStartEpochMs > 0L &&
                nowMs >= userPreferences.spotlightExpiryEpochMs
            list.add(
                HeroSlideItem.SpotlightSlide(
                    tool = spotlightTool,
                    shortLabel = "Free for ${totalHours}h today: ${spotlightTool.name}",
                    buttonLabel = when {
                        isRunning -> "Open"
                        isExpiredToday -> "Unlock"
                        else -> "Try ${totalHours}h"
                    },
                    isExpiredToday = isExpiredToday
                )
            )
            addedToolIds.add(spotlightTool.id)
        }

        // 3. Flagship Speaker Cleaner, Frequency & Media Studio slides
        supportedEnabledTools.find { it.id == "speaker_cleaner" }?.let { sc ->
            if (addedToolIds.add(sc.id)) list.add(HeroSlideItem.ToolSlide(sc))
        }
        supportedEnabledTools.find { it.id == "photo_converter" }?.let { pc ->
            if (addedToolIds.add(pc.id)) list.add(HeroSlideItem.ToolSlide(pc))
        }
        supportedEnabledTools.find { it.id == "background_remover" }?.let { bg ->
            if (addedToolIds.add(bg.id)) list.add(HeroSlideItem.ToolSlide(bg))
        }
        supportedEnabledTools.find { it.id == "frequency_tool" }?.let { fq ->
            if (addedToolIds.add(fq.id)) list.add(HeroSlideItem.ToolSlide(fq))
        }

        // 4. Solunar slide (hidden if outdoor interests is empty)
        if (userPreferences.outdoorInterestsSet.isNotEmpty()) {
            list.add(
                HeroSlideItem.SolunarSlide(
                    bestWindow = solunarState.todayBestWindowLabel,
                    score = solunarState.todayBestScore,
                    moonPhase = solunarState.todayReport?.moonPhaseName ?: "Waxing Gibbous"
                )
            )
        }

        // 5. Weather slide showing live temperature
        list.add(
            HeroSlideItem.WeatherSlide(
                tempLabel = weatherBadge,
                conditionLabel = weatherSnap?.conditionLabel ?: "Live Local Forecast & Barometer"
            )
        )

        // 6. Additional available free tools in the Hero carousel
        supportedEnabledTools.filter { !it.isPro }.take(8).forEach { tool ->
            if (addedToolIds.add(tool.id)) {
                list.add(HeroSlideItem.ToolSlide(tool))
            }
        }
        list
    }

    val effectiveTileSizeCode = userPreferences.effectiveHomeTileSize(isPro)
    val effectiveTileStyle = userPreferences.effectiveHomeTileStyle(isPro)
    val effectiveHeroStyle = userPreferences.effectiveHeroCarouselStyle(isPro)
    val effectiveIconSetStyle = userPreferences.effectiveIconSetStyle(isPro)

    val tileSizeDp: Dp = when (effectiveTileSizeCode.uppercase()) {
        "S" -> 104.dp
        "L" -> 136.dp
        else -> 118.dp
    }

    val orderedBlocks = remember(
        userPreferences.homeBlockOrderList,
        userPreferences.disabledHomeBlocksSet,
        isCustomizationUnlocked,
        isPro
    ) {
        val canonicalOrder = listOf(
            "HERO",
            "FAVORITES",
            "RECENTLY_USED",
            "SKY_NOW",
            "DYNE_PREMIUM",
            "CATEGORIES",
            "QUICK_ACCESS"
        )
        val base = if (isCustomizationUnlocked) {
            val raw = userPreferences.homeBlockOrderList.filterNot { userPreferences.disabledHomeBlocksSet.contains(it) }
            // Ensure HERO is always first right under the header and RECENTLY_USED is included
            val withRecent = if (raw.any { it.equals("RECENTLY_USED", ignoreCase = true) }) {
                raw
            } else {
                val favIndex = raw.indexOfFirst { it.equals("FAVORITES", ignoreCase = true) }
                if (favIndex >= 0) {
                    raw.toMutableList().apply { add(favIndex + 1, "RECENTLY_USED") }
                } else {
                    raw + "RECENTLY_USED"
                }
            }
            val heroFirst = listOf("HERO") + withRecent.filterNot { it.equals("HERO", ignoreCase = true) }
            heroFirst
        } else {
            canonicalOrder
        }
        if (isPro) base.filterNot { it.equals("DYNE_PREMIUM", ignoreCase = true) } else base
    }

    // Precompute category section models once so LazyColumn items are lightweight and stable
    val effectiveSectionsJson = userPreferences.effectiveCustomSectionsJson(isPro)
    val hiddenSet = remember(isCustomizationUnlocked, userPreferences.hiddenFeatureIdsCsv, isPro) {
        userPreferences.effectiveHiddenFeatureIdsSet(isPro)
    }
    val perTileSizes = remember(isCustomizationUnlocked, userPreferences.perTileSizeCsv, isPro) {
        com.example.domain.FeatureRegistry.parseTileSizes(userPreferences.effectivePerTileSizeCsv(isPro))
    }
    val categorySectionModels = remember(
        isCustomizationUnlocked,
        effectiveSectionsJson,
        hiddenSet,
        capabilitiesSnapshot
    ) {
        val rawSections = if (isCustomizationUnlocked && effectiveSectionsJson.isNotBlank()) {
            com.example.domain.FeatureRegistry.parseSectionsFromJson(
                effectiveSectionsJson,
                DeviceCapabilities
            ).filter { !it.isHidden }
        } else {
            com.example.domain.FeatureRegistry.getDefaultSections(DeviceCapabilities)
        }
        rawSections.mapNotNull { section ->
            val sectionFeatures = section.featureIds.mapNotNull { fid ->
                com.example.domain.FeatureRegistry.getFeatureById(fid)?.takeIf { feat ->
                    feat.id !in hiddenSet &&
                        com.example.domain.FeatureRegistry.isSupported(feat, capabilitiesSnapshot)
                }
            }
            if (sectionFeatures.isEmpty()) {
                null
            } else {
                val layoutCols = if (isCustomizationUnlocked) section.layoutStyle.columns else 0
                val synthTools = sectionFeatures.map { feat ->
                    RegisteredTool(
                        id = feat.id,
                        name = feat.title,
                        shortBenefit = feat.shortBenefit,
                        description = feat.shortBenefit,
                        icon = feat.icon,
                        category = feat.category,
                        isPro = feat.isPremium,
                        requiredCapabilities = feat.requiredCapabilities
                    )
                }
                CategorySectionUiModel(
                    id = section.id,
                    name = section.name,
                    layoutColumns = layoutCols,
                    features = sectionFeatures,
                    synthTools = synthTools
                )
            }
        }
    }

    val premiumCatalogFeatures = remember(
        isPro,
        capabilitiesSnapshot,
        isCustomizationUnlocked,
        hiddenSet
    ) {
        if (isPro) {
            emptyList()
        } else {
            com.example.domain.FeatureRegistry.premiumFeatures.filter { feat ->
                com.example.domain.FeatureRegistry.isSupported(feat, capabilitiesSnapshot) &&
                    feat.id !in hiddenSet
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        val isNarrowPhone = maxWidth < 380.dp

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            contentPadding = PaddingValues(bottom = bottomInset + DesignTokens.Space16)
        ) {
            // 1. HEADER: logo (40dp), "Dyne" (ExtraBold 28sp, weight(1f), one line, never truncated),
            // and fixed-size icon buttons: search, coin chip (Free only, hidden first on narrow phones), gear, crown (Free only)
            item(key = "home_header", contentType = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = DesignTokens.ScreenMargin,
                            vertical = DesignTokens.Space12
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Space8)
                ) {
                    com.example.DyneLogo(size = 40.dp)

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Dyne",
                            style = type.headline,
                            color = theme.textPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.testTag("home_title_dyne")
                        )
                        if (isPro) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(ProGold)
                                    .testTag("home_pro_gold_dot")
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Space8)
                    ) {
                        GlassIconButton(
                            icon = Icons.Default.Search,
                            contentDescription = "Search features",
                            hazeState = null,
                            onClick = {
                                isSearchBarVisible = true
                            },
                            testTag = "home_search_button"
                        )
                        if (!isPro && !isNarrowPhone) {
                            CoinBalanceTopBarChip(
                                coinBalance = coinBalance,
                                onClick = onOpenRewards,
                                hazeState = null
                            )
                        }
                        GlassIconButton(
                            icon = Icons.Default.Settings,
                            contentDescription = "Customize Home & Settings",
                            hazeState = null,
                            onClick = onOpenCustomizeHome,
                            testTag = "home_gear_button"
                        )
                        if (!isPro) {
                            ProCrownTopBarButton(
                                contentDescription = "Unlock Dyne Premium",
                                onClick = onOpenPaywall,
                                hazeState = null,
                                testTag = "home_crown_button"
                            )
                        }
                    }
                }
            }

            // 2. ORDERED HOME SECTIONS (Hero Carousel immediately under Header, then Favorites, Recently used, Sky Now, compact Dyne Premium row, Category sections, Quick Access)
            orderedBlocks.forEach { blockId ->
                when (blockId.lowercase()) {
                    "hero" -> {
                        item(key = "home_hero", contentType = "hero") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = 22.dp)
                            ) {
                                HeroCarouselSection(
                                    slides = heroSlides,
                                    isPro = isPro,
                                    reduceMotion = animationsDisabled,
                                    heroStyle = effectiveHeroStyle,
                                    isToolRewardUnlocked = isToolRewardUnlocked,
                                    onOpenTool = { tool ->
                                        val feat = com.example.domain.FeatureRegistry.resolveFeatureOrPrimaryForTool(tool.id)
                                        if (feat != null) {
                                            onOpenCatalogFeature(feat)
                                        } else {
                                            val unlocked = isPro || !tool.isPro || isToolRewardUnlocked(tool.id)
                                            if (!unlocked) onLockedToolClick(tool) else onSelectTool(tool)
                                        }
                                    },
                                    onActivateSpotlight = { tool, expiredToday ->
                                        if (expiredToday) {
                                            onLockedToolClick(tool)
                                        } else {
                                            onActivateSpotlightTool(tool)
                                        }
                                    },
                                    onOpenSolunar = onHeroActionClick,
                                    onOpenWeather = { onQuickAccessClick(QuickAccessTarget.WEATHER) }
                                )
                            }
                        }
                    }

                    "favorites" -> {
                        item(key = "home_favorites", contentType = "favorites") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = 20.dp)
                            ) {
                                FavoritesSection(
                                    favoriteItems = favoriteTileItems,
                                    removingFavoriteId = removingFavoriteId,
                                    showFlyingStar = showFlyingStar,
                                    isPro = isPro,
                                    isCustomizationUnlocked = isCustomizationUnlocked,
                                    tileSizeDp = tileSizeDp,
                                    showToolNames = userPreferences.showToolNames,
                                    tileStyle = effectiveTileStyle,
                                    iconSetStyle = effectiveIconSetStyle,
                                    isToolRewardUnlocked = { idOrFeatId ->
                                        val feat = com.example.domain.FeatureRegistry.getFeatureById(idOrFeatId)
                                        if (feat != null && featureGate != null) {
                                            featureGate.isCatalogFeatureUnlocked(feat, userPreferences)
                                        } else {
                                            isToolRewardUnlocked(idOrFeatId)
                                        }
                                    },
                                    getToolCountdownProgress = { idOrFeatId ->
                                        val feat = com.example.domain.FeatureRegistry.getFeatureById(idOrFeatId)
                                        getToolCountdownProgress(feat?.id ?: idOrFeatId)
                                    },
                                    getToolCountdownLabel = { idOrFeatId ->
                                        val feat = com.example.domain.FeatureRegistry.getFeatureById(idOrFeatId)
                                        getToolCountdownLabel(feat?.id ?: idOrFeatId)
                                    },
                                    onUnlockFavoritesClick = {
                                        if (featureGate != null) {
                                            featureGate.require(
                                                feature = com.example.billing.PremiumFeature.HOME_CUSTOMIZATION,
                                                customTitle = "Favorites & Home Customization",
                                                customDescription = "Pin unlimited Favorites, customize tiles, folders, and themes with Dyne Premium or a 1-hour unlock."
                                            )
                                        } else {
                                            onOpenPaywall()
                                        }
                                    },
                                    onAddFavoriteClick = onOpenAllFeatures,
                                    onItemClick = { item ->
                                        val feat = com.example.domain.FeatureRegistry.getFeatureById(item.favoriteKey)
                                            ?: com.example.domain.FeatureRegistry.resolveFeatureForToolAndMode(item.tool.id, item.mode?.id)
                                            ?: com.example.domain.FeatureRegistry.resolveFeatureOrPrimaryForTool(item.tool.id)
                                        if (feat != null) {
                                            onOpenCatalogFeature(feat)
                                        } else {
                                            val unlocked = isPro || !item.isLockedPro || isToolRewardUnlocked(item.tool.id)
                                            if (!unlocked) {
                                                onLockedToolClick(item.tool)
                                            } else {
                                                onSelectToolWithMode(item.tool, item.mode?.id)
                                            }
                                        }
                                    },
                                    onItemLongClick = { item, anchorOffset ->
                                        FeedbackHelper.hapticTick(context)
                                        longPressTarget = LongPressContextTarget(
                                            favoriteKey = item.favoriteKey,
                                            tool = item.tool,
                                            mode = item.mode,
                                            fromFavorites = true,
                                            anchorRootOffset = anchorOffset
                                        )
                                    }
                                )
                            }
                        }
                    }

                    "recently_used" -> {
                        if (recentTools.isNotEmpty()) {
                            item(key = "home_recently_used", contentType = "recently_used") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                        .padding(bottom = 20.dp)
                                ) {
                                    RecentlyUsedSection(
                                        recentTools = recentTools,
                                        onToolClick = { tool ->
                                            val feat = com.example.domain.FeatureRegistry.getFeatureById(tool.id)
                                                ?: com.example.domain.FeatureRegistry.resolveFeatureOrPrimaryForTool(tool.id)
                                            if (feat != null) {
                                                onOpenCatalogFeature(feat)
                                            } else {
                                                val unlocked = isPro || !tool.isPro || isToolRewardUnlocked(tool.id)
                                                if (!unlocked) onLockedToolClick(tool) else onSelectTool(tool)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    "sky_now" -> {
                        item(key = "home_sky_now", contentType = "sky_now") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = 20.dp)
                            ) {
                                SkyNowCompactDomeCard(
                                    sunMoonState = sunMoonState,
                                    onClick = onSkyNowClick
                                )
                            }
                        }
                    }

                    "dyne_premium" -> {
                        // Quiet upsell for Free users only: ONE compact row "Dyne Premium - N features - See all" with a small crown.
                        // Completely hidden when isPro.
                        if (!isPro && premiumCatalogFeatures.isNotEmpty()) {
                            item(key = "home_dyne_premium_row", contentType = "dyne_premium") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                        .padding(bottom = 20.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(theme.surface)
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                                            .dyneClickable(
                                                testTag = "dyne_premium_see_all",
                                                onClick = {
                                                    seeAllIsGoldSection = true
                                                    seeAllDialogState = Triple(
                                                        "Dyne Premium",
                                                        "${premiumCatalogFeatures.size} Premium features",
                                                        premiumCatalogFeatures
                                                    )
                                                }
                                            )
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.WorkspacePremium,
                                                contentDescription = null,
                                                tint = ProGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Dyne Premium - ${premiumCatalogFeatures.size} features - See all",
                                                style = type.body.copy(fontWeight = FontWeight.Bold),
                                                color = theme.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = theme.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "categories" -> {
                        items(
                            items = categorySectionModels,
                            key = { "cat_${it.id}" },
                            contentType = { "category_section" }
                        ) { sectionModel ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = 22.dp)
                            ) {
                                FreeToolCategorySection(
                                    categoryTitle = sectionModel.name,
                                    tools = sectionModel.synthTools,
                                    layoutColumns = sectionModel.layoutColumns,
                                    tileSizeDp = tileSizeDp,
                                    perTileSizes = perTileSizes,
                                    isPro = isPro,
                                    isFeatureUnlocked = { featId ->
                                        val feat = com.example.domain.FeatureRegistry.getFeatureById(featId)
                                        if (feat == null || !feat.isPremium) true
                                        else isPro ||
                                            (featureGate?.isCatalogFeatureUnlocked(feat, userPreferences) == true)
                                    },
                                    getToolCountdownProgress = getToolCountdownProgress,
                                    getToolCountdownLabel = getToolCountdownLabel,
                                    showToolNames = userPreferences.showToolNames,
                                    tileStyle = effectiveTileStyle,
                                    iconSetStyle = effectiveIconSetStyle,
                                    onSeeAllClick = {
                                        seeAllIsGoldSection = false
                                        seeAllDialogState = Triple(
                                            sectionModel.name,
                                            "${sectionModel.features.size} features in ${sectionModel.name}",
                                            sectionModel.features
                                        )
                                    },
                                    onToolClick = { synthTool ->
                                        val feat = com.example.domain.FeatureRegistry.getFeatureById(synthTool.id)
                                        if (feat != null) {
                                            onOpenCatalogFeature(feat)
                                        }
                                    },
                                    onToolLongClick = { synthTool, _ ->
                                        FeedbackHelper.hapticTick(context)
                                        val feat = com.example.domain.FeatureRegistry.getFeatureById(synthTool.id)
                                        if (feat != null) {
                                            activeLongPressFeature = feat
                                        }
                                    }
                                )
                            }
                        }
                    }

                    "quick_access" -> {
                        item(key = "home_quick_access", contentType = "quick_access") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = 22.dp)
                            ) {
                                QuickAccessBentoSection(
                                    solunarBestWindow = solunarState.todayBestWindowLabel,
                                    solunarScore = solunarState.todayBestScore,
                                    weatherCondition = weatherSnap?.conditionLabel ?: "Open-Meteo",
                                    weatherBadge = weatherBadge,
                                    isPro = isPro,
                                    isToolRewardUnlocked = isToolRewardUnlocked,
                                    getToolCountdownProgress = getToolCountdownProgress,
                                    getToolCountdownLabel = getToolCountdownLabel,
                                    onQuickAccessClick = onQuickAccessClick
                                )
                            }
                        }
                    }
                }
            }

            // 3. BOTTOM COMPACT ROW: "All features (N)" at the bottom of Home
            item(key = "home_all_features_bottom", contentType = "footer") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 12.dp)
                        .testTag("home_live_feature_counter_chip")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(theme.surface)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                            .dyneClickable(
                                testTag = "home_all_features_button",
                                onClick = onOpenAllFeatures
                            )
                            .padding(horizontal = 16.dp, vertical = 13.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                tint = theme.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "All features (${com.example.domain.FeatureRegistry.totalCount})",
                                style = type.body.copy(fontWeight = FontWeight.ExtraBold),
                                color = theme.textPrimary,
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = theme.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Flying star overlay when a tool is added to Favorites
        AnimatedVisibility(
            visible = showFlyingStar,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset {
                    val yOffset = (260f * (1f - flyingStarProgress.value) + 120f).roundToInt()
                    IntOffset(0, yOffset)
                }
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isPro) theme.accent else ProGold)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Added to Favorites", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
            }
        }

        // Compact 28dp blurred glass context menu anchored near the long-pressed tile
        longPressTarget?.let { target ->
            val isFav = favoriteIds.contains(target.favoriteKey)
            val menuWidthDp = 216.dp
            val menuHeightEstimateDp = 250.dp
            val menuWidthPx = with(density) { menuWidthDp.toPx() }
            val menuHeightPx = with(density) { menuHeightEstimateDp.toPx() }
            val horizontalPadPx = with(density) { 16.dp.toPx() }
            val verticalPadPx = with(density) { 80.dp.toPx() }

            val anchoredX = target.anchorRootOffset.x
                .coerceIn(horizontalPadPx, (screenWidthPx - menuWidthPx - horizontalPadPx).coerceAtLeast(horizontalPadPx))
                .roundToInt()
            val anchoredY = target.anchorRootOffset.y
                .coerceIn(verticalPadPx, (screenHeightPx - menuHeightPx - verticalPadPx).coerceAtLeast(verticalPadPx))
                .roundToInt()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            longPressTarget = null
                        }
                    }
            ) {
                GlassSurface(
                    hazeState = hazeState,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .offset { IntOffset(anchoredX, anchoredY) }
                        .width(menuWidthDp)
                        .shadow(20.dp, RoundedCornerShape(28.dp))
                        .testTag("home_glass_context_menu")
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Consume taps inside menu */ }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Compact header showing icon + name
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = target.mode?.icon ?: target.tool.icon,
                                contentDescription = null,
                                tint = theme.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (target.mode != null) "${target.tool.name} • ${target.mode.title}" else target.tool.name,
                                style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                                color = theme.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        HorizontalDivider(color = theme.textSecondary.copy(alpha = 0.14f))

                        // 1. Favorite
                        ContextMenuIconRow(
                            icon = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                            label = if (isFav) "Unfavorite" else "Favorite",
                            iconTint = if (isPro) theme.accent else ProGold,
                            onClick = {
                                FeedbackHelper.hapticTick(context)
                                val favKey = target.favoriteKey
                                longPressTarget = null
                                if (isFav) {
                                    removingFavoriteId = favKey
                                    scope.launch {
                                        delay(200L)
                                        onToggleFavoriteTool(favKey)
                                        removingFavoriteId = null
                                    }
                                } else {
                                    onToggleFavoriteTool(favKey)
                                    triggerFlyingStar()
                                }
                            }
                        )

                        // 2. Move left
                        ContextMenuIconRow(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            label = if (isCustomizationUnlocked) "Move left" else "Move left (Pro)",
                            iconTint = if (isCustomizationUnlocked) theme.textPrimary else ProGold,
                            onClick = {
                                FeedbackHelper.hapticTick(context)
                                val favKey = target.favoriteKey
                                longPressTarget = null
                                if (isCustomizationUnlocked) {
                                    if (!isFav) onToggleFavoriteTool(favKey)
                                    onMoveFavoriteTool(favKey, -1)
                                } else {
                                    featureGate?.require(
                                        feature = com.example.billing.PremiumFeature.HOME_CUSTOMIZATION,
                                        customTitle = "Reorder & Move with Premium"
                                    ) ?: onOpenPaywall()
                                }
                            }
                        )

                        // 3. Move right
                        ContextMenuIconRow(
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            label = if (isCustomizationUnlocked) "Move right" else "Move right (Pro)",
                            iconTint = if (isCustomizationUnlocked) theme.textPrimary else ProGold,
                            onClick = {
                                FeedbackHelper.hapticTick(context)
                                val favKey = target.favoriteKey
                                longPressTarget = null
                                if (isCustomizationUnlocked) {
                                    if (!isFav) onToggleFavoriteTool(favKey)
                                    onMoveFavoriteTool(favKey, 1)
                                } else {
                                    featureGate?.require(
                                        feature = com.example.billing.PremiumFeature.HOME_CUSTOMIZATION,
                                        customTitle = "Reorder & Move with Premium"
                                    ) ?: onOpenPaywall()
                                }
                            }
                        )

                        // 4. Hide
                        ContextMenuIconRow(
                            icon = Icons.Default.VisibilityOff,
                            label = if (isCustomizationUnlocked) "Hide" else "Hide (Pro)",
                            iconTint = if (isCustomizationUnlocked) theme.textSecondary else ProGold,
                            onClick = {
                                FeedbackHelper.hapticTick(context)
                                val toolId = target.tool.id
                                longPressTarget = null
                                if (isCustomizationUnlocked) {
                                    onHideTool(toolId)
                                } else {
                                    featureGate?.require(
                                        feature = com.example.billing.PremiumFeature.HOME_CUSTOMIZATION,
                                        customTitle = "Hide Tiles with Premium"
                                    ) ?: onOpenPaywall()
                                }
                            }
                        )

                        // 5. Open
                        ContextMenuIconRow(
                            icon = Icons.Default.OpenInNew,
                            label = "Open",
                            iconTint = theme.accent,
                            onClick = {
                                FeedbackHelper.hapticTick(context)
                                val tool = target.tool
                                val modeId = target.mode?.id
                                val feat = com.example.domain.FeatureRegistry.getFeatureById(target.favoriteKey)
                                    ?: com.example.domain.FeatureRegistry.resolveFeatureForToolAndMode(tool.id, modeId)
                                    ?: com.example.domain.FeatureRegistry.resolveFeatureOrPrimaryForTool(tool.id)
                                longPressTarget = null
                                if (feat != null) {
                                    onOpenCatalogFeature(feat)
                                } else {
                                    val isLocked = (tool.isPro || target.mode?.isPremium == true) && !isPro && !isToolRewardUnlocked(tool.id)
                                    if (isLocked) {
                                        onLockedToolClick(tool)
                                    } else {
                                        onSelectToolWithMode(tool, modeId)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // "See all (N)" or "See all (M)" full grid dialog
        seeAllDialogState?.let { (dialogTitle, dialogSubtitle, dialogFeatures) ->
            CategorySeeAllGridDialog(
                title = dialogTitle,
                subtitle = dialogSubtitle,
                isGoldPremiumSection = seeAllIsGoldSection && !isPro,
                features = dialogFeatures,
                preferences = userPreferences,
                featureGate = featureGate,
                isPro = isPro,
                onDismiss = { seeAllDialogState = null },
                onFeatureClick = { feat ->
                    seeAllDialogState = null
                    onOpenCatalogFeature(feat)
                },
                onFeatureLongPress = { feat ->
                    seeAllDialogState = null
                    activeLongPressFeature = feat
                }
            )
        }

        // Long-press menu for FeatureRegistry tiles
        activeLongPressFeature?.let { feat ->
            val currentSections = remember(userPreferences.customSectionsJson, capabilitiesSnapshot) {
                com.example.domain.FeatureRegistry.parseSectionsFromJson(
                    userPreferences.customSectionsJson,
                    DeviceCapabilities
                )
            }
            val currentTileSizes = remember(userPreferences.perTileSizeCsv) {
                com.example.domain.FeatureRegistry.parseTileSizes(userPreferences.perTileSizeCsv)
            }
            val isFav = favoriteIds.any {
                it == feat.id ||
                    com.example.domain.FeatureRegistry.resolveFeatureOrPrimaryForTool(it)?.id == feat.id
            }
            val isPinnedTop = feat.id in userPreferences.pinnedTopFeatureIdsList

            FeatureTileLongPressDialog(
                feature = feat,
                isFavorite = isFav,
                currentTileSize = currentTileSizes[feat.id] ?: com.example.domain.FeatureTileSize.MEDIUM,
                isPinnedTop = isPinnedTop,
                isCustomizationUnlocked = isCustomizationUnlocked,
                sections = currentSections,
                onDismiss = { activeLongPressFeature = null },
                onOpen = {
                    onOpenCatalogFeature(feat)
                },
                onToggleFavorite = {
                    if (settingsViewModel != null) {
                        settingsViewModel.toggleFavoriteFeature(
                            context = context,
                            featureId = feat.id,
                            isCustomizationUnlocked = isCustomizationUnlocked,
                            onLimitReached = {
                                featureGate?.require(
                                    feature = com.example.billing.PremiumFeature.HOME_CUSTOMIZATION,
                                    customTitle = "Favorites with Premium",
                                    customDescription = "Pin and reorder Favorites on Home with Dyne Premium or a 1-hour pass."
                                ) ?: onOpenPaywall()
                            }
                        )
                        if (!isFav && isCustomizationUnlocked) {
                            triggerFlyingStar()
                        }
                    } else {
                        onToggleFavoriteTool(feat.id)
                    }
                },
                onHideFeature = {
                    settingsViewModel?.setHiddenFeatures(userPreferences.hiddenFeatureIdsSet + feat.id)
                    settingsViewModel?.showStatusMessage("Hid \"${feat.title}\" from Home")
                },
                onMoveToSection = { targetSec ->
                    val updated = currentSections.map { sec ->
                        val cleaned = sec.featureIds.filterNot { it == feat.id }
                        if (sec.id == targetSec.id) {
                            sec.copy(featureIds = (listOf(feat.id) + cleaned).distinct())
                        } else {
                            sec.copy(featureIds = cleaned)
                        }
                    }
                    settingsViewModel?.saveCustomSections(updated)
                    settingsViewModel?.showStatusMessage("Moved \"${feat.title}\" to ${targetSec.name}")
                },
                onResizeTile = { newSize ->
                    settingsViewModel?.setPerTileSize(feat.id, newSize)
                    settingsViewModel?.showStatusMessage("Set \"${feat.title}\" tile size to ${newSize.label}")
                },
                onTogglePinTop = {
                    val nextPinned = if (isPinnedTop) {
                        userPreferences.pinnedTopFeatureIdsList - feat.id
                    } else {
                        userPreferences.pinnedTopFeatureIdsList + feat.id
                    }
                    settingsViewModel?.setPinnedTopFeatures(nextPinned)
                },
                onPinLauncherShortcut = {
                    val ok = com.example.domain.FeatureRegistry.requestPinFeatureShortcut(context, feat)
                    if (!ok) {
                        settingsViewModel?.showStatusMessage("Pinned shortcut for ${feat.title}")
                    }
                },
                onRequireCustomizationUnlock = {
                    if (featureGate != null) {
                        featureGate.require(
                            feature = com.example.billing.PremiumFeature.HOME_CUSTOMIZATION,
                            customTitle = "Customize Everything with Premium",
                            customDescription = "Hide, move, resize tiles (S/M/L), create custom folders, and save named layouts with Premium or a 1-hour ad unlock."
                        )
                    } else {
                        onOpenPaywall()
                    }
                }
            )
        }

        // Full-screen search overlay with keyboard auto-focus
        if (isSearchBarVisible) {
            HomeFullScreenSearchOverlay(
                query = homeSearchQuery,
                onQueryChange = { homeSearchQuery = it },
                results = if (homeSearchQuery.isBlank()) {
                    remember(capabilitiesSnapshot) {
                        com.example.domain.FeatureRegistry.allFeatures.filter {
                            com.example.domain.FeatureRegistry.isSupported(it, capabilitiesSnapshot)
                        }
                    }
                } else {
                    featureSearchResults
                },
                isPro = isPro,
                userPreferences = userPreferences,
                featureGate = featureGate,
                isToolRewardUnlocked = isToolRewardUnlocked,
                onSelectFeature = { feat ->
                    isSearchBarVisible = false
                    homeSearchQuery = ""
                    onOpenCatalogFeature(feat)
                },
                onClose = {
                    isSearchBarVisible = false
                    homeSearchQuery = ""
                }
            )
        }
    }
}

@Composable
private fun HomeFullScreenSearchOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<com.example.domain.Feature>,
    isPro: Boolean,
    userPreferences: UserPreferences,
    featureGate: com.example.billing.FeatureGate?,
    isToolRewardUnlocked: (String) -> Boolean,
    onSelectFeature: (com.example.domain.Feature) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .padding(horizontal = DesignTokens.ScreenMargin, vertical = DesignTokens.Space12)
            .testTag("home_fullscreen_search_overlay")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Space12),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Close search",
                hazeState = null,
                onClick = onClose,
                testTag = "home_search_close_button"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .height(DesignTokens.ButtonHeight)
                    .clip(RoundedCornerShape(DesignTokens.ButtonRadius))
                    .background(theme.surface)
                    .border(
                        1.dp,
                        theme.accent.copy(alpha = 0.55f),
                        RoundedCornerShape(DesignTokens.ButtonRadius)
                    )
                    .padding(horizontal = DesignTokens.Space16)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search features",
                    tint = theme.accent,
                    modifier = Modifier.size(DesignTokens.IconSize)
                )
                Spacer(modifier = Modifier.width(DesignTokens.Space12))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search ${com.example.domain.FeatureRegistry.totalCount} features...",
                            style = type.body,
                            color = theme.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = type.body.copy(color = theme.textPrimary),
                        cursorBrush = SolidColor(theme.accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag("home_search_input")
                    )
                }
                if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = theme.textSecondary,
                        modifier = Modifier
                            .size(DesignTokens.IconSize)
                            .dyneClickable { onQueryChange("") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(DesignTokens.Space16))

        if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = DesignTokens.Space24),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matching features found",
                    style = type.body,
                    color = theme.textSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Space8),
                contentPadding = PaddingValues(bottom = DesignTokens.Space24)
            ) {
                items(
                    items = results,
                    key = { it.id },
                    contentType = { "search_result" }
                ) { feat ->
                    val isFeatUnlocked = isPro ||
                        !feat.isPremium ||
                        (featureGate?.isCatalogFeatureUnlocked(feat, userPreferences) == true) ||
                        isToolRewardUnlocked(feat.toolId)
                    val showProMark = feat.isPremium && !isPro
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(DesignTokens.CardRadius))
                            .background(theme.surface)
                            .border(
                                1.dp,
                                if (showProMark) ProGold.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(DesignTokens.CardRadius)
                            )
                            .dyneClickable(
                                testTag = "search_result_${feat.id}",
                                onClick = { onSelectFeature(feat) }
                            )
                            .padding(horizontal = DesignTokens.Space16, vertical = DesignTokens.Space12)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = feat.icon,
                                contentDescription = feat.title,
                                tint = if (showProMark) ProGold else theme.accent,
                                modifier = Modifier.size(DesignTokens.IconSize)
                            )
                            Spacer(modifier = Modifier.width(DesignTokens.Space12))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = feat.title,
                                    style = type.body.copy(fontWeight = FontWeight.ExtraBold),
                                    color = theme.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = feat.shortBenefit,
                                    style = type.caption,
                                    color = theme.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (showProMark && !isFeatUnlocked) {
                            ProCrownChip()
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = theme.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextMenuIconRow(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .dyneClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = type.body.copy(fontWeight = FontWeight.SemiBold),
            color = theme.textPrimary,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroCarouselSection(
    slides: List<HeroSlideItem>,
    isPro: Boolean,
    reduceMotion: Boolean,
    heroStyle: String = "EDITORIAL",
    isToolRewardUnlocked: (String) -> Boolean,
    onOpenTool: (RegisteredTool) -> Unit,
    onActivateSpotlight: (RegisteredTool, Boolean) -> Unit = { tool, _ -> onOpenTool(tool) },
    onOpenSolunar: () -> Unit,
    onOpenWeather: () -> Unit
) {
    if (slides.isEmpty()) return
    val theme = LocalDyneTheme.current
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val cardHeight = when (heroStyle.uppercase()) {
        "COMPACT_PILL" -> 162.dp
        "MINIMAL_SPLIT" -> 182.dp
        else -> 204.dp
    }
    val cardCorner = when (heroStyle.uppercase()) {
        "COMPACT_PILL" -> 38.dp
        "MINIMAL_SPLIT" -> 24.dp
        else -> 32.dp
    }

    // Auto-advance every 4s with a spring, pause on touch, off with Reduce motion / Reduce glass
    LaunchedEffect(pagerState, slides.size, reduceMotion) {
        if (!reduceMotion && slides.size > 1) {
            while (true) {
                delay(4000L)
                if (!pagerState.isScrollInProgress) {
                    val nextPage = (pagerState.currentPage + 1) % slides.size
                    pagerState.animateScrollToPage(
                        page = nextPage,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            key = { index ->
                when (val s = slides[index]) {
                    is HeroSlideItem.ToolSlide -> "tool_${s.tool.id}"
                    is HeroSlideItem.SpotlightSlide -> "spotlight_${s.tool.id}"
                    is HeroSlideItem.SolunarSlide -> "solunar"
                    is HeroSlideItem.WeatherSlide -> "weather"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .testTag("home_hero_card")
        ) { page ->
            when (val slide = slides[page]) {
                is HeroSlideItem.ToolSlide -> {
                    val tool = slide.tool
                    val c1 = Color(tool.heroGradientHex.first)
                    val c2 = Color(tool.heroGradientHex.second)
                    val showProDecoration = tool.isPro && !isPro
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(cardCorner))
                            .background(Brush.linearGradient(listOf(c1, c2)))
                            .border(
                                1.dp,
                                if (showProDecoration) ProGold.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.16f),
                                RoundedCornerShape(cardCorner)
                            )
                            .dyneClickable(onClick = { onOpenTool(tool) })
                            .padding(22.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tool.icon,
                                        contentDescription = tool.name,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                if (showProDecoration) {
                                    ProCrownChip()
                                }
                            }

                            Column {
                                Text(
                                    text = tool.name,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tool.shortBenefit.ifBlank { tool.description },
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Open",
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                is HeroSlideItem.SpotlightSlide -> {
                    val tool = slide.tool
                    val c1 = Color(tool.heroGradientHex.first)
                    val c2 = Color(tool.heroGradientHex.second)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(cardCorner))
                            .background(Brush.linearGradient(listOf(c1, c2)))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(cardCorner)
                            )
                            .dyneClickable(
                                testTag = "home_daily_spotlight_card",
                                onClick = { onActivateSpotlight(tool, slide.isExpiredToday) }
                            )
                            .padding(22.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tool.icon,
                                        contentDescription = tool.name,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.35f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "DAILY SPOTLIGHT",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = slide.shortLabel,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tool.shortBenefit.ifBlank { tool.description },
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = slide.buttonLabel,
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                is HeroSlideItem.SolunarSlide -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(cardCorner))
                            .background(
                                Brush.linearGradient(
                                    listOf(theme.bentoLime, theme.accent)
                                )
                            )
                            .dyneClickable(
                                testTag = "hero_explore_button",
                                onClick = onOpenSolunar
                            )
                            .padding(22.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NightsStay,
                                        contentDescription = null,
                                        tint = BentoTextBlack,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "SOLUNAR • ${slide.moonPhase.uppercase()}",
                                        color = BentoTextBlack,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BentoTextBlack)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${slide.score}/100",
                                        color = theme.bentoLime,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Solunar Forecast",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp,
                                    color = BentoTextBlack
                                )
                                Text(
                                    text = "Best Window: ${slide.bestWindow}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextBlack.copy(alpha = 0.85f)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(BentoTextBlack)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Open",
                                    color = theme.bentoLime,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = theme.bentoLime,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                is HeroSlideItem.WeatherSlide -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(cardCorner))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1E3C72), Color(0xFF2A5298))
                                )
                            )
                            .dyneClickable(onClick = onOpenWeather)
                            .padding(22.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = slide.tempLabel,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 26.sp
                                )
                            }

                            Column {
                                Text(
                                    text = "Live Weather",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = slide.conditionLabel,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Open",
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Page indicator dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(slides.size.coerceAtMost(10)) { index ->
                val active = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (active) 18.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) theme.accent else Color.White.copy(alpha = 0.25f)
                        )
                )
            }
        }
    }
}

@Composable
private fun FavoritesSection(
    favoriteItems: List<FavoriteTileItem>,
    removingFavoriteId: String?,
    showFlyingStar: Boolean,
    isPro: Boolean,
    isCustomizationUnlocked: Boolean = false,
    tileSizeDp: Dp,
    showToolNames: Boolean,
    tileStyle: String = "GLASS",
    iconSetStyle: String = "FILLED",
    isToolRewardUnlocked: (String) -> Boolean,
    getToolCountdownProgress: (String) -> Float,
    getToolCountdownLabel: (String) -> String,
    onUnlockFavoritesClick: () -> Unit = {},
    onAddFavoriteClick: () -> Unit = {},
    onItemClick: (FavoriteTileItem) -> Unit,
    onItemLongClick: (FavoriteTileItem, Offset) -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    if (isCustomizationUnlocked) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = theme.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FAVORITES",
                        style = type.section,
                        color = theme.accent
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (favoriteItems.isNotEmpty()) {
                        Text(
                            text = "Hold to reorder or remove",
                            style = type.caption,
                            color = theme.textSecondary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.surface)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .dyneClickable(
                                testTag = "home_favorites_add_button",
                                onClick = onAddFavoriteClick
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Favorite",
                            tint = theme.accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Add",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.accent
                        )
                    }
                }
            }

            if (favoriteItems.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(theme.surface)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                        .dyneClickable(onClick = onAddFavoriteClick)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "Tap the star on any feature to add it to Favorites",
                        style = type.caption,
                        color = theme.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = theme.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = favoriteItems,
                        key = { "fav_${it.favoriteKey}" },
                        contentType = { "favorite_tile" }
                    ) { item ->
                        val tool = item.tool
                        val isRemoving = removingFavoriteId == item.favoriteKey
                        val rewardUnlocked = !isPro && isToolRewardUnlocked(tool.id)
                        val isUnlocked = isPro || !item.isLockedPro || rewardUnlocked
                        Box(
                            modifier = Modifier
                                .scale(if (isRemoving) 0.65f else 1f)
                                .alpha(if (isRemoving) 0.2f else 1f)
                        ) {
                            SquareDarkToolTile(
                                tool = tool,
                                overrideTitle = item.displayTitle,
                                overrideIcon = item.displayIcon,
                                tileSize = tileSizeDp,
                                showToolName = showToolNames,
                                tileStyle = tileStyle,
                                iconSetStyle = iconSetStyle,
                                isLocked = !isUnlocked,
                                isProTool = item.isLockedPro && !isPro,
                                isRewardUnlocked1h = item.isLockedPro && !isPro && rewardUnlocked,
                                countdownProgress = getToolCountdownProgress(tool.id),
                                countdownLabel = getToolCountdownLabel(tool.id),
                                onClick = { onItemClick(item) },
                                onLongClickWithOffset = { anchorOffset -> onItemLongClick(item, anchorOffset) }
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Free users: slim row "Favorites - Premium" that opens the Unlock sheet (FREE_FAVORITES_LIMIT = 0)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(theme.surface)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                .dyneClickable(
                    testTag = "home_favorites_locked_row",
                    onClick = onUnlockFavoritesClick
                )
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = ProGold,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Favorites - Premium",
                    style = type.body.copy(fontWeight = FontWeight.Bold),
                    color = theme.textPrimary
                )
            }
            ProCrownChip()
        }
    }
}

@Composable
private fun RecentlyUsedSection(
    recentTools: List<RegisteredTool>,
    onToolClick: (RegisteredTool) -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = theme.textSecondary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "RECENTLY USED",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.textSecondary
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = recentTools,
                key = { "recent_${it.id}" },
                contentType = { "recent_chip" }
            ) { tool ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(theme.surface)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .dyneClickable(onClick = { onToolClick(tool) })
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = null,
                        tint = theme.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tool.name,
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.textPrimary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * SKY NOW: compact card with a mini sky dome (sun and moon), azimuth/altitude, moon phase,
 * next sunrise/sunset; tap opens Outdoors > Sun & Moon (the full dome is Premium).
 */
@Composable
private fun SkyNowCompactDomeCard(
    sunMoonState: SunMoonUiState,
    onClick: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val report = sunMoonState.dailyReport
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.US) }
    val sunriseStr = report.sunrise?.format(timeFormatter) ?: "6:18 AM"
    val sunsetStr = report.sunset?.format(timeFormatter) ?: "7:42 PM"

    val sunAzDeg = sunMoonState.sunPosition.azimuthDeg
    val sunAltDeg = sunMoonState.sunPosition.altitudeDeg
    val moonAzDeg = sunMoonState.moonPosition.azimuthDeg
    val moonAltDeg = sunMoonState.moonPosition.altitudeDeg

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF0E1726), Color(0xFF18263E))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(26.dp))
            .dyneClickable(
                testTag = "home_sky_now_card",
                onClick = onClick
            )
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mini Sky Dome with drawWithCache for static rings & crosshairs
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(Color(0xFF090F1A))
                .padding(8.dp)
                .drawWithCache {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val outerStroke = Stroke(width = 1.5.dp.toPx())
                    val innerStroke = Stroke(width = 1.dp.toPx())
                    val sunRadiusPx = 5.5.dp.toPx()
                    val moonRadiusPx = 4.5.dp.toPx()

                    val sunAltNorm = ((90.0 - sunAltDeg.coerceIn(-15.0, 90.0)) / 105.0).toFloat()
                    val sunAzRad = Math.toRadians(sunAzDeg - 90.0)
                    val sunOffset = Offset(
                        center.x + (radius * sunAltNorm * cos(sunAzRad)).toFloat(),
                        center.y + (radius * sunAltNorm * sin(sunAzRad)).toFloat()
                    )

                    val moonAltNorm = ((90.0 - moonAltDeg.coerceIn(-15.0, 90.0)) / 105.0).toFloat()
                    val moonAzRad = Math.toRadians(moonAzDeg - 90.0)
                    val moonOffset = Offset(
                        center.x + (radius * moonAltNorm * cos(moonAzRad)).toFloat(),
                        center.y + (radius * moonAltNorm * sin(moonAzRad)).toFloat()
                    )

                    onDrawBehind {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.18f),
                            radius = radius,
                            center = center,
                            style = outerStroke
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = radius * 0.5f,
                            center = center,
                            style = innerStroke
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.1f),
                            start = Offset(center.x - radius, center.y),
                            end = Offset(center.x + radius, center.y)
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.1f),
                            start = Offset(center.x, center.y - radius),
                            end = Offset(center.x, center.y + radius)
                        )
                        drawCircle(
                            color = Color(0xFFFFD54F),
                            radius = sunRadiusPx,
                            center = sunOffset
                        )
                        drawCircle(
                            color = Color(0xFF90CAF9),
                            radius = moonRadiusPx,
                            center = moonOffset
                        )
                    }
                }
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SKY NOW",
                    style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                    color = theme.accent
                )
                Text(
                    text = "${report.moonPhaseName} (${report.moonIlluminationPercent}%)",
                    style = type.caption,
                    color = Color(0xFF90CAF9)
                )
            }

            Text(
                text = "Sun Az ${sunAzDeg.roundToInt()}° • Alt ${sunAltDeg.roundToInt()}°",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = "Moon Az ${moonAzDeg.roundToInt()}° • Alt ${moonAltDeg.roundToInt()}°",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 12.sp
            )
            Text(
                text = "Sunrise $sunriseStr • Sunset $sunsetStr",
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 12.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FreeToolCategorySection(
    categoryTitle: String,
    tools: List<RegisteredTool>,
    layoutColumns: Int,
    tileSizeDp: Dp,
    perTileSizes: Map<String, com.example.domain.FeatureTileSize> = emptyMap(),
    isPro: Boolean = false,
    isFeatureUnlocked: (String) -> Boolean = { true },
    getToolCountdownProgress: (String) -> Float = { 0.85f },
    getToolCountdownLabel: (String) -> String = { "1h" },
    showToolNames: Boolean,
    tileStyle: String = "GLASS",
    iconSetStyle: String = "FILLED",
    onSeeAllClick: () -> Unit = {},
    onToolClick: (RegisteredTool) -> Unit,
    onToolLongClick: (RegisteredTool, Offset) -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = categoryTitle.uppercase(),
                style = type.section,
                color = theme.accent
            )
            Text(
                text = "See all (${tools.size})",
                style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                color = theme.accent,
                modifier = Modifier
                    .testTag("category_see_all_${categoryTitle.lowercase().replace(" ", "_")}")
                    .dyneClickable(onClick = onSeeAllClick)
            )
        }

        when (layoutColumns) {
            1 -> {
                // List layout
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tools.forEach { tool ->
                        val unlocked = isFeatureUnlocked(tool.id)
                        var rowOffset by remember { mutableStateOf(Offset.Zero) }
                        val showProDecoration = tool.isPro && !isPro
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coords ->
                                    rowOffset = coords.positionInRoot() + Offset(0f, coords.size.height * 0.5f)
                                }
                                .clip(RoundedCornerShape(18.dp))
                                .background(theme.surface)
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.07f),
                                    RoundedCornerShape(18.dp)
                                )
                                .testTag("tool_tile_${tool.id}")
                                .combinedClickable(
                                    onClick = { onToolClick(tool) },
                                    onLongClick = { onToolLongClick(tool, rowOffset) }
                                )
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(theme.accent.copy(alpha = 0.16f))
                                ) {
                                    Icon(
                                        tool.icon,
                                        contentDescription = tool.name,
                                        tint = theme.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    if (showToolNames) {
                                        Text(tool.name, style = type.title, color = theme.textPrimary)
                                    }
                                    Text(
                                        text = tool.shortBenefit.ifBlank { tool.description },
                                        style = type.caption,
                                        color = theme.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (showProDecoration && !unlocked) {
                                ProCrownChip()
                            } else if (showProDecoration && unlocked) {
                                ProCountdownRing(
                                    progress = getToolCountdownProgress(tool.id),
                                    label = getToolCountdownLabel(tool.id)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = theme.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            2, 3, 4 -> {
                // Multi-column grid layout
                val rows = remember(tools, layoutColumns) { tools.chunked(layoutColumns) }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { rowTools ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowTools.forEach { tool ->
                                val unlocked = isFeatureUnlocked(tool.id)
                                val customSize = perTileSizes[tool.id]
                                val itemSizeDp = when (customSize) {
                                    com.example.domain.FeatureTileSize.SMALL -> 104.dp
                                    com.example.domain.FeatureTileSize.LARGE -> 136.dp
                                    else -> tileSizeDp
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    SquareDarkToolTile(
                                        tool = tool,
                                        tileSize = itemSizeDp,
                                        fillRowWidth = true,
                                        showToolName = showToolNames,
                                        tileStyle = tileStyle,
                                        iconSetStyle = iconSetStyle,
                                        isLocked = tool.isPro && !unlocked && !isPro,
                                        isProTool = tool.isPro && !isPro,
                                        isRewardUnlocked1h = tool.isPro && !isPro && unlocked,
                                        countdownProgress = getToolCountdownProgress(tool.id),
                                        countdownLabel = getToolCountdownLabel(tool.id),
                                        onClick = { onToolClick(tool) },
                                        onLongClickWithOffset = { anchorOffset -> onToolLongClick(tool, anchorOffset) }
                                    )
                                }
                            }
                            repeat(layoutColumns - rowTools.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            else -> {
                // Default horizontal scroll row using LazyRow with stable keys and contentType
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = tools,
                        key = { it.id },
                        contentType = { "feature_tile" }
                    ) { tool ->
                        val unlocked = isFeatureUnlocked(tool.id)
                        val customSize = perTileSizes[tool.id]
                        val itemSizeDp = when (customSize) {
                            com.example.domain.FeatureTileSize.SMALL -> 104.dp
                            com.example.domain.FeatureTileSize.LARGE -> 136.dp
                            else -> tileSizeDp
                        }
                        SquareDarkToolTile(
                            tool = tool,
                            tileSize = itemSizeDp,
                            showToolName = showToolNames,
                            tileStyle = tileStyle,
                            iconSetStyle = iconSetStyle,
                            isLocked = tool.isPro && !unlocked && !isPro,
                            isProTool = tool.isPro && !isPro,
                            isRewardUnlocked1h = tool.isPro && !isPro && unlocked,
                            countdownProgress = getToolCountdownProgress(tool.id),
                            countdownLabel = getToolCountdownLabel(tool.id),
                            onClick = { onToolClick(tool) },
                            onLongClickWithOffset = { anchorOffset -> onToolLongClick(tool, anchorOffset) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAccessBentoSection(
    solunarBestWindow: String,
    solunarScore: Int,
    weatherCondition: String,
    weatherBadge: String,
    isPro: Boolean = false,
    isToolRewardUnlocked: (String) -> Boolean = { false },
    getToolCountdownProgress: (String) -> Float = { 0.85f },
    getToolCountdownLabel: (String) -> String = { "1h" },
    onQuickAccessClick: (QuickAccessTarget) -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "QUICK ACCESS",
            style = type.section,
            color = theme.accent
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BentoQuickAccessCard(
                title = "Speaker Cleaner",
                subtitle = "165 Hz Water Eject",
                metricBadge = "165Hz",
                backgroundColor = Color(0xFF80DEEA),
                icon = com.example.ui.components.CustomToolIcons.SpeakerCleaner,
                onClick = { onQuickAccessClick(QuickAccessTarget.SPEAKER_CLEANER) },
                testTag = "bento_speaker_cleaner",
                modifier = Modifier.weight(1f)
            )
            BentoQuickAccessCard(
                title = "Frequency",
                subtitle = "20 Hz–20 kHz & Packs",
                metricBadge = "48kHz",
                backgroundColor = Color(0xFFFFCC80),
                icon = com.example.ui.components.CustomToolIcons.FrequencyWave,
                onClick = { onQuickAccessClick(QuickAccessTarget.FREQUENCY) },
                testTag = "bento_frequency",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BentoQuickAccessCard(
                title = "Documents",
                subtitle = "Library & PDFs",
                metricBadge = "PDF",
                backgroundColor = theme.bentoCoral,
                icon = Icons.Default.Description,
                onClick = { onQuickAccessClick(QuickAccessTarget.DOCUMENTS) },
                testTag = "bento_documents",
                modifier = Modifier.weight(1f)
            )
            BentoQuickAccessCard(
                title = "QR & Barcode",
                subtitle = "Instant Scanner",
                metricBadge = "13X",
                backgroundColor = theme.bentoSky,
                icon = Icons.Default.QrCodeScanner,
                onClick = { onQuickAccessClick(QuickAccessTarget.QR_BARCODE) },
                testTag = "bento_qr_barcode",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BentoQuickAccessCard(
                title = "Solunar",
                subtitle = solunarBestWindow,
                metricBadge = "$solunarScore/100",
                backgroundColor = theme.bentoLime,
                icon = Icons.Default.NightsStay,
                onClick = { onQuickAccessClick(QuickAccessTarget.SOLUNAR) },
                testTag = "bento_solunar",
                modifier = Modifier.weight(1f)
            )
            BentoQuickAccessCard(
                title = "Weather",
                subtitle = weatherCondition,
                metricBadge = weatherBadge,
                backgroundColor = theme.bentoLavender,
                icon = Icons.Default.Cloud,
                onClick = { onQuickAccessClick(QuickAccessTarget.WEATHER) },
                testTag = "bento_weather",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BentoQuickAccessCard(
                title = "Media Studio",
                subtitle = "Photo, BG, Audio, Video & PDF",
                metricBadge = "ON-DEVICE",
                backgroundColor = Color(0xFFFF80AB),
                icon = Icons.Default.GraphicEq,
                onClick = { onQuickAccessClick(QuickAccessTarget.MEDIA_STUDIO) },
                testTag = "bento_media_studio",
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Quick Access cards (clean when isPro; small crown/countdown when Free)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val plannerReward = !isPro && isToolRewardUnlocked("outdoor_planner")
            val calReward = !isPro && isToolRewardUnlocked("sun_moon_calendar")
            BentoQuickAccessCard(
                title = "Outdoor Planner",
                subtitle = "7-Day Rank & Best Day",
                metricBadge = "7D",
                backgroundColor = Color(0xFFA7F3D0),
                icon = Icons.Default.WbSunny,
                isProCard = !isPro,
                isRewardUnlocked1h = plannerReward,
                countdownProgress = getToolCountdownProgress("outdoor_planner"),
                countdownLabel = getToolCountdownLabel("outdoor_planner"),
                onClick = { onQuickAccessClick(QuickAccessTarget.OUTDOOR_PLANNER) },
                testTag = "bento_outdoor_planner",
                modifier = Modifier.weight(1f)
            )
            BentoQuickAccessCard(
                title = "Moon Calendar",
                subtitle = "Monthly Phases & Dome",
                metricBadge = "30D",
                backgroundColor = Color(0xFFBAE6FD),
                icon = Icons.Default.NightsStay,
                isProCard = !isPro,
                isRewardUnlocked1h = calReward,
                countdownProgress = getToolCountdownProgress("sun_moon_calendar"),
                countdownLabel = getToolCountdownLabel("sun_moon_calendar"),
                onClick = { onQuickAccessClick(QuickAccessTarget.SUN_MOON_CALENDAR) },
                testTag = "bento_sun_moon_calendar",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val widgetsReward = !isPro && isToolRewardUnlocked("home_widgets")
            val styleReward = !isPro && isToolRewardUnlocked("custom_style_studio")
            BentoQuickAccessCard(
                title = "Home Widgets",
                subtitle = "5 Glance Widgets",
                metricBadge = "5X",
                backgroundColor = Color(0xFFFDE68A),
                icon = Icons.Default.GridView,
                isProCard = !isPro,
                isRewardUnlocked1h = widgetsReward,
                countdownProgress = getToolCountdownProgress("home_widgets"),
                countdownLabel = getToolCountdownLabel("home_widgets"),
                onClick = { onQuickAccessClick(QuickAccessTarget.HOME_WIDGETS) },
                testTag = "bento_home_widgets",
                modifier = Modifier.weight(1f)
            )
            BentoQuickAccessCard(
                title = "Style Studio",
                subtitle = "Accent, Tiles & 8 Themes",
                metricBadge = "8X",
                backgroundColor = Color(0xFFDDD6FE),
                icon = Icons.Default.Settings,
                isProCard = !isPro,
                isRewardUnlocked1h = styleReward,
                countdownProgress = getToolCountdownProgress("custom_style_studio"),
                countdownLabel = getToolCountdownLabel("custom_style_studio"),
                onClick = { onQuickAccessClick(QuickAccessTarget.CUSTOM_STYLE_STUDIO) },
                testTag = "bento_style_studio",
                modifier = Modifier.weight(1f)
            )
        }

        val alertsReward = !isPro && isToolRewardUnlocked("outdoor_alerts")
        BentoQuickAccessCard(
            title = "Outdoor Alerts",
            subtitle = "Solunar, Golden Hour & Rain Soon",
            metricBadge = "LIVE",
            backgroundColor = Color(0xFFFED7AA),
            icon = Icons.Default.Notifications,
            isProCard = !isPro,
            isRewardUnlocked1h = alertsReward,
            countdownProgress = getToolCountdownProgress("outdoor_alerts"),
            countdownLabel = getToolCountdownLabel("outdoor_alerts"),
            onClick = { onQuickAccessClick(QuickAccessTarget.OUTDOOR_ALERTS) },
            testTag = "bento_outdoor_alerts",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SquareDarkToolTile(
    tool: RegisteredTool,
    overrideTitle: String? = null,
    overrideIcon: ImageVector? = null,
    tileSize: Dp = 118.dp,
    fillRowWidth: Boolean = false,
    showToolName: Boolean = true,
    tileStyle: String = "GLASS",
    iconSetStyle: String = "FILLED",
    isLocked: Boolean,
    isProTool: Boolean,
    isRewardUnlocked1h: Boolean,
    countdownProgress: Float = 0.85f,
    countdownLabel: String = "1h",
    onClick: () -> Unit,
    onLongClickWithOffset: ((Offset) -> Unit)? = null
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val displayTitle = overrideTitle ?: tool.name
    val displayIcon = overrideIcon ?: tool.icon

    var tileAnchorOffset by remember { mutableStateOf(Offset.Zero) }
    var textSizeSp by remember(displayTitle) { mutableFloatStateOf(15f) }

    val resolvedSurfaceBg = when (tileStyle.uppercase()) {
        "SOLID" -> theme.elevated
        "OUTLINE" -> Color.Transparent
        else -> theme.surface
    }
    val tileBorderModifier = when {
        tileStyle.equals("OUTLINE", ignoreCase = true) -> Modifier.border(
            width = 1.5.dp,
            color = theme.accent.copy(alpha = 0.55f),
            shape = RoundedCornerShape(DesignTokens.CardRadius)
        )
        else -> Modifier.border(
            width = 1.dp,
            color = if (theme.isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.06f),
            shape = RoundedCornerShape(DesignTokens.CardRadius)
        )
    }

    val effectiveSize = tileSize.coerceAtLeast(DesignTokens.TileMinWidth)
    val sizeModifier = if (fillRowWidth) {
        Modifier
            .fillMaxWidth()
            .widthIn(min = DesignTokens.TileMinWidth)
            .height(effectiveSize.coerceAtLeast(112.dp))
    } else {
        Modifier
            .widthIn(min = DesignTokens.TileMinWidth)
            .size(width = effectiveSize.coerceAtLeast(108.dp), height = effectiveSize.coerceAtLeast(112.dp))
    }

    Box(
        modifier = sizeModifier
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                tileAnchorOffset = Offset(pos.x, pos.y + coords.size.height * 0.45f)
            }
            .clip(RoundedCornerShape(24.dp))
            .background(resolvedSurfaceBg)
            .then(tileBorderModifier)
            .testTag("tool_tile_${tool.id}")
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongClickWithOffset?.invoke(tileAnchorOffset) }
            )
            .padding(12.dp)
    ) {
        when {
            isRewardUnlocked1h -> {
                ProCountdownRing(
                    progress = countdownProgress,
                    label = countdownLabel,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
            isProTool && isLocked -> {
                ProCrownChip(
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }

        Column(
            verticalArrangement = if (showToolName) Arrangement.SpaceBetween else Arrangement.Center,
            horizontalAlignment = if (showToolName) Alignment.Start else Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            val iconBadgeBg = when (iconSetStyle.uppercase()) {
                "OUTLINED" -> Color.Transparent
                "DUOTONE" -> theme.accent.copy(alpha = 0.28f)
                "MINIMAL" -> theme.elevated
                else -> theme.accent.copy(alpha = if (theme.isDark) 0.14f else 0.25f)
            }
            val iconBadgeBorder = if (iconSetStyle.equals("OUTLINED", ignoreCase = true)) {
                Modifier.border(1.5.dp, theme.accent.copy(alpha = 0.7f), CircleShape)
            } else {
                Modifier
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBadgeBg)
                    .then(iconBadgeBorder)
            ) {
                Icon(
                    imageVector = displayIcon,
                    contentDescription = displayTitle,
                    tint = if (theme.isDark) theme.accent else BentoTextBlack,
                    modifier = Modifier.size(DesignTokens.IconSize)
                )
            }

            if (showToolName) {
                Text(
                    text = displayTitle,
                    style = type.label.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = textSizeSp.sp,
                        lineHeight = (textSizeSp + 2.5f).sp
                    ),
                    color = theme.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { layoutResult ->
                        if (layoutResult.hasVisualOverflow && textSizeSp > 13f) {
                            textSizeSp = (textSizeSp - 0.75f).coerceAtLeast(13f)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BentoQuickAccessCard(
    title: String,
    subtitle: String,
    metricBadge: String,
    backgroundColor: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    isProCard: Boolean = false,
    isRewardUnlocked1h: Boolean = false,
    countdownProgress: Float = 0.85f,
    countdownLabel: String = "1h",
    modifier: Modifier = Modifier
) {
    val type = LocalDyneType.current

    Box(
        modifier = modifier
            .height(126.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(backgroundColor)
            .drawWithCache {
                val radiusPx = 52.dp.toPx()
                val circleCenter = Offset(size.width * 0.9f, size.height * 0.15f)
                val circleColor = BentoTextBlack.copy(alpha = 0.06f)
                onDrawBehind {
                    drawCircle(
                        color = circleColor,
                        radius = radiusPx,
                        center = circleCenter
                    )
                }
            }
            .dyneClickable(
                targetScale = 0.96f,
                testTag = testTag,
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(BentoTextBlack.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = BentoTextBlack,
                        modifier = Modifier.size(20.dp)
                    )
                }

                when {
                    isRewardUnlocked1h -> {
                        ProCountdownRing(
                            progress = countdownProgress,
                            label = countdownLabel
                        )
                    }
                    isProCard -> {
                        ProCrownChip()
                    }
                    else -> {
                        Text(
                            text = metricBadge,
                            style = type.gaugeSmall,
                            color = BentoTextBlack.copy(alpha = 0.72f)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = title,
                    style = type.title.copy(fontWeight = FontWeight.ExtraBold),
                    color = BentoTextBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = BentoTextBlack.copy(alpha = 0.76f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

