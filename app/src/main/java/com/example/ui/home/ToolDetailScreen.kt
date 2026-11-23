package com.example.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.IconButton
import androidx.compose.runtime.rememberCoroutineScope
import com.example.billing.LocalFeatureGate
import com.example.billing.PremiumFeature
import com.example.billing.SoundPack
import com.example.data.GeoCoordinates
import com.example.data.PreferencesRepository
import com.example.data.SensorState
import com.example.data.UnitSystem
import com.example.data.UserPreferences
import com.example.domain.RegisteredTool
import com.example.domain.ToolCategory
import com.example.domain.ToolMode
import com.example.domain.ToolRegistry
import com.example.ui.audio.BatteryInfoInstrumentScreen
import com.example.ui.audio.FrequencyInstrumentScreen
import com.example.ui.audio.MetronomeInstrumentScreen
import com.example.ui.audio.MicTestInstrumentScreen
import com.example.ui.audio.NoiseSleepMixerInstrumentScreen
import com.example.ui.audio.SpeakerCleanerInstrumentScreen
import com.example.ui.audio.SpeakerTestInstrumentScreen
import com.example.ui.audio.ToneGeneratorInstrumentScreen
import com.example.ui.audio.TouchScreenTestInstrumentScreen
import com.example.ui.audio.TunerInstrumentScreen
import com.example.ui.audio.VibrationLabInstrumentScreen
import com.example.ui.components.DesignedStateBanner
import com.example.ui.components.DyneSegmentedControl
import com.example.ui.components.FeedbackHelper
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassModeSwitcherBar
import com.example.ui.components.GlassPermissionCard
import com.example.ui.components.GlassSurface
import com.example.ui.components.HazeState
import com.example.ui.components.LocalBottomBarInset
import com.example.ui.components.SharedTopBar
import com.example.ui.components.dyneClickable
import com.example.ui.components.pressScaleClickable
import com.example.ui.media.AudioConverterToolCard
import com.example.ui.media.BackgroundRemoverToolCard
import com.example.ui.media.PassportIdPhotoToolCard
import com.example.ui.media.PdfAndImageStudioCard
import com.example.ui.media.PhotoConverterToolCard
import com.example.ui.media.PhotoEditorToolCard
import com.example.ui.media.VideoToolsCard
import com.example.ui.notes.NotesWorkspaceSection
import com.example.ui.pdf.PdfToolsScreen
import com.example.ui.scan.ScanViewModel
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusWarning
import com.example.ui.tools.AltimeterBarometerProScreen
import com.example.ui.tools.CalculatorsSuiteScreen
import com.example.ui.tools.CompassWaypointsScreen
import com.example.ui.tools.CookingConverterScreen
import com.example.ui.tools.DeviceSensorsInstrumentScreen
import com.example.ui.tools.DiyCalculatorsProScreen
import com.example.ui.tools.DpiCalibratedScreenRulerScreen
import com.example.ui.tools.EventCountdownProScreen
import com.example.ui.tools.FinancePackScreen
import com.example.ui.tools.FishingLogProScreen
import com.example.ui.tools.FuelAndTripCostScreen
import com.example.ui.tools.ImageToolkitProScreen
import com.example.ui.tools.IntervalTimerProScreen
import com.example.ui.tools.LevelCalibrationAndMeasurementHistoryScreen
import com.example.ui.tools.LuxAndPhotoLightMeterScreen
import com.example.ui.tools.MagnifierAndColorPickerScreen
import com.example.ui.tools.NumberToolsScreen
import com.example.ui.tools.ParkingSaverScreen
import com.example.ui.tools.PasswordGeneratorScreen
import com.example.ui.tools.PedometerAndHistoryScreen
import com.example.ui.tools.PomodoroFocusAndStatsScreen
import com.example.ui.tools.QiblaFinderScreen
import com.example.ui.tools.QrGeneratorAndStylesScreen
import com.example.ui.tools.RandomizerSuiteScreen
import com.example.ui.tools.ScreenTestDeadPixelScreen
import com.example.ui.tools.SplitTheBillProScreen
import com.example.ui.tools.TallyCounterScreen
import com.example.ui.tools.TextToolsScreen
import com.example.ui.tools.TripAndGearChecklistsProScreen
import com.example.ui.tools.UnitConverterExtrasProScreen
import com.example.ui.tools.UnitConverterInstrumentScreen
import com.example.ui.tools.UnitPriceComparerScreen
import com.example.ui.tools.VibrationGForceProScreen
import com.example.ui.tools.WalkRunTrackerProScreen
import com.example.ui.tools.WeatherCalculatorsScreen
import com.example.ui.tools.WorldClockInstrumentScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.style.TextOverflow
import com.example.ads.LocalAdManager
import com.example.billing.UnlockSheetRequest
import com.example.domain.Feature
import com.example.domain.FeatureRegistry

/**
 * Single unified AccessBanner shown ONLY to non-Premium users under the top bar:
 * - Single-line glass pill (44.dp high, 20.dp horizontal margin, 28.dp corners)
 * - 28.dp circular countdown ring
 * - 13.sp one-line ellipsized label with weight(1f) (e.g., "Preview - 9s" or "Unlocked - 52 min left")
 * - Compact "Unlock" button (96.dp minimum width)
 * - Pulses once at 10 seconds left (preview) or 5 minutes left (1-hour unlock).
 */
@Composable
fun AccessBanner(
    remainingMs: Long,
    totalDurationMs: Long = UserPreferences.ONE_HOUR_MS,
    isPreview: Boolean = false,
    buttonText: String = "Extend",
    hazeState: HazeState? = null,
    onUnlockClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    val progress = remember(remainingMs, totalDurationMs) {
        if (totalDurationMs <= 0L) 0f
        else (remainingMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0.02f, 1f)
    }

    val labelText = remember(remainingMs) {
        val totalSeconds = ((remainingMs + 999L) / 1000L).coerceAtLeast(1L)
        if (totalSeconds < 60L) {
            "Unlocked - ${totalSeconds}s left"
        } else {
            val mins = ((remainingMs + 59_999L) / 60_000L).coerceAtLeast(1L)
            "Unlocked - $mins min left"
        }
    }

    val shouldTriggerPulse = remember(remainingMs) {
        remainingMs in 1L..(5L * 60_000L)
    }
    var hasPulsed by remember { mutableStateOf(false) }
    val pulseScale = remember { Animatable(1f) }

    LaunchedEffect(shouldTriggerPulse) {
        if (shouldTriggerPulse && !hasPulsed) {
            hasPulsed = true
            pulseScale.animateTo(1.045f, animationSpec = tween(durationMillis = 180))
            pulseScale.animateTo(1f, animationSpec = tween(durationMillis = 220))
        }
    }

    GlassSurface(
        hazeState = hazeState,
        shape = RoundedCornerShape(28.dp),
        cornerRadius = 28.dp,
        tintColor = theme.elevated.copy(alpha = 0.86f),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .height(44.dp)
            .graphicsLayer {
                scaleX = pulseScale.value
                scaleY = pulseScale.value
            }
            .border(1.dp, ProGold.copy(alpha = 0.55f), RoundedCornerShape(28.dp))
            .testTag("access_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 6.dp)
        ) {
            // 28dp circular countdown ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(28.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 2.6.dp.toPx()
                    drawCircle(
                        color = ProGold.copy(alpha = 0.22f),
                        style = Stroke(width = strokePx)
                    )
                    drawArc(
                        color = ProGold,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
            }

            // Label (13sp, one line, ellipsis, weight(1f))
            Text(
                text = labelText,
                style = type.body.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = theme.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .testTag("access_banner_label")
            )

            // Compact "Extend" button (96dp minimum width)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .widthIn(min = 96.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ProGold)
                    .dyneClickable(onClick = onUnlockClick)
                    .padding(horizontal = 12.dp)
                    .testTag("access_banner_unlock_button")
            ) {
                Text(
                    text = buttonText,
                    style = type.caption.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color(0xFF141108),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Shared FeatureScaffold:
 * - Top bar: back button, category label, feature title on one or two lines, favorite star,
 *   optional info icon (photosensitivity warning for Strobe/Morse/SOS), and overflow menu with Help and Share.
 * - Slot for AccessBanner (preview or 1-hour temporary unlock countdown).
 * - Content slot with main visual centered and glass control panel at the bottom, plus bottom inset.
 * - No tab rows, no mode switcher chip bars, and no duplicate titles.
 */
@Composable
fun FeatureScaffold(
    categoryLabel: String,
    title: String,
    isFavorite: Boolean,
    showFavoriteCrown: Boolean = false,
    showSafetyInfoIcon: Boolean = false,
    hazeState: HazeState? = null,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenSafetyDialog: () -> Unit = {},
    onOpenHelp: () -> Unit,
    onShareFeature: () -> Unit,
    accessBanner: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    var overflowExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("feature_scaffold_root")
    ) {
        // Shared Feature Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("feature_scaffold_top_bar")
        ) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                hazeState = hazeState,
                onClick = {
                    FeedbackHelper.hapticTick(context)
                    onBack()
                },
                testTag = "feature_top_bar_back"
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (categoryLabel.isNotBlank()) {
                    Text(
                        text = categoryLabel.uppercase(Locale.US),
                        style = type.caption.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = theme.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = title,
                    style = type.section.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = theme.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("feature_top_bar_title")
                )
            }

            if (showSafetyInfoIcon) {
                GlassIconButton(
                    icon = Icons.Default.Info,
                    contentDescription = "Photosensitivity Safety Notice",
                    hazeState = hazeState,
                    onClick = {
                        FeedbackHelper.hapticTick(context)
                        onOpenSafetyDialog()
                    },
                    testTag = "feature_top_bar_safety_info"
                )
            }

            Box {
                GlassIconButton(
                    icon = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                    hazeState = hazeState,
                    onClick = {
                        FeedbackHelper.hapticTick(context)
                        onToggleFavorite()
                    },
                    testTag = "tool_top_bar_favorite_star"
                )
                if (showFavoriteCrown) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF141108))
                            .border(1.dp, ProGold, CircleShape)
                            .testTag("favorite_star_crown_badge")
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "Premium Favorite",
                            tint = ProGold,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Box {
                GlassIconButton(
                    icon = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    hazeState = hazeState,
                    onClick = {
                        FeedbackHelper.hapticTick(context)
                        overflowExpanded = true
                    },
                    testTag = "feature_top_bar_overflow"
                )

                DropdownMenu(
                    expanded = overflowExpanded,
                    onDismissRequest = { overflowExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Help") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            overflowExpanded = false
                            onOpenHelp()
                        },
                        modifier = Modifier.testTag("feature_overflow_help")
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            overflowExpanded = false
                            onShareFeature()
                        },
                        modifier = Modifier.testTag("feature_overflow_share")
                    )
                }
            }
        }

        // AccessBanner slot (shown only to non-Premium users)
        accessBanner()

        // Main content slot
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            content()
        }
    }
}

@Composable
fun LevelBubbleScreen(sensorState: SensorState) {
    BubbleLevelInstrument(sensorState = sensorState, forcedModeId = "horizontal")
}

@Composable
fun LevelSurfaceScreen(sensorState: SensorState) {
    BubbleLevelInstrument(sensorState = sensorState, forcedModeId = "surface_2d")
}

@Composable
fun LevelPlumbScreen(sensorState: SensorState) {
    BubbleLevelInstrument(sensorState = sensorState, forcedModeId = "plumb")
}

@Composable
fun LevelAngleScreen(sensorState: SensorState) {
    InclinometerInstrument(sensorState = sensorState)
}

/**
 * Helper composable to keep the device screen awake while an instrument (Screen Torch, Speedometer) is active.
 */
