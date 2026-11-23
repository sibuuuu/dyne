package com.example.ui.outdoors

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.InlineBannerAdSlot
import com.example.billing.LocalFeatureGate
import com.example.billing.PremiumFeature
import com.example.data.CitySearchResult
import com.example.data.PressureTrend
import com.example.data.SensorState
import com.example.data.UnitSystem
import com.example.data.WeatherConditionKind
import com.example.data.WeatherSnapshot
import com.example.domain.DailyAstroReport
import com.example.ui.components.DyneSegmentedControl
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassPermissionCard
import com.example.ui.components.GlassSheet
import com.example.ui.components.HazeState
import com.example.ui.components.LocalBottomBarInset
import com.example.ui.components.ProBadge
import com.example.ui.components.SharedTopBar
import com.example.ui.components.horizontalChipRowEdgeFade
import com.example.ui.components.pressScaleClickable
import com.example.ui.components.proBorder
import com.example.ui.solunar.SolunarUiState
import com.example.ui.solunar.SolunarViewModel
import com.example.ui.sunmoon.SunMoonUiState
import com.example.ui.sunmoon.SunMoonViewModel
import com.example.ui.theme.BentoTextBlack
import com.example.ui.theme.LocalDyneTheme
import com.example.ui.theme.LocalDyneType
import com.example.ui.theme.ProGold
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import com.example.ads.LocalAdManager
import com.example.billing.UnlockSheetRequest
import com.example.ui.components.GlassSurface
import com.example.ui.home.AccessBanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private fun formatAstroTime(time: LocalTime?, use24h: Boolean): String {
    if (time == null) return "--:--"
    val pattern = if (use24h) "HH:mm" else "h:mm a"
    return time.format(DateTimeFormatter.ofPattern(pattern, Locale.US))
}

private fun formatTemp(tempC: Double, useCelsius: Boolean): String {
    val value = if (useCelsius) tempC else (tempC * 9.0 / 5.0 + 32.0)
    return "${value.roundToInt()}°"
}

/**
 * Full-featured Outdoors screen with Segmented Control:
 *   0 -> Solunar (offline astronomy math: moon phase, moonrise/moonset, sunrise/sunset, moon overhead & underfoot,
 *        2 major (~2h) & 2 minor (~1h) periods on a 24h glass timeline, 0–100 Activity Score ring,
 *        Hunting/Fishing toggle, "Weather boost" line from pressure trend & wind, and 7-day forecast list)
 *   1 -> Sun & Moon (live sun & moon azimuth/altitude on a compass-style canvas that rotates with phone heading,
 *        golden hour & blue hour times, today's sun path arc, moon phase drawn on Canvas)
 *   2 -> Weather (WeatherRepository with Open-Meteo default provider, current/hourly/7-day forecast by GPS or searched city,
 *        offline cache with "updated x min ago", big temperature, Canvas-drawn condition icons, feels like,
 *        hourly strip, daily list, wind, humidity, UV, visibility, sunrise/sunset, pressure-trend arrow,
 *        °C/°F toggle, and 0–100 Outdoor Score)
 */
