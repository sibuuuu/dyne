package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.billing.AccessManager
import com.example.data.PreferencesRepository
import com.example.data.WeatherRepository
import com.example.domain.AstroCalculator
import com.example.domain.FeatureRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Jetpack Glance Home-Screen Widgets for Dyne:
 * 1) Quick Tile (1×1) — one feature of the user's choice, big icon, glass look; opens via FeatureGuard
 * 2) Feature Grid (2×2, 4×1, 4×2) — 4 to 8 features from FeatureRegistry (including individual modes), custom style
 * 3) Torch Widget (1×1 and 2×1) — toggles hardware flashlight WITHOUT opening app, saved intensity, TorchCallback sync, missing-flash handling
 * 4) Solunar Widget (2×2) — score and best window from cached/ephemeris data, refreshed by WorkManager every ~30m
 * 5) Weather Widget (2×2) — temperature and condition from cached data, refreshed by WorkManager every ~30m
 * 6) Scan Widget (2×1) — QR/Barcode and Document buttons opening scanners directly
 * 7) Sky Widget (2×2) — Sun & Moon azimuth/altitude and moon phase
 * 8) Compass Snapshot Widget (2×1)
 */
object DyneWidgetHelper {
    const val EXTRA_WIDGET_TARGET = "dyne_widget_target"
    const val ACTION_TOGGLE_TORCH_WIDGET = "com.example.widget.ACTION_TOGGLE_TORCH_WIDGET"
    const val UNIQUE_WIDGET_REFRESH_WORK = "dyne_widget_refresh_30m"

    val KEY_TORCH_ON = booleanPreferencesKey("glance_torch_on")
    val KEY_LAST_HEADING = stringPreferencesKey("glance_last_heading")

    suspend fun isWidgetsUnlocked(context: Context): Boolean {
        val prefs = PreferencesRepository(context).preferencesFlow.first()
        val isPro = prefs.cachedIsPro || prefs.simulateProEnabled
        if (isPro) return true
        return AccessManager.hasAccess(
            featureKey = "home_widgets",
            isPremium = false,
            prefs = prefs
        ) || prefs.isFeatureUnlockedTemporarily("home_widgets") ||
            prefs.isFeatureUnlockedTemporarily("home_screen_widgets") ||
            prefs.isToolUnlockedByReward("home_widgets")
    }

    fun launchIntent(context: Context, target: String): Intent {
        return Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_WIDGET_TARGET, target)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    fun openConfigIntent(
        context: Context,
        appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID,
        widgetKind: DyneWidgetKind = DyneWidgetKind.FEATURE_GRID_2X2
    ): Intent {
        return Intent(context, DyneWidgetConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(DyneWidgetConfigActivity.EXTRA_WIDGET_KIND_ID, widgetKind.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    fun requestPinWidget(
        context: Context,
        receiverClass: Class<*>,
        widgetKind: DyneWidgetKind = DyneWidgetKind.FEATURE_GRID_2X2
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val appWidgetManager = context.getSystemService(AppWidgetManager::class.java) ?: return false
        if (!appWidgetManager.isRequestPinAppWidgetSupported) return false
        val provider = ComponentName(context, receiverClass)
        val extras = Bundle().apply {
            putString(DyneWidgetConfigActivity.EXTRA_WIDGET_KIND_ID, widgetKind.id)
        }
        return runCatching {
            appWidgetManager.requestPinAppWidget(provider, extras, null)
        }.getOrDefault(false)
    }

    fun schedulePeriodicWidgetRefresh(context: Context) {
        runCatching {
            val req = PeriodicWorkRequestBuilder<DyneWidgetRefreshWorker>(30, TimeUnit.MINUTES)
                .setConstraints(Constraints.NONE)
                .addTag(UNIQUE_WIDGET_REFRESH_WORK)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WIDGET_REFRESH_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                req
            )
        }
    }

    suspend fun refreshTorchWidgets(context: Context) {
        runCatching {
            val manager = GlanceAppWidgetManager(context)
            listOf(
                Torch1x1GlanceWidget(),
                FlashlightGlanceWidget()
            ).forEach { widget ->
                manager.getGlanceIds(widget.javaClass).forEach { id ->
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                        prefs.toMutablePreferences().apply {
                            this[KEY_TORCH_ON] = DyneWidgetConfigStore.isTorchOn(context)
                        }
                    }
                    widget.update(context, id)
                }
            }
        }
    }

    suspend fun refreshAllWidgets(context: Context) {
        runCatching {
            DyneWidgetTorchSynchronizer.ensureRegistered(context)
            val manager = GlanceAppWidgetManager(context)
            listOf(
                QuickTileGlanceWidget(),
                FeatureGrid2x2GlanceWidget(),
                FeatureGrid4x1GlanceWidget(),
                FeatureGrid4x2GlanceWidget(),
                Torch1x1GlanceWidget(),
                FlashlightGlanceWidget(),
                SolunarGlanceWidget(),
                WeatherGlanceWidget(),
                QuickScanGlanceWidget(),
                SkyGlanceWidget(),
                CompassGlanceWidget()
            ).forEach { widget ->
                manager.getGlanceIds(widget.javaClass).forEach { id ->
                    widget.update(context, id)
                }
            }
        }
    }
}

/**
 * WorkManager background worker that refreshes Solunar, Weather, and Sky Glance widgets
 * approximately every 30 minutes from cached data with minimal battery use.
 */
class DyneWidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        DyneWidgetHelper.refreshAllWidgets(applicationContext)
        return Result.success()
    }
}