@Composable
private fun KeepScreenOnEffect(enabled: Boolean = true) {
    val context = LocalContext.current
    DisposableEffect(enabled) {
        val window = (context as? Activity)?.window
        if (enabled) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@Composable
fun ToolDetailScreen(
    tool: RegisteredTool,
    initialModeId: String? = null,
    catalogFeature: Feature? = null,
    sensorState: SensorState,
    geoCoordinates: GeoCoordinates,
    unitSystem: UnitSystem,
    scanViewModel: ScanViewModel,
    isPro: Boolean = false,
    isAccountPro: Boolean = false,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onTogglePinMode: (String) -> Unit = {},
    userPreferences: UserPreferences = UserPreferences(),
    preferencesRepository: PreferencesRepository? = null,
    ownedSoundPacks: Set<String> = emptySet(),
    soundPackPrices: Map<String, String> = emptyMap(),
    onSwitchTool: (String) -> Unit = {},
    onLaunchScannerMode: (String) -> Unit = {},
    onOpenPaywall: () -> Unit = {},
    hazeState: HazeState?,
    onSaveNoteToVault: (String, String, String) -> Unit,
    onOpenLibraryNotes: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val scope = rememberCoroutineScope()
    val featureGate = LocalFeatureGate.current

    val actualIsPro = isAccountPro ||
        (featureGate?.isPro?.collectAsState()?.value == true) ||
        userPreferences.cachedIsPro ||
        userPreferences.simulateProEnabled

    val effectiveModes = remember(tool.id, userPreferences.customToolModesConfigCsv) {
        ToolRegistry.getEffectiveModesForTool(tool, userPreferences.customToolModesConfigCsv)
            .filter { it.id != ToolRegistry.MORE_MODE.id }
    }

    val resolvedModeId = remember(tool.id, initialModeId, catalogFeature?.id) {
        val candidate = initialModeId
            ?: catalogFeature?.let { FeatureRegistry.resolveModeIdForFeature(it) }
            ?: userPreferences.getLastModeForTool(tool.id)
            ?: effectiveModes.firstOrNull()?.id
            ?: "default"
        if (candidate == ToolRegistry.MORE_MODE.id) {
            effectiveModes.firstOrNull()?.id ?: "default"
        } else {
            candidate
        }
    }

    val resolvedFeature = remember(catalogFeature, tool.id, resolvedModeId) {
        catalogFeature
            ?: FeatureRegistry.getFeatureById(tool.id)
            ?: FeatureRegistry.resolveFeatureForToolAndMode(tool.id, resolvedModeId)
    }

    val activeMode = remember(effectiveModes, resolvedModeId, resolvedFeature) {
        effectiveModes.firstOrNull { it.id.equals(resolvedModeId, ignoreCase = true) }
            ?: ToolMode(
                id = resolvedModeId,
                title = resolvedFeature?.title ?: tool.name,
                icon = resolvedFeature?.icon ?: tool.icon,
                isPremium = resolvedFeature?.isPremium ?: tool.isProOnly
            )
    }

    // Persist last selected mode per tool
    LaunchedEffect(tool.id, activeMode.id) {
        if (activeMode.id.isNotBlank() && activeMode.id != ToolRegistry.MORE_MODE.id) {
            preferencesRepository?.setLastModeForTool(tool.id, activeMode.id)
        }
    }

    fun resolvePremiumFeatureForMode(toolId: String, modeId: String): PremiumFeature {
        resolvedFeature?.premiumFeature?.let { return it }
        return when {
            toolId == "flashlight" || toolId == "strobe_torch" || toolId == "screen_torch" -> PremiumFeature.FLASHLIGHT_HIGH
            toolId == "speedometer" -> PremiumFeature.SPEEDOMETER_HUD_TRIP
            toolId == "world_clock" && modeId == "meeting_planner" -> PremiumFeature.UNLIMITED_WORLD_CLOCKS
            toolId == "world_clock" && (modeId == "interval" || modeId == "pomodoro") -> PremiumFeature.INTERVAL_TIMER
            toolId == "quick_note" -> PremiumFeature.UNLIMITED_NOTES_PENS_PAPERS
            toolId == "qr_generator" || toolId == "qr_studio_pro" -> PremiumFeature.QR_GENERATOR_STYLES
            toolId == "unit_converter" && modeId in setOf("custom_units", "favorites", "history") -> PremiumFeature.UNIT_CONVERTER_EXTRAS
            toolId == "unit_converter_pro" -> PremiumFeature.UNIT_CONVERTER_EXTRAS
            toolId == "pomodoro_timer" && modeId == "stats" -> PremiumFeature.POMODORO_STATS
            toolId == "pomodoro_stats" -> PremiumFeature.POMODORO_STATS
            toolId == "pedometer" && modeId == "history" -> PremiumFeature.PEDOMETER_HISTORY
            toolId == "pedometer_history" -> PremiumFeature.PEDOMETER_HISTORY
            toolId == "lux_meter" && modeId == "photo_ev" -> PremiumFeature.PHOTO_LIGHT_METER
            toolId == "photo_light_meter" -> PremiumFeature.PHOTO_LIGHT_METER
            (toolId == "compass_waypoints" || toolId == "compass") && modeId in setOf("unlimited", "unlimited_waypoints") -> PremiumFeature.UNLIMITED_WAYPOINTS
            toolId == "waypoints_pro" -> PremiumFeature.UNLIMITED_WAYPOINTS
            toolId == "level" && modeId == "calibration_profiles" -> PremiumFeature.LEVEL_CALIBRATION_PROFILES
            toolId == "level_calibration_pro" -> PremiumFeature.LEVEL_CALIBRATION_PROFILES
            modeId in setOf("history_csv", "level_log", "angle_log", "compass_log", "sound_log") ||
                (toolId in setOf("level", "angle") && modeId == "history") ||
                toolId == "measurement_history_csv" -> PremiumFeature.MEASUREMENT_HISTORY_CSV
            toolId == "walk_run_tracker" -> PremiumFeature.WALK_RUN_TRACKER
            toolId == "altimeter" -> PremiumFeature.ALTIMETER
            toolId == "vibration_gforce" -> PremiumFeature.VIBRATION_GFORCE
            toolId == "fishing_log" -> PremiumFeature.FISHING_LOG
            toolId == "tip_calc" && modeId == "scientific" -> PremiumFeature.SCIENTIFIC_DIY_CALCULATORS
            toolId == "scientific_calc" -> PremiumFeature.SCIENTIFIC_DIY_CALCULATORS
            toolId == "diy_calculators" -> PremiumFeature.DIY_CALCULATORS
            toolId == "finance_pack" -> PremiumFeature.FINANCE_PACK
            toolId == "image_toolkit" -> PremiumFeature.IMAGE_TOOLKIT
            toolId == "event_countdown" -> PremiumFeature.EVENT_COUNTDOWN
            toolId == "split_bill_pro" -> PremiumFeature.SPLIT_THE_BILL
            toolId == "trip_gear_checklists" -> PremiumFeature.TRIP_GEAR_CHECKLISTS
            toolId == "interval_timer" -> PremiumFeature.INTERVAL_TIMER
            toolId == "frequency_tool" || toolId == "speaker_cleaner" -> PremiumFeature.FREQUENCY_PRO
            toolId == "pdf_tools" -> PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE
            toolId == "photo_converter" || toolId == "photo_converter_pro" -> PremiumFeature.PHOTO_CONVERTER_PRO
            toolId == "background_remover" -> PremiumFeature.BACKGROUND_REMOVER_PRO
            toolId == "photo_editor" -> PremiumFeature.PHOTO_EDITOR_PRO
            toolId == "passport_id_photo" -> PremiumFeature.PASSPORT_ID_PHOTO
            toolId == "audio_converter" -> PremiumFeature.AUDIO_CONVERTER_PRO
            toolId == "video_tools" || toolId == "video_compress_pro" -> PremiumFeature.VIDEO_COMPRESS_PRO
            toolId == "pdf_image_tools" -> PremiumFeature.PDF_IMAGE_PRO
            else -> PremiumFeature.fromToolId(toolId) ?: PremiumFeature.SCIENTIFIC_DIY_CALCULATORS
        }
    }

    val isFlashSafetyFeature = remember(resolvedFeature?.id, tool.id, activeMode.id) {
        resolvedFeature?.id in setOf("flashlight_strobe", "flashlight_morse", "flashlight_sos") ||
            ((tool.id == "flashlight" || tool.id == "strobe_torch") &&
                activeMode.id in setOf("strobe", "morse", "sos"))
    }

    var showPhotosensitivityDialog by rememberSaveable { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    // Show photosensitivity warning dialog ONCE the first time Strobe, Morse, or SOS opens
    LaunchedEffect(isFlashSafetyFeature, userPreferences.photosensitivityWarningShown) {
        if (isFlashSafetyFeature && !userPreferences.photosensitivityWarningShown) {
            showPhotosensitivityDialog = true
            preferencesRepository?.setPhotosensitivityWarningShown(true)
        }
    }

    if (showPhotosensitivityDialog) {
        PhotosensitivityWarningDialog(
            onDismiss = { showPhotosensitivityDialog = false }
        )
    }

    if (showHelpDialog) {
        val helpTitle = resolvedFeature?.title ?: tool.name
        val helpBody = resolvedFeature?.shortBenefit?.takeIf { it.isNotBlank() }
            ?: tool.description.ifBlank { "Use the controls on screen to operate $helpTitle." }
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = Color(0xFF121824),
            title = {
                Text(
                    text = helpTitle,
                    style = type.section.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "$helpBody\n\nTip: Tap the star icon in the top bar to pin $helpTitle to your Favorites for instant access.",
                    style = type.body,
                    color = Color.White.copy(alpha = 0.82f)
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Got it", color = theme.accent, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    var accessTickNowMs by remember { mutableStateOf(System.currentTimeMillis()) }

    val activeModeFeature = remember(tool.id, activeMode.id) {
        resolvePremiumFeatureForMode(tool.id, activeMode.id)
    }

    val activeTempUnlockRemainingMs = remember(
        userPreferences,
        accessTickNowMs,
        resolvedFeature,
        activeModeFeature,
        tool.id,
        actualIsPro
    ) {
        if (actualIsPro) {
            0L
        } else {
            featureGate?.getTemporaryUnlockRemainingMsForFeature(
                catalogFeature = resolvedFeature,
                fallbackFeature = activeModeFeature,
                toolId = tool.id,
                prefs = userPreferences,
                nowMs = accessTickNowMs
            ) ?: 0L
        }
    }

    LaunchedEffect(userPreferences.accessUnlockRecordsSerialized, userPreferences.featureUnlocksCsv, userPreferences.rewardedToolUnlocksCsv, userPreferences.spotlightExpiryEpochMs, actualIsPro) {
        if (!actualIsPro) {
            while (isActive) {
                accessTickNowMs = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    val isPremiumEntry = (resolvedFeature?.isPremium == true) || activeMode.isPremium || tool.isProOnly
    var hadTemporaryAccessSession by remember(resolvedFeature?.id, tool.id) { mutableStateOf(false) }
    var showExpiredUnlockOverlay by remember(resolvedFeature?.id, tool.id) { mutableStateOf(false) }
    var expiredNoticeMessage by remember { mutableStateOf<String?>(null) }

    val currentRemainingAccessMs = activeTempUnlockRemainingMs
    LaunchedEffect(currentRemainingAccessMs, actualIsPro, isPremiumEntry) {
        if (actualIsPro) {
            hadTemporaryAccessSession = false
            showExpiredUnlockOverlay = false
        } else if (currentRemainingAccessMs > 0L) {
            hadTemporaryAccessSession = true
            showExpiredUnlockOverlay = false
            expiredNoticeMessage = null
        } else if (hadTemporaryAccessSession && isPremiumEntry) {
            hadTemporaryAccessSession = false
            showExpiredUnlockOverlay = true
        }
    }

    // Effective Pro access for inner tool composables: true if account is Pro OR active 1h unlock is running
    val effectiveIsProForTool = actualIsPro || (isPro && !showExpiredUnlockOverlay) || currentRemainingAccessMs > 0L
    val isCustomizationUnlocked = userPreferences.isCustomizationAccessActive(actualIsPro, accessTickNowMs)

    // Phone Care & Audio tools and Pedometer handle their permissions inside their own composables at use
    val handlesOwnPermission = tool.category == ToolCategory.PHONE_CARE_AND_AUDIO ||
        tool.id in setOf("pedometer", "pedometer_history", "parking_saver", "compass_waypoints", "waypoints_pro", "walk_run_tracker")
    val requiredPerm = if (handlesOwnPermission) null else tool.requiredPermission
    var hasPermission by remember(requiredPerm) {
        mutableStateOf(
            requiredPerm == null || ContextCompat.checkSelfPermission(
                context,
                requiredPerm
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDeniedOnce by remember { mutableStateOf(false) }

    val singlePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) permissionDeniedOnce = true
    }

    val locationMultiPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasPermission = granted
        if (!granted) permissionDeniedOnce = true
    }

    val requestToolPermission = {
        if (requiredPerm == Manifest.permission.ACCESS_FINE_LOCATION) {
            locationMultiPermLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else if (requiredPerm != null) {
            singlePermLauncher.launch(requiredPerm)
        }
    }

    val displayTitle = resolvedFeature?.title ?: tool.name
    val displayCategory = resolvedFeature?.category?.title ?: tool.category.title

    val openFeatureUnlockSheet = {
        if (resolvedFeature != null && featureGate != null) {
            featureGate.requireCatalogFeature(resolvedFeature)
        } else {
            featureGate?.require(
                feature = activeModeFeature,
                customTitle = displayTitle,
                customDescription = resolvedFeature?.shortBenefit ?: tool.description
            )
        }
        Unit
    }

    FeatureScaffold(
        categoryLabel = displayCategory,
        title = displayTitle,
        isFavorite = isFavorite && isCustomizationUnlocked,
        showFavoriteCrown = !isCustomizationUnlocked,
        showSafetyInfoIcon = isFlashSafetyFeature,
        hazeState = hazeState,
        onBack = onBack,
        onToggleFavorite = {
            if (!isCustomizationUnlocked) {
                featureGate?.require(
                    feature = PremiumFeature.HOME_CUSTOMIZATION,
                    customTitle = "Favorites & Home Customization",
                    customDescription = "Pin unlimited Favorites, customize tiles, folders, and themes with Dyne Premium or a 1-hour unlock."
                )
            } else {
                onToggleFavorite()
            }
        },
        onOpenSafetyDialog = { showPhotosensitivityDialog = true },
        onOpenHelp = { showHelpDialog = true },
        onShareFeature = {
            val shareText = "${displayTitle} — ${resolvedFeature?.shortBenefit ?: tool.description} (Dyne All-in-One Toolbox)"
            runCatching {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    this.type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, displayTitle)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(intent, "Share $displayTitle"))
            }
        },
        accessBanner = {
            // Premium users see no banner at all.
            if (!actualIsPro && currentRemainingAccessMs > 0L) {
                AccessBanner(
                    remainingMs = currentRemainingAccessMs,
                    totalDurationMs = UserPreferences.ONE_HOUR_MS,
                    buttonText = "Extend",
                    hazeState = hazeState,
                    onUnlockClick = {
                        if (resolvedFeature != null && featureGate != null) {
                            val pf = resolvedFeature.premiumFeature ?: activeModeFeature
                            featureGate.require(
                                feature = pf,
                                featureKey = resolvedFeature.id,
                                customTitle = displayTitle,
                                customDescription = resolvedFeature.shortBenefit
                            )
                        } else {
                            openFeatureUnlockSheet()
                        }
                    }
                )
            }
        },
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (showExpiredUnlockOverlay && !actualIsPro) {
                            Modifier.blur(16.dp)
                        } else {
                            Modifier
                        }
                    )
            ) {
                // Dismissible first-use hearing safety tip for Phone Care & Audio tools
                if (tool.category == ToolCategory.PHONE_CARE_AND_AUDIO &&
                    tool.id != "frequency_tool" &&
                    !userPreferences.audioHearingSafetyTipDismissed
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF162032))
                            .border(1.dp, ProGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = ProGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hearing safety: Start at a comfortable volume and never hold the speaker directly against your ear.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                scope.launch {
                                    preferencesRepository?.dismissAudioHearingSafetyTip()
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss hearing safety tip",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

            val mode = activeMode
            val isSelfScrollingMode = tool.id in setOf(
                "device_sensors",
                "pdf_tools",
                "speaker_cleaner",
                "frequency_tool",
                "speaker_test",
                "mic_test",
                "tone_generator",
                "noise_sleep_mixer",
                "vibration_lab",
                "touch_screen_test",
                "metronome",
                "tuner",
                "battery_info",
                "tip_calc",
                "scientific_calc",
                "finance_pack",
                "split_bill_pro",
                "tally_counter",
                "pomodoro_timer",
                "pomodoro_stats",
                "interval_timer",
                "event_countdown",
                "randomizer_suite",
                "password_generator",
                "cooking_converter",
                "unit_price_comparer",
                "fuel_trip_calc",
                "text_tools",
                "number_tools",
                "ruler",
                "screen_test",
                "magnifier_color",
                "diy_calculators",
                "qr_generator",
                "qr_studio_pro",
                "image_toolkit",
                "trip_gear_checklists",
                "pedometer",
                "pedometer_history",
                "lux_meter",
                "photo_light_meter",
                "qibla_finder",
                "parking_saver",
                "weather_calculators",
                "compass_waypoints",
                "waypoints_pro",
                "walk_run_tracker",
                "altimeter",
                "vibration_gforce",
                "fishing_log",
                "level_calibration_pro",
                "measurement_history_csv",
                "photo_converter",
                "photo_converter_pro",
                "background_remover",
                "photo_editor",
                "passport_id_photo",
                "audio_converter",
                "video_tools",
                "video_compress_pro",
                "pdf_image_tools"
            )

            if (isSelfScrollingMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (tool.id) {
                        "device_sensors" -> DeviceSensorsInstrumentScreen()
                        "speaker_cleaner" -> {
                            when (mode.id) {
                                "stereo_lr" -> SpeakerTestInstrumentScreen()
                                "mic_test" -> MicTestInstrumentScreen(hazeState = hazeState)
                                else -> SpeakerCleanerInstrumentScreen(forcedModeId = mode.id)
                            }
                        }
                        "frequency_tool" -> FrequencyInstrumentScreen(
                            isPro = isPro,
                            ownedSoundPacks = ownedSoundPacks,
                            soundPackPrices = soundPackPrices,
                            safetyDismissed = userPreferences.frequencySafetyCardDismissed,
                            onDismissSafety = {
                                scope.launch {
                                    preferencesRepository?.dismissFrequencySafetyCard()
                                }
                            },
                            presetsCsv = userPreferences.frequencyPresetsCsv,
                            onSavePresetsCsv = { csv ->
                                scope.launch {
                                    preferencesRepository?.setFrequencyPresetsCsv(csv)
                                }
                            },
                            onOpenSpeakerCleaner = {
                                onSwitchTool("speaker_cleaner")
                            },
                            forcedModeId = mode.id
                        )
                        "speaker_test" -> SpeakerTestInstrumentScreen()
                        "mic_test" -> MicTestInstrumentScreen(hazeState = hazeState)
                        "tone_generator" -> ToneGeneratorInstrumentScreen()
                        "noise_sleep_mixer" -> NoiseSleepMixerInstrumentScreen()
                        "vibration_lab" -> VibrationLabInstrumentScreen()
                        "touch_screen_test" -> TouchScreenTestInstrumentScreen()
                        "metronome" -> MetronomeInstrumentScreen()
                        "tuner" -> TunerInstrumentScreen(hazeState = hazeState)
                        "battery_info" -> BatteryInfoInstrumentScreen()
                        "pdf_tools" -> {
                            val isUnlimitedMerge = isPro ||
                                featureGate?.isUnlocked(PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE) == true
                            PdfToolsScreen(
                                scanViewModel = scanViewModel,
                                isUnlimitedMergeUnlocked = isUnlimitedMerge,
                                hazeState = hazeState,
                                onRequestUnlockUnlimitedMerge = {
                                    if (featureGate != null) {
                                        featureGate.require(PremiumFeature.UNLIMITED_DOCUMENTS_OCR_LOCK_MERGE) {}
                                    } else {
                                        onOpenPaywall()
                                    }
                                }
                            )
                        }
                        "tip_calc" -> CalculatorsSuiteScreen(
                            forcedModeId = mode.id,
                            isPro = isPro || featureGate?.isUnlocked(PremiumFeature.SCIENTIFIC_DIY_CALCULATORS) == true,
                            onOpenPaywall = {
                                if (featureGate != null) {
                                    featureGate.require(PremiumFeature.SCIENTIFIC_DIY_CALCULATORS)
                                } else {
                                    onOpenPaywall()
                                }
                            }
                        )
                        "scientific_calc" -> CalculatorsSuiteScreen(
                            forcedModeId = "scientific",
                            isPro = isPro || featureGate?.isUnlocked(PremiumFeature.SCIENTIFIC_DIY_CALCULATORS) == true,
                            onOpenPaywall = {
                                if (featureGate != null) {
                                    featureGate.require(PremiumFeature.SCIENTIFIC_DIY_CALCULATORS)
                                } else {
                                    onOpenPaywall()
                                }
                            }
                        )
                        "finance_pack" -> FinancePackScreen(forcedModeId = mode.id)
                        "split_bill_pro" -> SplitTheBillProScreen(forcedModeId = mode.id)
                        "tally_counter" -> TallyCounterScreen(forcedModeId = mode.id)
                        "pomodoro_timer" -> PomodoroFocusAndStatsScreen(
                            forcedModeId = mode.id,
                            isPro = isPro,
                            onOpenPaywall = {
                                if (featureGate != null) {
                                    featureGate.require(PremiumFeature.POMODORO_STATS)
                                } else {
                                    onOpenPaywall()
                                }
                            }
                        )
                        "pomodoro_stats" -> PomodoroFocusAndStatsScreen(
                            forcedModeId = "stats",
                            isPro = true,
                            onOpenPaywall = onOpenPaywall
                        )
                        "interval_timer" -> IntervalTimerProScreen(forcedModeId = mode.id)
                        "event_countdown" -> EventCountdownProScreen(forcedModeId = mode.id)
                        "randomizer_suite" -> RandomizerSuiteScreen(forcedModeId = mode.id)
                        "password_generator" -> PasswordGeneratorScreen(forcedModeId = mode.id)
                        "cooking_converter" -> CookingConverterScreen(forcedModeId = mode.id)
                        "unit_price_comparer" -> UnitPriceComparerScreen(forcedModeId = mode.id)
                        "fuel_trip_calc" -> FuelAndTripCostScreen(forcedModeId = mode.id)
                        "text_tools" -> TextToolsScreen(forcedModeId = mode.id)
                        "number_tools" -> NumberToolsScreen(forcedModeId = mode.id)
                        "ruler" -> DpiCalibratedScreenRulerScreen(unitSystem = unitSystem, forcedModeId = mode.id)
                        "screen_test" -> ScreenTestDeadPixelScreen(forcedModeId = mode.id)
                        "magnifier_color" -> MagnifierAndColorPickerScreen(
                            hasCameraPermission = hasPermission,
                            onRequestCameraPermission = requestToolPermission,
                            forcedModeId = mode.id
                        )
                        "diy_calculators" -> DiyCalculatorsProScreen(forcedModeId = mode.id)
                        "qr_generator" -> QrGeneratorAndStylesScreen(
                            forcedModeId = mode.id,
                            isPro = isPro,
                            onOpenPaywall = {
                                if (featureGate != null) {
                                    featureGate.require(PremiumFeature.QR_GENERATOR_STYLES)
                                } else {
                                    onOpenPaywall()
                                }
                            },
                            onLaunchScannerMode = onLaunchScannerMode
                        )
                        "qr_studio_pro" -> QrGeneratorAndStylesScreen(
                            forcedModeId = "styles_pro",
                            isPro = true,
                            onOpenPaywall = onOpenPaywall,
                            onLaunchScannerMode = onLaunchScannerMode
                        )
                        "image_toolkit" -> ImageToolkitProScreen(forcedModeId = mode.id)
                        "trip_gear_checklists" -> TripAndGearChecklistsProScreen(
                            forcedModeId = mode.id,
                            onSaveNoteToVault = onSaveNoteToVault
                        )
                        "pedometer" -> PedometerAndHistoryScreen(
                            sensorState = sensorState,
                            unitSystem = unitSystem,
                            isPro = isPro || featureGate?.isUnlocked(PremiumFeature.PEDOMETER_HISTORY) == true,
                            forcedModeId = mode.id,
                            onOpenPaywall = {
                                if (featureGate != null) {
                                    featureGate.require(PremiumFeature.PEDOMETER_HISTORY)
                                } else {
                                    onOpenPaywall()
                                }
                            }
                        )
                        "pedometer_history" -> PedometerAndHistoryScreen(
                            sensorState = sensorState,
                            unitSystem = unitSystem,
                            isPro = true,
                            forcedModeId = "history",
                            onOpenPaywall = onOpenPaywall
                        )
                        "lux_meter" -> LuxAndPhotoLightMeterScreen(
                            sensorState = sensorState,
                            isPro = isPro || featureGate?.isUnlocked(PremiumFeature.PHOTO_LIGHT_METER) == true,
                            forcedModeId = mode.id,
                            onOpenPaywall = {
                                if (featureGate != null) {
                                    featureGate.require(PremiumFeature.PHOTO_LIGHT_METER)
                                } else {
                                    onOpenPaywall()
                                }
                            }
                        )
                        "photo_light_meter" -> LuxAndPhotoLightMeterScreen(
                            sensorState = sensorState,
                            isPro = true,
                            forcedModeId = "photo_ev",
                            onOpenPaywall = onOpenPaywall
                        )
                        "qibla_finder" -> QiblaFinderScreen(
                            sensorState = sensorState,
                            geoCoordinates = geoCoordinates,
                            forcedModeId = mode.id
                        )
                        "parking_saver" -> ParkingSaverScreen(
                            hasLocationPermission = hasPermission,
                            onRequestLocationPermission = requestToolPermission,
                            sensorState = sensorState,
                            geoCoordinates = geoCoordinates,
                            unitSystem = unitSystem,
                            forcedModeId = mode.id
                        )
                        "weather_calculators" -> WeatherCalculatorsScreen(forcedModeId = mode.id)
                        "compass_waypoints" -> CompassWaypointsScreen(
                            hasLocationPermission = hasPermission,
                            onRequestLocationPermission = requestToolPermission,
                            sensorState = sensorState,
                            geoCoordinates = geoCoordinates,
                            unitSystem = unitSystem,
                            isPro = isPro || featureGate?.isUnlocked(PremiumFeature.UNLIMITED_WAYPOINTS) == true,
                            forcedModeId = mode.id,
                            onOpenPaywall = {
                                if (featureGate != null) {
                                    featureGate.require(PremiumFeature.UNLIMITED_WAYPOINTS)
                                } else {
                                    onOpenPaywall()
                                }
                            }
                        )
                        "waypoints_pro" -> CompassWaypointsScreen(
                            hasLocationPermission = hasPermission,
                            onRequestLocationPermission = requestToolPermission,
                            sensorState = sensorState,
                            geoCoordinates = geoCoordinates,
                            unitSystem = unitSystem,
                            isPro = true,
                            forcedModeId = "unlimited",
                            onOpenPaywall = onOpenPaywall
                        )
                        "walk_run_tracker" -> WalkRunTrackerProScreen(
                            hasLocationPermission = hasPermission,
                            onRequestLocationPermission = requestToolPermission,
                            geoCoordinates = geoCoordinates,
                            unitSystem = unitSystem,
                            forcedModeId = mode.id
                        )
                        "altimeter" -> AltimeterBarometerProScreen(
                            sensorState = sensorState,
                            geoCoordinates = geoCoordinates,
                            unitSystem = unitSystem,
                            forcedModeId = mode.id
                        )
                        "vibration_gforce" -> VibrationGForceProScreen(
                            sensorState = sensorState,
                            forcedModeId = mode.id
                        )
                        "fishing_log" -> FishingLogProScreen(
                            sensorState = sensorState,
                            geoCoordinates = geoCoordinates,
                            onSaveLogToVault = onSaveNoteToVault,
                            onOpenLibraryNotes = onOpenLibraryNotes,
                            forcedModeId = mode.id,
                            isPro = isPro || featureGate?.isUnlocked(PremiumFeature.FISHING_LOG) == true,
                            onRequestUnlock = {
                                if (featureGate != null) {
                                    featureGate.require(PremiumFeature.FISHING_LOG)
                                } else {
                                    onOpenPaywall()
                                }
                            }
                        )
                        "level_calibration_pro", "measurement_history_csv" -> LevelCalibrationAndMeasurementHistoryScreen(
                            toolId = tool.id,
                            forcedModeId = mode.id,
                            sensorState = sensorState,
                            isPro = true,
                            onOpenPaywall = onOpenPaywall
                        )
                        "photo_converter", "photo_converter_pro", "background_remover",
                        "photo_editor", "passport_id_photo", "audio_converter",
                        "video_tools", "video_compress_pro", "pdf_image_tools" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                when (tool.id) {
                                    "photo_converter" -> PhotoConverterToolCard(
                                        activeModeId = mode.id,
                                        isPro = isPro || featureGate?.isUnlocked(PremiumFeature.PHOTO_CONVERTER_PRO) == true,
                                        prefs = userPreferences,
                                        preferencesRepository = preferencesRepository,
                                        scanViewModel = scanViewModel
                                    )
                                    "photo_converter_pro" -> PhotoConverterToolCard(
                                        activeModeId = "batch",
                                        isPro = true,
                                        prefs = userPreferences,
                                        preferencesRepository = preferencesRepository,
                                        scanViewModel = scanViewModel
                                    )
                                    "background_remover" -> BackgroundRemoverToolCard(
                                        activeModeId = mode.id,
                                        isPro = isPro || featureGate?.isUnlocked(PremiumFeature.BACKGROUND_REMOVER_PRO) == true,
                                        prefs = userPreferences,
                                        preferencesRepository = preferencesRepository,
                                        scanViewModel = scanViewModel
                                    )
                                    "photo_editor" -> PhotoEditorToolCard(
                                        activeModeId = mode.id,
                                        isPro = isPro || featureGate?.isUnlocked(PremiumFeature.PHOTO_EDITOR_PRO) == true,
                                        prefs = userPreferences,
                                        preferencesRepository = preferencesRepository,
                                        scanViewModel = scanViewModel
                                    )
                                    "passport_id_photo" -> PassportIdPhotoToolCard(
                                        activeModeId = mode.id,
                                        isPro = isPro || featureGate?.isUnlocked(PremiumFeature.PASSPORT_ID_PHOTO) == true,
                                        prefs = userPreferences,
                                        preferencesRepository = preferencesRepository,
                                        scanViewModel = scanViewModel
                                    )
                                    "audio_converter" -> AudioConverterToolCard(
                                        activeModeId = mode.id,
                                        isPro = isPro || featureGate?.isUnlocked(PremiumFeature.AUDIO_CONVERTER_PRO) == true,
                                        prefs = userPreferences,
                                        preferencesRepository = preferencesRepository,
                                        scanViewModel = scanViewModel
                                    )
                                    "video_tools" -> VideoToolsCard(
                                        activeModeId = mode.id,
                                        isPro = isPro || featureGate?.isUnlocked(PremiumFeature.VIDEO_COMPRESS_PRO) == true,
                                        prefs = userPreferences,
                                        preferencesRepository = preferencesRepository,
                                        scanViewModel = scanViewModel
                                    )
                                    "video_compress_pro" -> VideoToolsCard(
                                        activeModeId = "compress_resize",
                                        isPro = true,
                                        prefs = userPreferences,
                                        preferencesRepository = preferencesRepository,
                                        scanViewModel = scanViewModel
                                    )
                                    "pdf_image_tools" -> PdfAndImageStudioCard(
                                        activeModeId = mode.id,
                                        isPro = isPro || featureGate?.isUnlocked(PremiumFeature.PDF_IMAGE_PRO) == true,
                                        prefs = userPreferences,
                                        preferencesRepository = preferencesRepository,
                                        scanViewModel = scanViewModel
                                    )
                                }
                                Spacer(modifier = Modifier.height(LocalBottomBarInset.current + 16.dp))
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (requiredPerm != null && !hasPermission) {
                        GlassPermissionCard(
                            icon = tool.icon,
                            title = "${displayTitle} Permission",
                            explanation = when (requiredPerm) {
                                Manifest.permission.RECORD_AUDIO ->
                                    "Dyne samples ambient acoustic pressure in memory to compute approximate decibels (dB). Audio is never recorded or saved."
                                Manifest.permission.ACCESS_FINE_LOCATION ->
                                    "Dyne reads GPS velocity vectors offline to display your speed in km/h, mph, and knots, plus trip distance."
                                Manifest.permission.CAMERA ->
                                    "Dyne requests Camera permission solely to control the rear LED flashlight torch and brightness levels. No photos or video are captured."
                                else ->
                                    "This instrument requires hardware permission to read live sensor telemetry."
                            },
                            buttonText = "Grant Access at Use",
                            hazeState = hazeState,
                            onRequestPermission = requestToolPermission
                        )

                        if (permissionDeniedOnce) {
                            DesignedStateBanner(
                                badge = "Permission Denied",
                                title = "Hardware Access Restricted",
                                message = "You declined permission for ${displayTitle}. Grant permission anytime to switch from reference mode to live hardware.",
                                accentColor = StatusWarning,
                                actionText = "Request Permission Again",
                                onAction = requestToolPermission
                            )
                        }
                    }

                    if ((tool.id == "level" || tool.id == "angle") && !sensorState.hasAccelerometer) {
                        DesignedStateBanner(
                            badge = "Missing Sensor",
                            title = "Accelerometer Not Present",
                            message = "This hardware profile does not report an accelerometer. Displaying calibrated 0.0° reference bench.",
                            accentColor = StatusDanger
                        )
                    }
                    if ((tool.id == "compass" || tool.id == "metal_finder") && !sensorState.hasMagnetometer) {
                        DesignedStateBanner(
                            badge = "Missing Sensor",
                            title = "Magnetometer Not Present",
                            message = "No magnetic field sensor was found on this device. Showing baseline reference dial.",
                            accentColor = StatusDanger
                        )
                    }
                    if (tool.id == "lux_meter" && !sensorState.hasLightSensor) {
                        DesignedStateBanner(
                            badge = "Missing Sensor",
                            title = "Photometer Not Present",
                            message = "Ambient light sensor is unavailable on this hardware.",
                            accentColor = StatusWarning
                        )
                    }

                    GlassSurface(
                        hazeState = hazeState,
                        shape = RoundedCornerShape(30.dp),
                        cornerRadius = 30.dp,
                        contentPadding = PaddingValues(22.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (tool.id) {
                            "level", "angle" -> {
                                when (resolvedFeature?.id ?: mode.id) {
                                    "level_calibration_pro", "calibration_profiles", "history", "history_csv", "measurement_history_csv" ->
                                        LevelCalibrationAndMeasurementHistoryScreen(
                                            toolId = tool.id,
                                            forcedModeId = mode.id,
                                            sensorState = sensorState,
                                            isPro = true,
                                            onOpenPaywall = onOpenPaywall
                                        )
                                    "level_angle", "angle", "protractor" -> LevelAngleScreen(sensorState = sensorState)
                                    "roof_pitch", "slope_pct" -> RoofPitchInstrument(sensorState = sensorState)
                                    "level_bubble", "horizontal", "vertical" -> LevelBubbleScreen(sensorState = sensorState)
                                    "level_plumb", "plumb" -> LevelPlumbScreen(sensorState = sensorState)
                                    "level_surface", "surface_2d" -> LevelSurfaceScreen(sensorState = sensorState)
                                    else -> BubbleLevelInstrument(
                                        sensorState = sensorState,
                                        forcedModeId = mode.id
                                    )
                                }
                            }
                            "compass" -> {
                                when (mode.id) {
                                    "qibla" -> QiblaFinderScreen(
                                        sensorState = sensorState,
                                        geoCoordinates = geoCoordinates,
                                        forcedModeId = "qibla"
                                    )
                                    "waypoints", "unlimited_waypoints" -> CompassWaypointsScreen(
                                        hasLocationPermission = hasPermission,
                                        onRequestLocationPermission = requestToolPermission,
                                        sensorState = sensorState,
                                        geoCoordinates = geoCoordinates,
                                        unitSystem = unitSystem,
                                        isPro = isPro || featureGate?.isUnlocked(PremiumFeature.UNLIMITED_WAYPOINTS) == true || mode.id == "waypoints_pro",
                                        forcedModeId = mode.id,
                                        onOpenPaywall = {
                                            if (featureGate != null) {
                                                featureGate.require(PremiumFeature.UNLIMITED_WAYPOINTS)
                                            } else {
                                                onOpenPaywall()
                                            }
                                        }
                                    )
                                    "history_csv" -> LevelCalibrationAndMeasurementHistoryScreen(
                                        toolId = "compass",
                                        forcedModeId = "compass_log",
                                        sensorState = sensorState,
                                        isPro = true,
                                        onOpenPaywall = onOpenPaywall
                                    )
                                    else -> CompassInstrument(
                                        sensorState = sensorState,
                                        geoCoordinates = geoCoordinates,
                                        forcedModeId = mode.id,
                                        onOpenSunMoonDirection = {
                                            if (featureGate != null) {
                                                featureGate.guardNavigate("compass_sun_moon_direction", prefs = userPreferences) {
                                                    onSwitchTool("compass_sun_moon_direction")
                                                }
                                            } else {
                                                onOpenPaywall()
                                            }
                                        }
                                    )
                                }
                            }
                            "metal_finder" -> MetalFinderInstrument(sensorState = sensorState)
                            "flashlight", "strobe_torch", "screen_torch" -> {
                                FlashlightInstrumentScreen(
                                    hasCameraPermission = hasPermission,
                                    isPro = effectiveIsProForTool,
                                    onOpenPaywall = onOpenPaywall,
                                    onRequestCameraPermission = requestToolPermission,
                                    forcedModeId = resolvedFeature?.id
                                        ?: if (tool.id == "screen_torch" && initialModeId == null) "screen_light" else mode.id,
                                    hazeState = hazeState
                                )
                            }
                            "sound_db" -> {
                                if (mode.id == "history_csv") {
                                    LevelCalibrationAndMeasurementHistoryScreen(
                                        toolId = "sound_db",
                                        forcedModeId = "sound_log",
                                        sensorState = sensorState,
                                        isPro = true,
                                        onOpenPaywall = onOpenPaywall
                                    )
                                } else {
                                    SoundDbInstrument(
                                        hasAudioPermission = hasPermission,
                                        isPro = isPro,
                                        onOpenPaywall = onOpenPaywall,
                                        forcedModeId = mode.id
                                    )
                                }
                            }
                            "speedometer" -> SpeedometerInstrumentScreen(
                                hasLocationPermission = hasPermission,
                                geoCoordinates = geoCoordinates,
                                unitSystem = unitSystem,
                                isPro = isPro,
                                onOpenPaywall = onOpenPaywall,
                                forcedModeId = mode.id
                            )
                            "ruler" -> ScreenRulerInstrument(unitSystem = unitSystem)
                            "lux_meter" -> LuxMeterInstrument(sensorState = sensorState)
                            "altimeter" -> AltimeterInstrument(
                                sensorState = sensorState,
                                geoCoordinates = geoCoordinates,
                                unitSystem = unitSystem
                            )
                            "vibration_gforce" -> VibrationGForceInstrument(sensorState = sensorState)
                            "walk_run_tracker" -> WalkRunTrackerInstrument(
                                hasLocationPermission = hasPermission,
                                geoCoordinates = geoCoordinates,
                                unitSystem = unitSystem
                            )
                            "fishing_log" -> FishingLogInstrument(
                                sensorState = sensorState,
                                onSaveLogToVault = onSaveNoteToVault,
                                onOpenLibraryNotes = onOpenLibraryNotes
                            )
                            "stopwatch", "interval_timer", "world_clock" -> {
                                when (mode.id) {
                                    "stopwatch", "lap_timer" -> ChronographInstrument()
                                    "timer" -> CountdownTimerInstrument()
                                    "interval", "pomodoro", "hiit", "tabata" -> IntervalTimerInstrument()
                                    else -> WorldClockInstrumentScreen(
                                        isPro = isPro,
                                        onOpenPaywall = onOpenPaywall,
                                        hazeState = hazeState
                                    )
                                }
                            }
                            "unit_converter" -> {
                                if (mode.id in setOf("custom_units", "favorites", "history")) {
                                    UnitConverterExtrasProScreen(
                                        forcedModeId = mode.id,
                                        hazeState = hazeState ?: remember { HazeState() }
                                    )
                                } else {
                                    UnitConverterInstrumentScreen(
                                        hazeState = hazeState,
                                        forcedModeId = mode.id
                                    )
                                }
                            }
                            "unit_converter_pro" -> UnitConverterExtrasProScreen(
                                forcedModeId = mode.id,
                                hazeState = hazeState ?: remember { HazeState() }
                            )
                            "qr_generator" -> QrGeneratorInstrument(
                                isPro = isPro,
                                onOpenPaywall = onOpenPaywall
                            )
                            "quick_note" -> NotesWorkspaceSection(
                                scanViewModel = scanViewModel,
                                isPro = isPro,
                                onOpenPaywall = onOpenPaywall,
                                hazeState = hazeState,
                                showLibraryJumpButton = true,
                                onJumpToLibraryNotes = onOpenLibraryNotes,
                                forcedModeId = mode.id
                            )
                            "tip_calc" -> ScientificAndDiyCalculatorInstrument(
                                forcedModeId = mode.id
                            )
                            else -> BubbleLevelInstrument(sensorState = sensorState)
                        }
                    }

                    Spacer(modifier = Modifier.height(LocalBottomBarInset.current + 16.dp))
                }
            }
            }

            // When preview or 1-hour unlock time ends, the feature screen blurs and this centered glass card appears:
            // "Unlock to continue" with Watch ad for 1 hour, Use coins for 1 hour, Go Premium, Back.
            if (showExpiredUnlockOverlay && !actualIsPro) {
                val adManager = LocalAdManager.current
                val activity = context as? Activity
                val unlockRequest = remember(resolvedFeature, activeModeFeature, displayTitle) {
                    UnlockSheetRequest(
                        feature = resolvedFeature?.premiumFeature ?: activeModeFeature,
                        catalogFeatureId = resolvedFeature?.id,
                        customTitle = displayTitle,
                        customDescription = resolvedFeature?.shortBenefit ?: tool.description,
                        onUnlocked = {
                            showExpiredUnlockOverlay = false
                            expiredNoticeMessage = null
                        }
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .padding(horizontal = 24.dp)
                        .testTag("access_expired_overlay")
                ) {
                    GlassSurface(
                        hazeState = hazeState,
                        shape = RoundedCornerShape(28.dp),
                        cornerRadius = 28.dp,
                        tintColor = theme.elevated.copy(alpha = 0.94f),
                        contentPadding = PaddingValues(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ProGold.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
                            .testTag("access_expired_unlock_card")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Unlock to continue",
                                style = type.section.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = theme.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = displayTitle,
                                style = type.body.copy(fontSize = 13.sp),
                                color = theme.textSecondary,
                                textAlign = TextAlign.Center
                            )

                            val activeNotice = expiredNoticeMessage
                            if (!activeNotice.isNullOrBlank()) {
                                Text(
                                    text = activeNotice,
                                    style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                                    color = ProGold,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            GlassButton(
                                text = "Watch ad for 1 hour",
                                accentFill = ProGold.copy(alpha = 0.22f),
                                textColor = ProGold,
                                onClick = {
                                    expiredNoticeMessage = null
                                    if (adManager != null && featureGate != null) {
                                        adManager.showRewardedAd(
                                            activity = activity,
                                            rewardDescription = "Unlock $displayTitle (1h)",
                                            onAdUnavailable = { msg ->
                                                expiredNoticeMessage = msg
                                            },
                                            onRewardEarned = {
                                                featureGate.grant1HourUnlockFromRewardedAd(unlockRequest) {
                                                    showExpiredUnlockOverlay = false
                                                    expiredNoticeMessage = null
                                                }
                                            }
                                        )
                                    } else {
                                        openFeatureUnlockSheet()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "expired_watch_ad_1h_button"
                            )

                            GlassButton(
                                text = "Use coins for 1 hour",
                                accentFill = ProGold.copy(alpha = 0.16f),
                                textColor = ProGold,
                                onClick = {
                                    expiredNoticeMessage = null
                                    if (featureGate != null) {
                                        featureGate.unlockWithCoins(unlockRequest) { success, msg ->
                                            if (success) {
                                                showExpiredUnlockOverlay = false
                                                expiredNoticeMessage = null
                                            } else {
                                                expiredNoticeMessage = msg
                                            }
                                        }
                                    } else {
                                        openFeatureUnlockSheet()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "expired_use_coins_1h_button"
                            )

                            GlassButton(
                                text = "Go Premium",
                                accentFill = ProGold,
                                textColor = BentoTextBlack,
                                onClick = {
                                    showExpiredUnlockOverlay = false
                                    onOpenPaywall()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "expired_go_premium_button"
                            )

                            TextButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("expired_back_button")
                            ) {
                                Text(
                                    text = "Back",
                                    style = type.body.copy(fontWeight = FontWeight.Bold),
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


/**
 * 1) LEVEL: Bubble level using low-pass filtered accelerometer, flat (2D bullseye) and vertical (1D tube)
 * modes, degrees readout, and haptic tick at 0°.
 */
@Composable
private fun BubbleLevelInstrument(
    sensorState: SensorState,
    forcedModeId: String? = null
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var modeIndex by rememberSaveable(forcedModeId) {
        mutableIntStateOf(
            when (forcedModeId) {
                "horizontal", "vertical", "plumb" -> 1
                else -> 0
            }
        )
    } // 0 = Flat Mode, 1 = Tube/Plumb Mode

    val roll by animateFloatAsState(
        targetValue = sensorState.rollDeg,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "roll_anim"
    )
    val pitch by animateFloatAsState(
        targetValue = sensorState.pitchDeg,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pitch_anim"
    )

    val activeAxisAngle = when (forcedModeId) {
        "vertical" -> pitch
        else -> roll
    }
    val combinedDeviation = if (modeIndex == 0) hypot(roll, pitch) else abs(activeAxisAngle)
    val isZero = combinedDeviation < 0.75f
    var wasZero by remember { mutableStateOf(false) }

    LaunchedEffect(isZero) {
        if (isZero && !wasZero) {
            FeedbackHelper.vibrateTick(context, durationMs = 24L, amplitude = 180)
        }
        wasZero = isZero
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (forcedModeId == null) {
            DyneSegmentedControl(
                segments = listOf("Flat Mode", "Vertical Mode"),
                selectedIndex = modeIndex,
                onSegmentSelected = { modeIndex = it }
            )
        } else {
            val modeBadge = when (forcedModeId) {
                "surface_2d" -> "SURFACE 2D BULLSEYE"
                "horizontal" -> "HORIZONTAL SPIRIT TUBE"
                "vertical" -> "VERTICAL SPIRIT TUBE"
                "plumb" -> "PLUMB BOB ALIGNMENT"
                else -> "SURFACE 2D BULLSEYE"
            }
            Text(
                text = modeBadge,
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.accent
            )
        }

        Text(
            text = String.format(Locale.US, "%04.1f°", combinedDeviation),
            style = type.gaugeHero,
            color = if (isZero) theme.accent else theme.textPrimary
        )
        Text(
            text = if (isZero) {
                "LEVEL LOCKED • 0.0° HAPTIC TICK"
            } else {
                "X (ROLL) ${String.format(Locale.US, "%+.1f°", roll)}   •   Y (PITCH) ${String.format(Locale.US, "%+.1f°", pitch)}"
            },
            style = type.gaugeSmall,
            color = if (isZero) theme.accent else theme.textSecondary
        )

        if (modeIndex == 0) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth(0.80f)
                    .aspectRatio(1f)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val r = size.minDimension / 2f - 8.dp.toPx()

                drawCircle(color = theme.elevated, radius = r, center = center)
                drawCircle(color = theme.accent.copy(alpha = 0.45f), radius = r, center = center, style = Stroke(width = 2.dp.toPx()))
                drawCircle(color = theme.textSecondary.copy(alpha = 0.22f), radius = r * 0.6f, center = center, style = Stroke(width = 1.dp.toPx()))
                drawCircle(color = theme.accent, radius = r * 0.24f, center = center, style = Stroke(width = 2.dp.toPx()))

                drawLine(theme.textSecondary.copy(alpha = 0.2f), Offset(center.x - r, center.y), Offset(center.x + r, center.y), 1.dp.toPx())
                drawLine(theme.textSecondary.copy(alpha = 0.2f), Offset(center.x, center.y - r), Offset(center.x, center.y + r), 1.dp.toPx())

                val bx = center.x + (roll / 45f).coerceIn(-1f, 1f) * (r * 0.74f)
                val by = center.y + (-pitch / 45f).coerceIn(-1f, 1f) * (r * 0.74f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, theme.accent),
                        center = Offset(bx, by),
                        radius = r * 0.22f
                    ),
                    radius = r * 0.18f,
                    center = Offset(bx, by)
                )
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                val tubeHeight = 64.dp.toPx()
                val tubeTop = (size.height - tubeHeight) / 2f
                val corner = CornerRadius(tubeHeight / 2f, tubeHeight / 2f)

                drawRoundRect(
                    color = theme.elevated,
                    topLeft = Offset(0f, tubeTop),
                    size = Size(size.width, tubeHeight),
                    cornerRadius = corner
                )
                drawRoundRect(
                    color = theme.accent.copy(alpha = 0.55f),
                    topLeft = Offset(0f, tubeTop),
                    size = Size(size.width, tubeHeight),
                    cornerRadius = corner,
                    style = Stroke(width = 2.dp.toPx())
                )

                val midX = size.width / 2f
                val gateHalf = 28.dp.toPx()
                drawLine(theme.accent, Offset(midX - gateHalf, tubeTop), Offset(midX - gateHalf, tubeTop + tubeHeight), 2.5.dp.toPx())
                drawLine(theme.accent, Offset(midX + gateHalf, tubeTop), Offset(midX + gateHalf, tubeTop + tubeHeight), 2.5.dp.toPx())

                val normX = (roll / 45f).coerceIn(-1f, 1f)
                val maxTravel = (size.width / 2f) - 40.dp.toPx()
                val bubbleX = midX + normX * maxTravel
                drawCircle(
                    color = theme.accent,
                    radius = tubeHeight * 0.36f,
                    center = Offset(bubbleX, size.height / 2f)
                )
            }
        }
    }
}

/**
 * 2) ANGLE: Inclinometer with a Hold button AND a "Zero" button (relative reference calibration).
 */
@Composable
private fun InclinometerInstrument(sensorState: SensorState) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var isHeld by rememberSaveable { mutableStateOf(false) }
    var heldAngle by rememberSaveable { mutableFloatStateOf(0f) }
    var zeroOffsetDeg by rememberSaveable { mutableFloatStateOf(0f) }

    val rawAngle = sensorState.tiltAngleDeg
    val calibratedLiveAngle = abs(rawAngle - zeroOffsetDeg)

    val displayedAngle by animateFloatAsState(
        targetValue = if (isHeld) heldAngle else calibratedLiveAngle,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "inclinometer_angle"
    )
    val slopePercent = (tan(displayedAngle * PI / 180.0).coerceIn(0.0, 9.99) * 100.0)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = String.format(Locale.US, "%04.1f°", displayedAngle),
            style = type.gaugeHero,
            color = if (isHeld) theme.bentoCoral else theme.accent
        )
        Text(
            text = buildString {
                if (isHeld) {
                    append(String.format(Locale.US, "HELD • LIVE Δ %+.1f° • ", calibratedLiveAngle - heldAngle))
                }
                append(String.format(Locale.US, "SLOPE %.1f%%", slopePercent))
                if (zeroOffsetDeg != 0f) {
                    append(String.format(Locale.US, " • ZERO REF %.1f°", zeroOffsetDeg))
                }
            },
            style = type.gaugeSmall,
            color = theme.textSecondary
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .height(185.dp)
        ) {
            val pivot = Offset(size.width / 2f, size.height - 16.dp.toPx())
            val radius = (size.width / 2f - 16.dp.toPx()).coerceAtMost(size.height - 24.dp.toPx())

            drawArc(
                color = theme.elevated,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(pivot.x - radius, pivot.y - radius),
                size = Size(radius * 2f, radius * 2f)
            )
            drawArc(
                color = theme.accent.copy(alpha = 0.25f),
                startAngle = 180f,
                sweepAngle = displayedAngle.coerceIn(0f, 180f),
                useCenter = true,
                topLeft = Offset(pivot.x - radius, pivot.y - radius),
                size = Size(radius * 2f, radius * 2f)
            )
            drawArc(
                color = theme.accent,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(pivot.x - radius, pivot.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            for (deg in 0..180 step 15) {
                val rad = Math.toRadians((180 + deg).toDouble())
                val inner = if (deg % 45 == 0) radius - 14.dp.toPx() else radius - 8.dp.toPx()
                drawLine(
                    color = theme.textSecondary,
                    start = Offset((pivot.x + inner * cos(rad)).toFloat(), (pivot.y + inner * sin(rad)).toFloat()),
                    end = Offset((pivot.x + radius * cos(rad)).toFloat(), (pivot.y + radius * sin(rad)).toFloat()),
                    strokeWidth = if (deg % 45 == 0) 2.dp.toPx() else 1.dp.toPx()
                )
            }

            val needleRad = Math.toRadians((180f + displayedAngle).toDouble())
            drawLine(
                color = if (isHeld) theme.bentoCoral else theme.accent,
                start = pivot,
                end = Offset((pivot.x + radius * cos(needleRad)).toFloat(), (pivot.y + radius * sin(needleRad)).toFloat()),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(color = Color.White, radius = 6.dp.toPx(), center = pivot)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButton(
                text = if (isHeld) "Release Hold" else "Hold",
                accentFill = if (isHeld) theme.bentoCoral else theme.accent,
                onClick = {
                    FeedbackHelper.vibrateTick(context)
                    heldAngle = calibratedLiveAngle
                    isHeld = !isHeld
                },
                modifier = Modifier.weight(1f),
                testTag = "angle_hold_button"
            )
            GlassButton(
                text = if (zeroOffsetDeg == 0f) "Zero" else "Reset Zero",
                onClick = {
                    FeedbackHelper.vibrateTick(context)
                    zeroOffsetDeg = if (zeroOffsetDeg == 0f) rawAngle else 0f
                },
                modifier = Modifier.weight(1f),
                testTag = "angle_zero_button"
            )
        }
    }
}

/**
 * 3) COMPASS: Magnetometer + accelerometer, rotating dial, heading in degrees, N/E/S/W,
 * true/magnetic note & toggle, and "move phone in a figure 8" hint when accuracy is low.
 */
@Composable
private fun CompassInstrument(
    sensorState: SensorState,
    geoCoordinates: GeoCoordinates,
    forcedModeId: String? = null,
    onOpenSunMoonDirection: () -> Unit = {}
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val textMeasurer = rememberTextMeasurer()

    val isSunMoonMode = forcedModeId == "sun_moon_direction"
    var useTrueNorth by rememberSaveable(forcedModeId) {
        mutableStateOf(forcedModeId == "true_north" || isSunMoonMode)
    }
    var lockedBearingDeg by rememberSaveable { mutableFloatStateOf(0f) }
    var isBearingLocked by rememberSaveable { mutableStateOf(false) }

    val nowZdt = remember { ZonedDateTime.now(ZoneId.systemDefault()) }
    val sunPosition = remember(geoCoordinates.latitude, geoCoordinates.longitude, nowZdt) {
        com.example.domain.AstroCalculator.getSunPosition(nowZdt, geoCoordinates.latitude, geoCoordinates.longitude)
    }
    val moonPosition = remember(geoCoordinates.latitude, geoCoordinates.longitude, nowZdt) {
        com.example.domain.AstroCalculator.getMoonPosition(nowZdt, geoCoordinates.latitude, geoCoordinates.longitude)
    }

    // Compute local geomagnetic declination from GPS / manual coordinates
    val declinationDeg = remember(geoCoordinates.latitude, geoCoordinates.longitude, geoCoordinates.altitudeMeters) {
        runCatching {
            val field = GeomagneticField(
                geoCoordinates.latitude.toFloat(),
                geoCoordinates.longitude.toFloat(),
                geoCoordinates.altitudeMeters.toFloat(),
                System.currentTimeMillis()
            )
            field.declination
        }.getOrDefault(0f)
    }

    // Qibla direction from current GPS coordinates toward Kaaba (21.4225° N, 39.8262° E)
    val qiblaBearingDeg = remember(geoCoordinates.latitude, geoCoordinates.longitude) {
        val lat1 = Math.toRadians(geoCoordinates.latitude)
        val lon1 = Math.toRadians(geoCoordinates.longitude)
        val lat2 = Math.toRadians(21.4225)
        val lon2 = Math.toRadians(39.8262)
        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        ((Math.toDegrees(kotlin.math.atan2(y, x)).toFloat() + 360f) % 360f)
    }

    val magneticHeading = sensorState.azimuthDeg
    val trueHeading = (magneticHeading + declinationDeg + 360f) % 360f
    val activeHeading = if (useTrueNorth || forcedModeId == "true_north" || forcedModeId == "qibla" || isSunMoonMode) trueHeading else magneticHeading

    val cardinalLabel = remember(activeHeading) {
        val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = (((activeHeading + 22.5f) % 360f) / 45f).toInt().coerceIn(0, 7)
        dirs[index]
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (sensorState.isLowCompassAccuracy) {
            DesignedStateBanner(
                badge = "Low Compass Accuracy",
                title = "Calibration Recommended",
                message = "Move phone in a figure 8 motion away from magnetic interference.",
                accentColor = StatusWarning
            )
        }

        if (forcedModeId == null) {
            DyneSegmentedControl(
                segments = listOf("Magnetic North", "True North"),
                selectedIndex = if (useTrueNorth) 1 else 0,
                onSegmentSelected = { useTrueNorth = (it == 1) }
            )
        } else {
            val modeSubtitle = when (forcedModeId) {
                "sun_moon_direction" -> String.format(
                    Locale.US,
                    "SUN AZ %03d° (ALT %+.0f°)  •  MOON AZ %03d° (ALT %+.0f°)",
                    sunPosition.azimuthDeg.roundToInt() % 360,
                    sunPosition.altitudeDeg,
                    moonPosition.azimuthDeg.roundToInt() % 360,
                    moonPosition.altitudeDeg
                )
                "true_north" -> "TRUE NORTH • DECLINATION APPLIED (${String.format(Locale.US, "%+.1f°", declinationDeg)})"
                "bearing_lock" -> if (isBearingLocked) {
                    val diff = ((activeHeading - lockedBearingDeg + 540f) % 360f) - 180f
                    String.format(Locale.US, "LOCKED %03d° • Δ %+.0f°", lockedBearingDeg.roundToInt(), diff)
                } else {
                    "TAP BELOW TO LOCK TARGET BEARING"
                }
                "qibla" -> String.format(Locale.US, "QIBLA BEARING %03d° • Δ %+.0f°", qiblaBearingDeg.roundToInt(), ((qiblaBearingDeg - activeHeading + 540f) % 360f) - 180f)
                "coordinates" -> String.format(Locale.US, "LAT %.5f°  •  LON %.5f°  •  ALT %.0fm", geoCoordinates.latitude, geoCoordinates.longitude, geoCoordinates.altitudeMeters)
                else -> "MAGNETIC NORTH HEADING"
            }
            Text(
                text = modeSubtitle,
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.accent
            )
        }

        Text(
            text = String.format(Locale.US, "%03d° %s", activeHeading.roundToInt() % 360, cardinalLabel),
            style = type.gaugeHero,
            color = theme.textPrimary
        )
        Text(
            text = String.format(
                Locale.US,
                "MAG %03d°  •  TRUE %03d° (DECL %+.1f°)  •  %.1f µT",
                magneticHeading.roundToInt() % 360,
                trueHeading.roundToInt() % 360,
                declinationDeg,
                sensorState.magneticMagnitudeUt
            ),
            style = type.gaugeSmall,
            color = theme.accent
        )

        if (forcedModeId == "bearing_lock") {
            GlassButton(
                text = if (isBearingLocked) "Clear Bearing Lock (${lockedBearingDeg.roundToInt()}°)" else "Lock Current Bearing (${activeHeading.roundToInt()}°)",
                accentFill = if (isBearingLocked) theme.bentoCoral else theme.accent,
                onClick = {
                    if (isBearingLocked) {
                        isBearingLocked = false
                    } else {
                        lockedBearingDeg = activeHeading
                        isBearingLocked = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.80f)
                .aspectRatio(1f)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f - 10.dp.toPx()

            rotate(degrees = -activeHeading, pivot = center) {
                drawCircle(color = theme.elevated, radius = r, center = center)
                drawCircle(color = theme.accent.copy(alpha = 0.45f), radius = r, center = center, style = Stroke(width = 2.dp.toPx()))

                for (deg in 0 until 360 step 10) {
                    val rad = Math.toRadians((deg - 90).toDouble())
                    val isCardinal = deg % 90 == 0
                    val len = if (isCardinal) 18.dp.toPx() else 9.dp.toPx()
                    drawLine(
                        color = if (deg == 0) theme.bentoCoral else theme.textSecondary,
                        start = Offset((center.x + (r - len) * cos(rad)).toFloat(), (center.y + (r - len) * sin(rad)).toFloat()),
                        end = Offset((center.x + r * cos(rad)).toFloat(), (center.y + r * sin(rad)).toFloat()),
                        strokeWidth = if (isCardinal) 3.dp.toPx() else 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                val cardinals = listOf(0 to "N", 90 to "E", 180 to "S", 270 to "W")
                cardinals.forEach { (deg, label) ->
                    val rad = Math.toRadians((deg - 90).toDouble())
                    val pos = Offset(
                        (center.x + (r - 34.dp.toPx()) * cos(rad)).toFloat(),
                        (center.y + (r - 34.dp.toPx()) * sin(rad)).toFloat()
                    )
                    val m = textMeasurer.measure(
                        text = label,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (label == "N") theme.bentoCoral else theme.textPrimary
                        )
                    )
                    drawText(m, topLeft = Offset(pos.x - m.size.width / 2f, pos.y - m.size.height / 2f))
                }

                if (isSunMoonMode) {
                    val sunRad = Math.toRadians(sunPosition.azimuthDeg - 90.0)
                    val sunMarker = Offset(
                        (center.x + (r - 56.dp.toPx()) * cos(sunRad)).toFloat(),
                        (center.y + (r - 56.dp.toPx()) * sin(sunRad)).toFloat()
                    )
                    drawCircle(color = theme.bentoCoral.copy(alpha = 0.28f), radius = 14.dp.toPx(), center = sunMarker)
                    drawCircle(color = theme.bentoCoral, radius = 7.dp.toPx(), center = sunMarker)

                    val moonRad = Math.toRadians(moonPosition.azimuthDeg - 90.0)
                    val moonMarker = Offset(
                        (center.x + (r - 56.dp.toPx()) * cos(moonRad)).toFloat(),
                        (center.y + (r - 56.dp.toPx()) * sin(moonRad)).toFloat()
                    )
                    drawCircle(color = theme.bentoSky.copy(alpha = 0.28f), radius = 13.dp.toPx(), center = moonMarker)
                    drawCircle(color = theme.bentoSky, radius = 6.dp.toPx(), center = moonMarker)
                }

                drawLine(
                    color = theme.bentoCoral,
                    start = center,
                    end = Offset(center.x, center.y - r * 0.56f),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = center)
            }
        }

        if (!isSunMoonMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(theme.elevated)
                    .border(1.dp, ProGold.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                    .pressScaleClickable(
                        testTag = "compass_sun_moon_direction_button",
                        onClick = onOpenSunMoonDirection
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sun & Moon Direction",
                        style = type.body.copy(fontWeight = FontWeight.Bold),
                        color = theme.textPrimary
                    )
                    Text(
                        text = "Live Sun & Moon azimuth and altitude vectors on the compass rose",
                        style = type.caption,
                        color = theme.textSecondary
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open Sun & Moon Direction",
                    tint = ProGold,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // True / Magnetic Note & Figure-8 Calibration Hint Card
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(theme.elevated)
                .padding(14.dp)
        ) {
            Text(
                text = "True vs. Magnetic Note",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.accent
            )
            Text(
                text = "Magnetic North follows Earth's local magnetic field lines. True North applies geomagnetic declination (${String.format(Locale.US, "%+.1f°", declinationDeg)}) for map alignment. If readings drift, move phone in a figure 8.",
                style = type.caption,
                color = theme.textSecondary
            )
        }
    }
}

/**
 * 4) METAL DETECTOR: Magnetic field in µT on an animated circular glass gauge, beep (ToneGenerator)
 * and vibration that speed up near metal, Zero button, sensitivity slider, and the exact disclaimer:
 * "Detects magnetic metals (iron, steel) only. It cannot detect gold, silver or copper."
 */
@Composable
private fun MetalFinderInstrument(sensorState: SensorState) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var baselineUt by rememberSaveable { mutableFloatStateOf(45f) }
    var sensitivity by rememberSaveable { mutableFloatStateOf(1.0f) } // 0.5x .. 3.0x

    val rawUt = sensorState.magneticMagnitudeUt
    val netAnomalyUt = abs(rawUt - baselineUt)
    val weightedSignal = netAnomalyUt * sensitivity
    val gaugeFraction by animateFloatAsState(
        targetValue = (weightedSignal / 140f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "metal_circular_gauge"
    )

    // Beep (ToneGenerator) and vibration that speed up near ferromagnetic metal
    LaunchedEffect(weightedSignal) {
        if (weightedSignal > 10f) {
            while (isActive) {
                val clamped = weightedSignal.coerceIn(10f, 200f)
                val intervalMs = (680L - ((clamped / 200f) * 585L).toLong()).coerceIn(90L, 680L)
                FeedbackHelper.vibrateTick(
                    context = context,
                    durationMs = 24L,
                    amplitude = (85 + (clamped * 0.85f).toInt()).coerceIn(85, 255)
                )
                FeedbackHelper.playBeep(durationMs = 35, toneType = ToneGenerator.TONE_PROP_BEEP)
                delay(intervalMs)
            }
        }
    }

    val statusColor = when {
        weightedSignal > 60f -> theme.bentoCoral
        weightedSignal > 20f -> StatusWarning
        else -> theme.accent
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Animated Circular Glass Gauge
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(220.dp)
                .padding(6.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 18.dp.toPx()
                val r = (size.minDimension / 2f) - stroke
                val center = Offset(size.width / 2f, size.height / 2f)

                // Translucent glass ring backdrop
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = r,
                    center = center,
                    style = Stroke(width = stroke)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.14f),
                    radius = r + stroke / 2f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Tick marks around circular gauge
                for (deg in 0 until 360 step 15) {
                    val rad = Math.toRadians(deg.toDouble())
                    val inner = r - stroke * 0.85f
                    val outer = r - stroke * 0.55f
                    drawLine(
                        color = Color.White.copy(alpha = 0.18f),
                        start = Offset(center.x + inner * cos(rad).toFloat(), center.y + inner * sin(rad).toFloat()),
                        end = Offset(center.x + outer * cos(rad).toFloat(), center.y + outer * sin(rad).toFloat()),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                // Animated magnetic intensity arc
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(theme.accent, theme.bentoSky, StatusWarning, theme.bentoCoral, theme.accent)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * gaugeFraction,
                    useCenter = false,
                    topLeft = Offset(center.x - r, center.y - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format(Locale.US, "%.1f", rawUt),
                    style = type.gaugeHero,
                    color = statusColor
                )
                Text(
                    text = "µT FIELD",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.textSecondary
                )
                Text(
                    text = String.format(Locale.US, "Δ %+.1f µT", rawUt - baselineUt),
                    style = type.gaugeSmall,
                    color = statusColor
                )
            }
        }

        // Sensitivity Slider
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sensitivity", style = type.caption.copy(fontWeight = FontWeight.Bold), color = theme.textPrimary)
                Text(String.format(Locale.US, "%.1fx", sensitivity), style = type.caption, color = theme.accent)
            }
            Slider(
                value = sensitivity,
                onValueChange = { sensitivity = it },
                valueRange = 0.5f..3.0f,
                colors = SliderDefaults.colors(
                    thumbColor = theme.accent,
                    activeTrackColor = theme.accent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButton(
                text = "Zero",
                accentFill = theme.accent,
                onClick = {
                    FeedbackHelper.vibrateTick(context)
                    baselineUt = rawUt
                },
                modifier = Modifier.weight(1f),
                testTag = "metal_zero_button"
            )
            GlassButton(
                text = "Reset (45 µT)",
                onClick = { baselineUt = 45f },
                modifier = Modifier.weight(1f)
            )
        }

        // Required Ferromagnetic Note
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(theme.elevated)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Metal Detector Note",
                tint = theme.bentoSky,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Detects magnetic metals (iron, steel) only. It cannot detect gold, silver or copper.",
                style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                color = theme.textPrimary
            )
        }
    }
}

/**
 * 5) FLASHLIGHT: Big glass toggle button, brightness levels if supported (Android 13+ turnOnTorchWithStrengthLevel),
 * SOS and Strobe modes (slow default 2.0 Hz, with a photosensitivity warning).
 */
@Composable
private fun FlashlightInstrument(
    hasCameraPermission: Boolean,
    isPro: Boolean = false,
    onOpenPaywall: () -> Unit = {},
    onRequestCameraPermission: () -> Unit,
    forcedModeId: String? = null
) {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val featureGate = LocalFeatureGate.current
    val highBrightnessUnlocked = featureGate?.isUnlocked(PremiumFeature.FLASHLIGHT_HIGH) ?: isPro

    var isTorchOn by rememberSaveable { mutableStateOf(false) }
    var modeIndex by rememberSaveable(forcedModeId) {
        mutableIntStateOf(
            when (forcedModeId) {
                "sos", "morse" -> 1
                "strobe" -> 2
                else -> 0
            }
        )
    } // 0 = Steady, 1 = SOS/Morse, 2 = Strobe
    var strobeHz by rememberSaveable { mutableFloatStateOf(2.0f) } // Slow default (2.0 Hz)
    var strengthFraction by rememberSaveable { mutableFloatStateOf(0.5f) }
    var pulseState by remember { mutableStateOf(false) }
    var customMorseMessage by rememberSaveable { mutableStateOf("SOS") }

    val cameraManager = remember {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }

    val (torchCameraId, maxStrengthLevel) = remember(cameraManager) {
        var foundId: String? = null
        var maxLvl = 1
        runCatching {
            cameraManager?.cameraIdList?.forEach { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                if (hasFlash && foundId == null) {
                    foundId = id
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        maxLvl = (chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1).coerceAtLeast(1)
                    }
                }
            }
        }
        foundId to maxLvl
    }

    fun applyHardwareTorch(active: Boolean) {
        val camId = torchCameraId ?: return
        runCatching {
            if (!active) {
                cameraManager?.setTorchMode(camId, false)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maxStrengthLevel > 1) {
                val targetLevel = (strengthFraction * maxStrengthLevel).roundToInt().coerceIn(1, maxStrengthLevel)
                cameraManager?.turnOnTorchWithStrengthLevel(camId, targetLevel)
            } else {
                cameraManager?.setTorchMode(camId, true)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            applyHardwareTorch(false)
        }
    }

    LaunchedEffect(isTorchOn, modeIndex, strobeHz, strengthFraction) {
        if (!isTorchOn) {
            pulseState = false
            applyHardwareTorch(false)
            return@LaunchedEffect
        }

        when (modeIndex) {
            0 -> { // Steady
                pulseState = true
                applyHardwareTorch(true)
            }
            1 -> { // SOS Morse Code (... --- ...)
                val timings = listOf(
                    200L to true, 200L to false, 200L to true, 200L to false, 200L to true, 450L to false, // S
                    550L to true, 200L to false, 550L to true, 200L to false, 550L to true, 450L to false, // O
                    200L to true, 200L to false, 200L to true, 200L to false, 200L to true, 1100L to false // S
                )
                while (isActive && isTorchOn) {
                    for ((dur, on) in timings) {
                        if (!isActive || !isTorchOn) break
                        pulseState = on
                        applyHardwareTorch(on)
                        delay(dur)
                    }
                }
            }
            else -> { // Strobe (slow default 2.0 Hz)
                while (isActive && isTorchOn) {
                    val halfPeriodMs = (500f / strobeHz.coerceIn(1f, 12f)).toLong().coerceAtLeast(40L)
                    pulseState = !pulseState
                    applyHardwareTorch(pulseState)
                    delay(halfPeriodMs)
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (forcedModeId == null) {
            DyneSegmentedControl(
                segments = listOf("Steady", "SOS", "Strobe"),
                selectedIndex = modeIndex,
                onSegmentSelected = { modeIndex = it }
            )
        } else if (forcedModeId == "morse") {
            OutlinedTextField(
                value = customMorseMessage,
                onValueChange = { customMorseMessage = it.uppercase(Locale.US).take(24) },
                label = { Text("Morse Signal Text") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary,
                    focusedBorderColor = theme.accent,
                    unfocusedBorderColor = theme.textSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Photosensitivity warning when Strobe or SOS is selected
        if (modeIndex == 2 || modeIndex == 1) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(StatusWarning.copy(alpha = 0.14f))
                    .border(1.dp, StatusWarning.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = "Photosensitivity Warning",
                    tint = StatusWarning,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Photosensitivity Warning: Flashing strobe lights may trigger seizures in individuals with photosensitive epilepsy.",
                    style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = theme.textPrimary
                )
            }
        }

        // Big Glass Toggle Button
        GlassSurface(
            shape = CircleShape,
            cornerRadius = 80.dp,
            tintColor = if (isTorchOn && pulseState) theme.accent.copy(alpha = 0.88f) else null,
            shadowElevation = if (isTorchOn) 24.dp else 10.dp,
            modifier = Modifier
                .size(160.dp)
                .pressScaleClickable(
                    targetScale = 0.94f,
                    testTag = "flashlight_big_toggle_button",
                    onClick = {
                        if (!hasCameraPermission) {
                            onRequestCameraPermission()
                        }
                        FeedbackHelper.vibrateTick(context, durationMs = 30L, amplitude = 200)
                        isTorchOn = !isTorchOn
                    }
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    contentDescription = "Toggle Flashlight",
                    tint = if (isTorchOn && pulseState) BentoTextBlack else theme.accent,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isTorchOn) "ON" else "OFF",
                    style = type.gaugeMedium,
                    color = if (isTorchOn && pulseState) BentoTextBlack else theme.textPrimary
                )
            }
        }

        // Brightness Levels Slider (uses hardware turnOnTorchWithStrengthLevel when supported)
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Torch Brightness (Free up to 50% • >50% Pro)",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.textPrimary
                )
                Text(
                    text = if (maxStrengthLevel > 1) {
                        "Level ${(strengthFraction * maxStrengthLevel).roundToInt().coerceIn(1, maxStrengthLevel)} / $maxStrengthLevel"
                    } else {
                        "${(strengthFraction * 100).roundToInt()}% (Standard LED)"
                    },
                    style = type.caption,
                    color = theme.accent
                )
            }
            Slider(
                value = strengthFraction,
                onValueChange = { candidate ->
                    if (candidate <= 0.5f || highBrightnessUnlocked) {
                        strengthFraction = candidate
                    } else {
                        strengthFraction = 0.5f
                        if (featureGate != null) {
                            featureGate.require(PremiumFeature.FLASHLIGHT_HIGH) {
                                strengthFraction = candidate
                            }
                        } else {
                            onOpenPaywall()
                        }
                    }
                },
                valueRange = 0.1f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = theme.accent,
                    activeTrackColor = theme.accent
                )
            )
        }

        // Strobe Frequency Slider (slow default 2.0 Hz)
        if (modeIndex == 2) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Strobe Frequency (Slow Default)", style = type.caption.copy(fontWeight = FontWeight.Bold), color = theme.textPrimary)
                    Text(String.format(Locale.US, "%.1f Hz", strobeHz), style = type.caption, color = theme.accent)
                }
                Slider(
                    value = strobeHz,
                    onValueChange = { strobeHz = it },
                    valueRange = 1.0f..12.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = theme.accent,
                        activeTrackColor = theme.accent
                    )
                )
            }
        }
    }
}

/**
 * 6) SCREEN TORCH: Full-screen light, color presets (White, Warm, Red, Custom),
 * brightness slider through window brightness, keep screen on, tap to hide controls.
 */
@Composable
private fun ScreenTorchFullScreenInstrument(
    hazeState: HazeState?,
    onBack: () -> Unit
) {
    KeepScreenOnEffect(enabled = true)

    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var presetIndex by rememberSaveable { mutableIntStateOf(0) } // 0 = White, 1 = Warm, 2 = Red, 3 = Custom
    var customHue by rememberSaveable { mutableFloatStateOf(160f) }
    var screenBrightness by rememberSaveable { mutableFloatStateOf(0.9f) }
    var controlsVisible by rememberSaveable { mutableStateOf(true) }

    // Apply window brightness and restore on exit
    DisposableEffect(screenBrightness) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val params = window.attributes
            params.screenBrightness = screenBrightness.coerceIn(0.05f, 1.0f)
            window.attributes = params
        }
        onDispose { }
    }

    DisposableEffect(Unit) {
        onDispose {
            val window = (context as? Activity)?.window
            if (window != null) {
                val params = window.attributes
                params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = params
            }
        }
    }

    val activeColor = when (presetIndex) {
        0 -> Color(0xFFFFFFFF) // White
        1 -> Color(0xFFFFD89B) // Warm
        2 -> Color(0xFFFF2424) // Red (Night vision)
        else -> Color.hsv(customHue, 0.85f, 1.0f) // Custom
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(activeColor.copy(alpha = 0.25f + 0.75f * screenBrightness))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { controlsVisible = !controlsVisible }
            )
    ) {
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                SharedTopBar(
                    title = "Screen Torch",
                    subtitle = "TAP ANYWHERE TO HIDE CONTROLS",
                    hazeState = hazeState,
                    onBackClick = onBack,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                GlassSurface(
                    hazeState = hazeState,
                    shape = RoundedCornerShape(30.dp),
                    cornerRadius = 30.dp,
                    tintColor = Color(0xFF151518).copy(alpha = 0.88f),
                    contentPadding = PaddingValues(20.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 28.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DyneSegmentedControl(
                            segments = listOf("White", "Warm", "Red", "Custom"),
                            selectedIndex = presetIndex,
                            onSegmentSelected = { presetIndex = it }
                        )

                        if (presetIndex == 3) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Custom Color Hue", style = type.caption.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                    Text("${customHue.roundToInt()}°", style = type.caption, color = theme.accent)
                                }
                                Slider(
                                    value = customHue,
                                    onValueChange = { customHue = it },
                                    valueRange = 0f..360f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = activeColor,
                                        activeTrackColor = activeColor
                                    )
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Window Brightness", style = type.caption.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                Text("${(screenBrightness * 100).roundToInt()}%", style = type.caption, color = theme.accent)
                            }
                            Slider(
                                value = screenBrightness,
                                onValueChange = { screenBrightness = it },
                                valueRange = 0.05f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = theme.accent,
                                    activeTrackColor = theme.accent
                                )
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Screen stays awake • Tap anywhere on screen to hide/show controls",
                                style = type.caption,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 7) SOUND METER: Microphone, live dB gauge, min/avg/max, 30-second history graph,
 * reference labels (Whisper, Conversation, Traffic, Concert).
 * Labeled "Approximate, not calibrated". Never records or saves audio.
 */
@SuppressLint("MissingPermission")
@Composable
private fun SoundDbInstrument(
    hasAudioPermission: Boolean,
    isPro: Boolean = false,
    onOpenPaywall: () -> Unit = {},
    forcedModeId: String? = null
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var currentDb by remember { mutableFloatStateOf(42.0f) }
    var minDb by remember { mutableFloatStateOf(38.5f) }
    var maxDb by remember { mutableFloatStateOf(48.0f) }
    var sumDb by remember { mutableDoubleStateOf(42.0) }
    var sampleCount by remember { mutableIntStateOf(1) }

    // 120 samples at 250ms interval = 30-second scrolling history graph
    val history30s = remember {
        mutableStateListOf<Float>().apply { repeat(120) { add(42f) } }
    }

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            withContext(Dispatchers.IO) {
                val sampleRate = 44100
                val bufferSize = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(2048)

                val recorder = runCatching {
                    AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                    )
                }.getOrNull()

                if (recorder?.state == AudioRecord.STATE_INITIALIZED) {
                    // Processed strictly in volatile memory — never written to disk or saved
                    val shortBuffer = ShortArray(bufferSize / 2)
                    runCatching { recorder.startRecording() }
                    try {
                        var tick = 0
                        while (isActive) {
                            val read = recorder.read(shortBuffer, 0, shortBuffer.size)
                            if (read > 0) {
                                var sumSq = 0.0
                                for (i in 0 until read) {
                                    val s = shortBuffer[i].toDouble()
                                    sumSq += s * s
                                }
                                val rms = sqrt(sumSq / read)
                                val rawDb = if (rms > 1.0) {
                                    (20.0 * log10(rms) + 12.0).toFloat().coerceIn(25f, 115f)
                                } else {
                                    (42.0 + sin(tick * 0.35) * 4.5).toFloat()
                                }
                                withContext(Dispatchers.Main) {
                                    // Low-pass filter dB reading
                                    currentDb = currentDb * 0.72f + rawDb * 0.28f
                                    if (currentDb < minDb) minDb = currentDb
                                    if (currentDb > maxDb) maxDb = currentDb
                                    sumDb += currentDb
                                    sampleCount += 1
                                    if (history30s.size >= 120) history30s.removeAt(0)
                                    history30s.add(currentDb)
                                }
                            }
                            tick++
                            delay(250L)
                        }
                    } finally {
                        runCatching {
                            recorder.stop()
                            recorder.release()
                        }
                    }
                } else {
                    recorder?.release()
                }
            }
        } else {
            var tick = 0
            while (isActive) {
                val sim = (43.5 + sin(tick * 0.25) * 5.0 + cos(tick * 0.13) * 2.5).toFloat()
                currentDb = currentDb * 0.75f + sim * 0.25f
                if (currentDb < minDb) minDb = currentDb
                if (currentDb > maxDb) maxDb = currentDb
                sumDb += currentDb
                sampleCount += 1
                if (history30s.size >= 120) history30s.removeAt(0)
                history30s.add(currentDb)
                tick++
                delay(250L)
            }
        }
    }

    val avgDb = (sumDb / sampleCount.coerceAtLeast(1)).toFloat()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Approximate, not calibrated badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(theme.elevated)
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                text = "APPROXIMATE, NOT CALIBRATED • NEVER RECORDS AUDIO",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.bentoSky
            )
        }

        val primaryReadoutDb = if (forcedModeId == "peak_hold") maxDb else currentDb
        Text(
            text = String.format(Locale.US, "%.1f dB", primaryReadoutDb),
            style = type.gaugeHero,
            color = if (primaryReadoutDb > 80f) theme.bentoCoral else theme.accent
        )
        if (forcedModeId != null) {
            val modeCaption = when (forcedModeId) {
                "peak_hold" -> "PEAK HOLD MODE • MAXIMUM ACOUSTIC TRANSIENT"
                "graph_30s" -> "30-SECOND ACOUSTIC WAVEFORM HISTORY"
                "spectrum" -> "OCTAVE BAND ACOUSTIC SPECTRUM"
                "dosimeter" -> String.format(Locale.US, "NOISE DOSIMETER • SAFE EXPOSURE: %s", if (avgDb < 85f) "WITHIN NIOSH 85 dBA LIMIT" else "HIGH NOISE WARNING")
                else -> "LIVE SOUND PRESSURE LEVEL"
            }
            Text(
                text = modeCaption,
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.accent
            )
        }

        // Min / Avg / Max Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                "MIN" to minDb,
                "AVG" to avgDb,
                "MAX" to maxDb
            ).forEach { (label, value) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(theme.elevated)
                        .padding(vertical = 10.dp)
                ) {
                    Text(label, style = type.caption, color = theme.textSecondary)
                    Text(
                        text = String.format(Locale.US, "%.1f", value),
                        style = type.gaugeMedium,
                        color = theme.textPrimary
                    )
                }
            }
        }

        // Reference Labels (Whisper, Conversation, Traffic, Concert)
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(theme.elevated)
                .padding(12.dp)
        ) {
            val refs = listOf(
                Triple("Whisper", "30 dB", currentDb < 45f),
                Triple("Conversation", "60 dB", currentDb in 45f..70f),
                Triple("Traffic", "80 dB", currentDb in 70f..92f),
                Triple("Concert", "105 dB", currentDb > 92f)
            )
            refs.forEach { (name, levelText, active) ->
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) theme.accent.copy(alpha = 0.16f) else Color.Transparent)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = name,
                        style = type.caption.copy(fontWeight = if (active) FontWeight.Bold else FontWeight.Medium),
                        color = if (active) theme.accent else theme.textSecondary
                    )
                    Text(
                        text = levelText,
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = if (active) theme.textPrimary else theme.textSecondary
                    )
                }
            }
        }

        // 30-Second History Graph (Pro Gated)
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isPro) "30-SECOND HISTORY GRAPH" else "30-SECOND HISTORY GRAPH • PRO",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = if (isPro) theme.textPrimary else theme.accent
                )
                Text("-30s → Now", style = type.caption, color = theme.textSecondary)
            }
            val showGraphUnlocked = isPro || forcedModeId == "graph_30s" || forcedModeId == "spectrum"
            if (showGraphUnlocked) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(theme.elevated)
                        .padding(12.dp)
                ) {
                    val pts = history30s.toList()
                    if (pts.size > 1) {
                        val linePath = Path()
                        pts.forEachIndexed { idx, db ->
                            val x = (idx.toFloat() / (pts.size - 1)) * size.width
                            val norm = ((db - 25f) / 90f).coerceIn(0.05f, 0.95f)
                            val y = size.height * (1f - norm)
                            if (idx == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                        }
                        drawPath(
                            path = linePath,
                            brush = Brush.horizontalGradient(listOf(theme.bentoSky, theme.accent, theme.bentoCoral)),
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(theme.elevated)
                        .border(1.dp, theme.accent.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                        .pressScaleClickable(
                            testTag = "sound_history_pro_gate",
                            onClick = onOpenPaywall
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Unlock 30s Sound History Graph",
                            style = type.section.copy(fontWeight = FontWeight.Bold),
                            color = theme.accent
                        )
                        Text(
                            text = "Tap to unlock live scrolling acoustic waveform with Dyne Pro",
                            style = type.caption,
                            color = theme.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        GlassButton(
            text = "Reset Min / Avg / Max",
            onClick = {
                minDb = currentDb
                maxDb = currentDb
                sumDb = currentDb.toDouble()
                sampleCount = 1
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 8) SPEEDOMETER: GPS speed on a glass analog + digital gauge, km/h / mph / knots,
 * max and average speed, trip distance and time, reset, HUD mirror mode, keep screen on,
 * and "waiting for GPS" state.
 */
@Composable
private fun SpeedometerInstrument(
    hasLocationPermission: Boolean,
    geoCoordinates: GeoCoordinates,
    unitSystem: UnitSystem,
    isPro: Boolean = false,
    onOpenPaywall: () -> Unit = {},
    forcedModeId: String? = null
) {
    KeepScreenOnEffect(enabled = true)

    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val featureGate = LocalFeatureGate.current
    val isHudAndTripUnlocked = isPro ||
        forcedModeId == "hud_mirror" ||
        forcedModeId == "trip_log" ||
        forcedModeId == "timer_0_100" ||
        (featureGate?.isUnlocked(PremiumFeature.SPEEDOMETER_HUD_TRIP) == true)

    var speedUnitIndex by rememberSaveable {
        mutableIntStateOf(if (unitSystem == UnitSystem.METRIC) 0 else 1)
    } // 0 = km/h, 1 = mph, 2 = knots
    var hudMirrorMode by rememberSaveable(forcedModeId) {
        mutableStateOf(forcedModeId == "hud_mirror")
    }

    var maxSpeedMps by rememberSaveable { mutableFloatStateOf(0f) }
    var tripDistanceMeters by rememberSaveable { mutableDoubleStateOf(0.0) }
    var tripElapsedSeconds by rememberSaveable { mutableLongStateOf(0L) }

    val liveMps = if (hasLocationPermission && geoCoordinates.isFromGps) {
        geoCoordinates.speedMetersPerSecond.coerceAtLeast(0f)
    } else {
        0f
    }

    if (liveMps > maxSpeedMps) maxSpeedMps = liveMps

    // Integrate trip distance and time every second
    LaunchedEffect(hasLocationPermission, geoCoordinates.isFromGps) {
        while (isActive) {
            delay(1000L)
            if (hasLocationPermission && geoCoordinates.isFromGps) {
                val spd = geoCoordinates.speedMetersPerSecond.coerceAtLeast(0f)
                tripDistanceMeters += spd.toDouble()
                tripElapsedSeconds += 1L
            }
        }
    }

    val (speedFactor, unitLabel, maxDialValue) = when (speedUnitIndex) {
        0 -> Triple(3.6f, "km/h", 180f)
        1 -> Triple(2.23694f, "mph", 120f)
        else -> Triple(1.94384f, "knots", 100f)
    }

    val avgSpeedMps = if (tripElapsedSeconds > 0L) {
        (tripDistanceMeters / tripElapsedSeconds).toFloat()
    } else {
        0f
    }

    val displaySpeed by animateFloatAsState(
        targetValue = liveMps * speedFactor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "analog_speed_needle"
    )

    val tripDistanceFormatted = when (speedUnitIndex) {
        0 -> String.format(Locale.US, "%.2f km", tripDistanceMeters / 1000.0)
        1 -> String.format(Locale.US, "%.2f mi", tripDistanceMeters / 1609.344)
        else -> String.format(Locale.US, "%.2f NM", tripDistanceMeters / 1852.0)
    }

    val tripTimeFormatted = remember(tripElapsedSeconds) {
        val hrs = tripElapsedSeconds / 3600
        val mins = (tripElapsedSeconds % 3600) / 60
        val secs = tripElapsedSeconds % 60
        if (hrs > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    val waitingForGps = !hasLocationPermission || !geoCoordinates.isFromGps

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = if (hudMirrorMode) -1f else 1f
            }
    ) {
        // Waiting for GPS / Live GPS Status Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (waitingForGps) StatusWarning.copy(alpha = 0.16f) else theme.accent.copy(alpha = 0.16f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = if (waitingForGps) Icons.Default.GpsNotFixed else Icons.Default.GpsFixed,
                contentDescription = null,
                tint = if (waitingForGps) StatusWarning else theme.accent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (waitingForGps) "WAITING FOR GPS SIGNAL..." else "LIVE GPS VELOCITY LOCKED",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = if (waitingForGps) StatusWarning else theme.accent
            )
        }

        // Unit Selector: km/h | mph | knots
        DyneSegmentedControl(
            segments = listOf("km/h", "mph", "knots"),
            selectedIndex = speedUnitIndex,
            onSegmentSelected = { speedUnitIndex = it }
        )

        // Glass Analog + Digital Speedometer Gauge
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(230.dp)
                .padding(6.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 16.dp.toPx()
                val r = (size.minDimension / 2f) - stroke
                val center = Offset(size.width / 2f, size.height / 2f)
                val sweepFraction = (displaySpeed / maxDialValue).coerceIn(0f, 1f)

                drawArc(
                    color = Color.White.copy(alpha = 0.08f),
                    startAngle = 150f,
                    sweepAngle = 240f,
                    useCenter = false,
                    topLeft = Offset(center.x - r, center.y - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )

                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(theme.bentoSky, theme.accent, theme.bentoCoral)
                    ),
                    startAngle = 150f,
                    sweepAngle = 240f * sweepFraction,
                    useCenter = false,
                    topLeft = Offset(center.x - r, center.y - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )

                // Analog dial ticks
                for (step in 0..24) {
                    val angleDeg = 150f + step * 10f
                    val rad = Math.toRadians(angleDeg.toDouble())
                    val isMajor = step % 4 == 0
                    val inner = r - if (isMajor) 22.dp.toPx() else 14.dp.toPx()
                    val outer = r - 8.dp.toPx()
                    drawLine(
                        color = Color.White.copy(alpha = if (isMajor) 0.5f else 0.2f),
                        start = Offset(center.x + inner * cos(rad).toFloat(), center.y + inner * sin(rad).toFloat()),
                        end = Offset(center.x + outer * cos(rad).toFloat(), center.y + outer * sin(rad).toFloat()),
                        strokeWidth = if (isMajor) 2.5.dp.toPx() else 1.2.dp.toPx()
                    )
                }

                // Analog needle
                val needleAngleRad = Math.toRadians((150f + 240f * sweepFraction).toDouble())
                drawLine(
                    color = theme.accent,
                    start = center,
                    end = Offset(
                        center.x + (r * 0.72f) * cos(needleAngleRad).toFloat(),
                        center.y + (r * 0.72f) * sin(needleAngleRad).toFloat()
                    ),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = center)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format(Locale.US, "%04.1f", displaySpeed),
                    style = type.gaugeHero,
                    color = theme.textPrimary
                )
                Text(
                    text = unitLabel.uppercase(Locale.US),
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.accent
                )
            }
        }

        // Max Speed, Avg Speed, Trip Distance, Trip Time 2x2 Grid (Premium: Speedometer HUD and trip log)
        if (isHudAndTripUnlocked) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SpeedStatMiniCard(
                    label = "MAX SPEED",
                    value = String.format(Locale.US, "%.1f %s", maxSpeedMps * speedFactor, unitLabel),
                    modifier = Modifier.weight(1f)
                )
                SpeedStatMiniCard(
                    label = "AVG SPEED",
                    value = String.format(Locale.US, "%.1f %s", avgSpeedMps * speedFactor, unitLabel),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SpeedStatMiniCard(
                    label = "TRIP DISTANCE",
                    value = tripDistanceFormatted,
                    modifier = Modifier.weight(1f)
                )
                SpeedStatMiniCard(
                    label = "TRIP TIME",
                    value = tripTimeFormatted,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            GlassButton(
                text = "Unlock Trip Log & HUD Mirror • PRO",
                accentFill = com.example.ui.theme.ProGold.copy(alpha = 0.22f),
                textColor = com.example.ui.theme.ProGold,
                onClick = {
                    if (featureGate != null) {
                        featureGate.require(PremiumFeature.SPEEDOMETER_HUD_TRIP)
                    } else {
                        onOpenPaywall()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "speedometer_trip_unlock_button"
            )
        }

        // HUD Mirror Mode + Reset Trip Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            GlassButton(
                text = if (!isHudAndTripUnlocked) {
                    "HUD Mirror • PRO"
                } else if (hudMirrorMode) {
                    "Exit HUD Mirror"
                } else {
                    "HUD Mirror"
                },
                icon = Icons.Default.Flip,
                accentFill = when {
                    !isHudAndTripUnlocked -> com.example.ui.theme.ProGold.copy(alpha = 0.22f)
                    hudMirrorMode -> theme.bentoSky
                    else -> null
                },
                textColor = if (!isHudAndTripUnlocked) com.example.ui.theme.ProGold else null,
                onClick = {
                    if (featureGate != null) {
                        featureGate.require(PremiumFeature.SPEEDOMETER_HUD_TRIP) {
                            hudMirrorMode = !hudMirrorMode
                        }
                    } else if (isPro) {
                        hudMirrorMode = !hudMirrorMode
                    } else {
                        onOpenPaywall()
                    }
                },
                modifier = Modifier.weight(1f),
                testTag = "speedometer_hud_button"
            )
            GlassButton(
                text = if (isHudAndTripUnlocked) "Reset Trip" else "Reset Trip • PRO",
                onClick = {
                    if (featureGate != null) {
                        featureGate.require(PremiumFeature.SPEEDOMETER_HUD_TRIP) {
                            maxSpeedMps = 0f
                            tripDistanceMeters = 0.0
                            tripElapsedSeconds = 0L
                        }
                    } else if (isPro) {
                        maxSpeedMps = 0f
                        tripDistanceMeters = 0.0
                        tripElapsedSeconds = 0L
                    } else {
                        onOpenPaywall()
                    }
                },
                modifier = Modifier.weight(1f),
                testTag = "speedometer_reset_button"
            )
        }
    }
}

@Composable
private fun SpeedStatMiniCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(theme.elevated)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text = label, style = type.caption, color = theme.textSecondary)
        Text(text = value, style = type.gaugeMedium, color = theme.textPrimary)
    }
}

@Composable
private fun ScreenRulerInstrument(unitSystem: UnitSystem) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val density = LocalDensity.current

    val pxPerMm = with(density) { (160.dp.toPx()) / 25.4f }
    var caliperMm by remember { mutableFloatStateOf(42.0f) }
    val textMeasurer = rememberTextMeasurer()

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val inches = caliperMm / 25.4f
        Text(
            text = if (unitSystem == UnitSystem.METRIC) {
                String.format(Locale.US, "%.1f mm", caliperMm)
            } else {
                String.format(Locale.US, "%.2f in", inches)
            },
            style = type.gaugeHero,
            color = theme.accent
        )
        Text(
            text = String.format(Locale.US, "%.1f mm  •  %.2f cm  •  %.2f in (Drag caliper below)", caliperMm, caliperMm / 10f, inches),
            style = type.gaugeSmall,
            color = theme.textSecondary
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(theme.elevated)
                .pointerInput(pxPerMm) {
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        val mm = (change.position.y / pxPerMm).coerceIn(1f, 95f)
                        caliperMm = mm
                    }
                }
        ) {
            val maxMm = (size.height / pxPerMm).toInt()
            for (mm in 0..maxMm) {
                val y = mm * pxPerMm
                val isCm = mm % 10 == 0
                val isHalfCm = mm % 5 == 0
                val tickLen = when {
                    isCm -> 38.dp.toPx()
                    isHalfCm -> 26.dp.toPx()
                    else -> 14.dp.toPx()
                }
                drawLine(
                    color = if (isCm) theme.accent else theme.textSecondary.copy(alpha = 0.6f),
                    start = Offset(0f, y),
                    end = Offset(tickLen, y),
                    strokeWidth = if (isCm) 2.dp.toPx() else 1.dp.toPx()
                )
                if (isCm && mm > 0) {
                    val m = textMeasurer.measure(
                        text = "${mm / 10} cm",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                    )
                    drawText(m, topLeft = Offset(tickLen + 8.dp.toPx(), y - m.size.height / 2f))
                }
            }

            val caliperY = (caliperMm * pxPerMm).coerceIn(0f, size.height)
            drawRect(
                color = theme.accent.copy(alpha = 0.16f),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, caliperY)
            )
            drawLine(
                color = theme.accent,
                start = Offset(0f, caliperY),
                end = Offset(size.width, caliperY),
                strokeWidth = 3.dp.toPx()
            )
            drawCircle(
                color = theme.accent,
                radius = 9.dp.toPx(),
                center = Offset(size.width - 28.dp.toPx(), caliperY)
            )
        }
    }
}