@Composable
fun OutdoorsScreen(
    selectedSegment: Int,
    onSegmentChange: (Int) -> Unit,
    solunarViewModel: SolunarViewModel,
    sunMoonViewModel: SunMoonViewModel,
    sensorState: SensorState,
    unitSystem: UnitSystem,
    isPro: Boolean,
    hazeState: HazeState?,
    nestedScrollConnection: NestedScrollConnection,
    onOpenPaywall: () -> Unit,
    modifier: Modifier = Modifier,
    scrollToTopTrigger: Int = 0
) {
    val solunarState by solunarViewModel.uiState.collectAsStateWithLifecycle()
    val showManualSheet by solunarViewModel.showManualLocationSheet.collectAsStateWithLifecycle()
    val cityResults by solunarViewModel.citySearchResults.collectAsStateWithLifecycle()
    val sunMoonState by sunMoonViewModel.uiState.collectAsStateWithLifecycle()
    val mainScrollState = rememberScrollState()

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            mainScrollState.animateScrollTo(0)
        }
    }

    var locationPermissionDenied by rememberSaveable { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationPermissionDenied = !granted
        if (granted) {
            solunarViewModel.refreshGpsLocation()
        }
    }

    val segments = remember { listOf("Solunar", "Sun & Moon", "Weather") }

    val featureGate = LocalFeatureGate.current
    val appContext = androidx.compose.ui.platform.LocalContext.current
    val prefsRepo = remember(appContext) { com.example.data.PreferencesRepository(appContext) }
    val userPrefs by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(initialValue = com.example.data.UserPreferences())

    var accessTickNowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(userPrefs.accessUnlockRecordsSerialized, userPrefs.featureUnlocksCsv, isPro) {
        if (!isPro) {
            while (isActive) {
                accessTickNowMs = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    val isSunMoonUnlocked = isPro || (featureGate?.hasAccess("outdoor_sky_dome", prefs = userPrefs, nowMs = accessTickNowMs) == true)
    val isSunPathGoldenHourUnlocked = isPro || (featureGate?.hasAccess("outdoor_sun_moon", prefs = userPrefs, nowMs = accessTickNowMs) == true)
    val isSolunar7DayUnlocked = isPro || (featureGate?.hasAccess("outdoor_solunar_7day", prefs = userPrefs, nowMs = accessTickNowMs) == true)
    val isWeatherHourly7DayUnlocked = isPro || (featureGate?.hasAccess("outdoor_weather_7day", prefs = userPrefs, nowMs = accessTickNowMs) == true)

    val activeBannerKey = remember(selectedSegment, userPrefs, accessTickNowMs, isPro) {
        if (isPro || featureGate == null) {
            null
        } else {
            val candidateKeys = when (selectedSegment) {
                0 -> listOf("outdoor_solunar_7day", "outdoor_planner_7day", "outdoor_alerts")
                1 -> listOf("outdoor_sky_dome", "outdoor_sun_moon", "outdoor_sun_moon_calendar")
                else -> listOf("outdoor_weather_7day", "outdoor_air_quality_uv", "outdoor_alerts")
            }
            candidateKeys.firstOrNull { key ->
                featureGate.getRemainingMsForKey(key, prefs = userPrefs, nowMs = accessTickNowMs) > 0L
            }
        }
    }
    val activeBannerRemainingMs = remember(activeBannerKey, userPrefs, accessTickNowMs, isPro) {
        if (isPro || activeBannerKey == null || featureGate == null) 0L
        else featureGate.getRemainingMsForKey(activeBannerKey, prefs = userPrefs, nowMs = accessTickNowMs)
    }

    val skyDomeRemainingMs = remember(userPrefs, accessTickNowMs, isPro) {
        if (isPro || featureGate == null) 0L
        else featureGate.getRemainingMsForKey("outdoor_sky_dome", prefs = userPrefs, nowMs = accessTickNowMs)
    }
    var hadSkyDomeSession by remember { mutableStateOf(false) }
    var showSkyDomeExpiredOverlay by remember { mutableStateOf(false) }
    var expiredNoticeMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedSegment, skyDomeRemainingMs, isPro) {
        if (isPro || selectedSegment != 1) {
            hadSkyDomeSession = false
            showSkyDomeExpiredOverlay = false
        } else if (skyDomeRemainingMs > 0L) {
            hadSkyDomeSession = true
            showSkyDomeExpiredOverlay = false
            expiredNoticeMessage = null
        } else if (hadSkyDomeSession) {
            hadSkyDomeSession = false
            showSkyDomeExpiredOverlay = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .verticalScroll(mainScrollState)
                .then(
                    if (showSkyDomeExpiredOverlay && !isPro && selectedSegment == 1) {
                        Modifier.blur(16.dp)
                    } else {
                        Modifier
                    }
                )
        ) {
            SharedTopBar(
                title = "Outdoors",
                subtitle = "SOLUNAR • SKY COMPASS • OPEN-METEO",
                hazeState = hazeState,
                rightActionIcon = Icons.Default.WorkspacePremium,
                rightActionDescription = "Dyne Pro",
                onRightActionClick = onOpenPaywall
            )

            if (!isPro && activeBannerKey != null && activeBannerRemainingMs > 0L) {
                AccessBanner(
                    remainingMs = activeBannerRemainingMs,
                    totalDurationMs = com.example.data.UserPreferences.ONE_HOUR_MS,
                    buttonText = "Extend",
                    hazeState = hazeState,
                    onUnlockClick = {
                        featureGate?.guardNavigate(activeBannerKey, prefs = userPrefs) {}
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                DyneSegmentedControl(
                    segments = listOf(
                        "Solunar",
                        if (isSunMoonUnlocked) "Sun & Moon" else "Sun & Moon • PRO",
                        "Weather"
                    ),
                    selectedIndex = selectedSegment,
                    proIndices = if (isSunMoonUnlocked) emptySet() else setOf(1),
                    onSegmentSelected = { idx ->
                        if (idx == 1 && !isSunMoonUnlocked) {
                            if (featureGate != null) {
                                featureGate.guardNavigate("outdoor_sky_dome", prefs = userPrefs) {
                                    onSegmentChange(1)
                                }
                            } else {
                                onOpenPaywall()
                            }
                        } else {
                            onSegmentChange(idx)
                        }
                    }
                )

                // Shared GPS / City & Coordinate Bar
                LocationCoordinateCard(
                    label = solunarState.locationLabel,
                    lat = solunarState.latitude,
                    lng = solunarState.longitude,
                    isFromGps = solunarState.isFromGps,
                    onRequestGps = {
                        if (solunarViewModel.hasLocationPermission()) {
                            locationPermissionDenied = false
                            solunarViewModel.refreshGpsLocation()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    onOpenManualEditor = {
                        solunarViewModel.setShowManualSheet(true)
                    }
                )

                if (locationPermissionDenied) {
                    GlassPermissionCard(
                        icon = Icons.Default.LocationOn,
                        title = "Location Permission for Local Forecast",
                        explanation = "Dyne uses your GPS coordinates to calculate exact sun, moon, solunar transits, and local weather. You can also search any city or enter lat/lng manually.",
                        buttonText = "Allow GPS Access",
                        hazeState = hazeState,
                        onRequestPermission = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        secondaryActionText = "Search City or Enter Lat/Lng",
                        onSecondaryAction = { solunarViewModel.setShowManualSheet(true) }
                    )
                }

                when (selectedSegment) {
                    0 -> {
                        SolunarSegmentContent(
                            state = solunarState,
                            isPro = isSolunar7DayUnlocked,
                            onOpenPaywall = {
                                if (featureGate != null) {
                                    featureGate.guardNavigate("outdoor_solunar_7day", prefs = userPrefs) {}
                                } else {
                                    onOpenPaywall()
                                }
                            },
                            onToggleFishingMode = solunarViewModel::setFishingMode,
                            onSelectDay = { dayIdx ->
                                if (dayIdx > 0 && !isSolunar7DayUnlocked) {
                                    if (featureGate != null) {
                                        featureGate.guardNavigate("outdoor_solunar_7day", prefs = userPrefs) {
                                            solunarViewModel.selectDayIndex(dayIdx)
                                        }
                                    } else {
                                        onOpenPaywall()
                                    }
                                } else {
                                    solunarViewModel.selectDayIndex(dayIdx)
                                }
                            }
                        )

                        solunarState.activeReport?.let { activeRep ->
                            TideFreeBestDayAndShareCard(
                                reports = solunarState.forecastDays,
                                selectedReport = activeRep,
                                weatherSnapshot = solunarState.weatherSnapshot,
                                locationLabel = solunarState.locationLabel,
                                use24Hour = solunarState.use24HourTime,
                                isTempFahrenheit = !solunarState.useCelsius,
                                featureGate = featureGate,
                                isPro = isPro,
                                userPreferences = userPrefs,
                                onRequestPaywall = onOpenPaywall
                            )
                        }

                        if (solunarState.forecastDays.isNotEmpty()) {
                            OutdoorPlanner7DayCard(
                                reports = solunarState.forecastDays,
                                weatherSnapshot = solunarState.weatherSnapshot,
                                use24Hour = solunarState.use24HourTime,
                                featureGate = featureGate,
                                isPro = isPro,
                                userPreferences = userPrefs,
                                onRequestPaywall = onOpenPaywall
                            )
                        }

                        OutdoorAlertsConfigCard(
                            userPreferences = userPrefs,
                            preferencesRepository = prefsRepo,
                            featureGate = featureGate,
                            isPro = isPro,
                            onRequestPaywall = onOpenPaywall
                        )
                    }

                    1 -> {
                        SunMoonSegmentContent(
                            state = sunMoonState,
                            isPro = isSunMoonUnlocked,
                            isSunPathGoldenHourUnlocked = isSunPathGoldenHourUnlocked,
                            onOpenPaywall = {
                                if (featureGate != null) {
                                    featureGate.guardNavigate("outdoor_sky_dome", prefs = userPrefs) {}
                                } else {
                                    onOpenPaywall()
                                }
                            },
                            onOpenSunPathUnlock = {
                                if (featureGate != null) {
                                    featureGate.guardNavigate("outdoor_sun_moon", prefs = userPrefs) {}
                                } else {
                                    onOpenPaywall()
                                }
                            }
                        )

                        sunMoonState.dailyReport.let { todayRep ->
                            InteractiveSkyDomeScrubberPanel(
                                selectedDate = todayRep.date,
                                lat = sunMoonState.latitude,
                                lng = sunMoonState.longitude,
                                report = todayRep,
                                use24Hour = sunMoonState.use24HourTime,
                                isUnlocked = isSunPathGoldenHourUnlocked,
                                onUnlockClick = {
                                    if (featureGate != null) {
                                        featureGate.guardNavigate("outdoor_sun_moon", prefs = userPrefs) {}
                                    } else {
                                        onOpenPaywall()
                                    }
                                }
                            )
                        }

                        MonthlySunMoonCalendarCard(
                            lat = sunMoonState.latitude,
                            lng = sunMoonState.longitude,
                            use24Hour = sunMoonState.use24HourTime,
                            featureGate = featureGate,
                            isPro = isPro,
                            userPreferences = userPrefs,
                            onRequestPaywall = onOpenPaywall
                        )
                    }

                    else -> {
                        WeatherSegmentContent(
                            state = solunarState,
                            sensorState = sensorState,
                            isPro = isWeatherHourly7DayUnlocked,
                            onOpenPaywall = {
                                if (featureGate != null) {
                                    featureGate.guardNavigate("outdoor_weather_7day", prefs = userPrefs) {}
                                } else {
                                    onOpenPaywall()
                                }
                            },
                            onToggleCelsiusFahrenheit = solunarViewModel::toggleCelsiusFahrenheit,
                            onRefreshWeather = solunarViewModel::refreshWeatherNow,
                            onSearchCityClick = { solunarViewModel.setShowManualSheet(true) }
                        )

                        AirQualityAndUvCard(
                            weatherSnapshot = solunarState.weatherSnapshot,
                            featureGate = featureGate,
                            isPro = isPro,
                            userPreferences = userPrefs,
                            onRequestPaywall = onOpenPaywall
                        )

                        OutdoorAlertsConfigCard(
                            userPreferences = userPrefs,
                            preferencesRepository = prefsRepo,
                            featureGate = featureGate,
                            isPro = isPro,
                            onRequestPaywall = onOpenPaywall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(LocalBottomBarInset.current))
            }
        }

        if (showSkyDomeExpiredOverlay && !isPro && selectedSegment == 1) {
            val theme = LocalDyneTheme.current
            val type = LocalDyneType.current
            val adManager = LocalAdManager.current
            val activity = appContext as? android.app.Activity
            val unlockReq = remember {
                UnlockSheetRequest(
                    feature = PremiumFeature.SUN_MOON_DOME,
                    catalogFeatureId = "outdoor_sky_dome",
                    featureKey = "outdoor_sky_dome",
                    customTitle = "Sun & Moon Sky Dome",
                    customDescription = "Live compass-aligned sky dome with hourly Sun & Moon trajectories.",
                    onUnlocked = {
                        showSkyDomeExpiredOverlay = false
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
                            text = "Sun & Moon Sky Dome",
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
                                        rewardDescription = "Unlock Sun & Moon Sky Dome (1h)",
                                        onAdUnavailable = { msg ->
                                            expiredNoticeMessage = msg
                                        },
                                        onRewardEarned = {
                                            featureGate.grant1HourUnlockFromRewardedAd(unlockReq) {
                                                showSkyDomeExpiredOverlay = false
                                                expiredNoticeMessage = null
                                            }
                                        }
                                    )
                                } else {
                                    featureGate?.guardNavigate("outdoor_sky_dome", prefs = userPrefs) {}
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
                                    featureGate.unlockWithCoins(unlockReq) { success, msg ->
                                        if (success) {
                                            showSkyDomeExpiredOverlay = false
                                            expiredNoticeMessage = null
                                        } else {
                                            expiredNoticeMessage = msg
                                        }
                                    }
                                } else {
                                    onOpenPaywall()
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
                                showSkyDomeExpiredOverlay = false
                                onOpenPaywall()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "expired_go_premium_button"
                        )

                        TextButton(
                            onClick = {
                                showSkyDomeExpiredOverlay = false
                                onSegmentChange(0)
                            },
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

    if (showManualSheet) {
        ManualLocationGlassSheet(
            initialLat = solunarState.latitude,
            initialLng = solunarState.longitude,
            initialLabel = solunarState.locationLabel,
            cityResults = cityResults,
            hazeState = hazeState,
            onSearchCityQuery = solunarViewModel::searchCities,
            onSave = { lat, lng, label ->
                solunarViewModel.saveManualCoordinates(lat, lng, label)
            },
            onDismiss = { solunarViewModel.setShowManualSheet(false) }
        )
    }
}

@Composable
private fun LocationCoordinateCard(
    label: String,
    lat: Double,
    lng: Double,
    isFromGps: Boolean,
    onRequestGps: () -> Unit,
    onOpenManualEditor: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(theme.surface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isFromGps) theme.accent else theme.bentoSky)
                )
                Text(
                    text = label,
                    style = type.label.copy(fontWeight = FontWeight.Bold),
                    color = theme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = String.format(Locale.US, "%.4f°, %.4f°", lat, lng),
                style = type.caption,
                color = theme.textSecondary
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(theme.elevated)
                    .pressScaleClickable(
                        targetScale = 0.95f,
                        testTag = "solunar_gps_refresh_button",
                        onClick = onRequestGps
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "Use Live GPS",
                    tint = theme.accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(theme.elevated)
                    .pressScaleClickable(
                        targetScale = 0.95f,
                        testTag = "solunar_manual_location_button",
                        onClick = onOpenManualEditor
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.EditLocationAlt,
                    contentDescription = "Search City or Edit Coordinates",
                    tint = theme.bentoSky,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ============================================================================
// 1) SOLUNAR TAB
// ============================================================================

@Composable
private fun SolunarSegmentContent(
    state: SolunarUiState,
    isPro: Boolean = false,
    onOpenPaywall: () -> Unit = {},
    onToggleFishingMode: (Boolean) -> Unit,
    onSelectDay: (Int) -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val report = state.activeReport ?: return
    val dayFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US) }
    val chipFmt = remember { DateTimeFormatter.ofPattern("EEE d", Locale.US) }
    val activeScore = state.activeAdjustedScore
    val ratingText = when {
        activeScore >= 85 -> "Peak Solunar Activity"
        activeScore >= 70 -> "High Field Activity"
        activeScore >= 50 -> "Moderate Activity"
        else -> "Low Solunar Activity"
    }

    // Selectable Day Strip (LazyRow with 20.dp side padding and edge fade)
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalChipRowEdgeFade()
    ) {
        itemsIndexed(state.forecastDays) { idx, dayReport ->
            val isSelected = idx == state.selectedDayIndex
            val isLockedDay = idx > 0 && !isPro
            val boost = if (idx == 0) state.weatherBoostPoints else (state.weatherBoostPoints / 2)
            val dayScore = (dayReport.scoreForMode(state.isFishingMode) + boost).coerceIn(15, 99)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isSelected) theme.accent else theme.surface)
                    .let { base ->
                        when {
                            isSelected -> base.border(1.dp, theme.accent, RoundedCornerShape(18.dp))
                            isLockedDay -> base.proBorder(RoundedCornerShape(18.dp))
                            else -> base.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                        }
                    }
                    .pressScaleClickable(
                        targetScale = 0.96f,
                        testTag = "solunar_day_chip_$idx",
                        onClick = { onSelectDay(idx) }
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = if (idx == 0) "TODAY" else dayReport.date.format(chipFmt).uppercase(Locale.US),
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = when {
                        isSelected -> BentoTextBlack
                        isLockedDay -> ProGold
                        else -> theme.textSecondary
                    },
                    maxLines = 1
                )
                Text(
                    text = "$dayScore",
                    style = type.label.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) BentoTextBlack else theme.textPrimary,
                    maxLines = 1
                )
            }
        }
    }

    // Hunting / Fishing Mode Toggle + 0-100 Activity Score Ring + Weather Boost Line
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(theme.surface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(true to "Fishing Mode", false to "Hunting Mode").forEach { (fishing, label) ->
                val active = state.isFishingMode == fishing
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) theme.accent else theme.elevated)
                        .pressScaleClickable(
                            targetScale = 0.96f,
                            testTag = if (fishing) "solunar_mode_fishing" else "solunar_mode_hunting",
                            onClick = { onToggleFishingMode(fishing) }
                        )
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = label,
                        style = type.label.copy(fontWeight = FontWeight.Bold),
                        color = if (active) BentoTextBlack else theme.textSecondary
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(134.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 12.dp.toPx()
                    drawArc(
                        color = Color.White.copy(alpha = 0.08f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = theme.accent,
                        startAngle = 135f,
                        sweepAngle = 270f * (activeScore / 100f),
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$activeScore",
                        style = type.gaugeLarge,
                        color = theme.textPrimary
                    )
                    Text(
                        text = "ACTIVITY / 100",
                        style = type.caption,
                        color = theme.textSecondary
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = ratingText.uppercase(Locale.US),
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.accent
                )
                Text(
                    text = "${report.moonPhaseName} (${report.moonIlluminationPercent}%)",
                    style = type.section,
                    color = theme.textPrimary
                )
                Text(
                    text = "Base ephemeris: ${state.activeBaseScore}/100",
                    style = type.caption,
                    color = theme.textSecondary
                )
                Text(
                    text = if (state.isFishingMode) {
                        "Feeding intensity peaks during Major lunar transits overhead & underfoot."
                    } else {
                        "Wildlife movement peaks during dawn/dusk & Minor moonrise/moonset windows."
                    },
                    style = type.caption,
                    color = theme.textSecondary
                )
            }
        }

        // Weather Boost Line (adjusts score slightly using pressure trend and wind)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(theme.elevated)
                .border(1.dp, theme.bentoSky.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WEATHER BOOST",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.bentoSky
                )
                Text(
                    text = state.weatherBoostLine,
                    style = type.caption,
                    color = theme.textPrimary
                )
            }
        }
    }

    // 24-Hour Glass Timeline with 2 Major (~2h) and 2 Minor (~1h) Periods
    Solunar24hTimelineCard(report = report, use24h = state.use24HourTime)

    // 2 Major (~2h) and 2 Minor (~1h) Period Cards
    val allPeriods = remember(report) { report.majorPeriods + report.minorPeriods }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("2 MAJOR (~2H) & 2 MINOR (~1H) PERIODS", style = type.caption, color = theme.textSecondary)
        allPeriods.forEach { period ->
            val isMajor = period.isMajor
            val accentColor = if (isMajor) theme.bentoLime else theme.bentoSky
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(theme.surface)
                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isMajor) "MAJOR PERIOD (~2H)" else "MINOR PERIOD (~1H)",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                    Text(
                        text = period.title,
                        style = type.body.copy(fontWeight = FontWeight.SemiBold),
                        color = theme.textPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${formatAstroTime(period.startTime, state.use24HourTime)} – ${
                            formatAstroTime(period.endTime, state.use24HourTime)
                        }",
                        style = type.label.copy(fontWeight = FontWeight.Bold),
                        color = theme.textPrimary
                    )
                    Text(
                        text = "Peak ${formatAstroTime(period.peakTime, state.use24HourTime)}",
                        style = type.caption,
                        color = theme.textSecondary
                    )
                }
            }
        }
    }

    // Sunrise/Sunset + Moonrise/Moonset + Moon Overhead & Moon Underfoot Grid
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        EphemerisMiniCard(
            title = "SUNRISE & SUNSET",
            line1 = "Sunrise: ${formatAstroTime(report.sunrise, state.use24HourTime)}",
            line2 = "Sunset: ${formatAstroTime(report.sunset, state.use24HourTime)}",
            line3 = "Solar Noon: ${formatAstroTime(report.solarNoon, state.use24HourTime)}",
            accent = theme.bentoCoral,
            modifier = Modifier.weight(1f)
        )
        EphemerisMiniCard(
            title = "MOONRISE & TRANSITS",
            line1 = "Rise: ${formatAstroTime(report.moonrise, state.use24HourTime)} • Set: ${formatAstroTime(report.moonset, state.use24HourTime)}",
            line2 = "Overhead: ${formatAstroTime(report.moonOverhead, state.use24HourTime)}",
            line3 = "Underfoot: ${formatAstroTime(report.moonUnderfoot, state.use24HourTime)}",
            accent = theme.bentoLavender,
            modifier = Modifier.weight(1f)
        )
    }

    // 7-Day Solunar Forecast List (Today free; days 1..6 gated for Pro)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "7-DAY SOLUNAR FORECAST LIST",
                    style = type.section,
                    color = theme.accent
                )
                if (!isPro) {
                    ProBadge()
                }
            }
            if (!isPro) {
                Text(
                    text = "Unlock 7-Day Forecast",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = ProGold,
                    modifier = Modifier.pressScaleClickable(onClick = onOpenPaywall)
                )
            }
        }
        state.forecastDays.forEachIndexed { idx, day ->
            val boost = if (idx == 0) state.weatherBoostPoints else (state.weatherBoostPoints / 2)
            val dayScore = (day.scoreForMode(state.isFishingMode) + boost).coerceIn(15, 99)
            val major1 = day.majorPeriods.firstOrNull()
            val major2 = day.majorPeriods.getOrNull(1)
            val isSelected = idx == state.selectedDayIndex
            val isLockedDay = idx > 0 && !isPro

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(theme.surface)
                    .let { base ->
                        when {
                            isSelected -> base.border(1.dp, theme.accent, RoundedCornerShape(22.dp))
                            isLockedDay -> base.proBorder(RoundedCornerShape(22.dp))
                            else -> base.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                        }
                    }
                    .pressScaleClickable(
                        testTag = "solunar_forecast_row_$idx",
                        onClick = {
                            if (isLockedDay) onOpenPaywall() else onSelectDay(idx)
                        }
                    )
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    MoonPhaseCanvas(
                        phaseFraction = day.moonPhaseFraction,
                        accentColor = theme.bentoLavender,
                        modifier = Modifier.size(34.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (idx == 0) "Today • ${day.date.format(dayFmt)}" else day.date.format(dayFmt),
                            style = type.body.copy(fontWeight = FontWeight.Bold),
                            color = theme.textPrimary
                        )
                        Text(
                            text = "${day.moonPhaseName} (${day.moonIlluminationPercent}%)",
                            style = type.caption,
                            color = theme.bentoLavender
                        )
                        if (major1 != null) {
                            val m2Text = if (major2 != null) {
                                " & ${formatAstroTime(major2.startTime, state.use24HourTime)}"
                            } else {
                                ""
                            }
                            Text(
                                text = "Major: ${formatAstroTime(major1.startTime, state.use24HourTime)}$m2Text",
                                style = type.caption,
                                color = theme.textSecondary
                            )
                        }
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.accent.copy(alpha = 0.18f))
                        .border(1.dp, theme.accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$dayScore / 100",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.accent
                    )
                }
            }
        }
    }
}

