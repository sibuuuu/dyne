package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.example.data.PreferencesRepository
import com.example.domain.FeatureRegistry
import com.example.ui.theme.ThemeEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class DyneWidgetKind(
    val id: String,
    val title: String,
    val sizeBadge: String,
    val subtitle: String,
    val supportsFeatureSelection: Boolean,
    val minFeatures: Int,
    val maxFeatures: Int
) {
    QUICK_TILE_1X1(
        id = "quick_tile_1x1",
        title = "Quick Tile",
        sizeBadge = "1×1",
        subtitle = "One feature of your choice, big icon, glass look; opens via FeatureGuard",
        supportsFeatureSelection = true,
        minFeatures = 1,
        maxFeatures = 1
    ),
    FEATURE_GRID_2X2(
        id = "feature_grid_2x2",
        title = "Feature Grid (2×2)",
        sizeBadge = "2×2",
        subtitle = "Select 4 to 8 features from FeatureRegistry with custom glass style",
        supportsFeatureSelection = true,
        minFeatures = 4,
        maxFeatures = 8
    ),
    FEATURE_GRID_4X1(
        id = "feature_grid_4x1",
        title = "Feature Grid (4×1)",
        sizeBadge = "4×1",
        subtitle = "Horizontal bar of 4 to 8 features from FeatureRegistry",
        supportsFeatureSelection = true,
        minFeatures = 4,
        maxFeatures = 8
    ),
    FEATURE_GRID_4X2(
        id = "feature_grid_4x2",
        title = "Feature Grid (4×2)",
        sizeBadge = "4×2",
        subtitle = "2-row deck of 4 to 8 features from FeatureRegistry",
        supportsFeatureSelection = true,
        minFeatures = 4,
        maxFeatures = 8
    ),
    TORCH_1X1(
        id = "torch_1x1",
        title = "Torch Widget (1×1)",
        sizeBadge = "1×1",
        subtitle = "Toggles flashlight without opening app, with saved intensity & TorchCallback sync",
        supportsFeatureSelection = false,
        minFeatures = 0,
        maxFeatures = 0
    ),
    TORCH_2X1(
        id = "torch_2x1",
        title = "Torch Widget (2×1)",
        sizeBadge = "2×1",
        subtitle = "Wide hardware flashlight toggle with on/off state & saved intensity level",
        supportsFeatureSelection = false,
        minFeatures = 0,
        maxFeatures = 0
    ),
    SOLUNAR_2X2(
        id = "solunar_2x2",
        title = "Solunar Widget",
        sizeBadge = "2×2",
        subtitle = "Solunar score and best window from cached data, refreshed every ~30m",
        supportsFeatureSelection = false,
        minFeatures = 0,
        maxFeatures = 0
    ),
    WEATHER_2X2(
        id = "weather_2x2",
        title = "Weather Widget",
        sizeBadge = "2×2",
        subtitle = "Temperature and condition from cached data, refreshed every ~30m",
        supportsFeatureSelection = false,
        minFeatures = 0,
        maxFeatures = 0
    ),
    SCAN_2X1(
        id = "scan_2x1",
        title = "Scan Widget",
        sizeBadge = "2×1",
        subtitle = "Direct QR/Barcode and Document scanner buttons",
        supportsFeatureSelection = false,
        minFeatures = 0,
        maxFeatures = 0
    ),
    SKY_2X2(
        id = "sky_2x2",
        title = "Sky Widget",
        sizeBadge = "2×2",
        subtitle = "Live Sun & Moon azimuth/altitude and moon phase",
        supportsFeatureSelection = false,
        minFeatures = 0,
        maxFeatures = 0
    ),
    COMPASS_2X1(
        id = "compass_2x1",
        title = "Compass Widget",
        sizeBadge = "2×1",
        subtitle = "Heading snapshot & one-tap launcher for live 60fps Compass",
        supportsFeatureSelection = false,
        minFeatures = 0,
        maxFeatures = 0
    );

    companion object {
        fun fromId(id: String?): DyneWidgetKind =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: FEATURE_GRID_2X2
    }
}

enum class WidgetVisualStyle(val id: String, val label: String) {
    GLASS("glass", "Glass"),
    SOLID("solid", "Solid"),
    OUTLINE("outline", "Outline"),
    GRADIENT("gradient", "Gradient");