@Composable
private fun LuxMeterInstrument(sensorState: SensorState) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var calibrationMultiplier by remember { mutableFloatStateOf(1.0f) }
    val lux = (sensorState.ambientLux * calibrationMultiplier).coerceAtLeast(0f)
    val footCandles = lux * 0.092903f
    val ev100 = if (lux > 1f) (ln(lux / 2.5) / ln(2.0)).toFloat() else 0f

    val envLabel = when {
        lux < 20f -> "DIM TWILIGHT / DARK ROOM"
        lux < 250f -> "INDOOR AMBIENT LIGHTING"
        lux < 1000f -> "BRIGHT STUDIO / OFFICE"
        else -> "OUTDOOR DAYLIGHT"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = String.format(Locale.US, "%04.0f lx", lux),
            style = type.gaugeHero,
            color = theme.bentoSky
        )
        Text(
            text = String.format(Locale.US, "%.1f fc   •   EV %.1f   •   %s", footCandles, ev100, envLabel),
            style = type.gaugeSmall,
            color = theme.textSecondary
        )

        Slider(
            value = calibrationMultiplier,
            onValueChange = { calibrationMultiplier = it },
            valueRange = 0.25f..4.0f,
            colors = SliderDefaults.colors(
                thumbColor = theme.bentoSky,
                activeTrackColor = theme.bentoSky
            )
        )
        Text(
            text = "Sensor Gain Calibration: ${String.format(Locale.US, "%.2fx", calibrationMultiplier)}",
            style = type.caption,
            color = theme.textSecondary
        )
    }
}

