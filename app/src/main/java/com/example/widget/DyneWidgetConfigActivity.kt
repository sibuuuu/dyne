package com.example.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PreferencesRepository
import com.example.data.UserPreferences
import com.example.domain.Feature
import com.example.domain.FeatureRegistry
import com.example.domain.ToolCategory
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.DyneAppTheme
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import kotlinx.coroutines.launch

/**
 * Widget Configuration Activity:
 * Shown automatically when a widget is added to the launcher (`android:configure`) and
 * re-openable from the launcher (`reconfigurable`) or from `Settings > Home & Tools > Widgets`.
 */
class DyneWidgetConfigActivity : ComponentActivity() {

    companion object {
        const val EXTRA_WIDGET_KIND_ID = "extra_widget_kind_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Default result is CANCELED if user backs out before saving a newly placed launcher widget
        val cancelResult = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_CANCELED, cancelResult)

        val detectedKind = resolveWidgetKindForId(appWidgetId, intent?.getStringExtra(EXTRA_WIDGET_KIND_ID))
        val prefsRepository = PreferencesRepository(applicationContext)

        setContent {
            val userPrefs by prefsRepository.preferencesFlow.collectAsStateWithLifecycle(initialValue = UserPreferences())
            val isPro = userPrefs.cachedIsPro || userPrefs.simulateProEnabled

            DyneAppTheme(themeName = userPrefs.effectiveThemeName(isPro)) {
                DyneWidgetConfigScreen(
                    appWidgetId = appWidgetId,
                    initialKind = detectedKind,
                    onSaveAndFinish = { savedConfig ->
                        DyneWidgetConfigStore.saveConfig(applicationContext, savedConfig)
                        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        setResult(Activity.RESULT_OK, resultValue)
                        finish()
                    },
                    onCancel = {
                        finish()
                    }
                )
            }
        }
    }

    private fun resolveWidgetKindForId(appWidgetId: Int, explicitKindId: String?): DyneWidgetKind {
        if (!explicitKindId.isNullOrBlank()) {
            return DyneWidgetKind.fromId(explicitKindId)
        }
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val mgr = AppWidgetManager.getInstance(this)
            val info = mgr.getAppWidgetInfo(appWidgetId)
            val clsName = info?.provider?.className.orEmpty()
            return when {
                clsName.contains("QuickTileGlanceWidgetReceiver") -> DyneWidgetKind.QUICK_TILE_1X1
                clsName.contains("FeatureGrid2x2GlanceWidgetReceiver") -> DyneWidgetKind.FEATURE_GRID_2X2
                clsName.contains("FeatureGrid4x1GlanceWidgetReceiver") -> DyneWidgetKind.FEATURE_GRID_4X1
                clsName.contains("FeatureGrid4x2GlanceWidgetReceiver") -> DyneWidgetKind.FEATURE_GRID_4X2
                clsName.contains("Torch1x1GlanceWidgetReceiver") -> DyneWidgetKind.TORCH_1X1
                clsName.contains("FlashlightGlanceWidgetReceiver") -> DyneWidgetKind.TORCH_2X1
                clsName.contains("SolunarGlanceWidgetReceiver") -> DyneWidgetKind.SOLUNAR_2X2
                clsName.contains("WeatherGlanceWidgetReceiver") -> DyneWidgetKind.WEATHER_2X2
                clsName.contains("QuickScanGlanceWidgetReceiver") -> DyneWidgetKind.SCAN_2X1
                clsName.contains("SkyGlanceWidgetReceiver") -> DyneWidgetKind.SKY_2X2
                clsName.contains("CompassGlanceWidgetReceiver") -> DyneWidgetKind.COMPASS_2X1
                else -> DyneWidgetKind.FEATURE_GRID_2X2
            }
        }
        return DyneWidgetKind.FEATURE_GRID_2X2
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DyneWidgetConfigScreen(
    appWidgetId: Int,
    initialKind: DyneWidgetKind,
    onSaveAndFinish: (DyneWidgetInstanceConfig) -> Unit,
    onCancel: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val scope = rememberCoroutineScope()

    var selectedKind by remember { mutableStateOf(initialKind) }
    val initialCfg = remember(appWidgetId, selectedKind) {
        DyneWidgetConfigStore.getConfig(context, appWidgetId, selectedKind)
    }

    var selectedStyle by remember(initialCfg) { mutableStateOf(initialCfg.style) }
    var selectedAccentHex by remember(initialCfg) { mutableStateOf(initialCfg.accentColorHex) }
    var selectedTileSize by remember(initialCfg) { mutableStateOf(initialCfg.tileSize) }
    var cornerRadiusDp by remember(initialCfg) { mutableIntStateOf(initialCfg.cornerRadiusDp) }
    var showLabels by remember(initialCfg) { mutableStateOf(initialCfg.showLabels) }
    var torchIntensityLevel by remember(initialCfg) { mutableIntStateOf(initialCfg.torchIntensityLevel) }
    val selectedFeatureIds = remember(initialCfg) {
        mutableStateListOf<String>().apply {
            addAll(initialCfg.featureIds.ifEmpty { DyneWidgetConfigStore.defaultFeaturesForKind(selectedKind) })
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ToolCategory?>(null) }
    var isWidgetsUnlocked by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isWidgetsUnlocked = DyneWidgetHelper.isWidgetsUnlocked(context)
    }

    val resolvedAccentColor = remember(selectedAccentHex, theme.accent) {
        if (selectedAccentHex.equals("AUTO", ignoreCase = true)) {
            theme.accent
        } else {
            val argb = DyneWidgetConfigStore.ACCENT_COLOR_PRESETS
                .firstOrNull { it.first.equals(selectedAccentHex, ignoreCase = true) }?.second
            if (argb != null) Color(argb) else theme.accent
        }
    }

    val allCatalogFeatures = remember { FeatureRegistry.allFeatures }
    val filteredCatalogFeatures = remember(searchQuery, selectedCategory, allCatalogFeatures) {
        allCatalogFeatures.filter { f ->
            val catOk = selectedCategory == null || f.category == selectedCategory
            val q = searchQuery.trim()
            val queryOk = q.isEmpty() ||
                f.title.contains(q, ignoreCase = true) ||
                f.shortBenefit.contains(q, ignoreCase = true) ||
                f.id.contains(q, ignoreCase = true) ||
                f.aliases.any { it.contains(q, ignoreCase = true) }
            catOk && queryOk
        }
    }

    Scaffold(
        containerColor = theme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("widget_config_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cancel",
                            tint = theme.textPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "Configure ${selectedKind.title}",
                            style = type.title.copy(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold),
                            color = theme.textPrimary
                        )
                        Text(
                            text = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                "${selectedKind.sizeBadge} • Widget ID #$appWidgetId"
                            } else {
                                "${selectedKind.sizeBadge} • Default Widget Template"
                            },
                            style = type.caption,
                            color = theme.textSecondary
                        )
                    }
                }

                Button(
                    onClick = {
                        val finalFeatures = if (selectedKind == DyneWidgetKind.QUICK_TILE_1X1) {
                            listOf(selectedFeatureIds.firstOrNull() ?: "flashlight_strobe")
                        } else if (selectedKind.supportsFeatureSelection) {
                            selectedFeatureIds.toList().let { list ->
                                if (list.size < 4) (list + DyneWidgetConfigStore.DEFAULT_GRID_FEATURES_8).distinct().take(4)
                                else list.take(8)
                            }
                        } else {
                            emptyList()
                        }
                        val cfg = DyneWidgetInstanceConfig(
                            appWidgetId = appWidgetId,
                            kind = selectedKind,
                            featureIds = finalFeatures,
                            style = selectedStyle,
                            accentColorHex = selectedAccentHex,
                            tileSize = selectedTileSize,
                            cornerRadiusDp = cornerRadiusDp,
                            showLabels = showLabels,
                            torchIntensityLevel = torchIntensityLevel
                        )
                        scope.launch {
                            DyneWidgetConfigStore.saveConfig(context, cfg)
                            DyneWidgetHelper.refreshAllWidgets(context)
                            onSaveAndFinish(cfg)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = resolvedAccentColor,
                        contentColor = BentoTextBlack
                    ),
                    modifier = Modifier.testTag("widget_config_save_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Widget", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = innerPadding.calculateTopPadding() + 4.dp,
                bottom = 48.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // 1. LIVE WIDGET PREVIEW CARD
            item {
                WidgetLivePreviewCard(
                    kind = selectedKind,
                    featureIds = selectedFeatureIds,
                    style = selectedStyle,
                    accentColor = resolvedAccentColor,
                    tileSize = selectedTileSize,
                    cornerRadiusDp = cornerRadiusDp,
                    showLabels = showLabels,
                    torchIntensityLevel = torchIntensityLevel,
                    isWidgetsUnlocked = isWidgetsUnlocked
                )
            }

            // 2. WIDGET TYPE SELECTOR (When opened from Settings > Widgets as template editor)
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = theme.elevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "WIDGET TYPE",
                                style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                                color = resolvedAccentColor
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(DyneWidgetKind.entries) { k ->
                                    FilterChip(
                                        selected = selectedKind == k,
                                        onClick = {
                                            selectedKind = k
                                            selectedFeatureIds.clear()
                                            selectedFeatureIds.addAll(
                                                DyneWidgetConfigStore.getConfig(context, appWidgetId, k).featureIds
                                            )
                                        },
                                        label = { Text("${k.title} (${k.sizeBadge})") }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. STYLE, ACCENT COLOR, TILE SIZE, CORNER RADIUS & LABELS
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = theme.elevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = resolvedAccentColor, modifier = Modifier.size(18.dp))
                            Text(
                                text = "STYLE & APPEARANCE",
                                style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                                color = resolvedAccentColor
                            )
                        }

                        // Style: Glass, Solid, Outline, Gradient
                        Text("Surface Style", style = type.caption, color = theme.textSecondary)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            WidgetVisualStyle.entries.forEach { st ->
                                val selected = selectedStyle == st
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) resolvedAccentColor else theme.surface)
                                        .clickable { selectedStyle = st }
                                        .padding(vertical = 10.dp)
                                        .testTag("widget_style_${st.id}")
                                ) {
                                    Text(
                                        text = st.label,
                                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                                        color = if (selected) BentoTextBlack else theme.textPrimary
                                    )
                                }
                            }
                        }

                        // Accent Color Picker
                        Text("Accent Color (or Auto from App Theme)", style = type.caption, color = theme.textSecondary)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DyneWidgetConfigStore.ACCENT_COLOR_PRESETS.forEach { (hex, argb) ->
                                val isAuto = hex == "AUTO"
                                val selected = selectedAccentHex.equals(hex, ignoreCase = true)
                                val swatchColor = if (isAuto) theme.accent else Color(argb)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) Color.White else Color.Black.copy(alpha = 0.25f),
                                            shape = CircleShape
                                        )
                                        .clickable { selectedAccentHex = hex }
                                        .testTag("widget_accent_$hex")
                                ) {
                                    if (isAuto) {
                                        Text(
                                            text = "AUTO",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = BentoTextBlack
                                        )
                                    } else if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = BentoTextBlack,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Tile Size: Compact, Medium, Large
                        Text("Tile Density & Icon Scale", style = type.caption, color = theme.textSecondary)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            WidgetTileSize.entries.forEach { ts ->
                                val selected = selectedTileSize == ts
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) resolvedAccentColor else theme.surface)
                                        .clickable { selectedTileSize = ts }
                                        .padding(vertical = 9.dp)
                                        .testTag("widget_tile_size_${ts.id}")
                                ) {
                                    Text(
                                        text = ts.label,
                                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                                        color = if (selected) BentoTextBlack else theme.textPrimary
                                    )
                                }
                            }
                        }

                        // Corner Radius
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Corner Radius", style = type.caption, color = theme.textSecondary)
                            Text("${cornerRadiusDp}dp", style = type.caption.copy(fontWeight = FontWeight.Bold), color = resolvedAccentColor)
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(12, 16, 22, 28).forEach { rad ->
                                val selected = cornerRadiusDp == rad
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) resolvedAccentColor.copy(alpha = 0.22f) else theme.surface)
                                        .border(
                                            1.dp,
                                            if (selected) resolvedAccentColor else Color.White.copy(alpha = 0.08f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { cornerRadiusDp = rad }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "${rad}dp",
                                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                                        color = if (selected) resolvedAccentColor else theme.textPrimary
                                    )
                                }
                            }
                        }

                        // Show or hide labels
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show Feature Labels",
                                    style = type.body.copy(fontWeight = FontWeight.Bold),
                                    color = theme.textPrimary
                                )
                                Text(
                                    text = "Toggle text labels beneath widget icons",
                                    style = type.caption,
                                    color = theme.textSecondary
                                )
                            }
                            Switch(
                                checked = showLabels,
                                onCheckedChange = { showLabels = it },
                                modifier = Modifier.testTag("widget_show_labels_switch")
                            )
                        }

                        // Torch Saved Intensity Level (shown for Torch 1x1 & 2x1 widgets)
                        if (selectedKind == DyneWidgetKind.TORCH_1X1 || selectedKind == DyneWidgetKind.TORCH_2X1) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Saved Torch Brightness Level",
                                        style = type.body.copy(fontWeight = FontWeight.Bold),
                                        color = theme.textPrimary
                                    )
                                    Text(
                                        text = "Level $torchIntensityLevel/6 (${(torchIntensityLevel * 100) / 6}%)",
                                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                                        color = resolvedAccentColor
                                    )
                                }
                                Slider(
                                    value = torchIntensityLevel.toFloat(),
                                    onValueChange = { torchIntensityLevel = it.toInt().coerceIn(1, 6) },
                                    valueRange = 1f..6f,
                                    steps = 4,
                                    colors = SliderDefaults.colors(
                                        thumbColor = resolvedAccentColor,
                                        activeTrackColor = resolvedAccentColor
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 4. FEATURE REGISTRY PICKER (For Quick Tile 1x1 and Feature Grid 2x2, 4x1, 4x2)
            if (selectedKind.supportsFeatureSelection) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = theme.elevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = if (selectedKind == DyneWidgetKind.QUICK_TILE_1X1) {
                                            "SELECT 1 FEATURE (${allCatalogFeatures.size} AVAILABLE)"
                                        } else {
                                            "SELECT 4 TO 8 FEATURES (${selectedFeatureIds.size}/8 SELECTED)"
                                        },
                                        style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                                        color = resolvedAccentColor
                                    )
                                    Text(
                                        text = "Any feature or sub-mode in FeatureRegistry can be chosen (e.g., Flashlight Strobe, Level Bubble, Scan QR).",
                                        style = type.caption,
                                        color = theme.textSecondary
                                    )
                                }
                            }

                            // Selected features pill strip
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                selectedFeatureIds.forEachIndexed { idx, fId ->
                                    val feat = FeatureRegistry.getFeatureById(fId)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(resolvedAccentColor.copy(alpha = 0.18f))
                                            .border(1.dp, resolvedAccentColor, RoundedCornerShape(12.dp))
                                            .clickable {
                                                if (selectedKind != DyneWidgetKind.QUICK_TILE_1X1 && selectedFeatureIds.size > 4) {
                                                    selectedFeatureIds.remove(fId)
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${idx + 1}. ${feat?.title ?: fId}",
                                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                                            color = theme.textPrimary
                                        )
                                        if (selectedKind != DyneWidgetKind.QUICK_TILE_1X1 && selectedFeatureIds.size > 4) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = theme.textSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search features (e.g. Flashlight Strobe, Level Bubble, Scan QR)") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = resolvedAccentColor) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = resolvedAccentColor,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("widget_config_feature_search")
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    FilterChip(
                                        selected = selectedCategory == null,
                                        onClick = { selectedCategory = null },
                                        label = { Text("All (${allCatalogFeatures.size})") }
                                    )
                                }
                                items(ToolCategory.entries) { cat ->
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                        label = { Text(cat.title) }
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                filteredCatalogFeatures.take(45).forEach { feat ->
                                    val isSelected = feat.id in selectedFeatureIds
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isSelected) resolvedAccentColor.copy(alpha = 0.14f) else theme.surface)
                                            .border(
                                                1.dp,
                                                if (isSelected) resolvedAccentColor else Color.White.copy(alpha = 0.06f),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .clickable {
                                                if (selectedKind == DyneWidgetKind.QUICK_TILE_1X1) {
                                                    selectedFeatureIds.clear()
                                                    selectedFeatureIds.add(feat.id)
                                                } else {
                                                    if (isSelected) {
                                                        if (selectedFeatureIds.size > 4) {
                                                            selectedFeatureIds.remove(feat.id)
                                                        }
                                                    } else if (selectedFeatureIds.size < 8) {
                                                        selectedFeatureIds.add(feat.id)
                                                    }
                                                }
                                            }
                                            .padding(12.dp)
                                            .testTag("widget_feature_option_${feat.id}")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(resolvedAccentColor.copy(alpha = 0.16f))
                                            ) {
                                                Icon(
                                                    imageVector = feat.icon,
                                                    contentDescription = null,
                                                    tint = resolvedAccentColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = feat.title,
                                                    style = type.body.copy(fontWeight = FontWeight.Bold),
                                                    color = theme.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${feat.category.title} • ${feat.shortBenefit}",
                                                    style = type.caption,
                                                    color = theme.textSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = resolvedAccentColor
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

@Composable
private fun WidgetLivePreviewCard(
    kind: DyneWidgetKind,
    featureIds: List<String>,
    style: WidgetVisualStyle,
    accentColor: Color,
    tileSize: WidgetTileSize,
    cornerRadiusDp: Int,
    showLabels: Boolean,
    torchIntensityLevel: Int,
    isWidgetsUnlocked: Boolean
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    var previewLockedState by remember { mutableStateOf(false) }

    val cardModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(cornerRadiusDp.dp))
        .then(
            when (style) {
                WidgetVisualStyle.GLASS -> Modifier
                    .background(theme.elevated.copy(alpha = 0.82f))
                    .border(1.5.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(cornerRadiusDp.dp))
                WidgetVisualStyle.SOLID -> Modifier
                    .background(theme.elevated)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(cornerRadiusDp.dp))
                WidgetVisualStyle.OUTLINE -> Modifier
                    .background(theme.background.copy(alpha = 0.65f))
                    .border(2.dp, accentColor, RoundedCornerShape(cornerRadiusDp.dp))
                WidgetVisualStyle.GRADIENT -> Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.28f),
                                theme.elevated
                            )
                        )
                    )
                    .border(1.5.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(cornerRadiusDp.dp))
            }
        )
        .padding(16.dp)
        .testTag("widget_live_preview_card")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "LIVE PREVIEW (${kind.sizeBadge} • ${style.label.uppercase()})",
                style = type.caption.copy(fontWeight = FontWeight.ExtraBold),
                color = accentColor
            )
            FilterChip(
                selected = previewLockedState,
                onClick = { previewLockedState = !previewLockedState },
                label = {
                    Text(
                        text = if (previewLockedState) "Showing Free Locked State" else "Preview Free Locked State",
                        fontSize = 11.sp
                    )
                }
            )
        }

        Box(modifier = cardModifier) {
            if (previewLockedState || !isWidgetsUnlocked) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ProGold)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = BentoTextBlack, modifier = Modifier.size(12.dp))
                        Text("DYNE PRO", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = BentoTextBlack)
                    }
                    Text(
                        text = "Premium widget - tap to unlock",
                        style = type.body.copy(fontWeight = FontWeight.Bold),
                        color = theme.textPrimary
                    )
                    Text(
                        text = "Your configuration is saved and ready when unlocked",
                        style = type.caption,
                        color = theme.textSecondary
                    )
                }
            } else {
                when (kind) {
                    DyneWidgetKind.QUICK_TILE_1X1 -> {
                        val fId = featureIds.firstOrNull() ?: "flashlight_strobe"
                        val feat = FeatureRegistry.getFeatureById(fId)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size((tileSize.iconDp + 26).dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(accentColor.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = feat?.icon ?: Icons.Default.GridView,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size((tileSize.iconDp + 6).dp)
                                )
                            }
                            if (showLabels) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = feat?.title ?: fId,
                                    fontSize = tileSize.textSp.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.textPrimary
                                )
                            }
                        }
                    }

                    DyneWidgetKind.FEATURE_GRID_2X2,
                    DyneWidgetKind.FEATURE_GRID_4X1,
                    DyneWidgetKind.FEATURE_GRID_4X2 -> {
                        val itemsToRender = when (kind) {
                            DyneWidgetKind.FEATURE_GRID_4X1 -> featureIds.take(4)
                            DyneWidgetKind.FEATURE_GRID_2X2 -> featureIds.take(4)
                            else -> featureIds.take(8)
                        }
                        val rows = if (kind == DyneWidgetKind.FEATURE_GRID_2X2) {
                            itemsToRender.chunked(2)
                        } else {
                            itemsToRender.chunked(4)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            rows.forEach { rowList ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowList.forEach { fId ->
                                        val feat = FeatureRegistry.getFeatureById(fId)
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape((cornerRadiusDp - 6).coerceAtLeast(10).dp))
                                                .background(theme.surface.copy(alpha = 0.85f))
                                                .padding(tileSize.padDp.dp)
                                        ) {
                                            Icon(
                                                imageVector = feat?.icon ?: Icons.Default.GridView,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(tileSize.iconDp.dp)
                                            )
                                            if (showLabels) {
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = feat?.title ?: fId,
                                                    fontSize = tileSize.textSp.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = theme.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    DyneWidgetKind.TORCH_1X1, DyneWidgetKind.TORCH_2X1 -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("DYNE TORCH", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
                                Text(
                                    text = "Direct Toggle • Level $torchIntensityLevel/6 (${(torchIntensityLevel * 100) / 6}%)",
                                    style = type.body.copy(fontWeight = FontWeight.Bold),
                                    color = theme.textPrimary
                                )
                                if (showLabels) {
                                    Text("Synced with CameraManager.TorchCallback", style = type.caption, color = theme.textSecondary)
                                }
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accentColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("ON / OFF", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
                            }
                        }
                    }

                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(kind.title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
                            Text(kind.subtitle, style = type.body.copy(fontWeight = FontWeight.Bold), color = theme.textPrimary)
                        }
                    }
                }
            }
        }
    }
}
