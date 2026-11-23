package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.AccessManager
import com.example.billing.FeatureGate
import com.example.billing.PremiumFeature
import com.example.data.PreferencesRepository
import com.example.data.UserPreferences
import com.example.ui.components.GlassCard
import com.example.ui.components.ProBadge
import com.example.ui.components.ProCountdownRing
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.ThemeEngine
import com.example.widget.CompassGlanceWidgetReceiver
import com.example.widget.DyneWidgetHelper
import com.example.widget.FlashlightGlanceWidgetReceiver
import com.example.widget.QuickScanGlanceWidgetReceiver
import com.example.widget.SolunarGlanceWidgetReceiver
import com.example.widget.WeatherGlanceWidgetReceiver
import kotlinx.coroutines.launch

/**
 * Premium Customization Studio Card:
 * - Custom Accent Color Picker (10 curated swatches + custom Hex input + theme default reset)
 * - Tile Styles: Glass, Solid, Outline (while Free keeps Tile Size S / M / L)
 * - Hero Carousel Styles: Glass Editorial, Compact Pill, Minimal Split
 * - Icon Sets: Default Filled, Precision Outlined, Duotone Neon, Monochrome Minimal
 * - 6 Extra Pro Themes + 2 Free Base Themes summary
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PremiumStyleAndAccentStudioCard(
    userPreferences: UserPreferences,
    preferencesRepository: PreferencesRepository,
    featureGate: FeatureGate?,
    isPro: Boolean,
    onRequestPaywall: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val scope = rememberCoroutineScope()

    val isUnlocked = isPro ||
        (featureGate?.hasAccess(AccessManager.KEY_CUSTOMIZE, prefs = userPreferences) == true)
    val remMs = featureGate?.getRemainingMsForKey(AccessManager.KEY_CUSTOMIZE, prefs = userPreferences) ?: 0L

    val activeAccentHex = userPreferences.effectiveCustomAccentHex(isPro)
    val activeTileStyle = userPreferences.effectiveHomeTileStyle(isPro)
    val activeHeroStyle = userPreferences.effectiveHeroCarouselStyle(isPro)
    val activeIconSetStyle = userPreferences.effectiveIconSetStyle(isPro)

    var customHexInput by remember(activeAccentHex) {
        mutableStateOf(activeAccentHex.ifBlank { "#CCFF00" })
    }

    fun guardCustomization(action: suspend () -> Unit) {
        if (isUnlocked) {
            scope.launch { action() }
        } else if (featureGate != null) {
            featureGate.guardNavigate(
                featureKey = AccessManager.KEY_CUSTOMIZE,
                prefs = userPreferences,
                onAllowed = {
                    scope.launch { action() }
                }
            )
        } else {
            onRequestPaywall()
        }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = theme.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "STYLE & ACCENT STUDIO (PRO)",
                        color = theme.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }
                if (!isPro && remMs > 0L && featureGate != null) {
                    ProCountdownRing(
                        progress = featureGate.getCountdownProgressForKey(AccessManager.KEY_CUSTOMIZE, prefs = userPreferences),
                        label = featureGate.formatRemainingRingBadge(remMs)
                    )
                } else {
                    ProBadge()
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Custom Accent Color Picker
            Text(
                text = "Custom Accent Color Picker",
                color = theme.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (activeAccentHex.isBlank()) "Using theme default accent" else "Active custom accent: $activeAccentHex",
                color = theme.textSecondary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Default theme accent chip
                val isDefaultSelected = activeAccentHex.isBlank()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDefaultSelected) theme.accent.copy(alpha = 0.2f) else theme.elevated)
                        .border(
                            1.dp,
                            if (isDefaultSelected) theme.accent else theme.textSecondary.copy(alpha = 0.25f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            scope.launch { preferencesRepository.setCustomAccentHex("") }
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "Theme Default",
                        color = theme.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                ThemeEngine.customAccentPresets.forEach { (label, hex) ->
                    val swatchColor = ThemeEngine.parseCustomHexColor(hex) ?: theme.accent
                    val isSelected = activeAccentHex.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable {
                                guardCustomization {
                                    preferencesRepository.setCustomAccentHex(hex)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = label,
                                tint = Color(0xFF0A0A0C),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customHexInput,
                    onValueChange = { customHexInput = it.take(7) },
                    label = { Text("Hex (#RRGGBB)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val validColor = ThemeEngine.parseCustomHexColor(customHexInput)
                        if (validColor != null) {
                            val normalized = "#" + customHexInput.trim().removePrefix("#").uppercase()
                            guardCustomization {
                                preferencesRepository.setCustomAccentHex(normalized)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.accent,
                        contentColor = theme.onAccent
                    )
                ) {
                    Text("Apply Hex", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = theme.textSecondary.copy(alpha = 0.14f))
            Spacer(modifier = Modifier.height(12.dp))

            // 2. Tile Styles (Glass, Solid, Outline)
            Text(
                text = "Home Tile Style (Pro) • Free keeps S / M / L tile sizes",
                color = theme.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "GLASS" to "Glass",
                    "SOLID" to "Solid",
                    "OUTLINE" to "Outline"
                ).forEach { (id, label) ->
                    val selected = activeTileStyle.equals(id, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) theme.accent else theme.elevated)
                            .border(
                                1.dp,
                                if (selected) theme.accent else theme.textSecondary.copy(alpha = 0.22f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (id == "GLASS") {
                                    scope.launch { preferencesRepository.setHomeTileStyle(id) }
                                } else {
                                    guardCustomization {
                                        preferencesRepository.setHomeTileStyle(id)
                                    }
                                }
                            }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) theme.onAccent else theme.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Hero Carousel Styles
            Text(
                text = "Hero Carousel Style (Pro)",
                color = theme.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "EDITORIAL" to "Editorial",
                    "COMPACT_PILL" to "Compact",
                    "MINIMAL_SPLIT" to "Minimal Split"
                ).forEach { (id, label) ->
                    val selected = activeHeroStyle.equals(id, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) theme.accent else theme.elevated)
                            .border(
                                1.dp,
                                if (selected) theme.accent else theme.textSecondary.copy(alpha = 0.22f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (id == "EDITORIAL") {
                                    scope.launch { preferencesRepository.setHeroCarouselStyle(id) }
                                } else {
                                    guardCustomization {
                                        preferencesRepository.setHeroCarouselStyle(id)
                                    }
                                }
                            }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) theme.onAccent else theme.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Icon Set Styles
            Text(
                text = "Icon Set Style (Pro)",
                color = theme.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "FILLED" to "Default Filled",
                    "OUTLINED" to "Precision Badge",
                    "DUOTONE" to "Duotone Neon",
                    "MINIMAL" to "Monochrome Minimal"
                ).forEach { (id, label) ->
                    val selected = activeIconSetStyle.equals(id, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) theme.accent else theme.elevated)
                            .border(
                                1.dp,
                                if (selected) theme.accent else theme.textSecondary.copy(alpha = 0.22f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (id == "FILLED") {
                                    scope.launch { preferencesRepository.setIconSetStyle(id) }
                                } else {
                                    guardCustomization {
                                        preferencesRepository.setIconSetStyle(id)
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (selected) theme.onAccent else theme.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Jetpack Glance Home Screen Widgets Gallery & Pin Card (`Settings > Home & Tools > Widgets`):
 * - Short explainer with an "Add a widget" button (`AppWidgetManager.requestPinAppWidget` where supported)
 * - Lists all customizable Dyne Glance widgets (Quick Tile 1×1, Feature Grid 2×2/4×1/4×2,
 *   Torch Widget 1×1/2×1, Solunar 2×2, Weather 2×2, Scan 2×1, Sky 2×2, Compass 2×1)
 * - Each widget row offers "Configure" (opens `DyneWidgetConfigActivity`) and "Pin" actions.
 */