    companion object {
        fun fromId(id: String?): WidgetVisualStyle =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: GLASS
    }
}

enum class WidgetTileSize(val id: String, val label: String, val iconDp: Int, val textSp: Int, val padDp: Int) {
    COMPACT("compact", "Compact", 18, 10, 6),
    MEDIUM("medium", "Medium", 22, 11, 10),
    LARGE("large", "Large", 28, 13, 12);

    companion object {
        fun fromId(id: String?): WidgetTileSize =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: MEDIUM
    }
}

data class DyneWidgetInstanceConfig(
    val appWidgetId: Int,
    val kind: DyneWidgetKind,
    val featureIds: List<String>,
    val style: WidgetVisualStyle = WidgetVisualStyle.GLASS,
    val accentColorHex: String = "AUTO",
    val tileSize: WidgetTileSize = WidgetTileSize.MEDIUM,
    val cornerRadiusDp: Int = 22,
    val showLabels: Boolean = true,
    val torchIntensityLevel: Int = 6 // 1..6
)

data class ResolvedWidgetPalette(
    val surfaceArgb: Int,
    val elevatedArgb: Int,
    val borderArgb: Int,
    val accentArgb: Int,
    val textPrimaryArgb: Int,
    val textSecondaryArgb: Int,
    val isDark: Boolean
)

/**
 * Persistent per-widget configuration store & hardware TorchCallback state synchronizer.
 * Configurations are stored by `appWidgetId` and are NEVER deleted when Premium expires.
 */
object DyneWidgetConfigStore {
    private const val PREFS_NAME = "dyne_widget_configs_v2"
    private const val KEY_TORCH_ON = "torch_active_state"
    private const val KEY_TORCH_HAS_FLASH = "torch_has_flash"
    private const val KEY_TORCH_SAVED_INTENSITY = "torch_saved_intensity_level"

    private val _configVersion = MutableStateFlow(0L)
    val configVersion: StateFlow<Long> = _configVersion.asStateFlow()

    val DEFAULT_GRID_FEATURES_4 = listOf(
        "flashlight_normal",
        "flashlight_strobe",
        "level_bubble",
        "scanner_qr"
    )

    val DEFAULT_GRID_FEATURES_8 = listOf(
        "flashlight_normal",
        "flashlight_strobe",
        "level_bubble",
        "scanner_qr",
        "scanner_document",
        "compass",
        "outdoor_solunar_today",
        "outdoor_weather_current"
    )

    val ACCENT_COLOR_PRESETS: List<Pair<String, Int>> = listOf(
        "AUTO" to 0xFFCCFF00.toInt(),
        "#CCFF00" to 0xFFCCFF00.toInt(),
        "#38BDF8" to 0xFF38BDF8.toInt(),
        "#34D399" to 0xFF34D399.toInt(),
        "#F59E0B" to 0xFFF59E0B.toInt(),
        "#A78BFA" to 0xFFA78BFA.toInt(),
        "#F43F5E" to 0xFFF43F5E.toInt()
    )

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun defaultFeaturesForKind(kind: DyneWidgetKind): List<String> = when (kind) {
        DyneWidgetKind.QUICK_TILE_1X1 -> listOf("flashlight_strobe")
        DyneWidgetKind.FEATURE_GRID_4X2 -> DEFAULT_GRID_FEATURES_8
        DyneWidgetKind.FEATURE_GRID_2X2, DyneWidgetKind.FEATURE_GRID_4X1 -> DEFAULT_GRID_FEATURES_4
        else -> emptyList()
    }

    fun getConfig(
        context: Context,
        appWidgetId: Int,
        fallbackKind: DyneWidgetKind = DyneWidgetKind.FEATURE_GRID_2X2
    ): DyneWidgetInstanceConfig {
        val p = prefs(context)
        val raw = p.getString("widget_$appWidgetId", null)
        if (raw.isNullOrBlank()) {
            // Check if a default template exists for this widget kind
            val kindDefaultRaw = p.getString("default_kind_${fallbackKind.id}", null)
            if (!kindDefaultRaw.isNullOrBlank()) {
                return parseConfigJson(appWidgetId, fallbackKind, kindDefaultRaw)
            }
            return DyneWidgetInstanceConfig(
                appWidgetId = appWidgetId,
                kind = fallbackKind,
                featureIds = defaultFeaturesForKind(fallbackKind),
                style = WidgetVisualStyle.GLASS,
                accentColorHex = "AUTO",
                tileSize = WidgetTileSize.MEDIUM,
                cornerRadiusDp = 22,
                showLabels = true,
                torchIntensityLevel = getSavedTorchIntensity(context)
            )
        }
        return parseConfigJson(appWidgetId, fallbackKind, raw)
    }