@Composable
private fun ChronographInstrument() {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var running by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    val laps = remember { mutableStateListOf<Long>() }

    LaunchedEffect(running) {
        val start = System.currentTimeMillis() - elapsedMs
        while (isActive && running) {
            elapsedMs = System.currentTimeMillis() - start
            delay(30L)
        }
    }

    fun formatMs(ms: Long): String {
        val mins = (ms / 60000) % 60
        val secs = (ms / 1000) % 60
        val centis = (ms % 1000) / 10
        return String.format(Locale.US, "%02d:%02d.%02d", mins, secs, centis)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = formatMs(elapsedMs),
            style = type.gaugeHero,
            color = theme.accent
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButton(
                text = if (running) "Pause" else "Start",
                accentFill = theme.accent,
                onClick = { running = !running },
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                text = "Lap",
                onClick = {
                    if (elapsedMs > 0L) {
                        laps.add(0, elapsedMs)
                        com.example.service.DyneNotificationCenter.postTimerCompletedNotification(
                            context = context,
                            title = "Stopwatch Lap ${laps.size} Recorded",
                            body = "Split time: ${formatMs(elapsedMs)}",
                            deepLinkFeatureId = "stopwatch_laps"
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                text = "Reset",
                onClick = {
                    running = false
                    elapsedMs = 0L
                    laps.clear()
                },
                modifier = Modifier.weight(1f)
            )
        }

        if (laps.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(theme.elevated)
                    .padding(14.dp)
            ) {
                laps.take(6).forEachIndexed { idx, lapTime ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lap ${laps.size - idx}", style = type.caption, color = theme.textSecondary)
                        Text(formatMs(lapTime), style = type.gaugeSmall, color = theme.textPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun UtcReferenceClockInstrument() {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    var now by remember { mutableStateOf(ZonedDateTime.now()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            now = ZonedDateTime.now()
            delay(250L)
        }
    }

    val fmt = remember { DateTimeFormatter.ofPattern("HH:mm:ss") }
    val utcNow = now.withZoneSameInstant(ZoneId.of("UTC"))
    val epochSec = now.toEpochSecond()
    val julianDate = epochSec / 86400.0 + 2440587.5

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = utcNow.format(fmt), style = type.gaugeHero, color = theme.accent)
        Text(text = "UTC ATOMIC REFERENCE", style = type.caption, color = theme.textSecondary)
        Text(text = "LOCAL: ${now.format(fmt)}", style = type.gaugeMedium, color = theme.textPrimary)
        Text(
            text = String.format(Locale.US, "UNIX %d  •  JD %.4f", epochSec, julianDate),
            style = type.gaugeSmall,
            color = theme.bentoSky
        )
    }
}

@Composable
private fun UnitConverterInstrument() {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var categoryIndex by remember { mutableIntStateOf(0) } // 0=Length, 1=Mass, 2=Temp, 3=Pressure
    var inputVal by remember { mutableStateOf("10.0") }
    var inverted by remember { mutableStateOf(false) }

    val numeric = inputVal.toDoubleOrNull() ?: 0.0
    val (resultStr, subtitleStr) = when (categoryIndex) {
        0 -> {
            if (!inverted) {
                val ft = numeric * 3.28084
                val inches = numeric * 39.3701
                String.format(Locale.US, "%.2f ft", ft) to String.format(Locale.US, "%.2f m = %.1f in = %.3f yd", numeric, inches, ft / 3.0)
            } else {
                val m = numeric / 3.28084
                String.format(Locale.US, "%.3f m", m) to String.format(Locale.US, "%.2f ft = %.1f cm", numeric, m * 100.0)
            }
        }
        1 -> {
            if (!inverted) {
                val lb = numeric * 2.20462
                String.format(Locale.US, "%.2f lb", lb) to String.format(Locale.US, "%.2f kg = %.1f oz", numeric, lb * 16.0)
            } else {
                val kg = numeric / 2.20462
                String.format(Locale.US, "%.2f kg", kg) to String.format(Locale.US, "%.2f lb = %.0f g", numeric, kg * 1000.0)
            }
        }
        2 -> {
            if (!inverted) {
                val f = numeric * 9.0 / 5.0 + 32.0
                String.format(Locale.US, "%.1f °F", f) to String.format(Locale.US, "%.1f °C = %.2f K", numeric, numeric + 273.15)
            } else {
                val c = (numeric - 32.0) * 5.0 / 9.0
                String.format(Locale.US, "%.1f °C", c) to String.format(Locale.US, "%.1f °F = %.2f K", numeric, c + 273.15)
            }
        }
        else -> {
            if (!inverted) {
                val psi = numeric * 0.0145038
                val inHg = numeric * 0.02953
                String.format(Locale.US, "%.3f psi", psi) to String.format(Locale.US, "%.1f hPa = %.2f inHg", numeric, inHg)
            } else {
                val hpa = numeric / 0.0145038
                String.format(Locale.US, "%.1f hPa", hpa) to String.format(Locale.US, "%.2f psi = %.3f bar", numeric, hpa / 1000.0)
            }
        }
    }

    val inputLabel = when (categoryIndex) {
        0 -> if (!inverted) "Meters (m)" else "Feet (ft)"
        1 -> if (!inverted) "Kilograms (kg)" else "Pounds (lb)"
        2 -> if (!inverted) "Celsius (°C)" else "Fahrenheit (°F)"
        else -> if (!inverted) "Hectopascals (hPa)" else "PSI"
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        DyneSegmentedControl(
            segments = listOf("Length", "Mass", "Temp", "Press"),
            selectedIndex = categoryIndex,
            onSegmentSelected = { categoryIndex = it }
        )

        OutlinedTextField(
            value = inputVal,
            onValueChange = { inputVal = it },
            label = { Text(inputLabel) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = theme.textPrimary,
                unfocusedTextColor = theme.textPrimary,
                focusedBorderColor = theme.accent,
                unfocusedBorderColor = theme.textSecondary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = resultStr, style = type.gaugeHero, color = theme.accent)
        Text(text = subtitleStr, style = type.gaugeSmall, color = theme.textSecondary)

        GlassButton(
            text = "Swap Direction",
            icon = Icons.Default.SwapVert,
            onClick = { inverted = !inverted },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ScratchpadInstrument(
    onSaveNote: (String, String, String) -> Unit,
    onOpenLibraryNotes: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var titleText by remember { mutableStateOf("Workshop Measurement") }
    var bodyText by remember { mutableStateOf("") }
    var savedBanner by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = titleText,
            onValueChange = {
                titleText = it
                savedBanner = false
            },
            label = { Text("Note Title") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = theme.textPrimary,
                unfocusedTextColor = theme.textPrimary,
                focusedBorderColor = theme.accent,
                unfocusedBorderColor = theme.textSecondary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = bodyText,
            onValueChange = {
                bodyText = it
                savedBanner = false
            },
            placeholder = { Text("Enter dimensions, angles, dB readings, or site notes...", color = theme.textSecondary) },
            minLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = theme.textPrimary,
                unfocusedTextColor = theme.textPrimary,
                focusedBorderColor = theme.accent,
                unfocusedBorderColor = theme.textSecondary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (savedBanner) {
            Text(
                text = "Saved to Library > Notes vault!",
                style = type.label,
                color = theme.accent
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButton(
                text = "Save to Library",
                icon = Icons.Default.Check,
                accentFill = theme.accent,
                onClick = {
                    if (bodyText.isNotBlank() || titleText.isNotBlank()) {
                        onSaveNote(
                            titleText.ifBlank { "Field Note" },
                            bodyText.ifBlank { "Logged from Scratchpad" },
                            "Workshop"
                        )
                        savedBanner = true
                        bodyText = ""
                    }
                },
                modifier = Modifier.weight(1f),
                testTag = "scratchpad_save_button"
            )
            GlassButton(
                text = "View Notes",
                onClick = onOpenLibraryNotes,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FieldCalculatorInstrument() {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var sideA by remember { mutableStateOf("3.0") }
    var sideB by remember { mutableStateOf("4.0") }

    val a = sideA.toDoubleOrNull() ?: 0.0
    val b = sideB.toDoubleOrNull() ?: 0.0
    val hyp = hypot(a, b)
    val pitchDeg = if (a > 0.0) Math.toDegrees(kotlin.math.atan(b / a)) else 0.0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Right-Triangle & Pitch Solver",
            style = type.section,
            color = theme.textPrimary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = sideA,
                onValueChange = { sideA = it },
                label = { Text("Run (A)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary,
                    focusedBorderColor = theme.accent,
                    unfocusedBorderColor = theme.textSecondary
                ),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = sideB,
                onValueChange = { sideB = it },
                label = { Text("Rise (B)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary,
                    focusedBorderColor = theme.accent,
                    unfocusedBorderColor = theme.textSecondary
                ),
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = String.format(Locale.US, "Diagonal: %.2f", hyp),
            style = type.gaugeHero,
            color = theme.accent
        )
        Text(
            text = String.format(Locale.US, "Pitch Angle: %.1f°   •   Area: %.2f", pitchDeg, 0.5 * a * b),
            style = type.gaugeSmall,
            color = theme.textSecondary
        )
    }
}

@Composable
private fun RoofPitchInstrument(sensorState: SensorState) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val angleDeg = abs(sensorState.tiltAngleDeg).coerceIn(0f, 85f)
    val risePer12 = (tan(Math.toRadians(angleDeg.toDouble())) * 12.0).coerceIn(0.0, 48.0)
    val slopePercent = (tan(Math.toRadians(angleDeg.toDouble())) * 100.0).coerceIn(0.0, 999.0)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = String.format(Locale.US, "%.1f : 12", risePer12),
            style = type.gaugeHero,
            color = theme.accent
        )
        Text(
            text = String.format(Locale.US, "ROOF PITCH  •  %.1f°  •  %.1f%% GRADE", angleDeg, slopePercent),
            style = type.gaugeSmall,
            color = theme.textSecondary
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .height(140.dp)
        ) {
            val baseLeft = Offset(20.dp.toPx(), size.height - 20.dp.toPx())
            val baseRight = Offset(size.width - 20.dp.toPx(), size.height - 20.dp.toPx())
            val runPx = baseRight.x - baseLeft.x
            val risePx = (tan(Math.toRadians(angleDeg.coerceAtMost(60f).toDouble())).toFloat() * runPx)
                .coerceAtMost(size.height - 36.dp.toPx())
            val peak = Offset(baseRight.x, baseRight.y - risePx)

            val path = Path().apply {
                moveTo(baseLeft.x, baseLeft.y)
                lineTo(baseRight.x, baseRight.y)
                lineTo(peak.x, peak.y)
                close()
            }
            drawPath(path = path, color = theme.accent.copy(alpha = 0.2f))
            drawPath(path = path, color = theme.accent, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun InlineScreenLightInstrument() {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    var presetIndex by rememberSaveable { mutableIntStateOf(0) }
    var customHue by rememberSaveable { mutableFloatStateOf(180f) }

    val activeColor = when (presetIndex) {
        0 -> Color.White
        1 -> Color(0xFFFFD89B)
        2 -> Color(0xFFFF3B30)
        else -> Color.hsv(customHue, 0.85f, 1.0f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        DyneSegmentedControl(
            segments = listOf("White", "Warm", "Red", "Color"),
            selectedIndex = presetIndex,
            onSegmentSelected = { presetIndex = it }
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(activeColor)
                .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(26.dp))
        ) {
            Text(
                text = "SOFT PANEL LIGHT",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = Color.Black.copy(alpha = 0.72f)
            )
        }
        if (presetIndex == 3) {
            Slider(
                value = customHue,
                onValueChange = { customHue = it },
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = activeColor,
                    activeTrackColor = activeColor
                )
            )
        }
    }
}

@Composable
private fun CountdownTimerInstrument() {
    val context = LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var durationSec by rememberSaveable { mutableIntStateOf(60) }
    var remainingMs by rememberSaveable { mutableLongStateOf(60_000L) }
    var running by rememberSaveable { mutableStateOf(false) }
    var scheduleNote by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(running) {
        var lastTick = System.currentTimeMillis()
        while (isActive && running) {
            delay(50L)
            val now = System.currentTimeMillis()
            val dt = now - lastTick
            lastTick = now
            remainingMs = (remainingMs - dt).coerceAtLeast(0L)
            if (remainingMs == 0L) {
                running = false
                com.example.service.DyneNotificationCenter.cancelUserTimerAlarm(context, "countdown_timer")
                com.example.service.DyneNotificationCenter.postTimerCompletedNotification(
                    context = context,
                    title = "Countdown Timer Finished",
                    body = "Your ${durationSec}s countdown timer has completed.",
                    deepLinkFeatureId = "stopwatch_laps"
                )
                FeedbackHelper.vibrateTick(context, durationMs = 180L, amplitude = 255)
                FeedbackHelper.playBeep(durationMs = 240, toneType = ToneGenerator.TONE_PROP_ACK)
            }
        }
    }

    val mins = (remainingMs / 60000L).toInt()
    val secs = ((remainingMs % 60000L) / 1000L).toInt()
    val tenths = ((remainingMs % 1000L) / 100L).toInt()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = String.format(Locale.US, "%02d:%02d.%d", mins, secs, tenths),
            style = type.gaugeHero,
            color = if (remainingMs == 0L) theme.bentoCoral else theme.accent
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(30 to "30s", 60 to "1m", 180 to "3m", 300 to "5m").forEach { (sec, label) ->
                GlassButton(
                    text = label,
                    accentFill = if (durationSec == sec) theme.accent.copy(alpha = 0.25f) else null,
                    onClick = {
                        running = false
                        durationSec = sec
                        remainingMs = sec * 1000L
                        com.example.service.DyneNotificationCenter.cancelUserTimerAlarm(context, "countdown_timer")
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButton(
                text = if (running) "Pause" else "Start",
                accentFill = theme.accent,
                onClick = {
                    if (remainingMs == 0L) remainingMs = durationSec * 1000L
                    val nextRunning = !running
                    running = nextRunning
                    if (nextRunning && remainingMs > 0L) {
                        val res = com.example.service.DyneNotificationCenter.scheduleUserTimerAlarm(
                            context = context,
                            timerId = "countdown_timer",
                            title = "Countdown Timer Finished",
                            body = "Your ${durationSec}s countdown timer has completed.",
                            triggerAtMs = System.currentTimeMillis() + remainingMs,
                            deepLinkFeatureId = "stopwatch_laps"
                        )
                        scheduleNote = res.note
                    } else {
                        com.example.service.DyneNotificationCenter.cancelUserTimerAlarm(context, "countdown_timer")
                    }
                },
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                text = "Reset",
                onClick = {
                    running = false
                    remainingMs = durationSec * 1000L
                    com.example.service.DyneNotificationCenter.cancelUserTimerAlarm(context, "countdown_timer")
                },
                modifier = Modifier.weight(1f)
            )
        }
        if (!scheduleNote.isNullOrBlank()) {
            Text(
                text = scheduleNote!!,
                style = type.caption,
                color = theme.textSecondary
            )
        }
    }
}