@Composable
fun HomeScreenWidgetsGalleryCard(
    userPreferences: UserPreferences,
    featureGate: FeatureGate?,
    isPro: Boolean,
    onRequestPaywall: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isUnlocked = isPro ||
        (featureGate?.hasAccess("home_widgets", prefs = userPreferences) == true)
    val remMs = featureGate?.getRemainingMsForKey("home_widgets", prefs = userPreferences) ?: 0L

    data class GlanceWidgetSpec(
        val kind: com.example.widget.DyneWidgetKind,
        val title: String,
        val sizeBadge: String,
        val subtitle: String,
        val icon: ImageVector,
        val receiverClass: Class<*>
    )

    val specs = remember {
        listOf(
            GlanceWidgetSpec(
                kind = com.example.widget.DyneWidgetKind.QUICK_TILE_1X1,
                title = "Quick Tile (1×1)",
                sizeBadge = "1×1",
                subtitle = "One feature of your choice from FeatureRegistry, big icon, glass look • Opens through FeatureGuard",
                icon = Icons.Default.AutoAwesome,
                receiverClass = com.example.widget.QuickTileGlanceWidgetReceiver::class.java
            ),
            GlanceWidgetSpec(
                kind = com.example.widget.DyneWidgetKind.FEATURE_GRID_2X2,
                title = "Feature Grid (2×2)",
                sizeBadge = "2×2",
                subtitle = "Choose 4 to 8 features (including Flashlight Strobe, Level Bubble, Scan QR) & custom glass style",
                icon = Icons.Default.GridView,
                receiverClass = com.example.widget.FeatureGrid2x2GlanceWidgetReceiver::class.java
            ),
            GlanceWidgetSpec(
                kind = com.example.widget.DyneWidgetKind.FEATURE_GRID_4X1,
                title = "Feature Grid (4×1)",
                sizeBadge = "4×1",
                subtitle = "Horizontal dock of 4 to 8 chosen features with custom style, accent color, tile size & corner radius",
                icon = Icons.Default.GridView,
                receiverClass = com.example.widget.FeatureGrid4x1GlanceWidgetReceiver::class.java
            ),
            GlanceWidgetSpec(
                kind = com.example.widget.DyneWidgetKind.FEATURE_GRID_4X2,
                title = "Feature Grid (4×2)",
                sizeBadge = "4×2",
                subtitle = "Full 2-row launcher grid of up to 8 features • Opens each feature directly via FeatureGuard",
                icon = Icons.Default.GridView,
                receiverClass = com.example.widget.FeatureGrid4x2GlanceWidgetReceiver::class.java
            ),
            GlanceWidgetSpec(
                kind = com.example.widget.DyneWidgetKind.TORCH_1X1,
                title = "Torch Widget (1×1)",
                sizeBadge = "1×1",
                subtitle = "Toggles flashlight WITHOUT opening the app • Saved intensity & real-time TorchCallback sync",
                icon = Icons.Default.FlashlightOn,
                receiverClass = com.example.widget.Torch1x1GlanceWidgetReceiver::class.java
            ),
            GlanceWidgetSpec(
                kind = com.example.widget.DyneWidgetKind.TORCH_2X1,
                title = "Torch Widget (2×1)",
                sizeBadge = "2×1",
                subtitle = "Wide LED Torch toggle with ON/OFF state, saved brightness level & missing-flash handling",
                icon = Icons.Default.FlashlightOn,
                receiverClass = FlashlightGlanceWidgetReceiver::class.java
            ),
            GlanceWidgetSpec(
                kind = com.example.widget.DyneWidgetKind.SOLUNAR_2X2,
                title = "Solunar Widget (2×2)",
                sizeBadge = "2×2",
                subtitle = "Daily Solunar score & best major window from cached data • Refreshed by WorkManager every ~30m",
                icon = Icons.Default.Waves,
                receiverClass = SolunarGlanceWidgetReceiver::class.java
            ),
            GlanceWidgetSpec(
                kind = com.example.widget.DyneWidgetKind.WEATHER_2X2,
                title = "Weather Widget (2×2)",
                sizeBadge = "2×2",
                subtitle = "Temperature, condition, wind & UV from cached data • Refreshed by WorkManager every ~30m",
                icon = Icons.Default.WbSunny,
                receiverClass = WeatherGlanceWidgetReceiver::class.java
            ),
            GlanceWidgetSpec(
                kind = com.example.widget.DyneWidgetKind.SCAN_2X1,
                title = "Scan Widget (2×1)",
                sizeBadge = "2×1",
                subtitle = "Direct QR / Barcode and Document buttons that open the scanners immediately",
                icon = Icons.Default.QrCodeScanner,
                receiverClass = QuickScanGlanceWidgetReceiver::class.java
            ),
            GlanceWidgetSpec(
                kind = com.example.widget.DyneWidgetKind.SKY_2X2,
                title = "Sky Widget (2×2)",
                sizeBadge = "2×2",
                subtitle = "Sun and Moon azimuth / altitude and current Moon phase with illumination %",
                icon = Icons.Default.Explore,
                receiverClass = com.example.widget.SkyGlanceWidgetReceiver::class.java
            ),
            GlanceWidgetSpec(
                kind = com.example.widget.DyneWidgetKind.COMPASS_2X1,
                title = "Compass Widget (2×1)",
                sizeBadge = "2×1",
                subtitle = "Last calibrated heading snapshot & quick launcher for 60fps Compass",
                icon = Icons.Default.Explore,
                receiverClass = CompassGlanceWidgetReceiver::class.java
            )
        )
    }

    fun requestPinOrUnlock(spec: GlanceWidgetSpec) {
        if (isUnlocked) {
            scope.launch {
                DyneWidgetHelper.refreshAllWidgets(context)
            }
            val pinned = DyneWidgetHelper.requestPinWidget(context, spec.receiverClass)
            if (!pinned) {
                Toast.makeText(
                    context,
                    "Long-press your Android home screen > Widgets > Dyne to add ${spec.title}.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else if (featureGate != null) {
            featureGate.guardNavigate(
                featureKey = "home_widgets",
                prefs = userPreferences,
                onAllowed = {
                    scope.launch { DyneWidgetHelper.refreshAllWidgets(context) }
                    DyneWidgetHelper.requestPinWidget(context, spec.receiverClass)
                }
            )
        } else {
            onRequestPaywall()
        }
    }

    fun openWidgetConfigurator(kind: com.example.widget.DyneWidgetKind) {
        val intent = android.content.Intent(context, com.example.widget.DyneWidgetConfigActivity::class.java).apply {
            putExtra(
                android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,
                android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
            )
            putExtra(com.example.widget.DyneWidgetConfigActivity.EXTRA_WIDGET_KIND_ID, kind.id)
        }
        context.startActivity(intent)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        tint = theme.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "HOME-SCREEN WIDGETS (JETPACK GLANCE)",
                        color = theme.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }
                if (!isPro) {
                    if (remMs > 0L && featureGate != null) {
                        ProCountdownRing(
                            progress = featureGate.getCountdownProgressForKey("home_widgets", prefs = userPreferences),
                            label = featureGate.formatRemainingRingBadge(remMs)
                        )
                    } else {
                        ProBadge()
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Place customizable Dyne glass widgets on your Android home screen. Choose features, visual style (Glass, Solid, Outline, Gradient), accent color, tile size, corner radius, and labels. Widgets follow your app theme and light/dark mode automatically.",
                color = theme.textSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Primary "Add a widget" action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        requestPinOrUnlock(specs[1]) // Default to Feature Grid (2×2)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isUnlocked) theme.accent else Color(0xFFF59E0B),
                        contentColor = if (isUnlocked) theme.onAccent else Color(0xFF0A0A0C)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isUnlocked) "Add a widget" else "Unlock & Add a widget",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.elevated)
                        .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable { openWidgetConfigurator(com.example.widget.DyneWidgetKind.FEATURE_GRID_2X2) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Configure",
                        color = theme.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            specs.forEach { spec ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.elevated.copy(alpha = 0.65f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(theme.accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = spec.icon,
                                contentDescription = null,
                                tint = theme.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = spec.title,
                                    color = theme.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(theme.accent.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = spec.sizeBadge,
                                        color = theme.accent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Text(
                                text = spec.subtitle,
                                color = theme.textSecondary,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Button(
                            onClick = { requestPinOrUnlock(spec) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isUnlocked) theme.accent else Color(0xFFF59E0B),
                                contentColor = if (isUnlocked) theme.onAccent else Color(0xFF0A0A0C)
                            )
                        ) {
                            Text(
                                text = if (isUnlocked) "Add" else "Unlock",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.surface.copy(alpha = 0.7f))
                                .border(1.dp, theme.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .clickable { openWidgetConfigurator(spec.kind) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Configure",
                                color = theme.textPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