    private fun parseConfigJson(
        appWidgetId: Int,
        fallbackKind: DyneWidgetKind,
        rawJson: String
    ): DyneWidgetInstanceConfig {
        return runCatching {
            val obj = JSONObject(rawJson)
            val kind = DyneWidgetKind.fromId(obj.optString("kind", fallbackKind.id))
            val arr = obj.optJSONArray("featureIds") ?: JSONArray()
            val features = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val fId = arr.optString(i).trim()
                if (fId.isNotEmpty() && FeatureRegistry.getFeatureById(fId) != null) {
                    features.add(fId)
                }
            }
            val validFeatures = if (kind.supportsFeatureSelection && features.isEmpty()) {
                defaultFeaturesForKind(kind)
            } else if (kind == DyneWidgetKind.QUICK_TILE_1X1) {
                listOf(features.firstOrNull() ?: "flashlight_strobe")
            } else if (kind.supportsFeatureSelection) {
                features.take(8).let { list ->
                    if (list.size < 4) (list + DEFAULT_GRID_FEATURES_8).distinct().take(4) else list
                }
            } else {
                features
            }
            DyneWidgetInstanceConfig(
                appWidgetId = appWidgetId,
                kind = kind,
                featureIds = validFeatures,
                style = WidgetVisualStyle.fromId(obj.optString("style", "glass")),
                accentColorHex = obj.optString("accentColorHex", "AUTO"),
                tileSize = WidgetTileSize.fromId(obj.optString("tileSize", "medium")),
                cornerRadiusDp = obj.optInt("cornerRadiusDp", 22).coerceIn(8, 32),
                showLabels = obj.optBoolean("showLabels", true),
                torchIntensityLevel = obj.optInt("torchIntensityLevel", 6).coerceIn(1, 6)
            )
        }.getOrElse {
            DyneWidgetInstanceConfig(
                appWidgetId = appWidgetId,
                kind = fallbackKind,
                featureIds = defaultFeaturesForKind(fallbackKind)
            )
        }
    }

    fun saveConfig(context: Context, config: DyneWidgetInstanceConfig) {
        val obj = JSONObject().apply {
            put("appWidgetId", config.appWidgetId)
            put("kind", config.kind.id)
            val arr = JSONArray()
            config.featureIds.forEach { arr.put(it) }
            put("featureIds", arr)
            put("style", config.style.id)
            put("accentColorHex", config.accentColorHex)
            put("tileSize", config.tileSize.id)
            put("cornerRadiusDp", config.cornerRadiusDp.coerceIn(8, 32))
            put("showLabels", config.showLabels)
            put("torchIntensityLevel", config.torchIntensityLevel.coerceIn(1, 6))
        }
        val serialized = obj.toString()
        val edit = prefs(context).edit()
        if (config.appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            edit.putString("widget_${config.appWidgetId}", serialized)
        }
        // Also save as the default template for this widget kind so newly pinned instances inherit user choices
        edit.putString("default_kind_${config.kind.id}", serialized)
        edit.putInt(KEY_TORCH_SAVED_INTENSITY, config.torchIntensityLevel.coerceIn(1, 6))
        edit.apply()
        _configVersion.value = System.currentTimeMillis()
    }

    fun getAllSavedConfigs(context: Context): List<DyneWidgetInstanceConfig> {
        val p = prefs(context)
        val result = mutableListOf<DyneWidgetInstanceConfig>()
        for ((key, value) in p.all) {
            if (key.startsWith("widget_") && value is String) {
                val id = key.removePrefix("widget_").toIntOrNull() ?: continue
                if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    result.add(parseConfigJson(id, DyneWidgetKind.FEATURE_GRID_2X2, value))
                }
            }
        }
        return result.sortedBy { it.appWidgetId }
    }

    fun isTorchOn(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TORCH_ON, false)

    fun setTorchOnState(context: Context, isOn: Boolean) {
        prefs(context).edit().putBoolean(KEY_TORCH_ON, isOn).apply()
        _configVersion.value = System.currentTimeMillis()
    }

    fun getSavedTorchIntensity(context: Context): Int =
        prefs(context).getInt(KEY_TORCH_SAVED_INTENSITY, 6).coerceIn(1, 6)

    fun setSavedTorchIntensity(context: Context, level: Int) {
        prefs(context).edit().putInt(KEY_TORCH_SAVED_INTENSITY, level.coerceIn(1, 6)).apply()
        _configVersion.value = System.currentTimeMillis()
    }

    fun hasCameraFlash(context: Context): Boolean {
        return runCatching {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
            val camId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            val available = camId != null
            prefs(context).edit().putBoolean(KEY_TORCH_HAS_FLASH, available).apply()
            available
        }.getOrElse {
            prefs(context).getBoolean(KEY_TORCH_HAS_FLASH, false)
        }
    }

    /**
     * Toggles the hardware flashlight directly without opening the app.
     * Uses `CameraManager.turnOnTorchWithStrengthLevel` on Android 13+ when variable intensity is supported,
     * falling back cleanly to `CameraManager.setTorchMode`.
     */
    fun toggleHardwareTorch(context: Context, targetWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID): Pair<Boolean, String> {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return false to "Camera service unavailable"
        val camId = runCatching {
            cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()

        if (camId == null) {
            setTorchOnState(context, false)
            return false to "No camera flash on this device"
        }

        val nextState = !isTorchOn(context)
        val savedLevel = if (targetWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            getConfig(context, targetWidgetId, DyneWidgetKind.TORCH_2X1).torchIntensityLevel
        } else {
            getSavedTorchIntensity(context)
        }.coerceIn(1, 6)

        return runCatching {
            if (nextState && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val chars = cm.getCameraCharacteristics(camId)
                val maxLevel = chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                if (maxLevel > 1) {
                    val mappedStrength = ((savedLevel / 6.0) * maxLevel).toInt().coerceIn(1, maxLevel)
                    cm.turnOnTorchWithStrengthLevel(camId, mappedStrength)
                } else {
                    cm.setTorchMode(camId, true)
                }
            } else {
                cm.setTorchMode(camId, nextState)
            }
            setTorchOnState(context, nextState)
            nextState to if (nextState) "ON (${(savedLevel * 100) / 6}%)" else "OFF"
        }.getOrElse { e ->
            false to "Torch error: ${e.message ?: "unavailable"}"
        }
    }

    /**
     * Resolves widget colors from the app's active theme (`ThemeEngine`) and system dark/light configuration,
     * plus the per-widget visual style (`Glass`, `Solid`, `Outline`, `Gradient`) and custom accent color.
     */
    suspend fun resolvePalette(
        context: Context,
        config: DyneWidgetInstanceConfig
    ): ResolvedWidgetPalette {
        val userPrefs = runCatching { PreferencesRepository(context).preferencesFlow.first() }.getOrNull()
        val themeName = userPrefs?.effectiveThemeName(userPrefs.cachedIsPro || userPrefs.simulateProEnabled) ?: "Lime Night"
        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isSystemLight = uiMode == Configuration.UI_MODE_NIGHT_NO
        val appTheme = ThemeEngine.resolveTheme(themeName = themeName, darkTheme = !isSystemLight)

        val themeAccentArgb = appTheme.accent.value.shr(32).toInt()
        val resolvedAccentArgb = if (config.accentColorHex.equals("AUTO", ignoreCase = true)) {
            themeAccentArgb
        } else {
            ACCENT_COLOR_PRESETS.firstOrNull { it.first.equals(config.accentColorHex, ignoreCase = true) }?.second
                ?: themeAccentArgb
        }

        val baseSurface = if (isSystemLight) 0xFFF4F7FB.toInt() else 0xFF0E131F.toInt()
        val baseElevated = if (isSystemLight) 0xFFFFFFFF.toInt() else 0xFF182032.toInt()
        val textPrimary = if (isSystemLight) 0xFF0F172A.toInt() else 0xFFF8FAFC.toInt()
        val textSecondary = if (isSystemLight) 0xFF475569.toInt() else 0xFF94A3B8.toInt()

        val (surfaceArgb, elevatedArgb, borderArgb) = when (config.style) {
            WidgetVisualStyle.GLASS -> Triple(
                if (isSystemLight) 0xE6F4F7FB.toInt() else 0xDC101624.toInt(),
                if (isSystemLight) 0xEEFFFFFF.toInt() else 0xE61B2438.toInt(),
                (resolvedAccentArgb and 0x00FFFFFF) or 0x55000000
            )
            WidgetVisualStyle.SOLID -> Triple(
                baseSurface,
                baseElevated,
                (resolvedAccentArgb and 0x00FFFFFF) or 0x33000000
            )
            WidgetVisualStyle.OUTLINE -> Triple(
                if (isSystemLight) 0xB8F8FAFC.toInt() else 0xB80B0F19.toInt(),
                if (isSystemLight) 0xCCFFFFFF.toInt() else 0xCC131A2A.toInt(),
                (resolvedAccentArgb and 0x00FFFFFF) or 0xCC000000.toInt()
            )
            WidgetVisualStyle.GRADIENT -> Triple(
                blendArgb(baseSurface, resolvedAccentArgb, 0.22f),
                blendArgb(baseElevated, resolvedAccentArgb, 0.16f),
                (resolvedAccentArgb and 0x00FFFFFF) or 0x88000000.toInt()
            )
        }

        return ResolvedWidgetPalette(
            surfaceArgb = surfaceArgb,
            elevatedArgb = elevatedArgb,
            borderArgb = borderArgb,
            accentArgb = resolvedAccentArgb,
            textPrimaryArgb = textPrimary,
            textSecondaryArgb = textSecondary,
            isDark = !isSystemLight
        )
    }

    private fun blendArgb(c1: Int, c2: Int, ratio: Float): Int {
        val r = ratio.coerceIn(0f, 1f)
        val a1 = (c1 ushr 24) and 0xFF
        val r1 = (c1 ushr 16) and 0xFF
        val g1 = (c1 ushr 8) and 0xFF
        val b1 = c1 and 0xFF

        val r2 = (c2 ushr 16) and 0xFF
        val g2 = (c2 ushr 8) and 0xFF
        val b2 = c2 and 0xFF

        val nr = (r1 * (1f - r) + r2 * r).toInt().coerceIn(0, 255)
        val ng = (g1 * (1f - r) + g2 * r).toInt().coerceIn(0, 255)
        val nb = (b1 * (1f - r) + b2 * r).toInt().coerceIn(0, 255)
        return (a1 shl 24) or (nr shl 16) or (ng shl 8) or nb
    }

    /**
     * Renders a crisp glass badge bitmap for a feature or widget symbol so Glance `Image`
     * displays a distinctive vector glyph for any chosen `Feature` in `FeatureRegistry`.
     */
    fun createFeatureBadgeBitmap(
        featureId: String,
        accentArgb: Int,
        elevatedArgb: Int,
        style: WidgetVisualStyle,
        sizePx: Int = 96
    ): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val pad = sizePx * 0.06f
        val rect = RectF(pad, pad, sizePx - pad, sizePx - pad)
        val radius = sizePx * 0.28f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
            if (style == WidgetVisualStyle.GRADIENT) {
                shader = LinearGradient(
                    0f, 0f, sizePx.toFloat(), sizePx.toFloat(),
                    blendArgb(elevatedArgb, accentArgb, 0.38f),
                    elevatedArgb,
                    Shader.TileMode.CLAMP
                )
            } else {
                color = blendArgb(elevatedArgb, accentArgb, 0.18f)
            }
        }
        canvas.drawRoundRect(rect, radius, radius, bgPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = if (style == WidgetVisualStyle.OUTLINE) sizePx * 0.055f else sizePx * 0.03f
            color = (accentArgb and 0x00FFFFFF) or (if (style == WidgetVisualStyle.OUTLINE) 0xDD000000.toInt() else 0x66000000)
        }
        canvas.drawRoundRect(rect, radius, radius, strokePaint)

        val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentArgb or 0xFF000000.toInt()
            strokeWidth = sizePx * 0.075f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.style = Paint.Style.STROKE
        }

        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val r = sizePx * 0.22f
        val idLower = featureId.lowercase()

        when {
            idLower.contains("flashlight") || idLower.contains("torch") || idLower.contains("screen_light") -> {
                val path = Path().apply {
                    moveTo(cx + r * 0.2f, cy - r)
                    lineTo(cx - r * 0.6f, cy + r * 0.1f)
                    lineTo(cx + r * 0.1f, cy + r * 0.1f)
                    lineTo(cx - r * 0.2f, cy + r)
                    lineTo(cx + r * 0.6f, cy - r * 0.1f)
                    lineTo(cx - r * 0.1f, cy - r * 0.1f)
                    close()
                }
                canvas.drawPath(path, glyphPaint)
            }
            idLower.contains("qr") || idLower.contains("barcode") || idLower.contains("scan") -> {
                val s = r * 0.78f
                canvas.drawRect(cx - s, cy - s, cx - s * 0.15f, cy - s * 0.15f, glyphPaint)
                canvas.drawRect(cx + s * 0.15f, cy - s, cx + s, cy - s * 0.15f, glyphPaint)
                canvas.drawRect(cx - s, cy + s * 0.15f, cx - s * 0.15f, cy + s, glyphPaint)
                canvas.drawPoint(cx + s * 0.55f, cy + s * 0.55f, glyphPaint)
            }
            idLower.contains("level") || idLower.contains("angle") || idLower.contains("clinometer") -> {
                canvas.drawCircle(cx, cy, r, glyphPaint)
                canvas.drawLine(cx - r * 0.7f, cy, cx + r * 0.7f, cy, glyphPaint)
                glyphPaint.style = Paint.Style.FILL
                canvas.drawCircle(cx, cy, r * 0.22f, glyphPaint)
            }
            idLower.contains("compass") || idLower.contains("qibla") || idLower.contains("sky") -> {
                canvas.drawCircle(cx, cy, r, glyphPaint)
                val needle = Path().apply {
                    moveTo(cx, cy - r * 0.68f)
                    lineTo(cx + r * 0.3f, cy + r * 0.5f)
                    lineTo(cx, cy + r * 0.2f)
                    lineTo(cx - r * 0.3f, cy + r * 0.5f)
                    close()
                }
                canvas.drawPath(needle, glyphPaint)
            }
            idLower.contains("solunar") || idLower.contains("fish") || idLower.contains("moon") -> {
                canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -45f, 240f, false, glyphPaint)
                canvas.drawCircle(cx + r * 0.35f, cy - r * 0.35f, r * 0.18f, glyphPaint)
            }
            idLower.contains("weather") || idLower.contains("sun") || idLower.contains("uv") -> {
                canvas.drawCircle(cx, cy, r * 0.55f, glyphPaint)
                for (i in 0 until 8) {
                    val ang = Math.toRadians(i * 45.0)
                    val x1 = cx + (r * 0.75f * kotlin.math.cos(ang)).toFloat()
                    val y1 = cy + (r * 0.75f * kotlin.math.sin(ang)).toFloat()
                    val x2 = cx + (r * 1.02f * kotlin.math.cos(ang)).toFloat()
                    val y2 = cy + (r * 1.02f * kotlin.math.sin(ang)).toFloat()
                    canvas.drawLine(x1, y1, x2, y2, glyphPaint)
                }
            }
            else -> {
                val feature = FeatureRegistry.getFeatureById(featureId)
                val initials = (feature?.title ?: featureId)
                    .split(" ", "-", "/")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercaseChar().toString() }
                    .ifEmpty { "DY" }
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentArgb or 0xFF000000.toInt()
                    textSize = sizePx * 0.34f
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                }
                val fm = textPaint.fontMetrics
                val textY = cy - (fm.ascent + fm.descent) / 2f
                canvas.drawText(initials, cx, textY, textPaint)
            }
        }
        return bmp
    }
}

/**
 * Registers a system-wide `CameraManager.TorchCallback` so that whenever the flashlight is toggled
 * from Quick Settings, inside the app, or from the Torch Widget, the widget's on/off state stays in sync.
 */
object DyneWidgetTorchSynchronizer {
    private var registered = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun ensureRegistered(context: Context) {
        if (registered) return
        val appContext = context.applicationContext
        runCatching {
            val cm = appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
            cm.registerTorchCallback(
                object : CameraManager.TorchCallback() {
                    override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                        super.onTorchModeChanged(cameraId, enabled)
                        val prev = DyneWidgetConfigStore.isTorchOn(appContext)
                        if (prev != enabled) {
                            DyneWidgetConfigStore.setTorchOnState(appContext, enabled)
                            scope.launch {
                                DyneWidgetHelper.refreshTorchWidgets(appContext)
                            }
                        }
                    }
                },
                Handler(Looper.getMainLooper())
            )
            registered = true
        }
    }
}