@Composable
private fun Solunar24hTimelineCard(
    report: DailyAstroReport,
    use24h: Boolean
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val nowMinutes = remember {
        val now = LocalTime.now()
        now.hour * 60 + now.minute
    }
    val allPeriods = remember(report) { report.majorPeriods + report.minorPeriods }

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
            Text(
                text = "24-HOUR GLASS TIMELINE",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.textPrimary
            )
            Text(
                text = if (use24h) "00:00 – 12:00 – 24:00" else "12A – 12P – 12A",
                style = type.caption,
                color = theme.textSecondary
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        ) {
            val trackY = size.height * 0.5f
            val trackH = 20.dp.toPx()
            drawRoundRect(
                color = Color.White.copy(alpha = 0.07f),
                topLeft = Offset(0f, trackY - trackH / 2f),
                size = Size(size.width, trackH),
                cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)
            )

            // Hour tick marks every 3 hours
            for (h in 0..24 step 3) {
                val x = (h / 24f) * size.width
                drawLine(
                    color = Color.White.copy(alpha = 0.14f),
                    start = Offset(x, trackY - trackH / 2f - 6.dp.toPx()),
                    end = Offset(x, trackY + trackH / 2f + 6.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw Major & Minor windows across 1440 minutes
            allPeriods.forEach { period ->
                val startMin = period.startMinutesOfDay.coerceIn(0, 1439)
                val endMin = period.endMinutesOfDay.coerceIn(startMin + 30, 1440)
                val startX = (startMin / 1440f) * size.width
                val widthPx = (((endMin - startMin).coerceAtLeast(45)) / 1440f) * size.width
                val color = if (period.isMajor) theme.bentoLime else theme.bentoSky
                drawRoundRect(
                    color = color,
                    topLeft = Offset(startX, trackY - trackH / 2f),
                    size = Size(widthPx.coerceAtMost(size.width - startX), trackH),
                    cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)
                )
            }

            // Current time vertical marker
            val nowX = (nowMinutes / 1440f) * size.width
            drawLine(
                color = theme.bentoCoral,
                start = Offset(nowX, 2f),
                end = Offset(nowX, size.height - 2f),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = theme.bentoCoral,
                radius = 4.dp.toPx(),
                center = Offset(nowX, 6.dp.toPx())
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(theme.bentoLime))
                Text("Major (~2h)", style = type.caption, color = theme.textSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(theme.bentoSky))
                Text("Minor (~1h)", style = type.caption, color = theme.textSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(theme.bentoCoral))
                Text("Now", style = type.caption, color = theme.textSecondary)
            }
        }
    }
}