/**
 * BroadcastReceiver that toggles the hardware flashlight directly without opening the app
 * when triggered by a widget broadcast intent, keeping state synced via TorchCallback.
 */
class DyneTorchWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != DyneWidgetHelper.ACTION_TOGGLE_TORCH_WIDGET) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                DyneWidgetTorchSynchronizer.ensureRegistered(appContext)
                if (!DyneWidgetHelper.isWidgetsUnlocked(appContext)) {
                    appContext.startActivity(DyneWidgetHelper.launchIntent(appContext, "home_widgets"))
                } else {
                    DyneWidgetConfigStore.toggleHardwareTorch(appContext, appWidgetId)
                    DyneWidgetHelper.refreshTorchWidgets(appContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class ToggleFlashlightAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        DyneWidgetTorchSynchronizer.ensureRegistered(context)
        val unlocked = DyneWidgetHelper.isWidgetsUnlocked(context)
        if (!unlocked) {
            context.startActivity(DyneWidgetHelper.launchIntent(context, "home_widgets"))
            return
        }

        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)

        val (newState, _) = DyneWidgetConfigStore.toggleHardwareTorch(context, appWidgetId)
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[DyneWidgetHelper.KEY_TORCH_ON] = newState
            }
        }
        DyneWidgetHelper.refreshTorchWidgets(context)
    }
}

// ============================================================================
// 1. QUICK TILE WIDGET (1×1)
// ============================================================================

class QuickTileGlanceWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val unlocked = DyneWidgetHelper.isWidgetsUnlocked(context)
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
        val cfg = DyneWidgetConfigStore.getConfig(context, appWidgetId, DyneWidgetKind.QUICK_TILE_1X1)
        val palette = DyneWidgetConfigStore.resolvePalette(context, cfg)
        val featureId = cfg.featureIds.firstOrNull() ?: "flashlight_strobe"
        val feature = FeatureRegistry.getFeatureById(featureId)
        val featureTitle = feature?.title ?: "Flashlight Strobe"
        val badgeBmp = DyneWidgetConfigStore.createFeatureBadgeBitmap(
            featureId = featureId,
            accentArgb = palette.accentArgb,
            elevatedArgb = palette.elevatedArgb,
            style = cfg.style,
            sizePx = 96
        )

        provideContent {
            GlanceTheme {
                if (!unlocked) {
                    LockedWidgetCard(
                        palette = palette,
                        cornerRadiusDp = cfg.cornerRadiusDp,
                        compact1x1 = true,
                        unlockIntent = DyneWidgetHelper.launchIntent(context, "home_widgets")
                    )
                } else {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .cornerRadius(cfg.cornerRadiusDp.dp)
                            .background(ColorProvider(Color(palette.surfaceArgb)))
                            .clickable(actionStartActivity(DyneWidgetHelper.launchIntent(context, featureId)))
                            .padding(cfg.tileSize.padDp.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                provider = ImageProvider(badgeBmp),
                                contentDescription = featureTitle,
                                modifier = GlanceModifier.size((cfg.tileSize.iconDp + 16).dp)
                            )
                            if (cfg.showLabels) {
                                Spacer(GlanceModifier.height(4.dp))
                                Text(
                                    text = featureTitle,
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = ColorProvider(Color(palette.textPrimaryArgb)),
                                        fontSize = cfg.tileSize.textSp.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class QuickTileGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickTileGlanceWidget()
}

// ============================================================================
// 2. FEATURE GRID WIDGETS (2×2, 4×1, 4×2)
// ============================================================================

abstract class BaseFeatureGridGlanceWidget(
    private val defaultKind: DyneWidgetKind
) : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val unlocked = DyneWidgetHelper.isWidgetsUnlocked(context)
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
        val cfg = DyneWidgetConfigStore.getConfig(context, appWidgetId, defaultKind)
        val palette = DyneWidgetConfigStore.resolvePalette(context, cfg)

        val features = cfg.featureIds.ifEmpty { DyneWidgetConfigStore.defaultFeaturesForKind(defaultKind) }
        val bitmaps = features.associateWith { fId ->
            DyneWidgetConfigStore.createFeatureBadgeBitmap(
                featureId = fId,
                accentArgb = palette.accentArgb,
                elevatedArgb = palette.elevatedArgb,
                style = cfg.style,
                sizePx = 84
            )
        }

        provideContent {
            GlanceTheme {
                if (!unlocked) {
                    LockedWidgetCard(
                        palette = palette,
                        cornerRadiusDp = cfg.cornerRadiusDp,
                        compact1x1 = false,
                        unlockIntent = DyneWidgetHelper.launchIntent(context, "home_widgets")
                    )
                } else {
                    val rows: List<List<String>> = when (defaultKind) {
                        DyneWidgetKind.FEATURE_GRID_4X1 -> listOf(features.take(4))
                        DyneWidgetKind.FEATURE_GRID_2X2 -> features.take(4).chunked(2)
                        else -> features.take(8).chunked(4)
                    }
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .cornerRadius(cfg.cornerRadiusDp.dp)
                            .background(ColorProvider(Color(palette.surfaceArgb)))
                            .padding(10.dp)
                    ) {
                        Column(
                            modifier = GlanceModifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rows.forEachIndexed { rIdx, rowItems ->
                                if (rIdx > 0) Spacer(GlanceModifier.height(6.dp))
                                Row(
                                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    rowItems.forEachIndexed { cIdx, fId ->
                                        if (cIdx > 0) Spacer(GlanceModifier.width(6.dp))
                                        val feat = FeatureRegistry.getFeatureById(fId)
                                        val label = feat?.title ?: fId
                                        val bmp = bitmaps[fId]
                                        Box(
                                            modifier = GlanceModifier
                                                .defaultWeight()
                                                .fillMaxHeight()
                                                .cornerRadius((cfg.cornerRadiusDp - 6).coerceAtLeast(10).dp)
                                                .background(ColorProvider(Color(palette.elevatedArgb)))
                                                .clickable(actionStartActivity(DyneWidgetHelper.launchIntent(context, fId)))
                                                .padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (bmp != null) {
                                                    Image(
                                                        provider = ImageProvider(bmp),
                                                        contentDescription = label,
                                                        modifier = GlanceModifier.size((cfg.tileSize.iconDp + 8).dp)
                                                    )
                                                }
                                                if (cfg.showLabels) {
                                                    Spacer(GlanceModifier.height(3.dp))
                                                    Text(
                                                        text = label,
                                                        maxLines = 1,
                                                        style = TextStyle(
                                                            color = ColorProvider(Color(palette.textPrimaryArgb)),
                                                            fontSize = cfg.tileSize.textSp.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class FeatureGrid2x2GlanceWidget : BaseFeatureGridGlanceWidget(DyneWidgetKind.FEATURE_GRID_2X2)
class FeatureGrid4x1GlanceWidget : BaseFeatureGridGlanceWidget(DyneWidgetKind.FEATURE_GRID_4X1)
class FeatureGrid4x2GlanceWidget : BaseFeatureGridGlanceWidget(DyneWidgetKind.FEATURE_GRID_4X2)

class FeatureGrid2x2GlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FeatureGrid2x2GlanceWidget()
}

class FeatureGrid4x1GlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FeatureGrid4x1GlanceWidget()
}

class FeatureGrid4x2GlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FeatureGrid4x2GlanceWidget()
}

// ============================================================================
// 3. TORCH WIDGETS (1×1 and 2×1) — TOGGLES WITHOUT OPENING APP
// ============================================================================

class Torch1x1GlanceWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        DyneWidgetTorchSynchronizer.ensureRegistered(context)
        val unlocked = DyneWidgetHelper.isWidgetsUnlocked(context)
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
        val cfg = DyneWidgetConfigStore.getConfig(context, appWidgetId, DyneWidgetKind.TORCH_1X1)
        val palette = DyneWidgetConfigStore.resolvePalette(context, cfg)
        val hasFlash = DyneWidgetConfigStore.hasCameraFlash(context)
        val activeColor = if (DyneWidgetConfigStore.isTorchOn(context)) palette.accentArgb else palette.textSecondaryArgb
        val bmp = DyneWidgetConfigStore.createFeatureBadgeBitmap(
            featureId = "flashlight_normal",
            accentArgb = activeColor,
            elevatedArgb = palette.elevatedArgb,
            style = cfg.style,
            sizePx = 96
        )

        provideContent {
            val prefs = currentState<Preferences>()
            val isOn = prefs[DyneWidgetHelper.KEY_TORCH_ON] ?: DyneWidgetConfigStore.isTorchOn(context)
            GlanceTheme {
                if (!unlocked) {
                    LockedWidgetCard(
                        palette = palette,
                        cornerRadiusDp = cfg.cornerRadiusDp,
                        compact1x1 = true,
                        unlockIntent = DyneWidgetHelper.launchIntent(context, "home_widgets")
                    )
                } else {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .cornerRadius(cfg.cornerRadiusDp.dp)
                            .background(ColorProvider(Color(palette.surfaceArgb)))
                            .clickable(actionRunCallback<ToggleFlashlightAction>())
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                provider = ImageProvider(bmp),
                                contentDescription = "Torch",
                                modifier = GlanceModifier.size(34.dp)
                            )
                            if (cfg.showLabels) {
                                Spacer(GlanceModifier.height(4.dp))
                                Text(
                                    text = when {
                                        !hasFlash -> "No Flash"
                                        isOn -> "ON • L${cfg.torchIntensityLevel}"
                                        else -> "Torch OFF"
                                    },
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = ColorProvider(Color(if (isOn) palette.accentArgb else palette.textPrimaryArgb)),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class Torch1x1GlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = Torch1x1GlanceWidget()
}

class FlashlightGlanceWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        DyneWidgetTorchSynchronizer.ensureRegistered(context)
        val unlocked = DyneWidgetHelper.isWidgetsUnlocked(context)
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
        val cfg = DyneWidgetConfigStore.getConfig(context, appWidgetId, DyneWidgetKind.TORCH_2X1)
        val palette = DyneWidgetConfigStore.resolvePalette(context, cfg)
        val hasFlash = DyneWidgetConfigStore.hasCameraFlash(context)
        val intensityPct = (cfg.torchIntensityLevel * 100) / 6

        provideContent {
            val prefs = currentState<Preferences>()
            val isOn = prefs[DyneWidgetHelper.KEY_TORCH_ON] ?: DyneWidgetConfigStore.isTorchOn(context)
            GlanceTheme {
                if (!unlocked) {
                    LockedWidgetCard(
                        palette = palette,
                        cornerRadiusDp = cfg.cornerRadiusDp,
                        compact1x1 = false,
                        unlockIntent = DyneWidgetHelper.launchIntent(context, "home_widgets")
                    )
                } else {
                    WidgetGlassSurface(
                        title = "DYNE TORCH",
                        badge = when {
                            !hasFlash -> "NO FLASH"
                            isOn -> "ON • $intensityPct%"
                            else -> "OFF"
                        },
                        palette = palette,
                        cornerRadiusDp = cfg.cornerRadiusDp,
                        modifier = GlanceModifier.clickable(actionRunCallback<ToggleFlashlightAction>())
                    ) {
                        Text(
                            text = when {
                                !hasFlash -> "Camera flash unavailable on this device"
                                isOn -> "Flashlight Active (Level ${cfg.torchIntensityLevel}/6)"
                                else -> "Tap to toggle LED Torch"
                            },
                            style = TextStyle(
                                color = ColorProvider(Color(palette.textPrimaryArgb)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (cfg.showLabels) {
                            Spacer(GlanceModifier.height(3.dp))
                            Text(
                                text = "Instant hardware toggle • Synced via TorchCallback",
                                style = TextStyle(
                                    color = ColorProvider(Color(palette.textSecondaryArgb)),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

class FlashlightGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FlashlightGlanceWidget()
}

// ============================================================================
// 4. SOLUNAR SCORE WIDGET (2×2)
// ============================================================================

class SolunarGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val unlocked = DyneWidgetHelper.isWidgetsUnlocked(context)
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
        val cfg = DyneWidgetConfigStore.getConfig(context, appWidgetId, DyneWidgetKind.SOLUNAR_2X2)
        val palette = DyneWidgetConfigStore.resolvePalette(context, cfg)

        val userPrefs = PreferencesRepository(context).preferencesFlow.first()
        val report = AstroCalculator.calculateDailyReport(
            LocalDate.now(),
            userPrefs.manualLat,
            userPrefs.manualLng,
            ZoneId.systemDefault()
        )
        val timeFmt = if (userPrefs.use24HourTime) {
            DateTimeFormatter.ofPattern("HH:mm", Locale.US)
        } else {
            DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        }
        val nextMajor = report.majorPeriods.firstOrNull()
        val windowStr = if (nextMajor != null) {
            "Best window: ${nextMajor.startTime.format(timeFmt)}–${nextMajor.endTime.format(timeFmt)}"
        } else {
            "Best window: 11:00–13:00"
        }

        provideContent {
            GlanceTheme {
                if (!unlocked) {
                    LockedWidgetCard(
                        palette = palette,
                        cornerRadiusDp = cfg.cornerRadiusDp,
                        compact1x1 = false,
                        unlockIntent = DyneWidgetHelper.launchIntent(context, "home_widgets")
                    )
                } else {
                    WidgetGlassSurface(
                        title = "SOLUNAR FORECAST",
                        badge = "${report.fishingScore}/100",
                        palette = palette,
                        cornerRadiusDp = cfg.cornerRadiusDp,
                        modifier = GlanceModifier.clickable(
                            actionStartActivity(DyneWidgetHelper.launchIntent(context, "outdoor_solunar_today"))
                        )
                    ) {
                        Text(
                            text = windowStr,
                            style = TextStyle(
                                color = ColorProvider(Color(palette.textPrimaryArgb)),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            text = "${report.moonPhaseName} (${report.moonIlluminationPercent}%) • Hunt ${report.huntingScore}/100",
                            style = TextStyle(
                                color = ColorProvider(Color(palette.textSecondaryArgb)),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

class SolunarGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SolunarGlanceWidget()
}

// ============================================================================
// 5. WEATHER WIDGET (2×2)
// ============================================================================

class WeatherGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val unlocked = DyneWidgetHelper.isWidgetsUnlocked(context)
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
        val cfg = DyneWidgetConfigStore.getConfig(context, appWidgetId, DyneWidgetKind.WEATHER_2X2)
        val palette = DyneWidgetConfigStore.resolvePalette(context, cfg)

        val userPrefs = PreferencesRepository(context).preferencesFlow.first()
        val snap = WeatherRepository(context).getCachedSnapshot()
        val tempStr = if (snap != null) {
            if (userPrefs.temperatureUnit == "°F") {
                "${((snap.currentTempC * 9.0 / 5.0) + 32.0).toInt()}°F"
            } else {
                "${snap.currentTempC.toInt()}°C"
            }
        } else {
            "--°"
        }
        val condStr = if (snap != null) {
            WeatherRepository.wmoDescription(snap.currentWeatherCode)
        } else {
            "Open Dyne to refresh weather"
        }
        val subStr = if (snap != null) {
            "Wind ${snap.currentWindKmh.toInt()} km/h • UV ${snap.currentUvIndex.toInt()} • ${snap.locationLabel}"
        } else {
            userPrefs.locationName
        }

        provideContent {
            GlanceTheme {
                if (!unlocked) {
                    LockedWidgetCard(
                        palette = palette,
                        cornerRadiusDp = cfg.cornerRadiusDp,
                        compact1x1 = false,
                        unlockIntent = DyneWidgetHelper.launchIntent(context, "home_widgets")
                    )
                } else {
                    WidgetGlassSurface(
                        title = "OUTDOOR WEATHER",
                        badge = tempStr,
                        palette = palette,
                        cornerRadiusDp = cfg.cornerRadiusDp,
                        modifier = GlanceModifier.clickable(
                            actionStartActivity(DyneWidgetHelper.launchIntent(context, "outdoor_weather_current"))
                        )
                    ) {
                        Text(
                            text = condStr,
                            style = TextStyle(
                                color = ColorProvider(Color(palette.textPrimaryArgb)),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            text = subStr,
                            style = TextStyle(
                                color = ColorProvider(Color(palette.textSecondaryArgb)),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

class WeatherGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherGlanceWidget()
}

// ============================================================================
// 6. SCAN WIDGET (2×1): QR/BARCODE AND DOCUMENT BUTTONS
// ============================================================================

class QuickScanGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val unlocked = DyneWidgetHelper.isWidgetsUnlocked(context)
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
        val cfg = DyneWidgetConfigStore.getConfig(context, appWidgetId, DyneWidgetKind.SCAN_2X1)
        val palette = DyneWidgetConfigStore.resolvePalette(context, cfg)

        provideContent {
            GlanceTheme {
                if (!unlocked) {
                    LockedWidgetCard(
                        palette = palette,
                        cornerRadiusDp = cfg.cornerRadiusDp,
                        compact1x1 = false,
                        unlockIntent = DyneWidgetHelper.launchIntent(context, "home_widgets")
                    )
                } else {
                    WidgetGlassSurface(
                        title = "DYNE SCANNER",
                        badge = "DIRECT",
                        palette = palette,
                        cornerRadiusDp = cfg.cornerRadiusDp
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WidgetPillButton(
                                label = "QR / Barcode",
                                palette = palette,
                                intent = DyneWidgetHelper.launchIntent(context, "scanner_qr"),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            Spacer(GlanceModifier.width(8.dp))
                            WidgetPillButton(
                                label = "Document",
                                palette = palette,
                                intent = DyneWidgetHelper.launchIntent(context, "scanner_document"),
                                modifier = GlanceModifier.defaultWeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

class QuickScanGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickScanGlanceWidget()
}

// ============================================================================
// 7. SKY WIDGET (2×2): SUN & MOON AZIMUTH/ALTITUDE AND MOON PHASE
// ============================================================================

class SkyGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val unlocked = DyneWidgetHelper.isWidgetsUnlocked(context)
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
        val cfg = DyneWidgetConfigStore.getConfig(context, appWidgetId, DyneWidgetKind.SKY_2X2)
        val palette = DyneWidgetConfigStore.resolvePalette(context, cfg)

        val userPrefs = PreferencesRepository(context).preferencesFlow.first()
        val nowZdt = ZonedDateTime.now()
        val sunPos = AstroCalculator.getSunPosition(nowZdt, userPrefs.manualLat, userPrefs.manualLng)
        val moonPos = AstroCalculator.getMoonPosition(nowZdt, userPrefs.manualLat, userPrefs.manualLng)
        val report = AstroCalculator.calculateDailyReport(
            LocalDate.now(),
            userPrefs.manualLat,
            userPrefs.manualLng,
            ZoneId.systemDefault()
        )

        val sunStr = "Sun: Az ${sunPos.azimuthDeg.roundToInt()}° • Alt ${sunPos.altitudeDeg.roundToInt()}°"
        val moonStr = "Moon: Az ${moonPos.azimuthDeg.roundToInt()}° • Alt ${moonPos.altitudeDeg.roundToInt()}°"
        val phaseStr = "${report.moonPhaseName} (${report.moonIlluminationPercent}%)"

        provideContent {
            GlanceTheme {
                if (!unlocked) {
                    LockedWidgetCard(
                        palette = palette,
                        cornerRadiusDp = cfg.cornerRadiusDp,
                        compact1x1 = false,
                        unlockIntent = DyneWidgetHelper.launchIntent(context, "home_widgets")
                    )
                } else {
                    WidgetGlassSurface(
                        title = "SKY NOW • SUN & MOON",
                        badge = "${report.moonIlluminationPercent}%",
                        palette = palette,
                        cornerRadiusDp = cfg.cornerRadiusDp,
                        modifier = GlanceModifier.clickable(
                            actionStartActivity(DyneWidgetHelper.launchIntent(context, "outdoor_sky_dome"))
                        )
                    ) {
                        Text(
                            text = sunStr,
                            style = TextStyle(
                                color = ColorProvider(Color(palette.textPrimaryArgb)),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            text = moonStr,
                            style = TextStyle(
                                color = ColorProvider(Color(palette.textPrimaryArgb)),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            text = "Phase: $phaseStr",
                            style = TextStyle(
                                color = ColorProvider(Color(palette.textSecondaryArgb)),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

class SkyGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SkyGlanceWidget()
}

// ============================================================================
// 8. COMPASS SNAPSHOT & LAUNCHER WIDGET (2×1)
// ============================================================================

class CompassGlanceWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val unlocked = DyneWidgetHelper.isWidgetsUnlocked(context)
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
        val cfg = DyneWidgetConfigStore.getConfig(context, appWidgetId, DyneWidgetKind.COMPASS_2X1)
        val palette = DyneWidgetConfigStore.resolvePalette(context, cfg)

        provideContent {
            val prefs = currentState<Preferences>()
            val heading = prefs[DyneWidgetHelper.KEY_LAST_HEADING] ?: "0° N"
            GlanceTheme {
                if (!unlocked) {
                    LockedWidgetCard(
                        palette = palette,
                        cornerRadiusDp = cfg.cornerRadiusDp,
                        compact1x1 = false,
                        unlockIntent = DyneWidgetHelper.launchIntent(context, "home_widgets")
                    )
                } else {
                    WidgetGlassSurface(
                        title = "DYNE COMPASS",
                        badge = heading,
                        palette = palette,
                        cornerRadiusDp = cfg.cornerRadiusDp,
                        modifier = GlanceModifier.clickable(
                            actionStartActivity(DyneWidgetHelper.launchIntent(context, "compass"))
                        )
                    ) {
                        Text(
                            text = "Heading Snapshot • Tap for Live 60fps",
                            style = TextStyle(
                                color = ColorProvider(Color(palette.textPrimaryArgb)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            text = "Opens live magnetometer compass & waypoints",
                            style = TextStyle(
                                color = ColorProvider(Color(palette.textSecondaryArgb)),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

class CompassGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompassGlanceWidget()
}

// ============================================================================
// SHARED LOCKED STATE & GLASS SURFACE COMPOSABLES
// ============================================================================

@Composable
private fun LockedWidgetCard(
    palette: ResolvedWidgetPalette,
    cornerRadiusDp: Int,
    compact1x1: Boolean,
    unlockIntent: Intent
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(cornerRadiusDp.dp)
            .background(ColorProvider(Color(palette.surfaceArgb)))
            .clickable(actionStartActivity(unlockIntent))
            .padding(if (compact1x1) 8.dp else 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .cornerRadius(8.dp)
                    .background(ColorProvider(Color(0xFFF59E0B)))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "DYNE PRO",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF141108)),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = "Premium widget - tap to unlock",
                maxLines = if (compact1x1) 2 else 1,
                style = TextStyle(
                    color = ColorProvider(Color(palette.textPrimaryArgb)),
                    fontSize = if (compact1x1) 10.sp else 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            if (!compact1x1) {
                Spacer(GlanceModifier.height(3.dp))
                Text(
                    text = "Your widget configuration is saved",
                    style = TextStyle(
                        color = ColorProvider(Color(palette.textSecondaryArgb)),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun WidgetGlassSurface(
    title: String,
    badge: String,
    palette: ResolvedWidgetPalette,
    cornerRadiusDp: Int,
    modifier: GlanceModifier = GlanceModifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .cornerRadius(cornerRadiusDp.dp)
            .background(ColorProvider(Color(palette.surfaceArgb)))
            .padding(14.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        color = ColorProvider(Color(palette.accentArgb)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.defaultWeight())
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(8.dp)
                        .background(ColorProvider(Color(palette.elevatedArgb)))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badge,
                        style = TextStyle(
                            color = ColorProvider(Color(palette.accentArgb)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun WidgetPillButton(
    label: String,
    palette: ResolvedWidgetPalette,
    intent: Intent,
    modifier: GlanceModifier = GlanceModifier
) {
    Box(
        modifier = modifier
            .cornerRadius(12.dp)
            .background(ColorProvider(Color(palette.elevatedArgb)))
            .clickable(actionStartActivity(intent))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(Color(palette.accentArgb)),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