@Composable
private fun EphemerisMiniCard(
    title: String,
    line1: String,
    line2: String,
    line3: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(theme.surface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Text(title, style = type.caption.copy(fontWeight = FontWeight.Bold), color = accent)
        Text(line1, style = type.caption, color = theme.textPrimary)
        Text(line2, style = type.caption, color = theme.textPrimary)
        Text(line3, style = type.caption, color = theme.textSecondary)
    }
}

// ============================================================================
// 2) SUN & MOON TAB
// ============================================================================

@Composable
private fun SunMoonSegmentContent(
    state: SunMoonUiState,
    isPro: Boolean = false,
    isSunPathGoldenHourUnlocked: Boolean = false,
    onOpenPaywall: () -> Unit = {},
    onOpenSunPathUnlock: () -> Unit = onOpenPaywall
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val report = state.dailyReport

    if (!isPro) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(theme.elevated)
                .proBorder(RoundedCornerShape(22.dp))
                .pressScaleClickable(
                    testTag = "sun_moon_pro_gate",
                    onClick = onOpenPaywall
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SUN & MOON SKY COMPASS",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = ProGold
                    )
                    ProBadge()
                }
                Text(
                    text = "Tap to unlock live Sun & Moon Sky Dome, Golden Hour & Blue Hour times with Dyne Pro.",
                    style = type.caption,
                    color = theme.textPrimary
                )
            }
        }
    }

    // Live Rotating Sky Dome Compass Canvas
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(theme.surface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "LIVE SKY DOME COMPASS",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.accent
                )
                Text(
                    text = "Rotates live with phone heading",
                    style = type.caption,
                    color = theme.textSecondary
                )
            }
            Text(
                text = "${state.phoneHeadingDeg.roundToInt()}°",
                style = type.gaugeMedium,
                color = theme.textPrimary
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(264.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f - 14.dp.toPx()

                // Altitude rings: Horizon (0°), 30°, 60° + Zenith crosshair
                drawCircle(
                    color = Color.White.copy(alpha = 0.16f),
                    radius = r,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = r * 0.66f,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = r * 0.33f,
                    style = Stroke(width = 1.dp.toPx())
                )

                rotate(-state.phoneHeadingDeg) {
                    // Cardinal & 15-degree tick marks
                    for (deg in 0 until 360 step 15) {
                        val rad = Math.toRadians(deg.toDouble() - 90.0)
                        val isCardinal = deg % 90 == 0
                        val inner = if (isCardinal) r - 14.dp.toPx() else r - 7.dp.toPx()
                        drawLine(
                            color = if (deg == 0) theme.bentoCoral else Color.White.copy(alpha = if (isCardinal) 0.65f else 0.22f),
                            start = Offset(
                                center.x + inner * cos(rad).toFloat(),
                                center.y + inner * sin(rad).toFloat()
                            ),
                            end = Offset(
                                center.x + r * cos(rad).toFloat(),
                                center.y + r * sin(rad).toFloat()
                            ),
                            strokeWidth = if (isCardinal) 3.dp.toPx() else 1.dp.toPx()
                        )
                    }

                    // Plot today's hourly sun trajectory curve on the dome
                    val trajectoryPath = Path()
                    var started = false
                    report.sunPathHourly.forEach { pos ->
                        if (pos.altitudeDeg >= -5.0) {
                            val altClamped = pos.altitudeDeg.coerceIn(0.0, 90.0)
                            val dist = r * ((90.0 - altClamped) / 90.0).toFloat()
                            val rad = Math.toRadians(pos.azimuthDeg - 90.0)
                            val pt = Offset(
                                center.x + dist * cos(rad).toFloat(),
                                center.y + dist * sin(rad).toFloat()
                            )
                            if (!started) {
                                trajectoryPath.moveTo(pt.x, pt.y)
                                started = true
                            } else {
                                trajectoryPath.lineTo(pt.x, pt.y)
                            }
                        }
                    }
                    if (started) {
                        drawPath(
                            path = trajectoryPath,
                            color = theme.bentoCoral.copy(alpha = 0.35f),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Plot Live Sun position on Sky Dome
                    val sunAltClamped = state.sunPosition.altitudeDeg.coerceIn(-15.0, 90.0)
                    val sunDist = r * ((90.0 - sunAltClamped.coerceAtLeast(0.0)) / 90.0).toFloat()
                    val sunRad = Math.toRadians(state.sunPosition.azimuthDeg - 90.0)
                    val sunOffset = Offset(
                        center.x + sunDist * cos(sunRad).toFloat(),
                        center.y + sunDist * sin(sunRad).toFloat()
                    )
                    drawCircle(
                        color = theme.bentoCoral.copy(alpha = 0.28f),
                        radius = 18.dp.toPx(),
                        center = sunOffset
                    )
                    drawCircle(
                        color = theme.bentoCoral,
                        radius = 9.dp.toPx(),
                        center = sunOffset
                    )

                    // Plot Live Moon position on Sky Dome
                    val moonAltClamped = state.moonPosition.altitudeDeg.coerceIn(-15.0, 90.0)
                    val moonDist = r * ((90.0 - moonAltClamped.coerceAtLeast(0.0)) / 90.0).toFloat()
                    val moonRad = Math.toRadians(state.moonPosition.azimuthDeg - 90.0)
                    val moonOffset = Offset(
                        center.x + moonDist * cos(moonRad).toFloat(),
                        center.y + moonDist * sin(moonRad).toFloat()
                    )
                    drawCircle(
                        color = theme.bentoSky.copy(alpha = 0.28f),
                        radius = 16.dp.toPx(),
                        center = moonOffset
                    )
                    drawCircle(
                        color = theme.bentoSky,
                        radius = 8.dp.toPx(),
                        center = moonOffset
                    )
                }
            }
        }

        // Sun & Moon Live Azimuth / Altitude Readout
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(theme.elevated)
                    .padding(12.dp)
            ) {
                Icon(Icons.Default.WbSunny, contentDescription = "Sun", tint = theme.bentoCoral)
                Column {
                    Text("SUN", style = type.caption.copy(fontWeight = FontWeight.Bold), color = theme.bentoCoral)
                    Text(
                        text = "Az ${state.sunPosition.azimuthDeg.roundToInt()}° • Alt ${state.sunPosition.altitudeDeg.roundToInt()}°",
                        style = type.caption,
                        color = theme.textPrimary
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(theme.elevated)
                    .padding(12.dp)
            ) {
                Icon(Icons.Default.NightlightRound, contentDescription = "Moon", tint = theme.bentoSky)
                Column {
                    Text("MOON", style = type.caption.copy(fontWeight = FontWeight.Bold), color = theme.bentoSky)
                    Text(
                        text = "Az ${state.moonPosition.azimuthDeg.roundToInt()}° • Alt ${state.moonPosition.altitudeDeg.roundToInt()}°",
                        style = type.caption,
                        color = theme.textPrimary
                    )
                }
            }
        }
    }

    // Today's Sun Path Arc Card (gated by outdoor_sun_moon)
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(theme.surface)
            .let { base ->
                if (isSunPathGoldenHourUnlocked) {
                    base.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                } else {
                    base.proBorder(RoundedCornerShape(24.dp))
                        .pressScaleClickable(
                            testTag = "sun_path_pro_gate",
                            onClick = onOpenSunPathUnlock
                        )
                }
            }
            .padding(18.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "TODAY'S SUN PATH ARC",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.bentoCoral
            )
            if (!isSunPathGoldenHourUnlocked) {
                ProBadge()
            }
        }
        if (isSunPathGoldenHourUnlocked) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            ) {
                val horizonY = size.height * 0.78f
                drawLine(
                    color = Color.White.copy(alpha = 0.16f),
                    start = Offset(0f, horizonY),
                    end = Offset(size.width, horizonY),
                    strokeWidth = 1.5.dp.toPx()
                )
                val path = Path().apply {
                    for (i in 0..100) {
                        val t = i / 100f
                        val x = t * size.width
                        val y = horizonY - sin(t * Math.PI).toFloat() * (size.height * 0.62f)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = theme.bentoCoral,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                val now = LocalTime.now()
                val progress = ((now.hour * 60 + now.minute) / 1440f).coerceIn(0f, 1f)
                val sunX = progress * size.width
                val sunY = horizonY - sin(progress * Math.PI).toFloat() * (size.height * 0.62f)
                drawCircle(color = theme.bentoCoral.copy(alpha = 0.3f), radius = 14.dp.toPx(), center = Offset(sunX, sunY))
                drawCircle(color = theme.bentoCoral, radius = 7.dp.toPx(), center = Offset(sunX, sunY))
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Sunrise ${formatAstroTime(report.sunrise, state.use24HourTime)}",
                    style = type.caption,
                    color = theme.textSecondary
                )
                Text(
                    text = "Noon ${formatAstroTime(report.solarNoon, state.use24HourTime)}",
                    style = type.caption,
                    color = theme.textPrimary
                )
                Text(
                    text = "Sunset ${formatAstroTime(report.sunset, state.use24HourTime)}",
                    style = type.caption,
                    color = theme.textSecondary
                )
            }
        } else {
            Text(
                text = "Tap to unlock the interactive Sun Path Arc, Golden Hour, and Blue Hour windows.",
                style = type.caption,
                color = theme.textSecondary
            )
        }
    }

    // Golden Hour & Blue Hour Card + Canvas-Drawn Moon Phase
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .weight(1.2f)
                .clip(RoundedCornerShape(24.dp))
                .background(theme.surface)
                .let { base ->
                    if (isSunPathGoldenHourUnlocked) {
                        base.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    } else {
                        base.proBorder(RoundedCornerShape(24.dp))
                            .pressScaleClickable(
                                testTag = "golden_blue_hour_pro_gate",
                                onClick = onOpenSunPathUnlock
                            )
                    }
                }
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "GOLDEN & BLUE HOUR",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.bentoCoral
                )
                if (!isSunPathGoldenHourUnlocked) {
                    ProBadge()
                }
            }
            if (isSunPathGoldenHourUnlocked) {
                Text(
                    text = "AM Blue: ${formatAstroTime(report.morningBlueHourStart, state.use24HourTime)} – ${
                        formatAstroTime(report.sunrise, state.use24HourTime)
                    }",
                    style = type.caption,
                    color = theme.textSecondary
                )
                Text(
                    text = "AM Golden: ${formatAstroTime(report.sunrise, state.use24HourTime)} – ${
                        formatAstroTime(report.morningGoldenHourEnd, state.use24HourTime)
                    }",
                    style = type.caption,
                    color = theme.textPrimary
                )
                Text(
                    text = "PM Golden: ${formatAstroTime(report.eveningGoldenHourStart, state.use24HourTime)} – ${
                        formatAstroTime(report.sunset, state.use24HourTime)
                    }",
                    style = type.caption,
                    color = theme.textPrimary
                )
                Text(
                    text = "PM Blue: ${formatAstroTime(report.sunset, state.use24HourTime)} – ${
                        formatAstroTime(report.eveningBlueHourEnd, state.use24HourTime)
                    }",
                    style = type.caption,
                    color = theme.textSecondary
                )
            } else {
                Text(
                    text = "Tap to unlock morning & evening Golden Hour and Blue Hour windows.",
                    style = type.caption,
                    color = theme.textSecondary
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(0.8f)
                .clip(RoundedCornerShape(24.dp))
                .background(theme.surface)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "MOON PHASE",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.bentoLavender
            )
            MoonPhaseCanvas(
                phaseFraction = report.moonPhaseFraction,
                accentColor = theme.bentoLavender,
                modifier = Modifier.size(68.dp)
            )
            Text(
                text = report.moonPhaseName,
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${report.moonIlluminationPercent}% Lit • Age ${String.format(Locale.US, "%.1f", report.moonAgeDays)}d",
                style = type.caption,
                color = theme.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Draws an accurate lunar phase disc (New, Waxing Crescent, First Quarter, Waxing Gibbous,
 * Full, Waning Gibbous, Last Quarter, Waning Crescent) on Compose Canvas.
 */
@Composable
private fun MoonPhaseCanvas(
    phaseFraction: Double,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        val darkSide = Color.White.copy(alpha = 0.12f)
        drawCircle(color = darkSide, radius = r, center = center)

        // Draw scanlines across the lunar disc to render the exact terminator curve
        val p = ((phaseFraction % 1.0) + 1.0) % 1.0
        val isWaxing = p <= 0.5
        val k = cos(p * 2.0 * Math.PI).toFloat() // +1 at New, -1 at Full

        val steps = 40
        for (i in -steps..steps) {
            val ny = i / steps.toFloat()
            val y = center.y + ny * r
            val halfChord = kotlin.math.sqrt((1f - ny * ny).coerceAtLeast(0f)) * r
            val terminatorX = k * halfChord
            val startX = if (isWaxing) center.x + terminatorX else center.x - halfChord
            val endX = if (isWaxing) center.x + halfChord else center.x - terminatorX
            if (endX > startX) {
                drawLine(
                    color = accentColor,
                    start = Offset(startX, y),
                    end = Offset(endX, y),
                    strokeWidth = (size.height / steps) + 1.5f,
                    cap = StrokeCap.Round
                )
            }
        }

        drawCircle(
            color = accentColor.copy(alpha = 0.5f),
            radius = r,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

// ============================================================================
// 3) WEATHER TAB (Open-Meteo + Offline Cache + Canvas Condition Icons + Outdoor Score)
// ============================================================================

@Composable
private fun WeatherSegmentContent(
    state: SolunarUiState,
    sensorState: SensorState,
    isPro: Boolean = false,
    onOpenPaywall: () -> Unit = {},
    onToggleCelsiusFahrenheit: () -> Unit,
    onRefreshWeather: () -> Unit,
    onSearchCityClick: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current
    val weather: WeatherSnapshot = state.weatherSnapshot ?: return
    val useCelsius = state.useCelsius

    val minutesAgo = remember(weather.fetchedAtMillis) {
        val diffMin = ((System.currentTimeMillis() - weather.fetchedAtMillis) / 60_000L).coerceAtLeast(0L)
        when {
            diffMin == 0L -> "Updated just now"
            diffMin == 1L -> "Updated 1 min ago"
            diffMin < 60L -> "Updated $diffMin min ago"
            else -> "Updated ${diffMin / 60L}h ago"
        }
    }

    // Top Hero Weather Card: Big Temperature, Canvas Condition Icon, Feels Like, Pressure Trend Arrow, °C/°F Toggle
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(theme.surface)
            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        // Header row: Cache status ("updated x min ago") + Refresh + °C/°F toggle
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (weather.isOfflineCached) "OPEN-METEO • OFFLINE CACHE" else "OPEN-METEO • LIVE FORECAST",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = theme.bentoLavender
                )
                Text(
                    text = if (state.isLoadingWeather) "Refreshing forecast..." else minutesAgo,
                    style = type.caption,
                    color = theme.textSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // °C / °F Pill Toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.elevated)
                        .border(1.dp, theme.accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .pressScaleClickable(
                            testTag = "weather_unit_toggle_button",
                            onClick = onToggleCelsiusFahrenheit
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (useCelsius) "°C • Metric" else "°F • Imperial",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = theme.accent
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(theme.elevated)
                        .pressScaleClickable(
                            testTag = "weather_refresh_button",
                            onClick = onRefreshWeather
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Weather",
                        tint = theme.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(theme.elevated)
                        .pressScaleClickable(
                            testTag = "weather_city_search_button",
                            onClick = onSearchCityClick
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search City",
                        tint = theme.bentoSky,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Big Temperature + Canvas Weather Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = formatTemp(weather.currentTempC, useCelsius),
                    style = type.hero.copy(fontWeight = FontWeight.ExtraBold),
                    color = theme.textPrimary
                )
                Text(
                    text = weather.conditionLabel,
                    style = type.title,
                    color = theme.accent
                )
                Text(
                    text = "Feels like ${formatTemp(weather.feelsLikeC, useCelsius)} • Rain chance ${weather.precipProbPercent}%",
                    style = type.body,
                    color = theme.textSecondary
                )
            }

            WeatherConditionCanvasIcon(
                kind = weather.conditionKind,
                iconSize = 84.dp
            )
        }

        // Pressure-Trend Arrow Strip
        val deltaFormatted = String.format(
            Locale.US,
            "%s%.1f hPa (3h)",
            if (weather.pressureDelta3hHpa >= 0) "+" else "",
            weather.pressureDelta3hHpa
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(theme.elevated)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = weather.pressureTrend.arrow,
                    style = type.title.copy(fontWeight = FontWeight.ExtraBold),
                    color = theme.accent
                )
                Column {
                    Text(
                        text = "Pressure ${weather.pressureTrend.label} • ${String.format(Locale.US, "%.1f", weather.pressureHpa)} hPa",
                        style = type.label.copy(fontWeight = FontWeight.Bold),
                        color = theme.textPrimary
                    )
                    Text(
                        text = "$deltaFormatted • Device sensor: ${String.format(Locale.US, "%.1f", sensorState.pressureHpa)} hPa",
                        style = type.caption,
                        color = theme.textSecondary
                    )
                }
            }
        }
    }

    // Reserved banner slot below current conditions
    InlineBannerAdSlot(placementTag = "weather_below_current")

    // Outdoor Score (0-100) Card from pressure trend, wind, cloud cover, and rain chance
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(theme.surface)
            .border(1.dp, theme.bentoLime.copy(alpha = 0.4f), RoundedCornerShape(26.dp))
            .padding(18.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(92.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 9.dp.toPx()
                drawArc(
                    color = Color.White.copy(alpha = 0.08f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = theme.bentoLime,
                    startAngle = 135f,
                    sweepAngle = 270f * (weather.outdoorScore / 100f),
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${weather.outdoorScore}",
                    style = type.gaugeMedium,
                    color = theme.textPrimary
                )
                Text(
                    text = "/ 100",
                    style = type.caption,
                    color = theme.textSecondary
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(
                text = "OUTDOOR SCORE",
                style = type.caption.copy(fontWeight = FontWeight.Bold),
                color = theme.bentoLime
            )
            Text(
                text = when {
                    weather.outdoorScore >= 80 -> "Ideal Field Conditions"
                    weather.outdoorScore >= 60 -> "Favorable Outdoors"
                    weather.outdoorScore >= 40 -> "Mixed Weather Window"
                    else -> "Challenging Conditions"
                },
                style = type.section,
                color = theme.textPrimary
            )
            Text(
                text = "Computed from ${weather.pressureTrend.label.lowercase(Locale.US)} pressure, " +
                    "${weather.windSpeedKmh.roundToInt()} km/h wind, ${weather.cloudCoverPercent}% cloud cover & ${weather.precipProbPercent}% rain chance.",
                style = type.caption,
                color = theme.textSecondary
            )
        }
    }

    // 24-Hour Hourly Strip (Premium: hourly and 7-day Weather)
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(theme.surface)
            .let { base ->
                if (isPro) base.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                else base.proBorder(RoundedCornerShape(24.dp))
            }
            .pressScaleClickable(
                enabled = !isPro,
                testTag = "weather_hourly_pro_gate",
                onClick = onOpenPaywall
            )
            .padding(18.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "HOURLY FORECAST (24H)",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = if (isPro) theme.textSecondary else ProGold
                )
                if (!isPro) {
                    ProBadge()
                }
            }
            if (!isPro) {
                Text(
                    text = "Tap to unlock",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = ProGold
                )
            }
        }
        val visibleHourly = if (isPro) weather.hourly else weather.hourly.take(3)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            visibleHourly.forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(theme.elevated)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = item.hourLabel,
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = if (item.hourLabel == "Now") theme.accent else theme.textSecondary
                    )
                    WeatherConditionCanvasIcon(
                        kind = item.conditionKind,
                        iconSize = 30.dp
                    )
                    Text(
                        text = formatTemp(item.tempC, useCelsius),
                        style = type.label.copy(fontWeight = FontWeight.Bold),
                        color = theme.textPrimary
                    )
                    Text(
                        text = "${item.precipProbPercent}%",
                        style = type.caption,
                        color = theme.bentoSky
                    )
                }
            }
            if (!isPro) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(theme.elevated)
                        .proBorder(RoundedCornerShape(18.dp))
                        .pressScaleClickable(onClick = onOpenPaywall)
                        .padding(horizontal = 16.dp, vertical = 22.dp)
                ) {
                    ProBadge()
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "+21 Hours",
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = ProGold
                    )
                }
            }
        }
    }

    // 7-Day Daily Forecast List (Premium: hourly and 7-day Weather)
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(theme.surface)
            .let { base ->
                if (isPro) base.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                else base.proBorder(RoundedCornerShape(24.dp))
            }
            .padding(18.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "7-DAY FORECAST",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = if (isPro) theme.textSecondary else ProGold
                )
                if (!isPro) {
                    ProBadge()
                }
            }
            if (!isPro) {
                Text(
                    text = "Unlock 7-Day Forecast",
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = ProGold,
                    modifier = Modifier.pressScaleClickable(onClick = onOpenPaywall)
                )
            }
        }
        weather.daily.forEachIndexed { idx, day ->
            val isLockedDay = idx > 0 && !isPro
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.elevated)
                    .let { base -> if (isLockedDay) base.proBorder(RoundedCornerShape(16.dp)) else base }
                    .pressScaleClickable(
                        enabled = isLockedDay,
                        onClick = onOpenPaywall
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(modifier = Modifier.width(88.dp)) {
                    Text(
                        text = day.dayLabel,
                        style = type.body.copy(fontWeight = FontWeight.Bold),
                        color = if (isLockedDay) ProGold else theme.textPrimary
                    )
                    Text(
                        text = if (isLockedDay) "Unlock with Pro" else day.conditionLabel,
                        style = type.caption,
                        color = theme.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isLockedDay) {
                    ProBadge()
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeatherConditionCanvasIcon(kind = day.conditionKind, iconSize = 26.dp)
                        Text(
                            text = "${day.precipProbMaxPercent}%",
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.bentoSky,
                            modifier = Modifier.width(34.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatTemp(day.tempMinC, useCelsius),
                            style = type.caption,
                            color = theme.textSecondary
                        )
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(theme.bentoSky, theme.accent, theme.bentoCoral)
                                    )
                                )
                        )
                        Text(
                            text = formatTemp(day.tempMaxC, useCelsius),
                            style = type.label.copy(fontWeight = FontWeight.Bold),
                            color = theme.textPrimary
                        )
                    }
                }
            }
        }
    }

    // Weather Telemetry Grid: Wind, Humidity, UV, Visibility, Sunrise/Sunset, Barometer
    val windDisplay = if (useCelsius) {
        "${weather.windSpeedKmh.roundToInt()} km/h ${weather.windCardinal}"
    } else {
        "${(weather.windSpeedKmh * 0.621371).roundToInt()} mph ${weather.windCardinal}"
    }
    val visDisplay = if (useCelsius) {
        String.format(Locale.US, "%.1f km", weather.visibilityKm)
    } else {
        String.format(Locale.US, "%.1f mi", weather.visibilityKm * 0.621371)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            WeatherMetricTile(
                icon = Icons.Default.Air,
                label = "WIND",
                value = windDisplay,
                subtitle = "Direction ${weather.windDirectionDeg}°",
                accent = theme.bentoSky,
                modifier = Modifier.weight(1f)
            )
            WeatherMetricTile(
                icon = Icons.Default.WaterDrop,
                label = "HUMIDITY",
                value = "${weather.humidityPercent}%",
                subtitle = "Cloud cover ${weather.cloudCoverPercent}%",
                accent = theme.bentoLavender,
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            WeatherMetricTile(
                icon = Icons.Default.WbSunny,
                label = "UV INDEX",
                value = String.format(Locale.US, "%.1f", weather.uvIndex),
                subtitle = when {
                    weather.uvIndex >= 8 -> "Very High UV"
                    weather.uvIndex >= 6 -> "High UV"
                    weather.uvIndex >= 3 -> "Moderate UV"
                    else -> "Low UV"
                },
                accent = theme.bentoCoral,
                modifier = Modifier.weight(1f)
            )
            WeatherMetricTile(
                icon = Icons.Default.Visibility,
                label = "VISIBILITY",
                value = visDisplay,
                subtitle = "Clear line of sight",
                accent = theme.bentoLime,
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            WeatherMetricTile(
                icon = Icons.Default.WbSunny,
                label = "SUNRISE & SUNSET",
                value = "${weather.sunriseLabel} / ${weather.sunsetLabel}",
                subtitle = "Local solar horizon",
                accent = theme.bentoCoral,
                modifier = Modifier.weight(1f)
            )
            WeatherMetricTile(
                icon = Icons.Default.Speed,
                label = "PRESSURE TREND",
                value = "${weather.pressureTrend.arrow} ${String.format(Locale.US, "%.1f", weather.pressureHpa)} hPa",
                subtitle = "${weather.pressureTrend.label} (${String.format(Locale.US, "%+.1f", weather.pressureDelta3hHpa)} hPa)",
                accent = theme.accent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WeatherMetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(theme.surface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(imageVector = icon, contentDescription = label, tint = accent, modifier = Modifier.size(16.dp))
            Text(text = label, style = type.caption.copy(fontWeight = FontWeight.Bold), color = accent)
        }
        Text(
            text = value,
            style = type.section.copy(fontWeight = FontWeight.Bold),
            color = theme.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = type.caption,
            color = theme.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Draws custom vector-style Weather Condition Icons on Compose Canvas
 * (Clear Sun, Partly Cloudy, Cloudy, Fog, Rain, Snow, Thunderstorm).
 */
@Composable
fun WeatherConditionCanvasIcon(
    kind: WeatherConditionKind,
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    val theme = LocalDyneTheme.current
    val sunColor = theme.bentoCoral
    val cloudColor = Color.White.copy(alpha = 0.88f)
    val rainColor = theme.bentoSky
    val boltColor = theme.accent

    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f

        when (kind) {
            WeatherConditionKind.CLEAR -> {
                val r = w * 0.24f
                drawCircle(color = sunColor.copy(alpha = 0.25f), radius = r * 1.45f, center = Offset(cx, cy))
                drawCircle(color = sunColor, radius = r, center = Offset(cx, cy))
                for (deg in 0 until 360 step 45) {
                    val rad = Math.toRadians(deg.toDouble())
                    val r1 = r * 1.28f
                    val r2 = r * 1.68f
                    drawLine(
                        color = sunColor,
                        start = Offset(cx + r1 * cos(rad).toFloat(), cy + r1 * sin(rad).toFloat()),
                        end = Offset(cx + r2 * cos(rad).toFloat(), cy + r2 * sin(rad).toFloat()),
                        strokeWidth = (w * 0.06f).coerceAtLeast(2f),
                        cap = StrokeCap.Round
                    )
                }
            }

            WeatherConditionKind.PARTLY_CLOUDY -> {
                val sunCenter = Offset(w * 0.38f, h * 0.38f)
                val sunR = w * 0.18f
                drawCircle(color = sunColor, radius = sunR, center = sunCenter)
                drawRoundRect(
                    color = cloudColor,
                    topLeft = Offset(w * 0.25f, h * 0.46f),
                    size = Size(w * 0.56f, h * 0.28f),
                    cornerRadius = CornerRadius(h * 0.14f, h * 0.14f)
                )
                drawCircle(color = cloudColor, radius = w * 0.16f, center = Offset(w * 0.44f, h * 0.46f))
                drawCircle(color = cloudColor, radius = w * 0.13f, center = Offset(w * 0.62f, h * 0.49f))
            }

            WeatherConditionKind.CLOUDY -> {
                drawRoundRect(
                    color = cloudColor,
                    topLeft = Offset(w * 0.18f, h * 0.44f),
                    size = Size(w * 0.64f, h * 0.30f),
                    cornerRadius = CornerRadius(h * 0.15f, h * 0.15f)
                )
                drawCircle(color = cloudColor, radius = w * 0.18f, center = Offset(w * 0.40f, h * 0.42f))
                drawCircle(color = cloudColor, radius = w * 0.15f, center = Offset(w * 0.60f, h * 0.45f))
            }

            WeatherConditionKind.FOG -> {
                for (i in 0..2) {
                    val y = h * (0.35f + i * 0.16f)
                    drawLine(
                        color = cloudColor.copy(alpha = 0.75f),
                        start = Offset(w * 0.20f, y),
                        end = Offset(w * 0.80f, y),
                        strokeWidth = (w * 0.08f).coerceAtLeast(3f),
                        cap = StrokeCap.Round
                    )
                }
            }

            WeatherConditionKind.RAIN -> {
                drawRoundRect(
                    color = cloudColor,
                    topLeft = Offset(w * 0.18f, h * 0.30f),
                    size = Size(w * 0.64f, h * 0.28f),
                    cornerRadius = CornerRadius(h * 0.14f, h * 0.14f)
                )
                drawCircle(color = cloudColor, radius = w * 0.16f, center = Offset(w * 0.42f, h * 0.30f))
                for (i in 0..2) {
                    val rx = w * (0.32f + i * 0.18f)
                    drawLine(
                        color = rainColor,
                        start = Offset(rx, h * 0.64f),
                        end = Offset(rx - w * 0.06f, h * 0.82f),
                        strokeWidth = (w * 0.06f).coerceAtLeast(2.5f),
                        cap = StrokeCap.Round
                    )
                }
            }

            WeatherConditionKind.SNOW -> {
                drawRoundRect(
                    color = cloudColor,
                    topLeft = Offset(w * 0.18f, h * 0.30f),
                    size = Size(w * 0.64f, h * 0.28f),
                    cornerRadius = CornerRadius(h * 0.14f, h * 0.14f)
                )
                for (i in 0..2) {
                    val sx = w * (0.32f + i * 0.18f)
                    drawCircle(
                        color = rainColor,
                        radius = (w * 0.045f).coerceAtLeast(2.5f),
                        center = Offset(sx, h * 0.74f)
                    )
                }
            }

            WeatherConditionKind.THUNDERSTORM -> {
                drawRoundRect(
                    color = cloudColor,
                    topLeft = Offset(w * 0.18f, h * 0.26f),
                    size = Size(w * 0.64f, h * 0.28f),
                    cornerRadius = CornerRadius(h * 0.14f, h * 0.14f)
                )
                val bolt = Path().apply {
                    moveTo(w * 0.52f, h * 0.54f)
                    lineTo(w * 0.40f, h * 0.70f)
                    lineTo(w * 0.52f, h * 0.70f)
                    lineTo(w * 0.44f, h * 0.88f)
                }
                drawPath(
                    path = bolt,
                    color = boltColor,
                    style = Stroke(width = (w * 0.07f).coerceAtLeast(3f), cap = StrokeCap.Round)
                )
            }
        }
    }
}

/**
 * Glass Sheet for City Search (via Open-Meteo Geocoding + offline preset cities) and manual Lat/Lng coordinates.
 */
@Composable
private fun ManualLocationGlassSheet(
    initialLat: Double,
    initialLng: Double,
    initialLabel: String,
    cityResults: List<CitySearchResult>,
    hazeState: HazeState?,
    onSearchCityQuery: (String) -> Unit,
    onSave: (Double, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalDyneTheme.current
    val type = LocalDyneType.current

    var cityQuery by rememberSaveable { mutableStateOf("") }
    var labelText by rememberSaveable { mutableStateOf(initialLabel) }
    var latText by rememberSaveable { mutableStateOf(initialLat.toString()) }
    var lngText by rememberSaveable { mutableStateOf(initialLng.toString()) }

    GlassSheet(
        onDismissRequest = onDismiss,
        hazeState = hazeState
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Location & City Search", style = type.title, color = theme.textPrimary)
            Text(
                text = "Search any city worldwide or enter exact latitude and longitude.",
                style = type.caption,
                color = theme.textSecondary
            )

            // City search box (52.dp high, singleLine = true)
            OutlinedTextField(
                value = cityQuery,
                onValueChange = {
                    cityQuery = it
                    onSearchCityQuery(it)
                },
                placeholder = {
                    Text(
                        text = "Search cities",
                        style = type.body,
                        color = theme.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search City", tint = theme.accent)
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.accent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )

            // City Search Results Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalChipRowEdgeFade()
            ) {
                items(cityResults.take(6)) { city ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.elevated)
                            .border(1.dp, theme.accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .pressScaleClickable(onClick = {
                                onSave(city.latitude, city.longitude, city.displayTitle)
                            })
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = city.displayTitle,
                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                            color = theme.textPrimary,
                            maxLines = 1
                        )
                    }
                }
            }

            OutlinedTextField(
                value = labelText,
                onValueChange = { labelText = it },
                label = { Text("Custom Location Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.accent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it },
                    label = { Text("Latitude (-90..90)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = lngText,
                    onValueChange = { lngText = it },
                    label = { Text("Longitude (-180..180)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GlassButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "Apply Coordinates",
                    accentFill = theme.accent,
                    onClick = {
                        val lat = latText.toDoubleOrNull()?.coerceIn(-90.0, 90.0) ?: initialLat
                        val lng = lngText.toDoubleOrNull()?.coerceIn(-180.0, 180.0) ?: initialLng
                        onSave(lat, lng, labelText)
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "save_manual_coordinates_button"
                )
            }
        }
    }
}
